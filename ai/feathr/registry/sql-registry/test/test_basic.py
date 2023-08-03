import registry
from registry.models import EntityType
r=registry.DbRegistry()

l=r.get_lineage('226b42ee-0c34-4329-b935-744aecc63fb4').to_dict()
assert(len(l["guidEntityMap"]) == 4)

af1=r.get_entity('2380fe5b-ce2a-401e-98bf-af8b98460f67')
af2=r.get_entity('feathr_ci_registry_12_33_182947__request_features__f_day_of_week')
assert(af1.to_dict()==af2.to_dict())
df1=r.get_entity('226b42ee-0c34-4329-b935-744aecc63fb4')
df2=r.get_entity('feathr_ci_registry_12_33_182947__f_trip_time_distance')
assert(df1.to_dict()==df2.to_dict())

p=r.get_project('feathr_ci_registry_12_33_182947')
assert(len(p.to_dict()['guidEntityMap'])==14)

es=r.search_entity("time", [registry.EntityType.DerivedFeature])
qns=set([e.qualified_name for e in es])
assert qns == set(['feathr_ci_registry_12_33_182947__f_trip_time_distance', 'feathr_ci_registry_12_33_182947__f_trip_time_rounded', 'feathr_ci_registry_12_33_182947__f_trip_time_rounded_plus'])
