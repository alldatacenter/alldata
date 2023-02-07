# Drill Apache Druid Plugin

Drill druid storage plugin allows you to perform SQL queries against Druid datasource(s).
This storage plugin is part of [Apache Drill](https://github.com/apache/drill)

### Tested with Druid version
[0.22.0](https://github.com/apache/druid/releases/tag/druid-0.22.0)

### Druid API

Druid supports multiple native queries to address sundry use-cases.
To fetch raw druid rows, druid API support two forms of query, `SELECT` (no relation to SQL) and `SCAN`.
Currently, this plugin uses the [Scan](https://druid.apache.org/docs/latest/querying/scan-query.html)
query API to fetch raw rows from druid as json.

### Filter Push-Down

Filters pushed down to native druid filter structure, converting SQL where clauses to the respective druid [Filters](https://druid.apache.org/docs/latest/querying/filters.html).

### Plugin Registration

The plugin can be registered in Apache Drill using the drill web interface by navigating to the ```storage``` page.
Following is the default registration configuration.
```json
{
  "type" : "druid",
  "brokerAddress" : "http://localhost:8082",
  "coordinatorAddress": "http://localhost:8081",
  "averageRowSizeBytes": 100,
  "enabled" : false
}
```

### Druid storage plugin developer notes.

* Building the plugin 

    `mvn install -pl contrib/storage-druid`

* Building DRILL

    `mvn clean install -DskipTests`
    
* Start Drill In Embedded Mode (mac)

    ```shell script
    distribution/target/apache-drill-1.20.0-SNAPSHOT/apache-drill-1.20.0-SNAPSHOT/bin/drill-embedded
    ```
  
* Starting Druid (Docker and Docker Compose required)
    ```
    cd contrib/storage-druid/src/test/resources/druid
    docker-compose up -d
    ```
  
  * There is an `Indexing Task Json` in the same folder as the docker compose file. It can be used to ingest the wikipedia datasource.
  
  * Make sure the druid storage plugin is enabled in Drill.

