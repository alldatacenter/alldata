
<a name="e7cd2651"></a>
# Datahub 环境部署

> Datahub 官方部署文档：[https://datahubproject.io/docs/quickstart](https://datahubproject.io/docs/quickstart)


<a name="85bc6e0a"></a>
## 前提环境准备
| 组件 | 版本 | 下载地址（官方） | 备注 |
| --- | --- | --- | --- |
| Datahub  | 0.10.0 | [Datahub 0.10.0](https://github.com/datahub-project/datahub/archive/refs/tags/v0.10.0.zip) |  部署步骤来自官方，部分做了调整 |
| Python | 3.10.2 | [Python3.10.2](https://www.python.org/ftp/python/3.10.10/Python-3.10.10.tgz) | 网上安装教程，有很多，这里不再赘述  |
| Docker | 20.10.5 | [CentOS 7.9](https://repo.huaweicloud.com/centos/7.9.2009/isos/x86_64/CentOS-7-x86_64-Everything-2207-02.iso) | 网上安装教程，有很多，这里不再赘述  |
| Docker-compose | 1.29.2 | [Dokcer-compose1.29.2](https://github.com/docker/compose/releases/download/1.29.2/docker-compose-Linux-x86_64) | 网上安装教程，有很多，这里不再赘述  |


<a name="m9h1T"></a>
# 数据中心快速入门指南
<a name="fX7JE"></a>
## 部署[数据中心](https://datahubproject.io/docs/quickstart#deploying-datahub)

### 1. 安装 DataHub CLIA。
 1. 确保安装并配置了 Python 3.7+。（检查使用python3 --version）。
 2.  在终端中运行以下命令
```shell
python3 -m pip install --upgrade pip wheel setuptools
python3 -m pip install --upgrade acryl-datahub
datahub version
```
> 笔记
> 如果您看到“找不到命令”，请尝试使用前缀“python3 -m”运行 cli 命令，而不是像注意python3 -m datahub version DataHub CLI 不支持 Python 2.x。

**说明：官方这里的安装步骤有些问题。无法使用datahub 命令。这里有个解决办法。**

1. 使用 `whereis python3` 命令，找到 Python3 安装的 `$PYTHON_HONE/bin`目录。我这里是 `/mnt/poc/python/python-current/bin`。你会看到`datahub`的客户端在这个目录下面。如下：
```shell
[root@16gdata ~]# whereis python3
python3: /usr/lib/python3.6 
  /usr/local/bin/python3 
  /usr/local/python3 
  /mnt/poc/python/python-current/bin/python3.10 
  /mnt/poc/python/python-current/bin/python3 
  /mnt/poc/python/python-current/bin/python3.10-config
[root@16gdata ~]# cd /mnt/poc/python/python-current/bin
[root@16gdata bin]# ls
2to3       datahub         dotenv         idle3.10    pip      __pycache__  python3            python3-config  wsdump.py
2to3-3.10  distro          humanfriendly  jsonschema  pip3     pydoc3       python3.10         tabulate
avro       docker-compose  idle3          normalizer  pip3.10  pydoc3.10    python3.10-config  wheel
```

2. 创建软连接：`ln -s /mnt/poc/python/python-current/bin/datahub /usr/local/bin/datahub`。
> 这样就可以愉快使用datahub客户端命令了。

```shell
[root@16gdata bin]# ll /usr/local/bin/datahub 
lrwxrwxrwx 1 root root 42 2月  18 17:42 /usr/local/bin/datahub -> /mnt/poc/python/python-current/bin/datahub
[root@16gdata bin]# datahub version
DataHub CLI version: 0.10.0
Python version: 3.10.10 (main, Feb 12 2023, 22:31:04) [GCC 4.8.5 20150623 (Red Hat 4.8.5-44)]
```


###  2. 要在本地部署 DataHub 实例，请从您的终端运行以下 CLI 命令
```shell
datahub docker quickstart
```
[这将使用docker-compose](https://docs.docker.com/compose/) 部署一个 DataHub 实例。docker-compose.yaml文件将下载到您的主目录下的目录中`~/.datahub/quickstart。` <br />说明：这种方式是从 github 下载下来一个 docker-compose。需要科学上网，如果你的服务器无法访问github。需要重试几次。如果你是离线部署，可以使用如下方式。

1. 使用离线的方式部署：下载datahub离线包到服务器上。解压，找到 `docker-compose-without-neo4j.quickstart.yml`文件。
```shell
datahub docker quickstart -f  ${DATAHUB_HOME}/docker/quickstart/docker-compose-without-neo4j.quickstart.yml
```
如果你是Mac电脑，用如下命令。
```shell
datahub docker quickstart -f  ${DATAHUB_HOME}/docker/quickstart/docker-compose-without-neo4j-m1.quickstart.yml
```

### 3. 完成此步骤后，您应该能够在浏览器中通过[http://localhost:9002](http://localhost:9002/) 导航到 DataHub UI。 datahub您可以使用用户名和密码登录。
> 笔记
> 在装有 Apple Silicon（M1、M2 等）的 Mac 计算机上，您可能会看到类似 的错误no matching manifest for linux/arm64/v8 in the manifest list entries，这通常意味着数据集线器 cli 无法检测到您正在 Apple Silicon 上运行它。要解决此问题，请通过发出以下命令覆盖默认体系结构检测datahub docker quickstart --arch m1


### 4. 要获取示例元数据，请从您的终端运行以下 CLI 命令
```shell
datahub docker ingest-sample-data
```

> 笔记
> 如果您启用了[元数据服务身份验证](https://datahubproject.io/docs/authentication/introducing-metadata-service-authentication)，则需要使用--token <token>命令中的参数提供个人访问令牌。
> 就是这样！现在可以随意使用 DataHub 了！


<a name="vr0hj"></a>
## 停止[数据中心](https://datahubproject.io/docs/quickstart#stopping-datahub)
要停止 DataHub 的快速启动，您可以发出以下命令。

```shell
datahub docker quickstart --stop
```

<a name="pziWH"></a>
## 升级本地[DataHub](https://datahubproject.io/docs/quickstart#upgrading-your-local-datahub)
如果你一直在本地测试 DataHub，新版本的 DataHub 发布了，你想尝试新版本，那么你可以再次发出 quickstart 命令。它将拉下较新的图像并重新启动您的实例而不会丢失任何数据。

```
datahub docker quickstart
```

<a name="aka6h"></a>
## 重置 DataHub（又名恢复出厂设置[）](https://datahubproject.io/docs/quickstart#resetting-datahub-aka-factory-reset)
要清除 DataHub 的所有状态（例如，在摄取您自己的状态之前），您可以使用 CLInuke命令。

```shell
datahub docker nuke
```

<a name="NQUOK"></a>
## 后续[步骤](https://datahubproject.io/docs/quickstart#next-steps)
<a name="FAYBZ"></a>
### 摄取元[数据](https://datahubproject.io/docs/quickstart#ingest-metadata)
要开始将贵公司的元数据推送到 DataHub，请查看[基于 UI 的摄取指南](https://datahubproject.io/docs/ui-ingestion)，或使用 cli 运行摄取，请查看[元数据摄取指南](https://datahubproject.io/docs/metadata-ingestion) 。

<a name="EPxGu"></a>
### 备份您的 DataHub 快速入门（实验性[）](https://datahubproject.io/docs/quickstart#backing-up-your-datahub-quickstart-experimental)
不建议将快速入门映像用作生产实例。有关设置生产集群的建议，请参阅[转向生产。](https://datahubproject.io/docs/quickstart#move-to-production)但是，如果您想备份当前的快速启动状态（例如，您有一个公司演示即将开始，并且您想要创建快速启动数据的副本，以便在将来恢复它），您可以提供--backup标志以快速启动。

```
datahub docker quickstart --backup
```

将备份您的 MySQL 映像，并默认将其~/.datahub/quickstart/作为文件写入您的目录backup.sql。您可以通过传递参数来自定义它--backup-file。例如

```
datahub docker quickstart --backup --backup-file /home/my_user/datahub_backups/quickstart_backup_2002_22_01.sql
```




