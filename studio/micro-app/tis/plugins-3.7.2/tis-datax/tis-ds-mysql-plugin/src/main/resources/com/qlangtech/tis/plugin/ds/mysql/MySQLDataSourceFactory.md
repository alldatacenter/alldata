## useCompression

与服务端通信时采用zlib进行压缩，效果请参考[https://blog.csdn.net/Shadow_Light/article/details/100749537](https://blog.csdn.net/Shadow_Light/article/details/100749537)

## splitTableStrategy

如数据库中采用分表存放，可以开启此选项，默认为： `off`(不启用)

`on`: 分表策略支持海量数据存放，每张表的数据结构需要保证相同，且有规则的后缀作为物理表的分区规则，逻辑层面视为同一张表。
如逻辑表`order` 对应的物理分表为：  `order_01`,`order_02`,`order_03`,`order_04`

[详细说明](https://tis.pub/docs/guide/datasource/multi-table-rule/)




