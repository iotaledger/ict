package org.iota.ict.ixi;

import org.iota.ict.Ict;
import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipListener;
import org.iota.ict.network.event.GossipReceiveEvent;
import org.iota.ict.network.event.GossipSubmitEvent;
import org.iota.ict.utils.ErrorHandler;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoteIctImplementation extends UnicastRemoteObject implements RemoteIct {

    static {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
        }
    }

    private final Map<String, RemoteIxiModule> ixiModulesByName = new HashMap<>();
    private final Map<RemoteIxiModule, GossipFilter> ixiModuleFilters = new HashMap<>();
    private final String name;
    private final Ict ict;

    public RemoteIctImplementation(final Ict ict) throws RemoteException {
        this.ict = ict;
        name = ict.getProperties().name;
        try {
            Naming.rebind("//localhost/" + name, this);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        ict.addGossipListener(new GossipListener() {
            @Override
            public void onTransactionSubmitted(GossipSubmitEvent event) {
                for (String ixiModuleName : ixiModulesByName.keySet()) {
                    RemoteIxiModule ixiModule = ixiModulesByName.get(ixiModuleName);
                    if (ixiModuleFilters.containsKey(ixiModule) && ixiModuleFilters.get(ixiModule).passes(event.getTransaction()))
                        try {
                            ixiModule.onTransactionSubmitted(event);
                        } catch (RemoteException e) {
                        }
                }
            }

            @Override
            public void onTransactionReceived(GossipReceiveEvent event) {
                for (String ixiModuleName : ixiModulesByName.keySet()) {
                    RemoteIxiModule ixiModule = ixiModulesByName.get(ixiModuleName);
                    if (ixiModuleFilters.containsKey(ixiModule) && ixiModuleFilters.get(ixiModule).passes(event.getTransaction()))
                        try {
                            ixiModule.onTransactionReceived(event);
                        } catch (RemoteException e) {
                        }
                }
            }
        });
    }

    @Override
    public void onIxiConnect(String ixiName) {
        try {
            RemoteIxiModule ixiModule = (RemoteIxiModule) Naming.lookup("//localhost/" + ixiName);
            ixiModulesByName.put(ixiName, ixiModule);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            ErrorHandler.handleError(Ict.LOGGER, e, "Failed to accept connecting IXI module '"+ixiName+"'.");
        }
    }

    @Override
    public Transaction submit(String asciiMessage) {
        return ict.submit(asciiMessage);
    }

    @Override
    public void submit(Transaction transaction) {
        ict.submit(transaction);
    }

    @Override
    public void setGossipFilter(String moduleName, GossipFilter filter) {
        RemoteIxiModule ixiModule = ixiModulesByName.get(moduleName);
        ixiModuleFilters.put(ixiModule, filter);
    }

    public void terminate() {
        // TODO
    }

    @Override
    public Transaction findTransactionByHash(String hash) {
        return ict.getTangle().findTransactionByHash(hash);
    }

    @Override
    public Set<Transaction> findTransactionsByAddress(String address) {
        return ict.getTangle().findTransactionsByAddress(address);
    }

    @Override
    public Set<Transaction> findTransactionsByTag(String tag) {
        return ict.getTangle().findTransactionsByTag(tag);
    }
}