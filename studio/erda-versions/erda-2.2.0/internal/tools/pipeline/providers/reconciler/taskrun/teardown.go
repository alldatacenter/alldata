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

package taskrun

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/pkg/strutil"
)

// Teardown tear down task.
func (tr *TaskRun) Teardown() {
	logrus.Infof("reconciler: pipelineID: %d, task %q begin tear down", tr.P.ID, tr.Task.Name)
	defer logrus.Infof("reconciler: pipelineID: %d, taskID: %d, taskName: %s, end tear down", tr.P.ID, tr.Task.ID, tr.Task.Name)
	// handle aop synchronously, then do subsequent tasks
	_ = aop.Handle(aop.NewContextForTask(*tr.Task, *tr.P, aoptypes.TuneTriggerTaskAfterExec))

	// invalidate openapi oauth2 token
	tokens := strutil.DedupSlice([]string{
		tr.Task.Extra.PublicEnvs[apistructs.EnvOpenapiTokenForActionBootstrap],
		tr.Task.Extra.PrivateEnvs[apistructs.EnvOpenapiToken],
	}, true)
	for _, token := range tokens {
		_, err := tr.Bdl.InvalidateOAuth2Token(apistructs.OAuth2TokenInvalidateRequest{AccessToken: token})
		if err != nil {
			logrus.Errorf("[alert] reconciler: pipelineID: %d, taskID: %d, task %q failed to invalidate openapi oauth2 token, token: %s, err: %v",
				tr.P.ID, tr.Task.ID, tr.Task.Name, token, err)
		}
	}
}
