package org.iota.ict.network;

import org.junit.Test;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import static org.junit.Assert.*;

/**
 * @author https://github.com/georgmittendorfer (https://github.com/iotaledger/ict/issues/3#issuecomment-450352986)
 * */
public class HostResolveTest {

    private final Neighbor neighbor = new Neighbor(new InetSocketAddress("localhost", 42), 0);

    @Test
    public void sameIpWhenSentPacketFromSameIpThenTrue() {
        assertTrue("expected match", neighbor.sentPacketFromSameIP(packetFrom("localhost", 42)));
        assertTrue("expected match", neighbor.sentPacketFromSameIP(packetFrom("localhost", 666)));
        assertTrue("expected match", neighbor.sentPacketFromSameIP(packetFrom("127.0.0.1", 42)));
        assertTrue("expected match", neighbor.sentPacketFromSameIP(packetFrom("127.0.0.1", 666)));
    }

    @Test
    public void differentIpWhenSentPacketFromSameIpThenFalse() {
        assertFalse("expected no match", neighbor.sentPacketFromSameIP(packetFrom("google-public-dns-a.google.com", 42)));
        assertFalse("expected no match", neighbor.sentPacketFromSameIP(packetFrom("8.8.8.8", 42)));
    }

    private DatagramPacket packetFrom(String hostOrIp, int port) {
        return new DatagramPacket(new byte[] {}, 0, new InetSocketAddress(hostOrIp, port));
    }

}