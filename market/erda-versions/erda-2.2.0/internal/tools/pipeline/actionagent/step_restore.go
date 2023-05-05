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
	"time"

	"github.com/c2h5oh/datasize"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/actionagent/agenttool"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/pvolumes"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/filehelper"
)

const (
	cacheTempDir    = "/tmp"
	cacheTempPrefix = "cache"
)

func (agent *Agent) restore() {
	for _, in := range agent.Arg.Context.InStorages {
		switch in.Type {

		case string(spec.StoreTypeNFS):
			tarFile := strings.TrimPrefix(in.Value, spec.StoreTypeNFSProto)
			tarDir := agent.EasyUse.ContainerContext
			err := agenttool.UnTar(tarFile, tarDir)
			if err != nil {
				if in.Optional {
					logrus.Printf("[restore] ignore optional restore, type: %s, (prepare to untar [%s] into [%s]).\n",
						spec.StoreTypeNFS, tarFile, tarDir)
					continue
				}
				agent.AppendError(err)
			}
		case string(spec.StoreTypeDiceVolumeLocal), string(spec.StoreTypeDiceVolumeFake):
			// nothing
		case string(spec.StoreTypeOSS):
			agent.AppendError(errors.New("[restore] not implemented now for OSS storage"))

		// dice-nfs-volume 类型，restore 时将 volume.path 下的 data (.tar) 解压到 containerContext 下
		case string(spec.StoreTypeDiceVolumeNFS):
			tarFile := filepath.Join(in.Value, "data")
			tarDir := agent.EasyUse.ContainerContext
			err := agenttool.UnTar(tarFile, tarDir)
			if err != nil {
				if in.Optional {
					logrus.Printf("[restore] ignore optional restore, type: %s, (prepare to untar [%s] into [%s]).\n",
						spec.StoreTypeDiceVolumeNFS, tarFile, tarDir)
					continue
				}
				agent.AppendError(err)
			}
		case string(spec.StoreTypeDiceCacheNFS):
			tarExecPath := in.Labels[pvolumes.TaskCachePath]
			tarFile := in.Value + "/" + in.Labels[pvolumes.TaskCacheHashName] + pvolumes.TaskCacheCompressionSuffix
			if filehelper.CheckExist(tarFile, false) != nil {
				logrus.Printf("not get action cache: %s", in.Labels[pvolumes.TaskCachePath])
				continue
			}
			if err := agent.restoreCache(tarFile, tarExecPath); err != nil {
				logrus.Debugf("failed to untar cache file: %s to exec dir: %s, err: %v", tarFile, tarExecPath, err)
				continue
			}
			logrus.Printf("get action cache: %s success", in.Labels[pvolumes.TaskCachePath])
		default:
			agent.AppendError(errors.Errorf("[restore] unsupported store type: %s", in.Type))
		}
	}

	for _, f := range agent.Arg.Context.CmsDiceFiles {
		// invoke openapi /api/files?file=${uuid} to download files
		if err := agent.CallbackReporter.GetCmsFile(f.Labels[pvolumes.VoLabelKeyDiceFileUUID], f.Value); err != nil {
			agent.AppendError(err)
			continue
		}
	}
}

func (agent *Agent) restoreCache(tarFile, tarExecPath string) (err error) {
	if tarFileSize, isExceed := agent.isCachePathExceedLimit(tarFile); isExceed {
		return fmt.Errorf("untar file: %s size: %d bytes exceed limit size: %d bytes", tarFile, tarFileSize.Bytes(),
			agent.MaxCacheFileSizeMB.Bytes())
	}
	tmpDir, err := ioutil.TempDir(cacheTempDir, cacheTempPrefix)
	if err != nil {
		return err
	}
	if err = agenttool.UnTar(tarFile, tmpDir); err != nil {
		return err
	}
	// if tarExecDir exist, move tarExecDir to a temp directory
	if err = filehelper.CheckExist(tarExecPath, true); err == nil {
		tmpExecDir := fmt.Sprintf("%s%d", tarExecPath, time.Now().Unix())
		if err = agenttool.Mv(tarExecPath, tmpExecDir); err != nil {
			return err
		}
	}
	tarExecDir := filepath.Dir(tarExecPath)
	if err = agenttool.Mv(filepath.Join(tmpDir, filepath.Base(tarExecPath)), tarExecDir); err != nil {
		return err
	}
	return err
}

// isCacheFileExceedLimit return path size and is-exceed-limit-size
func (agent *Agent) isCachePathExceedLimit(tarFile string) (datasize.ByteSize, bool) {
	size, err := agenttool.GetDiskSize(tarFile)
	if err != nil {
		return 0, false
	}
	return size, size.Bytes() > agent.MaxCacheFileSizeMB.Bytes()
}
