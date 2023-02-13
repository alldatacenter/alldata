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

package actionrunner

import (
	"io/ioutil"
	"os"
	"os/exec"
	"path"
	"strings"
	"sync/atomic"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/template"
)

// Runner .
type Runner struct {
	Conf  *Conf
	queue chan *Task
	tasks int32
}

// New .
func New(Conf *Conf) *Runner {
	return &Runner{
		Conf:  Conf,
		queue: make(chan *Task, Conf.MaxTask),
	}
}

// Run .
func (r *Runner) Run() error {
	err := r.runStartUpCommands()
	if err != nil {
		return err
	}
	for i := 0; i < r.Conf.MaxTask; i++ {
		go r.worker()
	}
	go r.cleanBuildDir()
	r.reloadTasks()
	return nil
}

func (r *Runner) runStartUpCommands() error {
	for _, commandTemplate := range r.Conf.StartupCommands {
		commandStr := template.Render(commandTemplate, r.Conf.Params)
		cmd := exec.Command("/bin/bash", "-c", commandStr)
		cmd.Stderr = os.Stderr
		cmd.Stdout = os.Stdout
		err := cmd.Run()
		if err != nil {
			return err
		}
	}
	return nil
}

func (r *Runner) cleanBuildDir() {
	interval := 15 * time.Minute
	for {
		fileInfoList, err := ioutil.ReadDir(r.Conf.BuildPath)
		if err != nil {
			logrus.Errorf("failed to read build dir %s err:%s", r.Conf.BuildPath, err)
		} else {
			for _, info := range fileInfoList {
				if info.IsDir() && strings.HasPrefix(info.Name(), "pipeline-task") {
					if info.ModTime().Add(time.Hour*time.Duration(r.Conf.FailedTaskKeepHours)).Unix() < time.Now().Unix() {
						buildDir := path.Join(r.Conf.BuildPath, info.Name())
						logrus.Infof("remove build dir %s", buildDir)
						os.RemoveAll(buildDir)
					}
				}
			}
		}
		time.Sleep(interval)
	}
}

func (r *Runner) reloadTasks() {
	interval := 20 * time.Second
	for {
		tasks := atomic.LoadInt32(&r.tasks)
		if tasks < int32(r.Conf.MaxTask) {
			list := r.fetchTasks()
			for _, task := range list {
				r.queue <- task
			}
		}
		time.Sleep(interval)
		if tasks > 0 {
			logrus.Infof("running task %d", tasks)
		}
	}
}

func (r *Runner) worker() {
	log := r.newLogger()
	for task := range r.queue {
		atomic.AddInt32(&r.tasks, 1)
		w := &worker{
			Task:        task,
			r:           r,
			conf:        r.Conf,
			contextPath: path.Join(r.Conf.BuildPath, task.JobID),
			log:         newJobLogger(log, task.JobID, task.Token),
		}
		w.Execute()
		atomic.AddInt32(&r.tasks, -1)
	}
}
