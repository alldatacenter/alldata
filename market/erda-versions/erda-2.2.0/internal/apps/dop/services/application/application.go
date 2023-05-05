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

package application

import (
	"context"
	"fmt"
	"net/url"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"gopkg.in/yaml.v2"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
)

type Application struct {
	db           *dao.DBClient
	bdl          *bundle.Bundle
	cms          cmspb.CmsServiceServer
	tokenService tokenpb.TokenServiceServer
}

type Option func(*Application)

func New(options ...Option) *Application {
	app := &Application{}
	for _, op := range options {
		op(app)
	}
	return app
}

func WithDBClient(db *dao.DBClient) Option {
	return func(a *Application) {
		a.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(a *Application) {
		a.bdl = bdl
	}
}

func WithPipelineCms(cms cmspb.CmsServiceServer) Option {
	return func(a *Application) {
		a.cms = cms
	}
}

func WithTokenSvc(tokenService tokenpb.TokenServiceServer) Option {
	return func(a *Application) {
		a.tokenService = tokenService
	}
}

func (a *Application) Init(initReq *apistructs.ApplicationInitRequest) (uint64, error) {
	if initReq.MobileAppName == "" {
		return 0, apierrors.ErrInitApplication.MissingParameter("mobileAppName")
	}
	if initReq.BundleID == "" {
		return 0, apierrors.ErrInitApplication.MissingParameter("bundleID")
	}
	if initReq.PackageName == "" {
		return 0, apierrors.ErrInitApplication.MissingParameter("packageName")
	}
	if initReq.MobileDisplayName == "" {
		initReq.MobileDisplayName = initReq.MobileAppName
	}

	app, err := a.bdl.GetApp(initReq.ApplicationID)
	if err != nil {
		return 0, err
	}
	if app.Mode != string(apistructs.ApplicationModeMobile) {
		return 0, errors.Errorf("only support mobile app template init")
	}

	actionProjectName := strings.Replace(initReq.MobileAppName, "-", "", -1)
	// generate mobile template action
	mobileTemplateStage := &apistructs.PipelineYmlAction{
		Type: "mobile-template",
		Params: map[string]interface{}{
			"project_name": actionProjectName,
			"display_name": initReq.MobileDisplayName,
			"bundle_id":    initReq.BundleID,
			"package_name": initReq.PackageName,
		},
	}

	// generate remote url
	var token string
	res, err := a.tokenService.QueryTokens(context.Background(), &tokenpb.QueryTokensRequest{
		Scope:     string(apistructs.OrgScope),
		ScopeId:   strconv.FormatUint(app.OrgID, 10),
		Type:      mysqltokenstore.PAT.String(),
		CreatorId: initReq.UserID,
	})
	if err != nil {
		return 0, err
	}
	if res.Total == 0 {
		return 0, errors.New("the member is not exist")
	}
	token = res.Data[0].AccessKey

	org, err := a.bdl.GetOrg(app.OrgID)
	if err != nil {
		return 0, err
	}
	u, _ := url.Parse(conf.UIPublicURL())
	remoteUrl := fmt.Sprintf("%s://git:%s@%s/%s/dop/%s/%s", u.Scheme, token, conf.UIDomain(), org.Name, app.ProjectName, app.Name)

	// generate git push action
	gitPushStage := &apistructs.PipelineYmlAction{
		Type: "git-push",
		Params: map[string]interface{}{
			"workdir":    "${mobile-template}/" + actionProjectName,
			"remote_url": remoteUrl,
		},
	}

	// generate pipeline.yml
	pys := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{
			{
				mobileTemplateStage,
			},
			{
				gitPushStage,
			},
		},
	}

	// fetch cluster name
	project, err := a.bdl.GetProject(app.ProjectID)
	if err != nil {
		return 0, err
	}
	clusterName, ok := project.ClusterConfig[string(apistructs.DevWorkspace)]
	if !ok {
		return 0, errors.Errorf("not found cluster")
	}

	ymlContent, err := yaml.Marshal(&pys)
	if err != nil {
		return 0, err
	}
	req := &apistructs.PipelineCreateRequestV2{
		PipelineYml:     string(ymlContent),
		PipelineYmlName: fmt.Sprintf("%s_%s_pipeline.yml", project.Name, app.Name),
		PipelineSource:  apistructs.PipelineSourceDice,
		ClusterName:     clusterName,
		Labels: map[string]string{
			apistructs.LabelBranch:        "master",
			apistructs.LabelOrgID:         strconv.FormatUint(app.OrgID, 10),
			apistructs.LabelProjectID:     strconv.FormatUint(app.ProjectID, 10),
			apistructs.LabelAppID:         strconv.FormatUint(app.ID, 10),
			apistructs.LabelDiceWorkspace: string(apistructs.ProdWorkspace),
		},
		AutoRunAtOnce: true,
	}
	// create pipeline info
	pipelineInfo, err := a.bdl.CreatePipeline(req)
	if err != nil {
		return 0, err
	}

	return pipelineInfo.ID, nil
}

// QueryPublishItemRelations 查询应用发布内容关联关系
func (a *Application) QueryPublishItemRelations(req apistructs.QueryAppPublishItemRelationRequest) ([]apistructs.AppPublishItemRelation, error) {
	return a.db.QueryAppPublishItemRelations(req)
}

func (a *Application) GetPublishItemRelationsMap(req apistructs.QueryAppPublishItemRelationRequest) (map[string]apistructs.AppPublishItemRelation, error) {
	relations, err := a.db.QueryAppPublishItemRelations(req)
	if err != nil {
		return nil, err
	}
	result := map[string]apistructs.AppPublishItemRelation{}
	for _, relation := range relations {
		var itemNs []string
		itemNs = append(itemNs, a.BuildItemMonitorPipelineCmsNs(relation.AppID, relation.Env))
		relation.PublishItemNs = itemNs
		result[relation.Env] = relation
	}
	return result, nil
}

// UpdatePublishItemRelations 增量更新或创建publishItemRelations
func (a *Application) UpdatePublishItemRelations(request *apistructs.UpdateAppPublishItemRelationRequest) error {
	relations, err := a.db.QueryAppPublishItemRelations(apistructs.QueryAppPublishItemRelationRequest{AppID: request.AppID})
	if err != nil {
		return err
	}
	result := map[string]apistructs.AppPublishItemRelation{}
	for _, relation := range relations {
		var itemNs []string
		itemNs = append(itemNs, a.BuildItemMonitorPipelineCmsNs(relation.AppID, relation.Env))
		relation.PublishItemNs = itemNs
		result[relation.Env] = relation
	}

	app, err := a.db.GetApplicationByID(request.AppID)
	if err != nil {
		return err
	}

	for _, workspace := range apistructs.DiceWorkspaceSlice {
		// 获取AK(TK)
		monitorAddon, err := a.bdl.ListByAddonName("monitor", strconv.FormatInt(app.ProjectID, 10), workspace.String())
		if err != nil {
			return err
		}
		if len(monitorAddon.Data) == 0 {
			return errors.Errorf("the monitor addon doesn't exist ENV: %s, projectID: %d", workspace.String(), app.ProjectID)
		}
		AK, ok := monitorAddon.Data[0].Config["TERMINUS_KEY"]
		if !ok {
			return errors.Errorf("the monitor addon doesn't have TERMINUS_KEY")
		}
		request.AKAIMap[workspace] = apistructs.MonitorKeys{AK: AK.(string), AI: app.Name}
		// 更新app relation
		relation, ok := result[workspace.String()]
		if ok && request.GetPublishItemIDByWorkspace(workspace) == relation.PublishItemID &&
			request.AppID == relation.AppID && relation.AK == AK.(string) && relation.AI == app.Name {
			// 数据库已经存在记录，且不需要更新
			request.SetPublishItemIDTo0ByWorkspace(workspace)
		}
	}

	if err := a.db.UpdateAppPublishItemRelations(request); err != nil {
		return err
	}

	return a.PipelineCmsConfigRequest(request)
}

// PipelineCmsConfigRequest 请求pipeline cms，将publisherKey和publishItemKey设置进配置管理
func (a *Application) PipelineCmsConfigRequest(request *apistructs.UpdateAppPublishItemRelationRequest) error {
	for workspace, mk := range request.AKAIMap {
		// bundle req
		if _, err := a.cms.UpdateCmsNsConfigs(apis.WithInternalClientContext(context.Background(), "dop"),
			&cmspb.CmsNsConfigsUpdateRequest{
				Ns:             a.BuildItemMonitorPipelineCmsNs(request.AppID, workspace.String()),
				PipelineSource: apistructs.PipelineSourceDice.String(),
				KVs:            map[string]*cmspb.PipelineCmsConfigValue{"AI": {Value: mk.AI}, "AK": {Value: mk.AK}},
			}); err != nil {
			return err
		}
	}

	return nil
}

func (a *Application) RemovePublishItemRelations(request *apistructs.RemoveAppPublishItemRelationsRequest) error {
	return a.db.RemovePublishItemRelations(request)
}

// BuildItemMonitorPipelineCmsNs 生成namespace
func (a *Application) BuildItemMonitorPipelineCmsNs(appID int64, workspace string) string {
	return fmt.Sprintf("publish-item-monitor-%s-%d", workspace, appID)
}
