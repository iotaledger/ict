package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.model.tangle.Tangle;
import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.RestartableThread;
import org.iota.ict.utils.Trytes;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * This class receives transactions from neighbors. Together with the {@link Sender}, they are the two important gateways
 * for transaction gossip between Ict nodes. Each Ict instance has exactly one {@link Receiver} and one {@link Sender}
 * to communicate with its neighbors.
 *
 * @see Ict
 * @see Sender
 */
public class Receiver extends RestartableThread {

    protected static final Logger LOGGER = LogManager.getLogger("Receiver");
    protected Node node;

    public Receiver(Node node) {
        super(LOGGER);
        this.node = node;
    }

    @Override
    public void run() {
        while (isRunning()) {
            DatagramPacket packet = new DatagramPacket(new byte[Constants.PACKET_SIZE_BYTES], Constants.PACKET_SIZE_BYTES);
            try {
                node.socket.receive(packet);
                processIncoming(packet);
            } catch (IOException e) {
                if (isRunning())
                    e.printStackTrace();
            }
        }
    }

    @Override
    public void onTerminate() {
        node.socket.close();
    }

    private void processIncoming(DatagramPacket packet) {

        Neighbor sender = determineNeighborWhoSent(packet);
        if (sender == null)
            return;

        sender.stats.receivedAll++;

        if (sender.reachedLimitOfAllowedTransactions()) {
            sender.stats.ignored++;
            return;
        }

        Transaction transaction = unpack(packet);

        if (transaction == null) {
            sender.stats.receivedInvalid++;
            return;
        }

        if (Math.abs(transaction.issuanceTimestamp - System.currentTimeMillis()) > Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS) {
            sender.stats.ignored++;
            return;
        }

        updateTransactionLog(sender, transaction);

        String requestedHash = Trytes.fromBytes(packet.getData(), Constants.TRANSACTION_SIZE_BYTES, Transaction.Field.BRANCH_HASH.byteLength);
        processRequest(sender, requestedHash);
    }

    private Transaction unpack(DatagramPacket packet) {
        try {
            byte[] bytes = packet.getData();
            return new Transaction(bytes);
        } catch (Throwable t) {
            return null;
        }
    }

    private void updateTransactionLog(Neighbor sender, Transaction transaction) {
        Tangle.TransactionLog log = node.ict.getTangle().findTransactionLog(transaction);
        if (log == null) {
            log = node.ict.getTangle().createTransactionLogIfAbsent(transaction);
            sender.stats.receivedNew++;
            log.senders.add(sender);
            node.ict.onGossipEvent(new GossipEvent(transaction, false));
        }
        log.senders.add(sender);
    }

    private void processRequest(Neighbor requester, String requestHash) {
        if (requestHash.equals(Trytes.NULL_HASH))
            return; // no transaction requested
        Transaction requested = node.ict.findTransactionByHash(requestHash);
        requester.stats.requested++;
        if (requested == null)
            return; // unknown transaction
        answerRequest(requested, requester);
    }

    private void answerRequest(Transaction requested, Neighbor requester) {
        Tangle.TransactionLog requestedLog = node.ict.getTangle().findTransactionLog(requested);
        requestedLog.senders.remove(requester); // remove so requester is no longer marked as already knowing this transaction
        node.sender.queue(requested);
    }

    private Neighbor determineNeighborWhoSent(DatagramPacket packet) {
        for (Neighbor nb : node.neighbors)
            if (nb.sentPacket(packet))
                return nb;
        for (Neighbor nb : node.neighbors)
            if (nb.sentPacketFromSameIP(packet))
                return nb;
        return null;
    }
}
