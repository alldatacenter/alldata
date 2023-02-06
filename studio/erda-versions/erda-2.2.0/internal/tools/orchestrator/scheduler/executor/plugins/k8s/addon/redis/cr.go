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

package redis

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// RedisFailover represents a Redis failover
type RedisFailover struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              RedisFailoverSpec `json:"spec"`
}

// RedisFailoverSpec represents a Redis failover spec
type RedisFailoverSpec struct {
	Redis          RedisSettings    `json:"redis,omitempty"`
	Sentinel       SentinelSettings `json:"sentinel,omitempty"`
	Auth           AuthSettings     `json:"auth,omitempty"`
	LabelWhitelist []string         `json:"labelWhitelist,omitempty"`
}

// RedisSettings defines the specification of the redis cluster
type RedisSettings struct {
	Image              string                        `json:"image,omitempty"`
	ImagePullPolicy    corev1.PullPolicy             `json:"imagePullPolicy,omitempty"`
	Replicas           int32                         `json:"replicas,omitempty"`
	Resources          corev1.ResourceRequirements   `json:"resources,omitempty"`
	CustomConfig       []string                      `json:"customConfig,omitempty"`
	Command            []string                      `json:"command,omitempty"`
	ShutdownConfigMap  string                        `json:"shutdownConfigMap,omitempty"`
	Storage            RedisStorage                  `json:"storage,omitempty"`
	Exporter           RedisExporter                 `json:"exporter,omitempty"`
	Affinity           *corev1.Affinity              `json:"affinity,omitempty"`
	SecurityContext    *corev1.PodSecurityContext    `json:"securityContext,omitempty"`
	ImagePullSecrets   []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	Tolerations        []corev1.Toleration           `json:"tolerations,omitempty"`
	NodeSelector       map[string]string             `json:"nodeSelector,omitempty"`
	PodAnnotations     map[string]string             `json:"podAnnotations,omitempty"`
	ServiceAnnotations map[string]string             `json:"serviceAnnotations,omitempty"`
	HostNetwork        bool                          `json:"hostNetwork,omitempty"`
	DNSPolicy          corev1.DNSPolicy              `json:"dnsPolicy,omitempty"`
	Envs               map[string]string             `json:"envs,omitempty"`
}

// SentinelSettings defines the specification of the sentinel cluster
type SentinelSettings struct {
	Image              string                        `json:"image,omitempty"`
	ImagePullPolicy    corev1.PullPolicy             `json:"imagePullPolicy,omitempty"`
	Replicas           int32                         `json:"replicas,omitempty"`
	Resources          corev1.ResourceRequirements   `json:"resources,omitempty"`
	CustomConfig       []string                      `json:"customConfig,omitempty"`
	Command            []string                      `json:"command,omitempty"`
	Affinity           *corev1.Affinity              `json:"affinity,omitempty"`
	SecurityContext    *corev1.PodSecurityContext    `json:"securityContext,omitempty"`
	ImagePullSecrets   []corev1.LocalObjectReference `json:"imagePullSecrets,omitempty"`
	Tolerations        []corev1.Toleration           `json:"tolerations,omitempty"`
	NodeSelector       map[string]string             `json:"nodeSelector,omitempty"`
	PodAnnotations     map[string]string             `json:"podAnnotations,omitempty"`
	ServiceAnnotations map[string]string             `json:"serviceAnnotations,omitempty"`
	Exporter           SentinelExporter              `json:"exporter,omitempty"`
	HostNetwork        bool                          `json:"hostNetwork,omitempty"`
	DNSPolicy          corev1.DNSPolicy              `json:"dnsPolicy,omitempty"`
	Envs               map[string]string             `json:"envs,omitempty"`
}

// AuthSettings contains settings about auth
type AuthSettings struct {
	SecretPath string `json:"secretPath,omitempty"`
}

// RedisExporter defines the specification for the redis exporter
type RedisExporter struct {
	Enabled         bool              `json:"enabled,omitempty"`
	Image           string            `json:"image,omitempty"`
	ImagePullPolicy corev1.PullPolicy `json:"imagePullPolicy,omitempty"`
}

// SentinelExporter defines the specification for the sentinel exporter
type SentinelExporter struct {
	Enabled         bool              `json:"enabled,omitempty"`
	Image           string            `json:"image,omitempty"`
	ImagePullPolicy corev1.PullPolicy `json:"imagePullPolicy,omitempty"`
}

// RedisStorage defines the structure used to store the Redis Data
type RedisStorage struct {
	KeepAfterDeletion     bool                          `json:"keepAfterDeletion,omitempty"`
	EmptyDir              *corev1.EmptyDirVolumeSource  `json:"emptyDir,omitempty"`
	PersistentVolumeClaim *corev1.PersistentVolumeClaim `json:"persistentVolumeClaim,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// RedisFailoverList represents a Redis failover list
type RedisFailoverList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata"`

	Items []RedisFailover `json:"items"`
}
