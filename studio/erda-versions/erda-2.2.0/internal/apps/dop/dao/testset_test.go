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

package dao

import (
	"math/rand"
	"testing"
	"time"
)

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandStringRunes(n int) string {
	rand.Seed(time.Now().UnixNano())
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[rand.Intn(len(letterRunes))]
	}
	return string(b)
}

func TestValidateTestSetDirectoryLength(t *testing.T) {
	type args struct {
		dir string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "too long directory",
			args: args{
				dir: RandStringRunes(maxTestSetDirectoryLength + 1),
			},
			wantErr: true,
		},
		{
			name: "directory with exact length",
			args: args{
				dir: RandStringRunes(maxTestSetDirectoryLength),
			},
			wantErr: false,
		},
		{
			name: "small directory",
			args: args{
				dir: RandStringRunes(30),
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := ValidateTestSetDirectoryLength(tt.args.dir); (err != nil) != tt.wantErr {
				t.Errorf("ValidateTestSetDirectoryLength() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
