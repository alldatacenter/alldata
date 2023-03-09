<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Griffin DSL Guide
Griffin DSL is designed for DQ measurement, as a SQL-like language, which describes the DQ domain request.

## Apache Griffin DSL Syntax Description
Griffin DSL syntax is easy to learn as it's SQL-like, case insensitive.

### Supporting process
- logical operation: `not, and, or, in, between, like, rlike, is null, is nan, =, !=, <>, <=, >=, <, >`
- mathematical operation: `+, -, *, /, %`
- sql statement: `as, where, group by, having, order by, limit`

### Keywords
- `null, nan, true, false`
- `not, and, or`
- `in, between, like, rlike, is`
- `select, distinct, from, as, where, group, by, having, order, desc, asc, limit`

### Operators
- `!, &&, ||, =, !=, <, >, <=, >=, <>`
- `+, -, *, /, %`
- `(, )`
- `., [, ]`

### Literals
- **string**: any string surrounded with a pair of " or ', with escape character \ if any request.  
	e.g. `"test"`, `'string 1'`, `"hello \" world \" "`
- **number**: double or integer number.  
	e.g. `123`, `33.5`
- **time**: an integer with unit in a string, will be translated to an integer number by millisecond.  
	e.g. `3d`, `5h`, `4ms`
- **boolean**: boolean value directly.  
	e.g. `true`, `false`

### Selections
- **selection head**: data source name.  
	e.g. `source`, `target`, `` `my table name` ``
- **all field selection**: * or with data source name ahead.  
	e.g. `*`, `source.*`, `target.*`
- **field selection**: field name or with data source name ahead.  
	e.g. `source.age`, `target.name`, `user_id`
- **index selection**: integer between square brackets "[]" with field name ahead.  
	e.g. `source.attributes[3]`
- **function selection**: function name with brackets "()", with field name ahead or not.  
	e.g. `count(*)`, `*.count()`, `source.user_id.count()`, `max(source.age)`
- **alias**: declare an alias after a selection.  
	e.g. `source.user_id as id`, `target.user_name as name`

### Math expressions
- **math factor**: literal or function or selection or math exression with brackets.  
	e.g. `123`, `max(1, 2, 3, 4)`, `source.age`, `(source.age + 13)`
- **unary math expression**: unary math operator with factor.  
	e.g. `-(100 - source.score)`
- **binary math expression**: math factors with binary math operators.  
	e.g. `source.age + 13`, `score * 2 + ratio`

### Logical expression
- **in**: in clause like sql.  
	e.g. `source.country in ("USA", "CHN", "RSA")`
- **between**: between clause like sql.  
	e.g. `source.age between 3 and 30`, `source.age between (3, 30)`
- **like**: like clause like sql.  
	e.g. `source.name like "%abc%"`
- **rlike**: rlike clause like spark sql.  
    e.g. `source.name rlike "^abc.*$"`
- **is null**: is null operator like sql.  
	e.g. `source.desc is not null`
- **is nan**: check if the value is not a number, the syntax like `is null`  
	e.g. `source.age is not nan`
- **logical factor**: math expression or logical expressions above or other logical expressions with brackets.  
	e.g. `(source.user_id = target.user_id AND source.age > target.age)`
- **unary logical expression**: unary logical operator with factor.  
	e.g. `NOT source.has_data`, `!(source.age = target.age)`
- **binary logical expression**: logical factors with binary logical operators, including `and`, `or` and comparison operators.  
	e.g. `source.age = target.age OR source.ticket = target.tck`


### Expression
- **expression**: logical expression and math expression.

### Function
- **argument**: expression.
- **function**: function name with arguments between brackets.  
	e.g. `max(source.age, target.age)`, `count(*)`

### Clause
- **select clause**: the result columns like sql select clause, we can ignore the word "select" in Apache Griffin DSL.  
	e.g. `select user_id.count(), age.max() as max`, `source.user_id.count() as cnt, source.age.min()`
- **from clause**: the table name like sql from clause, in which the data source name must be one of data source names or the output table name of the former rule steps, we can ignore this clause by configoring the data source name.  
	e.g. `from source`, ``from `target` ``
- **where clause**: the filter condition like sql where clause, optional.  
	e.g. `where source.id = target.id and source.age = target.age`
- **group-by clause**: like the group-by clause in sql, optional. Optional having clause could be following.  
	e.g. `group by cntry`, `group by gender having count(*) > 50`
- **order-by clause**: like the order-by clause, optional.  
	e.g. `order by name`, `order by first_name desc, age asc`
- **limit clause**: like the limit clause in sql, optional.  
	e.g. `limit 5`

### Accuracy Rule
Accuracy rule expression in Apache Griffin DSL is a logical expression, telling the mapping relation between data sources.  
	e.g. `source.id = target.id and source.name = target.name and source.age between (target.age, target.age + 5)`

### Profiling Rule
Profiling rule expression in Apache Griffin DSL is a sql-like expression, with select clause ahead, following optional from clause, where clause, group-by clause, order-by clause, limit clause in order.  
	e.g. `source.gender, source.id.count() where source.age > 20 group by source.gender`, `select country, max(age), min(age), count(*) as cnt from source group by country order by cnt desc limit 5`

### Distinctness Rule
Distinctness rule expression in Apache Griffin DSL is a list of selection expressions separated by comma, indicates the columns to check if is distinct.
    e.g. `name, age`, `name, (age + 1) as next_age`

### Uniqueness Rule
Uniqueness rule expression in Apache Griffin DSL is a list of selection expressions separated by comma, indicates the columns to check if is unique. The uniqueness indicates the items without any replica of data.
    e.g. `name, age`, `name, (age + 1) as next_age`

### Completeness Rule
Completeness rule expression in Apache Griffin DSL is a list of selection expressions separated by comma, indicates the columns to check if is null.
    e.g. `name, age`, `name, (age + 1) as next_age`

### Timeliness Rule
Timeliness rule expression in Apache Griffin DSL is a list of selection expressions separated by comma, indicates the input time and output time (calculate time as default if not set).  
	e.g. `ts`, `ts, end_ts`

## Apache Griffin DSL translation to SQL
Apache Griffin DSL is defined for DQ measurement, to describe DQ domain problem.  
Actually, in Apache Griffin, we get Apache Griffin DSL rules, translate them into spark-sql rules for calculation in spark-sql engine.  
In DQ domain, there're multiple dimensions, we need to translate them in different ways.

### Accuracy
For accuracy, we need to get the match count between source and target, the rule describes the mapping relation between data sources. Apache Griffin needs to translate the dsl rule into multiple sql rules.  
For example, the dsl rule is `source.id = target.id and source.name = target.name`, which represents the match condition of accuracy. After the translation, the sql rules are as below:  
- **get miss items from source**: `SELECT source.* FROM source LEFT JOIN target ON coalesce(source.id, '') = coalesce(target.id, '') and coalesce(source.name, '') = coalesce(target.name, '') WHERE (NOT (source.id IS NULL AND source.name IS NULL)) AND (target.id IS NULL AND target.name IS NULL)`, save as table `miss_items`.
- **get miss count**: `SELECT COUNT(*) AS miss FROM miss_items`, save as table `miss_count`.
- **get total count from source**: `SELECT COUNT(*) AS total FROM source`, save as table `total_count`.
- **get accuracy metric**: `SELECT miss_count.miss AS miss, total_count.total AS total, (total_count.total - miss_count.miss) AS matched FROM miss_count FULL JOIN total_count`, save as table `accuracy`.  

After the translation, the metrics will be persisted in table `accuracy`.

### Profiling
For profiling, the request is always the aggregation function of data, the rule is mainly the same as sql, but only supporting `select`, `from`, `where`, `group-by`, `having`, `order-by`, `limit` clauses, which can describe most of the profiling requests. If any complicate request, you can use sql rule directly to describe it.  
For example, the dsl rule is `source.cntry, source.id.count(), source.age.max() group by source.cntry`, which represents the profiling requests. After the translation, the sql rule is as below:  
- **profiling sql rule**: `SELECT source.cntry, count(source.id), max(source.age) FROM source GROUP BY source.cntry`, save as table `profiling`.  

After the translation, the metrics will be persisted in table `profiling`.

### Distinctness
For distinctness, is to find out the duplicate items of data, the same as uniqueness in batch mode, but with some differences in streaming mode.
In most time, you need distinctness other than uniqueness.
For example, the dsl rule is `name, age`, which represents the distinct requests, in this case, source and target are the same data set. After the translation, the sql rule is as below:
- **total count of source**: `SELECT COUNT(*) AS total FROM source`, save as table `total_count`.
- **group by fields**: `SELECT name, age, (COUNT(*) - 1) AS dup, TRUE AS dist FROM source GROUP BY name, age`, save as table `dup_count`.
- **distinct metric**: `SELECT COUNT(*) AS dist_count FROM dup_count WHERE dist`, save as table `distinct_metric`.
- **source join distinct metric**: `SELECT source.*, dup_count.dup AS dup, dup_count.dist AS dist FROM source LEFT JOIN dup_count ON source.name = dup_count.name AND source.age = dup_count.age`, save as table `dist_joined`.
- **add row number**: `SELECT *, ROW_NUMBER() OVER (DISTRIBUTE BY name, age SORT BY dist) row_num FROM dist_joined`, save as table `row_numbered`.
- **duplicate records**: `SELECT name, age, dup FROM row_numbered WHERE NOT dist OR row_num > 1`, save as table `dup_records`.
- **duplicate metric**: `SELECT name, age, dup, COUNT(*) AS num FROM dup_records GROUP BY name, age, dup`, save as table `dup_metric`.

After the translation, the metrics will be persisted in table `distinct_metric` and `dup_metric`.

### Completeness
For completeness, is to check for null. The columns you measure are incomplete if they are null. 
- **total count of source**: `SELECT COUNT(*) AS total FROM source`, save as table `total_count`.
- **incomplete metric**: `SELECT count(*) as incomplete FROM source WHERE NOT (id IS NOT NULL)`, save as table `incomplete_count`.
- **complete metric**: `SELECT (source.total - incomplete_count.incomplete) AS complete FROM source LEFT JOIN incomplete_count`, save as table `complete_count`.

### Timeliness
For timeliness, is to measure the latency of each item, and get the statistics of the latencies.  
For example, the dsl rule is `ts, out_ts`, the first column means the input time of item, the second column means the output time of item, if not set, `__tmst` will be the default output time column. After the translation, the sql rule is as below:  
- **get input and output time column**: `SELECT *, ts AS _bts, out_ts AS _ets FROM source`, save as table `origin_time`.  
- **get latency**: `SELECT *, (_ets - _bts) AS latency FROM origin_time`, save as table `lat`.
- **get timeliness metric**: `SELECT CAST(AVG(latency) AS BIGINT) AS avg, MAX(latency) AS max, MIN(latency) AS min FROM lat`, save as table `time_metric`.

After the translation, the metrics will be persisted in table `time_metric`.

## Alternative Rules
You can simply use Griffin DSL rule to describe your problem in DQ domain, for some complicate requirement, you can also use some alternative rules supported by Apache Griffin.  

### Spark sql
Griffin supports spark-sql directly, you can write rule in sql like this:  
```
{
	"dsl.type": "spark-sql",
	"name": "source",
	"rule": "SELECT count(id) AS cnt, max(timestamp) AS fresh_time FROM source"
}
```
Griffin will calculate it in spark-sql engine directly.  

### Data frame operation
Griffin supports some other operations on data frame in spark, like converting json string data frame into extracted data frame with extracted object schema. For example:  
```
{
	"dsl.type": "df-opr",
	"name": "ext_source",
	"rule": "from_json",
	"details": {
		"df.name": "json_source"
	}
}
```
Griffin will do the operation to extract json strings.  
Actually, you can also extend the df-opr engine and df-opr adaptor in Apache Griffin to support more types of data frame operations.  

## Tips
Griffin engine runs on spark, it might work in two phases, pre-proc phase and run phase.  
- **Pre-proc phase**: Apache Griffin calculates data source directly, to get appropriate data format, as a preparation for DQ calculation. In this phase, you can use df-opr and spark-sql rules.  
After preparation, to support streaming DQ calculation, a timestamp column will be added in each row of data, so the data frame in run phase contains an extra column named "__tmst".  
- **Run phase**: Apache Griffin calculates with prepared data, to get the DQ metrics. In this phase, you can use griffin-dsl, spark-sql rules, and a part of df-opr rules.  
For griffin-dsl rule, Apache Griffin translates it into spark-sql rule with a group-by condition for column "__tmst", it's useful for especially streaming DQ calculation.  
But for spark-sql rule, Apache Griffin use it directly, you need to add the "__tmst" column in your spark-sql rule explicitly, or you can't get correct metrics result after calculation.
