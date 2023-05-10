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
	"encoding/base64"
	"encoding/json"
	"io"
	"io/ioutil"
	"os"
	"strconv"

	"github.com/sirupsen/logrus"
)

func (agent *Agent) Execute(r io.Reader) {

	// log level
	debug, _ := strconv.ParseBool(os.Getenv("ACTIONAGENT_DEBUG"))
	if debug {
		logrus.SetLevel(logrus.DebugLevel)
	}

	agent.getOpenAPIInfo()
	if len(agent.Errs) > 0 {
		return
	}

	agent.SetCallbackReporter()
	if len(agent.Errs) > 0 {
		return
	}

	agent.parseArg(r)
	if len(agent.Errs) > 0 {
		return
	}

	agent.pullBootstrapInfo()
	if len(agent.Errs) > 0 {
		return
	}

	// 1. validate
	agent.validate()
	if len(agent.Errs) > 0 {
		return
	}

	// 2. prepare
	agent.prepare()
	if len(agent.Errs) > 0 {
		return
	}
	defer func() {
		// it's not necessary to write flag end line for tail, because tail watcher listen context to end
		agent.writeEndFlagLine()
	}()

	// 3. restore / store
	agent.restore()
	if len(agent.Errs) > 0 {
		return
	}
	defer func() {
		agent.store()
	}()

	// 4. logic
	agent.logic()
	if len(agent.Errs) > 0 {
		return
	}
}

func (agent *Agent) parseArg(r io.Reader) {
	// base64 decode
	encodedArg, err := ioutil.ReadAll(r)
	if err != nil {
		agent.AppendError(err)
		return
	}
	decodedArg, err := base64.StdEncoding.DecodeString(string(encodedArg))
	if err != nil {
		agent.AppendError(err)
		return
	}
	agent.Arg = &AgentArg{}
	if err := json.Unmarshal(decodedArg, agent.Arg); err != nil {
		agent.AppendError(err)
		return
	}
}
