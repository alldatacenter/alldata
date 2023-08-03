//go:build arm64
// +build arm64

/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package system

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetCacheInfo(t *testing.T) {
	tests := []struct {
		name    string
		arg     string
		want    string
		want1   int32
		wantErr bool
	}{
		{
			name:    "empty cache info",
			arg:     "-       ",
			want:    "0",
			want1:   0,
			wantErr: false,
		},
		{
			name:    "empty cache info 1",
			arg:     "-",
			want:    "0",
			want1:   0,
			wantErr: false,
		},
		{
			name:    "valid cache info",
			arg:     "1:1:1:0",
			want:    "1",
			want1:   0,
			wantErr: false,
		},
		{
			name:    "invalid cache info",
			arg:     "1:1:1:xxxx",
			want:    "",
			want1:   0,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		got, got1, gotErr := GetCacheInfo(tt.arg)
		assert.Equal(t, tt.want, got)
		assert.Equal(t, tt.want1, got1)
		assert.Equal(t, tt.wantErr, gotErr != nil)
	}
}
