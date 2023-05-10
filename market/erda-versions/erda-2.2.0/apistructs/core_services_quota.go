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
	"time"

	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

type GetWorkspaceQuotaRequest struct {
	ProjectID string `json:"projectID"`
	Workspace string `json:"workspace"`
}

type GetWorkspaceQuotaResponse struct {
	Header
	Data WorkspaceQuotaData `json:"data"`
}

type WorkspaceQuotaData struct {
	CPU    int64 `json:"cpu"`
	Memory int64 `json:"memory"`
}

type GetQuotaOnClustersResponse struct {
	ClusterNames []string `json:"clusterNames"`
	// CPUQuota is the total cpu quota on the clusters
	CPUQuota           float64 `json:"cpuQuota"`
	CPUQuotaMilliValue uint64
	// MemQuota is hte total mem quota on the clusters
	MemQuota     float64 `json:"memQuota"`
	MemQuotaByte uint64
	Owners       []*OwnerQuotaOnClusters `json:"owners"`
}

// AccuQuota accumulate cpu and mem quota value
func (q *GetQuotaOnClustersResponse) AccuQuota(cpu, mem uint64) {
	q.CPUQuotaMilliValue += cpu
	q.MemQuotaByte += mem
}

func (q *GetQuotaOnClustersResponse) ReCalcu() {
	q.CPUQuotaMilliValue = 0
	q.MemQuotaByte = 0
	for _, owner := range q.Owners {
		owner.ReCalcu()
		q.AccuQuota(owner.CPUQuotaMilliValue, owner.MemQuotaByte)
	}
	q.CPUQuota = calcu.MillcoreToCore(q.CPUQuotaMilliValue, 3)
	q.MemQuota = calcu.ByteToGibibyte(q.MemQuotaByte, 3)
}

type OwnerQuotaOnClusters struct {
	ID       uint64 `json:"id"`
	Name     string `json:"name"`
	Nickname string `json:"nickname"`
	// CPUQuota is the total cpu quota for the owner on the clusters
	CPUQuota           float64 `json:"cpuQuota"`
	CPUQuotaMilliValue uint64
	// MemQuota is the total mem quota for the owner on the clusters
	MemQuota     float64 `json:"memQuota"`
	MemQuotaByte uint64
	Projects     []*ProjectQuotaOnClusters `json:"projects"`
}

// AccuQuota accumulate cpu and mem quota value
func (q *OwnerQuotaOnClusters) AccuQuota(cpu, mem uint64) {
	q.CPUQuotaMilliValue += cpu
	q.MemQuotaByte += mem
}

func (q *OwnerQuotaOnClusters) ReCalcu() {
	q.CPUQuotaMilliValue = 0
	q.MemQuotaByte = 0
	for _, project := range q.Projects {
		project.ReCalcu()
		q.AccuQuota(project.CPUQuotaMilliValue, project.MemQuotaByte)
	}
	q.CPUQuota = calcu.MillcoreToCore(q.CPUQuotaMilliValue, 3)
	q.MemQuota = calcu.ByteToGibibyte(q.MemQuotaByte, 3)
}

type ProjectQuotaOnClusters struct {
	ID          uint64 `json:"id"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	// CPUQuota is the total cpu quota for the project on the clusters
	CPUQuota           float64 `json:"cpuQuota"`
	CPUQuotaMilliValue uint64
	// CPUQuota is the total mem quota for the project on the clusters
	MemQuota     float64 `json:"memQuota"`
	MemQuotaByte uint64
}

// AccuQuota accumulate cpu and mem quota value
func (q *ProjectQuotaOnClusters) AccuQuota(cpu, mem uint64) {
	q.CPUQuotaMilliValue += cpu
	q.MemQuotaByte += mem
}

func (q *ProjectQuotaOnClusters) ReCalcu() {
	q.CPUQuota = calcu.MillcoreToCore(q.CPUQuotaMilliValue, 3)
	q.MemQuota = calcu.ByteToGibibyte(q.MemQuotaByte, 3)
}

type GetProjectsNamesapcesResponseData struct {
	Total uint32               `json:"total"`
	List  []*ProjectNamespaces `json:"list"`
}

func (d *GetProjectsNamesapcesResponseData) GetProjectNamespaces(id uint) (*ProjectNamespaces, bool) {
	for _, p := range d.List {
		if p.ProjectID == id {
			return p, true
		}
	}
	return nil, false
}

type ProjectNamespaces struct {
	ProjectID          uint   `json:"projectID"`
	ProjectName        string `json:"projectName"`
	ProjectDisplayName string `json:"projectDisplayName"`
	ProjectDesc        string `json:"projectDesc"`
	OwnerUserID        uint   `json:"ownerUserID"`
	OwnerUserName      string `json:"ownerUserName"`
	OwnerUserNickname  string `json:"ownerUserNickname"`
	CPUQuota           uint64 `json:"cpuQuota"`
	MemQuota           uint64 `json:"memQuota"`
	// Clusters the key is cluster name, the value is the list of namespaces
	Clusters map[string][]string `json:"clusters"`

	cpuRequest uint64
	memRequest uint64
}

func (p *ProjectNamespaces) PatchClusters(quota *ProjectQuota, filterClusters []string) {
	if quota == nil || len(filterClusters) == 0 {
		return
	}

	var filter = make(map[string]struct{})
	for _, cluster := range filterClusters {
		filter[cluster] = struct{}{}
	}

	if p.Clusters == nil {
		p.Clusters = make(map[string][]string)
	}
	for _, cluster := range []string{
		quota.ProdClusterName,
		quota.StagingClusterName,
		quota.TestClusterName,
		quota.DevClusterName,
	} {
		if _, ok := filter[cluster]; ok {
			p.Clusters[cluster] = p.Clusters[cluster]
		}
	}
}

func (p *ProjectNamespaces) PatchClustersNamespaces(namespaces map[string][]string) {
	if len(p.Clusters) == 0 || len(namespaces) == 0 {
		return
	}
	for cluster := range p.Clusters {
		if list, ok := namespaces[cluster]; ok {
			p.Clusters[cluster] = list
		}
	}
}

func (p *ProjectNamespaces) PatchQuota(quota *ProjectQuota) {
	if quota == nil {
		return
	}

	p.CPUQuota = 0
	p.MemQuota = 0

	for _, q := range []struct {
		ClusterName string
		CPUQuota    uint64
		MemQuota    uint64
	}{
		{
			ClusterName: quota.ProdClusterName,
			CPUQuota:    quota.ProdCPUQuota,
			MemQuota:    quota.ProdMemQuota,
		}, {
			ClusterName: quota.StagingClusterName,
			CPUQuota:    quota.StagingCPUQuota,
			MemQuota:    quota.StagingMemQuota,
		}, {
			ClusterName: quota.TestClusterName,
			CPUQuota:    quota.TestCPUQuota,
			MemQuota:    quota.TestMemQuota,
		}, {
			ClusterName: quota.DevClusterName,
			CPUQuota:    quota.DevCPUQuota,
			MemQuota:    quota.DevMemQuota,
		},
	} {
		if _, ok := p.Clusters[q.ClusterName]; ok {
			p.CPUQuota += q.CPUQuota
			p.MemQuota += q.MemQuota
		}
	}
}

func (p *ProjectNamespaces) AddResource(cpu, mem uint64) {
	p.cpuRequest += cpu
	p.memRequest += mem
}

func (p *ProjectNamespaces) GetCPUReqeust() uint64 {
	return p.cpuRequest
}

func (p *ProjectNamespaces) GetMemRequest() uint64 {
	return p.memRequest
}

func (p *ProjectNamespaces) Has(cluster, namespace string) bool {
	namespaces, ok := p.Clusters[cluster]
	if !ok {
		return false
	}
	for _, name := range namespaces {
		if name == namespace {
			return true
		}
	}
	return false
}

type ProjectNamespaceModel struct {
	ID           uint64    `json:"id" gorm:"id"`
	CreatedAt    time.Time `json:"createdAt" gorm:"created_at"`
	UpdatedAt    time.Time `json:"updatedAt" gorm:"updated_at"`
	ProjectID    uint64    `json:"projectID" gorm:"project_id"`
	ProjectName  string    `json:"projectName" gorm:"project_name"`
	ClusterName  string    `json:"clusterName" gorm:"cluster_name"`
	K8sNamespace string    `json:"k8s_namespace" gorm:"k8s_namespace"`
}

func (model *ProjectNamespaceModel) TableName() string {
	return "project_namespace"
}
