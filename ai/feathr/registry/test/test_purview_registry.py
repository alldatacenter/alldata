import sys, os
sys.path.append(os.path.join(os.path.dirname(sys.path[0]),'purview-registry'))
from datetime import datetime
import unittest, pytest
from unicodedata import name
from registry.models import AnchorDef, AnchorFeatureDef, DerivedFeatureDef, ExpressionTransformation, WindowAggregationTransformation, UdfTransformation, FeatureType, ProjectDef, SourceDef, TensorCategory, TypedKey, ValueType, VectorType, EntityType
from registry.purview_registry import PurviewRegistry, ConflictError

class PurviewRegistryTest(unittest.TestCase):
    
    def setup(self):
        purview_name = os.getenv('PURVIEW_NAME')
        if purview_name is None:
            raise RuntimeError("Failed to run Purview registry test case. Cannot get environment variable: 'PURVIEW_NAME'")
        
        self.registry = PurviewRegistry(purview_name)
    
    def cleanup(self, ids):
        for id in ids:
            self.registry.delete_entity(id)
            
    def create_and_get_project(self, project_name):
        project_id = self.registry.create_project(ProjectDef(project_name))
        assert project_id is not None
        project = self.registry.get_entity(project_id)
        assert project.qualified_name == project_name
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
        assert anchor.attributes.source['guid'] == str(source_id)
        return anchor_id
    
    def create_and_get_anchor_feature(self, project_id, anchor_id, qualified_name, name, feature_type, transformation, keys):
        af_id = self.registry.create_project_anchor_feature(project_id, anchor_id, AnchorFeatureDef(
        qualified_name=qualified_name, name=name, feature_type=feature_type,  transformation=transformation, key=keys))
        assert af_id is not None
        af = self.registry.get_entity(af_id)
        assert af.qualified_name == qualified_name
        return af_id
     
    def create_and_get_derived_feature(self, project_id, qualified_name, name, feature_type, transformation, keys, input_anchor_features, input_derived_features):
        df_id = self.registry.create_project_derived_feature(project_id, DerivedFeatureDef(qualified_name=qualified_name,
                                          name=name, feature_type=feature_type, transformation=transformation, key=keys, input_anchor_features=input_anchor_features, input_derived_features=input_derived_features))
        assert df_id is not None
        df = self.registry.get_entity(df_id)
        assert df.qualified_name == qualified_name
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
        assert self.registry.get_entity_id(project_name) == str(project_id)      
        projects = self.registry.get_projects()
        assert len(projects) >= 1
        project_ids = self.registry.get_projects_ids()
        assert len(project_ids.keys()) >= 1
        
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
        df_id2 = self.create_and_get_derived_feature(project_id, project_name+"__df2", "df2", ft1, t2, [k], [af_id], [df_id])
        df_id3 = self.create_and_get_derived_feature(project_id, project_name+"__df3", "df3", ft1, t3, [k], [af_id], [df_id, df_id2])
        
        # test all entities and relations
        features = self.registry.get_project_features(project_id, project_name)
        assert(len(features) > 0)
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
        
        self.cleanup([project_id, source_id, anchor_id, af_id, af_id2, df_id, df_id2,df_id3])
        