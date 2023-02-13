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

package webhook

import (
	"encoding/json"

	"github.com/erda-project/erda/apistructs"
)

// message which response to hooked url
type EventMessage struct {
	Event         string `json:"event"`
	Action        string `json:"action"`
	OrgID         string `json:"orgID"`
	ProjectID     string `json:"projectID"`
	ApplicationID string `json:"applicationID"`
	Env           string `json:"env"`
	// content 结构跟具体 event 相关
	Content   json.RawMessage `json:"content"`
	TimeStamp string          `json:"timestamp"`
}

func PingEvent(org, project, application string, h Hook) (*EventMessage, error) {
	hraw, err := json.Marshal(h)
	if err != nil {
		return nil, err
	}
	m := MkEventMessage(EventLabel{
		Event:         "ping",
		Action:        "ping",
		OrgID:         org,
		ProjectID:     project,
		ApplicationID: application},
		hraw)
	return &m, nil
}

func MkEventMessage(label EventLabel, content []byte) EventMessage {
	return EventMessage{
		Event:         label.Event,
		Action:        label.Action,
		OrgID:         label.OrgID,
		ProjectID:     label.ProjectID,
		ApplicationID: label.ApplicationID,
		Env:           label.Env,
		Content:       content,
		TimeStamp:     nowTimestamp(),
	}
}

type EventLabel = apistructs.EventHeader
