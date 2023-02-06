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
	"strings"

	"github.com/pkg/errors"
)

type URLType string

var OrgURL URLType = "ErdaOrganizationURL"
var ProjectURL URLType = "ErdaProjectURL"
var ApplicatinURL URLType = "ErdaApplicationURL"
var GittarURL URLType = "ErdaGittarURL"

type OrganizationURLInfo struct {
	Scheme string
	Host   string
	Org    string
}

type ProjectURLInfo struct {
	OrganizationURLInfo
	ProjectId uint64
}

type ApplicationURLInfo struct {
	ProjectURLInfo
	ApplicationId uint64
}

type GitterURLInfo struct {
	OrganizationURLInfo
	Project     string
	Application string
}

func ClassifyURL(path string) (URLType, []string, error) {
	paths := strings.Split(path, "/")

	l := len(paths)

	if l < 3 {
		return "", nil, errors.Errorf("invalid erda url path %s", path)
	}

	if paths[2] == "dop" {
		if paths[3] == "projects" {
			if l >= 7 && paths[5] == "apps" {
				// /<org>/dop/projects/<projectID>/apps/<applicationID>
				return ApplicatinURL, paths, nil
			} else if l == 5 || (l == 6 && paths[5] == "apps") {
				// /<org>/dop/projects/<projectID>
				return ProjectURL, paths, nil
			}
		} else if l == 5 {
			// /<org>/dop/<project>/<app>
			return GittarURL, paths, nil
		}
	}

	return "", nil, errors.Errorf("invalid erda url path %s", path)
}
