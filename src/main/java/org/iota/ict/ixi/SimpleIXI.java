package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSentEvent;

/**
 * An example IXI for demonstration purposes.
 */
public class SimpleIXI extends IXI {
    public SimpleIXI(Ict ict) {
        super(ict);
        System.out.println("[MONITOR] Started Transaction Monitoring IXI");
        addGossipListener(new GossipListener() {

            @Override
            public void onReceiveTransaction(GossipReceiveEvent e) {
                System.out.println("[MONITOR] Received transaction: " + e.getTransaction().decodedSignatureFragments);
            }

            @Override
            public void onSentTransaction(GossipSentEvent e) {
                System.out.println("[MONITOR] Sent transaction: " + e.getTransaction().decodedSignatureFragments);
            }
        });
    }
}
