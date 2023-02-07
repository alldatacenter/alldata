# drill--opentsdb-storage

Implementation of TSDB storage plugin. Plugin uses REST API to work with TSDB. 

For more information about openTSDB follow this link <http://opentsdb.net>

There is list of required params:

* metric     - The name of a metric stored in the db.

* start      - The start time for the query. This can be a relative or absolute timestamp.

* aggregator - The name of an aggregation function to use.

optional param is: 

* downsample - An optional downsampling function to reduce the amount of data returned.

* end - An end time for the query. If not supplied, the TSD will assume the local system time on the server. 
This may be a relative or absolute timestamp. This param is optional, and if it isn't specified we will send null
to the db in this field, but in this case db will assume the local system time on the server.

List of supported aggregators

<http://opentsdb.net/docs/build/html/user_guide/query/aggregators.html>

List of supported time 

<http://opentsdb.net/docs/build/html/user_guide/query/dates.html>

Params must be specified in FROM clause of the query separated by commas. For example

`opentsdb.(metric=metric_name, start=4d-ago, aggregator=sum)`

Supported queries for now are listed below:

```
USE opentsdb
```

```
SHOW tables
```
Will print available metrics. Max number of the printed results is a Integer.MAX value

```
SELECT * FROM opentsdb. `(metric=warp.speed.test, start=47y-ago, aggregator=sum)` 
```
Return aggregated elements from `warp.speed.test` table since 47y-ago 

```
SELECT * FROM opentsdb.`(metric=warp.speed.test, aggregator=avg, start=47y-ago)`
```
Return aggregated elements from `warp.speed.test` table

```
SELECT `timestamp`, sum(`aggregated value`) FROM opentsdb.`(metric=warp.speed.test, aggregator=avg, start=47y-ago)` GROUP BY `timestamp`
```
Return aggregated and grouped value by standard drill functions from `warp.speed.test table`, but with the custom aggregator

```
SELECT * FROM opentsdb.`(metric=warp.speed.test, aggregator=avg, start=47y-ago, downsample=5m-avg)`
```
Return aggregated data limited by downsample

```
SELECT * FROM opentsdb.`(metric=warp.speed.test, aggregator=avg, start=47y-ago, end=1407165403000)`
```
Return aggregated data limited by end time