## tabDecorator

进入离线计算引擎的表有时候需要对原始表进行别名处理

* `off`: 保留原始表名称
* `tabPrefix`: 在原始表之前添加一个前缀，例如：`ods_`

## partitionRetainNum

每进行一次DataX导入在Hive表中会生成一个新的分区，现在系统分区名称为`pt`格式为开始导入数据的时间戳

## partitionFormat

每进行一次DataX导入在Hive表中会生成一个新的分区，现在系统分区名称为'pt'格式为开始导入数据的当前时间戳，格式为`yyyyMMddHHmmss`或者`yyyyMMdd`     
