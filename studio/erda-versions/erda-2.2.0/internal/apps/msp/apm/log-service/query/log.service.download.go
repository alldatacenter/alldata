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

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/providers/httpserver"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) initRoutes(routes httpserver.Router) {
	routes.GET("/api/log-service/:addon/download", p.logDownload)
}

type LogDownloadRequest struct {
	Start       int64    `query:"start" validate:"gte=1"`
	End         int64    `query:"end" validate:"gte=1"`
	Query       string   `query:"query"`
	Sort        []string `query:"sort"`
	Debug       bool     `query:"debug"`
	Addon       string   `param:"addon"`
	ClusterName string   `query:"clusterName"`
	Size        int      `query:"pageSize"`
	MaxReturn   int64    `param:"maxReturn"`
}

func (p *provider) logDownload(r *http.Request, w http.ResponseWriter, params *LogDownloadRequest) interface{} {
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

	w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	w.Header().Set("charset", "utf-8")
	w.Header().Set("Content-Disposition", "attachment;filename="+fileName)
	w.Header().Set("Content-Type", "application/octet-stream")

	err := p.DownloadLogsFromMonitor(r, w, params)
	if err != nil {
		return err
	}
	err = p.DownloadLogsFromLoghub(r, w, params)
	if err != nil {
		return err
	}
	return nil
}

func (p *provider) DownloadLogsFromMonitor(r *http.Request, w http.ResponseWriter, params *LogDownloadRequest) error {
	if !p.Cfg.QueryLogESEnabled {
		return nil
	}

	logKeys, err := p.logService.getLogKeys(params.Addon)
	if err != nil {
		return err
	}

	start, end := params.Start*int64(time.Millisecond), params.End*int64(time.Millisecond)
	expr := params.Query
	if len(expr) > 0 {
		expr = fmt.Sprintf("(%s) AND", expr)
	}
	if start > p.logService.startTime {
		expr = fmt.Sprintf("%s (%s)", expr, logKeys.
			ToESQueryString())
	} else if logKeys.Contains(logServiceKey) {
		expr = fmt.Sprintf("%s (%s)", expr, logKeys.
			Where(func(k LogKeyType, v StringList) bool { return k == logServiceKey }).
			ToESQueryString())
	} else {
		return nil
	}

	maxReturn := params.MaxReturn
	isDescendingOrder := !StringList(params.Sort).All(func(item string) bool { return strings.HasSuffix(item, " asc") })
	if isDescendingOrder {
		maxReturn = -maxReturn
	}

	stream, err := p.MonitorLogSvcClient.ScanLogsByExpression(context.Background(), &monitorpb.GetLogByExpressionRequest{
		Start:           start,
		End:             end,
		QueryExpression: expr,
		QueryMeta: &monitorpb.QueryMeta{
			OrgName:                 api.OrgName(r),
			IgnoreMaxTimeRangeLimit: true,
			PreferredIterateStyle:   monitorpb.IterateStyle_Scroll,
			PreferredBufferSize:     int32(params.Size),
			SkipTotalStat:           true,
		},
		Count: maxReturn,
		Debug: params.Debug,
		Live:  false,
	})
	if err != nil {
		return err
	}

	count := 0
	for {
		item, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		_, err = w.Write([]byte(item.Content))
		if err != nil {
			return err
		}
		_, err = w.Write([]byte("\n"))
		if err != nil {
			return err
		}
		count++
		if count%10 == 0 {
			w.(http.Flusher).Flush()
			continue
		}
	}
	w.(http.Flusher).Flush()
	return nil
}

func (p *provider) DownloadLogsFromLoghub(r *http.Request, w http.ResponseWriter, params *LogDownloadRequest) error {
	if p.Cfg.QueryLogESEnabled && params.Start*int64(time.Millisecond) > p.logService.startTime {
		return nil
	}

	orgId, err := strconv.ParseInt(api.OrgID(r), 10, 64)
	if err != nil {
		return fmt.Errorf("invalid Org-ID")
	}

	err = p.LoghubQuery.DownloadLogs(&query.LogDownloadRequest{
		LogRequest: query.LogRequest{
			OrgID:       orgId,
			ClusterName: params.ClusterName,
			Addon:       params.Addon,
			Start:       params.Start,
			End:         params.End,
			TimeScale:   time.Millisecond,
			Query:       params.Query,
			Debug:       params.Debug,
			Lang:        api.Language(r),
		},
		Sort:      params.Sort,
		Size:      params.Size,
		MaxReturn: params.MaxReturn,
	}, func(batchLogs []*query.Log) error {
		for _, item := range batchLogs {
			_, err = w.Write([]byte(item.Content))
			if err != nil {
				return err
			}
			w.Write([]byte("\n"))
		}
		w.(http.Flusher).Flush()
		return nil
	})

	return err
}
