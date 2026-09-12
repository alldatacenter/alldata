# 本地容器化部署
## 准备
+ data-server部署包
+ docker
+ docker-compose

### 上传服务器
data-server-*.tar.gz

## Dockerfile
构建镜像需要使用到的配置文件，告诉docker如何构建镜像内容

创建并编辑 Dockerfile 文件

```plain
vim Dockerfile
```

添加以下内容

```dockerfile
FROM openjdk:1.8.1

COPY data-server-*.tar.gz /

RUN mkdir -p /opt/data-server && \
    tar -zxvf data-server-*.tar.gz -C /opt/data-server --strip-components 1 && \
    rm data-server-*.tar.gz && \
    chmod 755 /opt/data-server/bin/*.sh && \
    chmod 755 /opt/data-server/conf/*.sh

EXPOSE 18585

WORKDIR /opt/data-server

ENTRYPOINT ["/bin/bash","bin/startup-docker.sh"]

```

+ FROM openjdk:1.8.1
  - 设置基础镜像
  - data-server的jdk运行环境为1.8，因此这里直接指定一个包含jdk1.8运行环境的基础镜像
  - 也可以指定其他基础镜像，再另行安装jdk环境
+ COPY data-server-*.tar.gz /
  - 复制部署包到镜像内
+ RUN mkdir -p /opt/data-server && \

  tar -zxvf data-server-*.tar.gz -C /opt/data-server --strip-components 1 && \

  rm data-server-*.tar.gz && \

  chmod 755 /opt/data-server/bin/*.sh && \

  chmod 755 /opt/data-server/conf/*.sh

  - mkdir -p /opt/data-server：创建部署包解压目录
  - tar -zxvf data-server-*.tar.gz -C /opt/data-server --strip-components 1：解压部署包
  - rm data-server-*.tar.gz：删除部署包
  - chmod 755 /opt/data-server/bin/*.sh：修改文件权限
  - chmod 755 /opt/data-server/conf/*.sh：修改文件权限
+ EXPOSE 18585
  - 暴露容器端口
+ WORKDIR /opt/data-server
  - 设置容器工作目录，为镜像设置的指令都将在该目录下执行
+ ENTRYPOINT ["/bin/bash","bin/startup-docker.sh"]
  - 启动容器时执行的命令

保存并退出

## 构建镜像
执行以下命令

```plain
docker build -f Dockerfile -t data-server:0.6.x .
```

+ docker build：构建镜像
+ -f：指定配置文件来构建镜像，如 Dockerfile
+ -t：指定镜像标签，如 data-server:0.6.x
  - data-server：镜像名称
  - 1.0.0：版本号

执行完成后，执行以下命令查看镜像

```plain
docker images
```

## 启动容器
执行以下命令，配置 docker-compose.yml

```plain
vim docker-compose.yml
```

添加以下内容

```plain
version: '2' # docker-compose 版本号

services: # 服务列表
  data-server: # 服务名称
    restart: always # 无论容器退出状态如何，总是重启容器
    image: data-server:0.6.x # 镜像
    container_name: data-server # 容器名称
    ports:
      - 服务port:服务port # 宿主机端口以及容器端口
    extra_hosts:
      - "mysql_host_ip:43.138.156.44" # 数据库服务主机
      - "服务ip:服务ip" # data-server 服务主机
```

启动

```plain
docker-compose up -d
```

## 镜像推送
### 私有云仓库创建
首先需要有自己的私有仓库，这里使用的腾讯云服务器容器镜像服务个人版

相关信息可参考进行了解：[https://cloud.tencent.com/document/product/1141/63910#.E6.8E.A8.E9.80.81.E5.AE.B9.E5.99.A8.E9.95.9C.E5.83.8F](https://cloud.tencent.com/document/product/1141/63910#.E6.8E.A8.E9.80.81.E5.AE.B9.E5.99.A8.E9.95.9C.E5.83.8F)



配置好自己的私有云仓库后，在服务器终端页面使用以下命令连接私有仓库，按提示输入密码即可连接

```plain
sudo docker login ccr.ccs.tencentyun.com --username=100043854373
```

### 推送镜像
基于本地构建好的镜像，创建私有云仓库标签引用，用于推送

```plain
docker tag data-server:0.6.x ccr.ccs.tencentyun.com/aolingdata/data-server:0.6.x
```

推送到私有仓库

```plain
docker push ccr.ccs.tencentyun.com/aolingdata/data-server:0.6.x
```

推送成功后，可以在我们的私有仓库云看到

[https://console.cloud.tencent.com/tcr/repository](https://console.cloud.tencent.com/tcr/repository)

# 云仓库镜像部署
确保已连接私有云仓库，可参考1.5.1

## 编写 docker-compose.yml 文件
执行以下命令，配置 docker-compsoe.yml

```plain
vim docker-compose.yml
```

文件内容

```plain
version: '2' # docker-compose 版本号

services: # 服务列表
  data-server: # 服务名称
    restart: always # 无论容器退出状态如何，总是重启容器
    image: ccr.ccs.tencentyun.com/aolingdata/data-server:0.6.x # 镜像
    container_name: data-server # 容器名称
    ports:
      - 服务端口:服务端口
    extra_hosts:
      - "mysql_host_ip:mysql_host_ip" # 数据库服务主机
```

这里对 application.yml 进行挂载是为了方便修改配置，如 MySQL 连接信息

## 启动
执行以下命令，启动容器

```plain
docker-compose up -d
```

此时docker会自动下载镜像，然后启动容器