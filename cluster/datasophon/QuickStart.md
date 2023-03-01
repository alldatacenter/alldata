# DataSophon部署文档

## 环境准备

#### 网络要求

要求各机器各组件正常运行提供如下的网络端口配置：

| **组件**                | **默认端口**     | **说明**                                                  |
| ----------------------- | ---------------- | --------------------------------------------------------- |
| DDHApplicationServer    | 8081、2551、8586 | 8081为http server端口，2551为rpc通信端口，8586为jmx端口   |
| WorkerApplicationServer | 2552、9100、8585 | 2552 rpc通信端口，8585为jmx端口，9100为主机数据采集器端口 |
| nginx                   | 8888             | 提供 UI 端通信端口                                        |

查看端口是否占用命令：

```
netstat -anp |grep port
```



#### 客户端浏览器要求

推荐 Chrome 以及使用 Chrome 内核的较新版本浏览器访问前端可视化操作界面。



#### 关闭防火墙

各主机防火墙需关闭，命令如下

```
systemctl stop firewalld
```



#### 配置主机host

大数据集群所有机器需配置主机host。修改配置文件/etc/hosts

```
vi /etc/hosts
格式如下：
192.168.0.xxx hostname
```



#### 免密登录配置

部署机器中，DataSophon节点以及大数据服务主节点与从节点之间需免密登录。

生成ssh 秘钥：

```
ssh-keygen -P ''
```

执行 ssh-copy-id 目标主机：

```
ssh-copy-id -i ~/.ssh/id_rsa.pub root@192.168.0.xxx
```



#### 环境要求

Jdk环境需安装。建议mysql版本为5.7.X，并关闭ssl。

查看ssl状态，mysql客户端输入：

```
show variables like '%ssl%'
```

看到have_ssl的值为YES，表示已开启SSL。（have_openssl表示是否支持SSL）

修改配置文件my.cnf，加入以下内容：

```
# disable_ssl
skip_ssl
```

再次查看ssl状态发现为DISABLED，即为关闭成功



#### 组件介绍

DDHApplicationServer为API接口层即web后端，主要负责处理前端UI层的请求。该服务统一提供RESTful api向外部提供请求服务。

WorkerApplicationServer负责执行DDHApplicationServer发送的指令，包括服务安装、启动、停止、重启等指令。



## 快速部署

#### 执行mysql脚本

```sql
CREATE DATABASE IF NOT EXISTS datasophon DEFAULT CHARACTER SET utf8;
set global validate_password_policy = low;
grant all privileges on *.* to datasophon@"%" identified by 'datasophon' with grant option;
GRANT ALL PRIVILEGES ON *.* TO 'datasophon'@'%';
FLUSH PRIVILEGES;
```

初始化sql文件，sql文件存放于datasophon-manager-1.0.0路径的sql路径下

```sql
source /datasophon-manager-1.0.0/sql/datasophon-manager-1.0.0.sql
```

常见错误：

ERROR 1118 (42000): Row size too large (> 8126). Changing some columns to TEXT or BLOB or using ROW_FORMAT=DYNAMIC or ROW_FORMAT=COMPRESSED may help. In current row format, BLOB prefix of 768 bytes is stored inline.

此问题由于mysql版本不一致造成，推荐使用版本5.7，mysql8.0报错如上



解决方法：

修改ROW_FORMAT = COMPACT为 ROW_FORMAT = DYNAMIC





#### 添加如下配置文件到nginx的conf目录下

```yaml
 server {
                listen 8888;# 访问端口(自行修改)
                server_name localhost;
                #charset koi8-r;
                #access_log /var/log/nginx/host.access.log main;
                location / {
                        root /usr/local/nginx/dist; # 前端解压的 dist 目录地址(自行修改)
                        index index.html index.html;
                }
                location /ddh {
                        proxy_pass http://ddp1:8081; # 接口地址(自行修改)
                        proxy_set_header Host $host;
                        proxy_set_header X-Real-IP $remote_addr;
                        proxy_set_header x_real_ipP $remote_addr;
                        proxy_set_header remote_addr $remote_addr;
                        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                        proxy_http_version 1.1;
                        proxy_connect_timeout 4s;
                        proxy_read_timeout 30s;
                        proxy_send_timeout 12s;
                        proxy_set_header Upgrade $http_upgrade;
                        proxy_set_header Connection "upgrade";
                }

                #error_page 404 /404.html;
                # redirect server error pages to the static page /50x.html
                #
                error_page 500 502 503 504 /50x.html;
                location = /50x.html {
                        root /usr/share/nginx/html;
                }
        }
```



重新加载nginx配置文件：

```
nginx -s reload
```



查看nginx服务是否启动成功：

```
lsof -i:8888
```



#### 修改Datasophon配置文件

路径在/datasophon-manager-1.0.0/conf/application.yml



#### 启动Datasophon服务

```
bin/datasophon-api.sh start api
```



#### 访问Web端

端口为自己配置的端口，用户密码默认为admin/admin123

```
http://192.168.xx.xx:8888
```





## 界面部署

#### 创建集群

集群编码可以理解为主键，我们给值为1

![1677239660970](C:\Users\86138\AppData\Roaming\Typora\typora-user-images\1677239660970.png)



#### 配置集群

- ssh连接方式，按照自己的机器配置即可，主机列表可以使用正则表达式或者逗号分隔即可

![1677239711611](C:\Users\86138\AppData\Roaming\Typora\typora-user-images\1677239711611.png)

- 主机分发需要保证/opt/datasophon/DDP/packages路径下有datasophon-worker.tar.gz包，或者通过修改common.properties中的installpath路径

![1677241531379](C:\Users\86138\AppData\Roaming\Typora\typora-user-images\1677241531379.png)



进度卡死如上，75%然后就失败！