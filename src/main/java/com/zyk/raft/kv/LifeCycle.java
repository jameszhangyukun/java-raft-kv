package com.zyk.raft.kv;

public interface LifeCycle {
    /**
     * 初始化资源
     *
     * @throws Throwable
     */
    void init() throws Throwable;


    /**
     * 释放资源
     *
     * @throws Throwable
     */
    void destroy() throws Throwable;
}
