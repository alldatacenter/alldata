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

package apis

import (
	"context"
	"encoding/json"
	"fmt"
	"math"
	"strconv"
	"strings"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/cache"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type checkerV1Service struct {
	p             *provider
	projectDB     *db.ProjectDB
	metricDB      *db.MetricDB
	cache         *cache.Cache
	metricq       metricpb.MetricServiceServer
	projectServer projectpb.ProjectServiceServer
}

func (s *checkerV1Service) CreateCheckerV1(ctx context.Context, req *pb.CreateCheckerV1Request) (*pb.CreateCheckerV1Response, error) {
	if req.Data == nil {
		return nil, errors.NewMissingParameterError("data")
	}
	if req.Data.ProjectID <= 0 {
		return nil, errors.NewMissingParameterError("projectId")
	}
	if req.Data.TenantId == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}

	args, argsStr, err := s.ConvertArgsByMode(req.Data.Mode, req.Data.Config)

	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	extra := strconv.FormatInt(req.Data.ProjectID, 10)
	now := time.Now()
	m := &db.Metric{
		ProjectID:  req.Data.ProjectID,
		TenantId:   req.Data.TenantId,
		Env:        req.Data.Env,
		Name:       req.Data.Name,
		Mode:       req.Data.Mode,
		Extra:      extra,
		URL:        args.(*pb.HttpModeConfig).Url,
		Config:     argsStr,
		CreateTime: now,
		UpdateTime: now,
	}
	if err := s.metricDB.Create(m); err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	checker := s.ConvertToChecker(ctx, m)
	if checker != nil {
		err := s.cache.Put(checker)
		if err != nil {
			return nil, err
		}
	}
	return &pb.CreateCheckerV1Response{Data: m.ID}, nil
}

func (s *checkerV1Service) ConvertArgsByMode(mode string, args map[string]*structpb.Value) (interface{}, string, error) {
	switch mode {
	case "http":
		bytes, err := json.Marshal(args)
		jsonStr := string(bytes)
		if err != nil {
			return nil, "", err
		}
		var httpArgs pb.HttpModeConfig
		err = json.Unmarshal(bytes, &httpArgs)
		if httpArgs.Interval > int64(30*time.Minute.Seconds()) {
			httpArgs.Interval = int64(30 * time.Minute.Seconds())
		}
		if httpArgs.Interval < int64(15*time.Second.Seconds()) {
			httpArgs.Interval = int64(15 * time.Second.Seconds())
		}
		if httpArgs.Retry > 10 {
			httpArgs.Retry = 10
		}
		if httpArgs.Retry < 0 {
			httpArgs.Retry = 0
		}
		if err != nil {
			return nil, "", err
		}
		return &httpArgs, jsonStr, err
	default:
		return nil, "", nil
	}
}

func (s *checkerV1Service) ConvertToChecker(ctx context.Context, m *db.Metric) *pb.Checker {
	config := make(map[string]*structpb.Value)
	err := json.Unmarshal([]byte(m.Config), &config)
	if err != nil {
		return nil
	}

	ck := &pb.Checker{
		Id:     m.ID,
		Name:   m.Name,
		Type:   m.Mode,
		Config: config,
		Tags: map[string]string{
			"_metric_scope": "micro_service",
			"project_id":    strconv.FormatInt(m.ProjectID, 10),
			"env":           m.Env,
			"metric":        strconv.FormatInt(m.ID, 10),
			"metric_name":   m.Name,
		},
	}
	s.addTenantTags(ck, m.TenantId)
	return ck
}

func (s *checkerV1Service) addTenantTags(ck *pb.Checker, tenantId string) {
	ck.Tags["_metric_scope_id"] = tenantId
	ck.Tags["terminus_key"] = tenantId
}

func (s *checkerV1Service) UpdateCheckerV1(ctx context.Context, req *pb.UpdateCheckerV1Request) (*pb.UpdateCheckerV1Response, error) {
	if req.Data == nil {
		return nil, errors.NewMissingParameterError("data")
	}
	if req.Data.TenantId == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}
	metric, err := s.metricDB.GetByID(req.Id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if metric == nil {
		return nil, errors.NewNotFoundError(fmt.Sprintf("metric/%d", req.Id))
	}
	args, argsStr, err := s.ConvertArgsByMode(req.Data.Mode, req.Data.Config)
	metric.Name = req.Data.Name
	metric.URL = args.(*pb.HttpModeConfig).Url
	metric.Config = argsStr
	if metric.TenantId == "" {
		metric.TenantId = req.Data.TenantId
	}

	if err := s.metricDB.Update(metric); err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	projectID, err := s.getProjectID(metric)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	req.Data.ProjectID = projectID
	checker := s.ConvertToChecker(ctx, metric)
	if checker != nil {
		err := s.cache.Put(checker)
		if err != nil {
			return nil, err
		}
	}
	return &pb.UpdateCheckerV1Response{Data: req.Id}, nil
}

func (s *checkerV1Service) DeleteCheckerV1(ctx context.Context, req *pb.DeleteCheckerV1Request) (*pb.DeleteCheckerV1Response, error) {
	metric, err := s.metricDB.GetByID(req.Id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if metric == nil {
		return &pb.DeleteCheckerV1Response{}, nil
	}

	projectID, err := s.getProjectID(metric)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}

	err = s.metricDB.Delete(req.Id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	err = s.cache.Remove(req.Id)
	if err != nil {
		return nil, err
	}
	return &pb.DeleteCheckerV1Response{Data: &pb.CheckerV1{
		Name: metric.Name,
		Mode: metric.Mode,
		//Url:       metric.URL,
		ProjectID: projectID,
		Env:       metric.Env,
	}}, nil
}

func (s *checkerV1Service) GetCheckerV1(ctx context.Context, req *pb.GetCheckerV1Request) (*pb.GetCheckerV1Response, error) {
	metric, err := s.metricDB.GetByID(req.Id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if metric == nil {
		return &pb.GetCheckerV1Response{}, nil
	}
	projectID, err := s.getProjectID(metric)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return &pb.GetCheckerV1Response{
		Data: &pb.CheckerV1{
			Name: metric.Name,
			Mode: metric.Mode,
			//Url:       metric.URL,
			ProjectID: projectID,
			Env:       metric.Env,
		},
	}, nil
}

func (s *checkerV1Service) getProjectID(m *db.Metric) (int64, error) {
	var projectID int64 = -1
	if m.Extra != "" {
		_, err := strconv.ParseInt(m.Extra, 10, 64)
		if err == nil {
			projectID = m.ProjectID
		}
	}
	if projectID < 0 {
		proj, err := s.projectDB.GetByID(m.ProjectID)
		if err != nil {
			return 0, err
		}
		projectID = proj.ProjectID
	}
	return projectID, nil
}

func (s *checkerV1Service) DescribeCheckersV1(ctx context.Context, req *pb.DescribeCheckersV1Request) (*pb.DescribeCheckersV1Response, error) {
	proj, err := s.projectDB.GetByProjectID(req.ProjectID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	var list []*db.Metric
	if proj != nil {
		// history record
		oldMetrics, err := s.metricDB.ListByProjectIDAndEnv(proj.ID, req.Env)
		if err != nil {
			return nil, errors.NewDatabaseError(err)
		}
		for _, m := range oldMetrics {
			if m.Extra == "" {
				// data fix for history record
				m.ProjectID = proj.ProjectID
				m.Extra = strconv.FormatInt(proj.ProjectID, 10)
				if m.TenantId == "" {
					m.TenantId = req.TenantId
				}
				err := s.metricDB.Update(m)
				if err != nil {
					return nil, errors.NewDatabaseError(err)
				}
			}
		}
		newMetrics, err := s.metricDB.ListByProjectIDAndEnv(req.ProjectID, req.Env)
		if err != nil {
			return nil, errors.NewDatabaseError(err)
		}
		for _, m := range newMetrics {
			if m.Extra != "" {
				extra, err := strconv.ParseInt(m.Extra, 10, 64)
				if err == nil && m.ProjectID == extra {
					list = append(list, m)
				}
			}
		}
	} else {
		list, err = s.metricDB.ListByProjectIDAndEnv(req.ProjectID, req.Env)
		if err != nil {
			return nil, errors.NewDatabaseError(err)
		}
	}

	results := make(map[int64]*pb.DescribeItemV1)
	for _, item := range list {
		if item.TenantId == "" {
			item.TenantId = req.TenantId
			err := s.metricDB.Update(item)
			if err != nil {
				return nil, errors.NewDatabaseError(err)
			}
		}

		config := make(map[string]*structpb.Value)
		if item.Config != "" {
			err := handleBody(item, config)
			if err != nil {
				return nil, err
			}
		} else {
			oldConfig(item, config)
		}

		result := &pb.DescribeItemV1{
			Name:   item.Name,
			Mode:   item.Mode,
			Url:    item.URL,
			Config: config,
			Status: StatusMiss,
		}
		results[item.ID] = result
	}
	apis.Language(ctx)
	err = s.QueryCheckersLatencySummaryByProject(apis.Language(ctx), req.ProjectID, results)
	if err != nil {
		return nil, errors.NewServiceInvokingError(fmt.Sprintf("status_page.project_id/%d", req.ProjectID), err)
	}
	var downCount int64
	for _, item := range results {
		if item.Status == StatusRED {
			downCount++
		}
	}
	return &pb.DescribeCheckersV1Response{
		Data: &pb.DescribeResultV1{
			DownCount: downCount,
			Metrics:   results,
		},
	}, nil
}

func oldConfig(item *db.Metric, config map[string]*structpb.Value) {
	switch item.Mode {
	case "http":
		config["url"] = structpb.NewStringValue(item.URL)
		config["method"] = structpb.NewStringValue("GET")
	}
}

func (s *checkerV1Service) DescribeCheckerV1(ctx context.Context, req *pb.DescribeCheckerV1Request) (*pb.DescribeCheckerV1Response, error) {
	metric, err := s.metricDB.GetByID(req.Id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	results := make(map[int64]*pb.DescribeItemV1)
	var downCount int64
	if metric != nil {
		config := make(map[string]*structpb.Value)
		if metric.Config != "" {
			err := handleBody(metric, config)
			if err != nil {
				return nil, err
			}
		} else {
			oldConfig(metric, config)
		}
		results[req.Id] = &pb.DescribeItemV1{
			Name:   metric.Name,
			Mode:   metric.Mode,
			Url:    metric.URL,
			Config: config,
			Status: StatusMiss,
		}
		language := apis.Language(ctx)
		err = s.QueryCheckersLatencySummary(language, req.Id, req.Period, results)
		if err != nil {
			return nil, errors.NewServiceInvokingError(fmt.Sprintf("status_page.metric/%d", req.Id), err)
		}
		for _, item := range results {
			if item.Status == StatusRED {
				downCount++
			}
		}
	}
	return &pb.DescribeCheckerV1Response{
		Data: &pb.DescribeResultV1{
			DownCount: downCount,
			Metrics:   results,
		},
	}, nil
}

func handleBody(metric *db.Metric, config map[string]*structpb.Value) error {
	if metric.Config == "" {
		return errors.NewNotFoundError("config")
	}
	err := json.Unmarshal([]byte(metric.Config), &config)
	if err != nil {
		return err
	}
	if v, ok := config["body"]; ok {
		bodyBytes, err := json.Marshal(v.GetStructValue())
		if err != nil {
			return err
		}
		bodyStr := string(bodyBytes)
		if !strings.Contains(bodyStr, "content") {
			// history
			fields := v.GetStructValue().GetFields()
			fields["content"] = structpb.NewStringValue(bodyStr)
			fields["type"] = structpb.NewStringValue("none")
			headers := config["headers"].GetStructValue().GetFields()
			if v, ok := headers["Content-Type"]; ok {
				fields["type"] = structpb.NewStringValue(v.GetStringValue())
			}

			for k := range fields {
				if k != "content" && k != "type" {
					delete(fields, k)
				}
			}
		}
	}
	return nil
}

func getTimeRange(unit string, num int, align bool) (start int64, end int64, interval string) {
	now := time.Now()
	alignTime := func(interval string) time.Time {
		if align {
			ts := now.UnixNano()
			switch interval {
			case "24h":
				const day = 24 * int64(time.Hour)
				if ts%day != 0 {
					ts = ts - ts%day + day
					now = time.Unix(ts/int64(time.Second), ts%int64(time.Second))
				}
			case "1h":
				const hour = int64(time.Hour)
				if ts%hour != 0 {
					ts = ts - ts%hour + hour
					now = time.Unix(ts/int64(time.Second), ts%int64(time.Second))
				}
			case "1m":
				const minute = int64(time.Minute)
				if ts%minute != 0 {
					ts = ts - ts%minute + minute
					now = time.Unix(ts/int64(time.Second), ts%int64(time.Second))
				}
			}
		}
		return now
	}
	switch strings.ToLower(unit) {
	case "year":
		interval = "24h"
		now = alignTime(interval)
		now = now.AddDate(0, 0, 1)
	case "month":
		interval = "24h"
		now = alignTime(interval)
		start = now.AddDate(0, -1*num, 0).UnixNano() / int64(time.Millisecond)
	case "week":
		interval = "24h"
		now = alignTime(interval)
		start = now.AddDate(0, 0, -7*num).UnixNano() / int64(time.Millisecond)
	case "day":
		interval = "1h"
		now = alignTime(interval)
		start = now.AddDate(0, 0, -1*num).UnixNano() / int64(time.Millisecond)
	default:
		interval = "1m"
		now = alignTime(interval)
		start = now.Add(time.Duration(-1*int64(num)*int64(time.Hour))).UnixNano() / int64(time.Millisecond)
	}
	end = now.UnixNano() / int64(time.Millisecond)
	return
}

func (s *checkerV1Service) QueryCheckersLatencySummaryByProject(lang i18n.LanguageCodes, projectID int64, metrics map[int64]*pb.DescribeItemV1) error {
	start, end, duration := getTimeRange("hour", 1, false)
	interval, _ := structpb.NewValue(map[string]interface{}{"duration": duration})
	return s.queryCheckerMetrics(lang, start, end, `
	SELECT timestamp(), metric::tag, status_name::tag, round_float(avg(latency),2), max(latency), min(latency), count(latency), sum(latency)
	FROM status_page 
	WHERE project_id=$projectID 
	GROUP BY time($interval), metric::tag, status_name::tag 
	LIMIT 200`,
		map[string]*structpb.Value{
			"projectID": structpb.NewStringValue(strconv.FormatInt(projectID, 10)),
			"interval":  interval,
		}, metrics,
	)
}

func (s *checkerV1Service) QueryCheckersLatencySummary(lang i18n.LanguageCodes, metricID int64, timeUnit string, metrics map[int64]*pb.DescribeItemV1) error {
	start, end, duration := getTimeRange(timeUnit, 1, false)
	interval, _ := structpb.NewValue(map[string]interface{}{"duration": duration})
	return s.queryCheckerMetrics(lang, start, end, `
	SELECT timestamp(), metric::tag, status_name::tag, round_float(avg(latency),2), max(latency), min(latency), count(latency), sum(latency)
	FROM status_page 
	WHERE metric=$metric 
	GROUP BY time($interval), metric::tag, status_name::tag 
	LIMIT 200`,
		map[string]*structpb.Value{
			"metric":   structpb.NewStringValue(strconv.FormatInt(metricID, 10)),
			"interval": interval,
		}, metrics,
	)
}

func (s *checkerV1Service) queryCheckerMetrics(lang i18n.LanguageCodes, start, end int64, statement string, params map[string]*structpb.Value, metrics map[int64]*pb.DescribeItemV1) error {
	req := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    params,
	}
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	resp, err := s.metricq.QueryWithInfluxFormat(ctx, req)
	if err != nil {
		return err
	}
	s.parseMetricSummaryResponse(lang, resp, metrics)
	return nil
}

const (
	StatusRED   = "Major Outage"
	StatusGreen = "Operational"
	StatusMiss  = "Miss"
)

func (s *checkerV1Service) parseMetricSummaryResponse(lang i18n.LanguageCodes, resp *metricpb.QueryWithInfluxFormatResponse, metrics map[int64]*pb.DescribeItemV1) {
	type summaryItem struct {
		time  []int64
		avg   []float64
		max   []float64
		min   []float64
		sum   []float64
		count []int64
	}
	if len(resp.Results) > 0 && len(resp.Results[0].Series) > 0 {
		summary := make(map[string]map[string]*summaryItem)
		serie := resp.Results[0].Series[0]
		groupedRows := groupSerieRows(serie.Rows, 2, 3)
		for _, group := range groupedRows {
			if len(group.keys) < 2 {
				continue
			}
			metric := group.keys[0]
			status := summary[metric]
			if status == nil {
				status = make(map[string]*summaryItem)
				summary[metric] = status
			}
			statusName := group.keys[1]
			item := status[statusName]
			if item == nil {
				item = &summaryItem{}
				status[statusName] = item
			}
			for _, row := range group.rows {
				timestamp := int64(row.Values[1].GetNumberValue())
				avg := row.Values[4].GetNumberValue()
				max := row.Values[5].GetNumberValue()
				min := row.Values[6].GetNumberValue()
				count := int64(row.Values[7].GetNumberValue())
				sum := row.Values[8].GetNumberValue()
				item.time = append(item.time, timestamp/int64(time.Millisecond))
				item.avg = append(item.avg, avg)
				item.max = append(item.max, max)
				item.min = append(item.min, min)
				item.count = append(item.count, count)
				item.sum = append(item.sum, sum)
			}
		}
		for id, m := range metrics {
			idstr := strconv.FormatInt(id, 10)
			status, ok := summary[idstr]
			if !ok {
				continue
			}
			chart := &pb.CheckerChartV1{}
			m.Chart = chart
			for _, item := range status {
				if len(item.time) > len(chart.Time) {
					chart.Time = item.time
				}
				if len(chart.Latency) == 0 {
					chart.Latency = item.avg
				} else {
					minList, maxList := chart.Latency, item.avg
					if len(chart.Latency) > len(item.avg) {
						minList, maxList = item.avg, chart.Latency
					}
					for i, v := range minList {
						if v > maxList[i] {
							maxList[i] = v
						}
					}
					chart.Latency = maxList
				}
			}

			var downCount, upCount int64
			for stat, item := range status {
				if stat != StatusRED && stat != StatusGreen {
					stat = StatusMiss
				}
				for i := range item.time {
					if i < len(chart.Status) {
						if item.count[i] > 0 {
							if stat == StatusRED || chart.Status[i] == StatusMiss {
								chart.Status[i] = stat
							}
						}
					} else {
						if item.count[i] > 0 {
							chart.Status = append(chart.Status, stat)
						} else {
							chart.Status = append(chart.Status, StatusMiss)
						}
					}
					if stat == StatusRED {
						downCount += item.count[i]
					} else {
						upCount += item.count[i]
					}
				}
				interval := int64(m.Config["interval"].GetNumberValue())
				if interval == 0 {
					// old record
					interval = int64(30)
				}
				if stat == StatusRED {
					duration := int64(0)
					for i := len(item.time) - 1; i >= 0; i-- {
						if item.count[i] > 0 {
							j := i - 1
							jcounts := int64(0)
							for ; j >= 0; j-- {
								jcounts += item.count[j]
								if item.count[j] <= 0 {
									break
								}
							}
							if j < 0 {
								j = 0
							}
							duration = jcounts*interval + item.count[i]*interval
							i = j
						}
					}
					if duration < 60 {
						m.DownDuration = fmt.Sprintf("%d%s", duration, s.p.I18n.Text(lang, "s"))
					} else if duration < 60*60 {
						m.DownDuration = fmt.Sprintf("%d%s", (duration+60-1)/60, s.p.I18n.Text(lang, "m"))
					} else {
						m.DownDuration = fmt.Sprintf("%d%s", (duration+60*60-1)/(60*60), s.p.I18n.Text(lang, "h"))
					}
				}
			}
			if len(m.DownDuration) <= 0 {
				m.DownDuration = fmt.Sprintf("0%s", s.p.I18n.Text(lang, "s"))
			}
			if len(chart.Status) > 0 {
				for i := len(chart.Status) - 1; i >= 0; i-- {
					if chart.Status[i] != StatusMiss {
						m.Status = chart.Status[i]
						break
					}
				}
			}
			if len(m.Status) <= 0 {
				m.Status = StatusMiss
			}
			totalCount := downCount + upCount
			if totalCount > 0 {
				m.Downtime = fmt.Sprintf("%.2f%%", float64(downCount)*100/float64(totalCount))
				m.Uptime = fmt.Sprintf("%.2f%%", float64(upCount)*100/float64(totalCount))
			} else {
				m.Downtime = "0%"
				m.Uptime = "0%"
			}

			var max, min, sum float64
			var count int64
			for _, item := range status {
				for _, v := range item.max {
					if v > max {
						max = v
					}
				}
				for i, v := range item.min {
					if v != 0 && item.count[i] > 0 {
						if min == 0 || v < min {
							min = v
						}
					}
				}
				for _, v := range item.sum {
					sum += v
				}
				for _, v := range item.count {
					count += v
				}
			}
			if count != 0 {
				m.Avg = roundFloat(sum / float64(count))
				m.Latency = m.Avg
			}
			m.Max = max
			m.Min = min
			m.Apdex = computeApdex(chart.Latency)
		}
	}
}

func computeApdex(values []float64) float64 {
	if len(values) == 0 {
		return 0
	}
	var sc, tc float64
	for _, v := range values {
		v = v / 1000
		if v < 1 {
			sc++
		} else if v < 3 {
			tc++
		}
	}
	return (sc + tc/2) / float64(len(values))
}

func (s *checkerV1Service) GetCheckerStatusV1(ctx context.Context, req *pb.GetCheckerStatusV1Request) (*pb.GetCheckerStatusV1Response, error) {
	start, end, duration := getTimeRange("month", 3, true)
	interval, _ := structpb.NewValue(map[string]interface{}{"duration": duration})
	mreq := &metricpb.QueryWithInfluxFormatRequest{
		Start: strconv.FormatInt(start, 10),
		End:   strconv.FormatInt(end, 10),
		Statement: `
		SELECT timestamp(), status_name::tag, count(latency)
		FROM status_page 
		WHERE metric=$metric 
		GROUP BY time($interval), status_name::tag 
		LIMIT 200`,
		Params: map[string]*structpb.Value{
			"metric":   structpb.NewStringValue(strconv.FormatInt(req.Id, 10)),
			"interval": interval,
		},
	}
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	resp, err := s.metricq.QueryWithInfluxFormat(ctx, mreq)
	if err != nil {
		return nil, errors.NewServiceInvokingError(fmt.Sprintf("status_page.metric/%d", req.Id), err)
	}
	var times []int64
	var status []string
	if len(resp.Results) > 0 && len(resp.Results[0].Series) > 0 {
		serie := resp.Results[0].Series[0]
		groupedRows := groupSerieRows(serie.Rows, 2)
		for _, group := range groupedRows {
			if len(group.keys) < 1 {
				continue
			}
			stat := group.keys[0]
			if stat != StatusRED && stat != StatusGreen {
				stat = StatusMiss
			}
			var ts []int64
			for i, row := range group.rows {
				timestamp := int64(row.Values[1].GetNumberValue())
				count := int64(row.Values[3].GetNumberValue())

				ts = append(ts, timestamp/int64(time.Millisecond))
				if i < len(status) {
					if count > 0 {
						if stat == StatusRED || status[i] == StatusMiss {
							status[i] = stat
						}
					}
				} else {
					if count > 0 {
						status = append(status, stat)
					} else {
						status = append(status, StatusMiss)
					}
				}
			}
			if len(ts) > len(times) {
				times = ts
			}
		}
	}
	return &pb.GetCheckerStatusV1Response{
		Data: &pb.CheckerStatusV1{
			Time:   times,
			Status: status,
		},
	}, nil
}

func (s *checkerV1Service) GetCheckerIssuesV1(ctx context.Context, req *pb.GetCheckerIssuesV1Request) (*pb.GetCheckerIssuesV1Response, error) {
	return &pb.GetCheckerIssuesV1Response{
		Data: make([]*structpb.Value, 0), // depracated, so return empty list
	}, nil
}

func roundFloat(v float64) float64 {
	v, _ = strconv.ParseFloat(fmt.Sprintf("%.2f", v), 64)
	return v
}

type groupedRows struct {
	keys []string
	rows []*metricpb.Row
}

func groupSerieRows(rows []*metricpb.Row, keys ...int) []*groupedRows {
	var groups []*groupedRows
	lastTime := int64(math.MinInt64)
loop:
	for _, row := range rows {
		for _, val := range row.Values {
			if val == nil {
				continue loop
			}
		}
		timestamp := int64(row.Values[1].GetNumberValue())
		if len(groups) > 0 && timestamp > lastTime {
			groups[len(groups)-1].rows = append(groups[len(groups)-1].rows, row)
		} else {
			groups = append(groups, &groupedRows{rows: []*metricpb.Row{row}})
		}
		lastTime = timestamp
	}
	for _, group := range groups {
	rowsloop:
		for _, row := range group.rows {
			var kvals []string
			for _, i := range keys {
				val := row.Values[i].AsInterface()
				if val == nil {
					continue rowsloop
				}
				kvals = append(kvals, fmt.Sprint(val))
			}
			group.keys = kvals
			break
		}
	}
	return groups
}
