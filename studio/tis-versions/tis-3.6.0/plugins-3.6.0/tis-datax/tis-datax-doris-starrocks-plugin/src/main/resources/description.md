* DorisWriter / StarRocksWriter  

    插件实现了写入数据到 StarRocks/Doris 主库的目的表的功能。在底层实现上，StarRocksWriter 通过Streamload以csv格式导入数据至StarRocks。
    [详细](https://github.com/StarRocks/DataX/blob/main/starrockswriter/doc/starrockswriter.md)
    
    该插件现同时支持StarRocks和Doris两种数据库
    
* DorisSourceFactory / StarRocksSourceFactory

  支持定义Doris和StarRocks两种类型的数据源   
