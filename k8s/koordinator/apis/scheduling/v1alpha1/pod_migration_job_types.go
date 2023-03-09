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
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

type PodMigrationJobSpec struct {
	// Paused indicates whether the PodMigrationJob should to work or not.
	// Default is false
	// +optional
	Paused bool `json:"paused,omitempty"`

	// TTL controls the PodMigrationJob timeout duration.
	// +optional
	TTL *metav1.Duration `json:"ttl,omitempty"`

	// Mode represents the operating mode of the Job
	// Default is PodMigrationJobModeReservationFirst
	// +optional
	Mode PodMigrationJobMode `json:"mode,omitempty"`

	// PodRef represents the Pod that be migrated
	// +required
	PodRef *corev1.ObjectReference `json:"podRef"`

	// ReservationOptions defines the Reservation options for migrated Pod
	// +optional
	ReservationOptions *PodMigrateReservationOptions `json:"reservationOptions,omitempty"`

	// DeleteOptions defines the deleting options for the migrated Pod and preempted Pods
	// +optional
	DeleteOptions *metav1.DeleteOptions `json:"deleteOptions,omitempty"`
}

type PodMigrationJobMode string

const (
	PodMigrationJobModeReservationFirst PodMigrationJobMode = "ReservationFirst"
	PodMigrationJobModeEvictionDirectly PodMigrationJobMode = "EvictDirectly"
)

type PodMigrateReservationOptions struct {
	// ReservationRef if specified, PodMigrationJob will check if the status of Reservation is available.
	// ReservationRef if not specified, PodMigrationJob controller will create Reservation by Template,
	// and update the ReservationRef to reference the Reservation
	// +optional
	ReservationRef *corev1.ObjectReference `json:"reservationRef,omitempty"`

	// Template is the object that describes the Reservation that will be created if not specified ReservationRef
	// +kubebuilder:pruning:PreserveUnknownFields
	// +kubebuilder:validation:Schemaless
	// +optional
	Template *ReservationTemplateSpec `json:"template,omitempty"`

	// PreemptionOption decides whether to preempt other Pods.
	// The preemption is safe and reserves resources for preempted Pods.
	// +optional
	PreemptionOptions *PodMigrationJobPreemptionOptions `json:"preemptionOptions,omitempty"`
}

type PodMigrationJobPreemptionOptions struct {
	// Reserved object.
}

type PodMigrationJobStatus struct {
	// PodMigrationJobPhase represents the phase of a PodMigrationJob is a simple, high-level summary of where the PodMigrationJob is in its lifecycle.
	// e.g. Pending/Running/Failed
	Phase PodMigrationJobPhase `json:"phase,omitempty"`
	// Status represents the current status of PodMigrationJob
	// e.g. ReservationCreated
	Status string `json:"status,omitempty"`
	// Reason represents a brief CamelCase message indicating details about why the PodMigrationJob is in this state.
	Reason string `json:"reason,omitempty"`
	// Message represents a human-readable message indicating details about why the PodMigrationJob is in this state.
	Message string `json:"message,omitempty"`
	// Conditions records the stats of PodMigrationJob
	Conditions []PodMigrationJobCondition `json:"conditions,omitempty"`
	// NodeName represents the node's name of migrated Pod
	NodeName string `json:"nodeName,omitempty"`
	// PodRef represents the newly created Pod after being migrated
	PodRef *corev1.ObjectReference `json:"podRef,omitempty"`
	// PreemptedPodsRef represents the Pods that be preempted
	PreemptedPodsRef []corev1.ObjectReference `json:"preemptedPodsRef,omitempty"`
	// PreemptedPodsReservations records information about Reservations created due to preemption
	PreemptedPodsReservations []PodMigrationJobPreemptedReservation `json:"preemptedPodsReservation,omitempty"`
}

type PodMigrationJobPreemptedReservation struct {
	// Namespace represents the namespace of Reservation
	Namespace string `json:"namespace,omitempty"`
	// Name represents the name of Reservation
	Name string `json:"name,omitempty"`
	// NodeName represents the assigned node for Reservation by scheduler
	NodeName string `json:"nodeName,omitempty"`
	// Phase represents the Phase of Reservation
	Phase string `json:"phase,omitempty"`
	// PreemptedPodRef represents the Pod that be preempted
	PreemptedPodRef *corev1.ObjectReference `json:"preemptedPodRef,omitempty"`
	// PodsRef represents the newly created Pods after being preempted
	PodsRef []corev1.ObjectReference `json:"podsRef,omitempty"`
}

type PodMigrationJobCondition struct {
	// Type is the type of the condition.
	Type PodMigrationJobConditionType `json:"type"`
	// Status is the status of the condition.
	// Can be True, False, Unknown.
	Status PodMigrationJobConditionStatus `json:"status"`
	// Last time we probed the condition.
	// +nullable
	LastProbeTime metav1.Time `json:"lastProbeTime,omitempty"`
	// Last time the condition transitioned from one status to another.
	// +nullable
	LastTransitionTime metav1.Time `json:"lastTransitionTime,omitempty"`
	// Unique, one-word, CamelCase reason for the condition's last transition.
	Reason string `json:"reason,omitempty"`
	// Human-readable message indicating details about last transition.
	Message string `json:"message,omitempty"`
}

type PodMigrationJobPhase string

const (
	// PodMigrationJobPending represents the initial status
	PodMigrationJobPending PodMigrationJobPhase = "Pending"
	// PodMigrationJobRunning represents the PodMigrationJob is being processed
	PodMigrationJobRunning PodMigrationJobPhase = "Running"
	// PodMigrationJobSucceeded represents the PodMigrationJob processed successfully
	PodMigrationJobSucceeded PodMigrationJobPhase = "Succeeded"
	// PodMigrationJobFailed represents the PodMigrationJob process failed caused by Timeout, Reservation failed, etc.
	PodMigrationJobFailed PodMigrationJobPhase = "Failed"
	// PodMigrationJobAborted represents the user forcefully aborted the PodMigrationJob.
	PodMigrationJobAborted PodMigrationJobPhase = "Aborted"
)

type PodMigrationJobConditionType string

// These are valid conditions of PodMigrationJob.
const (
	PodMigrationJobConditionReservationCreated             PodMigrationJobConditionType = "ReservationCreated"
	PodMigrationJobConditionReservationScheduled           PodMigrationJobConditionType = "ReservationScheduled"
	PodMigrationJobConditionPreemption                     PodMigrationJobConditionType = "Preemption"
	PodMigrationJobConditionEviction                       PodMigrationJobConditionType = "Eviction"
	PodMigrationJobConditionPodScheduled                   PodMigrationJobConditionType = "PodScheduled"
	PodMigrationJobConditionReservationPodBoundReservation PodMigrationJobConditionType = "PodBoundReservation"
	PodMigrationJobConditionReservationBound               PodMigrationJobConditionType = "ReservationBound"
)

// These are valid reasons of PodMigrationJob.
const (
	PodMigrationJobReasonTimeout                   = "Timeout"
	PodMigrationJobReasonFailedCreateReservation   = "FailedCreateReservation"
	PodMigrationJobReasonReservationExpired        = "ReservationExpired"
	PodMigrationJobReasonUnschedulable             = "Unschedulable"
	PodMigrationJobReasonForbiddenMigratePod       = "ForbiddenMigratePod"
	PodMigrationJobReasonMissingPod                = "MissingPod"
	PodMigrationJobReasonMissingReservation        = "MissingReservation"
	PodMigrationJobReasonPreempting                = "Preempting"
	PodMigrationJobReasonPreemptComplete           = "PreemptComplete"
	PodMigrationJobReasonEvicting                  = "Evicting"
	PodMigrationJobReasonFailedEvict               = "FailedEvict"
	PodMigrationJobReasonEvictComplete             = "EvictComplete"
	PodMigrationJobReasonWaitForPodBindReservation = "WaitForPodBindReservation"
)

type PodMigrationJobConditionStatus string

const (
	PodMigrationJobConditionStatusTrue    PodMigrationJobConditionStatus = "True"
	PodMigrationJobConditionStatusFalse   PodMigrationJobConditionStatus = "False"
	PodMigrationJobConditionStatusUnknown PodMigrationJobConditionStatus = "Unknown"
)

// PodMigrationJob is the Schema for the PodMigrationJob API
// +k8s:openapi-gen=true
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +genclient
// +genclient:nonNamespaced
// +kubebuilder:resource:scope=Cluster,shortName=pmj
// +kubebuilder:object:root=true
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The phase of PodMigrationJob"
// +kubebuilder:printcolumn:name="Status",type="string",JSONPath=".status.status",description="The status of PodMigrationJob"
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
// +kubebuilder:printcolumn:name="Node",type="string",JSONPath=".status.nodeName"
// +kubebuilder:printcolumn:name="Reservation",type="string",JSONPath=".spec.reservationOptions.reservationRef.name"
// +kubebuilder:printcolumn:name="PodNamespace",type="string",JSONPath=".spec.podRef.namespace"
// +kubebuilder:printcolumn:name="Pod",type="string",JSONPath=".spec.podRef.name"
// +kubebuilder:printcolumn:name="NewPod",type="string",JSONPath=".status.podRef.name"
// +kubebuilder:printcolumn:name="TTL",type="string",JSONPath=".spec.ttl"

type PodMigrationJob struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   PodMigrationJobSpec   `json:"spec,omitempty"`
	Status PodMigrationJobStatus `json:"status,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// PodMigrationJobList contains a list of PodMigrationJob
type PodMigrationJobList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []PodMigrationJob `json:"items"`
}

func init() {
	SchemeBuilder.Register(&PodMigrationJob{}, &PodMigrationJobList{})
}
