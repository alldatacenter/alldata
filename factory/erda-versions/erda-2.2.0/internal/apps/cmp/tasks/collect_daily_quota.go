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

package tasks

import (
	"context"
	"net/url"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/http/httpclient"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

type DailyQuotaCollector struct {
	db  *dbclient.DBClient
	bdl *bundle.Bundle
	cmp interface {
		ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error)
		GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error)
		GetClustersResources(ctx context.Context, cReq *pb.GetClustersResourcesRequest) (*pb.GetClusterResourcesResponse, error)
		GetAllClusters() []string
	}
}

func NewDailyQuotaCollector(opts ...DailyQuotaCollectorOption) *DailyQuotaCollector {
	var collector DailyQuotaCollector
	for _, f := range opts {
		f(&collector)
	}
	return &collector
}

func (d *DailyQuotaCollector) Task() (bool, error) {
	l := logrus.WithField("func", "DailyQuotaCollector.Task")

	// 1) 查出所有的 clusters
	l.Debugln("query all clusters")
	clusterNames := d.cmp.GetAllClusters()
	l.WithField("clusterNames", clusterNames).
		Debugln("query all clusters")

	// 2) 查出所有的 namespace
	l.Debugln("query all namespaces")
	var namespacesM = make(url.Values)
	for _, clusterName := range clusterNames {
		resources, err := d.cmp.ListSteveResource(context.Background(), &apistructs.SteveRequest{
			NoAuthentication: true,
			UserID:           "",
			OrgID:            "",
			Type:             apistructs.K8SNamespace,
			ClusterName:      clusterName,
			Name:             "",
			Namespace:        "",
			LabelSelector:    nil,
			FieldSelector:    nil,
			Obj:              nil,
		})
		if err != nil {
			err = errors.Wrap(err, "failed to ListSteveResource")
			l.WithError(err).Warnln()
		}
		l.Debugf("ListSteveResource, length of resource: %v", len(resources))
		for _, resource := range resources {
			l.Debugf("ListSteveResource resource: %+v", resource)
			namespace := resource.Data().String("metadata", "name")
			namespacesM.Add(clusterName, namespace)
		}
	}
	l.WithField("namespacesM", namespacesM).Debugln("query all namespaces")

	l.Debugln("collectProjectDaily")
	if err := d.collectProjectDaily(namespacesM); err != nil {
		err = errors.Wrap(err, "failed to collectProjectDaily")
		logrus.WithError(err).WithField("namespaces", namespacesM).Errorln()
	}
	l.Debugln("collectClusterDaily")
	if err := d.collectClusterDaily(clusterNames); err != nil {
		err = errors.Wrap(err, "failed to collectClusterDaily")
		logrus.WithError(err).WithField("clusters", clusterNames).Errorln()
	}

	d.clearExpire()

	return false, nil
}

func (d *DailyQuotaCollector) collectProjectDaily(namespacesM map[string][]string) error {
	l := logrus.WithField("func", "DailyQuotaCollector.collectProjectDaily")

	// 3) 查询 project 列表
	l.Debugln("GetAllProjects")
	projects, err := d.bdl.GetAllProjects()
	if err != nil {
		err = errors.Wrap(err, "failed to GetAllProjects")
		l.WithError(err).Errorln()
		return err
	}
	l.Debugf("GetAllProjects, result: %+v", projects)

	for _, project := range projects {
		var record apistructs.ProjectResourceDailyModel
		record.ProjectID = project.ID
		record.ProjectName = project.Name

		projectDTO, err := d.bdl.GetProjectWithSetter(project.ID, httpclient.SetParams(url.Values{"withQuota": {"true"}}))
		if err != nil {
			err = errors.Wrap(err, "failed to GetProject")
			l.WithError(err).Errorln()
			continue
		}

		if projectDTO.ResourceConfig == nil {
			l.Warnf("the ResourceConfig is nil. projectID: %v, projectDTO: %+v", projectDTO.ID, projectDTO)
			continue
		}

		var (
			clustersM   = make(map[string]bool)
			cpuQuotaM   = make(map[string]uint64)
			memQuotaM   = make(map[string]uint64)
			cpuRequestM = make(map[string]uint64)
			memRequestM = make(map[string]uint64)
		)

		for workspace, resource := range map[string]*apistructs.ResourceConfigInfo{
			"PROD":    projectDTO.ResourceConfig.PROD,
			"STAGING": projectDTO.ResourceConfig.STAGING,
			"TEST":    projectDTO.ResourceConfig.TEST,
			"DEV":     projectDTO.ResourceConfig.DEV,
		} {
			if resource == nil {
				l.Warnf("the ResourceConfig.%s is nil. projectID: %v, projectDTO: %+v", workspace, projectDTO.ID, projectDTO)
				continue
			}
			clustersM[resource.ClusterName] = true
			cpuQuotaM[resource.ClusterName] += calcu.CoreToMillcore(resource.CPUQuota)
			memQuotaM[resource.ClusterName] += calcu.GibibyteToByte(resource.MemQuota)
			cpuRequestM[resource.ClusterName] += calcu.CoreToMillcore(resource.CPURequest)
			memRequestM[resource.ClusterName] += calcu.GibibyteToByte(resource.MemRequest)
		}

		for clusterName := range clustersM {
			if clusterName == "" {
				continue
			}
			record.ClusterName = clusterName
			record.CPUQuota = cpuQuotaM[clusterName]
			record.MemQuota = memQuotaM[clusterName]
			record.CPURequest = cpuRequestM[clusterName]
			record.MemRequest = memRequestM[clusterName]

			// insert record
			var existsRecord apistructs.ProjectResourceDailyModel
			err := d.db.Where("created_at >= ? and created_at < ?",
				time.Now().Format("2006-01-02 00:00:00"),
				time.Now().Add(time.Hour*24).Format("2006-01-02 00:00:00")).
				First(&existsRecord, map[string]interface{}{"project_id": record.ProjectID, "cluster_name": record.ClusterName}).
				Error
			switch {
			case err == nil:
				record.ID = existsRecord.ID
				if err = d.db.Debug().Save(&record).Error; err != nil {
					logrus.WithError(err).Errorln("failed to save project resource daily record")
				}
			case gorm.IsRecordNotFoundError(err):
				if err = d.db.Debug().Create(&record).Error; err != nil {
					logrus.WithError(err).Errorln("failed to create project resource daily record")
				}
			default:
				err = errors.Wrap(err, "failed to Save or Create project daily record")
				logrus.WithError(err).WithField("project daily record", record).Errorln()
			}
		}
	}

	return nil
}

func (d *DailyQuotaCollector) collectClusterDaily(clusterNames []string) error {
	// 3) 调用本地接口，查询各 cluster 上的 request
	l := logrus.WithField("func", "DailyQuotaCollector.collectClusterDaily")
	l.Debugf("query clusters resources, clusterNames: %v", clusterNames)
	req := pb.GetClustersResourcesRequest{
		ClusterNames: clusterNames,
	}
	clustersResources, err := d.cmp.GetClustersResources(context.Background(), &req)
	if err != nil {
		err = errors.Wrap(err, "failed to GetClustersResources")
		l.WithError(err).WithField("clusterNames", clusterNames).Errorln()
		return err
	}
	l.Debugf("GetClustersResources result: %v", clustersResources.List)

	// 4) 累计
	l.Debugln("accumulate resource for every cluster")
	var records = make(map[string]*apistructs.ClusterResourceDailyModel)
	for _, cluster := range clustersResources.List {
		record, ok := records[cluster.GetClusterName()]
		if !ok {
			record = &apistructs.ClusterResourceDailyModel{
				ClusterName: cluster.GetClusterName(),
			}
		}
		for _, host := range cluster.Hosts {
			record.CPUTotal += host.GetCpuTotal()
			record.CPURequested += host.GetCpuRequest()
			record.MemTotal += host.GetMemTotal()
			record.MemRequested += host.GetMemRequest()
		}
		records[cluster.GetClusterName()] = record
	}

	// 5) 插入库表
	l.Debugf("create record. length of records: %v", len(records))
	for clusterName, record := range records {
		var existsRecord apistructs.ClusterResourceDailyModel
		err := d.db.Where("updated_at >= ? and updated_at < ?",
			time.Now().Format("2006-01-02 00:00:00"),
			time.Now().Add(time.Hour*24).Format("2006-01-02 00:00:00")).
			First(&existsRecord, map[string]interface{}{"cluster_name": clusterName}).
			Error
		switch {
		case err == nil:
			record.ID = existsRecord.ID
			if err = d.db.Debug().Save(&record).Error; err != nil {
				logrus.WithError(err).Errorln("failed to save cluster resource daily record")
			}
		case gorm.IsRecordNotFoundError(err):
			if err = d.db.Debug().Create(&record).Error; err != nil {
				logrus.WithError(err).Errorln("failed to create cluster resource daily record")
			}
		default:
			err = errors.Wrap(err, "failed to First ClusterResourceDailyModel")
			logrus.WithError(err).Errorln()
		}
	}

	return nil
}

func (d *DailyQuotaCollector) clearExpire() {
	lastYear := time.Now().AddDate(-1, 0, 0).Format("2006-01-02 00:00:00")
	if err := d.db.Where("created_at < ?", lastYear).Delete(new(apistructs.ClusterResourceDailyModel)).Error; err != nil {
		logrus.Warnln("failed to clear expire data from ClusterResourceDailyModel")
	}
	if err := d.db.Where("created_at < ?", lastYear).Delete(new(apistructs.ProjectResourceDailyModel)).Error; err != nil {
		logrus.Warnln("failed to clear expire data from ProjectResourceDailyModel")
	}
}

type DailyQuotaCollectorOption func(collector *DailyQuotaCollector)

func DailyQuotaCollectorWithDBClient(db *dbclient.DBClient) DailyQuotaCollectorOption {
	return func(c *DailyQuotaCollector) {
		c.db = db
	}
}

func DailyQuotaCollectorWithBundle(bndl *bundle.Bundle) DailyQuotaCollectorOption {
	return func(c *DailyQuotaCollector) {
		c.bdl = bndl
	}
}

func DailyQuotaCollectorWithCMPAPI(cmp interface {
	ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error)
	GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error)
	GetClustersResources(ctx context.Context, cReq *pb.GetClustersResourcesRequest) (*pb.GetClusterResourcesResponse, error)
	GetAllClusters() []string
}) DailyQuotaCollectorOption {
	return func(c *DailyQuotaCollector) {
		c.cmp = cmp
	}
}
