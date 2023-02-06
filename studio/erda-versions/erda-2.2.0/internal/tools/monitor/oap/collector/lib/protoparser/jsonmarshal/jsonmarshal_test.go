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

package jsonmarshal

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/common/unmarshalwork"
)

func TestMain(m *testing.M) {
	unmarshalwork.Start()
	m.Run()
	unmarshalwork.Stop()
}

func TestParseInterface(t *testing.T) {
	type args struct {
		src      interface{}
		callback func(buf []byte) error
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			args: args{
				src: map[string]string{"hello": "world"},
				callback: func(buf []byte) error {
					assert.Equal(t, []byte(`{"hello":"world"}`), buf)
					return nil
				},
			},
			wantErr: false,
		},
		{
			args: args{
				src: map[string]string{"hello": "world"},
				callback: func(buf []byte) error {
					return fmt.Errorf("err")
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := ParseInterface(tt.args.src, tt.args.callback); (err != nil) != tt.wantErr {
				t.Errorf("ParseInterface() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
