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

package resourceinfo

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
)

type ResourceInfo interface {
	Info(clusterName string, brief bool) (apistructs.ClusterResourceInfoData, error)
}

type ResourceInfoImpl struct {
}

func NewResourceInfoImpl() ResourceInfo {
	return &ResourceInfoImpl{}
}

func (r *ResourceInfoImpl) Info(clusterName string, brief bool) (apistructs.ClusterResourceInfoData, error) {
	executorName := clusterutil.GenerateExecutorByClusterName(clusterName)
	executor, err := executor.GetManager().Get(executortypes.Name(executorName))
	if err != nil {
		return apistructs.ClusterResourceInfoData{}, fmt.Errorf("not found executor: %s", executorName)
	}
	return executor.ResourceInfo(brief)
}
