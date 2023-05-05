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

// import (
// 	"fmt"
// 	"math"

// 	"github.com/prometheus/client_golang/prometheus"

// 	"github.com/erda-project/erda/apistructs"
// 	"github.com/erda-project/erda/bundle"
// 	"github.com/erda-project/erda/internal/tools/monitor/core/log/schema"
// 	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
// )

// const tsqlLog = `SELECT keyspace::tag, address::tag, value(cassandra_columnfamily_totaldiskspaceused::field) FROM cassandra WHERE columnfamily::tag='base_log' AND keyspace::tag=~/spot_.*?/ AND keyspace::tag!='spot_prod' GROUP BY keyspace::tag, address::tag`

// var (
// 	logStorageUsage = prometheus.NewGaugeVec(
// 		prometheus.GaugeOpts{
// 			Name:      "usage_bytes",
// 			Namespace: "log",
// 			Subsystem: "storage",
// 			Help:      "log storage usage of organization",
// 		},
// 		[]string{"x_org_name"},
// 	)
// )
// var bdl = bundle.New(bundle.WithDOP())

// // store log with cassandra
// type cassandraStorageLog struct {
// 	metricQ metricq.Queryer
// }

// func newCassandraStorageLog(metricQ metricq.Queryer) storageMetric {
// 	return &cassandraStorageLog{metricQ: metricQ}
// }

// type keyspaceUsage struct {
// 	keyspace   string
// 	address    string
// 	usageBytes uint64
// }

// func (c *cassandraStorageLog) UsageSummaryOrg() (map[string]uint64, error) {
// 	orgMap, err := c.orgKeyspaceMap()
// 	if err != nil {
// 		return nil, err
// 	}
// 	data, err := c.dataInfo()
// 	if err != nil {
// 		return nil, err
// 	}
// 	return c.calculateUsage(orgMap, data), nil
// }

// func (c *cassandraStorageLog) calculateUsage(orgMap map[string]string, data []*keyspaceUsage) map[string]uint64 {
// 	res := make(map[string]uint64, len(data))
// 	for _, item := range data {
// 		orgName, ok := orgMap[item.keyspace]
// 		if !ok {
// 			continue
// 		}
// 		res[orgName] += item.usageBytes
// 	}
// 	return res
// }

// func (c *cassandraStorageLog) orgKeyspaceMap() (map[string]string, error) {
// 	resp, err := bdl.ListDopOrgs(&apistructs.OrgSearchRequest{PageNo: 1, PageSize: math.MaxInt64})
// 	if err != nil {
// 		return nil, fmt.Errorf("get orglist failed. err: %s", err)
// 	}
// 	ret := make(map[string]string, len(resp.List))
// 	for _, item := range resp.List {
// 		ret[schema.KeyspaceWithOrgName(item.Name)] = item.Name
// 	}
// 	return ret, nil
// }

// func (c *cassandraStorageLog) dataInfo() ([]*keyspaceUsage, error) {
// 	rs, err := c.metricQ.Query(metricq.InfluxQL, tsqlLog, map[string]interface{}{"start": "before_10m", "end": "now"}, nil)
// 	if err != nil {
// 		return nil, fmt.Errorf("query metricQ failed: %w", err)
// 	}
// 	ret := make([]*keyspaceUsage, 0, len(rs.Rows))
// 	for _, row := range rs.Rows {
// 		if len(row) != 3 {
// 			continue
// 		}
// 		keyspace, ok := row[0].(string)
// 		if !ok {
// 			continue
// 		}
// 		address, ok := row[1].(string)
// 		if !ok {
// 			continue
// 		}
// 		usage, ok := row[2].(float64)
// 		if !ok {
// 			continue
// 		}

// 		ret = append(ret, &keyspaceUsage{
// 			keyspace:   keyspace,
// 			address:    address,
// 			usageBytes: uint64(usage),
// 		})
// 	}
// 	return ret, nil
// }
