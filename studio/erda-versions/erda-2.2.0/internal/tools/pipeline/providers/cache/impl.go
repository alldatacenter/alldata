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

// @Title  This file is used to cache the objects that need to be reused in the reconciler
// @Description  The cached data is placed in the global sync.Map, you need to call clearPipelineContextCaches to clean up the corresponding data after you use it at will
package cache

import (
	"fmt"

	"github.com/erda-project/erda/internal/tools/pipeline/pkg/action_info"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const (
	pipelineContextCacheKey                            = "reconciler_caches"
	pipelineStagesContextCachesPrefixKey               = "reconciler_caches_stages"
	pipelineYmlContextCachesPrefixKey                  = "reconciler_caches_yml"
	pipelineRerunSuccessTaskContextCachesPrefixKey     = "reconciler_caches_rerun_success_tasks"
	pipelinePassedDataWhenCreateContextCachesPrefixKey = "reconciler_caches_passed_data_when_create"
	pipelineSecretCacheKey                             = "pipeline_secret"
)

// ClearReconcilerPipelineContextCaches clear context map rwLock by pipelineID
func (p *provider) ClearReconcilerPipelineContextCaches(pipelineID uint64) {
	p.cacheMap.Delete(makeMapKey(pipelineID, pipelineStagesContextCachesPrefixKey))
	p.cacheMap.Delete(makeMapKey(pipelineID, pipelineYmlContextCachesPrefixKey))
	p.cacheMap.Delete(makeMapKey(pipelineID, pipelineRerunSuccessTaskContextCachesPrefixKey))
	p.cacheMap.Delete(makeMapKey(pipelineID, pipelinePassedDataWhenCreateContextCachesPrefixKey))
}

// get cache map from context
func makeMapKey(pipelineID uint64, key string) string {
	return fmt.Sprintf("%v_%v_%v", pipelineID, pipelineContextCacheKey, key)
}

// obtain all types of values from the map according to the key, and then do the conversion at each call
func (p *provider) getInterfaceValueByKey(pipelineID uint64, key string) interface{} {
	value, ok := p.cacheMap.Load(makeMapKey(pipelineID, key))
	if !ok {
		return nil
	}
	return value
}

// ----------- caches pipeline stages

// GetOrSetStagesFromContext
// get value from caches map
// if not exist search value from db and save to caches map
func (p *provider) GetOrSetStagesFromContext(pipelineID uint64) (stages []spec.PipelineStage, err error) {
	stages = p.getStagesCachesFromContextByPipelineID(pipelineID)
	if stages != nil {
		return stages, nil
	}

	stages, err = p.dbClient.ListPipelineStageByPipelineID(pipelineID)
	if err != nil {
		return nil, err
	}

	p.setStagesCachesToContextByPipelineID(stages, pipelineID)
	return stages, nil
}

// get stages from context caches map by pipelineID
func (p *provider) getStagesCachesFromContextByPipelineID(pipelineID uint64) []spec.PipelineStage {
	value := p.getInterfaceValueByKey(pipelineID, pipelineStagesContextCachesPrefixKey)
	if value == nil {
		return nil
	}
	stages, ok := value.([]spec.PipelineStage)
	if !ok {
		return nil
	}

	return stages
}

func (p *provider) setStagesCachesToContextByPipelineID(stages []spec.PipelineStage, pipelineID uint64) {
	p.cacheMap.Store(makeMapKey(pipelineID, pipelineStagesContextCachesPrefixKey), stages)
}

// -------- cache PipelineYml

// GetOrSetPipelineYmlFromContext
// get value from caches map
// if not exist search value from db and save to caches map
func (p *provider) GetOrSetPipelineYmlFromContext(pipelineID uint64) (yml *pipelineyml.PipelineYml, err error) {
	yml = p.getPipelineYmlCachesFromContextByPipelineID(pipelineID)
	if yml != nil {
		return yml, nil
	}
	pipeline, err := p.dbClient.GetPipeline(pipelineID)
	if err != nil {
		return nil, err
	}
	pipelineYml, err := pipelineyml.New(
		[]byte(pipeline.PipelineYml),
	)
	if err != nil {
		return nil, err
	}

	p.setPipelineYmlCachesToContextByPipelineID(pipelineYml, pipelineID)
	return pipelineYml, nil
}

func (p *provider) getPipelineYmlCachesFromContextByPipelineID(pipelineID uint64) *pipelineyml.PipelineYml {
	value := p.getInterfaceValueByKey(pipelineID, pipelineYmlContextCachesPrefixKey)
	if value == nil {
		return nil
	}
	yml, ok := value.(*pipelineyml.PipelineYml)
	if !ok {
		return nil
	}

	return yml
}

func (p *provider) setPipelineYmlCachesToContextByPipelineID(yml *pipelineyml.PipelineYml, pipelineID uint64) {
	p.cacheMap.Store(makeMapKey(pipelineID, pipelineYmlContextCachesPrefixKey), yml)
}

// ------- cache pre pipeline success tasks map[string]*spec.PipelineTask

// GetOrSetPipelineRerunSuccessTasksFromContext
// get value from caches map
// if not exist search value from db and save to caches map
func (p *provider) GetOrSetPipelineRerunSuccessTasksFromContext(pipelineID uint64) (successTasks map[string]*spec.PipelineTask, err error) {
	successTasks = p.getPipelineRerunSuccessTasksFromContextByPipelineID(pipelineID)
	if successTasks != nil {
		return successTasks, nil
	}
	pipeline, err := p.dbClient.GetPipeline(pipelineID)
	if err != nil {
		return nil, err
	}
	lastSuccessTaskMap, _, err := p.dbClient.ParseRerunFailedDetail(pipeline.Extra.RerunFailedDetail)
	if err != nil {
		return nil, err
	}
	p.setPipelineRerunSuccessTasksToContextByPipelineID(lastSuccessTaskMap, pipelineID)
	return lastSuccessTaskMap, nil
}

func (p *provider) getPipelineRerunSuccessTasksFromContextByPipelineID(pipelineID uint64) map[string]*spec.PipelineTask {
	value := p.getInterfaceValueByKey(pipelineID, pipelineRerunSuccessTaskContextCachesPrefixKey)
	if value == nil {
		return nil
	}
	successTasks, ok := value.(map[string]*spec.PipelineTask)
	if !ok {
		return nil
	}

	return successTasks
}

func (p *provider) setPipelineRerunSuccessTasksToContextByPipelineID(successTasks map[string]*spec.PipelineTask, pipelineID uint64) {
	p.cacheMap.Store(makeMapKey(pipelineID, pipelineRerunSuccessTaskContextCachesPrefixKey), successTasks)
}

// ------- cache task_extensions.PassedDataWhenCreate

// GetOrSetPassedDataWhenCreateFromContext
// get value from caches map
// if not exist search value from db and save to caches map
func (p *provider) GetOrSetPassedDataWhenCreateFromContext(pipelineYml *pipelineyml.PipelineYml, pipeline *spec.Pipeline) (passedDataWhenCreate *action_info.PassedDataWhenCreate, err error) {
	pipelineID := pipeline.ID
	passedDataWhenCreate = p.getPassedDataWhenCreateFromContextByPipelineID(pipelineID)
	if passedDataWhenCreate != nil {
		return passedDataWhenCreate, nil
	}

	passedDataWhenCreate = &action_info.PassedDataWhenCreate{}
	passedDataWhenCreate.InitData(p.bdl, p.ActionMgr)
	if err := passedDataWhenCreate.PutPassedDataByPipelineYml(pipelineYml, pipeline); err != nil {
		return nil, err
	}

	p.setPassedDataWhenCreateToContextByPipelineID(passedDataWhenCreate, pipelineID)
	return passedDataWhenCreate, nil
}
func (p *provider) getPassedDataWhenCreateFromContextByPipelineID(pipelineID uint64) *action_info.PassedDataWhenCreate {

	value := p.getInterfaceValueByKey(pipelineID, pipelinePassedDataWhenCreateContextCachesPrefixKey)
	if value == nil {
		return nil
	}
	passedData, ok := value.(*action_info.PassedDataWhenCreate)
	if !ok {
		return nil
	}

	return passedData
}

func (p *provider) setPassedDataWhenCreateToContextByPipelineID(passedDataWhenCreate *action_info.PassedDataWhenCreate, pipelineID uint64) {
	p.cacheMap.Store(makeMapKey(pipelineID, pipelinePassedDataWhenCreateContextCachesPrefixKey), passedDataWhenCreate)
}

func (p *provider) SetPipelineSecretByPipelineID(pipelineID uint64, secret *SecretCache) {
	p.cacheMap.Store(makeMapKey(pipelineID, pipelineSecretCacheKey), secret)
}

func (p *provider) GetPipelineSecretByPipelineID(pipelineID uint64) (secret *SecretCache) {
	value := p.getInterfaceValueByKey(pipelineID, pipelineSecretCacheKey)
	if value == nil {
		return nil
	}
	secret, ok := value.(*SecretCache)
	if !ok {
		return nil
	}
	return secret
}

func (p *provider) ClearPipelineSecretByPipelineID(pipelineID uint64) {
	p.cacheMap.Delete(makeMapKey(pipelineID, pipelineSecretCacheKey))
}
