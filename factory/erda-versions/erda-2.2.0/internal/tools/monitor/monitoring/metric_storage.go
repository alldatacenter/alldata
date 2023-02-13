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

package monitoring

import (
	"context"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/dustin/go-humanize"
	"github.com/olivere/elastic"
	"github.com/prometheus/client_golang/prometheus"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
)

const tsqlMetric = `SELECT count(%s) AS doc_cnt, %s AS doc_label FROM %s GROUP BY %s`

var (
	metricStorageUsage = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Name:      "usage_bytes",
			Namespace: "metric",
			Subsystem: "storage",
			Help:      "metric storage usage of organization",
		},
		[]string{"x_org_name"},
	)
)

// store metric with elasticsearch
type esStorageMetric struct {
	metricQ metricq.Queryer
}

func newEsStorageMetric(metricQ metricq.Queryer) storageMetric {
	return &esStorageMetric{metricQ: metricQ}
}

// 1. get all indices info
// 2. get doc count with group within recent duration
// 3. get disk usage by math
func (es *esStorageMetric) UsageSummaryOrg() (map[string]uint64, error) {
	info, err := es.indicesInfo()
	if err != nil {
		return nil, err
	}

	type ele struct {
		mIndex *metricIndex
		orgMap map[string]uint64
	}
	ch := make(chan *ele)
	block := make(chan struct{}, 5)

	var wg sync.WaitGroup
	wg.Add(len(info))
	for m, item := range info {
		go func(mName string, mIndex *metricIndex) {
			defer func() {
				<-block
				wg.Done()
			}()

			block <- struct{}{}
			labelMap, err := es.docCount("org_name::tag", mName)
			if err != nil {
				return
			}
			ch <- &ele{
				mIndex: mIndex,
				orgMap: labelMap,
			}
		}(m, item)
	}
	go func() {
		wg.Wait()
		close(ch)
		close(block)
	}()

	// map<org_name>bytes
	usageMap := make(map[string]uint64)
	for data := range ch {
		for k, v := range data.orgMap {
			usage := v * data.mIndex.sizeBytes / data.mIndex.docCount
			usageMap[k] += usage
		}
	}

	return usageMap, nil
}

func (es *esStorageMetric) indicesInfo() (map[string]*metricIndex, error) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*60)
	defer cancel()
	resp, err := es.metricQ.Client().CatIndices().Bytes("b").Columns("index,docs.count,store.size").Do(ctx)
	if err != nil {
		return nil, err
	}
	return getMetricIdxMap(resp), nil
}

func getMetricIdxMap(resp elastic.CatIndicesResponse) map[string]*metricIndex {
	metricIdxMap := make(map[string]*metricIndex, len(resp))
	for _, indice := range resp {
		m := getMetricName(indice.Index)
		if m == "" {
			continue
		}

		bytes, err := humanize.ParseBytes(indice.StoreSize)
		if err != nil {
			continue
		}

		info, ok := metricIdxMap[m]
		if !ok {
			metricIdxMap[m] = &metricIndex{
				indices:   []string{indice.Index},
				docCount:  uint64(indice.DocsCount),
				sizeBytes: bytes,
			}
		} else {
			info.indices = append(info.indices, indice.Index)
			info.docCount += uint64(indice.DocsCount)
			info.sizeBytes += bytes
		}
	}
	return metricIdxMap
}

func getMetricName(index string) string {
	index = strings.TrimPrefix(index, "spot-")
	end := strings.Index(index, "-full_cluster")
	if end == -1 {
		return ""
	}
	return index[:end]
}

type metricIndex struct {
	indices   []string
	docCount  uint64
	sizeBytes uint64
}

type record struct {
	label string
	cnt   uint64
}

func (es *esStorageMetric) docCount(contField, metricName string) (map[string]uint64, error) {
	stmt := fmt.Sprintf(tsqlMetric, contField, contField, metricName, contField)

	ret := make(map[string]uint64)
	for end := time.Now(); ; {
		start := end.Add(-6 * time.Hour)
		rs, err := es.metricQ.Query(metricq.InfluxQL, stmt, nil, timeRange(start, end))
		if err != nil {
			return nil, err
		}
		// arrived at most start of time range
		if len(rs.Rows) == 0 {
			break
		}
		end = start

		for _, row := range rs.Rows {
			lable, ok := row[1].(string)
			if !ok {
				continue
			}
			cnt, ok := row[0].(float64)
			if !ok {
				continue
			}
			ret[lable] += uint64(cnt)
		}
	}

	return ret, nil
}

func timeRange(start, end time.Time) url.Values {
	options := url.Values{}
	options.Set("start", strconv.Itoa(int(start.UnixNano())/1000000))
	options.Set("end", strconv.Itoa(int(end.UnixNano())/1000000))
	return options
}
