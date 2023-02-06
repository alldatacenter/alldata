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

package query

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	// 项目 + env 日志查询
	routes.GET("/api/micro_service/:addon/logs/statistic/histogram", p.logStatistic)
	routes.GET("/api/micro_service/:addon/logs/search", p.logSearch)
	routes.GET("/api/micro_service/:addon/logs/sequentialSearch", p.logSequentialSearch)
	routes.GET("/api/micro_service/logs/tags/tree", p.logMSTagsTree)
	routes.GET("/api/micro_service/:addon/logs/fields", p.logFields)
	routes.GET("/api/micro_service/:addon/logs/fields/aggregation", p.logFieldsAggregation)
	routes.GET("/api/micro_service/:addon/logs/download", p.logDownload)

	// 企业日志查询
	routes.GET("/api/org/logs/statistic/histogram", p.logStatistic)
	routes.GET("/api/org/logs/search", p.logSearch)
	routes.GET("/api/org/logs/tags/tree", p.orgLogTagsTree)

	routes.GET("/api/logs-query/indices", p.inspectIndices)
	return nil
}

func (p *provider) buildLogFilters(r *http.Request) []*Tag {
	if r.URL == nil {
		return nil
	}
	var filters []*Tag
	for k, vs := range r.URL.Query() {
		if strings.HasPrefix(k, "tags.") {
			k = k[len("tags."):]
			if len(k) <= 0 {
				continue
			}
			for _, v := range vs {
				filters = append(filters, &Tag{
					Key:   k,
					Value: v,
				})
			}
		}
	}
	return filters
}

func (p *provider) checkTime(start, end int64) error {
	if end <= start {
		return fmt.Errorf("end must after start")
	}
	if end-start > 6*31*24*60*60*1000 {
		return fmt.Errorf("too large time span")
	}
	return nil
}

func (p *provider) logStatistic(r *http.Request, params struct {
	Start       int64  `query:"start" validate:"gte=1"`
	End         int64  `query:"end" validate:"gte=1"`
	Query       string `query:"query"`
	Points      int64  `query:"points"`
	Interval    int64  `query:"interval"`
	Debug       bool   `query:"debug"`
	Addon       string `param:"addon"`
	ClusterName string `query:"clusterName"`
}) interface{} {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}
	if params.Points <= 0 {
		params.Points = 60
	}
	filters := p.buildLogFilters(r)
	data, err := p.StatisticLogs(&LogStatisticRequest{
		LogRequest: LogRequest{
			OrgID:       orgid,
			ClusterName: params.ClusterName,
			Addon:       params.Addon,
			Start:       params.Start,
			End:         params.End,
			TimeScale:   time.Millisecond,
			Filters:     filters,
			Query:       params.Query,
			Debug:       params.Debug,
			Lang:        api.Language(r),
		},
		Points:   params.Points,
		Interval: params.Interval,
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(data)
}

func (p *provider) logFieldsAggregation(r *http.Request, params struct {
	Start       int64    `query:"start" validate:"gte=1"`
	End         int64    `query:"end" validate:"gte=1"`
	Query       string   `query:"query"`
	Debug       bool     `query:"debug"`
	Addon       string   `param:"addon"`
	ClusterName string   `query:"clusterName"`
	AggFields   []string `query:"aggFields"`
	TermsSize   int64    `query:"termsSize"`
}) interface{} {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}
	err = p.checkTime(params.Start, params.End)
	if err != nil {
		return api.Errors.InvalidParameter(err)
	}
	if len(params.AggFields) == 0 {
		api.Errors.InvalidParameter("aggFields should not empty")
	}
	filters := p.buildLogFilters(r)
	termsSize := params.TermsSize
	if termsSize == 0 {
		termsSize = 20
	}
	data, err := p.AggregateLogFields(&LogFieldsAggregationRequest{
		LogRequest: LogRequest{
			OrgID:       orgid,
			ClusterName: params.ClusterName,
			Addon:       params.Addon,
			Start:       params.Start,
			End:         params.End,
			TimeScale:   time.Millisecond,
			Filters:     filters,
			Query:       params.Query,
			Debug:       params.Debug,
			Lang:        api.Language(r),
		},
		AggFields: params.AggFields,
		TermsSize: int(termsSize),
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(data)
}

func (p *provider) logDownload(r *http.Request, w http.ResponseWriter, params struct {
	Start       int64    `query:"start" validate:"gte=1"`
	End         int64    `query:"end" validate:"gte=1"`
	Query       string   `query:"query"`
	Sort        []string `query:"sort"`
	Debug       bool     `query:"debug"`
	Addon       string   `param:"addon"`
	ClusterName string   `query:"clusterName"`
	Size        int      `query:"pageSize"`
	MaxReturn   int64    `param:"maxReturn"`
}) interface{} {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}

	err = p.checkTime(params.Start, params.End)
	if err != nil {
		return api.Errors.InvalidParameter(err)
	}

	if params.MaxReturn <= 0 {
		params.MaxReturn = 1000000
	}
	if params.Size <= 0 {
		params.Size = 1000
	}

	fileName := strings.Join(
		[]string{
			time.Now().Format("20060102150405.000"),
			strconv.FormatInt(params.Start, 10),
			strconv.FormatInt(params.End, 10),
		},
		"_") + ".log"

	flusher := w.(http.Flusher)
	w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	w.Header().Set("charset", "utf-8")
	w.Header().Set("Content-Disposition", "attachment;filename="+fileName)
	w.Header().Set("Content-Type", "application/octet-stream")

	filters := p.buildLogFilters(r)
	err = p.DownloadLogs(&LogDownloadRequest{
		LogRequest: LogRequest{
			OrgID:       orgid,
			ClusterName: params.ClusterName,
			Addon:       params.Addon,
			Start:       params.Start,
			End:         params.End,
			TimeScale:   time.Millisecond,
			Filters:     filters,
			Query:       params.Query,
			Debug:       params.Debug,
			Lang:        api.Language(r),
		},
		Sort:      params.Sort,
		Size:      params.Size,
		MaxReturn: params.MaxReturn,
	}, func(batchLogs []*Log) error {
		for _, item := range batchLogs {
			_, err = w.Write([]byte(item.Content))
			if err != nil {
				return err
			}
			w.Write([]byte("\n"))
		}
		flusher.Flush()
		return nil
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	return nil
}

func (p *provider) logSequentialSearch(r *http.Request, params struct {
	TimestampNanos int64  `query:"timestampNanos"`
	Id             string `query:"id"`
	Offset         int64  `query:"offset"`
	Count          int64  `query:"count"`
	Query          string `query:"query"`
	Sort           string `query:"sort"`
	Debug          bool   `query:"debug"`
	Addon          string `param:"addon"`
	ClusterName    string `query:"clusterName"`
}) interface{} {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}
	if params.Count <= 0 {
		params.Count = 20
	}
	filters := p.buildLogFilters(r)
	start, end := params.TimestampNanos, int64(0)
	if params.Sort == "desc" {
		start, end = end, start
	}
	sorts := []string{"timestamp " + params.Sort, "id " + params.Sort, "offset " + params.Sort}

	logs, err := p.SearchLogs(&LogSearchRequest{
		LogRequest: LogRequest{
			OrgID:       orgid,
			ClusterName: params.ClusterName,
			Addon:       params.Addon,
			Start:       start,
			End:         end,
			TimeScale:   time.Nanosecond,
			Filters:     filters,
			Query:       params.Query,
			Debug:       params.Debug,
			Lang:        api.Language(r),
		},
		Page:        1,
		Size:        params.Count,
		Sort:        sorts,
		SearchAfter: []interface{}{params.TimestampNanos, params.Id, params.Offset},
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(logs)
}

func (p *provider) logSearch(r *http.Request, params struct {
	Start       int64    `query:"start" validate:"gte=1"`
	End         int64    `query:"end" validate:"gte=1"`
	Page        int64    `query:"pageNo" validate:"gte=1"`
	Size        int64    `query:"pageSize"`
	Query       string   `query:"query"`
	Sort        []string `query:"sort"`
	Debug       bool     `query:"debug"`
	Addon       string   `param:"addon"`
	ClusterName string   `query:"clusterName"`
	Highlight   bool     `query:"highlight"`
}) interface{} {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}
	if params.Size <= 0 {
		params.Size = 10
	}
	err = p.checkTime(params.Start, params.End)
	if err != nil {
		return api.Errors.InvalidParameter(err)
	}
	filters := p.buildLogFilters(r)
	logs, err := p.SearchLogs(&LogSearchRequest{
		LogRequest: LogRequest{
			OrgID:       orgid,
			ClusterName: params.ClusterName,
			Addon:       params.Addon,
			Start:       params.Start,
			End:         params.End,
			TimeScale:   time.Millisecond,
			Filters:     filters,
			Query:       params.Query,
			Debug:       params.Debug,
			Lang:        api.Language(r),
		},
		Page:      params.Page,
		Size:      params.Size,
		Sort:      params.Sort,
		Highlight: params.Highlight,
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(logs)
}

func (p *provider) logMSTagsTree(r *http.Request) interface{} {
	return api.Success(p.GetTagsTree("micro_service", api.Language(r)))
}
func (p *provider) orgLogTagsTree(r *http.Request) interface{} {
	return api.Success(p.GetTagsTree("org", api.Language(r)))
}

func (p *provider) inspectIndices(r *http.Request) interface{} {
	return api.Success(p.indices.Load())
}

func (p *provider) logFields() interface{} {
	return api.Success(p.ListDefaultFields())
}
