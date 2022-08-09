---
name: Notifications
route: /Notifications
menu: Documentation
submenu: Features
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Notifications

## Notifications from Apache Atlas
Apache Atlas sends notifications about metadata changes to Kafka topic named ATLAS_ENTITIES .
Applications interested in metadata changes can monitor for these notifications.
For example, Apache Ranger processes these notifications to authorize data access based on classifications.


### Notifications - V2: Apache Atlas version 1.0
Apache Atlas 1.0 sends notifications for following operations on metadata.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
   {`ENTITY_CREATE:         sent when an entity instance is created
      ENTITY_UPDATE:         sent when an entity instance is updated
      ENTITY_DELETE:         sent when an entity instance is deleted
      CLASSIFICATION_ADD:    sent when classifications are added to an entity instance
      CLASSIFICATION_UPDATE: sent when classifications of an entity instance are updated
      CLASSIFICATION_DELETE: sent when classifications are removed from an entity instance`}
 </SyntaxHighlighter>

Notification includes the following data.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`AtlasEntity  entity;
   OperationType operationType;
   List<AtlasClassification>  classifications;`}
</SyntaxHighlighter>

### Notifications - V1: Apache Atlas version 0.8.x and earlier
Notifications from Apache Atlas version 0.8.x and earlier have content formatted differently, as detailed below.

__*Operations*__

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
   {`ENTITY_CREATE: sent when an entity instance is created
      ENTITY_UPDATE: sent when an entity instance is updated
      ENTITY_DELETE: sent when an entity instance is deleted
      TRAIT_ADD:     sent when classifications are added to an entity instance
      TRAIT_UPDATE:  sent when classifications of an entity instance are updated
      TRAIT_DELETE:  sent when classifications are removed from an entity instance`}
</SyntaxHighlighter>

Notification includes the following data.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`Referenceable entity;
   OperationType operationType;
   List<Struct>  traits;`}
</SyntaxHighlighter>

Apache Atlas 1.0 can be configured to send notifications in older version format, instead of the latest version format.
This can be helpful in deployments that are not yet ready to process notifications in latest version format.
To configure Apache Atlas 1.0 to send notifications in earlier version format, please set following configuration in atlas-application.properties:

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
 atlas.notification.entity.version=v1
</SyntaxHighlighter>

## Notifications to Apache Atlas
Apache Atlas can be notified of metadata changes and lineage via notifications to Kafka topic named ATLAS_HOOK.
Atlas hooks for Apache Hive/Apache HBase/Apache Storm/Apache Sqoop use this mechanism to notify Apache Atlas of events of interest.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`ENTITY_CREATE            : create an entity. For more details, refer to Java class HookNotificationV1.EntityCreateRequest
ENTITY_FULL_UPDATE       : update an entity. For more details, refer to Java class HookNotificationV1.EntityUpdateRequest
ENTITY_PARTIAL_UPDATE    : update specific attributes of an entity. For more details, refer to HookNotificationV1.EntityPartialUpdateRequest
ENTITY_DELETE            : delete an entity. For more details, refer to Java class HookNotificationV1.EntityDeleteRequest
ENTITY_CREATE_V2         : create an entity. For more details, refer to Java class HookNotification.EntityCreateRequestV2
ENTITY_FULL_UPDATE_V2    : update an entity. For more details, refer to Java class HookNotification.EntityUpdateRequestV2
ENTITY_PARTIAL_UPDATE_V2 : update specific attributes of an entity. For more details, refer to HookNotification.EntityPartialUpdateRequestV2
ENTITY_DELETE_V2         : delete one or more entities. For more details, refer to Java class HookNotification.EntityDeleteRequestV2`}
</SyntaxHighlighter>




