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

package sqllint

import (
	"encoding/json"
	"io/ioutil"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"sigs.k8s.io/yaml"
)

type Config struct {
	Name  string          `yaml:"name" json:"name"`
	Alias string          `yaml:"alias" json:"alias"`
	White *White          `yaml:"white" json:"white"`
	Meta  json.RawMessage `yaml:"meta" json:"meta"`
}

func (c Config) DoNotLintOn(moduleName, scriptName string) bool {
	return c.White != nil && c.White.Match(moduleName, scriptName)
}

type White struct {
	Everything  bool     `json:"everything"`
	Modules     []string `yaml:"modules"`
	Filenames   []string `yaml:"filenames"`
	CommittedAt []string `yaml:"committedAt"`
	Patterns    []string `yaml:"patterns"`
}

func (w White) Match(moduleName, scriptName string) bool {
	if w.Everything {
		return true
	}

	// whether it can match the module
	for _, module := range w.Modules {
		if module == moduleName {
			return true
		}
	}
	// whether is can match the filename
	scriptName = filepath.Base(scriptName)
	ext := filepath.Ext(scriptName)
	scriptName = strings.TrimSuffix(scriptName, ext)
	for _, filename := range w.Filenames {
		filename := filepath.Base(filename)
		ext := filepath.Ext(filename)
		filename = strings.TrimSuffix(filename, ext)
		if filename == scriptName {
			return true
		}
	}
	// whether it can match the committedAt
	if len(scriptName) >= 8 {
		if _, err := time.Parse("20060102", scriptName[:8]); err == nil {
			for _, t := range w.CommittedAt {
				switch {
				case strings.HasPrefix(t, ">="):
					if scriptName[:8] >= strings.TrimPrefix(t, ">=") {
						return true
					}
				case strings.HasPrefix(t, ">"):
					if scriptName[:8] >= strings.TrimPrefix(t, ">") {
						return true
					}
				case strings.HasPrefix(t, "<="):
					if scriptName[:8] <= strings.TrimPrefix(t, "<=") {
						return true
					}
				case strings.HasPrefix(t, "<"):
					if scriptName[:8] < strings.TrimPrefix(t, "<") {
						return true
					}
				case strings.HasPrefix(t, "="):
					if scriptName[:8] == strings.TrimLeft(t, "=") {
						return true
					}
				}
			}
		}
	}
	// whether it can match the regex pattern
	for _, pat := range w.Patterns {
		if ok, _ := regexp.Match(pat, []byte(scriptName)); ok {
			return true
		}
	}

	return false
}

func LoadConfig(data []byte) (map[string]Config, error) {
	var (
		list    []Config
		configs = make(map[string]Config)
	)
	if err := yaml.Unmarshal(data, &list); err != nil {
		return nil, errors.Wrap(err, "invalid configuration file")
	}
	for _, config := range list {
		if config.Alias == "" {
			config.Alias = config.Name
		}
		if _, ok := configs[config.Alias]; ok {
			return nil, errors.Errorf("the lint item %s already exists, please do not repeat the definetion.", strconv.Quote(config.Alias))
		}
		configs[config.Alias] = config
	}
	return configs, nil
}

func LoadConfigFromLocal(filename string) (map[string]Config, error) {
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to ReadFile: %s", filename)
	}
	return LoadConfig(data)
}
