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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/websocket"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type PipelineTaskEvent struct {
	DefaultEvent
	IdentityInfo
	EventHeader apistructs.EventHeader
	Task        *spec.PipelineTask
	Pipeline    *spec.Pipeline
}

func (e *PipelineTaskEvent) Kind() EventKind {
	return EventKindPipelineTask
}

func (e *PipelineTaskEvent) Header() apistructs.EventHeader {
	return e.EventHeader
}

func (e *PipelineTaskEvent) Sender() string {
	return SenderPipeline
}

func (e *PipelineTaskEvent) Content() interface{} {
	content := apistructs.PipelineTaskEventData{
		PipelineTaskID:  e.Task.ID,
		PipelineID:      e.Pipeline.ID,
		ActionType:      e.Task.Type,
		ClusterName:     e.Pipeline.ClusterName,
		Status:          string(e.Task.Status),
		UserID:          e.UserID,
		CreatedAt:       e.Task.TimeCreated,
		QueueTimeSec:    costtimeutil.CalculateTaskQueueTimeSec(e.Task),
		CostTimeSec:     costtimeutil.CalculateTaskCostTimeSec(e.Task),
		OrgName:         e.Pipeline.GetOrgName(),
		ProjectName:     e.Pipeline.GetLabel(apistructs.LabelProjectName),
		ApplicationName: e.Pipeline.GetLabel(apistructs.LabelAppName),
		TaskName:        e.Task.Name,
		RuntimeID:       e.Task.RuntimeID(),
		ReleaseID:       e.Task.ReleaseID(),
	}
	return content
}

func (e *PipelineTaskEvent) String() string {
	return fmt.Sprintf("event: %s, action: %s, pipelineID: %d, pipelineTaskID: %d",
		e.EventHeader.Event, e.EventHeader.Action, e.Pipeline.ID, e.Task.ID)
}

func (e *PipelineTaskEvent) HandleWebhook() error {
	req := &apistructs.EventCreateRequest{}
	req.Sender = SenderPipeline
	req.EventHeader = e.Header()
	req.Content = e.Content()
	return e.DefaultEvent.CreateEvent(req)
}

const (
	WSTypePipelineTaskStatusUpdate = "PIPELINE_TASK_STATUS_UPDATE"
)

type WSPipelineTaskStatusUpdatePayload struct {
	wsHeader
	PipelineTaskID uint64                    `json:"pipelineTaskID"`
	Status         apistructs.PipelineStatus `json:"status"`
	Result         taskresult.Result         `json:"result"`

	CostTimeSec int64 `json:"costTimeSec"`
}

func (e *PipelineTaskEvent) HandleWebSocket() error {
	state := e.Task.Status
	if e.Task.Type == "manual-review" {
		state = e.Task.Status.ChangeStateForManualReview()
	}

	payload := WSPipelineTaskStatusUpdatePayload{}
	payload.PipelineTaskID = e.Task.ID
	payload.PipelineID = e.Pipeline.ID
	payload.ApplicationID = e.Pipeline.Labels[apistructs.LabelAppID]
	payload.ProjectID = e.Pipeline.Labels[apistructs.LabelProjectID]
	payload.OrgID = e.Pipeline.Labels[apistructs.LabelOrgID]
	payload.Status = state
	if e.Task.Result != nil {
		payload.Result = *e.Task.Result
	}
	payload.CostTimeSec = e.Content().(apistructs.PipelineTaskEventData).CostTimeSec

	wsEvent := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   e.Header().ApplicationID,
		},
		Type:    WSTypePipelineTaskStatusUpdate,
		Payload: payload,
	}

	return e.DefaultEvent.wsClient.EmitEvent(context.Background(), wsEvent)
}
