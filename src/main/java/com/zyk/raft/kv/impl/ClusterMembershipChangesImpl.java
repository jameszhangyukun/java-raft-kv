package com.zyk.raft.kv.impl;

import com.zyk.raft.kv.entity.Peer;
import com.zyk.raft.kv.membership.ClusterMembershipChanges;
import com.zyk.raft.kv.membership.Result;

public class ClusterMembershipChangesImpl implements ClusterMembershipChanges {
    @Override
    public Result addPeer(Peer newPeer) {
        return null;
    }

    @Override
    public Result removePeer(Peer oldPeer) {
        return null;
    }
}
