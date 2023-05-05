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

package dbclient

import (
	"crypto/md5" // #nosec G501
	"database/sql/driver"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"hash/fnv"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// ServiceGroup is the common resource based on which deployments are created
// projectId, git branch and working dev determine a runtime
type Runtime struct {
	dbengine.BaseModel
	Name                string `gorm:"not null;unique_index:idx_unique_app_id_name"`
	ApplicationID       uint64 `gorm:"not null;unique_index:idx_unique_app_id_name"`
	Workspace           string `gorm:"not null;unique_index:idx_unique_app_id_name"`
	GitBranch           string // Deprecated
	ProjectID           uint64 `gorm:"not null"` // TODO: currently equal to applicationID, fix later
	Env                 string // Deprecated
	ClusterName         string
	ClusterId           uint64 // Deprecated: use clusterName
	Creator             string `gorm:"not null"`
	ScheduleName        ScheduleName
	Status              string `gorm:"column:runtime_status"`
	DeploymentStatus    apistructs.DeploymentStatus
	CurrentDeploymentID uint64
	DeploymentOrderId   string
	ReleaseVersion      string
	LegacyStatus        string `gorm:"column:status"`
	FileToken           string
	Deployed            bool
	Deleting            bool `gorm:"-"` // TODO: after legacyStatus removed, we use deleting instead
	Version             string
	Source              apistructs.RuntimeSource
	DiceVersion         string
	CPU                 float64
	Mem                 float64 // 单位: MB
	ConfigUpdatedDate   *time.Time
	// Deprecated
	ReadableUniqueId string
	// Deprecated
	GitRepoAbbrev string
	OrgID         uint64 `gorm:"not null"`
	ExtraParams   string
}

const (
	LegacyStatusDeleting = "DELETING"
)

func (Runtime) TableName() string {
	return "ps_v2_project_runtimes"
}

type ScheduleName struct {
	Namespace string
	Name      string
}

func (r *Runtime) InitScheduleName(clusterType string) {
	name := fnvV(fmt.Sprintf("%d-%s-%s", r.ApplicationID, r.Workspace, r.Name))
	if clusterType == apistructs.EDAS {
		name = fmt.Sprintf("%s-%d", strings.ToLower(r.Workspace), r.ID)
	}
	r.ScheduleName = ScheduleName{
		Namespace: "services",
		Name:      name,
	}
}

// fnvV 生成10位的哈希值
func fnvV(s string) string {
	h := fnv.New64a()
	h.Write([]byte(s))
	return hex.EncodeToString(h.Sum(nil))[:10]
}

// md5V md5加密
func md5V(str string) string {
	h := md5.New() // #nosec G401
	h.Write([]byte(str))
	return hex.EncodeToString(h.Sum(nil))
}

func (s ScheduleName) Value() (driver.Value, error) {
	if s.Namespace == "" || s.Name == "" {
		return nil, nil
	}
	return strutil.Concat(s.Namespace, "/", s.Name), nil
}

func (s *ScheduleName) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	var str string
	switch v := value.(type) {
	case string:
		str = v
	case []byte:
		str = string(v)
	default:
		str = fmt.Sprintf("%v", v)
	}
	if str == "" {
		return nil
	}
	v := strutil.Split(str, "/", true)
	if len(v) != 2 {
		return errors.Errorf("scheduleName not format: %s", str)
	}
	s.Namespace = v[0]
	s.Name = v[1]
	return nil
}

func (s ScheduleName) Args() (string, string) {
	return s.Namespace, s.Name
}

type RuntimeService struct {
	dbengine.BaseModel
	RuntimeId   uint64 `gorm:"not null;unique_index:idx_runtime_id_service_name"`
	ServiceName string `gorm:"not null;unique_index:idx_runtime_id_service_name"`
	Cpu         string
	Mem         int
	Environment string `gorm:"type:text"`
	Ports       string
	Replica     int
	Status      string
	Errors      string `gorm:"type:text"`
}

// TableName runtime service 表名
func (RuntimeService) TableName() string {
	return "ps_runtime_services"
}

// RuntimeDomain indicated default and custom domain for endpoints
type RuntimeDomain struct {
	dbengine.BaseModel
	RuntimeId    uint64 `gorm:"not null"`
	Domain       string `gorm:"unique_index:unique_domain_key"`
	DomainType   string
	EndpointName string
	UseHttps     bool
}

func (RuntimeDomain) TableName() string {
	return "ps_v2_domains"
}

func (db *DBClient) CreateRuntime(runtime *Runtime) error {
	return db.Save(runtime).Error
}

// if not found, return (nil, error)
func (db *DBClient) GetRuntime(id uint64) (*Runtime, error) {
	var runtime Runtime
	if err := db.
		Where("id = ?", id).
		Find(&runtime).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get runtime by id: %d", id)
	}
	return &runtime, nil
}

func (db *DBClient) GetRuntimeAllowNil(id uint64) (*Runtime, error) {
	var runtime Runtime
	result := db.
		Where("id = ?", id).
		Find(&runtime)
	if result.Error != nil {
		if result.RecordNotFound() {
			return nil, nil
		}
		return nil, errors.Wrapf(result.Error, "failed to get runtime by id: %d", id)
	}
	return &runtime, nil
}

// if not found, return (nil, error)
func (db *DBClient) GetRuntimeByScheduleName(scheduleName string) (*Runtime, error) {
	runtime := &Runtime{}
	result := db.Where("schedule_name = ?", scheduleName).First(runtime)
	if result.Error != nil {
		return nil, errors.Wrapf(result.Error, "failed to get runtime by scheduleName: %v", scheduleName)
	}
	return runtime, nil
}

// if not found, return (nil, nil)
func (db *DBClient) FindRuntime(uniqueId spec.RuntimeUniqueId) (*Runtime, error) {
	var runtime Runtime
	result := db.
		Where("application_id = ? AND workspace = ? AND name = ?",
			uniqueId.ApplicationId, uniqueId.Workspace, uniqueId.Name).
		Take(&runtime)
	if result.Error != nil {
		if result.RecordNotFound() {
			return nil, nil
		}
		return nil, errors.Wrapf(result.Error, "failed to find runtime by uniqueId: %v", uniqueId)
	}
	return &runtime, nil
}

func (db *DBClient) FindRuntimesByIds(ids []uint64) ([]Runtime, error) {
	var runtimes []Runtime
	if len(ids) == 0 {
		return runtimes, nil
	}
	if err := db.
		Where("id in (?)", ids).
		Find(&runtimes).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find runtimes by Ids: %v", ids)
	}
	return runtimes, nil
}

func (db *DBClient) FindRuntimesByAppId(appId uint64) ([]Runtime, error) {
	var runtimes []Runtime
	if appId <= 0 {
		return runtimes, nil
	}
	if err := db.
		Where("application_id = ?", appId).
		Find(&runtimes).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find runtimes by appId: %v", appId)
	}
	return runtimes, nil
}

func (db *DBClient) FindRuntimesByAppIdAndWorkspace(appId uint64, workspace string) ([]Runtime, error) {
	var runtimes []Runtime
	if appId <= 0 {
		return runtimes, nil
	}
	if err := db.
		Where("application_id = ?  AND workspace = ?", appId, workspace).
		Find(&runtimes).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find runtimes by appId: %v", appId)
	}
	return runtimes, nil
}

// FindRuntimesInApps finds all runtimes for the given appIDs.
// The key in the returned map is appID.
func (db *DBClient) FindRuntimesInApps(appIDs []uint64, env string) (map[uint64][]*Runtime, []uint64, error) {
	var (
		runtimes []*Runtime
		m        = make(map[uint64][]*Runtime)
		ids      []uint64
	)
	if env != "" {
		if err := db.Where("application_id IN (?) AND workspace = ? ", appIDs, env).
			Find(&runtimes).Error; err != nil && !gorm.IsRecordNotFoundError(err) {
			return nil, nil, err
		}
	} else {
		if err := db.Where("application_id IN (?) ", appIDs).
			Find(&runtimes).Error; err != nil && !gorm.IsRecordNotFoundError(err) {
			return nil, nil, err
		}
	}
	for _, runtime := range runtimes {
		m[runtime.ApplicationID] = append(m[runtime.ApplicationID], runtime)
		ids = append(ids, runtime.ID)
	}
	return m, ids, nil
}

func (db *DBClient) FindRuntimeOrCreate(uniqueId spec.RuntimeUniqueId, operator string, source apistructs.RuntimeSource,
	clusterName string, clusterId uint64, gitRepoAbbrev string, projectID, orgID uint64, deploymentOrderId,
	releaseVersion, extraParams string) (*Runtime, bool, error) {

	runtime, err := db.FindRuntime(uniqueId)
	if err != nil {
		return nil, false, errors.Wrapf(err, "failed to find runtime or create by uniqueId: %v, operator: %v",
			uniqueId, operator)
	}

	created := false
	if runtime == nil {
		created = true
		runtime = &Runtime{
			ApplicationID:     uniqueId.ApplicationId,
			ProjectID:         projectID, // TODO: currently equal to applicationID, fix later
			Creator:           operator,
			Workspace:         uniqueId.Workspace,
			Env:               uniqueId.Workspace,
			Name:              uniqueId.Name,
			GitBranch:         uniqueId.Name,
			Status:            "Init",
			LegacyStatus:      "INIT",
			Source:            source,
			Deleting:          false,
			Deployed:          false,
			Version:           "1",
			DiceVersion:       "2",
			ClusterName:       clusterName,
			ClusterId:         clusterId,
			ReadableUniqueId:  "dice-orchestrator",
			GitRepoAbbrev:     gitRepoAbbrev,
			Mem:               0.0,
			CPU:               0.0,
			OrgID:             orgID,
			DeploymentOrderId: deploymentOrderId,
			ReleaseVersion:    releaseVersion,
			ExtraParams:       extraParams,
		}
		err = db.CreateRuntime(runtime)
		if err != nil {
			return nil, created, errors.Wrapf(err, "failed to find runtime or create by uniqueId: %v, operator: %v",
				uniqueId, operator)
		}
	} else {
		// update deployment order name
		if runtime.DeploymentOrderId != deploymentOrderId || runtime.ReleaseVersion != releaseVersion || runtime.ExtraParams != extraParams {
			runtime.DeploymentOrderId = deploymentOrderId
			runtime.ReleaseVersion = releaseVersion
			runtime.ExtraParams = extraParams
			if err := db.UpdateRuntime(runtime); err != nil {
				return nil, false, errors.Wrapf(err, "failed to update runtime deployment order or release info, err: %v", err)
			}
		}
	}
	return runtime, created, nil
}

func (db *DBClient) FindDeletingRuntimes() ([]Runtime, error) {
	var runtimes []Runtime
	if err := db.
		Where("status = 'DELETING'").
		Find(&runtimes).Error; err != nil {
		return nil, errors.Wrap(err, "failed to find deleting runtimes")
	}
	return runtimes, nil
}

// find runtimes newer than minId (id > minId)
func (db *DBClient) FindRuntimesNewerThan(minId uint64, limit int) ([]Runtime, error) {
	var runtimes []Runtime
	if err := db.
		Where("id > ?", minId).
		Limit(limit).
		Find(&runtimes).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to find runtimes after: %d", minId)
	}
	return runtimes, nil
}

func (db *DBClient) UpdateRuntime(runtime *Runtime) error {
	if err := db.Save(runtime).Error; err != nil {
		return errors.Wrapf(err, "failed to update runtime, id: %v", runtime.ID)
	}
	return nil
}

func (db *DBClient) UpdateRuntimeDeploymentInfo(runtimeId, curDeploymentId uint64, status apistructs.DeploymentStatus) error {
	if err := db.Model(&Runtime{}).Where("id = ?", runtimeId).Update(&Runtime{
		DeploymentStatus:    status,
		CurrentDeploymentID: curDeploymentId,
	}).Error; err != nil {
		return errors.Wrapf(err, "failed to update runtime deployment status, id: %v", runtimeId)
	}
	return nil
}

func (db *DBClient) DeleteRuntime(runtimeId uint64) error {
	if err := db.
		Where("id = ?", runtimeId).
		Delete(&Runtime{}).Error; err != nil {
		return errors.Wrapf(err, "failed to delete runtime: %v", runtimeId)
	}
	return nil
}

// ListRuntimeByCluster 根据 clusterName 查找 runtime 列表
func (db *DBClient) ListRuntimeByCluster(clusterName string) ([]Runtime, error) {
	var runtimes []Runtime
	if err := db.Where("cluster_name = ?", clusterName).Find(&runtimes).Error; err != nil {
		return nil, err
	}

	return runtimes, nil
}

// ListRuntimeByOrgCluster 根据 clusterName 和 orgID 查找 runtime 列表
func (db *DBClient) ListRuntimeByOrgCluster(clusterName string, orgID uint64) ([]Runtime, error) {
	var runtimes []Runtime
	if err := db.Where("cluster_name = ? AND org_id = ?", clusterName, orgID).Find(&runtimes).Error; err != nil {
		return nil, err
	}

	return runtimes, nil
}

func (db *DBClient) CreateOrUpdateRuntimeService(service *RuntimeService, overrideStatus bool) error {
	var old RuntimeService
	result := db.
		Where("runtime_id = ? AND service_name = ?", service.RuntimeId, service.ServiceName).
		Take(&old)
	if result.Error != nil {
		if result.RecordNotFound() { // Create
			if err := db.Save(service).Error; err != nil {
				return errors.Wrap(err, "failed to CreateOrUpdateRuntimeService")
			}
			return nil
		}
		return errors.Wrap(result.Error, "failed to CreateOrUpdateRuntimeService")
	} else { // Update
		service.ID = old.ID
		service.CreatedAt = old.CreatedAt
		service.UpdatedAt = old.UpdatedAt
		service.Errors = old.Errors // TODO: should we change errors or not ?
		if !overrideStatus {
			// not override status, still use old.StatusDetail
			service.Status = old.Status
		}
		if err := db.Save(service).Error; err != nil {
			return errors.Wrap(err, "failed to CreateOrUpdateRuntimeService")
		}
	}
	return nil
}

func (db *DBClient) ClearRuntimeServiceErrors(serviceId uint64) error {
	if err := db.Model(&RuntimeService{}).
		Where("id = ?", serviceId).
		Update("errors", "").Error; err != nil {
		return errors.Wrapf(err, "failed to clear RuntimeService errors, serviceId: %v", serviceId)
	}
	return nil
}

func (db *DBClient) SetRuntimeServiceErrors(serviceId uint64, errs []apistructs.ErrorResponse) error {
	b, err := json.Marshal(errs)
	if err != nil {
		return errors.Wrapf(err, "failed to set RuntimeService errors, marshal failed, serviceId: %v, errs: %v",
			serviceId, errs)
	}
	if err := db.Model(&RuntimeService{}).
		Where("id = ?", serviceId).
		Update("errors", string(b)).Error; err != nil {
		return errors.Wrapf(err, "failed to set RuntimeService errors, serviceId: %v, errs: %v",
			serviceId, errs)
	}
	return nil
}

func (db *DBClient) FindRuntimeServices(runtimeId uint64) ([]RuntimeService, error) {
	var services []RuntimeService
	if err := db.
		Where("runtime_id = ?", runtimeId).
		Find(&services).Error; err != nil {
		return nil, err
	}
	return services, nil
}

// GetRuntimeByProjectIDs 通过projectIDs获取对应runtime
func (db *DBClient) GetRuntimeByProjectIDs(projectIDs []uint64) (*[]Runtime, error) {
	var runtimes []Runtime
	if err := db.Where("project_id in (?)", projectIDs).Find(&runtimes).Error; err != nil {
		return nil, err
	}
	return &runtimes, nil
}

func (db *DBClient) GetRuntimeByDeployOrderId(projectId uint64, orderId string) (*[]Runtime, error) {
	var runtimes []Runtime
	if err := db.Where("project_id = ? and deployment_order_id = ?", projectId, orderId).
		Find(&runtimes).Error; err != nil {
		return nil, err
	}
	return &runtimes, nil
}

func (db *DBClient) GetProjectRuntimeNumberByWorkspace(projectId uint64, env string) (uint64, error) {
	var num uint64
	if err := db.Model(Runtime{}).Where("project_id = ? and workspace = ?", projectId, env).
		Count(&num).Error; err != nil {
		return 0, err
	}
	return num, nil
}

func (db *DBClient) GetAppRuntimeNumberByWorkspace(projectId uint64, env string) (uint64, error) {
	var num uint64
	if err := db.Model(Runtime{}).Where("application_id = ? and workspace = ?", projectId, env).
		Count(&num).Error; err != nil {
		return 0, err
	}
	return num, nil
}

// TODO: we no need app, just redundant fields into runtime table
func ConvertRuntimeDTO(runtime *Runtime, app *apistructs.ApplicationDTO) *apistructs.RuntimeDTO {
	return &apistructs.RuntimeDTO{
		ID:              runtime.ID,
		Name:            runtime.Name,
		GitBranch:       runtime.Name,
		Workspace:       runtime.Workspace,
		ClusterName:     runtime.ClusterName,
		Status:          runtime.Status,
		ClusterId:       runtime.ClusterId,
		ApplicationID:   runtime.ApplicationID,
		ApplicationName: app.Name,
		ProjectID:       app.ProjectID,
		ProjectName:     app.ProjectName,
		OrgID:           app.OrgID,
	}
}

// CountServiceReferenceByClusterAndOrg 统计集群中service数量
func (db *DBClient) CountServiceReferenceByClusterAndOrg(clusterName, orgID string) (int, error) {
	var total int
	if err := db.Where("org_id = ?", orgID).
		Where("cluster_name = ?", clusterName).
		Model(&Runtime{}).
		Count(&total).Error; err != nil {
		return 0, err
	}
	return total, nil
}

func (db *DBClient) ListRuntimesByAppsName(env string, projectId uint64, appsName []string) (*[]Runtime, error) {
	var runtimes []Runtime
	if err := db.Model(&Runtime{}).Where("project_id = ? and env = ?", projectId, strings.ToUpper(env)).
		Where("name in (?)", appsName).Find(&runtimes).Error; err != nil {
		return nil, err
	}
	return &runtimes, nil
}

func (db *DBClient) GetRuntimeByAppName(env string, projectId uint64, appName string) (*Runtime, error) {
	var runtime Runtime
	if err := db.Model(&Runtime{}).Where("project_id = ? and env = ?", projectId, strings.ToUpper(env)).
		Where("name = ?", appName).Find(&runtime).Error; err != nil {
		return nil, err
	}
	return &runtime, nil
}
