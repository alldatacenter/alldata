UPDATE ta_user SET user_id = "empid::${ACCOUNT_SUPER_PK}", tenant_id = "alibaba" WHERE aliyun_pk = "${ACCOUNT_SUPER_PK}";
UPDATE ta_user SET user_id = "empid::${ACCOUNT_ODPS_PK}", tenant_id = "alibaba" WHERE aliyun_pk = "${ACCOUNT_ODPS_PK}";
UPDATE ta_user SET user_id = "empid::${ACCOUNT_BASE_PK}", tenant_id = "alibaba" WHERE aliyun_pk = "${ACCOUNT_BASE_PK}";
