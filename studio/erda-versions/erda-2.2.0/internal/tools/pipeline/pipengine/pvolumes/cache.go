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

package pvolumes

import (
	"crypto/sha256"
	"encoding/hex"
	"path/filepath"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const (
	TaskCacheHashName          = "action_cache_hash_name"
	TaskCachePath              = "action_cache_path"
	TaskCacheMame              = "action_cache"
	TaskCacheBasePath          = "/actions/caches"
	TaskCacheCompressionSuffix = ".tar"
	TaskCachePathBasePath      = "{{basePath}}"
	TaskCachePathEndPath       = "{{endPath}}"
)

func HandleTaskCacheVolumes(p *spec.Pipeline, task *spec.PipelineTask, diceYmlJob *diceyml.Job, mountPoint string) {
	caches := task.Extra.Action.Caches
	if len(caches) == 0 {
		return
	}

	// 从 labels 中获取 projectID 和 appID
	projectID := p.GetLabel(apistructs.LabelProjectID)
	appID := p.GetLabel(apistructs.LabelAppID)

	var volumes []metadata.MetadataField
	var binds diceyml.Binds
	for _, cache := range caches {
		// 根据指定的绝对目录名生成一个 hash 的文件名
		hasher := sha256.New()
		hasher.Write([]byte(cache.Path))
		hash := hex.EncodeToString(hasher.Sum(nil))

		// key 为空就根据 hash 值和一些前缀生成一个固定的挂载目录
		key := cache.Key
		if key == "" {
			key = filepath.Join(mountPoint, TaskCacheBasePath, projectID, appID, hash)
		} else {
			// key 不为空就需要根据占位符替换成固定的挂载目录，其中只有非占位符之间是用户可自定义的一部分
			key = strings.ReplaceAll(key, " ", "")
			// 用户自定义，且 projectID 为空，appID 为空, 可以不用 projectID 和 appID 为前缀
			if projectID == "" && appID == "" {
				key = strings.ReplaceAll(key, TaskCachePathBasePath, filepath.Join(mountPoint+TaskCacheBasePath))
				key = strings.ReplaceAll(key, TaskCachePathEndPath, hash)
			} else {
				key = strings.ReplaceAll(key, TaskCachePathBasePath, filepath.Join(mountPoint, TaskCacheBasePath, projectID, appID))
				key = strings.ReplaceAll(key, TaskCachePathEndPath, hash)
			}
		}

		labels := make(map[string]string)
		labels[VoLabelKeyContainerPath] = key
		labels[VoLabelKeyContextPath] = key
		labels[TaskCacheHashName] = hash
		labels[TaskCachePath] = cache.Path
		var storage = metadata.MetadataField{
			Name:   TaskCacheMame + "_" + hash,
			Type:   string(spec.StoreTypeDiceCacheNFS),
			Value:  key,
			Labels: labels,
		}
		volumes = append(volumes, storage)
		binds = append(binds, key+":"+key)
	}

	// add volumes
	task.Context.InStorages = append(task.Context.InStorages, volumes...)
	task.Context.OutStorages = append(task.Context.OutStorages, volumes...)
	// add binds
	diceYmlJob.Binds = append(diceYmlJob.Binds, binds...)
}
