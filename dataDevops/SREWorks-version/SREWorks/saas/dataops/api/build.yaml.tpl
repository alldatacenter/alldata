componentType: K8S_MICROSERVICE
componentName: dataset
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        repoPath: saas/dataops/api/dataset
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  initContainers:
    - name: db-migration
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile-db-migration.tpl
        repoPath: saas/dataops/api/dataset
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}
    - name: db-migration-datasource
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile-db-migration-datasource.tpl
        repoPath: saas/dataops/api/dataset
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}



  env:
    - DATA_DB_HOST
    - DATA_DB_PORT
    - DATA_DB_USER
    - DATA_DB_PASSWORD
    - DATA_DB_DATASET_NAME
    - DATA_DB_PMDB_NAME
    - DATA_DB_DATASOURCE_NAME
    - DATA_SKYW_HOST
    - DATA_SKYW_PORT
    - DATA_SKYW_ENABLE


---


componentType: K8S_JOB
componentName: dataset-postrun
options:
  job:
    name: init-job
    build:
      imagePush: ${IMAGE_BUILD_ENABLE}
      imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
      dockerfileTemplateArgs:
        POSTRUN_IMAGE: ${POSTRUN_IMAGE}
      dockerfileTemplate: Dockerfile-postrun.tpl
      repoPath: saas/dataops/api/dataset
      branch: ${SOURCE_BRANCH}
      repo: ${SOURCE_REPO}
      ciAccount: ${SOURCE_CI_ACCOUNT}
      ciToken: ${SOURCE_CI_TOKEN}

  env:
    - MINIO_ENDPOINT
    - MINIO_ACCESS_KEY
    - MINIO_SECRET_KEY
    - DATA_ES_USER
    - DATA_ES_PASSWORD


---



componentType: K8S_MICROSERVICE
componentName: pmdb
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        repoPath: saas/dataops/api/pmdb
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  initContainers:
    - name: db-migration
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile_db_migration.tpl
        repoPath: saas/dataops/api/pmdb
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
    - DATA_DB_HOST
    - DATA_DB_PORT
    - DATA_DB_USER
    - DATA_DB_PASSWORD
    - DATA_DB_PMDB_NAME
    - DATA_SKYW_HOST
    - DATA_SKYW_PORT
    - DATA_SKYW_ENABLE
    - KAFKA_ENDPOINT
    - DATA_ES_HOST
    - DATA_ES_PORT
    - DATA_ES_USER
    - DATA_ES_PASSWORD


---

componentType: K8S_MICROSERVICE
componentName: warehouse
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE8_IMAGE: ${JRE8_IMAGE}
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        repoPath: saas/dataops/api/warehouse
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
        dockerfileTemplate: Dockerfile-db-migration.tpl
        repoPath: saas/dataops/api/warehouse
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  env:
    - DATA_DB_HOST
    - DATA_DB_PORT
    - DATA_DB_USER
    - DATA_DB_PASSWORD
    - DATA_DB_WAREHOUSE_NAME
    - DATA_ES_HOST
    - DATA_ES_PORT
    - DATA_ES_USER
    - DATA_ES_PASSWORD


---

componentType: K8S_JOB
componentName: metric-flink-init
options:
  job:
    name: init-job
    build:
      imagePush: ${IMAGE_BUILD_ENABLE}
      imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
      args:
        TAG: ack
      dockerfileTemplateArgs:
        MAVEN_IMAGE: ${MAVEN_IMAGE}
        ALPINE_IMAGE: ${ALPINE_IMAGE}
        APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
        MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        MINIO_CLIENT_URL: ${MINIO_CLIENT_URL}
        PYTHON_PIP: ${PYTHON_PIP}
        PYTHON_PIP_DOMAIN: ${PYTHON_PIP_DOMAIN}
      dockerfileTemplate: Dockerfile.tpl
      repoPath: saas/dataops/api/metric-flink
      branch: ${SOURCE_BRANCH}
      repo: ${SOURCE_REPO}
      ciAccount: ${SOURCE_CI_ACCOUNT}
      ciToken: ${SOURCE_CI_TOKEN}

  env:
    - DATA_DB_HOST
    - DATA_DB_PORT
    - DATA_DB_USER
    - DATA_DB_PASSWORD
    - DATA_DB_PMDB_NAME
    - DATA_DB_HEALTH_NAME
    - DATA_ES_HOST
    - DATA_ES_PORT
    - DATA_ES_PASSWORD
    - DATA_ES_USER
    - MINIO_ENDPOINT
    - MINIO_ACCESS_KEY
    - MINIO_SECRET_KEY
    - VVP_ENDPOINT
    - KAFKA_URL
    - ES_URL
    - VVP_ENDPOINT
    - HEALTH_ENDPOINT






