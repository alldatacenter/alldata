

alter table productops_element
    add `is_import` int(10) default "0" not null comment '是否被导入';

update productops_element set is_import = 1 where is_import = 0;

alter table productops_node
    add `is_import` int(10) default "0" not null comment '是否被导入';

update productops_node set is_import = 1 where is_import = 0;

alter table productops_node_element
    add `is_import` int(10) default "0" not null comment '是否被导入';

update productops_node_element set is_import = 1 where is_import = 0;

alter table productops_tab
    add `is_import` int(10) default "0" not null comment '是否被导入';

update productops_tab set is_import = 1 where is_import = 0;

alter table productops_component
    add `is_import` int(10) default "0" not null comment '是否被导入';

update productops_component set is_import = 1 where is_import = 0;

