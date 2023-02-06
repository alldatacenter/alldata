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

package extension

import (
	"fmt"
	"io/ioutil"
	"os"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"
)

func TestAddSyncExtension(t *testing.T) {
	var s = &extensionService{}
	file := NewFileExtensionSource(s)
	RegisterExtensionSource(file)

	patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(s), "InitExtension", func(s *extensionService, addr string, forceUpdate bool) error {
		return fmt.Errorf("test")
	})
	defer patch1.Unpatch()

	dir, err := ioutil.TempDir(os.TempDir(), "*")
	assert.NoError(t, err)

	err = AddSyncExtension(dir)
	assert.Error(t, err)
}

func TestFileExtensionSource_add(t *testing.T) {

	var s = &extensionService{}
	file := NewFileExtensionSource(s)

	patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(s), "InitExtension", func(s *extensionService, addr string, forceUpdate bool) error {
		return nil
	})
	defer patch1.Unpatch()

	dir, err := ioutil.TempDir(os.TempDir(), "*")
	assert.NoError(t, err)

	err = file.add(dir)
	assert.NoError(t, err)
}

func Test_isDir(t *testing.T) {
	dir, err := ioutil.TempDir(os.TempDir(), "*")
	assert.NoError(t, err)
	got := isDir(dir)
	assert.True(t, got)

	file, err := ioutil.TempFile(os.TempDir(), "*")
	assert.NoError(t, err)
	got = isDir(file.Name())
	assert.True(t, !got)
}

func TestGitExtensionSource_match(t *testing.T) {
	type args struct {
		addr string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "test_http",
			args: args{
				addr: "http://test.git",
			},
			want: true,
		},
		{
			name: "test_https",
			args: args{
				addr: "http://test.git",
			},
			want: true,
		},
		{
			name: "test_git",
			args: args{
				addr: "git@github.com:erda-project/erda.git",
			},
			want: true,
		},
		{
			name: "test_file",
			args: args{
				addr: "/test/aaa",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := &GitExtensionSource{}
			if got := g.match(tt.args.addr); got != tt.want {
				t.Errorf("match() = %v, want %v", got, tt.want)
			}
		})
	}
}
