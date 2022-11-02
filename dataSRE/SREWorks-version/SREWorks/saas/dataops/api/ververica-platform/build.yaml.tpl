componentType: HELM
componentName: ververica-platform
options:
  repoPath: saas/dataops/api/ververica-platform/ververica-platform-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

