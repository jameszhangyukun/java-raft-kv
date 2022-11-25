package com.zyk.raft.kv.impl;

import com.zyk.raft.client.ClientKVAck;
import com.zyk.raft.client.ClientKVReq;
import com.zyk.raft.kv.*;
import com.zyk.raft.kv.constants.NodeStatus;
import com.zyk.raft.kv.current.RaftThreadPool;
import com.zyk.raft.kv.entity.*;
import com.zyk.raft.kv.exception.RaftRemotingException;
import com.zyk.raft.kv.membership.ClusterMembershipChanges;
import com.zyk.raft.kv.membership.Result;
import com.zyk.raft.kv.raft.*;
import com.zyk.raft.kv.util.LongConvert;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zyk.raft.kv.constants.NodeStatus.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Getter
@Setter
@Slf4j
public class DefaultNode implements Node, ClusterMembershipChanges {


    /**
     * 选举时间间隔基数
     */
    public volatile long electionTime = 15 * 1000;
    /**
     * 上一次选举时间
     */
    public volatile long preElectionTime = 0;

    /**
     * 上次一心跳时间戳
     */
    public volatile long preHeartBeatTime = 0;
    /**
     * 心跳间隔基数
     */
    public final long heartBeatTick = 5 * 1000;

    PeerSet peerSet;


    /**
     * 节点当前状态
     */
    public volatile int status = NodeStatus.FOLLOWER;

    volatile boolean running = false;


    StateMachine stateMachine;

    /* ============ 所有服务器上持久存在的 ============= */
    /**
     * 当前节点知道的最后一次任期号
     */
    volatile long currentTerm = 0;
    /**
     * 当前获得选票的候选人Id
     */
    volatile String votedFor;
    /**
     * 日志条目模块，每一个日志条目包含一个用户状态机执行的指令和收到时的任期号
     */
    LogModule logModule;

    /* ============ 服务器上经常改变的 ============= */
    /**
     * 已知的最大的已经被提交的日志条目的索引值
     */
    volatile long commitIndex;
    /**
     * 最后被应用到状态的日志条目索引
     */
    volatile long lastApplies = 0;

    /* ============ leader上经常改变的 ============= */
    /**
     * 对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一）
     */
    Map<Peer, Long> nextIndexs;

    /**
     * 对于每一个服务器，已经复制给他的日志的最高索引值
     */
    Map<Peer, Long> matchIndexs;

    private NodeConfig nodeConfig;

    public RpcService rpcServer;

    public RpcClient rpcClient = new DefaultRpcClient();

    Consensus consensus;

    ClusterMembershipChanges delegate;

    private HeartbeatTask heartbeatTask = new HeartbeatTask();

    private ElectionTask electionTask = new ElectionTask();

    private ReplicationFailQueueConsumer replicationFailQueueConsumer =
            new ReplicationFailQueueConsumer();

    private LinkedBlockingQueue<ReplicationFailModel> replicationFailModels =
            new LinkedBlockingQueue<>();


    private DefaultNode() {

    }


    public static DefaultNode getInstance() {
        return DefaultNodeLazyHolder.INSTANCE;
    }

    @Override
    public Result addPeer(Peer newPeer) {
        return delegate.addPeer(newPeer);
    }

    @Override
    public Result removePeer(Peer oldPeer) {
        return delegate.removePeer(oldPeer);
    }

    private static class DefaultNodeLazyHolder {
        private static final DefaultNode INSTANCE = new DefaultNode();
    }


    @Override
    public void init() throws Throwable {
        running = true;
        rpcClient.init();
        rpcServer.init();

        consensus = new DefaultConsensusImpl(this);
        delegate = new ClusterMembershipChangesImpl();

        RaftThreadPool.scheduleWithFixedDelay(heartbeatTask, 500);
        RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
        RaftThreadPool.execute(replicationFailQueueConsumer);

        LogEntry logEntry = logModule.getLast();
        if (logEntry != null) {
            currentTerm = logEntry.getTerm();
        }
        log.info("start success, selfId : {} ", peerSet.getSelf());
    }

    @Override
    public void destroy() throws Throwable {
        rpcServer.destroy();
        stateMachine.destroy();
        rpcClient.destroy();
        running = false;
        log.info("destroy success");
    }

    @Override
    public void setConfig(NodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
        this.stateMachine = nodeConfig.stateMachineSaveType.stateMachine;
        logModule = DefaultLogModule.getInstance();

        peerSet = PeerSet.getInstance();
        for (String s : nodeConfig.getPeerAddrs()) {
            Peer peer = new Peer(s);
            peerSet.addPeer(peer);
            if (s.equals("localhost:" + nodeConfig.getSelfPort())) {
                peerSet.setSelf(peer);
            }
        }
        rpcServer = new DefaultRpcServiceImpl(
                nodeConfig.getSelfPort(),
                this
        );
    }

    class ElectionTask implements Runnable {
        @Override
        public void run() {
            if (status == LEADER) {
                return;
            }
            long current = System.currentTimeMillis();
            // 使用随机算法避免冲突
            electionTime = electionTime + ThreadLocalRandom.current().nextInt(50);

            if (current - preElectionTime < electionTime) {
                return;
            }
            status = CANDIDATE;
            log.error("node {} will become CANDIDATE and start election leader, current term : [{}], LastEntry : [{}]",
                    peerSet.getSelf(), currentTerm, logModule.getLast());
            // 下一次选举的时间
            preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(250);
            currentTerm += 1;
            votedFor = peerSet.getSelf().getAddr();
            List<Peer> peers = peerSet.getPeers();
            ArrayList<Future<VoteResult>> futureVoteResult = new ArrayList<>(peers.size());
            for (Peer peer : peers) {
                futureVoteResult.add(RaftThreadPool.submit(() -> {
                    long lastTerm = 0L;
                    LogEntry last = logModule.getLast();
                    if (last != null) {
                        lastTerm = last.getTerm();
                    }

                    VoteParam param = VoteParam.builder()
                            .term(currentTerm)
                            .candidateId(peerSet.getSelf().getAddr())
                            .lastLogIndex(LongConvert.convert(logModule.getLastIndex()))
                            .lastLogTerm(lastTerm)
                            .build();

                    Request request = Request.builder()
                            .cmd(Request.R_VOTE)
                            .obj(param)
                            .url(peer.getAddr())
                            .build();
                    try {
                        return getRpcClient().<VoteResult>send(request);
                    } catch (RaftRemotingException e) {
                        log.error("ElectionTask RPC Fail , URL : " + request.getUrl());
                        return null;
                    }
                }));
            }

            log.info("futureVoteResult.size() : {}", futureVoteResult.size());
            AtomicInteger success2 = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureVoteResult.size());
            for (Future future : futureVoteResult) {
                RaftThreadPool.submit(() -> {
                    try {
                        Response<VoteResult> response = (Response<VoteResult>) future.get(3000, MILLISECONDS);
                        if (response == null) {
                            return -1;
                        }
                        boolean isVoteGranted = response.getResult().isVoteGranted();

                        if (isVoteGranted) {
                            success2.incrementAndGet();
                        } else {
                            long resTerm = response.getResult().getTerm();
                            if (resTerm >= currentTerm) {
                                currentTerm = resTerm;
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        log.error("future.get exception , e : ", e);
                        return -1;
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                // 稍等片刻
                latch.await(3500, MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("InterruptedException By Master election Task");
            }

            int success = success2.get();
            log.info("node {} maybe become leader , success count = {} , status : {}", peerSet.getSelf(), success, NodeStatus.Enum.value(status));
            // 如果投票期间,有其他服务器发送 appendEntry , 就可能变成 follower ,这时,应该停止.
            if (status == NodeStatus.FOLLOWER) {
                return;
            }
            // 加上自身.
            if (success >= peers.size() / 2) {
                log.warn("node {} become leader ", peerSet.getSelf());
                status = LEADER;
                peerSet.setLeader(peerSet.getSelf());
                votedFor = "";
                becomeLeaderToDoThing();
            } else {
                // else 重新选举
                votedFor = "";
            }

        }
    }

    /**
     * 初始化所有的nextIndex值为自己的最后一条日志的index + 1
     * 如果下次RPC时，follower和leader就会不一致
     * 那么leader就会尝试递减nextIndex 并进行重试，最终达成一致
     */
    private void becomeLeaderToDoThing() {
        nextIndexs = new ConcurrentHashMap<>();
        matchIndexs = new ConcurrentHashMap<>();
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            nextIndexs.put(peer, logModule.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }
    }

    /**
     * 复制到其他机器上
     *
     * @param peer
     * @param entry
     * @return
     */
    public Future<Boolean> replication(Peer peer, LogEntry entry) {
        return RaftThreadPool.submit(() -> {
            long start = System.currentTimeMillis(), end = start;
            while (end - start < 20 * 1000L) {
                AppendEntryParam appendEntryParam = AppendEntryParam.builder()
                        .term(currentTerm)
                        .leaderId(peerSet.getSelf().getAddr())
                        .serverId(peer.getAddr())
                        .leaderCommit(commitIndex)
                        .build();

                Long nextIndex = nextIndexs.get(peer);
                LinkedList<LogEntry> logEntries = new LinkedList<>();
                if (entry.getIndex() >= nextIndex) {
                    for (long i = nextIndex; i <= entry.getIndex(); i++) {
                        LogEntry l = logModule.read(i);
                        if (l != null) {
                            logEntries.add(l);
                        }
                    }
                } else {
                    logEntries.add(entry);
                }
                // 最小的日志
                LogEntry preLog = getPreLog(logEntries.getFirst());
                appendEntryParam.setPrevLogIndex(preLog.getIndex());
                appendEntryParam.setPrevLogTerm(preLog.getTerm());
                appendEntryParam.setLogEntries(logEntries.toArray(new LogEntry[0]));

                Request request = Request.builder()
                        .url(peer.getAddr())
                        .cmd(Request.A_ENTRIES)
                        .obj(appendEntryParam)
                        .build();

                try {
                    AppendEntryResult result = getRpcClient().send(request);
                    if (result == null) {
                        return false;
                    }
                    if (result.isSuccess()) {
                        log.info("append follower entry success , follower=[{}], entry=[{}]", peer, appendEntryParam.getLogEntries());
                        // update 这两个追踪值
                        nextIndexs.put(peer, entry.getIndex() + 1);
                        matchIndexs.put(peer, entry.getIndex());
                        return true;
                    } else {
                        if (result.getTerm() > currentTerm) {
                            log.warn("follower [{}] term [{}] than more self, and my term = [{}], so, I will become follower",
                                    peer, result.getTerm(), currentTerm);
                            currentTerm = result.getTerm();
                            status = NodeStatus.FOLLOWER;
                            return false;
                        } else {
                            if (nextIndex == 0) {
                                nextIndex = 1L;
                            }
                            nextIndexs.put(peer, nextIndex - 1);
                            log.warn("follower {} nextIndex not match, will reduce nextIndex and retry RPC append, nextIndex : [{}]", peer.getAddr(),
                                    nextIndex);
                        }
                    }
                    end = System.currentTimeMillis();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                    return false;
                }

            }
            return true;
        });
    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);

        if (entry == null) {
            log.warn("get perLog is null , parameter logEntry : {}", logEntry);
            entry = LogEntry.builder().index(0L).term(0).command(null).build();
        }
        return entry;
    }

    class ReplicationFailQueueConsumer implements Runnable {
        long intervalTime = 1000 * 60;

        @Override
        public void run() {
            while (running) {
                try {
                    ReplicationFailModel model = replicationFailModels.poll(1000, MILLISECONDS);
                    if (model == null) {
                        continue;
                    }
                    if (status != LEADER) {
                        replicationFailModels.clear();
                        continue;
                    }
                    log.warn("replication Fail Queue Consumer take a task, will be retry replication, content detail : [{}]", model.logEntry);
                    long offerTime = model.offerTime;
                    if (System.currentTimeMillis() - offerTime > intervalTime) {
                        log.warn("replication Fail event Queue maybe full or handler slow");
                    }
                    Callable callable = model.callable;
                    Future<Boolean> future = RaftThreadPool.submit(callable);
                    Boolean r = future.get(3000, MILLISECONDS);
                    // 重试成功
                    if (r) {
                        // 有可能应用到状态机
                        tryApplyStateMachine(model);
                    }
                } catch (InterruptedException e) {

                } catch (ExecutionException | TimeoutException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }

    /**
     * 尝试应用到状态机
     *
     * @param replicationFailModel
     */
    private void tryApplyStateMachine(ReplicationFailModel replicationFailModel) {
        String success = stateMachine.getString(replicationFailModel.successKey);
        stateMachine.setString(replicationFailModel.successKey, String.valueOf(Integer.parseInt(success) + 1));

        String count = stateMachine.getString(replicationFailModel.countKey);

        if (Integer.parseInt(success) >= Integer.parseInt(count) / 2) {
            stateMachine.apply(replicationFailModel.logEntry);
            stateMachine.delString(replicationFailModel.countKey, replicationFailModel.countKey);
        }
    }

    /**
     * 心跳线程
     */
    class HeartbeatTask implements Runnable {
        @Override
        public void run() {
            // 1. 判断是否是leader
            if (status != LEADER) {
                return;
            }
            // 判断是否到达心跳间隔
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - preHeartBeatTime < heartBeatTick) {
                return;
            }
            log.info("=========== NextIndex =============");
            for (Peer peer : peerSet.getPeersWithOutSelf()) {
                log.info("Peer {} nextIndex={}", peer.getAddr(), nextIndexs.get(peer));
            }

            preHeartBeatTime = System.currentTimeMillis();

            for (Peer peer : peerSet.getPeersWithOutSelf()) {
                // 发送空心跳
                AppendEntryParam param = AppendEntryParam.builder()
                        .logEntries(null)
                        .leaderId(peerSet.getLeader().getAddr())
                        .term(currentTerm)
                        .serverId(peer.getAddr())
                        .build();

                Request request = new Request(Request.A_ENTRIES, param, peer.getAddr());
                RaftThreadPool.execute(() -> {
                    try {
                        AppendEntryResult appendEntryResult = getRpcClient().send(request);
                        long term = appendEntryResult.getTerm();

                        if (term > currentTerm) {
                            log.error("self will become follower, he's term : {}, my term : {}", term, currentTerm);
                            currentTerm = term;
                            votedFor = "";
                            status = NodeStatus.FOLLOWER;
                        }
                    } catch (Exception e) {
                        log.error("HeartBeatTask RPC Fail, request URL : {} ", request.getUrl());
                    }
                }, false);
            }
        }
    }

    @Override
    public VoteResult handlerRequestVote(VoteParam voteParam) {
        log.warn("handlerRequestVote will be invoke, param info : {}", voteParam);
        return consensus.requestVote(voteParam);
    }

    @Override
    public AppendEntryResult handlerAppendEntries(AppendEntryParam param) {
        if (param.getLogEntries() != null) {
            log.warn("node receive node {} append entry, entry content = {}", param.getLeaderId(), param.getLogEntries());
        }
        return consensus.appendEntries(param);
    }

    @Override
    public synchronized ClientKVAck handlerClientRequest(ClientKVReq request) {
        log.warn("handlerClientRequest handler {} operation,  and key : [{}], value : [{}]",
                ClientKVReq.Type.value(request.getType()), request.getKey(), request.getValue());
        if (status != LEADER) {
            log.warn("I not am leader , only invoke redirect method, leader addr : {}, my addr : {}",
                    peerSet.getLeader(), peerSet.getSelf().getAddr());
            return redirect(request);
        }

        if (request.getType() == ClientKVReq.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return new ClientKVAck(logEntry.getCommand());
            }
            return new ClientKVAck(null);
        }

        LogEntry logEntry = LogEntry.builder()
                .term(currentTerm)
                .command(Command.builder()
                        .key(request.getKey())
                        .value(request.getValue())
                        .build())
                .build();

        // 与提交到本地日志
        logModule.write(logEntry);
        log.info("write logModule success, logEntry info : {}, log index : {}", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);
        List<Future<Boolean>> futures = new ArrayList<>();
        int count = 0;
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            count++;
            futures.add(replication(peer, logEntry));
        }

        CountDownLatch latch = new CountDownLatch(futures.size());
        List<Boolean> results = new CopyOnWriteArrayList<>();

        getRPCAppendResult(futures, latch, results);
        try {
            latch.await(4000, MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        for (Boolean aBoolean : results) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }

        // 如果存在一个满足 N > commitIndex的N，并且大多数的commitIndex[i]>=N 成立
        // 并且log[N].term == currentTerm成立，那么令commitIndex等于N （5.3 和 5.4 节）
        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());

        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        Long N = matchIndexList.get(median);
        if (N > commitIndex) {
            LogEntry entry = logModule.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitIndex = N;
            }
        }

        if (success.get() >= count / 2) {
            // 更新
            commitIndex = logEntry.getIndex();
            // 应用到状态机
            getStateMachine().apply(logEntry);
            lastApplies = commitIndex;
            log.info("success apply local state machine,  logEntry info : {}", logEntry);
            // 返回成功.
            return ClientKVAck.ok();
        } else {
            // 回滚已经提交的日志.
            logModule.removeOnStartIndex(logEntry.getIndex());
            log.warn("fail apply local state  machine,  logEntry info : {}", logEntry);
            // TODO 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
            // 这里应该返回错误, 因为没有成功复制过半机器.
            return ClientKVAck.fail();
        }


    }


    private void getRPCAppendResult(List<Future<Boolean>> futures, CountDownLatch countDownLatch,
                                    List<Boolean> results) {
        for (Future<Boolean> future : futures) {
            RaftThreadPool.execute(() -> {
                try {
                    results.add(future.get(3000, MILLISECONDS));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
    }

    @Override
    public ClientKVAck redirect(ClientKVReq req) {
        Request request = Request.builder()
                .url(peerSet.getLeader().getAddr())
                .cmd(Request.CLIENT_REQ)
                .obj(req)
                .build();
        return rpcClient.send(request);
    }
}
