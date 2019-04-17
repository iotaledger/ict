# Ict - Software Architecture

## Abstract
This paper documents architectural design choices in regards to the implementation of Ict. It was written with the intent to be used as a reference for completely rewriting the current Java prototype (this repository) from scratch and utilizing the knowledge that was gained during the development of the first Ict iteration.

## About Ict
The Iota Controlled agenT (Ict) is an IOTA node. In contrast to [IRI](https://github.com/iotaledger/iri), it is a light-weight node designed specifically for the Internet-of-Thing. It provides a basic gossip protocol which can be extended with various functionality through the IOTA eXtending Interface (IXI). This modular design enables the customization of the core node, allowing for all kinds of extensions to be plugged in. Ict nodes are intended to be used for a specific use cases in contrast to the IRI full node that provides general access to the Tangle while the applications are running remotely and connecting through the client libraries. Instead, the applications in Ict - the IXI modules - are working closely with the node and define how it behaves and which transactions it processes.

## Top Level Architectural Overview

<img src="https://raw.githubusercontent.com/iotaledger/ict/master/docs/assets/deployment.png" />

# Components

## Network

<img src="https://raw.githubusercontent.com/iotaledger/ict/master/docs/assets/network.png" />

*An Ict node is deployed on some kind of server, usually a VPS or a raspberry PI. Each Ict node is ideally connected to three other Ict nodes (the neighbors) usually over the Internet. The Node component acts as interface to the network and is responsible for the communication with neighbors. The communication is limited to transaction gossip. UDP is used to transact messages of fixed length. Each such message consists of a transaction and the hash of any other transaction the sending node is requesting. Neighbors answer these requests by responding with the requested transaction in the transaction part of the message.*

### Neighbors
Instances of the Neighbor class model our neighboring nodes. In order to evaluate the quality of each neighbor, we count the amount of transactions we received from each node in a separate stats object. We regularly archive this Stats object and replace it with a new Stats instance starting all counts at zero. This happens after each round (a user defined time interval). The round duration is specified in the properties.

Each stats instances tracks the following integer values:


Name | Increased for every incoming packet that... | Ideally
-- | -- | --
`txsOld` | contains a transaction that is neither invalid nor ignored but has already been received before. | low
`txsNew` | contains a transaction that is neither invalid nor ignored and new to us. | high
`txsRequested` | contains a transaction request we are going to answer. | low
`txsInvalid` | cannot be decoded to a valid transaction (missing proof-of-work). | very low
`txsIgnored` | we ignore (spam protection or unexpected timestamp, see Persistence) | low

In order to identify the origin of incoming traffic, each Neighbor provides a `isOriginOf()` function that allows the Receiver to determine whether a certain packet did originate from that neighbor. Note that the port cannot always be resolved in which case the IP should provide a fallback ([issue #3](https://github.com/iotaledger/ict/issues/3)).


### Message
There is only one object being transferred between nodes: the message. Each message is represented through bytes and encodes two parts: the trytes of a transaction to forward and the transaction hash of another transaction to request. The first part is used to transfer transactions between nodes (either active forwarding of new received transactions or to answer custom transaction requests). The second part is used to request transactions. If there are no transactions to request, this field is set to NULL (all trytes filled with `9`s). Requests are not forwarded.

### Sender
The Sender is responsible for broadcasting transactions to neighbors and responding to requests. To avoid redundant gossip, it does not forward transactions to neighbors from which these transactions were originally received.

### Receiver
The receiver listens on the node socket for incoming packets, identifies the neighbor who submitted the packet and decodes them into transactions. If no neighbor could be identified, the packet is dropped without further processing to prevent DoS attacks.

There are two possible ways to connect the Receiver to the Sender in order to broadcast new received transactions. The obvious is simply calling the Sender from the Receiver. Another more modular way, where the receiver must not be aware of the sender, is to register the Sender as a gossip listener (see Gossip). Another advantage of the letter approach is that this allows EEE to be used for submitting new transactions. Thus `submit` in the IXI can be projected onto the `submitEffect()` method through a helper method (which is available for IXI modules but not part of the actual IXI interface). I recommend this to further minimize the IXI. But most importantly, this approach allows gossip preprocessor to intervene into the forwarding of transactions (see Gossip Preprocessors).

#### Avoid Redundant Decoding and Hashing
Quite frequently the same transaction is received multiple times from different neighbors. For performance reasons, one should check whether the transaction has already been received before decoding the bytes to trytes and calculating the hash of the transaction.

To enable that we compare the bytes received to the byte representation of any already known transaction that has the same nonce. We can do that because the nonce is used to perform proof-of-work and can therefore be expected to be a unique identifier for most transactions. Note that this is easy to bypass by using a different field for noncing purposes and hence the uniqueness is not guaranteed, so we should rather use a multimap to map nonces to their respective transactions.

The approach is simple:

1. After receiving incoming bytes, we decode only the trytes of the nonce field.
2. We then find all transactions associated with this nonce from our multimap.
3. Depending on the amount we then decide whether we should check against all those transactions (linear complexity O(N) where N is the amount of transactions with the same nonce) or whether it’s more efficient to skip this approach and simply decode and hash the transaction (constant complexity O(1)). If we do not consider skipping, we provide an attack surface which is vulnerable to anyone spamming transactions with the same nonce, resulting in increasing computational effort.
4. We iterate over all transactions and check whether any of these have the same bytes we received. If so, we already know this transaction and should not process it any further. If not, it is a new transaction and we should further process it by decoding and calculating the hash.

### Spam Protection
Currently we use hash-based proof-of-work as an intermediate spam protection mechanism. Note that the minimum weight magnitude must be sufficiently low to allow Raspberry Pis to participate in the network by issuing transactions.

In the future we will use some kind of network-bound proof-of-work (specification is being worked out by research team). Additionally, once Economic Clustering is reliable, it can provide the stakes required by the [Rate Control Algorithm](https://blog.iota.org/whos-in-who-s-out-a-rate-control-algorithm-for-the-tangle-c7b5ecf85677).

## The Local Tangle

*New transactions received by the Node will be forwarded to the Tangle. This component is responsible for storing transactions during runtime. It provides an interface to find a specific transaction by its hash or a set of transactions by their address or tag. If the Node receives a transaction request from a neighbor, it looks for the requested transaction in the Tangle by its hash and answers the request if the transaction was found. This component must therefore be thread-safe.*

### Persistence
Ict is not intended to store transactions persistently (persistence could be realized through an IXI module). All transactions can be kept in the working memory and should be pruned away in a queue like fashion (FIFO) after a while similar to a ring memory. Depending on the abilities of the programming language, the tangle size/capacity should either be configurable or the pruning should be dynamically based on the amount of free memory. Note that when removing transactions, you do not want to add them to the Tangle again in case you receive them a second time. To prevent this, Ict currently rejects transactions whose timestamp is not in a certain interval (±20s) relative to the current local time on the device.

Further, you might have to do some language-specific cleaning up when deleting transactions. Transactions might be referenced from many other components (bundle collector, IXI modules, gossip events, tangle vertexes). Make sure that these components do neither hinder the deletion of the transaction nor run into bugs because a transaction suddenly went missing.

### Compression
To increase the amount of transactions that can be stored in the working memory, a compression is useful. Transaction objects contain a lot of redundant data because the values of most transaction fields are available as tryte sequences. Each tryte only encodes one of 27 possible values. But because tryte sequences are implemented as strings or byte arrays, each tryte requires a full byte.

Therefore it makes sense to store the compressed byte representation that is used to transfer transactions between nodes. Trytes can than be derived from these bytes on demand. This buys memory at the cost of time. However, if the mechanism is designed in a smart fashion, we can reduce the amount of decompressions. Because every decompression costs time, trytes should be kept decompressed until we assume that nobody is using them any longer.

Note that we have to decompress all trytes anyways right after receiving the transaction in order to calculate the transaction hash and verify that the correct amount of proof-of-work was done. Since new transactions will be shown around to any components who have registered themselves as gossip listeners on the EEE infrastructure, we should wait at least until every listener has seen that transaction before deciding to throw away our decompressed trytes for memory reasons.

Additionally, one could implement a counter of active viewers of a transaction. Transactions will not be decompressed before that viewer count reaches zero. This allows components to actively intervene into the compression of frequently read transactions as well as transactions which are put aside but are expected to be used soon.

### Vertexes
The Tangle is a directed acyclic graph (DAG). The Tangle graph is obviously important in many ways. Therefore it is required to model the direct dependencies between transactions. This includes linking each transaction to the two transactions referenced (its parents) as well as to transactions referencing it (its children).

Rather than doing so in the transaction objects themselves - as it is done in the current implementation - this should be done on a higher level data structure we will call Vertex. Transaction objects should be pure data representations without any external relations.

Please note that it is not guaranteed that you receive parents before children. Therefore make sure that all edges are added in retrospect once the second transaction (parent or child) is received.


## The IXI (IOTA eXtending Interface)

*The Ict component organizes the node by bundling and connecting all components together. It also provides the IXI (Iota eXtending Interface) - the interface used for applications/plugins (IXI Modules) to communicate with the Tangle. The IXI is a central component of Ict that enables a high grade of modularity and allows to extend the node with custom functionality through IXI modules. Therefore the design of IXI is critical. IXI abstracts from the operational side of the node (neighbors, configurations, etc.) and provides only a minimal interface to “talk to” the tangle without seeing the inner workings of Ict.*

### Transaction Related Functionality

IXI Function | Description
--- | ---
findTransactionByHash(hash) | Returns the transaction with the specific hash from the local tangle.
findTransactionsByAddress(address) |Returns all transactions with the specific address.
findTransactionByTag(tag) | Returns all transactions with the specific tag.
submit(transaction) | Adds a new transaction to the local tangle and broadcasts it to the network. You might project this function to EEE (see Receiver).

### EEE Related Functionality

IXI Function | Description
--- | ---
addListener(effectListener) | Registers an effect listener (see EEE) that will be notified about new effects submitted to its environment.
removeListener(effectListener) | Unregisters a previously registered effect listener.
submitEffect(environment, effect) | Submits an effect to a certain environment by notifying every registered listener of that environment about the effect.

# Logic and Utils
## Trytes
*Transactions are stored in ternary format. They are not naturally represented by bytes (`0`, `1`) but as trits (`-`, `0`, `1`). However, due to the lack of ternary hardware, we can only emulate trits on top of the binary hardware available. Further, because there are no ternary data types available in most programming languages, we have to use bytes to represent trits internally. In order to not waste an entire byte to represent only 3 states, we will encode trytes (triplets of trits) instead. A tryte can represent 27 different values. For readability purposes we chose to represent bytes in human readable form with the characters 27 consisting of the 26 letters from ‚A‘ to ‚Z‘ and the ‚9‘ character.*

### Conversion Trit – Integer
In order to represent numbers with trits, we have to encode integers into trit sequences. We chose the balanced ternary format. In a trit sequence (t0,t1,…,tn) each trit ti encodes the coefficient for the respective potence of 3: 3i. The values are then summed up to calculate the resulting integer. For example: `(-,1,0,0)` encodes `-1•3⁰ + 1•3¹ + 0•3² + 0•3³ = -1•1 + 1•3 = -1 + 3 = 2`.

Note that this representation is bijective. Also note that the negative of any integer can simply be derived by inversing all trits (`-`→`1`, `0`→`0`, `1`→`-`). Further note that in contrast to binary (where 8 bits could encode all integers from -128 to +127), the smallest integer that can be encoded is the negative of the largest.

### Conversion Trit – Trytes
Since trytes are simply triplets of trits, technically no conversion is required. However, since we decided to use the characters ‚9‘ and ‚A‘-‚Z‘ for representation, we have to provide a mapping between the readable tryte string format and the raw trit representation.

To map a trit triplet (t0, t1, t2) to the corresponding tryte representation, we first calculate the integer represented by this trit triplet (see Conversion Trit – Integer). We then let the integer value 0 encode the `9`, the values from +1 to +13 are mapped to ‚A‘ - ‚M‘ and the remaining values ranging from -13 to -1 to ‚N‘ - ‚Z‘ in this order.

### Conversion Trit – Ascii
In order to truly encode human readable formats, we need a way to convert between trits and ASCII. ASCII encodes 127 different values. We will use two trytes to encode one ASCII character. Each ASCII character is first mapped to its integer value which is then simply converted to a 6 trit sequence.

Two trytes allow us to encode 27x27 = 729 different values. Therefore this mechanism can also be used to encode entire byte arrays. In many circumstances it might be useful to use an advanced encoding to reduce the amount of redundancy. We will leave it open to improve the encoding.

### Conversion Trit – Byte
In some circumstances we want to avoid the redundant tryte representation to maximize the amount of trits we can represent in a byte array of limited size. Most importantly, this byte representation will be used for network communication because bandwidth will be the most restricting resource.

The conversation used differs from the method currently implemented in IRI. Instead, Ict utilizes a 5+4 encoding where two bytes encode 9 trits: the first byte 5 and the second 4. A small overhead is left for convenience. Since 2 bytes always encode 3 trytes, it is easy to extract the byte representation of certain tryte sub-sequence simply by multiplying the tryte offsets with 2/3.


## Cryptography
The final hashing and signature scheme has not been decided on. We recommend implementing a stubs to keep it abstract and easily replaceable. For hashing use Troika or Curl. Since Ict is still in testnet phase and many Ict nodes will run on constrained devices it is reasonable to lower the amount of hashing rounds performed in order to increase performance at the kost of security. For the signature scheme please implement [this Lamport signature scheme](https://iota.stackexchange.com/questions/1690/iota-winternitz-signature-scheme-details/1701#1701) on top of the hashing function.

## Transactions
### Implementation
Transaction objects should be pure data representations. They should not be dependent on a certain context (e.g. reference a Tangle or other transaction objects). It has been of advantage to implement a TransactionField class (current implementation: Transaction.Field) which specifies the tryte and byte offsets and lengths of each transaction field. This keeps the code cleaner because offsets and lengths can be represented by references to the according transaction field instance instead of constant numbers in the code which must be updated individually if the transaction structure should change.
### Structure
<img src="https://raw.githubusercontent.com/iotaledger/ict/master/docs/assets/tx_structure.png" />

The transaction structure in the current Ict version is mostly similar to the current transaction structure as used in the IRI with a few modifications:

* fields have been rearranged into a slightly different order
* the total transaction size did not change
* new fields have been added:
  * Extra Data (Digest) (81 trytes)
  * Bundle Nonce (27 trytes)
  * Time Lock Lower Bound (9 trytes)
  * Time Lock Upper Bound (9 trytes)
* old fields have been removed:
  * Bundle (81 trytes)
  * Obsolete Tag (27 trytes)
  * Current Index (9 trytes)
  * Last Index (9 trytes)



### Bundle
A bundle is a linear sequence of transactions linked together through their trunk. Each transaction can be a bundle head. Independently of that, each transaction can be a bundle tail. Whether a transaction is one of these, is defined through trits #1 and #2 (with #0 being first trit) of the transaction hash. If the respective trit is 1, the flag is set to true. If the trit is -1, the flag is set to false. If the trit is 0, the transaction is invalid.

A bundle starts at the bundle head. From there it can be fetched by following the trunk chain until reaching a bundle tail. To create a bundle, one must start with the tail and work towards the head. A single transaction that is both bundle head and bundle tail, represents a complete bundle of size 1. If in the linear trunk chain sequence, a second bundle head appears before any bundle tail is reached, the bundle is considered invalid.

<img src="https://raw.githubusercontent.com/iotaledger/ict/master/docs/assets/bundle_structure.png" />

A bundle consists of inputs which deduct tokens from an address and outputs which add tokens to an address. Each input requires a signature of the associated address. Each output can contain a message. Depending on their length, each signature or messages of an input or output is fragmented and distributed over one or more transactions. The SignatureOrMessage fields of these transactions contain the message or signature fragments in correct order (order pointing into bundle tail direction). The first transaction of each input must have a negative value, the first transaction of each output must have a non-negative value. All other transactions must have a value of exactly zero. All transactions in an input or output must share the same address.


#### Bundle Hash
To build the bundle hash, go from head to tail to concatenate the following trytes:
 - if transaction is part of an input: concatenate the essence (see transaction structure picture).
 - else (part of an output): concatenate the 123-round hash of the Signature or Message field and the essence

#### Security Level
The security level can be derived from the 81-tryte bundle hash:
* if the sum of the first 27 trytes of the bundle hash is not zero, the security level is 0
* else if the sum of the second 27 trytes of the bundle hash is not zero, the security level is 1
* else if the sum of the third 27 trytes of the bundle hash is not zero, the security level is 2
* else if the sum of the third 27 trytes of the bundle hash is zero, the security level is 3
So for security level 3, the sum of all three 27-tryte segments must be zero. The sum of a tryte sequence is defined as the sum of the balanced ternary values of their trit-triplets. For example: 

```
sum(--10-01-0) = (--1) + (0-0) + (1-0) = 5 + (-3) + (-2) = 0
```

#### Signature Validation
The first fragment of each input's signature signs the first 27 trytes of the bundle hash. For security level 2 and 3, the second fragment signs the second 27 trytes of the bundle hash. For security level 3, the third fragment signs the third 27 trytes. If the security level is 3, there can be more input fragments. The n-th fragment signs the n%3-th segment of the bundle hash. For example, the fourth and seventh fragment sign the first 27 trytes, the fifth and eighth fragment signs the second 27 trytes. This enables multi-signatures.

#### Validity
A bundle is considered valid if the following conditions are met:
* the structure is valid
* the sum of the values of all transactions in the bundle is zero
* for security levels less than 3, the amount of fragments per input equals the security level
* for security levels 3, each input consists of at least 3 fragments
* the signatures of all inputs are valid

#### Bundle Collector
Ict should only work with complete bundles. Incomplete bundles shouldn’t reach the inner state of Ict and bundles should be deleted as a whole. The task of the Bundle Collector is to intercept transactions until all transactions in the bundle are available.
...

## EEE (Environment Entity Effect)

*Due to the modularity and event-based nature of Ict, a standardized communication infrastructure can help to improve the overall design. EEE (Environment Entity Effect) provides such a mechanism. It is mainly used for two purposes: to allow IXI modules to communicate with each other (inter-ixi communication) and to notify components (IXI modules and native components such as the Sender) about newly received transactions. A general description of EEE as well as its appliance to inter-ixi communication can be found [here](https://github.com/iotaledger/ixi/blob/master/docs/inter-ixi.md). The advantage of EEE is that it offers a streamlined mechanism where no entity must be aware of the location of the other entity. Neither the language the other entity is written in, nor the path nor whether that entity is on the local device or the other side of a socket is relevant to communicate.*

### Gossip
Until Ict version 0.5, gossip (incoming/outgoing transactions) propagated  between components within Ict via its own infrastructure. Functionally the infrastructure was identical to EEE, therefore it was translated to EEE with 0.6. This measure simplified the IXI because the gossip specific hooks `addGossipListener()` and `removeGossipListener` could be replaced with the general-purpose EEE equivalents `addListener()` and `removeListener`.

As of now there are two kinds of gossip events that are propagated, outgoing transactions (to be submitted) and incoming transactions (received). The GossipEvent class should reference a transaction object and contain some kind of flags to identify whether that transaction belongs to the outgoing or incoming group.

### Gossip Preprocessors
Since IXI modules are extensions that customize the Ict node for a specific use case, they should be able to put themselves between the network and the inner tangle. This can happen either by intervening to prevent transactions from being forwarded or by inserting transactions into the Tangle even though they were not received (e.g. an IXI module loading transactions from a persistent file or database). This functionality is realized through gossip preprocessors.

Gossip preprocessors offer a conveyor like mechanism for gossip events. Gossip preprocessor are linked together in a chain like structure. A gossip event is sent to a specific environment, the first preprocessor at the start of the chain at works itself through it until it has been processed by the last preprocessor and leaves the chain at the other end. Each gossip preprocessor within this chain can provide gossip events as output which will be the inputs for the next preprocessor. This allows gossip preprocessors to reject or inject gossip events from/for further processing.

Other than in the current implementation I advice against modifying EEE to enable this additional functionality. Instead, the chain architecture should be realized on top of EEE. This can happen through a specific environment (e.g. `GP_manager`) where listeners can register their position. A supervising listener listens to that environment and keeps track of every gossip preprocessor that has registered itself. New gossip events are inserted into a starting environment (e.g. `GP_start`), from where the supervisor forwards them to the first gossip preprocessor. Each preprocessor provides a numeric position so that they can be ordered. If the lowest registered position is -1337, the supervisor would forward the gossip event to the preprocessors environment (e.g. `GP_input#-1337`). That preprocessor can then post its output to an output environment (e.g. `GP_output#-1337`) from where it’s picked up by the supervisor again to be forwarded to the next preprocessor until the event reaches the end of the chain which would be the input for the gossip listeners.

A smarter approach could reduce the amount of necessary environments by bundling all input and output environments into single environments which are used by all preprocessors simultaneously. The current position would be encoded in the effect and only the preprocessor responsible for the current position would process the respective event. It’s even possible to combine them all into a single environment. I leave it open to decide on how to realize this technically. However, if designed properly, no module programmer should have to be aware about the technical translation to EEE.

### Virtual Functions
In some use cases - especially for inter ixi communication - it is necessary to call functions via EEE. Virtual function calls can be implemented on top of EEE (see [here](https://github.com/iotaledger/ixi/blob/master/docs/inter-ixi.md#virtual-function-calls)). This allows IXI modules to publicly offer their functionality in a service oriented fashion, enabling inter-ixi dependencies while maintaining a high grade of modularity.

### Implementing the E’s
There are two things an entity can do: submit effects and listen to effects. For the former we do not need a separate class because this kind of entities are simply the objects evoking the `submitEffect()` method provided by EEE. The latter kind of entities will however require an EffectListener class that can be notified about new effects published to the environment it’s listening to.

The EffectListener has been implemented to contain the name of the environment, although it would be possible to implement it in a way where the environment is specified when adding the listener: `addListener(environment, listener)`. However, in that case the removing of listeners would be more complicated than when the environment is an intrinsic attribute of the listener. For simplicity reasons we have designed the listener so that each listener is part of exactly one environment, although it should be reevaluated whether there might be use cases where one listener instance should be able to subscribe to multiple environments..

It makes sense to type the environment with separate classes which can be projected to strings instead of simply using raw strings. That is because there are certain subsets of environments which are identified through custom prefixes, suffixes or patterns in the string. When using classes, the prefixes, suffixe and patterns can be managed from one central point rather than specifying and forcing module programmers to learn certain environment naming conventions.

Since inter-ixi communication uses strings for effects and the gossip architecture uses custom gossip events, the general EEE architecture should not make any assumptions on effect type and be kept as abstract as possible.

### The EffectDispatcher
Besides these three E’s, a central point connecting all environments, listeners and effects is required. The EffectDisptacher allows listeners to subscribe/unsubscribe and keeps track of which listeners are subscribed to which environments. It also offers a public hook for submitting an effect to an environment.

## Restartable Thread
Due to the modularity and for performance reasons many components are running in separate threads. To reduce the complexity, it is highly recommended to streamline the threading process. That can be done by implementing a base class that manages most operations of a restartable thread. Some documentation of the currently implemented method in the context of IXI modules can be found [here](https://github.com/iotaledger/ixi/blob/master/docs/guide.md#starting-and-terminating). Good practice is utilizing a state machine for this purpose (states: starting, started or running, terminating, terminated). Make sure the thread can be restarted and provides a way to register subworkers that will automatically be started/terminated when the mother thread is started/terminated. Further, it might be useful to implement a onStart/onTerminate and a onStarted/onTerminated hook that is called before/after starting/terminating the subworkers.


## Properties
*We will need some variables that can be adjusted/configured by the user. For example, the user should be able to change the neighbors. For that it makes sense to implement a properties class that keeps track of all variables. I recommend designing this class with scalability in mind from the beginning as if it would have to handle hundreds of variables. This will make sure that new variables can be added, changed or removed with a single line instead of having to change the source code in 10 different places - as it is necessary in the current Java implementation.*

The user should be able to update the properties during runtime (e.g. from the GUI) without having to restart the node. To allow that a PropertiesUser interface can be used. This interface would specify a function `updateProperties(Properties properties)` that is evoked in the top level class (should be Ict) when the properties change. Each class that works with configurable variables or contains components that do, would implement that interface and update its internal variables based on the new properties object or delegate the changed properties to the respective sub-component.

If components are storing the entire properties object rather than only specific values of that and make this object available to other components (e.g. through a getter `getProperties()`), it should be possible to lock the properties object from manipulation - direct changes to the properties object from a remote component instead of using the `updateProperties()` hook. This is the reason why the current implementation provides two versions of properties: FinalProperties and EditableProperties, that can be converted into each but only by creating a separate instance.

Also make sure that the properties object can be persisted as a file in a readable manner.

## Economic Clustering
Economic Clustering is the standard consensus mechanism we will use for Ict. It allows nodes to build clusters (fuzzy partitions of the Tangle) and follow economic actors in that cluster who perform the ledger validation. The set of actors followed acts similar to a decentralized and probabilistic coordinator. This allows weak edge devices to work with consensus without having to do the ledger validation themselves.

Economic Clustering will not be implemented into Ict itself. It can run as a separate IXI module on top. Besides reducing the code base and increasing modularity, this allows to test different consensus mechanisms on top of Ict. Detailed documentation of Economic Clustering can be found in the [official repository](https://github.com/iotaledger/ec.ixi).

## API
Ict provides an API that is used by the Web GUI to connect to Ict. Additionally, the API allows IXI modules to access certain functionality (such as the neighbors) that is not available through the IXI. A detailed documentation of the API endpoints provided can be found [here](https://qubiota.com/ict-rest). We recommend using a very similar API, possibly with minor improvements to allow the current GUI to be deployed with little to no changes.

## IXI Modules
### Implementation
IXI modules must most likely run in their own thread and therefore should be derived from [RestartableThread](#RestartableThread). Additionally, modules should offer an `install()` and `uninstall()` hook that is invoked upon (un)installation (see [here](https://github.com/iotaledger/ixi/blob/master/docs/guide.md#the-installation-process)).

### Bridge.ixi
It should be possible to write IXI modules in almost every major language. However, it is sufficient if Ict enables support of modules written in the native programming language. Other languages can be supported indirectly through [Bridge.ixi](https://github.com/iotaledger/bridge.ixi).

### The `module.json` File
To improve the user experience, Ict utilizes metadata provided by IXI modules in order to reason about the module and offer additional services, such as providing descriptions of IXI modules, automatically downloading dependencies or updating modules. This metadata is specified in a `module.json` file that should either be part of all modules or be available on every module’s repository so that Ict can download and store it separately. The structure of this file should be well thought through so it can be interpreted from future Ict versions.

### The `versions.json` File
Currently modules must provide an additional `versions.json` file that specifies which module version to download from GitHub based on the Ict version but it might make sense to combine both into a single artifact. Because this file is version overlapping, it should not be downloaded but looked up in the repository.
