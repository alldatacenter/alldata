OUTPUT_PATH_TAG = "output_path"
# spark config for output format setting
OUTPUT_FORMAT = "spark.feathr.outputFormat"
REDIS_PASSWORD = 'REDIS_PASSWORD'

# 1MB = 1024*1024
MB_BYTES = 1048576

INPUT_CONTEXT="PASSTHROUGH"
RELATION_CONTAINS = "CONTAINS"
RELATION_BELONGSTO = "BELONGSTO"
RELATION_CONSUMES = "CONSUMES"
RELATION_PRODUCES = "PRODUCES"
# For use in registry. 
# For type definition, think it's like a schema of a table. 
# This version field is mainly to smooth possible future upgrades, 
# for example, backward incompatible changes should be introduced in v2, to make sure that features registered with v1 schema can still be used
REGISTRY_TYPEDEF_VERSION="v1"

TYPEDEF_SOURCE=f'feathr_source_{REGISTRY_TYPEDEF_VERSION}'
# TODO: change the name from feathr_workspace_ to feathr_project_
TYPEDEF_FEATHR_PROJECT=f'feathr_workspace_{REGISTRY_TYPEDEF_VERSION}'
TYPEDEF_DERIVED_FEATURE=f'feathr_derived_feature_{REGISTRY_TYPEDEF_VERSION}'
TYPEDEF_ANCHOR=f'feathr_anchor_{REGISTRY_TYPEDEF_VERSION}'
TYPEDEF_ANCHOR_FEATURE=f'feathr_anchor_feature_{REGISTRY_TYPEDEF_VERSION}'

TYPEDEF_ARRAY_ANCHOR=f"array<feathr_anchor_{REGISTRY_TYPEDEF_VERSION}>"
TYPEDEF_ARRAY_DERIVED_FEATURE=f"array<feathr_derived_feature_{REGISTRY_TYPEDEF_VERSION}>"
TYPEDEF_ARRAY_ANCHOR_FEATURE=f"array<feathr_anchor_feature_{REGISTRY_TYPEDEF_VERSION}>"


JOIN_CLASS_NAME="com.linkedin.feathr.offline.job.FeatureJoinJob"
GEN_CLASS_NAME="com.linkedin.feathr.offline.job.FeatureGenJob"