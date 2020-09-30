# Simple-Dynamo

## Objective
The main goal is to provide both availability and linearizability at the same time. 
The application perform read and write operations successfully even under failures. 
At the same time, a read operation always return the most recent value. 
The Application performs partitioning and replication, exactly the way Dynamo does.

The following is a guideline used to implement the Simple Dynamo based on the design of Amazon Dynamo:
### 1. Membership
a. Just as the original Dynamo, every node can know every other node.​ This means
that each node knows all other nodes in the system and also knows exactly which partition belongs to which node; any node can forward a request to the correct node without using a ring-based routing.
### 2. Request routing
a. Unlike Chord, each Dynamo node knows all other nodes in the system and also
knows exactly which partition belongs to which node.
b. Under no failures, a request for a key is directly forwarded to the coordinator (i.e., the successor of the key), and the coordinator should be in charge of serving read/write operations.
### 3. Quorum replication
a. For linearizability, implemented a quorum-based replication used by
Dynamo.
b. The replication degree N is 3.​ This means that given a key, the key’s
coordinator as well as the 2 successor nodes in the Dynamo ring should store the
key.
c. Both the reader quorum size R and the writer quorum size W is 2.
d. The coordinator for a get/put request should ​always contact other two nodes​ and
get a vote from each (i.e., an acknowledgement for a write, or a value for a read).
e. For write operations, all objects can be ​versioned​ in order to distinguish stale
copies from the most recent copy.
f. For read operations, if the readers in the reader quorum have different versions
of the same object, the coordinator picks the most recent version and
return it.

### 5. Failure handling
a. Handling failures is done very carefully because there can be many
corner cases to consider and cover.
b. Just as the original Dynamo, each request can be used to detect a node failure.
c. For this purpose, I used a timeout for a socket read;​ I picked a
reasonable timeout value, e.g., 100 ms, and if a node does not respond within
the timeout, I consider it a failure.
d. When a coordinator for a request fails and it does not respond to the request, ​its successor is contacted next for the request.
