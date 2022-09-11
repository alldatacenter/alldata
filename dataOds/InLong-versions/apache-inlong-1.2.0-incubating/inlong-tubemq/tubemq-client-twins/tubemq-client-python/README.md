### TubeMQ Python Client
TubeMQ Python Client library is a wrapper over the existing [C++ client library](https://github.com/apache/incubator-tubemq/tree/master/tubemq-client-twins/tubemq-client-cpp/) and exposes all of the same features.

#### Install from source
##### install python-devel
- build and install C++ client SDK

build C++ client SDK from source, and install:

1, copy `tubemq` include directory  to `/usr/local/include/`

2, copy `libtubemq_rel.a` to `/usr/local/lib`
&nbsp;

- install python-devel
```
yum install python-devel -y
```
- install required dependency
```
pip install -r requirements.txt
```

- install client
```
pip install ./
```

#### Examples
##### Producer example
##### Consumer example

The following example creates a TubeMQ consumer with a master IP address, a group name, and a subscribed topic list. The consumer receives incoming messages, prints the length of messages that arrive, and acknowledges each message to the TubeMQ broker.
```
import time
import tubemq

topic_list = ['demo']
master_addr = '127.0.0.1:8000'
group_name = 'test_group'

# Start consumer
consumer = tubemq.consumer(master_addr, group_name, topic_list)

# Test consumer
start_time = time.time()
while True:
    msgs = consumer.receive()
    if msgs:
        print("GetMessage success, msssage count =", len(msgs))
        consumer.acknowledge()

    # used for test, consume 10 minutes only
    stop_time = time.time()
    if stop_time - start_time > 10 * 60:
        break

# Stop consumer
consumer.stop()
```
