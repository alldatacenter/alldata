# XML Format Reader
This plugin enables Drill to read XML files without defining any kind of schema. 

## Configuration
Aside from the file extension, there is one configuration option:

* `dataLevel`: XML data often contains a considerable amount of nesting which is not necesarily useful for data analysis. This parameter allows you to set the nesting level 
  where the data actually starts.  The levels start at `1`.

The default configuration is shown below:

```json
"xml": {
  "type": "xml",
  "extensions": [
    "xml"
  ],
  "dataLevel": 2
}
```

## Data Types
All fields are read as strings.  Nested fields are read as maps.  Future functionality could include support for lists.

### Attributes
XML events can have attributes which can also be useful.
```xml
<book>
  <author>O.-J. Dahl</author>
  <title binding="hardcover" subcategory="non-fiction">Structured Programming</title>
  <category>PROGRAMMING</category>
  <year>1972</year>
</book>
```

In the example above, the `title` field contains two attributes, the `binding` and `subcategory`.  In order to access these fields, Drill creates a map called `attributes` and 
adds an entry for each attribute with the field name and then the attribute name.  Every XML file will have a field called `atttributes` regardless of whether the data actually 
has attributes or not.

```xml
<books>
   <book>
     <author>Mark Twain</author>
     <title>The Adventures of Tom Sawyer</title>
     <category>FICTION</category>
     <year>1876</year>
   </book>
   <book>
     <authors>
         <author>Niklaus Wirth</author>
         <author>Somebody else</author>
     </authors>
     <title binding="paperback">The Programming Language Pascal</title>
     <category >PASCAL</category>
     <year>1971</year>
   </book>
   <book>
     <author>O.-J. Dahl</author>
     <title binding="hardcover" subcategory="non-fiction">Structured Programming</title>
     <category>PROGRAMMING</category>
     <year>1972</year>
   </book>
 </books>
```
If you queried this data in Drill you'd get the table below:

```sql
SELECT * 
FROM <path>.`attributes.xml`
```

```
apache drill> select * from dfs.test.`attributes.xml`;
+-----------------------------------------------------------------+------------+---------------------------------+-------------+------+-----------------------------------------+
|                           attributes                            |   author   |              title              |  category   | year |                 authors                 |
+-----------------------------------------------------------------+------------+---------------------------------+-------------+------+-----------------------------------------+
| {}                                                              | Mark Twain | The Adventures of Tom Sawyer    | FICTION     | 1876 | {}                                      |
| {"title_binding":"paperback"}                                   | null       | The Programming Language Pascal | PASCAL      | 1971 | {"author":"Niklaus WirthSomebody else"} |
| {"title_binding":"hardcover","title_subcategory":"non-fiction"} | O.-J. Dahl | Structured Programming          | PROGRAMMING | 1972 | {}                                      |
+-----------------------------------------------------------------+------------+---------------------------------+-------------+------+-----------------------------------------+
```

## Limitations:  Malformed XML
Drill can read properly formatted XML.  If the XML is not properly formatted, Drill will throw errors. Some issues include illegal characters in field names, or attribute names.
Future functionality will include some degree of data cleaning and fault tolerance. 

## Limitations: Schema Ambiguity
XML is a challenging format to process as the structure does not give any hints about the schema.  For example, a JSON file might have the following record:

```json
"record" : {
  "intField:" : 1,
  "listField" : [1, 2],
  "otherField" : {
    "nestedField1" : "foo",
    "nestedField2" : "bar"
  }
}
```

From this data, it is clear that `listField` is a `list` and `otherField` is a map.  This same data could be represented in XML as follows:

```xml
<record>
  <intField>1</intField>
  <listField>
    <value>1</value>
    <value>2</value>
  </listField>
  <otherField>
    <nestedField1>foo</nestedField1>
    <nestedField2>bar</nestedField2>
  </otherField>
</record>
```
This is no problem to parse this data. But consider what would happen if we encountered the following first:
```xml
<record>
  <intField>1</intField>
  <listField>
    <value>2</value>
  </listField>
  <otherField>
    <nestedField1>foo</nestedField1>
    <nestedField2>bar</nestedField2>
  </otherField>
</record>
```
In this example, there is no way for Drill to know whether `listField` is a `list` or a `map` because it only has one entry. 

## Future Functionality

* **Build schema from XSD file or link**:  One of the major challenges of this reader is having to infer the schema of the data. XML files do provide a schema although this is not
 required.  In the future, if there is interest, we can extend this reader to use an XSD file to build the schema which will be used to parse the actual XML file. 
  
* **Infer Date Fields**: It may be possible to add the ability to infer data fields.

* **List Support**:  Future functionality may include the ability to infer lists from data structures.  