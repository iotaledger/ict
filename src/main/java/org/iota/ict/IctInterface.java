package org.iota.ict;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.model.Tangle;
import org.iota.ict.network.Neighbor;
import org.iota.ict.network.Node;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.utils.properties.FinalProperties;
import org.iota.ict.utils.properties.PropertiesUser;
import org.iota.ict.utils.interfaces.Restartable;

import java.util.List;

public interface IctInterface extends Ixi, GossipListener, PropertiesUser, Restartable {

    List<Neighbor> getNeighbors();

    IxiModuleHolder getModuleHolder();

    Tangle getTangle();

    FinalProperties getProperties();

    List<Node.Round> getRounds();

    void request(String transactionHash);
}
