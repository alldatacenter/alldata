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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (svc *Service) QueryMetricKeys(req *apistructs.SonarMetricRulesListRequest) (httpserver.Responser, error) {

	rule := dao.QASonarMetricRules{
		ScopeID:   req.ScopeID,
		ScopeType: req.ScopeType,
	}

	dbRules, err := svc.db.ListSonarMetricRules(&rule)
	if err != nil {
		return nil, err
	}

	rule = dao.QASonarMetricRules{
		ScopeID:   "-1",
		ScopeType: "platform",
	}
	defaultDbRules, err := svc.db.ListSonarMetricRules(&rule)
	if err != nil {
		return nil, err
	}

	// 假如没有对应的值，就设置默认值
	if dbRules == nil || len(dbRules) <= 0 {
		dbRules = append(dbRules, defaultDbRules...)
	} else {
		// 有对应的值，然后不是 default 的值的话就设置上 default
		for _, defaultRules := range defaultDbRules {
			find := false
			for _, v := range dbRules {
				if v.MetricKeyID == defaultRules.MetricKeyID {
					find = true
					break
				}
			}
			if !find {
				dbRules = append(dbRules, defaultRules)
			}
		}
	}

	var results []*apistructs.SonarMetricKey
	for _, rule := range dbRules {
		key := apistructs.SonarMetricKeys[rule.MetricKeyID]
		results = append(results, &apistructs.SonarMetricKey{
			MetricKey:   key.MetricKey,
			Operational: getOperational(key.Operational),
			MetricValue: rule.MetricValue,
		})
	}

	return httpserver.OkResp(results)
}

// 查询 list 的时候根据实际 operational 转换成 gt 和 lt，前端看到的是 > < 但是给 sonar 服务器看的是 gt lt，数据库存储的是 -1 1
func getOperational(operational string) string {
	if operational == "-1" || operational == ">" {
		return "GT"
	} else if operational == "1" || operational == "<" {
		return "LT"
	}
	return ""
}
