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

package filehelper

import (
	"fmt"
	"net/url"
	"path/filepath"
	"regexp"
	"strings"
)

const apiFile string = "/api/files"

func Abs2Rel(path string) string {
	path = filepath.Clean(path)
	if strings.HasPrefix(path, "/") {
		path = fmt.Sprintf(".%s", path)
	}
	return filepath.Clean(path)
}

func APIFileUrlRetriever(fileUrl string) string {
	u, err := url.Parse(fileUrl)
	if err != nil {
		return fileUrl
	}

	if strings.HasPrefix(u.Path, apiFile) {
		return u.Path
	}
	return fileUrl
}

func FilterAPIFileUrl(content string) string {
	r := regexp.MustCompile(`\(([^)]+)\)`)
	for _, sub := range r.FindAllStringSubmatch(content, -1) {
		path := APIFileUrlRetriever(sub[1])
		if path != sub[1] {
			content = strings.Replace(content, sub[1], path, 1)
		}
	}
	return content
}
