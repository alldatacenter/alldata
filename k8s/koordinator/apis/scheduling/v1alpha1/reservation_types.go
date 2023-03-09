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

type ReservationSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	// Template defines the scheduling requirements (resources, affinities, images, ...) processed by the scheduler just
	// like a normal pod.
	// If the `template.spec.nodeName` is specified, the scheduler will not choose another node but reserve resources on
	// the specified node.
	// +kubebuilder:pruning:PreserveUnknownFields
	// +kubebuilder:validation:Schemaless
	// +kubebuilder:validation:Required
	Template *corev1.PodTemplateSpec `json:"template"`
	// Specify the owners who can allocate the reserved resources.
	// Multiple owner selectors and ORed.
	// +kubebuilder:validation:Required
	// +kubebuilder:validation:MinItems=1
	Owners []ReservationOwner `json:"owners"`
	// Time-to-Live period for the reservation.
	// `expires` and `ttl` are mutually exclusive. Defaults to 24h. Set 0 to disable expiration.
	// +kubebuilder:default="24h"
	// +optional
	TTL *metav1.Duration `json:"ttl,omitempty"`
	// Expired timestamp when the reservation is expected to expire.
	// If both `expires` and `ttl` are set, `expires` is checked first.
	// `expires` and `ttl` are mutually exclusive. Defaults to being set dynamically at runtime based on the `ttl`.
	// +optional
	Expires *metav1.Time `json:"expires,omitempty"`
	// By default, the resources requirements of reservation (specified in `template.spec`) is filtered by whether the
	// node has sufficient free resources (i.e. Reservation Request <  Node Free).
	// When `preAllocation` is set, the scheduler will skip this validation and allow overcommitment. The scheduled
	// reservation would be waiting to be available until free resources are sufficient.
	// +optional
	PreAllocation bool `json:"preAllocation,omitempty"`
	// By default, reserved resources are always allocatable as long as the reservation phase is Available. When
	// `AllocateOnce` is set, the reserved resources are only available for the first owner who allocates successfully
	// and are not allocatable to other owners anymore.
	// +optional
	AllocateOnce bool `json:"allocateOnce,omitempty"`
}

// ReservationTemplateSpec describes the data a Reservation should have when created from a template
type ReservationTemplateSpec struct {
	// Standard object's metadata.
	// +kubebuilder:pruning:PreserveUnknownFields
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	// Specification of the desired behavior of the Reservation.
	// +optional
	Spec ReservationSpec `json:"spec,omitempty"`
}

type ReservationStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	// The `phase` indicates whether is reservation is waiting for process, available to allocate or failed/expired to
	// get cleanup.
	// +optional
	Phase ReservationPhase `json:"phase,omitempty"`
	// The `conditions` indicate the messages of reason why the reservation is still pending.
	// +optional
	Conditions []ReservationCondition `json:"conditions,omitempty"`
	// Current resource owners which allocated the reservation resources.
	// +optional
	CurrentOwners []corev1.ObjectReference `json:"currentOwners,omitempty"`
	// Name of node the reservation is scheduled on.
	// +optional
	NodeName string `json:"nodeName,omitempty"`
	// Resource reserved and allocatable for owners.
	// +optional
	Allocatable corev1.ResourceList `json:"allocatable,omitempty"`
	// Resource allocated by current owners.
	// +optional
	Allocated corev1.ResourceList `json:"allocated,omitempty"`
}

// ReservationOwner indicates the owner specification which can allocate reserved resources.
// +kubebuilder:validation:MinProperties=1
type ReservationOwner struct {
	// Multiple field selectors are ANDed.
	// +optional
	Object *corev1.ObjectReference `json:"object,omitempty"`
	// +optional
	Controller *ReservationControllerReference `json:"controller,omitempty"`
	// +optional
	LabelSelector *metav1.LabelSelector `json:"labelSelector,omitempty"`
}

type ReservationControllerReference struct {
	// Extend with a `namespace` field for reference different namespaces.
	metav1.OwnerReference `json:",inline"`
	Namespace             string `json:"namespace,omitempty"`
}

type ReservationPhase string

const (
	// ReservationPending indicates the Reservation has not been processed by the scheduler or is unschedulable for
	// some reasons (e.g. the resource requirements cannot get satisfied).
	ReservationPending ReservationPhase = "Pending"
	// ReservationAvailable indicates the Reservation is both scheduled and available for allocation.
	ReservationAvailable ReservationPhase = "Available"
	// ReservationSucceeded indicates the Reservation is scheduled and allocated for a owner, but not allocatable anymore.
	ReservationSucceeded ReservationPhase = "Succeeded"
	// ReservationWaiting indicates the Reservation is scheduled, but the resources to reserve are not ready for
	// allocation (e.g. in pre-allocation for running pods).
	ReservationWaiting ReservationPhase = "Waiting"
	// ReservationFailed indicates the Reservation is failed to reserve resources, due to expiration or marked as
	// unavailable, which the object is not available to allocate and will get cleaned in the future.
	ReservationFailed ReservationPhase = "Failed"
)

type ReservationConditionType string

const (
	ReservationConditionScheduled ReservationConditionType = "Scheduled"
	ReservationConditionReady     ReservationConditionType = "Ready"
)

type ConditionStatus string

const (
	ConditionStatusTrue    ConditionStatus = "True"
	ConditionStatusFalse   ConditionStatus = "False"
	ConditionStatusUnknown ConditionStatus = "Unknown"
)

const (
	ReasonReservationScheduled     = "Scheduled"
	ReasonReservationUnschedulable = "Unschedulable"

	ReasonReservationAvailable = "Available"
	ReasonReservationSucceeded = "Succeeded"
	ReasonReservationExpired   = "Expired"
)

type ReservationCondition struct {
	Type               ReservationConditionType `json:"type,omitempty"`
	Status             ConditionStatus          `json:"status,omitempty"`
	Reason             string                   `json:"reason,omitempty"`
	Message            string                   `json:"message,omitempty"`
	LastProbeTime      metav1.Time              `json:"lastProbeTime,omitempty"`
	LastTransitionTime metav1.Time              `json:"lastTransitionTime,omitempty"`
}

// +genclient
// +genclient:nonNamespaced
// +kubebuilder:resource:scope=Cluster
// +kubebuilder:object:root=true
// +kubebuilder:subresource:status
// +kubebuilder:printcolumn:name="Phase",type="string",JSONPath=".status.phase",description="The phase of reservation"
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
// +kubebuilder:printcolumn:name="Node",type="string",JSONPath=".status.nodeName"
// +kubebuilder:printcolumn:name="TTL",type="string",JSONPath=".spec.ttl"
// +kubebuilder:printcolumn:name="Expires",type="string",JSONPath=".spec.expires"

// Reservation is the Schema for the reservation API.
// A Reservation object is non-namespaced.
// Any namespaced affinity/anti-affinity of reservation scheduling can be specified in the spec.template.
type Reservation struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   ReservationSpec   `json:"spec,omitempty"`
	Status ReservationStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true

// ReservationList contains a list of Reservation
type ReservationList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Reservation `json:"items"`
}

func init() {
	SchemeBuilder.Register(&Reservation{}, &ReservationList{})
}
