# ---备用文档 SREWorks开源构建

<a name="kliWz"></a>
# 1. SREWorks源码构建及上传
<a name="xPY76"></a>
## 1.1 构建环境准备

- 一台安装有 `git / docker`命令的机器

<a name="naB3D"></a>
## 1.2 拉取 SREWorks 项目源码
```shell
git clone http://gitlab.alibaba-inc.com/sreworks/sreworks.git --recursive --single-branch
cd sreworks
SW_ROOT=$(pwd)

```
<a name="mMUoO"></a>
## 
<a name="bIQPN"></a>
## 1.3 构建 sw-appmanager 镜像及上传
在sreworks目录下，直接执行构建脚本并推送：
```shell
#准备一个能够上传镜像的仓库
SW_REPO="registry.cn-hangzhou.aliyuncs.com/alisre"

./build.sh -t all -p $SW_REPO -b
```
也可以手动分步执行相关服务的构建，包含sw-appmanager依赖及自身镜像的构建三大步骤：
```shell
#准备一个能够上传镜像的仓库
SW_REPO="registry.cn-hangzhou.aliyuncs.com/alisre"

# sw-appmanager 依赖构建
./build.sh -t base -p $SW_REPO -b

# sw-appmanager 构建
./build.sh -t appmanager -p $SW_REPO -b

```

<a name="jiRmc"></a>
# 2. SREWorks部署
<a name="zFb7r"></a>
## 2.1. 部署环境准备

- 一个能访问该镜像仓库的k8s集群，并确认要部署的namspace
- 一台安装有 `helm`  命令的机器
<a name="kfKy5"></a>
## 2.2 安装 SREworks helm包
```bash
helm install appmanager $SW_ROOT/chart/sreworks-chart \
    --create-namespace --namespace sreworks \
    --set appmanager.home.url="https://sreworks.c38cca9c474484bdc9873f44f733d8bcd.cn-beijing.alicontainer.com"
```
<a name="j6pra"></a>
## 
<a name="R1Xzu"></a>
# 附: swcli安装本地方法
```shell
# MACOS https://abm-storage.oss-cn-zhangjiakou.aliyuncs.com/swcli-darwin-amd64
# Linux https://abm-storage.oss-cn-zhangjiakou.aliyuncs.com/swcli-linux-amd64
# Windows https://abm-storage.oss-cn-zhangjiakou.aliyuncs.com/swcli-windows-amd64

wget https://abm-storage.oss-cn-zhangjiakou.aliyuncs.com/swcli-darwin-amd64 -O $SW_ROOT/swcli
chmod 755 $SW_ROOT/swcli

# 配置swcli

```



