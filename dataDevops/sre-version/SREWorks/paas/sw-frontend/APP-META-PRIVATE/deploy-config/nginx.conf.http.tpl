daemon                          off;
worker_processes                4;

error_log                       /var/log/nginx/error.log warn;

events {
    worker_connections          1024;
}

http {
    include                     /etc/nginx/mime.types;
    default_type                application/octet-stream;
    sendfile                    on;
    access_log                  /var/log/nginx/access.log;
    keepalive_timeout           3000;

    proxy_connect_timeout 300s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;

    proxy_set_header        Host $host;
    proxy_set_header        X-Real-IP $remote_addr;
    proxy_set_header        Web-Server-Type nginx;
    proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_redirect          off;
    proxy_buffers           128 8k;
    proxy_intercept_errors  on;
    server_names_hash_bucket_size 512;

    gzip                    on;
    gzip_http_version       1.0;
    gzip_comp_level         6;
    gzip_min_length         1024;
    gzip_proxied            any;
    gzip_vary               on;
    gzip_disable            msie6;
    gzip_buffers            96 8k;
    gzip_types              text/xml text/plain text/css application/javascript application/x-javascript application/rss+xml application/json;

    client_header_timeout   30m;
    send_timeout            30m;
    client_max_body_size    1024m;

    server {
        listen              80 default_server;
        server_name         ${DNS_PAAS_HOME};

        default_type "text/javascript; charset=utf-8";

        location ~* /docs/documents/([0-9A-Za-z]+)$ {
            alias /app/docs/documents/;
            try_files $uri $uri.html;
        }

        location / {
            alias  /app/;
            try_files $uri $uri/ /index.html;
        }

        location /gateway/ {
            proxy_pass http://${ENDPOINT_PAAS_GATEWAY}/;
            proxy_connect_timeout 7200s;
            proxy_send_timeout 7200s;
            proxy_read_timeout 7200s;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_http_version 1.1;
        }
    }
}
