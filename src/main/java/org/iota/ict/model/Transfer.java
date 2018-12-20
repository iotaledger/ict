package org.iota.ict.model;

import java.math.BigInteger;
import java.util.List;

public class Transfer {

    private final List<BalanceChange> changes;

    public Transfer(List<BalanceChange> changes) {
        ensureSumIsZero(changes);
        this.changes = changes;
    }

    public Bundle buildBundle() {
        BundleBuilder bundleBuilder = new BundleBuilder();
        for(BalanceChange change : changes)
            change.appendToBundleBuilder(bundleBuilder);
        return bundleBuilder.build();
    }

    private static void ensureSumIsZero(List<BalanceChange> changes) {
        BigInteger sum = BigInteger.ZERO;
        for(BalanceChange change : changes) {
            sum = sum.add(change.value);
        }
        if(sum.compareTo(BigInteger.ZERO) != 0)
            throw new IllegalArgumentException("Total sum of changes must be 0 but is '"+sum.toString()+"'.");
    }
}
