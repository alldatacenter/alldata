---
name: Business Metadata
route: /BusinessMetadata
menu: Documentation
submenu: Features
---

import Img from 'theme/components/shared/Img'
import themen  from 'theme/styles/styled-colors';
import * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Business Metadata
## Overview
Atlas typesystem allows users to define a model and create entities for the metadata objects they want to manage.
Typically the model captures technical attributes - like name, description, create time, number of replicas, etc; and
 metadata objects are created and updated by processes that monitor the real objects. It is often necessary to
augment technical attributes with additional attributes to capture business details that can help organize, search and
manage metadata entities. For example, a steward from marketing department can define set of attributes for a campaign,
and add these attributes to relevant metadata objects.
## Create Business Metadata
Business Metadata is a type supported by Atlas typesystem - similar to entity, enum, struct, classification types. A
business metadata type can have attributes of primitive type - similar to a struct type. In addition, each business
metadata attribute can be associated with a number of entity-types, like hive_db/hive_table/hbase_table.

<Img src={`/images/twiki/bm-create-01.png`}/>
<Img src={`/images/twiki/bm-create-02.png`}/>

## Add business attributes on entity instances
Once a business metadata attribute is associated with an entity-type, Apache Atlas allows values to be assigned to
entities - via UI and REST APIs.

<Img src={`/images/twiki/bm-entity-association.png`}/>

## Search for entities using business attributes
Apache Atlas enables finding entities based on values assigned to business attributes - via UI and REST APIs.

<Img src={`/images/twiki/bm-search-01.png`}/>
<Img src={`/images/twiki/bm-search-02.png`}/>

## Authorizations
Apache Atlas authorization has been updated to enable control on who can create business-metadata, and update business
attributes on entities. Apache Ranger authorization plugin has been updated to support policies for the same.

<Img src={`/images/twiki/bm-ranger-policies.png`}/>

## REST APIs
Apache Atlas supports REST APIs to create and update business metadata, add/update business attributes on entities and
find entities based on business entity attributes. Please refer to REST API documentation for more details.
