componentType: HELM
componentName: grafana
options:
  repoPath: saas/dataops/api/grafana/grafana-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

