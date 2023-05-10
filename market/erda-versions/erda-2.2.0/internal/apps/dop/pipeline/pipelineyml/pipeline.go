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
	"fmt"
	"os"
	"strings"

	"github.com/ghodss/yaml"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

type PipelineYml struct {
	// byteData represents byte format pipeline.yml
	byteData []byte
	// obj represents the struct parsed according to byteData field
	obj      *Pipeline
	metadata PipelineMetadata
}

// PipelineMetadata contains extra info needs by parse process.
type PipelineMetadata struct {
	uuid       string
	contextDir string

	contextMap map[string][]string

	clusterName string

	gitBranch string

	// used for render pipeline.yml placeholder
	publicTemplateVars map[string]string
	secretTemplateVars map[string]string

	publicPlatformEnvs map[string]string
	secretPlatformEnvs map[string]string

	alreadyTransformed bool
}

func New(b []byte) *PipelineYml {
	return &PipelineYml{
		byteData: b,
	}
}

func (y *PipelineYml) CreatePipeline(gittarURL, gittarBranch, commitId string) (*Pipeline, error) {
	//Create new pipeline
	p := &Pipeline{}

	pOld, err := ParsePipeline(gittarURL, gittarBranch)
	if err != nil {
		logrus.Warningf("parse pipeline.yml failed, err:%v.", err)
		return nil, err
	}

	//Get contexts.
	paths := GetLanguagePaths(pOld)
	if len(paths) == 0 {
		logrus.Warningf("get context failed")
	}

	//Insert version.
	p.Version = "1.0"

	err = composeResource(pOld, p, gittarURL, gittarBranch)
	if err != nil {
		return nil, err
	}

	err = composeStage(pOld, p, commitId)
	if err != nil {
		return nil, err
	}

	// 增加sonar相关的环境变量
	env := make(map[string]string)
	env["SONAR_PASSWORD"] = os.Getenv("SONAR_PASSWORD")
	env["SONAR_ADDR"] = os.Getenv("SONAR_ADDR")
	env["SONAR_PUBLIC_URL"] = os.Getenv("SONAR_PUBLIC_URL")
	env["SONAR_TOKEN"] = os.Getenv("SONAR_TOKEN")
	p.Envs = env

	return p, nil
}

// Unmarshal unmarshal byteData to obj with evaluate, need metadata templateVars.
func (y *PipelineYml) Unmarshal() error {

	err := yaml.Unmarshal(y.byteData, &y.obj)
	if err != nil {
		return err
	}

	if y.obj == nil {
		return errors.New("PipelineYml.obj is nil pointer")
	}

	//err = y.Evaluate(applyEnvsWithPriority(y.metadata.publicTemplateVars, y.metadata.secretTemplateVars))
	//if err != nil {
	//	return err
	//}

	// re unmarshal to obj, because byteData updated by evaluate
	err = yaml.Unmarshal(y.byteData, &y.obj)
	return err
}

func (y *PipelineYml) YAML() (string, error) {
	b, err := yaml.Marshal(y.obj)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func (y *PipelineYml) Evaluate(variables map[string]string) error {
	ymlStr := string(y.byteData)
	lines := strings.Split(ymlStr, "\n")
	for i := range lines {
		// remove comment
		lines[i] = removeComment(lines[i])

		// 遍历替换一行中的所有 ((placeholder))
		for {
			placeholder, find := findFirstPlaceholder(lines[i])
			if !find {
				break
			}
			value, ok := variables[placeholder]
			if !ok {
				return errors.Errorf("failed to render placeholder: %s at line: %d", placeholder, i+1)
			}
			lines[i] = strings.Replace(lines[i], placeholder, value, 1)
		}
	}
	y.byteData = []byte(strings.Join(lines, "\n"))
	return nil
}

func findFirstPlaceholder(line string) (string, bool) {
	// li: left index
	// ri: right index
	for li := 0; li < len(line); li++ {
		var left, right int
		// find first ((
		var findRight bool
		if line[li] == '(' && li+1 < len(line) && line[li+1] == '(' {
			left = li
			// find matched ))
			for ri := li; ri < len(line); ri++ {
				if line[ri] == ')' && ri+1 < len(line) && line[ri+1] == ')' {
					right = ri + 1
					findRight = true
					break
				}
			}
			if findRight {
				return fmt.Sprintf("%s", line[left:right+1]), true
			}
		}
	}
	return "", false
}

func removeComment(line string) string {
	i := strings.IndexByte(line, '#')
	if i == -1 {
		return line
	}
	return line[:i]
}

func composeResource(pOld, p *Pipeline, gittarUri, gittarBranch string) error {
	resources := GetReSources(pOld)
	if _, ok := resources[RES_TYPE_GIT]; !ok {
		return errors.New("Git resource is not exist.")
	}
	//Modify gittar uri, branch, username, password.
	gitResource := resources[RES_TYPE_GIT]
	gitResource.Source["branch"] = gittarBranch
	gitResource.Source["uri"] = "((gittar.repo))"
	gitResource.Source["username"] = "((gittar.username))"
	gitResource.Source["password"] = "((gittar.password))"

	p.Resources = append(p.Resources, gitResource)

	// make sonar resource
	if _, ok := resources[RES_TYPE_SONAR]; ok {
		p.Resources = append(p.Resources, resources[RES_TYPE_SONAR])
	} else {
		var context string
		paths := GetLanguagePaths(pOld)
		if len(paths) > 0 {
			for _, path := range paths {
				if context != "" {
					context = fmt.Sprint(context, ",", path)
				} else {
					context = path
				}
			}
		}
		sonarRes := Resource{}
		sonarRes.Name = "代码质量分析"
		sonarRes.Type = RES_TYPE_SONAR
		source := make(Source)
		source["context"] = context
		sonarRes.Source = source

		p.Resources = append(p.Resources, sonarRes)
	}

	// make ut resource
	if _, ok := resources[RES_TYPE_UT]; ok {
		p.Resources = append(p.Resources, resources[RES_TYPE_UT])
	} else {
		var context string
		paths := GetLanguagePaths(pOld)
		if len(paths) > 0 {
			for _, path := range paths {
				if context != "" {
					context = fmt.Sprint(context, ",", path)
				} else {
					context = path
				}
			}
		}
		sonarRes := Resource{}
		sonarRes.Name = "单元测试"
		sonarRes.Type = RES_TYPE_UT
		source := make(Source)
		source["context"] = context

		sonarRes.Source = source

		p.Resources = append(p.Resources, sonarRes)
	}

	return nil
}

func composeStage(pOld, p *Pipeline, commitId string) error {
	stages := GetStages(pOld)
	if _, ok := stages[RES_TYPE_GIT]; !ok {
		return errors.New("Git stage is not exist.")
	}
	p.Stages = append(p.Stages, stages[RES_TYPE_GIT])

	// make sonar task
	if _, ok := stages[RES_TYPE_SONAR]; ok {
		p.Stages = append(p.Stages, stages[RES_TYPE_SONAR])
	} else {
		stage := Stage{}
		stage.Name = RES_TYPE_SONAR
		task := []TaskConfig{}

		params := make(map[string]interface{})
		params["project_version"] = commitId
		config := make(TaskConfig)
		config["put"] = "代码质量分析"
		config["params"] = params

		task = append(task, config)
		stage.TaskConfigs = task

		p.Stages = append(p.Stages, &stage)
	}

	// make ut task
	if _, ok := stages[RES_TYPE_UT]; ok {
		p.Stages = append(p.Stages, stages[RES_TYPE_UT])
	} else {
		stage := Stage{}
		stage.Name = RES_TYPE_UT
		task := []TaskConfig{}

		config := make(TaskConfig)
		config["put"] = "单元测试"

		task = append(task, config)
		stage.TaskConfigs = task

		p.Stages = append(p.Stages, &stage)
	}

	return nil
}
