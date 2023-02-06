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

package table

import (
	"context"
	"fmt"
	"reflect"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/cmp"
)

type NopTranslator struct{}

func (t NopTranslator) Get(lang i18n.LanguageCodes, key, def string) string { return key }

func (t NopTranslator) Text(lang i18n.LanguageCodes, key string) string { return key }

func (t NopTranslator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return fmt.Sprintf(key, args...)
}
func TestSortByNode(t *testing.T) {
	type args struct {
		data       []RowItem
		sortColumn string
		asc        bool
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "test",
			args: args{
				data: []RowItem{{
					Node: Node{
						Renders: []interface{}{
							[]interface{}{},
							[]interface{}{NodeLink{
								RenderType: "linkText",
								Value:      "123",
							},
							},
						},
					},
				}, {
					Node: Node{
						Renders: []interface{}{
							[]interface{}{},
							[]interface{}{
								NodeLink{
									RenderType: "linkText",
									Value:      "321",
								}, Labels{
									RenderType: "tagsRow",
								}},
						},
					},
				}, {
					ID: "1",
					Node: Node{
						Renders: []interface{}{
							[]interface{}{},
							[]interface{}{
								NodeLink{
									RenderType: "linkText",
									Value:      "123123",
								},
							},
						},
					},
				}},
				sortColumn: "Node",
				asc:        false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			SortByNode(tt.args.data, tt.args.sortColumn, tt.args.asc)
		})
	}
}

func TestSortByDistribution(t *testing.T) {
	type args struct {
		data       []RowItem
		sortColumn string
		asc        bool
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "testDistribution",
			args: args{
				data: []RowItem{{
					Distribution: Distribution{
						RenderType: "",
						Value:      "30",
						Status:     "",
						Tip:        "",
					},
				}, {
					Distribution: Distribution{
						RenderType: "",
						Value:      "20",
						Status:     "",
						Tip:        "",
					},
				}, {
					Distribution: Distribution{
						RenderType: "",
						Value:      "10",
						Status:     "",
						Tip:        "",
					},
				}},
				sortColumn: "Distribution",
				asc:        false,
			},
		},
		{
			name: "testUsage",
			args: args{
				data: []RowItem{{
					Usage: Distribution{
						RenderType: "",
						Value:      "28",
						Status:     "",
						Tip:        "",
					},
				}, {
					Usage: Distribution{
						RenderType: "",
						Value:      "30",
						Status:     "",
						Tip:        "",
					},
				}, {
					Usage: Distribution{
						RenderType: "",
						Value:      "10",
						Status:     "",
						Tip:        "",
					},
				}},
				sortColumn: "Usage",
				asc:        false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			SortByDistribution(tt.args.data, tt.args.sortColumn, tt.args.asc)
		})
	}
}
func TestSortByDistributionRate(t *testing.T) {
	type args struct {
		data       []RowItem
		sortColumn string
		asc        bool
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "testDistribution",
			args: args{
				data: []RowItem{{
					DistributionRate: DistributionRate{
						RenderType:        "",
						DistributionValue: 1,
					},
				}, {
					DistributionRate: DistributionRate{
						RenderType:        "",
						DistributionValue: 2,
					},
				}, {
					DistributionRate: DistributionRate{
						RenderType:        "",
						DistributionValue: 3,
					},
				}},
				sortColumn: "DistributionRate",
				asc:        true,
			},
		},
		{
			name: "testUsage",
			args: args{
				data: []RowItem{{
					DistributionRate: DistributionRate{
						RenderType:        "",
						DistributionValue: 1,
					},
				}, {
					DistributionRate: DistributionRate{
						RenderType:        "",
						DistributionValue: 2,
					},
				}, {
					DistributionRate: DistributionRate{
						RenderType:        "",
						DistributionValue: 3,
					},
				}},
				sortColumn: "DistributionRate",
				asc:        false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			SortByDistributionRate(tt.args.data, tt.args.sortColumn, tt.args.asc)
		})
	}
}

func TestSortByString(t *testing.T) {
	type args struct {
		data       []RowItem
		sortColumn string
		asc        bool
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "testRole",
			args: args{
				data: []RowItem{{
					Role: Role{Value: RoleValue{Label: "worker"}},
				}, {
					Role: Role{Value: RoleValue{Label: "lb"}},
				}, {
					Role: Role{Value: RoleValue{Label: "master"}},
				}},
				sortColumn: "Role",
				asc:        false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			SortByString(tt.args.data, tt.args.sortColumn, tt.args.asc)
		})
	}
}

func TestSortByStatus(t *testing.T) {
	type args struct {
		data       []RowItem
		sortColumn string
		asc        bool
	}
	tests := []struct {
		name string
		args args
	}{{
		name: "testStatus",
		args: args{
			data: []RowItem{{
				Status: SteveStatus{
					Value: "Ready",
				},
			}, {
				Status: SteveStatus{
					Value: "Ready",
				},
			}, {
				Status: SteveStatus{
					Value: "error",
				},
			}},
			sortColumn: "Status",
			asc:        false,
		},
	},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			SortByStatus(tt.args.data, tt.args.sortColumn, tt.args.asc)
		})
	}
}

func TestTable_GetScaleValue(t1 *testing.T) {
	type args struct {
		a            float64
		b            float64
		resourceType TableType
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test",
			args: args{
				a:            1024,
				b:            102400000,
				resourceType: Memory,
			},
			want: "1.0 KiB/100000.0 KiB",
		},
		{
			name: "test1",
			args: args{
				a:            1024,
				b:            1024,
				resourceType: Memory,
			},
			want: "1.0 KiB/1.0 KiB",
		},
		{
			name: "test2",
			args: args{
				a:            2047,
				b:            1024,
				resourceType: Memory,
			},
			want: "2.0 KiB/1.0 KiB",
		},
		{
			name: "test3",
			args: args{
				a:            1024,
				b:            1024,
				resourceType: Cpu,
			},
			want: "1.024/1.024",
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Table{}
			if got := t.GetScaleValue(tt.args.a, tt.args.b, tt.args.resourceType); got != tt.want {
				t1.Errorf("GetScaleValue() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTable_GetUnusedRate(t1 *testing.T) {
	type fields struct {
	}
	type args struct {
		a, b         float64
		resourceType TableType
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   DistributionRate
	}{
		// TODO: Add test cases.
		{
			name:   "text",
			fields: fields{},
			args: args{
				a:            1.1,
				b:            1.2,
				resourceType: Memory,
			},
			want: DistributionRate{
				RenderType:        "text",
				Value:             "High",
				DistributionValue: 0.917,
			},
		},
		{
			name:   "text",
			fields: fields{},
			args: args{
				a:            0.8,
				b:            1.2,
				resourceType: Memory,
			},
			want: DistributionRate{
				RenderType:        "text",
				Value:             "Middle",
				DistributionValue: 0.667,
			},
		},
		{
			name:   "text",
			fields: fields{},
			args: args{
				a:            0.3,
				b:            1.2,
				resourceType: Memory,
			},
			want: DistributionRate{
				RenderType:        "text",
				Value:             "Low",
				DistributionValue: 0.25,
			},
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Table{}
			t.SDK = &cptype.SDK{
				Tran: NopTranslator{},
			}
			if got := t.GetDistributionRate(tt.args.a, tt.args.b, tt.args.resourceType); !reflect.DeepEqual(got, tt.want) {
				t1.Errorf("GetDistributionRate() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTable_GetDistributionValue(t1 *testing.T) {
	type fields struct {
		TableComponent GetRowItem
		SDK            *cptype.SDK
		Ctx            context.Context
		Server         cmp.SteveServer
		Type           string
		Props          map[string]interface{}
		Operations     map[string]interface{}
		State          State
	}
	type args struct {
		a, b         float64
		resourceType TableType
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   DistributionValue
	}{
		{
			name: "1",
			args: args{
				a:            2,
				b:            3,
				resourceType: Pod,
			},
			want: DistributionValue{
				Text:    "2/3",
				Percent: "66.7",
			},
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Table{}
			if got := t.GetDistributionValue(tt.args.a, tt.args.b, tt.args.resourceType); !reflect.DeepEqual(got, tt.want) {
				t1.Errorf("GetDistributionValue() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTable_GetUsageValue(t1 *testing.T) {
	type fields struct {
		TableComponent GetRowItem
		SDK            *cptype.SDK
		Ctx            context.Context
		Server         cmp.SteveServer
		Type           string
		Props          map[string]interface{}
		Operations     map[string]interface{}
		State          State
	}
	type args struct {
		a, b         float64
		resourceType TableType
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   DistributionValue
	}{
		{
			name: "1",
			args: args{
				a:            1,
				b:            3,
				resourceType: Pod,
			},
			want: DistributionValue{
				Text:    "1/3",
				Percent: "33.3",
			},
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Table{}
			if got := t.GetUsageValue(tt.args.a, tt.args.b, tt.args.resourceType); !reflect.DeepEqual(got, tt.want) {
				t1.Errorf("GetUsageValue() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTable_DecodeURLQuery(t1 *testing.T) {
	tests := []struct {
		name    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "2",
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &Table{
				SDK: &cptype.SDK{
					InParams: map[string]interface{}{
						"table__urlQuery": "eyJzb3J0ZXJEYXRhIjp7ImZpZWxkIjoiRGlzdHJpYnV0aW9uIiwib3JkZXIiOiJhc2NlbmQifX0=",
					},
				},
			}
			if err := t.DecodeURLQuery(); (err != nil) != tt.wantErr {
				t1.Errorf("DecodeURLQuery() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestTable_EncodeURLQuery(t1 *testing.T) {
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
			t := &Table{}
			if err := t.EncodeURLQuery(); (err != nil) != tt.wantErr {
				t1.Errorf("EncodeURLQuery() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestSortByRole(t *testing.T) {
	type args struct {
		data   []RowItem
		in1    string
		ascend bool
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{
				data: []RowItem{{
					Role: Role{
						Value: RoleValue{Label: "1"},
					},
				}, {
					Role: Role{
						Value: RoleValue{Label: "2"},
					},
				}, {
					Role: Role{
						Value: RoleValue{Label: "3"},
					},
				}},
				in1:    "",
				ascend: false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			SortByRole(tt.args.data, tt.args.in1, tt.args.ascend)
		})
	}
}
