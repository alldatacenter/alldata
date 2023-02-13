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

package metrics

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/time/rate"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/cmp/cache"
)

type ResourceType string

const (
	// SELECT host_ip::tag, mem_used::field FROM host_summary WHERE cluster_name::tag=$cluster_name
	// usage rate , distribution rate , usage percent of distribution
	//NodeCpuUsageSelectStatement    = `SELECT last(cpu_cores_usage::field) FROM host_summary WHERE cluster_name::tag=$cluster_name AND host_ip::tag=$host_ip `
	NodeResourceUsageSelectStatement = `SELECT last(mem_used::field) as memRate , last(cpu_cores_usage::field) as cpuRate , host_ip::tag FROM host_summary WHERE cluster_name::tag=$cluster_name GROUP BY host_ip::tag limit 100000`
	//NodeResourceUsageSelectStatement = `SELECT  mem_usage::field  ,cpu_cores_usage::field, host_ip FROM host_summary WHERE cluster_name::tag=$cluster_name GROUP BY host_ip::tag`
	//PodCpuUsageSelectStatement     = `SELECT SUM(cpu_allocation::field) * 100 / SUM(cpu_limit::field) as cpuRate, pod_name FROM docker_container_summary WHERE pod_namespace::tag=$pod_namespace and podsandbox != true GROUP BY pod_name::tag`
	PodResourceUsageSelectStatement = `SELECT round_float(SUM(mem_usage::field) * 100 / SUM(mem_limit::field),2) as memoryRate,round_float(SUM(cpu_usage_percent::field) / SUM(cpu_limit::field) ,2) as cpuRate ,pod_name::tag ,pod_namespace::tag FROM docker_container_summary WHERE  cluster_name::tag=$cluster_name and podsandbox != true GROUP BY pod_name::tag, pod_namespace::tag limit 100000 `

	DiskResourceUsageSelectStatement = `SELECT last(used::field) as diskUsed , cluster_name::tag FROM disk WHERE org_name::tag = $org_name GROUP BY cluster_name::tag limit 100000`

	Memory  = "memory"
	Cpu     = "cpu"
	Disk    = "disk"
	NodeAll = "nodeall" // cpu + disk + memory

	Pod  = "pod"
	Node = "node"

	nodeAndPodSyncKey = "nodeAndPodSyncMetricsCacheSync"
	diskSyncKey       = "diskMetricsCacheSync"

	queryTimeout = 30 * time.Second
)

var (
	ResourceNotSupport = errors.New("resource type not support")
	QueryTimeoutError  = errors.New("metrics query timeout")
	ParameterNotFound  = errors.New("parameter required")
)

type Metric struct {
	ctx           context.Context
	Metricq       pb.MetricServiceServer
	metricReqChan chan *MetricsReq
	limiter       *rate.Limiter
}

type MetricsReq struct {
	rawReq  *pb.QueryWithInfluxFormatRequest
	sync    bool
	resType string
	resKind string
}

func New(metricq pb.MetricServiceServer, ctx context.Context) *Metric {
	m := &Metric{}
	m.ctx = ctx
	m.Metricq = metricq
	m.metricReqChan = make(chan *MetricsReq, 1024)
	burst := 100
	if size, err := strconv.Atoi(os.Getenv("METRICS_QUEUE_SIZE")); err == nil && size > burst {
		burst = size
	}
	m.limiter = rate.NewLimiter(rate.Every(time.Millisecond), burst)
	go m.queryAsync()
	return m
}

func (m *Metric) queryAsync() {
	for {
		err := m.limiter.Wait(m.ctx)
		if err != nil {
			return
		}
		select {
		case req := <-m.metricReqChan:
			if req == nil {
				continue
			} else {
				//metricReq := m.mergeReq(reqs)
				resp, err := m.query(m.ctx, req.rawReq)
				if err != nil || resp == nil {
					logrus.Errorf("query metrics err,%v", err)
				} else {
					m.Store(resp, req)
				}
			}
		}
	}
}

type Interface interface {
	NodeMetrics(ctx context.Context, req *MetricsRequest) (map[string]*MetricsData, error)
	PodMetrics(ctx context.Context, req *MetricsRequest) (map[string]*MetricsData, error)
	NodeAllMetrics(ctx context.Context, req *MetricsRequest) (map[string]*MetricsData, error)
}

func isEmptyResponse(resp *pb.QueryWithInfluxFormatResponse) bool {
	if resp == nil || len(resp.Results) == 0 ||
		len(resp.Results[0].Series) == 0 || len(resp.Results[0].Series[0].Rows) == 0 ||
		len(resp.Results[0].Series[0].Rows[0].Values) == 0 {
		return true
	}
	return false
}

func (m *Metric) query(ctx context.Context, req *pb.QueryWithInfluxFormatRequest) (*pb.QueryWithInfluxFormatResponse, error) {
	return m.Metricq.QueryWithInfluxFormat(ctx, req)
}

// querySync returns influxdb data
func (m *Metric) querySync(ctx context.Context, req *MetricsReq, c chan map[string]*MetricsData) {
	var (
		res  = make(map[string]*MetricsData)
		resp *pb.QueryWithInfluxFormatResponse
		err  error
	)
	if req == nil {
		c <- res
		return
	}
	if !req.sync {
		logrus.Infof("cache expired, try fetch metrics asynchronized")
		//asyncReqs = append(asyncReqs, metricsReq)
		select {
		case m.metricReqChan <- req:
		default:
			logrus.Warnf("channel is blocked, ayscn query skip")
		}
		c <- res
		return
	}
	//metricsReq := m.mergeReq(syncReqs)
	err = m.limiter.Wait(ctx)
	if err != nil {
		logrus.Errorf("limiter error:%v", err)
		c <- res
		return
	}
	resp, err = m.query(ctx, req.rawReq)
	if err != nil || resp == nil {
		logrus.Errorf("metrics query error:%v, resp:%v", err, resp)
	} else {
		res = m.Store(resp, req)
	}
	c <- res
}

func (m *Metric) Store(response *pb.QueryWithInfluxFormatResponse, metricsRequest *MetricsReq) map[string]*MetricsData {
	if !isEmptyResponse(response) {
		var (
			k      = ""
			allKey = ""
			d      *MetricsData
			res    = make(map[string]*MetricsData)
		)
		for _, row := range response.Results[0].Series[0].Rows {
			switch metricsRequest.resKind {
			case Pod:
				k = cache.GenerateKey(Pod, Cpu, row.Values[3].GetStringValue(), metricsRequest.rawReq.Params["cluster_name"].GetStringValue(), row.Values[2].GetStringValue())
				if row.Values[1].GetKind() != nil {
					d = &MetricsData{
						Used: row.Values[1].GetNumberValue(),
					}
					SetCache(k, d)
					res[k] = d
				}
				if row.Values[0].GetKind() != nil {
					k = cache.GenerateKey(Pod, Memory, row.Values[3].GetStringValue(), metricsRequest.rawReq.Params["cluster_name"].GetStringValue(), row.Values[2].GetStringValue())
					d = &MetricsData{
						Used: row.Values[0].GetNumberValue(),
					}
					SetCache(k, d)
					res[k] = d
				}
			case Node:
				if row.Values[1].GetKind() != nil && row.Values[2].GetKind() != nil {
					k = cache.GenerateKey(Node, Cpu, metricsRequest.rawReq.Params["cluster_name"].GetStringValue(), row.Values[2].GetStringValue())
					d = &MetricsData{
						Used: row.Values[1].GetNumberValue(),
					}
					SetCache(k, d)
					res[k] = d
				}
				if row.Values[0].GetKind() != nil && row.Values[2].GetKind() != nil {
					k = cache.GenerateKey(Node, Memory, metricsRequest.rawReq.Params["cluster_name"].GetStringValue(), row.Values[2].GetStringValue())
					d = &MetricsData{
						Used: row.Values[0].GetNumberValue(),
					}
					SetCache(k, d)
					res[k] = d
				}
			case NodeAll:
				if row.Values[0].GetKind() != nil {
					k = cache.GenerateKey(Node, Disk, metricsRequest.rawReq.Params["org_name"].GetStringValue(), row.Values[1].GetStringValue())
					d = &MetricsData{
						Used: row.Values[0].GetNumberValue(),
					}
					SetCache(k, d)
					res[k] = d
				}
			}
		}
		switch metricsRequest.resKind {
		case Node:
			allKey = GenerateNodeAllKey(metricsRequest.rawReq.Params["cluster_name"].GetStringValue())
		case Pod:
			allKey = GeneratePodAllKey(metricsRequest.rawReq.Params["cluster_name"].GetStringValue())
		case NodeAll:
			allKey = GenerateNodeallAllKey(metricsRequest.rawReq.Params["org_name"].GetStringValue())
		}
		if allKey != "" {
			logrus.Infof("set all cache %s %v", allKey, res)
			SetCache(allKey, res)
		}
		return res
	}
	return nil
}

func GenerateNodeAllKey(cluster string) string {
	return cache.GenerateKey(cluster, Node)
}

func GeneratePodAllKey(cluster string) string {
	return cache.GenerateKey(cluster, Pod)
}

func GenerateNodeallAllKey(orgName string) string {
	return cache.GenerateKey(orgName, Disk)
}

func SetCache(k string, d interface{}) {
	data, err := cache.GetInterfaceValue(d)
	if err != nil {
		logrus.Errorf("cache marshal metrics %v err: %v", k, err)
	} else {
		err := cache.GetFreeCache().Set(k, data, int64(time.Second*30))
		if err != nil {
			logrus.Errorf("cache set metrics %v err: %v", k, err)
		}
	}
}

func GetCache(key string) *MetricsData {
	v, _, err := cache.GetFreeCache().Get(key)
	if err != nil {
		logrus.Errorf("get metrics %v err :%v", key, err)
		return nil
	}
	var (
		d  *MetricsData
		ok bool
	)
	if v != nil {
		d, ok = v[0].Value().(*MetricsData)
		if !ok {
			logrus.Errorf("get metrics %v assert err", key)
		}
	}
	return d
}

func GetAllCache(key string) map[string]*MetricsData {
	v, _, err := cache.GetFreeCache().Get(key)
	if err != nil {
		logrus.Errorf("get metrics %v err :%v", key, err)
		return nil
	}
	var (
		d  = map[string]*MetricsData{}
		ok bool
	)
	if v != nil {
		d, ok = v[0].Value().(map[string]*MetricsData)
		if !ok {
			logrus.Errorf("get metrics %v assert err", key)
		}
	}
	return d
}

// NodeMetrics query cpu and memory metrics from es database, return immediately if cache hit.
func (m *Metric) NodeMetrics(ctx context.Context, req *MetricsRequest) (map[string]*MetricsData, error) {
	metricsReq, noNeed, err := m.ToInfluxReq(req, Node)
	if metricsReq == nil || err != nil {
		return noNeed, err
	}
	ctx2, cancelFunc := context.WithTimeout(ctx, queryTimeout)
	defer cancelFunc()
	logrus.Infof("qeury node metrics with timeout")
	c := make(chan map[string]*MetricsData)
	go m.querySync(ctx2, metricsReq[0], c)
	select {
	case <-ctx2.Done():
		return nil, QueryTimeoutError
	case resp := <-c:
		for key, v := range resp {
			noNeed[key] = v
		}
	}
	return noNeed, nil
}

func (m *Metric) PodMetrics(ctx context.Context, req *MetricsRequest) (map[string]*MetricsData, error) {
	metricsReq, noNeed, err := m.ToInfluxReq(req, Pod)
	if metricsReq == nil || err != nil {
		return noNeed, err
	}
	ctx2, cancelFunc := context.WithTimeout(ctx, queryTimeout)
	defer cancelFunc()
	logrus.Infof("query pod metrics with timeout")
	c := make(chan map[string]*MetricsData)
	go m.querySync(ctx2, metricsReq[0], c)
	select {
	case <-ctx2.Done():
		return nil, QueryTimeoutError
	case resp := <-c:
		for key, v := range resp {
			noNeed[key] = v
		}
	}
	return noNeed, nil
}

func (m *Metric) NodeAllMetrics(ctx context.Context, req *MetricsRequest) (map[string]*MetricsData, error) {
	metricsReq, noNeed, err := m.ToInfluxReq(req, NodeAll)
	res := make(map[string]*MetricsData)
	res[req.Cluster+Cpu] = &MetricsData{}
	res[req.Cluster+Memory] = &MetricsData{}
	res[req.Cluster+Disk] = &MetricsData{}
	res = m.mergeRes(req.Cluster, noNeed, res)

	if metricsReq == nil || len(metricsReq) == 0 || err != nil {
		return res, err
	}
	ctx2, cancelFunc := context.WithTimeout(ctx, queryTimeout)
	defer cancelFunc()
	logrus.Infof("query cluster metrics with timeout")
	c := make(chan map[string]*MetricsData)
	for _, mreq := range metricsReq {
		go m.querySync(ctx2, mreq, c)
	}
	for i := 0; i < len(metricsReq); i++ {
		select {
		case <-ctx2.Done():
			return nil, QueryTimeoutError
		case resp := <-c:
			res = m.mergeRes(req.Cluster, resp, res)
		}
	}
	return res, nil
}

// mergeRes only Nodeall used
func (m *Metric) mergeRes(cluster string, ma map[string]*MetricsData, res map[string]*MetricsData) map[string]*MetricsData {
	if ma == nil {
		return res
	}
	for s, data := range ma {
		if strings.Contains(s, cluster) {
			if strings.Contains(s, Cpu) {
				res[cluster+Cpu].Used += data.Used
			}
			if strings.Contains(s, Memory) {
				res[cluster+Memory].Used += data.Used
			}
			if strings.Contains(s, Disk) {
				res[cluster+Disk].Used += data.Used
			}
		}
	}
	return res
}

func (m *Metric) ToInfluxReq(request *MetricsRequest, reqKind string) ([]*MetricsReq, map[string]*MetricsData, error) {
	cluster := request.ClusterName()
	orgName := request.OrgName()
	var reqs []*MetricsReq
	var noNeed = map[string]*MetricsData{}
	if request.Cluster == "" {
		return nil, nil, errors.New(fmt.Sprintf("parameter %s not found", request.Cluster))
	}
	switch reqKind {
	case Node:
		req, err := m.toNodeReq(cluster, request.ResourceType(), request.ResourceKind(), NodeResourceUsageSelectStatement)
		if err != nil {
			return nil, noNeed, err
		}
		if req == nil || !req.sync {
			key := GenerateNodeAllKey(cluster)
			noNeed = GetAllCache(key)
		}
		reqs = append(reqs, req)
		return reqs, noNeed, err
	case Pod:
		req, err := m.toNodeReq(cluster, request.ResourceType(), request.ResourceKind(), PodResourceUsageSelectStatement)
		if err != nil {
			return nil, noNeed, err
		}
		if req == nil || !req.sync {
			key := GeneratePodAllKey(cluster)
			noNeed = GetAllCache(key)
		}
		reqs = append(reqs, req)
		return reqs, noNeed, err
	case NodeAll:
		req, err := m.toNodeReq(cluster, request.ResourceType(), Node, NodeResourceUsageSelectStatement)
		if err != nil {
			return nil, noNeed, err
		}
		if req == nil || !req.sync {
			key := GenerateNodeAllKey(cluster)
			noNeed = GetAllCache(key)
		}
		reqs = append(reqs, req)
		req2, err := m.toClusterReq(orgName, request.ResourceType(), NodeAll, DiskResourceUsageSelectStatement)
		if err != nil {
			return nil, noNeed, err
		}
		if req2 == nil || !req.sync {
			key := GenerateNodeallAllKey(orgName)
			ma := GetAllCache(key)
			for k, v := range ma {
				noNeed[k] = v
			}
		}
		reqs = append(reqs, req2)
		return reqs, noNeed, err
	default:
		logrus.Errorf("query metrics kind %v, %v", reqKind, ResourceNotSupport)
		return nil, nil, ResourceNotSupport
	}
}

//
//func (m *Metric) ToInfluxReqs(request *MetricsRequest, reqKind string) ([]*MetricsReq, map[string]*MetricsData, error) {
//	cluster := request.ClusterName()
//	if request.Cluster == "" {
//		return nil, nil, errors.New(fmt.Sprintf("parameter %s not found", request.Cluster))
//	}
//	switch reqKind {
//	case Disk:
//		return m.toNodeReq(request.NodeRequests, cluster, request.ResourceType(), request.ResourceKind(), NodeResourceUsageSelectStatement)
//	case Pod:
//		return m.toNodeReq(request.PodRequests, cluster, request.ResourceType(), request.ResourceKind(), PodResourceUsageSelectStatement)
//	default:
//		logrus.Errorf("query metrics kind %v, %v", reqKind, ResourceNotSupport)
//		return nil, nil, ResourceNotSupport
//	}
//}

func (m *Metric) toNodeReq(clusterName, resType, resKind string, sql string) (*MetricsReq, error) {
	var queryReqs *MetricsReq
	if v, expired, err := cache.GetFreeCache().Get(nodeAndPodSyncKey); err == nil {
		if v == nil || expired {
			queryReq := &pb.QueryWithInfluxFormatRequest{}
			queryReq.Start = "before_5m"
			queryReq.End = "now"
			queryReq.Statement = sql
			// cluster_name is unique in different org?
			queryReq.Params = map[string]*structpb.Value{
				"cluster_name": structpb.NewStringValue(clusterName),
			}
			s := false
			if v == nil {
				syncV, err := cache.GetBoolValue(true)
				if err != nil {
					return nil, err
				}
				err = cache.GetFreeCache().Set(nodeAndPodSyncKey, syncV, int64(queryTimeout))
				if err != nil {
					return nil, err
				}
				s = true
			}
			queryReqs = &MetricsReq{rawReq: queryReq, sync: s, resType: resType, resKind: resKind}
		}
	} else {
		logrus.Errorf("get %s %s cache error,%v", clusterName, resType, err)
	}
	return queryReqs, nil
}

func (m *Metric) toClusterReq(orgName, resType, resKind string, sql string) (*MetricsReq, error) {
	var queryReqs *MetricsReq
	if v, expired, err := cache.GetFreeCache().Get(diskSyncKey); err == nil {
		if v != nil && !expired {
			return nil, nil
		} else {
			queryReq := &pb.QueryWithInfluxFormatRequest{}
			queryReq.Start = "before_5m"
			queryReq.End = "now"
			queryReq.Statement = sql
			queryReq.Params = map[string]*structpb.Value{
				"org_name": structpb.NewStringValue(orgName),
			}
			s := false
			if v == nil {
				syncV, err := cache.GetBoolValue(true)
				if err != nil {
					return nil, err
				}
				err = cache.GetFreeCache().Set(diskSyncKey, syncV, int64(queryTimeout))
				if err != nil {
					return nil, err
				}
				s = true
			}
			queryReqs = &MetricsReq{rawReq: queryReq, sync: s, resType: resType, resKind: resKind}
		}
	} else {
		logrus.Errorf("get %s %s cache error,%v", orgName, resType, err)
	}
	return queryReqs, nil
}
