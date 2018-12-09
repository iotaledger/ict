package org.iota.ict.network;

import org.iota.ict.Ict;
import org.iota.ict.model.Tangle;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver extends Thread {
    private final Tangle tangle;
    private final Ict ict;
    private final DatagramSocket socket;
    private long roundStart;


    public Receiver(Ict ict, Tangle tangle, DatagramSocket socket) {
        super("Receiver");
        this.ict = ict;
        this.tangle = tangle;
        this.socket = socket;
    }

    @Override
    public void run() {
        roundStart = System.currentTimeMillis();
        while (ict.isRunning()) {
            manageRounds();

            DatagramPacket packet = new DatagramPacket(new byte[Constants.PACKET_SIZE], Constants.PACKET_SIZE);
            try {
                socket.receive(packet);
                Transaction transaction = new Transaction(new String(packet.getData()));
                for (Neighbor nb : ict.getNeighbors())
                    if (nb.getAddress().equals(packet.getSocketAddress())) {
                        tangle.createTransactionLogIfAbsent(transaction).senders.add(nb);
                        nb.stats.receivedAll++;
                        break;
                    }
                ict.notifyListeners(new GossipReceiveEvent(transaction));
            } catch (IOException e) {
                if (ict.isRunning())
                    e.printStackTrace();
            }
        }
    }

    private void manageRounds() {
        if (roundStart + ict.getProperties().roundDuration < System.currentTimeMillis()) {
            ict.logRound();
            roundStart = System.currentTimeMillis();
        }
    }
}
