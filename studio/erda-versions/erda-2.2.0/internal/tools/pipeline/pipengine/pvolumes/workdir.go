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

package pvolumes

import (
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/metadata"
)

// GetAvailableTaskContainerWorkdirs 查询当前存在的所有 Workdir
func GetAvailableTaskContainerWorkdirs(tasks []spec.PipelineTask, currentTask spec.PipelineTask) map[string]string {
	workdirs := make(map[string]string)

	taskNameMap := make(map[string]struct{}, len(tasks))
	for _, task := range tasks {
		taskNameMap[task.Name] = struct{}{}
	}

	// 当前所有可用的 containerPaths
	containerPaths := GetAvailableTaskContainerPaths(tasks, currentTask)

	// 过滤出 taskName
	for name, containerPath := range containerPaths {
		if _, ok := taskNameMap[name]; !ok {
			continue
		}
		workdirs[name] = containerPath
	}

	return workdirs
}

func GetAvailableTaskContainerPaths(tasks []spec.PipelineTask, currentTask spec.PipelineTask) map[string]string {
	containerPaths := make(map[string]string)

	// 当前所有可用的 volumes
	for _, vo := range GetAvailableTaskOutStorages(tasks) {
		// 可用的 vo labels 里包含必要信息
		if vo.Labels == nil {
			continue
		}
		containerPath, ok := vo.Labels[VoLabelKeyContainerPath]
		if !ok {
			continue
		}
		containerPaths[vo.Name] = containerPath
	}

	// 可以通过 ref 引用自己
	for _, namespace := range currentTask.Extra.Action.Namespaces {
		containerPaths[namespace] = MakeTaskContainerWorkdir(namespace)
	}

	return containerPaths
}

func GetAvailableTaskOutStorages(tasks []spec.PipelineTask) []metadata.MetadataField {
	var volumes []metadata.MetadataField
	for _, task := range tasks {
		for _, out := range task.Context.OutStorages {
			volumes = append(volumes, out)
		}
	}
	return volumes
}
