## startupOptions

Optional startup mode for Oracle CDC consumer, valid enumerations are "initial" and "latest-offset". Please see Startup Reading Positionsection for more detailed information.

参数详细请参考：[https://ververica.github.io/flink-cdc-connectors/master/content/connectors/oracle-cdc.html#connector-options](https://ververica.github.io/flink-cdc-connectors/master/content/connectors/oracle-cdc.html#connector-options)
，[https://debezium.io/documentation/reference/1.5/connectors/oracle.html#oracle-connector-properties](https://debezium.io/documentation/reference/1.5/connectors/oracle.html#oracle-connector-properties)

* `Initial`:
  Performs an initial snapshot on the monitored database tables upon first startup, and continue to read the latest binlog.
     
* `Latest`:
 Never to perform a snapshot on the monitored database tables upon first startup, just read from the change since the connector was started.
 
 **Note**: the mechanism of `scan.startup.mode` option relying on Debezium’s `snapshot.mode` configuration. So please do not use them together. 
 If you specific both `scan.startup.mode` and `debezium.snapshot.mode` options in the table DDL, it may make `scan.startup.mode` doesn’t work.

     
