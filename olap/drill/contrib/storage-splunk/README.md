# Drill Connector for Splunk
This plugin enables Drill to query Splunk. 

## Configuration
To connect Drill to Splunk, create a new storage plugin with the following configuration:
```json
{
   "type":"splunk",
   "enabled": false,
   "username": "admin",
   "password": "changeme",
   "hostname": "localhost",
   "port": 8089,
   "earliestTime": "-14d",
   "latestTime": "now",
   "reconnectRetries": 3   
}
```
To successfully connect, Splunk uses port `8089` for interfaces.  This port must be open for Drill to query Splunk.

Sometimes Splunk has issue in connection to it:
https://github.com/splunk/splunk-sdk-java/issues/62 <br>
To bypass it by Drill please specify "reconnectRetries": 3. It allows you to retry the connection several times. 

## Understanding Splunk's Data Model
Splunk's primary use case is analyzing event logs with a timestamp. As such, data is indexed by the timestamp, with the most recent data being indexed first.  By default, Splunk
 will sort the data in reverse chronological order.  Large Splunk installations will put older data into buckets of hot, warm and cold storage with the "cold" storage on the
  slowest and cheapest disks.
  
With this understood, it is **very** important to put time boundaries on your Splunk queries. The Drill plugin allows you to set default values in the configuration such that every
 query you run will be bounded by these boundaries.  Alternatively, you can set the time boundaries at query time.  In either case, you will achieve the best performance when
  you are asking Splunk for the smallest amount of data possible.
  
## Understanding Drill's Data Model with Splunk
Drill treats Splunk indexes as tables. Splunk's access model does not restrict to the catalog, but does restrict access to the actual data. It is therefore possible that you can
 see the names of indexes to which you do not have access.  You can view the list of available indexes with a `SHOW TABLES IN splunk` query.
  
```
apache drill> SHOW TABLES IN splunk;
+--------------+----------------+
| TABLE_SCHEMA |   TABLE_NAME   |
+--------------+----------------+
| splunk       | summary        |
| splunk       | splunklogger   |
| splunk       | _thefishbucket |
| splunk       | _audit         |
| splunk       | _internal      |
| splunk       | _introspection |
| splunk       | main           |
| splunk       | history        |
| splunk       | _telemetry     |
+--------------+----------------+
9 rows selected (0.304 seconds)
```
To query Splunk from Drill, use the following format: 
```sql
SELECT <fields>
FROM splunk.<index>
```
  
 ## Bounding Your Queries
  When you learn to query Splunk via their interface, the first thing you learn is to bound your queries so that they are looking at the shortest time span possible. When using
   Drill to query Splunk, it is advisable to do the same thing, and Drill offers two ways to accomplish this: via the configuration and at query time.
   
  ### Bounding your Queries at Query Time
  The easiest way to bound your query is to do so at querytime via special filters in the `WHERE` clause. There are two special fields, `earliestTime` and `latestTime` which can
   be set to bound the query. If they are not set, the query will be bounded to the defaults set in the configuration.
   
   You can use any of the time formats specified in the Splunk documentation here:   
  https://docs.splunk.com/Documentation/Splunk/8.0.3/SearchReference/SearchTimeModifiers
  
  So if you wanted to see your data for the last 15 minutes, you could execute the following query:

```sql
SELECT <fields>
FROM splunk.<index>
WHERE earliestTime='-15m' AND latestTime='now'
```
The variables set in a query override the defaults from the configuration. 
  
 ## Data Types
  Splunk does not have sophisticated data types and unfortunately does not provide metadata from its query results.  With the exception of the fields below, Drill will interpret
   all fields as `VARCHAR` and hence you will have to convert them to the appropriate data type at query time.
  
  #### Timestamp Fields
  * `_indextime`
  * `_time` 
  
  #### Numeric Fields
  * `date_hour` 
  * `date_mday`
  * `date_minute`
  * `date_second` 
  * `date_year`
  * `linecount`
  
 ### Nested Data
 Splunk has two different types of nested data which roughly map to Drill's `LIST` and `MAP` data types. Unfortunately, there is no easy way to identify whether a field is a
  nested field at querytime as Splunk does not provide any metadata and therefore all fields are treated as `VARCHAR`.
  
  However, Drill does have built in functions to easily convert Splunk multifields into Drill `LIST` and `MAP` data types. For a LIST, simply use the 
  `SPLIT(<field>, ' ')` function to split the field into a `LIST`.
  
  `MAP` data types are rendered as JSON in Splunk. Fortunately JSON can easily be parsed into a Drill Map by using the `convert_fromJSON()` function.  The query below
   demonstrates how to convert a JSON column into a Drill `MAP`.
  
```sql
SELECT convert_fromJSON(_raw) 
FROM splunk.spl
WHERE spl = '| makeresults
| eval _raw="{\"pc\":{\"label\":\"PC\",\"count\":24,\"peak24\":12},\"ps3\":
{\"label\":\"PS3\",\"count\":51,\"peak24\":10},\"xbox\":
{\"label\":\"XBOX360\",\"count\":40,\"peak24\":11},\"xone\":
{\"label\":\"XBOXONE\",\"count\":105,\"peak24\":99},\"ps4\":
{\"label\":\"PS4\",\"count\":200,\"peak24\":80}}"'
```

### Selecting Fields
When you execute a query in Drill for Splunk, the fields you select are pushed down to Splunk. Therefore, it will always be more efficient to explicitly specify fields to push
 down to Splunk rather than using `SELECT *` queries.
 
 ### Special Fields
 There are several fields which can be included in a Drill query 
 
 * `spl`:  If you just want to send an SPL query to Splunk, this will do that. 
 * `earliestTime`: Overrides the `earliestTime` setting in the configuration. 
 * `latestTime`: Overrides the `latestTime` setting in the configuration. 
  
### Sorting Results
Due to the nature of Splunk indexes, data will always be returned in reverse chronological order. Thus, sorting is not necessary if that is the desired order.

## Sending Arbitrary SPL to Splunk
There is a special table called `spl` which you can use to send arbitrary queries to Splunk. If you use this table, you must include a query in the `spl` filter as shown below:
```sql
SELECT *
FROM splunk.spl
WHERE spl='<your SPL query'
```

# Testing the Plugin
This plugin includes a series of unit tests in the `src/test/` directory, however there are a few tests for which you will need an active Splunk installation to run them.  
Simply follow the instructions below to test Splunk with Drill.
 
 ###  Step 1: Get Splunk
 From Splunk's website, simply download and install the free version here: https://www.splunk.com/en_us/download/splunk-enterprise.html
 
 Once you've downloaded Splunk, start it up, and make sure everything is working properly. 
 
 ### Step 2:  Add Data
 Next, go here: https://docs.splunk.com/Documentation/Splunk/7.0.3/SearchTutorial/Systemrequirements and download the dummy datasets that Splunk provides. Once you've downloaded
  this data, have Splunk index this data and you're ready to go from the Splunk end. 
  
## Known Limitations
* At present, Drill will not interpret Splunk multifields as anything other than a String. If there is interest, this feature can be implemented.
 