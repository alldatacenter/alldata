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

package expression

import (
	"context"
	"encoding/json"
	"io/fs"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda-proto-go/core/monitor/expression/pb"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	alertdb "github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/internal/tools/monitor/core/expression/model"
)

const (
	Org          = "org"
	MicroService = "micro_service"
)

var (
	Expressions      []*model.Expression
	MetricExpression []*pb.Expression
	TemplateIndex    map[string][]*model.NotifyTemplate
	ExpressionIndex  map[string]*model.Expression
	AlertConfig      map[string]*model.AlertConfig
	Templates        []*model.NotifyTemplate

	OrgAlertType          []string
	MicroServiceAlertType []string
)

type expressionService struct {
	alertDB                        *alertdb.AlertExpressionDB
	metricDB                       *alertdb.MetricExpressionDB
	customizeAlertNotifyTemplateDB *alertdb.CustomizeAlertNotifyTemplateDB
	alertNotifyDB                  *alertdb.AlertNotifyDB
	clientDB                       *dao.DBClient
}

func (e *expressionService) GetOrgsLocale(ctx context.Context, request *pb.GetOrgsLocaleRequest) (*pb.GetOrgsLocaleResponse, error) {
	list, err := e.clientDB.GetOrgList()
	if err != nil {
		return nil, err
	}
	orgLocale := make(map[string]string)
	for _, v := range list {
		if v.Locale == "" || (v.Locale != model.ZHLange && v.Locale != model.ENLange) {
			v.Locale = "zh-CN"
		}
		orgLocale[v.Name] = v.Locale
	}
	return &pb.GetOrgsLocaleResponse{
		Data: orgLocale,
	}, nil
}

func (e *expressionService) init(alertRules, metricRules string) error {
	err := e.readAlertRule(alertRules)
	if err != nil {
		return err
	}
	err = e.readMetricRule(metricRules)
	if err != nil {
		return err
	}
	e.getAlertType()
	return nil
}

func (e *expressionService) getAlertType() {
	OrgAlertType = make([]string, 0)
	MicroServiceAlertType = make([]string, 0)
	OrgAlertTypeMap := make(map[string]bool)
	MicroServiceAlertTypeMap := make(map[string]bool)
	for _, v := range AlertConfig {
		if v.AlertScope == Org {
			OrgAlertTypeMap[v.AlertType] = true
		} else {
			MicroServiceAlertTypeMap[v.AlertType] = true
		}
	}
	for k := range OrgAlertTypeMap {
		OrgAlertType = append(OrgAlertType, k)
	}
	for k := range MicroServiceAlertTypeMap {
		MicroServiceAlertType = append(MicroServiceAlertType, k)
	}
}

func (e *expressionService) readMetricRule(root string) error {
	MetricExpression = make([]*pb.Expression, 0)
	f, err := os.ReadDir(root)
	if err != nil {
		return err
	}
	for _, pkg := range f {
		err := filepath.Walk(filepath.Join(root, pkg.Name()), func(path string, info fs.FileInfo, err error) error {
			if err != nil {
				return nil
			}
			if info.IsDir() {
				dir, err := os.ReadDir(path)
				if err != nil {
					return err
				}
				f = append(f, dir...)
				return nil
			}
			f, err := ioutil.ReadFile(path)
			expression := &pb.Expression{}
			err = json.Unmarshal(f, expression)
			if err != nil {
				return err
			}
			expression.Enable = true
			MetricExpression = append(MetricExpression, expression)
			return nil
		})
		if err != nil {
			return err
		}
	}
	return nil
}

func (e *expressionService) readAlertRule(root string) error {
	Expressions = make([]*model.Expression, 0)
	TemplateIndex = make(map[string][]*model.NotifyTemplate)
	ExpressionIndex = make(map[string]*model.Expression)
	AlertConfig = make(map[string]*model.AlertConfig)
	Templates = make([]*model.NotifyTemplate, 0)
	f, err := os.ReadDir(root)
	if err != nil {
		return err
	}
	for _, pkg := range f {
		err := filepath.Walk(filepath.Join(root, pkg.Name()), func(path string, info fs.FileInfo, err error) error {
			if err != nil {
				return nil
			}
			if info.IsDir() {
				dir, err := os.ReadDir(path)
				if err != nil {
					return err
				}
				f = append(f, dir...)
				return nil
			}
			f, err := ioutil.ReadFile(path)
			allRoute := strings.Split(path, "/")
			alertIndex := allRoute[len(allRoute)-2]
			alertType := allRoute[len(allRoute)-3]
			if info.Name() == model.NOTIFY_TEMPLATE {
				templates := make([]*model.NotifyTemplate, 0)
				err = yaml.Unmarshal(f, &templates)
				if err != nil {
					return err
				}
				for _, v := range templates {
					v.AlertIndex = alertIndex
					v.AlertType = alertType
					//(*v).AlertIndex = alertIndex
					//(*v).AlertType = alertType
				}
				TemplateIndex[alertIndex] = templates
				Templates = append(Templates, templates...)
			}
			if info.Name() == model.ALERT_RULE {
				alertConfig := &model.AlertConfig{}
				err = yaml.Unmarshal(f, alertConfig)
				if err != nil {
					return err
				}
				alertConfig.AlertType = alertType
				AlertConfig[alertIndex] = alertConfig
			}
			if info.Name() == model.ANALYZER_EXPRESSION {
				expression := &model.Expression{}
				err = json.Unmarshal(f, expression)
				if err != nil {
					return err
				}
				Expressions = append(Expressions, expression)
				ExpressionIndex[alertIndex] = expression
			}
			return nil
		})
		if err != nil {
			return err
		}
	}
	return nil
}

func (e *expressionService) GetAlertExpressions(ctx context.Context, request *pb.GetExpressionsRequest) (*pb.GetExpressionsResponse, error) {
	alertExpressions, count, err := e.alertDB.GetAllAlertExpression(request.PageNo, request.PageSize)
	if err != nil {
		return nil, err
	}
	data, err := json.Marshal(alertExpressions)
	if err != nil {
		return nil, err
	}
	alertExpressionArr := make([]*pb.Expression, 0)
	err = json.Unmarshal(data, &alertExpressionArr)
	if err != nil {
		return nil, err
	}
	return &pb.GetExpressionsResponse{
		Data: &pb.ExpressionData{
			List:  alertExpressionArr,
			Total: count,
		},
	}, nil
}

func (e *expressionService) GetMetricExpressions(ctx context.Context, request *pb.GetMetricExpressionsRequest) (*pb.GetMetricExpressionsResponse, error) {
	result := &pb.GetMetricExpressionsResponse{
		Data: &pb.ExpressionData{
			Total: int64(len(MetricExpression)),
		},
	}
	metricLength := int64(len(MetricExpression))
	from, end := e.MemoryPage(request.PageNo, request.PageSize, metricLength)
	if from == -1 || end == -1 {
		return result, nil
	}
	result.Data.List = MetricExpression[from:end]
	return result, nil
}

func (e *expressionService) MemoryPage(pageNo, pageSize, length int64) (int64, int64) {
	if pageNo < 1 {
		pageNo = 1
	}
	if pageSize <= 0 {
		pageSize = 100
	}
	if pageSize > length {
		pageSize = length
	}
	from, end := (pageNo-1)*pageSize, pageNo*pageSize
	if from > length {
		return -1, -1
	}
	if end > length {
		end = length
	}
	return from, end
}

func (e *expressionService) GetAlertNotifies(ctx context.Context, request *pb.GetAlertNotifiesRequest) (*pb.GetAlertNotifiesResponse, error) {
	alertNotifies, count, err := e.alertNotifyDB.QueryAlertNotify(request.PageNo, request.PageSize)
	if err != nil {
		return nil, err
	}
	data, err := json.Marshal(alertNotifies)
	if err != nil {
		return nil, err
	}
	alertNotifyArr := make([]*pb.AlertNotify, 0)
	err = json.Unmarshal(data, &alertNotifyArr)
	if err != nil {
		return nil, err
	}
	return &pb.GetAlertNotifiesResponse{
		Data: &pb.AlertNotifyData{
			List:  alertNotifyArr,
			Total: count,
		},
	}, nil
}

func (e *expressionService) GetTemplates(ctx context.Context, request *pb.GetTemplatesRequest) (*pb.GetTemplatesResponse, error) {
	customizeTemplate, err := e.customizeAlertNotifyTemplateDB.QueryCustomizeAlertTemplate()
	if err != nil {
		return nil, err
	}
	data, err := json.Marshal(customizeTemplate)
	if err != nil {
		return nil, err
	}
	customizeTemplates := make([]*model.NotifyTemplate, 0)
	err = json.Unmarshal(data, &customizeTemplates)
	alertAndCustomTemplate := make([]*model.NotifyTemplate, 0)
	alertAndCustomTemplate = append(alertAndCustomTemplate, customizeTemplates...)
	alertAndCustomTemplate = append(alertAndCustomTemplate, Templates...)
	result := &pb.GetTemplatesResponse{
		Data: &pb.AlertTemplateData{
			List:  make([]*pb.AlertTemplate, 0),
			Total: int64(len(alertAndCustomTemplate)),
		},
	}
	templateLength := int64(len(alertAndCustomTemplate))
	from, end := e.MemoryPage(request.PageNo, request.PageSize, templateLength)
	if from == -1 || end == -1 {
		return result, nil
	}
	data, err = json.Marshal(alertAndCustomTemplate[from:end])
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(data, &result.Data.List)
	if err != nil {
		return nil, err
	}
	return result, nil
}
