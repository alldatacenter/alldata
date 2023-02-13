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

package i18n

import (
	"encoding/json"
	"io/ioutil"
	"path"
	"path/filepath"
	"strings"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
)

type LocaleResourceLoader struct {
	localeMap     map[string]map[string]string
	defaultLocale string
}

func NewLoader() *LocaleResourceLoader {
	return &LocaleResourceLoader{
		localeMap:     map[string]map[string]string{},
		defaultLocale: "zh-CN",
	}
}

func (loader *LocaleResourceLoader) addResource(locale string, keys map[string]string) {
	resourceMap, ok := loader.localeMap[locale]
	if !ok {
		resourceMap = map[string]string{}
	}
	for k, v := range keys {
		resourceMap[k] = v
	}
	loader.localeMap[locale] = resourceMap
}

func (loader *LocaleResourceLoader) LoadFile(fileList ...string) error {
	for _, filePath := range fileList {
		resourceBytes, err := ioutil.ReadFile(filePath)
		if err != nil {
			return err
		}
		var resourceData map[string]map[string]string
		ext := filepath.Ext(filePath)
		if ext == ".json" {
			err = json.Unmarshal(resourceBytes, &resourceData)
			if err != nil {
				logrus.Errorf("error parse json file %s %s\n", filePath, err)
				return err
			}
		} else if ext == ".yaml" || ext == ".yml" {
			err = yaml.Unmarshal(resourceBytes, &resourceData)
			if err != nil {
				logrus.Errorf("error parse yaml file %s %s\n", filePath, err)
				return err
			}
		}
		for localeName, localeResource := range resourceData {
			loader.addResource(localeName, localeResource)
		}
	}
	return nil
}

func (loader *LocaleResourceLoader) LoadDir(dir string) error {
	fileInfos, err := ioutil.ReadDir(dir)
	if err != nil {
		return err
	}
	for _, fileInfo := range fileInfos {
		if !fileInfo.IsDir() {
			err := loader.LoadFile(path.Join(dir, fileInfo.Name()))
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (loader *LocaleResourceLoader) DefaultLocale(defaultLocale string) {
	loader.defaultLocale = defaultLocale
}

func (loader *LocaleResourceLoader) Locale(locales ...string) *LocaleResource {
	defaultKeyMap, defaultExist := loader.localeMap[loader.defaultLocale]
	for _, locale := range locales {
		keyMap, ok := loader.localeMap[locale]
		if ok {
			return &LocaleResource{
				name:               locale,
				resourceMap:        keyMap,
				defaultResourceMap: defaultKeyMap,
			}
		}
		if locale == "" {
			continue
		}
		// 尽可能尝试匹配接近的名称,前缀相同的优先
		for localeName := range loader.localeMap {
			if strings.Index(localeName, locale) == 0 || strings.Index(locale, localeName) == 0 {
				return &LocaleResource{
					name:               localeName,
					resourceMap:        loader.localeMap[localeName],
					defaultResourceMap: defaultKeyMap,
				}
			}
		}
	}
	if defaultExist {
		return &LocaleResource{
			name:        loader.defaultLocale,
			resourceMap: defaultKeyMap,
		}
	}

	logrus.Debugf("no valid locale exist %s", locales)
	return &LocaleResource{resourceMap: map[string]string{}}
}
