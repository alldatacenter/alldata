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

package agenttool

import (
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strings"

	"github.com/c2h5oh/datasize"
	"github.com/mholt/archiver"

	"github.com/erda-project/erda/pkg/retry"
)

// Tar is the forth step of reource agent do
//
// 可选方案：
// a. tar 目标地址直接指向 NFS
// b. 在容器内制作 tar，移动至 NFS
// 经过测试，选用 a 速度更快
func Tar(tarFile, tarDir string) error {
	tarExecDir := filepath.Dir(tarFile)
	if err := os.Chdir(tarExecDir); err != nil {
		return err
	}
	if err := tar(tarFile, tarDir); err != nil {
		return err
	}
	return nil
}

func UnTar(tarFile, unTarDir string) error {
	err := retry.Do(func() error {
		return unTar(tarFile, unTarDir)
	}, 3)
	return err
}

func tar(tarAbsPath string, mainSrc string, otherSrcs ...string) error {
	if IsTarCommandExist() {
		args := []string{"-cf", tarAbsPath}
		args = append(args, getTarSrcArg(mainSrc)...)
		for _, src := range otherSrcs {
			args = append(args, getTarSrcArg(src)...)
		}
		cmd := exec.Command("tar", args...)
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		err := cmd.Run()
		if err != nil {
			os.RemoveAll(tarAbsPath)
			return archiver.Tar.Make(tarAbsPath, append([]string{mainSrc}, otherSrcs...))
		}
		return err
	}
	return archiver.Tar.Make(tarAbsPath, append([]string{mainSrc}, otherSrcs...))
}

func unTar(tarAbsPath string, destDir string) error {
	if IsTarCommandExist() {
		os.Mkdir(destDir, os.ModePerm)
		cmd := exec.Command("tar", "-xf", tarAbsPath, "-C", destDir)
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		err := cmd.Run()
		if err != nil {
			os.RemoveAll(destDir)
			return archiver.Tar.Open(tarAbsPath, destDir)
		}
		return err
	}
	return archiver.Tar.Open(tarAbsPath, destDir)
}

func getTarSrcArg(sourcePath string) []string {
	parentIndex := strings.LastIndex(sourcePath, "/")
	if parentIndex == -1 {
		return []string{sourcePath}
	}
	//去除绝对路径中的父路径
	pathName := filepath.Base(sourcePath)
	return []string{"-C", sourcePath[0:parentIndex], pathName}
}
func IsTarCommandExist() bool {
	cmdName := "tar"
	pathEnv := os.Getenv("PATH")
	pathEnvList := strings.Split(pathEnv, ":")
	for _, pathItem := range pathEnvList {
		fullCmdName := path.Join(pathItem, cmdName)
		if _, err := os.Stat(fullCmdName); err == nil {
			return true
		}
	}
	return false
}

// GetDiskSize if dir recursion sum size, file return size directly.
// return size unit is Byte
func GetDiskSize(path string) (datasize.ByteSize, error) {
	var totalSize datasize.ByteSize
	fi, err := os.Stat(path)
	if err != nil {
		return 0, err
	}
	if fi.IsDir() {
		files, err := os.ReadDir(path)
		if err != nil {
			return 0, err
		}
		for _, file := range files {
			size, err := GetDiskSize(filepath.Join(path, file.Name()))
			if err != nil {
				return 0, err
			}
			totalSize += size
		}
	} else {
		totalSize += datasize.ByteSize(fi.Size())
	}
	return totalSize, nil
}
