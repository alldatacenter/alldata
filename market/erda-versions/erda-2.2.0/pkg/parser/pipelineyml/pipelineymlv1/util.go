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

package pipelineymlv1

import (
	"fmt"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/mitchellh/mapstructure"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/metadata"
)

func (y *PipelineYml) FindDockerImageByResourceName(name string) (repository, tag string, err error) {
	resMap := y.validResourceMap()
	res, ok := resMap[name]
	if !ok {
		err = errors.Errorf("cannot find resource_type of resource: %s", name)
		return
	}

	resTypeMap := y.validResTypesMap()
	resType, ok := resTypeMap[res.Type]
	if !ok {
		err = errors.Errorf("cannot find docker image of resource_type: %s", name)
		return
	}
	type imageSource struct {
		Repository string
		Tag        string
	}
	var source imageSource
	if err = mapstructure.Decode(&resType.Source, &source); err != nil {
		return
	}
	repository = source.Repository
	tag = source.Tag
	if tag == "" {
		tag = y.option.builtinResourceTypeDockerImageTag
	}
	return
}

func (y *PipelineYml) FindResourceByName(name string) (Resource, bool) {
	resMap := y.validResourceMap()
	res, ok := resMap[name]
	if !ok {
		return Resource{}, false
	}
	return res, true
}

func ResourceDir(suffix string) string {
	return filepath.Join("/tmp", "build", suffix)
}

func GenerateFlowUUID(yUUID string) string {
	return fmt.Sprintf("ccflow-%s", yUUID)
}

func GenerateStageUUID(stageName string, index int, yUUID string) string {
	return strings.ToLower(fmt.Sprintf("ccstage-%d-%s-%s", index, stageName, yUUID))
}

// GenerateTaskUUID have some constrains
// 如果存在多个 job 执行器，需要同时满足约束
func GenerateTaskUUID(stageIndex int, stageName string, taskIndex int, taskName string, yUUID string) string {
	taskUUID := strings.Replace(strings.ToLower(fmt.Sprintf("ccTask-%d-%s-%d-%s-%s", stageIndex, stageName, taskIndex, taskName, yUUID)), "_", "-", -1)
	// satisfy constraints
	/*
	  metronome:
	    Unique identifier for the job consisting of a series of names separated by dots.
	    Each name must be at least 1 character and may only contain digits (`0-9`), dashes (`-`), and lowercase letters (`a-z`).
	    The name may not begin or end with a dash."
	*/
	doMetronomeConstraints := func(taskUUID string) string {
		metronomeRegexp := regexp.MustCompile(`[^a-zA-Z0-9-]+`)
		taskUUID = metronomeRegexp.ReplaceAllString(taskUUID, "")
		taskUUID = strings.TrimLeft(strings.TrimRight(taskUUID, "-"), "-")
		return taskUUID
	}
	/*
	  demo
	*/
	doDemoConstraints := func(taskUUID string) string {
		return taskUUID
	}
	return doDemoConstraints(doMetronomeConstraints(taskUUID))
}

// UpdatePipelineOnGlobal update pipeline object and byteData together.
func (y *PipelineYml) UpdatePipelineOnGlobal(kvs map[string]string) error {

	// update global envs, just replace
	y.obj.Envs = kvs

	newYmlByte, err := y.YAML()
	if err != nil {
		return err
	}
	y.byteData = []byte(newYmlByte)
	if err = y.Parse(); err != nil {
		return err
	}

	return nil
}

func (y *PipelineYml) GetGlobalEnvs() map[string]string {
	return y.obj.Envs
}

func (y *PipelineYml) GetTaskEnvs(taskUUID string) (map[string]string, error) {

	for si, stage := range y.obj.Stages {
		for ti, task := range stage.Tasks {
			tmpUUID := GenerateTaskUUID(si, stage.Name, ti, task.Name(), y.metadata.instanceID)
			if tmpUUID == taskUUID {
				return task.GetEnvs(), nil
			}
		}
	}

	return nil, errors.Errorf("failed to get task envs, no matched task found, task uuid: %s", taskUUID)
}

// Update workspace into bp_args
func (y *PipelineYml) UpdatePipelineWorkspaceOnBpArgs(env string) error {
	var checkType = func(t string) bool {
		var rTypes = []string{string(RES_TYPE_BUILDPACK), string(RES_TYPE_BP_COMPILE)}
		for _, rt := range rTypes {
			if t == rt {
				return true
			}
		}
		return false
	}

	for i := range y.obj.Resources {
		if checkType(y.obj.Resources[i].Type) {
			if y.obj.Resources[i].Source["bp_args"] != nil {
				if val, ok := y.obj.Resources[i].Source["bp_args"].(map[string]interface{}); ok {
					val["DICE_WORKSPACE"] = strings.ToLower(env)
				} else {
					return errors.Errorf("invalid type of bp_args in Source of Resource %s", y.obj.Resources[i].Name)
				}
			} else {
				y.obj.Resources[i].Source["bp_args"] = map[string]interface{}{"DICE_WORKSPACE": strings.ToLower(env)}
			}
		}
	}

	newYmlByte, err := y.YAML()
	if err != nil {
		return err
	}
	y.byteData = []byte(newYmlByte)
	if err = y.Parse(); err != nil {
		return err
	}

	return nil
}

// Update bp_args, docker_args.
func (y *PipelineYml) UpdatePipelineOnBpArgs(resource string, bp_args, bp_repo_args map[string]string) error {
	for i := range y.obj.Resources {
		if y.obj.Resources[i].Name == resource {
			logrus.Infof("bp-args settings: %v", bp_args)
			y.obj.Resources[i].Source["bp_args"] = bp_args
			y.obj.Resources[i].Source["bp_repo"] = bp_repo_args["bp_repo"]
			y.obj.Resources[i].Source["bp_ver"] = bp_repo_args["bp_ver"]
		}
	}

	newYmlByte, err := y.YAML()
	if err != nil {
		return err
	}
	y.byteData = []byte(newYmlByte)
	if err = y.Parse(); err != nil {
		return err
	}

	return nil
}

func (y *PipelineYml) GetBpArgs(sourceId int) map[string]string {
	if _, ok := y.obj.Resources[sourceId].Source["bp_args"]; !ok {
		return nil
	}
	if val, ok := y.obj.Resources[sourceId].Source["bp_args"].(map[string]interface{}); ok {
		result := make(map[string]string)
		for k, v := range val {
			result[k] = fmt.Sprint(v)
		}
		return result
	}
	return nil
}

func (y *PipelineYml) GetBpRepoArgs(sourceId int) map[string]string {
	repo := make(map[string]string)
	if _, ok := y.obj.Resources[sourceId].Source["bp_repo"]; !ok {
		repo["bp_repo"] = ""
	} else {
		repo["bp_repo"] = y.obj.Resources[sourceId].Source["bp_repo"].(string)
	}

	if _, ok := y.obj.Resources[sourceId].Source["bp_ver"]; !ok {
		repo["bp_ver"] = ""
	} else {
		repo["bp_ver"] = y.obj.Resources[sourceId].Source["bp_ver"].(string)
	}

	if repo["bp_repo"] == "" && repo["bp_ver"] == "" {
		return nil
	}

	return repo
}

func (y *PipelineYml) GetResourceID(taskName string) int {
	for id := range y.obj.Resources {
		if y.obj.Resources[id].Name == taskName {
			resourceType := y.obj.Resources[id].Type
			if strings.HasPrefix(resourceType, string(RES_TYPE_BUILDPACK)) ||
				strings.HasPrefix(resourceType, string(RES_TYPE_BP_COMPILE)) ||
				strings.HasPrefix(resourceType, string(RES_TYPE_BP_IMAGE)) {
				return id
			}
		}
	}
	return -1
}

func (y *PipelineYml) GetTaskDisableStatus(taskUUID string) (bool, error) {
	for si, stage := range y.obj.Stages {
		for ti, task := range stage.Tasks {
			tmpUUID := GenerateTaskUUID(si, stage.Name, ti, task.Name(), y.metadata.instanceID)
			if tmpUUID == taskUUID {
				return task.IsDisable(), nil
			}
		}
	}

	return false, errors.Errorf("failed to get task disable status, no matched task found, task uuid: %s", taskUUID)
}

func (y *PipelineYml) GetTaskPauseStatus(taskUUID string) (bool, error) {
	for si, stage := range y.obj.Stages {
		for ti, task := range stage.Tasks {
			tmpUUID := GenerateTaskUUID(si, stage.Name, ti, task.Name(), y.metadata.instanceID)
			if tmpUUID == taskUUID {
				return task.IsPause(), nil
			}
		}
	}

	return false, errors.Errorf("failed to get task pause status, no matched task found, task uuid: %s", taskUUID)
}

func (y *PipelineYml) GetTaskForceBuildpackStatus(taskUUID string) bool {
	for si, stage := range y.obj.Stages {
		for ti, task := range stage.Tasks {
			tmpUUID := GenerateTaskUUID(si, stage.Name, ti, task.Name(), y.metadata.instanceID)
			if tmpUUID == taskUUID {
				if task.GetTaskParams() != nil {
					if v, ok := task.GetTaskParams()["force_buildpack"]; ok {
						if forceBuildpack, ok := v.(bool); ok {
							return forceBuildpack
						}
					}
					return false
				}
			}
		}
	}

	return false
}

// ApplyKVsWithPriority generates a result map with the rule that
// the latter will overwrite the former.
// For example:
//   priority: e1 > e2 > e3
// 	 you should pass param `envs` as: e3, e2, e1
func ApplyKVsWithPriority(kvs ...map[string]string) map[string]string {
	result := make(map[string]string)
	for _, env := range kvs {
		for k, v := range env {
			result[k] = v
		}
	}
	return result
}

func MetadataFields2Map(metas []metadata.MetadataField) map[string]string {
	m := make(map[string]string, len(metas))
	for _, meta := range metas {
		m[meta.Name] = meta.Value
	}
	return m
}

func Map2MetadataFields(m map[string]string) []metadata.MetadataField {
	var metas []metadata.MetadataField
	for k, v := range m {
		metas = append(metas, metadata.MetadataField{Name: k, Value: v})
	}
	return metas
}
