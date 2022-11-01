package com.zyk.raft.kv.exception;

public class RaftNotSupportException extends RuntimeException {

    public RaftNotSupportException() {
    }

    public RaftNotSupportException(String message) {
        super(message);
    }
}
