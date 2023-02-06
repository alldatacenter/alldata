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
	"net/url"
	"path/filepath"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

// ParseDiceYmlJobBinds 将 diceYmlJob 里老格式的 binds 转换为新的格式
func ParseDiceYmlJobBinds(diceYmlJob *diceyml.Job) ([]apistructs.Bind, error) {
	binds, err := diceyml.ParseBinds(diceYmlJob.Binds)
	if err != nil {
		return nil, err
	}
	var result []apistructs.Bind
	for _, bind := range binds {
		result = append(result, apistructs.Bind{
			ContainerPath: bind.ContainerPath,
			HostPath:      bind.HostPath,
			ReadOnly:      bind.Type == "r",
		})
	}
	return result, nil
}

// GenerateTaskCommonBinds 生成 task 通用 binds
func GenerateTaskCommonBinds(mountPoint string) []apistructs.Bind {

	const (
		dockerSock = "/var/run/docker.sock"
	)

	var binds []apistructs.Bind
	dockerSockBind := apistructs.Bind{
		HostPath:      dockerSock,
		ContainerPath: dockerSock,
		ReadOnly:      true,
	}
	binds = append(binds, dockerSockBind)

	storageURL := conf.StorageURL()
	URL, _ := url.Parse(storageURL)
	if URL.Scheme == "file" {
		var storageBind apistructs.Bind
		_path := filepath.Join(mountPoint, URL.Path)
		storageBind = apistructs.Bind{
			HostPath:      _path,
			ContainerPath: _path,
			ReadOnly:      false,
		}
		binds = append(binds, storageBind)
	}
	return binds
}
