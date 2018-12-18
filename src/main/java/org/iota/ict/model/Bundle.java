package org.iota.ict.model;

import java.util.ArrayList;

public class Bundle {
    private final ArrayList<Transaction> fragments = new ArrayList<>();
    private boolean complete, valid;

    public Bundle(Transaction head) {
        fragments.add(head);
        fetchFragments();
    }

    private void fetchFragments() {
        Transaction fetchedLast = fragments.get(fragments.size()-1);
        while (fetchedLast.trunk != null) {
            fragments.add(fetchedLast.trunk);
            fetchedLast = fetchedLast.trunk;
        }

    }

    public boolean isValid() {
        return valid;
    }

    public boolean isComplete() {
        return complete;
    }
}
