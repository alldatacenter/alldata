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

const (
	RunnerTaskStatusPending  = "pending"
	RunnerTaskStatusRunning  = "running"
	RunnerTaskStatusSuccess  = "success"
	RunnerTaskStatusFailed   = "failed"
	RunnerTaskStatusCanceled = "canceled"
)

type RunnerTask struct {
	ID             uint64   `json:"id"`
	JobID          string   `json:"job_id"`
	Status         string   `json:"status"` // pending running success failed
	ContextDataUrl string   `json:"context_data_url"`
	OpenApiToken   string   `json:"openapi_token"`
	ResultDataUrl  string   `json:"result_data_url"`
	Commands       []string `json:"commands"`
	Targets        []string `json:"targets"`
	WorkDir        string   `json:"workdir"`
}

type QueryRunnerTaskRequest struct {
	TaskID string
}

type QueryRunnerTaskResponse struct {
	Header
	Data RunnerTask `json:"data"`
}

type CreateRunnerTaskRequest struct {
	JobID          string   `json:"job_id"`
	ContextDataUrl string   `json:"context_data_url"`
	Commands       []string `json:"commands"`
	Targets        []string `json:"targets"`
	WorkDir        string   `json:"workdir"`
}

type CreateRunnerTaskResponse struct {
	Header
	Data int64 `json:"data"`
}

type UpdateRunnerTaskRequest struct {
	ID             int64  `json:"-"`
	TaskID         string `json:"task_id"`
	Status         string `json:"status"`
	ContextDataUrl string `json:"context_data_url"`
	ResultDataUrl  string `json:"result_data_url"`
}
