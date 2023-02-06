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
	"fmt"
	"time"

	"github.com/erda-project/erda-proto-go/core/pipeline/queue/pb"
	"github.com/erda-project/erda/pkg/strutil"
)

type EnqueueConditionType string

const (
	EnqueueConditionSkipAlreadyRunningLimit EnqueueConditionType = "skip_already_running_limit"
)

func (e EnqueueConditionType) IsSkipAlreadyRunningLimit() bool {
	return e == EnqueueConditionSkipAlreadyRunningLimit
}

const PipelinePreCheckResultContextKey = "precheck_result"

type PipelineQueueMode string

var (
	PipelineQueueModeStrict PipelineQueueMode = "STRICT"
	PipelineQueueModeLoose  PipelineQueueMode = "LOOSE"
)

func (m PipelineQueueMode) String() string { return string(m) }
func (m PipelineQueueMode) IsValid() bool {
	switch m {
	case PipelineQueueModeStrict, PipelineQueueModeLoose:
		return true
	default:
		return false
	}
}

type PipelineQueue struct {
	ID uint64 `json:"id"`

	Name             string                              `json:"name"`
	PipelineSource   PipelineSource                      `json:"pipelineSource"`
	ClusterName      string                              `json:"clusterName"`
	ScheduleStrategy ScheduleStrategyInsidePipelineQueue `json:"scheduleStrategy"`
	Mode             PipelineQueueMode                   `json:"mode,omitempty"`
	Priority         int64                               `json:"priority"`
	Concurrency      int64                               `json:"concurrency"`
	MaxCPU           float64                             `json:"maxCPU"`
	MaxMemoryMB      float64                             `json:"maxMemoryMB"`

	Labels map[string]string `json:"labels,omitempty"`

	TimeCreated *time.Time `json:"timeCreated,omitempty"`
	TimeUpdated *time.Time `json:"timeUpdated,omitempty"`

	Usage *pb.QueueUsage `json:"usage"`
}

// ScheduleStrategyInsidePipelineQueue represents the schedule strategy of workflows inside a queue.
type ScheduleStrategyInsidePipelineQueue string

var (
	ScheduleStrategyInsidePipelineQueueOfFIFO ScheduleStrategyInsidePipelineQueue = "FIFO"
)

func (strategy ScheduleStrategyInsidePipelineQueue) String() string {
	return string(strategy)
}

func (strategy ScheduleStrategyInsidePipelineQueue) IsValid() bool {
	switch strategy {
	case ScheduleStrategyInsidePipelineQueueOfFIFO:
		return true
	default:
		return false
	}
}

var (
	PipelineQueueDefaultPriority         int64 = 10
	PipelineQueueDefaultScheduleStrategy       = ScheduleStrategyInsidePipelineQueueOfFIFO
	PipelineQueueDefaultMode                   = PipelineQueueModeLoose
	PipelineQueueDefaultConcurrency      int64 = 1
)

// PipelineQueueCreateRequest represents queue create request.
type PipelineQueueCreateRequest struct {

	// Name is the queue name.
	// +required
	Name string `json:"name,omitempty"`

	// PipelineSource group queues by source.
	// +required
	PipelineSource PipelineSource `json:"pipelineSource,omitempty"`

	// ClusterName represents which cluster this queue belongs to.
	// +required
	ClusterName string `json:"clusterName,omitempty"`

	// ScheduleStrategy defines schedule strategy.
	// If not present, will use default strategy.
	// +optional
	ScheduleStrategy ScheduleStrategyInsidePipelineQueue `json:"scheduleStrategy,omitempty"`

	// Mode defines queue mode.
	// If not present, will use default mode.
	// +optional
	Mode PipelineQueueMode `json:"mode,omitempty"`

	// Priority defines item default priority inside queues.
	// Higher number means higher priority.
	// If not present, will use default priority.
	// +optional
	Priority int64 `json:"priority,omitempty"`

	// Concurrency defines how many item can running at the same time.
	// If not present, will use default concurrency.
	// +optional
	Concurrency int64 `json:"concurrency,omitempty"`

	// MaxCPU is the cpu resource this queue holds.
	// +optional
	MaxCPU float64 `json:"maxCPU,omitempty"`

	// MaxMemoryMB is the memory resource this queue holds.
	// +optional
	MaxMemoryMB float64 `json:"maxMemoryMB,omitempty"`

	// Labels contains the other infos for this queue.
	// Labels can be used to query and filter queues.
	// +optional
	Labels map[string]string `json:"labels,omitempty"`

	IdentityInfo
}

// Validate validate and handle request.
func (req *PipelineQueueCreateRequest) Validate() error {
	// name
	if err := strutil.Validate(req.Name, strutil.MinLenValidator(1), strutil.MaxRuneCountValidator(191)); err != nil {
		return fmt.Errorf("invalid name: %v", err)
	}
	// source
	if req.PipelineSource == "" {
		return fmt.Errorf("missing pipelineSource")
	}
	if !req.PipelineSource.Valid() {
		return fmt.Errorf("invalid pipelineSource: %s", req.PipelineSource)
	}
	// clusterName
	if req.ClusterName == "" {
		return fmt.Errorf("missing clusterName")
	}
	// strategy
	if req.ScheduleStrategy == "" {
		req.ScheduleStrategy = PipelineQueueDefaultScheduleStrategy
	}
	// mode
	if req.Mode == "" {
		req.Mode = PipelineQueueDefaultMode
	}
	if !req.Mode.IsValid() {
		return fmt.Errorf("invalid mode: %s", req.Mode)
	}
	// scheduleStrategy
	if !req.ScheduleStrategy.IsValid() {
		return fmt.Errorf("invalid schedule strategy: %s", req.ScheduleStrategy)
	}
	// priority
	if req.Priority == 0 {
		req.Priority = PipelineQueueDefaultPriority
	}
	if req.Priority < 0 {
		return fmt.Errorf("priority must > 0")
	}
	// concurrency
	if req.Concurrency == 0 {
		req.Concurrency = PipelineQueueDefaultConcurrency
	}
	if req.Concurrency < 0 {
		return fmt.Errorf("concurrency must > 0")
	}
	// max cpu
	if req.MaxCPU < 0 {
		return fmt.Errorf("max cpu must >= 0")
	}
	// max memoryMB
	if req.MaxMemoryMB < 0 {
		return fmt.Errorf("max memory(MB) must >= 0")
	}
	return nil
}

type PipelineQueueCreateResponse struct {
	Header
	Data *PipelineQueue `json:"data"`
}

type PipelineQueueListResponse struct {
	Header
	Data *PipelineQueuePagingData `json:"data"`
}

// PipelineQueuePagingRequest
type PipelineQueuePagingRequest struct {
	Name string `schema:"name"`

	PipelineSources []PipelineSource `schema:"pipelineSource"`

	ClusterName string `schema:"clusterName"`

	ScheduleStrategy ScheduleStrategyInsidePipelineQueue `schema:"scheduleStrategy"`

	Priority int64 `schema:"priority"`

	Concurrency int64 `schema:"concurrency"`

	// MUST match
	MustMatchLabels []string `schema:"mustMatchLabel"`
	// ANY match
	AnyMatchLabels []string `schema:"anyMatchLabel"`

	// AllowNoPipelineSources, default is false.
	// 默认查询必须带上 pipeline source，增加区分度
	AllowNoPipelineSources bool `schema:"allowNoPipelineSources"`

	// OrderByTargetIDAsc 根据 target_id 升序，默认为 false，即降序
	OrderByTargetIDAsc bool `schema:"orderByTargetIDAsc"`

	PageNo   int `schema:"pageNo"`
	PageSize int `schema:"pageSize"`
}

// PipelineQueuePagingData .
type PipelineQueuePagingData struct {
	Queues []*PipelineQueue `json:"queues"`
	Total  int64            `json:"total"`
}

// PipelineQueueUpdateRequest .
type PipelineQueueUpdateRequest struct {
	ID uint64 `json:"-"` // get from path variable

	// create request include all fields can be updated
	PipelineQueueCreateRequest
}

// Validate request.
func (req *PipelineQueueUpdateRequest) Validate() error {
	// id
	if req.ID == 0 {
		return fmt.Errorf("missing queue id")
	}
	// pipeline source
	if req.PipelineSource != "" {
		return fmt.Errorf("cannot change queue's source")
	}

	return nil
}

// PipelineQueueValidateResult represents queue validate result.
type PipelineQueueValidateResult struct {
	Success     bool                      `json:"success"`
	Reason      string                    `json:"reason"`
	IsEnd       bool                      `json:"isEnd"`
	RetryOption *QueueValidateRetryOption `json:"retryOption"`
}

type QueueValidateRetryOption struct {
	IntervalSecond      uint64 `json:"intervalSecond"`
	IntervalMillisecond uint64 `json:"intervalMillisecond"`
}

func (r PipelineQueueValidateResult) IsSuccess() bool {
	return r.Success
}
func (r PipelineQueueValidateResult) IsFailed() bool {
	return !r.Success
}

func (r PipelineQueueValidateResult) IsEndStatus() bool {
	return r.IsEnd
}
