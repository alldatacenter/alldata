* 封装PostgreSQL作为数据源的DataSource插件，可以向TIS导入PostgreSQL中的数据表作后续分析处理
* PostgresqlReader
  
  实现了Alibaba DataXReader从PostgreSQL读取数据。在底层实现上，PostgresqlReader通过JDBC连接远程PostgreSQL数据库，并执行相应的sql语句将数据从PostgreSQL库中SELECT出来[详细](https://github.com/alibaba/DataX/blob/master/postgresqlreader/doc/postgresqlreader.md)
* PostgresqlWriter

  实现了Alibaba DataXWriter 插件，写入数据到 PostgreSQL主库目的表的功能。在底层实现上，PostgresqlWriter通过JDBC连接远程 PostgreSQL 数据库，并执行相应的 insert into ... sql 语句将数据写入 PostgreSQL，内部会分批次提交入库。 [详细](https://github.com/alibaba/DataX/blob/master/postgresqlwriter/doc/postgresqlwriter.md)
  
* 使用postgresql JDBC驱动（Java JDBC 4.2 (JRE 8+) driver for PostgreSQL database），版本：**42.3.1** [https://github.com/pgjdbc/pgjdbc](https://github.com/pgjdbc/pgjdbc)
