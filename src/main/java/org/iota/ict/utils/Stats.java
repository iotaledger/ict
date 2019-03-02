package org.iota.ict.utils;

import org.iota.ict.network.Neighbor;
import org.json.JSONObject;

public class Stats {
    public long timestamp;
    public final Neighbor neighbor;
    public long receivedAll, receivedNew, invalid, requested, ignored;

    public Stats(Neighbor neighbor) {
        this.timestamp = System.currentTimeMillis();
        this.neighbor = neighbor;
        receivedAll = 0;
        receivedNew = 0;
        invalid = 0;
        requested = 0;
        ignored = 0;
    }

    public Stats(JSONObject json) {
        neighbor = null;
        receivedAll = json.getInt("all");
        receivedNew = json.getInt("new");
        ignored = json.getInt("ignored");
        invalid = json.getInt("invalid");
        requested = json.getInt("requested");
    }

    public Stats(Stats reference) {
        timestamp = reference.timestamp;
        neighbor = reference.neighbor;
        receivedAll = reference.receivedAll;
        receivedNew = reference.receivedNew;
        invalid = reference.invalid;
        requested = reference.requested;
        ignored = reference.ignored;
    }

    public void accumulate(Stats reference) {
        receivedAll += reference.receivedAll;
        receivedNew += reference.receivedNew;
        invalid += reference.invalid;
        requested += reference.requested;
        ignored += reference.ignored;
    }

    public JSONObject toJSON() {
        return new JSONObject().put("timestamp", timestamp)
            .put("all", receivedAll)
            .put("new", receivedNew)
            .put("ignored", ignored)
            .put("requested", requested)
            .put("invalid", invalid);
    }
}
