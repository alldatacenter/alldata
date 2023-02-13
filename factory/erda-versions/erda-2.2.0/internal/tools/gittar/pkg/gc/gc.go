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

package gc

import (
	"fmt"
	"io/ioutil"
	"os/exec"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/pkg/cron"
	"github.com/erda-project/erda/pkg/limit_sync_group"
)

// all gittar repository root address
var repositoryRootAddr = "/repository"

// gc repository concurrent number
var concurrentNum = 0

// gc repository cron time
var cronExpression = ""

// cron task to clean repository
func ScheduledExecuteClean() {
	cronProcess := cron.New(
		cron.WithoutDLock(true),
	)

	cronExpression = conf.GitGCCronExpression()
	concurrentNum = conf.GitGCMaxNum()
	repositoryRootAddr = conf.RepoRoot()

	err := cronProcess.AddFunc(cronExpression, repositoryClean)
	if err != nil {
		panic(fmt.Errorf("cannot perform cleanup scheduled tasks, error: %v", err))
	}
	cronProcess.Start()
}

// query the project according to the root directory,
// then query each application according to the project,
// enter the application directory and execute 'git gc'
func repositoryClean() {
	logrus.Infof("gc: start gc")
	defer logrus.Infof("gc: end gc")
	projectFileInfos, err := ioutil.ReadDir(repositoryRootAddr)
	if err != nil {
		logrus.Errorf("gc: read dir %s error: %v", repositoryRootAddr, err)
		return
	}
	// initialize a waitGroup according to the number of concurrent
	var wait = limit_sync_group.NewSemaphore(concurrentNum)
	for _, projectFileInfo := range projectFileInfos {
		if !projectFileInfo.IsDir() {
			continue
		}
		var projectPath = repositoryRootAddr + "/" + projectFileInfo.Name()
		applicationFileInfos, err := ioutil.ReadDir(projectPath)
		if err != nil {
			logrus.Errorf("gc: read dir %s error: %v", projectPath, err)
			continue
		}
		for _, appFileInfo := range applicationFileInfos {
			if !appFileInfo.IsDir() {
				continue
			}
			// concurrently execute git gc commands
			wait.Add(1)
			go func(path string) {
				defer wait.Done()
				doGcCommand(path)
			}(projectPath + "/" + appFileInfo.Name())
		}
	}
	wait.Wait()
}

// execute the git gc command and print the returned information or error
func doGcCommand(path string) {
	// remove spaces to prevent injection attacks
	path = strings.Replace(path, " ", "", -1)
	cmd := exec.Command("sh", "-c", "cd '"+path+"' && git gc")
	output, err := cmd.CombinedOutput()
	logrus.Infof("gc: start gc path: %v", path)
	if err != nil {
		logrus.Errorf("gc: command run error: %v", err)
		return
	}
	logrus.Infof("gc: command output: %s", string(output))
	logrus.Infof("gc: end gc path: %v", path)
}
