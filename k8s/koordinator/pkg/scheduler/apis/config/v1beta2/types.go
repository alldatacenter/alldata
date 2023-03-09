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

package v1beta2

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	schedconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// LoadAwareSchedulingArgs holds arguments used to configure the LoadAwareScheduling plugin.
type LoadAwareSchedulingArgs struct {
	metav1.TypeMeta `json:",inline"`

	// FilterExpiredNodeMetrics indicates whether to filter nodes where koordlet fails to update NodeMetric.
	FilterExpiredNodeMetrics *bool `json:"filterExpiredNodeMetrics,omitempty"`
	// NodeMetricExpirationSeconds indicates the NodeMetric expiration in seconds.
	// When NodeMetrics expired, the node is considered abnormal.
	// Default is 180 seconds.
	NodeMetricExpirationSeconds *int64 `json:"nodeMetricExpirationSeconds,omitempty"`
	// ResourceWeights indicates the weights of resources.
	// The weights of CPU and Memory are both 1 by default.
	ResourceWeights map[corev1.ResourceName]int64 `json:"resourceWeights,omitempty"`
	// UsageThresholds indicates the resource utilization threshold of the whole machine.
	// The default for CPU is 65%, and the default for memory is 95%.
	UsageThresholds map[corev1.ResourceName]int64 `json:"usageThresholds,omitempty"`
	// ProdUsageThresholds indicates the resource utilization threshold of Prod Pods compared to the whole machine.
	// Not enabled by default
	ProdUsageThresholds map[corev1.ResourceName]int64 `json:"prodUsageThresholds,omitempty"`
	// ScoreAccordingProdUsage controls whether to score according to the utilization of Prod Pod
	ScoreAccordingProdUsage *bool `json:"scoreAccordingProdUsage,omitempty"`
	// EstimatedScalingFactors indicates the factor when estimating resource usage.
	// The default value of CPU is 85%, and the default value of Memory is 70%.
	EstimatedScalingFactors map[corev1.ResourceName]int64 `json:"estimatedScalingFactors,omitempty"`
	// Aggregated supports resource utilization filtering and scoring based on percentile statistics
	Aggregated *LoadAwareSchedulingAggregatedArgs `json:"aggregated,omitempty"`
}

type LoadAwareSchedulingAggregatedArgs struct {
	// UsageThresholds indicates the resource utilization threshold of the machine based on percentile statistics
	UsageThresholds map[corev1.ResourceName]int64 `json:"usageThresholds,omitempty"`
	// UsageAggregationType indicates the percentile type of the machine's utilization when filtering
	UsageAggregationType slov1alpha1.AggregationType `json:"usageAggregationType,omitempty"`
	// UsageAggregatedDuration indicates the statistical period of the percentile of the machine's utilization when filtering
	UsageAggregatedDuration *metav1.Duration `json:"usageAggregatedDuration,omitempty"`

	// ScoreAggregationType indicates the percentile type of the machine's utilization when scoring
	ScoreAggregationType slov1alpha1.AggregationType `json:"scoreAggregationType,omitempty"`
	// ScoreAggregatedDuration indicates the statistical period of the percentile of Prod Pod's utilization when scoring
	ScoreAggregatedDuration *metav1.Duration `json:"scoreAggregatedDuration,omitempty"`
}

// ScoringStrategyType is a "string" type.
type ScoringStrategyType string

const (
	// MostAllocated strategy favors node with the least amount of available resource
	MostAllocated ScoringStrategyType = "MostAllocated"
	// BalancedAllocation strategy favors nodes with balanced resource usage rate
	BalancedAllocation ScoringStrategyType = "BalancedAllocation"
	// LeastAllocated strategy favors node with the most amount of available resource
	LeastAllocated ScoringStrategyType = "LeastAllocated"
)

// ScoringStrategy define ScoringStrategyType for the plugin
type ScoringStrategy struct {
	// Type selects which strategy to run.
	Type ScoringStrategyType

	// Resources a list of pairs <resource, weight> to be considered while scoring
	// allowed weights start from 1.
	Resources []schedconfig.ResourceSpec
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// NodeNUMAResourceArgs holds arguments used to configure the NodeNUMAResource plugin.
type NodeNUMAResourceArgs struct {
	metav1.TypeMeta

	DefaultCPUBindPolicy CPUBindPolicy    `json:"defaultCPUBindPolicy,omitempty"`
	ScoringStrategy      *ScoringStrategy `json:"scoringStrategy,omitempty"`
}

// CPUBindPolicy defines the CPU binding policy
type CPUBindPolicy = extension.CPUBindPolicy

const (
	// CPUBindPolicyDefault performs the default bind policy that specified in koord-scheduler configuration
	CPUBindPolicyDefault CPUBindPolicy = extension.CPUBindPolicyDefault
	// CPUBindPolicyFullPCPUs favor cpuset allocation that pack in few physical cores
	CPUBindPolicyFullPCPUs CPUBindPolicy = extension.CPUBindPolicyFullPCPUs
	// CPUBindPolicySpreadByPCPUs favor cpuset allocation that evenly allocate logical cpus across physical cores
	CPUBindPolicySpreadByPCPUs CPUBindPolicy = extension.CPUBindPolicySpreadByPCPUs
	// CPUBindPolicyConstrainedBurst constrains the CPU Shared Pool range of the Burstable Pod
	CPUBindPolicyConstrainedBurst CPUBindPolicy = extension.CPUBindPolicyConstrainedBurst
)

type CPUExclusivePolicy = extension.CPUExclusivePolicy

const (
	// CPUExclusivePolicyNone does not perform any exclusive policy
	CPUExclusivePolicyNone CPUExclusivePolicy = extension.CPUExclusivePolicyNone
	// CPUExclusivePolicyPCPULevel represents mutual exclusion in the physical core dimension
	CPUExclusivePolicyPCPULevel CPUExclusivePolicy = extension.CPUExclusivePolicyPCPULevel
	// CPUExclusivePolicyNUMANodeLevel indicates mutual exclusion in the NUMA topology dimension
	CPUExclusivePolicyNUMANodeLevel CPUExclusivePolicy = extension.CPUExclusivePolicyNUMANodeLevel
)

// NUMAAllocateStrategy indicates how to choose satisfied NUMA Nodes
type NUMAAllocateStrategy = extension.NUMAAllocateStrategy

const (
	// NUMAMostAllocated indicates that allocates from the NUMA Node with the least amount of available resource.
	NUMAMostAllocated NUMAAllocateStrategy = extension.NUMAMostAllocated
	// NUMALeastAllocated indicates that allocates from the NUMA Node with the most amount of available resource.
	NUMALeastAllocated NUMAAllocateStrategy = extension.NUMALeastAllocated
	// NUMADistributeEvenly indicates that evenly distribute CPUs across NUMA Nodes.
	NUMADistributeEvenly NUMAAllocateStrategy = extension.NUMADistributeEvenly
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// ReservationArgs holds arguments used to configure the Reservation plugin.
type ReservationArgs struct {
	metav1.TypeMeta

	// EnablePreemption indicates whether to enable preemption for reservations.
	EnablePreemption *bool `json:"enablePreemption,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// ElasticQuotaArgs holds arguments used to configure the ElasticQuota plugin.
type ElasticQuotaArgs struct {
	metav1.TypeMeta

	// DelayEvictTime is the duration to handle the jitter of used and runtime
	DelayEvictTime *metav1.Duration `json:"delayEvictTime,omitempty"`

	// RevokePodInterval is the interval to check quotaGroup's used and runtime
	RevokePodInterval *metav1.Duration `json:"revokePodInterval,omitempty"`

	// DefaultQuotaGroupMax limit the maxQuota of DefaultQuotaGroup
	DefaultQuotaGroupMax corev1.ResourceList `json:"defaultQuotaGroupMax,omitempty"`

	// SystemQuotaGroupMax limit the maxQuota of SystemQuotaGroup
	SystemQuotaGroupMax corev1.ResourceList `json:"systemQuotaGroupMax,omitempty"`

	// QuotaGroupNamespace is the namespace of the DefaultQuotaGroup
	QuotaGroupNamespace string `json:"quotaGroupNamespace,omitempty"`

	// MonitorAllQuotas monitor the quotaGroups' used and runtime Quota to revoke pods
	MonitorAllQuotas *bool `json:"monitorAllQuotas,omitempty"`

	// EnableCheckParentQuota check parentQuotaGroups' used and runtime Quota in PreFilter
	EnableCheckParentQuota *bool `json:"enableCheckParentQuota,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// CoschedulingArgs defines the parameters for Gang Scheduling plugin.
type CoschedulingArgs struct {
	metav1.TypeMeta

	// DefaultTimeout is the default gang's waiting time in Permit stage
	// default is 600 seconds
	DefaultTimeout *metav1.Duration `json:"defaultTimeout,omitempty"`
	// Workers number of controller
	// default is 1
	ControllerWorkers *int64 `json:"controllerWorkers,omitempty"`
}
