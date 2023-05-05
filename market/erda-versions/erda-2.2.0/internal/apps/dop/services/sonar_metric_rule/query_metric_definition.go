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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/i18n"
)

func (svc *Service) QueryMetricDefinition(req *apistructs.SonarMetricRulesDefinitionListRequest, local *i18n.LocaleResource) (httpserver.Responser, error) {

	rule := dao.QASonarMetricRules{
		ScopeID:   req.ScopeID,
		ScopeType: req.ScopeType,
	}

	// 获取已经新增的 key
	var metricKeysQuery = func(engine *gorm.DB) *gorm.DB {
		var searchMetricKey []int64
		for k := range apistructs.SonarMetricKeys {
			searchMetricKey = append(searchMetricKey, k)
		}
		engine.Where("metric_key_id in (?)", searchMetricKey)
		return engine
	}
	dbRules, err := svc.db.ListSonarMetricRules(&rule, metricKeysQuery)
	if err != nil {
		return nil, err
	}

	// 过滤出用户没有新增的 key
	var sonarMetricKeys []*apistructs.SonarMetricKey
	for key := range apistructs.SonarMetricKeys {
		var find = false
		for _, dbRule := range dbRules {
			if dbRule.MetricKeyID == key {
				find = true
				break
			}
		}
		if !find {
			sonarMetricKeys = append(sonarMetricKeys, apistructs.SonarMetricKeys[key])
		}
	}

	for _, v := range sonarMetricKeys {
		v.FormatValue()

		// 国际化
		localDesc := getMetricKeyLocal(v.MetricKey, local)
		if localDesc != "" {
			v.MetricKeyDesc = localDesc
		}

	}

	return httpserver.OkResp(sonarMetricKeys)
}
