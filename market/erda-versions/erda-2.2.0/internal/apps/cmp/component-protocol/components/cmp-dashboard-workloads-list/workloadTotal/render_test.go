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

package workloadTotal

import (
	"encoding/json"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

func TestComponentWorkloadTitle_GenComponentState(t *testing.T) {
	component := &cptype.Component{
		State: map[string]interface{}{
			"values": Values{
				DeploymentsCount: Count{
					Active:   1,
					Abnormal: 1,
					Updating: 1,
				},
				DaemonSetCount: Count{
					Active:   1,
					Abnormal: 1,
				},
				StatefulSetCount: Count{
					Active:   1,
					Abnormal: 1,
				},
				JobCount: Count{
					Active:    1,
					Succeeded: 1,
					Failed:    1,
				},
				CronJobCount: Count{
					Active: 1,
				},
			},
		},
	}
	src, err := json.Marshal(component.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	f := &ComponentWorkloadTotal{}
	if err := f.GenComponentState(component); err != nil {
		t.Errorf("test failed, %v", err)
	}

	dst, err := json.Marshal(f.State)
	if err != nil {
		t.Errorf("test failed, %v", err)
	}

	if string(src) != string(dst) {
		t.Error("test failed, generate result is unexpected")
	}
}
