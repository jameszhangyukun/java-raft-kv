package com.zyk.raft.kv.raft;

import com.zyk.raft.kv.LifeCycle;

public interface RpcService extends LifeCycle {


    Response<?> handlerRequest(Request request);

}
