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

package logic

import (
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/tools/pipeline/pkg/task_uuid"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

var (
	errMissingNamespace = errors.New("action missing namespace")
	errMissingUUID      = errors.New("action missing UUID")
)

type ErrorWrapper struct {
	name string
}

func NewErrorWrapper(name string) *ErrorWrapper {
	return &ErrorWrapper{name: name}
}

func (e *ErrorWrapper) WrapTaskError(err *error, op string, task *spec.PipelineTask) {
	if err == nil || *err == nil {
		return
	}
	*err = errors.Errorf("failed to invoke %s to %s, actionInfo: %s, err: %v", e.name, op, PrintTaskInfo(task), *err)
}

func PrintTaskInfo(task *spec.PipelineTask) string {
	return fmt.Sprintf("pipelineID: %d, id: %d, name: %s, namespace: %s, schedulerJobID: %s, clusterName: %s",
		task.PipelineID, task.ID, task.Name, task.Extra.Namespace, task_uuid.MakeJobID(task), task.Extra.ClusterName)
}

func ValidateAction(action *spec.PipelineTask) error {
	if action.Extra.Namespace == "" {
		return errMissingNamespace
	}
	if action.Extra.UUID == "" {
		return errMissingUUID
	}
	return nil
}
