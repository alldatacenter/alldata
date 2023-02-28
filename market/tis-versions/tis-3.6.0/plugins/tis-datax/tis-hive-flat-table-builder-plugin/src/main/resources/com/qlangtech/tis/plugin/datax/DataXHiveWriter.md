## tabPrefix

每次导入Hive表，会在对应源表前加一个后缀，这样更加符合数据仓库的规范，一般是加`ods_`, 用户也可以修改成其他值

## partitionRetainNum

每进行一次DataX导入在Hive表中会生成一个新的分区，现在系统分区名称为`pt`格式为开始导入数据的时间戳

## partitionFormat

每进行一次DataX导入在Hive表中会生成一个新的分区，现在系统分区名称为'pt'格式为开始导入数据的当前时间戳，格式为`yyyyMMddHHmmss`或者`yyyyMMdd`     
