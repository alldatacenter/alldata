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

package common

import (
	"context"
	"reflect"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

func TestFilterValues_StringSlice(t *testing.T) {
	type args struct {
		key string
	}
	tests := []struct {
		name string
		v    FilterValues
		args args
		want []string
	}{
		{
			name: "bad1",
			v:    FilterValues{"k1": ""},
			args: args{key: "k1"},
			want: nil,
		},
		{
			name: "bad2",
			v:    FilterValues{"k1": struct{ A string }{A: "a"}},
			args: args{key: "k1"},
			want: nil,
		},
		{
			name: "ok1",
			v:    FilterValues{"k1": []string{"1", "2", "3"}},
			args: args{key: "k1"},
			want: []string{"1", "2", "3"},
		},
		{
			name: "ok2",
			v:    FilterValues{"k1": []interface{}{1, "2", 3.0}},
			args: args{key: "k1"},
			want: []string{"1", "2", "3"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.v.StringSlice(tt.args.key); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("StringSlice() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetFilterBase64(t *testing.T) {
	type args struct {
		ctx context.Context
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "f1",
			args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
				InParams: cptype.InParams{
					"filter__urlQuery": "123",
				},
			})},
			want: "123",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetFilterBase64(tt.args.ctx); got != tt.want {
				t.Errorf("GetFilterBase64() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetValues(t *testing.T) {
	type args struct {
		filterBase64 string
	}
	tests := []struct {
		name    string
		args    args
		want    map[string]interface{}
		wantErr bool
	}{
		{
			name:    "map",
			args:    args{filterBase64: "eyJrMSI6InYxIn0="},
			want:    map[string]interface{}{"k1": "v1"},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetValues(tt.args.filterBase64)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetValues() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetValues() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestInitPageDataAccount(t *testing.T) {
	type args struct {
		ctx context.Context
	}
	tests := []struct {
		name    string
		args    args
		want    *PageDataAccount
		wantErr bool
	}{
		{name: "err", args: args{ctx: context.Background()}, want: nil, wantErr: true},
		{name: "err2", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
			InParams: nil,
		})}, want: nil, wantErr: true},
		{name: "err3", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
			InParams: cptype.InParams{
				"account": "",
			},
		})}, want: nil, wantErr: true},
		{name: "have", args: args{ctx: context.WithValue(context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
			InParams: cptype.InParams{
				"projectId":  "24",
				"instanceId": "1111",
			},
		}), cptype.GlobalInnerKeyStateTemp, map[string]interface{}{})}, want: &PageDataAccount{
			ProjectID:  24,
			InstanceID: "1111",
		}, wantErr: false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := InitPageDataAccount(tt.args.ctx)
			if (err != nil) != tt.wantErr {
				t.Errorf("InitPageDataAccount() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("InitPageDataAccount() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestInitPageDataAttachment(t *testing.T) {
	type args struct {
		ctx context.Context
	}
	tests := []struct {
		name    string
		args    args
		want    *PageDataAttachment
		wantErr bool
	}{
		{name: "err", args: args{ctx: context.Background()}, want: nil, wantErr: true},
		{name: "err2", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
			InParams: nil,
		})}, want: nil, wantErr: true},
		{name: "err3", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
			InParams: cptype.InParams{
				"account": "",
			},
		})}, want: nil, wantErr: true},
		{name: "have", args: args{ctx: context.WithValue(context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{
			InParams: cptype.InParams{
				"projectId":  "24",
				"instanceId": "1111",
			},
		}), cptype.GlobalInnerKeyStateTemp, map[string]interface{}{})}, want: &PageDataAttachment{
			ProjectID:  24,
			InstanceID: "1111",
		}, wantErr: false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := InitPageDataAttachment(tt.args.ctx)
			if (err != nil) != tt.wantErr {
				t.Errorf("InitPageDataAttachment() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("InitPageDataAttachment() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestLoadPageDataAccount(t *testing.T) {
	type args struct {
		ctx context.Context
	}
	tests := []struct {
		name string
		args args
		want *PageDataAccount
	}{
		{name: "empty", args: args{ctx: nil}, want: nil},
		{name: "empty2", args: args{ctx: context.Background()}, want: nil},
		{name: "empty3", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyStateTemp, map[string]interface{}{})}, want: nil},
		{name: "have", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyStateTemp,
			map[string]interface{}{
				"pageDataAccount": &PageDataAccount{AccountID: "1"},
			})}, want: &PageDataAccount{AccountID: "1"}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := LoadPageDataAccount(tt.args.ctx); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("LoadPageDataAccount() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestLoadPageDataAttachment(t *testing.T) {
	type args struct {
		ctx context.Context
	}

	tests := []struct {
		name string
		args args
		want *PageDataAttachment
	}{
		{name: "empty", args: args{ctx: nil}, want: nil},
		{name: "empty2", args: args{ctx: context.Background()}, want: nil},
		{name: "empty3", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyStateTemp, map[string]interface{}{})}, want: nil},
		{name: "have", args: args{ctx: context.WithValue(context.Background(), cptype.GlobalInnerKeyStateTemp,
			map[string]interface{}{
				"pageDataAttachment": &PageDataAttachment{AttachmentID: "1"},
			})}, want: &PageDataAttachment{AttachmentID: "1"}},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := LoadPageDataAttachment(tt.args.ctx); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("LoadPageDataAttachment() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestToBase64(t *testing.T) {
	type args struct {
		values interface{}
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name:    "map",
			args:    args{map[string]string{"k1": "v1"}},
			want:    "eyJrMSI6InYxIn0=",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := ToBase64(tt.args.values)
			if (err != nil) != tt.wantErr {
				t.Errorf("ToBase64() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("ToBase64() got = %v, want %v", got, tt.want)
			}
		})
	}
}
