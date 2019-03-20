package org.iota.ict.std;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipPreprocessor;
import org.iota.ict.utils.RestartableThread;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class collects bundles before they are further processed by other components. It ensures that no incomplete bundles
 * enter the internal state of the Ict node.
 * */
public class BundleCollector extends RestartableThread {

    private final Ict ict;
    private static final Logger LOGGER = LogManager.getLogger("BndlColl");

    private GossipPreprocessor gossipPreprocessor;
    private Map<String, GossipEvent> existingTransactionsByHash = new HashMap<>();
    private Map<String, GossipEvent> existingTransactionsByTrunk = new HashMap<>();

    public BundleCollector(Ict ict) {
        super(LOGGER);
        this.gossipPreprocessor = new GossipPreprocessor(ict, -1000);
        this.ict = ict;
    }

    @Override
    public void run() {
        while (isRunning()) {
            try {
                GossipEvent gossipEvent = gossipPreprocessor.takeEffect();
                addIncompleteBundleTransaction(gossipEvent);
                passOnBundleIfComplete(gossipEvent);
            } catch (InterruptedException e) {
                if(isRunning())
                    throw new RuntimeException(e);
            }
        }
        // TODO ict.removeListener(gossipPreprocessor);
    }

    private void addIncompleteBundleTransaction(GossipEvent event) {
        Transaction transaction = event.getTransaction();
        if(!transaction.isBundleHead)
            existingTransactionsByHash.put(transaction.hash, event);
        if(!transaction.isBundleTail)
            existingTransactionsByTrunk.put(transaction.trunkHash(), event);
    }

    private void passOnBundleIfComplete(GossipEvent last) {

        GossipEvent head = findHead(last);
        List<GossipEvent> headToTail = fetchToTail(head);

        if(headToTail == null)
            // bundle not yet complete
            return;

        for(int i = headToTail.size()-1; i >= 0; i--)
            gossipPreprocessor.passOn(headToTail.get(i));

        for(GossipEvent event : headToTail)
            existingTransactionsByTrunk.remove(event.getTransaction().hash);
    }

    private GossipEvent findHead(GossipEvent last) {
        while (last != null && !last.getTransaction().isBundleHead) {
            last = existingTransactionsByTrunk.get(last.getTransaction().hash);
        }
        return last;
    }

    private List<GossipEvent> fetchToTail(GossipEvent last) {
        List<GossipEvent> toTail = new LinkedList<>();
        toTail.add(last);
        while (last != null && !last.getTransaction().isBundleTail) {
            last = existingTransactionsByHash.get(last.getTransaction().trunkHash());
            toTail.add(last);
        }
        return last == null ? null : toTail;
    }

    @Override
    public void onStart() {
        ict.addListener(gossipPreprocessor);
    }

    @Override
    public void onTerminate() {
        runningThread.interrupt();
    }
}
