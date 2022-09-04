

alter table productops_node
    add `app_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL comment '应用ID';

alter table productops_tab
    add `app_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL comment '应用ID';

update productops_node set app_id = SUBSTRING_INDEX(node_type_path,'|',1);
update productops_node_element set app_id = SUBSTRING_INDEX(node_type_path,'|',1);
update productops_tab set app_id = SUBSTRING_INDEX(node_type_path,'|',1);