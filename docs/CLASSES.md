# Class Documentation

This file has been generated from the JavaDoc class documentation.

## org.iota.ict

### [Ict](../src/main/java/org/iota/ict/Ict.java)
This class is the central component of the project. Each instance is an independent Ict node that can communicate with
other Icts. This class is not supposed to perform complex tasks but to delegate them to the correct submodule. It can
therefore be seen as a hub of all those components which, when working together, form an Ict node.

### [Main](../src/main/java/org/iota/ict/Main.java)
This class controls what happens when the program is run by a user. It is the entry point when starting this application
and helps the user to set up a new Ict node. As such it is a convenience and not technically required to create Ict nodes.
A good example are the jUnit tests which work entirely independently from this class.

## org.iota.ict.utils

### [Properties](../src/main/java/org/iota/ict/utils/Properties.java)
With instances of this class, the Ict and its sub-components can be easily configured. The properties can be read from
files or defined during runtime. Some properties might not be changeable yet after passing them to the Ict.

### [Trytes](../src/main/java/org/iota/ict/utils/Trytes.java)
This class is a helper tool which allows to perform basic tryte operations, such as conversion between trytes and
trits, bytes, numbers and ascii strings. Each tryte String must consist entirely of the uppercase letters A-Z and
he digit 9 (see <b>TRYTES</b>. Each trit is a number -1, 0 or 1. 3 trits form one tryte (see <b>TRITS_BY_TRYTE</b>).
<p>
When compressed to bytes, 3 trytes (= 9 trits) are stored in 2 bytes. The first byte encodes the first 5 trits, the
second byte the other 4 trits.

### [Constants](../src/main/java/org/iota/ict/utils/Constants.java)
Important constants which are not changed during runtime but might be changed during development or are used by
multiple classes are kept together here to make them easier to find and adjust.

## org.iota.ict.network

### [Sender](../src/main/java/org/iota/ict/network/Sender.java)
This class sends transactions to neighbors. Together with the <b>Receiver</b>, they are the two important gateways
for transaction gossip between Ict nodes. Each Ict instance has exactly one <b>Receiver</b> and one <b>Sender</b>
to communicate with its neighbors.
<p>
The sending process happens in its own Thread to not block other components. Before being sent, transactions are put
into a <b>queue</b>. This class also requests transactions which are not known to the Ict but were referenced by
received transactions either through the branch or trunk.

### [Receiver](../src/main/java/org/iota/ict/network/Receiver.java)
This class receives transactions from neighbors. Together with the <b>Sender</b>, they are the two important gateways
for transaction gossip between Ict nodes. Each Ict instance has exactly one <b>Receiver</b> and one <b>Sender</b>
to communicate with its neighbors.

### [Neighbor](../src/main/java/org/iota/ict/network/Neighbor.java)
This class defines a neighbored Ict node. Neighbor nodes usually run remotely on a different device and connection
is established via the Internet. Besides the address, this class collect stats about the transaction flow from the
neighbor.

## org.iota.ict.model

### [BalanceChangeBuilder](../src/main/java/org/iota/ict/model/BalanceChangeBuilder.java)
The <b>BalanceChangeBuilder</b> makes it possible to accumulate transactions which are part of the same <b>BalanceChange</b>
via <b>append(Transaction)</b> or as container of <b>TransactionBuilder</b> (stored in <b>buildersFromTailToHead</b>)
during the creation of a new <b>Transfer</b>.

### [Transfer](../src/main/java/org/iota/ict/model/Transfer.java)
A <b>Transfer</b> is a <b>Bundle</b> which transfers value. Every transfer can be interpreted as a bundle and every
bundle as transfer. They provide different views on the same thing. The bundle perspective puts its focus on the
transactions and how they are linked together, while the transfer abstracts from the individual transactions and
groups them together into <b>BalanceChange</b> objects.
<p>
Compared to <b>Bundle</b>, this class also provides additional functionality which is only useful in the context of
value transfers but not for general data bundles (which do not actually transfer a value). Examples are <b>getSecurityLevel()</b>
and <b>areSignaturesValid()</b>.

### [Bundle](../src/main/java/org/iota/ict/model/Bundle.java)
This class allows to operate on the bundle structure, a linear sequence of linked <b>Transaction</b> objects.
Note that anything related to value transactions and signatures is not modelled by this class but by <b>Transfer</b>.
Instead, this classes scope is reduced to the core bundle structure, regardless of their content and interpretation.
<p>
Since it is not guaranteed that all transactions of a bundle become available at the same time, a bundle is not always
complete after instantiation. Whether it is can be checked with <b>isComplete()</b>. To fetch missing parts of a bundle,
unknown transactions must be requested from neighbors - which can be done via <b>tryToComplete(Ict)</b>.
<p>
Each transaction can be a bundle head or not (see <b>Transaction.isBundleHead</b>). The same applies to being a bundle
tail (see <b>Transaction.isBundleTail</b>). A bundle must always start with a bundle head and must end with a bundle
tail. Each inner transaction must be neither. If this principle is violated, the bundle structure is considered invalid.
This can be queried with <b>isStructureValid()</b>.

### [TransactionBuilder](../src/main/java/org/iota/ict/model/TransactionBuilder.java)
Since <b>Transaction</b> objects are final and their fields cannot be modified during runtime, the <b>TransactionBuilder</b>
can be used to define transaction fields before the transaction is created. This class is intended to create <b>new</b> transactions.

### [TransferBuilder](../src/main/java/org/iota/ict/model/TransferBuilder.java)
<b>TransferBuilder</b> is a tool to create a new <b>Transfer</b> via <b>buildBundle(Set, int)</b>.

### [Tangle](../src/main/java/org/iota/ict/model/Tangle.java)
Instances of this class provide a database which stores <b>Transaction</b> objects during runtime and allows to find
them by their hash, address or tag. Each <b>Ict</b> uses a <b>Tangle</b> object to keep track of all received transactions.

### [Transaction](../src/main/java/org/iota/ict/model/Transaction.java)
Instances of this class are IOTA transactions which together form a tangle. Actually an IOTA transaction is no more
than a sequence of trytes. This class makes it possible to interpret these trytes and operate on them. A transaction's
tryte sequence consists of multiple tryte fields (e.g. <b>address</b>, <b>value</b>, <b>nonce</b>, see <b>Transaction.Field</b>).
of static length. With this class, these fields can be easily accessed. <b>Transaction</b> objects are data  objects,
and their fields are not supposed to change after instantiation. To create custom transaction, one should use <b>TransactionBuilder.build()</b>.

### [BalanceChange](../src/main/java/org/iota/ict/model/BalanceChange.java)
A <b>BalanceChange</b> models a proposed change for the IOTA token balance of an IOTA address. Since tokens can neither be
burned nor created, no positive or negative <b>BalanceChange</b> cannot exist on its own but requires other balance
changes so the sum of their proposed changes is zero. They are grouped together in a <b>Transfer</b>.
<p>
Depending on its proposed change, each instance is either an input or an output (see <b>isInput()</b> and <b>isOutput()</b>).
Inputs have a negative <b>value</b> (they remove funds from an address so that another address can receive them). Outputs have a positive or a zero <b>value</b>.
In inputs, the <b>signatureOrMessage</b> is used as signature signing the proposed change with the addresses private key,
in outputs as optional message.
<p>
Each <b>BalanceChange</b> is realized through at least one transactions. The required amount depends on the length
of <b>signatureOrMessage</b>.

### [BundleBuilder](../src/main/java/org/iota/ict/model/BundleBuilder.java)
Similar to <b>TransactionBuilder</b>, <b>BundleBuilder</b> makes it possible to create a <b>Bundle</b>.
Bundles are read from head to tail but created from tail to head. This is why it makes sense to have a dedicated class
for this purpose.
