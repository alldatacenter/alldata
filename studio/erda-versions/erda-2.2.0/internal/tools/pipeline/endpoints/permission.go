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

package endpoints

import (
	"fmt"
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

// checkPipelineCronPermission 包装校验 app 权限
func (e *Endpoints) checkAppPermission(r *http.Request, appID uint64, permissionAction string) error {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return err
	}
	return e.permissionSvc.CheckApp(identityInfo, appID, permissionAction)
}

// CheckBranch 方便用户直接使用分支进行鉴权
func (e *Endpoints) checkBranchPermission(r *http.Request, appIDStr, branch string, permissionAction string) error {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return err
	}
	if identityInfo.IsInternalClient() {
		return nil
	}
	// TODO adaptor
	return nil
}

// checkPipelineOperatePermission 包装校验 pipeline 编辑操作权限
func (e *Endpoints) checkPipelineOperatePermission(r *http.Request, pipelineID uint64,
	operateRequest apistructs.PipelineOperateRequest, permissionAction string) error {

	// Verify that the pipeline is legal
	p, err := e.dbClient.GetPipeline(pipelineID)
	if err != nil {
		return apierrors.ErrGetPipeline.InvalidParameter(err)
	}

	yml, err := pipelineyml.New([]byte(p.PipelineYml))
	if err != nil {
		return apierrors.ErrCheckPermission.InvalidParameter(
			fmt.Sprintf("yml parse error: %v", p.ID))
	}

	stages, err := e.dbClient.ListPipelineStageByPipelineID(p.ID)
	if err != nil {
		return apierrors.ErrCheckPermission.InvalidParameter(
			fmt.Sprintf("list pipeline stages error pipelineID: %v", p.ID))
	}

	tasks, err := e.pipelineSvc.MergePipelineYmlTasks(yml, nil, &p, stages, nil)
	if err != nil {
		return apierrors.ErrListPipelineTasks.InvalidParameter(err)
	}

	if err = checkTaskOperatesBelongToTasks(operateRequest.TaskOperates, tasks); err != nil {
		return err
	}

	return e.checkBranchPermission(r, p.Labels[apistructs.LabelAppID], p.Labels[apistructs.LabelBranch], permissionAction)
}

func checkTaskOperatesBelongToTasks(taskOperates []apistructs.PipelineTaskOperateRequest, tasks []spec.PipelineTask) error {
	validTaskMap := make(map[string]struct{}, 0)
	for _, task := range tasks {
		validTaskMap[task.Name] = struct{}{}
	}
	for _, taskReq := range taskOperates {
		if _, ok := validTaskMap[taskReq.TaskAlias]; !ok {
			return apierrors.ErrCheckPermission.InvalidParameter(
				fmt.Sprintf("task not belong to pipeline, taskAlias: %s", taskReq.TaskAlias))
		}
	}
	return nil
}
