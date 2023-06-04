## GitHub Workflows

This directory contains all InLong CI checks.

- [![InLong Build](https://github.com/apache/inlong/actions/workflows/ci_build.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_build.yml)

  Build InLong when pushing changes or opening a pull request.

  If it passes, you can download the InLong binary package from the workflow run's URL.

- [![InLong Unit Test](https://github.com/apache/inlong/actions/workflows/ci_ut.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_ut.yml)

  Unit testing when pushing changes or opening a pull request.

  If it fails, you can check out reports form the workflow run's URL.

- [![InLong Docker Build and Push](https://github.com/apache/inlong/actions/workflows/ci_docker.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_docker.yml)

  Build Docker images when pushing changes or opening a pull request.

  Only when pushing changes to the `apache/inlong` repository will the Docker images are pushed to [Docker Hub](https://hub.docker.com/u/inlong).

- [![InLong Helm Charts Lint and Test](https://github.com/apache/inlong/actions/workflows/ci_chart_test.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_chart_test.yml)

  Lint and test the InLong Helm Chart when pushing changes or opening a pull request.

  In this workflow, firstly, install [Helm](https://helm.sh) using the [azure/setup-helm](https://github.com/Azure/setup-helm) action,
  then install [chart-testing](https://github.com/helm/chart-testing) using the [helm/chart-testing-action](https://github.com/helm/chart-testing-action)
  and install [Kind](https://github.com/kubernetes-sigs/kind) using the [helm/kind-action](https://github.com/helm/kind-action),
  finally, use the chart-testing tool to lint and install the InLong Helm Chart.
  And here is the [chart-testing configuration](../ct.yml) and here is the [kind configuration](../kind.yml).

  > NOTE: If the charts have not changed, they will not be linted, validated, installed and tested.

- [![InLong Check License Header](https://github.com/apache/inlong/actions/workflows/ci_check_license.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_check_license.yml)

  Check licence header when pushing changes or opening a pull request using the [apache/skywalking-eyes](https://github.com/apache/skywalking-eyes) action.
  And here is the [configuration](../../.licenserc.yaml).

- [![InLong Pull Request Labeler](https://github.com/apache/inlong/actions/workflows/ci_labeler.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_labeler.yml)

  label new pull requests based on the paths of files being changed using the [actions/labeler](https://github.com/actions/labeler) action.
  And here is the [labeler configuration](../labeler.yml)

- [![InLong Mark Stale Issues and PRs](https://github.com/apache/inlong/actions/workflows/ci_stale.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_stale.yml)

  Mark issues and pull requests that have not had recent interaction using the [actions/stale](https://github.com/actions/stale) action.

- [![InLong Greeting](https://github.com/apache/inlong/actions/workflows/ci_greeting.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_greeting.yml)

  Interact with newcomers using the [actions/first-interaction](https://github.com/actions/first-interaction) action.

- [![InLong Check Pull Request Title](https://github.com/apache/inlong/actions/workflows/ci_check_pr_title.yml/badge.svg)](https://github.com/apache/inlong/actions/workflows/ci_check_pr_title.yml)

  Check pull request title.

  Title Example: `[INLONG-XYZ][Component] Title of the pull request`
  
  > - **XYZ** should be replaced by the actual [GitHub Issue](https://github.com/apache/inlong/issues) number, e.g. `[INLONG-123]`
  >
  > - **Component** should be replaced by the InLong component name, e.g. `[INLONG-123][Manager]`

### Troubleshooting

If you have any questions, welcome to contact the maintainers. And feel free to make a [pull request](https://github.com/apache/inlong/compare)!

### Maintainers

- [dockerzhang](https://github.com/dockerzhang)
- [shink](https://github.com/shink)
