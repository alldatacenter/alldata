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

package actionagent

import (
	"fmt"
	"io/ioutil"
	"path/filepath"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/actionagent/agenttool"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/pvolumes"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/filehelper"
)

func (agent *Agent) store() {
	for _, out := range agent.Arg.Context.OutStorages {
		switch out.Type {

		case string(spec.StoreTypeNFS):
			tarFile := strings.TrimPrefix(out.Value, spec.StoreTypeNFSProto)
			tarDir := filepath.Join(agent.EasyUse.ContainerContext, out.Name)
			err := agenttool.Tar(tarFile, tarDir)
			if err != nil {
				agent.AppendError(err)
			}
		case string(spec.StoreTypeDiceVolumeLocal), string(spec.StoreTypeDiceVolumeFake):
			// nothing
		case string(spec.StoreTypeOSS):
			agent.AppendError(errors.New("[store] not implemented now for OSS storage"))

		// dice-nfs-volume 类型，store 时将对应 containerContext 下的 task namespace 整个压缩为 volume.path 下的 data (.tar)
		case string(spec.StoreTypeDiceVolumeNFS):
			tarFile := filepath.Join(out.Value, "data")
			tarDir := filepath.Join(agent.EasyUse.ContainerContext, out.Name)
			err := agenttool.Tar(tarFile, tarDir)
			if err != nil {
				agent.AppendError(err)
			}
		case string(spec.StoreTypeDiceCacheNFS):
			tarFile := out.Value + "/" + out.Labels[pvolumes.TaskCacheHashName] + pvolumes.TaskCacheCompressionSuffix
			if filehelper.CheckExist(out.Labels[pvolumes.TaskCachePath], true) != nil {
				logrus.Printf("upload action cache error: %s is not dir", out.Labels[pvolumes.TaskCachePath])
				continue
			}
			if err := agent.storeCache(tarFile, out.Labels[pvolumes.TaskCachePath]); err != nil {
				logrus.Debugf("failed to tar cache path: %s to file: %s, err: %v", out.Labels[pvolumes.TaskCachePath], tarFile, err)
				continue
			}
			logrus.Printf("upload action cache %s success", out.Labels[pvolumes.TaskCachePath])
		default:
			agent.AppendError(errors.Errorf("[store] unsupported store type: %s", out.Type))
		}
	}
}

func (agent *Agent) storeCache(tarFile, cachePath string) (err error) {
	if cachePathSize, isExceed := agent.isCachePathExceedLimit(cachePath); isExceed {
		return fmt.Errorf("tar path: %s size: %d bytes exceed limit size: %d bytes", cachePath, cachePathSize.Bytes(),
			agent.MaxCacheFileSizeMB.Bytes())
	}
	tmpFile, err := ioutil.TempFile(cacheTempDir, cacheTempPrefix)
	if err != nil {
		return err
	}
	if err = agenttool.Tar(tmpFile.Name(), cachePath); err != nil {
		return err
	}
	if err = agenttool.Mv(tmpFile.Name(), tarFile); err != nil {
		return err
	}
	return nil
}
