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

package deployment

import (
	"context"
	"crypto/md5" // #nosec G501
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/environment"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/log"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/migration"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/resource"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/sexp"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/template"
)

type DeployFSMContext struct {
	Deployment        *dbclient.Deployment
	Runtime           *dbclient.Runtime
	Cluster           *clusterpb.ClusterInfo
	App               *apistructs.ApplicationDTO
	Spec              *diceyml.Object
	ProjectNamespaces map[string]string

	deploymentID uint64

	// deployment logger
	d *log.DeployLogHelper

	// db and etc.
	db    *dbclient.DBClient
	evMgr *events.EventManager
	bdl   *bundle.Bundle
	// TODO: should we put deployment.Deployment here?
	addon            *addon.Addon
	migration        *migration.Migration
	resource         *resource.Resource
	encrypt          *encryption.EnvEncrypt
	releaseSvc       pb.ReleaseServiceServer
	serviceGroupImpl servicegroup.ServiceGroup
	scheduler        *scheduler.Scheduler
	envConfig        *environment.EnvConfig
	clusterSvc       clusterpb.ClusterServiceServer
}

// TODO: context should base on deployment service
func NewFSMContext(deploymentID uint64, db *dbclient.DBClient, evMgr *events.EventManager, bdl *bundle.Bundle, a *addon.Addon, m *migration.Migration, encrypt *encryption.EnvEncrypt, resource *resource.Resource, releaseSvc pb.ReleaseServiceServer, serviceGroupImpl servicegroup.ServiceGroup, scheduler *scheduler.Scheduler, envConfig *environment.EnvConfig, clusterSvc clusterpb.ClusterServiceServer) *DeployFSMContext {
	logger := log.DeployLogHelper{DeploymentID: strconv.FormatUint(deploymentID, 10), Bdl: bdl}
	// prepare the context
	return &DeployFSMContext{
		deploymentID:     deploymentID,
		d:                &logger,
		db:               db,
		evMgr:            evMgr,
		bdl:              bdl,
		addon:            a,
		migration:        m,
		encrypt:          encrypt,
		resource:         resource,
		releaseSvc:       releaseSvc,
		serviceGroupImpl: serviceGroupImpl,
		scheduler:        scheduler,
		envConfig:        envConfig,
		clusterSvc:       clusterSvc,
	}
}

func (fsm *DeployFSMContext) Load() error {
	deployment, err := fsm.db.GetDeployment(fsm.deploymentID)
	if err != nil {
		return err
	}
	// TODO: useless work
	var dice diceyml.Object
	if err := json.Unmarshal([]byte(deployment.Dice), &dice); err != nil {
		return fsm.failDeploy(err)
	}
	runtime, err := fsm.db.GetRuntime(deployment.RuntimeId)
	if err != nil {
		return err
	}
	runtime.CurrentDeploymentID = fsm.deploymentID
	if err := fsm.db.UpdateRuntime(runtime); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment status for runtime: %v", err)
		logrus.Errorf("%s", errMsg)
		return errors.Errorf("%s", errMsg)
	}
	if len(runtime.ClusterName) == 0 {
		return errors.Errorf("cluster_name null, runtimeID: %v", runtime.ID)
	}
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := fsm.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: runtime.ClusterName})
	if err != nil {
		return err
	}
	cluster := resp.Data
	app, err := fsm.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return err
	}
	nsinfo, err := fsm.bdl.GetProjectNamespaceInfo(runtime.ProjectID)
	if err != nil {
		return err
	}

	fsm.Deployment = deployment
	fsm.Runtime = runtime
	fsm.Cluster = cluster
	fsm.App = app
	fsm.Spec = &dice
	fsm.ProjectNamespaces = nsinfo.Namespaces
	if len(fsm.Runtime.ScheduleName.Name) == 10 || fsm.Runtime.ScheduleName.Name == "" {
		pid := strconv.FormatUint(fsm.Runtime.ProjectID, 10)
		fsm.ProjectNamespaces = fsm.genProjectNamespace(pid)
	}
	return nil
}

// GetProjectNamespace 获取项目命名空间
func (fsm *DeployFSMContext) GetProjectNamespace(workspace string) string {
	prjNS, ok := fsm.ProjectNamespaces[workspace]
	if ok {
		return prjNS
	}

	return ""
}

// IsEnabledProjectNamespace 是否开启了项目命名空间
func (fsm *DeployFSMContext) IsEnabledProjectNamespace() bool {
	if len(fsm.ProjectNamespaces) != 4 {
		return false
	}
	return true
}

func (fsm *DeployFSMContext) timeout() (bool, error) {
	now := time.Now()
	if now.Sub(fsm.Deployment.UpdatedAt) > 1*time.Hour {
		fsm.Deployment.Extra.AutoTimeout = true
		return true, fsm.failDeploy(errors.Errorf("deployment timeout (>1hr)"))
	}
	return false, nil
}

// precheck precheck dice.yml or other thing
func (fsm *DeployFSMContext) precheck() error {
	// check service name
	var invalidSvcName []string
	for name := range fsm.Spec.Services {
		if !utils.IsValidK8sSvcName(name) {
			invalidSvcName = append(invalidSvcName, name)
		}
	}
	if len(invalidSvcName) != 0 {
		svcNames := strings.Join(invalidSvcName, ",")
		fsm.bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
			ErrorLog: apistructs.ErrorLog{
				Level:          apistructs.ErrorLevel,
				ResourceType:   apistructs.RuntimeError,
				ResourceID:     strconv.FormatUint(fsm.Runtime.ID, 10),
				OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
				HumanLog:       i18n.OrgSprintf(strconv.FormatUint(fsm.Runtime.OrgID, 10), "InvalidServiceName", svcNames),
				PrimevalLog: `The service name should conform to the following specifications:
1. contain at most 63 characters 2. contain only lowercase alphanumeric characters or '-'
3. start with an alphanumeric character 4. end with an alphanumeric character`,
				DedupID: fmt.Sprintf("orch-%d", fsm.Runtime.ID),
			},
		})
		return errors.Errorf("invalid service name: %s", svcNames)
	}

	return nil
}

func (fsm *DeployFSMContext) continueWaiting() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusWaiting {
		return nil
	}
	fsm.pushLog(`start deploying...`)
	fsm.Deployment.Status = apistructs.DeploymentStatusDeploying
	fsm.Deployment.Phase = apistructs.DeploymentPhaseInit
	fsm.Deployment.FailCause = "" // clear fail (for test)
	if err := fsm.db.UpdateDeployment(fsm.Deployment); err != nil {
		return err
	}
	if err := fsm.UpdateDeploymentStatusToRuntimeAndOrder(); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment status for runtime: %v", err)
		logrus.Errorf("%s", errMsg)
		fsm.pushLog(errMsg)
		return err
	}
	if len(fsm.Deployment.ReleaseId) > 0 {
		fsm.pushLog("increasing release reference...")
		ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		if _, err := fsm.releaseSvc.UpdateReleaseReference(ctx, &pb.ReleaseReferenceUpdateRequest{
			ReleaseID: fsm.Deployment.ReleaseId,
			Increase:  true,
		}); err != nil {
			return fsm.failDeploy(err)
		}
	}
	// emit runtime deploy status changed event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployStatusChanged,
		Operator:   fsm.Deployment.Operator,
		Runtime:    dbclient.ConvertRuntimeDTO(fsm.Runtime, fsm.App),
		Deployment: fsm.Deployment.Convert(),
	}
	fsm.evMgr.EmitEvent(&event)
	return nil
}

func (fsm *DeployFSMContext) continueDeploying() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying {
		return nil
	}
	// TODO: we should introduce custom flow instead of switch-case
	switch fsm.Deployment.Phase {
	case apistructs.DeploymentPhaseInit:
		return fsm.continuePhasePreAddon()
	case apistructs.DeploymentPhaseAddon:
		return fsm.continuePhaseAddon()
	case apistructs.DeploymentPhaseScript:
		return fsm.continuePhasePreService()
	case apistructs.DeploymentPhaseService:
		return fsm.continuePhaseService()
	case apistructs.DeploymentPhaseRegister:
		return fsm.continuePhaseRegister()
	case apistructs.DeploymentPhaseCompleted:
		return fsm.continuePhaseCompleted()
	default:
		return nil
	}
}

func (fsm *DeployFSMContext) continueCanceling() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusCanceling {
		return nil
	}
	if fsm.Deployment.Extra.CancelStartAt == nil {
		if fsm.Deployment.Phase == apistructs.DeploymentPhaseService {
			now := time.Now()
			// set start at before invoke scheduler (if error occur, we can keep the startAt)
			fsm.Deployment.Extra.CancelStartAt = &now
			if _, err := fsm.scheduler.CancelServiceGroup(fsm.Runtime.ScheduleName.Args()); err != nil {
				return fsm.failDeploy(err)
			}
			if err := fsm.db.UpdateDeployment(fsm.Deployment); err != nil {
				return err
			}
		} else {
			// cancel directly
			fsm.pushOnCanceled()
		}
		return nil
	}
	fsm.pushLog(`deployment canceling...`)
	if p, err := fsm.checkCancelOk(); err != nil {
		return fsm.failDeploy(err)
	} else {
		if p {
			return fsm.pushOnCanceled()
		}
	}
	return nil
}

func (fsm *DeployFSMContext) pushOnCanceled() error {
	fsm.pushLog("deployment canceled")
	fsm.Deployment.Status = apistructs.DeploymentStatusCanceled
	now := time.Now()
	if fsm.Deployment.Extra.CancelStartAt == nil {
		// for directly cancel
		fsm.Deployment.Extra.CancelStartAt = &now
	}
	fsm.Deployment.Extra.CancelEndAt = &now
	fsm.Deployment.FinishedAt = &now
	if err := fsm.db.UpdateDeployment(fsm.Deployment); err != nil {
		return err
	}
	if err := fsm.UpdateDeploymentStatusToRuntimeAndOrder(); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment status for runtime: %v", err)
		logrus.Errorf("%s", errMsg)
		fsm.pushLog(errMsg)
	}
	// emit runtime deploy status changed event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployCanceled,
		Operator:   fsm.Deployment.Operator,
		Runtime:    dbclient.ConvertRuntimeDTO(fsm.Runtime, fsm.App),
		Deployment: fsm.Deployment.Convert(),
	}
	fsm.evMgr.EmitEvent(&event)
	return nil
}

func (fsm *DeployFSMContext) continuePhaseInit() error {
	// TODO: should step in next phase
	return nil
}

// TODO: should combine into continuePhaseAddon
func (fsm *DeployFSMContext) continuePhasePreAddon() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying ||
		fsm.Deployment.Phase != apistructs.DeploymentPhaseInit {
		return nil
	}
	fsm.pushLog(`request addon resources...`)
	if err := fsm.requestAddons(); err != nil {
		return fsm.failDeploy(err)
	}
	fsm.pushLog(`accepted addon resources requesting, now waiting for addon ready...`)
	if err := fsm.pushOnPhase(apistructs.DeploymentPhaseAddon); err != nil {
		return err
	}
	return nil
}

func (fsm *DeployFSMContext) continuePhaseAddon() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying ||
		fsm.Deployment.Phase != apistructs.DeploymentPhaseAddon {
		return nil
	}
	fsm.pushLog(` * checking addon...`)

	logrus.Info("start waiting for addon deploying")
	status, err := fsm.addon.RuntimeAddonStatus(strconv.FormatUint(fsm.Deployment.RuntimeId, 10))
	switch status {
	case 0:
		// the error will only occur when the status is 0
		return fsm.failDeploy(errors.Errorf("received addon request failed, status is 0(fail), errMsg: %v", err))
	case 1:
		// success
		fsm.pushLog(`addon is ready, now waiting for data migration...`)
		if err := fsm.pushOnPhase(apistructs.DeploymentPhaseScript); err != nil {
			return err
		}
	case 2:
		// still progressing, do nothing
		// TODO: support delay task in queue
	default:
		return fsm.failDeploy(errors.Errorf("received addon request, unknown status %d", status))
	}
	return nil
}

func (fsm *DeployFSMContext) continuePhasePreService() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying ||
		fsm.Deployment.Phase != apistructs.DeploymentPhaseScript {
		return nil
	}
	migrationStatus, err := fsm.continueMigration()
	if err != nil {
		return fsm.failDeploy(err)
	}
	if migrationStatus == apistructs.MigrationStatusRunning || migrationStatus == apistructs.MigrationStatusInit {
		return nil
	}
	fsm.pushLog(`prepare default domain...`)
	// TODO: create default domain should be one phase
	var expose bool
	for name, serv := range fsm.Spec.Services {
		// serv.Expose will abandoned, serv.Ports.Expose is recommended
		for _, svcPort := range serv.Ports {
			if svcPort.Expose {
				expose = true
				break
			}
		}
		if len(serv.Expose) != 0 || expose {
			rootDomains := strings.Split(fsm.Cluster.WildcardDomain, ",")
			if len(rootDomains) < 1 {
				return errors.Errorf("domain not exist, cluster %s", fsm.Cluster.Name)
			}
			rootDomain := rootDomains[0]
			if _, err := fsm.db.GetDefaultDomainOrCreate(fsm.Runtime.ID, name,
				// TODO: default domain format should be refactored
				fmt.Sprintf("%s-%s-%d-app.%s", name, strings.ToLower(fsm.Runtime.Workspace), fsm.Runtime.ID, rootDomain)); err != nil {
				return err
			}
		}
	}

	// do start deploying
	fsm.pushLog(`service deploying...`)
	if err := fsm.deployService(); err != nil {
		return fsm.failDeploy(err)
	}

	// pushOn Phase
	if err := fsm.pushOnPhase(apistructs.DeploymentPhaseService); err != nil {
		return err
	}
	return nil
}

// continueMigration migration信息处理
func (fsm *DeployFSMContext) continueMigration() (string, error) {
	fsm.pushLog(`start migration...`)

	mig, err := fsm.db.GetMigrationLogByDeploymentID(fsm.Deployment.ID)
	if err != nil {
		return "", err
	}
	// 该deployment已经存在对应的migrationLog信息，则判断该migration job的状态
	if mig != nil {
		fsm.pushLog(`migration job already existed`)
		if mig.Status == apistructs.MigrationStatusFail {
			return "", errors.Errorf("migration job failed, please rebuild the service")
		}
		if mig.Status == apistructs.MigrationStatusFinish {
			fsm.pushLog(`migration job already finish, keep going deployment...`)
			return apistructs.MigrationStatusFinish, nil
		}
		if mig.Status == apistructs.MigrationStatusInit || mig.Status == apistructs.MigrationStatusRunning {
			if err := fsm.handleMigrationStatus(mig); err != nil {
				logrus.Errorf("handle migration status error, msg is: %v", err)
				return "", err
			}
			migrationId := strconv.FormatUint(mig.ID, 10)
			fsm.pushLog("migration logs right here...##to_link:migrationId:" + migrationId)
			return apistructs.MigrationStatusRunning, nil
		}
	} else {
		logrus.Infof("没有找到migration相关信息, releaseId为：%s", fsm.Deployment.ReleaseId)
		ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		releaseResp, err := fsm.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: fsm.Deployment.ReleaseId})
		if err != nil {
			logrus.Errorf("get release error: %v", err)
			return "", err
		}
		if len(releaseResp.Data.Resources) == 0 {
			fsm.pushLog(`no migration found, keep going deployment...`)
			return "", nil
		}
		// 遍历resource数组，查看是否存在migration信息需要执行
		var resourceRelease apistructs.ReleaseResource
		for _, v := range releaseResp.Data.Resources {
			if v.Type == string(apistructs.ResourceTypeMigration) {
				meta := make(map[string]interface{})
				for key, value := range v.Meta {
					meta[key] = value.GetStringValue()
				}
				resourceRelease = apistructs.ReleaseResource{
					Type: apistructs.ResourceType(v.Type),
					Name: v.Name,
					URL:  v.URL,
					Meta: meta,
				}
			}
		}
		// 如果没有migration信息需要执行，则直接返回，不执行migration信息
		if resourceRelease.Name == "" {
			fsm.pushLog(`no migration found, keep going deployment...`)
			return "", nil
		}

		runtimeAddonPrebuilds, err := fsm.db.GetPreBuildsByRuntimeID(fsm.Runtime.ID)
		if err != nil {
			fsm.pushLog(`get runtime addon relation error...`)
			return "", err
		}
		if len(*runtimeAddonPrebuilds) == 0 {
			fsm.pushLog(`migration found, but not found mysql addon...`)
			return "", nil
		}
		var mysqlInstance dbclient.AddonInstance
		for _, v := range *runtimeAddonPrebuilds {
			if v.DeleteStatus == apistructs.AddonPrebuildNotDeleted && v.AddonName == apistructs.AddonMySQL {
				if v.InstanceID != "" {
					mysqlInstanceResult, err := fsm.decryptConfig(v.InstanceID)
					if err != nil {
						fsm.pushLog(`get mysql addon relation error...`)
						return "", err
					}
					mysqlInstance = *mysqlInstanceResult
				}
			}
		}
		if mysqlInstance.ID == "" {
			fsm.pushLog(`migration found, but not found mysql addon...`)
			return "", nil
		}
		// 保存migration执行记录
		operatorID, err := strconv.ParseUint(fsm.Deployment.Operator, 10, 64)
		baseMigrationLog := dbclient.MigrationLog{
			ProjectID:           fsm.App.ProjectID,
			ApplicationID:       fsm.App.ID,
			RuntimeID:           fsm.Runtime.ID,
			DeploymentID:        fsm.Deployment.ID,
			OperatorID:          operatorID,
			Status:              apistructs.MigrationStatusInit,
			AddonInstanceID:     mysqlInstance.ID,
			AddonInstanceConfig: mysqlInstance.Config,
		}
		if err := fsm.db.CreateMigrationLog(&baseMigrationLog); err != nil {
			fsm.pushLog(`create migration log error...`)
			return "", err
		}
		migrationDiceyml, err := fsm.bdl.GetDiceYAML(resourceRelease.URL)
		if err != nil {
			fsm.pushLog(`get migration diceyml error...`)
			return "", err
		}
		migrationDiceyml_aux := migrationDiceyml.Obj()
		for _, service := range migrationDiceyml_aux.Services {
			nexususer, err := fsm.bdl.GetNexusOrgDockerCredentialByImage(fsm.App.OrgID, service.Image)
			if err != nil {
				return "", err
			}
			if nexususer != nil {
				service.ImagePassword = nexususer.Password
				service.ImageUsername = nexususer.Name
			}
		}
		migrationDiceyml_bytes, _ := json.Marshal(migrationDiceyml_aux)
		migrationDiceyml, err = diceyml.New(migrationDiceyml_bytes, false)
		if err != nil {
			return "", err
		}
		ymlValue, err := migrationDiceyml.YAML()
		logrus.Infof("migration release way diceyml: %s", ymlValue)
		if err := fsm.migration.Start(&baseMigrationLog, migrationDiceyml, fsm.Runtime, fsm.App); err != nil {
			fsm.pushLog(`migration job start error...`)
			return "", err
		}
		baseMigrationId := strconv.FormatUint(baseMigrationLog.ID, 10)
		fsm.pushLog("migration logs right here...##to_link:migrationId:" + baseMigrationId)
		return apistructs.MigrationStatusInit, nil
	}

	return "", nil
}

// decryptConfig 环境变量解密
func (fsm *DeployFSMContext) decryptConfig(instanceID string) (*dbclient.AddonInstance, error) {
	mysqlInstanceResult, err := fsm.db.GetAddonInstance(instanceID)
	if err != nil {
		fsm.pushLog(`get mysql addon relation error...`)
		return nil, err
	}
	var configMap map[string]interface{}
	if err := json.Unmarshal([]byte(mysqlInstanceResult.Config), &configMap); err != nil {
		return nil, err
	}
	// password解密
	if mysqlInstanceResult.KmsKey != "" {
		for k, v := range configMap {
			if strings.Contains(strings.ToLower(k), "pass") || strings.Contains(strings.ToLower(k), "secret") {
				password := v.(string)
				decPwd, err := fsm.addon.DecryptPassword(&mysqlInstanceResult.KmsKey, password)
				if err != nil {
					logrus.Errorf("mysql password decript err, %v", err)
					return nil, err
				}
				configMap[k] = decPwd
			}
		}
	} else {
		if _, ok := configMap[apistructs.AddonPasswordHasEncripy]; ok {
			fsm.encrypt.DecryptAddonConfigMap(&configMap)
		}
	}

	logrus.Infof("migration decrypt config map info: %v", configMap)
	configJson, err := json.Marshal(configMap)
	if err != nil {
		return nil, err
	}
	mysqlInstanceResult.Config = string(configJson)
	return mysqlInstanceResult, nil
}

func (fsm *DeployFSMContext) handleMigrationStatus(mig *dbclient.MigrationLog) error {
	fsm.pushLog(`migration job running now, please wait job finish...`)
	migStatus, err := fsm.migration.Status(mig)
	if err != nil {
		return err
	}
	fsm.pushLog(fmt.Sprintf("migration job status: %s, describe: %s", migStatus.Status, migStatus.Desc))
	switch migStatus.Status {
	case apistructs.StatusCreated, apistructs.StatusUnschedulable:
		fsm.pushLog(`migration job is created, please wait for the run to complete...`)
	case apistructs.StatusRunning:
		fsm.pushLog(`migration job is running, please wait for completion...`)
		mig.Status = apistructs.MigrationStatusRunning
		if err := fsm.db.UpdateMigrationLog(mig); err != nil {
			logrus.Errorf("migration job update status error, %v", err)
		}
	case apistructs.StatusStoppedOnOK, apistructs.StatusFinished:
		fsm.pushLog(`migration job finish, keep service deploying...`)
		mig.Status = apistructs.MigrationStatusFinish
		if err := fsm.db.UpdateMigrationLog(mig); err != nil {
			logrus.Errorf("migration job update status error, %v", err)
		}
	case apistructs.StatusStoppedOnFailed, apistructs.StatusFailed, apistructs.StatusUnknown, apistructs.StatusError, apistructs.StatusStoppedByKilled, apistructs.StatusNotFoundInCluster:
		fsm.pushLog(`migration job fail, details are as follows: ` + migStatus.Desc)
		mig.Status = apistructs.MigrationStatusFail
		if err := fsm.db.UpdateMigrationLog(mig); err != nil {
			logrus.Errorf("migration job update status error, %v", err)
		}
		return errors.Errorf(`migration job fail, details are as follows: ` + migStatus.Desc)
	}
	return nil
}

func (fsm *DeployFSMContext) continuePhaseService() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying ||
		fsm.Deployment.Phase != apistructs.DeploymentPhaseService {
		return nil
	}
	fsm.pushLog(" * checking service...")
	if p, err := fsm.checkServiceReady(); err != nil {
		return fsm.failDeploy(err)
	} else {
		if p {
			fsm.pushLog("service is ready")
			if err := fsm.pushOnPhase(apistructs.DeploymentPhaseRegister); err != nil {
				return err
			}
		}
	}
	return nil
}

func (fsm *DeployFSMContext) continuePhaseRegister() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying ||
		fsm.Deployment.Phase != apistructs.DeploymentPhaseRegister {
		return nil
	}
	// pushOn Phase
	if err := fsm.pushOnPhase(apistructs.DeploymentPhaseCompleted); err != nil {
		return err
	}
	return nil
}

func (fsm *DeployFSMContext) continuePhaseCompleted() error {
	if fsm.Deployment.Status != apistructs.DeploymentStatusDeploying ||
		fsm.Deployment.Phase != apistructs.DeploymentPhaseCompleted {
		return nil
	}

	// 部署runtime之后，orchestrator需要将服务域名信息通过此接口提交给hepa
	if err := fsm.PutHepaService(); err != nil {
		fsm.pushLog(fmt.Sprintf("hepa request error (%v)", err))
		return err
	}

	if err := fsm.clearPreviousMySQLAccountState(); err != nil {
		fsm.pushLog(fmt.Sprintf("clear previous MySQL Account fialed, error (%v)", err))
	}

	fsm.pushLog(`Deployment Is READY`)
	fsm.Deployment.Status = apistructs.DeploymentStatusOK
	now := time.Now()
	fsm.Deployment.FinishedAt = &now
	if err := fsm.db.UpdateDeployment(fsm.Deployment); err != nil {
		// db update fail mess up everything!
		return err
	}
	if err := fsm.UpdateDeploymentStatusToRuntimeAndOrder(); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment status for runtime: %v", err)
		logrus.Errorf("%s", errMsg)
		fsm.pushLog(errMsg)
	}
	// emit runtime deploy ok event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployOk,
		Operator:   fsm.Deployment.Operator,
		Runtime:    dbclient.ConvertRuntimeDTO(fsm.Runtime, fsm.App),
		Deployment: fsm.Deployment.Convert(),
	}
	fsm.evMgr.EmitEvent(&event)
	return nil
}

func (fsm *DeployFSMContext) UpdateDeploymentStatusToRuntimeAndOrder() error {
	var (
		err error
		app *apistructs.ApplicationDTO
	)

	fsm.Runtime.DeploymentStatus = fsm.Deployment.Status
	if err := fsm.db.UpdateRuntimeDeploymentInfo(fsm.Runtime.ID, fsm.Deployment.ID, fsm.Deployment.Status); err != nil {
		logrus.Errorf("update runtime deployment status error: %v", err)
		return err
	}

	DeploymentOrderID := fsm.Deployment.DeploymentOrderId
	if app, err = fsm.bdl.GetApp(fsm.Runtime.ApplicationID); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment order status of deployment[%s]: %v",
			DeploymentOrderID, err)
		logrus.Errorf("%s", errMsg)
		return nil
	}
	appDeploymentStatus := apistructs.DeploymentOrderStatusItem{
		AppID:            app.ID,
		DeploymentID:     fsm.deploymentID,
		DeploymentStatus: fsm.Deployment.Status,
		RuntimeID:        fsm.Runtime.ID,
	}
	logrus.Infof("update deployment(%+v) status for app (%+v) to deployment_order (%+v) detail is: %+v",
		fsm.deploymentID, app.Name, DeploymentOrderID, appDeploymentStatus)
	if err := fsm.db.UpdateDeploymentOrderAppStatus(DeploymentOrderID,
		app.Name, appDeploymentStatus); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment order status of deployment[%s]: %v",
			DeploymentOrderID, err)
		logrus.Errorf("%s", errMsg)
		return nil
	}
	return nil
}

func (fsm *DeployFSMContext) pushOnPhase(phase apistructs.DeploymentPhase) error {
	deployment := fsm.Deployment
	runtime := fsm.Runtime
	app := fsm.App
	deployment.Phase = phase
	if err := fsm.db.UpdateDeployment(deployment); err != nil {
		// db update fail mess up everything!
		return err
	}
	// emit runtime deploy fail event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployStatusChanged,
		Operator:   deployment.Operator,
		Runtime:    dbclient.ConvertRuntimeDTO(runtime, app),
		Deployment: deployment.Convert(),
	}
	fsm.evMgr.EmitEvent(&event)
	return nil
}

func (fsm *DeployFSMContext) failDeploy(oriErr error) error {
	deployment := fsm.Deployment
	runtime := fsm.Runtime
	app := fsm.App
	fsm.pushLog(fmt.Sprintf("deployment is fail, status: %v, phrase: %v, (%v)",
		deployment.Status, deployment.Phase, oriErr))
	deployment.FailCause = oriErr.Error()
	deployment.Status = apistructs.DeploymentStatusFailed
	now := time.Now()
	deployment.FinishedAt = &now
	if err := fsm.db.UpdateDeployment(deployment); err != nil {
		// db update fail mess up everything!
		fsm.pushLog(fmt.Sprintf("failed to update deployment, (%v)", err))
		return err
	}
	if err := fsm.UpdateDeploymentStatusToRuntimeAndOrder(); err != nil {
		errMsg := fmt.Sprintf("failed to update deployment status for runtime: %v", err)
		logrus.Errorf("%s", errMsg)
		fsm.pushLog(errMsg)
	}
	// emit runtime deploy fail event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployFailed,
		Operator:   deployment.Operator,
		Runtime:    dbclient.ConvertRuntimeDTO(runtime, app),
		Deployment: deployment.Convert(),
	}
	fsm.evMgr.EmitEvent(&event)
	return nil
}

func (fsm *DeployFSMContext) requestAddons() error {
	deployment := fsm.Deployment
	runtime := fsm.Runtime
	app := fsm.App

	var baseAddons []apistructs.AddonCreateItem
	for name, a := range fsm.Spec.AddOns {
		if a == nil {
			fsm.pushLog(fmt.Sprintf("addon information is empty, jump over, addon name: %s", name))
			continue
		}
		plan := strings.SplitN(a.Plan, ":", 2)
		if len(plan) != 2 {
			return errors.Errorf("addon plan information is not compliant")
		}
		baseAddons = append(baseAddons, apistructs.AddonCreateItem{
			Name:    name,
			Type:    plan[0],
			Plan:    plan[1],
			Options: a.Options,
		})
	}

	addonReq := apistructs.AddonCreateRequest{
		OrgID:         app.OrgID,
		ProjectID:     app.ProjectID,
		ApplicationID: runtime.ApplicationID,
		Workspace:     runtime.Workspace,
		RuntimeID:     runtime.ID,
		RuntimeName:   runtime.Name,
		ClusterName:   runtime.ClusterName,
		Operator:      deployment.Operator,
		Addons:        baseAddons,
		Options: apistructs.AddonCreateOptions{
			OrgID:           strconv.FormatUint(app.OrgID, 10),
			OrgName:         app.OrgName,
			ProjectID:       strconv.FormatUint(app.ProjectID, 10),
			ProjectName:     app.ProjectName,
			ApplicationID:   strconv.FormatUint(runtime.ApplicationID, 10),
			ApplicationName: app.Name,
			Workspace:       runtime.Workspace,
			Env:             runtime.Workspace,
			RuntimeID:       strconv.FormatUint(runtime.ID, 10),
			RuntimeName:     runtime.Name,
			DeploymentID:    strconv.FormatUint(deployment.ID, 10), // used by addon-platform to put log in
			LogSource:       "deploy",
			ClusterName:     runtime.ClusterName,
		},
	}
	bb, err := json.Marshal(addonReq)
	if err != nil {
		return err
	}
	logrus.Infof("addon create request body: %v", string(bb))
	if err := fsm.addon.BatchCreate(&addonReq); err != nil {
		return errors.Wrapf(err, "failed to request addons, runtimeId %d", runtime.ID)
	}
	return nil
}

func (fsm *DeployFSMContext) genProjectNamespace(prjIDStr string) map[string]string {
	return map[string]string{"DEV": "project-" + prjIDStr + "-dev", "TEST": "project-" + prjIDStr + "-test",
		"STAGING": "project-" + prjIDStr + "-staging", "PROD": "project-" + prjIDStr + "-prod"}
}

func (fsm *DeployFSMContext) deployService() error {
	// make sure runtime must have scheduleName
	if fsm.Runtime.ScheduleName.Name == "" {
		// if no scheduleName, we set it
		ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
		resp, err := fsm.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: fsm.Runtime.ClusterName})
		if err != nil {
			return err
		}
		cluster := resp.Data
		fsm.Runtime.InitScheduleName(cluster.Type)
		if err := fsm.db.UpdateRuntime(fsm.Runtime); err != nil {
			return err
		}
	}

	// 计算项目预留资源，是否满足发布徐局
	deployNeedCpu, deployNeedMem, err := fsm.PrepareCheckProjectResource(fsm.App, fsm.App.ProjectID, fsm.Spec, fsm.Runtime)
	if err != nil {
		fsm.pushLog(err.Error())
		return apierrors.ErrCreateRuntime.InternalError(err)
	}
	if fsm.Runtime.CPU > 0.0 {
		fsm.Runtime.CPU += deployNeedCpu
		fsm.Runtime.Mem += deployNeedMem
	} else {
		fsm.Runtime.CPU = deployNeedCpu
		fsm.Runtime.Mem = deployNeedMem
	}
	if err := fsm.db.UpdateRuntime(fsm.Runtime); err != nil {
		return err
	}

	// prepare env context

	projectAddons, err := fsm.db.GetAliveProjectAddons(strconv.FormatUint(fsm.Runtime.ProjectID, 10), fsm.Runtime.ClusterName, fsm.Runtime.Workspace)
	if err != nil {
		return err
	}

	projectAddonTenants, err := fsm.db.ListAddonInstanceTenantByProjectIDs([]uint64{fsm.Runtime.ProjectID}, fsm.Runtime.Workspace)
	if err != nil {
		return err
	}

	projectECI := utils.IsProjectECIEnable(fsm.bdl, fsm.Runtime.ProjectID, fsm.Runtime.Workspace, fsm.Runtime.OrgID, fsm.Runtime.Creator)
	// generate request
	group := apistructs.ServiceGroupCreateV2Request{}
	usedAddonInsMap, usedAddonTenantMap, err := fsm.generateDeployServiceRequest(&group, *projectAddons, projectAddonTenants, projectECI)
	if err != nil {
		return err
	}
	if projectECI {
		// TODO: vendor need get by cluster
		utils.AddECIConfigToServiceGroupCreateV2Request(&group, apistructs.ECIVendorAlibaba)
	}

	// precheck，检查标签匹配，如果没有机器能匹配上，走下去也是pending的
	if len(group.DiceYml.Services) > 0 {
		precheckResp, err := fsm.serviceGroupImpl.Precheck(apistructs.ServiceGroupPrecheckRequest(group))
		if err != nil {
			fsm.pushLog(fmt.Sprintf("precheck service error, %s", err.Error()))
			return err
		}
		// 如果返回不ok，直接返回error
		if strings.ToLower(precheckResp.Status) != strings.ToLower(string(apistructs.DeploymentStatusOK)) {
			fsm.bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
				ErrorLog: apistructs.ErrorLog{
					Level:          apistructs.ErrorLevel,
					ResourceType:   apistructs.RuntimeError,
					ResourceID:     strconv.FormatUint(fsm.Runtime.ID, 10),
					OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
					HumanLog:       i18n.OrgSprintf(strconv.FormatUint(fsm.Runtime.OrgID, 10), "FailedToSchedule.NoNodeToDeploy"),
					PrimevalLog:    fmt.Sprintf("没有匹配的节点能部署, %s", precheckResp.Info),
					DedupID:        fmt.Sprintf("orch-%d", fsm.Runtime.ID),
				},
			})
			fsm.pushLog(fmt.Sprintf("No node resource information matches, %s", precheckResp.Info))
			return errors.Errorf("No node resource information matches, %s", precheckResp.Info)
		}
	}
	b, _ := json.Marshal(&group)
	logrus.Debugf("service group body: %s", string(b))

	// do deploy
	if fsm.Runtime.Deployed {
		if err = fsm.UpdateServiceGroupWithLoop(group); err != nil {
			return err
		}
	} else {
		if _, err := fsm.serviceGroupImpl.Create(group); err != nil {
			return err
		}
	}
	if !fsm.Runtime.Deployed {
		// after create group success, set deployed as true
		fsm.Runtime.Deployed = true
		if err := fsm.db.UpdateRuntime(fsm.Runtime); err != nil {
			return err
		}
	}
	// update addon_attachments
	attachements, err := fsm.db.GetAttachMentsByRuntimeID(fsm.Runtime.ID)
	if err != nil {
		return err
	}
	for _, attachment := range *attachements {
		var options map[string]string
		if err := json.Unmarshal([]byte(attachment.Options), &options); err != nil {
			logrus.Error(err)
			continue
		}
		_, from_sexp_env := options["FROM_SEXP_ENV"]
		if !from_sexp_env {
			continue
		}
		attachment.Deleted = apistructs.AddonDeleted
		if err := fsm.db.UpdateAttachment(&attachment); err != nil {
			logrus.Error(err)
			continue
		}
	}
	for _, ins := range usedAddonInsMap {
		attachment := dbclient.AddonAttachment{
			InstanceID:        ins.RealInstance,
			RoutingInstanceID: ins.ID,
			OrgID:             ins.OrgID,
			ProjectID:         ins.ProjectID,
			ApplicationID:     strconv.FormatUint(fsm.Runtime.ApplicationID, 10),
			RuntimeID:         strconv.FormatUint(fsm.Runtime.ID, 10),
			InsideAddon:       ins.InsideAddon,
			RuntimeName:       fsm.Runtime.Name,
			Deleted:           apistructs.AddonNotDeleted,
			CreatedAt:         time.Now(),
			UpdatedAt:         time.Now(),
			Options:           `{"FROM_SEXP_ENV":"true"}`,
		}
		if err := fsm.db.CreateAttachment(&attachment); err != nil {
			logrus.Error(err)
			continue
		}
	}
	for _, tenant := range usedAddonTenantMap {
		attachment := dbclient.AddonAttachment{
			TenantInstanceID: tenant.ID,
			OrgID:            tenant.OrgID,
			ProjectID:        tenant.ProjectID,
			ApplicationID:    strconv.FormatUint(fsm.Runtime.ApplicationID, 10),
			RuntimeID:        strconv.FormatUint(fsm.Runtime.ID, 10),
			InsideAddon:      apistructs.NOT_INSIDE,
			RuntimeName:      fsm.Runtime.Name,
			Deleted:          apistructs.AddonNotDeleted,
			CreatedAt:        time.Now(),
			UpdatedAt:        time.Now(),
			Options:          `{"FROM_SEXP_ENV":"true"}`,
		}
		if err := fsm.db.CreateAttachment(&attachment); err != nil {
			logrus.Error(err)
			continue
		}
	}

	return nil
}

func (fsm *DeployFSMContext) UpdateServiceGroupWithLoop(group apistructs.ServiceGroupCreateV2Request) error {
	if err := loop.New(loop.WithInterval(time.Second), loop.WithMaxTimes(3)).Do(func() (bool, error) {
		if _, err := fsm.serviceGroupImpl.Update(apistructs.ServiceGroupUpdateV2Request(group)); err != nil {
			return false, err
		}
		return true, nil
	}); err != nil {
		return err
	}
	return nil
}

func (fsm *DeployFSMContext) FetchDeploymentConfig(namespace string) (map[string]string, map[string]string, error) {
	envDetail, fileDetail, err := fsm.FetchDeploymentConfigDetail(namespace)
	if err != nil {
		return nil, nil, err
	}

	envs := make(map[string]string, 0)
	files := make(map[string]string, 0)

	for _, c := range envDetail {
		envs[c.Key] = c.Value
	}

	for _, c := range fileDetail {
		files[c.Key] = c.Value
	}

	return envs, files, nil
}

func (fsm *DeployFSMContext) FetchDeploymentConfigDetail(namespace string) ([]apistructs.EnvConfig, []apistructs.EnvConfig, error) {
	envConfigs, err := fsm.envConfig.GetDeployConfigs(namespace)
	if err != nil {
		return nil, nil, err
	}
	envs := make([]apistructs.EnvConfig, 0)
	files := make([]apistructs.EnvConfig, 0)
	for _, c := range envConfigs {
		if c.ConfigType == "FILE" {
			files = append(files, c)
		} else {
			envs = append(envs, c)
		}
	}

	return envs, files, nil
}

func (fsm *DeployFSMContext) generateDeployServiceRequest(group *apistructs.ServiceGroupCreateV2Request,
	projectAddons []dbclient.AddonInstanceRouting,
	projectAddonTenants []dbclient.AddonInstanceTenant,
	projectECI bool) (
	map[string]dbclient.AddonInstanceRouting, map[string]dbclient.AddonInstanceTenant, error) {
	// prepare context
	deployment := fsm.Deployment
	runtime := fsm.Runtime
	app := fsm.App
	obj := fsm.Spec

	if len(runtime.ClusterName) == 0 {
		return nil, nil, errors.Errorf("failed to deployService, clusterName empty")
	}

	// prepare envs
	logrus.Info("start load addon configs")
	addonEnvList, err := fsm.addon.GetRuntimeAddonConfig(runtime.ID)
	if err != nil {
		return nil, nil, err
	}
	logrus.Info("start load addon configs end")
	env := make(map[string]string)
	if len(*addonEnvList) > 0 {
		for _, config := range *addonEnvList {
			for k, v := range config.Config {
				switch t := v.(type) {
				case string:
					env[k] = t
				default:
					env[k] = fmt.Sprintf("%v", t)
				}
			}
		}
	}

	groupEnv := make(map[string]string)
	groupFileconfigs := make(map[string]string)
	// globalEnv priority lower than config-center
	for k, v := range obj.Envs {
		groupEnv[k] = v
	}
	var configNamespace string
	for _, w := range fsm.App.Workspaces {
		if w.Workspace == runtime.Workspace {
			configNamespace = w.ConfigNamespace
			break
		}
	}
	if len(configNamespace) > 0 {
		if fsm.Deployment.Param != "" {
			var configs apistructs.DeploymentOrderParam

			if err := json.Unmarshal([]byte(fsm.Deployment.Param), &configs); err != nil {
				return nil, nil, fmt.Errorf("failed to unmarshal deployment params: %v", err)
			}

			// configs come from config-center do override globalEnv
			for _, config := range configs {
				switch config.Type {
				case "ENV":
					groupEnv[config.Key] = config.Value
				case "FILE":
					groupFileconfigs[config.Key] = config.Value
				}
			}
		} else {
			// TODO: deprecated
			// get configs from config-center
			envconfigs, fileconfigs, err := fsm.FetchDeploymentConfig(configNamespace)
			if err != nil {
				return nil, nil, err
			}

			// configs come from config-center do override globalEnv
			for k, v := range envconfigs {
				groupEnv[k] = v
			}

			for k, v := range fileconfigs {
				groupFileconfigs[k] = v
			}
		}
	}

	// generate value into serviceGroup
	group.Type, group.ID = runtime.ScheduleName.Args()
	group.ClusterName = runtime.ClusterName

	// generate project namespace into serviceGroup
	group.ProjectNamespace = fsm.GetProjectNamespace(runtime.Workspace)

	groupLabels := make(map[string]string)
	utils.AppendEnv(groupLabels, obj.Meta)
	utils.AppendEnv(groupLabels, convertGroupLabels(app, runtime, deployment.ID))
	obj.Meta = groupLabels
	usedAddonInsMap := map[string]dbclient.AddonInstanceRouting{}
	usedAddonTenantMap := map[string]dbclient.AddonInstanceTenant{}

	extraParams := make(map[string]string)
	if fsm.Runtime != nil && fsm.Runtime.ExtraParams != "" {
		err := json.Unmarshal([]byte(fsm.Runtime.ExtraParams), &extraParams)
		if err != nil {
			return nil, nil, errors.Errorf("failed to unmarshal extraParams, %v", err)
		}
	}

	for name, serv := range obj.Services {
		usedAddonInsMap_, usedAddonTenantMap_, err := fsm.convertService(name, serv, extraParams, obj.Meta, env, groupEnv, groupFileconfigs,
			runtime, projectAddons, projectAddonTenants, projectECI)
		if err != nil {
			return nil, nil, err
		}
		for k, v := range usedAddonInsMap_ {
			usedAddonInsMap[k] = v
		}
		for k, v := range usedAddonTenantMap_ {
			usedAddonTenantMap[k] = v
		}

	}

	for name, job := range obj.Jobs {
		usedAddonInsMap_, usedAddonTenantMap_, err := fsm.convertJob(name, job, extraParams, obj.Meta, env, groupEnv, groupFileconfigs,
			runtime, projectAddons, projectAddonTenants, projectECI)
		if err != nil {
			return nil, nil, err
		}
		for k, v := range usedAddonInsMap_ {
			usedAddonInsMap[k] = v
		}
		for k, v := range usedAddonTenantMap_ {
			usedAddonTenantMap[k] = v
		}
	}
	//handle env template
	err = fsm.convertEnvForTemplate(obj, group.ProjectNamespace, runtime.OrgID, runtime.ProjectID, runtime.Workspace)
	if err != nil {
		return nil, nil, err
	}
	group.DiceYml = *obj
	return usedAddonInsMap, usedAddonTenantMap, nil
}

func Render(template string) string {
	subMatchs := regexp.MustCompile(`^{{\s*(.+)\s*}}`).FindStringSubmatch(template)
	if len(subMatchs) > 0 {
		return subMatchs[1]
	}
	return ""
}

func (fsm *DeployFSMContext) convertEnvForTemplate(obj *diceyml.Object, projectNs string, orgID uint64, projectID uint64, workspace string) error {
	var err error
	for k, v := range obj.Envs {
		tv := template.GetTemplateValue(v)
		if tv == "" || !strings.HasPrefix(tv, "erdaService") {
			continue
		}
		obj.Envs[k], err = fsm.convertErdaServiceTemplate(v, projectNs, orgID, projectID, workspace)
		if err != nil {
			return err
		}
	}

	for k, v := range obj.Services {
		for k1, v1 := range v.Envs {
			tv := template.GetTemplateValue(v1)
			if tv == "" || !strings.HasPrefix(tv, "erdaService") {
				continue
			}
			obj.Services[k].Envs[k1], err = fsm.convertErdaServiceTemplate(v1, projectNs, orgID, projectID, workspace)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (fsm *DeployFSMContext) convertErdaServiceTemplate(v string, projectNs string, orgID uint64, projectID uint64, workspace string) (string, error) {
	svcSuffix := "svc.cluster.local"
	var (
		result          string
		applicationResp *apistructs.ApplicationListResponseData
		err             error
		runtimes        []dbclient.Runtime
	)
	nodes := strings.Split(v, ".")
	if len(nodes) != 3 {
		return "", errors.New("erdaService env template must be erdaService.<appName>.<serviceName>")
	}
	applicationName := nodes[1]
	serviceName := nodes[2]

	//edas集群的svc都是在defalut空间下
	if fsm.Cluster.Type == apistructs.EDAS {
		result = fmt.Sprintf("%s.%s.%s", serviceName, "default", svcSuffix)
		return result, nil
	}

	if applicationResp, err = fsm.bdl.GetAppsByProjectAndAppName(projectID, orgID, fsm.Deployment.Operator, applicationName, map[string][]string{httputil.InternalHeader: {"orchestrator"}}); err != nil {
		return "", err
	}
	if applicationResp.Total != 1 {
		errMsg := fmt.Sprintf("convert erdaService Tempalte env error: %s application not found", applicationName)
		return "", errors.New(errMsg)
	}
	//根据应用ID以及环境获取应用对应的runtime
	applicationID := applicationResp.List[0].ID
	if runtimes, err = fsm.db.FindRuntimesByAppIdAndWorkspace(applicationID, workspace); err != nil {
		return "", err
	}
	//没有runtime统一按照项目级namespace来处理
	if len(runtimes) < 1 {
		result = fmt.Sprintf("%s.%s.%s", serviceName, projectNs, svcSuffix)
		return result, nil
	}

	//scheduler的Name长度为10或者为空，则认为是项目级namespace
	if len(runtimes[0].ScheduleName.Name) == 10 || runtimes[0].ScheduleName.Name == "" {
		result = fmt.Sprintf("%s.%s.%s", serviceName, projectNs, svcSuffix)
		return result, nil
	}
	//其余当做非项目级namespace来对待
	ns := fmt.Sprintf("%s--%s", runtimes[0].ScheduleName.Namespace, runtimes[0].ScheduleName.Name)
	result = fmt.Sprintf("%s.%s.%s", serviceName, ns, svcSuffix)
	return result, nil
}

func (fsm *DeployFSMContext) checkCancelOk() (bool, error) {
	if fsm.Deployment.Extra.CancelStartAt != nil {
		startCheckPoint := fsm.Deployment.Extra.CancelStartAt.Add(30 * time.Second)
		if time.Now().Before(startCheckPoint) {
			fsm.pushLog(fmt.Sprintf("checking too early, delay to: %s", startCheckPoint.String()))
			// too early to check
			return false, nil
		}
	}
	if fsm.Runtime.Status == apistructs.RuntimeStatusHealthy {
		return true, nil
	}
	return false, nil
}

// true means service is ready (Healthy), and if error occur, we fail the deployment
func (fsm *DeployFSMContext) checkServiceReady() (bool, error) {
	runtime := fsm.Runtime
	// do not check if nil for compatibility
	if fsm.Deployment.Extra.ServicePhaseStartAt != nil {
		startCheckPoint := fsm.Deployment.Extra.ServicePhaseStartAt.Add(30 * time.Second)
		if time.Now().Before(startCheckPoint) {
			fsm.pushLog(fmt.Sprintf("checking too early, delay to: %s", startCheckPoint.String()))
			// too early to check
			return false, nil
		}
	}
	isReplicasZero := false
	for _, s := range fsm.Spec.Services {
		if s.Deployments.Replicas == 0 {
			isReplicasZero = true
			break
		}
	}
	if isReplicasZero {
		fsm.pushLog("checking status by inspect")
		// we do double check to prevent `fake Healthy`
		// runtime.ScheduleName must have
		sg, err := fsm.getServiceGroup()
		if err != nil {
			return false, err
		}
		return sg.Status == "Ready" || sg.Status == "Healthy", nil
	}

	// 获取addon状态
	serviceGroup, err := fsm.getServiceGroup()
	if err != nil {
		fsm.pushLog(fmt.Sprintf("获取service状态失败，%s", err.Error()))
		return false, nil
	}
	fsm.pushLog(fmt.Sprintf("checking status: %s, servicegroup: %v", serviceGroup.Status, runtime.ScheduleName))
	// 如果状态是failed，说明服务或者job运行失败
	if serviceGroup.Status == apistructs.StatusFailed {
		return false, errors.New(serviceGroup.LastMessage)
	}
	// 如果状态是ready或者healthy，说明服务已经发起来了
	runtimeStatus := apistructs.RuntimeStatusUnHealthy
	if serviceGroup.Status == apistructs.StatusReady || serviceGroup.Status == apistructs.StatusHealthy {
		runtimeStatus = apistructs.RuntimeStatusHealthy
	}
	runtimeItem := fsm.Runtime
	if runtimeItem.Status != runtimeStatus {
		runtimeItem.Status = runtimeStatus
		if err := fsm.db.UpdateRuntime(runtime); err != nil {
			logrus.Errorf("failed to update runtime status changed, runtime: %v, err: %v", runtime.ID, err.Error())
			return false, nil
		}
	}
	if runtimeStatus == apistructs.RuntimeStatusHealthy {
		return true, nil
	}
	return false, nil
}

// error means reach the threshold, we need fail the deployment
// we also force set runtime.Status to Progressing
func (fsm *DeployFSMContext) increaseFakeHealthyCount() error {
	deployment := fsm.Deployment
	runtime := fsm.Runtime
	if deployment == nil || runtime == nil {
		return nil
	}
	deployment.Extra.FakeHealthyCount += 1
	if err := fsm.db.UpdateDeployment(deployment); err != nil {
		return errors.Wrapf(err, "failed to increase FakeHealthyCount, deploymentId: %d", deployment.ID)
	}
	if deployment.Extra.FakeHealthyCount <= 3 {
		// ignore first 3 times
		// TODO: the first times is too quick
		return nil
	}
	liarCount := deployment.Extra.FakeHealthyCount - 3
	fsm.pushLog(fmt.Sprintf("健康误报 %d 次", liarCount))
	// TODO: checking service need a standalone goroutine and standalone cron (about 10s one time)
	if runtime.Status == apistructs.RuntimeStatusHealthy && liarCount >= 2 {
		// TODO: do we really need reset the status?
		// this is really fake (but we only force set status when the second time fake come)
		runtime.Status = apistructs.RuntimeStatusUnHealthy
		if err := fsm.db.UpdateRuntime(runtime); err != nil {
			return errors.Wrapf(err, "failed to increase FakeHealthyCount, deploymentId: %d", deployment.ID)
		}
		fsm.pushLog("强制设置状态, 修正误报")
	}
	if liarCount >= 6 {
		logContent := fmt.Sprintf("遭遇过多健康误报 (超过 %d 次), 采取不信任策略并关闭部署单, deploymentId: %d",
			liarCount, deployment.ID)
		fsm.pushLog(logContent)
		return errors.New(logContent)
	}
	return nil
}

func (fsm *DeployFSMContext) buildAddonVars(addonnameMap map[string][]dbclient.AddonInstanceRouting,
	addonIDMap map[string]dbclient.AddonInstanceRouting,
	addonTenantNameMap map[string][]dbclient.AddonInstanceTenant,
	addonTenantIDMap map[string]dbclient.AddonInstanceTenant) (map[string]string, error) {
	r := map[string]string{}
	for k, addons := range addonnameMap {
		if len(addons) != 1 {
			// 忽略有多个同名的addon, 这种情况只有通过id来引用
			continue
		}
		addonins, err := fsm.db.GetAddonInstance(addons[0].RealInstance)
		if err != nil {
			logrus.Errorf("failed to GetAddonInstance: %s, %v", addons[0].RealInstance, err)
			continue
		}
		addonConfig, err := fsm.addon.GetAddonConfig(addonins)
		if err != nil {
			return nil, err
		}
		if addonConfig != nil {
			config := addonConfig.Config
			for configk, v := range config {
				r[fmt.Sprintf("addons.%s.%s", k, configk)] = fmt.Sprintf("%v", v)
			}
		}
	}

	for id, addon := range addonIDMap {
		addonins, err := fsm.db.GetAddonInstance(addon.RealInstance)
		if err != nil {
			logrus.Errorf("failed to GetAddonInstance: %s, %v", addon.RealInstance, err)
			continue
		}
		addonConfig, err := fsm.addon.GetAddonConfig(addonins)
		if err != nil {
			return nil, err
		}
		if addonConfig != nil {
			config := addonConfig.Config
			for configk, v := range config {
				r[fmt.Sprintf("addons.%s.%s", id, configk)] = fmt.Sprintf("%v", v)
			}
		}
	}

	for k, tenants := range addonTenantNameMap {
		if len(tenants) != 1 {
			continue
		}
		tenant := tenants[0]
		config, err := fsm.addon.GetAddonTenantConfig(&tenant)
		if err != nil {
			return nil, err
		}
		for configk, v := range config {
			r[fmt.Sprintf("addons.%s.%s", k, configk)] = fmt.Sprintf("%v", v)
		}
	}

	for id, tenant := range addonTenantIDMap {
		config, err := fsm.addon.GetAddonTenantConfig(&tenant)
		if err != nil {
			return nil, err
		}
		for configk, v := range config {
			r[fmt.Sprintf("addons.%s.%s", id, configk)] = fmt.Sprintf("%v", v)
		}
	}

	return r, nil
}

func isTemplate(env string) (string, bool) {
	env_trim := strutil.Trim(env)
	is := strutil.HasPrefixes(env_trim, "${{") && strutil.HasSuffixes(env_trim, "}}")
	if is {
		return strutil.TrimSuffixes(strutil.TrimPrefixes(env_trim, "${{"), "}}"), is
	}
	return "", is
}

func addTemplateContextVars(from sexp.Context, vars map[string]string) sexp.Context {
	result_vars := map[string]sexp.Sexp{}
	for k, v := range from.Vars {
		result_vars[k] = v
	}
	for k, v := range vars {
		result_vars[k] = sexp.Sexp{I: sexp.QString(v)}
	}
	return sexp.Context{Funcs: from.Funcs, Vars: result_vars}
}

func (fsm *DeployFSMContext) logAddonVars(addonvars map[string]string) {
	s := []string{}
	for k := range addonvars {
		s = append(s, k)
	}
	ss := strutil.Join(s, ", ", true)
	fsm.pushLog("Available addon vars: " + ss)
}

func (fsm *DeployFSMContext) evalTemplate(projectAddons []dbclient.AddonInstanceRouting,
	projectAddonTenants []dbclient.AddonInstanceTenant, envs map[string]string) (map[string]string, map[string]dbclient.AddonInstanceRouting, map[string]dbclient.AddonInstanceTenant, error) {
	addonnameMap, addonIDMap, addonTenantNameMap, addonTenantIDMap := fsm.addon.BuildAddonAndTenantMap(
		projectAddons,
		projectAddonTenants)
	usedAddonInsMap := map[string]dbclient.AddonInstanceRouting{}
	usedAddonTenantMap := map[string]dbclient.AddonInstanceTenant{}

	// build sexp context
	addonvars, err := fsm.buildAddonVars(addonnameMap, addonIDMap, addonTenantNameMap, addonTenantIDMap)
	if err != nil {
		return nil, nil, nil, err
	}
	fsm.logAddonVars(addonvars)
	ctx := addTemplateContextVars(sexp.Builtin, addonvars)

	result_envs := map[string]string{}
	for k, v := range envs {
		exp_s, ok := isTemplate(v)
		if !ok {
			result_envs[k] = v
			continue
		}
		exp, err := sexp.Parse(exp_s)
		if err != nil {
			return nil, nil, nil, err
		}
		expresult, err := sexp.Eval(&ctx, exp)
		if err != nil {
			return nil, nil, nil, err
		}
		switch s := expresult.I.(type) {
		case sexp.QString:
			result_envs[k] = string(s)
		case string:
			result_envs[k] = s
		case int:
			result_envs[k] = strconv.Itoa(s)
		case float64:
			result_envs[k] = fmt.Sprintf("%.2f", s)
		}

		vars := sexp.ReferencedVars(exp)
		for _, v := range vars {
			vs := strutil.Split(v, ".", true)
			if len(vs) != 3 {
				continue
			}
			if vs[0] != "addons" {
				continue
			}
			nameorid := vs[1]
			if routings, ok := addonnameMap[nameorid]; ok {
				if len(routings) == 1 {
					usedAddonInsMap[routings[0].ID] = routings[0]
				}
			} else if routing, ok := addonIDMap[nameorid]; ok {
				usedAddonInsMap[routing.ID] = routing
			} else if tenants, ok := addonTenantNameMap[nameorid]; ok {
				if len(tenants) == 1 {
					usedAddonTenantMap[tenants[0].ID] = tenants[0]
				}
			} else if tenant, ok := addonTenantIDMap[nameorid]; ok {
				usedAddonTenantMap[tenant.ID] = tenant
			}
		}

	}
	return result_envs, usedAddonInsMap, usedAddonTenantMap, nil
}

func BuildVolumeRootDir(runtime *dbclient.Runtime) string {
	return fmt.Sprintf("/netdata/volumes/%s/%s", runtime.GitRepoAbbrev, strings.ToLower(runtime.Workspace))
}

func (fsm *DeployFSMContext) convertService(serviceName string, service *diceyml.Service,
	extraEnv, groupLabels map[string]string, addonEnv map[string]string, groupEnv, groupFileconfigs map[string]string,
	runtime *dbclient.Runtime, projectAddons []dbclient.AddonInstanceRouting,
	projectAddonTenants []dbclient.AddonInstanceTenant, projectECI bool) (map[string]dbclient.AddonInstanceRouting, map[string]dbclient.AddonInstanceTenant, error) {

	newVolumes := make([]diceyml.Volume, 0)
	// 用于兼容使用旧的 volume 定义方式的 volume，避免创建新 volume
	oldTypeVolumes := make([]diceyml.Volume, 0)

	if projectECI {
		// 全部使用新的 volumes 定义
		for _, vol := range service.Volumes {
			newVolumes = append(newVolumes, utils.ConvertVolume(vol))
		}
	} else {
		for _, vol := range service.Volumes {
			if vol.Path != "" {
				oldTypeVolumes = append(oldTypeVolumes, vol)
			} else {
				newVolumes = append(newVolumes, vol)
			}
		}
	}

	volumePrefixDir := BuildVolumeRootDir(runtime)
	bs, err := convertBinds(serviceName, volumePrefixDir, oldTypeVolumes)
	if err != nil {
		return nil, nil, err
	}
	service.Binds = append(service.Binds, bs...)
	if len(newVolumes) > 0 {
		service.Volumes = newVolumes
	} else {
		service.Volumes = nil
	}
	service.Labels = utils.ConvertServiceLabels(groupLabels, service.Labels, serviceName)
	// TODO:
	// currently platformEnv > serviceEnv > addonEnv > groupEnv
	// we desire platformEnv > addonEnv > serviceEnv > groupEnv
	envs := make(map[string]string)
	utils.AppendEnv(envs, groupEnv)
	utils.AppendEnv(envs, addonEnv)
	utils.AppendEnv(envs, service.Envs)
	// at last, append platformEnv
	envs["TERMINUS_APP"] = serviceName
	// clear existing DICE_*
	if _, ok := groupLabels["ERDA_COMPONENT"]; !ok {
		for k := range envs {
			if strings.HasPrefix(k, "DICE_") {
				delete(envs, k)
			}
		}
	}
	for k, v := range service.Labels {
		if strings.HasPrefix(k, "DICE_") {
			envs[k] = v
		}
	}
	for k, v := range extraEnv {
		envs[k] = v
	}
	replaced_envs, usedAddonInsMap, usedAddonTenantMap, err := fsm.evalTemplate(projectAddons, projectAddonTenants, envs)
	if err != nil {
		return nil, nil, err
	}
	service.Envs = replaced_envs

	if len(service.Expose) > 0 {
		service.Labels["IS_ENDPOINT"] = "true"
		// TODO: we should get domain by api: GetClusterByName
		rootDomains := strings.Split(fsm.Cluster.WildcardDomain, ",")
		if len(rootDomains) < 1 {
			return nil, nil, errors.Errorf("domain not exist, cluster %s", fsm.Cluster.Name)
		}
		rootDomain := rootDomains[0]
		domains, err := fsm.db.FindDomainsByRuntimeIdAndServiceName(runtime.ID, serviceName)
		if err != nil {
			return nil, nil, err
		}
		var vHosts []string
		for _, d := range domains {
			vHosts = append(vHosts, d.Domain)
			if len(rootDomains) > 1 && strings.HasSuffix(d.Domain, rootDomain) {
				vHosts = append(vHosts, strings.TrimSuffix(d.Domain, rootDomain)+rootDomains[1])
			}
		}
		vHost := strings.Join(vHosts, ",")
		service.Labels["HAPROXY_GROUP"] = "external"
		service.Labels["HAPROXY_0_VHOST"] = vHost
	}

	if len(service.Deployments.Labels) == 0 {
		service.Deployments.Labels = map[string]string{}
	}

	nexususer, err := fsm.bdl.GetNexusOrgDockerCredentialByImage(fsm.App.OrgID, service.Image)
	if err != nil {
		return nil, nil, err
	}
	if nexususer != nil {
		service.ImagePassword = nexususer.Password
		service.ImageUsername = nexususer.Name
	}
	if len(groupFileconfigs) > 0 {
		if err := fsm.generateRuntimeFileToken(); err != nil {
			return nil, nil, err
		}
		// TODO: diceyml add dependon: openapi
		openapiPublicAddr := os.Getenv("OPENAPI_PUBLIC_ADDR")
		if service.Init == nil {
			service.Init = map[string]diceyml.InitContainer{}
		}
		service.Init["internal-init-data"] = diceyml.InitContainer{
			Image:      conf.InitContainerImage(),
			SharedDirs: []diceyml.SharedDir{{Main: "/init-data", SideCar: "/data"}},
			Cmd:        buildCurlDownloadFileCmd(groupFileconfigs, openapiPublicAddr, fsm.Runtime.FileToken, "/data"),
		}
	}
	return usedAddonInsMap, usedAddonTenantMap, nil
}
func (fsm *DeployFSMContext) generateRuntimeFileToken() error {
	if fsm.Runtime.FileToken != "" {
		return nil
	}
	if tokeninfo, err := fsm.bdl.GetOAuth2Token(apistructs.OAuth2TokenGetRequest{
		ClientID:     conf.TokenClientID(),
		ClientSecret: conf.TokenClientSecret(),
		Payload: apistructs.OAuth2TokenPayload{
			AccessTokenExpiredIn: "0",
			AccessibleAPIs: []apistructs.AccessibleAPI{{
				Path:   "/api/files",
				Method: http.MethodGet,
				Schema: "http",
			}},
			Metadata: map[string]string{
				httputil.InternalHeader: "orchestrator",
				"RuntimeID":             strconv.FormatUint(fsm.Runtime.ID, 10),
			},
		}}); err != nil {
		return err
	} else {
		fsm.Runtime.FileToken = tokeninfo.AccessToken
	}
	if err := fsm.db.UpdateRuntime(fsm.Runtime); err != nil {
		return err
	}
	return nil
}

func (fsm *DeployFSMContext) convertJob(jobName string, job *diceyml.Job,
	extraEnv, groupLabels map[string]string, addonEnv map[string]string, groupEnv, groupFileconfigs map[string]string,
	runtime *dbclient.Runtime, projectAddons []dbclient.AddonInstanceRouting,
	projectAddonTenants []dbclient.AddonInstanceTenant, projectECI bool) (map[string]dbclient.AddonInstanceRouting, map[string]dbclient.AddonInstanceTenant, error) {

	newVolumes := make([]diceyml.Volume, 0)
	// 用于兼容使用旧的 volume 定义方式的 volume，避免创建新 volume
	oldTypeVolumes := make([]diceyml.Volume, 0)

	if projectECI {
		// 全部使用新的 volumes 定义
		for _, vol := range job.Volumes {
			newVolumes = append(newVolumes, utils.ConvertVolume(vol))
		}
	} else {
		for _, vol := range job.Volumes {
			if vol.Path != "" {
				oldTypeVolumes = append(oldTypeVolumes, vol)
			} else {
				newVolumes = append(newVolumes, vol)
			}
		}
	}

	volumePrefixDir := BuildVolumeRootDir(runtime)
	bs, err := convertBinds(jobName, volumePrefixDir, oldTypeVolumes)
	if err != nil {
		return nil, nil, err
	}
	job.Binds = append(job.Binds, bs...)
	if len(newVolumes) > 0 {
		job.Volumes = newVolumes
	} else {
		job.Volumes = nil
	}
	job.Labels = utils.ConvertServiceLabels(groupLabels, job.Labels, jobName)
	// TODO:
	// currently platformEnv > serviceEnv > addonEnv > groupEnv
	// we desire platformEnv > addonEnv > serviceEnv > groupEnv
	envs := make(map[string]string)
	utils.AppendEnv(envs, groupEnv)
	utils.AppendEnv(envs, addonEnv)
	utils.AppendEnv(envs, job.Envs)
	// at last, append platformEnv
	envs["TERMINUS_APP"] = jobName
	// clear existing DICE_*
	if _, ok := groupLabels["ERDA_COMPONENT"]; !ok {
		for k := range envs {
			if strings.HasPrefix(k, "DICE_") {
				delete(envs, k)
			}
		}
	}
	for k, v := range job.Labels {
		if strings.HasPrefix(k, "DICE_") {
			envs[k] = v
		}
	}
	for k, v := range extraEnv {
		envs[k] = v
	}
	replaced_envs, usedAddonInsMap, usedAddonTenantMap, err := fsm.evalTemplate(projectAddons, projectAddonTenants, envs)
	if err != nil {
		return nil, nil, err
	}
	job.Envs = replaced_envs

	if len(groupFileconfigs) > 0 {
		if fsm.Runtime.FileToken == "" {
			if tokeninfo, err := fsm.bdl.GetOAuth2Token(apistructs.OAuth2TokenGetRequest{
				ClientID:     conf.TokenClientID(),
				ClientSecret: conf.TokenClientSecret(),
				Payload: apistructs.OAuth2TokenPayload{
					AccessTokenExpiredIn: "0",
					AccessibleAPIs: []apistructs.AccessibleAPI{{
						Path:   "/api/files",
						Method: http.MethodGet,
						Schema: "http",
					}},
					Metadata: map[string]string{
						httputil.InternalHeader: "orchestrator",
						"RuntimeID":             strconv.FormatUint(fsm.Runtime.ID, 10),
					},
				}}); err != nil {
				return nil, nil, err
			} else {
				fsm.Runtime.FileToken = tokeninfo.AccessToken
			}
			if err := fsm.db.UpdateRuntime(fsm.Runtime); err != nil {
				return nil, nil, err
			}
		}
		// TODO: diceyml add dependon: openapi
		openapiPublicAddr := os.Getenv("OPENAPI_PUBLIC_ADDR")
		if job.Init == nil {
			job.Init = map[string]diceyml.InitContainer{}
		}
		job.Init["internal-init-data"] = diceyml.InitContainer{
			Image:      conf.InitContainerImage(),
			SharedDirs: []diceyml.SharedDir{{Main: "/init-data", SideCar: "/data"}},
			Cmd:        buildCurlDownloadFileCmd(groupFileconfigs, openapiPublicAddr, fsm.Runtime.FileToken, "/data"),
		}
	}
	return usedAddonInsMap, usedAddonTenantMap, nil
}

func buildCurlDownloadFileCmd(files map[string]string, openapiAddr string, token string, dstdir string) string {
	cmds := []string{}
	for filename, v := range files {
		cmds = append(cmds, fmt.Sprintf("curl -L '%s/api/files?file=%s' -H 'Authorization: Bearer %s' > %s",
			openapiAddr, v, token, filepath.Join(dstdir, filename)))
	}
	return strutil.Join(cmds, "&&")
}

func (fsm *DeployFSMContext) doCancelDeploy(operator string, force bool) error {
	switch fsm.Deployment.Status {
	case apistructs.DeploymentStatusWaitApprove:
		fsm.Deployment.Status = apistructs.DeploymentStatusCanceled
		if err := fsm.db.UpdateDeployment(fsm.Deployment); err != nil {
			// db update fail mess up everything!
			return errors.Wrapf(err, "failed to doCancel deploy, operator: %v", operator)
		}
		if err := fsm.UpdateDeploymentStatusToRuntimeAndOrder(); err != nil {
			logrus.Errorf("failed to update deployment status for runtime: %v", err)
			return err
		}
	case apistructs.DeploymentStatusInit, apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
		// normal cancel
		fsm.Deployment.Extra.ForceCanceled = true
		fsm.pushOnCanceled()
	case apistructs.DeploymentStatusCanceling:
		// status in Canceling, only force=true can work
		if !force {
			return errors.Errorf("正在取消中，禁止取消！若已确认风险，请强制取消")
		}
		fsm.Deployment.Extra.ForceCanceled = true
		fsm.pushOnCanceled()
	}
	return nil
}

func (fsm *DeployFSMContext) getServiceGroup() (*apistructs.ServiceGroup, error) {
	return fsm.serviceGroupImpl.InspectServiceGroupWithTimeout(fsm.Runtime.ScheduleName.Namespace, fsm.Runtime.ScheduleName.Name)
}

// TODO: we should redundant app info into runtime, so then we can move this func to utils
func convertGroupLabels(app *apistructs.ApplicationDTO, runtime *dbclient.Runtime,
	deploymentId uint64) map[string]string {
	var configNamespace string
	for _, w := range app.Workspaces {
		if w.Workspace != runtime.Workspace {
			continue
		}
		configNamespace = w.ConfigNamespace
	}
	// we do prefer DICE_X_ID more than DICE_X, but still keep DICE_X for compatibility
	return map[string]string{
		"SERVICE_TYPE":              "STATELESS",
		"SERVICE_DISCOVERY_MODE":    "DEPEND", // default values are used for service discovery
		"DICE_ORG":                  strconv.FormatUint(app.OrgID, 10),
		"DICE_ORG_ID":               strconv.FormatUint(app.OrgID, 10),
		"DICE_ORG_NAME":             app.OrgName,
		"DICE_PROJECT":              strconv.FormatUint(app.ProjectID, 10),
		"DICE_PROJECT_ID":           strconv.FormatUint(app.ProjectID, 10),
		"DICE_PROJECT_NAME":         app.ProjectName,
		"DICE_APPLICATION":          strconv.FormatUint(app.ID, 10),
		"DICE_APPLICATION_ID":       strconv.FormatUint(app.ID, 10),
		"DICE_APPLICATION_NAME":     app.Name,
		"DICE_WORKSPACE":            strings.ToLower(runtime.Workspace),
		"DICE_CLUSTER_NAME":         runtime.ClusterName,
		"DICE_RUNTIME":              strconv.FormatUint(runtime.ID, 10),
		"DICE_RUNTIME_ID":           strconv.FormatUint(runtime.ID, 10),
		"DICE_RUNTIME_NAME":         runtime.Name,
		"DICE_DEPLOYMENT":           strconv.FormatUint(deploymentId, 10),
		"DICE_DEPLOYMENT_ID":        strconv.FormatUint(deploymentId, 10),
		"DICE_APP_CONFIG_NAMESPACE": configNamespace,
	}
}

func convertBinds(serviceName, volumePrefixDir string, volumes diceyml.Volumes) (diceyml.Binds, error) {
	var vols []string
	volumeMap := map[string]string{}
	for _, v := range volumes {
		vols = append(vols, strutil.Concat(v.Path, ":", v.Path))
		volumeMap[v.Path] = v.Storage
	}
	binds, err := diceyml.ParseBinds(vols)
	if err != nil {
		return nil, err
	}
	var bs diceyml.Binds
	for _, b := range binds {
		// local volume直接采用localpv的方式
		if typeOfst, ok := volumeMap[b.ContainerPath]; ok {
			if typeOfst == "local" {
				h := md5.New() // #nosec G401
				h.Write([]byte(b.ContainerPath))
				bs = append(bs, strutil.Concat(serviceName, "-", hex.EncodeToString(h.Sum(nil)),
					":", b.ContainerPath, ":", b.Type))
				continue
			}
		}

		hostPath := b.HostPath
		if !strings.HasPrefix(hostPath, "/") {
			hostPath = "/" + hostPath
		}
		hostPath = volumePrefixDir + hostPath
		one := strutil.Concat(hostPath, ":", b.ContainerPath, ":", b.Type)
		bs = append(bs, one)
	}
	return bs, nil
}

// putHepaService 发送给hepa的数据信息
func (fsm *DeployFSMContext) PutHepaService() error {
	// 部署runtime之后，orchestrator需要将服务域名信息通过此接口提交给hepa
	logrus.Info("start request hepa service.")
	var (
		sg  *apistructs.ServiceGroup
		err error
	)
	if fsm.Runtime.ScheduleName.Name != "" {
		sg, err = fsm.serviceGroupImpl.InspectServiceGroupWithTimeout(fsm.Runtime.ScheduleName.Namespace, fsm.Runtime.ScheduleName.Name)
		if err != nil {
			return err
		}
	}
	if sg != nil {
		logrus.Info("start request hepa service, sg is not null.")
		// 查询该runtime各个模块的domain信息
		domains, err := fsm.db.FindDomainsByRuntimeId(fsm.Runtime.ID)
		if err != nil {
			return err
		}
		endPointMap := make(map[string][]apistructs.EndpointDomainsItem)
		for _, v := range domains {
			endPointMap[v.EndpointName] = append(endPointMap[v.EndpointName], apistructs.EndpointDomainsItem{
				Domain: v.Domain,
				Type:   v.DomainType,
			})
		}
		// 所有service列表获取，组装
		serviceArray := make([]apistructs.ServiceItem, 0, len(sg.Services))
		for _, itemGroup := range sg.Services {
			if len(itemGroup.Ports) == 0 {
				continue
			}
			exposePort := itemGroup.Ports[0].Port
			for _, port := range itemGroup.Ports {
				if port.Expose {
					exposePort = port.Port
					break
				}
			}
			item := apistructs.ServiceItem{
				ServiceName:  itemGroup.Name,
				InnerAddress: itemGroup.Vip + ":" + strconv.Itoa(exposePort),
			}
			serviceArray = append(serviceArray, item)
		}
		runtimeServiceReq := apistructs.RuntimeServiceRequest{
			ProjectID:             strconv.FormatUint(fsm.App.ProjectID, 10),
			OrgID:                 strconv.FormatUint(fsm.App.OrgID, 10),
			Workspace:             fsm.Runtime.Workspace,
			ClusterName:           fsm.Runtime.ClusterName,
			ReleaseID:             fsm.Deployment.ReleaseId,
			ServiceGroupName:      fsm.Runtime.ScheduleName.Name,
			ServiceGroupNamespace: fsm.Runtime.ScheduleName.Namespace,
			RuntimeID:             strconv.FormatUint(fsm.Runtime.ID, 10),
			RuntimeName:           fsm.Runtime.Name,
			AppID:                 strconv.FormatUint(fsm.App.ID, 10),
			AppName:               fsm.App.Name,
			Services:              serviceArray,
			UseApigw:              false,
			ProjectNamespace:      fsm.GetProjectNamespace(fsm.Runtime.Workspace),
		}
		// 查询出是否addon中依赖了api gateway
		useApigw := false
		prebuilds, err := fsm.db.GetPreBuildsByRuntimeID(fsm.Runtime.ID)
		if err == nil {
			if prebuilds != nil && len(*prebuilds) > 0 {
				for _, v := range *prebuilds {
					if v.DeleteStatus != apistructs.AddonPrebuildNotDeleted {
						continue
					}
					if v.AddonName == apistructs.AddonApiGateway || v.AddonName == apistructs.AddonMicroService {
						useApigw = true
					}
				}
			}
		}
		runtimeServiceReq.UseApigw = useApigw
		if err := fsm.bdl.PutRuntimeService(&runtimeServiceReq); err != nil {
			return err
		}
	}
	return nil
}

// prepareCheckProjectResource 计算项目预留资源，是否满足发布徐局
func (fsm *DeployFSMContext) PrepareCheckProjectResource(app *apistructs.ApplicationDTO, projectID uint64, legacyDice *diceyml.Object, runtime *dbclient.Runtime) (float64, float64, error) {
	/* todo: 修改逻辑
	查找目标集群带该环境标签和stateless-service标签的集群的 allocatable 资源和 request 资源，分别记为 Allocatable 和 ActualRequest；
	计算该项目当前环境已有的 services 和 addons, 他们的 dice.yml 声明的资源，记为 AlreadyRequest；
	计算要部署的服务的 dice.yml 声明的资源，记为 CurrentRequest；
	查找该项目当前环境的 quota，记为 Q；
	如果 Q - AlreadyRequest < CurrentRequest, 则配额不够，不可部署。
	如果 Allocatable - ActualRequest < CurrentRequest, 则提示实际资源不够，但仍允许部署（不管它是否部得起来）
	注意：声明的资源都是超卖后的。
	*/

	// 获取项目资源信息
	projectInfo, err := fsm.bdl.GetProject(projectID)
	if err != nil {
		return 0.0, 0.0, errors.Errorf("Failed to get project info, err: %v", err)
	}
	if projectInfo == nil {
		return 0.0, 0.0, errors.Errorf("No project information found, err: %v", err)
	}
	// 获取项目所使用service信息
	serviceResource, err := fsm.resource.GetProjectServiceResource([]uint64{projectID})
	if err != nil {
		return 0.0, 0.0, errors.Errorf("Failed to get project service resources, err: %v", err)
	}
	cc, _ := json.Marshal(serviceResource)
	logrus.Infof("PrepareCheckProjectResource serviceResource: %s", string(cc))
	// 获取项目所使用addon信息
	addonResource, err := fsm.resource.GetProjectAddonResource([]uint64{projectID})
	if err != nil {
		return 0.0, 0.0, errors.Errorf("Failed to get project addon resources, err: %v", err)
	}
	bb, _ := json.Marshal(addonResource)
	logrus.Infof("PrepareCheckProjectResource addonResource: %s", string(bb))
	// 对service和addon的资源，进行累加
	usedMem := 0.0
	usedCpu := 0.0
	if len(*serviceResource) > 0 {
		// GB转MB
		usedMem += (*serviceResource)[projectID].MemServiceUsed * 1024
		usedCpu += (*serviceResource)[projectID].CpuServiceUsed
	}
	if len(*addonResource) > 0 {
		usedMem += (*addonResource)[projectID].MemAddonUsed * 1024
		usedCpu += (*addonResource)[projectID].CpuAddonUsed
	}
	// 定义发布需要用到的cpu、mem资源变量
	deployNeedMem := 0.0
	deployNeedCpu := 0.0
	if runtime.Mem == 0.0 {
		// runtime不存在，直接判断现有余下资源，是否满足发布条件
		for _, v := range legacyDice.Services {
			deployNeedMem += float64(v.Deployments.Replicas) * float64(v.Resources.Mem)
			deployNeedCpu += float64(v.Deployments.Replicas) * v.Resources.CPU
		}
	} else {
		// runtime存在，需要判断现有资源是否有变化
		localMem := 0.0
		localCpu := 0.0
		for _, v := range legacyDice.Services {
			localMem += float64(v.Deployments.Replicas) * float64(v.Resources.Mem)
			localCpu += float64(v.Deployments.Replicas) * v.Resources.CPU
		}
		// 如果需要发布的cpu、mem，大于runtime 目前的cpu、mem，那取差值来比较
		deployNeedCpu = localCpu - runtime.CPU
		deployNeedMem = localMem - float64(runtime.Mem)
	}
	//logrus.Infof("PrepareCheckProjectResource deployNeedMem: %f", deployNeedMem)
	//logrus.Infof("PrepareCheckProjectResource deployNeedCpu: %f", deployNeedCpu)
	//logrus.Infof("PrepareCheckProjectResource usedMem: %f", usedMem)
	//logrus.Infof("PrepareCheckProjectResource usedCpu: %f", usedCpu)
	//// 比较项目quota预留资源是不是够
	//if utils.Smaller(projectInfo.CpuQuota-usedCpu, deployNeedCpu) {
	//	s := fmt.Sprintf("The CPU reserved for the project is %.2f cores, %.2f cores have been occupied, %.2f CPUs are required for deploy, and the resources for application release are insufficient", projectInfo.CpuQuota, usedCpu, deployNeedCpu)
	//	fsm.ExportLogInfoDetail(apistructs.ErrorLevel, fmt.Sprintf("%d", fsm.Runtime.ID), "资源配额不足无法部署", s)
	//	return 0.0, 0.0, errors.Errorf(s)
	//}
	//useMem2, err := strconv.ParseFloat(fmt.Sprintf("%.2f", usedMem), 64)
	//if err != nil {
	//	return 0.0, 0.0, err
	//}
	//if utils.Smaller(projectInfo.MemQuota*1024.0-float64(usedMem), deployNeedMem) {
	//	s := fmt.Sprintf("The memory reserved for the project is %.2f G, %.2f G have been occupied, %.2f G are required for deploy, and the resources for application release are insufficient", projectInfo.MemQuota, useMem2/1024, deployNeedMem/1024.0)
	//	fsm.ExportLogInfoDetail(apistructs.ErrorLevel, fmt.Sprintf("%d", fsm.Runtime.ID), "资源配额不足无法部署", s)
	//	return 0.0, 0.0, errors.Errorf(s)
	//}

	return deployNeedCpu, deployNeedMem, nil
}

func (fsm *DeployFSMContext) ExportLogInfoDetail(level apistructs.ErrorLogLevel, id string, humanlog, detaillog string) {
	if err := fsm.bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
		ErrorLog: apistructs.ErrorLog{
			ResourceType:   apistructs.RuntimeError,
			Level:          level,
			ResourceID:     id,
			OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
			HumanLog:       humanlog,
			PrimevalLog:    detaillog,
			DedupID:        fmt.Sprintf("orch-%s", id),
		},
	}); err != nil {
		logrus.Errorf("[ExportLogInfo] %v", err)
	}
}

func (fsm *DeployFSMContext) clearPreviousMySQLAccountState() error {
	attachments, err := fsm.db.GetAttachMentsByRuntimeID(fsm.Runtime.ID)
	if err != nil {
		return err
	}
	if attachments == nil {
		return nil
	}
	var failedAccounts []string
	for _, attach := range *attachments {
		if attach.PreviousMySQLAccountID != "" && attach.MySQLAccountState == "PRE" {
			attach.MySQLAccountState = "CUR"
			if err := fsm.db.UpdateAttachment(&attach); err != nil {
				failedAccounts = append(failedAccounts, attach.PreviousMySQLAccountID)
				continue
			}
		}
	}
	if len(failedAccounts) > 0 {
		errText := fmt.Sprintf("clear attach previous account failed: %v", failedAccounts)
		fsm.pushLog(errText)
		return errors.New(errText)
	}
	return nil
}

func (fsm *DeployFSMContext) pushLog(content string) {
	if fsm.d == nil {
		return
	}
	tags := map[string]string{}
	if fsm.App != nil && len(fsm.App.OrgName) > 0 {
		tags[log.TAG_ORG_NAME] = fsm.App.OrgName
	}
	fsm.d.Log(content, tags)
}
