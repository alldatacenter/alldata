FROM {{ MIGRATE_IMAGE }}
#ENV DB_NAME ${DATA_DB_DATASOURCE_NAME}
#ENV DB_PORT ${DATA_DB_PORT}
COPY ./APP-META-PRIVATE/db-datasource /sql
COPY ./sbin/db_datasource_init.sh /db_datasource_init.sh
RUN chmod +x /db_datasource_init.sh
ENTRYPOINT ["/db_datasource_init.sh"]
