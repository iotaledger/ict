package org.iota.ict.utils;

import org.iota.ict.network.Neighbor;

public class Stats {
    public final Neighbor neighbor;
    public long receivedAll, receivedNew, invalid, requested, ignored;

    public Stats(Neighbor neighbor) {
        this.neighbor = neighbor;
        receivedAll = 0;
        receivedNew = 0;
        invalid = 0;
        requested = 0;
        ignored = 0;
    }

    public Stats(Stats reference) {
        this.neighbor = reference.neighbor;
        receivedAll = reference.receivedAll;
        receivedNew = reference.receivedNew;
        invalid = reference.invalid;
        requested = reference.requested;
        ignored = reference.ignored;
    }
}
