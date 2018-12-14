package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.model.TransactionBuilder;
import org.iota.ict.utils.Constants;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LogRoundTest extends GossipTest {

    private static final PrintStream DUMMY_STREAM = new PrintStream(new OutputStream() {
        @Override
        public void write(int arg0) {

        }
    });

    @Test
    public void testTransactionCounting() {
        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);

        sendMessages(a, 10);
        waitUntilCommunicationEnds(200);

        Neighbor.Stats statsForB = a.getNeighbors().get(0).stats;
        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        Assert.assertEquals("transaction was back-broadcasted (useless communication)", 0, statsForB.receivedAll);
        Assert.assertEquals("neighbor did not receive all transactions", 10, statsForA.receivedAll);
        Assert.assertEquals("neighbor considered new transaction as not new", 10, statsForA.receivedNew);
        Assert.assertEquals("neighbor received invalid transactions", 0, statsForA.receivedInvalid);

        statsForA.newRound();
        Assert.assertEquals("stats was not reset upon new round", 0, statsForA.receivedAll);
        Assert.assertEquals("previous stats were lost upon new round", 10, statsForA.prevReceivedAll);
    }

    @Test
    public void testInvalidTransactionBecauseOfSize() throws IOException {
        PrintStream originalErr = System.err;
        System.setErr(DUMMY_STREAM);

        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);
        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        // must terminate so socket address can be reused
        a.terminate();
        runningIcts.remove(a);
        DatagramSocket socket = new DatagramSocket(a.getAddress());

        // packet with invalid size
        DatagramPacket invalidPacket1 = new DatagramPacket(new byte[Constants.TRANSACTION_SIZE_TRYTES - 1], Constants.TRANSACTION_SIZE_TRYTES - 1);
        invalidPacket1.setSocketAddress(b.getAddress());
        socket.send(invalidPacket1);
        sleep(100);
        Assert.assertEquals("neighbor did not recognize invalid transaction as invalid", 1, statsForA.receivedInvalid);

        socket.close();
        System.setErr(originalErr);
    }

    @Test
    public void testInvalidTransactionBecauseNotTrytes() throws IOException {
        PrintStream originalErr = System.err;
        System.setErr(DUMMY_STREAM);

        Ict a = createIct();
        Ict b = createIct();
        connect(a, b);
        Neighbor.Stats statsForA = b.getNeighbors().get(0).stats;

        // packet containing a non-tryte character
        Transaction invalidTransaction = new TransactionBuilder().build();
        a.request("z");
        a.submit(invalidTransaction);
        sleep(100);
        Assert.assertEquals("neighbor did not recognize invalid transaction as invalid", 1, statsForA.receivedInvalid);

        System.setErr(originalErr);
    }
}
