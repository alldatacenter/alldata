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

package publish_item

import (
	"encoding/json"
	"fmt"
	"math"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/gogap/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/metrics"
	"github.com/erda-project/erda/internal/pkg/metrics/query"
	"github.com/erda-project/erda/pkg/discover"
)

// GetStatisticsTrend 获取统计大盘，整体趋势
func (i *PublishItem) GetStatisticsTrend(mk *apistructs.MonitorKeys) (*apistructs.PublishItemStatisticsTrendResponse, error) {
	return aggregationTrenddata(mk.AK, mk.AI)
}

func aggregationTrenddata(ak, ai string) (*apistructs.PublishItemStatisticsTrendResponse, error) {
	trend := apistructs.PublishItemStatisticsTrendResponse{}

	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	currentTime := time.Now()
	// 当天0点
	zeroTime := time.Date(currentTime.Year(), currentTime.Month(), currentTime.Day(),
		0, 0, 0, 0, currentTime.Location())
	logrus.Infof("monitor request params 计算七日平均新增用户, zeroTime: %d", zeroTime.Unix())
	// 当天24点
	midnightTime := zeroTime.AddDate(0, 0, 1)
	// 7天前
	sevenDayAgo := zeroTime.AddDate(0, 0, -7)
	// 6天前
	sixDayAgo := zeroTime.AddDate(0, 0, -6)
	// 30天前
	thirtyDayAgo := midnightTime.AddDate(0, 0, -30)
	// 计算七日平均新增用户
	req := query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		SetDiagram("histogram").
		StartFrom(sevenDayAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Apply("cardinality", "fields.firstDayUserId_value").
		Apply("align", "false").
		LimitPoint(8)

	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }
	logrus.Infof("monitor request params 计算七日平均新增用户: %s", req.ConstructParam().Encode())
	resp, err := metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}
	lastSevenDayUsers, lastSevenDayUsersGrowth, err := SevenDayAvg(resp)
	if err != nil {
		return nil, err
	}
	trend.SevenDayAvgNewUsers = lastSevenDayUsers
	trend.SevenDayAvgNewUsersGrowth = lastSevenDayUsersGrowth

	// 计算七日平均活跃用户
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		SetDiagram("histogram").
		StartFrom(sevenDayAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Apply("cardinality", "tags.uid").
		Match("uid", "?*").
		Apply("align", "false").
		LimitPoint(8)

	logrus.Infof("monitor request params 计算七日平均活跃用户: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}
	sevenDayActiveUsers, sevenDayActiveUsersGrowth, err := SevenDayAvg(resp)
	if err != nil {
		return nil, err
	}
	trend.SevenDayAvgActiveUsers = sevenDayActiveUsers
	trend.SevenDayAvgActiveUsersGrowth = sevenDayActiveUsersGrowth

	// 计算七日总活跃用户
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(sixDayAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("uid", "?*").
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 计算七日总活跃用户: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	sevenDayTotalActionUsers, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}

	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(sevenDayAgo).
		EndWith(zeroTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("uid", "?*").
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 计算七日总活跃用户2: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	lastSevenDayTotalActionUsers, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}
	trend.SevenDayTotalActiveUsers = sevenDayTotalActionUsers
	trend.SevenDayTotalActiveUsersGrowth = Growth(float64(lastSevenDayTotalActionUsers), float64(sevenDayTotalActionUsers))

	// 计算30天活跃用户
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(thirtyDayAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("uid", "?*").
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 计算30天活跃用户: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	thirtyDayTotalActionUsers, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}

	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(thirtyDayAgo).
		EndWith(zeroTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("uid", "?*").
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 计算30天活跃用户2: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	lastThirtyDayTotalActionUsers, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}
	trend.MonthTotalActiveUsers = thirtyDayTotalActionUsers
	trend.MonthTotalActiveUsersGrowth = Growth(float64(lastThirtyDayTotalActionUsers), float64(thirtyDayTotalActionUsers))

	// 累计用户数
	twoMonthAgo := thirtyDayAgo.AddDate(0, 0, -30)
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(twoMonthAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("date", "*").
		Match("uid", "?*").
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 累计用户数: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	totalUsers, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}
	trend.TotalUsers = totalUsers

	// 七日平均新用户次日留存率
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		SetDiagram("histogram").
		StartFrom(sevenDayAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("fields.firstDayUserId_value", "?*").
		Apply("cardinality", "fields.firstDayUserId_value").
		Apply("align", "false").
		LimitPoint(8)
	logrus.Infof("monitor request params 七日平均新用户次日留存率: %s", req.ConstructParam().Encode())
	firstDayResp, err := metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}

	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		SetDiagram("histogram").
		StartFrom(sevenDayAgo).
		EndWith(midnightTime).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("fields.secondDayUserId_value", "?*").
		Apply("cardinality", "fields.secondDayUserId_value").
		Apply("align", "false").
		LimitPoint(8)
	logrus.Infof("monitor request params 七日平均新用户次日留存率: %s", req.ConstructParam().Encode())
	secondResp, err := metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}

	sevenDayNewUserRetention, sevenDayNewUserRetentionGrowth, err := SevenDayUserRetension(firstDayResp, secondResp)
	if err != nil {
		return nil, err
	}
	trend.SevenDayAvgNewUsersRetention = sevenDayNewUserRetention
	trend.SevenDayAvgNewUsersRetentionGrowth = sevenDayNewUserRetentionGrowth

	// 总崩溃率
	errReq := query.CreateQueryRequest("ta_error_mobile")
	errReq = errReq.
		StartFrom(twoMonthAgo).
		EndWith(midnightTime).
		Match("error", "*").
		Filter("tk", ak).
		Filter("ai", ai).
		Apply("count", "tags.error")
	logrus.Infof("monitor request params 总崩溃率: %s", errReq.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(errReq)
	if err != nil {
		return nil, err
	}
	totalErrs, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}
	if totalErrs == 0 {
		trend.TotalCrashRate = "0.0%"
	} else {
		req = query.CreateQueryRequest("ta_metric_mobile_metrics")
		req = req.
			StartFrom(twoMonthAgo).
			EndWith(midnightTime).
			Filter("tk", ak).
			Filter("ai", ai).
			Match("date", "*").
			Apply("count", "tags.cid")
		logrus.Infof("monitor request params: %s", req.ConstructParam().Encode())
		resp, err = metrics.Client.QueryMetric(req)
		totalStartUp, err := MetricsTotal(resp)
		if err != nil {
			return nil, err
		}
		trend.TotalCrashRate = fmt.Sprintf("%.2f", (float64(totalErrs)/float64(totalStartUp)*100.0)) + "%"
	}

	// 计算平均使用时长
	documentReq := query.CreateQueryRequest("ta_metric_mobile_document_metrics")
	// 计算今天往前推7天
	documentReq = documentReq.
		StartFrom(sixDayAgo).
		Filter("tk", ak).
		Filter("ai", ai).
		EndWith(midnightTime).Apply("avg", "activeTime")
	logrus.Infof("monitor request params 计算今天往前推7天: %s", documentReq.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(documentReq)
	if err != nil {
		return nil, err
	}
	recentAvgActiveTime, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}
	// 计算昨天往前推7天
	documentReq = query.CreateQueryRequest("ta_metric_mobile_document_metrics")
	documentReq = documentReq.
		StartFrom(sixDayAgo).
		Filter("tk", ak).
		Filter("ai", ai).
		EndWith(zeroTime).Apply("avg", "activeTime")
	logrus.Infof("monitor request params 计算昨天往前推7天: %s", documentReq.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(documentReq)
	if err != nil {
		return nil, err
	}
	lastAvgActiveTime, err := MetricsTotal(resp)
	if err != nil {
		return nil, err
	}
	result := 0.0
	if lastAvgActiveTime > 0 {
		result, err = strconv.ParseFloat(fmt.Sprintf("%.2f", ((float64(recentAvgActiveTime)-float64(lastAvgActiveTime))/float64(lastAvgActiveTime))*100.0), 64)
		if err != nil {
			return nil, err
		}
	} else {
		result = 100
	}

	timeNow := time.Unix(int64(recentAvgActiveTime/1000), 0)
	timeString := timeNow.Format("00:04:05")
	trend.SevenDayAvgDuration = timeString
	trend.SevenDayAvgDurationGrowth = result

	logrus.Infof("trend resp body: %+v", trend)
	return &trend, nil
}

// GetStatisticsVersionInfo 获取版本详情，明细数据
func (i *PublishItem) GetStatisticsVersionInfo(end time.Time, mk *apistructs.MonitorKeys) ([]*apistructs.PublishItemStatisticsDetailResponse, error) {
	return VersionChannelList("av", mk.AK, mk.AI, end)
}

func VersionChannelList(groupKey, ak, ai string, end time.Time) ([]*apistructs.PublishItemStatisticsDetailResponse, error) {
	result := map[string]*apistructs.PublishItemStatisticsDetailResponse{}

	var zeroTime time.Time
	if end.Hour() == 0 && end.Minute() == 0 && end.Second() == 0 && end.Nanosecond() == 0 {
		zeroTime = end.AddDate(0, 0, -1)
	} else {
		currentTime := time.Now()
		zeroTime = time.Date(currentTime.Year(), currentTime.Month(), currentTime.Day(),
			0, 0, 0, 0, currentTime.Location())
	}

	// 30天前
	thirtyDayAgo := end.AddDate(0, 0, -30)

	logrus.Infof("monitor url host: %s", discover.Monitor())
	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }

	// 截止今日版本累计用户
	req := query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(thirtyDayAgo).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("date", "*").
		Match("uid", "?*").
		GroupBy([]string{groupKey}).
		Apply("cardinality", "tags.uid")
	resp, err := metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}
	logrus.Infof("monitor request params 截止今日版本累计用户: %s", req.ConstructParam().Encode())
	respBody, err := parsingResp(resp)
	if err != nil {
		return nil, err
	}
	totalUsers := uint64(0)
	if respBody != nil {
		for _, v := range respBody.Data.Results[0].Data {
			for _, value := range v {
				totalUsers += uint64(value.Data)
				result[value.Tag] = &apistructs.PublishItemStatisticsDetailResponse{
					Key:        value.Tag,
					TotalUsers: uint64(value.Data),
				}
			}
		}
	}

	// 计算今日新增用户
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(zeroTime).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("fields.firstDayUserId_value", "?*").
		GroupBy([]string{groupKey}).
		Apply("cardinality", "fields.firstDayUserId_value")
	resp, err = metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}
	respBody, err = parsingResp(resp)
	if err != nil {
		return nil, err
	}
	if respBody != nil {
		for _, v := range respBody.Data.Results[0].Data {
			for _, value := range v {
				item := result[value.Tag]
				if item == nil {
					item = &apistructs.PublishItemStatisticsDetailResponse{}
				}
				item.NewUsers = uint64(value.Data)
				result[value.Tag] = item
			}
		}
	}

	// 计算活跃用户
	req = query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		StartFrom(zeroTime).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("date", "*").
		Match("uid", "?*").
		GroupBy([]string{groupKey}).
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 计算活跃用户: %s", req.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}
	respBody, err = parsingResp(resp)
	if err != nil {
		return nil, err
	}
	totalActiveUsers := uint64(0)
	if respBody != nil {
		for _, v := range respBody.Data.Results[0].Data {
			for _, value := range v {
				item := result[value.Tag]
				if item == nil {
					item = &apistructs.PublishItemStatisticsDetailResponse{}
				}
				item.ActiveUsers = uint64(value.Data)
				totalActiveUsers += uint64(value.Data)

				result[value.Tag] = item
			}
		}
	}

	// 计算启动次数
	startReq := query.CreateQueryRequest("ta_metric_mobile_metrics")
	startReq = startReq.
		StartFrom(zeroTime).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		GroupBy([]string{groupKey}).
		Match("date", "*").
		Apply("count", "tags.cid")
	logrus.Infof("monitor request params 计算版本或渠道启动次数: %s", startReq.ConstructParam().Encode())
	resp, err = metrics.Client.QueryMetric(startReq)
	if err != nil {
		return nil, err
	}
	startUpRespBody, err := parsingResp(resp)
	if err != nil {
		return nil, err
	}
	if respBody != nil {
		for _, v := range startUpRespBody.Data.Results[0].Data {
			for key, value := range v {
				item := result[value.Tag]
				if item == nil {
					item = &apistructs.PublishItemStatisticsDetailResponse{}
				}
				if strings.Contains(key, "cid") {
					item.Launches = uint64(value.Data)
				}
				result[value.Tag] = item
			}
		}
	}

	// 计算升级用户
	upgradeReq := query.CreateQueryRequest("ta_metric_mobile_metrics")
	upgradeReq = upgradeReq.
		StartFrom(zeroTime).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		Filter("upgrade", "1").
		GroupBy([]string{groupKey}).
		Apply("cardinality", "tags.uid")
	resp, err = metrics.Client.QueryMetric(upgradeReq)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != 200 {
		logrus.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
		return nil, errors.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
	}
	var upgradeRespBody apistructs.PublishItemMetricsCardinalitySingleResp
	if err := json.Unmarshal(resp.Body, &upgradeRespBody); err != nil {
		return nil, err
	}
	if len(upgradeRespBody.Data.Results[0].Data) != 0 {
		for _, v := range upgradeRespBody.Data.Results[0].Data[0] {
			item := result[v.Tag]
			if item == nil {
				item = &apistructs.PublishItemStatisticsDetailResponse{}
			}
			item.UpgradeUser = uint64(v.Data)
			result[v.Tag] = item
		}
	}

	appVersions := make([]string, 0, len(result))
	for k, v := range result {
		appVersions = append(appVersions, k)
		v.Key = k
		if totalActiveUsers > 0 {
			v.ActiveUsersGrowth = fmt.Sprintf("%.2f", (float64(v.ActiveUsers)/float64(totalActiveUsers))*100.0)
		}
		if totalUsers > 0 {
			v.TotalUsersGrowth = fmt.Sprintf("%.2f", (float64(v.TotalUsers)/float64(totalUsers))*100.0)
		}
	}
	sort.Sort(sort.StringSlice(appVersions))

	list := make([]*apistructs.PublishItemStatisticsDetailResponse, 0, len(appVersions))
	for _, v := range appVersions {
		list = append(list, result[v])
	}

	return list, nil
}

// GetStatisticsChannelInfo 获取渠道详情，明细数据
func (i *PublishItem) GetStatisticsChannelInfo(end time.Time, mk *apistructs.MonitorKeys) ([]*apistructs.PublishItemStatisticsDetailResponse, error) {
	return VersionChannelList("ch", mk.AK, mk.AI, end)
}

// GetErrTrend 获取错误报告，错误趋势
func (i *PublishItem) GetErrTrend(mk *apistructs.MonitorKeys) (*apistructs.PublishItemStatisticsErrTrendResponse, error) {
	return ErrTrendInfo(mk.AK, mk.AI)
}

func ErrTrendInfo(ak, ai string) (*apistructs.PublishItemStatisticsErrTrendResponse, error) {
	result := &apistructs.PublishItemStatisticsErrTrendResponse{}
	currentTime := time.Now()
	// 当天0点
	zeroTime := time.Date(currentTime.Year(), currentTime.Month(), currentTime.Day(),
		0, 0, 0, 0, currentTime.Location())
	lastDayTime := zeroTime.AddDate(0, 0, -1)
	// 获取今天的计算指标
	totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err := errInfact(zeroTime, currentTime, ak, ai)
	if err != nil {
		return nil, err
	}
	// 获取昨天的计算指标
	_, yesterdayTotalErrRate, _, yesterdayTotalInfactUsersRate, err := errInfact(lastDayTime, zeroTime, ak, ai)
	if err != nil {
		return nil, err
	}
	rate := 0.0
	if yesterdayTotalErrRate != 0.0 {
		rate, err = strconv.ParseFloat(fmt.Sprintf("%.2f", (totalErrRate-yesterdayTotalErrRate)/yesterdayTotalErrRate*100.0), 64)
		if err != nil {
			return nil, err
		}

	} else {
		rate = 100.0
	}
	affectUsersRate := 0.0
	if yesterdayTotalErrRate != 0.0 {
		affectUsersRate, err = strconv.ParseFloat(fmt.Sprintf("%.2f", (totalInfactUsersRate-yesterdayTotalInfactUsersRate)/yesterdayTotalInfactUsersRate*100.0), 64)
		if err != nil {
			return nil, err
		}
	} else {
		affectUsersRate = 100.0
	}

	result.CrashTimes = totalErrs
	result.CrashRate = fmt.Sprintf("%.2f", totalErrRate) + "%"
	result.CrashRateGrowth = rate
	result.AffectUsers = totalInfactUsers
	result.AffectUsersProportion = fmt.Sprintf("%.2f", totalInfactUsersRate) + "%"
	result.AffectUsersProportionGrowth = affectUsersRate

	return result, nil
}

// GetErrList 获取错误报告，错误列表
func (i *PublishItem) GetErrList(start, end time.Time, av string, mk *apistructs.MonitorKeys) ([]*apistructs.PublishItemStatisticsErrListResponse, error) {
	return errList(mk.AK, mk.AI, av, start, end)
}

func (i *PublishItem) CumulativeUsers(point uint64, start, end time.Time, mk *apistructs.MonitorKeys) (*apistructs.CardinalityResults, error) {
	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }
	req := query.CreateQueryRequest("ta_metric_mobile_metrics")
	req = req.
		SetDiagram("histogram").
		StartFrom(start).
		EndWith(end).
		Filter("tk", mk.AK).
		Filter("ai", mk.AI).
		Match("date", "*").
		Match("uid", "?*").
		Apply("cardinality", "tags.uid").
		LimitPoint(int(point))
	resp, err := metrics.Client.QueryMetric(req)
	if err != nil {
		return nil, err
	}
	respBody, err := parsingDataListResp(resp)
	if err != nil {
		return nil, err
	}
	if respBody == nil {
		return nil, errors.Errorf("no data from monitor")
	}
	times := respBody.Data.Times

	data := make([]uint64, 0, point)
	// 30天前
	thirtyDayAgo := start.AddDate(0, 0, -30)
	for _, t := range times {
		endTime := time.Unix(int64(t/1000), 0)

		itemReq := query.CreateQueryRequest("ta_metric_mobile_metrics")
		itemReq = itemReq.
			StartFrom(thirtyDayAgo).
			EndWith(endTime).
			Filter("tk", mk.AK).
			Filter("ai", mk.AI).
			Match("date", "*").
			Match("uid", "?*").
			Apply("cardinality", "tags.uid")
		logrus.Infof("monitor request params 累计用户: %s", itemReq.ConstructParam().Encode())
		itemResp, err := metrics.Client.QueryMetric(itemReq)
		if err != nil {
			return nil, err
		}
		itemRespBody, err := parsingResp(itemResp)
		if err != nil {
			return nil, err
		}
		for _, v := range itemRespBody.Data.Results[0].Data[0] {
			data = append(data, uint64(v.Data))
		}
	}
	for k := range respBody.Data.Results[0].Data[0] {
		item := respBody.Data.Results[0].Data[0][k]
		item.Data = data
	}
	return &(respBody.Data), nil
}

func errList(ak, ai, av string, start, end time.Time) ([]*apistructs.PublishItemStatisticsErrListResponse, error) {
	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }
	errReq := query.CreateQueryRequest("ta_error_mobile")
	errReq = errReq.
		StartFrom(start).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("error", "*").
		GroupBy([]string{"(doc['tags.error'].value + ',' +doc['tags.av'].value)"}).
		Apply("count", "tags.error").
		Apply("cardinality", "tags.uid").
		Apply("max", ".timestamp").
		Apply("min", ".timestamp")
	if av != "" {
		errReq = errReq.Filter("av", av)
	}
	logrus.Infof("monitor request params 错误列表: %s", errReq.ConstructParam().Encode())
	resp, err := metrics.Client.QueryMetric(errReq)
	if err != nil {
		return nil, err
	}
	logrus.Infof("monitor request params 错误列表: %s", string(resp.Body))
	respBody, err := parsingResp(resp)
	if err != nil {
		return nil, err
	}
	if respBody == nil {
		return []*apistructs.PublishItemStatisticsErrListResponse{}, err
	}
	result := make(map[string]*apistructs.PublishItemStatisticsErrListResponse, len(respBody.Data.Results[0].Data))
	for _, v := range respBody.Data.Results[0].Data {
		for key, value := range v {
			itemResult := apistructs.PublishItemStatisticsErrListResponse{}
			if item, ok := result[value.Tag]; ok {
				itemResult = *item
			}
			if strings.Contains(key, "error") {
				itemResult.TotalErr = uint64(value.Data)
			}
			if strings.Contains(key, "uid") {
				itemResult.AffectUsers = uint64(value.Data)
			}
			if key == "max.timestamp" {
				itemResult.TimeOfRecent = time.Unix(int64(value.Data)/int64(time.Second), int64(value.Data)%int64(time.Second))
			}
			if key == "min.timestamp" {
				itemResult.TimeOfFirst = time.Unix(int64(value.Data)/int64(time.Second), int64(value.Data)%int64(time.Second))
			}
			result[value.Tag] = &itemResult
		}
	}
	arr := make([]*apistructs.PublishItemStatisticsErrListResponse, 0, len(result))
	for k, v := range result {
		keyArr := strings.Split(k, ",")
		v.ErrSummary = keyArr[0]
		v.AppVersion = keyArr[1]
		arr = append(arr, v)
	}
	return arr, nil
}

func errInfact(start, end time.Time, ak, ai string) (uint64, float64, uint64, float64, error) {
	totalErrs := uint64(0)
	totalInfactUsers := uint64(0)
	totalErrRate := 0.0
	totalInfactUsersRate := 0.0

	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
	// }
	errReq := query.CreateQueryRequest("ta_error_mobile")
	errReq = errReq.
		StartFrom(start).
		EndWith(end).
		Filter("tk", ak).
		Filter("ai", ai).
		Match("error", "*").
		Apply("count", "tags.error").
		Apply("cardinality", "tags.uid")
	logrus.Infof("monitor request params 错误趋势: %s", errReq.ConstructParam().Encode())
	resp, err := metrics.Client.QueryMetric(errReq)
	if err != nil {
		return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
	}
	logrus.Infof("monitor request params 错误趋势: %s", string(resp.Body))
	respBody, err := parsingResp(resp)
	if err != nil {
		return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
	}
	if respBody == nil {
		return 0, 0, 0, 0, errors.Errorf("no data from monitor")
	}

	for k, v := range respBody.Data.Results[0].Data[0] {
		if strings.Contains(k, "error") {
			totalErrs = uint64(v.Data)
		}
		if strings.Contains(k, "uid") {
			totalInfactUsers = uint64(v.Data)
		}
	}
	logrus.Infof("monitor request params totalErrs: %d, totalInfactUsers: %d", totalErrs, totalInfactUsers)
	if totalErrs == 0 {
		return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
	} else {
		deviceReq := query.CreateQueryRequest("ta_metric_mobile_metrics")
		deviceReq = deviceReq.
			StartFrom(start).
			EndWith(end).
			Filter("tk", ak).
			Filter("ai", ai).
			Match("date", "*").
			Apply("count", "tags.cid")
		logrus.Infof("monitor request params 错误趋势总体: %s", deviceReq.ConstructParam().Encode())
		resp, err = metrics.Client.QueryMetric(deviceReq)
		totalStartUp, err := MetricsTotal(resp)
		if err != nil {
			return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
		}

		deviceReq = deviceReq.
			StartFrom(start).
			EndWith(end).
			Filter("tk", ak).
			Filter("ai", ai).
			Match("date", "*").
			Match("uid", "?*").
			Apply("cardinality", "tags.uid")
		resp, err = metrics.Client.QueryMetric(deviceReq)
		logrus.Infof("monitor request params 错误趋势总体1: %s", deviceReq.ConstructParam().Encode())

		totalActiveUsers, err := MetricsTotal(resp)
		if err != nil {
			return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
		}

		errRate := 0.0
		if totalStartUp != 0 {
			logrus.Infof("monitor request params totalErrs: %d, totalStartUp: %d", totalErrs, totalStartUp)
			errRate, err = strconv.ParseFloat(fmt.Sprintf("%.2f", (float64(totalErrs)/float64(totalStartUp)*100.0)), 64)
			if err != nil {
				return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
			}
			totalErrRate = errRate
		}

		infactUsersRate := 0.0
		if totalActiveUsers != 0 {
			logrus.Infof("monitor request params totalInfactUsers: %d, totalActiveUsers: %d", totalInfactUsers, totalActiveUsers)
			infactUsersRate, err = strconv.ParseFloat(fmt.Sprintf("%.2f", (float64(totalInfactUsers)/float64(totalActiveUsers)*100.0)), 64)
			if err != nil {
				return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
			}
			totalInfactUsersRate = infactUsersRate
		}
	}
	return totalErrs, totalErrRate, totalInfactUsers, totalInfactUsersRate, err
}

func MetricsTotal(resp *query.MetricQueryResponse) (uint64, error) {
	respBody, err := parsingResp(resp)
	if err != nil {
		return 0, err
	}
	if respBody == nil {
		return 0, errors.Errorf("no data from monitor")
	}
	lastSevenDayTotal := uint64(0)
	for _, v := range respBody.Data.Results[0].Data[0] {
		lastSevenDayTotal = uint64(v.Data)
	}
	return lastSevenDayTotal, nil
}

func SevenDayAvg(resp *query.MetricQueryResponse) (uint64, float64, error) {
	respBody, err := parsingDataListResp(resp)
	if err != nil {
		return 0, 0.0, err
	}
	if respBody == nil {
		return 0, 0, errors.Errorf("no data from monitor")
	}
	recentSevenDayUsers := uint64(0)
	lastSevenDayUsersGrowth := 0.0
	for _, v := range respBody.Data.Results[0].Data[0] {
		lastSevenDay := v.Data[0 : len(v.Data)-1]
		recentSevenDay := v.Data[1:len(v.Data)]

		// 计算今日七日平均值
		recentSevenDayUsers = Average(recentSevenDay, 7) //7
		// 计算昨日七日平均值
		lastSevenDayUsers := Average(lastSevenDay, 7) // 5
		// 计算昨日七日平均值增长率
		lastSevenDayUsersGrowth = Growth(float64(lastSevenDayUsers), float64(recentSevenDayUsers))
	}
	return recentSevenDayUsers, lastSevenDayUsersGrowth, nil
}

func SevenDayUserRetension(firstResp, secondResp *query.MetricQueryResponse) (string, float64, error) {
	firstRespBody, err := parsingDataListResp(firstResp)
	if err != nil {
		return "", 0.0, err
	}
	if firstRespBody == nil {
		return "", 0, errors.Errorf("no data from monitor")
	}
	firstDayUserIdDatas := []uint64{}
	for k, v := range firstRespBody.Data.Results[0].Data[0] {
		if strings.Contains(k, "firstDayUserId") {
			firstDayUserIdDatas = v.Data
		}
	}

	secondRespBody, err := parsingDataListResp(secondResp)
	if err != nil {
		return "", 0.0, err
	}
	if secondRespBody == nil {
		return "", 0, errors.Errorf("no data from monitor")
	}
	secondDayUserIdDatas := []uint64{}
	for k, v := range secondRespBody.Data.Results[0].Data[0] {
		if strings.Contains(k, "secondDayUserId") {
			secondDayUserIdDatas = v.Data
		}
	}

	rate := []float64{}
	for index, v := range firstDayUserIdDatas {
		if v == 0 {
			rate = append(rate, 0.0)
			continue
		}
		rate = append(rate, float64(secondDayUserIdDatas[index])/float64(v))
	}
	lastSevenDayArr := rate[0 : len(rate)-1]
	recentSevenDayArr := rate[1:]

	// 计算今日七日平均值
	recentSevenDay := AverageFloat(recentSevenDayArr, 7)
	// 计算昨日七日平均值
	lastSevenDay := AverageFloat(lastSevenDayArr, 7)
	// 计算昨日七日平均值增长率
	lastSevenDayUsersGrowth := Growth(lastSevenDay, recentSevenDay)

	return fmt.Sprintf("%.2f", (recentSevenDay)*100), lastSevenDayUsersGrowth, nil
}

func parsingDataListResp(resp *query.MetricQueryResponse) (*apistructs.PublishItemMetricsCardinalityResp, error) {
	if resp.StatusCode != 200 {
		logrus.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
		return nil, errors.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
	}
	var respBody apistructs.PublishItemMetricsCardinalityResp
	logrus.Infof("monitor resp data: %s", string(resp.Body))
	if err := json.Unmarshal(resp.Body, &respBody); err != nil {
		return nil, err
	}
	if len(respBody.Data.Results[0].Data) == 0 {
		return nil, nil
	}
	return &respBody, nil
}

func parsingResp(resp *query.MetricQueryResponse) (*apistructs.PublishItemMetricsCardinalitySingleResp, error) {
	if resp.StatusCode != 200 {
		logrus.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
		return nil, errors.Errorf("请求监控数据失败, statusCode: %d, body: %s", resp.StatusCode, string(resp.Body))
	}
	var respBody apistructs.PublishItemMetricsCardinalitySingleResp
	logrus.Infof("monitor single resp data: %s", string(resp.Body))
	if err := json.Unmarshal(resp.Body, &respBody); err != nil {
		return nil, err
	}
	if len(respBody.Data.Results[0].Data) == 0 {
		return nil, nil
	}
	return &respBody, nil
}

// 影响用户占比
func (i *PublishItem) EffactUsersRate(point uint64, start, end time.Time, av string, mk *apistructs.MonitorKeys) (*apistructs.CardinalityResultsInterface, error) {
	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }
	errReq := query.CreateQueryRequest("ta_error_mobile")
	errReq = errReq.
		SetDiagram("histogram").
		StartFrom(start).
		EndWith(end).
		Match("error", "*").
		Filter("tk", mk.AK).
		Filter("ai", mk.AI).
		Apply("cardinality", "tags.uid").
		LimitPoint(int(point))
	if av != "" {
		errReq = errReq.Filter("av", av)
	}
	logrus.Infof("monitor request params 影响用户占比: %s", errReq.ConstructParam().Encode())
	resp, err := metrics.Client.QueryMetric(errReq)
	if err != nil {
		return nil, err
	}
	dataList, err := parsingDataListResp(resp)
	if err != nil {
		return nil, err
	}
	if dataList == nil {
		return nil, errors.Errorf("no data from monitor")
	}
	effactUsers := make([]uint64, 0, point)
	for _, v := range dataList.Data.Results[0].Data[0] {
		effactUsers = v.Data
	}

	// get cumulative users for each point
	allUserResp, err := i.CumulativeUsers(point, start, end, mk)
	if err != nil {
		return nil, err
	}
	logrus.Debugf("all user resp: %v", allUserResp)
	allUsers := allUserResp.Results[0].Data[0]["cardinality.tags.uid"].Data

	rateArr := make([]float64, 0, point)
	for index, v := range effactUsers {
		if allUsers[index] == 0 {
			rateArr = append(rateArr, 0.0)
			continue
		}
		errRate, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", (float64(v)/float64(allUsers[index])*100.0)), 64)
		rateArr = append(rateArr, errRate)
	}
	resultItemMap := map[string]apistructs.CardinalityResultDataMapInterfaceValue{}
	for _, v := range dataList.Data.Results[0].Data[0] {
		resultItemData := apistructs.CardinalityResultDataMapInterfaceValue{
			Agg:  v.Agg,
			Tag:  "effectUsers",
			Name: "effectUsers",
			Unit: "%",
			Data: rateArr,
		}
		resultItemMap["cardinality.tags.rate"] = resultItemData
	}
	resultItem := apistructs.CardinalityResultInterfaceItem{
		Name: dataList.Data.Results[0].Name,
		Data: []map[string]apistructs.CardinalityResultDataMapInterfaceValue{resultItemMap},
	}
	result := &apistructs.CardinalityResultsInterface{
		Title:   dataList.Data.Title,
		Total:   dataList.Data.Total,
		Times:   dataList.Data.Times,
		Results: []apistructs.CardinalityResultInterfaceItem{resultItem},
	}
	return result, nil
}

// 崩溃率
func (i *PublishItem) CrashRate(point uint64, start, end time.Time, av string, mk *apistructs.MonitorKeys) (*apistructs.CardinalityResultsInterface, error) {
	// cfg := metrics.NewQueryConfig(metrics.WithSchema("http"), metrics.WithHost(discover.Monitor()))
	// client, err := metrics.NewClient().NewQuery(cfg)
	// if err != nil {
	// 	return nil, err
	// }
	errReq := query.CreateQueryRequest("ta_error_mobile")
	errReq = errReq.
		SetDiagram("histogram").
		StartFrom(start).
		EndWith(end).
		Match("error", "*").
		Filter("tk", mk.AK).
		Filter("ai", mk.AI).
		Apply("count", "tags.error").
		LimitPoint(int(point))
	if av != "" {
		errReq = errReq.Filter("av", av)
	}
	resp, err := metrics.Client.QueryMetric(errReq)
	if err != nil {
		return nil, err
	}
	dataList, err := parsingDataListResp(resp)
	if err != nil {
		return nil, err
	}
	if dataList == nil {
		logrus.Errorf("failed to parsing data list resp for monitor, data is nil")
		return nil, errors.Errorf("no data from monitor")
	}
	errs := make([]uint64, 0, point)
	for _, v := range dataList.Data.Results[0].Data[0] {
		errs = v.Data
	}

	startReq := query.CreateQueryRequest("ta_metric_mobile_metrics")
	startReq = startReq.
		SetDiagram("histogram").
		StartFrom(start).
		EndWith(end).
		Filter("tk", mk.AK).
		Filter("ai", mk.AI).
		Match("date", "*").
		Apply("count", "tags.cid").
		LimitPoint(int(point))
	if av != "" {
		startReq = startReq.Filter("av", av)
	}
	startResp, err := metrics.Client.QueryMetric(startReq)
	if err != nil {
		return nil, err
	}
	startDataList, err := parsingDataListResp(startResp)
	if err != nil {
		return nil, err
	}
	if startDataList == nil {
		logrus.Errorf("failed to parsing start data list resp for monitor, data is nil")
		return nil, errors.Errorf("no data from monitor")
	}
	allStart := make([]uint64, 0, point)
	for _, v := range startDataList.Data.Results[0].Data[0] {
		allStart = v.Data
	}

	rateArr := make([]float64, 0, point)
	for index, v := range errs {
		if allStart[index] == 0 {
			rateArr = append(rateArr, 0.0)
			continue
		}
		errRate, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", (float64(v)/float64(allStart[index])*100.0)), 64)
		rateArr = append(rateArr, errRate)
	}
	resultItemMap := map[string]apistructs.CardinalityResultDataMapInterfaceValue{}
	for _, v := range dataList.Data.Results[0].Data[0] {
		resultItemData := apistructs.CardinalityResultDataMapInterfaceValue{
			Agg:  v.Agg,
			Tag:  "crashRate",
			Name: "crashRate",
			Unit: "%",
			Data: rateArr,
		}
		resultItemMap["cardinality.tags.rate"] = resultItemData
	}
	resultItem := apistructs.CardinalityResultInterfaceItem{
		Name: dataList.Data.Results[0].Name,
		Data: []map[string]apistructs.CardinalityResultDataMapInterfaceValue{resultItemMap},
	}
	result := &apistructs.CardinalityResultsInterface{
		Title:   dataList.Data.Title,
		Total:   dataList.Data.Total,
		Times:   dataList.Data.Times,
		Results: []apistructs.CardinalityResultInterfaceItem{resultItem},
	}
	return result, nil
}

func Growth(last, recent float64) float64 {
	if last == 0.0 {
		if recent == 0.0 {
			return 0.0
		} else {
			return 100
		}
	}
	if recent == 0 {
		return 0.0
	}
	logrus.Infof("monitor request params last %f, recent %f", last, recent)
	result, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", ((recent-last)/last)*100), 64)
	return result
}

func Average(arr []uint64, cardinality uint64) uint64 {
	sum := uint64(0)
	for _, val := range arr {
		sum += val
	}
	return Round(float64(sum) / float64(cardinality))
}

func AverageFloat(arr []float64, cardinality uint64) float64 {
	sum := 0.0
	for _, val := range arr {
		sum += val
	}
	return sum / float64(cardinality)
}

func Round(x float64) uint64 {
	return uint64(math.Floor(x + 0.5))
}
