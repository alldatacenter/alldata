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
	"path/filepath"
)

const (
	VoLabelKeyContainerPath = "containerPath"
	VoLabelKeyContextPath   = "contextPath"
	VoLabelKeyStageOrder    = "stageOrder"
	VoLabelKeyVolumeID      = "ID"
	VoLabelKeyShareVolume   = "shareVolume"
	VoLabelKeyDiceFileUUID  = "diceFileUUID"
)

var (
	ContainerRootDir     = "/.pipeline/container"
	ContainerContextDir  = filepath.Join(ContainerRootDir, "context")
	ContainerMetadataDir = filepath.Join(ContainerRootDir, "metadata")
	ContainerUploadDir   = filepath.Join(ContainerRootDir, "uploaddir")

	ContainerVolumeMountRootDir = "/.pipeline/context"                  // task volume 的挂载目录的父目录
	ContainerDiceFilesDir       = "/.pipeline/container/cms/dice_files" // cms dice files 类型在运行时的挂载目录
)

// MakeTaskContainerWorkdir 生成 task 在容器内的 workdir 目录
func MakeTaskContainerWorkdir(taskName string) string {
	return filepath.Join(ContainerContextDir, taskName)
}

// MakeTaskContainerMetafilePath 生成 task 在容器内的 metadata 目录
func MakeTaskContainerMetafilePath(taskName string) string {
	return filepath.Join(ContainerMetadataDir, taskName, "metadata")
}

// MakeTaskContainerVolumeMountDir 生成 task 在容器内的 volume 挂载点
func MakeTaskContainerVolumeMountDir(taskName string) string {
	return filepath.Join(ContainerVolumeMountRootDir, taskName)
}

// MakeTaskContainerDiceFilesPath 生成 task 在容器内的文件配置路径
func MakeTaskContainerDiceFilesPath(fileName string) string {
	return filepath.Join(ContainerDiceFilesDir, fileName)
}
