# FlowTorrent
A class project demonstrating basic concept of peer to peer downloading

# OVERVIEW

This is FlowTorrent, a simple bittorrent-like demonstration that illustrates
  the use of Java sockets, threading, and peer to peer networking. It contains
  integrated as well as explicit file-splitting tools, a configurator for
  specifying a test network, and torrent (NFO) files to allow multiple files
  sharing and checking for hash correctness.

# USAGE
```
  java FlowTorrent [chunk|execute|unchunk] options...
  java FlowTorrent --help
```

# DIRECTORY STRUCTURE
```
ChunkCommand.class
  Comand line parameter description for using the chunking tool.

com/
  JCommander dependency files, used to generate command line option parser.

CommanderMain.class
  Main command line parameter description file for global option flags.

config.txt
  Example network configuration for testing purposes.

DownloadData.class
  Peer download information manager that maintains piece status to keep
  all threads in sync and prevent accidental double or re-downloads.
  This is the primary synchronization object in FlowTorrent. All shared
  download state belongs here.

ExecuteCommand.class
  Command line parameter description file for the 'execute' mode.

FileChunker.class
  Static library (written by me) implementing file splitting and rejoin
  operations used in the rest of the program. 

FlowTorrent.class
  Main class of FlowTorrent. Builds and executes command line option
  parsers, configures the Peer object, adds Server and Client instances
  to the peer, and then takes the main thread into the Peer object
  to monitor the peer for download completion.

NetworkConfig.class
  Implements processing of config.txt. Real use of FlowTorrent will
  not require an explicit network configuration. Config.txt and this
  class for parsing it is used for demonstration purposes.

NFO.class
  Abstraction of the NFO file. Responsible for writing the Summary.txt
  and verifying the correct download of chunks. The NFO stores expected
  file names, cryptographic hashes, and checks full download correctness
  before rejoining the downloaded pieces.

Peer.class
  This class wraps PeerServer and PeerClient objects and traps the main
  thread until both the server and client threads terminate. A peer
  may harbor arbitrarily many server and client instances. 

PeerClient.class
  Runnable containing an instance of the client. This class should
  execute in its own thread and will manage one download neighbor
  relationship. DownloadData maintains the download state of the 
  client such that its progress does not conflict with the activity
  of the PeerServer instances.

PeerServer.class
  Runnable containing an instance of the server. This class should
  execute in its own thread and will manage any number of upload
  neighbor relationships. Sync is maintained through locking of
  the DownloadData object. 

PieceData.class
  Minor capsule-class used with DownloadData to store information
  about a piece and download status.

PieceStatus.class
  Enumeration type indicating WANT, DOWNLOADING, and HAVE status
  information about a chunk.

README.txt
  This file. 

ServerClientHandler.class
  Runnable task dispatched by the PeerServer to handle an accepted
  connection from an upload neighbor. This tasking frees the server
  to accept additional connections as soon as possible.

src/
  The Java source code directory for FlowTorrent.

UnchunkCommand.class
  Comand line parameter description for using the unchunking tool.

config.txt
  Initial configuration for project demonstration.
  
test.bin:
  Generic test file used for the project demonstration.
```

# EXAMPLE
```
  java FlowTorrent execute -config config.txt -dir test/s/ -pn 0 -server
  java FlowTorrent execute -config config.txt -dir test/c1/ -pn 1 -rhost 127.0.0.1
  java FlowTorrent execute -config config.txt -dir test/c2/ -pn 2 -rhost 127.0.0.1
```
