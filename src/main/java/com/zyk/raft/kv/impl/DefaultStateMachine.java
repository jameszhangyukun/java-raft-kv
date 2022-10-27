package com.zyk.raft.kv.impl;

import com.zyk.raft.kv.StateMachine;
import com.zyk.raft.kv.entity.LogEntry;


public class DefaultStateMachine implements StateMachine {
    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {

    }

    @Override
    public void apply(LogEntry logEntry) {

    }

    @Override
    public LogEntry get(String key) {
        return null;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public void setString(String key, String value) {

    }

    @Override
    public void delString(String... key) {

    }
}
