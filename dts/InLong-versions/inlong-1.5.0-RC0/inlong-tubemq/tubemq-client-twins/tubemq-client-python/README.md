### TubeMQ Python Client
TubeMQ Python Client library is a wrapper over the existing [C++ client library](https://github.com/apache/inlong/tree/master/inlong-tubemq/tubemq-client-twins/tubemq-client-cpp/) and exposes all of the same features.

#### Install from source
##### install python-devel
- build and install C++ client SDK

build C++ client SDK from source, and install:

1, copy `include/tubemq` directory  to `/usr/local/include/`

2, copy `./release/tubemq/lib/libtubemq_rel.a` to `/usr/local/lib`
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

This is a simple example for how to use Python TubeMQ producer,  like Java/C++ producer, the `master_addr`, `topic_list` should be provided. A more detailed example is [`src/python/example/test_producer.py`](src/python/example/test_producer.py)

```python
import time
import tubemq
import tubemq_message

topic_list = ['demo']
MASTER_ADDR = "127.0.0.1:8000"

# Start producer
producer = tubemq.Producer(MASTER_ADDR)

# publish the topic
producer.publish(topic_list)

# wait for the first heartbeath to master ready
time.sleep(10)

# Test Producer
send_data = "hello_tubemq"
while True:
    msg = tubemq_message.Message(topic_list[0], send_data, len(send_data))
    res = producer.send(msg, is_sync=True) # default is asynchronous mode, convience for demo
    if res:
        print("Push successfully!!!")

# Stop the producer
producer.stop()
       
```



##### Consumer example

The following example creates a TubeMQ consumer with a master IP address, a group name, and a subscribed topic list. The consumer receives incoming messages, prints the length of messages that arrive, and acknowledges each message to the TubeMQ broker.
```python
import time
import tubemq

topic_list = ['demo']
MASTER_ADDR = '127.0.0.1:8000'
GROUP_NAME = 'test_group'

# Start consumer
consumer = tubemq.Consumer(MASTER_ADDR, GROUP_NAME, topic_list)

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
