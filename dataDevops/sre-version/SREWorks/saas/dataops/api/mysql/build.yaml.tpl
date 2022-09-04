componentType: HELM
componentName: mysql
options:
  repoPath: saas/dataops/api/mysql/mysql-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

