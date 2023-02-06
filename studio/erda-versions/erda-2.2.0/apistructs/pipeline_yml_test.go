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

package apistructs

import (
	"testing"

	"github.com/bmizerany/assert"
)

func TestSnippetConfigToString(t *testing.T) {
	var table = []struct {
		snippetConfig     SnippetConfig
		sameSnippetConfig SnippetConfig
	}{
		{
			snippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
				Labels: map[string]string{
					"key1": "key",
					"key2": "key",
					"key3": "key",
				},
			},
			sameSnippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
				Labels: map[string]string{
					"key2": "key",
					"key1": "key",
					"key3": "key",
				},
			},
		},
		{
			snippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
				Labels: map[string]string{
					"key1": "key",
					"key2": "key",
					"key3": "key",
				},
			},
			sameSnippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
				Labels: map[string]string{
					"key1": "key",
					"key2": "key",
					"key3": "key",
				},
			},
		},
		{
			snippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
				Labels: map[string]string{},
			},
			sameSnippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
				Labels: map[string]string{},
			},
		},
		{
			snippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
			},
			sameSnippetConfig: SnippetConfig{
				Source: "autotest",
				Name:   "custom",
			},
		},
	}

	for _, data := range table {
		assert.Equal(t, data.snippetConfig.ToString(), data.sameSnippetConfig.ToString())
	}
}
