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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// Create 创建测试集
func (svc *Service) BatchInsert(req *apistructs.SonarMetricRulesBatchInsertRequest) (httpserver.Responser, error) {
	var rules []*dao.QASonarMetricRules
	if req.Metrics == nil || len(req.Metrics) <= 0 {
		return nil, fmt.Errorf("insert metrics is empry")
	}
	for _, metric := range req.Metrics {
		insertRule := &dao.QASonarMetricRules{
			ScopeID:     req.ScopeID,
			ScopeType:   req.ScopeType,
			MetricKeyID: metric.MetricKeyID,
			MetricValue: metric.MetricValue,
			Description: metric.Description,
		}
		if err := checkAndTruncatedMetricValue(insertRule); err != nil {
			return nil, err
		}
		rules = append(rules, insertRule)
	}

	if err := svc.db.BatchInsertSonarMetricRules(rules); err != nil {
		return nil, err
	}

	return httpserver.OkResp(rules)
}

// 校验 key 和 Operational 是否符合规范
func checkAndTruncatedMetricValue(rule *dao.QASonarMetricRules) error {
	metricKey := apistructs.SonarMetricKeys[rule.MetricKeyID]
	if metricKey == nil {
		return fmt.Errorf("not find this metricKey %d", rule.MetricKeyID)
	}

	switch metricKey.ValueType {
	case "WORK_DUR":
		_, err := strconv.Atoi(rule.MetricValue)
		if err != nil {
			return fmt.Errorf("%s type value can not be %s, write value like int", metricKey.MetricKeyDesc, rule.MetricValue)
		}
	case "RATING":
		if rule.MetricValue != "1" && rule.MetricValue != "2" && rule.MetricValue != "3" && rule.MetricValue != "4" {
			return fmt.Errorf("%s type value can not be %s, write value like A", metricKey.MetricKeyDesc, rule.MetricValue)
		}
	case "PERCENT":
		value, err := strconv.ParseFloat(rule.MetricValue, 64)
		if err != nil {
			return fmt.Errorf("%s type value can not be %s, write value like float", metricKey.MetricKeyDesc, rule.MetricValue)
		}
		if metricKey.DecimalScale > 0 {
			rule.MetricValue = fmt.Sprintf("%."+strconv.Itoa(metricKey.DecimalScale)+"f", value)
		}
	case "MILLISEC":
		_, err := strconv.Atoi(rule.MetricValue)
		if err != nil {
			return fmt.Errorf("%s type value can not be %s, write value like int", metricKey.MetricKeyDesc, rule.MetricValue)
		}
	case "INT":
		_, err := strconv.Atoi(rule.MetricValue)
		if err != nil {
			return fmt.Errorf("%s type value can not be %s, write value like int", metricKey.MetricKeyDesc, rule.MetricValue)
		}
	case "FLOAT":
		_, err := strconv.ParseFloat(rule.MetricValue, 64)
		if err != nil {
			return fmt.Errorf("%s type value can not be %s, write value like float", metricKey.MetricKeyDesc, rule.MetricValue)
		}
	}

	return nil
}
