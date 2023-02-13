* 封装Oracle作为数据源的DataSource插件，可以向TIS导入Oracle中的数据表作后续分析处理
* OracleReader
  
  实现了alibaba DataXReader从Oracle读取数据。在底层实现上，OracleReader通过JDBC连接远程Oracle数据库，并执行相应的sql语句将数据从Oracle库中SELECT出来。[详细](https://github.com/alibaba/DataX/blob/master/oraclereader/doc/oraclereader.md)

* OracleWriter

 实现了alibaba DataXWriter写入数据到 Oracle 主库的目的表的功能。在底层实现上， OracleWriter 通过 JDBC 连接远程 Oracle 数据库，并执行相应的 insert into ... sql 语句将数据写入 Oracle，内部会分批次提交入库。
 
 OracleWriter 面向ETL开发工程师，他们使用 OracleWriter 从数仓导入数据到 Oracle。同时 OracleWriter 亦可以作为数据迁移工具为DBA等用户提供服务。 [详细](https://github.com/alibaba/DataX/blob/master/oraclewriter/doc/oraclewriter.md) 
  