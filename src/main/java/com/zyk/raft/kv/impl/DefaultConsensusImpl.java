package com.zyk.raft.kv.impl;

import com.zyk.raft.kv.Consensus;
import com.zyk.raft.kv.constants.NodeStatus;
import com.zyk.raft.kv.entity.*;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一致性模块的默认实现
 */
public class DefaultConsensusImpl implements Consensus {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsensusImpl.class);


    public final DefaultNode node;
    public final ReentrantLock voteLock = new ReentrantLock();
    public final ReentrantLock appendLock = new ReentrantLock();

    public DefaultConsensusImpl(DefaultNode node) {
        this.node = node;
    }

    @Override
    public VoteResult requestVote(VoteParam param) {
        try {
            VoteResult.Builder builder = VoteResult.newBuilder();

            if (!voteLock.tryLock()) {
                return builder.term(node.getCurrentTerm()).voteGranted(false).build();
            }

            // 对方的term没有自己的新
            if (param.getTerm() < node.getCurrentTerm()) {
                return builder.term(node.getCurrentTerm()).voteGranted(false).build();
            }

            // (当前节点并没有投票 或者 已经投票过了且是对方节点) && 对方日志和自己一样新
            LOGGER.info("node {} current vote for [{}], param candidateId : {}", node.peerSet.getSelf(), node.getVotedFor(), param.getCandidateId());
            LOGGER.info("node {} current term {}, peer term : {}", node.peerSet.getSelf(), node.getCurrentTerm(), param.getTerm());

            if ((StringUtil.isNullOrEmpty(node.votedFor) || node.getVotedFor().equals(param.getCandidateId()))) {
                if (node.getLogModule().getLast() != null &&
                        (node.getLogModule().getLast().getTerm() > param.getLastLogTerm() || node.getLogModule().getLastIndex() > param.getLastLogIndex())) {
                    return VoteResult.fail();
                }

                node.status = NodeStatus.FOLLOWER;
                node.votedFor = param.getCandidateId();
                node.currentTerm = param.getTerm();
                node.peerSet.setLeader(new Peer(param.getServerId()));

                return builder.term(node.getCurrentTerm()).voteGranted(true).build();
            }

            return builder.term(node.getCurrentTerm()).voteGranted(false).build();
        } finally {
            voteLock.unlock();
        }
    }

    @Override
    public AppendEntryResult appendEntries(AppendEntryParam param) {
        try {
            AppendEntryResult.Builder builder = AppendEntryResult.newBuilder();
            if (appendLock.tryLock()) {
                return builder.term(node.currentTerm).success(false).build();
            }

            if (param.getTerm() < node.currentTerm) {
                return builder.term(node.currentTerm).success(false).build();
            }
            node.preHeartBeatTime = System.currentTimeMillis();
            node.preElectionTime = System.currentTimeMillis();
            node.peerSet.setLeader(new Peer(param.getLeaderId()));

            if (param.getTerm() >= node.getCurrentTerm()) {
                LOGGER.debug("node {} become FOLLOWER, currentTerm : {}, param Term : {}, param serverId {}",
                        node.peerSet.getSelf(), node.currentTerm, param.getTerm(), param.getServerId());

                node.status = NodeStatus.FOLLOWER;
            }
            node.currentTerm = param.getTerm();

            // 心跳
            if (param.getLogEntries() == null || param.getLogEntries().length == 0) {
                LOGGER.info("node {} append heartbeat success , he's term : {}, my term : {}",
                        param.getLeaderId(), param.getTerm(), node.getCurrentTerm());
                return builder.term(node.getCurrentTerm()).success(true).build();
            }

            if (node.getLogModule().getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = node.getLogModule().read(param.getPrevLogIndex())) != null) {
                    if (logEntry.getTerm() != param.getPrevLogTerm()) {
                        return AppendEntryResult.fail();
                    }
                } else {
                    return AppendEntryResult.fail();
                }
            }
            // 如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的
            LogEntry logEntry = node.getLogModule().read(param.getPrevLogIndex() + 1);
            if (logEntry != null && logEntry.getTerm() != param.getLogEntries()[0].getTerm()) {
                // 删除这一条和之后所有的, 然后写入日志和状态机.
                node.getLogModule().removeOnStartIndex(param.getPrevLogIndex() + 1);
            } else if (logEntry != null) {
                // 已经有日志了, 不能重复写入.
                return builder.success(true).build();
            }

            // 写进日志并且应用到状态机
            for (LogEntry entry : param.getLogEntries()) {
                node.getLogModule().write(entry);
                node.stateMachine.apply(entry);
                builder.success(true);
            }

            //如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
            if (param.getLeaderCommit() > node.commitIndex) {
                int commitIndex = (int) Math.min(param.getLeaderCommit(), node.getLogModule().getLastIndex());
                node.commitIndex = commitIndex;
                node.lastApplies = commitIndex;
            }
            builder.term(node.getCurrentTerm());
            node.status = NodeStatus.FOLLOWER;
            // TODO, 是否应当在成功回复之后, 才正式提交? 防止 leader "等待回复"过程中 挂掉.
            return builder.build();

        } finally {
            appendLock.unlock();
        }
    }
}
