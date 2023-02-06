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

// Package k8sapi contains necessary k8s types and objects
package k8sapi

import (
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

const (
	// storage
	PodEmptyDirSizeLimit10Gi    = "10Gi"
	EphemeralStorageSizeRequest = "1Gi"
	EphemeralStorageSizeLimit   = "20Gi"
)

// NamespacePhase describes phase of a namespace
type NamespacePhase string

// FinalizerName is the name identifying a finalizer during namespace lifecycle.
type FinalizerName string

// ServiceType describes ingress methods for a service
type ServiceType string

// ObjectMeta is metadata that all persisted resources must have, which includes all objects users must create
// https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.10/#objectmeta-v1-meta
type ObjectMeta struct {
	Name            string            `json:"name,omitempty"`
	Namespace       string            `json:"namespace,omitempty"`
	Labels          map[string]string `json:"labels,omitempty"`
	ResourceVersion string            `json:"resourceVersion,omitempty"`
}

// Namespace provides a scope for Names.
type Namespace struct {
	// APIVersion defines the versioned schema of this representation of an object
	APIVersion string `json:"apiVersion"`
	// Kind is a string value representing the REST resource this object represents
	Kind string `json:"kind"`
	// Metadata of the namespace
	Metadata ObjectMeta `json:"metadata,omitempty"`
	// Spec defines the behavior of the Namespace.
	Spec NamespaceSpec `json:"spec,omitempty"`
	// Status describes the current status of a Namespace
	Status NamespaceStatus `json:"status,omitempty"`
}

// NamespaceSpec describes the attributes on a Namespace
type NamespaceSpec struct {
	// Finalizers is an opaque list of values that must be empty to permanently remove object from storage
	Finalizers []FinalizerName
}

// NamespaceStatus is information about the current status of a Namespace.
type NamespaceStatus struct {
	// Phase is the current lifecycle phase of the namespace.
	Phase NamespacePhase
}

// DeleteOptions may be provided when deleting an API object
var DeleteOptions = &CascadingDeleteOptions{
	Kind:       "DeleteOptions",
	APIVersion: "v1",
	// 'Foreground' - a cascading policy that deletes all dependents in the foreground
	// e.g. if you delete a deployment, this option would delete related replicaSets and pods
	// See more: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.10/#delete-24
	PropagationPolicy: string(metav1.DeletePropagationBackground),
}

// CascadingDeleteOptions describe the option of cascading deletion
// https://kubernetes.io/docs/concepts/workloads/controllers/garbage-collection/
type CascadingDeleteOptions struct {
	Kind              string `json:"kind"`
	APIVersion        string `json:"apiVersion"`
	PropagationPolicy string `json:"propagationPolicy"`
}

// Deployment includes necessary fields for related k8s deployment
type Deployment struct {
	APIVersion string           `json:"apiVersion"`
	Kind       string           `json:"kind"`
	Metadata   ObjectMeta       `json:"metadata,omitempty"`
	Spec       DeploymentSpec   `json:"spec,omitempty"`
	Status     DeploymentStatus `json:"status,omitempty"`
}

// DeploymentSpec describes specification of the desired behavior of the Deployment.
type DeploymentSpec struct {
	Replicas int32           `json:"replicas,omitempty"`
	Template PodTemplateSpec `json:"template"`
}

// DeploymentStatus is the most recently observed status of the Deployment.
type DeploymentStatus struct {
	Replicas          int32                 `json:"replicas,omitempty"`
	UpdatedReplicas   int32                 `json:"updatedReplicas,omitempty"`
	ReadyReplicas     int32                 `json:"readyReplicas,omitempty"`
	AvailableReplicas int32                 `json:"availableReplicas,omitempty"`
	Conditions        []DeploymentCondition `json:"conditions,omitempty"`
}

// DeploymentCondition describes the state of a deployment at a certain point.
type DeploymentCondition struct {
	Type               string `json:"type,omitempty"`
	Status             string `json:"status,omitempty"`
	LastUpdateTime     string `json:"lastUpdateTime,omitempty"`
	LastTransitionTime string `json:"lastTransitionTime,omitempty"`
	Reason             string `json:"reason,omitempty"`
	Message            string `json:"message,omitempty"`
}

// PodTemplateSpec describes the pods that will be created
type PodTemplateSpec struct {
	Metadata ObjectMeta `json:"metadata,omitempty"`
	Spec     PodSpec    `json:"spec,omitempty"`
}

// PodSpec describes specification of the desired behavior of the pod
type PodSpec struct {
	Containers []Container  `json:"containers"`
	Volumes    []Volume     `json:"volumes,omitempty"`
	Affinity   *v1.Affinity `json:"affinity,omitempty"`
}

// Container represents a single container that is expected to be run on the host.
type Container struct {
	Name           string        `json:"name"`
	Image          string        `json:"image,omitempty"`
	Resources      Resources     `json:"resources,omitempty"`
	Command        []string      `json:"command,omitempty"`
	LivenessProbe  *Probe        `json:"livenessProbe,omitempty"`
	ReadinessProbe *Probe        `json:"readinessProbe,omitempty"`
	Env            []EnvVar      `json:"env,omitempty"`
	VolumeMounts   []VolumeMount `json:"volumeMounts,omitempty"`
}

// Volume represents a named volume in a pod that may be accessed by any container in the pod.
type Volume struct {
	// Volume's name.
	// Must be a DNS_LABEL and unique within the pod.
	// More info: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names
	Name string `json:"name"`

	HostPath *HostPathVolumeSource `json:"hostPath,omitempty"`

	PersistentVolumeClaim *PersistentVolumeClaimVolumeSource `json:"persistentVolumeClaim,omitempty"`
}

// Resources describes the compute resource requirements.
type Resources struct {
	Requests Requests `json:"requests,omitempty"`
	Limits   Limits   `json:"limits,omitempty"`
}

// Probe describes a health check to be performed against a container to determine whether it is
// alive or ready to receive traffic.
type Probe struct {
	// The action taken to determine the health of a container
	Handler `json:",inline"`
	// Number of seconds after the container has started before liveness probes are initiated.
	// More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes
	InitialDelaySeconds int32 `json:"initialDelaySeconds,omitempty"`
	// Number of seconds after which the probe times out.
	// Defaults to 1 second. Minimum value is 1.
	// More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#container-probes
	TimeoutSeconds int32 `json:"timeoutSeconds,omitempty"`
	// How often (in seconds) to perform the probe.
	// Default to 10 seconds. Minimum value is 1.
	PeriodSeconds int32 `json:"periodSeconds,omitempty"`
	// Minimum consecutive successes for the probe to be considered successful after having failed.
	// Defaults to 1. Must be 1 for liveness. Minimum value is 1.
	SuccessThreshold int32 `json:"successThreshold,omitempty"`
	// Minimum consecutive failures for the probe to be considered failed after having succeeded.
	// Defaults to 3. Minimum value is 1.
	FailureThreshold int32 `json:"failureThreshold,omitempty"`
}

// Handler defines a specific action that should be taken
// TODO: pass structured data to these actions, and document that data here.
type Handler struct {
	// One and only one of the following should be specified.
	// Exec specifies the action to take.
	Exec *ExecAction `json:"exec,omitempty"`
	// HTTPGet specifies the http request to perform.
	HTTPGet *HTTPGetAction `json:"httpGet,omitempty"`
	// TCPSocket specifies an action involving a TCP port.
	// TCP hooks not yet supported
	TCPSocket *TCPSocketAction `json:"tcpSocket,omitempty"`
}

// ExecAction describes a "run in container" action.
type ExecAction struct {
	// Command is the command line to execute inside the container, the working directory for the
	// command  is root ('/') in the container's filesystem. The command is simply exec'd, it is
	// not run inside a shell, so traditional shell instructions ('|', etc) won't work. To use
	// a shell, you need to explicitly call out to that shell.
	// Exit status of 0 is treated as live/healthy and non-zero is unhealthy.
	Command []string `json:"command,omitempty"`
}

// HTTPGetAction describes an action based on HTTP Get requests.
type HTTPGetAction struct {
	// Path to access on the HTTP server.
	Path string `json:"path,omitempty"`
	// Name or number of the port to access on the container.
	// Number must be in the range 1 to 65535.
	// Name must be an IANA_SVC_NAME.
	Port int `json:"port"`
	// Host name to connect to, defaults to the pod IP. You probably want to set
	// "Host" in httpHeaders instead.
	Host string `json:"host,omitempty"`
	// Scheme to use for connecting to the host.
	// Defaults to HTTP.
	Scheme URIScheme `json:"scheme,omitempty"`
	// Custom headers to set in the request. HTTP allows repeated headers.
	HTTPHeaders []HTTPHeader `json:"httpHeaders,omitempty"`
}

// URIScheme describes uri scheme
type URIScheme string

// HTTPHeader describes a custom header to be used in HTTP probes
type HTTPHeader struct {
	// The header field name
	Name string `json:"name"`
	// The header field value
	Value string `json:"value"`
}

// TCPSocketAction describes an action based on opening a socket
type TCPSocketAction struct {
	// Number or name of the port to access on the container.
	// Number must be in the range 1 to 65535.
	// Name must be an IANA_SVC_NAME.
	Port int `json:"port"`
	// Optional: Host name to connect to, defaults to the pod IP.
	Host string `json:"host,omitempty"`
}

// Requests describes the minimum amount of compute resources required
type Requests struct {
	CPU     string `json:"cpu,omitempty"`
	Memory  string `json:"memory,omitempty"`
	Storage string `json:"storage,omitempty"`
}

// EnvVar represents an environment variable present in a Container.
type EnvVar struct {
	// Name of the environment variable. Must be a C_IDENTIFIER.
	Name string `json:"name"`

	// Variable references $(VAR_NAME) are expanded
	// using the previous defined environment variables in the container and
	// any service environment variables. If a variable cannot be resolved,
	// the reference in the input string will be unchanged. The $(VAR_NAME)
	// syntax can be escaped with a double $$, ie: $$(VAR_NAME). Escaped
	// references will never be expanded, regardless of whether the variable
	// exists or not.
	// Defaults to "".
	Value string `json:"value,omitempty"`
	// Source for the environment variable's value. Cannot be used if value is not empty.
	//ValueFrom *EnvVarSource `json:"valueFrom,omitempty" protobuf:"bytes,3,opt,name=valueFrom"`
}

// VolumeMount describes a mounting of a Volume within a container.
type VolumeMount struct {
	// This must match the Name of a Volume.
	Name string `json:"name"`
	// Mounted read-only if true, read-write otherwise (false or unspecified).
	// Defaults to false.
	ReadOnly bool `json:"readOnly,omitempty"`
	// Path within the container at which the volume should be mounted.  Must
	// not contain ':'.
	MountPath string `json:"mountPath"`
	// Path within the volume from which the container's volume should be mounted.
	// Defaults to "" (volume's root).
	SubPath string `json:"subPath,omitempty"`
}

// HostPathVolumeSource represents a host path mapped into a pod.
// Host path volumes do not support ownership management or SELinux relabeling.
type HostPathVolumeSource struct {
	// Path of the directory on the host.
	// If the path is a symlink, it will follow the link to the real path.
	// More info: https://kubernetes.io/docs/concepts/storage/volumes#hostpath
	Path string `json:"path"`
	// Type for HostPath Volume
	// Defaults to ""
	//Type *HostPathType `json:"type,omitempty" protobuf:"bytes,2,opt,name=type"`
}

// PersistentVolumeClaimVolumeSource references the user's PVC in the same namespace.
// This volume finds the bound PV and mounts that volume for the pod. A
// PersistentVolumeClaimVolumeSource is, essentially, a wrapper around another
// type of volume that is owned by someone else (the system).
type PersistentVolumeClaimVolumeSource struct {
	// ClaimName is the name of a PersistentVolumeClaim in the same namespace as the pod using this volume.
	ClaimName string `json:"claimName"`
	// Will force the ReadOnly setting in VolumeMounts.
	// Default false.
	ReadOnly bool `json:"readOnly,omitempty"`
}

// Limits describes the maximum amount of compute resources allowed.
type Limits struct {
	CPU    string `json:"cpu,omitempty"`
	Memory string `json:"memory,omitempty"`
}

// Service is a named abstraction of software service (for example, mysql) consisting of local port
// (for example 3306) that the proxy listens on, and the selector that determines which pods
// will answer requests sent through the proxy.
type Service struct {
	APIVersion string        `json:"apiVersion"`
	Kind       string        `json:"kind"`
	Metadata   ObjectMeta    `json:"metadata,omitempty"`
	Spec       ServiceSpec   `json:"spec"`
	Status     ServiceStatus `json:"status,omitempty"`
}

// ServiceSpec describes the attributes that a user creates on a service.
type ServiceSpec struct {
	ClusterIP string            `json:"clusterIP,omitempty"`
	Type      ServiceType       `json:"type,omitempty"`
	Selector  map[string]string `json:"selector,omitempty"`
	Ports     []ServicePort     `json:"ports,omitempty"`
}

// ServicePort contains information on service's port.
type ServicePort struct {
	Name       string      `json:"name"`
	Port       int32       `json:"port"`
	TargetPort interface{} `json:"targetPort,omitempty"`
}

// ServiceStatus represents the current status of a service.
type ServiceStatus struct {
	LoadBalancer LoadBalancerStatus `json:"loadBalancer,omitempty"`
}

// LoadBalancerStatus represents the status of a load-balancer
type LoadBalancerStatus struct {
	Ingress []LoadBalancerIngress `json:"ingress,omitempty"`
}

// LoadBalancerIngress represents the status of a load-balancer ingress point:
// traffic intended for the service should be sent to an ingress point.
type LoadBalancerIngress struct {
	IP       string `json:"ip,omitempty"`
	Hostname string `json:"hostname,omitempty"`
}

// DeploymentList is a list of Deployments.
type DeploymentList struct {
	APIVersion string       `json:"apiVersion"`
	Kind       string       `json:"kind"`
	Items      []Deployment `json:"items"`
}

// Ingress is a collection of rules that allow inbound connections to reach the
// endpoints defined by a backend. An Ingress can be configured to give services
// externally-reachable urls, load balance traffic, terminate SSL, offer name
// based virtual hosting etc.
type Ingress struct {
	APIVersion string        `json:"apiVersion"`
	Kind       string        `json:"kind"`
	Metadata   ObjectMeta    `json:"metadata,omitempty"`
	Spec       IngressSpec   `json:"spec,omitempty"`
	Status     IngressStatus `json:"status,omitempty"`
}

// IngressSpec describes the specification of ingress
type IngressSpec struct {
	Backend *IngressBackend `json:"backend,omitempty"`
	Rules   []IngressRule   `json:"rules,omitempty"`
	TLS     []IngressTLS    `json:"tls,omitempty" protobuf:"bytes,2,rep,name=tls"`
}

// IngressTLS describes the transport layer security associated with an Ingress.
type IngressTLS struct {
	// Hosts are a list of hosts included in the TLS certificate. The values in
	// this list must match the name/s used in the tlsSecret. Defaults to the
	// wildcard host setting for the loadbalancer controller fulfilling this
	// Ingress, if left unspecified.
	// +optional
	Hosts []string `json:"hosts,omitempty" protobuf:"bytes,1,rep,name=hosts"`
	// SecretName is the name of the secret used to terminate SSL traffic on 443.
	// Field is left optional to allow SSL routing based on SNI hostname alone.
	// If the SNI host in a listener conflicts with the "Host" header field used
	// by an IngressRule, the SNI host is used for termination and value of the
	// Host header is used for routing.
	// +optional
	SecretName string `json:"secretName,omitempty" protobuf:"bytes,2,opt,name=secretName"`
	// TODO: Consider specifying different modes of termination, protocols etc.
}

// IngressBackend describes all endpoints for a given service and port.
type IngressBackend struct {
	ServiceName string `json:"serviceName,omitempty"`
	ServicePort int    `json:"servicePort,omitempty"`
}

// IngressRule represents the rules mapping the paths under a specified host to
// the related backend services. Incoming requests are first evaluated for a host
// match, then routed to the backend associated with the matching IngressRuleValue.
type IngressRule struct {
	Host string                `json:"host,omitempty"`
	HTTP *HTTPIngressRuleValue `json:"http,omitempty"`
}

// HTTPIngressRuleValue is a list of http selectors pointing to backends.
// In the example: http://<host>/<path>?<searchpart> -> backend where
// where parts of the url correspond to RFC 3986, this resource will be used
// to match against everything after the last '/' and before the first '?'
// or '#'.
type HTTPIngressRuleValue struct {
	Paths []HTTPIngressPath `json:"paths,omitempty"`
}

// HTTPIngressPath associates a path regex with a backend. Incoming urls matching
// the path are forwarded to the backend.
type HTTPIngressPath struct {
	// Path is an extended POSIX regex as defined by IEEE Std 1003.1,
	// (i.e this follows the egrep/unix syntax, not the perl syntax)
	// matched against the path of an incoming request. Currently it can
	// contain characters disallowed from the conventional "path"
	// part of a URL as defined by RFC 3986. Paths must begin with
	// a '/'. If unspecified, the path defaults to a catch all sending
	// traffic to the backend.
	Path string `json:"path,omitempty"`

	// Backend defines the referenced service endpoint to which the traffic
	// will be forwarded to.
	Backend IngressBackend `json:"backend,omitempty"`
}

// IngressStatus describe the current state of the Ingress.
type IngressStatus struct {
	// LoadBalancer contains the current status of the load-balancer.
	LoadBalancer LoadBalancerStatus `json:"loadBalancer,omitempty"`
}

// PodStatus describes most recently observed status of the pod.
type PodStatus struct {
	Phase string `json:"phase,omitempty"`
	PodIP string `json:"podIP,omitempty"`
}

// PersistentVolumeClaim is a user's request for and claim to a persistent volume
type PersistentVolumeClaim struct {
	APIVersion string     `json:"apiVersion"`
	Kind       string     `json:"kind"`
	Metadata   ObjectMeta `json:"metadata,omitempty"`

	// Spec defines the desired characteristics of a volume requested by a pod author.
	Spec PersistentVolumeClaimSpec `json:"spec,omitempty"`

	// Status represents the current information/status of a persistent volume claim.
	// Read-only.
	Status PersistentVolumeClaimStatus `json:"status,omitempty"`
}

// PersistentVolumeClaimSpec describes the common attributes of storage devices
// and allows a Source for provider-specific attributes
type PersistentVolumeClaimSpec struct {
	// AccessModes contains the desired access modes the volume should have.
	AccessModes []PersistentVolumeAccessMode `json:"accessModes,omitempty"`
	// A label query over volumes to consider for binding.
	//Selector *metav1.LabelSelector `json:"selector,omitempty"`
	// Resources represents the minimum resources the volume should have.
	Resources Resources `json:"resources,omitempty"`
	// VolumeName is the binding reference to the PersistentVolume backing this claim.
	//VolumeName string `json:"volumeName,omitempty"`
	// Name of the StorageClass required by the claim.
	StorageClassName *string `json:"storageClassName,omitempty"`
	// volumeMode defines what type of volume is required by the claim.
	//VolumeMode *PersistentVolumeMode `json:"volumeMode,omitempty"`
}

// PersistentVolumeAccessMode defines the PV access mode
type PersistentVolumeAccessMode string

// PersistentVolumeClaimStatus is the current status of a persistent volume claim.
type PersistentVolumeClaimStatus struct {
	// Phase represents the current phase of PersistentVolumeClaim.
	Phase PersistentVolumeClaimPhase `json:"phase,omitempty"`
	// AccessModes contains the actual access modes the volume backing the PVC has.
	//AccessModes []PersistentVolumeAccessMode `json:"accessModes,omitempty"`
	// Represents the actual resources of the underlying volume.
	//Capacity ResourceList `json:"capacity,omitempty"`
}

// PersistentVolumeClaimPhase represents the current phase of PersistentVolumeClaim
type PersistentVolumeClaimPhase string

// ServiceList holds a list of services.
type ServiceList struct {
	Kind       string    `json:"kind"`
	APIVersion string    `json:"apiVersion"`
	Items      []Service `json:"items"`
}

// PodList describes the list of pods
type PodList struct {
	Items []PodItem `json:"items,omitempty"`
}

// PodItem used by edas executor
type PodItem struct {
	Metadata ObjectMeta `json:"metadata,omitempty"`
	Status   PodStatus  `json:"status,omitempty"`
}

// These are valid conditions of a deployment.
const (
	// Available means the deployment is available, ie. at least the minimum available
	// replicas required are up and running for at least minReadySeconds.
	DeploymentAvailable = "Available"
	// Progressing means the deployment is progressing. Progress for a deployment is
	// considered when a new replica set is created or adopted, and when new pods scale
	// up or old pods scale down. Progress is not estimated for paused deployments or
	// when progressDeadlineSeconds is not specified.
	DeploymentProgressing = "Progressing"
	// ReplicaFailure is added in a deployment when one of its pods fails to be created
	// or deleted.
	DeploymentReplicaFailure = "ReplicaFailure"
)

const (
	// ServiceTypeClusterIP means a service will only be accessible inside the
	// cluster, via the cluster IP.
	ServiceTypeClusterIP ServiceType = "ClusterIP"

	// ServiceTypeNodePort means a service will be exposed on one port of
	// every node, in addition to 'ClusterIP' type.
	ServiceTypeNodePort ServiceType = "NodePort"

	// ServiceTypeLoadBalancer means a service will be exposed via an
	// external load balancer (if the cloud provider supports it), in addition
	// to 'NodePort' type.
	ServiceTypeLoadBalancer ServiceType = "LoadBalancer"

	// ServiceTypeExternalName means a service consists of only a reference to
	// an external name that kubedns or equivalent will return as a CNAME
	// record, with no exposing or proxying of any pods involved.
	ServiceTypeExternalName ServiceType = "ExternalName"
)
