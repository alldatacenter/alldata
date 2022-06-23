---
name: Basic Search
route: /SearchBasic
menu: Documentation
submenu: Search
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';
import Img from 'theme/components/shared/Img'

# Basic Search

The basic search allows you to query using typename of an entity, associated classification/tag and has support for filtering on the entity attribute(s) as well as the classification/tag attributes.

The entire query structure can be represented using the following JSON structure (called SearchParameters)

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
  "typeName":               "hive_column",
  "excludeDeletedEntities": true,
  "classification":         "PII",
  "query":                  "",
  "offset":                 0,
  "limit":                  25,
  "entityFilters":          {  },
  "tagFilters":             { },
  "attributes":             [ "table", "qualifiedName"]
}`}
</SyntaxHighlighter>

**Field description**

 <SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
   {`typeName:               the type of entity to look for
excludeDeletedEntities: should the search exclude deleted entities? (default: true)
classification:         only include entities with given classification
query:                  any free text occurrence that the entity should have (generic/wildcard queries might be slow)
offset:                 starting offset of the result set (useful for pagination)
limit:                  max number of results to fetch
entityFilters:          entity attribute filter(s)
tagFilters:             classification attribute filter(s)
attributes:             attributes to include in the search result`}
</SyntaxHighlighter>

<Img src={`/images/twiki/search-basic-hive_column-PII.png`} height="500" width="840"/>

   Attribute based filtering can be done on multiple attributes with AND/OR conditions.

**Examples of filtering (for hive_table attributes)**
   * Single attribute

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`   {
     "typeName":               "hive_table",
     "excludeDeletedEntities": true,
     "offset":                 0,
     "limit":                  25,
     "entityFilters": {
        "attributeName":  "name",
        "operator":       "contains",
        "attributeValue": "customers"
     },
     "attributes": [ "db", "qualifiedName" ]
   }`}
</SyntaxHighlighter>

<Img src={`/images/twiki/search-basic-hive_table-customers.png`} height="500" width="840"/>

   * Multi-attribute with OR

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`   {
     "typeName":               "hive_table",
     "excludeDeletedEntities": true,
     "offset":                 0,
     "limit":                  25,
     "entityFilters": {
        "condition": "OR",
        "criterion": [
           {
              "attributeName":  "name",
              "operator":       "contains",
              "attributeValue": "customers"
           },
           {
              "attributeName":  "name",
              "operator":       "contains",
              "attributeValue": "provider"
           }
        ]
     },
     "attributes": [ "db", "qualifiedName" ]
   }`}
</SyntaxHighlighter>

<Img src={`/images/twiki/search-basic-hive_table-customers-or-provider.png`} height="500" width="840"/>

   * Multi-attribute with AND

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`   {
     "typeName":               "hive_table",
     "excludeDeletedEntities": true,
     "offset":                 0,
     "limit":                  25,
     "entityFilters": {
        "condition": "AND",
        "criterion": [
           {
              "attributeName":  "name",
              "operator":       "contains",
              "attributeValue": "customers"
           },
           {
              "attributeName":  "owner",
              "operator":       "eq",
              "attributeValue": "hive"
           }
        ]
     },
     "attributes": [ "db", "qualifiedName" ]
  }`}
</SyntaxHighlighter>

<Img src={`/images/twiki/search-basic-hive_table-customers-owner_is_hive.png`} height="500" width="840"/>

**Supported operators for filtering**

   * LT (symbols: <, lt) works with Numeric, Date attributes
   * GT (symbols: >, gt) works with Numeric, Date attributes
   * LTE (symbols: <=, lte) works with Numeric, Date attributes
   * GTE (symbols: >=, gte) works with Numeric, Date attributes
   * EQ (symbols: eq, =) works with Numeric, Date, String attributes
   * NEQ (symbols: neq, !=) works with Numeric, Date, String attributes
   * LIKE (symbols: like, LIKE) works with String attributes
   * STARTS_WITH (symbols: startsWith, STARTSWITH) works with String attributes
   * ENDS_WITH (symbols: endsWith, ENDSWITH) works with String attributes
   * CONTAINS (symbols: contains, CONTAINS) works with String attributes

**CURL Samples**

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`curl -sivk -g
    -u <user>:<password>
    -X POST
    -d '{
            "typeName":               "hive_table",
            "excludeDeletedEntities": true,
            "classification":         "",
            "query":                  "",
            "offset":                 0,
            "limit":                  50,
            "entityFilters": {
               "condition": "AND",
               "criterion": [
                  {
                     "attributeName":  "name",
                     "operator":       "contains",
                     "attributeValue": "customers"
                  },
                  {
                     "attributeName":  "owner",
                     "operator":       "eq",
                     "attributeValue": "hive"
                  }
               ]
            },
            "attributes": [ "db", "qualifiedName" ]
          }'
    <protocol>://<atlas_host>:<atlas_port>/api/atlas/v2/search/basic`}
</SyntaxHighlighter>
