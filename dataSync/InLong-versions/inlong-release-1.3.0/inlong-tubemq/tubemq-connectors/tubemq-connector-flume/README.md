### TubeMQ Flume Connector
#### Prerequisites

Copy the following files to flume library path.

```
tubemq-connector-flume-[TUBEMQ-VERSION].jar
tubemq-client-[TUBEMQ-VERSION].jar
tubemq-core-[TUBEMQ-VERSION].jar
```

#### Flume Sink Configuration Template 

```
agent.sinks = tubemq
agent.sinks.tubemq.type = org.apache.flume.sink.tubemq.TubemqSink
// master host addresses, it could separate with ",".
agent.sinks.tubemq.master-host-port-list = 127.0.0.1:8000
// the default topic name, it could be override by topic in envent header.
agent.sinks.tubemq.topic = demo
```