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

package endpoints

import (
	"testing"

	"gotest.tools/assert"
)

func Test_getAppNameFromYmlPath(t *testing.T) {
	var tables = []struct {
		path    string
		appName string
	}{
		{
			path:    "name/dev/master/dice.yml",
			appName: "name",
		},
		{
			path:    "/baseName/dev/master/dice.yml",
			appName: "baseName",
		},
		{
			path:    "",
			appName: "",
		},
	}

	for _, v := range tables {
		assert.Equal(t, getAppNameFromYmlPath(v.path), v.appName)
	}
}

func Test_getBranchFromYmlPath(t *testing.T) {
	var tables = []struct {
		path   string
		branch string
		name   string
	}{
		{
			path:   "name/dev/master/dice.yml",
			branch: "master",
			name:   "/dice.yml",
		},
		{
			path:   "/baseName/dev/dev/aa/dice.yml",
			branch: "dev",
			name:   "/aa/dice.yml",
		},
		{
			path:   "64/DEV/feature/local/pipeline.yml",
			branch: "feature/local",
			name:   "/pipeline.yml",
		},
		{
			path:   "docker-spring-boot-java-web-service-example/TEST/develop/.dice/pipelines/xxx.yml",
			branch: "develop",
			name:   "/.dice/pipelines/xxx.yml",
		},
	}

	for _, v := range tables {
		assert.Equal(t, getBranchFromYmlPath(v.path, v.name), v.branch)
	}
}
