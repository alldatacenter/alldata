# 部署方式
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/226297187-d36d6ebf-9cdc-4e1a-81bb-860af018d14e.png">
<br/>
<br/>
<img width="1215" alt="image" src="https://user-images.githubusercontent.com/20246692/221345609-45a34a1a-8316-4810-8624-bc43a0e3c91d.png">
<br/>

| 16gmaster                | port | ip             |
|--------------------------|------| -------------- |
| system-service           | 8000 | 16gmaster  |
| data-market-service      | 8822 | 16gmaster  |
| service-data-integration | 8824 | 16gmaster  |
| data-metadata-service    | 8820 | 16gmaster  |
| data-system-service      | 8810 | 16gmaster  |
| service-data-dts         | 9536 | 16gmaster  |
| config                   | 8611 | 16gmaster  |

| 16gslave                      | port | ip             |
|-------------------------------| ---- | -------------- |
| eureka                  | 8610 | 16gslave    |
| service-workflow        | 8814 | 16gslave    |
| data-metadata-service-console    | 8821 | 16gslave    |
| service-data-mapping    | 8823 | 16gslave    |
| data-masterdata-service | 8828 | 16gslave    |
| data-quality-service    | 8826 | 16gslave    |

| 16gdata               | port | ip             |
|-----------------------| ---- | -------------- |
| data-standard-service | 8825 | 16gdata |
| data-visual-service   | 8827 | 16gdata |
| email-service         | 8812 | 16gdata |
| file-service          | 8811 | 16gdata |
| quartz-service        | 8813 | 16gdata |
| gateway               | 9538 | 16gslave    |


### 部署方式

> 数据库版本为 **mysql5.7** 及以上版本
### 1、`studio`数据库初始化
>
> 1.1 source install/16gmaster/studio/studio.sql
> 1.2 source install/16gmaster/studio/studio-v0.3.7.sql

### 2、修改 **config** 配置中心

> **config** 文件夹下的配置文件，修改 **redis**，**mysql** 和 **rabbitmq** 的配置信息
>
### 3、项目根目录下执行
> mvn clean install -DskipTests && mvn clean package -DskipTests
>
> 获取安装包build/studio-release-0.3.x.tar.gz
>
> 上传服务器解压
>
### 4、部署`stuido`[后端]
## 单节点启动[All In One]

> 1、启动eureka on `16gslave`
>
> 2、启动config on `16gmaster`
>
> 3、启动gateway on `16gdata`
>
> 4、启动其他Jar

## 三节点启动[16gmaster, 16gslave, 16gdata]
> 1. 单独启动 eureka on `16gslave`
>
> 2. 单独启动config on `16gmaster`
>
> 3. 单独启动gateway on `16gdata`
>
> 4. 启动`16gslave`, sh start16gslave.sh
>
> 5. 启动`16gdata`, sh start16gdata.sh
>
> 6. 启动`16gmaster`, sh start16gmaster.sh

### 5、部署`studio`[前端]:
## 前端部署

### 安装依赖

> 依次安装：
> nvm install v10.15.3 && nvm use v10.15.3

> npm install -g @vue/cli

> npm install script-loader

> npm install jsonlint

> npm install vue2-jsoneditor

> npm install

> npm run build:prod [生产]
>
> 生产环境启动前端ui项目，需要[配置nginx]
```markdown
# For more information on configuration, see:
#   * Official English Documentation: http://nginx.org/en/docs/
#   * Official Russian Documentation: http://nginx.org/ru/docs/

user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

# Load dynamic modules. See /usr/share/doc/nginx/README.dynamic.
include /usr/share/nginx/modules/*.conf;

events {
worker_connections 1024;
}

http {
log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
'$status $body_bytes_sent "$http_referer" '
'"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile            on;
    tcp_nopush          on;
    tcp_nodelay         on;
    keepalive_timeout   65;
    types_hash_max_size 4096;

    include             /etc/nginx/mime.types;
    default_type        application/octet-stream;

    # Load modular configuration files from the /etc/nginx/conf.d directory.
    # See http://nginx.org/en/docs/ngx_core_module.html#include
    # for more information.
    include /etc/nginx/conf.d/*.conf;
    server {
		listen       80;
		server_name  16gmaster;	
		add_header Access-Control-Allow-Origin *;
		add_header Access-Control-Allow-Headers X-Requested-With;
		add_header Access-Control-Allow-Methods GET,POST,OPTIONS;
		location / {
			root /studio/ui/dist;
			index index.html;
			try_files $uri $uri/ /index.html;
		}
		location /api/ {
			proxy_pass  http://16gdata:9538/;
			proxy_set_header Host $proxy_host;
			proxy_set_header X-Real-IP $remote_addr;
			proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
		}
	}
}
```
> 测试环境启动前端ui项目
>
> npm run dev [测试]
>
> 访问`studio`页面
>
> curl http://localhost:8013
>
> 用户名：admin 密码：123456