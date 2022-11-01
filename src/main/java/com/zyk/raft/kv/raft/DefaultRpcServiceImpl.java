package com.zyk.raft.kv.raft;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.zyk.raft.client.ClientKVReq;
import com.zyk.raft.kv.entity.AppendEntryParam;
import com.zyk.raft.kv.entity.Peer;
import com.zyk.raft.kv.entity.VoteParam;
import com.zyk.raft.kv.impl.DefaultNode;
import com.zyk.raft.kv.membership.ClusterMembershipChanges;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultRpcServiceImpl implements RpcService {


    private final DefaultNode node;

    private final RpcServer rpcServer;

    public DefaultRpcServiceImpl(int port, DefaultNode node) {
        this.rpcServer = new RpcServer(port, false, false);
        rpcServer.registerUserProcessor(new RaftUserProcessor<Request>() {
            @Override
            public Object handleRequest(BizContext bizCtx, Request request) {
                return handlerRequest(request);
            }
        });
        this.node = node;
    }

    @Override
    public void init() throws Throwable {
        rpcServer.start();
    }

    @Override
    public void destroy() throws Throwable {
        rpcServer.stop();
        log.info("destroy success");
    }

    @Override
    public Response<?> handlerRequest(Request request) {
        if (request.getCmd() == Request.R_VOTE) {
            return new Response<>(node.handlerRequestVote((VoteParam) request.getObj()));
        } else if (request.getCmd() == Request.A_ENTRIES) {
            return new Response<>(node.handlerAppendEntries((AppendEntryParam) request.getObj()));
        } else if (request.getCmd() == Request.CLIENT_REQ) {
            return new Response<>(node.handlerClientRequest((ClientKVReq) request.getObj()));
        } else if (request.getCmd() == Request.CHANGE_CONFIG_REMOVE) {
            return new Response<>(((ClusterMembershipChanges) node).removePeer((Peer) request.getObj()));
        } else if (request.getCmd() == Request.CHANGE_CONFIG_ADD) {
            return new Response<>(((ClusterMembershipChanges) node).addPeer((Peer) request.getObj()));
        }
        return null;
    }
}
