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
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

// Valid values for event levels (new level could be added in future)
const (
	// EventLevelNormal represents information only and will not cause any problems
	EventLevelNormal string = "Normal"
	// EventLevelWarning represents events are to warn that something might go wrong
	EventLevelWarning string = "Warning"
)

type PipelineStreamEvent struct {
	DefaultEvent
	IdentityInfo
	EventHeader apistructs.EventHeader
	PipelineID  uint64
	Events      []*apistructs.PipelineEvent
}

func (e *PipelineStreamEvent) Kind() EventKind {
	return EventKindPipelineStream
}

func (e *PipelineStreamEvent) Header() apistructs.EventHeader {
	return e.EventHeader
}

func (e *PipelineStreamEvent) Sender() string {
	return SenderPipeline
}

func (e *PipelineStreamEvent) Content() interface{} {
	return e.Events
}

func (e *PipelineStreamEvent) String() string {
	eventJson, _ := json.Marshal(e.Events)
	return fmt.Sprintf("event: %s, action: %s, pipelineID: %d, event: %s",
		e.EventHeader.Event, e.EventHeader.Action, e.PipelineID, eventJson)
}

func (e *PipelineStreamEvent) HandleDB() error {
	err := e.dbClient.AppendPipelineEvent(e.PipelineID, e.Events)
	if err != nil {
		logrus.Errorf("pipeline event: failed to handleDB, pipelineID: %d, err: %v", e.PipelineID, err)
	}
	return nil
}
