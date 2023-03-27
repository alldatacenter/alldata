import { ConnectorProperty } from "@debezium/ui-models";

const CONNECTOR_PROPERIES = [
    {
      "category":"CONNECTION",
      "description":"Unique topic prefix that is used as a prefix for all schemas and topics. Each distinct installation should have a separate namespace and be monitored by at most one Debezium connector.",
      "displayName":"Namespace",
      "isMandatory":true,
      "name":"topic.prefix",
      "type":"STRING"
    } as ConnectorProperty,
    {
      "category": "CONNECTION_ADVANCED",
      "defaultValue": true,
      "description": "Enable or disable TCP keep-alive probe to avoid dropping TCP connection",
      "displayName": "TCP keep-alive probe",
      "isMandatory": false,
      "name": "database.tcpKeepAlive",
      "type": "BOOLEAN"
    } as ConnectorProperty,
    {
      "category":"CONNECTION_ADVANCED_SSL",
      "description":"File containing the SSL Certificate for the client. See the Postgres SSL docs for further information",
      "displayName":"SSL Client Certificate",
      "isMandatory":false,
      "name":"database.sslcert",
      "type":"STRING"
    } as ConnectorProperty,
    {
      "allowedValues":["pgoutput","decoderbufs","wal2json_streaming","wal2json_rds_streaming","wal2json","wal2json_rds"],
      "category":"CONNECTION_ADVANCED_REPLICATION",
      "defaultValue":"decoderbufs",
      "description":"The name of the Postgres logical decoding plugin installed on the server. Supported values are 'decoderbufs', 'wal2json', 'pgoutput', 'wal2json_streaming', 'wal2json_rds' and 'wal2json_rds_streaming'. Defaults to 'decoderbufs'.",
      "displayName":"Plugin",
      "isMandatory":false,
      "name":"plugin.name",
      "type":"STRING"
    } as ConnectorProperty,
    {
      "allowedValues": [
        "disabled",
        "all_tables",
        "filtered"
      ],
      "category": "CONNECTION_ADVANCED_PUBLICATION",
      "defaultValue": "all_tables",
      "description": "Applies only when streaming changes using pgoutput.Determine how creation of a publication should work, the default is all_tables.DISABLED - The connector will not attempt to create a publication at all. The expectation is that the user has created the publication up-front. If the publication isn't found to exist upon startup, the connector will throw an exception and stop.ALL_TABLES - If no publication exists, the connector will create a new publication for all tables. Note this requires that the configured user has access. If the publication already exists, it will be used. i.e CREATE PUBLICATION <publication_name> FOR ALL TABLES;FILTERED - If no publication exists, the connector will create a new publication for all those tables matchingthe current filter configuration (see table/database include/exclude list properties). If the publication already exists, it will be used. i.e CREATE PUBLICATION <publication_name> FOR TABLE <tbl1, tbl2, etc>",
      "displayName": "Publication Auto Create Mode",
      "isMandatory": false,
      "name": "publication.autocreate.mode",
      "type": "STRING"
    } as ConnectorProperty,
    {
      "category": "FILTERS",
      "description": "The tables for which changes are to be captured",
      "displayName": "Include Tables",
      "isMandatory": false,
      "name": "table.include.list",
      "type": "LIST"
    } as ConnectorProperty,
    {
      "allowedValues": [
        "precise",
        "string",
        "double"
      ],
      "category": "CONNECTOR",
      "defaultValue": "precise",
      "description": "Specify how DECIMAL and NUMERIC columns should be represented in change events, including:'precise' (the default) uses java.math.BigDecimal to represent values, which are encoded in the change events using a binary representation and Kafka Connect's 'org.apache.kafka.connect.data.Decimal' type; 'string' uses string to represent values; 'double' represents values using Java's 'double', which may not offer the precision but will be far easier to use in consumers.",
      "displayName": "Decimal Handling",
      "isMandatory": false,
      "name": "decimal.handling.mode",
      "type": "STRING"
    } as ConnectorProperty,
    {
      "category": "CONNECTOR_ADVANCED",
      "defaultValue": "__debezium_unavailable_value",
      "description": "Specify the constant that will be provided by Debezium to indicate that the original value is a toasted value not provided by the database. If starts with 'hex:' prefix it is expected that the rest of the string repesents hexadecimally encoded octets.",
      "displayName": "Toasted value placeholder",
      "isMandatory": false,
      "name": "toasted.value.placeholder",
      "type": "STRING"
    } as ConnectorProperty,
    {
      "category": "CONNECTOR_SNAPSHOT",
      "description": "The maximum number of records that should be loaded into memory while performing a snapshot",
      "displayName": "Snapshot fetch size",
      "isMandatory": false,
      "name": "snapshot.fetch.size",
      "type": "INT"
    } as ConnectorProperty,
    {
      "category": "ADVANCED",
      "defaultValue": 2048,
      "description": "Maximum size of each batch of source records. Defaults to 2048.",
      "displayName": "Change event batch size",
      "isMandatory": false,
      "name": "max.batch.size",
      "type": "INT"
    } as ConnectorProperty,
    {
      "category": "ADVANCED_HEARTBEAT",
      "defaultValue": 0,
      "description": "Length of an interval in milli-seconds in in which the connector periodically sends heartbeat messages to a heartbeat topic. Use 0 to disable heartbeat messages. Disabled by default.",
      "displayName": "Connector heartbeat interval (milli-seconds)",
      "isMandatory": false,
      "name": "heartbeat.interval.ms",
      "type": "INT"
    } as ConnectorProperty
];

export default CONNECTOR_PROPERIES;
  