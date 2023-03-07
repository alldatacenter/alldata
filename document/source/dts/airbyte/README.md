<p align="center">
  <a href="https://airbyte.com"><img src="https://assets.website-files.com/605e01bc25f7e19a82e74788/624d9c4a375a55100be6b257_Airbyte_logo_color_dark.svg" alt="Airbyte"></a>
</p>
<p align="center">
    <em>Data integration platform for ELT pipelines from APIs, databases & files to databases, warehouses & lakes</em>
</p>
<p align="center">
<a href="https://github.com/airbytehq/airbyte/stargazers/" target="_blank">
    <img src="https://img.shields.io/github/stars/airbytehq/airbyte?style=social&label=Star&maxAge=2592000" alt="Test">
</a>
<a href="https://github.com/airbytehq/airbyte/releases" target="_blank">
    <img src="https://img.shields.io/github/v/release/airbytehq/airbyte?color=white" alt="Release">
</a>
<a href="https://airbytehq.slack.com/" target="_blank">
    <img src="https://img.shields.io/badge/slack-join-white.svg?logo=slack" alt="Slack">
</a>
<a href="https://www.youtube.com/c/AirbyteHQ/?sub_confirmation=1" target="_blank">
    <img alt="YouTube Channel Views" src="https://img.shields.io/youtube/channel/views/UCQ_JWEFzs1_INqdhIO3kmrw?style=social">
</a>
<a href="https://github.com/airbytehq/airbyte/actions/workflows/gradle.yml" target="_blank">
    <img src="https://img.shields.io/github/actions/workflow/status/airbytehq/airbyte/gradle.yml?branch=master" alt="Build">
</a>
<a href="https://github.com/airbytehq/airbyte/tree/master/docs/project-overview/licenses" target="_blank">
    <img src="https://img.shields.io/static/v1?label=license&message=MIT&color=white" alt="License">
</a>
<a href="https://github.com/airbytehq/airbyte/tree/master/docs/project-overview/licenses" target="_blank">
    <img src="https://img.shields.io/static/v1?label=license&message=ELv2&color=white" alt="License">
</a>
</p>

We believe that only an **open-source** solution to data movement can cover the **long tail of data sources** while empowering data engineers to **customize existing connectors**. Our ultimate vision is to help you move data from any source to any destination. Airbyte already provides [300+ connectors](https://docs.airbyte.com/integrations/) for popular APIs, databases, data warehouses and data lakes.

Airbyte connectors can be implemented in any language and take the form of a Docker image that follows the [Airbyte specification](https://docs.airbyte.com/understanding-airbyte/airbyte-protocol/). You can create new connectors very fast with:
 - The [low-code Connector Development Kit](https://docs.airbyte.com/connector-development/config-based/low-code-cdk-overview) (CDK) for API connectors ([demo](https://www.youtube.com/watch?v=i7VSL2bDvmw))
 - The [Python CDK](https://docs.airbyte.com/connector-development/cdk-python/) ([tutorial](https://docs.airbyte.com/connector-development/tutorials/cdk-speedrun))

Airbyte has a built-in scheduler and uses [Temporal](https://airbyte.com/blog/scale-workflow-orchestration-with-temporal) to orchestrate jobs and ensure reliability at scale. Airbyte leverages [dbt](https://www.youtube.com/watch?v=saXwh6SpeHA) to normalize extracted data and can trigger custom transformations in SQL and dbt. You can also orchestrate Airbyte syncs with [Airflow](https://docs.airbyte.com/operator-guides/using-the-airflow-airbyte-operator), [Prefect](https://docs.airbyte.com/operator-guides/using-prefect-task) or [Dagster](https://docs.airbyte.com/operator-guides/using-dagster-integration).

![Airbyte OSS Connections UI](https://user-images.githubusercontent.com/2302748/205949986-5207ca24-f1f0-41b1-97e1-a0745a0de55a.png)

Explore our [demo app](https://demo.airbyte.io/).

## Quick start

### Run Airbyte locally

You can run Airbyte locally with Docker. The shell script below will retrieve the requisite docker files from the [platform repository](https://github.com/airbytehq/airbyte-platform) and run docker compose for you.

```bash
git clone --depth 1 https://github.com/airbytehq/airbyte.git
cd airbyte
./run-ab-platform.sh
```

Login to the web app at [http://localhost:8000](http://localhost:8000) by entering the default credentials found in your .env file.

```
BASIC_AUTH_USERNAME=airbyte
BASIC_AUTH_PASSWORD=password
```

Follow web app UI instructions to set up a source, destination and connection to replicate data. Connections support the most popular sync modes: full refresh, incremental and change data capture for databases.

Read the [Airbyte docs](https://docs.airbyte.com).

### Manage Airbyte configurations with code

You can also programmatically manage sources, destinations, and connections with YAML files, [Octavia CLI](https://github.com/airbytehq/airbyte/tree/master/octavia-cli), and API.

### Deploy Airbyte to production

Deployment options: [Docker](https://docs.airbyte.com/deploying-airbyte/local-deployment), [AWS EC2](https://docs.airbyte.com/deploying-airbyte/on-aws-ec2), [Azure](https://docs.airbyte.com/deploying-airbyte/on-azure-vm-cloud-shell), [GCP](https://docs.airbyte.com/deploying-airbyte/on-gcp-compute-engine), [Kubernetes](https://docs.airbyte.com/deploying-airbyte/on-kubernetes), [Restack](https://docs.airbyte.com/deploying-airbyte/on-restack), [Plural](https://docs.airbyte.com/deploying-airbyte/on-plural), [Oracle Cloud](https://docs.airbyte.com/deploying-airbyte/on-oci-vm), [Digital Ocean](https://docs.airbyte.com/deploying-airbyte/on-digitalocean-droplet)...


## License

See the [LICENSE](docs/project-overview/licenses/) file for licensing information, and our [FAQ](docs/project-overview/licenses/license-faq.md) for any questions you may have on that topic.


## 官方项目地址
https://github.com/airbytehq/airbyte