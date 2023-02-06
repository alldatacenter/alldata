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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/queue"
)

type DeployPusher struct {
	manager *EventManager
	queue   *queue.PusherQueue
}

func NewDeployPusher(manager *EventManager, queue *queue.PusherQueue) *EventListener {
	var l EventListener = &DeployPusher{manager: manager, queue: queue}
	return &l
}

func (p *DeployPusher) OnEvent(event *RuntimeEvent) {
	switch event.EventName {
	case RuntimeDeployStatusChanged:
		if err := p.queue.Push(queue.DEPLOY_CONTINUING, strconv.Itoa(int(event.Deployment.ID))); err != nil {
			logrus.Errorf("[alert] failed to continue deploy, event: %v, err is %v", event, err.Error())
		}
	}
}
