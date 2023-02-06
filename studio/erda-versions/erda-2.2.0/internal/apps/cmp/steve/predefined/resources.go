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

package predefined

var (
	ServiceAccountExpression = `
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: erda-org-manager
  namespace: {{.}}
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: erda-org-ops
  namespace: {{.}}
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: erda-org-support
  namespace: {{.}}
`
	ClusterRoleExpression = `
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: erda-pod-exec
rules:
- apiGroups:
  - ""
  resources:
  - 'pods/exec'
  verbs:
  - '*'
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: erda-readonly
rules:
- apiGroups:
  - "*"
  resources:
  - '*'
  verbs:
  - get
  - list
  - watch
`
	ClusterRoleBindingExpression = `
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: erda-readonly
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: erda-readonly
subjects:
- kind: Group
  name: erda-org-support
- kind: ServiceAccount
  name: erda-org-support
  namespace: {{.}}
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: erda-pod-exec
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: erda-pod-exec
subjects:
- kind: Group
  name: erda-org-support
- kind: ServiceAccount
  name: erda-org-support
  namespace: {{.}}
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: erda-admin
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: Group
  name: erda-org-manager
- kind: ServiceAccount
  name: erda-org-manager
  namespace: {{.}}
- kind: Group
  name: erda-org-ops
- kind: ServiceAccount
  name: erda-org-ops
  namespace: {{.}}
`
)
