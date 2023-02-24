# Apache Atlas Python Client

Python library for Apache Atlas.

## Installation

Use the package manager [pip](https://pip.pypa.io/en/stable/) to install Python client for Apache Atlas.

```bash
> pip install apache-atlas
```

Verify if apache-atlas client is installed:
```bash
> pip list

Package      Version
------------ ---------
apache-atlas 0.0.12
```

## Usage

```python atlas_example.py```
```python
# atlas_example.py

import time

from apache_atlas.client.base_client import AtlasClient
from apache_atlas.model.instance     import AtlasEntity, AtlasEntityWithExtInfo, AtlasEntitiesWithExtInfo, AtlasRelatedObjectId
from apache_atlas.model.enums        import EntityOperation


## Step 1: create a client to connect to Apache Atlas server
client = AtlasClient('http://localhost:21000', ('admin', 'atlasR0cks!'))

# For Kerberos authentication, use HTTPKerberosAuth as shown below
#
# from requests_kerberos import HTTPKerberosAuth
#
# client = AtlasClient('http://localhost:21000', HTTPKerberosAuth())

# to disable SSL certificate validation (not recommended for production use!)
#
# client.session.verify = False


## Step 2: Let's create a database entity
test_db            = AtlasEntity({ 'typeName': 'hive_db' })
test_db.attributes = { 'name': 'test_db', 'clusterName': 'prod', 'qualifiedName': 'test_db@prod' }

entity_info        = AtlasEntityWithExtInfo()
entity_info.entity = test_db

print('Creating test_db')

resp = client.entity.create_entity(entity_info)

guid_db = resp.get_assigned_guid(test_db.guid)

print('    created test_db: guid=' + guid_db)


## Step 3: Let's create a table entity, and two column entities - in one call
test_tbl                        = AtlasEntity({ 'typeName': 'hive_table' })
test_tbl.attributes             = { 'name': 'test_tbl', 'qualifiedName': 'test_db.test_tbl@prod' }
test_tbl.relationshipAttributes = { 'db': AtlasRelatedObjectId({ 'guid': guid_db }) }

test_col1                        = AtlasEntity({ 'typeName': 'hive_column' })
test_col1.attributes             = { 'name': 'test_col1', 'type': 'string', 'qualifiedName': 'test_db.test_tbl.test_col1@prod' }
test_col1.relationshipAttributes = { 'table': AtlasRelatedObjectId({ 'guid': test_tbl.guid }) }

test_col2                        = AtlasEntity({ 'typeName': 'hive_column' })
test_col2.attributes             = { 'name': 'test_col2', 'type': 'string', 'qualifiedName': 'test_db.test_tbl.test_col2@prod' }
test_col2.relationshipAttributes = { 'table': AtlasRelatedObjectId({ 'guid': test_tbl.guid }) }

entities_info          = AtlasEntitiesWithExtInfo()
entities_info.entities = [ test_tbl, test_col1, test_col2 ]

print('Creating test_tbl')

resp = client.entity.create_entities(entities_info)

guid_tbl  = resp.get_assigned_guid(test_tbl.guid)
guid_col1 = resp.get_assigned_guid(test_col1.guid)
guid_col2 = resp.get_assigned_guid(test_col2.guid)

print('    created test_tbl:           guid=' + guid_tbl)
print('    created test_tbl.test_col1: guid=' + guid_col1)
print('    created test_tbl.test_col2: guid=' + guid_col2)


## Step 4: Let's create a view entity that feeds from the table created earlier
#          Also create a lineage between the table and the view, and lineages between their columns as well
test_view                        = AtlasEntity({ 'typeName': 'hive_table' })
test_view.attributes             = { 'name': 'test_view', 'qualifiedName': 'test_db.test_view@prod' }
test_view.relationshipAttributes = { 'db': AtlasRelatedObjectId({ 'guid': guid_db }) }

test_view_col1                        = AtlasEntity({ 'typeName': 'hive_column' })
test_view_col1.attributes             = { 'name': 'test_col1', 'type': 'string', 'qualifiedName': 'test_db.test_view.test_col1@prod' }
test_view_col1.relationshipAttributes = { 'table': AtlasRelatedObjectId({ 'guid': test_view.guid }) }

test_view_col2                        = AtlasEntity({ 'typeName': 'hive_column' })
test_view_col2.attributes             = { 'name': 'test_col2', 'type': 'string', 'qualifiedName': 'test_db.test_view.test_col2@prod' }
test_view_col2.relationshipAttributes = { 'table': AtlasRelatedObjectId({ 'guid': test_view.guid }) }

test_process                         = AtlasEntity({ 'typeName': 'hive_process' })
test_process.attributes              = { 'name': 'create_test_view', 'userName': 'admin', 'operationType': 'CREATE', 'qualifiedName': 'create_test_view@prod' }
test_process.attributes['queryText'] = 'create view test_view as select * from test_tbl'
test_process.attributes['queryPlan'] = '<queryPlan>'
test_process.attributes['queryId']   = '<queryId>'
test_process.attributes['startTime'] = int(time.time() * 1000)
test_process.attributes['endTime']   = int(time.time() * 1000)
test_process.relationshipAttributes  = { 'inputs': [ AtlasRelatedObjectId({ 'guid': guid_tbl }) ], 'outputs': [ AtlasRelatedObjectId({ 'guid': test_view.guid }) ] }

test_col1_lineage                        = AtlasEntity({ 'typeName': 'hive_column_lineage' })
test_col1_lineage.attributes             = { 'name': 'test_view.test_col1 lineage', 'depenendencyType': 'read', 'qualifiedName': 'test_db.test_view.test_col1@prod' }
test_col1_lineage.attributes['query']    = { 'guid': test_process.guid }
test_col1_lineage.relationshipAttributes = { 'inputs': [ AtlasRelatedObjectId({ 'guid': guid_col1 }) ], 'outputs': [ AtlasRelatedObjectId({ 'guid': test_view_col1.guid }) ] }

test_col2_lineage                        = AtlasEntity({ 'typeName': 'hive_column_lineage' })
test_col2_lineage.attributes             = { 'name': 'test_view.test_col2 lineage', 'depenendencyType': 'read', 'qualifiedName': 'test_db.test_view.test_col2@prod' }
test_col2_lineage.attributes['query']    = { 'guid': test_process.guid }
test_col2_lineage.relationshipAttributes = { 'inputs': [ AtlasRelatedObjectId({ 'guid': guid_col2 }) ], 'outputs': [ AtlasRelatedObjectId({ 'guid': test_view_col2.guid }) ] }

entities_info          = AtlasEntitiesWithExtInfo()
entities_info.entities = [ test_process, test_col1_lineage, test_col2_lineage ]

entities_info.add_referenced_entity(test_view)
entities_info.add_referenced_entity(test_view_col1)
entities_info.add_referenced_entity(test_view_col2)

print('Creating test_view')

resp = client.entity.create_entities(entities_info)

guid_view         = resp.get_assigned_guid(test_view.guid)
guid_view_col1    = resp.get_assigned_guid(test_view_col1.guid)
guid_view_col2    = resp.get_assigned_guid(test_view_col2.guid)
guid_process      = resp.get_assigned_guid(test_process.guid)
guid_col1_lineage = resp.get_assigned_guid(test_col1_lineage.guid)
guid_col2_lineage = resp.get_assigned_guid(test_col2_lineage.guid)

print('    created test_view:           guid=' + guid_view)
print('    created test_view.test_col1: guid=' + guid_view_col1)
print('    created test_view.test_col2: guid=' + guid_view_col1)
print('    created test_view lineage:   guid=' + guid_process)
print('    created test_col1 lineage:   guid=' + guid_col1_lineage)
print('    created test_col2 lineage:   guid=' + guid_col2_lineage)


## Step 5: Finally, cleanup by deleting entities created above
print('Deleting entities')

resp = client.entity.delete_entities_by_guids([ guid_col1_lineage, guid_col2_lineage, guid_process, guid_view, guid_tbl, guid_db ])

deleted_count = len(resp.mutatedEntities[EntityOperation.DELETE.name]) if resp and resp.mutatedEntities and EntityOperation.DELETE.name in resp.mutatedEntities else 0

print('    ' + str(deleted_count) + ' entities deleted')
```
For more examples, checkout `sample-app` python project in [atlas-examples](https://github.com/apache/atlas/blob/master/atlas-examples/sample-app/src/main/python/sample_client.py) module.
