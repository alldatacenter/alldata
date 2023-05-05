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

package dto

import (
	"github.com/pkg/errors"
)

const (
	EDT_CUSTOM  = "CUSTOM"
	EDT_DEFAULT = "DEFAULT"
	EDT_PACKAGE = "PACKAGE"
)

type EndpointDomainDto struct {
	Domain string `json:"domain"`
	Type   string `json:"type"`
}

type ServiceDetailDto struct {
	ServiceName     string              `json:"serviceName"`
	InnerAddress    string              `json:"innerAddress"`
	EndpointDomains []EndpointDomainDto `json:"endpointDomains"`
}

type RuntimeServiceReqDto struct {
	OrgId                 string             `json:"orgId"`
	ProjectId             string             `json:"projectID"`
	Env                   string             `json:"env"`
	ClusterName           string             `json:"clusterName"`
	RuntimeId             string             `json:"runtimeID"`
	RuntimeName           string             `json:"runtimeName"`
	ReleaseId             string             `json:"releaseId"`
	ServiceGroupNamespace string             `json:"serviceGroupNamespace"`
	ProjectNamespace      string             `json:"projectNamespace"`
	ServiceGroupName      string             `json:"serviceGroupName"`
	AppId                 string             `json:"appID"`
	AppName               string             `json:"appName"`
	Services              []ServiceDetailDto `json:"services"`
	UseApigw              bool               `json:"useApigw"`
}

func (dto EndpointDomainDto) CheckValid() error {
	if dto.Domain == "" {
		return errors.New("empty domain")
	}
	if dto.Type != EDT_CUSTOM && dto.Type != EDT_DEFAULT {
		return errors.Errorf("invalid domain type:%s", dto.Type)
	}
	return nil
}

func (dto ServiceDetailDto) CheckValid() error {
	if dto.ServiceName == "" || dto.InnerAddress == "" {
		return errors.Errorf("invalid service req dto:%+v", dto)
	}
	for _, domain := range dto.EndpointDomains {
		err := domain.CheckValid()
		if err != nil {
			return err
		}
	}
	return nil
}

func (dto RuntimeServiceReqDto) CheckValid() error {
	if dto.ProjectId == "" || dto.OrgId == "" || dto.Env == "" || dto.ClusterName == "" ||
		dto.RuntimeId == "" || dto.RuntimeName == "" || dto.AppId == "" {
		return errors.Errorf("invalid runtime req dto:%+v", dto)
	}
	for _, service := range dto.Services {
		err := service.CheckValid()
		if err != nil {
			return errors.WithMessagef(err, "invalid runtime req dto:%+v ", dto)
		}
	}
	return nil
}
