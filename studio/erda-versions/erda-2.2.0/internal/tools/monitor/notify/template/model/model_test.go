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

package model

import "testing"

func TestValidateString(t *testing.T) {
	type args struct {
		field string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test_ValidateString",
			args: args{},
			want: "this field not be empty",
		},
		{
			name: "test_ValidateString",
			args: args{
				field: "ddfdf",
			},
			want: "",
		},
		{
			name: "test_ValidateString",
			args: args{
				field: "      ",
			},
			want: `this field can't just contain spaces`,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ValidateString(tt.args.field); got != tt.want {
				t.Errorf("ValidateString() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestValidateInt(t *testing.T) {
	type args struct {
		field int64
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test_ValidateInt",
			args: args{
				34,
			},
			want: "",
		},
		{
			name: "test_ValidateInt",
			args: args{
				0,
			},
			want: "this field must not be zero",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ValidateInt(tt.args.field); got != tt.want {
				t.Errorf("ValidateInt() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestCheckElements(t *testing.T) {
	type args struct {
		formats  []map[string]string
		title    []string
		template []string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test_CheckElements",
			args: args{
				formats: []map[string]string{
					{
						"template": "notify template",
					},
				},
				title:    []string{"template title"},
				template: []string{"this is notify template"},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := CheckElements(tt.args.formats, tt.args.title, tt.args.template); (err != nil) != tt.wantErr {
				t.Errorf("CheckElements() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
