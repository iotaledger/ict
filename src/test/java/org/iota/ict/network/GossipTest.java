package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.IctTestTemplate;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.junit.Assert;

import java.util.*;

public abstract class GossipTest extends IctTestTemplate {


    void testBidirectionalCommunication(Ict ictA, Ict ictB, int messagesPerDirection) {
        testUnidirectionalCommunication(ictA, ictB, messagesPerDirection);
        testUnidirectionalCommunication(ictB, ictA, messagesPerDirection);
    }

    void testUnidirectionalCommunication(Ict sender, Ict receiver, int amountOfMessages) {
        Map<String, String> sentMessagesByHash = sendMessages(sender, amountOfMessages);
        Assert.assertEquals("unique hashes of sent transactions", amountOfMessages, sentMessagesByHash.values().size());
        waitUntilCommunicationEnds(1000);
        assertThatTransactionsReceived(receiver, sentMessagesByHash, (int) Math.ceil(amountOfMessages * 0.85));
    }

    Map<String, String> sendMessages(Ict sender, int amountOfMessages) {
        Map<String, String> sentMessagesByHash = new HashMap<>();
        TransactionBuilder builder = new TransactionBuilder();
        for (int i = 0; i < amountOfMessages; i++) {
            String message = randomAsciiMessage();
            builder.asciiMessage(message);
            Transaction transaction = builder.buildWhileUpdatingTimestamp();
            sender.submit(transaction);
            sentMessagesByHash.put(transaction.hash, message);
        }
        return sentMessagesByHash;
    }

    void assertThatTransactionsReceived(Ict receiver, Map<String, String> sentMessagesByHash, int minRequired) {
        int receivedTransactions = 0;
        for (String hash : sentMessagesByHash.keySet())
            if (receiver.findTransactionByHash(hash) != null)
                receivedTransactions++;
        if (receivedTransactions < minRequired)
            Assert.fail("sent " + sentMessagesByHash.size() + " but received " + receivedTransactions);
    }

    private static String randomAsciiMessage() {
        char[] message = new char[(int) (Math.random() * 20)];
        for (int i = 0; i < message.length - 1; i++)
            message[i] = (char) (Math.random() * 127 + 1);
        return new String(message);
    }
}
