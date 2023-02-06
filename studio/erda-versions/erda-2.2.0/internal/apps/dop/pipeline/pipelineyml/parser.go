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

package pipelineyml

import (
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

const (
	GittarFilePath = apistructs.DefaultPipelineYmlName
	GittarUserName = ""
	GittarPassWord = ""
)

func ParsePipeline(gittarURL, ref string) (*Pipeline, error) {
	bdl := bundle.New(bundle.WithGittar())
	contents, err := bdl.GetGittarFile(gittarURL, ref, GittarFilePath, GittarUserName, GittarPassWord, "")

	y := New([]byte(contents))

	err = y.Unmarshal()
	if err != nil {
		return nil, err
	}

	return y.obj, nil
}

func GetReSources(p *Pipeline) map[string]Resource {
	resources := make(map[string]Resource)

	for _, res := range p.Resources {
		if checkPrefix(res.Type, RES_TYPE_GIT) {
			resources[RES_TYPE_GIT] = res
		}
		if checkPrefix(res.Type, RES_TYPE_SONAR) {
			resources[RES_TYPE_SONAR] = res
		}
		if checkPrefix(res.Type, RES_TYPE_UT) {
			resources[RES_TYPE_UT] = res
		}
	}

	return resources
}

func GetStages(p *Pipeline) map[string]*Stage {
	stages := make(map[string]*Stage)
	for _, res := range p.Resources {
		if checkPrefix(res.Type, RES_TYPE_GIT) {
			resName := res.Name
			for _, stage := range p.Stages {
				for _, task := range stage.TaskConfigs {
					if _, ok := task["get"]; !ok {
						continue
					}
					name := task["get"]
					if resName == name.(string) {
						stages[RES_TYPE_GIT] = stage
					}
				}
			}
		}
		if checkPrefix(res.Type, RES_TYPE_SONAR) {
			resName := res.Name
			for _, stage := range p.Stages {
				for _, task := range stage.TaskConfigs {
					if _, ok := task["put"]; !ok {
						continue
					}
					name := task["put"]
					if resName == name.(string) {
						stages[RES_TYPE_SONAR] = stage
					}
				}
			}
		}
		if checkPrefix(res.Type, RES_TYPE_UT) {
			resName := res.Name
			for _, stage := range p.Stages {
				for _, task := range stage.TaskConfigs {
					if _, ok := task["put"]; !ok {
						continue
					}
					name := task["put"]
					if resName == name.(string) {
						stages[RES_TYPE_UT] = stage
					}
				}
			}
		}

	}

	return stages
}

func GetLanguagePaths(p *Pipeline) []string {
	var paths []string

	for _, res := range p.Resources {
		path := getPath(RES_TYPE_SONAR, res)
		if path != "" {
			paths = append(paths, path)
		}
	}

	if len(paths) == 0 {
		for _, res := range p.Resources {
			path := getPath(RES_TYPE_BUILDPACK, res)
			if path != "" {
				if !pathAlreadyExist(paths, path) {
					paths = append(paths, path)
				}
			} else {
				path = getPath(RES_TYPE_BP_IMAGE, res)
				if path != "" {
					if !pathAlreadyExist(paths, path) {
						paths = append(paths, path)
					}
				}
			}
		}
	}

	return paths
}

func pathAlreadyExist(pathList []string, path string) bool {
	for _, p := range pathList {
		if p == path {
			return true
		}
	}
	return false
}

func getPath(resType string, res Resource) string {
	if checkPrefix(res.Type, resType) {
		if _, ok := res.Source["context"]; !ok {
			return ""
		}

		path := res.Source["context"]
		return path.(string)
	}

	return ""
}

func checkPrefix(str, prefix string) bool {
	return strings.HasPrefix(str, prefix)
}
