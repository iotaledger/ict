package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.utils.Properties;
import org.junit.After;
import org.junit.Assert;

import java.util.*;

public abstract class GossipTest {

    Set<Ict> runningIcts = new HashSet<>();
    private final static int DEFAULT_PORT = 1337;

    Ict createIct() {
        Properties properties = new Properties().port(DEFAULT_PORT + runningIcts.size());
        properties.minForwardDelay = 0;
        properties.maxForwardDelay = 10;
        Ict ict = new Ict(properties);
        runningIcts.add(ict);
        return ict;
    }

    void testBidirectionalCommunication(Ict ictA, Ict ictB, int messagesPerDirection) {
        testCommunicationPath(ictA, ictB, messagesPerDirection);
        testCommunicationPath(ictB, ictA, messagesPerDirection);
    }

    private void testCommunicationPath(Ict sender, Ict receiver, int amountOfMessages) {
        Map<String, String> sentMessagesByHash = sendMessages(sender, amountOfMessages);
        Assert.assertEquals("unique hashes of sent transactions", amountOfMessages, sentMessagesByHash.values().size());
        waitUntilCommunicationEnds(1000);
        assertThatTransactionsReceived(receiver, sentMessagesByHash);
    }

    Map<String, String> sendMessages(Ict sender, int amountOfMessages) {
        Map<String, String> sentMessagesByHash = new HashMap<>();

        for (int i = 0; i < amountOfMessages; i++) {
            String message = randomAsciiMessage();
            String hash = sender.submit(message).hash;
            sentMessagesByHash.put(hash, message);
        }
        return sentMessagesByHash;
    }

    void waitUntilCommunicationEnds(long maxWaitTime) {
        int lastReceived;
        int newReceived = sumNeighborStatsReceivedTransaction(runningIcts);
        do {
            lastReceived = newReceived;
            sleep(50);
            maxWaitTime -= 50;
            newReceived = sumNeighborStatsReceivedTransaction(runningIcts);
        } while (lastReceived != newReceived && maxWaitTime > 0);
    }

    private static int sumNeighborStatsReceivedTransaction(Iterable<Ict> network) {
        int sum = 0;
        for (Ict ict : network)
            sum += sumNeighborStatsReceivedTransaction(ict);
        return sum;
    }

    private static int sumNeighborStatsReceivedTransaction(Ict ict) {
        int sum = 0;
        for (Neighbor nb : ict.getNeighbors())
            sum += nb.stats.receivedAll;
        return sum;
    }

    void assertThatTransactionsReceived(Ict receiver, Map<String, String> sentMessagesByHash) {
        int receivedTransactions = 0;
        for (String hash : sentMessagesByHash.keySet())
            if (receiver.getTangle().findTransactionByHash(hash) != null)
                receivedTransactions++;
        // tolerate if 80% of transactions went through
        if (receivedTransactions < sentMessagesByHash.size())
            Assert.fail("sent " + sentMessagesByHash.size() + " but received " + receivedTransactions);
    }

    private static String randomAsciiMessage() {
        char[] message = new char[(int) (Math.random() * 20)];
        for (int i = 0; i < message.length - 1; i++)
            message[i] = (char) (Math.random() * 127 + 1);
        return new String(message);
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    static void connect(Ict ict1, Ict ict2) {
        ict1.neighbor(ict2.getAddress());
        ict2.neighbor(ict1.getAddress());
    }

    @After
    public void tearDown() {
        for (Ict ict : runningIcts)
            ict.terminate();
        runningIcts = new HashSet<>();
        sleep(50);
    }
}
