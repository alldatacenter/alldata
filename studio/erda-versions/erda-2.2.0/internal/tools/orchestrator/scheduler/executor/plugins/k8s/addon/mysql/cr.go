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

package mysql

import (
	"github.com/appscode/go/encoding/json/types"
	apps "k8s.io/api/apps/v1"
	core "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	mona "kmodules.xyz/monitoring-agent-api/api/v1"
	store "kmodules.xyz/objectstore-api/api/v1"
	ofst "kmodules.xyz/offshoot-api/api/v1"
)

const (
	ResourceCodeMySQL     = "my"
	ResourceKindMySQL     = "MySQL"
	ResourceSingularMySQL = "mysql"
	ResourcePluralMySQL   = "mysqls"
)

type MySQLClusterMode string

const (
	MySQLClusterModeGroup MySQLClusterMode = "GroupReplication"
)

type MySQLGroupMode string

const (
	MySQLGroupModeSinglePrimary MySQLGroupMode = "Single-Primary"
	MySQLGroupModeMultiPrimary  MySQLGroupMode = "Multi-Primary"
)

// +genclient
// +k8s:openapi-gen=true
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// Mysql defines a Mysql database.
type MySQL struct {
	metav1.TypeMeta   `json:",inline,omitempty"`
	metav1.ObjectMeta `json:"metadata,omitempty"`
	Spec              MySQLSpec   `json:"spec,omitempty"`
	Status            MySQLStatus `json:"status,omitempty"`
}

type MySQLSpec struct {
	// Version of MySQL to be deployed.
	Version types.StrYo `json:"version"`

	// Number of instances to deploy for a MySQL database. In case of MySQL group
	// replication, max allowed value is 9 (default 3).
	// (see ref: https://dev.mysql.com/doc/refman/5.7/en/group-replication-frequently-asked-questions.html)
	Replicas *int32 `json:"replicas,omitempty"`

	// MySQL cluster topology
	Topology *MySQLClusterTopology `json:"topology,omitempty"`

	// StorageType can be durable (default) or ephemeral
	StorageType StorageType `json:"storageType,omitempty"`

	// Storage spec to specify how storage shall be used.
	Storage *core.PersistentVolumeClaimSpec `json:"storage,omitempty"`

	// Database authentication secret
	DatabaseSecret *core.SecretVolumeSource `json:"databaseSecret,omitempty"`

	// Init is used to initialize database
	// +optional
	Init *InitSpec `json:"init,omitempty"`

	// BackupSchedule spec to specify how database backup will be taken
	// +optional
	BackupSchedule *BackupScheduleSpec `json:"backupSchedule,omitempty"`

	// Monitor is used monitor database instance
	// +optional
	Monitor *mona.AgentSpec `json:"monitor,omitempty"`

	// ConfigSource is an optional field to provide custom configuration file for database (i.e custom-mysql.cnf).
	// If specified, this file will be used as configuration file otherwise default configuration file will be used.
	ConfigSource *core.VolumeSource `json:"configSource,omitempty"`

	// PodTemplate is an optional configuration for pods used to expose database
	// +optional
	PodTemplate ofst.PodTemplateSpec `json:"podTemplate,omitempty"`

	// ServiceTemplate is an optional configuration for service used to expose database
	// +optional
	ServiceTemplate ofst.ServiceTemplateSpec `json:"serviceTemplate,omitempty"`

	// updateStrategy indicates the StatefulSetUpdateStrategy that will be
	// employed to update Pods in the StatefulSet when a revision is made to
	// Template.
	UpdateStrategy apps.StatefulSetUpdateStrategy `json:"updateStrategy,omitempty"`

	// TerminationPolicy controls the delete operation for database
	// +optional
	TerminationPolicy TerminationPolicy `json:"terminationPolicy,omitempty"`
}

type MySQLClusterTopology struct {
	// If set to -
	// "GroupReplication", GroupSpec is required and MySQL servers will start  a replication group
	Mode *MySQLClusterMode `json:"mode,omitempty"`

	// Group replication info for MySQL
	Group *MySQLGroupSpec `json:"group,omitempty"`
}

type MySQLGroupSpec struct {
	// TODO: "Multi-Primary" needs to be implemented
	// Group Replication can be deployed in either "Single-Primary" or "Multi-Primary" mode
	Mode *MySQLGroupMode `json:"mode,omitempty"`

	// Group name is a version 4 UUID
	// ref: https://dev.mysql.com/doc/refman/5.7/en/group-replication-options.html#sysvar_group_replication_group_name
	Name string `json:"name,omitempty"`

	// On a replication master and each replication slave, the --server-id
	// option must be specified to establish a unique replication ID in the
	// range from 1 to 2^32 − 1. “Unique”, means that each ID must be different
	// from every other ID in use by any other replication master or slave.
	// ref: https://dev.mysql.com/doc/refman/5.7/en/server-system-variables.html#sysvar_server_id
	//
	// So, BaseServerID is needed to calculate a unique server_id for each member.
	BaseServerID *uint `json:"baseServerID,omitempty"`
}

type MySQLStatus struct {
	Phase  DatabasePhase `json:"phase,omitempty"`
	Reason string        `json:"reason,omitempty"`
	// observedGeneration is the most recent generation observed for this resource. It corresponds to the
	// resource's generation, which is updated on mutation by the API Server.
	// +optional
	ObservedGeneration *types.IntHash `json:"observedGeneration,omitempty"`
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

type MySQLList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	// Items is a list of MySQL TPR objects
	Items []MySQL `json:"items,omitempty"`
}

type InitSpec struct {
	ScriptSource *ScriptSourceSpec `json:"scriptSource,omitempty"`
	// Deprecated
	SnapshotSource *SnapshotSourceSpec `json:"snapshotSource,omitempty"`
	// PostgresWAL    *PostgresWALSourceSpec `json:"postgresWAL,omitempty"`
	// Name of stash restoreSession in same namespace of kubedb object.
	// ref: https://github.com/stashed/stash/blob/09af5d319bb5be889186965afb04045781d6f926/apis/stash/v1beta1/restore_session_types.go#L22
	StashRestoreSession *core.LocalObjectReference `json:"stashRestoreSession,omitempty"`
}

type ScriptSourceSpec struct {
	ScriptPath        string `json:"scriptPath,omitempty"`
	core.VolumeSource `json:",inline,omitempty"`
}

type SnapshotSourceSpec struct {
	Namespace string `json:"namespace"`
	Name      string `json:"name"`
	// Arguments to the restore job
	Args []string `json:"args,omitempty"`
}

type BackupScheduleSpec struct {
	CronExpression string `json:"cronExpression,omitempty"`

	// Snapshot Spec
	store.Backend `json:",inline"`

	// StorageType can be durable or ephemeral.
	// If not given, database storage type will be used.
	// +optional
	StorageType *StorageType `json:"storageType,omitempty"`

	// PodTemplate is an optional configuration for pods used to take database snapshots
	// +optional
	PodTemplate ofst.PodTemplateSpec `json:"podTemplate,omitempty"`

	// PodVolumeClaimSpec is used to specify temporary storage for backup/restore Job.
	// If not given, database's PvcSpec will be used.
	// If storageType is durable, then a PVC will be created using this PVCSpec.
	// If storageType is ephemeral, then an empty directory will be created of size PvcSpec.Resources.Requests[core.ResourceStorage].
	// +optional
	PodVolumeClaimSpec *core.PersistentVolumeClaimSpec `json:"podVolumeClaimSpec,omitempty"`
}

// LeaderElectionConfig contains essential attributes of leader election.
// ref: https://github.com/kubernetes/client-go/blob/6134db91200ea474868bc6775e62cc294a74c6c6/tools/leaderelection/leaderelection.go#L105-L114
type LeaderElectionConfig struct {
	// LeaseDuration is the duration in second that non-leader candidates will
	// wait to force acquire leadership. This is measured against time of
	// last observed ack. Default 15
	LeaseDurationSeconds int32 `json:"leaseDurationSeconds"`
	// RenewDeadline is the duration in second that the acting master will retry
	// refreshing leadership before giving up. Normally, LeaseDuration * 2 / 3.
	// Default 10
	RenewDeadlineSeconds int32 `json:"renewDeadlineSeconds"`
	// RetryPeriod is the duration in second the LeaderElector clients should wait
	// between tries of actions. Normally, LeaseDuration / 3.
	// Default 2
	RetryPeriodSeconds int32 `json:"retryPeriodSeconds"`
}

type DatabasePhase string

const (
	// used for Databases that are currently running
	DatabasePhaseRunning DatabasePhase = "Running"
	// used for Databases that are currently creating
	DatabasePhaseCreating DatabasePhase = "Creating"
	// used for Databases that are currently initializing
	DatabasePhaseInitializing DatabasePhase = "Initializing"
	// used for Databases that are Failed
	DatabasePhaseFailed DatabasePhase = "Failed"
)

type StorageType string

const (
	// default storage type and requires spec.storage to be configured
	StorageTypeDurable StorageType = "Durable"
	// Uses emptyDir as storage
	StorageTypeEphemeral StorageType = "Ephemeral"
)

type TerminationPolicy string

const (
	// Pauses database into a DormantDatabase
	TerminationPolicyPause TerminationPolicy = "Pause"
	// Deletes database pods, service, pvcs but leave the snapshot data intact. This will not create a DormantDatabase.
	TerminationPolicyDelete TerminationPolicy = "Delete"
	// Deletes database pods, service, pvcs and snapshot data. This will not create a DormantDatabase.
	TerminationPolicyWipeOut TerminationPolicy = "WipeOut"
	// Rejects attempt to delete database using ValidationWebhook. This replaces spec.doNotPause = true
	TerminationPolicyDoNotTerminate TerminationPolicy = "DoNotTerminate"
)
