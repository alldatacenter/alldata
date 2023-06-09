# [DRILL-7863](https://issues.apache.org/jira/browse/DRILL-7863): Add Storage Plugin for Apache Phoenix

## Description

 Phoenix say : "We put the SQL back in NoSQL",<br/>
 Drill call : "We use the SQL to cross almost all the file systems and storage engines",<br/>
 "Cheers !", users said.

## Documentation

Features :

 - Full support for Enhanced Vector Framework.
 - Tested in phoenix 4.14 and 5.1.2.
 - Support the array data type.
 - Support the pushdown (Project, Limit, Filter, Aggregate, Join, CrossJoin, Join_Filter, GroupBy, Distinct and more).
 - Use the PQS client (6.0).

Related Information :

 1. PHOENIX-6398: Returns uniform SQL dialect in calcite for the PQS
 2. PHOENIX-6582: Bump default HBase version to 2.3.7 and 2.4.8
 3. PHOENIX-6605, PHOENIX-6606 and PHOENIX-6607.
 4. DRILL-8060, DRILL-8061 and DRILL-8062.
 5. [QueryServer 6.0.0-drill-r1](https://github.com/luocooong/phoenix-queryserver/releases/tag/6.0.0-drill-r1)

## Configuration

 To connect Drill to Phoenix, create a new storage plugin with the following configuration :

Option 1 (Use the host and port):

```json
{
  "type": "phoenix",
  "host": "the.queryserver.hostname",
  "port": 8765,
  "enabled": true
}
```

Option 2 (Use the jdbcURL) :

```json
{
  "type": "phoenix",
  "jdbcURL": "jdbc:phoenix:thin:url=http://the.queryserver.hostname:8765;serialization=PROTOBUF",
  "enabled": true
}
```

Use the connection properties :

```json
{
  "type": "phoenix",
  "host": "the.queryserver.hostname",
  "port": 8765,
  "props": {
    "phoenix.query.timeoutMs": 60000
  },
  "enabled": true
}
```

Tips :
 * More connection properties, see also [PQS Configuration](http://phoenix.apache.org/server.html).
 * If you provide the `jdbcURL`, the connection will ignore the value of `host` and `port`.
 * If you [extended the authentication of QueryServer](https://github.com/Boostport/avatica/issues/28), you can also pass the `userName` and `password`.

```json
{
  "type": "phoenix",
  "host": "the.queryserver.hostname",
  "port": 8765,
  "userName": "my_user",
  "password": "my_pass",
  "enabled": true
}
```

### Impersonation
Configurations :
1. Enable [Drill User Impersonation](https://drill.apache.org/docs/configuring-user-impersonation/)
2. Enable [PQS Impersonation](https://phoenix.apache.org/server.html#Impersonation)
3. PQS URL:
  1. Provide `host` and `port` and Drill will generate the PQS URL with a doAs parameter of current session user
  2. Provide the `jdbcURL` with a `doAs` url param and `$user` placeholder as a value, for instance:
     `jdbc:phoenix:thin:url=http://localhost:8765?doAs=$user`. In case Drill Impersonation is enabled, but `doAs=$user`
     is missing the User Exception is thrown.

## Testing

 The test framework of phoenix queryserver required the Hadoop 3, but exist `PHOENIX-5993` and `HBASE-22394` :

```
" The HBase PMC does not release multiple artifacts for both Hadoop2 and Hadoop3 support at the current time.
Current HBase2 releases still compile against Hadoop2 by default, and using Hadoop 3 against HBase2
requires a recompilation of HBase because of incompatible changes between Hadoop2 and Hadoop3. "
```

### Recommended Practices

 1. Download HBase 2.4.2 sources and rebuild with Hadoop 3.

    ```mvn clean install -DskipTests -Dhadoop.profile=3.0 -Dhadoop-three.version=3.2.3```

 2. Remove the `Ignore` annotation in `PhoenixTestSuite.java`.
    
  ```
    @Ignore
    @Category({ SlowTest.class })
    public class PhoenixTestSuite extends ClusterTest {
   ```
    
 3. Go to the phoenix root folder and run test.

  ```
  cd contrib/storage-phoenix/
  mvn test
  ```

### To Add Features

 - Don't forget to add a test function to the test class.
 
 - If a new test class is added, please declare it in the `PhoenixTestSuite` class.

### Play in CLI

```
apache drill> use phoenix123.v1;
+------+-------------------------------------------+
|  ok  |                  summary                  |
+------+-------------------------------------------+
| true | Default schema changed to [phoenix123.v1] |
+------+-------------------------------------------+
1 row selected (0.308 seconds)
apache drill (phoenix123.v1)> select n_regionkey, max(n_nationkey) from nation group by n_regionkey having max(n_nationkey) > 20;
+-------------+--------+
| n_regionkey | EXPR$1 |
+-------------+--------+
| 1           | 24     |
| 2           | 21     |
| 3           | 23     |
+-------------+--------+
3 rows selected (0.734 seconds)
apache drill (phoenix123.v1)> select a.n_name, b.r_name from nation a join region b on a.n_regionkey = b.r_regionkey where b.r_name = 'ASIA';
+-----------+--------+
|  n_name   | r_name |
+-----------+--------+
| INDIA     | ASIA   |
| INDONESIA | ASIA   |
| JAPAN     | ASIA   |
| CHINA     | ASIA   |
| VIETNAM   | ASIA   |
+-----------+--------+
5 rows selected (1.199 seconds)
apache drill (phoenix123.v1)> select n_name, n_regionkey from nation limit 3 offset 10;
+--------+-------------+
| n_name | n_regionkey |
+--------+-------------+
| IRAN   | 4           |
| IRAQ   | 4           |
| JAPAN  | 2           |
+--------+-------------+
3 rows selected (0.77 seconds)
```
