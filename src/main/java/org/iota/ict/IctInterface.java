package org.iota.ict;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModuleHolder;
import org.iota.ict.model.Tangle;
import org.iota.ict.network.Neighbor;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.utils.Properties;
import org.iota.ict.utils.PropertiesUser;
import org.iota.ict.utils.Restartable;

import java.util.List;

public interface IctInterface extends Ixi, GossipListener, PropertiesUser, Restartable {

    List<Neighbor> getNeighbors();
    IxiModuleHolder getModuleHolder();
    Tangle getTangle();
    Properties getCopyOfProperties();

    void request(String transactionHash);
}
