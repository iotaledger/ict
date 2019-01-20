package org.iota.ict.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.Ict;
import org.iota.ict.network.event.GossipEvent;
import org.iota.ict.utils.*;
import org.iota.ict.model.Tangle;
import org.iota.ict.model.Transaction;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.Properties;

import java.net.DatagramPacket;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class sends transactions to neighbors. Together with the {@link Receiver}, they are the two important gateways
 * for transaction gossip between Ict nodes. Each Ict instance has exactly one {@link Receiver} and one {@link Sender}
 * to communicate with its neighbors.
 * <p>
 * The sending process happens in its own Thread to not block other components. Before being sent, transactions are put
 * into a {@link #queue}. This class also requests transactions which are not known to the Ict but were referenced by
 * received transactions either through the branch or trunk.
 *
 * @see Ict
 * @see Receiver
 */
public class Sender extends RestartableThread implements SenderInterface {

    private Node node;
    private final SendingTaskQueue queue = new SendingTaskQueue();

    private final Queue<String> transactionsToRequest = new PriorityBlockingQueue<>();
    private static final Logger LOGGER = LogManager.getLogger("Sender");
    private Properties properties;

    public Sender(Node node, Properties properties) {
        super(LOGGER);
        this.node = node;
        this.properties = properties;
    }

    @Override
    public void onGossipEvent(GossipEvent event) {
        Tangle.TransactionLog log = node.ict.getTangle().createTransactionLogIfAbsent(event.getTransaction());
        if (!log.wasSent && log.senders.size() < node.neighbors.size()) {
            log.wasSent = true;
            queue(event.getTransaction());
        }
    }

    @Override
    public void run() {
        while (isRunning()) {
            if (!queue.isEmpty() && queue.peek().sendingTime <= System.currentTimeMillis()) {
                sendTransaction(queue.poll().transaction);
            } else {
                waitForNextTransaction();
            }
        }
    }

    private void waitForNextTransaction() {
        try {
            synchronized (queue) {
                // keep queue.isEmpty() within the synchronized block so notify is not called after the empty check and before queue.wait()
                queue.wait(queue.isEmpty() ? properties.roundDuration() : Math.max(1, queue.peek().sendingTime - System.currentTimeMillis()));
            }
        } catch (InterruptedException e) {
            if (isRunning())
                logger.error("Unexpected interrupt.", e);
        }
    }

    private void sendTransaction(Transaction transaction) {
        Tangle.TransactionLog transactionLog = node.ict.getTangle().findTransactionLog(transaction);
        if (Math.abs(transaction.issuanceTimestamp - System.currentTimeMillis()) > Constants.TIMESTAMP_DIFFERENCE_TOLERANCE_IN_MILLIS * 0.9)
            return;
        for (Neighbor nb : node.neighbors)
            if (transactionLog == null || !transactionLog.senders.contains(nb))
                sendTransactionToNeighbor(nb, transaction);
    }

    private void sendTransactionToNeighbor(Neighbor nb, Transaction transaction) {
        try {
            DatagramPacket packet = transaction.toDatagramPacket(transactionsToRequest.isEmpty() ? Trytes.NULL_HASH : transactionsToRequest.poll());
            packet.setSocketAddress(nb.getAddress());
            node.socket.send(packet);
        } catch (Exception e) {
            if (isRunning())
                logger.error("Failed to send transaction to neighbor.", e);
        }
    }

    @Override
    public void onTerminate() {
        synchronized (queue) {
            queue.notify();
        }
    }

    public void queue(Transaction transaction) {
        long forwardDelay = properties.minForwardDelay()+ ThreadLocalRandom.current().nextLong(properties.maxForwardDelay() - properties.minForwardDelay());
        queue.add(new SendingTask(System.currentTimeMillis() + forwardDelay, transaction));
        synchronized (queue) {
            queue.notify();
        }
    }

    @Override
    public void updateProperties(FinalProperties properties) {
        this.properties = properties;
        synchronized (queue) {
            // notify queue to stop wait() and enforce new round duration
            queue.notify();
        }
    }

    @Override
    public void request(String requestedHash) {
        transactionsToRequest.add(requestedHash);
    }

    private static class SendingTask {

        private final long sendingTime;
        private final Transaction transaction;

        private SendingTask(long sendingTime, Transaction transaction) {
            this.sendingTime = sendingTime;
            this.transaction = transaction;
        }
    }

    public int queueSize() {
        return queue.size();
    }


    private static class SendingTaskQueue extends PriorityBlockingQueue<SendingTask> {
        private SendingTaskQueue() {
            super(1, new Comparator<SendingTask>() {
                @Override
                public int compare(SendingTask task1, SendingTask task2) {
                    return Long.compare(task1.sendingTime, task2.sendingTime);
                }
            });
        }
    }
}
