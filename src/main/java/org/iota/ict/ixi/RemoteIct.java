package org.iota.ict.ixi;

import org.iota.ict.model.Transaction;
import org.iota.ict.network.event.GossipFilter;
import org.iota.ict.network.event.GossipListener;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface RemoteIct extends Remote {

    Set<Transaction> findTransactionsByAddress(String address) throws RemoteException;

    Set<Transaction> findTransactionsByTag(String tag) throws RemoteException;

    Transaction findTransactionByHash(String hash) throws RemoteException;

    Transaction submit(String asciiMessage) throws RemoteException;

    void setGossipFilter(String moduleName, GossipFilter filter) throws RemoteException;

    void submit(Transaction transaction) throws RemoteException;

    void onIxiConnect(String name) throws RemoteException;
}