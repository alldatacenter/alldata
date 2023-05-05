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

package command

import (
	"io/ioutil"
	"os"

	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/tools/cli/utils"
)

var ConfigVersion string = "v0.0.1"

type OrgInfo struct {
	ID   uint64 `json:"id"`
	Name string `json:"name"`
	Desc string `json:"desc"`
}

type ProjectInfo struct {
	Version      string            `yaml:"version"`
	Server       string            `yaml:"server"`
	Org          string            `yaml:"org"`
	OrgID        uint64            `yaml:"org_id"`
	Project      string            `yaml:"project"`
	ProjectID    uint64            `yaml:"project_id"`
	Applications []ApplicationInfo `yaml:"applications"`
}

type ApplicationInfo struct {
	Application   string `yaml:"name"`
	ApplicationID uint64 `yaml:"id"`
	Mode          string `yaml:"mode"`
	Desc          string `yaml:"desc"`
	Sonarhost     string `yaml:"sonarhost"`
	Sonartoken    string `yaml:"sonartoken"`
	Sonarproject  string `yaml:"sonarproject"`
}

func GetProjectConfigFrom(configfile string) (*ProjectInfo, error) {
	info := ProjectInfo{Version: ConfigVersion}

	f, err := os.Open(configfile)
	if err != nil {
		return &info, err
	}
	if err := yaml.NewDecoder(f).Decode(&info); err != nil {
		return &info, err
	}

	return &info, nil
}

func GetProjectConfig() (string, *ProjectInfo, error) {
	info := ProjectInfo{Version: ConfigVersion}
	config, err := utils.FindProjectConfig()
	if err != nil {
		return config, &info, err
	}

	f, err := os.Open(config)
	if err != nil {
		return config, &info, err
	}
	if err := yaml.NewDecoder(f).Decode(&info); err != nil {
		os.Remove(config)
		return config, &info, err
	}

	return config, &info, nil
}

func SetProjectConfig(file string, conf *ProjectInfo) error {
	c, err := yaml.Marshal(conf)
	if err != nil {
		return err
	}
	err = ioutil.WriteFile(file, c, 0655)
	if err != nil {
		return err
	}

	return nil
}
