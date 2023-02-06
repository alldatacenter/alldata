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

package pipelinesvc

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (s *PipelineSvc) TaskDetail(taskID uint64) (*spec.PipelineTask, error) {
	task, err := s.dbClient.GetPipelineTask(taskID)
	if err != nil {
		return nil, apierrors.ErrGetPipelineTaskDetail.InternalError(err)
	}
	return &task, nil
}

func (s *PipelineSvc) GetOpenapiOAuth2TokenForActionInvokeOpenapi(task *spec.PipelineTask) (*apistructs.OAuth2Token, error) {
	var tokenInfo *apistructs.OAuth2Token
	var err error
	req := apistructs.OAuth2TokenGetRequest{
		ClientID:     conf.OpenapiOAuth2TokenClientID(),
		ClientSecret: conf.OpenapiOAuth2TokenClientSecret(),
		Payload:      task.Extra.OpenapiOAuth2TokenPayload,
	}
	if s.edgeRegister.IsEdge() {
		tokenInfo, err = s.edgeRegister.GetOAuth2Token(req)
	} else {
		tokenInfo, err = s.bdl.GetOAuth2Token(req)
	}
	if err != nil {
		return nil, apierrors.ErrGetOpenapiOAuth2Token.InternalError(err)
	}
	if task.Extra.PrivateEnvs == nil {
		task.Extra.PrivateEnvs = make(map[string]string)
	}
	task.Extra.PrivateEnvs[apistructs.EnvOpenapiToken] = tokenInfo.AccessToken
	// store tokenInfo into task
	if err := s.dbClient.UpdatePipelineTaskExtra(task.ID, task.Extra); err != nil {
		logrus.Errorf("[alert] failed to update pipeline task extra to add %s, pipelineID: %d, taskID: %d, err: %v",
			apistructs.EnvOpenapiToken, task.PipelineID, task.ID, err)
	}
	return tokenInfo, nil
}
