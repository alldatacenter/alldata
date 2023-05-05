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

package taskerror

import (
	"context"
	"strconv"

	"github.com/erda-project/erda-proto-go/core/dop/taskerror/pb"
	errboxpb "github.com/erda-project/erda-proto-go/core/services/errorbox/pb"
	"github.com/erda-project/erda/apistructs"
)

func (s *TaskErrorService) List(param *apistructs.ErrorLogListRequest) ([]*pb.ErrorLog, error) {
	resourceIDs, resourceTypes, err := s.aggregateResources(param.ResourceType, param.ResourceID)
	if err != nil {
		return nil, err
	}
	rTypes := make([]string, 0)
	for _, t := range resourceTypes {
		rTypes = append(rTypes, string(t))
	}

	errLogs, err := s.errBoxSvc.ListErrorLog(context.Background(), &errboxpb.TaskErrorListRequest{
		ResourceIds:   resourceIDs,
		ResourceTypes: rTypes,
		StartTime:     param.StartTime,
	})
	if err != nil {
		return nil, err
	}
	return errLogs.List, nil
}

func (s *TaskErrorService) aggregateResources(resourceType apistructs.ErrorResourceType, resourceID string) ([]string,
	[]apistructs.ErrorResourceType, error) {
	resourceTypes, resourceIDs := []apistructs.ErrorResourceType{resourceType}, []string{resourceID}

	switch resourceType {
	case apistructs.PipelineError:
		pipelineID, err := strconv.ParseUint(resourceID, 10, 64)
		if err != nil {
			return nil, nil, err
		}
		runtimeIDs, err := s.FindRuntimeByPipelineID(pipelineID)
		if err != nil {
			return nil, nil, err
		}
		if len(runtimeIDs) != 0 {
			resourceIDs = append(resourceIDs, runtimeIDs...)
			resourceTypes = append(resourceTypes, apistructs.RuntimeError)
		}

		for _, v := range runtimeIDs {
			runtimeID, err := strconv.ParseUint(v, 10, 64)
			if err != nil {
				return nil, nil, err
			}
			addonIDs, err := s.FindAddonByRuntimeID(runtimeID)
			if err != nil {
				return nil, nil, err
			}
			if len(addonIDs) != 0 {
				resourceIDs = append(resourceIDs, addonIDs...)
				resourceTypes = append(resourceTypes, apistructs.AddonError)
			}
		}
	case apistructs.RuntimeError:
		runtimeID, err := strconv.ParseUint(resourceID, 10, 64)
		if err != nil {
			return nil, nil, err
		}
		addonIDs, err := s.FindAddonByRuntimeID(runtimeID)
		if err != nil {
			return nil, nil, err
		}
		if len(addonIDs) != 0 {
			resourceIDs = append(resourceIDs, addonIDs...)
			resourceTypes = append(resourceTypes, apistructs.AddonError)
		}
		// case apistructs.AddonError:
	}

	return resourceIDs, resourceTypes, nil
}

func (s *TaskErrorService) FindRuntimeByPipelineID(pipelineID uint64) ([]string, error) {
	pipelineDetail, err := s.bdl.GetPipeline(pipelineID)
	if err != nil {
		return nil, err
	}

	var resourceIDs []string
	for _, stage := range pipelineDetail.PipelineStages {
		for _, task := range stage.PipelineTasks {
			for _, metadata := range task.Result.Metadata {
				if metadata.Name == "runtimeID" {
					resourceIDs = append(resourceIDs, metadata.Value)
				}
			}
		}
	}

	return resourceIDs, nil
}

// FindAddonByRuntimeID 根据 runtime ID 获取 addon id
func (s *TaskErrorService) FindAddonByRuntimeID(runtimeID uint64) ([]string, error) {
	addons, err := s.bdl.ListAddonByRuntimeID(strconv.FormatUint(runtimeID, 10))
	if err != nil {
		return nil, err
	}

	var resourceIDs []string
	for _, addon := range addons.Data {
		resourceIDs = append(resourceIDs, addon.RealInstanceID)
	}

	return resourceIDs, nil
}
