# 部署ui_moat前端

## 前端安装依赖

nvm install v16.20 && nvm use v16.20

npm run bootstrap

npm run start

npm run build:prod [生产]

生产环境启动前端micro-ui项目, 需要[配置nginx]

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
			root /studio/micro-ui/dist;
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

### 测试环境启动前端micro-ui项目

npm run dev [测试]

访问`moat_ui`页面

curl http://localhost:8013

用户名：admin 密码：123456