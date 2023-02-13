# DINKY SUPPORT RUNNING HIVEQL ON FLINK

## 1、新增hive2flink元数据

## 2、新增hive2flink任务类型

## 3、引入flink-sql-parser-hive解析HiveSQL

## 4、启用Flink natively support REST Endpoint and HiveServer2 Endpoint

### 4.1 ./bin/sql-gateway.sh start -Dsql-gateway.endpoint.type=hiveserver2

### 4.2 Flink CONF配置：sql-gateway.endpoint.type: hiveserver2

## 5、使用样例

```markdown
    @Test
    void testAlterDatabase() {
        sql("alter database db1 set dbproperties('k1'='v1')")
                .ok("ALTER DATABASE `DB1` SET DBPROPERTIES (\n" + "  'k1' = 'v1'\n" + ")");
        sql("alter database db1 set location '/new/path'")
                .ok("ALTER DATABASE `DB1` SET LOCATION '/new/path'");
        sql("alter database db1 set owner user user1")
                .ok("ALTER DATABASE `DB1` SET OWNER USER `USER1`");
        sql("alter database db1 set owner role role1")
                .ok("ALTER DATABASE `DB1` SET OWNER ROLE `ROLE1`");
    }
```
