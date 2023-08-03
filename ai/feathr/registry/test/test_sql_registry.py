import sys, os
sys.path.append(os.path.join(os.path.dirname(sys.path[0]),'sql-registry'))
from datetime import datetime
#sys.path.append(os.path.join(os.path.dirname(sys.path[0]),'purview-registry'))
import unittest, pytest

from registry.db_registry import DbRegistry, quote, ConflictError
from registry.models import AnchorDef, AnchorFeatureDef, DerivedFeatureDef, ExpressionTransformation, WindowAggregationTransformation, UdfTransformation, FeatureType, ProjectDef, SourceDef, TensorCategory, Transformation, TypedKey, ValueType, VectorType, EntityType

class SqlRegistryTest(unittest.TestCase):
    
    def setup(self):
        if os.getenv('CONNECTION_STR') is None:
            raise RuntimeError("Failed to run SQL registry test case. Cannot get environment variable: 'CONNECTION_STR'")
        self.registry = DbRegistry()
    
    def cleanup(self, ids):
        with self.registry.conn.transaction() as c:
            ids = quote(ids)
            c.execute(
                f"delete from edges where from_id in ({ids}) or to_id in ({ids})")
            c.execute(
                f"delete from entities where entity_id in ({ids})")
            
    def create_and_get_project(self, project_name):
        project_id = self.registry.create_project(ProjectDef(project_name))
        assert project_id is not None
        project = self.registry.get_entity(project_id)
        assert project.qualified_name == project_name
        assert self.registry.get_entity_id(project_name) == str(project_id)
        return project_id
        
    def create_and_get_data_source(self, project_id, qualified_name, name, path, type):
        source_id = self.registry.create_project_datasource(project_id, SourceDef(
        qualified_name=qualified_name, name=name, path=path, type=type))
        assert source_id is not None
        source = self.registry.get_entity(source_id)
        assert source.qualified_name == qualified_name
        return source_id
    
    def create_and_get_anchor(self, project_id, source_id, qualified_name, name):
        anchor_id = self.registry.create_project_anchor(project_id, AnchorDef(
        qualified_name=qualified_name, name=name, source_id=source_id))
        assert anchor_id is not None
        anchor = self.registry.get_entity(anchor_id)
        assert anchor.qualified_name == qualified_name
        # anchor1 has source "source1"
        assert anchor.attributes.source.id == source_id
        return anchor_id
    
    def create_and_get_anchor_feature(self, project_id, anchor_id, qualified_name, name, feature_type, transformation, keys):
        af_id = self.registry.create_project_anchor_feature(project_id, anchor_id, AnchorFeatureDef(
        qualified_name=qualified_name, name=name, feature_type=feature_type,  transformation=transformation, key=keys))
        assert af_id is not None
        af = self.registry.get_entity(af_id)
        assert af.qualified_name == qualified_name
        return af_id
     
    def create_and_get_derived_feature(self, project_id, qualified_name, name, feature_type, transformation, keys, input_anchor_features, inpur_derived_features):
        df_id = self.registry.create_project_derived_feature(project_id, DerivedFeatureDef(qualified_name=qualified_name,
                                          name=name, feature_type=feature_type, transformation=transformation, key=keys, input_anchor_features=input_anchor_features, input_derived_features=inpur_derived_features))
        assert df_id is not None
        df = self.registry.get_entity(df_id)
        assert df.qualified_name == qualified_name
        # df1 has only 1 input anchor feature "af1"
        assert df.attributes.input_anchor_features[0].id == input_anchor_features[0]
        af_id = input_anchor_features[0]   
        af1_downstream_entities = self.registry.get_dependent_entities(af_id)
        assert len(af1_downstream_entities) > 0
        return df_id

    def test_registry(self):
        self.setup()
        now = datetime.now()
        project_name = ''.join(["unit_test_project", str(now.minute), str(now.second)])
        # test create project
        project_id = self.create_and_get_project(project_name)
        # re-create project, should return the same id
        id = self.registry.create_project(ProjectDef(project_name))
        assert project_id == id       
        project_id2 = self.create_and_get_project("unit_test_project2")
        projects = self.registry.get_projects()
        assert len(projects) >= 2
        project_ids = self.registry.get_projects_ids()
        assert len(project_ids.keys()) >= 2
        
        # test create data source
        source_id = self.create_and_get_data_source(project_id, project_name+"__source", "source","hdfs://somewhere","hdfs")
        
        # test create anchor
        anchor_id = self.create_and_get_anchor(project_id, source_id, project_name+"__anchor", "anchor")
        
        # test create anchor feature
        ft1 = FeatureType(type=VectorType.TENSOR,  tensor_category=TensorCategory.DENSE,
                  dimension_type=[], val_type=ValueType.INT)
        t1 = ExpressionTransformation("af")
        t2 = WindowAggregationTransformation("def_expr","agg_func","window","group_by","filter",limit=10)
        t3 = UdfTransformation(name = "udf_trans")
        k = TypedKey(key_column="c1", key_column_type=ValueType.INT, full_name="tk", description="description", key_column_alias="alias")
        af_id = self.create_and_get_anchor_feature(project_id, anchor_id,project_name+"__anchor__af", name="af", feature_type=ft1,  transformation=t1, keys=[k])
        af_id2 = self.create_and_get_anchor_feature(project_id, anchor_id,project_name+"__anchor__af2", name="af2", feature_type=ft1,  transformation=t2, keys=[k])
        # test create derived feature
        df_id = self.create_and_get_derived_feature(project_id, project_name+"__df", "df", ft1, t1, [k], [af_id], [])
        df_id2 = self.create_and_get_derived_feature(project_id, project_name+"__df2", "df2", ft1, t1, [k], [af_id], [df_id])
        df_id3 = self.create_and_get_derived_feature(project_id, project_name+"__df3", "df3", ft1, t1, [k], [af_id], [df_id, df_id2])
        # test all entities and relations
        entities_and_relations =self.registry.get_lineage(df_id).to_dict()
        assert(len(entities_and_relations['guidEntityMap']) > 0)
        assert(len(entities_and_relations['relations']) > 0)
        
        entities_and_relations2 = self.registry.get_project(project_id).to_dict()
        assert(len(entities_and_relations2['guidEntityMap']) > 0)
        assert(len(entities_and_relations2['relations']) > 0)
        
        # test downstreams entities
        project_downstream_ids = self.registry.get_dependent_entities(project_id)
        assert(len(project_downstream_ids) > 0 )
        source_downstream_ids = self.registry.get_dependent_entities(source_id)
        assert(len(source_downstream_ids) > 0)
        anchor_downstream_ids = self.registry.get_dependent_entities(anchor_id)
        assert(len(anchor_downstream_ids) > 0)
        
        # test search entities
        entities = self.registry.search_entity(project_name+"__anchor", [EntityType.Anchor], project_id)
        assert(len(entities) > 0)
        entities = self.registry.search_entity(project_name, [EntityType.Project])
        assert(len(entities) > 0)
        
        # test create entities with existing names
        with pytest.raises(ConflictError):
            project_id3 = self.registry.create_project(ProjectDef(project_name+"__anchor"))
            assert project_id3 == anchor_id
        with pytest.raises(ConflictError):
            source_id2 = self.registry.create_project_datasource(project_id, SourceDef(
        qualified_name=project_name+"__anchor", name="anchor", path="somepath", type="hdfs"))
            assert source_id2 == anchor_id
        with pytest.raises(ConflictError):
            anchor_id2 = self.registry.create_project_anchor(project_id, AnchorDef(
        qualified_name=project_name+"__source", name="source", source_id=source_id))
            assert anchor_id2 == source_id
        af_id3 = self.registry.create_project_anchor_feature(project_id, anchor_id, AnchorFeatureDef(
        qualified_name=project_name+"__anchor__af", name="af", feature_type=ft1,  transformation=t1, key=[k]))
        assert af_id3  == af_id
        df_id4 = self.registry.create_project_derived_feature(project_id, DerivedFeatureDef(qualified_name=project_name+"__df",
                                          name="df", feature_type=ft1, transformation=t1, key=[k], input_anchor_features=[], input_derived_features=[]))
        assert df_id4 == df_id
        
        self.registry.delete_entity(project_id2)
        self.cleanup([project_id, source_id, anchor_id, af_id, af_id2, df_id, df_id2,df_id3])
       