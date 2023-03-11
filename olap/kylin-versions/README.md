# HYBRID OLAP FOR DATA PLATFORM

```markdown

基于Kylin二次开发Hybrid OLAP。

1、Kylin元数据包括Cube定义、星型/雪花模型定义、Job和Job执行信息、维度信息等

以json格式存储在HBase(也支持存储在MySQL) HBase:存储Cube的地方，充分利用了HBase的随机读写、横向扩展能力

Kylin元数据会默认生成两张表：

desc kylin_metadata;

+--------------------+--------------+------+-----+---------+-------+
| Field              | Type         | Null | Key | Default | Extra |
+--------------------+--------------+------+-----+---------+-------+
| META_TABLE_KEY     | varchar(255) | NO   | PRI | NULL    |       |
| META_TABLE_TS      | bigint(20)   | YES  | MUL | NULL    |       |
| META_TABLE_CONTENT | longblob     | YES  |     | NULL    |       |
+--------------------+--------------+------+-----+---------+-------+

desc kylin_metadata_log;
+--------------------+--------------+------+-----+---------+-------+
| Field              | Type         | Null | Key | Default | Extra |
+--------------------+--------------+------+-----+---------+-------+
| META_TABLE_KEY     | varchar(255) | NO   | PRI | NULL    |       |
| META_TABLE_TS      | bigint(20)   | YES  | MUL | NULL    |       |
| META_TABLE_CONTENT | longblob     | YES  |     | NULL    |       |
+--------------------+--------------+------+-----+---------+-------+

2、基于Kylin提供的RestfulAPI和JDBC/ODBC接口，第三方webapp或者基于SQL的BI工具，包括Excel都可以轻松跟Kylin集成。

常见BI工具均可整合：Tableau，PowerBI/Excel，MSTR，QlikSense，Hue和SuperSet等。

http://kylin.apache.org/docs16/howto/howto_use_restapi.html

```