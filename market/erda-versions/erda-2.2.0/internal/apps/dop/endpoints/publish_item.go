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

package endpoints

import (
	"context"
	"net/http"
	"net/url"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// GetPublishItemCertificationlist 获取publishItem认证列表
func (e *Endpoints) GetPublishItemCertificationlist(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	currentTime := time.Now()
	queryReq := apistructs.PublishItemCertificationListRequest{
		PageSize:  uint64(getInt(r.URL, "pageSize", 20)),
		PageNo:    uint64(getInt(r.URL, "pageNo", 1)),
		StartTime: uint64(getInt64(r.URL, "start", currentTime.AddDate(0, -1, 0).UnixNano()/1e6)),
		EndTime:   uint64(getInt64(r.URL, "end", time.Now().UnixNano()/1e6)),
	}
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.GetCertificationlist(&queryReq, mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetStatisticsTrend 获取统计大盘，整体趋势
func (e *Endpoints) GetStatisticsTrend(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.GetStatisticsTrend(mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetStatisticsVersionInfo 获取版本详情，明细数据
func (e *Endpoints) GetStatisticsVersionInfo(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	endTimestamp := r.URL.Query().Get("endTime")
	if endTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("endTime").ToResp(), nil
	}
	end, err := strconv.ParseInt(endTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	endTime := time.Unix(end/1000, 0)
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.GetStatisticsVersionInfo(endTime, mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// CumulativeUsers .
func (e *Endpoints) CumulativeUsers(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	startTimestamp := r.URL.Query().Get("start")
	if startTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("start").ToResp(), nil
	}
	start, err := strconv.ParseInt(startTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	startTime := time.Unix(start/1000, 0)
	endTimestamp := r.URL.Query().Get("end")
	if endTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("end").ToResp(), nil
	}
	end, err := strconv.ParseInt(endTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	endTime := time.Unix(end/1000, 0)

	pointsStr := r.URL.Query().Get("points")
	if pointsStr == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("points").ToResp(), nil
	}
	points, err := strconv.ParseUint(pointsStr, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter("points").ToResp(), nil
	}
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.CumulativeUsers(points, startTime, endTime, mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetStatisticsChannelInfo 获取渠道详情，明细数据
func (e *Endpoints) GetStatisticsChannelInfo(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	endTimestamp := r.URL.Query().Get("endTime")
	if endTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("endTime").ToResp(), nil
	}
	end, err := strconv.ParseInt(endTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	endTime := time.Unix(end/1000, 0)
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.GetStatisticsChannelInfo(endTime, mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetErrTrend 获取错误报告，错误趋势
func (e *Endpoints) GetErrTrend(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.GetErrTrend(mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetErrList 获取错误报告，错误趋势
func (e *Endpoints) GetErrList(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	startTimestamp := r.URL.Query().Get("start")
	if startTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("start").ToResp(), nil
	}
	start, err := strconv.ParseInt(startTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	startTime := time.Unix(start/1000, 0)
	endTimestamp := r.URL.Query().Get("end")
	if endTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("end").ToResp(), nil
	}
	end, err := strconv.ParseInt(endTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	endTime := time.Unix(end/1000, 0)

	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter(err).ToResp(), nil
	}

	artifact, err := e.publishItem.GetErrList(startTime, endTime, r.URL.Query().Get("filter_av"), mk)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// MetricsRouting 获取渠道详情，明细数据
func (e *Endpoints) MetricsRouting(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter(err).ToResp(), nil
	}
	metricsName := vars["metricName"]
	params := r.URL.Query()
	params["filter_tk"] = []string{mk.AK}
	params["filter_ai"] = []string{mk.AI}
	resultData, err := e.bdl.MetricsRouting(r.RequestURI, metricsName, params)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(resultData)
}

// GetErrAffectUserRate .
func (e *Endpoints) GetErrAffectUserRate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	startTimestamp := r.URL.Query().Get("start")
	if startTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("start").ToResp(), nil
	}
	start, err := strconv.ParseInt(startTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	startTime := time.Unix(start/1000, 0)
	endTimestamp := r.URL.Query().Get("end")
	if endTimestamp == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("end").ToResp(), nil
	}
	end, err := strconv.ParseInt(endTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InternalError(err).ToResp(), nil
	}
	endTime := time.Unix(end/1000, 0)

	pointsStr := r.URL.Query().Get("points")
	if pointsStr == "" {
		return apierrors.ErrSratisticsErrList.MissingParameter("pointsStr").ToResp(), nil
	}
	points, _ := strconv.ParseUint(pointsStr, 10, 64)
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrSratisticsErrList.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.EffactUsersRate(points, startTime, endTime, r.URL.Query().Get("filter_av"), mk)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetCrashRate 崩溃率
func (e *Endpoints) GetCrashRate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	startTimestamp := r.URL.Query().Get("start")
	if startTimestamp == "" {
		return apierrors.ErrCrashRateList.MissingParameter("start").ToResp(), nil
	}
	start, err := strconv.ParseInt(startTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrCrashRateList.InternalError(err).ToResp(), nil
	}
	startTime := time.Unix(start/1000, 0)
	endTimestamp := r.URL.Query().Get("end")
	if endTimestamp == "" {
		return apierrors.ErrCrashRateList.MissingParameter("end").ToResp(), nil
	}
	end, err := strconv.ParseInt(endTimestamp, 10, 64)
	if err != nil {
		return apierrors.ErrCrashRateList.InternalError(err).ToResp(), nil
	}
	endTime := time.Unix(end/1000, 0)

	pointsStr := r.URL.Query().Get("points")
	if pointsStr == "" {
		return apierrors.ErrCrashRateList.MissingParameter("pointsStr").ToResp(), nil
	}
	points, err := strconv.ParseUint(pointsStr, 10, 64)
	if err != nil {
		return apierrors.ErrCrashRateList.InvalidParameter("pointsStr").ToResp(), nil
	}
	mk, err := getMonitorKeys(r.URL)
	if err != nil {
		return apierrors.ErrCrashRateList.InvalidParameter(err).ToResp(), nil
	}
	artifact, err := e.publishItem.CrashRate(points, startTime, endTime, r.URL.Query().Get("filter_av"), mk)
	if err != nil {
		return apierrors.ErrCrashRateList.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

func getMonitorKeys(url *url.URL) (*apistructs.MonitorKeys, error) {
	ak, ai := url.Query().Get("ak"), url.Query().Get("ai")
	if ak == "" || ai == "" {
		return nil, errors.New("nil ak or ai")
	}

	return &apistructs.MonitorKeys{
		AK: ak,
		AI: ai,
	}, nil
}

func getInt64(url *url.URL, key string, defaultValue int64) int64 {
	valueStr := url.Query().Get(key)
	value, err := strconv.ParseInt(valueStr, 10, 64)
	if err != nil {
		logrus.Errorf("get int err: %+v", err)
		return defaultValue
	}
	return value
}
