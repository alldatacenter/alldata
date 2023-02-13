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

package template

import "regexp"

// Render 渲染简单模板 占位符格式: {{key}}
func Render(template string, params map[string]string) string {
	reg := regexp.MustCompile(`{{.+?}}`)
	result := reg.ReplaceAllStringFunc(template, func(s string) string {
		key := s[2 : len(s)-2]
		value, ok := params[key]
		if ok {
			return value
		}
		return s
	})
	return result
}

//获取模板内容 {{ context }}
func GetTemplateValue(template string) string {
	subMatchs := regexp.MustCompile(`^{{\s*(\S+)\s*}}`).FindStringSubmatch(template)
	if len(subMatchs) > 0 {
		return subMatchs[1]
	}
	return ""
}
