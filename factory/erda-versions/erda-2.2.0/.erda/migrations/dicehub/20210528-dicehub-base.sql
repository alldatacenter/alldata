-- MIGRATION_BASE

CREATE TABLE `dice_app_publish_item_relation`
(
    `id`              bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`          bigint(20) NOT NULL COMMENT '应用ID',
    `publish_item_id` bigint(20) NOT NULL COMMENT '发布内容ID',
    `env`             varchar(100) NOT NULL DEFAULT '' COMMENT '环境',
    `creator`         varchar(255) NOT NULL DEFAULT '' COMMENT '创建用户',
    `created_at`      datetime     NOT NULL COMMENT '创建时间',
    `updated_at`      datetime              DEFAULT NULL COMMENT '更新时间',
    `ak`              varchar(64)           DEFAULT NULL COMMENT '监控AK',
    `ai`              varchar(50)           DEFAULT NULL COMMENT '监控AI,一般是应用名',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用发布关联表';

CREATE TABLE `dice_extension`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`   timestamp NULL DEFAULT NULL,
    `updated_at`   timestamp NULL DEFAULT NULL,
    `type`         varchar(128) DEFAULT NULL,
    `name`         varchar(255) DEFAULT NULL,
    `category`     varchar(255) DEFAULT NULL,
    `display_name` varchar(255) DEFAULT NULL,
    `logo_url`     varchar(255) DEFAULT NULL,
    `desc`         varchar(255) DEFAULT NULL,
    `public`       tinyint(1) DEFAULT NULL,
    `labels`       varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='action,addon扩展信息';

CREATE TABLE `dice_extension_version`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at`   timestamp NULL DEFAULT NULL,
    `updated_at`   timestamp NULL DEFAULT NULL,
    `extension_id` bigint(20) unsigned DEFAULT NULL,
    `name`         varchar(128) DEFAULT NULL,
    `version`      varchar(128) DEFAULT NULL,
    `spec`         text,
    `dice`         text,
    `swagger`      longtext,
    `readme`       longtext,
    `public`       tinyint(1) DEFAULT NULL,
    `is_default`   tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY            `idx_name` (`name`),
    KEY            `idx_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='action,addon扩展版本信息';

CREATE TABLE `dice_pipeline_template_versions`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `template_id` bigint(20) NOT NULL,
    `name`        varchar(255) NOT NULL,
    `version`     varchar(255) NOT NULL,
    `spec`        text         NOT NULL,
    `readme`      text         NOT NULL,
    `created_at`  datetime     NOT NULL,
    `updated_at`  datetime     NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COMMENT='流水线模板版本表';


INSERT INTO `dice_pipeline_template_versions` (`id`, `template_id`, `name`, `version`, `spec`, `readme`, `created_at`,
                                               `updated_at`)
VALUES (8, 8, 'custom', '1.0', 'name: custom\nversion: \"1.0\"\ndesc: 自定义模板\n\ntemplate: |\n  version: 1.1\n  stages:',
        '', '2020-10-14 10:05:17', '2020-10-14 10:05:17'),
       (9, 9, 'java-boot-gradle-dice', '1.0',
        'name: java-boot-gradle-dice\nversion: \"1.0\"\ndesc: springboot gradle 打包构建部署到 dice 的模板\n\ntemplate: |\n\n  version: 1.1\n  stages:\n    - stage:\n        - git-checkout:\n            params:\n              depth: 1\n    - stage:\n        - java-build:\n            version: \"1.0\"\n            params:\n              build_cmd:\n                - ./gradlew bootJar\n              jdk_version: 8\n              workdir: ${git-checkout}\n    - stage:\n        - release:\n            params:\n              dice_yml: ${git-checkout}/dice.yml\n              services:\n                dice.yml中的服务名:\n                  image: registry.cn-hangzhou.aliyuncs.com/terminus/terminus-openjdk:v11.0.6\n                  copys:\n                    - ${java-build:OUTPUT:buildPath}/build/jar包的路径/jar包的名称:/target/jar包的名称\n                  cmd: java -jar /target/jar包的名称\n\n    - stage:\n        - dice:\n            params:\n              release_id: ${release:OUTPUT:releaseID}\n\n\nparams:\n\n  - name: pipeline_version\n    desc: 生成的pipeline的版本\n    default: \"1.1\"\n    required: false\n\n  - name: pipeline_cron\n    desc: 定时任务的cron表达式\n    required: false\n\n  - name: pipeline_scheduling\n    desc: 流水线调度策略\n    required: false\n',
        '', '2020-10-14 10:05:49', '2020-10-14 13:35:57'),
       (10, 10, 'java-boot-maven-dice', '1.0',
        'name: java-boot-maven-dice\nversion: \"1.0\"\ndesc: springboot maven 打包构建部署到 dice 的模板\n\ntemplate: |\n\n  version: 1.1\n  stages:\n    - stage:\n        - git-checkout:\n            params:\n              depth: 1\n\n    - stage:\n        - java-build:\n            version: \"1.0\"\n            params:\n              build_cmd:\n                - mvn package\n              jdk_version: 8\n              workdir: ${git-checkout}\n\n    - stage:\n        - release:\n            params:\n              dice_yml: ${git-checkout}/dice.yml\n              services:\n                dice.yml中的服务名:\n                  image: registry.cn-hangzhou.aliyuncs.com/terminus/terminus-openjdk:v11.0.6\n                  copys:\n                    - ${java-build:OUTPUT:buildPath}/target/jar包的名称:/target/jar包的名称\n                  cmd: java -jar /target/jar包的名称\n\n    - stage:\n        - dice:\n            params:\n              release_id: ${release:OUTPUT:releaseID}\n\n\nparams:\n\n  - name: pipeline_version\n    desc: 生成的pipeline的版本\n    default: \"1.1\"\n    required: false\n\n  - name: pipeline_cron\n    desc: 定时任务的cron表达式\n    required: false\n\n  - name: pipeline_scheduling\n    desc: 流水线调度策略\n    required: false\n',
        '', '2020-10-14 10:06:16', '2020-10-14 13:36:28'),
       (11, 11, 'java-tomcat-maven-dice', '1.0',
        'name: java-tomcat-maven-dice\nversion: \"1.0\"\ndesc: java maven 打包构建放入 tomcat 部署到 dice 的模板\n\ntemplate: |\n\n  version: \"1.1\"\n  stages:\n    - stage:\n        - git-checkout:\n            params:\n              depth: 1\n\n    - stage:\n        - java-build:\n            version: \"1.0\"\n            params:\n              build_cmd:\n                - mvn clean package\n              jdk_version: 8\n              workdir: ${git-checkout}\n\n    - stage:\n        - release:\n            params:\n              dice_yml: ${git-checkout}/dice.yml\n              services:\n                dice_yaml中的服务名称:\n                  image: tomcat:jdk8-openjdk-slim\n                  copys:\n                    - ${java-build:OUTPUT:buildPath}/target/war包的名称.war:/usr/local/tomcat/webapps\n                  cmd: mv /usr/local/tomcat/webapps/war包的名称.war /usr/local/tomcat/webapps/ROOT.war && /usr/local/tomcat/bin/catalina.sh run\n\n    - stage:\n        - dice:\n            params:\n              release_id: ${release:OUTPUT:releaseID}\n\n',
        '', '2020-10-14 10:06:45', '2020-10-14 10:46:26'),
       (12, 12, 'js-herd-release-dice', '1.0',
        'name: js-herd-release-dice\nversion: \"1.0\"\ndesc: js 直接运行并部署到 dice 的模板\n\ntemplate: |\n\n  version: \"1.1\"\n  stages:\n    - stage:\n        - git-checkout:\n            alias: git-checkout\n            version: \"1.0\"\n    - stage:\n        - js-build:\n            alias: js-build\n            version: \"1.0\"\n            params:\n              build_cmd:\n                - cnpm i\n              workdir: ${git-checkout}\n\n    - stage:\n        - release:\n            alias: release\n            params:\n              dice_yml: ${git-checkout}/dice.yml\n              services:\n                dice.yml中的服务名:\n                  cmd: cd /root/js-build && ls && npm run dev\n                  copys:\n                    - ${js-build}:/root/\n                  image: registry.cn-hangzhou.aliyuncs.com/terminus/terminus-herd:1.1.8-node12\n    - stage:\n        - dice:\n            alias: dice\n            params:\n              release_id: ${release:OUTPUT:releaseID}\n\n',
        '', '2020-10-14 10:07:11', '2020-10-14 10:07:11'),
       (13, 13, 'js-spa-release-dice', '1.0',
        'name: js-spa-release-dice\nversion: \"1.0\"\ndesc: js 进行打包构建到 nginx 并部署到 dice 的模板\n\ntemplate: |\n\n  version: \"1.1\"\n  stages:\n    - stage:\n        - git-checkout:\n            alias: git-checkout\n            version: \"1.0\"\n    - stage:\n        - js-build:\n            alias: js-build\n            version: \"1.0\"\n            params:\n              build_cmd:\n                - cnpm i\n                - cnpm run build\n              workdir: ${git-checkout}\n\n    - stage:\n        - release:\n            alias: release\n            params:\n              dice_yml: ${git-checkout}/dice.yml\n              services:\n                dice.yml中的服务名:\n                  cmd: sed -i \"s^server_name .*^^g\" /etc/nginx/conf.d/nginx.conf.template && envsubst \"`printf \'$%s\' $(bash -c \"compgen -e\")`\" < /etc/nginx/conf.d/nginx.conf.template > /etc/nginx/conf.d/default.conf && /usr/local/openresty/bin/openresty -g \'daemon off;
\'\n\n                  copys:\n                    - ${js-build}/(build 产出的目录):/usr/share/nginx/html/\n                    - ${js-build}/nginx.conf.template:/etc/nginx/conf.d/\n                  image: registry.cn-hangzhou.aliyuncs.com/dice-third-party/terminus-nginx:0.2\n    - stage:\n        - dice:\n            alias: dice\n            params:\n              release_id: ${release:OUTPUT:releaseID}\n\n','','2020-10-14 10:07:38','2020-10-14 10:07:38');

CREATE TABLE `dice_pipeline_templates`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `name`            varchar(255) NOT NULL,
    `logo_url`        varchar(255) NOT NULL,
    `desc`            varchar(255) NOT NULL,
    `scope_type`      varchar(10)  NOT NULL,
    `scope_id`        varchar(255) NOT NULL,
    `default_version` varchar(255) NOT NULL,
    `created_at`      datetime     NOT NULL,
    `updated_at`      datetime     NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COMMENT='流水线模板表';


INSERT INTO `dice_pipeline_templates` (`id`, `name`, `logo_url`, `desc`, `scope_type`, `scope_id`, `default_version`,
                                       `created_at`, `updated_at`)
VALUES (8, 'custom', '', '自定义模板', 'dice', '0', '1.0', '2020-10-14 10:05:17', '2020-10-14 10:05:17'),
       (9, 'java-boot-gradle-dice', '', 'springboot gradle 打包构建部署到 dice 的模板', 'dice', '0', '1.0', '2020-10-14 10:05:49',
        '2020-10-14 13:35:57'),
       (10, 'java-boot-maven-dice', '', 'springboot maven 打包构建部署到 dice 的模板', 'dice', '0', '1.0', '2020-10-14 10:06:16',
        '2020-10-14 13:36:28'),
       (11, 'java-tomcat-maven-dice', '', 'java maven 打包构建放入 tomcat 部署到 dice 的模板', 'dice', '0', '1.0',
        '2020-10-14 10:06:45', '2020-10-14 10:46:26'),
       (12, 'js-herd-release-dice', '', 'js 直接运行并部署到 dice 的模板', 'dice', '0', '1.0', '2020-10-14 10:07:11',
        '2020-10-14 10:07:11'),
       (13, 'js-spa-release-dice', '', 'js 进行打包构建到 nginx 并部署到 dice 的模板', 'dice', '0', '1.0', '2020-10-14 10:07:38',
        '2020-10-14 10:07:38');

CREATE TABLE `dice_publish_item_h5_targets`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `created_at`         datetime              DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`         datetime              DEFAULT NULL COMMENT '表记录更新时间',
    `h5_version_id`      bigint(20) NOT NULL COMMENT 'h5包版本的id',
    `target_version`     varchar(40)           DEFAULT NULL COMMENT 'h5的目标版本',
    `target_build_id`    varchar(100) NOT NULL DEFAULT '' COMMENT 'h5目标版本的build id',
    `target_mobile_type` varchar(40)           DEFAULT NULL COMMENT '目标app类型',
    PRIMARY KEY (`id`),
    KEY                  `idx_h5_version_id` (`h5_version_id`),
    KEY                  `idx_target` (`target_version`,`target_build_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='h5包适配的移动应用版本信息';

CREATE TABLE `dice_publish_item_versions`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `version`            varchar(50) NOT NULL DEFAULT '' COMMENT '版本号',
    `meta`               text COMMENT '元信息',
    `resources`          text,
    `swagger`            longtext,
    `spec`               longtext,
    `readme`             longtext,
    `logo`               varchar(512)         DEFAULT NULL COMMENT '版本logo',
    `desc`               varchar(2048)        DEFAULT NULL COMMENT '描述信息',
    `creator`            varchar(255)         DEFAULT NULL COMMENT '创建者',
    `org_id`             bigint(20) DEFAULT NULL COMMENT '所属企业',
    `publish_item_id`    bigint(20) NOT NULL COMMENT '所属发布仓库',
    `created_at`         datetime             DEFAULT NULL COMMENT '创建时间',
    `updated_at`         datetime             DEFAULT NULL COMMENT '更新时间',
    `public`             tinyint(1) DEFAULT NULL,
    `is_default`         tinyint(1) DEFAULT NULL,
    `version_states`     varchar(20)          DEFAULT NULL COMMENT '版本状态release or beta',
    `gray_level_percent` int(11) DEFAULT NULL COMMENT '灰度百分比',
    `mobile_type`        varchar(40)          DEFAULT NULL COMMENT '移动应用的类型',
    `build_id`           varchar(255)         DEFAULT '1' COMMENT '移动应用的构建id',
    `package_name`       varchar(255)         DEFAULT NULL COMMENT '包名',
    PRIMARY KEY (`id`),
    KEY                  `idx_org_id` (`org_id`),
    KEY                  `publish_item_id` (`publish_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布版本';

CREATE TABLE `dice_publish_items`
(
    `id`                 bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`               varchar(100) NOT NULL DEFAULT '' COMMENT '发布名',
    `display_name`       varchar(100)          DEFAULT NULL,
    `type`               varchar(50)  NOT NULL DEFAULT '' COMMENT '发布内容类型 ANDROID|IOS',
    `logo`               varchar(512)          DEFAULT NULL COMMENT 'logo',
    `desc`               varchar(2048)         DEFAULT NULL COMMENT '描述信息',
    `creator`            varchar(255)          DEFAULT NULL COMMENT '创建者',
    `org_id`             bigint(20) NOT NULL COMMENT '所属企业',
    `publisher_id`       bigint(20) NOT NULL COMMENT '所属发布仓库',
    `public`             tinyint(1) NOT NULL DEFAULT '0',
    `ak`                 varchar(64)           DEFAULT NULL COMMENT '离线包的监控AK',
    `created_at`         datetime     NOT NULL COMMENT '创建时间',
    `updated_at`         datetime              DEFAULT NULL COMMENT '更新时间',
    `no_jailbreak`       tinyint(1) DEFAULT '0' COMMENT '是否禁止越狱配置',
    `geofence_lon`       double                DEFAULT NULL COMMENT '地理围栏，坐标经度',
    `geofence_lat`       double                DEFAULT NULL COMMENT '地理围栏，坐标纬度',
    `geofence_radius`    int(20) DEFAULT NULL COMMENT '地理围栏，合理半径',
    `gray_level_percent` int(11) NOT NULL DEFAULT '0' COMMENT '灰度百分比，0-100',
    `is_migration`       tinyint(4) DEFAULT '1' COMMENT '该item灰度逻辑是否已迁移',
    `preview_images`     text COMMENT '预览图',
    `background_image`   text COMMENT '背景图',
    `ai`                 varchar(50)           DEFAULT NULL COMMENT '离线包的监控AI,一般是发布内容的名字',
    PRIMARY KEY (`id`),
    KEY                  `idx_org_id` (`org_id`),
    KEY                  `idx_publisher_id` (`publisher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布内容';

CREATE TABLE `dice_publish_items_blacklist`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `user_id`          varchar(256) NOT NULL COMMENT '用户id',
    `publish_item_id`  bigint(20) NOT NULL COMMENT '发布内容id',
    `publish_item_key` varchar(64)           DEFAULT NULL COMMENT '监控收集数据需要',
    `user_name`        varchar(256)          DEFAULT NULL COMMENT '用户名称',
    `device_no`        varchar(512) NOT NULL DEFAULT '' COMMENT '设备号',
    `operator`         varchar(255) NOT NULL COMMENT '操作人',
    `created_at`       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY                `idx_publish_item_id` (`publish_item_id`),
    KEY                `idx_publish_item_key` (`publish_item_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布内容黑名单';

CREATE TABLE `dice_publish_items_erase`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `publish_item_id`  bigint(20) NOT NULL COMMENT '发布内容id',
    `publish_item_key` varchar(64)           DEFAULT NULL COMMENT '监控收集数据需要',
    `device_no`        varchar(512) NOT NULL DEFAULT '' COMMENT '设备号',
    `erase_status`     varchar(32)  NOT NULL DEFAULT '' COMMENT '擦除状态',
    `operator`         varchar(255) NOT NULL COMMENT '操作人',
    `created_at`       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY                `idx_publish_item_id` (`publish_item_id`),
    KEY                `idx_publish_item_key` (`publish_item_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布内容数据擦除列表';

CREATE TABLE `dice_release`
(
    `release_id`       varchar(64)  NOT NULL DEFAULT '',
    `release_name`     varchar(255) NOT NULL,
    `desc`             text,
    `dice`             text,
    `addon`            text,
    `labels`           varchar(1000)         DEFAULT NULL,
    `version`          varchar(100)          DEFAULT NULL,
    `org_id`           bigint(20) DEFAULT NULL,
    `project_id`       bigint(20) DEFAULT NULL,
    `application_id`   bigint(20) DEFAULT NULL,
    `project_name`     varchar(80)           DEFAULT NULL,
    `application_name` varchar(80)           DEFAULT NULL,
    `user_id`          varchar(50)           DEFAULT NULL,
    `cluster_name`     varchar(80)           DEFAULT NULL,
    `cross_cluster`    tinyint(4) NOT NULL DEFAULT '0',
    `resources`        text,
    `reference`        bigint(20) DEFAULT NULL,
    `created_at`       timestamp NULL DEFAULT NULL,
    `updated_at`       timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`release_id`),
    KEY                `idx_release_name` (`release_name`),
    KEY                `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT DYNAMIC COMMENT='Dice 版本表';

CREATE TABLE `ps_images`
(
    `id`         bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `created_at` timestamp NULL DEFAULT NULL,
    `updated_at` timestamp NULL DEFAULT NULL,
    `release_id` varchar(255) DEFAULT NULL,
    `image_name` varchar(128) NOT NULL,
    `image_tag`  varchar(64)  DEFAULT NULL,
    `image`      varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    KEY          `idx_release_id` (`release_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT DYNAMIC COMMENT='Dice 镜像表';

