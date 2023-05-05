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

package metrics

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func taskErrorLog(task spec.PipelineTask, format string, args ...interface{}) {
	logrus.WithField("type", "metrics").WithField("pipelineID", task.PipelineID).WithField("taskID", task.ID).Errorf(format, args...)
}

func taskDebugLog(task spec.PipelineTask, format string, args ...interface{}) {
	logrus.WithField("type", "metrics").WithField("pipelineID", task.PipelineID).WithField("taskID", task.ID).Debugf(format, args...)
}

func pipelineErrorLog(p spec.Pipeline, format string, args ...interface{}) {
	logrus.WithField("type", "metrics").WithField("pipelineID", p.ID).Errorf(format, args...)
}

func pipelineDebugLog(p spec.Pipeline, format string, args ...interface{}) {
	logrus.WithField("type", "metrics").WithField("pipelineID", p.ID).Debugf(format, args...)
}
