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
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestConvertToPbModes(t *testing.T) {
	modes := map[string]apistructs.ReleaseDeployMode{
		"default": {
			DependOn:               []string{"modeA"},
			Expose:                 true,
			ApplicationReleaseList: [][]string{{"id1", "id2"}, {"id3", "id4"}},
		},
	}

	data, err := json.Marshal(modes)
	if err != nil {
		t.Fatal(err)
	}

	var obj map[string]interface{}
	if err = json.Unmarshal(data, &obj); err != nil {
		t.Fatal(err)
	}

	res := convertToPbModes(obj)
	resMode := res["default"]
	targetMode := modes["default"]
	if resMode == nil {
		t.Fatalf("result mode is nil")
	}

	if !reflect.DeepEqual(resMode.DependOn, targetMode.DependOn) {
		t.Fatalf("dependOn field is not expected")
	}

	if resMode.Expose != targetMode.Expose {
		t.Fatalf("expose field is not expected")
	}

	if len(resMode.ApplicationReleaseList) != len(targetMode.ApplicationReleaseList) {
		t.Fatalf("length of applicationReleaseList field is not expected")
	}

	for i, l := range targetMode.ApplicationReleaseList {
		if len(resMode.ApplicationReleaseList[i].List) != len(l) {
			t.Fatalf("length of applicationReleaseList field is not expected")
		}
		for j, id := range l {
			if resMode.ApplicationReleaseList[i].List[j] != id {
				t.Fatalf("applicationRelease id is not expected")
			}
		}
	}
}
