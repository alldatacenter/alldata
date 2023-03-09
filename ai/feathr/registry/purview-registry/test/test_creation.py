from unicodedata import name
from registry.models import AnchorDef, AnchorFeatureDef, DerivedFeatureDef, ExpressionTransformation, FeatureType, ProjectDef, SourceDef, TensorCategory, TypedKey, ValueType, VectorType
from registry.purview_registry import PurviewRegistry

registry = PurviewRegistry("feathrazuretest3-purview1")

proj_id = registry.create_project(ProjectDef("yihui_test_registry","yihui_test_registry",{"obsolete":"False"}))

source_id = registry.create_project_datasource(proj_id,SourceDef(name="source1", qualified_name="yihui_test_registry__source1", path="hdfs://somewhere", type="hdfs"))

anchor1_id = registry.create_project_anchor(proj_id, AnchorDef(
    qualified_name="yihui_test_registry__anchor1", name="anchor1", source_id=source_id))
ft1 = FeatureType(type=VectorType.TENSOR,  tensor_category=TensorCategory.DENSE,
                  dimension_type=[], val_type=ValueType.INT)
t1 = ExpressionTransformation("af1")
k = TypedKey(key_column="c1", key_column_type=ValueType.INT)

feature1 = registry.create_project_anchor_feature(proj_id, anchor1_id, AnchorFeatureDef(
    qualified_name="yihui_test_registry__anchor1__af1", name="af1", feature_type=ft1,  transformation=t1, key=[k]))
derived = registry.create_project_derived_feature(proj_id, DerivedFeatureDef(qualified_name="yihui_test_registry__df1",
                                          name="df1", feature_type=ft1, transformation=t1, key=[k], input_anchor_features=[feature1], input_derived_features=[]))

print(proj_id,source_id,anchor1_id,feature1,derived)

derived_downstream_entities = registry.get_dependent_entities(derived)
assert len(derived_downstream_entities) == 0

feature1_downstream_entities = registry.get_dependent_entities(feature1)
assert len(feature1_downstream_entities) == 1

registry.delete_entity(derived)

# Try getting derived feature but KeyError exception should be thrown
derived_exists = 1
try:
    df1 = registry.get_entity(derived)
except KeyError:
    derived_exists = 0
assert derived_exists == 0

feature1_downstream_entities = registry.get_dependent_entities(feature1)
assert len(feature1_downstream_entities) == 0

# cleanup()
