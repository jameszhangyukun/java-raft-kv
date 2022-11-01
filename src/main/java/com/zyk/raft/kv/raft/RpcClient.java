package com.zyk.raft.kv.raft;

import com.zyk.raft.kv.LifeCycle;

public interface RpcClient extends LifeCycle {
    /**
     * 同步发送请求
     *
     * @param request
     * @param <R>
     * @return
     */
    <R> R send(Request request);

    /**
     * 异步发送请求
     *
     * @param request
     * @param timeout
     * @param <R>
     * @return
     */
    <R> R send(Request request, int timeout);
}
