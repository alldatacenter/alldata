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
	"os"
	"sync"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

var stdoutLogs = &[]apistructs.LogPushLine{}
var stderrLogs = &[]apistructs.LogPushLine{}
var stdoutLock sync.Mutex
var stderrLock sync.Mutex

// tailHandlerForPushCollectorLog push tailed log to collector
func tailHandlerForPushCollectorLog(line string, stream string, existLogLines *[]apistructs.LogPushLine, logID string, lock *sync.Mutex) {
	lock.Lock()
	defer lock.Unlock()
	*existLogLines = append(*existLogLines, apistructs.LogPushLine{
		ID:        logID,
		Source:    string(apistructs.DashboardSpotLogSourceJob),
		Stream:    &stream,
		Timestamp: time.Now().UnixNano(),
		Content:   line,
		Tags:      map[string]string{TagDiceOrgID: os.Getenv(apistructs.EnvDiceOrgID), TagDiceOrgName: os.Getenv(apistructs.EnvDiceOrgName)},
	})
}

const (
	asyncPushInterval = time.Second * 3
)

// asyncPushCollectorLog async push log to collector
func (agent *Agent) asyncPushCollectorLog() {
	// pushLogic do push logic
	pushLogic := func(logs *[]apistructs.LogPushLine, lock *sync.Mutex, _type string) {
		lock.Lock()
		defer lock.Unlock()
		if logs == nil || len(*logs) == 0 {
			logrus.Debugf("async push %s collector log, no log, skip", _type)
			return
		}
		logrus.Debugf("push log to collector, log lines: %d", len(*logs))
		err := agent.pushCollectorLog(logs)
		if err != nil {
			logrus.Error(err)
			// not refresh logs, try together at next time
			return
		}
		// refresh logs if success
		*logs = nil
	}

	// rangePush range push log to collector
	rangePush := func(logs *[]apistructs.LogPushLine, lock *sync.Mutex, _type string) {
		ticker := time.NewTicker(asyncPushInterval)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				logrus.Debugf("collector: range interval push %s", _type)
				pushLogic(logs, lock, _type)
			case <-agent.Ctx.Done():
				// sleep for waiting tail file handler
				waitTail := time.Second * 1
				logrus.Debugf("collector: recevied agent exit channel, wait %s and push %s directly", waitTail.String(), _type)
				time.Sleep(waitTail)
				pushLogic(logs, lock, _type)
				return
			}
		}
	}

	// start range
	go rangePush(stdoutLogs, &stdoutLock, "stdout")
	go rangePush(stderrLogs, &stderrLock, "stderr")

}

func (agent *Agent) pushCollectorLog(logLines *[]apistructs.LogPushLine) error {
	return agent.CallbackReporter.PushCollectorLog(logLines)
}
