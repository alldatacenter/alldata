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

package resource

import (
	"context"
	"errors"
	"fmt"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httputil"
)

const (
	CPU     = "cpu"
	Mem     = "mem"
	Memory  = "memory"
	Owner   = "owner"
	Project = "project"
	Cluster = "cluster"
	Day     = "day"
	Week    = "week"
	Month   = "month"
)

var (
	errNoClusterFound = errors.New("no cluster legal found")
)

type PieData struct {
	Series []PieSerie `json:"series"`
}

type PieSerie struct {
	Data []SerieData `json:"data"`
	Name string      `json:"name"`
	Type string      `json:"type"`
}

type HistogramSerie struct {
	Data []float64 `json:"data"`
	Name string    `json:"name"`
	Type string    `json:"type"`
}

type SerieData struct {
	Value float64 `json:"value"`
	Name  string  `json:"name"`
}

type DailyProjectQuota struct {
	Index      int64 `gorm:"column:idx"`
	CpuQuota   int64 `gorm:"column:cpu_quota"`
	CpuRequest int64 `gorm:"column:cpu_request"`
	MemQuota   int64 `gorm:"column:mem_quota"`
	MemRequest int64 `gorm:"column:mem_request"`
}

type DailyClusterQuota struct {
	Index      int64 `gorm:"column:idx"`
	CpuTotal   int64 `gorm:"column:cpu_total"`
	CpuRequest int64 `gorm:"column:cpu_requested"`
	MemTotal   int64 `gorm:"column:mem_total"`
	MemRequest int64 `gorm:"column:mem_requested"`
}

type Quota struct {
	cpuQuota float64
	memQuota float64
	nickName string
}

func (r *Resource) GetPie(ctx context.Context, ordId, userId string, request *apistructs.ClassRequest) (data map[string]*PieData, err error) {
	logrus.Debug("func GetPie start")
	defer logrus.Debug("func GetPie finished")
	var clusters []*clusterpb.ClusterInfo
	data = make(map[string]*PieData)
	orgID, err := strconv.ParseUint(ordId, 10, 64)
	if err != nil {
		return
	}

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	clusterResp, err := r.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{OrgID: uint32(orgID)})
	if err != nil {
		return
	}
	clusters = clusterResp.Data
	var filter = make(map[string]struct{})
	for _, cluster := range request.ClusterName {
		filter[cluster] = struct{}{}
	}
	request.ClusterName = r.FilterCluster(clusters, filter)
	if len(request.ClusterName) == 0 {
		return nil, errNoClusterFound
	}
	logrus.Debug("start fetch quota ")
	resp, err := r.Bdl.FetchQuotaOnClusters(orgID, request.ClusterName)
	logrus.Debug("func quota finished")
	if err != nil {
		return
	}

	greq := &pb.GetClustersResourcesRequest{}
	greq.ClusterNames = request.ClusterName
	logrus.Debug("start get cluster resource from steve")
	resources, err := r.Server.GetClustersResources(r.Ctx, greq)
	logrus.Debug("get cluster resource from steve finished ")

	irResp, err := r.GetQuotaResource(ctx, ordId, userId, request.ClusterName)
	if err != nil {
		return
	}

	// Project
	pie, err := r.GetProjectPie(ctx, request.ResourceType, resp, irResp)
	if err != nil {
		return
	}
	data[Project] = pie

	// principal
	pie, err = r.GetPrincipalPie(ctx, request.ResourceType, resp, irResp)
	if err != nil {
		return
	}
	data[Owner] = pie

	// Cluster
	pie, err = r.GetClusterPie(ctx, request.ResourceType, resources)
	if err != nil {
		return
	}
	data[Cluster] = pie
	return
}

func (r *Resource) GetProjectPie(ctx context.Context, resType string, resp *apistructs.GetQuotaOnClustersResponse, resource *apistructs.ResourceResp) (projectPie *PieData, err error) {
	var (
		q         Quota
		ok        bool
		langCodes i18n.LanguageCodes
	)
	projectPie = &PieData{}
	if langCodes, ok = ctx.Value(Lang).(i18n.LanguageCodes); !ok {
		logrus.Error("i18n translator is empty")
	}
	serie := PieSerie{
		Name: r.I18n(langCodes, "distribution by project"),
		Type: "pie",
	}
	projectMap := make(map[string]Quota)
	for _, owner := range resp.Owners {
		for _, p := range owner.Projects {
			if q, ok = projectMap[p.Name]; !ok {
				q = Quota{}
			}
			q.cpuQuota += p.CPUQuota
			q.memQuota += p.MemQuota
			q.nickName = p.DisplayName
			projectMap[p.Name] = q
		}
	}
	switch resType {

	case Memory:
		//sum := 0.0
		for _, v := range projectMap {
			f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", v.memQuota), 64)
			serie.Data = append(serie.Data, SerieData{f, v.nickName})
		}
		// not used
		//if resource != nil {
		//	ir, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", resource.IrrelevantMemRequest/G), 64)
		//	serie.Data = append(serie.Data, SerieData{ir, r.I18n(langCodes, "shared resource")})
		//	sum += resource.IrrelevantCpuRequest
		//	if resource.MemTotal > sum {
		//		unused, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", (resource.MemTotal-sum)/G), 64)
		//		serie.Data = append(serie.Data, SerieData{unused, r.I18n(langCodes, "unused resource")})
		//	}
		//}
	default:
		//sum := 0.0
		for _, v := range projectMap {
			f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", v.cpuQuota), 64)
			serie.Data = append(serie.Data, SerieData{f, v.nickName})
		}

		// not used
		//if resource != nil {
		//	ir, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", resource.IrrelevantCpuRequest/MilliCore), 64)
		//	serie.Data = append(serie.Data, SerieData{ir, r.I18n(langCodes, "shared resource")})
		//	sum += resource.IrrelevantCpuRequest
		//	if resource.CpuTotal > sum {
		//		unused, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", (resource.CpuTotal-sum)/MilliCore), 64)
		//		serie.Data = append(serie.Data, SerieData{unused, r.I18n(langCodes, "unused resource")})
		//	}
		//}
	}
	r.PieSort(serie.Data)
	projectPie.Series = append(projectPie.Series, serie)
	return
}

func (r *Resource) GetPrincipalPie(ctx context.Context, resType string, resp *apistructs.GetQuotaOnClustersResponse, resource *apistructs.ResourceResp) (principalPie *PieData, err error) {
	var (
		q         Quota
		langCodes i18n.LanguageCodes
		ok        bool
	)
	if langCodes, ok = ctx.Value(Lang).(i18n.LanguageCodes); !ok {
		logrus.Error("i18n translator is empty")
	}
	principalMap := make(map[string]Quota)
	for _, owner := range resp.Owners {
		if q, ok = principalMap[owner.Name]; !ok {
			q = Quota{}
		}
		q.cpuQuota = owner.CPUQuota
		q.memQuota = owner.MemQuota
		q.nickName = owner.Nickname
		principalMap[owner.Name] = q
	}

	principalPie = &PieData{}
	serie := PieSerie{
		Name: r.I18n(langCodes, "distribution by owner"),
		Type: "pie",
	}
	switch resType {
	case Memory:
		//sum := 0.0
		for _, v := range principalMap {
			f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", v.memQuota), 64)
			serie.Data = append(serie.Data, SerieData{f, v.nickName})
		}
		//if resource != nil {
		//	ir, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", resource.IrrelevantMemRequest/G), 64)
		//	serie.Data = append(serie.Data, SerieData{ir, r.I18n(langCodes, "shared resource")})
		//	sum += resource.IrrelevantCpuRequest
		//	if resource.MemTotal > sum {
		//		unused, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", (resource.MemTotal-sum)/G), 64)
		//		serie.Data = append(serie.Data, SerieData{unused, r.I18n(langCodes, "unused resource")})
		//	}
		//}

	default:
		//sum := 0.0
		for _, v := range principalMap {
			f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", v.cpuQuota), 64)
			serie.Data = append(serie.Data, SerieData{f, v.nickName})
		}
		//if resource != nil {
		//	ir, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", resource.IrrelevantCpuRequest/MilliCore), 64)
		//	serie.Data = append(serie.Data, SerieData{ir, r.I18n(langCodes, "shared resource")})
		//	sum += resource.IrrelevantCpuRequest
		//	if resource.CpuTotal > sum {
		//		unused, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", (resource.CpuTotal-sum)/MilliCore), 64)
		//		serie.Data = append(serie.Data, SerieData{unused, r.I18n(langCodes, "unused resource")})
		//	}
		//}
	}
	r.PieSort(serie.Data)
	principalPie.Series = append(principalPie.Series, serie)
	return
}

func (r *Resource) GetClusterPie(ctx context.Context, resourceType string, resources *pb.GetClusterResourcesResponse) (clusterPie *PieData, err error) {
	clusterPie = &PieData{}
	var (
		langCodes i18n.LanguageCodes
		ok        bool
	)
	if langCodes, ok = ctx.Value(Lang).(i18n.LanguageCodes); !ok {
		logrus.Error("i18n translator is empty")
	}
	serie := PieSerie{
		Name: r.I18n(langCodes, "distribution by cluster"),
		Type: "pie",
	}

	switch resourceType {
	case Memory:
		for _, c := range resources.List {
			memSum := 0.0
			for _, h := range c.Hosts {
				memSum += float64(h.MemTotal)
			}
			if memSum == 0.0 {
				continue
			}
			f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", memSum/G), 64)
			serie.Data = append(serie.Data, SerieData{f, c.ClusterName})
		}
	default:
		for _, c := range resources.List {
			cpuSum := 0.0
			for _, h := range c.Hosts {
				cpuSum += float64(h.CpuTotal)
			}
			if cpuSum == 0.0 {
				continue
			}
			f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", cpuSum/MilliCore), 64)
			serie.Data = append(serie.Data, SerieData{f, c.ClusterName})
		}
	}
	r.PieSort(serie.Data)
	clusterPie.Series = append(clusterPie.Series, serie)
	return
}

func (r *Resource) PieSort(series []SerieData) {
	sort.Slice(series, func(i, j int) bool {
		return series[i].Value > series[j].Value
	})
}

func (r *Resource) GetClusterTrend(ctx context.Context, orgId int64, userId string, request *apistructs.TrendRequest) (td *Histogram, err error) {
	logrus.Debug("func GetClusterTrend start")
	defer logrus.Debug("func GetClusterTrend finished")
	var (
		start, _ = request.Query.GetStart()
		end, _   = request.Query.GetEnd()
	)
	langCodes, _ := ctx.Value(Lang).(i18n.LanguageCodes)
	td = &Histogram{
		Name: r.I18n(langCodes, "cluster trend"),
	}
	td.XAxis = XAxis{
		Type: "category",
	}
	td.YAxis = YAxis{Type: "value"}
	td.Series = make([]HistogramSerie, 2)
	td.Series[0].Type = "bar"
	td.Series[1].Type = "bar"
	td.Series[0].Name = r.I18n(langCodes, "quota")
	td.Series[1].Name = r.I18n(langCodes, "total")
	var (
		pd       []apistructs.ClusterResourceDailyModel
		clusters []*clusterpb.ClusterInfo
	)
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := r.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{OrgID: uint32(orgId)})
	if err != nil {
		return nil, err
	}
	clusters = resp.Data
	request.Query.ClustersNames = r.FilterCluster(clusters, request.Query.GetClustersNames())
	if len(request.Query.ClustersNames) == 0 {
		return nil, errNoClusterFound
	}

	startTime := time.Unix(int64(start)/1e3, int64(start)%1e3*1e6)
	endTime := time.Unix(int64(end)/1e3, int64(end)%1e3*1e6)
	db := r.DB.Table("cmp_cluster_resource_daily")
	db = db.Where(" updated_at > ? and updated_at < ? ", startTime.Format("2006-01-02 15:01:05"), endTime.Format("2006-01-02 15:01:05"))
	db = db.Where("cluster_name in (?)", request.Query.ClustersNames)
	logrus.Debug("cluster trend start get daily quota from db")
	if err = db.Scan(&pd).Error; err != nil {
		return
	}
	logrus.Debug("cluster trend get daily quota finished")
	tRes := make(map[int]apistructs.ClusterResourceDailyModel)
	switch request.Query.Interval {
	case Week:
		for _, model := range pd {
			_, wk := model.CreatedAt.ISOWeek()
			if v, ok := tRes[wk]; ok {
				v.CPUTotal += model.CPUTotal
				v.MemTotal += model.MemTotal
				v.CPURequested += model.CPURequested
				v.MemRequested += model.MemRequested
				v.ID = uint64(wk)
				tRes[wk] = v
			} else {
				tRes[wk] = model
			}
		}
	case Month:
		for _, model := range pd {
			m := int(model.CreatedAt.Month())
			if v, ok := tRes[m]; ok {
				v.CPUTotal += model.CPUTotal
				v.MemTotal += model.MemTotal
				v.CPURequested += model.CPURequested
				v.MemRequested += model.MemRequested
				v.ID = uint64(model.CreatedAt.Month())
				tRes[m] = v
			} else {
				tRes[m] = model
			}
		}
	default:
		// Day
		for _, model := range pd {
			// assume data not over one year
			yd := model.CreatedAt.YearDay()
			if v, ok := tRes[yd]; ok {
				v.CPUTotal += model.CPUTotal
				v.MemTotal += model.MemTotal
				v.CPURequested += model.CPURequested
				v.MemRequested += model.MemRequested
				v.ID = uint64(model.CreatedAt.YearDay())
				tRes[yd] = v
			} else {
				tRes[yd] = model
			}
		}
	}
	pd = make([]apistructs.ClusterResourceDailyModel, 0)
	for _, model := range tRes {
		pd = append(pd, model)
	}
	sort.Slice(pd, func(i, j int) bool {
		return pd[i].ID < pd[j].ID
	})

	switch request.Query.ResourceType {
	case Memory, Mem:
		td.YAxis.Name = r.I18n(langCodes, "memory") + " (GB)"
		for _, quota := range pd {
			td.Series[0].Data = append(td.Series[0].Data, toGB(float64(quota.MemRequested)))
			td.Series[1].Data = append(td.Series[1].Data, toGB(float64(quota.MemTotal)))
			switch request.Query.Interval {
			case Month:
				td.XAxis.Data = append(td.XAxis.Data, r.I18n(langCodes, quota.CreatedAt.Format("2006-01")))
			case Week:
				_, wk := quota.CreatedAt.ISOWeek()
				td.XAxis.Data = append(td.XAxis.Data, fmt.Sprintf("%d", wk)+r.I18n(langCodes, "week"))
			default:
				td.XAxis.Data = append(td.XAxis.Data, quota.CreatedAt.Format("2006-01-02"))
			}
		}
	default:
		td.YAxis.Name = r.I18n(langCodes, "cpu") + " (" + r.I18n(langCodes, "core") + ")"
		for _, quota := range pd {
			td.Series[0].Data = append(td.Series[0].Data, toCore(float64(quota.CPURequested)))
			td.Series[1].Data = append(td.Series[1].Data, toCore(float64(quota.CPUTotal)))
			switch request.Query.GetInterval() {
			case Month:
				td.XAxis.Data = append(td.XAxis.Data, r.I18n(langCodes, quota.CreatedAt.Format("2006-01")))
			case Week:
				_, wk := quota.CreatedAt.ISOWeek()
				td.XAxis.Data = append(td.XAxis.Data, fmt.Sprintf("%d", wk)+r.I18n(langCodes, "week"))
			default:
				td.XAxis.Data = append(td.XAxis.Data, quota.CreatedAt.Format("2006-01-02"))
			}
		}
	}
	return
}

func (r *Resource) GetProjectTrend(ctx context.Context, request *apistructs.TrendRequest) (*Histogram, error) {
	var (
		//l          = logrus.WithField("func", "*Resource.GetProjectTrend")
		langCodes, _ = ctx.Value(Lang).(i18n.LanguageCodes)
		td           = new(Histogram)
		orgID, _     = request.GetOrgID()
		//userID, _  = request.GetUserID()
		start, _   = request.Query.GetStart()
		end, _     = request.Query.GetEnd()
		scopeID, _ = request.Query.GetScopeID()
	)
	td.Name = r.I18n(langCodes, "project trend")
	td.XAxis = XAxis{
		Type: "category",
	}
	td.YAxis = YAxis{Type: "value"}
	td.Series = make([]HistogramSerie, 2)
	td.Series[0].Type = "bar"
	td.Series[1].Type = "bar"
	td.Series[0].Name = r.I18n(langCodes, "request")
	td.Series[1].Name = r.I18n(langCodes, "quota")
	var (
		pd       []*apistructs.ProjectResourceDailyModel
		clusters []*clusterpb.ClusterInfo
	)
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	clusterResp, err := r.ClusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{OrgID: uint32(orgID)})
	if err != nil {
		return nil, err
	}
	clusters = clusterResp.Data
	request.Query.ClustersNames = r.FilterCluster(clusters, request.Query.GetClustersNames())
	if len(request.Query.ClustersNames) == 0 {
		return nil, errNoClusterFound
	}
	startTime := time.Unix(int64(start)/1e3, int64(start)%1e3*1e6)
	endTime := time.Unix(int64(end)/1e3, int64(end)%1e3*1e6)
	var projectDailies []*apistructs.ProjectResourceDailyModel
	db := r.DB.Where(" updated_at >= ? and created_at <= ? ",
		startTime.Format("2006-01-02 15:01:05"), endTime.Format("2006-01-02 15:01:05")).
		Where("cluster_name in (?)", request.Query.ClustersNames)
	switch strings.ToLower(request.Query.GetScope()) {
	case "owner":
		db = db.Where(map[string]interface{}{"owner_user_id": scopeID})
	default:
		db = db.Where(map[string]interface{}{"project_id": scopeID})
	}
	if err = db.Debug().Find(&projectDailies).Error; err != nil {
		return td, err
	}

	tRes := make(map[int]*apistructs.ProjectResourceDailyModel)
	switch request.Query.GetInterval() {
	case Week:
		for _, model := range projectDailies {
			_, wk := model.CreatedAt.ISOWeek()
			if v, ok := tRes[wk]; ok {
				v.CPURequest += model.CPURequest
				v.CPUQuota += model.CPUQuota
				v.MemRequest += model.MemRequest
				v.MemQuota += model.MemQuota
				v.ID = uint64(wk)
				tRes[wk] = v
			} else {
				tRes[wk] = model
			}
		}
	case Month:
		for _, model := range projectDailies {
			m := int(model.CreatedAt.Month())
			if v, ok := tRes[m]; ok {
				v.CPURequest += model.CPURequest
				v.CPUQuota += model.CPUQuota
				v.MemRequest += model.MemRequest
				v.MemQuota += model.MemQuota
				v.ID = uint64(model.CreatedAt.Month())
				tRes[m] = v
			} else {
				tRes[m] = model
			}
		}
	default:
		// Day
		var records = make(map[string]apistructs.ProjectResourceDailyModel)
		for _, model := range projectDailies {
			day := model.CreatedAt.Format("2006-01-02")
			var record = records[day]
			record.CreatedAt = model.CreatedAt
			record.CPUQuota += model.CPUQuota
			record.MemQuota += model.CPUQuota
			record.CPURequest += model.CPURequest
			record.MemRequest += model.MemRequest
			records[day] = record
		}
		for day := startTime; day.Before(endTime) && day.Before(time.Now()); day = day.Add(time.Hour * 24) {
			today := day.Format("2006-01-02")
			record := records[today]
			if record.CreatedAt.Format("2006-01-02") != today {
				record.CreatedAt = day
			}
			tRes[day.YearDay()] = &record
		}
	}
	for _, model := range tRes {
		pd = append(pd, model)
	}
	sort.Slice(pd, func(i, j int) bool {
		return pd[i].ID < pd[j].ID
	})
	switch request.Query.GetResourceType() {
	case Memory, Mem:
		td.YAxis.Name = r.I18n(langCodes, "memory") + " (GB)"
		for _, quota := range pd {
			td.Series[0].Data = append(td.Series[0].Data, toGB(float64(quota.MemRequest)))
			td.Series[1].Data = append(td.Series[1].Data, toGB(float64(quota.MemQuota)))
			switch request.Query.GetInterval() {
			case Month:
				td.XAxis.Data = append(td.XAxis.Data, r.I18n(langCodes, quota.CreatedAt.Format("2006-01")))
			case Week:
				_, wk := quota.CreatedAt.ISOWeek()
				td.XAxis.Data = append(td.XAxis.Data, fmt.Sprintf("%d", wk)+r.I18n(langCodes, "week"))
			default:
				td.XAxis.Data = append(td.XAxis.Data, quota.CreatedAt.Format("2006-01-02"))
			}
		}
	default:
		td.YAxis.Name = r.I18n(langCodes, "cpu") + " (" + r.I18n(langCodes, "core") + ")"
		for _, quota := range pd {
			td.Series[0].Data = append(td.Series[0].Data, toCore(float64(quota.CPURequest)))
			td.Series[1].Data = append(td.Series[1].Data, toCore(float64(quota.CPUQuota)))
			switch request.Query.GetInterval() {
			case Month:
				td.XAxis.Data = append(td.XAxis.Data, r.I18n(langCodes, quota.CreatedAt.Format("2006-01")))
			case Week:
				_, wk := quota.CreatedAt.ISOWeek()
				td.XAxis.Data = append(td.XAxis.Data, fmt.Sprintf("%d", wk)+r.I18n(langCodes, "week"))
			default:
				td.XAxis.Data = append(td.XAxis.Data, quota.CreatedAt.Format("2006-01-02"))
			}
		}
	}
	return td, nil
}

func toCore(mCores float64) float64 {
	return mCores / 1000
}

func toGB(b float64) float64 {
	f, _ := strconv.ParseFloat(fmt.Sprintf("%.3f", b/float64(1<<30)), 64)
	return f
}
