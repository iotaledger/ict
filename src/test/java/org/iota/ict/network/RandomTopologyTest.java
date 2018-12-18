package org.iota.ict.network;

import org.iota.ict.Ict;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Ignore // takes too long, only test occasionally
@RunWith(Parameterized.class)
public class RandomTopologyTest extends GossipTest {

    @Parameterized.Parameters
    public static Object[][] data() {
        // lets this Test run 10 times
        return new Object[10][0];
    }

    @Test
    public void testRandomNetworkTopologies() {
        List<Ict> network = createRandomNetworkTopology(10);
        randomlyConnectIcts(network, 3);
        Ict randomIct = network.get((int) (Math.random() * network.size()));
        LinkedList<Ict> otherIcts = new LinkedList<>(network);
        otherIcts.remove(randomIct);
        testCommunicationRange(randomIct, otherIcts, 20);
    }

    private void testCommunicationRange(Ict sender, List<Ict> otherIcts, int amountOfMessages) {
        Map<String, String> sentMessagesByHash = sendMessages(sender, amountOfMessages);
        waitUntilCommunicationEnds(1000);
        for (Ict receiver : otherIcts)
            assertThatTransactionsReceived(receiver, sentMessagesByHash, (int) Math.ceil(amountOfMessages * 0.85));
    }

    private List<Ict> createRandomNetworkTopology(int amountOfIcts) {
        List<Ict> icts = new LinkedList<>();
        for (; amountOfIcts > 0; amountOfIcts--)
            icts.add(createIct());
        return icts;
    }

    private void randomlyConnectIcts(List<Ict> icts, int requiredNeighbors) {
        for (Ict ict : icts) {
            LinkedList<Ict> neighborCandidates = new LinkedList<>(icts);
            neighborCandidates.remove(ict);
            while (ict.getNeighbors().size() < requiredNeighbors) {
                Ict neighbor = findIctWithLeastNeighbors(neighborCandidates);
                neighborCandidates.remove(neighbor);
                if (neighbor == null)
                    Assert.fail("Couldn't find neighbor.");
                connect(ict, neighbor);
            }
        }
    }

    private Ict findIctWithLeastNeighbors(List<Ict> icts) {
        Collections.shuffle(icts);
        Ict ictWithLeastNeighbors = null;
        for (Ict ict : icts)
            if (ictWithLeastNeighbors == null || ict.getNeighbors().size() < ictWithLeastNeighbors.getNeighbors().size())
                ictWithLeastNeighbors = ict;
        return ictWithLeastNeighbors;
    }
}
