# Generic REST API Storage Plugin

The HTTP storage plugin lets you query APIs over HTTP/REST. The plugin
expects JSON responses.

The HTTP plugin is new in Drill 1.18 and is an Alpha feature. It works well, and we
encourage you to use it and provide feedback. However, we reserve the right to change
the plugin based on that feedback.

## Configuration

To configure the plugin, create a new storage plugin, and add the following configuration options which apply to ALL connections defined in this plugin:

```json
{
  "type": "http",
  "cacheResults": true,
  "connections": {},
  "timeout": 0,
  "proxyHost": null,
  "proxyPort": 0,
  "proxyType": null,
  "proxyUsername": null,
  "proxyPassword": null,
  "enabled": true
}
```
The required options are:

* `type`:  This should be `http`
* `cacheResults`:  Enable caching of the HTTP responses.  Defaults to `false`
* `timeout`:  Sets the response timeout in seconds. Defaults to `0` which is no timeout.
* `connections`:  This field contains the details for individual connections. See the section *Configuring API Connections for Details*.

You can configure Drill to work behind a corporate proxy. Details are listed below.

### Configuring the API Connections

The HTTP Storage plugin allows you to configure multiple APIS which you can query directly from this plugin. To do so, first add a `connections` parameter to the configuration
. Next give the connection a name, which will be used in queries.  For instance `stockAPI` or `jira`.

The `connection` property can accept the following options.

#### URL

`url`: The base URL which Drill will query.

##### Parameters in the URL
Many APIs require parameters to be passed directly in the URL instead of as query arguments.  For example, github's API allows you to query an organization's repositories with the following
URL:  https://github.com/orgs/{org}/repos

As of Drill 1.20.0, you can simply set the URL in the connection using the curly braces.  If your API includes URL parameters you must include them in the `WHERE` clause in your 
query, or specify a default value in the configuration.

As an example, the API above, you would have to query as shown below:

```sql
SELECT * 
FROM api.github
WHERE org = 'apache'
```

This query would replace the `org`in the URL with the value from the `WHERE` clause, in this case `apache`.  You can specify a default value as follows:  `https://someapi.com/
{param1}/{param2=default}`.  In this case, the default would be used if and only if there isn't a parameter supplied in the query. 

#### Limitations on URL Parameters
* Drill does not support boolean expressions of URL parameters in queries.  For instance, for the above example, if you were to include `WHERE org='apache' OR org='linux'`, 
  these parameters could not be pushed down in the current state. 
* All URL parameter clauses must be equality only.

### Passing Parameters in the Query
`requireTail`: Set to `true` if the query must contain an additional part of the service
URL as a table name, `false` if the URL needs no additional suffix other than that
provided by `WHERE` clause filters. (See below.)


If your service requires parameters, you have three choices. Suppose your connection is called
`sunrise`. First, can include them directly in your URL if the parameters a fixed for a given
service:

```json
url: "https://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=2019-10-02",
requireTail: false
```

Query your table like this:

```sql
SELECT * FROM api.sunrise;
```

Second, you can specify the base URL here and the full URL in your query. Use this form if the
parameters define a table-like concept (the set of data to return):

```json
url: "https://api.sunrise-sunset.org/json",
requireTail: true
```

SQL query:

```sql
SELECT * FROM api.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`
```

If the URL requires a tail, specify it as if it were a table name. (See example
below.) Drill directly appends the "tail" to the base URL to create the final URL.

Third, you can use the `params` field below to specify the parameters as filters
if the parameters specify which data sets to return:

```json
url: "https://api.sunrise-sunset.org/json",
requireTail: false,
params: ["lat", "lng", "date"]
```

SQL query:

```sql
SELECT * FROM api.sunrise
WHERE `lat` = 36.7201600 AND `lng` = -4.4203400 AND `date` = '2019-10-02'
```

In this case, Drill appends the parameters to the URL, adding a question mark
to separate the two.

#### Method

`method`: The request method. Must be `GET` or `POST`. Other methods are not allowed and will default to `GET`.

`postBody`: Contains data, in the form of key value pairs, which are sent during a `POST` request.
The post body should be in the of a block of text with key/value pairs:

```json
postBody: "key1=value1
key2=value2"
```

#### Headers

`headers`: Often APIs will require custom headers as part of the authentication. This field allows
you to define key/value pairs which are submitted with the http request. The format is:

```json
headers: {
   "key1": "Value1",
   "key2": "Value2"
   }
```

#### Query Parmeters as Filters

* `params`: Allows you to map SQL `WHERE` clause conditions to query parameters.

```json
url: "https://api.sunrise-sunset.org/json",
params: ["lat", "lng", "date"]
```

SQL query:

```sql
SELECT * FROM api.sunrise
WHERE `lat` = 36.7201600 AND `lng` = -4.4203400 AND `date` = '2019-10-02'
```

HTTP parameters are untyped; Drill converts any value you provide into a string.
Drill allows you to use any data type which can convert unambiguously to a string:
`BIT`, `INT`, `BIGINT`, `FLOAT4`, `FLOAT8`, `VARDECIMAL`, `VARCHAR`. The `BIT` type
is translated to `true` and `false`. Note that none of the date or interval types
are allowed: each of those requires formatting.

Note the need for back-tick quotes around the names;
`date` is a reserved word. Notice also that the date is a string because of
the formatting limitation mentioned above.

Only equality conditions can be translated to parameters. The above filters are
translated to:

```
lat=36.7201600&lng=-4.4203400&date=2019-10-02
```

If your query contains other conditions (`!=`, `<`, etc.) then those conditions are applied
in Drill after the REST service returns the data.

Only fields listed in the `params` config filed will become parameters, all other
expressions are handled within Drill as explained above.

At present, Drill requires the values to be literals (constants). Drill does not
currently allow expressions. That is, the following will not become an HTTP parameter:

```sql
WHERE `lat` = 36 + 0.7201600
```

Drill will add parameters to the URL in the order listed in the config. Use this
feature if the API is strict about parameter ordering.

At present Drill does not enforce that parameters are provided in the query: Drill
assumes parameters are optional.

### Data Path

REST responses often have structure beyond the data you want to query. For example:

```json
 "results":
 {
   "sunrise":"7:27:02 AM",
   "sunset":"5:05:55 PM",
   "solar_noon":"12:16:28 PM",
   "day_length":"9:38:53",
   "civil_twilight_begin":"6:58:14 AM",
   "civil_twilight_end":"5:34:43 PM",
   "nautical_twilight_begin":"6:25:47 AM",
   "nautical_twilight_end":"6:07:10 PM",
   "astronomical_twilight_begin":"5:54:14 AM",
   "astronomical_twilight_end":"6:38:43 PM"
 },
  "status":"OK"
}
```

Drill can handle JSON structures such as the above; you can use SQL to obtain the
results you want. However, the SQL will be simpler if we skip over the portions we
don't want and simply read the `results` fields as our SQL fields. We do that with
the `dataPath` configuration:

```json
dataPath: "results"
```

The `dataPath` can contain any number of fields, for example: `"response/content/data"`.
Drill will ignore all JSON content outside of the data path.

At present, there is no provision to check the `status` code in a response such
as that shown above. Drill assumes that the server will uses HTTP status codes to
indicate a bad request or other error.

#### Input Type
The REST plugin accepts three different types of input: `json`, `csv` and `xml`.  The default is `json`.  If you are using `XML` as a data type, there is an additional 
configuration option called `xmlDataLevel` which reduces the level of unneeded nesting found in XML files.  You can find more information in the documentation for Drill's XML 
format plugin. 

#### JSON Configuration
Drill has a collection of JSON configuration options to allow you to configure how Drill interprets JSON files.  These are set at the global level, however the HTTP plugin 
allows you to configure these options individually per connection and override the Drill defaults.  The options are:

* `allowNanInf`:  Configures the connection to interpret `NaN` and `Inf` values
* `allTextMode`:  By default, Drill attempts to infer data types from JSON data. If the data is malformed, Drill may throw schema change exceptions. If your data is 
  inconsistent, you can enable `allTextMode` which when true, Drill will read all JSON values as strings, rather than try to infer the data type. 
* `readNumbersAsDouble`:  By default Drill will attempt to interpret integers, floating point number types and strings.  One challenge is when data is consistent, Drill may 
  throw schema change exceptions. In addition to `allTextMode`, you can make Drill less sensitive by setting the `readNumbersAsDouble` to `true` which causes Drill to read all 
  numeric fields in JSON data as `double` data type rather than trying to distinguish between ints and doubles.
* `enableEscapeAnyChar`:  Allows a user to escape any character with a \

All of these can be set by adding the `jsonOptions` to your connection configuration as shown below:

```json

"jsonOptions": {
  "allTextMode": true, 
  "readNumbersAsDouble": true
}

```

#### Authorization

`authType`: If your API requires authentication, specify the authentication
type. Defaults to `none`.
If the `authType` is set to `basic`, `username` and `password` must be set in the configuration as well.

`username`: The username for basic authentication.

`password`: The password for basic authentication.

#### Limiting Results
Some APIs support a query parameter which is used to limit the number of results returned by the API.  In this case you can set the `limitQueryParam` config variable to the query parameter name and Drill will automatically include this in your query.  For instance, if you have an API which supports a limit query parameter called `maxRecords` and you set the abovementioned config variable then execute the following query:
  
```sql
SELECT <fields>
FROM api.limitedApi
LIMIT 10 
```  
Drill will send the following request to your API:
```
https://<api>?maxRecords=10
```

### OAuth2.0
If the API which you are querying requires OAuth2.0 for authentication [read the documentation for configuring Drill to use OAuth2.0](OAuth.md).

### Pagination
If you want to use automatic pagination in Drill, [click here to read the documentation for pagination](Pagination.md).

#### errorOn400
When a user makes HTTP calls, the response code will be from 100-599.  400 series error codes can contain useful information and in some cases you would not want Drill to throw 
errors on 400 series errors.  This option allows you to define Drill's behavior on 400 series error codes.  When set to `true`, Drill will throw an exception and halt execution 
on 400 series errors, `false` will return an empty result set (with implicit fields populated).

#### verifySSLCert
Default is `true`, but when set to false, Drill will trust all SSL certificates.  Useful for debugging or on internal corporate networks using self-signed certificates or 
private certificate authorities.

#### caseSensitiveFilters
Some APIs are case sensitive with the fields which are pushed down.  If the endpoint that you are working with is in fact case sensitive, simply set this to `true`.  Defaults to `false`.

## Usage

This plugin is different from other plugins in that it the table component of the `FROM` clause
is different. In normal Drill queries, the `FROM` clause is constructed as follows:

```sql
FROM <storage plugin>.<schema>.<table>
```
For example, you might have:

```sql
FROM dfs.test.`somefile.csv`

-- or

FROM mongo.stats.sales_data
```

The HTTP/REST plugin the `FROM` clause enables you to pass arguments to your REST call if you
set the `requireTail` property to `true`. The structure is:

```sql
FROM <plugin>.<connection>.<arguments>
--Actual example:
FROM http.sunrise.`/json?lat=36.7201600&lng=-4.4203400&date=today`
```

Or, as explained above, you can have the URL act like a table and pass parameters
using a `WHERE` clause "filter" conditions.

## Proxy Setup

Some users access HTTP services from behind a proxy firewall. Drill provides three ways specify proxy
configuration.

### Proxy Environment Variables

Drill recognizes the usual Linux proxy environment variables:

* `http_proxy`, `HTTP_PROXY`
* `https_proxy`, `HTTP_PROXY`
* `all_proxy`, `ALL_PROXY`

This technique works well if your system is already configured to
handle proxies.

### Boot Configuration

You can also specify proxy configuration in the `drill-override.conf` file.
See `drill-override-example.conf` for a template. Use the boot configuration
is an attribute of your network environment. Doing so will ensure every
Drillbit and every HTTP/HTTPS request uses the same proxy configuration.

First, you can use the same form of URL you would use with the environment
variables:

```
drill.exec.net_proxy.http_url: "http://foo.com/1234"
```

There is one setting for HTTP, another for HTTPS.

Alternatively, you can specify each field separately:

```
drill.exec.net_proxy.http: {
      type: "none", # none, http, socks. Blank same as none.
      host: "",
      port: 80,
      user_name: "",
      password: ""
    },
```

The valid proxy types are `none`, `http` and `socks`. Blank is the same
as `none`.

Again, there is a parallel section for HTTPS.

### In the HTTP Storage Plugin Config

The final way to configure proxy is in the HTTP storage plugin itself. The proxy
applies to all connections defined in that plugin. Use this approach if the proxy
applies only to some external services, or if each service has a different proxy
(defined by creating a separate plugin config for each service.)

```json
      proxy_type: "direct",
      proxy_host: "",
      proxy_port: 80,
      proxy_user_name: "",
      proxy_password: ""
```

The valid proxy types are `direct`, `http` or `socks`. Blank is the same
as `direct`.

## Examples

### Example 1: Reference Data, A Sunrise/Sunset API

The API sunrise-sunset.org returns data in the following format:

 ```json
 "results":
 {
   "sunrise":"7:27:02 AM",
   "sunset":"5:05:55 PM",
   "solar_noon":"12:16:28 PM",
   "day_length":"9:38:53",
   "civil_twilight_begin":"6:58:14 AM",
   "civil_twilight_end":"5:34:43 PM",
   "nautical_twilight_begin":"6:25:47 AM",
   "nautical_twilight_end":"6:07:10 PM",
   "astronomical_twilight_begin":"5:54:14 AM",
   "astronomical_twilight_end":"6:38:43 PM"
 },
  "status":"OK"
}
```

To query this API, set the configuration as follows:

```json
{
  "type": "http",
  "cacheResults": false,
  "enabled": true,
  "timeout": 5,
  "connections": {
    "sunrise": {
      "url": "https://api.sunrise-sunset.org/json",
      "requireTail": true,
      "method": "GET",
      "headers": null,
      "authType": "none",
      "userName": null,
      "password": null,
      "postBody": null, 
      "inputType": "json",
       "errorOn400": true
    }
  }

```

Then, to execute a query:

```sql
SELECT api_results.results.sunrise AS sunrise,
       api_results.results.sunset AS sunset
FROM   http.sunrise.`?lat=36.7201600&lng=-4.4203400&date=today` AS api_results;
```
Which yields the following results:
```
+------------+------------+
|  sunrise   |   sunset   |
+------------+------------+
| 7:17:46 AM | 5:01:33 PM |
+------------+------------+
1 row selected (0.632 seconds)
```

#### Using Parameters

We can improvide the above configuration to use `WHERE` clause filters and
a `dataPath` to skip over the unwanted parts of the message
body. Set the configuration as follows:

```json
{
  "type": "http",
  "cacheResults": false,
  "enabled": true,
  "timeout": 5,
  "connections": {
    "sunrise": {
      "url": "https://api.sunrise-sunset.org/json",
      "requireTail": false,
      "method": "GET",
      "dataPath": "results",
      "headers": null,
      "params": [ "lat", "lng", "date" ],
      "authType": "none",
      "userName": null,
      "password": null,
      "postBody": null, 
       "errorOn400": true
    }
  }

```
Then, to execute a query:

```sql
SELECT sunrise, sunset
FROM   http.sunrise
WHERE  `lat` = 36.7201600 AND `lng` = -4.4203400 AND `date` = 'today'
```

Which yields the same results as before.

### Example 2: JIRA

JIRA Cloud has a REST API which is
[documented here](https://developer.atlassian.com/cloud/jira/platform/rest/v3/?utm_source=%2Fcloud%2Fjira%2Fplatform%2Frest%2F&utm_medium=302).

To connect Drill to JIRA Cloud, use the following configuration:

```json
{
  "type": "http",
  "cacheResults": false,
  "timeout": 5,
  "connections": {
    "jira": {
      "url": "https://<project>.atlassian.net/rest/api/3/",
      "method": "GET",
      "dataPath": "issues",
      "headers": {
        "Accept": "application/json"
      },
      "authType": "basic",
      "userName": "<username>",
      "password": "<API Key>",
      "postBody": null
    }
  },
  "enabled": true
}
```

Once you've configured Drill to query the API, you can now easily access any of your data in JIRA.
The JIRA API returns highly nested data, however with a little preparation, it
is pretty straightforward to transform it into a more useful table. For instance, the
query below:

```sql
SELECT key,
       t.fields.issueType.name AS issueType,
       SUBSTR(t.fields.created, 1, 10) AS created,
       SUBSTR(t.fields.updated, 1, 10) AS updated,
       t.fields.assignee.displayName as assignee,
       t.fields.creator.displayName as creator,
       t.fields.summary AS summary,
       t.fields.status.name AS currentStatus,
       t.fields.priority.name AS priority,
       t.fields.labels AS labels,
       t.fields.subtasks AS subtasks
FROM http.jira.`search?jql=project%20%3D%20<project>&&maxResults=100 AS t`
```

The query below counts the number of issues by priority:

```sql
SELECT t.fields.priority.name AS priority,
       COUNT(*) AS issue_count
FROM http.jira.`search?jql=project%20%3D%20<project>&&maxResults=100` AS t
GROUP BY priority
ORDER BY issue_count DESC
```

<img src="images/issue_count.png"  alt="Issue Count by Priority"/>


## Limitations

1. The plugin is supposed to follow redirects, however if you are using authentication,
   you may encounter errors or empty responses if you are counting on the endpoint for
   redirection.

~~2. At this time, the plugin does not support any authentication other than basic authentication.~~

3. This plugin does not implement join filter pushdowns (only constant pushdowns are
   supported). Join pushdown has the potential to improve performance if you use the HTTP service
   joined to another table.

~~4. This plugin only reads JSON and CSV responses.~~

5. `POST` bodies can only be in the format of key/value pairs. Some APIs accept
    JSON based `POST` bodies but this is not currently supported.

6. When using `dataPath`, the returned message should a single JSON object. The field
   pointed to by the `dataPath` should contain a single JSON object or an array of objects.

7. When not using `dataPath`, the response should be a single JSON object, an array of
   JSON objects, or a series of line-delimited JSON objects (the so-called
   [jsonlines](http://jsonlines.org/) format.)

8. Parameters are considered optional; no error will be given if a query omits
   parameters. An enhancement would be to mark parameters as required: all are required
   or just some. If parameters are required, but omitted, the report service will
   likely return an error.

## Troubleshooting

If anything goes wrong, Drill will provide a detailed error message, including URL:

```
DATA_READ ERROR: Failed to read the HTTP response body

Error message: Read timed out
Connection: sunrise
Plugin: api
URL: https://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=today
Fragment: 0:0
```

If using a "tail" in the query, verify that the tail is quoted using back-ticks
as shown in the examples.

Check that the URL is correct. If not, check the plugin configuration properties
described above to find out why the pieces were assembled as you want.

If the query works but delivers unexpected results, check the Drill log file.
Drill logs a message like the following at the info level when opening the HTTP connection:

```
Connection: sunrise, Method: GET,
  URL: https://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=today
```

If the query runs, but produces odd results, try a simple `SELECT *` query. This may reveal
if there is unexpected message context in addition to the data. Use the `dataPath` property
to ignore the extra content.

## Implicit Fields
The HTTP plugin includes four implicit fields which can be used for debugging.  These fields do not appear in star queries.  They are:

* `_response_code`: The response code from the HTTP request.  This field is an `INT`.
* `_response_message`:  The response message.
* `_response_protocol`:  The response protocol.
* `_response_url`:  The actual URL sent to the API. 
