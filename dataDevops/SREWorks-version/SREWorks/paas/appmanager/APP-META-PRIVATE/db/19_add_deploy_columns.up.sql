ALTER TABLE `am_deploy_component`
    ADD COLUMN `deploy_global_params`  longtext NULL COMMENT '部署过程中的全局参数' AFTER `deploy_options`,
    ADD COLUMN `deploy_global_options` longtext NULL COMMENT '部署过程中的全局常量' AFTER `deploy_global_params`;

ALTER TABLE `am_deploy_app`
    ADD COLUMN `deploy_global_variables` longtext NULL COMMENT '部署过程中的全局常量' AFTER `deploy_global_params`;

ALTER TABLE `am_deploy_component`
    CHANGE COLUMN `deploy_global_options` `deploy_global_variables` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '部署过程中的全局常量' AFTER `deploy_global_params`;
