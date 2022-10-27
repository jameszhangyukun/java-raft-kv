package com.zyk.raft.kv.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class VoteResult implements Serializable {
    /**
     * 当前任期号，便于候选人更新自己的任期
     */
    private long term;

    /**
     * 候选人获得选票为true
     */
    private boolean voteGranted;

    public VoteResult(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public static VoteResult fail() {
        return new VoteResult(false);
    }


    public static VoteResult ok() {
        return new VoteResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private long term;
        private boolean voteGranted;

        private Builder() {

        }

        public Builder term(long term) {
            this.term = term;
            return this;
        }


        public Builder voteGranted(boolean voteGranted) {
            this.voteGranted = voteGranted;
            return this;
        }

        public VoteResult build() {
            return new VoteResult(this);
        }

    }

    private VoteResult(Builder builder) {
        setTerm(builder.term);
        setVoteGranted(builder.voteGranted);
    }

}
