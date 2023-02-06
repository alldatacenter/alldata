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

package log_service

import "C"
import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda-proto-go/msp/apm/log-service/pb"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type logService struct {
	p               *provider
	logDeploymentDB *db.LogDeploymentDB
	logInstanceDB   *db.LogInstanceDB
	startTime       int64
}

func (s *logService) HistogramAggregation(ctx context.Context, req *pb.HistogramAggregationRequest) (*pb.HistogramAggregationResponse, error) {
	monitorResult, err := s.HistogramAggregationFromMonitor(ctx, req)
	if err != nil {
		return nil, err
	}
	loghubResult, err := s.HistogramAggregationFromLoghub(ctx, req)
	if err != nil {
		return nil, err
	}
	return s.mergeHistogramAggregationResponse(monitorResult, loghubResult), nil
}

func (s *logService) BucketAggregation(ctx context.Context, req *pb.BucketAggregationRequest) (*pb.BucketAggregationResponse, error) {
	monitorResult, err := s.TermsAggregationFromMonitor(ctx, req)
	if err != nil {
		return nil, err
	}
	loghubResult, err := s.TermsAggregationFromLoghub(ctx, req)
	if err != nil {
		return nil, err
	}
	return s.mergeTermAggregationResponse(monitorResult, loghubResult), nil
}

func (s *logService) PagedSearch(ctx context.Context, req *pb.PagedSearchRequest) (*pb.PagedSearchResponse, error) {
	ascendingOrder := StringList(req.Sort).All(func(item string) bool { return strings.HasSuffix(item, " asc") })
	monitorResult, err := s.PagedSearchFromMonitor(ctx, req)
	if err != nil {
		return nil, err
	}
	loghubResult, err := s.PagedSearchFromLoghub(ctx, req)
	if err != nil {
		return nil, err
	}
	return s.mergePagedSearchResponse(monitorResult, loghubResult, ascendingOrder, 0), nil
}

func (s *logService) SequentialSearch(ctx context.Context, req *pb.SequentialSearchRequest) (*pb.SequentialSearchResponse, error) {
	ascendingOrder := strings.ToLower(req.Sort) == "asc"
	count := int(req.Count)
	monitorResult, err := s.SequentialSearchFromMonitor(ctx, req)
	if err != nil {
		return nil, err
	}
	loghubResult, err := s.SequentialSearchFromLoghub(ctx, req)
	if err != nil {
		return nil, err
	}
	return s.mergeSequentialSearchResponse(monitorResult, loghubResult, ascendingOrder, count), nil
}

func (s *logService) GetFieldSettings(ctx context.Context, req *pb.GetFieldSettingsRequest) (*pb.GetFieldSettingsResponse, error) {
	defaultFields := s.listDefaultFields()
	return &pb.GetFieldSettingsResponse{
		Data: defaultFields,
	}, nil
}

func (s *logService) listDefaultFields() []*pb.LogField {
	var list []*pb.LogField
	if len(s.p.Cfg.IndexFieldSettings.DefaultSettings.Fields) == 0 {
		return list
	}
	for _, field := range s.p.Cfg.IndexFieldSettings.DefaultSettings.Fields {
		list = append(list, &pb.LogField{
			FieldName:          field.FieldName,
			SupportAggregation: field.SupportAggregation,
			Display:            field.Display,
			Group:              field.Group,
			AllowEdit:          field.AllowEdit,
		})
	}
	return list
}

func (s *logService) getRequestOrgIDOrDefault(ctx context.Context) int64 {
	str := apis.GetOrgID(ctx)
	id, _ := strconv.ParseInt(str, 10, 64)
	return id
}

func (s *logService) getLogKeys(logKey string) (LogKeyGroup, error) {
	instance, err := s.logInstanceDB.GetLatestByLogKey(logKey, "")
	if err != nil {
		return nil, err
	}
	if instance == nil {
		return nil, fmt.Errorf("no logInstance found")
	}
	instances, err := s.logInstanceDB.GetListByClusterAndProjectIdAndWorkspace(instance.ClusterName, instance.ProjectId, instance.Workspace)
	if err != nil {
		return nil, err
	}
	ids := LogKeys{}
	for _, logInstance := range instances {
		key := logInstance.LogKey
		if logInstance.LogType == string(db.LogTypeLogService) {
			var instanceConfig = struct {
				MspEnvID string `json:"MSP_ENV_ID"`
			}{}

			json.Unmarshal([]byte(logInstance.Config), &instanceConfig)
			key = instanceConfig.MspEnvID
			ids.Add(key, logServiceKey)
		} else if logInstance.Version == "1.0.0" {
			ids.Add(key, logAnalysisV1Key)
		} else {
			ids.Add(key, logAnalysisV2Key)
		}
	}

	if len(ids) == 0 {
		return nil, fmt.Errorf("no storage located")
	}
	return ids.Group(), nil
}
