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

package events

import (
	"context"
	"path/filepath"
	"strconv"
	"sync"
	"time"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events/eventtypes"
	"github.com/erda-project/erda/pkg/jsonstore"
)

const (
	// status_update_event state
	KILLING     = "TASK_KILLING"
	KILLED      = "TASK_KILLED"
	RUNNING     = "TASK_RUNNING"
	FINISHED    = "TASK_FINISHED"
	FAILED      = "TASK_FAILED"
	STARTING    = "TASK_STARTING"
	STAGING     = "TASK_STAGING"
	DROPPED     = "TASK_DROPPED"
	UNKNOWN     = "TASK_UNKNOWN"
	UNREACHABLE = "TASK_UNREACHABLE"

	// instance state
	INSTANCE_RUNNING  = "Running"
	INSTANCE_FAILED   = "Failed"
	INSTANCE_FINISHED = "Finished"
	INSTANCE_KILLED   = "Killed"
	// instance_health_changed_event state after packaging
	// instance and service shared
	HEALTHY   = "Healthy"
	UNHEALTHY = "UnHealthy"

	WATCHED_DIR = "/dice/service/"

	// The suffix of the sender's name when eventbox is called, used to zone the stage of event distribution
	//The events sent by the initialize executor phase
	SUFFIX_INIT = "_INIT"
	// Events in the periodic compensation phase
	SUFFIX_PERIOD = "_PERIOD"
	// Events in other periods on ordinary time
	SUFFIX_NORMAL = "_NORMAL"
	// Temporarily assigned prefix for edas event
	SUFFIX_EDAS = "_EDAS"
	// EDASV2 Events in the periodic compensation phase
	SUFFIX_EDASV2_PERIOD = "_EDASV2_PERIOD"
	// EDASV2 Incremental event
	SUFFIX_EDASV2_NORMAL = "_EDASV2_NORMAL"
	// EDASV2 Initial event
	SUFFIX_EDASV2_INIT = "_EDASV2_INIT"
	// K8S Events in the periodic compensation phase
	SUFFIX_K8S_PERIOD = "_K8S_PERIOD"
	// K8S Incremental event
	SUFFIX_K8S_NORMAL = "_K8S_NORMAL"
	// K8S Initial event
	SUFFIX_K8S_INIT = "_K8S_INIT"

	// Event type
	// The calculated event corresponds to SUFFIX_NORMAL
	EVENTS_INCR = "increment"
	//Initialization or periodic compensation event，corresponds to SUFFIX_INIT和SUFFIX_PERIOD
	EVENTS_TOTAL = "total"

	// instance suffix, which is stored in lstore as part of the key, identifies the time when the instance starts the health check
	START_HC_TIME_SUFFIX = "_start_hc_time"

	// Determine the left window edge of the health check timeout, including the interval(15s) time
	LEFT_EDGE int64 = -20
	// Determine the right window edge of the health check timeout, consider that sigterm cannot kill the container, wait for a period of time (20s~30s) and then receive sigkill exit
	RIGHT_DEGE int64 = 40

	// marathon event type of instance state change
	STATUS_UPDATE_EVENT = "status_update_event"
	// marathon type of instance health change
	INSTANCE_HEALTH_CHANGED_EVENT = "instance_health_changed_event"

	// The scheduler instance change event is processed according to the above two types of events
	INSTANCE_STATUS = "instances-status"
)

type LabelKey string

type Op struct {
	labels map[string]interface{}
	dest   map[string]interface{}
	sender string
}
type OpOperation func(*Op)

type Message struct {
	Sender  string                   `json:"sender"`
	Content interface{}              `json:"content"`
	Labels  map[LabelKey]interface{} `json:"labels"`
	Time    int64                    `json:"time,omitempty"` // UnixNano

	originContent interface{} `json:"-"`
}

type NotifierImpl struct {
	sender string
	labels map[string]interface{} // optional, can also assign `dest' as `Send' option
	dir    string
	js     jsonstore.JsonStore
}

type GetExecutorJsonStoreFn func(string) (*sync.Map, error)
type GetExecutorLocalStoreFn func(string) (*sync.Map, error)
type SetCallbackFn func(string) error

type Notifier interface {
	Send(content interface{}, options ...OpOperation) error
	SendRaw(message *Message) error
}

type EventMgr struct {
	ctx context.Context
	//Record the map of callback functions of all executors
	executorCbMap sync.Map
	// watch WATCHED_DIR path, synchronized to the local cache of the executor
	// 1, Used to calculate incremental state events
	// 2, Used for initialization and periodic compensation events
	MemEtcdStore jsonstore.JsonStore

	notifier Notifier
}

//Structure stuffed into eventbox
type RuntimeEvent struct {
	RuntimeName     string          `json:"runtimeName"`
	ServiceStatuses []ServiceStatus `json:"serviceStatuses,omitempty"`
	// Temporary field, which identifies whether the corresponding runtime is deleted
	IsDeleted bool   `json:"isDeleted,omitempty"`
	EventType string `json:"eventType,omitempty"`
}

type ServiceStatus struct {
	ServiceName      string           `json:"serviceName"`
	ServiceStatus    string           `json:"serviceStatus,omitempty"`
	Replica          int              `json:"replica,omitempty"`
	InstanceStatuses []InstanceStatus `json:"instanceStatuses,omitempty"`

	HealthCheckDuration int `json:"healthCheckDuration,omitempty"`
}

type InstanceStatus struct {
	ID             string `json:"id,omitempty"`
	Ip             string `json:"ip,omitempty"`
	InstanceStatus string `json:"instanceStatus,omitempty"`
	// stage records the exit stage of the container, which is only reflected in the exit (Killed, Failed, Finished) event in the incremental event
	// The current phases are:
	// a) Container startup phase (exit before health check timeout),"BeforeHealthCheckTimeout"
	// b) Health check timeout period (killed by health check）,"HealthCheckTimeout"
	// c) Post-health check stage (exit after the health check is completed),"AfterHealthCheckTimeout"
	Stage string `json:"stage,omitempty"`
	// Extended field
	Extra map[string]interface{} `json:"extra,omitempty"`
}

type EventLayer struct {
	InstanceId  string
	ServiceName string
	RuntimeName string
}

type WindowStatus struct {
	key        string
	statusType string
}

type InstancesWindow struct {
	key string
	e   *eventtypes.StatusEvent
}

func convert(m map[string]interface{}) map[LabelKey]interface{} {
	m_ := map[LabelKey]interface{}{}
	for k, v := range m {
		m_[LabelKey(k)] = v
	}
	return m_
}

func genMessagePath(dir string, timestamp int64) string {
	return filepath.Join(dir, strconv.FormatInt(timestamp, 10))
}

func genMessage(sender string, content interface{}, timestamp int64, labels map[string]interface{}) (*Message, error) {
	return &Message{
		Sender:  sender,
		Content: content,
		Labels:  convert(labels),
		Time:    timestamp,
	}, nil
}

func mergeMap(main map[string]interface{}, minor map[string]interface{}) map[string]interface{} {
	m := map[string]interface{}{}
	for k, v := range minor {
		m[k] = v
	}

	for k, v := range main {
		m[k] = v
	}
	return m
}

// Write message to etcd
func (n *NotifierImpl) Send(content interface{}, options ...OpOperation) error {
	option := &Op{}
	for _, op := range options {
		op(option)
	}
	timestamp := time.Now().UnixNano()
	labels := n.labels
	sender := n.sender
	if option.dest != nil {
		labels = mergeMap(option.dest, labels)
	}
	if option.labels != nil {
		labels = mergeMap(option.labels, labels)
	}
	if option.sender != "" {
		sender = option.sender
	}

	message, err := genMessage(sender, content, timestamp, labels)
	if err != nil {
		return err
	}
	return n.SendRaw(message)
}

func (n *NotifierImpl) SendRaw(message *Message) error {
	messagePath := genMessagePath(n.dir, message.Time)
	ctx := context.Background()
	return n.js.Put(ctx, messagePath, message)
}
