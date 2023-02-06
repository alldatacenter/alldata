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

package file_manager

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda-proto-go/core/services/filemanager/pb"
)

func Test_parseFileList(t *testing.T) {
	tests := []struct {
		name    string
		text    string
		want    *pb.FileDirectory
		wantErr bool
	}{
		{
			text: `/root
total 208
drwxr-xr-x   1 root root   4096 2021-12-14T16:55:23.218360893 .
drwxr-xr-x   1 root root   4096 2021-12-14T16:55:23.218360893 ..
-rw-r--r--   1 root root  12123 2019-10-01T09:16:32.000000000 anaconda-post.log
drwxr-xr-x   3 root root   4096 2021-09-23T14:46:27.000000000 app
-rw-r--r--   1 root root 137382 2021-09-23T14:45:23.000000000 arthas-boot.jar
lrwxrwxrwx   1 root root      7 2019-10-01T09:15:19.000000000 bin -> usr/bin`,
			want: &pb.FileDirectory{
				Directory: "/root",
				Files: []*pb.FileInfo{
					{
						Name:      ".",
						Mode:      "drwxr-xr-x",
						Size:      4096,
						HardLinks: 1,
						ModTime:   1639472123218,
						User:      "root",
						UserGroup: "root",
						IsDir:     true,
					},
					{
						Name:      "..",
						Mode:      "drwxr-xr-x",
						Size:      4096,
						HardLinks: 1,
						ModTime:   1639472123218,
						User:      "root",
						UserGroup: "root",
						IsDir:     true,
					},
					{
						Name:      "anaconda-post.log",
						Mode:      "-rw-r--r--",
						Size:      12123,
						HardLinks: 1,
						ModTime:   1569892592000,
						User:      "root",
						UserGroup: "root",
					},
					{
						Name:      "app",
						Mode:      "drwxr-xr-x",
						Size:      4096,
						HardLinks: 3,
						ModTime:   1632379587000,
						User:      "root",
						UserGroup: "root",
						IsDir:     true,
					},
					{
						Name:      "arthas-boot.jar",
						Mode:      "-rw-r--r--",
						Size:      137382,
						HardLinks: 1,
						ModTime:   1632379523000,
						User:      "root",
						UserGroup: "root",
					},
					{
						Name:      "bin",
						Mode:      "lrwxrwxrwx",
						Size:      7,
						HardLinks: 1,
						ModTime:   1569892519000,
						User:      "root",
						UserGroup: "root",
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := parseFileList(tt.text)
			if (err != nil) != tt.wantErr {
				t.Errorf("parseFileList() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("parseFileList() = %v, want %v", got, tt.want)
			}
		})
	}
}
