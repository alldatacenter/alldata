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

package deployment_order

import (
	"context"
	"fmt"
	"strings"
	"sync"

	"github.com/google/uuid"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	infrai18n "github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/runtime"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	I18nPermissionDeniedKey   = "DeployPermissionDenied"
	I18nFailedToParseErdaYaml = "FailedToParseErdaYaml"
	I18nEmptyErdaYaml         = "EmptyErdaYaml"
	I18nCustomAddonNotReady   = "CustomAddonNotReady"
	I18nAddonDoesNotExist     = "AddonDoesNotExist"
	I18nApplicationDeploying  = "ApplicationDeploying"

	AddonCustomCategory = "custom"
)

var (
	lang struct{ Lang string }
)

func (d *DeploymentOrder) RenderDetail(ctx context.Context, id, userId, releaseId, workspace string, modes []string) (*apistructs.DeploymentOrderDetail, error) {
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	langCodes, _ := ctx.Value(lang).(infrai18n.LanguageCodes)

	releaseResp, err := d.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: releaseId})
	if err != nil {
		return nil, fmt.Errorf("failed to get release %s, err: %v", releaseId, err)
	}

	if access, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userId,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(releaseResp.Data.ProjectID),
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}); err != nil || !access.Access {
		return nil, apierrors.ErrRenderDeploymentOrderDetail.AccessDenied()
	}

	asi, err := d.composeAppsInfoByReleaseResp(releaseResp.Data, workspace, modes)
	if err != nil {
		return nil, err
	}

	err = d.renderAppsPreCheckResult(langCodes, releaseResp.Data.ProjectID, userId, workspace, &asi)
	if err != nil {
		return nil, fmt.Errorf("failed to render application precheck result, err: %v", err)
	}

	orderId := id
	if orderId == "" {
		orderId = uuid.NewString()
	}

	return &apistructs.DeploymentOrderDetail{
		DeploymentOrderItem: apistructs.DeploymentOrderItem{
			ID:   orderId,
			Name: utils.ParseOrderName(orderId),
			ReleaseInfo: &apistructs.ReleaseInfo{
				Id:        releaseResp.Data.ReleaseID,
				Version:   releaseResp.Data.Version,
				Type:      convertReleaseType(releaseResp.Data.IsProjectRelease),
				Creator:   releaseResp.Data.UserID,
				CreatedAt: releaseResp.Data.CreatedAt.AsTime(),
				UpdatedAt: releaseResp.Data.UpdatedAt.AsTime(),
			},
			Type:      parseOrderType(releaseResp.Data.IsProjectRelease),
			Workspace: workspace,
		},
		ApplicationsInfo: asi,
	}, nil
}

func (d *DeploymentOrder) renderAppsPreCheckResult(langCodes infrai18n.LanguageCodes, projectId int64, userId, workspace string, asi *[][]*apistructs.ApplicationInfo) error {
	if asi == nil {
		return nil
	}

	appList := make([]string, 0)
	for _, apps := range *asi {
		for _, info := range apps {
			appList = append(appList, info.Name)
		}
	}

	appStatus, err := d.getDeploymentsStatus(workspace, uint64(projectId), appList)
	if err != nil {
		return err
	}

	wg := sync.WaitGroup{}
	mux := sync.Mutex{}
	for i := range *asi {
		for j := range (*asi)[i] {
			wg.Add(1)
			go func(i, j int) {
				defer wg.Done()
				app := (*asi)[i][j]
				failReasons, err := d.staticPreCheck(langCodes, userId, workspace, projectId, app.Id, []byte(app.DiceYaml))
				if err != nil {
					logrus.Errorf("failed to static pre check app %s, %v", app.Name, err)
					failReasons = append(failReasons, err.Error())
				}
				mux.Lock()
				isDeploying, ok := appStatus[app.Id]
				mux.Unlock()
				if ok && isDeploying {
					failReasons = append(failReasons, i18n.LangCodesSprintf(langCodes, I18nApplicationDeploying, app.Name))
				}

				checkResult := &apistructs.PreCheckResult{
					Success: true,
				}

				if len(failReasons) != 0 {
					checkResult.Success = false
					checkResult.FailReasons = failReasons
				}

				app.PreCheckResult = checkResult
			}(i, j)
		}
	}
	wg.Wait()
	return nil
}

func (d *DeploymentOrder) getDeploymentsStatus(workspace string, projectId uint64, appList []string) (map[uint64]bool, error) {
	ret := make(map[uint64]bool)

	runtimes, err := d.db.ListRuntimesByAppsName(workspace, projectId, appList)
	if err != nil {
		return nil, err
	}

	for _, r := range *runtimes {
		if runtime.IsDeploying(r.DeploymentStatus) {
			ret[r.ApplicationID] = true
			continue
		}
		ret[r.ApplicationID] = false
	}

	return ret, nil
}

func (d *DeploymentOrder) staticPreCheck(langCodes infrai18n.LanguageCodes, userId, workspace string, projectId int64, appId uint64, erdaYaml []byte) ([]string, error) {
	failReasons := make([]string, 0)

	// check execute permission
	isOk, err := d.checkExecutePermission(userId, workspace, appId)
	if err != nil {
		return nil, err
	}
	if !isOk {
		failReasons = append(failReasons, i18n.LangCodesSprintf(langCodes, I18nPermissionDeniedKey))
	}

	if len(erdaYaml) == 0 {
		failReasons = append(failReasons, i18n.LangCodesSprintf(langCodes, I18nEmptyErdaYaml))
		return failReasons, nil
	}

	// parse erda yaml
	dy, err := diceyml.New(erdaYaml, true)
	if err != nil {
		failReasons = append(failReasons, i18n.LangCodesSprintf(langCodes, I18nFailedToParseErdaYaml))
		return failReasons, nil
	}

	customAddonsMap := make(map[string]byte)
	customAddons, err := d.db.ListCustomInstancesByProjectAndEnv(projectId, strings.ToUpper(workspace))
	if err != nil {
		return nil, err
	}

	for _, v := range customAddons {
		customAddonsMap[v.Name] = 0
	}

	// check addon
	for instanceName, addOn := range dy.Obj().AddOns {
		plan := strutil.Split(addOn.Plan, ":", true)
		if len(plan) < 2 {
			plan = append(plan, apistructs.AddonDefaultPlan)
		}

		extensionI, ok := addon.AddonInfos.Load(plan[0])
		if !ok {
			// addon doesn't support
			failReasons = append(failReasons, i18n.LangCodesSprintf(langCodes, I18nAddonDoesNotExist, instanceName, plan[0]))
			continue
		}

		extension, ok := extensionI.(apistructs.Extension)
		if !ok {
			return nil, fmt.Errorf("failed to assert extension (%s) to Extension", plan[0])
		}

		if extension.Category == AddonCustomCategory {
			_, ok := customAddonsMap[instanceName]
			if !ok {
				failReasons = append(failReasons, i18n.LangCodesSprintf(langCodes, I18nCustomAddonNotReady, instanceName))
				continue
			}
		}
	}

	return failReasons, nil
}

func (d *DeploymentOrder) composeAppsInfoByReleaseResp(releaseResp *pb.ReleaseGetResponseData, workspace string, modes []string) (
	[][]*apistructs.ApplicationInfo, error) {

	asi := make([][]*apistructs.ApplicationInfo, 0)
	if releaseResp.IsProjectRelease {
		for _, mode := range modes {
			if _, ok := releaseResp.Modes[mode]; !ok {
				return nil, errors.Errorf("mode %s does not exist in release modes list", mode)
			}
		}
		deployList := renderDeployList(modes, releaseResp.Modes)
		params, err := d.fetchApplicationsParams(releaseResp, deployList, workspace)
		if err != nil {
			return nil, fmt.Errorf("failed to fetch deployment params, err: %v", err)
		}

		for _, batch := range deployList {
			ai := make([]*apistructs.ApplicationInfo, 0)
			for _, r := range batch {

				ai = append(ai, &apistructs.ApplicationInfo{
					Id:       uint64(r.ApplicationID),
					Name:     r.ApplicationName,
					Params:   covertParamsType(params[r.ApplicationName]),
					DiceYaml: r.DiceYml,
				})
			}
			asi = append(asi, ai)
		}
	} else {
		params, err := d.fetchDeploymentParams(releaseResp.ApplicationID, workspace)
		if err != nil {
			return nil, fmt.Errorf("failed to fetch deployment params, err: %v", err)
		}

		asi = append(asi, []*apistructs.ApplicationInfo{
			{
				Id:       uint64(releaseResp.ApplicationID),
				Name:     releaseResp.ApplicationName,
				Params:   covertParamsType(params),
				DiceYaml: releaseResp.Diceyml,
			},
		})
	}

	return asi, nil
}
