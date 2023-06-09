# Web Server Log Format Plugin (HTTPD)
This plugin enables Drill to read and query httpd (Apache Web Server) and nginx access logs natively. This plugin uses the work by [Niels Basjes](https://github.com/nielsbasjes
) which is available here: https://github.com/nielsbasjes/logparser.

## Configuration
There are several fields which you can specify in order for Drill to read web server logs. In general the defaults should be fine, however the fields are:
* **`logFormat`**:  The log format string is the format string found in your web server configuration. If you have multiple logFormats then you can add all of them in this
 single parameter separated by a newline (`\n`). The parser will automatically select the first matching format.
 Note that the well known formats `common`, `combined`, `combinedio`, `referer` and `agent` are also accepted as logFormat.
 Be aware of leading and trailing spaces on a line when configuring this!
* **`timestampFormat`**:  The format of time stamps in your log files. This setting is optional and is almost never needed.
* **`extensions`**:  The file extension of your web server logs.  Defaults to `httpd`.
* **`maxErrors`**:  Sets the plugin error tolerance. When set to any value less than `0`, Drill will ignore all errors. If unspecified then maxErrors is 0 which will cause the query to fail on the first error.
* **`flattenWildcards`**: There are a few variables which Drill extracts into maps.  Defaults to `false`.
* **`parseUserAgent`**: When set to true the [Yauaa useragent analyzer](https://yauaa.basjes.nl) will be applied to the UserAgent field if present. Defaults to `false` because of the extra startup and memory overhead.
* **`logParserRemapping`**: This makes it possible to parse deeper into the logline in custom situations. See documentation below for further info.

In common situations the config will look something like this (having two logformats with a newline `\n` separated):
```json
"httpd" : {
  "type" : "httpd",
  "logFormat" : "%h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-agent}i\" %V\ncombined",
  "maxErrors" : 0,
  "flattenWildcards" : true,
  "parseUserAgent" : true
}
```

## Data Model
The fields which Drill will return from HTTPD access logs should be fairly self explanatory and should all be mapped to correct data types.  For instance, `TIMESTAMP` fields are
 all Drill `TIMESTAMPS` and so forth.

### Nested Columns
The HTTPD parser can produce a few columns of nested data. For instance, the various `query_string` columns are parsed into Drill maps so that if you want to look for a specific
 field, you can do so.

 Drill allows you to directly access maps in with the format of:
 ```
<table>.<map>.<field>
```
 One note is that in order to access a map, you must assign an alias to your table as shown below:
 ```sql
SELECT mylogs.`request_firstline_uri_query_$`.`username` AS username
FROM dfs.test.`logfile.httpd` AS mylogs

```
In this example, we assign an alias of `mylogs` to the table, the column name is `request_firstline_uri_query_$` and then the individual field within that mapping is `username
`.  This particular example enables you to analyze items in query strings.

### Flattening Maps
In the event that you have a map field that you would like broken into columns rather than getting the nested fields, you can set the `flattenWildcards` option to `true` and
Drill will create columns for these fields.  For example if you have a URI Query option called `username`.  If you selected the `flattedWildcards` option, Drill will create a
field called `request_firstline_uri_query_username`.

** Note that underscores in the field name are replaced with double underscores **

## Useful Functions
 If you are using Drill to analyze web access logs, there are a few other useful functions which you should know about:

 * `parse_url(<url>)`: This function accepts a URL as an argument and returns a map of the URL's protocol, authority, host, and path.
 * `parse_query(<query_string>)`: This function accepts a query string and returns a key/value pairing of the variables submitted in the request.
 * `parse_user_agent(<user agent>)`, `parse_user_agent( <useragent field>, <desired field> )`: The function parse_user_agent() takes a user agent string as an argument and
  returns a map of the available fields. Note that not every field will be present in every user agent string.
  [Complete Docs Here](https://github.com/apache/drill/tree/master/contrib/udfs#user-agent-functions)

## LogParser type remapping
**Advanced feature**
The underlying [logparser](https://github.com/nielsbasjes/logparser) supports something called type remapping.
Essentially it means that an extracted value which would normally be treated as an unparsable STRING can now be 'cast' to something
that can be further cut into relevant pieces.

The parameter string is a `;` separated list of mappings.
Each mapping is a `:` separated list of
- the name of the underlying logparser field (which is different from th Drill column name),
- the underlying `type` which is used to determine which additional Dissectors can be applied.
- optionally the `cast` (one of `STRING`, `LONG`, `DOUBLE`) which may impact the type of the Drill column

Examples:
- If you have a query parameter in the URL called `ua` which is really the UserAgent string and you would like to parse this you can add
`request.firstline.uri.query.ua:HTTP.USERAGENT`
- If you have a query parameter in the URL called `timestamp` which is really the numerical timestamp (epoch milliseconds).
The additional "LONG" will cause the returned value be a long which tells Drill the `TIME.EPOCH` is to be interpreted as a `TIMESTAMP` column.
`request.firstline.uri.query.timestamp:TIME.EPOCH:LONG`

Combining all of this can make a query that does something like this:
```sql
SELECT
          `request_receive_time_epoch`
        , `request_user-agent`
        , `request_user-agent_device__name`
        , `request_user-agent_agent__name__version__major`
        , `request_firstline_uri_query_timestamp`
        , `request_firstline_uri_query_ua`
        , `request_firstline_uri_query_ua_device__name`
        , `request_firstline_uri_query_ua_agent__name__version__major`
FROM       table(
             cp.`httpd/typeremap.log`
                 (
                   type => 'httpd',
                   logFormat => 'combined\n%h %l %u %t \"%r\" %>s %b',
                   flattenWildcards => true,
                   parseUserAgent => true,
                   logParserRemapping => '
                       request.firstline.uri.query.ua        :HTTP.USERAGENT;
                       request.firstline.uri.query.timestamp :TIME.EPOCH    : LONG'
                 )
           )
```

## Implicit Columns
Data queried by this plugin will return two implicit columns:

* **`_raw`**: This returns the raw, unparsed log line
* **`_matched`**:  Returns `true` or `false` depending on whether the line matched the config string.

Thus, if you wanted to see which lines in your log file were not matching the config, you could use the following query:

```sql
SELECT _raw
FROM <data>
WHERE _matched = false
```