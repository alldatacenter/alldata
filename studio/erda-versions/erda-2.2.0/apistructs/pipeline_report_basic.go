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

package apistructs

import (
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
)

type PipelineBasicReport struct {
	ID               uint64         `json:"id"`
	Status           PipelineStatus `json:"status"`
	PipelineSource   PipelineSource `json:"pipelineSource"`
	PipelineYmlName  string         `json:"pipelineYmlName"`
	ClusterName      string         `json:"clusterName"`
	TimeCreated      *time.Time     `json:"timeCreated,omitempty"`
	TimeBegin        *time.Time     `json:"timeBegin,omitempty"`
	TimeEnd          *time.Time     `json:"timeEnd,omitempty"`
	TotalCostTimeSec int64          `json:"totalCostTimeSec"`

	TaskInfos []TaskReportInfo `json:"taskInfos,omitempty"`
}

type TaskReportInfo struct {
	ID               uint64                               `json:"id"`
	Status           PipelineStatus                       `json:"status"`
	Name             string                               `json:"name"`
	ActionType       string                               `json:"actionType"`
	ActionVersion    string                               `json:"actionVersion"`
	ExecutorType     string                               `json:"executorType"`
	ClusterName      string                               `json:"clusterName"`
	TimeBegin        *time.Time                           `json:"timeBegin,omitempty"`
	TimeEnd          *time.Time                           `json:"timeEnd,omitempty"`
	TimeBeginQueue   *time.Time                           `json:"timeBeginQueue,omitempty"`
	TimeEndQueue     *time.Time                           `json:"timeEndQueue,omitempty"`
	QueueCostTimeSec int64                                `json:"queueCostTimeSec"`
	RunCostTimeSec   int64                                `json:"runCostTimeSec"`
	MachineStat      *taskinspect.PipelineTaskMachineStat `json:"machineStat,omitempty"`
	Meta             map[string]string                    `json:"meta"`
}
