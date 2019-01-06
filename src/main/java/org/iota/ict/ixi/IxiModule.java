package org.iota.ict.ixi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public abstract class IxiModule {

    final Logger logger = LogManager.getLogger(IxiModule.class);

    static {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }
    }

    private final String name;
    private final RemoteIct ict;

    public IxiModule(String name, String ictName) {
        try {
            this.name = name;
            new IxiModuleAdapter(name);
            ict = (RemoteIct) Naming.lookup("//localhost/" + ictName);
            ict.onIxiConnect(name);
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setGossipFilter(GossipFilter filter) {
        assertThatIctConnected();
        try {
            ict.setGossipFilter(name, filter);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction findTransactionByHash(String hash) {
        assertThatIctConnected();
        try {
            return ict.findTransactionByHash(hash);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Transaction> findTransactionsByAddress(String address) {
        assertThatIctConnected();
        try {
            return ict.findTransactionsByAddress(address);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Transaction> findTransactionsByTag(String tag) {
        assertThatIctConnected();
        try {
            return ict.findTransactionsByTag(tag);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction submit(String asciiMessage) {
        assertThatIctConnected();
        try {
            return ict.submit(asciiMessage);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void submit(Transaction transaction) {
        assertThatIctConnected();
        try {
            ict.submit(transaction);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void onTransactionReceived(GossipReceiveEvent event);

    public abstract void onTransactionSubmitted(GossipSubmitEvent event);

    private void assertThatIctConnected() {
        if (ict == null)
            throw new RuntimeException("The Ict has not connected to this module yet.");
    }

    private class IxiModuleAdapter extends UnicastRemoteObject implements RemoteIxiModule {

        private IxiModuleAdapter(String name) throws RemoteException {
            super(0);
            try {
                Naming.rebind("//localhost/" + name, this);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onTransactionReceived(GossipReceiveEvent event) {
            IxiModule.this.onTransactionReceived(event);
        }

        @Override
        public void onTransactionSubmitted(GossipSubmitEvent event) {
            IxiModule.this.onTransactionSubmitted(event);
        }
    }
}