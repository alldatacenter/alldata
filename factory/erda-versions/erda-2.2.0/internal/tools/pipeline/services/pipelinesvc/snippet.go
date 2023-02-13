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

package pipelinesvc

import (
	"encoding/json"
	"fmt"
	"sync"

	"github.com/appscode/go/strings"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/pipeline_snippet_client"
	"github.com/erda-project/erda/pkg/strutil"
)

// handleQueryPipelineYamlBySnippetConfigs 统一查询 snippetConfigs
func (s *PipelineSvc) QueryDetails(req *apistructs.SnippetQueryDetailsRequest) (map[string]apistructs.SnippetQueryDetail, error) {

	configs := req.SnippetConfigs
	if configs == nil || len(configs) <= 0 {
		return nil, fmt.Errorf("snippetConfigs is empty")
	}

	for _, config := range configs {
		if config.Alias == "" {
			return nil, fmt.Errorf("config lost param alias")
		}
		if config.Name == "" {
			return nil, fmt.Errorf("%s config lost param name", config.Alias)
		}
		if config.Source == "" {
			return nil, fmt.Errorf("%s config lost param source", config.Alias)
		}
	}

	var snippetConfigs []apistructs.SnippetConfig
	for _, config := range configs {
		if config.Source == apistructs.ActionSourceType {
			continue
		}
		snippetConfigs = append(snippetConfigs, config.SnippetConfig)
	}

	result, err := s.HandleQueryPipelineYamlBySnippetConfigs(snippetConfigs)
	if err != nil {
		return nil, err
	}

	snippetDetailMap := make(map[string]apistructs.SnippetQueryDetail)
	for _, config := range configs {
		// 假如是 action 类型就跳过
		if config.Source == apistructs.ActionSourceType {
			continue
		}

		pipelineYmlContext, find := result[config.SnippetConfig.ToString()]
		if !find {
			return nil, fmt.Errorf("not find snippet: %v", config)
		}

		graph, err := pipelineyml.ConvertToGraphPipelineYml([]byte(pipelineYmlContext))
		if err != nil {
			return nil, err
		}

		var detail apistructs.SnippetQueryDetail
		// outputs
		for _, output := range graph.Outputs {
			detail.Outputs = append(detail.Outputs, expression.GenOutputRef(config.Alias, output.Name))
		}

		// params
		for _, param := range graph.Params {
			detail.Params = append(detail.Params, param)
		}

		snippetDetailMap[config.Alias] = detail
	}

	// action source 运行
	for _, config := range configs {
		// 假如不是 action 类型就跳过
		if config.Source != apistructs.ActionSourceType {
			continue
		}

		detail, err := s.getActionDetail(config)
		if err != nil {
			return nil, err
		}

		if detail != nil {
			snippetDetailMap[config.Alias] = *detail
		}
	}

	return snippetDetailMap, nil
}

func (s *PipelineSvc) getActionDetail(config apistructs.SnippetDetailQuery) (*apistructs.SnippetQueryDetail, error) {

	var detail = &apistructs.SnippetQueryDetail{}

	var actionVersion = s.actionMgr.MakeActionTypeVersion(&pipelineyml.Action{
		Type:    pipelineyml.ActionType(config.Name),
		Version: config.Labels[apistructs.LabelActionVersion],
	})
	extensionSearchRequest := apistructs.ExtensionSearchRequest{
		Extensions: []string{actionVersion},
		YamlFormat: true,
	}

	actions, err := s.bdl.SearchExtensions(extensionSearchRequest)
	if err != nil {
		return nil, err
	}
	version, ok := actions[actionVersion]
	if !ok {
		return nil, nil
	}

	specYml, ok := version.Spec.(string)
	if !ok {
		return nil, fmt.Errorf("action %s spec not string", config.Alias)
	}

	actionSpec := apistructs.ActionSpec{}
	err = yaml.Unmarshal([]byte(specYml), &actionSpec)
	if err != nil {
		return nil, err
	}

	for _, output := range actionSpec.Outputs {
		detail.Outputs = append(detail.Outputs, expression.GenOutputRef(config.Alias, output.Name))
	}

	actionJson := config.Labels[apistructs.LabelActionJson]
	var action apistructs.PipelineYmlAction
	err = json.Unmarshal([]byte(actionJson), &action)
	if err != nil {
		return nil, errors.Wrap(err, fmt.Sprintf("failed to parse actionJson %s", config.Alias))
	}

	for _, matchOutputs := range actionSpec.OutputsFromParams {
		switch matchOutputs.Type {
		case apistructs.JqActionMatchOutputType:
			outputs, err := handlerActionOutputsWithJq(&action, matchOutputs.Expression)
			if err != nil {
				return nil, err
			}
			for _, output := range outputs {
				detail.Outputs = append(detail.Outputs, expression.GenOutputRef(config.Alias, output))
			}
		}
	}

	return detail, nil
}

// handleQueryPipelineYamlBySnippetConfigs 统一查询 snippetConfigs
func (s *PipelineSvc) HandleQueryPipelineYamlBySnippetConfigs(sourceSnippetConfigs []apistructs.SnippetConfig) (map[string]string, error) {

	sourceSupportAsyncBatch := map[string]struct{}{
		apistructs.PipelineSourceAutoTest.String(): {},
	}

	batchSourceSnippetConfigs := []apistructs.SnippetConfig{}
	singleSourceSnippetConfigs := []apistructs.SnippetConfig{}
	for _, snippetConfig := range sourceSnippetConfigs {
		if _, supportAsync := sourceSupportAsyncBatch[snippetConfig.Source]; supportAsync {
			batchSourceSnippetConfigs = append(batchSourceSnippetConfigs, snippetConfig)
		} else {
			singleSourceSnippetConfigs = append(singleSourceSnippetConfigs, snippetConfig)
		}
	}

	var wait sync.WaitGroup
	var lock sync.Mutex
	var errInfo error
	var yamlResults = make(map[string]string)

	wait.Add(1)
	go func() {
		defer wait.Done()
		batchResults, err := s.batchQueryPipelineYAMLBySnippetConfigs(batchSourceSnippetConfigs)
		if err != nil {
			errInfo = err
			return
		}

		lock.Lock()
		defer lock.Unlock()
		for k, v := range batchResults {
			yamlResults[k] = v
		}
	}()

	wait.Add(1)
	go func() {
		defer wait.Done()
		for _, singleConfig := range singleSourceSnippetConfigs {
			yml, err := s.queryPipelineYAMLBySnippetConfig(&singleConfig)
			if err != nil {
				errInfo = err
				return
			}

			lock.Lock()
			yamlResults[singleConfig.ToString()] = yml
			lock.Unlock()
		}
	}()

	wait.Wait()
	if errInfo != nil {
		return nil, errInfo
	}

	var errMsgs []string
	for _, snippetConfig := range sourceSnippetConfigs {
		yml, ok := yamlResults[snippetConfig.ToString()]
		if !ok {
			errMsgs = append(errMsgs, "source: %s, name: %s", snippetConfig.Source, snippetConfig.Name)
		}
		if strings.IsEmpty(&yml) {
			errMsgs = append(errMsgs, "source: %s, name: %s", snippetConfig.Source, snippetConfig.Name)
		}
	}
	if len(errMsgs) > 0 {
		return nil, fmt.Errorf("not found yaml for snippet configs: %s", strutil.Join(errMsgs, ", ", true))
	}

	return yamlResults, nil
}

// queryPipelineYAMLBySnippetConfig 根据 snippetConfig 查询对应的 pipeline yaml
func (s *PipelineSvc) queryPipelineYAMLBySnippetConfig(cfg *apistructs.SnippetConfig) (string, error) {
	return pipeline_snippet_client.GetSnippetPipelineYml(apistructs.SnippetConfig{
		Name:   cfg.Name,
		Source: cfg.Source,
		Labels: cfg.Labels,
	})
}

// batchQueryPipelineYAMLBySnippetConfigs 根据 source snippetConfig 批量查询对应的 pipeline yaml
func (s *PipelineSvc) batchQueryPipelineYAMLBySnippetConfigs(snippetConfigs []apistructs.SnippetConfig) (map[string]string, error) {
	yamlResults, err := pipeline_snippet_client.BatchGetSnippetPipelineYml(snippetConfigs)
	if err != nil {
		return nil, err
	}
	result := make(map[string]string)
	for _, yamlResult := range yamlResults {
		result[yamlResult.Config.ToString()] = yamlResult.Yml
	}
	return result, nil
}

// createSnippetPipeline4Create 为 snippetTask 创建流水线对象
func (s *PipelineSvc) MakeSnippetPipeline4Create(p *spec.Pipeline, snippetTask *spec.PipelineTask, yamlContent string) (*spec.Pipeline, error) {
	snippetConfig := snippetTask.Extra.Action.SnippetConfig
	// runParams
	var runParams []apistructs.PipelineRunParam
	for k, v := range snippetTask.Extra.Action.Params {
		runParams = append(runParams, apistructs.PipelineRunParam{Name: k, Value: v})
	}
	// avoid concurrent problem
	labels := make(map[string]string, len(p.Labels))
	for k, v := range p.Labels {
		labels[k] = v
	}
	// transfer snippetTask to pipeline create request
	for k, v := range snippetConfig.Labels {
		labels[k] = v
	}
	snippetPipelineCreateReq := apistructs.PipelineCreateRequestV2{
		PipelineYml:            yamlContent,
		ClusterName:            snippetTask.Extra.ClusterName,
		PipelineYmlName:        snippetConfig.Name,
		PipelineSource:         apistructs.PipelineSource(snippetConfig.Source),
		Labels:                 labels,
		NormalLabels:           p.NormalLabels,
		Envs:                   p.Snapshot.Envs,
		ConfigManageNamespaces: p.Extra.ConfigManageNamespaces,
		AutoRunAtOnce:          false,
		RunParams:              runParams,
		IdentityInfo:           p.GenIdentityInfo(),
	}
	if err := s.validateCreateRequest(&snippetPipelineCreateReq); err != nil {
		return nil, apierrors.ErrCreateSnippetPipeline.InternalError(err)
	}
	snippetP, err := s.makePipelineFromRequestV2(&snippetPipelineCreateReq)
	if err != nil {
		return nil, err
	}
	snippetP.IsSnippet = true
	snippetP.ParentPipelineID = &p.ID
	snippetP.ParentTaskID = &snippetTask.ID
	snippetP.Extra.SnippetChain = append(p.Extra.SnippetChain, p.ID)
	return snippetP, nil
}
