## incrMode

控制写入数据到目标表采用 insert into 或者 replace into 或者 ON DUPLICATE KEY UPDATE 语句

## uniqueKey

* 描述：当写入模式为 update 和 replace 时，需要指定此参数的值为唯一索引字段
* 注意：如果此参数为空，并且写入模式为 update 和 replace 时，应用会自动获取数据库中的唯一索引；
如果数据表没有唯一索引，但是写入模式配置为 update 和 replace，应用会以 insert 的方式写入数据；
