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

	"github.com/stretchr/testify/assert"
)

func TestURLQueryString(t *testing.T) {
	req := TestReportRecordListRequest{
		Name:          "test-report",
		ProjectID:     1,
		IterationIDS:  []uint64{1, 2},
		GetReportData: true,
		OrderBy:       "createdAt",
		Asc:           true,
		PageSize:      1,
		PageNo:        20,
	}
	urlQuery := req.URLQueryString()
	assert.Equal(t, 2, len(urlQuery["iterationIDS"]))
	assert.Equal(t, []string{"true"}, urlQuery["asc"])
	assert.Equal(t, []string{"test-report"}, urlQuery["name"])
	assert.Equal(t, []string{"1"}, urlQuery["projectID"])
	assert.Equal(t, []string{"createdAt"}, urlQuery["orderBy"])
}

func TestGetQualityScore(t *testing.T) {
	scoreRecore := TestReportData{
		TestDashboard: &ComponentProtocolRequest{
			Protocol: &ComponentProtocol{
				GlobalState: &GlobalStateData{
					"GlobalQualityScore": 100.01,
				},
			},
		},
	}
	emptyScore := TestReportData{}
	assert.Equal(t, float64(100.01), scoreRecore.GetQualityScore())
	assert.Equal(t, float64(0), emptyScore.GetQualityScore())
}
