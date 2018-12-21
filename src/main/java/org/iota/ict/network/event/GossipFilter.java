package org.iota.ict.network.event;

import org.iota.ict.model.Transaction;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GossipFilter implements Serializable {

    private boolean watchingAll;
    private final Set<String> watchedAddresses = new HashSet<>();
    private final Set<String> watchedTags = new HashSet<>();

    public GossipFilter watchAddress(String address) {
        assert address.length() == Transaction.Field.ADDRESS.tryteLength;
        watchedAddresses.add(address);
        return this;
    }

    public GossipFilter unwatchAddress(String address) {
        watchedAddresses.remove(address);
        return this;
    }

    public GossipFilter watchTag(String tag) {
        assert tag.length() == Transaction.Field.TAG.tryteLength;
        watchedTags.add(tag);
        return this;
    }

    public GossipFilter unwatchTag(String tag) {
        watchedTags.remove(tag);
        return this;
    }

    public Set<String> getWatchedTags() {
        return new HashSet<>(watchedTags);
    }

    public Set<String> getWatchedAddresses() {
        return new HashSet<>(watchedAddresses);
    }

    public GossipFilter setWatchingAll(boolean watchingAll) {
        this.watchingAll = watchingAll;
        return this;
    }

    public boolean isWatchingAll() {
        return watchingAll;
    }

    public boolean passes(Transaction transaction) {
        return watchingAll || watchedAddresses.contains(transaction.address) || watchedTags.contains(transaction.tag);
    }
}