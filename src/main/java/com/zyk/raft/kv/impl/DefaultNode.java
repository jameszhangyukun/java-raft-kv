package com.zyk.raft.kv.impl;

import com.zyk.raft.client.ClientKVAck;
import com.zyk.raft.client.ClientKVReq;
import com.zyk.raft.kv.LogModule;
import com.zyk.raft.kv.Node;
import com.zyk.raft.kv.StateMachine;
import com.zyk.raft.kv.constants.NodeStatus;
import com.zyk.raft.kv.entity.*;
import lombok.Getter;

import java.util.Map;

@Getter
public class DefaultNode implements Node {


    public long preHeartBeatTime;
    public long preElectionTime;
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

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {

    }

    @Override
    public void setConfig(NodeConfig nodeConfig) {

    }

    @Override
    public VoteResult handlerRequestVote(VoteParam voteParam) {
        return null;
    }

    @Override
    public AppendEntryResult handlerAppendEntries(AppendEntryParam param) {
        return null;
    }

    @Override
    public ClientKVAck handlerClientRequest(ClientKVReq request) {
        return null;
    }

    @Override
    public ClientKVAck redirect(ClientKVReq requset) {
        return null;
    }
}
