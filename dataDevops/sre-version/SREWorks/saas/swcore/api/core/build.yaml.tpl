componentType: K8S_MICROSERVICE
componentName: paas-authproxy
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        repoPath: paas/tesla-authproxy
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  initContainers:
    - name: db-migration
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile_db_migration.tpl
        repoPath: paas/tesla-authproxy
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
  env:
    - AAS_DIRECT_LOGIN_URL
    - AAS_INNER_ENDPOINT
    - AAS_LOGIN_URL
    - AAS_LOGOUT_URL
    - AAS_OPENAPI_DOMAIN
    - AAS_OPENAPI_URL
    - AAS_POP_KEY
    - AAS_POP_SECRET
    - ABM_CLUSTER
    - ACCOUNT_BASE_ACCESS_ID
    - ACCOUNT_BASE_ACCESS_KEY
    - ACCOUNT_BASE_ID
    - ACCOUNT_BASE_PK
    - ACCOUNT_BASE_SECRET_KEY
    - ACCOUNT_ODPS_ACCESS_ID
    - ACCOUNT_ODPS_ACCESS_KEY
    - ACCOUNT_ODPS_ID
    - ACCOUNT_ODPS_PK
    - ACCOUNT_ODPS_SECRET_KEY
    - ACCOUNT_SUPER_ACCESS_ID
    - ACCOUNT_SUPER_ACCESS_KEY
    - ACCOUNT_SUPER_ID
    - ACCOUNT_SUPER_PK
    - ACCOUNT_SUPER_SECRET_KEY
    - ACCOUNT_SUPER_CLIENT_ID
    - ACCOUNT_SUPER_CLIENT_SECRET
    - ASS_CALLBACK_URL
    - CENTER_REGION
    - CICD_PROJECTID
    - CLOUD_TYPE
    - CLUSTER_SCALE_FLAG
    - COOKIE_DOMAIN
    - HOME_URL
    - DATABASE_AUTH_SERVICE_MGR_CLASS_NAME
    - DATABASE_LOGIN_INTERCEPTOR_CLASS_NAME
    - DB_HOST
    - DB_NAME
    - DB_PASSWORD
    - DB_PORT
    - DB_USER
    - DNS_PAAS_HOME
    - DNS_PAAS_HOME_DISASTER
    - DUBBO_DOMAIN
    - ENDPOINT_GRPC_PAAS_CHECK
    - ENDPOINT_GRPC_PAAS_TASKPLATFORM
    - ENDPOINT_GRPC_PAAS_TIANJI
    - ENDPOINT_PAAS_AUTHPROXY
    - ENDPOINT_PAAS_CHANNEL
    - ENDPOINT_PAAS_CHECK
    - ENDPOINT_PAAS_CMDB
    - ENDPOINT_PAAS_CONNECTOR
    - ENDPOINT_PAAS_DR_MUTILCLOUD
    - ENDPOINT_PAAS_FAAS_MANAGER
    - ENDPOINT_PAAS_FRONTEND_SERVICE
    - ENDPOINT_PAAS_GATEWAY
    - ENDPOINT_PAAS_GRAFANA
    - ENDPOINT_PAAS_HOME
    - ENDPOINT_PAAS_MINIO
    - ENDPOINT_PAAS_PROCESS
    - ENDPOINT_PAAS_PRODUCTOPS
    - ENDPOINT_PAAS_STARAGENT
    - ENDPOINT_PAAS_TASKPLATFORM
    - ENDPOINT_PAAS_TIANJI
    - ENDPOINT_PAAS_TKGONE
    - ENDPOINT_SAAS_BASE_CONSOLE
    - ENDPOINT_SAAS_BLINK
    - ENDPOINT_SAAS_CALIFORNIA
    - ENDPOINT_SAAS_DATAHUB
    - ENDPOINT_SAAS_DATAWORKS
    - ENDPOINT_SAAS_DWG
    - ENDPOINT_SAAS_EBLINK
    - ENDPOINT_SAAS_ELASTICSEARCH
    - ENDPOINT_SAAS_GRAPHCOMPUTE
    - ENDPOINT_SAAS_HOLO
    - ENDPOINT_SAAS_ODPS
    - ENDPOINT_SAAS_STANDARD_CLUSTER
    - ENDPOINT_SAAS_TESLA
    - ENV_TYPE
    - IDC_MAP
    - IDC_ROOM
    - LOGIN_URL
    - NETWORK_PROTOCOL
    - OAM_ENDPOINT
    - OAM_POP_KEY
    - OAM_POP_SECRET
    - REDIS_DB
    - REDIS_HOST
    - REDIS_PASSWORD
    - REDIS_PORT
    - REGION
    - TESLA_ADMIN_USERS
    - TESLA_AUTHPROXY_OAUTH2_ACCESS_TOKEN_URI
    - TESLA_AUTHPROXY_OAUTH2_CLIENT_ID
    - TESLA_AUTHPROXY_OAUTH2_CLIENT_SECRET
    - TESLA_AUTHPROXY_OAUTH2_REDIRECT_URI
    - TESLA_AUTHPROXY_OAUTH2_USER_AUTHORIZATION_URI
    - TESLA_AUTHPROXY_OAUTH2_USER_INFO_URI
    - UMM_AK_ID
    - UMM_AK_SECRET
    - UMM_ENDPOINT
    - URL_PAAS_GATEWAY
    - URL_PAAS_HOME
    - VIP_IP_PAAS_GRAFANA
    - VIP_IP_PAAS_HOME
    - VIP_IP_PAAS_TASKPLATFORM_GRPC
    - VIP_IP_PAAS_TKGONE
    - VIP_PORT_PAAS_TASKPLATFORM_GRPC
    - VIP_PORT_PAAS_TKGONE
    - VM_IP_LIST
    - ZONE
    - ZOOKEEPER_ENDPOINT
    - ZOOKEEPER_HOSTS
    - ZOOKEEPER_PORT
    - OAUTH2_JWT_SECRET_KEY
    - DEFAULT_AMDIN_AVATOR
    - ADMIN_INIT_PASSWORD

---
componentType: K8S_JOB
componentName: paas-authproxy-postrun
options:
  job:
    name: job
    build:
      imagePush: ${IMAGE_BUILD_ENABLE}
      imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
      args:
        TAG: ack
      dockerfileTemplateArgs:
        POSTRUN_IMAGE: ${POSTRUN_IMAGE}
      dockerfileTemplate: Dockerfile_postrun.tpl
      repoPath: paas/tesla-authproxy
      branch: ${SOURCE_BRANCH}
      repo: ${SOURCE_REPO}
      ciAccount: ${SOURCE_CI_ACCOUNT}
      ciToken: ${SOURCE_CI_TOKEN}
 
  env:
    - AAS_DIRECT_LOGIN_URL
    - AAS_INNER_ENDPOINT
    - AAS_LOGIN_URL
    - AAS_LOGOUT_URL
    - AAS_OPENAPI_DOMAIN
    - AAS_OPENAPI_URL
    - AAS_POP_KEY
    - AAS_POP_SECRET
    - ABM_CLUSTER
    - ACCOUNT_BASE_ACCESS_ID
    - ACCOUNT_BASE_ACCESS_KEY
    - ACCOUNT_BASE_ID
    - ACCOUNT_BASE_PK
    - ACCOUNT_BASE_SECRET_KEY
    - ACCOUNT_ODPS_ACCESS_ID
    - ACCOUNT_ODPS_ACCESS_KEY
    - ACCOUNT_ODPS_ID
    - ACCOUNT_ODPS_PK
    - ACCOUNT_ODPS_SECRET_KEY
    - ACCOUNT_SUPER_ACCESS_ID
    - ACCOUNT_SUPER_ACCESS_KEY
    - ACCOUNT_SUPER_ID
    - ACCOUNT_SUPER_PK
    - ACCOUNT_SUPER_SECRET_KEY
    - ACCOUNT_SUPER_CLIENT_ID
    - ACCOUNT_SUPER_CLIENT_SECRET
    - ASS_CALLBACK_URL
    - CENTER_REGION
    - CICD_PROJECTID
    - CLOUD_TYPE
    - CLUSTER_SCALE_FLAG
    - COOKIE_DOMAIN
    - HOME_URL
    - DATABASE_AUTH_SERVICE_MGR_CLASS_NAME
    - DATABASE_LOGIN_INTERCEPTOR_CLASS_NAME
    - DB_HOST
    - DB_NAME
    - DB_PASSWORD
    - DB_PORT
    - DB_USER
    - DNS_PAAS_HOME
    - DNS_PAAS_HOME_DISASTER
    - DUBBO_DOMAIN
    - ENDPOINT_GRPC_PAAS_CHECK
    - ENDPOINT_GRPC_PAAS_TASKPLATFORM
    - ENDPOINT_GRPC_PAAS_TIANJI
    - ENDPOINT_PAAS_AUTHPROXY
    - ENDPOINT_PAAS_CHANNEL
    - ENDPOINT_PAAS_CHECK
    - ENDPOINT_PAAS_CMDB
    - ENDPOINT_PAAS_CONNECTOR
    - ENDPOINT_PAAS_DR_MUTILCLOUD
    - ENDPOINT_PAAS_FAAS_MANAGER
    - ENDPOINT_PAAS_FRONTEND_SERVICE
    - ENDPOINT_PAAS_GATEWAY
    - ENDPOINT_PAAS_GRAFANA
    - ENDPOINT_PAAS_HOME
    - ENDPOINT_PAAS_MINIO
    - ENDPOINT_PAAS_PROCESS
    - ENDPOINT_PAAS_PRODUCTOPS
    - ENDPOINT_PAAS_STARAGENT
    - ENDPOINT_PAAS_TASKPLATFORM
    - ENDPOINT_PAAS_TIANJI
    - ENDPOINT_PAAS_TKGONE
    - ENDPOINT_SAAS_BASE_CONSOLE
    - ENDPOINT_SAAS_BLINK
    - ENDPOINT_SAAS_CALIFORNIA
    - ENDPOINT_SAAS_DATAHUB
    - ENDPOINT_SAAS_DATAWORKS
    - ENDPOINT_SAAS_DWG
    - ENDPOINT_SAAS_EBLINK
    - ENDPOINT_SAAS_ELASTICSEARCH
    - ENDPOINT_SAAS_GRAPHCOMPUTE
    - ENDPOINT_SAAS_HOLO
    - ENDPOINT_SAAS_ODPS
    - ENDPOINT_SAAS_STANDARD_CLUSTER
    - ENDPOINT_SAAS_TESLA
    - ENV_TYPE
    - IDC_MAP
    - IDC_ROOM
    - LOGIN_URL
    - NETWORK_PROTOCOL
    - OAM_ENDPOINT
    - OAM_POP_KEY
    - OAM_POP_SECRET
    - REDIS_DB
    - REDIS_HOST
    - REDIS_PASSWORD
    - REDIS_PORT
    - REGION
    - TESLA_ADMIN_USERS
    - TESLA_AUTHPROXY_OAUTH2_ACCESS_TOKEN_URI
    - TESLA_AUTHPROXY_OAUTH2_CLIENT_ID
    - TESLA_AUTHPROXY_OAUTH2_CLIENT_SECRET
    - TESLA_AUTHPROXY_OAUTH2_REDIRECT_URI
    - TESLA_AUTHPROXY_OAUTH2_USER_AUTHORIZATION_URI
    - TESLA_AUTHPROXY_OAUTH2_USER_INFO_URI
    - UMM_AK_ID
    - UMM_AK_SECRET
    - UMM_ENDPOINT
    - URL_PAAS_GATEWAY
    - URL_PAAS_HOME
    - VIP_IP_PAAS_GRAFANA
    - VIP_IP_PAAS_HOME
    - VIP_IP_PAAS_TASKPLATFORM_GRPC
    - VIP_IP_PAAS_TKGONE
    - VIP_PORT_PAAS_TASKPLATFORM_GRPC
    - VIP_PORT_PAAS_TKGONE
    - VM_IP_LIST
    - ZONE
    - ZOOKEEPER_ENDPOINT
    - ZOOKEEPER_HOSTS
    - ZOOKEEPER_PORT
    - DEFAULT_AMDIN_AVATOR
---
componentType: K8S_MICROSERVICE
componentName: paas-frontend
options:
  containers:
    - ports:
        - containerPort: 80
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          ALPINE_IMAGE: ${ALPINE_IMAGE}
          NODE_IMAGE: ${NODE_IMAGE}
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          NPM_REGISTRY_URL: ${NPM_REGISTRY_URL}
        dockerfileTemplate: Dockerfile_standalone.tpl
        repoPath: paas/sw-frontend
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
 
  env:
    - ABM_CLUSTER
    - CENTER_REGION
    - CICD_PROJECTID
    - CLOUD_TYPE
    - CLUSTER_SCALE_FLAG
    - COOKIE_DOMAIN
    - HOME_URL
    - DNS_PAAS_COMPATIBLE_HOME
    - DNS_PAAS_HOME
    - DNS_PAAS_HOME_DISASTER
    - ENDPOINT_GRPC_PAAS_CHECK
    - ENDPOINT_GRPC_PAAS_TASKPLATFORM
    - ENDPOINT_GRPC_PAAS_TIANJI
    - ENDPOINT_PAAS_AUTHPROXY
    - ENDPOINT_PAAS_CHANNEL
    - ENDPOINT_PAAS_CHECK
    - ENDPOINT_PAAS_CMDB
    - ENDPOINT_PAAS_CONNECTOR
    - ENDPOINT_PAAS_DR_MUTILCLOUD
    - ENDPOINT_PAAS_FAAS_MANAGER
    - ENDPOINT_PAAS_FRONTEND_SERVICE
    - ENDPOINT_PAAS_GATEWAY
    - ENDPOINT_PAAS_GRAFANA
    - ENDPOINT_PAAS_HOME
    - ENDPOINT_PAAS_MINIO
    - ENDPOINT_PAAS_PROCESS
    - ENDPOINT_PAAS_PRODUCTOPS
    - ENDPOINT_PAAS_STARAGENT
    - ENDPOINT_PAAS_TASKPLATFORM
    - ENDPOINT_PAAS_TIANJI
    - ENDPOINT_PAAS_TKGONE
    - ENDPOINT_SAAS_BASE_CONSOLE
    - ENDPOINT_SAAS_BLINK
    - ENDPOINT_SAAS_CALIFORNIA
    - ENDPOINT_SAAS_DATAHUB
    - ENDPOINT_SAAS_DATAWORKS
    - ENDPOINT_SAAS_DWG
    - ENDPOINT_SAAS_EBLINK
    - ENDPOINT_SAAS_ELASTICSEARCH
    - ENDPOINT_SAAS_GRAPHCOMPUTE
    - ENDPOINT_SAAS_HOLO
    - ENDPOINT_SAAS_ODPS
    - ENDPOINT_SAAS_STANDARD_CLUSTER
    - ENDPOINT_SAAS_TESLA
    - ENV_TYPE
    - EXTERNAL_DATAWORKS_CONSOLE_URL
    - IDC_MAP
    - IDC_ROOM
    - NETWORK_PROTOCOL
    - REGION
    - URL_PAAS_GATEWAY
    - URL_PAAS_HOME
    - VIP_IP_PAAS_GRAFANA
    - VIP_IP_PAAS_HOME
    - VIP_IP_PAAS_TASKPLATFORM_GRPC
    - VIP_IP_PAAS_TKGONE
    - VIP_PORT_PAAS_TASKPLATFORM_GRPC
    - VIP_PORT_PAAS_TKGONE
    - VM_IP_LIST
    - ZONE
    - K8S_NAMESPACE
    - PLATFORM_NAME
    - PLATFORM_LOGO

---
componentType: K8S_MICROSERVICE
componentName: paas-gateway
options:
  containers:
    - ports:
        - containerPort: 7001
        - containerPort: 7002
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile_paas.tpl
        repoPath: paas/tesla-gateway
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
 
  env:
    - DB_HOST
    - DB_PORT
    - DB_USER
    - DB_PASSWORD
    - DB_NAME
    - REDIS_HOST
    - REDIS_PORT
    - REDIS_DB
    - REDIS_PASSWORD
    - REGION
    - IDC_ROOM
    - AUTH_ADMIN_TOKEN
    - ENDPOINT_PAAS_AUTHPROXY
    - AUTH_COOKIE_KEY
    - AUTH_COOKIE_NAME
    - AUTH_JWT_SECRET
    - TESLA_AUTH_APP
    - TESLA_AUTH_KEY
    - STORE_NACOS_DATA_ID
    - STORE_NACOS_GROUP
    - NACOS_NAMESPACE
    - NACOS_ENDPOINT
    - MANAGER_SERVER_PORT
    - CORE_APP_ID
    - CORE_STAGE_ID

---


componentType: K8S_JOB
componentName: paas-gateway-route-config
options:
  env:
    - ENDPOINT_PAAS_GATEWAY
    - ACCOUNT_SUPER_CLIENT_ID
    - ACCOUNT_SUPER_CLIENT_SECRET
    - ACCOUNT_SUPER_ID
    - ACCOUNT_SUPER_SECRET_KEY
    - ENDPOINT_PAAS_APPMANAGER
    - CORE_APP_ID
    - CORE_STAGE_ID
    - NAMESPACE_DATAOPS
    - ENDPOINT_PAAS_MINIO
  job:
    build:
      imagePush: ${IMAGE_BUILD_ENABLE}
      imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
      dockerfileTemplate: Dockerfile_route_config.tpl
      branch: ${SOURCE_BRANCH}
      repo: ${SOURCE_REPO}
      ciAccount: ${SOURCE_CI_ACCOUNT}
      ciToken: ${SOURCE_CI_TOKEN}
      repoPath: paas/tesla-gateway
      dockerfileTemplateArgs:
        POSTRUN_IMAGE: ${POSTRUN_IMAGE}
    name: route-config


---
componentType: K8S_MICROSERVICE
componentName: paas-action
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML} 
        dockerfileTemplate: Dockerfile.tpl
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
        repoPath: paas/action
 
  initContainers:
    - name: db-migration
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile_db_migration.tpl
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
        repoPath: paas/action
  env:
    - DB_HOST
    - DB_PORT
    - DB_USER
    - DB_PASSWORD
    - DB_NAME
    - ENDPOINT_PAAS_TKGONE
    - URL_PAAS_HOME
    - ENDPOINT_PAAS_HOME

---

componentType: K8S_MICROSERVICE
componentName: paas-nacos
options:
  containers:
    - ports:
        - containerPort: 8848
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
        repoPath: paas/nacos

  initContainers:
    - name: db-migration
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: ack
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile_db_migration.tpl
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
        repoPath: paas/nacos
  env:
    - DB_HOST
    - DB_PORT
    - DB_USER
    - DB_PASSWORD
    - DB_NAME
    - REGION
    - IDC_ROOM
    - NACOS_SERVERS


