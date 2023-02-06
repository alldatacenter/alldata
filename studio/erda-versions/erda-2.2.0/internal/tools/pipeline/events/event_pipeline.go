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
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/websocket"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/linkutil"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type PipelineEvent struct {
	DefaultEvent
	IdentityInfo
	EventHeader apistructs.EventHeader
	Pipeline    *spec.Pipeline
}

func (e *PipelineEvent) Kind() EventKind {
	return EventKindPipeline
}

func (e *PipelineEvent) Header() apistructs.EventHeader {
	return e.EventHeader
}

func (e *PipelineEvent) Sender() string {
	return SenderPipeline
}

func (e *PipelineEvent) Content() interface{} {
	content := apistructs.PipelineInstanceEventData{
		PipelineID:      e.Pipeline.ID,
		Status:          e.Pipeline.Status.String(),
		Branch:          e.Pipeline.Labels[apistructs.LabelBranch],
		Source:          e.Pipeline.PipelineSource.String(),
		IsCron:          e.Pipeline.TriggerMode == apistructs.PipelineTriggerModeCron,
		PipelineYmlName: e.Pipeline.PipelineYmlName,
		UserID:          e.UserID,
		InternalClient:  e.InternalClient,
		CostTimeSec:     costtimeutil.CalculatePipelineCostTimeSec(e.Pipeline),
		DiceWorkspace:   e.Pipeline.Extra.DiceWorkspace.String(),
		ClusterName:     e.Pipeline.ClusterName,
		TimeBegin:       e.Pipeline.TimeBegin,
		CronExpr:        e.Pipeline.PipelineExtra.Extra.CronExpr,
		Labels:          e.Pipeline.MergeLabels(),
	}
	return content
}

func (e *PipelineEvent) String() string {
	return fmt.Sprintf("event: %s, action: %s, pipelineID: %d",
		e.EventHeader.Event, e.EventHeader.Action, e.Pipeline.ID)
}

func (e *PipelineEvent) HandleWebhook() error {
	req := &apistructs.EventCreateRequest{}
	req.Sender = SenderPipeline
	req.EventHeader = e.Header()
	req.Content = e.Content()
	return e.DefaultEvent.CreateEvent(req)
}

const (
	WSTypePipelineStatusUpdate = "PIPELINE_STATUS_UPDATE"
)

type wsHeader struct {
	PipelineID    uint64 `json:"pipelineID"`
	ApplicationID string `json:"applicationID"`
	ProjectID     string `json:"projectID"`
	OrgID         string `json:"orgID"`
}

type WSPipelineStatusUpdatePayload struct {
	wsHeader
	Status apistructs.PipelineStatus `json:"status"`

	CostTimeSec int64 `json:"costTimeSec"`
}

func (e *PipelineEvent) HandleWebSocket() error {
	payload := WSPipelineStatusUpdatePayload{}
	payload.PipelineID = e.Pipeline.ID
	payload.ApplicationID = e.Pipeline.Labels[apistructs.LabelAppID]
	payload.ProjectID = e.Pipeline.Labels[apistructs.LabelProjectID]
	payload.OrgID = e.Pipeline.Labels[apistructs.LabelOrgID]
	payload.Status = e.Pipeline.Status
	payload.CostTimeSec = e.Content().(apistructs.PipelineInstanceEventData).CostTimeSec

	wsEvent := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   e.Header().ApplicationID,
		},
		Type:    WSTypePipelineStatusUpdate,
		Payload: payload,
	}

	return e.DefaultEvent.wsClient.EmitEvent(context.Background(), wsEvent)
}

func (e *PipelineEvent) HandleDingDing() error {
	// dingding 地址目前是在应用维度，如果没有应用信息，则不发送
	if e.Pipeline.Labels[apistructs.LabelAppID] == "" {
		return nil
	}

	ddHookURL, err := getDingDingHookURL(e)
	if err != nil {
		return err
	}

	if len(ddHookURL) == 0 {
		return nil
	}

	content, err := makeDingDingMsg(e)
	if err != nil {
		return errors.Wrap(err, "failed to make dingding message")
	}

	if content == "" {
		return nil
	}

	return e.DefaultEvent.bdl.CreateMessage(&apistructs.MessageCreateRequest{
		Sender: "pipeline",
		Labels: map[string]interface{}{
			"DINGDING": []string{ddHookURL},
		},
		Content: content,
	})
}

func (e *PipelineEvent) HandleHTTP() error {
	var dests []string
	if len(e.Pipeline.Extra.CallbackURLs) == 0 {
		return nil
	}
	dests = e.Pipeline.Extra.CallbackURLs

	return e.DefaultEvent.bdl.CreateMessage(&apistructs.MessageCreateRequest{
		Sender: "pipeline-http",
		Labels: map[string]interface{}{
			"HTTP": dests,
		},
		Content: e.Content(),
	})
}

func (e *PipelineEvent) HandleDB() error {
	return nil
}

func getDingDingHookURL(e *PipelineEvent) (string, error) {
	appID, err := strconv.ParseUint(e.Pipeline.Labels[apistructs.LabelAppID], 10, 64)
	if err != nil {
		return "", err
	}
	app, err := e.DefaultEvent.bdl.GetApp(appID)
	if err != nil {
		return "", err
	}

	dingdingHookURL, ok := app.Config["ddHookUrl"]
	if ok {
		switch dingdingHookURL.(type) {
		case string:
			return dingdingHookURL.(string), nil
		default:
			return "", errors.Errorf("invalid dingdingHookUrl [%v]", dingdingHookURL)
		}
	}
	return "", nil
}

func makeDingDingMsg(e *PipelineEvent) (string, error) {
	var userName string
	user, err := e.DefaultEvent.bdl.GetCurrentUser(e.UserID)
	if err == nil {
		userName = user.Name
	}

	valid, link := linkutil.GetPipelineLink(e.bdl, *e.Pipeline)
	if !valid {
		return "", nil
	}

	msg := fmt.Sprintf("%s 在应用 %s/%s 的构建列表中 [%s]。分支: %s，环境: %s。链接: %s",
		userName, e.Pipeline.NormalLabels[apistructs.LabelProjectName], e.Pipeline.NormalLabels[apistructs.LabelAppName],
		apistructs.PipelineStatus(e.EventHeader.Action).ToDesc(),
		e.Pipeline.NormalLabels[apistructs.LabelBranch], e.Pipeline.Extra.DiceWorkspace,
		link)

	return msg, nil
}
