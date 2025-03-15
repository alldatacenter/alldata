# 安装问题记录


## AllData软件安装
```
前置 -
1、启动前是删除了pom.xml；
2、本地是V16版本的nodejs;

运行 -
1、启动后端相关服务；
2、启动前端npm run dev,报错：
multi ./node modules/.pnpm/webpack-dev-server3.1.3 webpack04.28.4/node modules/webpack-dev-server/clienthtp://192.168.0.118:8013/sockjs-node(webpack)/hot/dev-server.js ./src/main.js
Module not found: Error: Can't resolvebabel-loader'in D: workspaceldatacenter workspacelscit-datacenter-ui

原因 -
前端UI对应nodejs版本是v10.15.3 , 需要切换版本，为开发方便，一遍采用nvm进行管理
1、卸载nodejs;
2、安装nvm - https://www.jianshu.com/p/13c0b3ca7c71
3、安装v10.15.3版本： nvm install v10.15.3
4、根据实际切换版本：nvm use v10.15.3
5、安装依赖：npm install
6、启动前端：npm run dev
```

```
前置 -
1、数据集成教程

运行 -
1、数据集成教程

原因 -
1、教程
https://github.com/alldatacenter/alldata/blob/master/studio/modules/service-data-dts-parent/DTS_QuickStart.md

```

```
前置 -
1、数据集成教程

运行 -
1、数据集成教程

原因 -
1、教程
2、依赖datax,安装datax: https://blog.csdn.net/qq_18896247/article/details/123127487
3、https://github.com/alldatacenter/alldata/blob/master/studio/modules/service-data-dts-parent/DTS_QuickStart.md

```

```
前置 -
1、元数据数据库文档下载

运行 -
1、元数据数据库文档下载，依赖报错

原因 -
1、缺失aspose-words,要手动安装到本地仓库
2、cd alldata/studio/common
3、安装命令：windows使用git bash执行, mac直接执行以下命令
4、mvn install:install-file -Dfile=aspose-words-20.3.jar -DgroupId=com.aspose -DartifactId=aspose-words -Dversion=20.3 -Dpackaging=jar
5、安装成功重新刷新依赖,重新打包
```
