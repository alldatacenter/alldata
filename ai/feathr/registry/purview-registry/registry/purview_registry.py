import copy
from http.client import CONFLICT, HTTPException
import itertools
from typing import Any, Optional, Tuple, Union, List, Dict
from urllib.error import HTTPError
from uuid import UUID

from registry.models import to_snake
from pyapacheatlas.core.util import AtlasException

from azure.identity import DefaultAzureCredential
from loguru import logger
from pyapacheatlas.auth.azcredential import AzCredentialWrapper
from pyapacheatlas.core import (AtlasEntity, AtlasProcess,
                                PurviewClient)
from pyapacheatlas.core.typedef import (AtlasAttributeDef,Cardinality,EntityTypeDef)
from pyapacheatlas.core.util import GuidTracker
from pyhocon import ConfigFactory

from registry.interface import Registry
from registry.models import AnchorDef, AnchorFeatureDef, DerivedFeatureDef, Edge, EntitiesAndRelations, Entity, EntityRef, EntityType, ProjectDef, RelationshipType, SourceDef, Attributes, _to_uuid
Label_Contains = "CONTAINS"
Label_BelongsTo = "BELONGSTO"
Label_Consumes = "CONSUMES"
Label_Produces = "PRODUCES"
TYPEDEF_DERIVED_FEATURE="feathr_derived_feature_v1"
TYPEDEF_ANCHOR_FEATURE="feathr_anchor_feature_v1"

TYPEDEF_ARRAY_ANCHOR=f"array<feathr_anchor_v1>"
TYPEDEF_ARRAY_DERIVED_FEATURE=f"array<feathr_derived_feature_v1>"
TYPEDEF_ARRAY_ANCHOR_FEATURE=f"array<feathr_anchor_feature_v1>"

class ConflictError(Exception):
    pass

class PreconditionError(Exception):
    pass

class PurviewRegistry(Registry):
    def __init__(self,azure_purview_name: str, registry_delimiter: str = "__", credential=None,register_types = True):
        self.registry_delimiter = registry_delimiter
        self.azure_purview_name = azure_purview_name

        self.credential = DefaultAzureCredential(
            exclude_interactive_browser_credential=False) if credential is None else credential
        self.oauth = AzCredentialWrapper(credential=self.credential)
        self.purview_client = PurviewClient(
            account_name=self.azure_purview_name,
            authentication=self.oauth
        )
        self.guid = GuidTracker(starting=-1000)
        if register_types:
            self._register_feathr_feature_types()

    def get_projects(self) -> List[str]:
        """
        Returns the names of all projects
        """
        searchTerm = {"entityType": str(EntityType.Project)}
        result = self.purview_client.discovery.query(filter=searchTerm)
        result_entities = result['value']
        return [x['qualifiedName'] for x in result_entities]

    def get_projects_ids(self) -> Dict:
        """
        Returns the names and ids of all projects"""
        searchTerm = {"entityType": str(EntityType.Project)}
        result = self.purview_client.discovery.query(filter=searchTerm)
        result_entities = result['value']
        projects = {}
        for x in result_entities:
            projects[x['id']] = x['qualifiedName']
        return projects

    def get_entity(self, id_or_name: Union[str, UUID],recursive = False) -> Entity:
        id = self.get_entity_id(id_or_name)
        if not id:
            return None
        purview_entity =  self.purview_client.get_entity(id)['entities'][0]
        base_entity = self._atlasEntity_to_entity(purview_entity)  
        if recursive: 
            if base_entity.entity_type == EntityType.Project:
                edges = self.get_neighbors(base_entity.id, RelationshipType.Contains)
                ids = list([e.to_id for e in edges])
                children = self.get_entities(ids)
                base_entity.attributes.children = children
                return base_entity
            if base_entity.entity_type == EntityType.Anchor:
                conn = self.get_neighbors(base_entity.id, RelationshipType.Contains)
                feature_ids = [e.to_id for e in conn]
                features = self.get_entities(feature_ids)
                base_entity.attributes.features = features
                source_id = self.get_neighbors(
                    base_entity.id, RelationshipType.Consumes)[0].to_id
                source = self.get_entity(source_id)
                base_entity.attributes.source = source
                return base_entity
            if base_entity.entity_type == EntityType.DerivedFeature:
                conn = self.get_neighbors(base_entity.id, RelationshipType.Consumes)
                feature_ids = [e.to_id for e in conn]
                features = self.get_entities(feature_ids)
                base_entity.attributes.input_features = features
                return base_entity
        return base_entity

    def _atlasEntity_to_entity(self, purview_entity):
        entity_type = EntityType.new(purview_entity['typeName'])
        if entity_type in [EntityType.AnchorFeature,EntityType.DerivedFeature]:
            if "type" in purview_entity['attributes']:
                conf = ConfigFactory.parse_string(purview_entity['attributes']['type'])
                purview_entity['attributes']['type'] = dict(conf)
        base_entity =  Entity(
            purview_entity["guid"],
            purview_entity['attributes']["qualifiedName"],
            entity_type,
            attributes={x:y for x, y in purview_entity['attributes'].items() if y})
            
        return base_entity
                
    def get_entities(self, ids: List[UUID],recursive=False) -> List[Entity]:
        """
        Get list of entities by their ids
        """
        return [self.get_entity(x,recursive) for x in ids]

    def get_entity_id(self, id_or_name: Union[str, UUID]) -> UUID:
        try:
            id = _to_uuid(id_or_name)
            return id
        except ValueError:
            pass
        # It is a name
        return self._get_id_by_qualfiedName(id_or_name)
    
    def get_all_neighbours(self,id_or_name: Union[str, UUID]) -> List[Edge]:
        entity = self.get_entity(id_or_name)
        relation_lookup = {x.name.upper():x for x in RelationshipType}
        related_entities = self.purview_client.get_entity_lineage(str(entity.id),direction="BOTH")['guidEntityMap']        
        process_entities = [v for _,v in related_entities.items() if v['typeName']=="Process"]

        in_edges = []
        out_edges=[]
        result_edges = []
        for process_entity in process_entities:
            qualified_name_segs = process_entity['attributes']['qualifiedName'].split(self.registry_delimiter)
            if qualified_name_segs[2]==str(entity.id):
                in_edges.append(process_entity)
            elif qualified_name_segs[1] == str(entity.id):
                out_edges.append(process_entity)
        
        result_edges.extend([Edge(
            x['guid'],
            x['displayText'].split(' to ')[0],
            str(entity.id),
            relation_lookup[x['attributes']['qualifiedName'].split(self.registry_delimiter)[0]])
             for x in in_edges])
        result_edges.extend([Edge(
            x['guid'],
            str(entity.id),
            x['displayText'].split(' to ')[1],
            relation_lookup[x['attributes']['qualifiedName'].split(self.registry_delimiter)[0]])
             for x in out_edges])
        return result_edges

    def get_neighbors(self, id_or_name: Union[str, UUID], relationship: RelationshipType) -> List[Edge]:
        """
        Get list of edges with specified type that connect to this entity.
        The edge contains fromId and toId so we can follow to the entity it connects to
        """
        entity = self.get_entity(id_or_name)

        related_entities = self.purview_client.get_entity_lineage(str(entity.id),direction="BOTH")['guidEntityMap']        
        process_entities = [v for _,v in related_entities.items() if v['typeName']=="Process"]

        project_contain_process =\
        [x for x in process_entities \
            if x['attributes']['qualifiedName'].startswith(\
                str(relationship.name).upper()+self.registry_delimiter+str(entity.id))]
        
        edge_end_object = [related_entities[\
            x['displayText'].split(' to ')[1]] \
                for x in project_contain_process \
                    if x['displayText'].split(' to ')[1] in related_entities]
        
        result_edges = [Edge(x['guid'],str(entity.id),x['guid'],relationship) for x in edge_end_object]
        return result_edges

    def get_lineage(self, id_or_name: Union[str, UUID]) -> EntitiesAndRelations:
        """
        Get all the upstream and downstream entities of an entity, along with all edges connect them.
        Only meaningful to features and data sources.
        """
        id = self.get_entity_id(id_or_name)
        upstream_entities, upstream_edges = self._bfs(
            id, RelationshipType.Consumes)
        downstream_entities, downstream_edges = self._bfs(
            id, RelationshipType.Produces)
        return EntitiesAndRelations(
            upstream_entities + downstream_entities,
            upstream_edges + downstream_edges)
    
    def get_dependent_entities(self, entity_id: Union[str, UUID]) -> List[Entity]:
        """
        Given entity id, returns list of all entities that are downstream/dependent on given entity
        """
        entity_id = self.get_entity_id(entity_id)
        entity = self.get_entity(entity_id)
        downstream_entities = []
        if entity.entity_type == EntityType.Project:
            downstream_entities, _ = self._bfs(entity_id, RelationshipType.Contains)
        if entity.entity_type == EntityType.Source:
            downstream_entities, _ = self._bfs(entity_id, RelationshipType.Produces)
        if entity.entity_type == EntityType.Anchor:
            downstream_entities, _ = self._bfs(entity_id, RelationshipType.Contains)
        if entity.entity_type in (EntityType.AnchorFeature, EntityType.DerivedFeature):
            downstream_entities, _ = self._bfs(entity_id, RelationshipType.Produces)
        return [e for e in downstream_entities if str(e.id) != str(entity_id)]
    
    def delete_entity(self, entity_id: Union[str, UUID]):
        """
        Deletes given entity
        """
        entity_id = self.get_entity_id(entity_id)
        neighbors = self.get_all_neighbours(entity_id)
        edge_guids = [str(x.id) for x in neighbors]
        # Delete all edges associated with entity
        self.purview_client.delete_entity(edge_guids)
        #Delete entity
        self.purview_client.delete_entity(str(entity_id))

    def _get_edges(self, ids: List[UUID]) -> List[Edge]:
        all_edges = set()
        for id in ids:
            neighbours = self.get_all_neighbours(id)
            for neighbour in neighbours:
                if neighbour.from_id in ids \
                    and neighbour.to_id in ids:
                    all_edges.add(neighbour)
        return list(all_edges)
    
    def _create_edge_from_process(self, name:str, guid: str) -> Edge:
        names = name.split(self.registry_delimiter)
        return Edge(guid, names[1], names[2], RelationshipType.new(names[0]))

    def get_project_features(self, project:str, keywords:Optional[str] = None) -> List[Entity]:
        project_id = self.get_entity_id(project)
        if not project_id:
            return None
        guidAtlasEntityMap = self.purview_client.get_entity_lineage(project_id, depth=1, width=1, direction="OUTPUT")['guidEntityMap']
        atlasEntities = []

        for entity in guidAtlasEntityMap.values():
            type = entity['typeName']
            if type != "Process":
                type = EntityType.new(type)
                if type == EntityType.AnchorFeature or type == EntityType.DerivedFeature:
                    attributes = entity['attributes']
                    if not keywords or (attributes and keywords in attributes['qualifiedName']):
                        atlasEntity = self._atlasEntity_to_entity(entity)
                        atlasEntities.append(atlasEntity)
        return atlasEntities

    def get_project(self, id_or_name: Union[str, UUID]) -> EntitiesAndRelations:
        project_id = self.get_entity_id(id_or_name)
        if not project_id:
            return None
        lineage = self.purview_client.get_entity_lineage(project_id)
        guidAtlasEntityMap = lineage['guidEntityMap']
        guidEntityMap = {}
        finalGuidEntityMap = {}
        edges = []
        targetsRelationships = {
            EntityType.Project: RelationshipType.Contains.value,
            EntityType.Anchor: RelationshipType.Contains.value | RelationshipType.Consumes.value,
            EntityType.DerivedFeature: RelationshipType.Consumes.value
        }

        # Build edges and entities from guidEntityMap
        for id,entity in guidAtlasEntityMap.items():
            if entity['typeName'] == 'Process':
               name = entity['attributes']['qualifiedName']
               if not (name.startswith('BELONGSTO') and name.endswith(str(project_id))):
                    edges.append(self._create_edge_from_process(name, id))
            else:
                guidEntityMap[id] = self._atlasEntity_to_entity(entity)
            finalGuidEntityMap = copy.deepcopy(guidEntityMap)
        
        # Add relationships among each entity
        for edge in edges:
            edge_dict = edge.to_dict()
            relationship = edge_dict['relationshipTypeValue']
    
            fromId = edge_dict['fromEntityId']
            fromEntity = guidEntityMap[fromId]
            fromEntityType = EntityType.new(fromEntity.to_dict()['typeName'])
            if fromEntityType in targetsRelationships and targetsRelationships[fromEntityType] & relationship != 0:
                toId = edge_dict['toEntityId']
                toEntity = guidEntityMap[toId]
                toEntitytype = EntityType.new(toEntity.to_dict()['typeName'])
                if fromEntityType == EntityType.Project:
                    finalGuidEntityMap[fromId].attributes.children.append(toEntity)
                elif fromEntityType == EntityType.Anchor:
                    if toEntitytype == EntityType.Source:
                        finalGuidEntityMap[fromId].attributes.source = toEntity
                    else:
                        finalGuidEntityMap[fromId].attributes.features.append(toEntity)
                else:
                    curr_input_features = finalGuidEntityMap[fromId].attributes.input_features
                    curr_input_features.append(toEntity)
                    finalGuidEntityMap[fromId].attributes.input_features = curr_input_features
                        
        return EntitiesAndRelations(list(finalGuidEntityMap.values()), list(edges))

    def search_entity(self,
                      keyword: str,
                      type: List[EntityType],
                      project: Optional[Union[str, UUID]] = None) -> List[EntityRef]:
        """
        Search entities with specified type that also match the keyword in a project
        """
        query_result = self.purview_client.search_entities(keyword)
        result = []   
        for entity in query_result:
            qualified_name = entity["qualifiedName"]
            entity_id = entity['id']
            entity_type = entity['entityType']
            if type and entity_type in [str(x) for x in type]:
                if project:
                    if not (qualified_name.startswith(str(project)) or entity_id == str(project)):
                        continue
                result.append(EntityRef(UUID(entity_id),entity_type,qualified_name))
        return result

    def create_project(self, definition: ProjectDef) -> UUID:
        attrs = definition.to_attr().to_dict()
        feathr_project_entity = AtlasEntity(
            name=attrs['name'],
            qualified_name=attrs['name'],
            attributes=attrs['tags'],
            typeName=str(EntityType.Project),
            guid=self.guid.get_guid())

        self._upload_entity_batch([feathr_project_entity])
        return UUID(feathr_project_entity.guid)

    def create_project_datasource(self, project_id: UUID, definition: SourceDef) -> UUID:
        project_entity = self.get_entity(project_id)
        attrs = definition.to_attr().to_dict()
        qualified_name = self.registry_delimiter.join([project_entity.qualified_name,attrs['name']])
        source_entity = AtlasEntity(
            name=attrs['name'],
            qualified_name=qualified_name,
            attributes= {k:v for k,v in attrs.items() if k not in ['name','qualifiedName']},
            typeName=str(EntityType.Source),
            guid=self.guid.get_guid(),
        )
        self._upload_entity_batch(
            [source_entity])
        
        # change from AtlasEntity to Entity
        project_entity = self.get_entity(project_id)
        source_entity = self.get_entity(source_entity.guid)

        project_contains_source_relation = self._generate_relation_pairs(
            project_entity, source_entity, Label_Contains)
        self._upload_entity_batch(project_contains_source_relation)
        
        return source_entity.id

    def create_project_anchor(self, project_id: UUID, definition: AnchorDef) -> UUID:
        source_entity = self.get_entity(definition.source_id)
        attrs = definition.to_attr(source_entity).to_dict()
        project_entity = self.get_entity(project_id)
        qualified_name = self.registry_delimiter.join([project_entity.qualified_name,attrs['name']])
        anchor_entity = AtlasEntity(
            name=definition.name,
            qualified_name=qualified_name,
            attributes= {k:v for k,v in attrs.items() if k not in ['name','qualifiedName']},
            typeName=str(EntityType.Anchor),
            guid=self.guid.get_guid(),
        )

        self._upload_entity_batch(
            [anchor_entity])

        # change from AtlasEntity to Entity
        anchor_entity = self.get_entity(anchor_entity.guid)
        
        project_contains_anchor_relation = self._generate_relation_pairs(
            project_entity, anchor_entity, Label_Contains)
        anchor_consumes_source_relation = self._generate_relation_pairs(
            anchor_entity,source_entity, Label_Consumes)
        self._upload_entity_batch(
            project_contains_anchor_relation
            + anchor_consumes_source_relation)
        return anchor_entity.id

    def create_project_anchor_feature(self, project_id: UUID, anchor_id: UUID, definition: AnchorFeatureDef) -> UUID:
        attrs = definition.to_attr().to_dict()
        project_entity = self.get_entity(project_id)
        anchor_entity = self.get_entity(anchor_id,True)
        qualified_name = self.registry_delimiter.join([project_entity.qualified_name,
                                                       anchor_entity.attributes.name,
                                                        attrs['name']])

        anchor_feature_entity = AtlasEntity(
            name=definition.name,
            qualified_name=qualified_name,
            attributes= {k:v for k,v in attrs.items() if k not in ['name','qualifiedName']},
            typeName=str(EntityType.AnchorFeature),
            guid=self.guid.get_guid())
        self._upload_entity_batch(
            [anchor_feature_entity])

        # change from AtlasEntity to Entity
        anchor_feature_entity = self.get_entity(anchor_feature_entity.guid)
        source_entity = self.get_entity(anchor_entity.attributes.source.id)

        project_contains_feature_relation = self._generate_relation_pairs(
            project_entity, anchor_feature_entity, Label_Contains)
        anchor_contains_feature_relation = self._generate_relation_pairs(
            anchor_entity, anchor_feature_entity, Label_Contains)
        feature_consumes_source_relation = self._generate_relation_pairs(
            anchor_feature_entity, source_entity, Label_Consumes)

        self._upload_entity_batch(
            project_contains_feature_relation
            + anchor_contains_feature_relation
            + feature_consumes_source_relation)
        
        return anchor_feature_entity.id
        

    def create_project_derived_feature(self, project_id: UUID, definition: DerivedFeatureDef) -> UUID:
        input_features = self.get_entities(definition.input_anchor_features+definition.input_derived_features)
        attrs = definition.to_attr(input_features).to_dict()
        project_entity = self.get_entity(project_id)
        qualified_name = self.registry_delimiter.join([project_entity.qualified_name,attrs['name']])
        derived_feature_entity = AtlasEntity(
            name=definition.name,
            qualified_name=qualified_name,
            attributes={k:v for k,v in attrs.items() if k not in ['name','qualifiedName']},
            typeName=str(EntityType.DerivedFeature),
            guid=self.guid.get_guid())
        self._upload_entity_batch(
            [derived_feature_entity])
        
        # change from AtlasEntity to Entity
        project_entity = self.get_entity(project_id)
        derived_feature_entity = self.get_entity(derived_feature_entity.guid)

        feature_project_contain_belong_pairs = self._generate_relation_pairs(
            project_entity, derived_feature_entity, Label_Contains)

        consume_produce_pairs = []
        for input_feature in input_features:
            consume_produce_pairs += self._generate_relation_pairs(
                    derived_feature_entity, input_feature,Label_Consumes)

        self._upload_entity_batch(
            feature_project_contain_belong_pairs
            + consume_produce_pairs)
        
        return derived_feature_entity.id
    def _bfs(self, id: UUID, conn_type: RelationshipType) -> Tuple[List[Entity], List[Edge]]:
        """
        Breadth first traversal
        Starts from `id`, follow edges with `conn_type` only.

        WARN: There is no depth limit.
        """
        id_to_process = [id]
        entity_ids = [id]
        edges = []

        while len(id_to_process)!=0:
            outbound_edges = self._bfs_step(id_to_process,conn_type)
            edges += outbound_edges
            next_step_ids = list(set([x.to_id for x in outbound_edges]))
            entity_ids.extend(next_step_ids)
            entity_ids = list(set(entity_ids))
            id_to_process = next_step_ids
        
        entities = self.get_entities(entity_ids,True)
        return (entities,edges)

        
    
    def _bfs_step(self, ids: List[UUID], conn_type: RelationshipType) -> List[Edge]:
        """
        One step of the BFS process
        Returns all edges that connect to node ids the next step
        """
        return list(itertools.chain(*[self.get_neighbors(id,conn_type) for id in ids]))

            

    def _register_feathr_feature_types(self):
        """
         Register the feathr types if we haven't done so. Note that this only needs to be called once per provisioning
        a system. Basically this function registers all the feature type definition in a Atlas compatible system.
        """
        # Since old version of entity type definitions already exist, this method will not be called by default. 
        # Current schema is backward-compatible with existing. calling this method again will leads to "fail to delete def" error.
        # In the future, if moving to V2, call this method in registry initialization. 
        # Each feature is registered under a certain Feathr project. The project should what we refer to, however for backward compatibility, the type name would be `feathr_workspace`
        type_feathr_project = EntityTypeDef(
            name=str(EntityType.Project),
            attributeDefs=[
                # "anchor_features" and "derived_features" are removed, since we are moving to use process entity
                AtlasAttributeDef(
                    name="anchor_features", typeName=TYPEDEF_ARRAY_ANCHOR, cardinality=Cardinality.SET),
                AtlasAttributeDef(
                    name="derived_features", typeName=TYPEDEF_ARRAY_DERIVED_FEATURE, cardinality=Cardinality.SET),
                AtlasAttributeDef(name="tags", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
            ],
            superTypes=["DataSet"],

        )
        type_feathr_sources = EntityTypeDef(
            name=str(EntityType.Source),
            attributeDefs=[

                AtlasAttributeDef(
                    name="path", typeName="string", cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="event_timestamp_column",
                                  typeName="string", cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="timestamp_format",
                                  typeName="string", cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="type", typeName="string",
                                  cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="preprocessing", typeName="string",
                                  cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="tags", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
            ],
            superTypes=["DataSet"],
        )

        type_feathr_anchor_features = EntityTypeDef(
            name=str(EntityType.AnchorFeature),
            attributeDefs=[
                AtlasAttributeDef(name="type", typeName="string",
                                  cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="key", typeName="array<map<string,string>>",
                                  cardinality=Cardinality.SET),
                AtlasAttributeDef(name="transformation", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="tags", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
            ],
            superTypes=["DataSet"],
        )

        type_feathr_derived_features = EntityTypeDef(
            name=str(EntityType.DerivedFeature),
            attributeDefs=[
                AtlasAttributeDef(name="type", typeName="string",
                                  cardinality=Cardinality.SINGLE),
                # "input_anchor_features" and "input_derived_features" are deleted, use process entity instead
                AtlasAttributeDef(name="input_anchor_features", typeName=TYPEDEF_ARRAY_ANCHOR_FEATURE,
                                  cardinality=Cardinality.SET),
                AtlasAttributeDef(name="input_derived_features", typeName=TYPEDEF_ARRAY_DERIVED_FEATURE,
                                  cardinality=Cardinality.SET),
                AtlasAttributeDef(name="key", typeName="array<map<string,string>>",
                                  cardinality=Cardinality.SET),
                AtlasAttributeDef(name="transformation", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(name="tags", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
            ],
            superTypes=["DataSet"],
        )

        type_feathr_anchors = EntityTypeDef(
            name=str(EntityType.Anchor),
            attributeDefs=[
                # "source" will be removed, use process entity instead
                # "features" will be removed, use process entity instead
                AtlasAttributeDef(
                    name="source", typeName=str(EntityType.Source), cardinality=Cardinality.SINGLE),
                AtlasAttributeDef(
                    name="features", typeName=TYPEDEF_ARRAY_ANCHOR_FEATURE, cardinality=Cardinality.SET),
                AtlasAttributeDef(name="tags", typeName="map<string,string>",
                                  cardinality=Cardinality.SINGLE),
            ],
            superTypes=["DataSet"],
        )

        # Check if any of these type definitiones are already registered. 
        # If so this registration will be skipped.
        new_entitydefs = [type_feathr_anchor_features, type_feathr_anchors,
                        type_feathr_derived_features, type_feathr_sources, type_feathr_project]
        new_entitydefs_names = [entity.name for entity in new_entitydefs]
        existing_defs = self.purview_client.get_all_typedefs()
        existing_entitydefs_names = [entity['name'] for entity in existing_defs['entityDefs']] \
            if 'entityDefs' in existing_defs else []
        if existing_entitydefs_names \
            and any(name in new_entitydefs_names for name in existing_entitydefs_names):
            logger.warning(f"EntityTypeDefs Exists: {new_entitydefs_names}. Registration Skipped.")
            logger.info("Feathr Feature Type System Initialized.")
            return

        def_result = self.purview_client.upload_typedefs(
            entityDefs=new_entitydefs,
            force_update=True)
        logger.info("Feathr Feature Type System Initialized.")

    def _upload_entity_batch(self, entity_batch:List[AtlasEntity]):
        # we only support entity creation, update is not supported. 
        # setting lastModifiedTS ==0 will ensure this, if another entity with ts>=1 exist
        # upload function will fail with 412 Precondition fail.
        for entity in entity_batch:
            self._upload_single_entity(entity)
    
    def _upload_single_entity(self, entity:AtlasEntity):
        try:
            """
            Try to find existing entity/process first, if found, return the existing entity's GUID
            """
            response =  self.purview_client.get_entity(qualifiedName=entity.qualifiedName, typeName=entity.typeName)['entities'][0]
            j = entity.to_json()
            if j["typeName"] == response["typeName"]:
                if j["typeName"] == "Process":
                    if response["attributes"]["qualifiedName"] != j["attributes"]["qualifiedName"]:
                        raise ConflictError("The requested entity %s conflicts with the existing entity in PurView" % j["attributes"]["qualifiedName"])
                else:
                    if "type" in response['attributes'] and response["typeName"] in (TYPEDEF_ANCHOR_FEATURE, TYPEDEF_DERIVED_FEATURE):
                        conf = ConfigFactory.parse_string(response['attributes']['type'])
                        response['attributes']['type'] = dict(conf)
                    keys = set([to_snake(key) for key in j["attributes"].keys()]) - set(["qualified_name"])
                    keys.add("qualifiedName")
                    for k in keys:
                        if response["attributes"][k] != j["attributes"][k]:
                            raise ConflictError("The requested entity %s conflicts with the existing entity in PurView" % j["attributes"]["qualifiedName"])
                entity.guid = response["guid"]
                return
            else:
                raise ConflictError("The requested entity %s conflicts with the existing entity in PurView" % j["attributes"]["qualifiedName"])
        except AtlasException as e:
            pass
        except KeyError as e:
            # This is because the response is empty when the entity is not found
            pass

        entity.lastModifiedTS="0"
        results = None
        try:
            results = self.purview_client.upload_entities(
                batch=entity)
        except AtlasException as e:
            raise PreconditionError("Feature registration failed.", e)
        if results:
            d = {x.guid: x for x in [entity]}
            for k, v in results['guidAssignments'].items():
                d[k].guid = v
        else:
            raise PreconditionError("Feature registration failed.", results)
            
    def _generate_fully_qualified_name(self, segments):
        return self.registry_delimiter.join(segments)

    def _generate_relation_pairs(self, from_entity:Entity, to_entity:Entity, relation_type):
        type_lookup = {Label_Contains: Label_BelongsTo, Label_Consumes: Label_Produces}

        forward_relation =  AtlasProcess(
            name=str(from_entity.id) + " to " + str(to_entity.id),
            typeName="Process",
            qualified_name=self._generate_fully_qualified_name(
                [relation_type,str(from_entity.id), str(to_entity.id)]),
            inputs=[from_entity.to_min_repr()],
            outputs=[to_entity.to_min_repr()],
            guid=self.guid.get_guid())
        
        backward_relation = AtlasProcess(
            name=str(to_entity.id) + " to " + str(from_entity.id),
            typeName="Process",
            qualified_name=self._generate_fully_qualified_name(
                [type_lookup[relation_type], str(to_entity.id), str(from_entity.id)]),
            inputs=[to_entity.to_min_repr()],
            outputs=[from_entity.to_min_repr()],
            guid=self.guid.get_guid())
        return [forward_relation,backward_relation]
    
    def _get_id_by_qualfiedName(self, qualifiedName):
        """
        Get guid of a feature given its qualifiedName
        """        
        query_filter = {
            "attributeName": "qualifiedName",
            "operator": "eq",
            "attributeValue": qualifiedName
        }
        result = self.purview_client.discovery.query(keywords = None, filter=query_filter)
        entities = result['value']
        # There should be exactly one result, but we don't enforce the check here
        for entity in entities:
            if entity.get('qualifiedName') == qualifiedName:
                return entity.get('id')


