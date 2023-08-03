from typing import Optional, Tuple, Union
from uuid import UUID, uuid4
from typing import List, Set, Dict
from pydantic import UUID4
from registry import Registry
from registry import connect
from registry.models import AnchorAttributes, AnchorDef, AnchorFeatureAttributes, AnchorFeatureDef, DerivedFeatureAttributes, DerivedFeatureDef, Edge, EntitiesAndRelations, Entity, EntityRef, EntityType, ProjectAttributes, ProjectDef, RelationshipType, SourceAttributes, SourceDef, _to_type, _to_uuid
import json
import os
class ConflictError(Exception):
    pass


# This is a temp way so that the Feathr sandbox code can reuse existing code as much as possible
# Eventually we might want to move to ORM based SQL access ways, but need to make sure it also works well in multi-threading
# Currently the ORM based access way is only used in Sandbox so it's safe

from sqlalchemy.orm import Session
from sqlalchemy.orm.query import Query
from sqlalchemy import and_
import sqlalchemy as db
from sqlalchemy.ext.declarative import declarative_base
Base = declarative_base()
class Entities(Base):

    __tablename__ = 'entities'

    entity_id = db.Column('entity_id', db.String(50),nullable=False, primary_key=True)
    qualified_name =  db.Column('qualified_name', db.String(200), nullable=False)
    entity_type = db.Column('entity_type', db.String(100),nullable=False)
    attributes = db.Column('attributes', db.String(2000), nullable=False)

class Edges(Base):

    __tablename__ = 'edges'

    edge_id = db.Column('edge_id', db.String(50),nullable=False, primary_key=True)
    from_id =  db.Column('from_id', db.String(50), nullable=False)
    to_id = db.Column('to_id', db.String(20), nullable=False)
    conn_type = db.Column('conn_type', db.String(20), nullable=False) 


def quote(id):
    if isinstance(id, str):
        return f"'{id}'"
    if isinstance(id, UUID):
        return f"'{str(id)}'"
    else:
        return ",".join([quote(i) for i in id])

class DbRegistry(Registry):
    def __init__(self):
        self.conn = connect()
        if os.environ.get("FEATHR_SANDBOX"):
            sandbox_registry_url = os.environ.get("FEATHR_SANDBOX_REGISTRY_URL")
            if sandbox_registry_url:
                print(f"FEATHR_SANDBOX_REGISTRY_URL is set to {sandbox_registry_url}. Please refer to https://docs.sqlalchemy.org/en/20/core/engines.html#database-urls for how to construct the URLs.")
                engine = db.create_engine(sandbox_registry_url)
            else:
                engine = db.create_engine('sqlite:////tmp/feathr_registry.sqlite?check_same_thread=False') #Create test.sqlite automatically
            self.sql_session = Session(engine)
            self.connection = engine.connect()
            metadata = db.MetaData()


            self.entities_table = db.Table('entities', metadata,
                        db.Column('entity_id', db.String(50),nullable=False, primary_key=True),
                        db.Column('qualified_name', db.String(200), nullable=False),
                        db.Column('entity_type', db.String(100),nullable=False),
                        db.Column('attributes', db.String(2000), nullable=False) #TODO: sqlite doesn't enforce length but others might
                        )
            self.edges_table = db.Table('edges', metadata,
                        db.Column('edge_id', db.String(50),nullable=False, primary_key=True),
                        db.Column('from_id', db.String(50), nullable=False),
                        db.Column('to_id', db.String(20), nullable=False),
                        db.Column('conn_type', db.String(20), nullable=False) 
                        )
            metadata.create_all(engine) #Creates the table
    def _fetch_helper(self, query):
        """serves as a function to have max code similarity between the ORM based code and the SQL based code. Basically fetch all and return a dict (otherwise it might just return a list of `LegacyRow` object)
        """
        # 
        
        if isinstance(query, Query):
            # if this is already a query object, execute it
            r = query.all()
        else:
            # otherwise need a session to execute it
            r = self.connection.execute(query).fetchall()
        r = [ele._asdict() for ele in r]
        return r

    def get_projects(self) -> List[str]:
        if os.environ.get("FEATHR_SANDBOX"):
            query = db.select(self.entities_table.c.qualified_name).where((self.entities_table.c.entity_type == str(EntityType.Project))) 
            ret = self._fetch_helper(query)
        else:
            ret = self.conn.query(
            f"select qualified_name from entities where entity_type=%s", str(EntityType.Project))
        return list([r["qualified_name"] for r in ret])
    
    def get_projects_ids(self) -> Dict:
        projects = {}
        if os.environ.get("FEATHR_SANDBOX"):
            query = db.select(self.entities_table.c.entity_id,self.entities_table.c.qualified_name).where((self.entities_table.c.entity_type == str(EntityType.Project))) 
            ret = self._fetch_helper(query)
        else:
            ret = self.conn.query(
            f"select entity_id, qualified_name from entities where entity_type=%s", str(EntityType.Project))
        for r in ret:
            projects[r['entity_id']] = r['qualified_name']
        return projects

    def get_entity(self, id_or_name: Union[str, UUID]) -> Entity:
        return self._fill_entity(self._get_entity(id_or_name))

    def get_entities(self, ids: List[UUID]) -> List[Entity]:
        return list([self._fill_entity(e) for e in self._get_entities(ids)])

    def get_entity_id(self, id_or_name: Union[str, UUID]) -> UUID:
        try:
            id = _to_uuid(id_or_name)
            return id
        except ValueError:
            pass
        # It is a name
        if os.environ.get("FEATHR_SANDBOX"):
            query = db.select(self.entities_table.c.entity_id).where((self.entities_table.c.qualified_name == str(id_or_name))) 
            ret = self._fetch_helper(query)
        else:
            ret = self.conn.query(
            f"select entity_id from entities where qualified_name=%s", str(id_or_name))
        if len(ret) == 0:
            raise KeyError(f"Entity {id_or_name} not found")
        return ret[0]["entity_id"]

    def get_neighbors(self, id_or_name: Union[str, UUID], relationship: RelationshipType) -> List[Edge]:
        if os.environ.get("FEATHR_SANDBOX"):
            query = db.select(self.edges_table.c.edge_id, self.edges_table.c.from_id, self.edges_table.c.to_id,self.edges_table.c.conn_type).where((self.edges_table.c.from_id == str(self.get_entity_id(id_or_name))) & (self.edges_table.c.conn_type == relationship.name)) 
            rows = self._fetch_helper(query)
        else:
            rows = self.conn.query(fr'''
            select edge_id, from_id, to_id, conn_type
            from edges
            where from_id = %s
            and conn_type = %s
        ''', (str(self.get_entity_id(id_or_name)), relationship.name))
        return list([Edge(**row) for row in rows])

    def get_lineage(self, id_or_name: Union[str, UUID]) -> EntitiesAndRelations:
        """
        Get feature lineage on both upstream and downstream
        Returns [entity_id:entity] map and list of edges have been traversed.
        """
        id = self.get_entity_id(id_or_name)
        upstream_entities, upstream_edges = self._bfs(
            id, RelationshipType.Consumes)
        downstream_entities, downstream_edges = self._bfs(
            id, RelationshipType.Produces)
        return EntitiesAndRelations(
            upstream_entities + downstream_entities,
            upstream_edges + downstream_edges)

    def get_project(self, id_or_name: Union[str, UUID]) -> EntitiesAndRelations:
        """
        This function returns not only the project itself, but also everything in the project
        """
        project = self._get_entity(id_or_name)
        edges = set(self.get_neighbors(id_or_name, RelationshipType.Contains))
        ids = list([e.to_id for e in edges])
        children = self._get_entities(ids)
        child_map = dict([(e.id, e) for e in children])
        project.attributes.children = children
        for anchor in project.attributes.anchors:
            conn = self.get_neighbors(anchor.id, RelationshipType.Contains)
            feature_ids = [e.to_id for e in conn]
            edges = edges.union(conn)
            features = list([child_map[id] for id in feature_ids])
            anchor.attributes.features = features
            source_id = self.get_neighbors(
                anchor.id, RelationshipType.Consumes)[0].to_id
            anchor.attributes.source = child_map[source_id]
        for df in project.attributes.derived_features:
            conn = self.get_neighbors(df.id, RelationshipType.Consumes)
            input_ids = [e.to_id for e in conn]
            edges = edges.union(conn)
            features = list([child_map[id] for id in input_ids])
            df.attributes.input_features = features
        all_edges = self._get_edges(ids)
        return EntitiesAndRelations([project] + children, list(edges.union(all_edges)))
    
    def get_dependent_entities(self, entity_id: Union[str, UUID]) -> List[Entity]:
        """
        Given entity id, returns list of all entities that are downstream/dependant on the given entity
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
    
    def delete_empty_entities(self, entities: List[Entity]):
        """
        Given entity list, deleting all anchors that have no features and all sources that have no anchors.
        """
        if len(entities) == 0:
            return
        
        # clean up empty anchors
        for e in entities:
            if e.entity_type == EntityType.Anchor:
                downstream_entities, _ = self._bfs(e.id, RelationshipType.Contains) 
                if len(downstream_entities) == 1: # only anchor itself
                    self.delete_entity(e.id)
        # clean up empty sources
        for e in entities:
            if e.entity_type == EntityType.Source:
                downstream_entities, _ = self._bfs(e.id, RelationshipType.Produces) 
                if len(downstream_entities) == 1: # only source itself
                    self.delete_entity(e.id)

        return
        
    def delete_entity(self, entity_id: Union[str, UUID]):
        """
        Deletes given entity
        """
        entity_id = self.get_entity_id(entity_id)
        with self.conn.transaction() as c:
                self._delete_all_entity_edges(c, entity_id)
                self._delete_entity(c, entity_id)

    def search_entity(self,
                      keyword: str,
                      type: List[EntityType],
                      project: Optional[Union[str, UUID]] = None,
                      start: Optional[int] = None,
                      size: Optional[int] = None) -> List[EntityRef]:
        """
        WARN: This search function is implemented via `like` operator, which could be extremely slow.
        """
        
        top_clause = ""
        if start is not None and size is not None:
            top_clause = f"TOP({int(start) + int(size)})"
            
        if project:
            project_id = self.get_entity_id(project)
            if os.environ.get("FEATHR_SANDBOX"):
                query = self.sql_session.query(Entities.entity_id.label("id"),  Entities.qualified_name, Entities.entity_type.label("type")).join(Edges, and_(Entities.entity_id==Edges.from_id, Edges.conn_type=='BelongsTo')).filter(Edges.to_id==str(project_id), Entities.qualified_name.ilike("%" + keyword + "%"), Entities.entity_type.in_(tuple([str(t) for t in type]))).order_by(Entities.qualified_name).slice(int(start), int(start + size))
                rows = self._fetch_helper(query)
            else:
                sql = fr'''select {top_clause} entity_id as id, qualified_name, entity_type as type
                    from entities
                    inner join edges on entity_id=edges.from_id and edges.conn_type='BelongsTo'
                    where
                    edges.to_id=%s and qualified_name like %s and entity_type in %s
                    order by qualified_name'''
                rows = self.conn.query(sql, (str(project_id), '%' + keyword + '%', tuple([str(t) for t in type])))
                
        else:
            if os.environ.get("FEATHR_SANDBOX"):
                query = self.sql_session.query(Entities.entity_id.label("id"),  Entities.qualified_name, Entities.entity_type.label("type")).filter(Entities.qualified_name.ilike("%" + keyword + "%"), Entities.entity_type.in_(tuple([str(t) for t in type]))).order_by(Entities.qualified_name).slice(int(start), int(start + size))
                rows = self._fetch_helper(query)
            else:
                sql = fr'''select {top_clause} entity_id as id, qualified_name, entity_type as type from entities where qualified_name like %s and entity_type in %s order by qualified_name'''
                rows = self.conn.query(sql, ('%' + keyword + '%', tuple([str(t) for t in type])))
        if size:
            rows = rows[-size:]
        return list([EntityRef(**row) for row in rows])

    def create_project(self, definition: ProjectDef) -> UUID:
        # Here we start a transaction, any following step failed, everything rolls back
        definition.qualified_name = definition.name
        with self.conn.transaction() as c:
            # First we try to find existing entity with the same qualified name
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.select(self.entities_table.c.entity_id, self.entities_table.c.entity_type, self.entities_table.c.attributes).where((self.entities_table.c.qualified_name == definition.qualified_name)) 
                r = self._fetch_helper(query)
            else:
                c.execute(f'''select entity_id, entity_type, attributes from entities where qualified_name = %s''',
                        definition.qualified_name)
                r = c.fetchall()
            if r:
                if len(r) > 1:
                    assert False, "Data inconsistency detected, %d entities have same qualified_name %s" % (
                        len(r), definition.qualified_name)
                # The entity with same name already exists but with different type
                if _to_type(r[0]["entity_type"], EntityType) != EntityType.Project:
                    raise ConflictError("Entity %s already exists" %
                                     definition.qualified_name)
                # Just return the existing project id
                return _to_uuid(r[0]["entity_id"])
            id = uuid4()
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.insert(self.entities_table).values(entity_id= str(id), entity_type=str(EntityType.Project), qualified_name=definition.qualified_name, attributes =definition.to_attr().to_json())
                r = self.connection.execute(query)
            else:
                c.execute(f"insert into entities (entity_id, entity_type, qualified_name, attributes) values (%s, %s, %s, %s)",
                        (str(id),
                        str(EntityType.Project),
                        definition.qualified_name,
                        definition.to_attr().to_json()))
            return id

    def create_project_datasource(self, project_id: UUID, definition: SourceDef) -> UUID:
        project = self.get_entity(project_id)
        definition.qualified_name = f"{project.qualified_name}__{definition.name}"
        # Here we start a transaction, any following step failed, everything rolls back
        with self.conn.transaction() as c:
            # First we try to find existing entity with the same qualified name
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.select(self.entities_table.c.entity_id, self.entities_table.c.entity_type, self.entities_table.c.attributes).where((self.entities_table.c.qualified_name == definition.qualified_name)) 
                r = self._fetch_helper(query)
            else:
                c.execute(f'''select entity_id, entity_type, attributes from entities where qualified_name = %s''',
                        definition.qualified_name)
                r = c.fetchall()
            if r:
                if len(r) > 1:
                    # There are multiple entities with same qualified name， that means we already have errors in the db
                    assert False, "Data inconsistency detected, %d entities have same qualified_name %s" % (
                        len(r), definition.qualified_name)
                # The entity with same name already exists but with different type
                if _to_type(r[0]["entity_type"], EntityType) != EntityType.Source:
                    raise ConflictError("Entity %s already exists" %
                                     definition.qualified_name)
                attr: SourceAttributes = _to_type(
                    json.loads(r[0]["attributes"]), SourceAttributes)
                if attr.name == definition.name \
                        and attr.type == definition.type \
                        and attr.options == definition.options \
                        and attr.preprocessing == definition.preprocessing \
                        and attr.event_timestamp_column == definition.event_timestamp_column \
                        and attr.timestamp_format == definition.timestamp_format:
                    # Creating exactly same entity
                    # Just return the existing id
                    return _to_uuid(r[0]["entity_id"])
                raise ConflictError("Entity %s already exists" %
                                 definition.qualified_name)
            id = uuid4()
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.insert(self.entities_table).values(entity_id= str(id), entity_type=str(EntityType.Source), qualified_name=definition.qualified_name, attributes =definition.to_attr().to_json())
                self.connection.execute(query)
            else:
                c.execute(f"insert into entities (entity_id, entity_type, qualified_name, attributes) values (%s, %s, %s, %s)",
                        (str(id),
                        str(EntityType.Source),
                        definition.qualified_name,
                        definition.to_attr().to_json()))
            self._create_edge(c, project_id, id, RelationshipType.Contains)
            self._create_edge(c, id, project_id, RelationshipType.BelongsTo)
            return id

    def create_project_anchor(self, project_id: UUID, definition: AnchorDef) -> UUID:
        project = self.get_entity(project_id)
        definition.qualified_name = f"{project.qualified_name}__{definition.name}"
        # Here we start a transaction, any following step failed, everything rolls back
        with self.conn.transaction() as c:
            # First we try to find existing entity with the same qualified name
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.select(self.entities_table.c.entity_id, self.entities_table.c.entity_type, self.entities_table.c.attributes).where((self.entities_table.c.qualified_name == definition.qualified_name)) 
                r = self._fetch_helper(query)
            else:
                c.execute(f'''select entity_id, entity_type, attributes from entities where qualified_name = %s''',
                        definition.qualified_name)
                r = c.fetchall()
            if r:
                if len(r) > 1:
                    # There are multiple entities with same qualified name， that means we already have errors in the db
                    assert False, "Data inconsistency detected, %d entities have same qualified_name %s" % (
                        len(r), definition.qualified_name)
                # The entity with same name already exists but with different type
                if _to_type(r[0]["entity_type"], EntityType) != EntityType.Anchor:
                    raise ConflictError("Entity %s already exists" %
                                     definition.qualified_name)
                attr: AnchorAttributes = _to_type(
                    json.loads(r[0]["attributes"]), AnchorAttributes)
                if attr.name == definition.name:
                    # Creating exactly same entity
                    # Just return the existing id
                    return _to_uuid(r[0]["entity_id"])
                raise ConflictError("Entity %s already exists" %
                                 definition.qualified_name)
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.select(self.entities_table.c.entity_id, self.entities_table.c.qualified_name).where((self.entities_table.c.entity_id == str(definition.source_id)) & (self.entities_table.c.entity_type == str(EntityType.Source))) 
                r = self._fetch_helper(query)
            else:
                c.execute("select entity_id, qualified_name from entities where entity_id = %s and entity_type = %s", (str(
                    definition.source_id), str(EntityType.Source)))
                r = c.fetchall()
            if not r:
                raise ValueError("Source %s does not exist" %
                                 definition.source_id)
            ref = EntityRef(r[0]["entity_id"],
                            EntityType.Source, r[0]["qualified_name"])
            id = uuid4()
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.insert(self.entities_table).values(entity_id= str(id), entity_type=str(EntityType.Anchor), qualified_name=definition.qualified_name, attributes =definition.to_attr(ref).to_json())
                self.connection.execute(query)
            else:
                c.execute(f"insert into entities (entity_id, entity_type, qualified_name, attributes) values (%s, %s, %s, %s)",
                        (str(id),
                        str(EntityType.Anchor),
                        definition.qualified_name,
                        definition.to_attr(ref).to_json()))
            # Add "Contains/BelongsTo" relations between anchor and project
            self._create_edge(c, project_id, id, RelationshipType.Contains)
            self._create_edge(c, id, project_id, RelationshipType.BelongsTo)
            # Add "Consumes/Produces" relations between anchor and datasource
            self._create_edge(c, id, definition.source_id,
                              RelationshipType.Consumes)
            self._create_edge(c, definition.source_id, id,
                              RelationshipType.Produces)
            return id

    def create_project_anchor_feature(self, project_id: UUID, anchor_id: UUID, definition: AnchorFeatureDef) -> UUID:
        anchor = self.get_entity(anchor_id)
        definition.qualified_name = f"{anchor.qualified_name}__{definition.name}"
        # Here we start a transaction, any following step failed, everything rolls back
        with self.conn.transaction() as c:
            # First we try to find existing entity with the same qualified name
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.select(self.entities_table.c.entity_id, self.entities_table.c.entity_type, self.entities_table.c.attributes).where((self.entities_table.c.qualified_name == definition.qualified_name)) 
                r = self._fetch_helper(query)
            else:
                c.execute(f'''select entity_id, entity_type, attributes from entities where qualified_name = %s''',
                        definition.qualified_name)
                r = c.fetchall()
            if r:
                if len(r) > 1:
                    # There are multiple entities with same qualified name， that means we already have errors in the db
                    assert False, "Data inconsistency detected, %d entities have same qualified_name %s" % (
                        len(r), definition.qualified_name)
                # The entity with same name already exists but with different type
                if _to_type(r[0]["entity_type"], EntityType) != EntityType.AnchorFeature:
                    raise ConflictError("Entity %s already exists" %
                                     definition.qualified_name)
                attr: AnchorFeatureAttributes = _to_type(
                    json.loads(r[0]["attributes"]), AnchorFeatureAttributes)
                if attr.name == definition.name \
                        and attr.type == definition.feature_type \
                        and attr.transformation == definition.transformation \
                        and attr.key == definition.key:
                    # Creating exactly same entity
                    # Just return the existing id
                    return _to_uuid(r[0]["entity_id"])
                # The existing entity has different definition, that's a conflict
                raise ConflictError("Entity %s already exists" %
                                 definition.qualified_name)
            source_id = anchor.attributes.source.id
            id = uuid4()
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.insert(self.entities_table).values(entity_id= str(id), entity_type=str(EntityType.AnchorFeature), qualified_name=definition.qualified_name, attributes =definition.to_attr().to_json())
                r = self.connection.execute(query)
            else:
                c.execute(f"insert into entities (entity_id, entity_type, qualified_name, attributes) values (%s, %s, %s, %s)",
                        (str(id),
                        str(EntityType.AnchorFeature),
                        definition.qualified_name,
                        definition.to_attr().to_json()))
            # Add "Contains/BelongsTo" relations between anchor feature and project
            self._create_edge(c, project_id, id, RelationshipType.Contains)
            self._create_edge(c, id, project_id, RelationshipType.BelongsTo)
            # Add "Contains/BelongsTo" relations between anchor feature and anchor
            self._create_edge(c, anchor_id, id, RelationshipType.Contains)
            self._create_edge(c, id, anchor_id, RelationshipType.BelongsTo)
            # Add "Consumes/Produces" relations between anchor feature and datasource used by anchor
            self._create_edge(c, id, source_id, RelationshipType.Consumes)
            self._create_edge(c, source_id, id, RelationshipType.Produces)
            return id

    def create_project_derived_feature(self, project_id: UUID, definition: DerivedFeatureDef) -> UUID:
        project = self.get_entity(project_id)
        definition.qualified_name = f"{project.qualified_name}__{definition.name}"
        # Here we start a transaction, any following step failed, everything rolls back
        with self.conn.transaction() as c:
            # First we try to find existing entity with the same qualified name
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.select(self.entities_table.c.entity_id, self.entities_table.c.entity_type, self.entities_table.c.attributes).where((self.entities_table.c.qualified_name == definition.qualified_name)) 
                r = self._fetch_helper(query)
            else:
                c.execute(f'''select entity_id, entity_type, attributes from entities where qualified_name = %s''',
                        definition.qualified_name)
                r = c.fetchall()
            if r:
                if len(r) > 1:
                    # There are multiple entities with same qualified name， that means we already have errors in the db
                    assert False, "Data inconsistency detected, %d entities have same qualified_name %s" % (
                        len(r), definition.qualified_name)
                # The entity with same name already exists but with different type, that's conflict
                if _to_type(r[0]["entity_type"], EntityType) != EntityType.DerivedFeature:
                    raise ConflictError("Entity %s already exists" %
                                     definition.qualified_name)
                attr: DerivedFeatureAttributes = _to_type(
                    json.loads(r[0]["attributes"]), DerivedFeatureAttributes)
                if attr.name == definition.name \
                        and attr.type == definition.feature_type \
                        and attr.transformation == definition.transformation \
                        and attr.key == definition.key:
                    # Creating exactly same entity
                    # Just return the existing id
                    return _to_uuid(r[0]["entity_id"])
                # The existing entity has different definition, that's a conflict
                raise ConflictError("Entity %s already exists" %
                                 definition.qualified_name)
            r1 = []
            # Fill `input_anchor_features`, from `definition` we have ids only, we still need qualified names
            if definition.input_anchor_features:
                if os.environ.get("FEATHR_SANDBOX"):
                    query = self.sql_session.query(Entities.entity_id, Entities.entity_type, Entities.qualified_name).filter(Entities.entity_id.in_(tuple([str(id) for id in definition.input_anchor_features])), Entities.entity_type == str(EntityType.AnchorFeature))
                    r1 = self._fetch_helper(query)
                else:
                    c.execute(
                        fr'''select entity_id, entity_type, qualified_name from entities where entity_id in %s and entity_type = %s ''', (tuple([str(id) for id in definition.input_anchor_features]), str(EntityType.AnchorFeature)))
                    r1 = c.fetchall()
                if len(r1) != len(definition.input_anchor_features):
                    # TODO: More detailed error
                    raise(ValueError("Missing input anchor features"))
            # Fill `input_derived_features`, from `definition` we have ids only, we still need qualified names
            r2 = []
            if definition.input_derived_features:
                if os.environ.get("FEATHR_SANDBOX"):
                    query = self.sql_session.query(Entities.entity_id, Entities.entity_type, Entities.qualified_name).filter(Entities.entity_id.in_(tuple([str(id) for id in definition.input_derived_features])), Entities.entity_type == str(EntityType.DerivedFeature))
                    r2 = self._fetch_helper(query)
                else:
                    c.execute(
                        fr'''select entity_id, entity_type, qualified_name from entities where entity_id in %s and entity_type = %s ''', (tuple([str(id) for id in definition.input_derived_features]), str(EntityType.DerivedFeature)))
                    r2 = c.fetchall()
                if len(r2) != len(definition.input_derived_features):
                    # TODO: More detailed error
                    raise(ValueError("Missing input derived features"))
            refs = list([EntityRef(r["entity_id"], r["entity_type"], r["qualified_name"]) for r in r1+r2])
            id = uuid4()
            if os.environ.get("FEATHR_SANDBOX"):
                query = db.insert(self.entities_table).values(entity_id= str(id), entity_type=str(EntityType.DerivedFeature), qualified_name=definition.qualified_name, attributes =definition.to_attr(refs).to_json())
                self.connection.execute(query)
            else:
                c.execute(f"insert into entities (entity_id, entity_type, qualified_name, attributes) values (%s, %s, %s, %s)",
                        (str(id),
                        str(EntityType.DerivedFeature),
                        definition.qualified_name,
                        definition.to_attr(refs).to_json()))
            # Add "Contains/BelongsTo" relations between derived feature and project
            self._create_edge(c, project_id, id, RelationshipType.Contains)
            self._create_edge(c, id, project_id, RelationshipType.BelongsTo)
            for r in r1+r2:
                # Add "Consumes/Produces" relations between derived feature and all its upstream
                input_feature_id = r["entity_id"]
                self._create_edge(c, id, input_feature_id,
                                  RelationshipType.Consumes)
                self._create_edge(c, input_feature_id, id,
                                  RelationshipType.Produces)
            return id

    def _create_edge(self, cursor, from_id: UUID, to_id: UUID, type: RelationshipType):
        """
        Create an edge with specified type between 2 entities, skip if the same connection already exists
        """
        if os.environ.get("FEATHR_SANDBOX"):
            # TODO: might not be a safe solution since it's not transactional 
            query = db.insert(self.edges_table).values(edge_id= str(uuid4()), from_id=str(from_id), to_id=str(to_id), conn_type =type.name)
            self.connection.execute(query)
        else:
            sql = r'''
            IF NOT EXISTS (SELECT 1 FROM edges WHERE from_id=%(from_id)s and to_id=%(to_id)s and conn_type=%(type)s)
                    BEGIN
                        INSERT INTO edges
                        (edge_id, from_id, to_id, conn_type)
                        values
                        (%(edge_id)s, %(from_id)s, %(to_id)s, %(type)s)
                    END'''
            cursor.execute(sql, {
                "edge_id": str(uuid4()),
                "from_id": str(from_id),
                "to_id": str(to_id),
                "type": type.name
            })
    
    def _delete_all_entity_edges(self, cursor, entity_id: UUID):
        """
        Deletes all edges associated with an entity
        """
        if os.environ.get("FEATHR_SANDBOX"):
            row_to_delete = self.sql_session.query(Edges).filter((Edges.from_id == str(entity_id)) | Edges.to_id == str(entity_id))
            row_to_delete.delete()
            self.sql_session.commit()
        else:
            sql = fr'''DELETE FROM edges WHERE from_id = %s OR to_id = %s'''
            cursor.execute(sql, (str(entity_id), str(entity_id)))
    
    def _delete_entity(self, cursor, entity_id: UUID):
        """
        Deletes entity from entities table
        """
        if os.environ.get("FEATHR_SANDBOX"):
            row_to_delete = self.sql_session.query(Entities).filter((Entities.entity_id == str(entity_id)))
            # self.sql_session.delete(row_to_delete)
            row_to_delete.delete()
            self.sql_session.commit()
        else:
            sql = fr'''DELETE FROM entities WHERE entity_id = %s'''
            cursor.execute(sql, str(entity_id))

    def _fill_entity(self, e: Entity) -> Entity:
        """
        Entities in the DB contains only attributes belong to itself, but the returned
        data model contains connections/contents, so we need to fill this gap
        """
        if e.entity_type == EntityType.Project:
            edges = self.get_neighbors(e.id, RelationshipType.Contains)
            ids = list([e.to_id for e in edges])
            children = self._get_entities(ids)
            e.attributes.children = children
            return e
        if e.entity_type == EntityType.Anchor:
            conn = self.get_neighbors(e.id, RelationshipType.Contains)
            feature_ids = [e.to_id for e in conn]
            features = self._get_entities(feature_ids)
            e.attributes.features = features
            source_id = self.get_neighbors(
                e.id, RelationshipType.Consumes)[0].to_id
            source = self.get_entity(source_id)
            e.attributes.source = source
            return e
        if e.entity_type == EntityType.DerivedFeature:
            conn = self.get_neighbors(e.id, RelationshipType.Consumes)
            feature_ids = [e.to_id for e in conn]
            features = self._get_entities(feature_ids)
            e.attributes.input_features = features
            return e
        return e

    def _get_edges(self, ids: List[UUID], types: List[RelationshipType] = []) -> List[Edge]:
        if not ids:
            return []
        if os.environ.get("FEATHR_SANDBOX"):
            if len(types) > 0:
                query = self.sql_session.query(Edges.edge_id,Edges.from_id, Edges.to_id, Edges.conn_type).filter((Edges.from_id.in_(tuple([str(id) for id in ids]))) & (Edges.to_id.in_(tuple([str(id) for id in ids]))) & (Edges.conn_type.in_(tuple([t.name for t in types]))) )
            else:
                query = self.sql_session.query(Edges.edge_id,Edges.from_id, Edges.to_id, Edges.conn_type).filter((Edges.from_id.in_(tuple([str(id) for id in ids]))) & (Edges.to_id.in_(tuple([str(id) for id in ids]))))
            
            rows = self._fetch_helper(query)
        else:
            sql = fr"""select edge_id, from_id, to_id, conn_type from edges
            where from_id in %(ids)s
            and to_id in %(ids)s"""
            if len(types) > 0:
                sql = fr"""select edge_id, from_id, to_id, conn_type from edges
                where conn_type in %(types)s
                and from_id in %(ids)s
                and to_id in %(ids)s"""
            
            rows = self.conn.query(sql, {
                "ids": tuple([str(id) for id in ids]),
                "types": tuple([t.name for t in types]),
            })
        return list([_to_type(row, Edge) for row in rows])

    def _get_entity(self, id_or_name: Union[str, UUID]) -> Entity:
        if os.environ.get("FEATHR_SANDBOX"):
            query = db.select(self.entities_table.c.entity_id, self.entities_table.c.qualified_name, self.entities_table.c.entity_type, self.entities_table.c.attributes).where((self.entities_table.c.entity_id == str(self.get_entity_id(id_or_name)))) 
            row = self._fetch_helper(query)
        else:
            row = self.conn.query(fr'''
            select entity_id, qualified_name, entity_type, attributes
            from entities
            where entity_id = %s
        ''', self.get_entity_id(id_or_name))
        if not row:
            raise KeyError(f"Entity {id_or_name} not found")
        row=row[0]
        row["attributes"] = json.loads(row["attributes"])
        return _to_type(row, Entity)

    def _get_entities(self, ids: List[UUID]) -> List[Entity]:
        if not ids:
            return []
        if os.environ.get("FEATHR_SANDBOX"):
            query = self.sql_session.query(Entities.entity_id, Entities.qualified_name,Entities.entity_type, Entities.attributes).filter(Entities.entity_id.in_(tuple([str(id) for id in ids]), ))
            rows = self._fetch_helper(query)
        else:
            rows = self.conn.query(fr'''select entity_id, qualified_name, entity_type, attributes
                from entities
                where entity_id in %s
            ''', (tuple([str(id) for id in ids]), ))
        ret = []
        for row in rows:
            row["attributes"] = json.loads(row["attributes"])
            ret.append(Entity(**row))
        return ret

    def _bfs(self, id: UUID, conn_type: RelationshipType) -> Tuple[List[Entity], List[Edge]]:
        """
        Breadth first traversal
        Starts from `id`, follow edges with `conn_type` only.

        WARN: There is no depth limit.
        """
        connections = []
        to_ids = [{
            "to_id": id,
        }]
        # BFS over SQL
        while len(to_ids) != 0:
            to_ids = self._bfs_step(to_ids, conn_type)
            connections.extend(to_ids)
        ids = set([id])
        for r in connections:
            ids.add(r["from_id"])
            ids.add(r["to_id"])
        entities = self.get_entities(ids)
        edges = list([Edge(**c) for c in connections])
        return (entities, edges)

    def _bfs_step(self, ids: List[UUID], conn_type: RelationshipType) -> Set[Dict]:
        """
        One step of the BFS process
        Returns all edges that connect to node ids the next step
        """
        ids = list([id["to_id"] for id in ids])
        if os.environ.get("FEATHR_SANDBOX"):
            query = self.sql_session.query(Edges.edge_id,Edges.from_id, Edges.to_id, Edges.conn_type).filter(Edges.conn_type == conn_type.name, Edges.from_id.in_(tuple([str(id) for id in ids])))
            r = self._fetch_helper(query)
            return r
        else:
            sql = fr"""select edge_id, from_id, to_id, conn_type from edges where conn_type = %s and from_id in %s"""
            return self.conn.query(sql, (conn_type.name, tuple([str(id) for id in ids])))
