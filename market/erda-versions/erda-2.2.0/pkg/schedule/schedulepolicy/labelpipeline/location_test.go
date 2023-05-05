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

package labelpipeline

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

func TestLocationLabelFilter(t *testing.T) {
	r := labelconfig.RawLabelRuleResult{}
	r2 := labelconfig.RawLabelRuleResult2{}
	LocationLabelFilter(&r, &r2, &labelconfig.LabelInfo{
		Selectors: map[string]diceyml.Selectors{
			"servicename": {"location": diceyml.Selector{Values: []string{"xxx", "yyy"}}},
		},
	})
	assert.Equal(t, map[string]interface{}{"servicename": diceyml.Selector{Values: []string{"xxx", "yyy"}}}, r.Location)
}
