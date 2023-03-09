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

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

type AggregationType string

const (
	// max is not welcomed since it may import outliers
	AVG AggregationType = "avg"
	P99 AggregationType = "p99"
	P95 AggregationType = "p95"
	P90 AggregationType = "p90"
	P50 AggregationType = "p50"
)

type NodeMetricInfo struct {
	NodeUsage ResourceMap `json:"nodeUsage,omitempty"`
	// AggregatedNodeUsages will report only if there are enough samples
	AggregatedNodeUsages []AggregatedUsage `json:"aggregatedNodeUsages,omitempty"`
}

type AggregatedUsage struct {
	Usage    map[AggregationType]ResourceMap `json:"usage,omitempty"`
	Duration metav1.Duration                 `json:"duration,omitempty"`
}

type PodMetricInfo struct {
	Name      string      `json:"name,omitempty"`
	Namespace string      `json:"namespace,omitempty"`
	PodUsage  ResourceMap `json:"podUsage,omitempty"`
}

// NodeMetricSpec defines the desired state of NodeMetric
type NodeMetricSpec struct {
	// CollectPolicy defines the Metric collection policy
	CollectPolicy *NodeMetricCollectPolicy `json:"metricCollectPolicy,omitempty"`
}

// NodeMetricCollectPolicy defines the Metric collection policy
type NodeMetricCollectPolicy struct {
	// AggregateDurationSeconds represents the aggregation period in seconds
	AggregateDurationSeconds *int64 `json:"aggregateDurationSeconds,omitempty"`
	// ReportIntervalSeconds represents the report period in seconds
	ReportIntervalSeconds *int64 `json:"reportIntervalSeconds,omitempty"`
	// NodeAggregatePolicy represents the target grain of node aggregated usage
	NodeAggregatePolicy *AggregatePolicy `json:"nodeAggregatePolicy,omitempty"`
}

type AggregatePolicy struct {
	Durations []metav1.Duration `json:"durations,omitempty"`
}

// NodeMetricStatus defines the observed state of NodeMetric
type NodeMetricStatus struct {
	// UpdateTime is the last time this NodeMetric was updated.
	UpdateTime *metav1.Time `json:"updateTime,omitempty"`

	// NodeMetric contains the metrics for this node.
	NodeMetric *NodeMetricInfo `json:"nodeMetric,omitempty"`

	// PodsMetric contains the metrics for pods belong to this node.
	PodsMetric []*PodMetricInfo `json:"podsMetric,omitempty"`
}

// +genclient
// +genclient:nonNamespaced
// +kubebuilder:object:root=true
// +kubebuilder:subresource:status
// +kubebuilder:resource:scope=Cluster

// NodeMetric is the Schema for the nodemetrics API
type NodeMetric struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   NodeMetricSpec   `json:"spec,omitempty"`
	Status NodeMetricStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true

// NodeMetricList contains a list of NodeMetric
type NodeMetricList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []NodeMetric `json:"items"`
}

func init() {
	SchemeBuilder.Register(&NodeMetric{}, &NodeMetricList{})
}
