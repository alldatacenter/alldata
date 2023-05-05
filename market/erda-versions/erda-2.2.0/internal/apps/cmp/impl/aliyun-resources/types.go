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

package aliyun_resources

import (
	"fmt"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/jsonstore"
)

type AccessKeySecret struct {
	OrgID        string
	Vendor       string
	Region       string
	AccessKeyID  string
	AccessSecret string
}

type PageOption struct {
	PageSize   *int
	PageNumber *int
}

type Context struct {
	AccessKeySecret
	VpcID    string
	DB       *dbclient.DBClient
	Bdl      *bundle.Bundle
	JS       jsonstore.JsonStore
	CachedJs jsonstore.JsonStore
}

type ResponsePager struct {
	TotalCount int
	PageSize   int
	PageNumber int
}

const (
	TagPrefixCluster = "dice-cluster"
	TagPrefixProject = "dice-project"
)

const (
	CloudSourceExpireDays             = 10
	CloudSourceRunning                = "Running"
	CloudSourceStopped                = "Stopped"
	CloudSourceExpired                = "Expired"
	CloudSourceBeforeExpired          = "BeforeExpired"
	CloudSourceBeforeExpiredInTenDays = "BeforeExpired (10 days)"
)

func GenClusterTag(cluster string) (key string, value string) {
	key = fmt.Sprintf(TagPrefixCluster+"/%s", cluster)
	value = "true"
	return
}

func GenProjectTag(projectID string) (key string, value string) {
	key = fmt.Sprintf(TagPrefixProject+"/%s", projectID)
	value = "true"
	return
}

type CloudResourceType string

const (
	CloudResourceTypeCompute CloudResourceType = "Compute"
	CloudResourceTypeNetwork CloudResourceType = "Network"
	CloudResourceTypeStorage CloudResourceType = "Storage"
	CloudResourceTypeAddon   CloudResourceType = "Addon"
)

func (c CloudResourceType) String() string {
	return string(c)
}

var defaultPageSize = 50
var pageSizeOne = 1
var defaultPageNum = 1
var DefaultPageOption = PageOption{
	PageSize:   &defaultPageSize,
	PageNumber: &defaultPageNum,
}

var PageSizeOneOption = PageOption{
	PageSize:   &pageSizeOne,
	PageNumber: &defaultPageNum,
}

type CloudVendor string

const (
	CloudVendorAliCloud           CloudVendor = "alicloud"
	CloudResourceOverviewJsPrefix             = "/dice/ops/resource_overview"
	CloudResourcePrefix                       = "/dice/ops"

	ResourceOverview = "resource_overview"
	ResourceRegions  = "regions"
)

func (c CloudVendor) String() string {
	return string(c)
}

type TagResourceType string

const (
	TagResourceTypeVpc         TagResourceType = "VPC"
	TagResourceTypeVsw         TagResourceType = "VSWITCH"
	TagResourceTypeEip         TagResourceType = "EIP"
	TagResourceTypeRedis       TagResourceType = "REDIS"
	TagResourceTypeOss         TagResourceType = "OSS"
	TagResourceTypeRDS         TagResourceType = "RDS"
	TagResourceTypeECS         TagResourceType = "ECS"
	TagResourceTypeOnsInstance TagResourceType = "ONS"
	TagResourceTypeOnsGroup    TagResourceType = "ONS_GROUP"
	TagResourceTypeOnsTopic    TagResourceType = "ONS_TOPIC"

	TagResourceTypeOnsInstanceTag TagResourceType = "INSTANCE"
	TagResourceTypeOnsGroupTag    TagResourceType = "GROUP"
	TagResourceTypeOnsTopicTag    TagResourceType = "TOPIC"
)

func (t TagResourceType) String() string {
	return string(t)
}
