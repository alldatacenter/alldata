componentType: K8S_MICROSERVICE
componentName: plugin-aliyun-cluster
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: plugin-clustermanage-cluster-aliyun
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE11_IMAGE: ${JRE11_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        repoPath: saas/system/api/plugin-clustermanage-cluster-aliyun
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  env:
---
#componentType: K8S_MICROSERVICE
#componentName: plugin-aliyun-rds
#options:
#  containers:
#    - ports:
#        - containerPort: 7001
#      name: server
#      build:
#        imagePush: ${IMAGE_BUILD_ENABLE}
#        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
#        args:
#          TAG: plugin-clustermanage-resource-aliyun-rds
#        dockerfileTemplateArgs:
#          JRE11_IMAGE: registry.cn-hangzhou.aliyuncs.com/alisre/openjdk:11.0.10-jre
#        dockerfileTemplate: Dockerfile
#        repoPath: saas/system/api/plugin-clustermanage-resource-aliyun-rds
#        branch: ${SOURCE_BRANCH}
#        repo: ${SOURCE_REPO}
#        ciAccount: ${SOURCE_CI_ACCOUNT}
#        ciToken: ${SOURCE_CI_TOKEN}
#
#  env:
---
componentType: K8S_MICROSERVICE
componentName: plugin-account-aliyun
options:
  containers:
    - ports:
        - containerPort: 7001
      name: server
      build:
        imagePush: ${IMAGE_BUILD_ENABLE}
        imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
        args:
          TAG: plugin-teammanage-account-aliyun
        dockerfileTemplateArgs:
          MAVEN_IMAGE: ${MAVEN_IMAGE}
          JRE11_IMAGE: ${JRE11_IMAGE}
          MAVEN_SETTINGS_XML: ${MAVEN_SETTINGS_XML}
        dockerfileTemplate: Dockerfile.tpl
        repoPath: saas/system/api/plugin-teammanage-account-aliyun
        branch: ${SOURCE_BRANCH}
        repo: ${SOURCE_REPO}
        ciAccount: ${SOURCE_CI_ACCOUNT}
        ciToken: ${SOURCE_CI_TOKEN}

  env:
---

componentType: K8S_JOB
componentName: resource-upload
options:
  job:
    name: job
    build:
      imagePush: ${IMAGE_BUILD_ENABLE}
      imagePushRegistry: ${IMAGE_PUSH_REGISTRY}
      dockerfileTemplateArgs:
        APK_REPO_DOMAIN: ${APK_REPO_DOMAIN}
        MINIO_CLIENT_URL: ${MINIO_CLIENT_URL}
        PYTHON3_IMAGE: ${PYTHON3_IMAGE}
      dockerfileTemplate: Dockerfile.tpl
      repoPath: saas/system/api/resource
      branch: ${SOURCE_BRANCH}
      repo: ${SOURCE_REPO}
      ciAccount: ${SOURCE_CI_ACCOUNT}
      ciToken: ${SOURCE_CI_TOKEN}
 
  env:
    - APPMANAGER_PACKAGE_ENDPOINT_PROTOCOL
    - APPMANAGER_PACKAGE_ENDPOINT
    - APPMANAGER_PACKAGE_ACCESS_KEY
    - APPMANAGER_PACKAGE_SECRET_KEY



