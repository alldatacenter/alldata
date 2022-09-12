# Send Message To Multi-pulsar Cluster

# flume.conf
conf for use multi-pulsar cluster demo is like flume-mulit-pulsar-demo.conf
when use pulsar ,you can config these parameters to flume.conf like flume-mulit-pulsar-demo.conf:

 1. type (*): value must be 'org.apache.inlong.dataproxy.sink.PulsarSink'
 2. pulsar_server_url_list (*): value is pulsar broker url , like this 'pulsar://127.0.0.1:6650', multi-pulsar cluster use '|' as seperator
 3. send_timeout_MILL: send message timeout, unit is millisecond, default value is 30000 (mean 30s)
 4. stat_interval_sec: stat info will be made period time , unit is second, default value is 60s
 5. thread-num: sink thread num. default value  is 8
 6. client-id-cache: whether use cache in client, default value is true
 7. max_pending_messages: default value is 10000
 8. max_batching_messages: default value is 1000
 9. enable_batch: default is true
 10. block_if_queue_full: default is true