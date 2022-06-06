---
name: Authorization Model
route: /AuthorizationModel
menu: Documentation
submenu: Security
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';


# Atlas Authorization Model


##  Introduction

Atlas is a scalable and extensible set of core foundational governance services – enabling enterprises to effectively and
efficiently meet their compliance requirements within Hadoop and allows integration with the whole enterprise data ecosystem.
Apache Atlas provides open metadata management and governance capabilities for organizations to build a catalog of their
data assets, classify and govern these assets and provide collaboration capabilities around these data assets for data
scientists, analysts and the data governance team.

This document covers details of the authorization model supported by Apache Atlas to control access to metadata managed by Atlas.

## Authorization of access to Types
Apache Atlas provides a type system that allows users to model the metadata objects they would like to manage. The model
is composed of definitions called ‘types’. Apache Atlas type system supports following categories of types:
   * Entity
   * Classification
   * Relationship
   * Struct
   * Enum

The authorization model enables control of which users, groups can perform the following operations on types, based on
type names and type categories:
   * create
   * update
   * delete

Here are few examples of access controls supported by the model:
   * Admin users can create/update/delete types of all categories
   * Data stewards can create/update/delete classification types
   * Healthcare data stewards can create/update/delete types having names start with “hc”


## Authorization of access to Entities
An entity is an instance of an entity-type and such instances represent objects in the real world – for example a table
in Hive, a HDFS file, a Kafka topic. The authorization model enables control of which users, groups can perform the
following operations on entities – based on entity-types, entity-classifications, entity-id:

   * read
   * create
   * update
   * delete
   * read classification
   * add classification
   * update classification
   * remove classification

Here are few examples of access controls supported by the model:
   * Admin users can perform all entity operations on entities of all types
   * Data stewards can perform all entity operations, except delete, on entities of all types
   * Data quality admins can add/update/remove DATA_QUALITY classification
   * Users in specific groups can read/update entities with PII classification or its sub-classification
   * Finance users can read/update entities whose ID start with ‘finance’

##  Authorization of Admin operations
The authorization model enables control of which users, groups can perform the following administrative operations:
   * *import entities*
   * *export entities*

Users with above accesses can import/export entities without requiring them to be granted with fine-grained entity level accesses.

## Pluggable Authorization
Apache Atlas supports a pluggable authorization interface, as shown below, that enable alternate implementations to handle authorizations.

The name of the class implementing the authorization interface can be registered with Apache Atlas using configuration `atlas.authorizer.impl`. When this property is not set, Apache Atlas will use its default implementation in `org.apache.atlas.authorize.simple.AtlasSimpleAuthorizer`.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`
   package org.apache.atlas.authorize;

   public interface AtlasAuthorizer {
     void init();

         void cleanUp();

         boolean isAccessAllowed(AtlasAdminAccessRequest request) throws AtlasAuthorizationException;

         boolean isAccessAllowed(AtlasEntityAccessRequest request) throws AtlasAuthorizationException;

         boolean isAccessAllowed(AtlasTypeAccessRequest request) throws AtlasAuthorizationException;
   }`}
</SyntaxHighlighter>

## Simple Authorizer

Simple authorizer is the default authorizer implementation included in Apache Atlas. For details of setting up Apache Atlas
to use simple authorizer, please see [Setting up Atlas to use Simple Authorizer](#/AtlasSimpleAuthorizer)

## Ranger Authorizer

To configure Apache Atlas to use authorization implementation provided by Apache Ranger, include the following property
in application.properties config file:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authorizer.impl=ranger`}
</SyntaxHighlighter>

Apache Ranger Authorizer requires configuration files to be setup, for example to specify Apache Ranger admin server URL,
name of the service containing authorization policies, etc. For more details please see, [Setting up Atlas to use Ranger Authorizer](#/AtlasRangerAuthorizer).


##  None authorizer
In addition to the default authorizer, Apache Atlas includes an authorizer that permits all accesses to all users. This authorizer can be useful in test environments and unit tests. To use this authorizer, set the following configuration:
<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`atlas.authorizer.impl=NONE`}
</SyntaxHighlighter>
