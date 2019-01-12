package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Tangle;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.ErrorHandler;
import org.iota.ict.utils.Trytes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * This class receives transactions from neighbors. Together with the {@link Sender}, they are the two important gateways
 * for transaction gossip between Ict nodes. Each Ict instance has exactly one {@link Receiver} and one {@link Sender}
 * to communicate with its neighbors.
 *
 * @see Ict
 * @see Sender
 */
public class Receiver extends Thread {
    private final Tangle tangle;
    private final Ict ict;
    private final DatagramSocket socket;

    public Receiver(Ict ict, Tangle tangle, DatagramSocket socket) {
        super("Receiver");
        this.ict = ict;
        this.tangle = tangle;
        this.socket = socket;
    }

    @Override
    public void run() {
        while (ict.isRunning()) {

            DatagramPacket packet = new DatagramPacket(new byte[Constants.TRANSACTION_SIZE_BYTES], Constants.TRANSACTION_SIZE_BYTES);
            try {
                socket.receive(packet);
                processIncoming(packet);
            } catch (IOException e) {
                if (ict.isRunning())
                    e.printStackTrace();
            }
        }
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

        Transaction transaction = unpack(packet, sender);

        if (transaction == null) {
            sender.stats.receivedInvalid++;
            return;
        }

        if (Math.abs(transaction.issuanceTimestamp - System.currentTimeMillis()) > Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS) {
            sender.stats.ignored++;
        }

        updateTransactionLog(sender, transaction);
        processRequest(sender, transaction);
    }

    private Transaction unpack(DatagramPacket packet, Neighbor sender) {
        try {
            byte[] bytes = packet.getData();
            return new Transaction(Trytes.fromBytes(bytes), bytes);
        } catch (Throwable t) {
            //ErrorHandler.handleWarning(Ict.LOGGER, t,"Received invalid transaction from neighbor: " + sender.getAddress());
            return null;
        }
    }

    private void updateTransactionLog(Neighbor sender, Transaction transaction) {
        Tangle.TransactionLog log = tangle.findTransactionLog(transaction);
        if (log == null) {
            log = tangle.createTransactionLogIfAbsent(transaction);
            sender.stats.receivedNew++;
            log.senders.add(sender);
            ict.notifyListeners(new GossipEvent(transaction, false));
        }
        log.senders.add(sender);
    }

    private void processRequest(Neighbor requester, Transaction transaction) {
        if (transaction.requestHash.equals(Trytes.NULL_HASH))
            return; // no transaction requested
        Transaction requested = tangle.findTransactionByHash(transaction.requestHash);
        requester.stats.requested++;
        if (requested == null)
            return; // unknown transaction
        sendRequested(requested, requester);
        // unset requestHash because it's header information and does not actually belong to the transaction
        transaction.requestHash = Trytes.NULL_HASH;
    }

    private void sendRequested(Transaction requested, Neighbor requester) {
        Tangle.TransactionLog requestedLog = tangle.findTransactionLog(requested);
        requestedLog.senders.remove(requester); // remove so requester is no longer marked as already knowing this transaction
        ict.broadcast(requested);
    }

    private Neighbor determineNeighborWhoSent(DatagramPacket packet) {
        for (Neighbor nb : ict.getNeighbors())
            if (nb.sentPacket(packet))
                return nb;
        for (Neighbor nb : ict.getNeighbors())
            if (nb.sentPacketFromSameIP(packet))
                return nb;
        //Ict.LOGGER.warn("Received transaction from unknown address: " + packet.getAddress());
        return null;
    }
}
