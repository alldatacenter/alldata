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

package apistructs

import (
	"strconv"
	"strings"

	"github.com/pkg/errors"
)

type GaugeRequest struct {
	MemPerNode  uint64   `json:"memPerNode"`
	CpuPerNode  uint64   `json:"cpuPerNode"`
	ClusterName []string `json:"clusterName"`
}

type TableRequest struct {
	MemoryUnit  int
	CpuUnit     int
	ClusterName []string
}

type ClassRequest struct {
	ResourceType string
	ClusterName  []string
}

type TrendRequest struct {
	OrgID  string
	UserID string

	Query *TrendRequestQuery
}

func (r TrendRequest) Validate() error {
	if _, err := r.GetOrgID(); err != nil {
		return errors.Wrap(err, "OrgID is invalid")
	}
	if _, err := r.GetUserID(); err != nil {
		return errors.Wrap(err, "UserID is invalid")
	}
	if r.Query == nil {
		return errors.New("query is empty")
	}
	return r.Query.Validate()
}

func (r TrendRequest) GetOrgID() (uint64, error) {
	return strconv.ParseUint(r.OrgID, 10, 64)
}

func (r TrendRequest) GetUserID() (uint64, error) {
	return strconv.ParseUint(r.OrgID, 10, 64)
}

type TrendRequestQuery struct {
	Start         string   // 统计值起始时间, 13 位时间戳
	End           string   // 统计值结束时间, 13 位时间戳
	Interval      string   // 统计聚合维度: 周期, 枚举: day, week, month
	ClustersNames []string // 筛选条件, 集群列表
	Scope         string   // 统计聚合维度, 枚举: project, owner
	ScopeID       string   // Scope 的 ID, 如 projectID, owner 的 userID
	ResourceType  string   // 资源类型, 枚举: cpu, mem
}

func (rq TrendRequestQuery) Validate() error {
	if _, err := rq.GetStart(); err != nil {
		return errors.Wrap(err, "start is invalid")
	}
	if _, err := rq.GetEnd(); err != nil {
		return errors.Wrap(err, "end is invalid")
	}
	if _, err := rq.GetScopeID(); err != nil {
		return errors.Wrap(err, "scopeID is invalid")
	}
	return nil
}

func (rq TrendRequestQuery) GetStart() (uint64, error) {
	return rq.getTime(rq.Start)
}

func (rq TrendRequestQuery) GetEnd() (uint64, error) {
	return rq.getTime(rq.End)
}

func (rq TrendRequestQuery) getTime(t string) (uint64, error) {
	v, err := strconv.ParseUint(t, 10, 64)
	if err != nil {
		return 0, err
	}
	if v < 1_000_000_000_000 || v > 9_999_999_999_999 {
		return 0, errors.New("start time is invalid")
	}
	return v, nil
}

func (rq TrendRequestQuery) GetInterval() string {
	switch i := strings.ToLower(rq.Interval); i {
	case "day", "week", "month":
		return i
	default:
		return "day"
	}
}

func (rq TrendRequestQuery) GetClustersNames() map[string]struct{} {
	var m = make(map[string]struct{})
	for _, s := range rq.ClustersNames {
		m[s] = struct{}{}
	}
	return m
}

func (rq TrendRequestQuery) GetScope() string {
	switch i := strings.ToLower(rq.Scope); i {
	case "project", "owner":
		return i
	default:
		return "project"
	}
}

func (rq TrendRequestQuery) GetScopeID() (uint64, error) {
	return strconv.ParseUint(rq.ScopeID, 10, 64)
}

func (rq TrendRequestQuery) GetResourceType() string {
	if strings.EqualFold(rq.ResourceType, "mem") {
		return "mem"
	}
	return "cpu"
}

type ResourceResp struct {
	MemRequest           float64
	CpuRequest           float64
	MemTotal             float64
	CpuTotal             float64
	CpuQuota             float64
	MemQuota             float64
	IrrelevantCpuRequest float64
	IrrelevantMemRequest float64
}
