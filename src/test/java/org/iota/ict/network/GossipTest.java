package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.Properties;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GossipTest {

    @Test
    public void testCommunication() {
        Ict a = new Ict(new Properties().port(1337));
        Ict b = new Ict(new Properties().port(1338));
        Ict c = new Ict(new Properties().port(1339));

        buildConnection(a, b);
        buildConnection(b, c);

        testBidirectionalCommunication(a, b, 20);
        testBidirectionalCommunication(b, c, 20);
        testBidirectionalCommunication(a, c, 30);

        a.terminate();
        b.terminate();
        c.terminate();

        sleep(100);
    }

    private static void testBidirectionalCommunication(Ict ictA, Ict ictB, int messagesPerDirection) {
        testCommunicationPath(ictA, ictB, messagesPerDirection);
        testCommunicationPath(ictB, ictA, messagesPerDirection);
    }

    private static void testCommunicationPath(Ict sender, Ict receiver, int amountOfMessages) {
        Map<String, String> sentMessagesByHash = new HashMap<>();

        for (int i = 0; i < amountOfMessages; i++) {
            String message = randomAsciiMessage();
            String hash = sender.submit(message);
            sentMessagesByHash.put(hash, message);
        }
        Assert.assertEquals("unique hashes of sent transactions", amountOfMessages, sentMessagesByHash.values().size());
        sleep(100); // TODO base wait time on network and traffic
        assertThatTransactionsReceived(receiver, sentMessagesByHash);
    }

    private static void assertThatTransactionsReceived(Ict receiver, Map<String, String> sentMessagesByHash) {
        int receivedTransactions = 0;

        for (String hash : sentMessagesByHash.keySet())
            if (receiver.getTangle().findTransactionByHash(hash) != null)
                receivedTransactions++;
        if (receivedTransactions != sentMessagesByHash.size())
            Assert.fail("sent " + sentMessagesByHash.size() + " but received " + receivedTransactions);
    }

    private static String randomAsciiMessage() {
        char[] message = new char[(int) (Math.random() * 20)];
        for (int i = 0; i < message.length - 1; i++)
            message[i] = (char) (Math.random() * 127 + 1);
        return new String(message);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    private static void buildConnection(Ict ict1, Ict ict2) {
        ict1.neighbor(ict2.getAddress());
        ict2.neighbor(ict1.getAddress());
    }
}
