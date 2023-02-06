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
	"io/ioutil"
	"os"
	"path/filepath"
	"time"

	"github.com/c2h5oh/datasize"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent/agenttool"
	"github.com/erda-project/erda/pkg/filehelper"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/retry"
)

const (
	logUploadFilePrefix = "[upload files] "

	MetadataTypeDiceFile = "DiceFile"
)

func (agent *Agent) uploadDir() {
	// uploadDir 未指定，不上传
	if agent.EasyUse.ContainerUploadDir == "" {
		return
	}

	// 校验目录是否存在
	if err := filehelper.CheckExist(agent.EasyUse.ContainerUploadDir, true); err != nil {
		agent.AppendError(err)
		return
	}

	// 遍历目录
	files, err := ioutil.ReadDir(agent.EasyUse.ContainerUploadDir)
	if err != nil {
		agent.AppendError(err)
		return
	}
	if len(files) == 0 {
		return
	}

	logrus.Println(logUploadFilePrefix + "begin")
	defer logrus.Println(logUploadFilePrefix + "done")

	var needUploadFile []*os.File
	for _, f := range files {
		var (
			file *os.File
			err  error
		)
		// 目录压缩上传
		if f.IsDir() {
			tarName := filepath.Join(agent.EasyUse.ContainerTempTarUploadDir, f.Name()+".tar")
			err := agenttool.Tar(tarName, filepath.Join(agent.EasyUse.ContainerUploadDir, f.Name()))
			if err != nil {
				agent.AppendError(err)
				continue
			}
			file, err = os.Open(tarName)
			if err != nil {
				agent.AppendError(err)
				continue
			}
		} else {
			// 普通文件直接上传
			file, err = os.Open(filepath.Join(agent.EasyUse.ContainerUploadDir, f.Name()))
			if err != nil {
				agent.AppendError(err)
				continue
			}
		}
		needUploadFile = append(needUploadFile, file)
	}
	// 文件上传限制
	// 1. 单个文件大小 10MB
	// 2. 总大小 50MB
	const (
		singleFileSizeLimit = datasize.MB * 10
		totalFileSizeLimit  = datasize.MB * 50
	)
	var currentTotalFileSize datasize.ByteSize
	var uploadedFiles []*apistructs.File
	for _, f := range needUploadFile {
		// 判断单个文件大小
		fileInfo, err := f.Stat()
		if err != nil {
			agent.AppendError(err)
			continue
		}
		currentFileSize := datasize.ByteSize(fileInfo.Size())
		if currentFileSize > singleFileSizeLimit {
			logrus.Printf(logUploadFilePrefix+"ignore file to upload, file is too large, fileName: %s, size: %s, singleFileLimit: %s\n",
				fileInfo.Name(), currentFileSize.HumanReadable(), singleFileSizeLimit.HumanReadable())
			continue
		}
		if currentTotalFileSize+currentFileSize > totalFileSizeLimit {
			logrus.Printf(logUploadFilePrefix+"ignore file to upload, total file size is too large, fileName: %s, fileSize: %s, currentTotalSize: %s, totalLimit: %s\n",
				fileInfo.Name(), currentFileSize.HumanReadable(), currentTotalFileSize.HumanReadable(), totalFileSizeLimit.HumanReadable())
			continue
		}
		// 上传
		diceFile, err := agent.uploadFile(f)
		if err != nil {
			logrus.Printf(logUploadFilePrefix+"upload failed, fileName: %s, size: %s, err: %v\n", fileInfo.Name(), currentFileSize.HumanReadable(), err)
			continue
		}
		currentTotalFileSize += currentFileSize
		logrus.Printf(logUploadFilePrefix+"upload success, fileName: %s, size: %s, currentTotalSize: %s", fileInfo.Name(), currentFileSize.HumanReadable(), currentTotalFileSize.HumanReadable())
		uploadedFiles = append(uploadedFiles, diceFile)
	}
	// put into metafile
	var meta metadata.Metadata
	for _, f := range uploadedFiles {
		meta = append(meta, metadata.MetadataField{
			Name:  f.DisplayName,
			Value: f.UUID,
			Type:  MetadataTypeDiceFile,
		})
	}
	err = agent.callbackToPipelinePlatform(&Callback{Metadata: meta})
	if err != nil {
		agent.AppendError(err)
	}
}

func (agent *Agent) uploadFile(file *os.File) (*apistructs.File, error) {
	var uploadResp apistructs.FileUploadResponse

	err := retry.DoWithInterval(func() error {
		var err error
		uploadResp, err = agent.CallbackReporter.UploadFile(agent.Arg.PipelineID, agent.Arg.PipelineTaskID, file)
		if err != nil {
			return err
		}
		return nil
	}, 5, time.Second*5)
	if err != nil {
		return nil, err
	}

	return uploadResp.Data, nil
}
