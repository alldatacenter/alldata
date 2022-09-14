#!/usr/bin/env bash
#
# Copyright 2022 WeBank
#
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Use to upgrade from 0.1.0 to 0.2.0

if [ -f "~/.bashrc" ];then
  echo "Warning! user bashrc file does not exist."
else
  source ~/.bashrc
fi

shellDir=`dirname $0`
workDir=`cd ${shellDir}/..;pwd`

interact_echo(){
  while [ 1 ]; do
    read -p "$1 (Y/N)" yn
    if [[ "${yn}x" == "Yx" ]] || [[ "${yn}x" == "yx" ]]; then
      return 0
    elif [[ "${yn}x" == "Nx" ]] || [[ "${yn}x" == "nx" ]]; then
      return 1
    else
      echo "Unknown choose: [$yn], please choose again."
    fi
  done
}

interact_echo "Are you sure the current version of Streamis is 0.1.0 and need to upgrade to 0.2.0 ?"
if [[ $? == 0 ]]; then
  source ${workDir}/conf/db.sh
  echo "<------ Will connect to [${MYSQL_HOST}:${MYSQL_PORT}] to upgrade the tables in database... ------>"
  mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD -D$MYSQL_DB --default-character-set=utf8 << EOF 1>/dev/null
    /*Modify the table column*/
    ALTER TABLE \`linkis_stream_job\` MODIFY COLUMN \`project_name\` varchar(100) DEFAULT NULL;
    ALTER TABLE \`linkis_stream_job\` MODIFY COLUMN \`name\` varchar(200) DEFAULT NULL;
    ALTER TABLE \`linkis_stream_project\` MODIFY COLUMN \`name\` varchar(100) DEFAULT NULL;
    ALTER TABLE \`linkis_stream_task\` MODIFY COLUMN \`job_id\` varchar(200) DEFAULT NULL;
    ALTER TABLE \`linkis_stream_task\` MODIFY COLUMN \`linkis_job_id\` varchar(200) DEFAULT NULL;

    ALTER TABLE \`linkis_stream_project\` ADD create_time datetime DEFAULT NULL;
    ALTER TABLE \`linkis_stream_project\` ADD last_update_by varchar(50) DEFAULT NULL;
    ALTER TABLE \`linkis_stream_project\` ADD last_update_time datetime DEFAULT NULL;
    ALTER TABLE \`linkis_stream_project\` ADD is_deleted tinyint unsigned DEFAULT 0;

    /*Add indexes into the tables*/
    ALTER TABLE \`linkis_stream_job\` ADD UNIQUE KEY(\`project_name\`, \`name\`);
    ALTER TABLE \`linkis_stream_job_version\` ADD UNIQUE KEY(\`job_id\`, \`version\`);

    /*Add new tables*/
    DROP TABLE IF EXISTS \`linkis_stream_project_privilege\`;
    CREATE TABLE \`linkis_stream_project_privilege\` (
      \`id\` bigint(20) NOT NULL AUTO_INCREMENT,
      \`project_id\` bigint(20) NOT NULL,
      \`user_name\` varchar(100) NOT NULL,
      \`privilege\` tinyint(1) DEFAULT '0' NOT NULL COMMENT '1:发布权限 ，2:编辑权限 ，3:查看权限',
      PRIMARY KEY (\`id\`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='项目权限表';

    DROP TABLE IF EXISTS \`linkis_stream_job_config_def\`;
    CREATE TABLE \`linkis_stream_job_config_def\` (
      \`id\` bigint(20) NOT NULL AUTO_INCREMENT,
      \`key\` varchar(100) COLLATE utf8_bin NOT NULL,
      \`name\` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT 'Equals option',
      \`type\` varchar(50) COLLATE utf8_bin NOT NULL DEFAULT 'NONE' COMMENT 'def type, NONE: 0, INPUT: 1, SELECT: 2',
      \`sort\` int(10) DEFAULT '0' COMMENT 'In order to sort the configurations that have the same level',
      \`description\` varchar(200) COLLATE utf8_bin DEFAULT NULL COMMENT 'Description of configuration',
      \`validate_type\` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT 'Method the validate the configuration',
      \`validate_rule\` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT 'Value of validation rule',
      \`style\` varchar(200) COLLATE utf8_bin DEFAULT '' COMMENT 'Display style',
      \`visiable\` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0: hidden, 1: display',
      \`level\` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0: root, 1: leaf',
      \`unit\` varchar(25) COLLATE utf8_bin DEFAULT NULL COMMENT 'Unit symbol',
      \`default_value\` varchar(200) COLLATE utf8_bin DEFAULT NULL COMMENT 'Default value',
      \`ref_values\` varchar(200) COLLATE utf8_bin DEFAULT '',
      \`parent_ref\` bigint(20) DEFAULT NULL COMMENT 'Parent key of configuration def',
      \`required\` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'If the value of configuration is necessary',
      \`is_temp\` tinyint(1) DEFAULT '0' COMMENT 'Temp configuration',
      PRIMARY KEY (\`id\`),
      UNIQUE KEY \`config_def_key\` (\`key\`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

    DROP TABLE IF EXISTS \`linkis_stream_job_config\`;
    CREATE TABLE \`linkis_stream_job_config\` (
      \`job_id\` bigint(20) NOT NULL,
      \`job_name\` varchar(200) COLLATE utf8_bin NOT NULL COMMENT 'Just store the job name',
      \`key\` varchar(100) COLLATE utf8_bin NOT NULL,
      \`value\` varchar(500) COLLATE utf8_bin NOT NULL,
      \`ref_def_id\` bigint(20) DEFAULT NULL COMMENT 'Refer to id in config_def table',
      PRIMARY KEY (\`job_id\`,\`key\`),
      KEY \`config_def_id\` (\`ref_def_id\`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

    /*Execute dml*/
    source ${workDir}/db/streamis_dml.sql

    /*Data migration*/
    INSERT INTO \`linkis_stream_job_config\`(\`key\`, \`value\`, \`job_id\`, \`job_name\`, \`ref_def_id\`) SELECT ov.config_key, ov.config_value, ov.job_id, ov.job_name, d.id as refer_id from linkis_stream_configuration_config_value ov left join linkis_stream_job_config_def d on ov.config_key = d.key WHERE ov.config_value IS NOT NULL AND ov.job_name IS NOT NULL GROUP BY ov.job_id,ov.config_key;
    UPDATE linkis_stream_job_config SET \`key\` = "wds.linkis.flink.taskmanager.memory" WHERE \`key\` = "flink.taskmanager.memory";
    UPDATE linkis_stream_job_config SET \`key\` = "wds.linkis.flink.taskmanager.cpus" WHERE \`key\` = "flink.taskmanager.cpu.cores";
    UPDATE linkis_stream_job_config SET \`key\` = "wds.linkis.flink.taskmanager.cpus" WHERE \`key\` = "wds.linkis.flink.taskManager.cpus";
    UPDATE linkis_stream_job_config SET \`key\` = "wds.linkis.flink.taskmanager.numberOfTaskSlots" WHERE \`key\` = "flink.taskmanager.numberOfTaskSlots";
    UPDATE linkis_stream_job_config SET \`key\` = "wds.linkis.flink.app.parallelism" WHERE \`key\` = "wds.linkis.engineconn.flink.app.parallelism";
    UPDATE linkis_stream_job_config SET \`key\` = "wds.linkis.flink.jobmanager.memory" WHERE \`key\` = "flink.jobmanager.memory";
    UPDATE linkis_stream_job_config c SET \`ref_def_id\` = (SELECT d.id FROM linkis_stream_job_config_def d WHERE d.\`key\` = c.\`key\`) WHERE c.ref_def_id IS NULL;
    SELECT @flink_extra_param_id:=id FROM linkis_stream_job_config_def WHERE \`key\` = "wds.linkis.flink.custom";
    UPDATE linkis_stream_job_config SET ref_def_id = @flink_extra_param_id WHERE ref_def_id IS NULL;

    /*Drop tables*/
    /*DROP TABLE \`linkis_stream_configuration_config_key\`*/
    /*DROP TABLE \`linkis_stream_configuration_config_value\`*/

    /*update tables data*/
    delimiter %%

    create procedure update_project()
    BEGIN
        -- 声明变量
        DECLARE projectname varchar(50);
        DECLARE done INT default 0;

    -- 创建游标，并设置游标所指的数据
        DECLARE cur CURSOR for
            SELECT distinct j.project_name from linkis_stream_job j;
    -- 游标执行完，即遍历结束。设置done的值为1
        DECLARE CONTINUE HANDLER for not FOUND set done = 1;
    -- 开启游标
        open cur;
    -- 执行循环
        posLoop:
        LOOP
                -- 从游标中取出projectname
            FETCH cur INTO projectname ;
            -- 如果done的值为1，即遍历结束，结束循环
            IF done = 1 THEN
                LEAVE posLoop;
    -- 注意，if语句需要添加END IF结束IF
            END IF;
            insert into linkis_stream_project(\`name\`,\`create_by\`,\`create_time\`) values (projectname,\'system\',now());
    -- 关闭循环
        END LOOP posLoop;
    -- 关闭游标
        CLOSE cur;
    -- 关闭分隔标记
    END %%

    create procedure update_project_privilege()
    BEGIN
        -- 声明变量
        DECLARE projectid bigint(20);
        DECLARE create_by varchar(50);
        DECLARE done INT default 0;

    -- 创建游标，并设置游标所指的数据
        DECLARE cur CURSOR for
            SELECT distinct p.id,j.create_by  from linkis_stream_project p,linkis_stream_job j where p.name =j.project_name ;
    -- 游标执行完，即遍历结束。设置done的值为1
        DECLARE CONTINUE HANDLER for not FOUND set done = 1;
    -- 开启游标
        open cur;
    -- 执行循环
        posLoop:
        LOOP
                -- 从游标中取出id
            FETCH cur INTO projectid ,create_by;
            -- 如果done的值为1，即遍历结束，结束循环
            IF done = 1 THEN
                LEAVE posLoop;
    -- 注意，if语句需要添加END IF结束IF
            END IF;

            insert into linkis_stream_project_privilege (project_id ,user_name ,privilege) values (projectid,create_by,2);
    -- 关闭循环
        END LOOP posLoop;
    -- 关闭游标
        CLOSE cur;
    -- 关闭分隔标记
    END %%
    delimiter ;

    call update_project;
    call update_project_privilege;

    drop PROCEDURE update_project;
    drop PROCEDURE update_project_privilege;

EOF
  echo "<------ End to upgrade ------>"
fi



