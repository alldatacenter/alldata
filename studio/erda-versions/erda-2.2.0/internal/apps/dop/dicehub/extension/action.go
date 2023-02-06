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

package extension

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/apistructs"
)

type Repo struct {
	// addr workPath
	addr string
	// versions ExtensionVersion dir path
	versions []string
}

// Version is a version of an Extension
type Version struct {
	Name    string
	Dirname string

	Spec          *apistructs.Spec // structure of spec.yml
	SpecContent   []byte           // content of spec.yml
	DiceContent   []byte           // content of dice.yml
	ReadmeContent []byte           // content of readme.md

	SwaggerContent []byte // content of swagger.yml
}

func (s *extensionService) InitExtension(addr string, forceUpdate bool) error {
	logrus.Infoln("Start init extension")

	// get all extensionVersion in repo
	repo := LoadExtensions(addr)

	// get all extensions existed
	allActionVersions, err := s.db.QueryAllExtensions()
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
	}

	// extensionVersionMap key:ExtensionName value:ExtensionVersion
	// one extension can have version more then 1
	extensionVersionMap := make(map[string][]string)

	// extensionTypeMap key:ExtensionName value:ExtensionType
	extensionTypeMap := make(map[string][]string)

	// add all actionVersions to map
	for _, v := range allActionVersions {
		version := extensionVersionMap[v.Name]
		version = append(version, v.Version)
		extensionVersionMap[v.Name] = version

		specData := apistructs.Spec{}
		err = yaml.Unmarshal([]byte(v.Spec), &specData)
		if err != nil {
			return err
		}

		extensionType := extensionTypeMap[v.Name]
		extensionType = append(extensionType, specData.Type)
		extensionTypeMap[v.Name] = extensionType
	}

	// push all actionVersions
	for _, v := range repo.versions {
		name, version, err := s.RunExtensionsPush(v, extensionVersionMap, extensionTypeMap, forceUpdate)
		if err == nil {
			logrus.Infoln("extension create success, name: ", name, ", version: ", version)
		} else {
			logrus.Infoln("extension create false, name: ", name, ", version: ", version, " err: ", err)
		}
	}
	return nil
}

// RunExtensionsPush push extensions
func (s *extensionService) RunExtensionsPush(dir string, extensionVersionMap, extensionTypeMap map[string][]string, forceUpdate bool) (string, string, error) {
	version, err := NewVersion(dir)
	if err != nil {
		return "", "", err
	}

	specData := version.Spec

	// if extension is existed, return
	versionNow := extensionVersionMap[specData.Name]
	typeNow := extensionTypeMap[specData.Name]
	needCreate := true
	for i, version := range versionNow {
		if version == specData.Version && typeNow[i] == specData.Type {
			needCreate = false
			break
		}
	}
	if !needCreate && !forceUpdate {
		return specData.Name, specData.Version, errors.New("extension is existed")
	}

	var request = &pb.ExtensionVersionCreateRequest{
		Name:        specData.Name,
		Version:     specData.Version,
		SpecYml:     string(version.SpecContent),
		DiceYml:     string(version.DiceContent),
		SwaggerYml:  string(version.SwaggerContent),
		Readme:      string(version.ReadmeContent),
		Public:      specData.Public,
		ForceUpdate: forceUpdate,
		All:         true,
		IsDefault:   specData.IsDefault,
	}

	_, err = s.CreateExtensionVersionByRequest(request)
	if err != nil {
		return request.Name, request.Version, err
	}

	return request.Name, request.Version, err
}

func NewVersion(dirname string) (*Version, error) {
	fileInfos, err := ioutil.ReadDir(dirname)
	if err != nil {
		return nil, errors.Wrap(err, "failed to ReadDir")
	}

	var version = Version{
		Name:           filepath.Base(dirname),
		Dirname:        dirname,
		Spec:           new(apistructs.Spec),
		SpecContent:    nil,
		DiceContent:    nil,
		ReadmeContent:  nil,
		SwaggerContent: nil,
	}
	for _, fileInfo := range fileInfos {
		if fileInfo.IsDir() {
			continue
		}
		switch {
		case strings.EqualFold(fileInfo.Name(), "spec.yml") || strings.EqualFold(fileInfo.Name(), "spec.yaml"):
			version.SpecContent, err = ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))
			if err != nil {
				return nil, errors.Wrap(err, "failed to ReadFile")
			}
			if err = yaml.Unmarshal(version.SpecContent, version.Spec); err != nil {
				return nil, errors.Wrap(err, "failed to parse "+fileInfo.Name())
			}

		case strings.EqualFold(fileInfo.Name(), "dice.yml") || strings.EqualFold(fileInfo.Name(), "dice.yaml"):
			version.DiceContent, _ = ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))

		case strings.EqualFold(fileInfo.Name(), "readme.md") || strings.EqualFold(fileInfo.Name(), "readme.markdown"):
			version.ReadmeContent, _ = ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))

		case strings.EqualFold(fileInfo.Name(), "swagger.json") || strings.EqualFold(fileInfo.Name(), "swagger.yml") ||
			strings.EqualFold(fileInfo.Name(), "swagger.yaml"):
			version.SwaggerContent, _ = ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))
		}
	}

	if version.Spec == nil || len(version.SpecContent) == 0 {
		return nil, errors.Errorf("spec file not found in %s", dirname)
	}

	return &version, nil
}

// LoadExtensions loads all extensions from the repo (contains all versions below)
func LoadExtensions(addr string) *Repo {
	repo := &Repo{
		addr: addr,
	}
	repo.locate(repo.addr, 0)
	return repo
}

// locate Recursively traverse folders
func (repo *Repo) locate(dirname string, deep int) {
	infos, ok := isThereSpecFile(dirname)
	if ok {
		repo.versions = append(repo.versions, dirname)
		return
	}

	for _, cur := range infos {
		// only find path /repoName/actions||addons/extensionsName
		if deep == 1 && cur.Name() != "actions" && cur.Name() != "addons" {
			continue
		}
		repo.locate(filepath.Join(dirname, cur.Name()), deep+1)
	}
}

// isThereSpecFile  check is there have spec.yml
func isThereSpecFile(dirname string) ([]os.FileInfo, bool) {
	var dirs []os.FileInfo
	infos, err := ioutil.ReadDir(dirname)
	if err != nil {
		return nil, false
	}
	for _, file := range infos {
		if file.IsDir() {
			dirs = append(dirs, file)
			continue
		}
		if strings.EqualFold(file.Name(), "spec.yml") || strings.EqualFold(file.Name(), "spec.yaml") {
			return nil, true
		}
	}
	return dirs, false
}
