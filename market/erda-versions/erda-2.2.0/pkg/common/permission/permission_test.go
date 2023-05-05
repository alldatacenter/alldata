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

package permission

import (
	"context"
	"reflect"
	"testing"
)

func TestFieldValue(t *testing.T) {
	type args struct {
		field string
		req   interface{}
	}

	tests := []struct {
		name      string
		args      args
		want      string
		wantError bool
	}{
		{
			name: "struct",
			args: args{
				req: &struct {
					Field1 string
				}{Field1: "fieldval1"},
				field: "Field1",
			},
			want: "fieldval1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			getter := FieldValue(tt.args.field)
			val, err := getter(context.Background(), tt.args.req)
			if err != nil {
				if !tt.wantError {
					t.Errorf("FieldValue(%q)(req) got error: %s", tt.args.field, err)
				}
			} else if err == nil {
				if tt.wantError {
					t.Errorf("FieldValue(%q)(req) successfully, want error", tt.args.field)
				} else if val != tt.want {
					t.Errorf("FieldValue(%q)(req) = %v, want %v", tt.args.field, val, tt.want)
				}
			}
		})
	}
}

type testInterface interface {
	TestFunc_1()
	函数_1()
}

func Test_getMethodName(t *testing.T) {
	type args struct {
		method interface{}
	}
	tests := []struct {
		name      string
		args      args
		want      string
		wantError bool
	}{
		{
			name: "test empty",
			args: args{},
			want: "",
		},
		{
			name: "test string",
			args: args{
				method: "hello",
			},
			want: "hello",
		},
		{
			name: "interafce method",
			args: args{
				method: testInterface.TestFunc_1,
			},
			want: "TestFunc_1",
		},
		{
			name: "interafce Chinese method name",
			args: args{
				method: testInterface.函数_1,
			},
			want: "函数_1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			defer func() {
				err := recover()
				if err == nil && tt.wantError {
					t.Errorf("getMethodName() successfully, want error")
				} else if err != nil && !tt.wantError {
					t.Errorf("getMethodName() got error: %s", err)
				}
			}()
			if got := getMethodName(tt.args.method); got != tt.want {
				t.Errorf("getMethodName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestSetPermissionDataFromContext(t *testing.T) {
	type args struct {
		key   string
		value interface{}
	}
	tests := []struct {
		name string
		args args
	}{
		{
			args: args{
				key:   "test-key",
				value: "test-value",
			},
		},
		{
			args: args{
				key:   "test-key-2",
				value: 1024,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctx := WithPermissionDataContext(context.Background())
			SetPermissionDataFromContext(ctx, tt.args.key, tt.args.value)

			value, ok := GetPermissionDataFromContext(ctx, tt.args.key+"not-exit")
			if ok {
				t.Errorf("GetPermissionDataFromContext(ctx, %q) got %v,%v , want not fount", tt.args.key+"not-exit", value, ok)
				return
			}

			value, ok = GetPermissionDataFromContext(ctx, tt.args.key)
			if !ok {
				t.Errorf("GetPermissionDataFromContext(ctx, %q) not fount value, want %v", tt.args.key, tt.args.value)
				return
			}
			if !reflect.DeepEqual(value, tt.args.value) {
				t.Errorf("GetPermissionDataFromContext(ctx, %q) got %v, want %v", tt.args.key, value, tt.args.value)
				return
			}
		})
	}
}
