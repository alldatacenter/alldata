## inputDateformat
当数据字段内容为字符串类型，需要设定该字段的格式，例如：`yyyy-MM-dd HH:mm:ss`. 以便系统可以将字段内容解析成datetime实例

解析格式支持多种数据格式，不同的格式（format）之间需要用逗号分割，例如： `yyyy-MM-dd HH:mm:ss,yyyy-MM-dd HH:mm:ss.SSS`

## outputDateformat

将`datetime`实例格式化成分区格式字段:

1. 如果是按照天创建分区, 可以使用`yyyyMMdd`
2. 如果是按照月创建分区, 可以使用`yyyyMM`


