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

// Package monitor collect and export openapi metrics
package monitor

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/terminal/table"
)

// Metrics 返回单调递增的 openapi metrics
func Metrics(rw http.ResponseWriter, req *http.Request) {
	metricsAvgPart := std.pstatAvg.Metrics()
	metricsSumPart := std.pstatSum.Metrics()

	merged := metricsSumPart
	for k, v := range metricsAvgPart {
		merged[k] = v
	}
	content, err := json.Marshal(merged)
	if err != nil {
		logrus.Errorf("[alert] openapi monitor metrics api: %v ", err)
		rw.WriteHeader(500)
		if _, err = rw.Write([]byte(err.Error())); err != nil {
			logrus.Errorf("http write: %v", err)
		}
		return
	}
	rw.WriteHeader(200)
	if _, err := rw.Write(content); err != nil {
		logrus.Errorf("http write: %v", err)
	}
}

// Stat 返回所收集的 openapi 状态
func Stat(rw http.ResponseWriter, req *http.Request) {
	s1, s2, s3, s4, s5, s1Avg, s2Avg, s3Avg, s4Avg, s5Avg, err := collectStats()
	if err != nil {
		rw.WriteHeader(500)
		if _, err = rw.Write([]byte(err.Error())); err != nil {
			logrus.Errorf("http write: %v", err)
		}
		return
	}
	headers := []string{" ", "last5min", "last20min", "last1hour", "last6hour", "last1day"}
	fmt.Printf("s1-5: %+v, %+v, %+v, %+v, %+v\n", s1, s2, s3, s4, s5) // debug print
	tables := mkTables(headers, s1, s2, s3, s4, s5)
	fmt.Printf("tables: %+v\n", tables) // debug print

	tablesAvg := mkTables(headers, s1Avg, s2Avg, s3Avg, s4Avg, s5Avg)

	content := ""
	for tablename, tablecontent := range tables {
		content += tablename + "\n" + tablecontent + "\n\n\n"
	}
	for tablename, tablecontent := range tablesAvg {
		content += tablename + "\n" + tablecontent + "\n\n\n"
	}

	// 因为是分2种类型统计的，虽然 metrics count 都是单纯的累加，但是还是分开存的
	metricsAvgPart := std.pstatAvg.Metrics()
	metricsSumPart := std.pstatSum.Metrics()

	var metricsbuf bytes.Buffer
	metricsCounts := [][]string{}
	for k, v := range metricsAvgPart {
		metricsCounts = append(metricsCounts, []string{k, strconv.FormatInt(v, 10)})
	}
	for k, v := range metricsSumPart {
		metricsCounts = append(metricsCounts, []string{k, strconv.FormatInt(v, 10)})
	}
	if err := table.NewTable(table.WithWriter(&metricsbuf)).Header([]string{"metric", "count"}).Data(metricsCounts).Flush(); err != nil {
		logrus.Errorf("[alert] openapi monitor mktable: %v", err)
	}
	content += "\nMetrics\n" + metricsbuf.String()

	rw.WriteHeader(200)
	if _, err := rw.Write([]byte(content)); err != nil {
		logrus.Errorf("http write: %v", err)
	}
}

func collectStats() (s1, s2, s3, s4, s5, s1Avg, s2Avg, s3Avg, s4Avg, s5Avg map[string]int64, err error) {
	if s1, err = std.pstatSum.Last5Min(); err != nil {
		return
	}
	if s2, err = std.pstatSum.Last20Min(); err != nil {
		return
	}
	if s3, err = std.pstatSum.Last1Hour(); err != nil {
		return
	}
	if s4, err = std.pstatSum.Last6Hour(); err != nil {
		return
	}
	if s5, err = std.pstatSum.Last1Day(); err != nil {
		return
	}
	if s1Avg, err = std.pstatAvg.Last5Min(); err != nil {
		return
	}
	if s2Avg, err = std.pstatAvg.Last20Min(); err != nil {
		return
	}
	if s3Avg, err = std.pstatAvg.Last1Hour(); err != nil {
		return
	}
	if s4Avg, err = std.pstatAvg.Last6Hour(); err != nil {
		return
	}
	s5Avg, err = std.pstatAvg.Last1Day()
	return
}

// return map[tableName]tablecontent
func mkTables(headers []string, last5min, last20min, last1hour, last6hour, last1day map[string]int64) map[string]string {
	data := map[string][][]string{}
	for k := range last1day {
		api := k
		parts := strings.Split(k, "#")
		if len(parts) > 1 {
			api = parts[1]
		}

		if strings.HasPrefix(k, APIInvokeCount.String()) {
			data[APIInvokeCount.String()] = append(data[APIInvokeCount.String()],
				[]string{api,
					strconv.FormatInt(last5min[k], 10),
					strconv.FormatInt(last20min[k], 10),
					strconv.FormatInt(last1hour[k], 10),
					strconv.FormatInt(last6hour[k], 10),
					strconv.FormatInt(last1day[k], 10)})
			continue
		}
		if strings.HasPrefix(k, APIInvokeDuration.String()) {
			data[APIInvokeDuration.String()] = append(data[APIInvokeDuration.String()],
				[]string{api,
					strconv.FormatInt(last5min[k], 10),
					strconv.FormatInt(last20min[k], 10),
					strconv.FormatInt(last1hour[k], 10),
					strconv.FormatInt(last6hour[k], 10),
					strconv.FormatInt(last1day[k], 10)})
			continue
		}
		if strings.HasPrefix(k, API50xCount.String()) {
			data[API50xCount.String()] = append(data[API50xCount.String()],
				[]string{api,
					strconv.FormatInt(last5min[k], 10),
					strconv.FormatInt(last20min[k], 10),
					strconv.FormatInt(last1hour[k], 10),
					strconv.FormatInt(last6hour[k], 10),
					strconv.FormatInt(last1day[k], 10)})
			continue
		}
		if strings.HasPrefix(k, API40xCount.String()) {
			data[API40xCount.String()] = append(data[API40xCount.String()],
				[]string{api,
					strconv.FormatInt(last5min[k], 10),
					strconv.FormatInt(last20min[k], 10),
					strconv.FormatInt(last1hour[k], 10),
					strconv.FormatInt(last6hour[k], 10),
					strconv.FormatInt(last1day[k], 10)})
			continue
		}
		data[k] = append(data[k],
			[]string{api,
				strconv.FormatInt(last5min[k], 10),
				strconv.FormatInt(last20min[k], 10),
				strconv.FormatInt(last1hour[k], 10),
				strconv.FormatInt(last6hour[k], 10),
				strconv.FormatInt(last1day[k], 10)})

	}
	tables := map[string]string{}
	var buf bytes.Buffer
	for tablename, tabledata := range data {
		buf.Reset()
		if err := table.NewTable(table.WithWriter(&buf)).Header(headers).Data(tabledata).Flush(); err != nil {
			logrus.Errorf("[alert] openapi monitor newtable: %v", err)
			return nil
		}
		tables[tablename] = buf.String()
	}
	return tables
}
