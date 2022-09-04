componentType: K8S_MICROSERVICE
componentName: job-master
options:
  containers:
    - ports:
        - containerPort: 17001
      name: master
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          JRE11_IMAGE: ${JRE11_IMAGE}
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: master-Dockerfile.tpl
        repoPath: saas/job/api/sreworks-job
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: "${SOURCE_CI_ACCOUNT}"
        ciToken: "${SOURCE_CI_TOKEN}"

  initContainers:
    - name: db-migration
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          MIGRATE_IMAGE: ${MIGRATE_IMAGE}
        dockerfileTemplate: Dockerfile-db-migration.tpl
        repoPath: saas/job/api/sreworks-job
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: "${SOURCE_CI_ACCOUNT}"
        ciToken: "${SOURCE_CI_TOKEN}"
    - name: init
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          POSTRUN_IMAGE: ${POSTRUN_IMAGE}
        dockerfileTemplate: Dockerfile-init.tpl
        repoPath: saas/job/api/sreworks-job
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: "${SOURCE_CI_ACCOUNT}"
        ciToken: "${SOURCE_CI_TOKEN}"

  env:
    - DB_HOST
    - DB_PORT
    - DB_USER
    - DB_PASSWORD
    - DB_NAME
    - REDIS_HOST
    - REDIS_PORT
    - REDIS_DATABASE
    - REDIS_PASSWORD
    - ES_ENDPOINT
    - ES_USERNAME
    - ES_PASSWORD

---

componentType: K8S_MICROSERVICE
componentName: job-worker
options:
  containers:
    - ports:
        - containerPort: 27001
      name: master
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
          JRE11_ALPINE_IMAGE: ${JRE11_ALPINE_IMAGE}
          PYTHON_PIP: ${PYTHON_PIP}
          PYTHON_PIP_DOMAIN: ${PYTHON_PIP_DOMAIN}
        dockerfileTemplate: worker-Dockerfile.tpl
        repoPath: saas/job/api/sreworks-job
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: "${SOURCE_CI_ACCOUNT}"
        ciToken: "${SOURCE_CI_TOKEN}"

  env:
    - SREWORKS_JOB_MASTER_ENDPOINT
    - ES_ENDPOINT
    - ES_USERNAME
    - ES_PASSWORD


