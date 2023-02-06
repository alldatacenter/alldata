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

package orgapis

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/utils"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

type groupHostTypeData struct {
	Key    string      `json:"key"`
	Name   string      `json:"name"`
	Values interface{} `json:"values"`
	Unit   string      `json:"unit"`
	Prefix string      `json:"prefix"`
}

func getHostBoolQuery(orgName string, clusterNames ...interface{}) *elastic.BoolQuery {
	return elastic.NewBoolQuery().
		Filter(elastic.NewTermsQuery(tagsClusterName, clusterNames...)).
		Filter(elastic.NewTermQuery(fieldsLabels, orgPrefix+orgName)).
		MustNot(elastic.NewTermQuery(fieldsLabels, offline))
}

func (p *provider) getHostTypes(req *http.Request, params struct {
	ClusterName string `query:"clusterName" validate:"required"`
	OrgName     string `query:"orgName" validate:"required"`
}) interface{} {
	var clusterNames []interface{}
	for _, v := range strings.Split(params.ClusterName, ",") {
		clusterNames = append(clusterNames, v)
	}
	query := getHostBoolQuery(params.OrgName, clusterNames...)
	searchSource := elastic.NewSearchSource().Query(query).Size(0)
	for _, v := range []string{tagsClusterName, tagsCPUs, tagsMem, tagsHostIP, fieldsLabels} {
		searchSource.Aggregation(v, elastic.NewTermsAggregation().Field(v).Size(500))
	}
	result, err := p.metricq.SearchRaw(indexHostSummary, searchSource)
	if err != nil {
		return api.Errors.Internal(err)
	}

	if result.Aggregations == nil {
		return api.Success(make([]*groupHostTypeData, 0))
	}
	types := []*groupHostTypeData{
		{
			Key:    cpus,
			Values: parseTermsToList(result, tagsCPUs),
		},
		{
			Key:    mem,
			Values: parseTermsToList(result, tagsMem),
			Unit:   "GB",
		},
		{
			Key:    cluster,
			Values: parseTermsToList(result, tagsClusterName),
		},
		{
			Key:    host,
			Name:   "IP",
			Values: parseTermsToList(result, tagsHostIP),
		},
		{
			Key:    labels,
			Values: parseTermsToList(result, fieldsLabels),
		},
		{
			Key:    loadPercent,
			Name:   "load",
			Values: percents,
			Prefix: "load",
		},
		{
			Key:    cpuUsagePercent,
			Name:   "cpu used",
			Values: percents,
			Prefix: "cpu used",
		},
		{
			Key:    memUsagePercent,
			Name:   "mem used",
			Values: percents,
			Prefix: "mem used",
		},
		{
			Key:    diskUsagePercent,
			Name:   "disk used",
			Values: percents,
			Prefix: "disk used",
		},
		{
			Key:    cpuDispPercent,
			Name:   "cpu dispatch",
			Values: percents,
			Prefix: "cpu dispatch",
		},
		{
			Key:    memDispPercent,
			Name:   "mem dispatch",
			Values: percents,
			Prefix: "mem dispatch",
		},
	}

	lang := api.Language(req)
	for _, typ := range types {
		if len(typ.Name) <= 0 {
			typ.Name = p.t.Text(lang, typ.Key)
		} else {
			typ.Name = p.t.Text(lang, typ.Name)
		}
		if len(typ.Prefix) >= 0 {
			typ.Prefix = p.t.Text(lang, typ.Prefix)
		}
	}
	return api.Success(types)
}

func parseTermsToList(result *elastic.SearchResult, key string) (list []interface{}) {
	item, ok := result.Aggregations.Terms(key)
	if !ok {
		return nil
	}
	for _, v := range item.Buckets {
		list = append(list, v.Key)
	}
	return list
}

type hostRequest struct {
	OrgName string   `json:"org_name"`
	Hosts   []string `json:"hosts"`
}

type offlineHostRequest struct {
	ClusterName string   `json:"clusterName"`
	HostIPs     []string `json:"hostIPs"`
}

type resourceRequest struct {
	Clusters []*resourceCluster `json:"clusters"`
	Filters  []*resourceFilter  `json:"filters"`
	Groups   []string           `json:"groups"`
}

type resourceCluster struct {
	ClusterName string   `json:"clusterName"`
	HostIPs     []string `json:"hostIPs"`
}

type resourceFilter struct {
	Key    string   `json:"key"`
	Values []string `json:"values"`
}

type resourceValueFilter struct {
	key    string
	ranges []*resourceValuePair
}

type resourceValuePair struct {
	from float64
	to   float64
}

type groupHostData struct {
	Name          string           `json:"name"`
	Metric        *groupHostMetric `json:"metric"`
	Machines      []*hostData      `json:"machines"`
	Groups        []*groupHostData `json:"groups"`
	ClusterStatus uint8            `json:"clusterStatus"`
}

type groupHostMetric struct {
	Machines       int     `json:"machines"`
	CPUUsage       float64 `json:"cpuUsage"`
	CPURequest     float64 `json:"cpuRequest"`
	CPULimit       float64 `json:"cpuLimit"`
	CPUOrigin      float64 `json:"cpuOrigin"`
	CPUTotal       float64 `json:"cpuTotal"`
	CPUAllocatable float64 `json:"cpuAllocatable"`
	MemUsage       float64 `json:"memUsage"`
	MemRequest     float64 `json:"memRequest"`
	MemLimit       float64 `json:"memLimit"`
	MemOrigin      float64 `json:"memOrigin"`
	MemTotal       float64 `json:"memTotal"`
	MemAllocatable float64 `json:"memAllocatable"`
	DiskUsage      float64 `json:"diskUsage"`
	DiskTotal      float64 `json:"diskTotal"`
}

type hostData struct {
	ClusterName      string  `json:"clusterName"`
	IP               string  `json:"ip"`
	Hostname         string  `json:"hostname"`
	OS               string  `json:"os"`
	KernelVersion    string  `json:"kernelVersion"`
	Labels           string  `json:"labels"`
	Tasks            float64 `json:"tasks"`
	CPUUsage         float64 `json:"cpuUsage"`
	CPURequest       float64 `json:"cpuRequest"`
	CPULimit         float64 `json:"cpuLimit"`
	CPUOrigin        float64 `json:"cpuOrigin"`
	CPUTotal         float64 `json:"cpuTotal"`
	CPUAllocatable   float64 `json:"cpuAllocatable"`
	MemUsage         float64 `json:"memUsage"`
	MemRequest       float64 `json:"memRequest"`
	MemLimit         float64 `json:"memLimit"`
	MemOrigin        float64 `json:"memOrigin"`
	MemTotal         float64 `json:"memTotal"`
	MemAllocatable   float64 `json:"memAllocatable"`
	DiskUsage        float64 `json:"diskUsage"`
	DiskLimit        float64 `json:"diskLimit"`
	DiskTotal        float64 `json:"diskTotal"`
	Load1            float64 `json:"load1"`
	Load5            float64 `json:"load5"`
	Load15           float64 `json:"load15"`
	CPUUsagePercent  float64 `json:"cpuUsagePercent"`
	MemUsagePercent  float64 `json:"memUsagePercent"`
	DiskUsagePercent float64 `json:"diskUsagePercent"`
	LoadPercent      float64 `json:"loadPercent"`
	CPUDispPercent   float64 `json:"cpuDispPercent"`
	MemDispPercent   float64 `json:"memDispPercent"`
}

type containerData struct {
	ClusterName     string  `json:"clusterName"`
	HostIP          string  `json:"hostIP"`
	ContainerID     string  `json:"containerId"`
	InstanceType    string  `json:"instanceType"`
	InstanceID      string  `json:"instanceId"`
	Image           string  `json:"image"`
	Count           int64   `json:"count"`
	OrgID           string  `json:"orgId"`
	OrgName         string  `json:"orgName"`
	ProjectID       string  `json:"projectId"`
	ProjectName     string  `json:"projectName"`
	ApplicationID   string  `json:"applicationId"`
	ApplicationName string  `json:"applicationName"`
	Workspace       string  `json:"workspace"`
	RuntimeID       string  `json:"runtimeId"`
	RuntimeName     string  `json:"runtimeName"`
	ServiceID       string  `json:"serviceId"`
	ServiceName     string  `json:"serviceName"`
	JobID           string  `json:"jobId"`
	CpuUsage        float64 `json:"cpuUsage"`
	CpuRequest      float64 `json:"cpuRequest"`
	CpuLimit        float64 `json:"cpuLimit"`
	CpuOrigin       float64 `json:"cpuOrigin"`
	MemUsage        float64 `json:"memUsage"`
	MemRequest      float64 `json:"memRequest"`
	MemLimit        float64 `json:"memLimit"`
	MemOrigin       float64 `json:"memOrigin"`
	DiskUsage       float64 `json:"diskUsage"`
	DiskLimit       float64 `json:"diskLimit"`
	Status          string  `json:"status"`
	Container       string  `json:"container"`
	PodUid          string  `json:"podUid"`
	PodName         string  `json:"podName"`
	PodNamespace    string  `json:"podNamespace"`
}

type resourceChart struct {
	Title   string                 `json:"title"`
	Total   int64                  `json:"total"`
	Time    []int64                `json:"time"`
	Results []*resourceChartResult `json:"results"`
}

type resourceChartResult struct {
	Name string                          `json:"name"`
	Data []map[string]*resourceChartData `json:"data"`
}

type resourceChartData struct {
	Tag       string    `json:"tag"`
	Name      string    `json:"name"`
	Data      []float64 `json:"data"`
	Unit      string    `json:"unit"`
	UnitType  string    `json:"unitType"`
	ChartType string    `json:"chartType"`
	AxisIndex int       `json:"axisIndex"`
}

func (p *provider) getGroupHosts(req *http.Request, params struct {
	OrgName string `query:"orgName" validate:"required" json:"-"`
}, res resourceRequest) interface{} {
	var clusterNames []interface{}
	for _, cluster := range res.Clusters {
		clusterNames = append(clusterNames, cluster.ClusterName)
	}
	query := getHostBoolQuery(params.OrgName, clusterNames...)

	vfs := wrapGroupHostFilter(res.Filters, query)

	searchSource := elastic.NewSearchSource().
		Query(query).Size(0).
		Aggregation(createGroupHostAgg(res.Groups, 0))

	result, err := p.metricq.SearchRaw(indexHostSummary, searchSource)
	if err != nil {
		return api.Errors.Internal(err)
	}
	if result == nil {
		return api.Success(nil)
	}
	groups := parseGroupHost(result.Aggregations, res.Groups, 0, vfs)
	p.updateClusterStatus(groups)
	return api.Success(groups)
}

func (p *provider) updateClusterStatus(group *groupHostData) {
	if group == nil {
		return
	}
	for _, g := range group.Groups {
		s, _ := p.getComponentStatus(g.Name)
		status := p.createStatusResp(s)
		g.ClusterStatus = status.Status
	}
}

func wrapGroupHostFilter(filters []*resourceFilter, query *elastic.BoolQuery) []*resourceValueFilter {
	var vfs []*resourceValueFilter
	for _, filter := range filters {
		key := convertKey(filter.Key)
		if key == fieldsLabels {
			for _, v := range filter.Values {
				query.Filter(elastic.NewTermQuery(fieldsLabels, v))
			}
		} else {
			var values []interface{}
			vf := new(resourceValueFilter)
			for _, value := range filter.Values {
				if strings.HasPrefix(value, ">=") {
					val := value[2:]
					from, err := convertFilterPairValue(val)
					if err != nil {
						continue
					}
					pair := new(resourceValuePair)
					pair.from = from
					vf.ranges = append(vf.ranges, pair)
				} else if strings.Contains(value, "-") {
					vs := strings.Split(value, "-")
					val := vs[0]
					from, err := convertFilterPairValue(val)
					if err != nil {
						// when value is not a range, like xxx-prod
						values = append(values, value)
						continue
					}
					pair := new(resourceValuePair)
					pair.from = from
					if len(vs) > 1 {
						val = vs[1]
						to, err := convertFilterPairValue(val)
						if err != nil {
							// when value is not a range, like xxx-prod
							values = append(values, value)
							continue
						}
						pair.to = to
					}
					vf.ranges = append(vf.ranges, pair)
				} else {
					values = append(values, value)
				}
			}
			if len(vf.ranges) > 0 {
				vf.key = filter.Key
				vfs = append(vfs, vf)
			}
			if len(values) > 0 {
				query.Filter(elastic.NewTermsQuery(key, values...))
			}
		}
	}
	return vfs
}

func convertKey(key string) string {
	if key == labels {
		return fieldsLabels
	}
	if key == host {
		key = hostIP
	} else if key == cluster {
		key = clusterName
	}
	return tagsPrefix + key
}

func convertFilterPairValue(val string) (float64, error) {
	if val == "" {
		return 0, nil
	}
	if strings.HasSuffix(val, "%") {
		val = val[0 : len(val)-1]
	}
	return strconv.ParseFloat(val, 64)
}

func createGroupHostAgg(groups []string, index int) (string, elastic.Aggregation) {
	if index == len(groups) {
		topHitsAgg := elastic.NewTopHitsAggregation().Size(1).
			Sort(timestamp, false).
			FetchSourceContext(elastic.NewFetchSourceContext(true).Include(any))
		return tagsHostIP, elastic.NewTermsAggregation().Field(tagsHostIP).Size(500).
			SubAggregation(tagsTerminusVersion,
				elastic.NewTermsAggregation().Field(tagsTerminusVersion).Size(100).SubAggregation(topHits, topHitsAgg))
	}

	key := convertKey(groups[index])
	agg := elastic.NewTermsAggregation().Field(key).Size(500).
		SubAggregation(createGroupHostAgg(groups, index+1))
	return key, agg
}

func parseGroupHost(agg elastic.Aggregations, groups []string, index int, vfs []*resourceValueFilter) *groupHostData {
	group := new(groupHostData)
	if index == len(groups) {
		// 机器
		hostsData := parseHostData(agg, vfs)
		group.Machines = hostsData

		metric := new(groupHostMetric)
		for _, hostData := range hostsData {
			metric.Machines++
			metric.CPUUsage += hostData.CPUUsage
			metric.CPURequest += hostData.CPURequest
			metric.CPULimit += hostData.CPULimit
			metric.CPUOrigin += hostData.CPUOrigin
			metric.CPUTotal += hostData.CPUTotal
			metric.CPUAllocatable += hostData.CPUAllocatable
			metric.MemUsage += hostData.MemUsage
			metric.MemRequest += hostData.MemRequest
			metric.MemLimit += hostData.MemLimit
			metric.MemOrigin += hostData.MemOrigin
			metric.MemTotal += hostData.MemTotal
			metric.MemAllocatable += hostData.MemAllocatable
			metric.DiskUsage += hostData.DiskUsage
			metric.DiskTotal += hostData.DiskTotal
		}
		group.Metric = metric
	} else {
		// 机器聚合
		key := convertKey(groups[index])
		terms, ok := agg.Terms(key)
		if !ok {
			return nil
		}
		var innerGroups []*groupHostData
		for _, item := range terms.Buckets {
			innerGroup := parseGroupHost(item.Aggregations, groups, index+1, vfs)
			if innerGroup == nil {
				continue
			}
			innerGroup.Name, _ = item.Key.(string)
			innerGroups = append(innerGroups, innerGroup)
		}
		if len(innerGroups) == 0 {
			return nil
		}
		group.Groups = innerGroups

		metric := new(groupHostMetric)
		for _, innerGroup := range innerGroups {
			metric.Machines += innerGroup.Metric.Machines
			metric.CPUUsage += innerGroup.Metric.CPUUsage
			metric.CPURequest += innerGroup.Metric.CPURequest
			metric.CPULimit += innerGroup.Metric.CPULimit
			metric.CPUOrigin += innerGroup.Metric.CPUOrigin
			metric.CPUTotal += innerGroup.Metric.CPUTotal
			metric.CPUAllocatable += innerGroup.Metric.CPUAllocatable
			metric.MemUsage += innerGroup.Metric.MemUsage
			metric.MemRequest += innerGroup.Metric.MemRequest
			metric.MemLimit += innerGroup.Metric.MemLimit
			metric.MemOrigin += innerGroup.Metric.MemOrigin
			metric.MemTotal += innerGroup.Metric.MemTotal
			metric.MemAllocatable += innerGroup.Metric.MemAllocatable
			metric.DiskUsage += innerGroup.Metric.DiskUsage
			metric.DiskTotal += innerGroup.Metric.DiskTotal
		}
		group.Metric = metric
	}
	return group
}

func parseHostData(agg elastic.Aggregations, vfs []*resourceValueFilter) []*hostData {
	if agg == nil {
		return nil
	}

	hostIPAgg, ok := agg.Terms(tagsHostIP)
	if !ok {
		return nil
	}
	var hosts []*hostData
	for _, hostIPItem := range hostIPAgg.Buckets {
		var topHitsAgg *elastic.AggregationTopHitsMetric
		if versionAgg, ok := hostIPItem.Terms(tagsTerminusVersion); ok {
			var maxVersion int64
			for _, versionItem := range versionAgg.Buckets {
				if version, err := versionItem.KeyNumber.Int64(); err != nil {
					continue
				} else if version > maxVersion {
					topHitsAgg, ok = versionItem.Aggregations.TopHits(topHits)
					if !ok {
						continue
					}
					maxVersion = version
				}
			}
		}
		data := wrapHostsData(topHitsAgg, vfs)
		hosts = append(hosts, data...)
	}
	return hosts
}

func wrapHostsData(topHits *elastic.AggregationTopHitsMetric, vfs []*resourceValueFilter) []*hostData {
	if topHits == nil || topHits.Hits == nil {
		return nil
	}

	var hostsData []*hostData
	for _, hit := range topHits.Hits.Hits {
		var m map[string]interface{}
		if err := json.Unmarshal([]byte(*hit.Source), &m); err != nil {
			continue
		}

		tags, ok := utils.GetMapValueMap(m, tags)
		if !ok {
			continue
		}
		fields, ok := utils.GetMapValueMap(m, fields)
		if !ok {
			continue
		}

		hostData := new(hostData)
		hostData.ClusterName, _ = utils.GetMapValueString(tags, clusterName)
		hostData.IP, _ = utils.GetMapValueString(tags, hostIP)
		hostData.Hostname, _ = utils.GetMapValueString(tags, host)
		hostData.Labels, _ = utils.GetMapValueString(tags, labels)
		if idx := strings.Index(hostData.Labels, "="); idx >= 0 {
			hostData.Labels = hostData.Labels[idx+1:]
			if idx = strings.Index(hostData.Labels, ":"); idx >= 0 {
				hostData.Labels = hostData.Labels[idx+1:]
			}
		}
		hostData.OS, _ = utils.GetMapValueString(tags, os)
		hostData.KernelVersion, _ = utils.GetMapValueString(tags, kernelVersion)
		hostData.Tasks, _ = utils.GetMapValueFloat64(fields, tasks)

		hostData.CPUUsage, _ = utils.GetMapValueFloat64(fields, cpuUsage)
		hostData.CPURequest, _ = utils.GetMapValueFloat64(fields, cpuRequest)
		hostData.CPULimit, _ = utils.GetMapValueFloat64(fields, cpuLimit)
		hostData.CPUOrigin, _ = utils.GetMapValueFloat64(fields, cpuOrigin)
		hostData.CPUTotal, _ = utils.GetMapValueFloat64(fields, cpuTotal)
		hostData.CPUAllocatable, _ = utils.GetMapValueFloat64(fields, "cpu_allocatable")
		hostData.CPUUsagePercent, _ = utils.GetMapValueFloat64(fields, cpuUsagePercent)
		hostData.CPUDispPercent, _ = utils.GetMapValueFloat64(fields, cpuDispPercent)

		hostData.MemUsage, _ = utils.GetMapValueFloat64(fields, memUsage)
		hostData.MemRequest, _ = utils.GetMapValueFloat64(fields, memRequest)
		hostData.MemLimit, _ = utils.GetMapValueFloat64(fields, memLimit)
		hostData.MemOrigin, _ = utils.GetMapValueFloat64(fields, memOrigin)
		hostData.MemTotal, _ = utils.GetMapValueFloat64(fields, memTotal)
		hostData.MemAllocatable, _ = utils.GetMapValueFloat64(fields, "mem_allocatable")
		hostData.MemUsagePercent, _ = utils.GetMapValueFloat64(fields, memUsagePercent)
		hostData.MemDispPercent, _ = utils.GetMapValueFloat64(fields, memDispPercent)

		hostData.DiskUsage, _ = utils.GetMapValueFloat64(fields, diskUsage)
		hostData.DiskLimit, _ = utils.GetMapValueFloat64(fields, diskLimit)
		hostData.DiskTotal, _ = utils.GetMapValueFloat64(fields, diskTotal)
		hostData.DiskUsagePercent, _ = utils.GetMapValueFloat64(fields, diskUsagePercent)

		hostData.Load1, _ = utils.GetMapValueFloat64(fields, load1)
		hostData.Load5, _ = utils.GetMapValueFloat64(fields, load5)
		hostData.Load15, _ = utils.GetMapValueFloat64(fields, load15)
		hostData.LoadPercent, _ = utils.GetMapValueFloat64(fields, loadPercent)

		if ok := checkHostValue(fields, vfs); ok {
			hostsData = append(hostsData, hostData)
		}
	}
	return hostsData
}

func checkHostValue(m map[string]interface{}, vfs []*resourceValueFilter) bool {
	if len(vfs) == 0 {
		return true
	}

	for _, vf := range vfs {
		value, ok := utils.GetMapValueFloat64(m, vf.key)
		if !ok {
			return false
		}
		for _, r := range vf.ranges {
			if r.to == 0 {
				if value > r.from {
					return true
				}
			} else {
				if value > r.from && value < r.to {
					return true
				}
			}
		}
	}
	return false
}

func (p *provider) getHostStatus(req hostRequest) interface{} {
	res := make([]map[string]string, 0)
	for _, host := range req.Hosts {
		res = append(res, map[string]string{
			"host_ip":      host,
			"status_level": "normal",
			"abnormal_msg": "",
		})
	}
	return api.Success(res)
}

func (p *provider) offlineHost(req offlineHostRequest) interface{} {
	query := elastic.NewBoolQuery().
		Filter(elastic.NewTermQuery(tagsClusterName, req.ClusterName)).
		Filter(elastic.NewTermsQuery(tagsHostIP, utils.ConvertStringArrToInterfaceArr(req.HostIPs)...)).
		Filter(elastic.NewRangeQuery(timestamp).Lte(time.Now().UnixNano())).
		MustNot(elastic.NewTermQuery(fieldsLabels, offline))

	go p.doOfflineHost(query)
	p.L.Info("offline %s host: %s", req.ClusterName, strings.Join(req.HostIPs, ","))
	return api.Success(nil)
}

func (p *provider) doOfflineHost(query elastic.Query) {
	timeout := fmt.Sprintf("%dms", p.C.OfflineTimeout.Milliseconds())
	for i := 0; i < 5; i++ {
		searchSource := elastic.NewSearchSource().Query(query).Size(100)
		resp, apiErr := p.metricq.SearchRaw(indexHostSummary, searchSource)
		if apiErr != nil {
			p.L.Errorf("offline host: Search error: %s\n", apiErr)
			return
		}
		if resp == nil {
			p.L.Errorf("offline host: Search resp is nil\n")
			return
		}
		if resp.TotalHits() == 0 {
			p.L.Info("offline host: Search resp has no hits\n")
			return
		}
		var requests []elastic.BulkableRequest
		for _, hit := range resp.Hits.Hits {
			var m map[string]interface{}
			if err := json.Unmarshal([]byte(*hit.Source), &m); err != nil {
				p.L.Errorf("offline host: json unmarshal hit source error: %s\n", err)
				continue
			}
			fields, ok := utils.GetMapValueMap(m, fields)
			if !ok {
				p.L.Errorf("offline host: source has no fields map\n")
				continue
			}
			labelList, ok := utils.GetMapValueArr(fields, labels)
			if !ok {
				p.L.Errorf("offline host: fields has labels arr\n")
				continue
			}
			labelList = append(labelList, offline)
			fields[labels] = labelList
			request := elastic.NewBulkIndexRequest().Index(hit.Index).Type("spot").Doc(m).Id(hit.Id)
			requests = append(requests, request)
		}
		res, err := p.metricq.Client().Bulk().Add(requests...).
			Timeout(timeout).Do(context.Background())
		if err != nil {
			p.L.Errorf("offline host: send to es error: %s\n", err)
			return
		}
		if res != nil && res.Errors {
			for _, item := range res.Failed() {
				if item.Error == nil {
					continue
				}
				p.L.Errorf("offline host: Failure index data. [%s][%s] %s : %s %v\n", item.Index, item.Type, item.Error.Type, item.Error.Reason, item.Error.CausedBy)
			}
		}
		time.Sleep(p.C.OfflineSleep)
	}
}
