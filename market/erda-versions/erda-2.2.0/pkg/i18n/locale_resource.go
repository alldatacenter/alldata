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

type LocaleResource struct {
	name               string
	resourceMap        map[string]string
	defaultResourceMap map[string]string
}

func (locale *LocaleResource) Name() string {
	return locale.name
}

func (locale *LocaleResource) ExistKey(key string) bool {
	_, ok := locale.resourceMap[key]
	if ok {
		return true
	}
	if locale.defaultResourceMap != nil {
		_, ok := locale.defaultResourceMap[key]
		return ok
	}
	return false
}

func (locale *LocaleResource) Get(key string, defaults ...string) string {
	content, ok := locale.resourceMap[key]
	if !ok {
		// 不存在尝试使用默认
		if locale.defaultResourceMap != nil {
			content, ok := locale.defaultResourceMap[key]
			if !ok {
				if len(defaults) > 0 {
					return defaults[0]
				}
				return key
			}
			return content
		}
		if len(defaults) > 0 {
			return defaults[0]
		}
		return key
	}
	return content
}

func (locale *LocaleResource) GetTemplate(key string) *Template {
	content := locale.Get(key)
	return NewTemplate(key, content)
}
