# Feathr Registry API Specifications

## Data Models

### EntityType
Type: Enum

| Value                       |
|-----------------------------|
| `feathr_workspace_v1`       |
| `feathr_source_v1`          |
| `feathr_anchor_v1`          |
| `feathr_anchor_feature_v1`  |
| `feathr_derived_feature_v1` |

### ValueType
Type: Enum

| Value         |
|---------------|
| `UNSPECIFIED` |
| `BOOL`        |
| `INT32`       |
| `INT64`       |
| `FLOAT`       |
| `DOUBLE`      |
| `STRING`      |
| `BYTES`       |

### VectorType
Type: Enum

| Value    |
|----------|
| `TENSOR` |

### TensorCategory
Type: Enum

| Value    |
|----------|
| `DENSE`  |
| `SPARSE` |

### FeatureType
Type: Object

| Field          | Type                                |
|----------------|-------------------------------------|
| type           | [`VectorType`](#valuetype)          |
| tensorCategory | [`TensorCategory`](#tensorcategory) |
| dimensionType  | [`array<ValueType>`](#valuetype)    |
| valType        | [`ValueType`](#valuetype)           |

### TypedKey
Type: Object

| Field            | Type                        |
|------------------|-----------------------------|
| key_column       | `string`                    |
| key_column_type  | [`ValueType`](#valuetype)   |
| full_name        | `string`, optional          |
| description      | `string`, optional          |
| key_column_alias | `string`, optional          |

### ExpressionTransformation
Type: Object

| Field          | Type     |
|----------------|----------|
| transform_expr | `string` |

### WindowAggregationTransformation
Type: Object

| Field    | Type               |
|----------|--------------------|
| def_expr | `string`           |
| agg_func | `string`, optional |
| window   | `string`, optional |
| group_by | `string`, optional |
| filter   | `string`, optional |
| limit    | `number`, optional |

### UdfTransformation
Type: Object

| Field | Type     |
|-------|----------|
| name  | `string` |

### EntityReference
Type: Object

| Field            | Type                        | Comments                             |
|------------------|-----------------------------|--------------------------------------|
| guid             | `Guid`                      |                                      |
| typeName         | [`EntityType`](#entitytype) |                                      |
| uniqueAttributes | `map<string, string>`       | Contains `qualifiedName` only so far |

### ProjectAttributes
Type: Object

| Field            | Type                                         |
|------------------|----------------------------------------------|
| qualifiedName    | `string`                                     |
| name             | `string`                                     |
| anchors          | [`array<EntityReference>`](#entityreference) |
| sources          | [`array<EntityReference>`](#entityreference) |
| anchor_features  | [`array<EntityReference>`](#entityreference) |
| derived_features | [`array<EntityReference>`](#entityreference) |
| tags             | `map<string, string>`                        |

### SourceAttributes
Type: Object

| Field                | Type                  |
|----------------------|-----------------------|
| qualifiedName        | `string`              |
| name                 | `string`              |
| path                 | `string`              |
| preprocessing        | `string`, optional    |
| eventTimestampColumn | `string`, optional    |
| timestampFormat      | `string`, optional    |
| type                 | `string`              |
| tags                 | `map<string, string>` |

### AnchorAttributes
Type: Object

| Field         | Type                                         |
|---------------|----------------------------------------------|
| qualifiedName | `string`                                     |
| name          | `string`                                     |
| features      | [`array<EntityReference>`](#entityreference) |
| source        | [`EntityReference`](#entityreference)        |
| tags          | `map<string, string>`                        |

### AnchorFeatureAttributes
Type: Object

| Field          | Type                           |
|----------------|--------------------------------|
| qualifiedName  | `string`                       |
| name           | `string`                       |
| type           | [`FeatureType`](#featuretype)  |
| transformation | [`ExpressionTransformation`](#expressiontransformation) <br/> `or` [`WindowAggregationTransformation`](#windowaggregationtransformation) <br/> `or` [`UdfTransformation`](#udftransformation) |
| key            | [`array<TypedKey>`](#typedkey) |
| tags           | `map<string, string>`          |

### DerivedFeatureAttributes
Type: Object

| Field                  | Type                           |
|------------------------|--------------------------------|
| qualifiedName          | `string`                       |
| name                   | `string`                       |
| type                   | [`FeatureType`](#featuretype)  |
| transformation         | [`ExpressionTransformation`](#expressiontransformation) <br/> `or` [`WindowAggregationTransformation`](#windowaggregationtransformation) <br/> `or` [`UdfTransformation`](#udftransformation) |
| key                    | [`array<TypedKey>`](#typedkey) |
| input_anchor_features  | [`array<EntityReference>`](#entityreference) |
| input_derived_features | [`array<EntityReference>`](#entityreference) |
| tags                   | `map<string, string>`          |

### EntityStatus
Type: Enum

| Value    |
|----------|
| `ACTIVE` |

### Entity
Type: Object

| Field          | Type                            |
|----------------|---------------------------------|
| guid           | `Guid`                          |
| lastModifiedTS | `string`                        |
| status         | [`EntityStatus`](#entitystatus) |
| displayText    | `string`                        |
| typeName       | [`EntityType`](#entitytype)     |
| attributes     | [`ProjectAttributes`](#projectattributes) <br/> `or` [`SourceAttributes`](#sourceattributes) <br/> `or` [`AnchorAttributes`](#anchorattributes) <br/> `or` [`AnchorFeatureAttributes`](#anchorfeatureattributes) <br/> `or` [`DerivedFeatureAttributes`](#derivedfeatureattributes) |

### RelationshipType
Type: Enum

| Value       |
|-------------|
| `BelongsTo` |
| `Contains`  |
| `Produces`  |
| `Consumes`  |

### Relationship
Type: Object

| Field            | Type                                    |
|------------------|-----------------------------------------|
| relationshipId   | `Guid`                                  |
| relationshipType | [`RelationshipType`](#relationshiptype) |
| fromEntityId     | `Guid`                                  |
| toEntityId       | `Guid`                                  |

### ProjectDefinition
Type: Object

| Field                | Type                  |
|----------------------|-----------------------|
| qualifiedName        | `string`              |
| tags                 | `map<string, string>` |


### SourceDefinition
Type: Object

| Field                | Type                  |
|----------------------|-----------------------|
| qualifiedName        | `string`              |
| name                 | `string`              |
| path                 | `string`              |
| preprocessing        | `string`, optional    |
| eventTimestampColumn | `string`, optional    |
| timestampFormat      | `string`, optional    |
| type                 | `string`              |
| tags                 | `map<string, string>` |

### AnchorDefinition
Type: Object

| Field                | Type                  |
|----------------------|-----------------------|
| qualifiedName        | `string`              |
| name                 | `string`              |
| source_id            | `Guid`                |
| tags                 | `map<string, string>` |

### AnchorFeatureDefinition
Type: Object

| Field          | Type                           |
|----------------|--------------------------------|
| qualifiedName  | `string`                       |
| name           | `string`                       |
| featureType    | [`FeatureType`](#featuretype)  |
| transformation | [`ExpressionTransformation`](#expressiontransformation) <br/> `or` [`WindowAggregationTransformation`](#windowaggregationtransformation) <br/> `or` [`UdfTransformation`](#udftransformation) |
| key            | [`array<TypedKey>`](#typedkey) |
| tags           | `map<string, string>`          |

### DerivedFeatureDefinition
Type: Object

| Field                  | Type                           |
|------------------------|--------------------------------|
| qualifiedName          | `string`                       |
| name                   | `string`                       |
| featureType            | [`FeatureType`](#featuretype)  |
| transformation         | [`ExpressionTransformation`](#expressiontransformation) <br/> `or` [`WindowAggregationTransformation`](#windowaggregationtransformation) <br/> `or` [`UdfTransformation`](#udftransformation) |
| key                    | [`array<TypedKey>`](#typedkey) |
| input_anchor_features  | `array<Guid>`                  |
| input_derived_features | `array<Guid>`                  |
| tags                   | `map<string, string>`          |


### EntitiesAndRelationships
Type: Object

| Field         | Type                                   |
|---------------|----------------------------------------|
| guidEntityMap | [`map<Guid, Entity>`](#entity)         |
| relations     | [`array<Relationship>`](#relationship) |


## Feathr Registry API

### `GET /projects`
List **names** of all projects.

Response Type: `array<string>`

### `GET /projects-ids`
Dictionary of **id** to **names** mapping of all projects.

Response Type: `dict`

### `GET /projects/{project}`
Get everything defined in the project

### `GET /dependent/{entity}`
Gets downstream/dependent entities for given entity

Response Type: [`EntitiesAndRelationships`](#entitiesandrelationships)

### `GET /projects/{project}/datasources`
Get all sources defined in the project.

Response Type: [`array<Entity>`](#entity)

### `GET /projects/{project}/features`
Get all anchor features and derived features in the project, or only features meet the search criteria in the project.

Query Parameters:

| Field   | Type   |
|---------|--------|
| keyword | string |
| size    | number |
| offset  | number |


Response Type: Object

| Field    | Type                       |
|----------|----------------------------|
| features | [`array<Entity>`](#entity) |

### `GET /features/:feature`
Get feature details.

Response Type: Object

| Field           | Type                  | Comments                    |
|-----------------|-----------------------|-----------------------------|
| entity          | [`Entity`](#entity)   |                             |
| referredEntities| `map<string, object>` | For compatibility, not used |

### `DELETE /entity/{entity}`
Deletes entity

### `POST /projects`
Create new project

+ Request Type: [`ProjectDefinition`](#projectdefinition)
+ Response Type: Object

| Field | Type |
|-------|------|
| guid  | Guid |

### `POST /projects/{project}/datasources`
Create new source in the project

+ Request Type: [`SourceDefinition`](#sourcedefinition)
+ Response Type: Object

| Field | Type |
|-------|------|
| guid  | Guid |

### `POST /projects/{project}/anchors`
Create new anchor in the project

+ Request Type: [`AnchorDefinition`](#anchordefinition)
+ Response Type: Object

| Field | Type |
|-------|------|
| guid  | Guid |

### `POST /projects/{project}/anchors/{anchor}/features`
Create new anchor feature in the project under specified anchor

+ Request Type: [`AnchorFeatureDefinition`](#anchorfeaturedefinition)
+ Response Type: Object

| Field | Type |
|-------|------|
| guid  | Guid |

### `POST /projects/{project}/derivedfeatures`
Create new derived feature in the project

+ Request Type: [`DerivedFeatureDefinition`](#derivedfeaturedefinition)
+ Response Type: Object

| Field | Type |
|-------|------|
| guid  | Guid |
