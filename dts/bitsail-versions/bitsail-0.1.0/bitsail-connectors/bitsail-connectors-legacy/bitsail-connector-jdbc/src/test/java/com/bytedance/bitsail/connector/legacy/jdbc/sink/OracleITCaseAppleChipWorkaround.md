# Oracle Integration Test Apple Chip Workaround

Integration test of OracleSinkITCase or OracleSourceITCase may get Error of `SP2-0306: Invalid option.` when running on Apple M chips 
 because docker image of **gvenzl/oracle-xe:18.4.0-slim** cannot run on ARM chips via Docker Desktop. Reference https://hub.docker.com/r/gvenzl/oracle-xe

## Manual workaround steps:
1. Prerequisites:
   1. Install [Colima](https://github.com/abiosoft/colima#installation)
   1. Install Database IDE such as [DataGrip](https://www.jetbrains.com/datagrip/)
1. In Java files (Breakdown to OracleSinkITCase or OracleSourceITCase):
   1. OracleSinkITCase:
      1. Comment out line of `Startables.deepStart(Stream.of(container)).join();`
      1. Bypass connection parameters by replacing `connection.put` lines in *testInsertModeOracle()* with
         1. `connection.put("db_url", "jdbc:oracle:thin:@localhost:1521:");`
         1. `connection.put("host", "localhost");`
         1. `connection.put("port", "1521");`
   1. OracleSourceITCase:
      1. Comment out line of `Startables.deepStart(Stream.of(container)).join();`
      1. Bypass connection parameters by replacing builder lines of connectionInfo in *testInsertModeOracle()* with
         1. `.host("localhost")`
         1. `.port("1521")`
         1. `.url("jdbc:oracle:thin:@localhost:1521:")`
1. Run Colima to spin up x86_64 software on Apple M chips by commanding `colima start --arch x86_64 --memory 4` in terminal
1. After seeing `INFO[0000] colima is running` in terminal, run docker command in terminal to setup Oracle database:
    `docker run -d -p 1521:1521 -e ORACLE_PASSWORD=PW gvenzl/oracle-xe:18.4.0-slim`
1. Log in above database and do following SQL scripts (Recommend to use DataGrip or other database IDE)
   1. Login as SYSTEM with parameters: 
      - `Name=<self defined>`
      - `Host=localhost`
      - `Port=1521`
      - `User=SYSTEM`
      - `Password=PW`
      - `URL=jdbc:oracle:thin:@localhost:1521:`
   1. Create another user TEST by SQL script `CREATE USER TEST IDENTIFIED BY TEST_PASSWORD`
   1. Grant all privilege for user TEST by SQL script `GRANT ALL PRIVILEGES TO TEST`
   1. Login with TEST with parameters:
      - `Name=<self defined>`
      - `Host=localhost`
      - `Port=1521`
      - `User=TEST`
      - `Password=TEST_PASSWORD`
      - `URL=jdbc:oracle:thin:@localhost:1521:`
   1. Create a table named `ORACLE_DYNAMIC_TABLE` under created Oracle database (Breakdown to OracleSinkITCase or OracleSourceITCase):
      1. OracleSinkITCase: SQL script is from [fake_to_oracle_sink.sql](../../../../../../../../resources/scripts/fake_to_oracle_sink.sql)
      1. OracleSourceITCase: SQL script is from [oracle_source_to_print.sql](../../../../../../../../resources/scripts/oracle_source_to_print.sql)
1. You are free to run integration test NOW
1. After integration test, cleanup environment:
   1. Cleanup docker container:
      1. Get container id by commanding `docker container ls` in terminal
      1. Stop and remove container by commanding `docker container stop <container id> && docker container rm <container id>` in terminal
   1. Stop Colima by commanding `colima stop` in terminal
   1. Revert code-base changes