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

package cap

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
)

type CapImpl struct{}

type Cap interface {
	CapacityInfo(clustername string) apistructs.CapacityInfoData
}

func NewCapImpl() Cap {
	return &CapImpl{}
}

func (cap *CapImpl) CapacityInfo(clustername string) apistructs.CapacityInfoData {
	executorname := clusterutil.GenerateExecutorByCluster(clustername, clusterutil.ServiceKindMarathon)
	extor, err := executor.GetManager().Get(executortypes.Name(executorname))
	if err != nil {
		return apistructs.CapacityInfoData{}
	}
	return extor.CapacityInfo()
}
