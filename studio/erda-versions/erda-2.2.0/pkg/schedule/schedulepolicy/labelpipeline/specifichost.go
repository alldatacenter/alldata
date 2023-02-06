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
	"strings"

	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

func SpecificHostLabelFilter(
	r *labelconfig.RawLabelRuleResult, r2 *labelconfig.RawLabelRuleResult2, li *labelconfig.LabelInfo) {
	v, ok := li.Label[labelconfig.SPECIFIC_HOSTS]
	if !ok {
		return
	}
	result := []string{}
	hosts := strings.Split(v, ",")
	for _, host := range hosts {
		trimmedHost := strings.TrimSpace(host)
		if trimmedHost != "" {
			result = append(result, trimmedHost)
		}
	}
	r.SpecificHost = result
	r2.SpecificHost = result
}
