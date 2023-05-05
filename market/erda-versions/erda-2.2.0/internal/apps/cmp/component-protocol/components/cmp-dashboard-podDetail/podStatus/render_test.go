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

package PodStatus

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/cputil"
)

func TestPodStatus_GenComponentState(t *testing.T) {
	c := &cptype.Component{State: map[string]interface{}{
		"clusterName": "testClusterName",
		"podId":       "testPodID",
	}}
	podStatus := &PodStatus{}
	if err := podStatus.GenComponentState(c); err != nil {
		t.Fatal(err)
	}

	ok, err := cputil.IsDeepEqual(c.State, podStatus.State)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}

func TestPodStatus_Transfer(t *testing.T) {
	podStatus := &PodStatus{
		Props: Props{
			Text:      "testText",
			Status:    "testStatus",
			Breathing: true,
		},
		State: State{
			ClusterName: "testClusterID",
			PodID:       "testPodID",
		},
	}
	c := &cptype.Component{}
	podStatus.Transfer(c)

	ok, err := cputil.IsDeepEqual(c, podStatus)
	if err != nil {
		t.Fatal(err)
	}
	if !ok {
		t.Errorf("test failed, json is not equal")
	}
}
