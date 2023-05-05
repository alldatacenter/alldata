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
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func (s *PipelineSvc) PipelineYmlGraph(req *apistructs.PipelineYmlParseGraphRequest) (*apistructs.PipelineYml, error) {
	graph, err := pipelineyml.ConvertToGraphPipelineYml([]byte(req.PipelineYmlContent))
	if err != nil {
		return nil, apierrors.ErrParsePipelineYml.InvalidParameter(err)
	}

	if graph == nil {
		return graph, nil
	}

	// 设置logo和名称
	s.loadGraphActionNameAndLogo(graph)

	return graph, nil
}

func (s *PipelineSvc) loadGraphActionNameAndLogo(graph *apistructs.PipelineYml) {

	var extensionSearchRequest = apistructs.ExtensionSearchRequest{}
	extensionSearchRequest.YamlFormat = true
	for _, stage := range graph.Stages {
		for _, action := range stage {
			if action.Type == apistructs.ActionTypeSnippet {
				continue
			}
			extensionSearchRequest.Extensions = append(extensionSearchRequest.Extensions, action.Type)
		}
	}
	if extensionSearchRequest.Extensions != nil {
		extensionSearchRequest.Extensions = strutil.DedupSlice(extensionSearchRequest.Extensions, true)
	}

	resultMap, err := s.bdl.SearchExtensions(extensionSearchRequest)
	if err != nil {
		logrus.Errorf("pipelineYmlGraph to SearchExtensions error: %v", err)
		return
	}
	for _, stage := range graph.Stages {
		for _, action := range stage {
			if action.Type == pipelineyml.Snippet {
				action.LogoUrl = pipelineyml.SnippetLogo
				action.DisplayName = pipelineyml.SnippetDisplayName
				action.Description = pipelineyml.SnippetDesc
				continue
			}

			version, ok := resultMap[action.Type]
			if !ok {
				continue
			}

			specYmlStr, ok := version.Spec.(string)
			if !ok {
				continue
			}

			var actionSpec apistructs.ActionSpec
			if err := yaml.Unmarshal([]byte(specYmlStr), &actionSpec); err != nil {
				logrus.Errorf("pipelineYmlGraph Unmarshal spec error: %v", err)
				continue
			}

			action.DisplayName = actionSpec.GetLocaleDisplayName(i18n.GetGoroutineBindLang())
			action.LogoUrl = actionSpec.LogoUrl
			action.Description = actionSpec.GetLocaleDesc(i18n.GetGoroutineBindLang())
		}
	}
}
