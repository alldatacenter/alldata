# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

Shell scripts to make REST calls to Atlas Server using curl.

1. Update env_atlas.sh with details to contact Atlas Server

2. Usage of scripts given below:

  --------------------------------------------------------------
  |      Command                           |     Arguments
  |----------------------------------------|--------------------
  | admin_status.sh
  |
  | typedefs_create.sh                       inputFileName
  | typedefs_get.sh                          [outputFileName]
  | typedefs_update.sh                       inputFileName
  | typedefs_delete.sh                       inputFileName
  | classificationdef_get.sh                 typeName [outputFileName]
  | entitydef_get.sh                         typeName [outputFileName]
  | enumdef_get.sh                           typeName [outputFileName]
  | structdef_get.sh                         typeName [outputFileName]
  |
  | entity_create.sh                         inputFileName
  | entity_get_by_type_and_unique_attr.sh    typeName attrName attrValue [outputFileName]
  | entity_get_by_guid.sh                    guid [outputFileName]
  | entity_update.sh                         inputFileName
  | entity_update_by_type_and_unique_attr.sh typeName attrName attrValue inputFileName [outputFileName]
  | entity_classifications_add.sh            guid inputFileName
  | entity_classifications_update.sh         guid inputFileName
  | entity_classifications_delete.sh         guid classification
  | entity_classification_bulk.sh            inputFileName
  | entity_delete_by_guid.sh                 guid
  |
  | search_basic.sh                          [queryString] [typeName] [classificationName] [limit] [offset] [outputFileName]
  | search_basic_with_attribute_filters.sh   inputFileName
  | search_dsl.sh                            queryString [limit] [offset] [outputFileName]
  |
  | export_entity_by_guid.sh                 guid [outputFileName]
  | export_entity_by_type_and_attr.sh        typeName attrName attrValue [matchType] [fetchType] [outputFileName]
  | import_zip.sh                            inputFileName
  -----------------------------------------|--------------------
