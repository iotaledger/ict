# Iota Controlled Agent (Ict)

## About

The Iota Controlled Agent is a light-weight IOTA node for the Internet-of-Things relying on swarm logic.
It provides a basic gossip protocol which can be extended with various functionality through the IOTA Extension Interface (IXI).
This modular design enables the customization of the core node, allowing for all kinds of extensions to be plugged in.

## Getting Started

### Installation

#### Method A: Download the Pre-Compiled .jar File

This method is recommended for beginners.

1. Go to [releases](https://github.com/iotaledger/ict/releases).
2. Download **ict-{VERSION}.jar** from the most recent release.
3. Move the .jar file to your favourite directory/folder.


#### Method B: Build the .jar Yourself

This method is recommended for advanced users. You have to install **Git** and **Gradle**.

```shell
# 1) clone the source code (you will need Git)
git clone https://github.com/iotaledger/ict

# 2) move into the cloned repository
cd ict

# 3) build the runnable .jar file (you will need Gradle)
gradle fatJar
```

### Running the Client

You will need the JRE (Java Runtime Environment) or JDK (Java Development Kit).

```shell
# 1) move to whatever directory/folder where your .jar file is in
cd Desktop/ict/

# 2) run the .jar file
java -jar ict-{VERSION}.jar
```

On the very first start, this will create a configuration file (**ict.cfg**). Restart your Ict after modifying the configuration.

### Arguments

You can pass the following arguments to the .jar file when running it.

Argument|Alias|Example|Description
---|---|---|---
`-config`|`-c`|`-config ict.cfg`|Loads the Ict configuration from the specified file.

## IXI Modules

An example/template for an IXI module can be found on [iotaledger/ixi](https://github.com/iotaledger/ixi).

## Development

This project is still in early development. Community contributions are highly appreciated.
