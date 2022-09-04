apiVersion: core.oam.dev/v1alpha2
kind: Component
metadata:
  name: microservice-{{ appId }}-{{ componentName }}
  annotations:
    annotations.appmanager.oam.dev/version: "{{ version }}"
  labels:
    labels.appmanager.oam.dev/appId: "{{ appId }}"
    labels.appmanager.oam.dev/componentName: "{{ componentName }}"
    labels.appmanager.oam.dev/stageId: "PLACEHOLDER_STAGE_ID"
    labels.appmanager.oam.dev/clusterId: "PLACEHOLDER_CLUSTER_ID"
    appId: "{{ appId }}"
    componentName: "{{ componentName }}"
    stageId: "PLACEHOLDER_STAGE_ID"
spec:
{%- if imageTarList %}
  images:
{%- for imageTar in imageTarList %}
  - name: "{{ imageTar.name }}"
    arch: "{{ imageTar.arch }}"
    image: "{{ imageTar.image }}"
    sha256: "{{ imageTar.sha256 }}"
{%- endfor %}
{%- endif %}
  workload:
    apiVersion: apps.abm.io/v1
    kind: Microservice
    metadata:
      namespace: "PLACEHOLDER_NAMESPACE_ID"
      name: "PLACEHOLDER_NAME"
      labels:
        labels.appmanager.oam.dev/stageId: "PLACEHOLDER_STAGE_ID"
        labels.appmanager.oam.dev/appId: "{{ appId }}"
        labels.appmanager.oam.dev/componentName: "{{ componentName }}"
        labels.appmanager.oam.dev/clusterId: "PLACEHOLDER_CLUSTER_ID"
        labels.appmanager.oam.dev/appInstanceId: "PLACEHOLDER_APP_INSTANCE_ID"
        labels.appmanager.oam.dev/appInstanceName: "PLACEHOLDER_APP_INSTANCE_NAME"
        labels.appmanager.oam.dev/componentInstanceId: "PLACEHOLDER_COMPONENT_INSTANCE_ID"
        stageId: "PLACEHOLDER_STAGE_ID"
        appId: "{{ appId }}"
        componentName: "{{ componentName }}"
      annotations:
        annotations.appmanager.oam.dev/deployAppId: "PLACEHOLDER_DEPLOY_APP_ID"
        annotations.appmanager.oam.dev/deployComponentId: "PLACEHOLDER_DEPLOY_COMPONENT_ID"
        annotations.appmanager.oam.dev/version: "{{ version }}"
        annotations.appmanager.oam.dev/appInstanceId: "PLACEHOLDER_APP_INSTANCE_ID"
        annotations.appmanager.oam.dev/appInstanceName: "PLACEHOLDER_APP_INSTANCE_NAME"
        annotations.appmanager.oam.dev/componentInstanceId: "PLACEHOLDER_COMPONENT_INSTANCE_ID"
    spec:
      repoUrl: "{{ repoUrl }}"
      chartName: "{{ chartName }}"
      chartVersion: "{{ chartVersion }}"
      repoPath: "{{ repoPath }}"
      name: ""
      values: {}