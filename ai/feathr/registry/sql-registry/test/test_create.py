import registry
from registry.db_registry import quote
from registry.models import AnchorDef, AnchorFeatureDef, DerivedFeatureDef, ExpressionTransformation, FeatureType, ProjectDef, SourceDef, TensorCategory, Transformation, TypedKey, ValueType, VectorType

r = registry.DbRegistry()


def cleanup():
    with r.conn.transaction() as c:
        ids = quote([project1_id, source1_id, anchor1_id, af1_id, df1_id])
        c.execute(
            f"delete from edges where from_id in ({ids}) or to_id in ({ids})")
        c.execute(
            f"delete from entities where entity_id in ({ids})")


project1_id = r.create_project(ProjectDef("unit_test_project_1"))
print("project1 id ", project1_id)
project1 = r.get_entity(project1_id)
assert project1.qualified_name == "unit_test_project_1"

# Re-create project, should return the same id
id = r.create_project(ProjectDef("unit_test_project_1"))
assert project1_id == id

source1_id = r.create_project_datasource(project1_id, SourceDef(
    qualified_name="unit_test_project_1__source1", name="source1", path="hdfs://somewhere", type="hdfs"))
print("source1 id ", source1_id)
source1 = r.get_entity(source1_id)
assert source1.qualified_name == "unit_test_project_1__source1"

anchor1_id = r.create_project_anchor(project1_id, AnchorDef(
    qualified_name="unit_test_project_1__anchor1", name="anchor1", source_id=source1_id))
print("anchor1 id ", anchor1_id)
anchor1 = r.get_entity(anchor1_id)
assert anchor1.qualified_name == "unit_test_project_1__anchor1"
# anchor1 has source "source1"
assert anchor1.attributes.source.id == source1_id

ft1 = FeatureType(type=VectorType.TENSOR,  tensor_category=TensorCategory.DENSE,
                  dimension_type=[], val_type=ValueType.INT)
t1 = ExpressionTransformation("af1")
k = TypedKey(key_column="c1", key_column_type=ValueType.INT)
af1_id = r.create_project_anchor_feature(project1_id, anchor1_id, AnchorFeatureDef(
    qualified_name="unit_test_project_1__anchor1__af1", name="af1", feature_type=ft1,  transformation=t1, key=[k]))
print("af1 id ", af1_id)
af1 = r.get_entity(af1_id)
assert af1.qualified_name == "unit_test_project_1__anchor1__af1"

df1_id = r.create_project_derived_feature(project1_id, DerivedFeatureDef(qualified_name="unit_test_project_1__df1",
                                          name="df1", feature_type=ft1, transformation=t1, key=[k], input_anchor_features=[af1_id], input_derived_features=[]))
print("df1 id ", df1_id)
df1 = r.get_entity(df1_id)
assert df1.qualified_name == "unit_test_project_1__df1"
# df1 has only 1 input anchor feature "af1"
assert df1.attributes.input_anchor_features[0].id == af1_id

df1_downstream_entities = r.get_dependent_entities(df1_id)
assert len(df1_downstream_entities) == 0

af1_downstream_entities = r.get_dependent_entities(af1_id)
assert len(af1_downstream_entities) == 1

#Delete derived feature
r.delete_entity(df1_id)

# Try getting derived feature but KeyError exception should be thrown
derived_exists = 1
try:
    df1 = r.get_entity(df1_id)
except KeyError:
    derived_exists = 0
assert derived_exists == 0

af1_downstream_entities = r.get_dependent_entities(af1_id)
assert len(af1_downstream_entities) == 0

# cleanup()
