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

package mimetype

import (
	"testing"
)

func TestTypeByFilename(t *testing.T) {
	tests := []struct {
		name     string
		filePath string
		want     string
	}{
		{
			name:     "xml",
			filePath: "test.xml",
			want:     "application/xml",
		},
		{
			name:     "json",
			filePath: "test.json",
			want:     "application/json",
		},
		{
			name:     "atom",
			filePath: "test.atom",
			want:     "application/atom+xml",
		},
		{
			name:     "path",
			filePath: "dir/test.json",
			want:     "application/json",
		},
		{
			name:     "unknown",
			filePath: "test.unknown",
			want:     "",
		},
	}
	//
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := TypeByFilename(tt.filePath); got != tt.want {
				t.Errorf("TypeByFilename(%q) = %v, want %v", tt.filePath, got, tt.want)
			}
		})
	}
}
