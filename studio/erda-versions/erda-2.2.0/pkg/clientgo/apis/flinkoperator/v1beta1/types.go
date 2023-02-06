/*
Copyright 2019 Google LLC.

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

package v1beta1

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// ClusterState defines states for a cluster.
const (
	ClusterStateCreating         = "Creating"
	ClusterStateRunning          = "Running"
	ClusterStateReconciling      = "Reconciling"
	ClusterStateUpdating         = "Updating"
	ClusterStateStopping         = "Stopping"
	ClusterStatePartiallyStopped = "PartiallyStopped"
	ClusterStateStopped          = "Stopped"
)

// ComponentState defines states for a cluster component.
const (
	ComponentStateNotReady = "NotReady"
	ComponentStateReady    = "Ready"
	ComponentStateUpdating = "Updating"
	ComponentStateDeleted  = "Deleted"
)

// JobState defines states for a Flink job.
const (
	JobStatePending   = "Pending"
	JobStateRunning   = "Running"
	JobStateUpdating  = "Updating"
	JobStateSucceeded = "Succeeded"
	JobStateFailed    = "Failed"
	JobStateCancelled = "Cancelled"
	JobStateUnknown   = "Unknown"
)

// AccessScope defines the access scope of JobManager service.
const (
	AccessScopeCluster  = "Cluster"
	AccessScopeVPC      = "VPC"
	AccessScopeExternal = "External"
	AccessScopeNodePort = "NodePort"
	AccessScopeHeadless = "Headless"
)

// JobRestartPolicy defines the restart policy when a job fails.
type JobRestartPolicy string

const (
	// JobRestartPolicyNever - never restarts a failed job.
	JobRestartPolicyNever JobRestartPolicy = "Never"

	// JobRestartPolicyFromSavepointOnFailure - restart the job from the latest
	// savepoint if available, otherwise do not restart.
	JobRestartPolicyFromSavepointOnFailure JobRestartPolicy = "FromSavepointOnFailure"
)

// User requested control
const (
	// control annotation key
	ControlAnnotation = "flinkclusters.flinkoperator.k8s.io/user-control"

	// control name
	ControlNameSavepoint = "savepoint"
	ControlNameJobCancel = "job-cancel"

	// control state
	ControlStateProgressing = "Progressing"
	ControlStateSucceeded   = "Succeeded"
	ControlStateFailed      = "Failed"
)

// Savepoint status
const (
	SavepointStateNotTriggered  = "NotTriggered"
	SavepointStateInProgress    = "InProgress"
	SavepointStateTriggerFailed = "TriggerFailed"
	SavepointStateFailed        = "Failed"
	SavepointStateSucceeded     = "Succeeded"

	SavepointTriggerReasonUserRequested = "user requested"
	SavepointTriggerReasonScheduled     = "scheduled"
	SavepointTriggerReasonJobCancel     = "job cancel"
	SavepointTriggerReasonUpdate        = "update"
)

// FlinkCluster is the Schema for the flinkclusters API
// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type FlinkCluster struct {
	metav1.TypeMeta `json:",inline"`
	// +optional
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   FlinkClusterSpec   `json:"spec"`
	Status FlinkClusterStatus `json:"status,omitempty"`
}

// FlinkClusterList contains a list of FlinkCluster
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type FlinkClusterList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []FlinkCluster `json:"items"`
}

// ImageSpec defines Flink image of JobManager and TaskManager containers.
type ImageSpec struct {
	// Flink image name.
	Name string `json:"name"`

	// Image pull policy. One of Always, Never, IfNotPresent. Defaults to Always
	// if :latest tag is specified, or IfNotPresent otherwise.
	PullPolicy corev1.PullPolicy `json:"pullPolicy,omitempty"`

	// Secrets for image pull.
	PullSecrets []corev1.LocalObjectReference `json:"pullSecrets,omitempty"`
}

// NamedPort defines the container port properties.
type NamedPort struct {
	// If specified, this must be an IANA_SVC_NAME and unique within the pod. Each
	// named port in a pod must have a unique name. Name for the port that can be
	// referred to by services.
	Name string `json:"name,omitempty"`

	// Number of port to expose on the pod's IP address.
	// This must be a valid port number, 0 < x < 65536.
	// +kubebuilder:validation:Minimum=1
	// +kubebuilder:validation:Maximum=65535
	ContainerPort int32 `json:"containerPort"`

	// Protocol for port. Must be UDP, TCP, or SCTP.
	// Defaults to "TCP".
	// +kubebuilder:validation:Enum=TCP;UDP;SCTP
	Protocol string `json:"protocol,omitempty"`
}

// JobManagerPorts defines ports of JobManager.
type JobManagerPorts struct {
	// RPC port, default: 6123.
	RPC *int32 `json:"rpc,omitempty"`

	// Blob port, default: 6124.
	Blob *int32 `json:"blob,omitempty"`

	// Query port, default: 6125.
	Query *int32 `json:"query,omitempty"`

	// UI port, default: 8081.
	UI *int32 `json:"ui,omitempty"`
}

// JobManagerIngressSpec defines ingress of JobManager
type JobManagerIngressSpec struct {
	// Ingress host format. ex) {{$clusterName}}.example.com
	HostFormat *string `json:"hostFormat,omitempty"`

	// Ingress annotations.
	Annotations map[string]string `json:"annotations,omitempty"`

	// TLS use.
	UseTLS *bool `json:"useTls,omitempty"`

	// TLS secret name.
	TLSSecretName *string `json:"tlsSecretName,omitempty"`
}

// JobManagerSpec defines properties of JobManager.
type JobManagerSpec struct {
	// The number of replicas.
	Replicas *int32 `json:"replicas,omitempty"`

	// Access scope, enum("Cluster", "VPC", "External").
	AccessScope string `json:"accessScope"`

	// (Optional) Ingress.
	Ingress *JobManagerIngressSpec `json:"ingress,omitempty"`

	// Ports.
	Ports JobManagerPorts `json:"ports,omitempty"`

	// Extra ports to be exposed. For example, Flink metrics reporter ports: Prometheus, JMX and so on.
	ExtraPorts []NamedPort `json:"extraPorts,omitempty"`

	// Compute resources required by each JobManager container.
	// If omitted, a default value will be used.
	// Cannot be updated.
	// More info: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`

	// TODO: Memory calculation would be change. Let's watch the issue FLINK-13980.

	// Percentage of off-heap memory in containers, as a safety margin to avoid OOM kill, default: 25
	MemoryOffHeapRatio *int32 `json:"memoryOffHeapRatio,omitempty"`

	// Minimum amount of off-heap memory in containers, as a safety margin to avoid OOM kill, default: 600M
	// You can express this value like 600M, 572Mi and 600e6
	// More info: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/#meaning-of-memory
	MemoryOffHeapMin resource.Quantity `json:"memoryOffHeapMin,omitempty"`

	// Volumes in the JobManager pod.
	Volumes []corev1.Volume `json:"volumes,omitempty"`

	// Volume mounts in the JobManager container.
	VolumeMounts []corev1.VolumeMount `json:"volumeMounts,omitempty"`

	// Init containers of the Job Manager pod.
	InitContainers []corev1.Container `json:"initContainers,omitempty"`

	// Selector which must match a node's labels for the JobManager pod to be
	// scheduled on that node.
	// More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`

	// Defines the node affinity of the pod
	// More info: https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/
	Tolerations []corev1.Toleration `json:"tolerations,omitempty"`

	// Sidecar containers running alongside with the JobManager container in the
	// pod.
	Sidecars []corev1.Container `json:"sidecars,omitempty"`

	// JobManager Deployment pod template annotations.
	PodAnnotations map[string]string `json:"podAnnotations,omitempty"`
}

// TaskManagerPorts defines ports of TaskManager.
type TaskManagerPorts struct {
	// Data port, default: 6121.
	Data *int32 `json:"data,omitempty"`

	// RPC port, default: 6122.
	RPC *int32 `json:"rpc,omitempty"`

	// Query port.
	Query *int32 `json:"query,omitempty"`
}

// TaskManagerSpec defines properties of TaskManager.
type TaskManagerSpec struct {
	// The number of replicas.
	Replicas int32 `json:"replicas"`

	// Ports.
	Ports TaskManagerPorts `json:"ports,omitempty"`

	// Extra ports to be exposed. For example, Flink metrics reporter ports: Prometheus, JMX and so on.
	ExtraPorts []NamedPort `json:"extraPorts,omitempty"`

	// Compute resources required by each TaskManager container.
	// If omitted, a default value will be used.
	// Cannot be updated.
	// More info: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`

	// TODO: Memory calculation would be change. Let's watch the issue FLINK-13980.

	// Percentage of off-heap memory in containers, as a safety margin to avoid OOM kill, default: 25
	MemoryOffHeapRatio *int32 `json:"memoryOffHeapRatio,omitempty"`

	// Minimum amount of off-heap memory in containers, as a safety margin to avoid OOM kill, default: 600M
	// You can express this value like 600M, 572Mi and 600e6
	// More info: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/#meaning-of-memory
	MemoryOffHeapMin resource.Quantity `json:"memoryOffHeapMin,omitempty"`

	// Volumes in the TaskManager pods.
	// More info: https://kubernetes.io/docs/concepts/storage/volumes/
	Volumes []corev1.Volume `json:"volumes,omitempty"`

	// Volume mounts in the TaskManager containers.
	// More info: https://kubernetes.io/docs/concepts/storage/volumes/
	VolumeMounts []corev1.VolumeMount `json:"volumeMounts,omitempty"`

	// Init containers of the Task Manager pod.
	InitContainers []corev1.Container `json:"initContainers,omitempty"`

	// Selector which must match a node's labels for the TaskManager pod to be
	// scheduled on that node.
	// More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/
	NodeSelector map[string]string `json:"nodeSelector,omitempty"`

	// Defines the node affinity of the pod
	// More info: https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/
	Tolerations []corev1.Toleration `json:"tolerations,omitempty"`

	// Sidecar containers running alongside with the TaskManager container in the
	// pod.
	Sidecars []corev1.Container `json:"sidecars,omitempty"`

	// TaskManager Deployment pod template annotations.
	PodAnnotations map[string]string `json:"podAnnotations,omitempty"`
}

// CleanupAction defines the action to take after job finishes.
type CleanupAction string

const (
	// CleanupActionKeepCluster - keep the entire cluster.
	CleanupActionKeepCluster = "KeepCluster"
	// CleanupActionDeleteCluster - delete the entire cluster.
	CleanupActionDeleteCluster = "DeleteCluster"
	// CleanupActionDeleteTaskManager - delete task manager, keep job manager.
	CleanupActionDeleteTaskManager = "DeleteTaskManager"
)

// CleanupPolicy defines the action to take after job finishes.
type CleanupPolicy struct {
	// Action to take after job succeeds.
	AfterJobSucceeds CleanupAction `json:"afterJobSucceeds,omitempty"`
	// Action to take after job fails.
	AfterJobFails CleanupAction `json:"afterJobFails,omitempty"`
	// Action to take after job is cancelled.
	AfterJobCancelled CleanupAction `json:"afterJobCancelled,omitempty"`
}

// JobSpec defines properties of a Flink job.
type JobSpec struct {
	// JAR file of the job.
	JarFile string `json:"jarFile"`

	// Fully qualified Java class name of the job.
	ClassName *string `json:"className,omitempty"`

	// Args of the job.
	Args []string `json:"args,omitempty"`

	// FromSavepoint where to restore the job from (e.g., gs://my-savepoint/1234).
	FromSavepoint *string `json:"fromSavepoint,omitempty"`

	// Allow non-restored state, default: false.
	AllowNonRestoredState *bool `json:"allowNonRestoredState,omitempty"`

	// Savepoints dir where to store savepoints of the job.
	SavepointsDir *string `json:"savepointsDir,omitempty"`

	// Automatically take a savepoint to the `savepointsDir` every n seconds.
	AutoSavepointSeconds *int32 `json:"autoSavepointSeconds,omitempty"`

	// Update this field to `jobStatus.savepointGeneration + 1` for a running job
	// cluster to trigger a new savepoint to `savepointsDir` on demand.
	SavepointGeneration int32 `json:"savepointGeneration,omitempty"`

	// Job parallelism, default: 1.
	Parallelism *int32 `json:"parallelism,omitempty"`

	// No logging output to STDOUT, default: false.
	NoLoggingToStdout *bool `json:"noLoggingToStdout,omitempty"`

	// Volumes in the Job pod.
	// More info: https://kubernetes.io/docs/concepts/storage/volumes/
	Volumes []corev1.Volume `json:"volumes,omitempty"`

	// Volume mounts in the Job container.
	// More info: https://kubernetes.io/docs/concepts/storage/volumes/
	VolumeMounts []corev1.VolumeMount `json:"volumeMounts,omitempty"`

	// Init containers of the Job pod. A typical use case could be using an init
	// container to download a remote job jar to a local path which is
	// referenced by the `jarFile` property.
	// More info: https://kubernetes.io/docs/concepts/workloads/pods/init-containers/
	InitContainers []corev1.Container `json:"initContainers,omitempty"`

	// Restart policy when the job fails, "Never" or "FromSavepointOnFailure",
	// default: "Never".
	//
	// "Never" means the operator will never try to restart a failed job, manual
	// cleanup and restart is required.
	//
	// "FromSavepointOnFailure" means the operator will try to restart the failed
	// job from the savepoint recorded in the job status if available; otherwise,
	// the job will stay in failed state. This option is usually used together
	// with `autoSavepointSeconds` and `savepointsDir`.
	RestartPolicy *JobRestartPolicy `json:"restartPolicy"`

	// The action to take after job finishes.
	CleanupPolicy *CleanupPolicy `json:"cleanupPolicy,omitempty"`

	// Request the job to be cancelled. Only applies to running jobs. If
	// `savePointsDir` is provided, a savepoint will be taken before stopping the
	// job.
	CancelRequested *bool `json:"cancelRequested,omitempty"`

	// Job pod template annotations.
	PodAnnotations map[string]string `json:"podAnnotations,omitempty"`

	// Compute resources required by each Job container.
	// If omitted, a default value will be used.
	// Cannot be updated.
	// More info: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/
	Resources corev1.ResourceRequirements `json:"resources,omitempty"`
}

// FlinkClusterSpec defines the desired state of FlinkCluster
type FlinkClusterSpec struct {
	// Flink image spec for the cluster's components.
	Image ImageSpec `json:"image"`

	// BatchSchedulerName specifies the batch scheduler name for JobManager, TaskManager.
	// If empty, no batch scheduling is enabled.
	BatchSchedulerName *string `json:"batchSchedulerName,omitempty"`

	// Flink JobManager spec.
	JobManager JobManagerSpec `json:"jobManager"`

	// Flink TaskManager spec.
	TaskManager TaskManagerSpec `json:"taskManager"`

	// (Optional) Job spec. If specified, this cluster is an ephemeral Job
	// Cluster, which will be automatically terminated after the job finishes;
	// otherwise, it is a long-running Session Cluster.
	Job *JobSpec `json:"job,omitempty"`

	// Environment variables shared by all JobManager, TaskManager and job
	// containers.
	EnvVars []corev1.EnvVar `json:"envVars,omitempty"`

	// Environment variables injected from a source, shared by all JobManager,
	// TaskManager and job containers.
	EnvFrom []corev1.EnvFromSource `json:"envFrom,omitempty"`

	// Flink properties which are appened to flink-conf.yaml.
	FlinkProperties map[string]string `json:"flinkProperties,omitempty"`

	// Config for Hadoop.
	HadoopConfig *HadoopConfig `json:"hadoopConfig,omitempty"`

	// Config for GCP.
	GCPConfig *GCPConfig `json:"gcpConfig,omitempty"`

	// The logging configuration, which should have keys 'log4j-console.properties' and 'logback-console.xml'.
	// These will end up in the 'flink-config-volume' ConfigMap, which gets mounted at /opt/flink/conf.
	// If not provided, defaults that log to console only will be used.
	LogConfig map[string]string `json:"logConfig,omitempty"`

	// The maximum number of revision history to keep, default: 10.
	RevisionHistoryLimit *int32 `json:"revisionHistoryLimit,omitempty"`
}

// HadoopConfig defines configs for Hadoop.
type HadoopConfig struct {
	// The name of the ConfigMap which contains the Hadoop config files.
	// The ConfigMap must be in the same namespace as the FlinkCluster.
	ConfigMapName string `json:"configMapName,omitempty"`

	// The path where to mount the Volume of the ConfigMap.
	MountPath string `json:"mountPath,omitempty"`
}

// GCPConfig defines configs for GCP.
type GCPConfig struct {
	// GCP service account.
	ServiceAccount *GCPServiceAccount `json:"serviceAccount,omitempty"`
}

// GCPServiceAccount defines the config about GCP service account.
type GCPServiceAccount struct {
	// The name of the Secret holding the GCP service account key file.
	// The Secret must be in the same namespace as the FlinkCluster.
	SecretName string `json:"secretName,omitempty"`

	// The name of the service account key file.
	KeyFile string `json:"keyFile,omitempty"`

	// The path where to mount the Volume of the Secret.
	MountPath string `json:"mountPath,omitempty"`
}

// FlinkClusterComponentState defines the observed state of a component
// of a FlinkCluster.
type FlinkClusterComponentState struct {
	// The resource name of the component.
	Name string `json:"name"`

	// The state of the component.
	State string `json:"state"`
}

// FlinkClusterComponentsStatus defines the observed status of the
// components of a FlinkCluster.
type FlinkClusterComponentsStatus struct {
	// The state of configMap.
	ConfigMap FlinkClusterComponentState `json:"configMap"`

	// The state of JobManager deployment.
	JobManagerDeployment FlinkClusterComponentState `json:"jobManagerDeployment"`

	// The state of JobManager service.
	JobManagerService JobManagerServiceStatus `json:"jobManagerService"`

	// The state of JobManager ingress.
	JobManagerIngress *JobManagerIngressStatus `json:"jobManagerIngress,omitempty"`

	// The state of TaskManager deployment.
	TaskManagerDeployment FlinkClusterComponentState `json:"taskManagerDeployment"`

	// The status of the job, available only when JobSpec is provided.
	Job *JobStatus `json:"job,omitempty"`
}

// Control state
type FlinkClusterControlStatus struct {
	// Control name
	Name string `json:"name"`

	// Control data
	Details map[string]string `json:"details,omitempty"`

	// State
	State string `json:"state"`

	// Message
	Message string `json:"message,omitempty"`

	// State update time
	UpdateTime string `json:"updateTime"`
}

// JobStatus defines the status of a job.
type JobStatus struct {
	// The name of the Kubernetes job resource.
	Name string `json:"name"`

	// The ID of the Flink job.
	ID string `json:"id"`

	// The state of the Kubernetes job.
	State string `json:"state"`

	// The actual savepoint from which this job started.
	// In case of restart, it might be different from the savepoint in the job
	// spec.
	FromSavepoint string `json:"fromSavepoint,omitempty"`

	// The generation of the savepoint in `savepointsDir` taken by the operator.
	// The value starts from 0 when there is no savepoint and increases by 1 for
	// each successful savepoint.
	SavepointGeneration int32 `json:"savepointGeneration,omitempty"`

	// Savepoint location.
	SavepointLocation string `json:"savepointLocation,omitempty"`

	// Last savepoint trigger ID.
	LastSavepointTriggerID string `json:"lastSavepointTriggerID,omitempty"`

	// Last successful or failed savepoint operation timestamp.
	LastSavepointTime string `json:"lastSavepointTime,omitempty"`

	// The number of restarts.
	RestartCount int32 `json:"restartCount,omitempty"`
}

// SavepointStatus defines the status of savepoint progress
type SavepointStatus struct {
	// The ID of the Flink job.
	JobID string `json:"jobID,omitempty"`

	// Savepoint trigger ID.
	TriggerID string `json:"triggerID,omitempty"`

	// Savepoint triggered time.
	TriggerTime string `json:"triggerTime,omitempty"`

	// Savepoint triggered reason.
	TriggerReason string `json:"triggerReason,omitempty"`

	// Savepoint requested time.
	RequestTime string `json:"requestTime,omitempty"`

	// Savepoint state.
	State string `json:"state"`

	// Savepoint message.
	Message string `json:"message,omitempty"`
}

// JobManagerIngressStatus defines the status of a JobManager ingress.
type JobManagerIngressStatus struct {
	// The name of the Kubernetes ingress resource.
	Name string `json:"name"`

	// The state of the component.
	State string `json:"state"`

	// The URLs of ingress.
	URLs []string `json:"urls,omitempty"`
}

// JobManagerServiceStatus defines the observed state of FlinkCluster
type JobManagerServiceStatus struct {
	// The name of the Kubernetes jobManager service.
	Name string `json:"name"`

	// The state of the component.
	State string `json:"state"`

	// (Optional) The node port, present when `accessScope` is `NodePort`.
	NodePort int32 `json:"nodePort,omitempty"`
}

// FlinkClusterStatus defines the observed state of FlinkCluster
type FlinkClusterStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	// The overall state of the Flink cluster.
	State string `json:"state"`

	// The status of the components.
	Components FlinkClusterComponentsStatus `json:"components"`

	// The status of control requested by user
	Control *FlinkClusterControlStatus `json:"control,omitempty"`

	// The status of savepoint progress
	Savepoint *SavepointStatus `json:"savepoint,omitempty"`

	// When the controller creates new ControllerRevision, it generates hash string from the FlinkCluster spec
	// which is to be stored in ControllerRevision and uses it to compose the ControllerRevision name.
	// Then the controller updates nextRevision to the ControllerRevision name.
	// When update process is completed, the controller updates currentRevision as nextRevision.
	// currentRevision and nextRevision is composed like this:
	// <FLINK_CLUSTER_NAME>-<FLINK_CLUSTER_SPEC_HASH>-<REVISION_NUMBER_IN_CONTROLLERREVISION>
	// e.g., myflinkcluster-c464ff7-5

	// CurrentRevision indicates the version of FlinkCluster.
	CurrentRevision string `json:"currentRevision,omitempty"`

	// NextRevision indicates the version of FlinkCluster updating.
	NextRevision string `json:"nextRevision,omitempty"`

	// collisionCount is the count of hash collisions for the FlinkCluster. The controller
	// uses this field as a collision avoidance mechanism when it needs to create the name for the
	// newest ControllerRevision.
	CollisionCount *int32 `json:"collisionCount,omitempty"`

	// Last update timestamp for this status.
	LastUpdateTime string `json:"lastUpdateTime,omitempty"`
}
