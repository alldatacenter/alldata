---
name: Type System
route: /TypeSystem
menu: Documentation
submenu: Features
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';


# Type System

## Overview
Atlas allows users to define a model for the metadata objects they want to manage. The model is composed of definitions
called ‘types’. Instances of ‘types’ called ‘entities’ represent the actual metadata objects that are managed. The Type
System is a component that allows users to define and manage the types and entities. All metadata objects managed by
Atlas out of the box (like Hive tables, for e.g.) are modelled using types and represented as entities. To store new
types of metadata in Atlas, one needs to understand the concepts of the type system component.

## Types
A Type in Atlas is a definition of how a particular type of metadata objects are stored and accessed. A type represents one or a collection of attributes that define the properties for the metadata object. Users with a development background will recognize the similarity of a type to a ‘Class’ definition of object oriented programming languages, or a ‘table schema’ of relational databases.

An example of a type that comes natively defined with Atlas is a Hive table. A Hive table is defined with these
attributes:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`Name:         hive_table
TypeCategory: Entity
SuperTypes:   DataSet
Attributes:
    name:             string
    db:               hive_db
    owner:            string
    createTime:       date
    lastAccessTime:   date
    comment:          string
    retention:        int
    sd:               hive_storagedesc
    partitionKeys:    array<hive_column>
    aliases:          array<string>
    columns:          array<hive_column>
    parameters:       map<string>
    viewOriginalText: string
    viewExpandedText: string
    tableType:        string
    temporary:        boolean`}
</SyntaxHighlighter>

The following points can be noted from the above example:

   * A type in Atlas is identified uniquely by a ‘name’
   * A type has a metatype. Atlas has the following metatypes:
      * Primitive metatypes: boolean, byte, short, int, long, float, double, biginteger, bigdecimal, string, date
      * Enum metatypes
      * Collection metatypes: array, map
      * Composite metatypes: Entity, Struct, Classification, Relationship
   * Entity & Classification types can ‘extend’ from other types, called ‘supertype’ - by virtue of this, it will get to include the attributes that are defined in the supertype as well. This allows modellers to define common attributes across a set of related types etc. This is again similar to the concept of how Object Oriented languages define super classes for a class. It is also possible for a type in Atlas to extend from multiple super types.
      * In this example, every hive table extends from a pre-defined supertype called a ‘DataSet’. More details about this pre-defined types will be provided later.
   * Types which have a metatype of ‘Entity’, ‘Struct’, ‘Classification’ or 'Relationship' can have a collection of attributes. Each attribute has a name (e.g.  ‘name’) and some other associated properties. A property can be referred to using an expression type_name.attribute_name. It is also good to note that attributes themselves are defined using Atlas metatypes.
      * In this example, hive_table.name is a String, hive_table.aliases is an array of Strings, hive_table.db refers to an instance of a type called hive_db and so on.
   * Type references in attributes, (like hive_table.db) are particularly interesting. Note that using such an attribute, we can define arbitrary relationships between two types defined in Atlas and thus build rich models. Note that one can also collect a list of references as an attribute type (e.g. hive_table.columns which represents a list of references from hive_table to hive_column type)

## Entities
An ‘entity’ in Atlas is a specific value or instance of an Entity ‘type’ and thus represents a specific metadata object in the real world. Referring back to our analogy of Object Oriented Programming languages, an ‘instance’ is an‘Object’ of a certain ‘Class’.

An example of an entity will be a specific Hive Table. Say Hive has a table called ‘customers’ in the ‘default’database. This table will be an ‘entity’ in Atlas of type hive_table. By virtue of being an instance of an entity type, it will have values for every attribute that are a part of the Hive table ‘type’, such as:


<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`guid:     "9ba387dd-fa76-429c-b791-ffc338d3c91f"
typeName: "hive_table"
status:   "ACTIVE"
values:
    name:             “customers”
    db:               { "guid": "b42c6cfc-c1e7-42fd-a9e6-890e0adf33bc",
                        "typeName": "hive_db"
                      }
    owner:            “admin”
    createTime:       1490761686029
    updateTime:       1516298102877
    comment:          null
    retention:        0
    sd:               { "guid": "ff58025f-6854-4195-9f75-3a3058dd8dcf",
                        "typeName":
                        "hive_storagedesc"
                      }
    partitionKeys:    null
    aliases:          null
    columns:          [ { "guid": "65e2204f-6a23-4130-934a-9679af6a211f",
                          "typeName": "hive_column" },
                        { "guid": "d726de70-faca-46fb-9c99-cf04f6b579a6",
                          "typeName": "hive_column" },
                          ...
                      ]
    parameters:       { "transient_lastDdlTime": "1466403208"}
    viewOriginalText: null
    viewExpandedText: null
    tableType:        “MANAGED_TABLE”
    temporary:        false`}
</SyntaxHighlighter>

The following points can be noted from the example above:

   * Every instance ofan entity type is identified by a unique identifier, a GUID. This GUID is generated by the Atlas server when the object is defined, and remains constant for the entire lifetime of the entity. At any point in time, this particular entity can be accessed using its GUID.
      * In this example, the ‘customers’ table in the default database is uniquely identified by the GUID "9ba387dd-fa76-429c-b791-ffc338d3c91f"
   * An entity is of a given type, and the name of the type is provided with the entity definition.
      * In this example, the ‘customers’ table is a ‘hive_table.
   * The values of this entity are a map of all the attribute names and their values for attributes that are defined in the hive_table type definition.
   * Attribute values will be according to the datatype of the attribute. Entity-type attributes will have value of type AtlasObjectId

With this idea on entities, we can now see the difference between Entity and Struct metatypes. Entities and Structs both compose attributes of other types. However, instances of Entity types have an identity (with a GUID value) and can be referenced from other entities (like a hive_db entity is referenced from a hive_table entity). Instances of Struct types do not have an identity of their own. The value of a Struct type is a collection of attributes that are 'embedded' inside the entity itself.

## Attributes
We already saw that attributes are defined inside metatypes like Entity, Struct, Classification and Relationship. But we
implistically referred to attributes as having a name and a metatype value. However, attributes in Atlas have some more
properties that define more concepts related to the type system.

An attribute has the following properties:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`name:        string,
typeName:    string,
isOptional:  boolean,
isIndexable: boolean,
isUnique:    boolean,
cardinality: enum`}
</SyntaxHighlighter>

The properties above have the following meanings:

   * name - the name of the attribute
   * dataTypeName - the metatype name of the attribute (native, collection or composite)
   * isComposite -
      * This flag indicates an aspect of modelling. If an attribute is defined as composite, it means that it cannot have a lifecycle independent of the entity it is contained in. A good example of this concept is the set of columns that make a part of a hive table. Since the columns do not have meaning outside of the hive table, they are defined as composite attributes.
      * A composite attribute must be created in Atlas along with the entity it is contained in. i.e. A hive column must be created along with the hive table.
   * isIndexable -
      * This flag indicates whether this property should be indexed on, so that look ups can be performed using the attribute value as a predicate and can be performed efficiently.
   * isUnique -
      * This flag is again related to indexing. If specified to be unique, it means that a special index is created for this attribute in JanusGraph that allows for equality based look ups.
      * Any attribute with a true value for this flag is treated like a primary key to distinguish this entity from other entities. Hence care should be taken ensure that this attribute does model a unique property in real world.
         * For e.g. consider the name attribute of a hive_table. In isolation, a name is not a unique attribute for a hive_table, because tables with the same name can exist in multiple databases. Even a pair of (database name, table name) is not unique if Atlas is storing metadata of hive tables amongst multiple clusters. Only a cluster location, database name and table name can be deemed unique in the physical world.
   * multiplicity - indicates whether this attribute is required, optional, or could be multi-valued. If an entity’s definition of the attribute value does not match the multiplicity declaration in the type definition, this would be a constraint violation and the entity addition will fail. This field can therefore be used to define some constraints on the metadata information.

Using the above, let us expand on the attribute definition of one of the attributes of the hive table below.
Let us look at the attribute called ‘db’ which represents the database to which the hive table belongs:


<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`db:
    "name":        "db",
    "typeName":    "hive_db",
    "isOptional":  false,
    "isIndexable": true,
    "isUnique":    false,
    "cardinality": "SINGLE"`}
</SyntaxHighlighter>

Note the “isOptional=true” constraint - a table entity cannot be created without a db reference.


<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`columns:
    "name":        "columns",
    "typeName":    "array<hive_column>",
    "isOptional":  optional,
    "isIndexable": true,
    “isUnique":    false,
    "constraints": [ { "type": "ownedRef" } ]`}
</SyntaxHighlighter>

> Note: The “ownedRef” constraint for columns. By doing this, we are indicating that the defined column entities should
always be bound to the table entity they are defined with.

From this description and examples, you will be able to realize that attribute definitions can be used to influence
specific modelling behavior (constraints, indexing, etc) to be enforced by the Atlas system.

## System specific types and their significance
Atlas comes with a few pre-defined system types. We saw one example (DataSet) in preceding sections. In this
section we will see more of these types and understand their significance.

**Referenceable**: This type represents all entities that can be searched for using a unique attribute called
qualifiedName.

**Asset**: This type extends Referenceable and adds attributes like name, description and owner. Name is a required
attribute (isOptional=false), the others are optional.

The purpose of Referenceable and Asset is to provide modellers with way to enforce consistency when defining and
querying entities of their own types. Having these fixed set of attributes allows applications and user interfaces to
make convention based assumptions about what attributes they can expect of types by default.

**Infrastructure**: This type extends Asset and typically can be used to be a common super type for infrastructural
metadata objects like clusters, hosts etc.

**DataSet**: This type extends Referenceable. Conceptually, it can be used to represent an type that stores data. In Atlas,
hive tables, hbase_tables etc are all types that extend from DataSet. Types that extend DataSet can be expected to have
a Schema in the sense that they would have an attribute that defines attributes of that dataset. For e.g. the columns
attribute in a hive_table. Also entities of types that extend DataSet participate in data transformation and this
transformation can be captured by Atlas via lineage (or provenance) graphs.

**Process**: This type extends Asset. Conceptually, it can be used to represent any data transformation operation. For
example, an ETL process that transforms a hive table with raw data to another hive table that stores some aggregate can
be a specific type that extends the Process type. A Process type has two specific attributes, inputs and outputs. Both
inputs and outputs are arrays of DataSet entities. Thus an instance of a Process type can use these inputs and outputs
to capture how the lineage of a DataSet evolves.
