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

// @Title  this file is used to query the action definition
// @Description  query action definition and spec
package action_info

import (
	"sync"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// passedDataWhenCreate stores data passed recursively when create graph.
type PassedDataWhenCreate struct {
	bdl              *bundle.Bundle
	actionJobDefines *sync.Map
	actionJobSpecs   *sync.Map
	actionMgr        actionmgr.Interface
}

func (that *PassedDataWhenCreate) GetActionJobDefine(actionTypeVersion string) *diceyml.Job {
	if that == nil {
		return nil
	}
	if that.actionJobDefines == nil {
		return nil
	}

	if value, ok := that.actionJobDefines.Load(actionTypeVersion); ok {
		if job, ok := value.(*diceyml.Job); ok {
			return job
		}
	}
	return nil
}

func (that *PassedDataWhenCreate) GetActionJobSpecs(actionTypeVersion string) *apistructs.ActionSpec {

	if that == nil {
		return nil
	}
	if that.actionJobDefines == nil {
		return nil
	}

	if value, ok := that.actionJobSpecs.Load(actionTypeVersion); ok {
		if spec, ok := value.(*apistructs.ActionSpec); ok {
			return spec
		}
	}
	return nil
}

func (that *PassedDataWhenCreate) InitData(bdl *bundle.Bundle, actionMgr actionmgr.Interface) {
	if that == nil {
		return
	}

	if that.actionJobDefines == nil {
		that.actionJobDefines = &sync.Map{}
	}
	if that.actionJobSpecs == nil {
		that.actionJobSpecs = &sync.Map{}
	}
	that.actionMgr = actionMgr
	that.bdl = bdl
}

func (that *PassedDataWhenCreate) PutPassedDataByPipelineYml(pipelineYml *pipelineyml.PipelineYml, p *spec.Pipeline) error {
	if that == nil {
		return nil
	}
	// batch search extensions
	var extItems []string
	for _, stage := range pipelineYml.Spec().Stages {
		for _, typedAction := range stage.Actions {
			for _, action := range typedAction {
				if action.Type.IsSnippet() {
					continue
				}
				extItem := that.actionMgr.MakeActionTypeVersion(action)
				// extension already searched, skip
				if _, ok := that.actionJobDefines.Load(extItem); ok {
					continue
				}
				extItems = append(extItems, that.actionMgr.MakeActionTypeVersion(action))
			}
		}
	}

	extItems = strutil.DedupSlice(extItems, true)
	actionJobDefines, actionJobSpecs, err := that.actionMgr.SearchActions(extItems, that.actionMgr.MakeActionLocationsBySource(p.PipelineSource))
	if err != nil {
		return apierrors.ErrCreatePipelineGraph.InternalError(err)
	}

	for extItem, actionJobDefine := range actionJobDefines {
		that.actionJobDefines.Store(extItem, actionJobDefine)
	}
	for extItem, actionJobSpec := range actionJobSpecs {
		that.actionJobSpecs.Store(extItem, actionJobSpec)
	}
	return nil
}
