---
name: Import Entity Transforms
route: /ImportEntityTransforms
menu: Documentation
submenu: Import/Export
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# (New) Entity Transforms Framework

#### Background

During Import Process, entity transforms are required to make changes to the entity before it gets committed to the database. These modifications are necessary to make the entity conform to the environment it is going to reside. The Import Process provided a mechanism to do that.

#### Transformation Framework

A transformation framework allows a mechanism to selectively transform an entity or specific attributes of that entity.

To achieve this, the framework provides:

* Way to set a condition that needs to be satisfied for a transformation to be applied.
* Action to be taken on the entity once the condition is met.

The existing transformation frameworks allowed this to happen.

#### Reason for New Transformation Framework

While the existing framework provided the basic benefits of the transformation framework, it did not have support for some of the commonly used Atlas types. Which meant that users of this framework would have to meticulously define transformations for every type they are working with. This can be tedious and potentially error-prone.
The new framework addresses this problem by providing built-in transformations for some of the commonly used types. It can also be extended to accommodate new types.

#### Approach

The approach used by the new transformation framework creates a transformation by:
* Specifying a condition.
* Specifying action(s) to be taken if the condition is met.

##### Conditions

Following are built-in conditions.

|**Condition Types**                          | **Description**    |
-----------------------------------------|-----------------|
ENTITY_ALL                | Any/every entity               |
ENTITY_TOP_LEVEL          | Entity that is the top-level entity. This is also the entity present specified in _AtlasExportRequest_.|
EQUALS                    | Entity attribute equals to the one specified in the condition. |
EQUALS_IGNORE_CASE        | Entity attribute equals to the one specified in the condition ignoring case. |
STARTS_WITH               | Entity attribute starts with. |
STARTS_WITH_IGNORE_CASE   | Entity attribute starts with ignoring case. |
HAS_VALUE                 | Entity attribute has value. |


##### Actions

|**Action Type**        | *Description**                                 |
-------------------|------------------------------------------------|
ADD_CLASSIFICATION | Add classification                             |
REPLACE_PREFIX     | Replace value starting with another value.     |
TO_LOWER           | Convert a value of an attribute to lower case. |
SET                | Set the value of an attribute                  |
CLEAR              | Clear value of an attribute                    |

#### Built-in Transforms

##### Add Classification

During import, hive_db entity whose _qualifiedName_ is _stocks@cl1_ will get the classification _clSrcImported_.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "conditions": {
        "hive_db.qualifiedName": "stocks@cl1"
    },
    "action": {
        "__entity": "ADD_CLASSIFICATION: clSrcImported"
    }
}`}
</SyntaxHighlighter>

Every imported entity will get the classification by simply changing the condition. The __entity is a special condition which matches entity.


<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "conditions": {
        "__entity": ""
    },
    "action": {
        "__entity": "ADD_CLASSIFICATION: clSrcImported"
    }
}`}
</SyntaxHighlighter>

To add the classification to only the top-level entity (an entity that is used as a starting point for an export), use:

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "conditions": {
        "__entity": "topLevel:"
    },
    "action": {
        "__entity": "ADD_CLASSIFICATION: clSrcImported"
    }
}`}
</SyntaxHighlighter>

##### Replace Prefix

This action works on string values. The first parameter is the prefix that is searched for a match, once matched, it is replaced with the provided replacement string.

The sample below searches for _/aa/bb/_, once found replaces it with _/xx/yy/_.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "conditions": {
        "hdfs_path.clusterName": "EQUALS: CL1"
    },
    "action": {
        "hdfs_path.path": "REPLACE_PREFIX: = :/aa/bb/=/xx/yy/"
    }
}`}
</SyntaxHighlighter>

##### To Lower

Entity whose hdfs_path.clusterName is CL1 will get its path attribute converted to lower case.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "conditions": {
        "hdfs_path.clusterName": "EQUALS: CL1"
    },
    "action": {
        "hdfs_path.path": "TO_LOWER:"
    }
}`}
</SyntaxHighlighter>

##### Clear

Entity whose hdfs_path.clusterName has value set, will get its _replicatedTo_ attribute value cleared.

<SyntaxHighlighter wrapLines={true} language="json" style={theme.dark}>
{`{
    "conditions": {
        "hdfs_path.clusterName": "HAS_VALUE:"
    },
    "action": {
        "hdfs_path.replicatedTo": "CLEAR:"
    }
}`}
</SyntaxHighlighter>


#### Additional Examples

Please look at [these tests](https://github.com/apache/atlas/blob/master/intg/src/test/java/org/apache/atlas/entitytransform/TransformationHandlerTest.java) for examples using Java classes.
