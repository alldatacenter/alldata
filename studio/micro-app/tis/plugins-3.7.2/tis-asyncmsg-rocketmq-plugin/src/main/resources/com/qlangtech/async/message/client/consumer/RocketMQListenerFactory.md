## consumeName
 用于标示消费客户端，当消费者重启之后可以利用该标示所对应的服务端游标，重新从上次消费点消费消息
 
## deserialize

* defaultJson

    默认反序列化方式，从MQ中读取到字节流之后通过Alibaba FastJson反序列化成[DTO对象](https://github.com/qlangtech/tis-solr/blob/master/tis-plugin/src/main/java/com/qlangtech/tis/realtime/transfer/DTO.java)
    
    
## namesrvAddr

  通过封装[rocketmq-client](https://github.com/apache/rocketmq/tree/master/client)，消费RocketMQ中的消息。
  
  样例代码[simple-example](https://rocketmq.apache.org/docs/simple-example/)
  
  ```java
public class Consumer {

    public static void main(String[] args) throws InterruptedException, MQClientException {

        // Instantiate with specified consumer group name.
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
         
        // Specify name server addresses.
        consumer.setNamesrvAddr("localhost:9876");
        
        // Subscribe one more more topics to consume.
        consumer.subscribe("TopicTest", "*");
        // Register callback to execute on arrival of messages fetched from brokers.
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        //Launch the consumer instance.
        consumer.start();

        System.out.printf("Consumer Started.%n");
    }
}
  ```
 

