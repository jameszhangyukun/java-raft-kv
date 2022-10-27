package com.zyk.raft.kv.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点集合
 */
public class PeerSet implements Serializable {
    private List<Peer> peers = new ArrayList<>();

    private volatile Peer leader;
    private volatile Peer self;

    private PeerSet() {
    }

    public static PeerSet getInstance() {
        return PeerSetLazyHolder.INSTANCE;
    }

    private static class PeerSetLazyHolder {
        private static final PeerSet INSTANCE = new PeerSet();
    }

    public void setSelf(Peer peer) {
        self = peer;
    }

    public Peer getSelf() {
        return self;
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
    }

    public List<Peer> getPeers() {
        return peers;
    }

    public List<Peer> getPeersWithOutSelf() {
        List<Peer> list2 = new ArrayList<>(peers);
        list2.remove(self);
        return list2;
    }


    public Peer getLeader() {
        return leader;
    }

    public void setLeader(Peer peer) {
        leader = peer;
    }

    @Override
    public String toString() {
        return "PeerSet{" +
                "list=" + peers +
                ", leader=" + leader +
                ", self=" + self +
                '}';
    }
}
