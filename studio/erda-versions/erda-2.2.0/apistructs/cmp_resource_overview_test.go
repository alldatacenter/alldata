//  Copyright (c) 2021 Terminus, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package apistructs_test

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestResourceOverviewReportData_GroupByOwner(t *testing.T) {
	var data = new(apistructs.ResourceOverviewReportData)
	data.List = []*apistructs.ResourceOverviewReportDataItem{
		{
			ProjectID:          0,
			ProjectName:        "0",
			ProjectDisplayName: "0",
			ProjectDesc:        "0",
			OwnerUserID:        0,
			OwnerUserName:      "0",
			OwnerUserNickName:  "0",
			CPUQuota:           100,
			CPURequest:         50,
			CPUWaterLevel:      0,
			MemQuota:           1.024,
			MemRequest:         0.512,
			MemWaterLevel:      0,
			Nodes:              0,
		}, {
			ProjectID:          1,
			ProjectName:        "1",
			ProjectDisplayName: "1",
			ProjectDesc:        "1",
			OwnerUserID:        1,
			OwnerUserName:      "1",
			OwnerUserNickName:  "1",
			CPUQuota:           100,
			CPURequest:         50,
			CPUWaterLevel:      0,
			MemQuota:           1.024,
			MemRequest:         0.512,
			MemWaterLevel:      0,
			Nodes:              0,
		}, {
			ProjectID:          0,
			ProjectName:        "0",
			ProjectDisplayName: "0",
			ProjectDesc:        "0",
			OwnerUserID:        0,
			OwnerUserName:      "0",
			OwnerUserNickName:  "0",
			CPUQuota:           100,
			CPURequest:         50,
			CPUWaterLevel:      0,
			MemQuota:           1.024,
			MemRequest:         0.512,
			MemWaterLevel:      0,
			Nodes:              0,
		},
	}
	data.GroupByOwner()
	data.Calculates(0, 0)
	data.Calculates(100, 1)
}
