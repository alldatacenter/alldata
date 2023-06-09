# Drill Regex/Logfile Plugin

Plugin for Apache Drill that allows Drill to read and query arbitrary files
where the schema can be defined by a regex. The original intent was for this
to be used for log files, however, it can be used for any structured data.

The plugin provides multiple ways to define the regex:

* In the format plugin configuration
* Via Drill's Provided Schema feature
* Via a Table Function

The plugin provides multiple ways to associate a schema with your regex:

* In the format plugin configuration
* Via Drill's Provided Schema feature
* Implicit schema: columns are returned as a `columns` array
(Handy when using table functions to provide the regex.)

## Example Use Case: MySQL Log

If you wanted to analyze log files such as the MySQL log sample shown below
using Drill, it may be possible using various string functions, or you could
write a UDF specific to this data however, this is time consuming, difficult
and not reusable.

```
070823 21:00:32       1 Connect     root@localhost on test1
070823 21:00:48       1 Query       show tables
070823 21:00:56       1 Query       select * from category
070917 16:29:01      21 Query       select * from location
070917 16:29:12      21 Query       select * from location where id = 1 LIMIT 1
```

Using this plugin, you can configure Drill to directly query log files of
any configuration.

## Configuration Options

* **type**: Must be `logRegex`.
* **regex**: The regular expression which defines how the log file
lines will be split. You must enclose the parts of the regex in grouping
parentheses that you wish to extract. Note that this plugin uses Java regular
expressions and requires that shortcuts such as `\d` have an additional slash:
ie `\\d`. Required.
* **extension**: File extensions to be
mapped to this configuration. Note that you can have multiple configurations
of this plugin to allow you to query various log files. Required.
* **maxErrors**: Log files can be inconsistent and messy. Specifies the
number of errors the reader will ignore before
halting execution with an error. Defaults to 10.
* **schema**: Defines the structure of
the log file. Optional. If you do not define a schema, all
fields will be assigned a column name of `field_n` where `n` is the index
of the field. The undefined fields will be assigned a default data type of
`VARCHAR`. You typically should define a schema column for each group of
your regex.

### Defining a Schema

The easiest way to use the plugin is simply to define the regex only. In this case,
the plugin, like the text plugin, will place all matching fields into a single
column: the `columns` array column.

You can also define a schema consisting of column names and optionally column
types.

The schema variable is an JSON array of fields which have three attributes:

* **fieldName**: Field name. Must be a valid Drill field name and must be
unique. Required.
* **fieldType**: Field data type. Defaults to `VARCHAR` if undefined.
The reader supports: `VARCHAR`, `INT`, `SMALLINT`,
`BIGINT`, `FLOAT4`, `FLOAT8`, `DATE`, `TIMESTAMP`, `TIME`. (See note
below about using `CREATE SCHEMA` as an alternative way to specify types.)
* **format**: Format for date/time fields. Defaults to ISO format.

If you provide at least one field, but fewer than the number of regex
groups, then Drill will extract
all fields and give them the name `field_n`. The fields are indexed from
`0`. Therefore if you have a dataset with 5 fields and you have named the
first two `first` and `second`, then the following query would
be valid:

```
SELECT first, second, field_2, field_3, field_4
FROM ..
```

### Example Configuration

The configuration below demonstrates how to configure Drill to query the example
MySQL log file shown above.

```
"log" : {
      "type" : "logRegex",
      "extension" : "log",
      "regex" : "(\\d{6})\\s(\\d{2}:\\d{2}:\\d{2})\\s+(\\d+)\\s(\\w+)\\s+(.+)",
      "maxErrors": 10,
      "schema": [
        {
          "fieldName": "eventDate",
          "fieldType": "DATE",
          "format": "yyMMdd"
        },
        {
          "fieldName": "eventTime",
          "fieldType": "TIME",
          "format": "HH:mm:ss"
        },
        {
          "fieldName": "PID",
          "fieldType": "INT"
        },
        {
          "fieldName": "action"
        },
        {
          "fieldName": "query"
        }
      ]
   }
 ```

## Example Usage

This format plugin gives you two options for querying fields. If you define
the fields, you can query them as you would any other data source.:

```
SELECT eventDate, eventTime, PID
FROM ..
```

If you define a regex, but no field schema, then use either the wildcard
or `columns`:

```
SELECT * FROM ...
SELECT columns FROM ...
```

Without a schema, you can select specific columns as in the text plugin:
specify array indexes:

```
SELECT columns[0], columns[2] FROM ...
```

## Implicit Fields

In addition to the fields which the user defines, the format plugin has two
implicit fields which can be useful for debugging your regex. These fields
do not appear in wildcard (`SELECT *`) queries. They are included only via
an explicit projection: `SELECT _raw, _unmatched_rows, ...`.

* **_raw**: Returns the input line matched by your regex.
* **_unmatched_rows**: Returns input lines which *did not* match the regex.
For example, if you have
a data file of 10 lines, 8 of which match, `SELECT _unmatched_rows` will return
two rows. If however, you combine this with another field, such as `_raw`, the
`_unmatched_rows` will be `null` when the rows match and have a value when it
does not.

This plugin also supports Drill's standard implicit file and partition columns.

## Provided Schema

Drill 1.16 introduced the `CREATE SCHEMA` command to allow you to define the
schema for your table. This plugin was created earlier. Here is how the two schema
systems interact.

### Plugin Config Provides Regex and Field Names

The first way to use the provided schema is just to define column types.
In this use case, the plugin config provides the physical layout (pattern
and column names), the provided schema provides data types and default
values (for missing columns.)

In this case:

* The plugin config must provide the regex.
* The plugin config provides the list of column names. (If not provided,
the names will be `field_1`, `field_2`, etc.)
* The plugin config should not provide column types.
* The table provides a schema via `CREATE SCHEMA`. Column names
in the schema must match those in the plugin config by name. The types in the
provided schema are used instead of those specified in the plugin config. The schema
allows you to specify the data type, and either nullable or `not null`
cardinality.

### Including the Regex in the Provided Schema

Another way to use the provided schema is to define an empty plugin config; don't
even provide the regex. Use table properties to define the regex (and the maximum
error count, if desired.)

In this case:

* Set the table property `drill.logRegex.regex` to the desired pattern.
* Optionally set the table property `drill.logRegex.maxErrors` to the maximum
error count.
* Define columns names and types via the provided schema. Columns must
appear in the same order as the groups in the regex (they are matched by
position.)
* If you have more groups than columns, Drill will fill in the missing
columns as `field_n`, of type `VARCHAR` as described above.

Example `CREATE SCHEMA` statement:

```
CREATE SCHEMA (
  `year` int not null,
  `month` int not null,
  `day` int not null)
FOR TABLE dfs.example.myTable
PROPERTIES (
  'drill.logRegex.regex'='(\d\d\d\d)-(\d\d)-(\d\d) .*',
  'drill.logRegex.maxErrors'='10')
```

Use this model if you have a large number of log files with many different
formats. You can store the format information in the table directory so that
you don't have to create a plugin config for each.

Optionally, you can create a workspace for each kind of log (which must
be stored in distinct directory trees), and make your "blank" log format
config the default config for that workspace.

## Table Functions

Log files come in many forms. To quickly query a new format, if you do not
have a config, you can instead use a
[table function](https://drill.apache.org/docs/plugin-configuration-basics/#using-the-formats-attributes-as-table-function-parameters).

### Plugin Config Table Function

You can use a table function to specify the same properties that you can set in the
plugin config.

Example:

```
SELECT * FROM table(dfs.example.myTable (
  type => 'logRegex',
  regex => '(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d) .*',
  maxErrors => 10))
```

The fields match those in the format configuration as shown above.

* **type**: Must be `'logRegex'`. Required. Must be the first parameter.
* **regex**: The regular expression. You must escape back-slashes with a second
back-slash as shown in the example. (If you need to use a single-quote, escape
it with a second single-quote.) Required.
* **maxErrors**: As described above for the format plugin configuration.

In this mode, you cannot specify a schema (table functions do not support a complex type
such as the `schema` field.) Instead, fields will appear in the `columns` array as
explained earlier, and will have type `VARCHAR`.

Nor can you use the `schema` property to specify an in-line provisioned schema since,
unfortunately, the `schema` config property conflicts with the `schema` table property
field.

### Schema Table Function

The other table function option is to use a combination of a schema table function
and a plugin config. The plugin config must at least specify the file extension so that
Drill knows what config to use. See
[DRILL-6965](https://issues.apache.org/jira/browse/DRILL-6965) for details.

Example:

```
SELECT * FROM table(dfs.tf.table2(
  schema=>'inline=(`year` int, `month` int, `day` int) properties {`drill.logRegex.regex`=`(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d) .*`}'))
```

The quoted string for the `schema` value is the same as the SQL you would
use to create a provided schema. The regex must be quoted as described earlier.
Note the use of back-tick quotes to quote strings inside the quoted schema
value.

## Note to Developers

This plugin is a variation of the one discussed in the book
["Learning Apache Drill"](https://www.amazon.com/Learning-Apache-Drill-Analyze-Distributed/dp/1492032794),
Chapter 12, *Writing a Format Plug-in*. This plugin is also the subject of a
[tutorial on the "Enhanced Vector Framework"](https://github.com/paul-rogers/drill/wiki/Developer%27s-Guide-to-the-Enhanced-Vector-Framework).
