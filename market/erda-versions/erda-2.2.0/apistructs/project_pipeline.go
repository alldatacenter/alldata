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

package apistructs

import "path/filepath"

type PipelineCategory string

type ProjectPipelineType string

const (
	CategoryBuildDeploy          PipelineCategory = "build-deploy"
	CategoryBuildArtifact        PipelineCategory = "build-artifact"
	CategoryBuildCombineArtifact PipelineCategory = "build-combine-artifact"
	CategoryBuildIntegration     PipelineCategory = "build-integration"
	CategoryOthers               PipelineCategory = "others"
)

const (
	PipelineTypeDefault ProjectPipelineType = "default"
	PipelineTypeCICD    ProjectPipelineType = "cicd"
	PipelineTypeFDP     ProjectPipelineType = "fdp"
)

const (
	SourceTypeErda = "erda"
)

func (p ProjectPipelineType) String() string {
	return string(p)
}

func MakeLocation(app *ApplicationDTO, t ProjectPipelineType) string {
	return filepath.Join(t.String(), app.OrgName, app.ProjectName)
}

func (c PipelineCategory) String() string {
	return string(c)
}

var CategoryKeyRuleMap = map[PipelineCategory][]string{
	CategoryBuildDeploy:          {"pipeline.yml"},
	CategoryBuildArtifact:        {".erda/pipelines/ci-artifact.yml"},
	CategoryBuildCombineArtifact: {".erda/pipelines/combine-artifact.yml"},
	CategoryBuildIntegration:     {".erda/pipelines/integration.yml"},
}

var CategoryKeyI18NameMap = map[PipelineCategory]string{
	CategoryBuildDeploy:          "BuildDeploy",
	CategoryBuildArtifact:        "BuildArtifact",
	CategoryBuildCombineArtifact: "BuildCombineArtifact",
	CategoryBuildIntegration:     "BuildIntegration",
	CategoryOthers:               "Uncategorized",
}

func GetRuleCategoryKeyMap() map[string]PipelineCategory {
	m := make(map[string]PipelineCategory, 0)
	for k, rules := range CategoryKeyRuleMap {
		for _, v := range rules {
			m[v] = k
		}
	}
	return m
}
