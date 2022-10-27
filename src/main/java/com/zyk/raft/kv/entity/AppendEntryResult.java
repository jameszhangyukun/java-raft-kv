package com.zyk.raft.kv.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AppendEntryResult implements Serializable {
    /**
     * 当前的任期号，用于领导人去更新自己
     */
    long term;

    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    boolean success;


    public AppendEntryResult(long term) {
        this.term = term;
    }

    public AppendEntryResult(boolean success) {
        this.success = success;
    }

    public AppendEntryResult(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    private AppendEntryResult(Builder builder) {
        setTerm(builder.term);
        setSuccess(builder.success);
    }

    public static AppendEntryResult fail() {
        return new AppendEntryResult(false);
    }

    public static AppendEntryResult ok() {
        return new AppendEntryResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private boolean success;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder success(boolean val) {
            success = val;
            return this;
        }

        public AppendEntryResult build() {
            return new AppendEntryResult(this);
        }
    }
}

