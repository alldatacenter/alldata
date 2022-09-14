package v1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type JobSpec struct {
	// 包含的 Job 列表
	Job corev1.Container `json:"job,omitempty"`

	// 环境变量字典
	Env map[string]string `json:"env,omitempty"`

	// 运行成功的 Job 记录数限制
	// +kubebuilder:validation:Minimum=0
	SuccessfulJobsHistoryLimit *int32 `json:"successfulJobsHistoryLimit,omitempty"`

	// 运行失败的 Job 记录数限制
	// +kubebuilder:validation:Minimum=0
	FailedJobsHistoryLimit *int32 `json:"failedJobsHistoryLimit,omitempty"`

	// 重试次数
	// +kubebuilder:validation:Minimum=0
	BackoffLimit *int32 `json:"BackoffLimit,omitempty"`

	// 亲和性配置
	Affinity *corev1.Affinity `json:"affinity,omitempty"`

	// 污点设置
	Tolerations []corev1.Toleration `json:"tolerations,omitempty"`
}

// JobStatus defines the observed state of Job
type JobStatus struct{}

// +kubebuilder:object:root=true
// +kubebuilder:subresource:status

// Job is the Schema for the jobs API
type Job struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   JobSpec   `json:"spec,omitempty"`
	Status JobStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true

// JobList contains a list of Job
type JobList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Job `json:"items"`
}

func init() {
	SchemeBuilder.Register(&Job{}, &JobList{})
}
