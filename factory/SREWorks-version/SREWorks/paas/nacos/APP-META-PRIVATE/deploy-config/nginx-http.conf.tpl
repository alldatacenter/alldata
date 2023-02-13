server {
    server_name  ${private_bcc_nacos_endpoint};
    charset utf-8;

    location / {
        proxy_pass http://localhost:8848/;
    }
}
