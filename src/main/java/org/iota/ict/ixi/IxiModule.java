package org.iota.ict.ixi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public abstract class IxiModule {

    final Logger logger = LogManager.getLogger();

    static {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }
    }

    private RemoteIct ict;
    private String ictName;
    private final IxiModuleAdapter adapter;

    public IxiModule(String name) {
        try {
            adapter = new IxiModuleAdapter(name);
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

    private void setIct(String name) {
        try {
            ict = (RemoteIct) Naming.lookup("//localhost/" + name);
            ictName = name;
        } catch (Throwable t) {
            logger.error("Failed to accept connection to ict '" + name + "'", t);
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public abstract void onIctConnect(String name);

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

        @Override
        public void onIctConnect(String name) throws RemoteException {
            if (ict != null) {
                logger.warn("Refusing Ict '" + name + "' (already connected to '" + ictName + "').");
                throw new RemoteException("IXI is already connected to Ict '" + ictName + "'");
            }
            IxiModule.this.setIct(name);
            IxiModule.this.onIctConnect(name);
        }
    }
}