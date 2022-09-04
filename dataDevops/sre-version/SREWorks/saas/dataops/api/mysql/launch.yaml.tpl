apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: "master"
    - name: NAMESPACE_ID
      value: "${NAMESPACE_ID}"
    - name: STAGE_ID
      value: "${SAAS_STAGE_ID}"
    - name: ABM_CLUSTER
      value: "default-cluster"
    - name: CLOUD_TYPE
      value: "PaaS"
    - name: ENV_TYPE
      value: "PaaS"
  components:
  - dataOutputs: []
    revisionName: "HELM|mysql|_"
    traits: []
    dataInputs: []
    scopes:
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Namespace"
        name: "${NAMESPACE_ID}"
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Cluster"
        name: "master"
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Stage"
        name: "${SAAS_STAGE_ID}"
    dependencies: []
    parameterValues:
    - name: "values"
      value:
          global:
            storageClass: "${GLOBAL_STORAGE_CLASS}"

          primary:
            service:
              type: ClusterIP
            persistence:
              size: 50Gi
            extraFlags: "--max-connect-errors=1000 --max_connections=10000"

            configuration: |-
              [mysqld]
              default_authentication_plugin=mysql_native_password
              skip-name-resolve
              explicit_defaults_for_timestamp
              basedir=/opt/bitnami/mysql
              plugin_dir=/opt/bitnami/mysql/plugin
              port=3306
              socket=/opt/bitnami/mysql/tmp/mysql.sock
              datadir=/bitnami/mysql/data
              tmpdir=/opt/bitnami/mysql/tmp
              max_allowed_packet=16M
              bind-address=0.0.0.0
              pid-file=/opt/bitnami/mysql/tmp/mysqld.pid
              log-error=/opt/bitnami/mysql/logs/mysqld.log
              character-set-server=UTF8
              collation-server=utf8_general_ci
              expire_logs_days=3

              [client]
              port=3306
              socket=/opt/bitnami/mysql/tmp/mysql.sock
              default-character-set=UTF8
              plugin_dir=/opt/bitnami/mysql/plugin

              [manager]
              port=3306
              socket=/opt/bitnami/mysql/tmp/mysql.sock
              pid-file=/opt/bitnami/mysql/tmp/mysqld.pid


          replication:
            enabled: false

          image:
            registry: "${MYSQL_REGISTRY}"
            repository: "${MYSQL_REPO}"
            tag: "${MYSQL_IMAGE_TAG}"

          auth:
            rootPassword: ${DATAOPS_DB_PASSWORD}







      toFieldPaths:
      - "spec.values"

 

