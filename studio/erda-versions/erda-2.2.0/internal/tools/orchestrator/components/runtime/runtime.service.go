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

package runtime

import (
	"context"
	"encoding/json"
	"net/url"
	"strconv"

	"github.com/pkg/errors"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/pkg/transport"
	cpb "github.com/erda-project/erda-proto-go/common/pb"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/orchestrator/runtime/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// Service implements pb.RuntimeServiceServer
type Service struct {
	logger logs.Logger

	bundle           BundleService
	db               DBService
	evMgr            EventManagerService
	serviceGroupImpl servicegroup.ServiceGroup
	clusterSvc       clusterpb.ClusterServiceServer
}

func convertRuntimeToPB(runtime *dbclient.Runtime, app *apistructs.ApplicationDTO) *pb.Runtime {
	return &pb.Runtime{
		Name:            runtime.Name,
		GitBranch:       runtime.Name,
		Workspace:       runtime.Workspace,
		ClusterName:     runtime.ClusterName,
		Status:          runtime.Status,
		ClusterID:       runtime.ClusterId,
		ApplicationID:   runtime.ApplicationID,
		ApplicationName: app.Name,
		ProjectID:       app.ProjectID,
		ProjectName:     app.ProjectName,
		OrgID:           app.OrgID,
		Id:              runtime.ID,
	}
}

// Delete turn status of runtime to be Deleting
func (r *Service) Delete(operator user.ID, orgID uint64, runtimeID uint64) (*pb.Runtime, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrDeleteRuntime.InternalError(err)
	}
	// TODO: do not query app
	app, err := r.bundle.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, err
	}

	err = r.checkRuntimeScopePermission(operator, runtime, apistructs.DeleteAction)
	if err != nil {
		return nil, err
	}

	if runtime.LegacyStatus == dbclient.LegacyStatusDeleting {
		// already marked
		return convertRuntimeToPB(runtime, app), nil
	}
	// set status to DELETING
	runtime.LegacyStatus = dbclient.LegacyStatusDeleting
	if err := r.db.UpdateRuntime(runtime); err != nil {
		return nil, apierrors.ErrDeleteRuntime.InternalError(err)
	}
	event := events.RuntimeEvent{
		EventName: events.RuntimeDeleting,
		Runtime:   dbclient.ConvertRuntimeDTO(runtime, app),
		Operator:  operator.String(),
	}
	r.evMgr.EmitEvent(&event)
	// TODO: should emit RuntimeDeleted after really deleted or RuntimeDeleteFailed if failed
	return convertRuntimeToPB(runtime, app), nil
}

func (r *Service) DelRuntime(ctx context.Context, req *pb.DelRuntimeRequest) (*pb.Runtime, error) {
	var (
		userID    user.ID
		orgID     uint64
		err       error
		runtimeID uint64
	)

	if userID, _, err = r.getUserAndOrgID(ctx); err != nil {
		return nil, err
	}

	runtimeID, err = strconv.ParseUint(req.Id, 10, 64)

	if err != nil {
		return nil, apierrors.ErrDeleteRuntime.InvalidParameter(strutil.Concat("runtimeID: ", req.Id))
	}

	return r.Delete(userID, orgID, runtimeID)
}

func (r *Service) findByIDOrName(idOrName string, appIDStr string, workspace string) (*dbclient.Runtime, error) {
	runtimeID, err := strconv.ParseUint(idOrName, 10, 64)
	if err == nil {
		// parse int success, idOrName is an id
		return r.db.GetRuntimeAllowNil(runtimeID)
	}

	// idOrName is a name
	if workspace == "" {
		return nil, apierrors.ErrGetRuntime.MissingParameter("workspace")
	}

	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("applicationID: ", appIDStr))
	}

	// TODO: we shall not un-escape runtimeName, after we fix existing data and deny '/'
	name, err := url.PathUnescape(idOrName)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("idOrName: ", idOrName))
	}

	runtime, err := r.db.FindRuntime(spec.RuntimeUniqueId{Name: name, Workspace: workspace, ApplicationId: appID})
	if err == nil && runtime == nil {
		return nil, apierrors.ErrGetRuntime.NotFound()
	}

	return runtime, err
}

func (r *Service) getUserAndOrgID(ctx context.Context) (userID user.ID, orgID uint64, err error) {
	orgIntID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		err = apierrors.ErrGetRuntime.InvalidParameter(errors.New("Org-ID"))
		return
	}

	orgID = uint64(orgIntID)

	userID = user.ID(apis.GetUserID(ctx))
	if userID.Invalid() {
		err = apierrors.ErrGetRuntime.NotLogin()
		return
	}

	return
}

func (r *Service) checkRuntimeScopePermission(userID user.ID, runtime *dbclient.Runtime, action string) error {
	perm, err := r.bundle.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   action,
	})

	if err != nil {
		return err
	}

	if !perm.Access {
		return apierrors.ErrGetRuntime.AccessDenied()
	}

	return nil
}

func (r *Service) getRuntimeByRequest(request *pb.GetRuntimeRequest) (*dbclient.Runtime, error) {
	runtime, err := r.findByIDOrName(request.NameOrID, request.AppID, request.Workspace)

	if err != nil {
		return nil, err
	}

	if runtime == nil {
		return nil, apierrors.ErrGetRuntime.NotFound()
	}

	return runtime, nil
}

func (r *Service) getDeployment(runtimeID uint64) (*dbclient.Deployment, error) {
	deployment, err := r.db.FindLastDeployment(runtimeID)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}

	if deployment == nil {
		return nil, apierrors.ErrGetRuntime.InvalidState("last deployment not found")
	}

	return deployment, nil
}

// GetRuntime Get detail information of a single runtime
func (r *Service) GetRuntime(ctx context.Context, request *pb.GetRuntimeRequest) (*pb.RuntimeInspect, error) {
	var (
		err        error
		userID     user.ID
		runtime    *dbclient.Runtime
		deployment *dbclient.Deployment
		domainMap  map[string][]string
		cluster    *clusterpb.ClusterInfo
		sg         *apistructs.ServiceGroup
		app        *apistructs.ApplicationDTO
		ri         *pb.RuntimeInspect
		dice       diceyml.Object
	)

	ri = &pb.RuntimeInspect{
		Services:    make(map[string]*pb.Service),
		LastMessage: make(map[string]*pb.StatusMap),
	}

	if userID, _, err = r.getUserAndOrgID(ctx); err != nil {
		return nil, err
	}

	if runtime, err = r.getRuntimeByRequest(request); err != nil {
		return nil, err
	}

	if err = r.checkRuntimeScopePermission(userID, runtime, apistructs.GetAction); err != nil {
		return nil, err
	}

	if deployment, err = r.getDeployment(runtime.ID); err != nil {
		return nil, err
	}

	if domainMap, err = r.inspectDomains(runtime.ID); err != nil {
		return nil, err
	}

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := r.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: runtime.ClusterName})
	if err != nil {
		return nil, err
	}
	cluster = resp.Data

	if runtime.ScheduleName.Name != "" {
		sg, _ = r.serviceGroupImpl.InspectServiceGroupWithTimeout(runtime.ScheduleName.Args())
	}

	if app, err = r.bundle.GetApp(runtime.ApplicationID); err != nil {
		return nil, err
	}

	if err = json.Unmarshal([]byte(deployment.Dice), &dice); err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidState(strutil.Concat("dice.json invalid: ", err.Error()))
	}

	fillInspectBase(ri, runtime, sg)
	fillInspectByDeployment(ri, runtime, deployment, cluster)
	fillInspectByApp(ri, runtime, app)
	fillInspectDataWithServiceGroup(ri, dice.Services, dice.Jobs, sg, domainMap, string(deployment.Status))
	updateStatusToDisplay(ri)
	if deployment.Status == apistructs.DeploymentStatusDeploying {
		updateStatusWhenDeploying(ri)
	}

	return ri, nil
}

func (r *Service) CheckRuntimeExist(ctx context.Context, req *pb.CheckRuntimeExistReq) (*pb.CheckRuntimeExistResp, error) {
	runtime, err := r.db.GetRuntimeAllowNil(req.GetId())
	if err != nil {
		return nil, err
	}
	return &pb.CheckRuntimeExistResp{Ok: runtime != nil}, nil
}

func updateStatusWhenDeploying(runtime *pb.RuntimeInspect) {
	if runtime == nil {
		return
	}
	if runtime.Status == "UnHealthy" {
		runtime.Status = "Progressing"
	}
	for _, v := range runtime.Services {
		if v.Status == "UnHealthy" {
			v.Status = "Progressing"
		}
	}
}
func updateStatusToDisplay(runtime *pb.RuntimeInspect) {
	if runtime == nil {
		return
	}
	runtime.Status = isStatusForDisplay(runtime.Status)
	for key := range runtime.Services {
		runtime.Services[key].Status = isStatusForDisplay(runtime.Services[key].Status)
	}
}

func isStatusForDisplay(status string) string {
	switch status {
	case apistructs.RuntimeStatusHealthy, apistructs.RuntimeStatusUnHealthy, apistructs.RuntimeStatusInit:
		return status
	case "Ready", "ready":
		return apistructs.RuntimeStatusHealthy
	default:
		return status
	}
}

func fillInspectByApp(data *pb.RuntimeInspect, runtime *dbclient.Runtime, app *apistructs.ApplicationDTO) {
	data.ProjectID = app.ProjectID
	data.ApplicationName = app.Name
	data.CreatedAt = timestamppb.New(runtime.CreatedAt)
	data.UpdatedAt = timestamppb.New(runtime.UpdatedAt)
	data.TimeCreated = timestamppb.New(runtime.CreatedAt)
}

func fillInspectByDeployment(data *pb.RuntimeInspect, runtime *dbclient.Runtime, deployment *dbclient.Deployment, cluster *clusterpb.ClusterInfo) {
	data.DeployStatus = string(deployment.Status)
	if deployment.Status == apistructs.DeploymentStatusDeploying ||
		deployment.Status == apistructs.DeploymentStatusWaiting ||
		deployment.Status == apistructs.DeploymentStatusInit ||
		deployment.Status == apistructs.DeploymentStatusWaitApprove {
		data.Status = apistructs.RuntimeStatusInit
	}

	if runtime.LegacyStatus == "DELETING" {
		data.DeleteStatus = "DELETING"
	}
	data.ReleaseID = deployment.ReleaseId
	data.ClusterID = runtime.ClusterId
	data.ClusterName = runtime.ClusterName
	data.ClusterType = cluster.Type
	data.Extra = &pb.Extra{
		ApplicationID: runtime.ApplicationID,
		Workspace:     runtime.Workspace,
		BuildID:       deployment.BuildId,
	}
}

func fillInspectDataWithServiceGroup(data *pb.RuntimeInspect, targetService diceyml.Services, targetJob diceyml.Jobs,
	sg *apistructs.ServiceGroup, domainMap map[string][]string, status string) {
	statusServiceMap := map[string]string{}
	replicaMap := map[string]int{}
	resourceMap := map[string]*pb.Resources{}
	statusMap := make(map[string]*pb.StatusMap)
	if sg != nil {
		if sg.Status != apistructs.StatusReady && sg.Status != apistructs.StatusHealthy {
			for _, serviceItem := range sg.Services {
				statusMap[serviceItem.Name] = &pb.StatusMap{
					Msg:    serviceItem.LastMessage,
					Reason: serviceItem.Reason,
				}
			}
		}

		data.LastMessage = statusMap

		for _, v := range sg.Services {
			statusServiceMap[v.Name] = string(v.StatusDesc.Status)
			replicaMap[v.Name] = v.Scale
			resourceMap[v.Name] = &pb.Resources{
				Cpu:  v.Resources.Cpu,
				Mem:  int64(int(v.Resources.Mem)),
				Disk: int64(int(v.Resources.Disk)),
			}
		}
	}

	// TODO: no diceJson and no overlay, we just read dice from releaseId
	for k, v := range targetService {
		var expose []string
		var svcPortExpose bool
		// serv.Expose will abandoned, serv.Ports.Expose is recommended
		for _, svcPort := range v.Ports {
			if svcPort.Expose {
				svcPortExpose = true
			}
		}
		if len(v.Expose) != 0 || svcPortExpose {
			expose = domainMap[k]
		}

		runtimeInspectService := &pb.Service{
			Resources: &pb.Resources{
				Cpu:  v.Resources.CPU,
				Mem:  int64(v.Resources.Mem),
				Disk: int64(v.Resources.Disk),
			},
			Envs:        v.Envs,
			Addrs:       convertInternalAddrs(sg, k),
			Expose:      expose,
			Status:      status,
			Deployments: &pb.Deployments{Replicas: 0},
		}

		if sgStatus, ok := statusServiceMap[k]; ok {
			runtimeInspectService.Status = sgStatus
		}
		if sgReplicas, ok := replicaMap[k]; ok {
			runtimeInspectService.Deployments.Replicas = uint64(sgReplicas)
		}
		if sgResources, ok := resourceMap[k]; ok {
			runtimeInspectService.Resources = sgResources
		}

		data.Services[k] = runtimeInspectService
	}
	for k, v := range targetJob {
		runtimeInspectService := &pb.Service{
			Resources: &pb.Resources{
				Cpu:  v.Resources.CPU,
				Mem:  int64(v.Resources.Mem),
				Disk: int64(v.Resources.Disk),
			},
			Envs:        v.Envs,
			Type:        "job",
			Status:      statusServiceMap[k],
			Deployments: &pb.Deployments{Replicas: 1},
		}
		data.Services[k] = runtimeInspectService
	}
	data.Resources = &pb.Resources{Cpu: 0, Mem: 0, Disk: 0}
	for _, v := range data.Services {
		if v.Type == "job" {
			continue
		}
		data.Resources.Cpu += v.Resources.Cpu * float64(v.Deployments.Replicas)
		data.Resources.Mem += v.Resources.Mem * int64(v.Deployments.Replicas)
		data.Resources.Disk += v.Resources.Disk * int64(v.Deployments.Replicas)
	}
}

func convertInternalAddrs(sg *apistructs.ServiceGroup, serviceName string) []string {
	addrs := make([]string, 0)
	if sg == nil {
		return addrs
	}
	for _, s := range sg.Services {
		if s.Name != serviceName {
			continue
		}
		for _, p := range s.Ports {
			addrs = append(addrs, s.Vip+":"+strconv.Itoa(p.Port))
		}
	}
	return addrs
}

func fillInspectBase(data *pb.RuntimeInspect, runtime *dbclient.Runtime, sg *apistructs.ServiceGroup) {
	data.Id = runtime.ID
	data.Name = runtime.Name
	data.ServiceGroupNamespace = runtime.ScheduleName.Namespace
	data.ServiceGroupName = runtime.ScheduleName.Name
	data.Source = string(runtime.Source)
	data.Status = apistructs.RuntimeStatusUnHealthy
	if runtime.ScheduleName.Namespace != "" && runtime.ScheduleName.Name != "" && sg != nil {
		if sg.Status == "Ready" || sg.Status == "Healthy" {
			data.Status = apistructs.RuntimeStatusHealthy
		}
	}
	if runtime.LegacyStatus == "DELETING" {
		data.DeleteStatus = "DELETING"
	}
}

func (r *Service) Healthz(ctx context.Context, req *cpb.VoidRequest) (*cpb.VoidResponse, error) {
	return &cpb.VoidResponse{}, nil
}

func (r *Service) inspectDomains(id uint64) (map[string][]string, error) {
	domains, err := r.db.FindDomainsByRuntimeId(id)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	domainMap := make(map[string][]string)
	for _, d := range domains {
		if domainMap[d.EndpointName] == nil {
			domainMap[d.EndpointName] = make([]string, 0)
		}
		domainMap[d.EndpointName] = append(domainMap[d.EndpointName], "http://"+d.Domain)
	}

	return domainMap, nil
}

type ServiceOption func(*Service) *Service

func WithBundleService(s BundleService) ServiceOption {
	return func(service *Service) *Service {
		service.bundle = s
		return service
	}
}

func WithDBService(db DBService) ServiceOption {
	return func(service *Service) *Service {
		service.db = db
		return service
	}
}

func WithEventManagerService(evMgr EventManagerService) ServiceOption {
	return func(service *Service) *Service {
		service.evMgr = evMgr
		return service
	}
}

func WithServiceGroupImpl(serviceGroupImpl servicegroup.ServiceGroup) ServiceOption {
	return func(service *Service) *Service {
		service.serviceGroupImpl = serviceGroupImpl
		return service
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) ServiceOption {
	return func(service *Service) *Service {
		service.clusterSvc = clusterSvc
		return service
	}
}

func NewRuntimeService(options ...ServiceOption) pb.RuntimeServiceServer {
	s := &Service{}

	for _, option := range options {
		option(s)
	}

	return s
}
