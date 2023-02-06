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
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

// HostUniqueLabelFilter Process HOST_UNIQUE in Pass1ScheduleInfo.label
func HostUniqueLabelFilter(
	r *labelconfig.RawLabelRuleResult, r2 *labelconfig.RawLabelRuleResult2, li *labelconfig.LabelInfo) {
	hostUniqueStr, ok := li.Label[labelconfig.HOST_UNIQUE]
	if !ok {
		return
	}
	var hostUniqueGroup [][]string
	if err := json.Unmarshal([]byte(hostUniqueStr), &hostUniqueGroup); err != nil {
		logrus.Errorf("bad input label: %v, err: %v", labelconfig.HOST_UNIQUE, err)
		return
	}
	r.HostUnique = true
	r.HostUniqueInfo = hostUniqueGroup

	r2.HasHostUnique = true
	r2.HostUnique = hostUniqueGroup
}
