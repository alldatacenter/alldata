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

package issuefilterbm

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_(t *testing.T) {
	mp := MyFilterBmMap{
		"k1": []MyFilterBm{
			{
				ID:           "123",
				Name:         "n123",
				PageKey:      "k1",
				FilterEntity: "e1",
			},
		},
		"k2": []MyFilterBm{
			{
				ID:           "1234",
				Name:         "n1234",
				PageKey:      "k12",
				FilterEntity: "e12",
			},
		},
	}
	assert.Equal(t, []MyFilterBm{
		{
			ID:           "1234",
			Name:         "n1234",
			PageKey:      "k12",
			FilterEntity: "e12",
		},
	}, mp.GetByPageKey("k2"))
}

func Test_GenPageKey(t *testing.T) {
	i := New(WithDBClient(nil))
	assert.Equal(t, "IT-TY", i.GenPageKey("IT", "TY"))
	assert.Equal(t, "TY", i.GenPageKey("", "TY"))
	assert.Equal(t, "IT", i.GenPageKey("IT", ""))
	assert.Equal(t, "", i.GenPageKey("", ""))
}
