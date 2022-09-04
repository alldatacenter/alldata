componentType: HELM
componentName: elasticsearch
options:
  repoPath: saas/dataops/api/elasticsearch/elasticsearch-chart
  branch: ${SOURCE_BRANCH}
  repo: ${SOURCE_REPO}
  ciAccount: "${SOURCE_CI_ACCOUNT}"
  ciToken: "${SOURCE_CI_TOKEN}"

