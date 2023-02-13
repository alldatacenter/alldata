apiVersion: core.oam.dev/v1alpha2
kind: Component
metadata:
  name: resource-addon-{{ appId }}-{{ componentName }}
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
  workload:
    apiVersion: apps.abm.io/v1
    kind: {{ componentName|title }}Config
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
      annotations:
        annotations.appmanager.oam.dev/deployAppId: "PLACEHOLDER_DEPLOY_APP_ID"
        annotations.appmanager.oam.dev/deployComponentId: "PLACEHOLDER_DEPLOY_COMPONENT_ID"
        annotations.appmanager.oam.dev/version: "{{ version }}"
        annotations.appmanager.oam.dev/appInstanceId: "PLACEHOLDER_APP_INSTANCE_ID"
        annotations.appmanager.oam.dev/appInstanceName: "PLACEHOLDER_APP_INSTANCE_NAME"
        annotations.appmanager.oam.dev/componentInstanceId: "PLACEHOLDER_COMPONENT_INSTANCE_ID"
    spec: {}
