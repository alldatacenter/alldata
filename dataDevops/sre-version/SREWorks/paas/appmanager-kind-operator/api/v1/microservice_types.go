/*


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1

import (
	kruiseapps "github.com/openkruise/kruise-api/apps/v1alpha1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// AppManager 微服务类型
type MicroServiceKind string

const (
	KIND_DEPLOYMENT          MicroServiceKind = "Deployment"
	KIND_STATEFULSET         MicroServiceKind = "StatefulSet"
	KIND_CLONESET            MicroServiceKind = "CloneSet"
	KIND_ADVANCEDSTATEFULSET MicroServiceKind = "AdvancedStatefulSet"
)

// MicroServiceSpec defines the desired state of MicroService
type MicroserviceSpec struct {

	// 是否已经完成初始化 (内部使用)
	Initialized bool `json:"initialized,omitempty" hash:"ignore"`

	// 微服务类型
	Kind MicroServiceKind `json:"kind,omitempty"`

	// 环境变量配置
	Env map[string]string `json:"env,omitempty"`

	// CloneSet 映射
	CloneSet *kruiseapps.CloneSetSpec `json:"cloneSet,omitempty"`

	// AdvancedStatefulSet 映射
	AdvancedStatefulSet *kruiseapps.StatefulSetSpec `json:"advancedStatefulSet,omitempty"`

	// 副本数
	Replicas *int32 `json:"replicas,omitempty"`

	// 初始化容器 (已废弃)
	InitContainers []corev1.Container `json:"initContainers,omitempty"`

	// 服务容器 (已废弃)
	Containers []corev1.Container `json:"containers,omitempty"`

	// 亲和性配置 (已废弃)
	Affinity *corev1.Affinity `json:"affinity,omitempty"`

	// 污点设置 (已废弃)
	Tolerations []corev1.Toleration `json:"tolerations,omitempty"`

	// Service 配置 (已废弃)
	Service corev1.ServiceSpec `json:"service,omitempty"`

	// 存储卷挂载 (已废弃)
	Volumes []corev1.Volume `json:"volumes,omitempty"`

	// 超时时间 (已废弃)
	ProgressDeadlineSeconds *int32 `json:"progressDeadlineSeconds,omitempty" hash:"ignore"`
}

type MicroserviceCondition string

const (
	MicroserviceAvailable   MicroserviceCondition = "Available"
	MicroserviceProgressing MicroserviceCondition = "Progressing"
	MicroserviceFailure     MicroserviceCondition = "Failure"
	MicroserviceUnknown     MicroserviceCondition = "Unknown"
)

// MicroserviceStatus defines the observed state of Microservice
type MicroserviceStatus struct {
	Condition MicroserviceCondition `json:"condition,omitempty"`
	// +patchMergeKey=type
	// +patchStrategy=merge
	// +listType=map
	// +listMapKey=type
	Conditions []metav1.Condition `json:"conditions,omitempty" patchStrategy:"merge" patchMergeKey:"type"`
}

// +kubebuilder:subresource:status
// +kubebuilder:object:root=true

// Microservice is the Schema for the microservices API
type Microservice struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   MicroserviceSpec   `json:"spec,omitempty"`
	Status MicroserviceStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true

// MicroserviceList contains a list of Microservice
type MicroserviceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Microservice `json:"items"`
}

func init() {
	SchemeBuilder.Register(&Microservice{}, &MicroserviceList{})
}

func (in *Microservice) GetConditions() []metav1.Condition {
	return in.Status.Conditions
}

func (in *Microservice) SetConditions(conditions []metav1.Condition) {
	in.Status.Conditions = conditions
}
