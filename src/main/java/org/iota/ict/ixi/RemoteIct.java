package org.iota.ict.ixi;

import org.iota.ict.model.Transaction;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteIct extends Remote {

    Transaction findTransactionByHash(String hash) throws RemoteException;

    Transaction submit(String asciiMessage) throws RemoteException;

    void submit(Transaction transaction) throws RemoteException;
}