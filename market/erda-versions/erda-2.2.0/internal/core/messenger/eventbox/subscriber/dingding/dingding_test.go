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

package dingding

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_DingPrint(t *testing.T) {
	var contentArr []rune

	maxContentSize = 10

	for i := 0; i < maxContentSize; i++ {
		contentArr = append(contentArr, 'A')
	}
	content := string(contentArr)

	t.Log("content count", len(content))

	test := []struct {
		name    string
		want    string
		t       string
		isError bool
	}{
		{
			name:    "no_over",
			want:    content,
			t:       content,
			isError: true,
		},
		{
			name:    "over",
			want:    content[:7] + overflowText,
			t:       content + "1",
			isError: true,
		},
	}

	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			got := DingPrint(tt.t)
			require.Equal(t, got, tt.want)
		})
	}
}
