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

package apistructs

import (
	corev1 "k8s.io/api/core/v1"
)

const (
	AliyunRegistry                 = "aliyun-registry"
	ENABLE_SPECIFIED_K8S_NAMESPACE = "ENABLE_SPECIFIED_K8S_NAMESPACE"
)

// TaskInspect inspect k8s job, like kubectl describe
// contains task object and events
type TaskInspect struct {
	Object interface{}       `json:"object"`
	Events *corev1.EventList `json:"events"`
	Desc   string            `json:"desc"`
}
