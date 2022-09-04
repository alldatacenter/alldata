
componentType: K8S_MICROSERVICE
componentName: aisp
options:
  containers:
    - name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          JRE11_IMAGE: ${JRE11_IMAGE}
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile_sreworks.tpl
        repoPath: saas/aiops/api/aisp
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
        dockerfileTemplate: Dockerfile-sreworks-migration.tpl
        repoPath: saas/aiops/api/aisp
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
    - CACHE_TYPE
    - ACCOUNT_SUPER_CLIENT_ID
    - ACCOUNT_SUPER_CLIENT_SECRET
    - ACCOUNT_SUPER_ACCESS_ID
    - ACCOUNT_SUPER_SECRET_KEY
    - ACCOUNT_SUPER_ACCESS_KEY

---

componentType: K8S_MICROSERVICE
componentName: anomalydetection
options:
  containers:
    - name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          BENTOML_IMAGE: ${BENTOML_IMAGE}
          PYTHON_PIP: ${PYTHON_PIP}
          PYTHON_PIP_DOMAIN: ${PYTHON_PIP_DOMAIN}
        dockerfileTemplate: Dockerfile
        repoPath: saas/aiops/api/anomalydetection
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

---

componentType: K8S_MICROSERVICE
componentName: processstrategy
options:
  containers:
    - name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        dockerfileTemplateArgs:
          JRE11_IMAGE: ${JRE11_IMAGE}
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML} 
        dockerfileTemplate: Dockerfile.tpl
        repoPath: saas/aiops/api/aisp-process-strategy
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  env:
    - TASKPLATFORM_SUBMIT_URL
    - TASKPLATFORM_QUERY_URL
    - AISP_URL






