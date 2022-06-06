---
name: Overview
route: /
menu: Overview
---

# Overview

Atlas is a scalable and extensible set of core foundational governance services – enabling
enterprises to effectively and efficiently meet their compliance requirements within Hadoop and
allows integration with the whole enterprise data ecosystem.

Apache Atlas provides open metadata management and governance capabilities for organizations
to build a catalog of their data assets, classify and govern these assets and provide collaboration
capabilities around these data assets for data scientists, analysts and the data governance team.


## Features

### Metadata types & instances
   * Pre-defined types for various Hadoop and non-Hadoop metadata
   * Ability to define new types for the metadata to be managed
   * Types can have primitive attributes, complex attributes, object references; can inherit from other types
   * Instances of types, called entities, capture metadata object details and their relationships
   * REST APIs to work with types and instances allow easier integration

### Classification
   * Ability to dynamically create classifications - like PII, EXPIRES_ON, DATA_QUALITY, SENSITIVE
   * Classifications can include attributes - like expiry_date attribute in EXPIRES_ON classification
   * Entities can be associated with multiple classifications, enabling easier discovery and security enforcement
   * Propagation of classifications via lineage - automatically ensures that classifications follow the data as it goes through various processing

### Lineage
   * Intuitive UI to view lineage of data as it moves through various processes
   * REST APIs to access and update lineage

### Search/Discovery
   * Intuitive UI to search entities by type, classification, attribute value or free-text
   * Rich REST APIs to search by complex criteria
   * SQL like query language to search entities - Domain Specific Language (DSL)

### Security & Data Masking
   * Fine grained security for metadata access, enabling controls on access to entity instances and operations like add/update/remove classifications
   * Integration with Apache Ranger enables authorization/data-masking on data access based on classifications associated with entities in Apache Atlas. For example:
      * who can access data classified as PII, SENSITIVE
      * customer-service users can only see last 4 digits of columns classified as NATIONAL_ID


## Getting Started

   * [What's new in Apache Atlas 2.2?](#/WhatsNew-2.2)
   * [Build & Install](#/Installation)
   * [Quick Start](#/QuickStart)

## API Documentation

   * <a href="api/v2/index.html">REST API Documentation</a>
   * [Export & Import REST API Documentation](#/ImportExportAPI)
   * <a href="../api/rest.html">Legacy API Documentation</a>

## Developer Setup Documentation
   * [Developer Setup: Eclipse](#/EclipseSetup)
