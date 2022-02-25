---
name: WhatsNew-1.0
route: /WhatsNew-1.0
menu: Downloads
submenu: Whats New
---

# What's new in Apache Atlas 1.0?

## Features

* Introduction of relationships as a first-class type
* Support for propagation of classifications along entity relationships – like lineage
* Fine-grained metadata security, which enables access controls up to entity instance level
* Introduction of Glossary feature
* Introduction of V2 style notifications
* Introduction of Atlas hook for HBase
* Support for Cassandra and Elasticsearch (tech-preview)

## Updates

* Graph store has been updated from Titan 0.5.4 to JanusGraph 0.2.0
* DSL rewrite, to replace use of Scala based implementation with ANTLR
* Performance improvements in Atlas Hooks, by switching to use V2 style notifications
* Significant updates in Atlas Web UI

## Changes

### DSL search

With DSL rewrite and simplification, some older constructs may not work. Here's a list of behavior changes from previous
releases. More DSL related changes can be found [here](#/SearchAdvance).

   * When filtering or narrowing results using string attribute, the value **MUST** be enclosed in double quotes
      * Table name="Table1"
      * Table where name="Table1"
   * Join queries are no longer supported e.g. hive_table, hive_db
   * Select clauses only work with immediate entity attributes or a single referred (entity) type.
      * Table select name, owner
      * Table select Columns
      * Table select name, owner, Columns _*(won't work)*_
   * OrderBy clause can only be used with a _*single primitive*_ attribute.
   * GroupBy clause can only be used with a _*single primitive*_ attribute.
      * Table groupby name
      * Table groupby Columns (won't work)
   * Typename can't have multiple aliases
      * Table as t (OK)
      * Table as t1, t2 (won't work)
  * Has clause only works with primitive attributes.
     * Table has name
      * Table has Columns or Table has DB (NOT supported)
  * Aggregator clause can only be used with a _*single primitive*_ attribute.
      * Table select min(name)
      * Table select max(name)
      * Table select sum(createTime)
      * Table select min(Columns) (won't work)
      * Table select max(Columns) (won't work)
      * Table select sum(Columns) (won't work)
  * Aggregator clause can't be repeated with different _*primitive attribute*_, the clause appearing last would take preference.
      * Table select min(name), min(createTime) will ignore _*min(name)*_
  * Limit and offset are not applicable when using aggregator clauses (min, max, sum)
      *  Table select min(name) limit 10 offset 5 - min(name) is computed over **ALL** entities of type Asset
