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

package utils

import (
	"errors"
	"io/ioutil"
	"os"
	"os/user"
	"path"
	"path/filepath"
)

var (
	NotExist = errors.New("not exist")

	GlobalErdaDir    = ".erda.d"
	GlobalConfigFile = "config"

	ProjectDiceDir     = ".dice"
	ProjectPipelineDir = ".dice/pipelines"

	ProjectErdaDir = ".erda"

	ProjectErdaConfigDir = ".erda.d"
)

func existProjErdaConfigDir(path string) bool {
	f, err := os.Stat(mkProjErdaConfigDirPath(path))
	if os.IsNotExist(err) {
		return false
	}
	return f.IsDir()
}

func mkProjErdaConfigDirPath(path string) string {
	return filepath.Join(path, ProjectErdaConfigDir)
}

func FindProjectConfig() (string, error) {
	current, err := os.Getwd()
	if err != nil {
		return "", err
	}

	var res string

	for {
		if existProjErdaConfigDir(current) {
			res = mkProjErdaConfigDirPath(current)
			file := filepath.Join(res, GlobalConfigFile)
			f, err := os.Stat(file)
			if os.IsNotExist(err) {
				return file, NotExist
			}
			if f.IsDir() {
				return file, errors.New(res + " is a dirctory")
			}
			return file, nil
		}
		origin := current
		current = filepath.Dir(current)
		if current == origin {
			return "", NotExist
		}
	}

	return "", NotExist
}

func FindGlobalConfig() (string, error) {
	erdaDir, err := FindGlobalErdaDir()
	if err == NotExist {
		_, err = CreateGlobalErdaDir()
	}
	if err != nil {
		return "", err
	}
	file := filepath.Join(erdaDir, GlobalConfigFile)
	f, err := os.Stat(file)
	if os.IsNotExist(err) {
		return file, NotExist
	}
	if f.IsDir() {
		return file, errors.New(file + " is a dirctory")
	}
	return file, nil
}

func FindGlobalErdaDir() (string, error) {
	u, err := user.Current()
	if err != nil {
		return "", err
	}
	dir := filepath.Join(u.HomeDir, GlobalErdaDir)
	f, err := os.Stat(dir)
	if os.IsNotExist(err) {
		return dir, NotExist
	}
	if !f.IsDir() {
		return "", NotExist
	}
	return dir, nil
}

func CreateGlobalErdaDir() (string, error) {
	u, err := user.Current()
	if err != nil {
		return "", err
	}
	dir := filepath.Join(u.HomeDir, GlobalErdaDir)
	if err := os.Mkdir(dir, 0755); err != nil {
		return "", err
	}
	return dir, nil
}

func FindProjectDiceDir() (string, error) {
	current, err := os.Getwd()
	if err != nil {
		return "", err
	}
	var res string
	for {
		if existProjDiceDir(current) {
			res = mkProjDiceDirPath(current)
			return res, nil
		}
		origin := current
		current = filepath.Dir(current)
		if current == origin {
			return "", NotExist
		}
	}
}

func CreateProjectDiceDir() (string, error) {
	current, err := os.Getwd()
	if err != nil {
		return "", err
	}
	pdir := mkProjDiceDirPath(current)
	if err := os.Mkdir(pdir, 0755); err != nil {
		return "", err
	}
	return pdir, nil
}

func existProjDiceDir(path string) bool {
	f, err := os.Stat(mkProjDiceDirPath(path))
	if os.IsNotExist(err) {
		return false
	}
	return f.IsDir()
}

func mkProjDiceDirPath(path string) string {
	return filepath.Join(path, ProjectDiceDir)
}

func FindProjectErdaDir() (string, error) {
	current, err := os.Getwd()
	if err != nil {
		return "", err
	}
	var res string
	for {
		if existProjErdaDir(current) {
			res = mkProjErdaDirPath(current)
			return res, nil
		}
		origin := current
		current = filepath.Dir(current)
		if current == origin {
			return "", NotExist
		}
	}
}

func CreateProjectErdaDir() (string, error) {
	current, err := os.Getwd()
	if err != nil {
		return "", err
	}
	pdir := mkProjErdaDirPath(current)
	if err := os.Mkdir(pdir, 0755); err != nil && !os.IsExist(err) {
		return "", err
	}
	return pdir, nil
}

func existProjErdaDir(path string) bool {
	f, err := os.Stat(mkProjErdaDirPath(path))
	if os.IsNotExist(err) {
		return false
	}
	return f.IsDir()
}

func mkProjErdaDirPath(path string) string {
	return filepath.Join(path, ProjectErdaDir)
}

func ListDir(dir string) ([]string, error) {
	var files []string
	fileInfos, err := ioutil.ReadDir(dir)
	if err != nil {
		return nil, err
	}

	for _, fi := range fileInfos {
		f := path.Join(dir, fi.Name())
		files = append(files, f)
	}

	return files, nil
}
