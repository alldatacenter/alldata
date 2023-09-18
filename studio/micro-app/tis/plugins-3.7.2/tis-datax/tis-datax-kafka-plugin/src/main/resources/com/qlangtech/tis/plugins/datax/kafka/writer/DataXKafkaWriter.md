## acks

The number of acknowledgments the producer requires the leader to have received before considering a request complete. This controls the  durability of records that are sent.

**all**: 这意味着leader需要等待所有备份都成功写入日志，这种策略会保证只要有一个备份存活就不会丢失数据。这是最强的保证

## batchSize

The producer will attempt to batch records together into fewer requests whenever multiple records are being sent to the same partition.

控制发送者在发布到kafka之前等待批处理的字节数。满足batch.size和ling.ms之一，producer便开始发送消息
