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

package errorbox

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func (s *ErrorBoxService) List(param *apistructs.TaskErrorListRequest) ([]model.ErrorLog, error) {
	if param.StartTime != "" {
		startTime, err := param.GetFormartStartTime()
		if err != nil {
			return nil, err
		}
		return s.db.ListErrorLogByResourcesAndStartTime(param.ResourceTypes, param.ResourceIDS, *startTime)
	}

	return s.db.ListErrorLogByResources(param.ResourceTypes, param.ResourceIDS)
}
