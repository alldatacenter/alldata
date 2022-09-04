componentType: HELM
componentName: logstash
options:
  repoPath: saas/dataops/api/logstash/logstash-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

