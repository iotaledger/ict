# Class Documentation

This file has been generated from the JavaDoc class documentation.

## org.iota.ict

### Ict
This class is the central component of the project. Each instance is an independent Ict node that can communicate with
other Icts. This class is not supposed to perform complex tasks but to delegate them to the correct submodule. It can
therefore be seen as a hub of all those components which, when working together, form an Ict node.
### Main
This class controls what happens when the program is run by a user. It is the entry point when starting this application
and helps the user to set up a new Ict node. As such it is a convenience and not technically required to create Ict nodes.
A good example are the jUnit tests which work entirely independently from this class.

## org.iota.ict.utils

### Properties
With instances of this class, the Ict and its sub-components can be easily configured. The properties can be read from
files or defined during runtime. Some properties might not be changeable yet after passing them to the Ict.

### Trytes
This class is a helper tool which allows to perform basic tryte operations, such as conversion between trytes and
trits, bytes, numbers and ascii strings. Each tryte String must consist entirely of the uppercase letters A-Z and
he digit 9 (see **#TRYTES**. Each trit is a number -1, 0 or 1. 3 trits form one tryte (see **#TRITS_BY_TRYTE**).
<p>
When compressed to bytes, 3 trytes (= 9 trits) are stored in 2 bytes. The first byte encodes the first 5 trits, the
second byte the other 4 trits.
### Constants
Important constants which are not changed during runtime but might be changed during development or are used by
multiple classes are kept together here to make them easier to find and adjust.

## org.iota.ict.network

### Sender
This class sends transactions to neighbors. Together with the **Receiver**, they are the two important gateways
for transaction gossip between Ict nodes. Each Ict instance has exactly one **Receiver** and one **Sender**
to communicate with its neighbors.
<p>
The sending process happens in its own Thread to not block other components. Before being sent, transactions are put
into a **#queue**. This class also requests transactions which are not known to the Ict but were referenced by
received transactions either through the branch or trunk.

### Receiver
This class receives transactions from neighbors. Together with the **Sender**, they are the two important gateways
for transaction gossip between Ict nodes. Each Ict instance has exactly one **Receiver** and one **Sender**
to communicate with its neighbors.

### Neighbor
This class defines a neighbored Ict node. Neighbor nodes usually run remotely on a different device and connection
is established via the Internet. Besides the address, this class collect stats about the transaction flow to the
neighbor.

## org.iota.ict.model

### Bundle
This class allows to operate on the Bundle structure, a linear sequence of linked transactions. Note that anything
related to value transactions and signatures is not modelled by this class. Instead, this class is reduced to the
core Bundle structure.

