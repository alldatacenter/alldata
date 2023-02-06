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

package releaseFilter

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
)

func getPair() (Values, string) {
	v := Values{
		ApplicationIDs:    []string{"testAppID"},
		BranchID:          "testBranchID",
		CommitID:          "testCommitID",
		CreatedAtStartEnd: []int64{1, 1},
		ReleaseID:         "testReleaseID",
		UserIDs:           []string{"testUserID"},
		Latest:            "true",
		Version:           "test",
	}
	data, err := json.Marshal(v)
	if err != nil {
		panic(err)
	}
	encode := base64.StdEncoding.EncodeToString(data)
	return v, encode
}

func TestComponentReleaseFilter_DecodeURLQuery(t *testing.T) {
	values, encode := getPair()

	f := ReleaseFilter{
		sdk: &cptype.SDK{InParams: map[string]interface{}{
			"releaseFilter__urlQuery": encode,
		}},
	}

	if err := f.decodeURLQuery(); err != nil {
		t.Fatal(err)
	}

	isEqual, err := util.IsDeepEqual(values, f.State.Values)
	if err != nil {
		t.Fatal(err)
	}
	if !isEqual {
		t.Errorf("test failed, values is not expected after decode url query")
		fmt.Println(values)
		fmt.Println(f.State.Values)
	}
}

func TestComponentReleaseFilter_EncodeURLQuery(t *testing.T) {
	values, encode := getPair()
	f := ReleaseFilter{State: State{
		Values: values,
	}}
	if err := f.encodeURLQuery(); err != nil {
		t.Fatal(err)
	}
	if encode != f.State.ReleaseFilterURLQuery {
		t.Errorf("test failed, url query is not expected after encode")
	}
}
