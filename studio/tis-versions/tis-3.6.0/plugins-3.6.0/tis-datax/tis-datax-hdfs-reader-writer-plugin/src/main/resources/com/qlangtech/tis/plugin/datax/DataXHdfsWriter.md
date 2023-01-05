## path

存储到Hadoop hdfs文件系统的相对路径信息，HdfsWriter会根据并发配置在Path目录下写入多个文件。为与hive表关联，请填写hive表在hdfs上的存储路径。

例：Hive上设置的数据仓库的存储路径为：`/user/hive/warehouse/` ，已建立数据库：`test.db`，表：`hello`；则输入框中可输入`test.db/hello`, 最终对应HDFS中存储路径为：`/user/hive/warehouse/test.db/hello`  
  