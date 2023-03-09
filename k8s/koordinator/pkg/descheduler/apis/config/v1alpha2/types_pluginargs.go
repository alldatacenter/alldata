/*
Copyright 2022 The Koordinator Authors.

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

package v1alpha2

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// DefaultEvictorArgs holds arguments used to configure the DefaultEvictor plugin.
type DefaultEvictorArgs struct {
	metav1.TypeMeta

	DryRun *bool `json:"dryRun,omitempty"`
	// MaxNoOfPodsToEvictPerNode restricts maximum of pods to be evicted per node.
	MaxNoOfPodsToEvictPerNode *int `json:"maxNoOfPodsToEvictPerNode,omitempty"`
	// MaxNoOfPodsToEvictPerNamespace restricts maximum of pods to be evicted per namespace.
	MaxNoOfPodsToEvictPerNamespace *int `json:"maxNoOfPodsToEvictPerNamespace,omitempty"`

	// EvictFailedBarePods allows pods without ownerReferences and in failed phase to be evicted.
	EvictFailedBarePods bool `json:"evictFailedBarePods"`

	// EvictLocalStoragePods allows pods using local storage to be evicted.
	EvictLocalStoragePods bool `json:"evictLocalStoragePods"`

	// EvictSystemCriticalPods allows eviction of pods of any priority (including Kubernetes system pods)
	EvictSystemCriticalPods bool `json:"evictSystemCriticalPods"`

	// IgnorePVCPods prevents pods with PVCs from being evicted.
	IgnorePvcPods bool `json:"ignorePvcPods"`

	// NodeFit sets whether to consider taints, node selectors,
	// and pod affinity when evicting. A pod whose tolerations, node selectors,
	// and affinity match a node other than the one it is currently running on
	// is evictable.
	NodeFit bool `json:"nodeFit"`
	// PriorityThreshold represents a threshold for pod's priority class.
	// Any pod whose priority class is lower is evictable.
	PriorityThreshold *PriorityThreshold `json:"priorityThreshold,omitempty"`
	// LabelSelector sets whether to apply label filtering when evicting.
	// Any pod matching the label selector is considered evictable.
	LabelSelector *metav1.LabelSelector `json:"labelSelector,omitempty"`
}

type PriorityThreshold struct {
	Value *int32 `json:"value,omitempty"`
	Name  string `json:"name,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// RemovePodsViolatingNodeAffinityArgs holds arguments used to configure the RemovePodsViolatingNodeAffinity plugin.
type RemovePodsViolatingNodeAffinityArgs struct {
	metav1.TypeMeta

	Namespaces       *Namespaces           `json:"namespaces,omitempty"`
	LabelSelector    *metav1.LabelSelector `json:"labelSelector,omitempty"`
	NodeAffinityType []string              `json:"nodeAffinityType,omitempty"`
}

// Namespaces carries a list of included/excluded namespaces
// for which a given strategy is applicable
type Namespaces struct {
	Include []string `json:"include,omitempty"`
	Exclude []string `json:"exclude,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// MigrationControllerArgs holds arguments used to configure the MigrationController
type MigrationControllerArgs struct {
	metav1.TypeMeta

	// DryRun means only execute the entire migration logic except create Reservation or Delete Pod
	// Default is false
	DryRun bool `json:"dryRun,omitempty"`

	// MaxConcurrentReconciles is the maximum number of concurrent Reconciles which can be run. Defaults to 1.
	MaxConcurrentReconciles *int32 `json:"maxConcurrentReconciles,omitempty"`

	// EvictFailedBarePods allows pods without ownerReferences and in failed phase to be evicted.
	EvictFailedBarePods bool `json:"evictFailedBarePods"`

	// EvictLocalStoragePods allows pods using local storage to be evicted.
	EvictLocalStoragePods bool `json:"evictLocalStoragePods"`

	// EvictSystemCriticalPods allows eviction of pods of any priority (including Kubernetes system pods)
	EvictSystemCriticalPods bool `json:"evictSystemCriticalPods"`

	// IgnorePVCPods prevents pods with PVCs from being evicted.
	IgnorePvcPods bool `json:"ignorePvcPods"`

	// LabelSelector sets whether to apply label filtering when evicting.
	// Any pod matching the label selector is considered evictable.
	LabelSelector *metav1.LabelSelector `json:"labelSelector,omitempty"`

	// Namespaces carries a list of included/excluded namespaces
	Namespaces *Namespaces `json:"namespaces,omitempty"`

	// MaxMigratingPerNode represents he maximum number of pods that can be migrating during migrate per node.
	MaxMigratingPerNode *int32 `json:"maxMigratingPerNode,omitempty"`

	// MaxMigratingPerNamespace represents he maximum number of pods that can be migrating during migrate per namespace.
	MaxMigratingPerNamespace *int32 `json:"maxMigratingPerNamespace,omitempty"`

	// MaxMigratingPerWorkload represents he maximum number of pods that can be migrating during migrate per workload.
	// Value can be an absolute number (ex: 5) or a percentage of desired pods (ex: 10%).
	MaxMigratingPerWorkload *intstr.IntOrString `json:"maxMigratingPerWorkload,omitempty"`

	// MaxUnavailablePerWorkload represents he maximum number of pods that can be unavailable during migrate per workload.
	// The unavailable state includes NotRunning/NotReady/Migrating/Evicting
	// Value can be an absolute number (ex: 5) or a percentage of desired pods (ex: 10%).
	MaxUnavailablePerWorkload *intstr.IntOrString `json:"maxUnavailablePerWorkload,omitempty"`

	// ObjectLimiters control the frequency of migration/eviction to make it smoother,
	// and also protect Pods of the same class from being evicted frequently.
	// e.g. limiting the frequency of Pods of the same workload being evicted.
	// The default is to set the MigrationLimitObjectWorkload limiter.
	ObjectLimiters ObjectLimiterMap `json:"objectLimiters,omitempty"`

	// DefaultJobMode represents the default operating mode of the PodMigrationJob
	// Default is PodMigrationJobModeReservationFirst
	DefaultJobMode string `json:"defaultJobMode,omitempty"`

	// DefaultJobTTL represents the default TTL of the PodMigrationJob
	// Default is 5 minute
	DefaultJobTTL *metav1.Duration `json:"defaultJobTTL,omitempty"`

	// EvictQPS controls the number of evict per second
	EvictQPS string `json:"evictQPS,omitempty"`
	// EvictBurst is the maximum number of tokens
	EvictBurst int32 `json:"evictBurst,omitempty"`
	// EvictionPolicy represents how to delete Pod, support "Delete" and "Eviction", default value is "Eviction"
	EvictionPolicy string `json:"evictionPolicy,omitempty"`
	// DefaultDeleteOptions defines options when deleting migrated pods and preempted pods through the method specified by EvictionPolicy
	DefaultDeleteOptions *metav1.DeleteOptions `json:"defaultDeleteOptions,omitempty"`
}

type MigrationLimitObjectType string

const (
	MigrationLimitObjectWorkload MigrationLimitObjectType = "workload"
)

type ObjectLimiterMap map[MigrationLimitObjectType]MigrationObjectLimiter

// MigrationObjectLimiter means that if the specified dimension has multiple migrations within the configured time period
// and exceeds the configured threshold, it will be limited.
type MigrationObjectLimiter struct {
	// Duration indicates the time window of the desired limit.
	Duration metav1.Duration `json:"duration,omitempty"`
	// MaxMigrating indicates the maximum number of migrations/evictions allowed within the window time.
	// If configured as 0, the maximum number will be calculated according to MaxMigratingPerWorkload.
	MaxMigrating *intstr.IntOrString `json:"maxMigrating,omitempty"`
}
