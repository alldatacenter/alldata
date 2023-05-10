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

package conf

import (
	"embed"
	"encoding/json"
	"fmt"
	"log"
	"path"
	"strings"

	"gopkg.in/yaml.v3"
)

type Configure struct {
	FS      embed.FS
	Dirname string
}

type ConfigurationSearcher map[string]*ConfigurationFile

type ConfigurationFile struct {
	Index     string // file Index
	Filename  string // Filename with Extension
	Path      string // Path from dir root
	Extension string // file Extension
	Content   []byte // file contents
}

const (
	FileExtensionSep       = "|"
	JsonFileExtension      = ".json"
	YamlOrYmlFileExtension = ".yaml|.yml"
)

func (c *Configure) YamlOrYmlReader() *ConfigurationSearcher {
	files := ConfigurationSearcher{}
	reader(c.FS, c.Dirname, YamlOrYmlFileExtension, &files)
	return &files
}

func (c *Configure) JsonReader() *ConfigurationSearcher {
	files := ConfigurationSearcher{}
	reader(c.FS, c.Dirname, JsonFileExtension, &files)
	return &files
}

func reader(fs embed.FS, dirname, fileExtension string, files *ConfigurationSearcher) {
	entries, err := fs.ReadDir(dirname)
	if err != nil {
		log.Printf("Read dir(%s) with error: %+v\n", dirname, err)
	}
	for _, entry := range entries {
		info, err := entry.Info()
		if err != nil {
			log.Printf("Read fs entry with error: %+v\n", err)
		}

		filepath := fmt.Sprintf("%s/%s", dirname, info.Name())
		if info.IsDir() {
			reader(fs, filepath, fileExtension, files)
		}
		realFileExtension := path.Ext(info.Name())
		matchExtension := false
		extensions := strings.Split(fileExtension, FileExtensionSep)
		for _, s := range extensions {
			if realFileExtension == s {
				matchExtension = true
				break
			}
		}
		if !matchExtension {
			continue
		}
		file, err := fs.ReadFile(filepath)
		if err != nil {
			log.Printf("Read file(%s) with error: %+v\n", filepath, err)
			continue
		}

		filenameWithExtension := path.Base(info.Name())
		filenameWithOutExtension := strings.TrimSuffix(filenameWithExtension, realFileExtension)
		cf := ConfigurationFile{
			Index:     filenameWithOutExtension,
			Filename:  filenameWithExtension,
			Path:      filepath,
			Extension: realFileExtension,
			Content:   file,
		}
		(*files)[filenameWithOutExtension] = &cf
	}
}

func FileUnmarshal(fileExtension string, file []byte) (interface{}, error) {
	switch fileExtension {
	case JsonFileExtension:
		return jsonFileUnmarshal(file)
	case YamlOrYmlFileExtension:
		return yamlOrYmlFileUnmarshal(file)
	}
	return nil, nil
}

func jsonFileUnmarshal(file []byte) (interface{}, error) {
	var content map[string]interface{}
	err := json.Unmarshal(file, &content)
	if err != nil {
		return nil, err
	}
	return content, nil
}

func yamlOrYmlFileUnmarshal(file []byte) (interface{}, error) {
	var content interface{}
	err := yaml.Unmarshal(file, &content)
	if err != nil {
		return nil, err
	}
	return content, nil
}
