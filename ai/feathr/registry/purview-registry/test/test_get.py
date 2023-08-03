from registry.models import ProjectDef, RelationshipType
from registry.purview_registry import PurviewRegistry

registry = PurviewRegistry("feathrazuretest3-purview1")
projects = registry.get_projects()

entity_id_by_name = registry.get_entity_id("yihui_test_registry")
entity_id_by_id = registry.get_entity_id(entity_id_by_name)

entity_object = registry.get_entity("yihui_test_registry")
entity_object_full = registry.get_entity("yihui_test_registry",True)
assert len(entity_object.attributes.anchor_features)==0
assert len(entity_object_full.attributes.anchor_features)==1
assert len(entity_object_full.attributes.derived_features)==1

anchor_object_full = registry.get_entity("yihui_test_registry__anchor1",True)
assert len(anchor_object_full.attributes.features)==1

derived_object_full = registry.get_entity("yihui_test_registry__df1",True)
assert len(derived_object_full.attributes.input_features)==1
assert len(derived_object_full.attributes.input_anchor_features)==1

entity_list=registry.get_entities([registry.get_entity_id(x) for x in [
    'yihui_test_registry',
    'yihui_test_registry__source1',
    'yihui_test_registry__anchor1',
    'yihui_test_registry__anchor1__af1',
    'yihui_test_registry__df1']],True)

print(entity_list)
assert len(entity_list)==5

# project contains anchor group, anchor feature, derived feature and data source
neighbors = registry.get_neighbors("yihui_test_registry",RelationshipType.Contains)
assert len(neighbors)==4

# anchor group contains anchor feature
neighbors = registry.get_neighbors("yihui_test_registry__anchor1",RelationshipType.Contains)
assert len(neighbors)==1

# source produces anchor feature and anchor group
neighbors = registry.get_neighbors("yihui_test_registry__source1",RelationshipType.Produces)
assert len(neighbors)==2

df_lineage = registry.get_lineage('yihui_test_registry__df1')
# df1 Consumes af1 , af1 consumes source
assert len(df_lineage.entities)==3
assert len(df_lineage.edges)==2

anchor_lineage = registry.get_lineage('yihui_test_registry__anchor1')
# anchor CONTAINS feature (which is not captured in lineage)
# anchor consumes source
assert len(anchor_lineage.entities)==2
assert len(anchor_lineage.edges)==1