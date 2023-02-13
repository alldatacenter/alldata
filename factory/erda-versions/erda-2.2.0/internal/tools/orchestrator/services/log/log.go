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

package log

import (
	"fmt"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/monitor"
)

type DeployLogHelper struct {
	DeploymentID string
	Bdl          *bundle.Bundle
}

const TAG_ORG_NAME = string(monitor.TAGKEY_ORG_NAME)

func (d *DeployLogHelper) Log(content string, tags map[string]string) {
	content = "(orchestrator) " + content
	logrus.Debugf("deployment log -> %s", content)
	timestamp := time.Now().UnixNano()
	line := fmt.Sprintf("%s\n", content)
	lines := []apistructs.LogPushLine{
		{Source: "deploy", ID: d.DeploymentID, Content: line, Timestamp: timestamp, Tags: tags},
	}
	// TODO: buffer
	if err := d.Bdl.PushLog(&apistructs.LogPushRequest{Lines: lines}); err != nil {
		logrus.Errorf("[alert] failed to pushLog, deploymentId: %s, last err: %v",
			d.DeploymentID, err)
	}
}
