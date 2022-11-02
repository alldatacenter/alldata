apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: "master"
    - name: NAMESPACE_ID
      value: "${NAMESPACE_ID}"
    - name: STAGE_ID
      value: "prod"
    - name: ABM_CLUSTER
      value: "default-cluster"
    - name: CLOUD_TYPE
      value: "PaaS"
    - name: ENV_TYPE
      value: "PaaS"
    - name: REGION
      value: "default-region"
    - name: IDC_MAP
      value: ""
    - name: IDC_ROOM
      value: "default-room"
    - name: ZONE
      value: "default-zone"
    - name: CENTER_REGION
      value: "default-center-region"
    - name: CICD_PROJECTID
      value: "bcc-tjv3"
    - name: CLUSTER_SCALE_FLAG
      value: ""
    - name: NETWORK_PROTOCOL
      value: "http"
    - name: COOKIE_DOMAIN
      value: "{{ env.COOKIE_DOMAIN }}"
    - name: REDIS_HOST
      value: "{{ env.APPMANAGER_REDIS_HOST }}"
    - name: REDIS_PORT
      value: "{{ env.APPMANAGER_REDIS_PORT }}"
    - name: REDIS_PASSWORD
      value: "{{ env.APPMANAGER_REDIS_PASSWORD }}"
    - name: DB_HOST
      value: "{{ env.APPMANAGER_DB_HOST }}"
    - name: DB_PORT
      value: "{{ env.APPMANAGER_DB_PORT }}"
    - name: DB_USER
      value: "{{ env.APPMANAGER_DB_USER }}"
    - name: DB_PASSWORD
      value: "{{ env.APPMANAGER_DB_PASSWORD }}"
    - name: REDIS_PASSWORD
      value: "{{ env.APPMANAGER_REDIS_PASSWORD }}"
    - name: NACOS_NAMESPACE
      value: "ad2d92c6-1a21-47ac-9da8-203fcbed9146"
    - name: NACOS_CMDB_NAMESPACE
      value: "e3789b3d-4553-4240-8ec0-a36b1bf4970b"
    - name: NACOS_DUBBO_NAMESPACE
      value: "fa4ac72e-5c21-470e-90c6-a27e77650b86"
    - name: ZOOKEEPER_ENDPOINT
      value: "appmanagerbase-zookeeper"
    - name: ZOOKEEPER_HOSTS
      value: "appmanagerbase-zookeeper"
    - name: ZOOKEEPER_PORT
      value: "2181"
    - name: REDIS_DB
      value: "0"
    - name: ACCOUNT_SUPER_ACCESS_ID
      value: "test-access-id"
    - name: ACCOUNT_SUPER_ACCESS_KEY
      value: "test-access-key"
    - name: ACCOUNT_SUPER_CLIENT_ID
      value: "common"
    - name: ACCOUNT_SUPER_CLIENT_SECRET
      value: "common-9efab2399c7c560b34de477b9aa0a465"
    - name: ACCOUNT_SUPER_ID
      value: "admin"
    - name: ACCOUNT_SUPER_PK
      value: "999999999"
    - name: ACCOUNT_SUPER_SECRET_KEY
      value: "test-super-secret-key"
    - name: DR_MULTIREGION
      value: "false"
    - name: DR_MULTIZONE
      value: "false"
    - name: ELASTICSEARCH_HOST
      value: "prod-sreworks-flycore-paas-elasticsearch"
    - name: ELASTICSEARCH_PORT
      value: "9200"
    - name: ELASTICSEARCH_USER
      value: "elastic"
    - name: ELASTICSEARCH_PASSWORD
      value: "elastic"
    - name: DUBBO_DOMAIN
      value: ""
    - name: DNS_PAAS_COMPATIBLE_HOME
      value: "{{ env.COOKIE_DOMAIN }}"
    - name: DNS_PAAS_HOME
      value: "{{ env.COOKIE_DOMAIN }}"
    - name: ENDPOINT_PAAS_MINIO
      value: "appmanager-minio"
    - name: URL_PAAS_HOME
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: VIP_IP_PAAS_HOME
      value: "prod-sreworks-flycore-paas-frontend"
    - name: VIP_IP_PAAS_TKGONE
      value: "prod-sreworks-flycore-paas-tkgone"
    - name: NGINX_CONF_HOME
      value: "nginx-conf-home"
    - name: PAAS_ABM_URL
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: REQUESTED_IP
      value: "unknown"
    - name: STARAGENT_KEY
      value: "staragent-key"
    - name: STARAGENT_SECRET
      value: "staragent-secret"
    - name: TASK_IP
      value: "1.1.1.1"
    - name: TIANJI_API_URL
      value: "tianji-api-url"
    - name: TIANJI_METRICS_API
      value: "tianji-metrics-api"
    - name: TIANJI_PORTAL_URL
      value: "tianji-portal-url"
    - name: TIANJI_SUPERKEY_ID
      value: "tianji-superkey-id"
    - name: TIANJI_SUPERKEY_IDSECRET
      value: "tianji-superkey-idsecret"
    - name: bcc_portal_domain
      value: "{{ env.COOKIE_DOMAIN }}"
    - name: bcc_web_url
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: bcc_web_web_url
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: private_bcc_web_dns
      value: "{{ env.COOKIE_DOMAIN }}"
    - name: server_name
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: private_bcc_gateway_entry
      value: "http://prod-sreworks-flycore-paas-gateway"
    - name: auth_domain
      value: "prod-sreworks-flycore-paas-authproxy"
    - name: ass_callback_url
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: ASS_CALLBACK_URL
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}"
    - name: DNS_PAAS_HOME_DISASTER
      value: "prod-sreworks-flycore-paas-frontend"
    - name: ENDPOINT_PAAS_AUTHPROXY
      value: "prod-sreworks-flycore-paas-authproxy"
    - name: ENDPOINT_PAAS_APPMANAGER
      value: "appmanager"
    - name: ENDPOINT_PAAS_NACOS
      value: "prod-sreworks-flycore-paas-nacos-0.prod-sreworks-flycore-paas-nacos:8848"
    - name: ENDPOINT_PAAS_ACTION
      value: "prod-sreworks-flycore-paas-action"
    - name: ENDPOINT_PAAS_GATEWAY
      value: "prod-sreworks-flycore-paas-gateway"
    - name: ENDPOINT_PAAS_HOME
      value: "prod-sreworks-flycore-paas-frontend"
    - name: ENDPOINT_PAAS_PRODUCTOPS
      value: "prod-sreworks-flycore-paas-productops"
    - name: ENDPOINT_PAAS_TKGONE
      value: "prod-sreworks-flycore-paas-tkgone"
    - name: URL_PAAS_GATEWAY
      value: "{{ env.NETWORK_PROTOCOL }}://{{ env.COOKIE_DOMAIN }}/gateway"
    - name: VIP_IP_PAAS_HOME
      value: "prod-sreworks-flycore-paas-frontend"
    - name: VIP_IP_PAAS_TKGONE
      value: "prod-sreworks-flycore-paas-tkgone"
    - name: VIP_PORT_PAAS_TKGONE
      value: "80"
    - name: VIP_IP_PAAS_GRAFANA
      value: "127.0.0.1"
    - name: ENDPOINT_SAAS_DWG
      value: "127.0.0.1"
    - name: EXTERNAL_DATAWORKS_CONSOLE_URL
      value: "127.0.0.1"
  components:
    - revisionName: K8S_MICROSERVICE|paas-authproxy|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 7001
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "abm_paas_authproxy"
        - name: Global.AAS_DIRECT_LOGIN_URL
          value: ""
        - name: Global.AAS_INNER_ENDPOINT
          value: ""
        - name: Global.AAS_LOGIN_URL
          value: ""
        - name: Global.AAS_LOGOUT_URL
          value: ""
        - name: Global.AAS_OPENAPI_DOMAIN
          value: ""
        - name: Global.AAS_OPENAPI_URL
          value: ""
        - name: Global.AAS_POP_KEY
          value: "unknown"
        - name: Global.AAS_POP_SECRET
          value: "unknown"
        - name: Global.ACCOUNT_BASE_ACCESS_ID
          value: ""
        - name: Global.ACCOUNT_BASE_ACCESS_KEY
          value: ""
        - name: Global.ACCOUNT_BASE_ID
          value: ""
        - name: Global.ACCOUNT_BASE_PK
          value: ""
        - name: Global.ACCOUNT_BASE_SECRET_KEY
          value: ""
        - name: Global.ACCOUNT_ODPS_ACCESS_ID
          value: ""
        - name: Global.ACCOUNT_ODPS_ACCESS_KEY
          value: ""
        - name: Global.ACCOUNT_ODPS_ID
          value: ""
        - name: Global.ACCOUNT_ODPS_PK
          value: ""
        - name: Global.ACCOUNT_ODPS_SECRET_KEY
          value: ""
        - name: Global.ASS_CALLBACK_URL
          value: ""
        - name: Global.DATABASE_AUTH_SERVICE_MGR_CLASS_NAME
          value: "com.alibaba.tesla.authproxy.service.impl.DataBaseAuthServiceManager"
        - name: Global.DATABASE_LOGIN_INTERCEPTOR_CLASS_NAME
          value: "com.alibaba.tesla.authproxy.interceptor.DataBaseLoginInterceptor"
        - name: Global.LOGIN_URL
          value: "fake-login-url"
        - name: Global.OAM_ENDPOINT
          value: ""
        - name: Global.OAM_POP_KEY
          value: ""
        - name: Global.OAM_POP_SECRET
          value: ""
        - name: Global.TESLA_ADMIN_USERS
          value: ""
        - name: Global.TESLA_AUTHPROXY_OAUTH2_ACCESS_TOKEN_URI
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_CLIENT_ID
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_CLIENT_SECRET
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_REDIRECT_URI
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_USER_AUTHORIZATION_URI
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_USER_INFO_URI
          value: "unknown"
        - name: Global.UMM_AK_ID
          value: ""
        - name: Global.UMM_AK_SECRET
          value: ""
        - name: Global.UMM_ENDPOINT
          value: ""
    - revisionName: K8S_JOB|paas-authproxy-postrun|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      parameterValues:
        - name: Global.DB_NAME
          value: "abm_paas_authproxy"
        - name: Global.AAS_DIRECT_LOGIN_URL
          value: ""
        - name: Global.AAS_INNER_ENDPOINT
          value: ""
        - name: Global.AAS_LOGIN_URL
          value: ""
        - name: Global.AAS_LOGOUT_URL
          value: ""
        - name: Global.AAS_OPENAPI_DOMAIN
          value: ""
        - name: Global.AAS_OPENAPI_URL
          value: ""
        - name: Global.AAS_POP_KEY
          value: "unknown"
        - name: Global.AAS_POP_SECRET
          value: "unknown"
        - name: Global.ACCOUNT_BASE_ACCESS_ID
          value: ""
        - name: Global.ACCOUNT_BASE_ACCESS_KEY
          value: ""
        - name: Global.ACCOUNT_BASE_ID
          value: ""
        - name: Global.ACCOUNT_BASE_PK
          value: ""
        - name: Global.ACCOUNT_BASE_SECRET_KEY
          value: ""
        - name: Global.ACCOUNT_ODPS_ACCESS_ID
          value: ""
        - name: Global.ACCOUNT_ODPS_ACCESS_KEY
          value: ""
        - name: Global.ACCOUNT_ODPS_ID
          value: ""
        - name: Global.ACCOUNT_ODPS_PK
          value: ""
        - name: Global.ACCOUNT_ODPS_SECRET_KEY
          value: ""
        - name: Global.ASS_CALLBACK_URL
          value: ""
        - name: Global.DATABASE_AUTH_SERVICE_MGR_CLASS_NAME
          value: "com.alibaba.tesla.authproxy.service.impl.DataBaseAuthServiceManager"
        - name: Global.DATABASE_LOGIN_INTERCEPTOR_CLASS_NAME
          value: "com.alibaba.tesla.authproxy.interceptor.DataBaseLoginInterceptor"
        - name: Global.LOGIN_URL
          value: "fake-login-url"
        - name: Global.OAM_ENDPOINT
          value: ""
        - name: Global.OAM_POP_KEY
          value: ""
        - name: Global.OAM_POP_SECRET
          value: ""
        - name: Global.TESLA_ADMIN_USERS
          value: ""
        - name: Global.TESLA_AUTHPROXY_OAUTH2_ACCESS_TOKEN_URI
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_CLIENT_ID
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_CLIENT_SECRET
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_REDIRECT_URI
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_USER_AUTHORIZATION_URI
          value: "unknown"
        - name: Global.TESLA_AUTHPROXY_OAUTH2_USER_INFO_URI
          value: "unknown"
        - name: Global.UMM_AK_ID
          value: ""
        - name: Global.UMM_AK_SECRET
          value: ""
        - name: Global.UMM_ENDPOINT
          value: ""
      dependencies:
        - component: K8S_MICROSERVICE|paas-authproxy
    - revisionName: K8S_MICROSERVICE|paas-gateway|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 7001
              - protocol: TCP
                port: 7002
                targetPort: 7002 
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "abm_paas_gateway"
        - name: Global.AUTH_ADMIN_TOKEN
          value: "5e6b47fa-f905-4d1f-b1a9-3d8e06065426"
        - name: Global.AUTH_COOKIE_KEY
          value: ""
        - name: Global.AUTH_COOKIE_NAME
          value: ""
        - name: Global.AUTH_JWT_SECRET
          value: "5e6b47fa-f905-4d1f-b1a9-3d8e06065426"
        - name: Global.TESLA_AUTH_APP
          value: ""
        - name: Global.TESLA_AUTH_KEY
          value: ""
        - name: Global.STORE_NACOS_DATA_ID
          value: "abm-paas-gateway.route.config_default_default"
        - name: Global.STORE_NACOS_GROUP
          value: "DEFAULT_GROUP"
        - name: Global.AUTH_ADMIN_TOKEN
          value: "5e6b47fa-f905-4d1f-b1a9-3d8e06065426"
        - name: Global.NACOS_ENDPOINT
          value: "prod-sreworks-flycore-paas-nacos:8848"
        - name: Global.MANAGER_SERVER_PORT
          value: "7002"
      #dependencies:
    - revisionName: K8S_MICROSERVICE|paas-frontend|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 80
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
      dependencies:
        - component: K8S_MICROSERVICE|paas-gateway
    - revisionName: K8S_MICROSERVICE|paas-tkgone|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 7001
      parameterValues:
        - name: KIND
          value: StatefulSet
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 2
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "abm_paas_tkgone"
        - name: Global.CONTAINER_IP_LIST_TKGONE
          value: "prod-sreworks-flycore-paas-tkgone-0.prod-sreworks-flycore-paas-tkgone,prod-sreworks-flycore-paas-tkgone-1.prod-sreworks-flycore-paas-tkgone"
        - name: Global.CONTAINER_IP_LIST
          value: "prod-sreworks-flycore-paas-tkgone-0.prod-sreworks-flycore-paas-tkgone,prod-sreworks-flycore-paas-tkgone-1.prod-sreworks-flycore-paas-tkgone"
        - name: Global.ELASTICSEARCH_HOST
          value: "prod-sreworks-flycore-paas-elasticsearch"
        - name: Global.ELASTICSEARCH_PORT
          value: "9200"
        - name: Global.NACOS_ENDPOINT
          value: "prod-sreworks-flycore-paas-nacos:8848" 
      #dependencies:
      #  - component: K8S_MICROSERVICE|paas-elasticsearch
    - revisionName: K8S_JOB|paas-tkgone-postrun|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      parameterValues:
        - name: Global.DB_NAME
          value: "abm_paas_tkgone"
      dependencies:
        - component: K8S_MICROSERVICE|paas-tkgone
    - revisionName: K8S_MICROSERVICE|paas-productops|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 80
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "abm_paas_productops"
      dependencies:
        - component: K8S_JOB|paas-tkgone-postrun
    - revisionName: K8S_JOB|paas-productops-postrun|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      parameterValues:
        - name: Global.DB_NAME
          value: "abm_paas_productops"
      dependencies:
        - component: K8S_MICROSERVICE|paas-productops
    - revisionName: K8S_MICROSERVICE|paas-nacos|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            clusterIP: None
            ports:
              - protocol: TCP
                port: 8848
                targetPort: 8848
      parameterValues:
        - name: KIND
          value: StatefulSet
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "abm_paas_nacos"
      dependencies:
    - revisionName: K8S_JOB|paas-gateway-route-config|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      parameterValues:
      dependencies:
        - component: K8S_MICROSERVICE|paas-gateway
    - revisionName: K8S_MICROSERVICE|paas-action|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "abm_paas_action"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 7001
