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
)

const DockerImageResType = "docker-image"

type builtinResType string

// builtinResType declaration
// 如有更新，请进入 gogenerate 目录重新运行 go generate 并提交
const (
	RES_TYPE_GIT            builtinResType = "git"
	RES_TYPE_BUILDPACK      builtinResType = "buildpack"
	RES_TYPE_BP_COMPILE     builtinResType = "bp-compile"
	RES_TYPE_BP_IMAGE       builtinResType = "bp-image"
	RES_TYPE_DICE           builtinResType = "dice"
	RES_TYPE_ABILITY        builtinResType = "ability"
	RES_TYPE_ADDON_REGISTRY builtinResType = "addon-registry"
	RES_TYPE_DICEHUB        builtinResType = "dicehub"
	RES_TYPE_IT             builtinResType = "it"
	RES_TYPE_SONAR          builtinResType = "sonar"
	RES_TYPE_FLINK          builtinResType = "flink"
	RES_TYPE_SPARK          builtinResType = "spark"
	RES_TYPE_UT             builtinResType = "ut"
)

func (y *PipelineYml) builtinResTypes() map[string]ResourceType {

	return addBuiltInResTypes(
		y.option.builtinResourceTypeDockerImagePrefix,
		y.option.builtinResourceTypeDockerImageTag,
		BuiltinResTypeNames...,
	)
}

var addBuiltInResTypes = func(imagePrefix, imageTag string, resNames ...string) map[string]ResourceType {
	resTypes := make(map[string]ResourceType)
	for _, resName := range resNames {
		resTypes[resName] = ResourceType{
			Name: resName,
			Type: DockerImageResType,
			Source: Source{
				"repository": fmt.Sprintf("%s/%s-action", imagePrefix, resName),
				"tag":        imageTag,
			},
		}
	}
	return resTypes
}
