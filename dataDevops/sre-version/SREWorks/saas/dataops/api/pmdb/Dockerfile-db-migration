FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/sreworks/sw-migrate:latest
COPY ./APP-META-PRIVATE/db /sql
COPY ./sbin/db_init.sh /db_init.sh
RUN chmod +x /db_init.sh
ENTRYPOINT ["/db_init.sh"]