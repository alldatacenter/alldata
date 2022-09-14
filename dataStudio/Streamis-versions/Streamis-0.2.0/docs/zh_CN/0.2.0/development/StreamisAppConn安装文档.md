StreamisAppConn安装文档 本文主要介绍在DSS(DataSphere Studio)1.1.0中StreamisAppConn的部署、配置以及安装

# 1.部署StreamisAppConn的准备工作
您在部署StreamisAppConn之前，请安装完成Streamis0.2.0及其他相关组件的安装，并确保工程基本功能可用。

# 2.StreamisAppConn插件的下载和编译
1）下载二进制包
我们提供StreamisAppconn的物料包，您可直接下载使用。[点击跳转 Release 界面](https://github.com/WeBankFinTech/Streamis/releases)

2） 编译打包
如果您想自己开发和编译StreamisAppConn，具体编译步骤如下： 1.clone Streamis的代码 2.找到streamis-appconn模块，单独编译streamis-appconn
```shell script
cd {STREAMIS_CODE_HOME}/streamis-appconn
mvn clean install
```
会在该路径下找到streamis.zip安装包
```shell script
{STREAMIS_CODE_HOME}\streamis-appconn\target\streamis.zip
```

# 3.StreamisAppConn插件的部署和配置总体步骤
 1.拿到打包出来的streamis.zip物料包

 2.放置到如下目录并进行解压

注意：第一次解压streamis appconn后，确保当前文件夹下没有index_v0000XX.index文件，该文件在后面才会生成
```shell script
cd {DSS_Install_HOME}/dss/dss-appconns
unzip streamis.zip
```
解压出来的目录结构为：
```shell script
conf
db
lib
```
 3.执行脚本进行自动化安装
 ```shell script
cd {DSS_INSTALL_HOME}/dss/bin
sh ./appconn-install.sh
# 脚本是交互式的安装方案，您需要输入字符串streamis以及streamis服务的ip和端口，即可以完成安装
# 这里的streamis端口是指前端端口，在nginx进行配置。而不是后端的服务端口
```

## 4.完成streamis-appconn的安装后，需要重启dss服务，才能最终完成插件的更新
### 4.1）使部署好的APPCONN生效
使用DSS启停脚本使APPCONN生效，进入到脚本所在目录{DSS_INSTALL_HOME}/dss/sbin中，依次使用如下命令执行脚本：
```shell script
sh ./dss-stop-all.sh
sh ./dss-start-all.sh
```
### 4.2）验证streamis-appconn是否生效
在安装部署完成streamis-appconn之后，可通过以下步骤初步验证streamis-appconn是否安装成功。

在DSS工作空间创建一个新的项目
![DSS工作空间Streamis项目](../../../images/zh_CN/dss_streamis_project.png)

在streamis数据库查看是否同步创建项目，查询有记录说明appconn安装成功
```roomsql
SELECT * FROM linkis_stream_project WHERE name = '项目名称';
```

# 5.Streamis AppConn安装原理
Streamis 的相关配置信息会插入到以下表中，通过配置下表，可以完成 Streamis 的使用配置。(注：如果仅仅需要快速安装APPCONN，无需过分关注以下字段，提供的init.sql中大多以进行默认配置。重点关注以上操作即可)

|表名	            |表作用	                                  |备注    |
|-------------------|-----------------------------------------|------|
|dss_workspace_dictionary  |配置流式生产中心                  	|必须|
|dss_appconn	        |AppConn的基本信息，用于加载AppConn   	|必须|
|dss_workspace_menu_appconn  |AppConn菜单，前端连接Streamis	|必须|
|dss_appconn_instance	|AppConn的实例的信息，包括自身的url信息	|必须|
