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
	"encoding/json"
	"net/http"
	"net/url"
	"sort"
	"testing"
	"time"

	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
)

func TestUnmarshalApplicationReleaseList(t *testing.T) {
	modes := map[string]apistructs.ReleaseDeployMode{
		"modeA": {
			ApplicationReleaseList: [][]string{{"id1", "id2", "id3"}},
		},
		"modeB": {
			ApplicationReleaseList: [][]string{{"id4", "id5", "id6"}},
		},
	}
	data, err := json.Marshal(modes)
	if err != nil {
		t.Fatal(err)
	}

	res, err := unmarshalApplicationReleaseList(string(data))
	if err != nil {
		t.Fatal(err)
	}
	sort.Strings(res)

	list := []string{"id1", "id2", "id3", "id4", "id5", "id6"}
	if len(list) != len(res) {
		t.Fatal("test failed, length of res is not expected")
	}
	for i := range list {
		if list[i] != res[i] {
			t.Errorf("test failed, res is not expected")
			break
		}
	}
}

func TestMakeMetadata(t *testing.T) {
	modes := map[string]apistructs.ReleaseDeployMode{
		"default": {
			DependOn: []string{"modeA"},
			Expose:   true,
			ApplicationReleaseList: [][]string{
				{"release1"},
			},
		},
	}
	modesData, err := json.Marshal(modes)
	if err != nil {
		t.Fatal(err)
	}

	createdAt, err := time.Parse("2006-01-02T15:04:05Z", "2022-03-25T00:24:00Z")
	if err != nil {
		t.Fatal(err)
	}

	projectRelease := &dbclient.Release{
		Desc:      "testDesc",
		Changelog: "testMarkdown",
		Modes:     string(modesData),
		Version:   "testVersion",
		CreatedAt: createdAt,
	}
	labels := map[string]string{
		"gitBranch":        "testBranch",
		"gitCommitId":      "testCommitId",
		"gitCommitMessage": "testMsg",
		"gitRepo":          "testRepo",
	}
	data, err := json.Marshal(labels)
	if err != nil {
		t.Fatal(err)
	}
	appReleases := []dbclient.Release{
		{
			ReleaseID:       "release1",
			Changelog:       "testMarkdown",
			Labels:          string(data),
			Version:         "release/1.0",
			ApplicationName: "testApp",
		},
	}
	releaseMeta := apistructs.ReleaseMetadata{
		ApiVersion: "v1",
		Author:     "testUser",
		CreatedAt:  "2022-03-25T00:24:00Z",
		Source: apistructs.ReleaseSource{
			Org:     "erda",
			Project: "testProject",
			URL:     "https://erda.cloud/erda/dop/projects/999",
		},
		Version:   projectRelease.Version,
		Desc:      projectRelease.Desc,
		ChangeLog: projectRelease.Changelog,
		Modes: map[string]apistructs.ReleaseModeMetadata{
			"default": {
				DependOn: []string{"modeA"},
				Expose:   true,
				AppList: [][]apistructs.AppMetadata{
					{
						{
							AppName:          appReleases[0].ApplicationName,
							GitBranch:        labels["gitBranch"],
							GitCommitID:      labels["gitCommitId"],
							GitCommitMessage: labels["gitCommitMessage"],
							GitRepo:          labels["gitRepo"],
							ChangeLog:        appReleases[0].Changelog,
							Version:          appReleases[0].Version,
						},
					},
				},
			},
		},
	}

	target, err := yaml.Marshal(releaseMeta)
	if err != nil {
		t.Fatal(err)
	}

	u, err := url.Parse("https://erda.cloud")
	if err != nil {
		t.Fatal(err)
	}
	req := http.Request{URL: u}
	res, err := makeMetadata(&req, "erda", "testUser", "testProject", 999, projectRelease, appReleases)
	if err != nil {
		t.Fatal(err)
	}
	if string(res) != string(target) {
		t.Errorf("test failed, res is not expected")
	}
}
