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
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

// LocationLabelFilter LabelInfo.Selectors
func LocationLabelFilter(
	r *labelconfig.RawLabelRuleResult, r2 *labelconfig.RawLabelRuleResult2, li *labelconfig.LabelInfo) {
	if r.Location == nil {
		r.Location = make(map[string]interface{})
	}
	if r2.Location == nil {
		r2.Location = make(map[string]interface{})
	}
	for service, selectors := range li.Selectors {
		selector, ok := selectors["location"]
		if !ok {
			continue
		}
		r.Location[service] = selector
		r2.Location[service] = selector
	}
}
