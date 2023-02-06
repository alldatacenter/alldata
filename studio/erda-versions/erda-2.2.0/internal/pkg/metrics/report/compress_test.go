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

package report

import (
	"bytes"
	"io"
	"testing"
)

func TestCompressWithGzip(t *testing.T) {
	type args struct {
		data io.Reader
	}
	r := bytes.NewReader([]byte{97, 98, 99, 100})
	tests := []struct {
		name    string
		args    args
		want    io.Reader
		wantErr bool
	}{
		{
			name: "test_CompressWithGzip",
			args: args{
				data: r,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := CompressWithGzip(tt.args.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("CompressWithGzip() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			//if !reflect.DeepEqual(got, tt.want) {
			//	t.Errorf("CompressWithGzip() got = %v, want %v", got, tt.want)
			//}
		})
	}
}
