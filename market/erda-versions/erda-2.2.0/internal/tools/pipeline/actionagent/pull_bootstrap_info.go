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

package actionagent

import (
	"encoding/json"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/retry"
)

const EncryptedValueMinLen = "ACTIONAGENT_ENCRYPTED_VAlUE_MIN_LEN"

func (agent *Agent) pullBootstrapInfo() {
	if !agent.Arg.PullBootstrapInfo {
		return
	}

	tokenForBootstrap := os.Getenv(apistructs.EnvOpenapiTokenForActionBootstrap)
	if tokenForBootstrap == "" {
		agent.AppendError(errors.Errorf("missing env %s", apistructs.EnvOpenapiTokenForActionBootstrap))
		return
	}
	agent.EasyUse.TokenForBootstrap = tokenForBootstrap

	var getResp apistructs.PipelineTaskGetBootstrapInfoResponse
	err := retry.DoWithInterval(func() error {
		var err error
		getResp, err = agent.CallbackReporter.GetBootstrapInfo(agent.Arg.PipelineID, agent.Arg.PipelineTaskID)
		if err != nil {
			return err
		}
		return nil
	}, 5, time.Second*5)
	if err != nil {
		agent.AppendError(errors.Errorf("failed to get bootstrap info, err: %v", err))
		return
	}

	var bootstrapArg AgentArg
	if err := json.Unmarshal(getResp.Data.Data, &bootstrapArg); err != nil {
		agent.AppendError(err)
		return
	}

	agent.Arg.Commands = bootstrapArg.Commands
	agent.Arg.Context = bootstrapArg.Context
	agent.Arg.PrivateEnvs = bootstrapArg.PrivateEnvs
	agent.Arg.EncryptSecretKeys = bootstrapArg.EncryptSecretKeys

	valueLen, err := strconv.Atoi(os.Getenv(EncryptedValueMinLen))
	if err != nil || valueLen < 6 {
		valueLen = 6
	}

	for _, v := range agent.Arg.EncryptSecretKeys {
		// the value's len >= EncryptedValueLen will be appended to BlackList
		if value, ok := agent.Arg.PrivateEnvs[strings.ToUpper(v)]; ok && len(value) >= valueLen {
			agent.TextBlackList = append(agent.TextBlackList, value)
		}
	}

	// set envs to current process, so `run` and other scripts can inherit
	for k, v := range agent.Arg.PrivateEnvs {
		if err = os.Setenv(k, v); err != nil {
			agent.AppendError(err)
			return
		}
		if k == apistructs.EnvOpenapiToken {
			agent.CallbackReporter.SetOpenApiToken(v)
			agent.EasyUse.OpenAPIToken = v
		}
	}
}
