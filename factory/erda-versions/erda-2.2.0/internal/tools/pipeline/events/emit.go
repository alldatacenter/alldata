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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func EmitPipelineInstanceEvent(p *spec.Pipeline, userID string) {
	event := &PipelineEvent{DefaultEvent: defaultEvent}

	// EventHeader
	event.EventHeader.Event = string(EventKindPipeline)
	event.EventHeader.Action = string(p.Status)

	event.EventHeader.ApplicationID = p.Labels[apistructs.LabelAppID]
	event.EventHeader.ProjectID = p.Labels[apistructs.LabelProjectID]
	event.EventHeader.OrgID = p.Labels[apistructs.LabelOrgID]
	event.EventHeader.Env = p.Extra.DiceWorkspace.String()

	// Identity
	event.UserID = userID
	event.InternalClient = p.Extra.InternalClient

	// Pipeline
	event.Pipeline = p

	mgr.ch <- event
}

func EmitPipelineStreamEvent(pipelineID uint64, events []*apistructs.PipelineEvent) {
	event := &PipelineStreamEvent{DefaultEvent: defaultEvent}

	// EventHeader
	event.EventHeader.Event = string(EventKindPipelineStream)

	// Pipeline
	event.PipelineID = pipelineID

	// Stream Events
	event.Events = events

	mgr.ch <- event
}

func EmitTaskEvent(task *spec.PipelineTask, p *spec.Pipeline) {
	event := &PipelineTaskEvent{DefaultEvent: defaultEvent}

	// EventHeader
	event.EventHeader.Event = string(EventKindPipelineTask)
	event.EventHeader.Action = string(task.Status)

	event.EventHeader.ApplicationID = p.Labels[apistructs.LabelAppID]
	event.EventHeader.ProjectID = p.Labels[apistructs.LabelProjectID]
	event.EventHeader.OrgID = p.Labels[apistructs.LabelOrgID]
	event.EventHeader.Env = p.Extra.DiceWorkspace.String()

	// Identity
	event.UserID = p.GetRunUserID()
	event.InternalClient = p.Extra.InternalClient

	// Task
	event.Task = task

	// Pipeline
	event.Pipeline = p

	mgr.ch <- event
}

func EmitTaskRuntimeEvent(task *spec.PipelineTask, p *spec.Pipeline) {
	event := &PipelineTaskRuntimeEvent{DefaultEvent: defaultEvent}

	// EventHeader
	event.EventHeader.Event = string(EventKindPipelineTaskRuntime)
	event.EventHeader.Action = "update"

	event.EventHeader.ApplicationID = p.Labels[apistructs.LabelAppID]
	event.EventHeader.ProjectID = p.Labels[apistructs.LabelProjectID]
	event.EventHeader.OrgID = p.Labels[apistructs.LabelOrgID]
	event.EventHeader.Env = p.Extra.DiceWorkspace.String()

	// Identity
	event.UserID = p.GetRunUserID()
	event.InternalClient = p.Extra.InternalClient

	// Task
	event.Task = task

	// Pipeline
	event.Pipeline = p

	// RuntimeID
	event.RuntimeID = task.RuntimeID()

	mgr.ch <- event
}
