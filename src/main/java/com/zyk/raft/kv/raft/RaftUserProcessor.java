package com.zyk.raft.kv.raft;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import com.zyk.raft.kv.exception.RaftNotSupportException;

public abstract class RaftUserProcessor<T> extends AbstractUserProcessor<T> {

    @Override
    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, T t) {
        throw new RaftNotSupportException(
                "Raft Server not support handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) ");
    }

    @Override
    public String interest() {
        return Request.class.getName();
    }
}
