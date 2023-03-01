/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type ShuffleServerUpgradeStrategyType string

const (
	PartitionUpgrade ShuffleServerUpgradeStrategyType = "PartitionUpgrade"
	SpecificUpgrade  ShuffleServerUpgradeStrategyType = "SpecificUpgrade"
	FullUpgrade      ShuffleServerUpgradeStrategyType = "FullUpgrade"
	FullRestart      ShuffleServerUpgradeStrategyType = "FullRestart"
)

type RSSPhase string

const (
	// RSSPending represents RSS object is pending.
	RSSPending RSSPhase = "Pending"
	// RSSRunning represents RSS object is running normally.
	RSSRunning RSSPhase = "Running"
	// RSSTerminating represents RSS object is terminating.
	RSSTerminating RSSPhase = "Terminating"
	// RSSFailed represents RSS object has been failed.
	RSSFailed RSSPhase = "Failed"
	// RSSUpgrading represents RSS object is upgrading.
	RSSUpgrading RSSPhase = "Upgrading"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// RemoteShuffleServiceSpec defines the desired state of RemoteShuffleService.
type RemoteShuffleServiceSpec struct {
	// Coordinator contains configurations of the coordinators.
	Coordinator *CoordinatorConfig `json:"coordinator"`

	// ShuffleServer contains configuration of the shuffle servers.
	ShuffleServer *ShuffleServerConfig `json:"shuffleServer"`

	// ConfigMapName indicates configMap name stores configurations of coordinators and shuffle servers.
	ConfigMapName string `json:"configMapName"`
}

// CoordinatorConfig records configuration used to generate workload of coordination.
type CoordinatorConfig struct {
	*CommonConfig `json:",inline"`

	// +kubebuilder:default:=true
	// Sync indicates whether we need to sync configurations to the running coordinators.
	// +optional
	Sync *bool `json:"sync,omitempty"`

	// +kubebuilder:default:=2
	// Count is the number of coordinator workloads to be generated.
	// +optional
	Count *int32 `json:"count,omitempty"`

	// +kubebuilder:default:=1
	// Replicas is the initial replicas of coordinators.
	// +optional
	Replicas *int32 `json:"replicas,omitempty"`

	// +kubebuilder:default:=19997
	// RPCPort defines rpc port used by coordinators.
	// +optional
	RPCPort *int32 `json:"rpcPort,omitempty"`

	// +kubebuilder:default:=19996
	// HTTPPort defines http port used by coordinators.
	// +optional
	HTTPPort *int32 `json:"httpPort,omitempty"`

	// +kubebuilder:default:=/config/exclude_nodes
	// ExcludeNodesFilePath indicates exclude nodes file path in coordinators' containers.
	// +optional
	ExcludeNodesFilePath string `json:"excludeNodesFilePath,omitempty"`

	// RPCNodePort defines rpc port of node port service used for coordinators' external access.
	// +optional
	RPCNodePort []int32 `json:"rpcNodePort,omitempty"`

	// HTTPNodePort defines http port of node port service used for coordinators' external access.
	// +optional
	HTTPNodePort []int32 `json:"httpNodePort,omitempty"`
}

// ShuffleServerConfig records configuration used to generate workload of shuffle servers.
type ShuffleServerConfig struct {
	*CommonConfig `json:",inline"`

	// +kubebuilder:default:=false
	// Sync indicates whether we need to sync configurations to the running shuffle servers.
	// +optional
	Sync *bool `json:"sync,omitempty"`

	// +kubebuilder:default:=1
	// Replicas is the initial replicas of shuffle servers.
	// +optional
	Replicas *int32 `json:"replicas,omitempty"`

	// +kubebuilder:default:=19997
	// RPCPort defines rpc port used by shuffle servers.
	// +optional
	RPCPort *int32 `json:"rpcPort,omitempty"`

	// +kubebuilder:default:=19996
	// HTTPPort defines http port used by shuffle servers.
	// +optional
	HTTPPort *int32 `json:"httpPort,omitempty"`

	// RPCNodePort defines rpc port of node port service used for shuffle servers' external access.
	// +optional
	RPCNodePort *int32 `json:"rpcNodePort,omitempty"`

	// HTTPNodePort defines http port of node port service used for shuffle servers' external access.
	// +optional
	HTTPNodePort *int32 `json:"httpNodePort,omitempty"`

	// UpgradeStrategy defines upgrade strategy of shuffle servers.
	UpgradeStrategy *ShuffleServerUpgradeStrategy `json:"upgradeStrategy"`
}

// ShuffleServerUpgradeStrategy defines upgrade strategy of shuffle servers.
type ShuffleServerUpgradeStrategy struct {
	// Type represents upgrade type of shuffle servers, including partition, specific copy and full upgrade.
	Type ShuffleServerUpgradeStrategyType `json:"type"`

	// Partition means the minimum number that needs to be upgraded, the copies whose numbers are greater than or
	// equal to this number needs to be upgraded.
	// +optional
	Partition *int32 `json:"partition,omitempty"`

	// SpecificNames indicates the specific pod names of shuffle servers which we want to upgrade.
	// +optional
	SpecificNames []string `json:"specificNames,omitempty"`
}

// CommonConfig defines the common fields of coordinators and shuffle servers.
type CommonConfig struct {
	*RSSPodSpec `json:",inline"`

	// XmxSize defines xmx size of coordinators or shuffle servers.
	XmxSize string `json:"xmxSize"`

	// ConfigDir records the directory where the configuration of coordinators or shuffle servers resides.
	ConfigDir string `json:"configDir"`

	// Parameters holds the optional parameters used by coordinators or shuffle servers .
	// optional
	Parameters map[string]string `json:"parameters,omitempty"`
}

// RSSPodSpec defines the desired state of coordinators or shuffle servers' pods.
type RSSPodSpec struct {
	*MainContainer `json:",inline"`

	// Volumes stores volumes' information used by coordinators or shuffle servers.
	// +optional
	Volumes []corev1.Volume `json:"volumes,omitempty"`

	// SidecarContainers represents sidecar containers for monitoring, logging, etc.
	// +optional
	SidecarContainers []corev1.Container `json:"sidecarContainers,omitempty"`

	// SecurityContext holds pod-level security attributes and common container settings.
	// +optional
	SecurityContext *corev1.PodSecurityContext `json:"securityContext,omitempty"`

	// InitContainerImage represents image of init container used to change owner of host paths.
	// +optional
	InitContainerImage string `json:"initContainerImage,omitempty"`

	// +kubebuilder:default:=true
	// HostNetwork indicates whether we need to enable host network.
	// +optional
	HostNetwork *bool `json:"hostNetwork,omitempty"`

	// HostPathMounts indicates host path volumes and their mounting path within shuffle servers' containers.
	// +optional
	HostPathMounts map[string]string `json:"hostPathMounts,omitempty"`

	// LogHostPath represents host path used to save logs of shuffle servers.
	// +optional
	LogHostPath string `json:"logHostPath,omitempty"`

	// Labels represents labels to be added in coordinators or shuffle servers' pods.
	// +optional
	Labels map[string]string `json:"labels,omitempty"`

	// RuntimeClassName refers to a RuntimeClass object in the node.k8s.io group, which should be used
	// to run this pod.  If no RuntimeClass resource matches the named class, the pod will not be run.
	// If unset or empty, the "legacy" RuntimeClass will be used, which is an implicit class with an
	// empty definition that uses the default runtime handler.
	// +optional
	RuntimeClassName *string `json:"runtimeClassName,omitempty"`

	// NodeSelector is a selector which must be true for the pod to fit on a node.
	// Selector which must match a node's labels for the pod to be scheduled on that node.
	// +optional
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`

	// Tolerations indicates the tolerations the pods under this subset have.
	// +optional
	Tolerations []corev1.Toleration `json:"tolerations,omitempty"`

	// Affinity is a group of affinity scheduling rules.
	// +optional
	Affinity *corev1.Affinity `json:"affinity,omitempty"`
}

// MainContainer stores information of the main container of coordinators or shuffle servers,
// its information will be used to generate workload of coordinators or shuffle servers.
type MainContainer struct {
	// Image represents image of coordinators or shuffle servers.
	Image string `json:"image"`

	// Command represents command used by coordinators or shuffle servers.
	// +optional
	Command []string `json:"command,omitempty"`

	// Args represents args used by coordinators or shuffle servers.
	// +optional
	Args []string `json:"args,omitempty"`

	// Ports represents ports used by coordinators or shuffle servers.
	// +optional
	Ports []corev1.ContainerPort `json:"ports,omitempty"`

	// Resources represents resources used by coordinators or shuffle servers.
	// +optional
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`

	// Env represents env set for coordinators or shuffle servers.
	// +optional
	Env []corev1.EnvVar `json:"env,omitempty"`

	// VolumeMounts indicates describes mountings of volumes within shuffle servers' container.
	// +optional
	VolumeMounts []corev1.VolumeMount `json:"volumeMounts,omitempty"`
}

// RemoteShuffleServiceStatus defines the observed state of RemoteShuffleService
type RemoteShuffleServiceStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	// Phase defines phase of the RemoteShuffleService.
	Phase RSSPhase `json:"phase"`

	// TargetKeys records the target names of shuffle servers to be excluded when the RSS object is
	// upgrading or terminating.
	// +optional
	TargetKeys []string `json:"targetKeys,omitempty"`

	// DeletedKeys records the names of deleted shuffle servers.
	// +optional
	DeletedKeys []string `json:"deletedKeys,omitempty"`

	// Reason is the reason why the RSS object is failed.
	// +optional
	Reason string `json:"reason,omitempty"`
}

//+genclient
//+kubebuilder:resource:shortName=rss
//+kubebuilder:object:root=true
//+kubebuilder:subresource:status
//+kubebuilder:printcolumn:name="UpgradeStrategyType",type="string",JSONPath=".spec.shuffleServer.upgradeStrategy.type",description="upgradeStrategy type of shuffleServer"
//+kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="rss phase"

// RemoteShuffleService is the Schema for the remoteshuffleservices API
type RemoteShuffleService struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   RemoteShuffleServiceSpec   `json:"spec,omitempty"`
	Status RemoteShuffleServiceStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// RemoteShuffleServiceList contains a list of RemoteShuffleService
type RemoteShuffleServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []RemoteShuffleService `json:"items"`
}

func init() {
	SchemeBuilder.Register(&RemoteShuffleService{}, &RemoteShuffleServiceList{})
}
