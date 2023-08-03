-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
/
CREATE INDEX x_service_conf_def_IDX_defid ON x_service_config_def(def_id);
CREATE INDEX x_resource_def_IDX_def_id ON x_resource_def(def_id);
CREATE INDEX x_access_type_def_IDX_def_id ON x_access_type_def(def_id);
CREATE INDEX x_atd_grants_IDX_atdid ON x_access_type_def_grants(atd_id);
CREATE INDEX x_cont_enr_def_IDX_defid ON x_context_enricher_def(def_id);
CREATE INDEX x_enum_def_IDX_def_id ON x_enum_def(def_id);
CREATE INDEX x_enum_element_def_IDX_defid ON x_enum_element_def(enum_def_id);

CREATE INDEX x_service_conf_map_IDX_service ON x_service_config_map(service);

CREATE INDEX x_policy_res_IDX_policy_id ON x_policy_resource(policy_id);
CREATE INDEX x_policy_res_IDX_res_def_id ON x_policy_resource(res_def_id);
CREATE INDEX x_policy_res_map_IDX_res_id ON x_policy_resource_map(resource_id);

CREATE INDEX x_policy_item_IDX_policy_id ON x_policy_item(policy_id);
CREATE INDEX x_plc_item_access_IDX_pi_id ON x_policy_item_access(policy_item_id);
CREATE INDEX x_plc_item_access_IDX_type ON x_policy_item_access(type);
CREATE INDEX x_plc_item_cond_IDX_pi_id ON x_policy_item_condition(policy_item_id);
CREATE INDEX x_plc_item_cond_IDX_type ON x_policy_item_condition(type);
CREATE INDEX x_plc_itm_usr_perm_IDX_pi_id ON x_policy_item_user_perm(policy_item_id);
CREATE INDEX x_plc_itm_usr_perm_IDX_user_id ON x_policy_item_user_perm(user_id);
CREATE INDEX x_plc_itm_grp_perm_IDX_pi_id ON x_policy_item_group_perm(policy_item_id);
CREATE INDEX x_plc_itm_grp_perm_IDX_grp_id ON x_policy_item_group_perm(group_id);

CREATE INDEX x_srvc_res_IDX_service_id ON x_service_resource(service_id);
CREATE INDEX x_srvc_res_el_IDX_res_def_id ON x_service_resource_element(res_id);
CREATE INDEX x_srvc_res_el_IDX_res_id ON x_service_resource_element(res_def_id);