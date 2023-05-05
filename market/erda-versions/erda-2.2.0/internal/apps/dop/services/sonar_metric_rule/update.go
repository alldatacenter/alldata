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

package sonar_metric_rule

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (svc *Service) Update(req apistructs.SonarMetricRulesUpdateRequest) (httpserver.Responser, error) {

	dbRule, err := svc.db.GetSonarMetricRules(req.ID)
	if err != nil {
		return nil, err
	}
	if dbRule.ScopeType != req.ScopeType {
		return nil, fmt.Errorf("not find this sonarMetricRule")
	}
	if dbRule.ScopeID != req.ScopeID {
		return nil, fmt.Errorf("not find this sonarMetricRule")
	}
	dbRule.MetricValue = req.MetricValue
	dbRule.Description = req.Description

	if err := checkAndTruncatedMetricValue(dbRule); err != nil {
		return nil, err
	}
	if err := svc.db.UpdateSonarMetricRules(dbRule); err != nil {
		return nil, err
	}

	return httpserver.OkResp(dbRule)
}
