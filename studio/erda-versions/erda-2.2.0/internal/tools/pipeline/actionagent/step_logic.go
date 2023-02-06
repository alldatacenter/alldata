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
	"io"
	"os"
	"os/exec"

	"github.com/sirupsen/logrus"
)

func (agent *Agent) logic() {

	// go to ${WORKDIR}
	if err := os.Chdir(agent.EasyUse.ContainerWd); err != nil {
		agent.AppendError(err)
		return
	}

	actionRun := exec.Command(agent.EasyUse.RunScript)
	actionRun.Stdout = io.MultiWriter(agent.EasyUse.RunMultiStdout)
	actionRun.Stderr = io.MultiWriter(agent.EasyUse.RunMultiStderr)
	if err := actionRun.Start(); err != nil {
		agent.AppendError(err)
		return
	}
	agent.EasyUse.RunProcess = actionRun.Process
	if err := actionRun.Wait(); err != nil {
		agent.ExitCode = 1
		if err.Error() != fmt.Sprintf(`command "%s": exit status 1`, agent.EasyUse.RunScript) {
			logrus.Println(err)
			agent.AppendError(err)
		}
	}
}
