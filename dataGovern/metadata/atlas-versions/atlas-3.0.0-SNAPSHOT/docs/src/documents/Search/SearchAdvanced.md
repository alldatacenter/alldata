---
name: Advanced Search
route: /SearchAdvance
menu: Documentation
submenu: Search
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

#  Advanced Search

### **Background**
Advanced Search in Atlas is also referred to as DSL-based Search.

Domain Specific Search (DSL) is a language with simple constructs that help users navigate Atlas data repository. The syntax loosely emulates the popular Structured Query Language (SQL) from relation database world.

Benefits of DSL:
   * Abstracts the implementation-level database constructs. This avoids the necessity of knowing about the underlying graph database constructs.
   * User are provided with an abstraction that helps them retrieve the data by just being aware of the types and their relationships within their dataset.
   * Allows for a way to specify the desired output.
   * Use of classifications is accounted for in the syntax.
   * Provides way to group and aggregate results.

We will be using the quick start dataset in the examples that follow. This dataset is comprehensive enough to be used to to demonstrate the various features of the language.

For details on the grammar, please refer to Atlas DSL Grammer on [Github](https://github.com/apache/atlas/blob/master/repository/src/main/java/org/apache/atlas/query/antlr4/AtlasDSLParser.g4) (Antlr G4 format).

## Using Advanced Search

Within the Atlas UI, select Advanced in the Search pane on the left.

Notice that the *Favorite Searches* pane below the *Search By Query* box. Like *Basic Search*, it is possible to save the *Advanced Searches* as well.

## Introduction to Domain Specific Language

DSL uses the familiar SQL-like syntax.

At a high-level a query has a _from-where-select_ format. Additional keywords like _grouby_, _orderby_, _limit_ can be used to added to affect the output. We will see examples of these below.

### From Clause

Specifying the _from_ clause is mandatory. Using the _from_ keyword itself is optional. The value specified in the _from_ clause acts as the source or starting point for the rest of the query to source its inputs.

Example: To retrieve all entities of type _DB_:

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`DB
from DB`}
</SyntaxHighlighter>

In the absence of _where_ for filtering on the source, the dataset fetched by the _from_ clause is everything from the database. Based on the size of the data present in the database, there is a potential to overwhelm the server. The query processor thus adds _limit_ clause with a default value set. See the section on _limit_ clause for details.

### Where Clause

The _where_ clause allows for filtering over the dataset. This achieved by using conditions within the where clause.

A conditions is identifier followed by an operator followed by a literal. Literal must be enclosed in single or double quotes. Example, _name = "Sales"_. An identifier can be name of the property of the type specified in the _from_ clause or an alias.

Example: To retrieve entity of type _Table_ with a specific name say time_dim:

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table where name = 'time_dim'`}
</SyntaxHighlighter>

It is possible to specify multiple conditions by combining them using _and_, _or_ operators.

Example: To retrieve entity of type Table with name that can be either time_dim or customer_dim:

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table where name = 'time_dim' or name = 'customer_dim'`}
</SyntaxHighlighter>

Filtering based on a list of values is done using by specifying the values in the square brackets. A value array is a list of values enclosed within square brackets. This is a simple way to specify an OR clause on an identifier.

Note that having several OR clauses on the same attribute may be inefficient. Alternate way is to use the value array as shown in the example below.

Example: The query in the example above can be written using a value array as shown below.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table where name = ["customer_dim", "time_dim"]`}
</SyntaxHighlighter>

A condition that uses the LIKE operator, allows for filtering using wildcards like '*' or '?'.
Example: To retrieve entity of type _Table_ whose name ends with '_dim':


<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table where name LIKE '*_dim'`}
</SyntaxHighlighter>

Additional forms of regular expressions can also be used.

Example: To retrieve _DB_ whose name starts with _R_ followed by has any 3 characters, followed by _rt_ followed by at least 1 character, followed by none or any number of characters.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`DB where name like "R???rt?*"`}
</SyntaxHighlighter>

Example: To find all the columns in a Table.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Column where table.name="sales_fact"`}
</SyntaxHighlighter>

Example: To find all the Tables for a column.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table where columns.name="sales"`}
</SyntaxHighlighter>

Example: To retrieve all the entities of type _Table_ that are tagged with _Dimension_ classification and its attribute _priority_ having 'high'

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Table where Dimension.priority = "high"`}
</SyntaxHighlighter>



### Using Date Literals
Dates used in literals need to be specified using the ISO 8601 format.

Dates in this format follow this notation:
   * _yyyy-MM-ddTHH:mm:ss.SSSZ_. Which means, year-month-day followed by time in hour-minutes-seconds-milli-seconds. Date and time need to be separated by 'T'. It should end with 'Z'.
   * _yyyy-MM-dd_. Which means, year-month-day.

Example: Date represents December 11, 2017 at 2:35 AM.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`2017-12-11T02:35:0.0Z`}
</SyntaxHighlighter>

Example: To retrieve entity of type _Table_ created within 2017 and 2018.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table where createTime < '2018-01-01' and createTime > '2017-01-01'`}
</SyntaxHighlighter>

#### Using Boolean Literals
Properties of entities of type boolean can be used within queries.

Eample: To retrieve entity of type hdfs_path whose attribute _isFile_ is set to _true_ and whose name is _Invoice_.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from hdfs_path where isFile = true or name = "Invoice"`}
</SyntaxHighlighter>

Valid values for boolean literals are 'true' and 'false'.

### Existence of a Property
The has keyword can be used with or without the where clause. It is used to check existence of a property in an entity.

Example: To retreive entity of type Table with a property locationUri.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table has locationUri
from Table where Table has locationUri`}
</SyntaxHighlighter>

### Select Clause
If you noticed the output displayed on the web page, it displays a tabular display, each row corresponding to an entity and columns are properties of that entity. The select clause allows for choosing the properties of entity that are of interest.


Example: To retrieve entity of type _Table_ with few properties:

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table select owner, name, qualifiedName`}
</SyntaxHighlighter>

Example: To retrieve entity of type Table for a specific table with some properties.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table where name = 'customer_dim' select owner, name, qualifiedName`}
</SyntaxHighlighter>

To display column headers that are more meaningful, aliases can be added using the 'as' clause.

Example: To display column headers as 'Owner', 'Name' and 'FullName'.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`from Table select owner as Owner, name as Name, qualifiedName as FullName`}
</SyntaxHighlighter>

#### Note About Select Clauses

Given the complexity involved in using select clauses, these are the few rules to remember when using select clauses:
   * Works with all immediate attributes.
   * Works with Immediate attributes and aggregation on immediate attributes.
   * Referred attributes cannot be mixed with immediate attributes.

Example: To retrieve entity of type Table with name 'Sales' and display 'name' and 'owner' attribute of the referred entity DB.


<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Table where name = 'abcd' select DB.name, DB.owner`}
</SyntaxHighlighter>

Current implementation does not allow the following:

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table where name = 'abcd' select DB.name, Table.name`}
</SyntaxHighlighter>

### Classification-based Filtering
In order to retrieve entities based on classification, a query would use _is_ or _isa_ keywords.

Example: To retrieve all entities of type _Table_ that are tagged with _Dimension_ classification.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Table isa Dimension`}
</SyntaxHighlighter>

Since, from is optional and _is_ (or _isa_) are equivalent, the following queries yield the same results:

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table is Dimension`}
</SyntaxHighlighter>

The _is_ and _isa_ clauses can also be used in _where_ condition like:


<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Table where Table isa Dimension`}
</SyntaxHighlighter>

To search for all entities having a particular classification, simply use the name of the classification.

Example: To retrieve all entities that have _Dimension_ classification.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Dimension`}
</SyntaxHighlighter>

To search for all entities having a particular classification with its attribute, add filter in where clause.

Example: To retrieve all the entities that are tagged with _Dimension_ classification and its attribute _priority_ having 'high'

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Dimension where Dimension.priority = "high"`}
</SyntaxHighlighter>

###Non Primitive attribute Filtering
In the discussion so far we looked at where clauses with primitive types. This section will look at using properties that are non-primitive types.

#### Relationship-based filtering
In this model, the DB is modeled such that it is aware of all the Table it contains. Table on the other hand is aware of existence of the DB but is not aware of all the other _Table_ instances within the system. Each Table maintains reference of the _DB_ it belongs to.

Similar structure exists within the _hive_ data model.

Example: To retrieve all the instances of the _Table_ belonging to a database named 'Sales':

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table where db.name = "Sales"`}
</SyntaxHighlighter>

Example: To retrieve all the instances of the _Table_ belonging to a database named 'Sales' and whose column name starts with 'customer':

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table where db.name = "Sales" and columns.name like "customer*"`}
</SyntaxHighlighter>

The entity Column is modeled in a similar way. Each Table entity has outward edges pointing to Column entity instances corresponding to each column within the table.

Example: To retrieve all the Column entities for a given Table.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table where name = "time_dim" select columns`}
</SyntaxHighlighter>

The properties of each _Column_ entity type are displayed.

#### Glossary Term-based Filtering
In order to retrieve entities based on glossary term, a query would use _hasTerm_ keyword.

To search for entities having a particular glossary term, user needs to add a fully qualified name. i.e _{termName}@{glossaryName}_. In case the user adds only the term name, all the entities with particular term name will be returned, irrespective of which glossary it is in.

Example: To retrieve all entities of type _Table_ having glossary term _savingsAccount@Banking_, below are the possible ways.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Table hasTerm "savingsAccount@Banking"
Table hasTerm "savingsAccount@Banking"
Table hasTerm "savingsAccount"
Table where Table hasTerm "savingsAccount@Banking"`}
</SyntaxHighlighter>

Example: To retrieve all entities of type _Table_ having glossary term _savingsAccount@Banking_ and whose name is 'customer'.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Table hasTerm "savingsAccount@Banking" and name = "customer"`}
</SyntaxHighlighter>

Example: To retrieve all entities of type _Table_ having glossary term _savingsAccount@Banking_ or tagged with 'Dimension' classification and whose column name starts with 'customer'.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Table hasTerm "savingsAccount@Banking" or Table isA Dimension and (columns.name like "customer*")`}
</SyntaxHighlighter>

### Limit & Offset Clauses
Often a query yields large number of results. To limit the outcome of the query, the limit and offset clauses are used.

Example: To retrieve only the 5 entities from a result set.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Column limit 5`}
</SyntaxHighlighter>

The offset clauses retrieves results after the offset value.

Example: To retrieve only 5 entities from the result set after skipping the first 10.

<SyntaxHighlighter wrapLines={true} language="sql" style={theme.dark}>
{`Column limit 5 offset 10`}
</SyntaxHighlighter>

The _limit_ and _offset_ clauses are usually specified in conjunction.

If no limit clause is specified in the query, a limit clause with a default limit (usually 100) is added to the query. This prevents the query from inadvertently fetching large number of results.

The _offset_ clause is useful for displaying results in a user interface where few results from the result set are showing and more results are fetched as the user advances to next page.

### Ordering Results
The _orderby_ clause allows for sorting of results. Results are sorted in ascending order by default. Only immediate attributes can be used within this clause.

Ordering can be changed by using:
   * ASC Sort in ascending order. This is the default. If no ordering is specified after the _orderby_ clause.
   * DESC Sort in descending order. This needs to be explicitly specified after the _orderby_ clause.

Example: To retrieve the entities of type _Column_ that are sorted in ascending order using the name property.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Column orderby name
from Column orderby name asc`}
</SyntaxHighlighter>

Example: Same results as above except that they are sorted in descending order.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Column orderby name desc`}
</SyntaxHighlighter>

Example: To retrieve the entities of type _Column_ filtered with name and associated with 'savingsAccount@Banking' glossary term, that are sorted in ascending order using the name property.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`from Column hasTerm "savingsAccount@Banking" and name = "customer_id" orderby name asc`}
</SyntaxHighlighter>

### Aggregate Functions
Let's look at aggregate functions:

   * _sum_: Adds (sums up) a value of the property specified, within the result set.
   * _min_: Finds the minimum value of the property specified, within a result set.
   * _max_: Finds the maximum value of the property specified, within a result set.
   * _count_: Finds the number of items specified by the group by clause.

These work only on immediate attributes.

Other examples of these in the _Grouping Results_ section.

### The count Keyword
Shows the number of items in a result set.

Example: To know how may entities of a type Column.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Column select count()`}
</SyntaxHighlighter>
Example: Same as above with alias.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Column select count() as Cols`}
</SyntaxHighlighter>

Example: To find the number of tables in a database.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table where db.name = "Reporting" select count()`}
</SyntaxHighlighter>

Example: To find the number of terms associated with particular type 'Table'.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table hasTerm "savingsAccount@Banking" select count() as terms`}
</SyntaxHighlighter>


### The max Keyword
Using this keyword it is possible to retrieve the maximum value of a property for an entity.

Example: Get the most recently created value of the _createTime_ property of the Table entity.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table select max(createTime)`}
</SyntaxHighlighter>

### The min Keyword
Using this keyword it is possible to retrieve the minimum value of a property for an entity.

Example: Get the least recently created value of the _createTime_ property of the Table entity.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table select min(createTime)`}
</SyntaxHighlighter>

### Grouping Results
The _groupby_ clause groups results within the result using specified property.

Example: To retrieve entity of type Table such that tables belonging to an owner are together (grouped by owner).

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table groupby(owner)`}
</SyntaxHighlighter>

While _groupby_ can work without _select_, if aggregate functions are used within _select_ clause, using _groupby_ clause becomes mandatory as aggregate functions operate on a group.

Example: To retrieve entity of type Table such we know the most recently created entity.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table groupby(createTime) select owner, name, max(createTime)`}
</SyntaxHighlighter>

Example: To retrieve entity of type Table such we know the oldest entity.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table groupby(createTime) select owner, name, min(createTime)`}
</SyntaxHighlighter>

Example: To know the number of entities owned by each owner.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table groupby(owner) select owner, count()`}
</SyntaxHighlighter>

### Using System Attributes
Each type defined within Atlas gets few attributes by default. These attributes help with internal book keeping of the entities. All the system attributes are prefixed with '__' (double underscore). This helps in identifying them from other attributes.
Following are the system attributes:
   * __guid Each entity within Atlas is assigned a globally unique identifier (GUID for short).
   * __modifiedBy Name of the user who last modified the entity.
   * __createdBy Name of the user who created the entity.
   * __state Current state of the entity. Please see below for details.
   * __timestamp Timestamp (date represented as integer) of the entity at the time of creation.
   * __modificationTimestamp Timestamp (date represented as integer) of the entity at the time of last modification.

### State of an Entity
Entity within Atlas can be in the following states:
   * ACTIVE This is the state of entities that when it is available and is used within the system. It can be retrieved by default by searches.
   * DELETED When an entity is deleted, it's state is marked as DELETED. Entity in this state does not show up in search results. Explicit request needs to be made to retrieve this entity.

### Using System Attributes in Queries

Example: To retrieve all entities that are deleted.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Asset where __state = "DELETED"`}
</SyntaxHighlighter>

Example: To retrieve entity GUIDs.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`Table select __guid`}
</SyntaxHighlighter>

Example: To retrieve several system attributes.

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`hive_db select __timestamp, __modificationTimestamp, __state, __createdBy`}
</SyntaxHighlighter>

## Advanced Search REST API
Relevant models for these operations:
   *  *[AtlasSearchResult](https://github.com/apache/atlas/blob/master/intg/src/main/java/org/apache/atlas/model/discovery/AtlasSearchResult.java)*
   *  *[AtlasBaseException](https://github.com/apache/atlas/blob/master/intg/src/main/java/org/apache/atlas/exception/AtlasBaseException.java)*

### The V2 API
**Get Results using DSL Search**

| **_Example_** | **See Examples sections below.** |
|:----:|:----:|
|_URL_|_api/atlas/v2/search/dsl_|
|_Method_|_GET_|
|_URL Parameters_|_query_: Query conforming to DSL syntax.|
||_typeName_: Type name of the entity to be retrived.|
||_classification_: Classification associated with the type or query.|
||_limit_: Maximum number of items in the result set.|
||_offset_: Starting index of the item in the result set.|
|_Data Parameters_|_None_|
|_Success Response_|The JSON will correspond to [AtlasSearchResult](https://github.com/apache/atlas/blob/master/intg/src/main/java/org/apache/atlas/model/discovery/AtlasSearchResult.java).|
|_Error Response_|Errors that are handled within the system will be returned as [AtlasBaseException](https://github.com/apache/atlas/blob/master/intg/src/main/java/org/apache/atlas/exception/AtlasBaseException.java).|
|_Method Signature_|@GET|
||@Path("/dsl")|
||@Consumes(Servlets.JSON_MEDIA_TYPE)|
||@Produces(Servlets.JSON_MEDIA_TYPE)|

*Examples*

<SyntaxHighlighter wrapLines={true} language="html" style={theme.dark}>
{`curl -X GET -u admin:admin -H "Content-Type: application/json" "http://localhost:21000/api/atlas/v2/search/dsl?typeName=Table"
curl -X GET -u admin:admin -H "Content-Type: application/json" "http://localhost:21000/api/atlas/v2/search/dsl?typeName=Column&classification=PII"
curl -X GET -u admin:admin -H "Content-Type: application/json" "http://localhost:21000/api/atlas/v2/search/dsl?typeName=Table&classification=Dimension&limit=10&offset=2"
curl -X GET -u admin:admin -H "Content-Type: application/json" "http://localhost:21000/api/atlas/v2/search/dsl?query=Table%20isa%20Dimension"
curl -X GET -u admin:admin -H "Content-Type: application/json" "http://localhost:21000/api/atlas/v2/search/dsl?query=Table%20isa%20Dimension&limit=5&offset=2"`}
</SyntaxHighlighter>

## Implementation Approach
The general approach followed in implementation of DSL within Atlas can be enumerated in following steps:
   * Parser parses the incoming query for syntax.
   * Abstract syntax tree is generated by for a query that is parsed successfully.
   * Syntax tree is 'walked' using visitor pattern.
   * Each 'visit' within the tree adds a step in the Gremlin pipeline.
   * When done, the generated script is executed using Gremlin Script Engine.
   * Results generated be the query, if any, are processed and packaged in AtlasSearchResult structure.

## Differences Between Master and Earlier Versions
The following clauses are no longer supported:
   * path
   * loop

## Resources
   * Antlr [Book](https://pragprog.com/book/tpantlr2/the-definitive-antlr-4-reference).
   * Antlr [Quick Start](https://github.com/antlr/antlr4/blob/master/doc/getting-started.md).
   * Atlas DSL Grammar on [Github](https://github.com/apache/atlas/blob/master/repository/src/main/java/org/apache/atlas/query/antlr4/AtlasDSLParser.g4) (Antlr G4 format).
