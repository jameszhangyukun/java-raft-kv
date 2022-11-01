package com.zyk.raft.kv.raft;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.zyk.raft.kv.exception.RaftRemotingException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultRpcClient implements com.zyk.raft.kv.raft.RpcClient {
    private static final com.alipay.remoting.rpc.RpcClient CLIENT = new RpcClient();


    @Override
    public void init() throws Throwable {
        CLIENT.init();
    }

    @Override
    public void destroy() throws Throwable {
        CLIENT.shutdown();
        log.info("destroy success");
    }

    @Override
    public <R> R send(Request request) {
        return send(request, (int) TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public <R> R send(Request request, int timeout) {

        Response<R> result;
        try {
            result = (Response<R>) CLIENT.invokeSync(request.getUrl(), request.getObj(), timeout);
            return result.getResult();
        } catch (RemotingException e) {
            throw new RaftRemotingException("rpc RaftRemotingException ", e);
        } catch (InterruptedException e) {
            // ignore
        }
        return null;
    }
}
