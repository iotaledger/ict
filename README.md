# Iota Controlled agenT (Ict)

## About

The Iota Controlled agenT is a light-weight IOTA node for the Internet-of-Things relying on swarm logic.
It provides a basic gossip protocol which can be extended with various functionality through the IOTA Extension Interface (IXI).
This modular design enables the customization of the core node, allowing for all kinds of extensions to be plugged in.

## Getting Started

### Installation

#### Method A: Download the Pre-Compiled .jar File

This method is recommended for beginners.

1. Go to [releases](https://github.com/iotaledger/ict/releases).
2. Download **ict.jar** from the most recent release.
3. Move the .jar file to your favourite directory/folder.


#### Method B: Build the .jar Yourself

This method is recommended for advanced users. You have to install **Git**, **Gradle** and **NPM**.

```shell
# 1) clone the source code (you will need Git)
git clone https://github.com/iotaledger/ict

# 2) move into the cloned repository
cd ict

# 3) build the runnable .jar file (you will need Gradle)
gradle fatJar

#4) install dependencies and build the web gui
cd web
npm install
npm run build
```

### Running the Client

You will need the JRE (Java Runtime Environment) or JDK (Java Development Kit).

```shell
# 1) move to whatever directory/folder where your .jar file is in
cd Desktop/ict/

# 2) run the .jar file (example: java -jar ict-0.5.jar)
java -jar ict-[VERSION].jar
```

Use `--config-create` option to create a configuration file (**ict.cfg**). Restart your Ict after modifying the configuration.

### Arguments

You can pass the following arguments to the .jar file when running it.

Argument|Alias|Example|Description
---|---|---|---
`--config`|`-c`|`--config ict.cfg`|Loads the Ict configuration from the specified file.
`--config-print`| |`--config-print`|Print the Ict configuration to stdout and exit.
`--config-create`| |`--config-create`|Write the Ict configuration './ict.cfg'.
`--debug`|`--verbose`|`--debug`|Set root log level to 'debug' (default: INFO).
`--trace`|`--verbose2`|`--trace`|Set root log level to 'trace' (default: INFO).
`--logfile-enabled`| |`--logfile-enabled`|Enable logging to 'logs/ict.log'.
`--log-dir DIR`| |`--log-dir /tmp/`|Write logs to existing 'DIR' (default: logs/).
`--log-file FILE`| |`--log-file ict-log.txt`|Write logs to 'FILE' (default: ict.log).

### Usage 

```bash
# Usage: ict [OPTIONS]

  Start a 'ict' instance by config.

# Options
--help|-h              Print this help and exit
--config|-c FILE       Use this config 'FILE' (default: ./ict.cfg;if-exist)
                       - lookup first on environment for uppercase property keys
                       - and as last in system properties
--config-create        Create or overwrite './ict.cfg' file with parsed config
--config-print         Print parsed config and exit
                       - on verbose print cmdline- and log4j-config too

--logfile-enabled      Enable logging to 'logs/ict.log'
--log-dir DIR          Write logs to existing 'DIR' (default: logs/)
--log-file NAME        Write logs to 'FILE' (default: ict.log)
-v|--verbose|--debug   Set log.level=DEBUG (default:INFO)
-vv|--verbose2|--trace Set log.level=TRACE (default:INFO)

# Sample
$ict --config-print                   # print out config
$ict --config my-ict.cfg              # use my config
$ict --config my-ict.cfg  -Dport=1234 # use my config but with port=1234
$PORT=1234 && ict --config my-ict.cfg # use my config with port=1234 if not declared
```

## IXI Modules

An example/template for an IXI module can be found on [iotaledger/ixi](https://github.com/iotaledger/ixi).

## Development

This project is still in early development. Community contributions are highly appreciated. Please read our [contribution
sheet](/docs/CONTRIBUTE.md) first.
