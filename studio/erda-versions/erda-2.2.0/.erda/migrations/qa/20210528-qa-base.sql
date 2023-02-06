-- MIGRATION_BASE

CREATE TABLE `dice_api_test`
(
    `id`            int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`    datetime    DEFAULT NULL COMMENT '创建时间',
    `updated_at`    datetime    DEFAULT NULL COMMENT '更新时间',
    `usecase_id`    int(11) DEFAULT NULL COMMENT '所属用例 ID',
    `usecase_order` int(11) DEFAULT NULL COMMENT '接口顺序',
    `status`        varchar(16) DEFAULT NULL COMMENT '接口执行状态',
    `api_info`      longtext COMMENT 'API 信息',
    `api_request`   longtext COMMENT 'API 请求体',
    `api_response`  longtext COMMENT 'API 响应',
    `assert_result` text COMMENT '断言接口',
    `project_id`    int(11) DEFAULT NULL COMMENT '项目 ID',
    `pipeline_id`   int(11) DEFAULT NULL COMMENT '关联的流水线 ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-接口信息表';

CREATE TABLE `dice_api_test_env`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at` timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `env_id`     int(11) NOT NULL COMMENT '环境 ID',
    `env_type`   varchar(64)  DEFAULT NULL COMMENT '环境类型，分为项目级和用例级',
    `name`       varchar(255) DEFAULT NULL COMMENT '配置名',
    `domain`     varchar(255) DEFAULT NULL COMMENT '域名',
    `header`     text COMMENT '公共请求头',
    `global`     text COMMENT '全局变量配置',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-接口测试-环境配置表';

CREATE TABLE `dice_autotest_filetree_nodes`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`       varchar(1)   NOT NULL COMMENT '节点类型, f: 文件, d: 目录',
    `scope`      varchar(191) NOT NULL COMMENT 'scope，例如 project-autotest, project-autotest-testplan',
    `scope_id`   varchar(191) NOT NULL COMMENT 'scope 的具体 ID，例如 项目 ID，测试计划 ID',
    `pinode`     bigint(20) NOT NULL COMMENT '父节点 inode',
    `inode`      bigint(20) NOT NULL COMMENT 'inode',
    `name`       varchar(191) NOT NULL COMMENT '节点名',
    `desc`       varchar(512) DEFAULT NULL COMMENT '描述',
    `creator_id` varchar(191) DEFAULT NULL COMMENT '创建人',
    `updater_id` varchar(191) DEFAULT NULL COMMENT '更新人',
    `created_at` datetime     NOT NULL COMMENT '创建时间',
    `updated_at` datetime     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY          `idx_inode` (`inode`),
    KEY          `idx_pinode` (`pinode`),
    KEY          `idx_type_scope_pinode_inode` (`type`,`scope`,`scope_id`,`pinode`,`inode`),
    KEY          `idx_scope_pinode_inode` (`scope`,`scope_id`,`pinode`,`inode`),
    KEY          `idx_pinode_inode` (`pinode`,`inode`),
    KEY          `idx_pinode_name` (`pinode`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试目录树节点表';

CREATE TABLE `dice_autotest_filetree_nodes_histories`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `inode`          bigint(20) NOT NULL COMMENT '节点的node id',
    `pinode`         bigint(20) NOT NULL COMMENT '父节点的 node id',
    `pipeline_yml`   mediumtext   NOT NULL COMMENT '节点的yml标识',
    `snippet_action` mediumtext   NOT NULL COMMENT 'snippet config 配置',
    `name`           varchar(191) NOT NULL COMMENT '名称',
    `desc`           varchar(512) NOT NULL COMMENT '描述',
    `creator_id`     varchar(191) NOT NULL COMMENT '创建人',
    `updater_id`     varchar(191) NOT NULL COMMENT '更新人',
    `extra`          mediumtext   NOT NULL COMMENT '其他信息',
    `created_at`     datetime     NOT NULL COMMENT '创建时间',
    `updated_at`     datetime     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY              `idx_inode` (`inode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试目录树节点历史表';

CREATE TABLE `dice_autotest_filetree_nodes_meta`
(
    `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `inode`          bigint(20) NOT NULL,
    `pipeline_yml`   mediumtext,
    `snippet_action` mediumtext,
    `extra`          mediumtext,
    `created_at`     datetime NOT NULL,
    `updated_at`     datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY              `idx_inode` (`inode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试目录树节点元信息表';

CREATE TABLE `dice_autotest_plan`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at` timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `name`       varchar(191) DEFAULT NULL COMMENT '测试计划名称',
    `desc`       varchar(512) DEFAULT NULL COMMENT '测试计划描述',
    `creator_id` varchar(191) DEFAULT NULL COMMENT '创建人',
    `updater_id` varchar(191) DEFAULT NULL COMMENT '更新人',
    `space_id`   bigint(20) NOT NULL COMMENT '测试空间id',
    `project_id` bigint(20) DEFAULT NULL COMMENT '项目id',
    PRIMARY KEY (`id`),
    KEY          `idx_name` (`name`),
    KEY          `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试计划表';

CREATE TABLE `dice_autotest_plan_members`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `test_plan_id` bigint(20) DEFAULT NULL COMMENT '测试计划id',
    `role`         varchar(32) DEFAULT NULL COMMENT '角色',
    `user_id`      bigint(20) DEFAULT NULL COMMENT '用户id',
    `created_at`   datetime    DEFAULT NULL COMMENT '创建时间',
    `updated_at`   datetime    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动测试-测试计划成员表';

CREATE TABLE `dice_autotest_plan_step`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`   timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `updated_at`   timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `plan_id`      bigint(20) NOT NULL COMMENT '测试计划id',
    `scene_set_id` bigint(20) NOT NULL COMMENT '场景集id',
    `pre_id`       bigint(20) NOT NULL COMMENT '前节点',
    PRIMARY KEY (`id`),
    KEY            `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试计划步骤表';

CREATE TABLE `dice_autotest_scene`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`  datetime     NOT NULL COMMENT '创建时间',
    `updated_at`  datetime     NOT NULL COMMENT '更新时间',
    `name`        varchar(191) NOT NULL COMMENT '名称',
    `description` text         NOT NULL COMMENT '描述',
    `space_id`    bigint(20) NOT NULL COMMENT '测试空间id',
    `set_id`      bigint(20) NOT NULL COMMENT '场景集id',
    `pre_id`      bigint(20) NOT NULL COMMENT '前节点',
    `creator_id`  varchar(255) NOT NULL COMMENT '创建人',
    `updater_id`  varchar(255) NOT NULL COMMENT '更新人',
    `status`      varchar(255) DEFAULT NULL COMMENT '执行状态',
    `ref_set_id`  bigint(20) NOT NULL DEFAULT '0' COMMENT '引用场景集的id',
    PRIMARY KEY (`id`),
    KEY           `idx_set_id` (`set_id`),
    KEY           `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试场景表';

CREATE TABLE `dice_autotest_scene_input`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`  datetime     NOT NULL COMMENT '创建时间',
    `updated_at`  datetime     NOT NULL COMMENT '更新时间',
    `name`        varchar(255) NOT NULL COMMENT '名称',
    `value`       text         NOT NULL COMMENT '默认值',
    `temp`        text         NOT NULL COMMENT '当前值',
    `description` text         NOT NULL COMMENT '描述',
    `scene_id`    bigint(20) NOT NULL COMMENT '场景id',
    `space_id`    bigint(20) NOT NULL COMMENT '空间id',
    `creator_id`  varchar(255) NOT NULL COMMENT '创建人',
    `updater_id`  varchar(255) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY           `idx_scene_id` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试场景入参表';

CREATE TABLE `dice_autotest_scene_output`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`  datetime     NOT NULL COMMENT '创建时间',
    `updated_at`  datetime     NOT NULL COMMENT '更新时间',
    `name`        varchar(255) NOT NULL COMMENT '名称',
    `value`       text         NOT NULL COMMENT '值表达式',
    `description` text         NOT NULL COMMENT '描述',
    `scene_id`    bigint(20) NOT NULL COMMENT '场景id',
    `space_id`    bigint(20) NOT NULL COMMENT '空间id',
    `creator_id`  varchar(255) NOT NULL COMMENT '创建人',
    `updater_id`  varchar(255) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY           `idx_scene_id` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试场景出参表';

CREATE TABLE `dice_autotest_scene_set`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(191) DEFAULT NULL,
    `description` varchar(255) DEFAULT NULL COMMENT '场景集描述',
    `space_id`    bigint(20) unsigned NOT NULL COMMENT '测试空间id',
    `pre_id`      bigint(20) unsigned DEFAULT NULL COMMENT '上一个节点id',
    `creator_id`  varchar(255) NOT NULL COMMENT '创建人',
    `updater_id`  varchar(255) NOT NULL COMMENT '更新人',
    `created_at`  datetime     DEFAULT NULL COMMENT '创建时间',
    `updated_at`  datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景集表';

CREATE TABLE `dice_autotest_scene_step`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `type`        varchar(255) NOT NULL COMMENT '类型',
    `value`       mediumtext,
    `name`        varchar(255) NOT NULL COMMENT '名称',
    `pre_id`      bigint(20) NOT NULL COMMENT '前节点',
    `scene_id`    bigint(20) NOT NULL COMMENT '场景id',
    `space_id`    bigint(20) NOT NULL COMMENT '空间id',
    `creator_id`  varchar(255) NOT NULL COMMENT '创建人',
    `updater_id`  varchar(255) NOT NULL COMMENT '更新人',
    `pre_type`    varchar(255) NOT NULL COMMENT '排序类型',
    `api_spec_id` varchar(50)           DEFAULT NULL COMMENT 'api集市id',
    PRIMARY KEY (`id`),
    KEY           `idx_scene_id` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试场景步骤表';

CREATE TABLE `dice_autotest_space`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`            varchar(255) NOT NULL COMMENT '测试空间名称',
    `project_id`      bigint(20) NOT NULL COMMENT '项目id',
    `description`     varchar(1024) DEFAULT NULL,
    `creator_id`      varchar(255) NOT NULL COMMENT '创建人',
    `created_at`      datetime     NOT NULL COMMENT '创建时间',
    `updated_at`      datetime     NOT NULL COMMENT '更新时间',
    `deleted_at`      datetime      DEFAULT NULL COMMENT '删除时间',
    `source_space_id` bigint(20) DEFAULT NULL COMMENT '被复制的源测试空间',
    `status`          varchar(255) NOT NULL COMMENT '测试空间状态',
    `updater_id`      varchar(255) NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试空间表';

CREATE TABLE `dice_issue_testcase_relations`
(
    `id`                    bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `issue_id`              bigint(20) DEFAULT NULL,
    `test_plan_id`          bigint(20) DEFAULT NULL,
    `test_plan_case_rel_id` bigint(20) DEFAULT NULL,
    `test_case_id`          bigint(20) DEFAULT NULL,
    `creator_id`            varchar(191) DEFAULT NULL,
    `created_at`            datetime     DEFAULT NULL,
    `updated_at`            datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件测试用例关联表';

CREATE TABLE `dice_test_cases`
(
    `id`               int(10) unsigned NOT NULL AUTO_INCREMENT,
    `name`             varchar(191)  DEFAULT NULL,
    `project_id`       bigint(20) DEFAULT NULL,
    `test_set_id`      bigint(20) DEFAULT NULL,
    `priority`         varchar(191)  DEFAULT NULL,
    `pre_condition`    text,
    `step_and_results` text,
    `desc`             varchar(1024) DEFAULT NULL,
    `recycled`         tinyint(1) DEFAULT NULL,
    `from`             varchar(191)  DEFAULT NULL,
    `creator_id`       varchar(191)  DEFAULT NULL,
    `updater_id`       varchar(191)  DEFAULT NULL,
    `created_at`       datetime      DEFAULT NULL,
    `updated_at`       datetime      DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-测试用例表';

CREATE TABLE `dice_test_plan_case_relations`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `test_plan_id` bigint(20) DEFAULT NULL,
    `test_set_id`  bigint(20) DEFAULT NULL,
    `test_case_id` bigint(20) DEFAULT NULL,
    `exec_status`  varchar(191) DEFAULT NULL,
    `creator_id`   varchar(191) DEFAULT NULL,
    `updater_id`   varchar(191) DEFAULT NULL,
    `executor_id`  varchar(191) DEFAULT NULL,
    `created_at`   datetime     DEFAULT NULL,
    `updated_at`   datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-测试计划用例关联表';

CREATE TABLE `dice_test_plan_members`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `test_plan_id` bigint(20) DEFAULT NULL,
    `role`         varchar(32) DEFAULT NULL,
    `user_id`      bigint(20) DEFAULT NULL,
    `created_at`   datetime    DEFAULT NULL,
    `updated_at`   datetime    DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-测试计划成员表';

CREATE TABLE `dice_test_plans`
(
    `id`         int(11) unsigned NOT NULL AUTO_INCREMENT,
    `name`       varchar(191) DEFAULT NULL,
    `status`     varchar(191) DEFAULT NULL,
    `project_id` bigint(20) DEFAULT NULL,
    `summary`    text,
    `creator_id` varchar(191) DEFAULT NULL,
    `updater_id` varchar(191) DEFAULT NULL,
    `started_at` datetime     DEFAULT NULL,
    `ended_at`   datetime     DEFAULT NULL,
    `type`       varchar(1)   DEFAULT 'm',
    `inode`      varchar(20)  DEFAULT NULL,
    `created_at` datetime     DEFAULT NULL,
    `updated_at` datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-测试计划表';

CREATE TABLE `dice_test_sets`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `name`       varchar(256) NOT NULL COMMENT '测试集的中文名,可重名',
    `parent_id`  bigint(20) NOT NULL DEFAULT '0' COMMENT '上一级的所属测试集id,顶级时为0',
    `recycled`   tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已进入回收站，默认0为否，1为是。在回收站的顶层显示',
    `directory`  text         NOT NULL COMMENT '当前节点+所有父级节点的name集合（参考值：新建测试集1/新建测试集2/测试集名称3），这里冗余是为了方便界面展示。',
    `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目id，当前测试集所属的真正项目id',
    `order_num`  int(4) NOT NULL DEFAULT '0' COMMENT '用例集展示的顺序',
    `creator_id` varchar(191) NOT NULL DEFAULT '' COMMENT '创建人',
    `updater_id` varchar(191)          DEFAULT NULL COMMENT '修改人',
    `created_at` datetime              DEFAULT NULL COMMENT '创建时间',
    `updated_at` datetime              DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY          `idx_test_project_id` (`project_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手动测试-测试集表';

CREATE TABLE `qa_sonar`
(
    `id`                int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `key`               varchar(191) NOT NULL DEFAULT '' COMMENT '分析代码的key',
    `bugs`              longtext COMMENT '代码bug数量',
    `code_smells`       longtext COMMENT '代码异味数量',
    `vulnerabilities`   longtext COMMENT '代码漏洞数量',
    `coverage`          longtext COMMENT '代码覆盖率',
    `duplications`      longtext COMMENT '代码重复率',
    `issues_statistics` text COMMENT '代码质量统计',
    `created_at`        datetime              DEFAULT NULL COMMENT '创建时间',
    `updated_at`        datetime              DEFAULT NULL COMMENT '更新时间',
    `app_id`            bigint(20) DEFAULT NULL COMMENT '应用id',
    `operator_id`       varchar(255)          DEFAULT NULL COMMENT '用户id',
    `project_id`        bigint(20) DEFAULT NULL COMMENT '项目id',
    `commit_id`         varchar(50)           DEFAULT NULL COMMENT '提交id',
    `branch`            varchar(255)          DEFAULT NULL COMMENT '代码分支',
    `git_repo`          varchar(255)          DEFAULT NULL COMMENT 'git仓库地址',
    `build_id`          bigint(20) DEFAULT NULL COMMENT '创建id',
    `log_id`            varchar(40)           DEFAULT NULL COMMENT '日志id',
    `app_name`          varchar(255)          DEFAULT NULL COMMENT '应用名称',
    PRIMARY KEY (`id`),
    KEY                 `index_name` (`commit_id`),
    KEY                 `app_id` (`app_id`),
    KEY                 `idx_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sonar 代码质量扫描结果表';

CREATE TABLE `qa_sonar_metric_keys`
(
    `id`              int(20) NOT NULL AUTO_INCREMENT,
    `metric_key`      varchar(50)  NOT NULL COMMENT 'key',
    `value_type`      varchar(50)  NOT NULL COMMENT '值的类型',
    `name`            varchar(50)  NOT NULL COMMENT '名称',
    `metric_key_desc` varchar(255) NOT NULL COMMENT 'key的英文描述',
    `domain`          varchar(50)  NOT NULL COMMENT '所属类型',
    `operational`     varchar(20)  NOT NULL COMMENT '操作',
    `qualitative`     tinyint(1) NOT NULL COMMENT '是否是增量',
    `hidden`          tinyint(1) NOT NULL COMMENT '是否隐藏',
    `custom`          tinyint(1) NOT NULL COMMENT '是否是本地',
    `decimal_scale`   tinyint(5) NOT NULL COMMENT '小数点后几位',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COMMENT='代码质量扫描规则项';


INSERT INTO `qa_sonar_metric_keys` (`id`, `metric_key`, `value_type`, `name`, `metric_key_desc`, `domain`,
                                    `operational`, `qualitative`, `hidden`, `custom`, `decimal_scale`)
VALUES (1, 'blocker_violations', 'INT', 'Blocker Issues', 'Blocker issues', 'Issues', '-1', 1, 0, 0, 0),
       (2, 'bugs', 'INT', 'Bugs', 'Bugs', 'Reliability', '-1', 0, 0, 0, 0),
       (3, 'burned_budget', 'FLOAT', 'Burned budget', '', 'Management', '-1', 0, 0, 1, 1),
       (4, 'business_value', 'FLOAT', 'Business value', '', 'Management', '1', 1, 0, 1, 1),
       (5, 'classes', 'INT', 'Classes', 'Classes', 'Size', '-1', 0, 0, 0, 0),
       (6, 'code_smells', 'INT', 'Code Smells', 'Code Smells', 'Maintainability', '-1', 0, 0, 0, 0),
       (7, 'cognitive_complexity', 'INT', 'Cognitive Complexity', 'Cognitive complexity', 'Complexity', '-1', 0, 0, 0,
        0),
       (8, 'comment_lines', 'INT', 'Comment Lines', 'Number of comment lines', 'Size', '1', 0, 0, 0, 0),
       (9, 'comment_lines_density', 'PERCENT', 'Comments (%)', 'Comments balanced by ncloc + comment lines', 'Size',
        '1', 1, 0, 0, 1),
       (10, 'branch_coverage', 'PERCENT', 'Condition Coverage', 'Condition coverage', 'Coverage', '1', 1, 0, 0, 1),
       (11, 'conditions_to_cover', 'INT', 'Conditions to Cover', 'Conditions to cover', 'Coverage', '-1', 0, 0, 0, 0),
       (12, 'confirmed_issues', 'INT', 'Confirmed Issues', 'Confirmed issues', 'Issues', '-1', 1, 0, 0, 0),
       (13, 'coverage', 'PERCENT', 'Coverage', 'Coverage by tests', 'Coverage', '1', 1, 0, 0, 1),
       (14, 'critical_violations', 'INT', 'Critical Issues', 'Critical issues', 'Issues', '-1', 1, 0, 0, 0),
       (15, 'complexity', 'INT', 'Cyclomatic Complexity', 'Cyclomatic complexity', 'Complexity', '-1', 0, 0, 0, 0),
       (16, 'directories', 'INT', 'Directories', 'Directories', 'Size', '-1', 0, 0, 0, 0),
       (17, 'duplicated_blocks', 'INT', 'Duplicated Blocks', 'Duplicated blocks', 'Duplications', '-1', 1, 0, 0, 0),
       (18, 'duplicated_files', 'INT', 'Duplicated Files', 'Duplicated files', 'Duplications', '-1', 1, 0, 0, 0),
       (19, 'duplicated_lines', 'INT', 'Duplicated Lines', 'Duplicated lines', 'Duplications', '-1', 1, 0, 0, 0),
       (20, 'duplicated_lines_density', 'PERCENT', 'Duplicated Lines (%)', 'Duplicated lines balanced by statements',
        'Duplications', '-1', 1, 0, 0, 1),
       (21, 'effort_to_reach_maintainability_rating_a', 'WORK_DUR', 'Effort to Reach Maintainability Rating A',
        'Effort to reach maintainability rating A', 'Maintainability', '-1', 1, 0, 0, 0),
       (22, 'false_positive_issues', 'INT', 'False Positive Issues', 'False positive issues', 'Issues', '-1', 0, 0, 0,
        0),
       (23, 'files', 'INT', 'Files', 'Number of files', 'Size', '-1', 0, 0, 0, 0),
       (24, 'functions', 'INT', 'Functions', 'Functions', 'Size', '-1', 0, 0, 0, 0),
       (25, 'generated_lines', 'INT', 'Generated Lines', 'Number of generated lines', 'Size', '-1', 0, 0, 0, 0),
       (26, 'generated_ncloc', 'INT', 'Generated Lines of Code', 'Generated non Commenting Lines of Code', 'Size', '-1',
        0, 0, 0, 0),
       (27, 'info_violations', 'INT', 'Info Issues', 'Info issues', 'Issues', '-1', 1, 0, 0, 0),
       (28, 'violations', 'INT', 'Issues', 'Issues', 'Issues', '-1', 1, 0, 0, 0),
       (29, 'line_coverage', 'PERCENT', 'Line Coverage', 'Line coverage', 'Coverage', '1', 1, 0, 0, 1),
       (30, 'lines', 'INT', 'Lines', 'Lines', 'Size', '-1', 0, 0, 0, 0),
       (31, 'ncloc', 'INT', 'Lines of Code', 'Non commenting lines of code', 'Size', '-1', 0, 0, 0, 0),
       (32, 'lines_to_cover', 'INT', 'Lines to Cover', 'Lines to cover', 'Coverage', '-1', 0, 0, 0, 0),
       (33, 'sqale_rating', 'RATING', 'Maintainability Rating', 'A-to-E rating based on the technical debt ratio',
        'Maintainability', '-1', 1, 0, 0, 0),
       (34, 'major_violations', 'INT', 'Major Issues', 'Major issues', 'Issues', '-1', 1, 0, 0, 0),
       (35, 'minor_violations', 'INT', 'Minor Issues', 'Minor issues', 'Issues', '-1', 1, 0, 0, 0),
       (36, 'open_issues', 'INT', 'Open Issues', 'Open issues', 'Issues', '-1', 0, 0, 0, 0),
       (37, 'projects', 'INT', 'Projects', 'Number of projects', 'Size', '-1', 0, 0, 0, 0),
       (38, 'reliability_rating', 'RATING', 'Reliability Rating', 'Reliability rating', 'Reliability', '-1', 1, 0, 0,
        0),
       (39, 'reliability_remediation_effort', 'WORK_DUR', 'Reliability Remediation Effort',
        'Reliability Remediation Effort', 'Reliability', '-1', 1, 0, 0, 0),
       (40, 'reopened_issues', 'INT', 'Reopened Issues', 'Reopened issues', 'Issues', '-1', 1, 0, 0, 0),
       (41, 'security_hotspots_reviewed', 'PERCENT', 'Security Hotspots Reviewed',
        'Percentage of Security Hotspots Reviewed', 'SecurityReview', '1', 1, 0, 0, 1),
       (42, 'security_rating', 'RATING', 'Security Rating', 'Security rating', 'Security', '-1', 1, 0, 0, 0),
       (43, 'security_remediation_effort', 'WORK_DUR', 'Security Remediation Effort', 'Security remediation effort',
        'Security', '-1', 1, 0, 0, 0),
       (44, 'security_review_rating', 'RATING', 'Security Review Rating', 'Security Review Rating', 'SecurityReview',
        '-1', 1, 0, 0, 0),
       (45, 'skipped_tests', 'INT', 'Skipped Unit Tests', 'Number of skipped unit tests', 'Coverage', '-1', 1, 0, 0, 0),
       (46, 'statements', 'INT', 'Statements', 'Number of statements', 'Size', '-1', 0, 0, 0, 0),
       (47, 'team_size', 'INT', 'Team size', '', 'Management', '-1', 0, 0, 1, 0),
       (48, 'sqale_index', 'WORK_DUR', 'Technical Debt',
        'Total effort (in hours) to fix all the issues on the component and therefore to comply to all the requirements.',
        'Maintainability', '-1', 1, 0, 0, 0),
       (49, 'sqale_debt_ratio', 'PERCENT', 'Technical Debt Ratio',
        'Ratio of the actual technical debt compared to the estimated cost to develop the whole source code from scratch',
        'Maintainability', '-1', 1, 0, 0, 1),
       (50, 'uncovered_conditions', 'INT', 'Uncovered Conditions', 'Uncovered conditions', 'Coverage', '-1', 0, 0, 0,
        0),
       (51, 'uncovered_lines', 'INT', 'Uncovered Lines', 'Uncovered lines', 'Coverage', '-1', 0, 0, 0, 0),
       (52, 'test_execution_time', 'MILLISEC', 'Unit Test Duration', 'Execution duration of unit tests', 'Coverage',
        '-1', 0, 0, 0, 0),
       (53, 'test_errors', 'INT', 'Unit Test Errors', 'Number of unit test errors', 'Coverage', '-1', 1, 0, 0, 0),
       (54, 'test_failures', 'INT', 'Unit Test Failures', 'Number of unit test failures', 'Coverage', '-1', 1, 0, 0, 0),
       (55, 'test_success_density', 'PERCENT', 'Unit Test Success (%)', 'Density of successful unit tests', 'Coverage',
        '1', 1, 0, 0, 1),
       (56, 'tests', 'INT', 'Unit Tests', 'Number of unit tests', 'Coverage', '1', 0, 0, 0, 0),
       (57, 'vulnerabilities', 'INT', 'Vulnerabilities', 'Vulnerabilities', 'Security', '-1', 0, 0, 0, 0),
       (58, 'wont_fix_issues', 'INT', 'Wont Fix Issues', 'Wont fix issues', 'Issues', '-1', 0, 0, 0, 0),
       (59, 'burned_budget', 'FLOAT', 'Burned budget', '', 'Management', '1', 0, 0, 1, 1),
       (60, 'team_size', 'INT', 'Team size', '', 'Management', '1', 0, 0, 1, 0);

CREATE TABLE `qa_sonar_metric_rules`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `description`   varchar(150) NOT NULL COMMENT '描述',
    `created_at`    datetime     NOT NULL COMMENT '创建时间',
    `updated_at`    datetime     NOT NULL COMMENT '更新时间',
    `scope_type`    varchar(50)  NOT NULL COMMENT '所属类型',
    `scope_id`      varchar(50)  NOT NULL COMMENT '所属类型的id',
    `metric_key_id` varchar(255) NOT NULL COMMENT '指标的id',
    `metric_value`  varchar(255) NOT NULL COMMENT '指标的值',
    PRIMARY KEY (`id`),
    KEY             `scope` (`scope_type`,`scope_id`) USING BTREE COMMENT '指标规则的所属类型和id'
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COMMENT='代码质量扫描规则配置表';


INSERT INTO `qa_sonar_metric_rules` (`id`, `description`, `created_at`, `updated_at`, `scope_type`, `scope_id`,
                                     `metric_key_id`, `metric_value`)
VALUES (47, '', '2020-11-29 20:47:00', '2020-11-29 20:47:00', 'platform', '-1', '13', '80.0'),
       (48, '', '2020-11-29 20:47:00', '2020-11-29 20:47:00', 'platform', '-1', '20', '3.0'),
       (49, '', '2020-11-29 20:47:00', '2020-11-29 20:47:00', 'platform', '-1', '33', '1'),
       (50, '', '2020-11-29 20:47:00', '2020-11-29 20:47:00', 'platform', '-1', '38', '1'),
       (51, '', '2020-11-29 20:47:00', '2020-11-29 20:47:00', 'platform', '-1', '41', '100'),
       (52, '', '2020-11-29 20:47:00', '2020-11-29 20:47:00', 'platform', '-1', '42', '1');

CREATE TABLE `qa_test_records`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `created_at`    datetime      DEFAULT NULL COMMENT '创建时间',
    `updated_at`    datetime      DEFAULT NULL COMMENT '更新时间',
    `name`          varchar(255)  DEFAULT NULL COMMENT '名称',
    `app_id`        bigint(20) DEFAULT NULL COMMENT '应用id',
    `operator_id`   varchar(255)  DEFAULT NULL COMMENT '操作者id',
    `output`        varchar(1024) DEFAULT NULL COMMENT '测试结果输出地址',
    `type`          varchar(20)   DEFAULT NULL COMMENT '测试类型',
    `parser_type`   varchar(255)  DEFAULT NULL COMMENT '测试 parser 类型， JUNIT/TESTNG',
    `totals`        mediumtext COMMENT '测试用例执行结果及耗时分布',
    `desc`          varchar(1024) DEFAULT NULL COMMENT '描述',
    `extra`         varchar(1024) DEFAULT NULL COMMENT '附加信息',
    `project_id`    bigint(20) DEFAULT NULL COMMENT '项目id',
    `commit_id`     varchar(191)  DEFAULT NULL COMMENT '测试代码 commit id',
    `branch`        varchar(255)  DEFAULT NULL COMMENT '代码分支',
    `git_repo`      varchar(255)  DEFAULT NULL COMMENT 'git仓库地址',
    `envs`          varchar(1024) DEFAULT NULL COMMENT '环境变量',
    `case_dir`      varchar(255)  DEFAULT NULL COMMENT '执行目录',
    `workspace`     varchar(255)  DEFAULT NULL COMMENT '应用对应环境',
    `build_id`      bigint(20) DEFAULT NULL COMMENT '构建id',
    `app_name`      varchar(255)  DEFAULT NULL COMMENT '应用名字',
    `uuid`          varchar(40)   DEFAULT NULL COMMENT '用户id',
    `suites`        longtext COMMENT '测试结果数据',
    `operator_name` varchar(255)  DEFAULT NULL COMMENT '操作者名字',
    PRIMARY KEY (`id`),
    KEY             `app_id` (`app_id`),
    KEY             `test_type` (`type`),
    KEY             `commit_id` (`commit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单元测试执行记录表';

