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

package edas

import (
	"strings"
	"time"

	api "github.com/aliyun/alibaba-cloud-sdk-go/services/edas"
)

// SlbType slb type
type SlbType string

const (
	EDAS_SLB_INTERNAL SlbType = "intranet"
	EDAS_SLB_EXTERNAL SlbType = "internet"
)

// ChangeOrderStatus change orderId status
type ChangeOrderStatus int

const (
	CHANGE_ORDER_STATUS_ERROR     ChangeOrderStatus = -1
	CHANGE_ORDER_STATUS_PENDING   ChangeOrderStatus = 0
	CHANGE_ORDER_STATUS_EXECUTING ChangeOrderStatus = 1
	CHANGE_ORDER_STATUS_SUCC      ChangeOrderStatus = 2
	CHANGE_ORDER_STATUS_FAILED    ChangeOrderStatus = 3
	CHANGE_ORDER_STATUS_STOPPED   ChangeOrderStatus = 6
	CHANGE_ORDER_STATUS_ABNORMAL  ChangeOrderStatus = 10
)

var ChangeOrderStatusString = map[ChangeOrderStatus]string{
	CHANGE_ORDER_STATUS_ERROR:     "error",
	CHANGE_ORDER_STATUS_PENDING:   "pending",
	CHANGE_ORDER_STATUS_EXECUTING: "executing",
	CHANGE_ORDER_STATUS_SUCC:      "success",
	CHANGE_ORDER_STATUS_FAILED:    "failed",
	CHANGE_ORDER_STATUS_STOPPED:   "stopped",
	CHANGE_ORDER_STATUS_ABNORMAL:  "abnormal",
}

// AppState app state
type AppState int

const (
	APP_STATE_AGENT_OFF              AppState = 0
	APP_STATE_STOPPED                AppState = 1
	APP_STATE_RUNNING_BUT_URL_FAILED AppState = 3
	APP_STATE_RUNNING                AppState = 7
)

var AppStateString = map[AppState]string{
	APP_STATE_AGENT_OFF:              "agent off",
	APP_STATE_STOPPED:                "stopped",
	APP_STATE_RUNNING_BUT_URL_FAILED: "running but url failed",
	APP_STATE_RUNNING:                "running",
}

// TaskState task state
type TaskState int

const (
	TASK_STATE_UNKNOWN TaskState = iota
	TASK_STATE_PROCESSING
	TASK_STATE_SUCCESS
	TASK_STATE_FAILED
)

var TaskStateString = map[TaskState]string{
	TASK_STATE_UNKNOWN:    "unknown",
	TASK_STATE_PROCESSING: "processing",
	TASK_STATE_SUCCESS:    "success",
	TASK_STATE_FAILED:     "failed",
}

// AppStatus app status
type AppStatus string

const (
	AppStatusRunning   AppStatus = "Running"
	AppStatusDeploying AppStatus = "Deploying"
	AppStatusStopped   AppStatus = "Stopped"
	AppStatusFailed    AppStatus = "Failed"
	AppStatusUnknown   AppStatus = "Unknown"
)

type ServiceSpec struct {
	Name        string `json:"name"`
	Image       string `json:"image"`
	Cmd         string `json:"cmd"`
	Args        string `json:"args"` // e.g. [{"argument":"-c"},{"argument":"test"}]
	Instances   int    `json:"instances"`
	CPU         int    `json:"cpu"`
	Mcpu        int    `json:"mcpu"`
	Mem         int    `json:"mem"`
	Ports       []int  `json:"ports"`
	LocalVolume string `json:"localVolume"`
	Envs        string `json:"envs"` // e.g. [{"name":"testkey","value":"testValue"}]
	// e.g. {"failureThreshold": 3,"initialDelaySeconds": 5,"successThreshold": 1,"timeoutSeconds": 1,"tcpSocket":{"host":"", "port":8080}}
	Liveness string `json:"liveness"`
	// e.g. {"failureThreshold": 3,"initialDelaySeconds": 5,"successThreshold": 1,"timeoutSeconds": 1,"httpGet": {"path": "/consumer","port": 8080,"scheme": "HTTP","httpHeaders": [{"name": "test","value": "testvalue"}]}}
	Readiness string `json:"readiness"`
}

// ByCreateTime change order for sort
type ByCreateTime []api.ChangeOrder

func (a ByCreateTime) Len() int      { return len(a) }
func (a ByCreateTime) Swap(i, j int) { a[i], a[j] = a[j], a[i] }
func (a ByCreateTime) Less(i, j int) bool {
	it, ierr := time.Parse("2006-01-02 15:04:05", a[i].CreateTime)
	jt, jerr := time.Parse("2006-01-02 15:04:05", a[j].CreateTime)

	if ierr != nil || jerr != nil {
		return -1 == strings.Compare(a[i].CreateTime, a[j].CreateTime)
	}

	return it.Unix() < jt.Unix()
}

type ChangeType string

const (
	CHANGE_TYPE_CREATE ChangeType = "Create"
	CHANGE_TYPE_DEPLOY ChangeType = "Deploy"
	CHANGE_TYPE_STOP   ChangeType = "Stop"
)

type EdasEnv struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}
