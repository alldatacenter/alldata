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

package nodeFilter

import (
	"reflect"
	"testing"

	"github.com/rancher/wrangler/pkg/data"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodes/common/filter"
)

func TestDoFilter(t *testing.T) {
	type args struct {
		nodeList []data.Object
		values   filter.Values
	}
	d := []data.Object{{"id": "nameID", "metadata": map[string]interface{}{"name": "name", "labels": map[string]interface{}{"key1": "value1"}}}}
	tests := []struct {
		name string
		args args
		want []data.Object
	}{
		{
			name: "test",
			args: args{
				nodeList: d,
				values:   map[string]interface{}{"Q": "na", "service": []interface{}{"key1=value1"}},
			},
			want: d,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := DoFilter(tt.args.nodeList, tt.args.values); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("DoFilter() = %v, want %v", got, tt.want)
			}
		})
	}
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}

func TestNodeFilter_getState(t *testing.T) {
	type fields struct {
		Filter      filter.Filter
		clusterName string
	}
	type args struct {
		labels map[string]struct{}
	}
	sdk := &cptype.SDK{
		Tran:     &MockTran{},
		Identity: nil,
		InParams: nil,
		Lang:     nil,
	}
	f := &NodeFilter{Filter: filter.Filter{SDK: sdk}}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		// TODO: Add test cases.
		{
			name:   "test",
			fields: fields{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			f.getState(tt.args.labels)
		})
	}
}

func TestNodeFilter_EncodeURLQuery(t *testing.T) {
	type fields struct {
		Filter      filter.Filter
		clusterName string
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nf := &NodeFilter{
				Filter:      tt.fields.Filter,
				clusterName: tt.fields.clusterName,
			}
			if err := nf.EncodeURLQuery(); (err != nil) != tt.wantErr {
				t.Errorf("EncodeURLQuery() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestNodeFilter_DecodeURLQuery(t *testing.T) {
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
		t.Run(tt.name, func(t *testing.T) {
			nf := &NodeFilter{
				Filter: filter.Filter{
					SDK: &cptype.SDK{
						InParams: map[string]interface{}{
							"filter__urlQuery": "eyJzdGF0ZSI6WyJrdWJlcm5ldGVzLmlvL2hvc3RuYW1lPW5vZGUtMDEwMDAwMDA2MjIwIl19",
						},
					},
				},
			}
			if err := nf.DecodeURLQuery(); (err != nil) != tt.wantErr {
				t.Errorf("DecodeURLQuery() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
