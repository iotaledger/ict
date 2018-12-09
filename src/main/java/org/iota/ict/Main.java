package org.iota.ict;

import org.iota.ict.ixi.SimpleIXI;

public class Main {
    public static void main(String[] args) {
        Ict myNeighbor = new Ict(new Properties().port(1337));
        Ict me = new Ict(new Properties().port(1338));

        me.neighbor(myNeighbor.getAddress());
        myNeighbor.neighbor(me.getAddress());

        new SimpleIXI(me);
        myNeighbor.submit("This message was sent by my neighbor.");
        me.submit("This message was sent by me.");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        me.terminate();
        myNeighbor.terminate();
    }
}
