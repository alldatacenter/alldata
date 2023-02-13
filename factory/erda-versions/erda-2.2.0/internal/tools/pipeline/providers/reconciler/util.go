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

package reconciler

import (
	"context"
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (r *provider) mustFetchPipelineDetail(ctx context.Context, pipelineID uint64) *spec.Pipeline {
	for {
		select {
		case <-ctx.Done():
			r.Log.Errorf("failed to fetch pipeline detail(no retry), pipelineID: %d, err: %v", pipelineID, ctx.Err())
			return nil
		default:
			p, exist, err := r.dbClient.GetPipelineWithExistInfo(pipelineID)
			if err != nil {
				r.Log.Errorf("failed to fetch pipeline detail(auto retry), pipelineID: %d, err: %v", pipelineID, err)
				time.Sleep(r.Cfg.RetryInterval)
				continue
			}
			if !exist {
				r.Log.Errorf("failed to fetch pipeline detail(no retry), pipelineID: %d, err: %v", pipelineID, dbclient.ErrRecordNotFound)
				return nil
			}
			return &p
		}
	}
}

// YmlTaskMergeDBTasks .
// parse out tasks according to the yml structure, and then query the created tasks from the database,
// and replace the tasks that already exist in the database with yml tasks
func (r *provider) YmlTaskMergeDBTasks(pipeline *spec.Pipeline) ([]*spec.PipelineTask, error) {
	// get pipeline tasks from db
	tasks, err := r.dbClient.ListPipelineTasksByPipelineID(pipeline.ID)
	if err != nil {
		return nil, err
	}

	// get or set stages from caches
	stages, err := r.Cache.GetOrSetStagesFromContext(pipeline.ID)
	if err != nil {
		return nil, err
	}

	// get or set pipelineYml from caches
	pipelineYml, err := r.Cache.GetOrSetPipelineYmlFromContext(pipeline.ID)
	if err != nil {
		return nil, err
	}

	passedDataWhenCreate, err := r.Cache.GetOrSetPassedDataWhenCreateFromContext(pipelineYml, pipeline)
	if err != nil {
		return nil, err
	}

	tasks, err = r.pipelineSvcFuncs.MergePipelineYmlTasks(pipelineYml, tasks, pipeline, stages, passedDataWhenCreate)
	if err != nil {
		return nil, err
	}

	var newTasks []*spec.PipelineTask
	for index := range tasks {
		task := tasks[index]
		newTasks = append(newTasks, &task)
	}

	return newTasks, err
}
