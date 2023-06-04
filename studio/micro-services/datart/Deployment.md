---
部署
---

# 0. 在线体验 Demo

- http://datart-demo.retech.cc
- 用户名：demo
- 密码：123456

# 1. Docker 部署

```shell
docker run -p 8080:8080 datart/datart 
```
启动后可访问 <http://docker_ip:8080>  
默认账户:用户名`demo`,密码`123456`

## 1.1. 配置外部数据库
在没有外部数据库配置的情况下，Datart使用H2作为应用程序数据库。 强烈建议您将自己的Mysql数据库配置为应用程序数据库。  

创建空文件 `datart.conf` ,将以下内容粘贴到到文件中。

```shell
# 数据库连接配置
datasource.ip=   
datasource.port=
datasource.database=
datasource.username=
datasource.password=

# server
server.port=8080
server.address=0.0.0.0

# datart config
datart.address=http://127.0.0.1
datart.send-mail=false
datart.webdriver-path=http://127.0.0.1:4444/wd/hub
```

运行 `docker run -d --name datart -v your_path/datart.conf:/datart/config/datart.conf -p 8080:8080 datart/datart`

## 1.2. 将用户文件挂载到外部

默认配置下，用户文件（头像，文件数据源等）保存在 `files` 文件夹下，将这个路径挂载到外部，以在进行应用升级时，能够保留这些文件。

在配置文件中增加参数 `-v your_path/files:/datart/files` 即可。以下是完整命令

`docker run -d --name datart -v your_path/datart.conf:/datart/config/datart.conf -v your_path/files:/datart/files -p 8080:8080 datart/datart`

***更多配置，访问 <http://running-elephant.gitee.io/datart-docs/docs/index.html> ***

# 2. 本地部署 
## 2.1. 环境准备

- JDK 1.8+
- MySql5.7+
- datart安装包（datart-server-1.0.0-beta.x-install.zip)
- Mail Server （可选）
- [ChromeWebDriver](https://chromedriver.chromium.org/) （可选）
- Redis （可选）

方式1 :解压安装包 (官方提供的包)

```bash
unzip datart-server-1.0.0-beta.x-install.zip
```

方式2 :自行编译

```bash
git clone https://github.com/running-elephant/datart.git

cd datart

mvn clean package -Dmaven.test.skip=true

cp ./datart-server-1.0.0-beta.x-install.zip  ${deployment_basedir}

cd ${deployment_basedir}

unzip datart-server-1.0.0-beta.x-install.zip 

```

## 2.2. 以独立模式运行

安装包解压后，即可运行 ./bin/datart-server.sh start 来启动datart,启动后默认访问地址是: <http://127.0.0.1:8080>,默认用户`demo/123456`

***独立模式使用内置数据库作为应用数据库，数据的安全性和数据迁移无法保证，建议配置外部数据库作为应用数据库***

## 2.3. 配置外部数据库，要求Mysql5.7及以上版本。

- 创建数据库，指定数据库编码为utf8

```bash
mysql> CREATE DATABASE `datart` CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
```

***注意：1.0.0-beta.2版本以前，需要手动执行`bin/datart.sql`来初始化数据库。此版本及以上版本，创建好数据库即可，在初次连接时会自动初始化数据库***

***首次连接数据库(或者版本升级)时,建议使用一个权限较高的数据库账号登录(如root账号)。因为首次连接会执行数据库初始化脚本，如果使用的数据库账号权限太低，会导致数据库初始化失败***

- 基础配置：配置文件位于 config/datart.conf

```bash
   数据库配置(必填):
    1. datasource.ip(数据库IP地址)
    2. datasource.port(数据库端口数据库端口)
    3. datasource.database(指定数据库)
    4. datasource.username(用户名)
    5. datasource.password(密码)
    
   其它配置(选填):
    1. server.port(应用绑定端口地址,默认8080)
    2. server.address(应用绑定IP地址,默认 0.0.0.0)
    3. datart.address(datart 外部可访问地址,默认http://127.0.0.1)
    4. datart.send-mail(用户注册是否使用邮件激活,默认 false )
    5. datart.webdriver-path(截图驱动)
```

## 2.4. 高级配置 (可选) : 配置文件位于 config/profiles/application-config.yml

***高级配置文件格式是yml格式,配置错误会导致程序无法启动。配置时一定要严格遵循yml格式。***

***application-config.yml直接由spring-boot处理,其中的oauth2,redis,mail等配置项完全遵循spring-boot-autoconfigure配置***

### 2.4.1 配置文件信息

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://localhost:3306/datart?&allowMultiQueries=true
    username: datart
    password: datart123

  # mail config  is a aliyum email example 
  mail:
    host: smtp.mxhichina.com
    port: 465
    username: aliyun.djkjfhdjfhjd@aliyun.cn
    fromAddress: aliyun.djkjfhdjfhjd@aliyun.cn
    password: hdjksadsdjskdjsnd
    senderName: aliyun

    properties:
      smtp:
        starttls:
          enable: true
          required: true
        auth: true
      mail:
        smtp:
          ssl:
            enable: true
            trust: smtp.mxhichina.com

# redis config 如需开启缓存 需要配置
#  redis:
#    port: 6379
#    host: { HOST }

# 服务端配置 Web服务绑定IP和端口 使用 本机ip + 指定端口
server:
  port: youport
  address: youip


  compression:
    enabled: true
    mime-types: application/javascript,application/json,application/xml,text/html,text/xml,text/plain,text/css,image/*

# 配置服务端访问地址，创建分享，激活/邀请用户时，将使用这个地址作为服务端访问地址。 对外有域名的情况下可使用域名 
datart:
  server:
    address: http://youip:youport

  user:
    active:
      send-mail: true  # 注册用户时是否需要邮件验证激活，如果没配置邮箱，这里需要设置为false


  security:
    token:
      secret: "d@a$t%a^r&a*t" #加密密钥
      timeout-min: 30  # 登录会话有效时长，单位：分钟。

  env:
    file-path: ${user.dir}/files # 服务端文件保存位置

  # 可选配置 如需配置请参照 [3.2 截图配置 [ChromeWebDriver]-可选]
  screenshot:
    timeout-seconds: 60
    webdriver-type: CHROME
    webdriver-path: "http://youip:4444/wd/hub"

```

*注意：加密密钥每个服务端部署前应该进行修改，且部署后不能再次修改。如果是集群部署，同一个集群内的secret要保持统一*

### 2.4.2 截图配置 [ChromeWebDriver]-可选

```bash

docker pull selenium/standalone-chrome  # 拉取docker镜像

docker run -p 4444:4444 -d --name selenium-chrome --shm-size="2g" selenium/standalone-chrome  # run

```

### 2.5. 启动服务

*注意：启动脚本 已更新了 start|stop|status|restart*

```base
${DATART_HOME}/bin/datart-server.sh (start|stop|status|restart)
```

### 2.5 访问服务

*注意：没有默认用户 直接注册 成功后直接登录即可*

```base
http://youip:youport/login
```
