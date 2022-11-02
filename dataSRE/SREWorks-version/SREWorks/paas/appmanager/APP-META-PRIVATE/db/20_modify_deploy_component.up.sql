ALTER TABLE `am_deploy_component`
    DROP COLUMN `env_id`,
    CHANGE COLUMN `deploy_ext` `deploy_component_schema`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '部署过程中的 ComponentSchema 数据' AFTER `deploy_process_id`,
    CHANGE COLUMN `deploy_options` `deploy_component_options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '部署过程中的 ComponentOptions 数据' AFTER `deploy_component_schema`;

ALTER TABLE `am_deploy_app`
    CHANGE COLUMN `deploy_ext` `deploy_app_configuration` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '部署过程中的 Application Configuration 配置 (YAML)' AFTER `deploy_creator`;

ALTER TABLE `am_component_package`
    CHANGE COLUMN `package_ext` `component_schema` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '包的 ComponentSchema 信息 (YAML)' AFTER `component_name`,
    MODIFY COLUMN `component_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件类型' AFTER `package_creator`,
    MODIFY COLUMN `package_addon` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '当前 Component Package 的 Addon 依赖关系配置 (JSON)' AFTER `package_md5`,
    MODIFY COLUMN `package_options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '当前 Component Package 在构建时传入的 options 配置信息 (JSON)' AFTER `package_addon`;

ALTER TABLE `am_app_package`
    CHANGE COLUMN `package_ext` `app_schema` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '应用包 Schema 定义信息 (YAML)' AFTER `package_creator`;