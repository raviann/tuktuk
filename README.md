# tutuk

This app deals with provising APIs to receive driver location coordinates, and exposes API for customer to query for near-by drivers 

# Tech-stack 

Scala + Akka + MongoDB

When we talk about APIs and cloud, today's deployment model is primarily based on micro-services. For such micro-services JVM based tech stack would offer less friction and easy deployability. The problem with J2EE or Spring based stack is that they are little bloated w.r.to what exactly is needed. Also the thread model being followed can cause race conditions and adds to debugging nightmares at times

Instead chose Scala + Akka combination. Akka provides the actor model where threads don't share any state. Instead, the communication happens via passing messages across the actors (messages are immutable). This is based on Erlang model. Scala is being chosen becuase of my comfort level in it and it encourages immutable state, which is ideal when processing huge requests and chances of race condition does not arise as long as one sticks to immutability. Don't want to go with typeless language, as lots of errors can be mitigated during compile time.

Coming to choice of datastore. One of the considerations is to serve the request under 100ms latency. This requirement itself cuts out disk based storage and processing, and instead seeks RAM based solution. Also, support for Geo-spatial queries eliminates lot of in-memory solutions.
Would have preferred memsql + mysql (or) mysql ndb cluster, in any other alternate scenario where data loss cannot be tolerated. As far I remember, MongoDB does not gaurantee that writes to disk from RAM is losless, in case of failure (clustering mitigates it). In this location based APIs, a location update from driver even if missed, is not going to cause any damage to business. Cluster set-up has not been tried upon by me, which is required for production deployment


# Pre-requisites to start the tuktuk server
1. scala needs to be installed on the machine. The version being referred in build.sbt is 2.11.7
   The same can be downloaded from https://www.scala-lang.org/download/2.11.7.html
2. sbt (scala build tool) needs to be installed on machine. The same can be downloaded from http://www.scala-sbt.org/download.html 
3. MongoDB should be downloaded from https://www.mongodb.com/download-center?jmp=docs&_ga=1.269111985.1379895954.1491210920#community and unzipped 

# Installation and setup
## Setup Mongo
1. Unzip the downloaded mongoDB zip, navigate to bin folder
2. $ mongod --dbpath <some_location_where_data_to_be_kept>
   Refer to https://docs.mongodb.com/manual/tutorial/manage-mongodb-processes/
   Mongo should be runing on port 27017 (default port). API server relies on this port 
3. Create DBs and collections. In another shell, from 'bin' folder, run the following:
   1. mongo 
   2. use tuktuk
   3. db.createCollection("drivers")
   4. use test
   5. db.createCollection("testDrivers") 

## Setup Server
1. git clone https://github.com/raviann/tuktuk.git
2. cd tuktuk
3. sbt run

You should notice server running on port 8080. 
Ignore error messages due to Mongo cluster settings. Shall work on removing them later

# Command to run the test cases
1.sbt test  (run from inside tuktuk directory being cloned)

Ensure mongoDB is up and running before testcases are run. Ideally we can do with embedded mongoDB for UT, but not sure whether it supports spatial queries or not. This is like a spike

# To test from Curl or Postman
Kept the postman collection here: https://github.com/raviann/tuktuk/tree/master/client/postman. This can be imported to postman chrome plugin for REST API testing. Curl commands are kept at https://github.com/raviann/tuktuk/blob/master/client/curl.txt (these worked on mac)

# Code flow
Boot.scala -> WebServer.scala. object Boot is the main one and server port etc. can be changed here.
WebServer.scala has the REST Apis and MongoDB calls. 'def compositeRoute' in WebServer.scala handles the REST API calls.

The following line in Boot.scala links the REST APIs in WebServer.scala

val bindingFuture = Http().bindAndHandle(compositeRoute(driversCollection), "localhost", 8080)

# TODO:
1. All exception handling cases are not covered with proper messages
2. Moving some constants out to configuration files, as this would change based on which cloud hosting provider one leverages
3. Metrics and alerts. These are heavily dependent on cloud platform stack
4. Code coverage percentage not checked
