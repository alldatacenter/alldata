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
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/websocket"
	"github.com/erda-project/erda/internal/tools/orchestrator/ws"
)

type WsPublisher struct {
	manager   *EventManager
	publisher *websocket.Publisher
}

func NewWsPublisher(manager *EventManager) *EventListener {
	p, err := websocket.NewPublisher()
	if err != nil {
		logrus.Fatalf("failed to create websocket publisher, err is %v", err)
	}

	var l EventListener = &WsPublisher{
		publisher: p,
		manager:   manager,
	}
	return &l
}

func (p *WsPublisher) OnEvent(event *RuntimeEvent) {
	if err := p.publishWs(event); err != nil {
		logrus.Errorf("[alert] failed to publish ws, event: %v, err is %v", event, err.Error())
	}
}

func (p *WsPublisher) publishWs(event *RuntimeEvent) error {
	p.publishWsDeployStatusUpdate(event)
	p.publishWsRuntimeStatusChanged(event)
	p.publishWsRuntimeServiceStatusChanged(event)
	p.publishWsRuntimeDeleting(event)
	p.publishWsRuntimeDeleted(event)
	return nil
}

func (p *WsPublisher) publishWsDeployStatusUpdate(event *RuntimeEvent) {
	// all Deploy events make DeployStatusUpdate
	if !strings.HasPrefix(string(event.EventName), "RuntimeDeploy") {
		return
	}
	e := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   fmt.Sprintf("%d", event.Runtime.ApplicationID),
		},
		Type: ws.R_DEPLOY_STATUS_UPDATE,
		Payload: ws.DeployStatusUpdatePayload{
			DeploymentId: event.Deployment.ID,
			RuntimeId:    event.Runtime.ID,
			Status:       event.Deployment.Status,
			Phase:        event.Deployment.Phase,
			Step:         event.Deployment.Phase,
		},
	}
	p.emit(e)
}

func (p *WsPublisher) publishWsRuntimeStatusChanged(event *RuntimeEvent) {
	if event.EventName != RuntimeStatusChanged {
		return
	}
	e := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   fmt.Sprintf("%d", event.Runtime.ApplicationID),
		},
		Type: ws.R_RUNTIME_STATUS_CHANGED,
		Payload: ws.RuntimeStatusChangedPayload{
			RuntimeId: event.Runtime.ID,
			Status:    event.Runtime.Status,
			Errors:    event.Runtime.Errors,
		},
	}
	p.emit(e)
}

func (p *WsPublisher) publishWsRuntimeServiceStatusChanged(event *RuntimeEvent) {
	if event.EventName != RuntimeServiceStatusChanged {
		return
	}
	e := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   fmt.Sprintf("%d", event.Runtime.ApplicationID),
		},
		Type: ws.R_RUNTIME_SERVICE_STATUS_CHANGED,
		Payload: ws.RuntimeServiceStatusChangedPayload{
			RuntimeId:   event.Runtime.ID,
			ServiceName: event.Service.ServiceName,
			Status:      event.Service.Status,
			Errors:      event.Service.Errors,
		},
	}
	p.emit(e)
}

func (p *WsPublisher) publishWsRuntimeDeleting(event *RuntimeEvent) {
	if event.EventName != RuntimeDeleting {
		return
	}
	e := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   fmt.Sprintf("%d", event.Runtime.ApplicationID),
		},
		Type: ws.R_RUNTIME_DELETING,
		Payload: ws.RuntimeDeletingPayload{
			RuntimeId: event.Runtime.ID,
		},
	}
	p.emit(e)
}

func (p *WsPublisher) publishWsRuntimeDeleted(event *RuntimeEvent) {
	if event.EventName != RuntimeDeleted {
		return
	}
	e := websocket.Event{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   fmt.Sprintf("%d", event.Runtime.ApplicationID),
		},
		Type: ws.R_RUNTIME_DELETED,
		Payload: ws.RuntimeDeletingPayload{
			RuntimeId: event.Runtime.ID,
		},
	}
	p.emit(e)
}

func (p *WsPublisher) emit(e websocket.Event) {
	ctx := context.Background()
	p.publisher.EmitEvent(ctx, e)
}
