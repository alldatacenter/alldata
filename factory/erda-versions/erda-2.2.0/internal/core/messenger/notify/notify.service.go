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

package notify

import (
	"context"
	"encoding/json"
	"strconv"
	"strings"
	"time"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/common"
	"github.com/erda-project/erda/internal/core/messenger/notify/db"
	"github.com/erda-project/erda/internal/core/messenger/notify/model"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type notifyService struct {
	DB      *db.DB
	L       logs.Logger
	bdl     *bundle.Bundle
	Monitor monitor.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService" optional:"true"`
}

func (n notifyService) CreateNotifyHistory(ctx context.Context, request *pb.CreateNotifyHistoryRequest) (*pb.CreateNotifyHistoryResponse, error) {
	result := &pb.CreateNotifyHistoryResponse{}
	var historyId int64
	var err error
	if request.NotifyTags != nil {
		historyId, err = n.CreateHistoryAndIndex(request)
		if err != nil {
			return result, errors.NewInternalServerError(err)
		}
	} else {
		dbReq, err := ToDBNotifyHistory(request)
		if err != nil {
			return result, errors.NewInternalServerError(err)
		}
		history, err := n.DB.NotifyHistoryDB.CreateNotifyHistory(dbReq)
		if err != nil {
			return result, errors.NewInternalServerError(err)
		}
		historyId = history.ID
	}
	result.Data = historyId
	return result, nil
}

func ToDBNotifyHistory(request *pb.CreateNotifyHistoryRequest) (*db.NotifyHistory, error) {
	targetData, err := json.Marshal(request.NotifyTargets)
	if err != nil {
		return nil, err
	}
	sourceData, err := json.Marshal(request.NotifySource)
	if err != nil {
		return nil, err
	}
	history := &db.NotifyHistory{
		NotifyName:            request.NotifyName,
		NotifyItemDisplayName: request.NotifyItemDisplayName,
		Channel:               request.Channel,
		TargetData:            string(targetData),
		SourceData:            string(sourceData),
		Status:                request.Status,
		OrgID:                 request.OrgID,
		Label:                 request.Label,
		ClusterName:           request.ClusterName,
		SourceType:            request.NotifySource.SourceType,
		SourceID:              request.NotifySource.SourceID,
		ErrorMsg:              request.ErrorMsg,
	}
	return history, nil
}

func (n notifyService) CreateHistoryAndIndex(request *pb.CreateNotifyHistoryRequest) (historyId int64, err error) {
	tx := n.DB.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			n.L.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	dbReq, err := ToDBNotifyHistory(request)
	if err != nil {
		return 0, errors.NewInternalServerError(err)
	}
	history, err := tx.NotifyHistoryDB.CreateNotifyHistory(dbReq)
	if err != nil {
		return 0, err
	}
	attributes, err := json.Marshal(request.NotifyTags)
	if err != nil {
		return 0, err
	}
	alertId := int64(request.NotifyTags["alertId"].GetNumberValue())
	alertNotifyIndex := &db.AlertNotifyIndex{
		NotifyID:   history.ID,
		NotifyName: request.NotifyItemDisplayName,
		Status:     request.Status,
		Channel:    request.Channel,
		Attributes: string(attributes),
		CreatedAt:  time.Now(),
		SendTime:   history.CreatedAt,
		ScopeType:  request.NotifySource.SourceType,
		ScopeID:    request.NotifySource.SourceID,
		OrgID:      request.OrgID,
		AlertId:    alertId,
	}
	_, err = tx.AlertNotifyIndexDB.CreateAlertNotifyIndex(alertNotifyIndex)
	if err != nil {
		return 0, err
	}
	return history.ID, nil
}

func (n notifyService) QueryNotifyHistories(ctx context.Context, request *pb.QueryNotifyHistoriesRequest) (*pb.QueryNotifyHistoriesResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.Atoi(orgIdStr)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	request.OrgID = int64(orgId)
	queryReq := &model.QueryNotifyHistoriesRequest{}
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, queryReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	list, count, err := n.DB.NotifyHistoryDB.QueryNotifyHistories(queryReq)
	if err != nil {
		return &pb.QueryNotifyHistoriesResponse{}, nil
	}
	result := &pb.QueryNotifyHistoriesResponse{
		Data: &pb.QueryNotifyHistoryData{
			List: []*pb.NotifyHistory{},
		},
	}
	for _, notifyHistory := range list {
		notify, err := notifyHistory.ToApiData()
		if err != nil {
			return result, nil
		}
		result.Data.List = append(result.Data.List, notify)
	}
	result.Data.Total = count
	return result, nil
}

func (n notifyService) GetNotifyStatus(ctx context.Context, request *pb.GetNotifyStatusRequest) (*pb.GetNotifyStatusResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	result := &pb.GetNotifyStatusResponse{
		Data: make(map[string]int64),
	}
	orgId, err := strconv.Atoi(orgIdStr)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	filterStatus := &model.FilterStatusRequest{
		OrgId:     orgId,
		ScopeType: request.ScopeType,
		ScopeId:   request.ScopeId,
		StartTime: request.StartTime,
		EndTime:   request.EndTime,
	}
	filterStatusResult, err := n.DB.NotifyHistoryDB.FilterStatus(filterStatus)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	for _, v := range filterStatusResult {
		result.Data[v.Status] = v.Count
	}
	return result, nil
}

func (n notifyService) GetNotifyHistogram(ctx context.Context, request *pb.GetNotifyHistogramRequest) (*pb.GetNotifyHistogramResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.Atoi(orgIdStr)
	result := &pb.GetNotifyHistogramResponse{
		Data: &pb.NotifyHistogramData{
			Timestamp: make([]int64, 0),
			Value:     make(map[string]*pb.StatisticValue),
		},
	}
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	//(endTime-startTime)/points算出interval
	startTime, err := strconv.ParseInt(request.StartTime, 10, 64)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	endTime, err := strconv.ParseInt(request.EndTime, 10, 64)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	interval := (endTime - startTime) / request.Points
	//和最小的interval进行比较
	if interval < common.Interval {
		interval = common.Interval
		request.Points = (endTime - startTime) / interval
	}
	valueMap := map[string]*pb.StatisticValue{}
	rs, err := n.DB.NotifyHistoryDB.QueryNotifyValue(request.Statistic, orgId, request.ScopeId, request.ScopeType, interval, startTime, endTime)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	for i := 0; int64(i) < request.Points; i++ {
		result.Data.Timestamp = append(result.Data.Timestamp, startTime)
		startTime = startTime + interval
	}
	for _, v := range rs {
		_, ok := valueMap[v.Field]
		if !ok {
			valueMap[v.Field] = &pb.StatisticValue{
				Value: make([]int64, request.Points),
			}
		}
		var i int64
		timeUnix := v.RoundTime.UnixNano() / 1e6
		for i < request.Points {
			if timeUnix <= result.Data.Timestamp[i] {
				valueMap[v.Field].Value[i] = v.Count
				break
			}
			i++
		}
	}
	result.Data.Value = valueMap
	return result, nil
}

func (n notifyService) QueryAlertNotifyHistories(ctx context.Context, request *pb.QueryAlertNotifyHistoriesRequest) (*pb.QueryAlertNotifyHistoriesResponse, error) {
	result := &pb.QueryAlertNotifyHistoriesResponse{
		Data: &pb.AlertNotifyHistories{},
	}
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.Atoi(orgIdStr)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	queryRequest := &model.QueryAlertNotifyIndexRequest{}
	data, err := json.Marshal(request)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, queryRequest)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	queryRequest.OrgID = int64(orgId)
	list, count, err := n.DB.AlertNotifyIndexDB.QueryAlertNotifyHistories(queryRequest)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	result.Data.Total = count
	result.Data.List = make([]*pb.AlertNotifyIndex, 0)
	data, err = json.Marshal(list)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, &result.Data.List)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	return result, nil
}

func (n notifyService) GetAlertNotifyDetail(ctx context.Context, request *pb.GetAlertNotifyDetailRequest) (*pb.GetAlertNotifyDetailResponse, error) {
	result := &pb.GetAlertNotifyDetailResponse{
		Data: &pb.AlertNotifyDetail{},
	}
	alertNotifyIndex, err := n.DB.AlertNotifyIndexDB.GetAlertNotifyIndex(request.Id)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	attributes := model.AlertIndexAttribute{}
	err = json.Unmarshal([]byte(alertNotifyIndex.Attributes), &attributes)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	result.Data.Channel = alertNotifyIndex.Channel
	result.Data.SendTime = timestamppb.New(alertNotifyIndex.SendTime)
	result.Data.Status = alertNotifyIndex.Status
	result.Data.NotifyRule = attributes.AlertName
	result.Data.NotifyGroup = strconv.Itoa(int(attributes.GroupID))
	alertNotifyHistory, err := n.DB.NotifyHistoryDB.GetAlertNotifyHistory(alertNotifyIndex.NotifyID)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	str := strings.TrimLeft(alertNotifyHistory.NotifyName, "【")
	str = strings.TrimRight(str, "】\n")
	result.Data.AlertName = str
	sourceDataParam := model.NotifySourceData{}
	err = json.Unmarshal([]byte(alertNotifyHistory.SourceData), &sourceDataParam)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	result.Data.NotifyContent = sourceDataParam.Params.Content
	return result, nil
}

func (n notifyService) GetTypeNotifyHistogram(ctx context.Context, request *pb.GetTypeNotifyHistogramRequest) (*pb.GetTypeNotifyHistogramResponse, error) {
	result := &pb.GetTypeNotifyHistogramResponse{
		Data: &pb.TypeNotifyHistogram{
			Value: make(map[string]*pb.StatisticValue),
		},
	}
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.Atoi(orgIdStr)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	startTime, err := strconv.ParseInt(request.StartTime, 10, 64)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	endTime, err := strconv.ParseInt(request.EndTime, 10, 64)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	rs, err := n.DB.NotifyHistoryDB.NotifyHistoryType(request.Statistic, orgId, request.ScopeId, request.ScopeType, startTime, endTime)
	if err != nil {
		return result, errors.NewInternalServerError(err)
	}
	for _, item := range rs {
		result.Data.Value[item.Field] = &pb.StatisticValue{Value: []int64{item.Count}}
	}
	return result, nil
}
