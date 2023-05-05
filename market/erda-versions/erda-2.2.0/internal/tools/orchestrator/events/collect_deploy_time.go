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
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

type DeployTimeCollector struct {
	manager *EventManager
	db      *dbclient.DBClient
}

func NewDeployTimeCollector(manager *EventManager, db *dbclient.DBClient) *EventListener {
	var l EventListener = &DeployTimeCollector{manager: manager, db: db}
	return &l
}

func (c *DeployTimeCollector) OnEvent(event *RuntimeEvent) {
	c.collectDeployTimes(event)
}

func (c *DeployTimeCollector) collectDeployTimes(event *RuntimeEvent) {
	// only care DeployStatusChanged events
	if event.EventName != RuntimeDeployStatusChanged {
		return
	}

	deployment, err := c.db.GetDeployment(event.Deployment.ID)
	if err != nil {
		logrus.Warnf("failed to get deployment: %v for time collector, err: %v",
			event.Deployment.ID, err.Error())
		return
	}
	if deployment == nil {
		// not found
		return
	}

	isChanged := true
	now := time.Now()
	// TODO: need refactor, should introduce deployment_phases table to tracking times
	// TODO: Script is a virtual Phase now, it indicate the `pre-service` Phase
	switch deployment.Status {
	case apistructs.DeploymentStatusDeploying:
		// Status is Deploying, entering the Phase, so set the StartAt
		switch deployment.Phase {
		case apistructs.DeploymentPhaseAddon:
			deployment.Extra.AddonPhaseStartAt = &now
		case apistructs.DeploymentPhaseScript:
			// the end of addon, service start at same time
			deployment.Extra.AddonPhaseEndAt = &now
			deployment.Extra.ServicePhaseStartAt = &now
		case apistructs.DeploymentPhaseCompleted:
			deployment.Extra.ServicePhaseEndAt = &now
		default:
			isChanged = false
		}
	case apistructs.DeploymentStatusFailed, apistructs.DeploymentStatusCanceled:
		// Status is Failed or Canceled, Phase broken (end) at current time
		switch deployment.Phase {
		case apistructs.DeploymentPhaseAddon:
			deployment.Extra.AddonPhaseEndAt = &now
		case apistructs.DeploymentPhaseScript, apistructs.DeploymentPhaseService:
			deployment.Extra.ServicePhaseEndAt = &now
		default:
			isChanged = false
		}
	default:
		isChanged = false
	}

	if isChanged {
		if err := c.db.UpdateDeployment(deployment); err != nil {
			logrus.Errorf("[alert] failed to update Phase Times into deployment: %d, %v",
				deployment.ID, err)
		}
	}
}
