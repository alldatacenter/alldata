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

package tabs

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

func TestTableTabs_EncodeURLQuery(t1 *testing.T) {
	tests := []struct {
		name    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Tabs{}
			if err := t.EncodeURLQuery(); (err != nil) != tt.wantErr {
				t1.Errorf("EncodeURLQuery() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestTableTabs_DecodeURLQuery(t1 *testing.T) {

	tests := []struct {
		name    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Tabs{
				SDK: &cptype.SDK{
					InParams: map[string]interface{}{
						"tableTabs__urlQuery": "Im1lbS1hbmFseXNpcyI=",
					},
				},
			}
			if err := t.DecodeURLQuery(); (err != nil) != tt.wantErr {
				t1.Errorf("DecodeURLQuery() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
