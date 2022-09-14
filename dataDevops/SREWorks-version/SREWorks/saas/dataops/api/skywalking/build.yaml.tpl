componentType: HELM
componentName: skywalking
options:
  repoPath: saas/dataops/api/skywalking/skywalking-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

