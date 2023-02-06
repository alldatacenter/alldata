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
	"fmt"
	"os"

	"github.com/sirupsen/logrus"
)

// Teardown teardown agent, including prestop, callback.
func (agent *Agent) Teardown(exitCode ...int) {
	if len(exitCode) > 0 {
		agent.ExitCode = exitCode[0]
	}
	agent.PreStop()
	agent.stopWatchFiles()
	agent.Callback()
}

// Exit exit agent.
func (agent *Agent) Exit() {
	os.Exit(agent.ExitCode)
}

func (agent *Agent) writeEndFlagLine() {
	if _, err := fmt.Fprintf(agent.EasyUse.RunMultiStdout, "\n%s\n", agent.EasyUse.FlagEndLineForTail); err != nil {
		logrus.Println("stdout append flag err:", err)
	}
	if _, err := fmt.Fprintf(agent.EasyUse.RunMultiStderr, "\n%s\n", agent.EasyUse.FlagEndLineForTail); err != nil {
		logrus.Println("stderr append flag err:", err)
	}
}
