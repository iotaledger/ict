package org.iota.ict.ixi.rmi;

import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public abstract class IxiModule {

    static {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) { }
    }

    private RemoteIct ict;
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
            ict = (RemoteIct) Naming.lookup("//localhost/"+name);
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("Failed to accept connection to ict:");
            e.printStackTrace();
        }
    }

    public abstract void onIctConnect(String name);

    public abstract void onTransactionReceived(GossipReceiveEvent event);

    public abstract void onTransactionSubmitted(GossipSubmitEvent event);

    private void assertThatIctConnected() {
        if(ict == null)
            throw new RuntimeException("The Ict has not connected to this module yet.");
    }

    private class IxiModuleAdapter extends UnicastRemoteObject implements RemoteIxiModule {

        private IxiModuleAdapter(String name) throws RemoteException {
            super(0);
            try {
                Naming.rebind("//localhost/"+name, this);
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
        public void onIctConnect(String name) {
            IxiModule.this.setIct(name);
            IxiModule.this.onIctConnect(name);
        }
    }
}