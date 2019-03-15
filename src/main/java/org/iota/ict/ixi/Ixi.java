package org.iota.ict.ixi;

import org.iota.ict.model.transaction.Transaction;
import org.iota.ict.eee.EffectListener;
import org.iota.ict.network.gossip.GossipEvent;
import org.iota.ict.network.gossip.GossipListener;
import org.iota.ict.network.gossip.GossipPreprocessor;

import java.util.Set;

public interface Ixi {

    /**
     * Searches the local tangle for transaction associated with a specific address.
     * @param address The 81-tryte address for which to find all transactions.
     * @return All transactions found in the local tangle with the specific address field.
     * */
    Set<Transaction> findTransactionsByAddress(String address);


    /**
     * Searches the local tangle for transaction associated with a specific tag.
     * @param tag The 27-tryte tag for which to find all transactions.
     * @return All transactions found in the local tangle with the specific tag field.
     * */
    Set<Transaction> findTransactionsByTag(String tag);

    /**
     * Searches the local tangle for a specific transaction
     * @param hash The 81-tryte hash of the transaction to find.
     * @return The transaction with the respective hash or <code>null</code> if no such transaction was found.
     * */
    Transaction findTransactionByHash(String hash);

    /**
     * Adds a new transaction to the local tangle before broadcasting it to the network.
     * @param transaction The transaction to broadcast.
     * */
    void submit(Transaction transaction);

    /**
     * @param gossipListener The gossip listener to register.
     *
     * Registers a {@link GossipListener} whose {@link GossipListener#onGossipEvent(GossipEvent)} method will be invoked
     * when a new {@link GossipEvent} arrives. This is useful for event driven actions. All registered listeners will be
     * called synchronously. Therefore it is important that the invoked method is being processed quickly in order to not
     * block the {@link org.iota.ict.network.gossip.GossipEventDispatcher} which could cause latency issues for other
     * listeners as well as memory issues for the entire node due to the growing event dispatcher queue. If processing of
     * an event is expected to take longer, it should be queued instead to be processed by a separate thread.
     *
     * On termination of any IXI module, all registered listeners should be properly unregistered via {@link #removeGossipListener(GossipListener)} again.
     * */
    void addGossipListener(GossipListener gossipListener);

    /**
     * @param gossipListener Reference to the listener to be removed. Must have been registered previously.
     *
     * Unregisters a {@link GossipListener} previously registered via {@link #addGossipListener(GossipListener)}.
     * */
    void removeGossipListener(GossipListener gossipListener);

    /**
     * @param gossipPreprocessor The preprocessor to register.
     *
     * Registers a {@link GossipPreprocessor} in a similar fashion to {@link #addGossipListener(GossipListener)}. All
     * preprocessors will be ordered by their ascending {@link GossipPreprocessor#position} and linked together. Whenever
     * a new event is received, it will be put into the preprocessor's {@link GossipPreprocessor#incoming} queue. Until
     * the preprocessor passes the event on to its successor via {@link GossipPreprocessor#passOn(GossipEvent)}, the event
     * will not reach any other preprocessor. Only after an event has passed the last preprocessor, it will be passed on
     * to all registered {@link GossipListener}. This makes it possible to customly catch, replace or inject events. As
     * a possible use case, one could implement a persistence ixi module that injects events read from a database.
     * */
    void addGossipPreprocessor(GossipPreprocessor gossipPreprocessor);

    /**
     * @param gossipPreprocessor The previously registered preprocessor to remove.
     *
     * Unregisters a previously registered {@link GossipPreprocessor} in a similar fashion to {@link #removeGossipListener(GossipListener)}.
     * */
    void removeGossipPreprocessor(GossipPreprocessor gossipPreprocessor);

    /**
     * Determines the confidence with which a transaction is considered confirmed within the economic cluster the Ict is following.
     *
     * @param transactionHash The hash of the transaction whose approval confidence to determine.
     * @return Determined approval confidence for the transaction in the economic cluster.
     * */
    double determineApprovalConfidence(String transactionHash);

    /**
     * Adds an effect to a specific environment queue.
     *
     * @param environment the environment to which the effect should be sent
     * @param effectString g the effect
     */
    void submitEffect(String environment, String effectString);

    /**
     * Registers a new EffectListener.
     *
     * @param effectListener the EffectListner to that is to be registrated.
     */
    void addEffectListener(EffectListener effectListener);

}
