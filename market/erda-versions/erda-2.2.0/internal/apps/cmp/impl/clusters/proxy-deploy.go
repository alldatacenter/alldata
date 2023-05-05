// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package clusters

// TODO: instead template conf load

var ProxyDeployTemplate = `
---
apiVersion: v1
kind: Namespace
metadata:
  name: {{ .ErdaNamespace }}
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cluster-agent
  namespace: {{ .ErdaNamespace }}
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-agent-cr
rules:
- apiGroups:
  - '*'
  resources:
  - '*'
  verbs:
  - '*'
- nonResourceURLs:
  - '*'
  verbs:
  - '*'
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-agent-crb
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-agent-cr
subjects:
- kind: ServiceAccount
  name: cluster-agent
  namespace: {{ .ErdaNamespace }}
---
apiVersion: batch/v1
kind: Job
metadata:
  labels:
    job-name: erda-cluster-init
  name: erda-cluster-init
  namespace: {{ .ErdaNamespace }}
spec:
  backoffLimit: 0
  selector:
    matchLabels:
      job-name: erda-cluster-init
  template:
    metadata:
      labels:
        job-name: erda-cluster-init
    spec:
      serviceAccountName: cluster-agent
      restartPolicy: Never
      containers:
        - name: init
          env:
            {{- range .Envs }}
            - name: {{ .Name }}
              value: "{{ .Value }}"
            {{- end }}
          command:
            - sh
            - -c
            - /erda/cmd/cluster-ops/bin
          image: {{ .JobImage }}
          imagePullPolicy: Always
`
