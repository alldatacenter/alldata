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

package release

import (
	"encoding/json"
	"os"
	"sort"
	"testing"
	"time"

	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/release/db"
)

func TestConvertToListReleaseResponse(t *testing.T) {
	labels := map[string]string{
		"k1": "v1",
		"k2": "v2",
	}
	labelData, err := json.Marshal(labels)
	if err != nil {
		t.Fatal(err)
	}

	resources := []*pb.ReleaseResource{
		{
			Type: "testType",
			Name: "testName",
			URL:  "testURl",
		},
	}
	resourcesData, err := json.Marshal(resources)
	if err != nil {
		t.Fatal(err)
	}

	release := &db.Release{
		ReleaseID:        "testID",
		ReleaseName:      "testName",
		Desc:             "testDesc",
		Dice:             "testDice",
		Addon:            "testAddon",
		Changelog:        "testChangelog",
		IsStable:         true,
		IsFormal:         true,
		IsProjectRelease: true,
		Modes:            "testMode",
		Labels:           string(labelData),
		Version:          "testVersion",
		OrgID:            1,
		ProjectID:        1,
		ApplicationID:    1,
		ProjectName:      "testProject",
		ApplicationName:  "testApp",
		UserID:           "testUser",
		ClusterName:      "testCluster",
		Resources:        string(resourcesData),
		Reference:        1,
		CrossCluster:     true,
		CreatedAt:        time.Unix(0, 0),
		UpdatedAt:        time.Unix(0, 0),
	}

	respData, err := convertToListReleaseResponse(release, nil, nil, nil)
	if err != nil {
		t.Error(err)
	}

	if respData.ReleaseID != release.ReleaseID || respData.ReleaseName != release.ReleaseName || respData.Desc != release.Desc ||
		respData.Diceyml != release.Dice || respData.Addon != release.Addon || respData.Changelog != release.Changelog ||
		respData.IsStable != release.IsStable || respData.IsFormal != release.IsFormal || respData.IsProjectRelease != release.IsProjectRelease ||
		respData.Modes != release.Modes || respData.Version != release.Version || respData.OrgID != release.OrgID ||
		respData.ProjectID != release.ProjectID || respData.ApplicationID != release.ApplicationID ||
		respData.ProjectName != release.ProjectName || respData.ApplicationName != release.ApplicationName ||
		respData.UserID != release.UserID || respData.ClusterName != release.ClusterName ||
		respData.Reference != release.Reference || respData.CrossCluster != release.CrossCluster ||
		respData.CreatedAt.GetSeconds() != release.CreatedAt.Unix() ||
		respData.UpdatedAt.GetSeconds() != release.UpdatedAt.Unix() {
		t.Errorf("result is not expected")
	}

	respLabels, err := json.Marshal(respData.Labels)
	if err != nil {
		t.Fatal(err)
	}
	if string(respLabels) != string(labelData) {
		t.Errorf("labels field is not expected")
	}

	respResource, err := json.Marshal(respData.Resources)
	if err != nil {
		t.Fatal(err)
	}
	if string(respResource) != string(resourcesData) {
		t.Errorf("resource field is not expected")
	}
}

func TestRespDataReLoadImages(t *testing.T) {
	r := &pb.ReleaseGetResponseData{
		Diceyml: `
addons:
  demo-mysql:
    options:
      version: 5.7.29
    plan: mysql:basic
envs: {}
jobs:
  demo:
    cmd: echo "ok"
    image: addon-registry.default.svc.cluster.local:5000/erda-development-erda-development/go-demo:go-demo-1647947792062632471
    resources:
      cpu: 0.2
      mem: 128
services:
  java-demo:
    deployments:
      replicas: 1
    image: addon-registry.default.svc.cluster.local:5000/erda-java-demo/java-demo:java-demo-1641814084297378180
    ports:
    - expose: true
      port: 8080
    resources:
      cpu: 2
      mem: 512
version: "2.0"`,

		Modes: map[string]*pb.ModeSummary{
			"default": {
				ApplicationReleaseList: []*pb.ReleaseSummaryArray{
					{
						List: []*pb.ApplicationReleaseSummary{
							{
								DiceYml: `
addons:
  demo-mysql:
    options:
      version: 5.7.29
    plan: mysql:basic
envs: {}
jobs:
  demo:
    cmd: echo "ok"
    image: addon-registry.default.svc.cluster.local:5000/erda-development-erda-development/go-demo:go-demo-1647947792062632471
    resources:
      cpu: 0.2
      mem: 128
services:
  java-demo:
    deployments:
      replicas: 1
    image: addon-registry.default.svc.cluster.local:5000/erda-java-demo/java-demo:java-demo-1641814084297378180
    ports:
    - expose: true
      port: 8080
    resources:
      cpu: 2
      mem: 512
version: "2.0"`,
							},
						},
					},
				},
			},
		},
	}
	if err := respDataReLoadImages(r); err != nil {
		t.Error(err)
	}
}

func TestReleaseService_GetImages(t *testing.T) {
	dices := []string{
		`addons:
  mysql:
    options:
      version: 5.7.23
    plan: mysql:basic
jobs: {}
services:
  nginx:
    deployments:
      replicas: 1
    image: test.image.com/nginx:testTag
    resources:
      cpu: 0.1
      mem: 128
version: "2.0"
`,
	}
	s := &ReleaseService{}
	images := s.GetImages(dices)
	if len(images) != 1 {
		t.Fatal("length of images is not expected")
	}

	if images[0].Image != "test.image.com/nginx:testTag" || images[0].ImageName != "nginx" || images[0].ImageTag != "testTag" {
		t.Errorf("image is not expected")
	}

}

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

func TestParseMetadata(t *testing.T) {
	target := apistructs.ReleaseMetadata{
		ApiVersion: "v1",
		Author:     "erda",
		CreatedAt:  "2022-03-25T00:24:00Z",
		Source: apistructs.ReleaseSource{
			Org:     "erda",
			Project: "testProject",
			URL:     "https://erda.cloud/dop/projects/999",
		},
		Version:   "1.0",
		Desc:      "testDesc",
		ChangeLog: "testChangelog",
		Modes: map[string]apistructs.ReleaseModeMetadata{
			"modeA": {
				DependOn: []string{"modeB"},
				Expose:   true,
				AppList: [][]apistructs.AppMetadata{{
					{
						AppName:          "testApp",
						GitBranch:        "release/1.0",
						GitCommitID:      "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
						GitCommitMessage: "test",
						GitRepo:          "testRepo",
						ChangeLog:        "null",
						Version:          "1.0",
					},
				}},
			},
		},
	}

	file, err := os.Open("./release_test_data.zip")
	if err != nil {
		t.Fatal(err)
	}
	defer file.Close()

	metadata, err := parseMetadata(file)
	if err != nil {
		t.Fatal(err)
	}

	if metadata.Version != target.Version || metadata.Desc != target.Desc || metadata.ChangeLog != target.ChangeLog {
		// TODO: metadata.AppList[0][0] != target.AppList[0][0] {
		t.Errorf("test failed, result metadata is not expected")
	}
}

func TestIsSliceEqual(t *testing.T) {
	a := []string{"1", "2", "3"}
	b := []string{"1", "2", "2"}
	c := []string{"1"}

	if !isSliceEqual(a, a) {
		t.Errorf("expect equal, actual not")
	}
	if isSliceEqual(a, b) {
		t.Errorf("expect not equal, actual equal")
	}
	if isSliceEqual(a, c) {
		t.Errorf("expect not equal, actual equal")
	}
}

func TestHasLoopDependence(t *testing.T) {
	modes := map[string]*pb.Mode{
		"modeA": {
			DependOn: []string{"modeA"},
		},
	}
	if !hasLoopDependence(modes) {
		t.Errorf("expected: has loop dependence, actual not")
	}

	modes = map[string]*pb.Mode{
		"modeA": {
			DependOn: []string{"modeB"},
		},
		"modeB": {
			DependOn: []string{"modeA"},
		},
	}
	if !hasLoopDependence(modes) {
		t.Errorf("expected: has loop dependence, actual not")
	}

	modes = map[string]*pb.Mode{
		"modeA": {
			DependOn: []string{"modeB"},
		},
		"modeB": {
			DependOn: []string{"modeC"},
		},
		"modeC": {
			DependOn: []string{"modeA"},
		},
	}
	if !hasLoopDependence(modes) {
		t.Errorf("expected: has loop dependence, actual not")
	}

	modes = map[string]*pb.Mode{
		"modeA": {
			DependOn: []string{"modeB", "modeC"},
		},
		"modeB": {
			DependOn: []string{"modeC"},
		},
		"modeC": {},
	}
	if hasLoopDependence(modes) {
		t.Errorf("expected: no dependence, actual has")
	}
}
