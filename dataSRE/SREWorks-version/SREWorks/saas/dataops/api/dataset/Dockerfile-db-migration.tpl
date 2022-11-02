FROM {{ MIGRATE_IMAGE }}
#ENV DB_NAME ${DATA_DB_DATASET_NAME}
#ENV DB_PORT ${DATA_DB_PORT}
COPY ./APP-META-PRIVATE/db /sql
COPY ./sbin/db_init.sh /db_init.sh
RUN chmod +x /db_init.sh
ENTRYPOINT ["/db_init.sh"]
