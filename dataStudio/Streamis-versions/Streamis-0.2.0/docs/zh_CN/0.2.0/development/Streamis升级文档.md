Streamis 升级文档，本文主要介绍在原有安装Streamis服务的基础上适配DSS1.1.0和Linkis1.1.1的升级步骤，Streamis0.2.0相对与Streamis0.1.0版本最大的区别在于接入了DSS AppConn，对job的启停做了优化。

# 1.升级Streamis前的工作
您在升级Streamis之前，请先安装 Linkis1.1.1 和 DSS1.1.0 及以上版本，并且保证 Linkis Flink 引擎 和 DSS 可以正常使用，DSS 和 Linkis 安装，可参照 [DSS & Linkis 一键安装部署文档](https://github.com/WeBankFinTech/DataSphereStudio-Doc/blob/main/zh_CN/%E5%AE%89%E8%A3%85%E9%83%A8%E7%BD%B2/DSS%E5%8D%95%E6%9C%BA%E9%83%A8%E7%BD%B2%E6%96%87%E6%A1%A3.md)。

# 2.Streamis升级步骤

## 安装StreamisAppConn

1）删除旧版本StreamisAppConn包

进入下列目录，找到streamis的appconn文件夹并删除，如果存在的话：
```shell script
{DSS_Install_HOME}/dss/dss-appconns
```

2）StreamisAppConn安装部署

安装 DSS StreamisAppConn 插件，请参考: [StreamisAppConn 插件安装文档](development/StreamisAppConn安装文档.md)

## 安装Streamis后端
将获取到的安装包中lib更新到Streamis安装目录下的路径 `streamis-server/lib` 中,`streamis-server/conf`下的文件内容可根据需要进行更新。

进入安装目录下，执行更新脚本，完成对数据库表结构和数据的更新：
```shell script
cd {Streamis_Install_HOME}
sh bin/upgrade.sh
```

再通过以下命令完成 Streamis Server 的更新重启：
```shell script
cd {Streamis_Install_HOME}/streamis-server
sh bin/stop-streamis-server.sh 
sh bin/start-streamis-server.sh 
```

## 安装Streamis前端
先删除旧版本前端目录文件夹，再替换为新的前端安装包
```
mkdir ${STREAMIS_FRONT_PATH}
cd ${STREAMIS_FRONT_PATH}
#1.删除前端目录文件夹
#2.放置前端包
unzip streamis-${streamis-version}.zip
```