## ignoringSendingException
required boolean parameter responsible for raising (false) or not (true) ClickHouse sending exception in main thread. 
 - if ignoring-clickhouse-sending-exception-enabled is **是**, exception while clickhouse sending is ignored and failed data automatically goes to the disk. 
 - if ignoring-clickhouse-sending-exception-enabled is **否**, clickhouse sending exception thrown in "main" thread (thread which called ClickhHouseSink::invoke) and data also goes to the disk.
 
 ## timeout

 加载数据超时时间
 
 单位: **秒**
