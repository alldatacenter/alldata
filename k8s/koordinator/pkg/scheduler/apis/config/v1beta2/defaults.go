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
	"math"

	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	schedconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"
	"k8s.io/utils/pointer"
)

var (
	defaultNodeMetricExpirationSeconds int64 = 180

	defaultResourceWeights = map[corev1.ResourceName]int64{
		corev1.ResourceCPU:    1,
		corev1.ResourceMemory: 1,
	}

	defaultUsageThresholds = map[corev1.ResourceName]int64{
		corev1.ResourceCPU:    65, // 65%
		corev1.ResourceMemory: 95, // 95%
	}

	defaultEstimatedScalingFactors = map[corev1.ResourceName]int64{
		corev1.ResourceCPU:    85, // 85%
		corev1.ResourceMemory: 70, // 70%
	}

	defaultPreferredCPUBindPolicy          = CPUBindPolicyFullPCPUs
	defaultNodeNUMAResourceScoringStrategy = &ScoringStrategy{
		Type: MostAllocated,
		Resources: []schedconfig.ResourceSpec{
			{
				Name:   string(corev1.ResourceCPU),
				Weight: 1,
			},
		},
	}

	defaultEnablePreemption = pointer.Bool(false)

	defaultDelayEvictTime       = 120 * time.Second
	defaultRevokePodInterval    = 1 * time.Second
	defaultDefaultQuotaGroupMax = corev1.ResourceList{
		// pkg/scheduler/plugins/elasticquota/controller.go syncHandler patch will overflow when the spec Max/Min is too high.
		corev1.ResourceCPU:    *resource.NewQuantity(math.MaxInt64/5, resource.DecimalSI),
		corev1.ResourceMemory: *resource.NewQuantity(math.MaxInt64/5, resource.DecimalSI),
	}
	defaultSystemQuotaGroupMax = corev1.ResourceList{
		corev1.ResourceCPU:    *resource.NewQuantity(math.MaxInt64/5, resource.DecimalSI),
		corev1.ResourceMemory: *resource.NewQuantity(math.MaxInt64/5, resource.BinarySI),
	}

	defaultQuotaGroupNamespace = "koordinator-system"

	defaultMonitorAllQuotas       = pointer.Bool(false)
	defaultEnableCheckParentQuota = pointer.Bool(false)

	defaultTimeout           = 600 * time.Second
	defaultControllerWorkers = 1
)

// SetDefaults_LoadAwareSchedulingArgs sets the default parameters for LoadAwareScheduling plugin.
func SetDefaults_LoadAwareSchedulingArgs(obj *LoadAwareSchedulingArgs) {
	if obj.FilterExpiredNodeMetrics == nil {
		obj.FilterExpiredNodeMetrics = pointer.Bool(true)
	}
	if obj.NodeMetricExpirationSeconds == nil {
		obj.NodeMetricExpirationSeconds = pointer.Int64Ptr(defaultNodeMetricExpirationSeconds)
	}
	if len(obj.ResourceWeights) == 0 {
		obj.ResourceWeights = defaultResourceWeights
	}
	if len(obj.UsageThresholds) == 0 {
		obj.UsageThresholds = defaultUsageThresholds
	}
	if len(obj.EstimatedScalingFactors) == 0 {
		obj.EstimatedScalingFactors = defaultEstimatedScalingFactors
	}
}

// SetDefaults_NodeNUMAResourceArgs sets the default parameters for NodeNUMANodeResource plugin.
func SetDefaults_NodeNUMAResourceArgs(obj *NodeNUMAResourceArgs) {
	if obj.DefaultCPUBindPolicy == "" {
		obj.DefaultCPUBindPolicy = defaultPreferredCPUBindPolicy
	}
	if obj.ScoringStrategy == nil {
		obj.ScoringStrategy = defaultNodeNUMAResourceScoringStrategy
	}
}

func SetDefaults_ReservationArgs(obj *ReservationArgs) {
	if obj.EnablePreemption == nil {
		obj.EnablePreemption = defaultEnablePreemption
	}
}

func SetDefaults_ElasticQuotaArgs(obj *ElasticQuotaArgs) {
	if obj.DelayEvictTime == nil {
		obj.DelayEvictTime = &metav1.Duration{
			Duration: defaultDelayEvictTime,
		}
	}
	if obj.RevokePodInterval == nil {
		obj.RevokePodInterval = &metav1.Duration{
			Duration: defaultRevokePodInterval,
		}
	}
	if len(obj.DefaultQuotaGroupMax) == 0 {
		obj.DefaultQuotaGroupMax = defaultDefaultQuotaGroupMax
	}
	if len(obj.SystemQuotaGroupMax) == 0 {
		obj.SystemQuotaGroupMax = defaultSystemQuotaGroupMax
	}
	if len(obj.QuotaGroupNamespace) == 0 {
		obj.QuotaGroupNamespace = defaultQuotaGroupNamespace
	}
	if obj.MonitorAllQuotas == nil {
		obj.MonitorAllQuotas = defaultMonitorAllQuotas
	}
	if obj.EnableCheckParentQuota == nil {
		obj.EnableCheckParentQuota = defaultEnableCheckParentQuota
	}
}

func SetDefaults_CoschedulingArgs(obj *CoschedulingArgs) {
	if obj.DefaultTimeout == nil {
		obj.DefaultTimeout = &metav1.Duration{
			Duration: defaultTimeout,
		}
	}
	if obj.ControllerWorkers == nil {
		obj.ControllerWorkers = pointer.Int64Ptr(int64(defaultControllerWorkers))
	}
}
