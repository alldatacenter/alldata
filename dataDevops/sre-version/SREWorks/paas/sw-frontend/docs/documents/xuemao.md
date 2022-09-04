# 2.3 常见问题

<a name="cB4Be"></a>
### 1. 如何填写组件中的HELM社区仓库
![image.png](https://intranetproxy.alipay.com/skylark/lark/0/2022/png/2748/1650904677037-7c5838ab-098f-4948-aabe-9b73dc6e305d.png#clientId=u62befdae-b400-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=419&id=uc6a9a00b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=838&originWidth=2208&originalType=binary&ratio=1&rotation=0&showTitle=false&size=299004&status=done&style=none&taskId=u7477c582-7231-4b7d-948a-72eda646290&title=&width=1104)
<a name="pyyIT"></a>
### 2. Appmanager运行报错，无法创建新线程
```
java.lang.OutOfMemoryError: unable to create native thread
```
需要将 /var/lib/kubelet/config.yaml 中的 `podPidsLimit: 1000` 改为 `podPidsLimit: -1`

<a name="LYcu3"></a>
### 3. Helm安装参数清单
```shell
# 平台名称
--set platformName="SREWorks"

# 平台图标, icon格式要求（比如：48*48）
--set platformLogo="https://sreworks.oss-cn-beijing.aliyuncs.com/logo/demo.png" 

# 底层存储
--set global.storageClass="alicloud-disk-available"

# SREWorks平台启动使用的容器镜像仓库
--set global.images.registry="registry.cn-zhangjiakou.aliyuncs.com/sreworks"

# SaaS容器构建镜像仓库配置
--set appmanager.server.docker.account="sreworks"
--set appmanager.server.docker.password="***"
--set appmanager.server.docker.registry="registry.cn-zhangjiakou.aliyuncs.com"
--set appmanager.server.docker.namespace="builds"

# 源码构建模式的源码仓库来源
--set source.branch="master"
--set source.repo="https://code.aliyun.com/sreworks_public/mirror.git"


# 替换基础应用的主MySQL数据库
# MySQL这块需要注意，通常将主MySQL数据库和数智化MySQL数据库(吞吐较大)分成两个

--set appmanager.server.database.host="*.mysql.rds.aliyuncs.com" 
--set appmanager.server.database.password="****"
--set appmanager.server.database.user="root"
--set appmanager.server.database.port=3306
--set appmanagerbase.mysql.enabled=false

# 替换数智化应用的MySQL数据库
--set saas.dataops.dbHost="*.mysql.rds.aliyuncs.com"
--set saas.dataops.dbUser=root
--set saas.dataops.dbPassword="*****"
--set saas.dataops.dbPort=3306

# 替换数智化应用的ElasticSearch
--set saas.dataops.esHost="*.public.elasticsearch.aliyuncs.com"
--set saas.dataops.esPort="9200"
--set saas.dataops.esUser="elastic"
--set saas.dataops.esPassword="*******"

# 替换基础应用的MinIO存储
--set global.minio.accessKey="*******"
--set global.minio.secretKey="*******"
--set appmanager.server.package.endpoint="minio.*.com:9000"
--set appmanagerbase.minio.enabled=false
```
