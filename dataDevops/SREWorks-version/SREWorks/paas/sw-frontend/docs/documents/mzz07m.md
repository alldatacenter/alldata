# 2.2 源码构建安装


<a name="kliWz"></a>
# 1. SREWorks源码构建
<a name="j2ijx"></a>
### 构建环境准备

- Kubernetes 的版本需要大于等于 **1.20**
- 一台安装有 `git / docker / go`命令的机器
- 一个可用于上传构建容器镜像仓库(执行`docker push`推送镜像)
- 源码构建包含通过Pod构建镜像环节，所需服务器资源量大于快速构建方案(3台4核16G)

![](/pictures/1646727574970-7826d0ea-3ab4-4da0-a6cf-3338b178920c.jpeg.png)

<a name="cUaZS"></a>
### 拉取 SREWorks 项目源码
```shell
git clone http://github.com/alibaba/sreworks.git -b v1.2 sreworks
cd sreworks
SW_ROOT=$(pwd)
```

<a name="e3x9w"></a>
### 构建 SREWorks 底座容器镜像
在sreworks目录下，直接在本地执行构建脚本
```shell
./build.sh --target all --build --tag v1.2
```
附: 构建镜像清单，如果镜像无法构建成功，也可以直接去公网搬运，镜像内容是完全一致的。
```yaml
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-migrate:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-openjdk8-jre:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-postrun:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-progress-check:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-paas-appmanager:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-paas-appmanager-db-migration:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-paas-appmanager-postrun:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-paas-appmanager-cluster-init:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-paas-appmanager-operator:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/swcli:v1.2
sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/swcli-builtin-package:v1.2
```

<a name="U0oZr"></a>
### 上传SREWorks到仓库
将构建产物发布上传到镜像仓库，`SW_REPO`变量替换成用户自己准备的容器镜像仓库。
```shell
SW_REPO="your-registry.***.com/sreworks"
docker login --username=sre****s your-registry.***.com
./build.sh --target all --push $SW_REPO --tag v1.2
```



<a name="jiRmc"></a>
# 2. SREWorks部署&构建运维应用容器镜像
 步骤与快速安装大致相同，替换helm install参数， 触发运维应用来自源码的容器镜像构建，注意按照附录1和附录2替换参数
```shell
helm install sreworks $SW_ROOT/chart/sreworks-chart \
    --kubeconfig="****" \
    --create-namespace --namespace sreworks \
    --set appmanager.home.url="https://your-website.***.com" \
    --set build.enable=true \
    --set global.images.tag="v1.2" \
    --set global.images.registry=$SW_REPO

```

<a name="jPt3U"></a>
# 附录1. Helm安装参数清单
如果需要构建完的运维应用上传到自定义容器镜像仓库，请在执行helm安装命令时候传入以下的参数
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
--set source.branch="v1.2"
--set source.repo="https://code.aliyun.com/sreworks_public/mirror.git"

```
<a name="M4cYp"></a>
# 附录2. 源码构建依赖资源清单
在纯内网构建或者部分资源替换场景，需要用户自行准备资源，可参考下面的清单。

<a name="F2jkU"></a>
### 底座构建依赖资源参数
在执行 `./build.sh` 命令前可传入下列的环境变量来改变资源地址，如不传入则使用默认值。
```bash
# 容器镜像
export SW_PYTHON3_IMAGE="python:3.9.12-alpine"
export MIGRATE_IMAGE="migrate/migrate"
export MAVEN_IMAGE="maven:3.8.3-adoptopenjdk-11"
export GOLANG_IMAGE="golang:alpine"
export GOLANG_BUILD_IMAGE="golang:1.16"
export DISTROLESS_IMAGE="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/distroless-static:nonroot"

# 软件仓库
export APK_REPO_DOMAIN="mirrors.tuna.tsinghua.edu.cn"
export PYTHON_PIP="http://mirrors.aliyun.com/pypi/simple"
export GOPROXY="https://goproxy.cn"
export MAVEN_SETTINGS_XML="https://sreworks.oss-cn-beijing.aliyuncs.com/resource/settings.xml"

# 二进制命令
export HELM_BIN_URL="https://abm-storage.oss-cn-zhangjiakou.aliyuncs.com/lib/helm"
export KUSTOMIZE_BIN_URL="https://abm-storage.oss-cn-zhangjiakou.aliyuncs.com/lib/kustomize"
export MINIO_CLIENT_URL="https://sreworks.oss-cn-beijing.aliyuncs.com/bin/mc-linux-amd64"

# SREWorks内置应用包
export SREWORKS_BUILTIN_PACKAGE_URL="https://sreworks.oss-cn-beijing.aliyuncs.com/packages"
```

<a name="QZiQn"></a>
### 运维应用构建依赖资源参数
在执行helm install/upgrade 命令的时候，可以选择性传入以下参数，使得运维应用可以在内网进行构建及部署。
```bash
# 容器镜像
--set global.artifacts.mavenImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/maven:3.8.3-adoptopenjdk-11" \
--set global.artifacts.openjdk8Image="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/openjdk8:alpine-jre" \
--set global.artifacts.openjdk11Image="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/openjdk:11.0.10-jre" \
--set global.artifacts.openjdk11AlpineImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/openjdk11:alpine-jre" \
--set global.artifacts.alpineImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/alpine:latest" \
--set global.artifacts.nodeImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/node:10-alpine" \
--set global.artifacts.migrateImage="sw-migrate" \
--set global.artifacts.postrunImage="sw-postrun" \
--set global.artifacts.python3Image="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/python:3.9.12-alpine" \
--set global.artifacts.bentomlImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/bentoml-model-server:0.13.1-py37" \

# 软件仓库
--set global.artifacts.apkRepoDomain="mirrors.tuna.tsinghua.edu.cn" \
--set global.artifacts.mavenSettingsXml="https://sreworks.oss-cn-beijing.aliyuncs.com/resource/settings.xml" \
--set global.artifacts.npmRegistryUrl="https://registry.npmmirror.com" \
--set global.artifacts.pythonPip="http://mirrors.aliyun.com/pypi/simple" \

# 二进制命令
--set global.artifacts.minioClientUrl="https://sreworks.oss-cn-beijing.aliyuncs.com/bin/mc-linux-amd64" \

# SaaS应用Helm包中依赖的镜像
--set global.artifacts.logstashImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/logstash:7.10.2" \
--set global.artifacts.grafanaImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/grafana:7.5.3" \
--set global.artifacts.kibanaImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/kibana:7.10.2" \
--set global.artifacts.elasticsearchImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/elasticsearch:7.10.2-with-plugins" \
--set global.artifacts.skywalkingOapImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/skywalking-oap-server-utc-8:8.5.0-es7" \
--set global.artifacts.skywalkingUiImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/skywalking-ui:8.5.0" \
--set global.artifacts.busyboxImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/busybox:1.30" \
--set global.artifacts.vvpRegistry="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror" \
--set global.artifacts.vvpAppmanagerImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/vvp-appmanager:2.6.1" \
--set global.artifacts.vvpGatewayImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/vvp-gateway:2.6.1" \
--set global.artifacts.vvpUiImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/vvp-ui:2.6.1" \
--set global.artifacts.metricbeatImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/metricbeat:7.10.2" \
--set global.artifacts.filebeatImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/filebeat:7.10.2" \
--set global.artifacts.mysqlImage="sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/mysql:8.0.22-debian-10-r44" \

# 基础底座的Helm包中依赖的镜像
--set appmanagerbase.minio.image.registry="sreworks-registry.cn-beijing.cr.aliyuncs.com" \
--set appmanagerbase.minio.image.repository="mirror/minio" \
--set appmanagerbase.minio.image.tag="v1.0" \
--set appmanagerbase.mysql.image.registry="sreworks-registry.cn-beijing.cr.aliyuncs.com" \
--set appmanagerbase.mysql.image.repository="mirror/mysql" \
--set appmanagerbase.mysql.image.tag="v1.0" \
--set appmanagerbase.redis.image.registry="sreworks-registry.cn-beijing.cr.aliyuncs.com" \
--set appmanagerbase.redis.image.repository="mirror/redis" \
--set appmanagerbase.redis.image.tag="v1.0" \
--set appmanagerbase.zookeeper.image.registry="sreworks-registry.cn-beijing.cr.aliyuncs.com" \
--set appmanagerbase.zookeeper.image.repository="mirror/zookeeper" \
--set appmanagerbase.zookeeper.image.tag="v1.0" \
--set appmanagerbase.kafka.image.registry="sreworks-registry.cn-beijing.cr.aliyuncs.com" \
--set appmanagerbase.kafka.image.repository="mirror/kafka" \
--set appmanagerbase.kafka.image.tag="2.8.0-debian-10-r76" \
--set appmanagerbase.openebs.apiserver.image="openebs/m-apiserver" \
--set appmanagerbase.openebs.apiserver.imageTag="2.12.2" \
--set appmanagerbase.openebs.provisioner.image="openebs/openebs-k8s-provisioner" \
--set appmanagerbase.openebs.provisioner.imageTag="2.12.2" \
--set appmanagerbase.openebs.localprovisioner.image="openebs/provisioner-localpv" \
--set appmanagerbase.openebs.localprovisioner.imageTag="2.12.2" \
--set appmanagerbase.openebs.snapshotOperator.controller.image="openebs/snapshot-controller" \
--set appmanagerbase.openebs.snapshotOperator.controller.imageTag="2.12.2" \
--set appmanagerbase.openebs.snapshotOperator.provisioner.image="openebs/snapshot-provisioner" \
--set appmanagerbase.openebs.snapshotOperator.provisioner.imageTag="2.12.2" \
--set appmanagerbase.openebs.ndm.image="openebs/node-disk-manager" \
--set appmanagerbase.openebs.ndm.imageTag="1.8.0" \
--set appmanagerbase.openebs.ndmOperator.image="openebs/node-disk-operator" \
--set appmanagerbase.openebs.ndmOperator.imageTag="1.8.0" \
--set appmanagerbase.openebs.webhook.image="openebs/admission-server" \
--set appmanagerbase.openebs.webhook.imageTag="2.12.2" \
--set appmanagerbase.openebs.helper.image="openebs/linux-utils" \
--set appmanagerbase.openebs.helper.imageTag="3.1.0" \
--set appmanagerbase.openebs.policies.monitoring.image="openebs/m-exporter" \
--set appmanagerbase.openebs.policies.monitoring.imageTag="2.12.2" \
```

其中flink部分需要按照`global.artifacts.vvpRegistry`的前缀上传如下镜像，建议优先使用flink1.14版本， flink1.12和flink1.13版本根据实际需要提供
```bash
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/flink:1.12.7-stream1-scala_2.12-java8
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/flink:1.12.7-stream1-scala_2.12-java11
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/flink:1.13.5-stream1-scala_2.12-java8
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/flink:1.13.5-stream1-scala_2.12-java11
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/flink:1.14.2-stream1-scala_2.12-java8
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/flink:1.14.2-stream1-scala_2.12-java11
sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/vvp-result-fetcher-service:2.6.1
```
