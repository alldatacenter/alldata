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

	corev1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/pkg/metadata"
)

type ElfMetadata struct {
	ID               uint64    `json:"id"`
	Name             string    `json:"name"`
	Description      string    `json:"description"`
	Workspace        string    `json:"workspace"`
	OwnerName        string    `json:"ownerName"`
	OwnerID          uint64    `json:"ownerID"`
	OrganizationID   uint64    `json:"organizationID"`
	OrganizationName string    `json:"organizationName"`
	CreatedAt        time.Time `json:"createdAt"`
	UpdatedAt        time.Time `json:"updatedAt"`
}

type ListMetadata struct {
	Total int `json:"total"`
}

type Notebook struct {
	metadata.Metadata
	NotebookSpec
	NotebookStatus
}

type NotebookSpec struct {
	Envs             []corev1.EnvVar `json:"envs"` // 环境变量
	ClusterName      string          `json:"clusterName"`
	ProjectName      string          `json:"projectName"`
	ApplicationName  string          `json:"applicationName"`
	DBEnvs           string          `json:"-"`
	Image            string          `json:"image"`
	RequirementEnvID uint64          `json:"requirementEnvID"`
	DataSourceID     uint64          `json:"datasourceID,omitempty"`
	GenericDomain    string          `json:"genericDomain,omitempty"`
	ClusterDomain    string          `json:"clusterDomain,omitempty"`
	ElfResource      `json:"resource"`
}

type NotebookStatus struct {
	StartedAt time.Time `json:"startedAt"`
	State     string    `json:"state"`
}

type ElfResource struct {
	CPU    float64 `json:"cpu"`    // CPU 资源大小
	Memory int     `json:"memory"` // 内存资源大小
}

type NotebookResponse struct {
	Header
	Data Notebook `json:"data"`
}

type NotebookListResponse struct {
	Header
	Data NoteBookList `json:"data"`
}

type NoteBookList struct {
	ListMetadata
	NoteBookListSpec
}

type NoteBookListSpec struct {
	Items []Notebook `json:"data"`
}

type EnvironmentListResponse struct {
	Header
	Data EnvironmentList `json:"data"`
}

type EnvironmentList struct {
	ListMetadata
	EnvironmentListSpec
}

type EnvironmentListSpec struct {
	Items []Environment `json:"data"`
}

type Environment struct {
	ElfMetadata
	EnvironmentSpec
}

type EnvironmentSpec struct {
	Requires      []Require         `json:"requires"`
	DBRequires    string            `json:"-"`
	Labels        map[string]string `json:"labels"`
	DBLabels      string            `json:"-"`
	NotebookCount int               `json:"notebook_count"`
}

type EnvironmentResponse struct {
	Header
	Data Environment `json:"data"`
}

type Require struct {
	Type     string    `json:"type"`
	Packages []Package `json:"packages"`
}

type Package struct {
	Name    string `json:"name"`
	Version string `json:"version"`
}

type DependencyPackageTypeResponse struct {
	Header
	Data DependencyPackageType `json:"data"`
}

type DependencyPackageListResponse struct {
	Header
	Data DependencyPackageList `json:"data"`
}

type DependencyPackageList struct {
	ListMetadata
	DependencyPackageSpec
}

type DependencyPackageSpec struct {
	Items []DependencyPackageType `json:"data"`
}

type DependencyPackageType struct {
	Type     string                      `json:"type"`
	Packages []DependencyPackageTypeItem `json:"packages"`
}

type DependencyPackageTypeItem struct {
	Name    string   `json:"name"`
	Version []string `json:"version"`
}
