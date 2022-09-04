kind: DeploymentTarget
apiVersion: v1
metadata:
  name: sreworksDeploymentTarget
  namespace: ${VVP_WORK_NS}
  labels:
    buildIn: true
    app: sreworks
  annotations:
    app: sreworks
spec:
  kubernetes:
    namespace: sreworks-dataops
