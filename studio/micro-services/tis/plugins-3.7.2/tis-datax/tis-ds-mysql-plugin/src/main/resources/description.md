* 封装MySQL作为数据源的DataSource插件，可以向TIS导入MySQL中的数据表作后续分析处理
* MysqlReader

  MysqlReader插件实现了从Mysql读取数据。在底层实现上，MysqlReader通过JDBC连接远程Mysql数据库，并执行相应的sql语句将数据从mysql库中SELECT出来[详细](https://github.com/alibaba/DataX/blob/master/mysqlreader/doc/mysqlreader.md)
  
* MysqlWriter

  实现了Alibaba DataXWriter 插件，写入数据到 Mysql 主库的目的表的功能。在底层实现上， MysqlWriter 通过 JDBC 连接远程 Mysql 数据库，并执行相应的 insert into ... 或者 ( replace into ...) 的 sql 语句将数据写入 Mysql，内部会分批次提交入库，需要数据库本身采用 innodb 引擎。 [详细](https://github.com/alibaba/DataX/blob/master/mysqlwriter/doc/mysqlwriter.md)