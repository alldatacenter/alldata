# Streamis系统的AppConn插件使用

## 1.StreamisAppConn
----------

### 1.1介绍
StreamisAppConn是Streamis用来与DSS集成的一个AppConn，功能包括

|实现的规范和Service              | 功能                                      | 作用微服务                                                |
|---------------------|------------------------------------------------------|---------------------------------------------------------|
| 二级规范 | 与DSS工程打通，支持工程内容同步                                     | DSS-Framework-Project-Server                |
| 三级规范-CRUDService | 支持流式编排创建、获取、更新、删除等操作 | DSS-Framework-Orchestrator-Server |
| 三级规范-ExportService和ImportService   | 支持流式编排的导入导出        | DSS-Framework-Orchestrator-Server                                  |



### 1.2部署

1. 编译

```bash
#整体编译streamis代码
cd ${STREAMIS_CODE_HOME}
mvn -N install
mvn clean install
#单独编译appconn插件
cd ${STREAMIS_CODE_HOME}/streamis-plugins/streamis-appconn
mvn clean install
```

2. 部署
1. 从 ${STREAMIS_CODE_HOME}/streamis-plugins/streamis-appconn/target 获取appconn的安装包
2. 上传到DSS放置appconn的目录
```bash
cd ${DSS_HOME}/dss/dss-appconns
unzip streamis-appconn.zip
```
3. 执行sql
需要进入到
```roomsql
SET @STREAMIS_INSTALL_IP_PORT='127.0.0.1:9003';
SET @URL = replace('http://STREAMIS_IP_PORT', 'STREAMIS_IP_PORT', @STREAMIS_INSTALL_IP_PORT);
SET @HOMEPAGE_URL = replace('http://STREAMIS_IP_PORT', 'STREAMIS_IP_PORT', @STREAMIS_INSTALL_IP_PORT);
SET @PROJECT_URL = replace('http://STREAMIS_IP_PORT', 'STREAMIS_IP_PORT', @STREAMIS_INSTALL_IP_PORT);
SET @REDIRECT_URL = replace('http://STREAMIS_IP_PORT/udes/auth', 'STREAMIS_IP_PORT', @STREAMIS_INSTALL_IP_PORT);

delete from `dss_application` WHERE `name` = 'Streamis';
INSERT INTO `dss_application`(`name`,`url`,`is_user_need_init`,`level`,`user_init_url`,`exists_project_service`,`project_url`,`enhance_json`,`if_iframe`,`homepage_url`,`redirect_url`) VALUES ('Streamis', @URL, 0, 1, NULL, 0, @PROJECT_URL, '', 1, @HOMEPAGE_URL, @REDIRECT_URL);

select @dss_streamis_applicationId:=id from `dss_application` WHERE `name` = 'Streamis';

delete from `dss_onestop_menu` WHERE `name` = '数据交换';
select @dss_onestop_menu_id:=id from `dss_onestop_menu` where `name` = '数据交换';

delete from `dss_onestop_menu_application` WHERE title_en = 'Streamis';
```



### 1.3使用

## 2.Streamis DataSource AppConn
----------
### 2.1介绍
|实现的规范和Service              | 功能                                      | 作用微服务                                                |
|---------------------|------------------------------------------------------|---------------------------------------------------------|
| 三级规范的CRUDService | 支持数据源节点的创建、获取、更新、删除等操作           | DSS-Workflow-Server                |
| 三级规范的ExportService和ImportService | 支持数据源的导入导出 | DSS-Workflow-Server |
| 三级规范的ExecutionService   | 支持数据源的执行        | Linkis-AppConn-Engine                                 |

1. 实现三级规范的CRUDService，支持数据源节点的创建、获取、更新、删除等操作
2. 实现三级规范的ExportService和ImportService，支持数据源的导入导出
3. 实现三级规范的ExecutionService,支持数据源的执行
### 2.2部署

### 2.3使用


## 3.Streamis JobManager AppConn

### 3.1介绍
StreamisJobManager AppConn与SchedulisAppConn的功能是类似的，主要是将DSS的工作流转换成Streamis能够提交执行的流式应用，并把此流式应用发布到StreamisJobManager的

|实现的规范和Service              | 功能                                      | 作用微服务                                                |
|---------------------|------------------------------------------------------|---------------------------------------------------------|
| 工作流转换规范 | 支持将流式工作流转换成Linkis Flink引擎可以执行的流式应用           | DSS-Framework-Orchestrator-Server                |
| 工作流发布规范 | 支持将转换之后的流式应用发布到Streamis-JobManager | DSS-Framework-Orchestrator-Server |

### 3.2部署

