package com.zyk.raft.kv;

import com.zyk.raft.client.ClientKVAck;
import com.zyk.raft.client.ClientKVReq;
import com.zyk.raft.kv.entity.*;

/**
 * 节点
 */
public interface Node extends LifeCycle {

    /**
     * 设置配置文件
     *
     */
    void setConfig(NodeConfig nodeConfig);

    /**
     * 处理投票请求
     */
    VoteResult handlerRequestVote(VoteParam voteParam);

    /**
     * 处理追加日志请求
     */
    AppendEntryResult handlerAppendEntries(AppendEntryParam param);

    /**
     * 处理客户端请求
     */
    ClientKVAck handlerClientRequest(ClientKVReq request);

    /**
     * 请求转发给leader
     */
    ClientKVAck redirect(ClientKVReq requset);
}
