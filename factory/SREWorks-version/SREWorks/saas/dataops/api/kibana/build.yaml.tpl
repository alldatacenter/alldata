componentType: HELM
componentName: kibana
options:
  repoPath: saas/dataops/api/kibana/kibana-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

