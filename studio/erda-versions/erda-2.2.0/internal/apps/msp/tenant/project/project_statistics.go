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

package project

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

type projectStats struct {
	serviceCount   int64
	lastActiveTime int64
	alertCount     int64
}

func (p *projectStats) merge(data *projectStats) *projectStats {
	p.serviceCount += data.serviceCount
	p.alertCount += data.alertCount
	if data.lastActiveTime > p.lastActiveTime {
		p.lastActiveTime = data.lastActiveTime
	}
	return p
}

type projectStatisticMap map[string]map[string]*projectStats

func (ps projectStatisticMap) initOrUpdate(projectId, terminusKey string, updateFunc func(*projectStats)) {
	if ps[projectId] == nil {
		ps[projectId] = map[string]*projectStats{}
	}
	if ps[projectId][terminusKey] == nil {
		ps[projectId][terminusKey] = &projectStats{serviceCount: 0, lastActiveTime: 0, alertCount: 0}
	}
	updateFunc(ps[projectId][terminusKey])
}

func (ps projectStatisticMap) statForProjectId(projectId string) (*projectStats, bool) {
	tkMap, ok := ps[projectId]
	if !ok {
		return nil, false
	}
	result := &projectStats{}
	for _, stats := range tkMap {
		result.merge(stats)
	}
	return result, true
}

func (ps projectStatisticMap) statForTerminusKeys(projectId string, terminusKeys ...string) (*projectStats, bool) {
	tkMap, ok := ps[projectId]
	if !ok {
		return nil, false
	}
	result := &projectStats{}
	for _, terminusKey := range terminusKeys {
		if stats, ok := tkMap[terminusKey]; ok {
			result.merge(stats)
		}
	}
	return result, true
}

func (s *projectService) getProjectsStatistics(projectIds ...string) (map[string]*projectStats, error) {
	if len(projectIds) == 0 {
		return nil, fmt.Errorf("empty projects list")
	}
	endMillSeconds := time.Now().UnixNano() / int64(time.Millisecond)
	oneDayAgoMillSeconds := endMillSeconds - int64(24*time.Hour/time.Millisecond)
	//sevenDayAgoMillSeconds := endMillSeconds - int64(7*24*time.Hour/time.Millisecond)

	terminusKeyMap := map[string]map[string]string{}
	setTkMapKey := func(projectId, workspace, tk string) {
		ws, ok := terminusKeyMap[projectId]
		if !ok {
			ws = map[string]string{}
			terminusKeyMap[projectId] = ws
		}
		ws[workspace] = tk
	}
	for _, projectId := range projectIds {
		// get tk from tenant table
		tenants, err := s.MSPTenantDB.QueryTenantByProjectID(projectId)
		if err != nil {
			s.p.Log.Warnf("fail to get sp_tenant info, projectId: %s", projectId)
			continue
		}
		for _, tenant := range tenants {
			setTkMapKey(projectId, tenant.RelatedWorkspace, tenant.Id)
		}

		intPId, _ := strconv.ParseInt(projectId, 10, 64)
		monitors, err := s.MonitorDB.GetMonitorByProjectId(intPId)
		if err != nil {
			s.p.Log.Warnf("fail to get sp_monitor info, projectId: %s", projectId)
			continue
		}
		for _, m := range monitors {
			setTkMapKey(projectId, m.Workspace, m.TerminusKey)
		}
	}

	var terminusIds []interface{}
	linq.From(terminusKeyMap).SelectManyBy(func(i interface{}) linq.Query {
		return linq.From(i.(linq.KeyValue).Value)
	}, func(inner interface{}, outer interface{}) interface{} {
		return inner.(linq.KeyValue).Value
	}).ToSlice(&terminusIds)

	terminusKeyList, _ := structpb.NewList(terminusIds)
	statisticMap := projectStatisticMap{}

	// get services count and last active time
	req := &metricpb.QueryWithInfluxFormatRequest{
		Start: strconv.FormatInt(oneDayAgoMillSeconds, 10),
		End:   strconv.FormatInt(endMillSeconds, 10),
		Filters: []*metricpb.Filter{
			{
				Key:   "tags.terminus_key",
				Op:    "in",
				Value: structpb.NewListValue(terminusKeyList),
			},
		},
		Statement: `
		SELECT terminus_key::tag, distinct(service_id::tag), max(timestamp)
		FROM application_service_node
		WHERE _metric_scope::tag = 'micro_service'
		GROUP BY terminus_key::tag
        `,
	}
	if err := s.doInfluxQuery(req, func(row *metricpb.Row) {
		terminusKey := row.Values[0].GetStringValue()
		servicesCount := row.Values[1].GetNumberValue()
		activeTime := row.Values[2].GetNumberValue()

		statisticMap.initOrUpdate("", terminusKey, func(stats *projectStats) {
			stats.serviceCount = int64(servicesCount)
			stats.lastActiveTime = int64(activeTime) / int64(time.Millisecond)
		})
	}); err != nil {
		return nil, err
	}

	// get alert count
	req = &metricpb.QueryWithInfluxFormatRequest{
		Start: strconv.FormatInt(oneDayAgoMillSeconds, 10),
		End:   strconv.FormatInt(endMillSeconds, 10),
		Filters: []*metricpb.Filter{
			{
				Key:   "tags.terminus_key",
				Op:    "in",
				Value: structpb.NewListValue(terminusKeyList),
			},
		},
		Statement: `
		SELECT terminus_key::tag, count(project_id::tag)
		FROM analyzer_alert
		WHERE alert_scope::tag = 'micro_service'
		GROUP BY terminus_key::tag
		`,
	}
	if err := s.doInfluxQuery(req, func(row *metricpb.Row) {
		terminusKey := row.Values[0].GetStringValue()
		alertCount := row.Values[1].GetNumberValue()

		statisticMap.initOrUpdate("", terminusKey, func(stats *projectStats) {
			stats.alertCount = int64(alertCount)
		})
	}); err != nil {
		return nil, err
	}

	// merge results
	result := map[string]*projectStats{}
	for _, projectId := range projectIds {
		stats, ok := statisticMap.statForProjectId(projectId)
		if !ok {
			var tks []string
			linq.From(terminusKeyMap[projectId]).Select(func(i interface{}) interface{} {
				return i.(linq.KeyValue).Value
			}).ToSlice(&tks)
			stats, ok = statisticMap.statForTerminusKeys("", tks...)
		}
		if !ok {
			result[projectId] = &projectStats{}
			continue
		}

		result[projectId] = stats
	}

	return result, nil
}

func (s *projectService) doInfluxQuery(req *metricpb.QueryWithInfluxFormatRequest, rowCallback func(row *metricpb.Row)) error {
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	resp, err := s.metricq.QueryWithInfluxFormat(ctx, req)
	if err != nil {
		return fmt.Errorf("failed to do metrics: %s", err)
	}
	if len(resp.Results) > 0 &&
		len(resp.Results[0].Series) > 0 &&
		len(resp.Results[0].Series[0].Rows) > 0 {

		for _, row := range resp.Results[0].Series[0].Rows {
			rowCallback(row)
		}
	}
	return nil
}
