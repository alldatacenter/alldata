componentType: HELM
componentName: metricbeat
options:
  repoPath: saas/dataops/api/metricbeat/metricbeat-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

