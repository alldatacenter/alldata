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
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type PipelineTaskRuntimeEvent struct {
	DefaultEvent
	IdentityInfo
	EventHeader apistructs.EventHeader
	Task        *spec.PipelineTask
	Pipeline    *spec.Pipeline
	RuntimeID   string
}

func (e *PipelineTaskRuntimeEvent) Kind() EventKind {
	return EventKindPipelineTaskRuntime
}

func (e *PipelineTaskRuntimeEvent) Header() apistructs.EventHeader {
	return e.EventHeader
}

func (e *PipelineTaskRuntimeEvent) Sender() string {
	return SenderPipeline
}

func (e *PipelineTaskRuntimeEvent) Content() interface{} {
	content := apistructs.PipelineTaskRuntimeEventData{
		ClusterName:    e.Pipeline.ClusterName,
		PipelineTaskID: e.Task.ID,
		Status:         string(e.Task.Status),
		RuntimeID:      e.RuntimeID,
	}
	return content
}

func (e *PipelineTaskRuntimeEvent) String() string {
	return fmt.Sprintf("event: %s, action: %s, pipelineID: %d, pipelineTaskID: %d, runtimeID: %s",
		e.EventHeader.Event, e.EventHeader.Action, e.Pipeline.ID, e.Task.ID, e.RuntimeID)
}

func (e *PipelineTaskRuntimeEvent) HandleWebhook() error {
	req := &apistructs.EventCreateRequest{}
	req.Sender = SenderPipeline
	req.EventHeader = e.Header()
	req.Content = e.Content()

	return e.DefaultEvent.CreateEvent(req)
}

type WSPipelineTaskRuntimeIDUpdatePayload struct {
	wsHeader
	PipelineTaskID uint64 `json:"pipelineTaskID"`
	RuntimeID      string `json:"runtimeID"`
}

const (
	WSTypePipelineTaskRuntimeIDUpdate = "PIPELINE_TASK_RUNTIME_ID_UPDATE"
)

func (e *PipelineTaskRuntimeEvent) HandleWebSocket() error {
	payload := WSPipelineTaskRuntimeIDUpdatePayload{}
	payload.PipelineTaskID = e.Task.ID
	payload.PipelineID = e.Pipeline.ID
	payload.ApplicationID = e.Pipeline.Labels[apistructs.LabelAppID]
	payload.ProjectID = e.Pipeline.Labels[apistructs.LabelProjectID]
	payload.OrgID = e.Pipeline.Labels[apistructs.LabelOrgID]
	payload.RuntimeID = e.RuntimeID

	wsEvent := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   e.Pipeline.Labels[apistructs.LabelAppID],
		},
		Type:    WSTypePipelineTaskRuntimeIDUpdate,
		Payload: payload,
	}

	return e.DefaultEvent.wsClient.EmitEvent(context.Background(), wsEvent)
}
