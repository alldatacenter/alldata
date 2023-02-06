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

package errors

import (
	"fmt"
	"reflect"
	"testing"
)

func TestParseValidateError(t *testing.T) {
	type args struct {
		err error
	}
	tests := []struct {
		name string
		args args
		want error
	}{
		{
			name: "test nil",
			args: args{
				err: nil,
			},
			want: nil,
		},
		{
			name: "test missing parameter",
			args: args{
				err: fmt.Errorf(`invalid field %s: value '%v' must not be an empty string`, "field1.field2", ""),
			},
			want: NewMissingParameterError("field1.field2"),
		},
		{
			name: "test missing parameter",
			args: args{
				err: fmt.Errorf(`invalid field %s: value '%v' must be greater than '0'`, "field1", "-1"),
			},
			want: NewInvalidParameterError("field1", fmt.Sprintf(`value '%v' must be greater than '0'`, "-1")),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			if err := ParseValidateError(tt.args.err); !reflect.DeepEqual((interface{})(err), (interface{})(tt.want)) {
				t.Errorf("ParseValidateError() got %v, want %v", err, tt.want)
			}
		})
	}
}
