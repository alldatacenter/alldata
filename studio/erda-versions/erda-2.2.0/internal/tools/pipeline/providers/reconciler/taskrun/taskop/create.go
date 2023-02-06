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

package taskop

import (
	"context"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun"
)

type create taskrun.TaskRun

func NewCreate(tr *taskrun.TaskRun) *create {
	return (*create)(tr)
}

func (c *create) Op() taskrun.Op {
	return taskrun.Create
}

func (c *create) TaskRun() *taskrun.TaskRun {
	return (*taskrun.TaskRun)(c)
}

func (c *create) Processing() (interface{}, error) {
	_, err := c.Executor.Create(c.Ctx, c.Task)
	return nil, err
}

func (c *create) WhenDone(data interface{}) error {
	c.Task.Status = apistructs.PipelineStatusCreated
	logrus.Infof("reconciler: pipelineID: %d, taskID: %d, taskName: %s, end create (%s -> %s)",
		c.P.ID, c.Task.ID, c.Task.Name, apistructs.PipelineStatusBorn, apistructs.PipelineStatusCreated)
	return nil
}

func (c *create) WhenLogicError(err error) error {
	c.Task.Status = apistructs.PipelineStatusCreateError
	return nil
}

func (c *create) WhenTimeout() error {
	return nil
}

func (c *create) WhenCancel() error {
	return c.TaskRun().WhenCancel()
}

func (c *create) TimeoutConfig() (<-chan struct{}, context.CancelFunc, time.Duration) {
	return nil, nil, -1
}

func (c *create) TuneTriggers() taskrun.TaskOpTuneTriggers {
	return taskrun.TaskOpTuneTriggers{
		BeforeProcessing: aoptypes.TuneTriggerTaskBeforeCreate,
		AfterProcessing:  aoptypes.TuneTriggerTaskAfterCreate,
	}
}
