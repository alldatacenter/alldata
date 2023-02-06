//  Copyright (c) 2021 Terminus, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package apistructs

import "time"

// PodInfo is the table `s_pod_info`
type PodInfo struct {
	ID              uint64    `json:"id" gorm:"id"`
	CreatedAt       time.Time `json:"created_at" gorm:"created_at"`
	UpdatedAt       time.Time `json:"updated_at" gorm:"updated_at"`
	Cluster         string    `json:"cluster" gorm:"cluster"`
	Namespace       string    `json:"namespace" gorm:"namespace"`
	Name            string    `json:"name" gorm:"name"`
	OrgName         string    `json:"org_name" gorm:"org_name"`
	OrgID           string    `json:"org_id" gorm:"org_id"`
	ProjectName     string    `json:"project_name" gorm:"project_name"`
	ProjectID       string    `json:"project_id" gorm:"project_id"`
	ApplicationName string    `json:"application_name" gorm:"application_name"`
	ApplicationID   string    `json:"application_id" gorm:"application_id"`
	RuntimeName     string    `json:"runtime_name" gorm:"runtime_name"`
	RuntimeID       string    `json:"runtime_id" gorm:"runtime_id"`
	ServiceName     string    `json:"service_name" gorm:"service_name"`
	Workspace       string    `json:"workspace" gorm:"workspace"`
	ServiceType     string    `json:"service_type" gorm:"service_type"`
	AddonID         string    `json:"addon_id" gorm:"addon_id"`
	UID             string    `json:"uid" gorm:"uid"`
	K8sNamespace    string    `json:"k8s_namespace" gorm:"k8s_namespace"`
	PodName         string    `json:"pod_name" gorm:"pod_name"`
	Phase           string    `json:"phase" gorm:"phase"`
	Message         string    `json:"message" gorm:"message"`
	PodIP           string    `json:"pod_ip" gorm:"pod_ip"`
	HostIP          string    `json:"host_ip" gorm:"host_ip"`
	StartedAt       string    `json:"started_at" gorm:"started_at"`
	CPURequest      float64   `json:"cpu_request" gorm:"cpu_request"`
	MemRequest      float64   `json:"mem_request" gorm:"mem_request"`
	CPULimit        float64   `json:"cpu_limit" gorm:"cpu_limit"`
	MemLimit        float64   `json:"mem_limit" gorm:"mem_limit"`
}

func (PodInfo) TableName() string {
	return "s_pod_info"
}
