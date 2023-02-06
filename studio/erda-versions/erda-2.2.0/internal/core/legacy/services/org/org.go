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

// Package org 封装企业资源相关操作
package org

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/go-redis/redis"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Org 资源对象操作封装
type Org struct {
	db           *dao.DBClient
	uc           *ucauth.UCClient
	bdl          *bundle.Bundle
	redisCli     *redis.Client
	trans        i18n.Translator
	tokenService tokenpb.TokenServiceServer
}

// Option 定义 Org 对象的配置选项
type Option func(*Org)

// New 新建 Org 实例，通过 Org 实例操作企业资源
func New(options ...Option) *Org {
	o := &Org{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(o *Org) {
		o.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(o *Org) {
		o.uc = uc
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(o *Org) {
		o.bdl = bdl
	}
}

// WithRedisClient 配置 redis client
func WithRedisClient(cli *redis.Client) Option {
	return func(o *Org) {
		o.redisCli = cli
	}
}

// WithI18n sets the translator
func WithI18n(trans i18n.Translator) Option {
	return func(o *Org) {
		o.trans = trans
	}
}

func WithTokenSvc(tokenService tokenpb.TokenServiceServer) Option {
	return func(o *Org) {
		o.tokenService = tokenService
	}
}

// CreateWithEvent 创建企业 & 发送创建事件
func (o *Org) CreateWithEvent(userID string, createReq apistructs.OrgCreateRequest) (*model.Org, error) {
	// 创建企业
	org, err := o.Create(createReq)
	if err != nil {
		return nil, err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.OrgEvent,
			Action:        bundle.CreateAction,
			OrgID:         strconv.FormatInt(org.ID, 10),
			ProjectID:     "-1",
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderCoreServices,
		Content: *org,
	}
	// 发送企业创建事件
	if err = o.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send org create event, (%v)", err)
	}
	return org, nil
}

// Create 创建企业处理逻辑
func (o *Org) Create(createReq apistructs.OrgCreateRequest) (*model.Org, error) {
	if len(createReq.Admins) == 0 {
		return nil, errors.Errorf("failed to create org(param admins is empty)")
	}
	if createReq.DisplayName == "" {
		createReq.DisplayName = createReq.Name
	}
	if conf.RedirectPathList[strutil.Concat("/", createReq.Name)] {
		return nil, errors.Errorf("Org name in legacy redirect paths")
	}
	// name 校验
	if err := strutil.Validate(createReq.Name,
		strutil.NoChineseValidator,
		strutil.MaxLenValidator(50),
		strutil.AlphaNumericDashUnderscoreValidator,
	); err != nil {
		return nil, err
	}

	// 校验企业名唯一
	existOrg, err := o.db.GetOrgByName(createReq.Name)
	if err != nil && err != dao.ErrNotFoundOrg {
		return nil, err
	}
	if existOrg != nil {
		return nil, errors.Errorf("org name already exist")
	}

	// 添加企业至DB
	userID := createReq.Admins[0]
	org := &model.Org{
		Name:        createReq.Name,
		DisplayName: createReq.DisplayName,
		Desc:        createReq.Desc,
		Logo:        createReq.Logo,
		Locale:      createReq.Locale,
		UserID:      userID,
		Type:        createReq.Type.String(),
		Status:      "ACTIVE",
		IsPublic:    createReq.IsPublic,
	}
	if err := o.db.CreateOrg(org); err != nil {
		logrus.Warnf("failed to insert info to db, (%v)", err)
		return nil, errors.Errorf("failed to insert org info to db")
	}

	// 新增企业权限记录
	users, err := o.uc.FindUsers([]string{userID})
	if err != nil {
		logrus.Warnf("failed to query user info, (%v)", err)
	}
	if len(users) > 0 {
		_, err := o.tokenService.CreateToken(context.Background(), &tokenpb.CreateTokenRequest{
			Scope:     string(apistructs.OrgScope),
			ScopeId:   strconv.FormatInt(org.ID, 10),
			Type:      mysqltokenstore.PAT.String(),
			CreatorId: userID,
		})
		if err != nil {
			logrus.Warnf("failed to create token, (%v)", err)
		}

		member := model.Member{
			ScopeType:  apistructs.OrgScope,
			ScopeID:    org.ID,
			ScopeName:  org.Name,
			UserID:     userID,
			Email:      users[0].Email,
			Mobile:     users[0].Phone,
			Name:       users[0].Name,
			Nick:       users[0].Nick,
			Avatar:     users[0].AvatarURL,
			UserSyncAt: time.Now(),
			OrgID:      org.ID,
		}
		if err = o.db.CreateMember(&member); err != nil {
			logrus.Warnf("failed to insert member info to db, (%v)", err)
		}
		if err = o.db.CreateMemberExtra(&model.MemberExtra{
			UserID:        userID,
			ScopeType:     apistructs.OrgScope,
			ScopeID:       org.ID,
			ResourceKey:   apistructs.RoleResourceKey,
			ResourceValue: types.RoleOrgManager,
		}); err != nil {
			logrus.Warnf("failed to insert member extra to db, (%v)", err)
		}
	}

	return org, nil
}

// UpdateWithEvent 更新企业 & 发送更新事件
func (o *Org) UpdateWithEvent(orgID int64, updateReq apistructs.OrgUpdateRequestBody) (*model.Org, apistructs.AuditMessage, error) {
	if updateReq.DisplayName == "" {
		updateReq.DisplayName = updateReq.Name
	}
	org, auditMessage, err := o.Update(orgID, updateReq)
	if err != nil {
		return nil, apistructs.AuditMessage{}, err
	}

	ev := &apistructs.EventCreateRequest{
		EventHeader: apistructs.EventHeader{
			Event:         bundle.OrgEvent,
			Action:        bundle.UpdateAction,
			OrgID:         strconv.FormatInt(org.ID, 10),
			ProjectID:     "-1",
			ApplicationID: "-1",
			TimeStamp:     time.Now().Format("2006-01-02 15:04:05"),
		},
		Sender:  bundle.SenderCoreServices,
		Content: *org,
	}
	// 发送企业更新事件
	if err = o.bdl.CreateEvent(ev); err != nil {
		logrus.Warnf("failed to send org update event, (%v)", err)
	}

	return org, auditMessage, nil
}

// Update 更新企业
func (o *Org) Update(orgID int64, updateReq apistructs.OrgUpdateRequestBody) (*model.Org, apistructs.AuditMessage, error) {
	// 检查待更新的org是否存在
	org, err := o.db.GetOrg(orgID)
	if err != nil {
		logrus.Warnf("failed to find org when update org, (%v)", err)
		return nil, apistructs.AuditMessage{}, errors.Errorf("failed to find org when update org")
	}
	auditMessage := getAuditMessage(org, updateReq)

	// 更新企业元信息，企业名称暂不可更改
	org.Desc = updateReq.Desc
	org.Logo = updateReq.Logo
	org.DisplayName = updateReq.DisplayName
	org.Locale = updateReq.Locale
	org.IsPublic = updateReq.IsPublic
	if updateReq.Config != nil {
		org.Config.SMTPHost = updateReq.Config.SMTPHost
		org.Config.SMTPPort = updateReq.Config.SMTPPort
		org.Config.SMTPUser = updateReq.Config.SMTPUser
		org.Config.SMTPIsSSL = updateReq.Config.SMTPIsSSL
		org.Config.SMSKeyID = updateReq.Config.SMSKeyID
		org.Config.SMSSignName = updateReq.Config.SMSSignName
		if updateReq.Config.SMSKeySecret != "" && updateReq.Config.SMSKeySecret != apistructs.SECRECT_PLACEHOLDER {
			org.Config.SMSKeySecret = updateReq.Config.SMSKeySecret
		}
		if updateReq.Config.SMTPPassword != "" && updateReq.Config.SMTPPassword != apistructs.SECRECT_PLACEHOLDER {
			org.Config.SMTPPassword = updateReq.Config.SMTPPassword
		}
	}
	if updateReq.BlockoutConfig != nil {
		org.BlockoutConfig.BlockDEV = updateReq.BlockoutConfig.BlockDEV
		org.BlockoutConfig.BlockTEST = updateReq.BlockoutConfig.BlockTEST
		org.BlockoutConfig.BlockStage = updateReq.BlockoutConfig.BlockStage
		org.BlockoutConfig.BlockProd = updateReq.BlockoutConfig.BlockProd
	}

	// 更新企业信息至DB
	if err = o.db.UpdateOrg(&org); err != nil {
		logrus.Warnf("failed to update org, (%v)", err)
		return nil, apistructs.AuditMessage{}, errors.Errorf("failed to update org")
	}
	return &org, auditMessage, nil
}

func getAuditMessage(org model.Org, req apistructs.OrgUpdateRequestBody) apistructs.AuditMessage {
	var messageZH, messageEN strings.Builder
	if org.DisplayName != req.DisplayName {
		messageZH.WriteString(fmt.Sprintf("组织名称由 %s 改为 %s ", org.DisplayName, req.DisplayName))
		messageEN.WriteString(fmt.Sprintf("org name updated from %s to %s ", org.DisplayName, req.DisplayName))
	}
	if org.Locale != req.Locale {
		messageZH.WriteString(fmt.Sprintf("通知语言改为%s ", func() string {
			switch req.Locale {
			case "en-US":
				return "英文"
			case "zh-CN":
				return "中文"
			default:
				return ""
			}
		}()))
		messageEN.WriteString(fmt.Sprintf("language updated to %s ", req.Locale))
	}
	if org.IsPublic != req.IsPublic {
		messageZH.WriteString(func() string {
			if req.IsPublic {
				return "改为公开组织 "
			}
			return "改为私有组织 "
		}())
		messageEN.WriteString(func() string {
			if req.IsPublic {
				return "org updated to public "
			}
			return "org updated to private "
		}())
	}
	if org.Logo != req.Logo {
		messageZH.WriteString("组织Logo发生变更 ")
		messageEN.WriteString("org Logo changed ")
	}
	if org.Desc != req.Desc {
		messageZH.WriteString("组织描述信息发生变更 ")
		messageEN.WriteString("org desc changed ")
	}
	if req.BlockoutConfig != nil {
		if org.BlockoutConfig.BlockDEV != req.BlockoutConfig.BlockDEV {
			messageZH.WriteString(func() string {
				if req.BlockoutConfig.BlockDEV {
					return "开发环境开启封网 "
				}
				return "开发环境关闭封网 "
			}())
			messageEN.WriteString(func() string {
				if req.BlockoutConfig.BlockDEV {
					return "block network opened in dev environment "
				}
				return "block network closed in dev environment "
			}())
		}
		if org.BlockoutConfig.BlockTEST != req.BlockoutConfig.BlockTEST {
			messageZH.WriteString(func() string {
				if req.BlockoutConfig.BlockTEST {
					return "测试环境开启封网 "
				}
				return "测试环境关闭封网 "
			}())
			messageEN.WriteString(func() string {
				if req.BlockoutConfig.BlockTEST {
					return "block network opened in test environment "
				}
				return "block network closed in test environment "
			}())
		}
		if org.BlockoutConfig.BlockStage != req.BlockoutConfig.BlockStage {
			messageZH.WriteString(func() string {
				if req.BlockoutConfig.BlockStage {
					return "预发环境开启封网 "
				}
				return "预发环境关闭封网 "
			}())
			messageEN.WriteString(func() string {
				if req.BlockoutConfig.BlockStage {
					return "block network opened in staging environment "
				}
				return "block network closed in staging environment "
			}())
		}
		if org.BlockoutConfig.BlockProd != req.BlockoutConfig.BlockProd {
			messageZH.WriteString(func() string {
				if req.BlockoutConfig.BlockProd {
					return "生产环境开启封网 "
				}
				return "生产环境关闭封网 "
			}())
			messageEN.WriteString(func() string {
				if req.BlockoutConfig.BlockProd {
					return "block network opened in prod environment "
				}
				return "block network closed in prod environment "
			}())
		}
	}
	if messageZH.Len() == 0 {
		messageZH.WriteString("无信息变更")
		messageEN.WriteString("no message changed")
	}
	return apistructs.AuditMessage{
		MessageZH: messageZH.String(),
		MessageEN: messageEN.String(),
	}
}

// Get 获取企业
func (o *Org) Get(orgID int64) (*model.Org, error) {
	// 检查org是否存在
	org, err := o.db.GetOrg(orgID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to fetch org")
	}
	if org.DisplayName == "" {
		org.DisplayName = org.Name
	}
	return &org, nil
}

// Delete 删除企业
func (o *Org) Delete(orgID int64) error {
	// 检查企业下是否有项目
	projects, err := o.db.ListProjectByOrgID(uint64(orgID))
	if err != nil {
		return err
	}
	if len(projects) > 0 {
		return errors.Errorf("project is not empty")
	}

	relations, err := o.db.GetOrgClusterRelationsByOrg(orgID)
	if err != nil {
		return err
	}
	if len(relations) > 0 {
		return errors.Errorf("cluster relation exist")
	}

	// 删除企业下的成员及权限
	if err = o.db.DeleteMembersByScope(apistructs.OrgScope, orgID); err != nil {
		return errors.Errorf("failed to delete members, (%v)", err)
	}
	if err = o.db.DeleteMemberExtraByScope(apistructs.OrgScope, orgID); err != nil {
		return errors.Errorf("failed to delete members extra, (%v)", err)
	}

	return o.db.DeleteOrg(orgID)
}

// GetByName 获取企业
func (o *Org) GetByName(orgName string) (*model.Org, error) {
	// 检查org是否存在
	org, err := o.db.GetOrgByName(orgName)
	if err != nil {
		return nil, errors.Wrap(err, "failed to fetch org")
	}
	return org, nil
}

// SearchByName 按企业名称过滤
func (o *Org) SearchByName(name string, pageNo, pageSize int) (int, []model.Org, error) {
	total, orgs, err := o.db.GetOrgsByParam(name, pageNo, pageSize)
	if err != nil {
		logrus.Warnf("failed to get orgs, (%v)", err)
		return 0, nil, err
	}
	return total, orgs, nil
}

// Search public orgs
func (o *Org) SearchPublicOrgsByName(name string, pageNo, pageSize int) (int, []model.Org, error) {
	total, orgs, err := o.db.GetPublicOrgsByParam(name, pageNo, pageSize)
	if err != nil {
		return 0, nil, err
	}
	return total, orgs, nil
}

// ListByIDsAndName 根据IDs列表 & name 获取企业列表
func (o *Org) ListByIDsAndName(orgIDs []int64, name string, pageNo, pageSize int) (int, []model.Org, error) {
	return o.db.GetOrgsByIDsAndName(orgIDs, name, pageNo, pageSize)
}

func (o *Org) ListOrgs(orgIDs []int64, req *apistructs.OrgSearchRequest, all bool) (int, []model.Org, error) {
	var (
		total int
		orgs  []model.Org
		err   error
	)
	if all {
		total, orgs, err = o.SearchByName(req.Q, req.PageNo, req.PageSize)
	} else {
		total, orgs, err = o.ListByIDsAndName(orgIDs, req.Q, req.PageNo, req.PageSize)
	}
	if err != nil {
		logrus.Warnf("failed to get orgs, (%v)", err)
		return 0, nil, err
	}
	return total, orgs, nil
}

// ChangeCurrentOrg 切换用户当前所属企业
func (o *Org) ChangeCurrentOrg(userID string, req *apistructs.OrgChangeRequest) error {
	// 检查用户是否匹配
	if req.UserID != userID {
		return errors.Errorf("user id doesn't match")
	}
	// 检查企业是否存在
	if _, err := o.db.GetOrg(int64(req.OrgID)); err != nil {
		return err
	}

	orgID, err := o.db.GetCurrentOrgByUser(req.UserID)
	if err != nil || orgID == 0 { // 若当前登录用户currentOrg记录不存在
		currentOrg := &model.CurrentOrg{
			UserID: req.UserID,
			OrgID:  int64(req.OrgID),
		}
		return o.db.CreateCurrentOrg(currentOrg)
	}

	return o.db.UpdateCurrentOrg(req.UserID, int64(req.OrgID))
}

// GetCurrentOrgByUser 根据userID获取用户当前关联企业
func (o *Org) GetCurrentOrgByUser(userID string) (int64, error) {
	return o.db.GetCurrentOrgByUser(userID)
}

// List 获取所有企业列表
func (o *Org) List() ([]model.Org, error) {
	return o.db.GetOrgList()
}

// Get Org by domain and org name
func (o *Org) GetOrgByDomainAndOrgName(domain, orgName string) (*model.Org, error) {
	if orgName == "" {
		return o.GetOrgByDomain(domain)
	}
	org, err := o.db.GetOrgByName(orgName)
	if err != nil {
		if err != dao.ErrNotFoundOrg {
			return nil, err
		}
		// Not found, search by domain
		org, err = o.GetOrgByDomain(domain)
		if err != nil {
			if err != dao.ErrNotFoundOrg {
				return nil, err
			}
			return nil, nil
		}
	}
	return org, nil
}

// GetOrgByDomain 通过域名获取企业
func (o *Org) GetOrgByDomain(domain string) (*model.Org, error) {
	if domain != "" && conf.OrgWhiteList[domain] {
		return nil, nil
	}
	for _, rootDomain := range conf.RootDomainList() {
		if orgName := orgNameRetriever(domain, rootDomain); orgName != "" {
			return o.db.GetOrgByName(orgName)
		}
	}
	return nil, apierrors.ErrGetOrg.NotFound()
}

// Search org name in domain
func orgNameRetriever(domain, rootDomain string) string {
	suf := strutil.Concat(".", rootDomain)
	domain_and_port := strutil.Split(domain, ":", true)
	domain = domain_and_port[0]
	if strutil.HasSuffixes(domain, suf) {
		orgName := strutil.TrimSuffixes(domain, suf)
		if strutil.HasSuffixes(orgName, "-org") {
			orgName = strutil.TrimSuffixes(orgName, "-org")
		}
		return orgName
	}
	return ""
}

// RelateCluster 关联集群，创建企业集群关联关系
func (o *Org) RelateCluster(userID string, req *apistructs.OrgClusterRelationCreateRequest) error {
	org, err := o.db.GetOrg(int64(req.OrgID))
	if err != nil {
		return err
	}
	if org.ID == 0 {
		return errors.Errorf("org not found")
	}
	if req.OrgName == "" {
		req.OrgName = org.Name
	} else if org.Name != req.OrgName {
		return errors.Errorf("org info doesn't match")
	}
	cluster, err := o.bdl.GetCluster(req.ClusterName)
	if err != nil {
		return err
	}
	if cluster == nil {
		return errors.Errorf("cluster not found")
	}
	// 若企业集群关系已存在，则返回
	relation, err := o.db.GetOrgClusterRelationByOrgAndCluster(org.ID, int64(cluster.ID))
	if err != nil {
		return err
	}
	if relation != nil {
		return nil
	}

	relation = &model.OrgClusterRelation{
		OrgID:       req.OrgID,
		OrgName:     req.OrgName,
		ClusterID:   uint64(cluster.ID),
		ClusterName: req.ClusterName,
		Creator:     userID,
	}
	return o.db.CreateOrgClusterRelation(relation)
}

// FetchOrgResources 获取企业资源情况
//
// Deprecated: The calculation caliber of the quota was reset on erda 1.3.3, please see FetchOrgClusterResource
func (o *Org) FetchOrgResources(orgID uint64) (*apistructs.OrgResourceInfo, error) {
	relations, err := o.db.GetOrgClusterRelationsByOrg(int64(orgID))
	if err != nil {
		return nil, err
	}

	var (
		totalCpu float64
		totalMem float64
		usedCpu  float64
		usedMem  float64
	)
	for _, v := range relations {
		// 集群维度获取集群资源总量
		clusterRes, err := o.bdl.ResourceInfo(v.ClusterName, true)
		if err != nil {
			return nil, err
		}
		for _, node := range clusterRes.Nodes {
			cpuovercommit := 1.0
			memovercommit := 1.0

			if !node.IgnoreLabels { // dcos & edas 情况下忽略 label 过滤
				if !strutil.Exist(node.Labels, fmt.Sprintf("%s/org-%s", apistructs.PlatformLabelPrefix, v.OrgName)) {
					continue
				}
				if !strutil.Exist(node.Labels, fmt.Sprintf("%s/%s", apistructs.PlatformLabelPrefix, apistructs.StatelessLabel)) &&
					!strutil.Exist(node.Labels, fmt.Sprintf("%s/%s", apistructs.PlatformLabelPrefix, apistructs.StatefulLabel)) {
					continue
				}
				if strutil.Contains(strutil.Concat(node.Labels...), fmt.Sprintf("%s/workspace-dev", apistructs.PlatformLabelPrefix)) {
					if clusterRes.DevCPUOverCommit > cpuovercommit {
						cpuovercommit = clusterRes.DevCPUOverCommit
					}
					if clusterRes.DevMEMOverCommit > memovercommit {
						memovercommit = clusterRes.DevMEMOverCommit
					}
				}
				if strutil.Contains(strutil.Concat(node.Labels...), fmt.Sprintf("%s/workspace-test", apistructs.PlatformLabelPrefix)) {
					if clusterRes.TestCPUOverCommit > cpuovercommit {
						cpuovercommit = clusterRes.TestCPUOverCommit
					}
					if clusterRes.TestMEMOverCommit > memovercommit {
						memovercommit = clusterRes.TestMEMOverCommit
					}
				}
				if strutil.Contains(strutil.Concat(node.Labels...), fmt.Sprintf("%s/workspace-staging", apistructs.PlatformLabelPrefix)) {
					if clusterRes.StagingCPUOverCommit > cpuovercommit {
						cpuovercommit = clusterRes.StagingCPUOverCommit
					}
					if clusterRes.StagingMEMOverCommit > memovercommit {
						memovercommit = clusterRes.StagingMEMOverCommit
					}
				}
				if strutil.Contains(strutil.Concat(node.Labels...), fmt.Sprintf("%s/workspace-prod", apistructs.PlatformLabelPrefix)) {
					if clusterRes.ProdCPUOverCommit > cpuovercommit {
						cpuovercommit = clusterRes.ProdCPUOverCommit
					}
					if clusterRes.ProdMEMOverCommit > memovercommit {
						memovercommit = clusterRes.ProdMEMOverCommit
					}
				}
			}
			totalCpu += node.CPUAllocatable * cpuovercommit
			totalMem += float64(node.MemAllocatable) * memovercommit / apistructs.GB
		}
	}

	// 获取当前企业已分配资源
	projects, err := o.db.ListProjectByOrgID(orgID)
	if err != nil {
		return nil, err
	}
	for _, v := range projects {
		usedCpu += v.CpuQuota
		usedMem += v.MemQuota
	}

	return &apistructs.OrgResourceInfo{
		TotalCpu:     numeral.Round(totalCpu, 2),
		TotalMem:     numeral.Round(totalMem, 2),
		AvailableCpu: numeral.Round(totalCpu-usedCpu, 2),
		AvailableMem: numeral.Round(totalMem-usedMem, 2),
	}, nil
}

// ListAllOrgClusterRelation 获取所有企业对应集群关系
func (o *Org) ListAllOrgClusterRelation() ([]model.OrgClusterRelation, error) {
	return o.db.ListAllOrgClusterRelations()
}

// GetOrgClusterRelationsByOrg returns the list of clusters in the organization
func (o *Org) GetOrgClusterRelationsByOrg(orgID uint64) ([]model.OrgClusterRelation, error) {
	return o.db.GetOrgClusterRelationsByOrg(int64(orgID))
}

func (o *Org) checkReceiveTaskRuntimeEventParam(req *apistructs.PipelineTaskEvent) error {
	if req.OrgID == "" {
		return errors.Errorf("OrgID is empty")
	}

	if req.Content.RuntimeID == "" {
		return errors.Errorf("RuntimeID is empty")
	}

	if req.Content.PipelineTaskID == 0 {
		return errors.Errorf("pipelineTaskID is empty")
	}

	return nil
}

// SetReleaseCrossCluster 设置企业是否允许跨集群部署开关
func (o *Org) SetReleaseCrossCluster(orgID uint64, enable bool) error {
	// 检查待更新的org是否存在
	org, err := o.db.GetOrg(int64(orgID))
	if err != nil {
		return apierrors.ErrSetReleaseCrossCluster.InvalidParameter(err)
	}
	org.Config.EnableReleaseCrossCluster = enable
	return o.db.DB.Model(&model.Org{}).Update(org).Error
}

// GenVerifiCode 生成邀请成员加入企业的验证码
func (o *Org) GenVerifiCode(identityInfo apistructs.IdentityInfo, orgID uint64) (string, error) {
	now := time.Now()
	key := apistructs.OrgInviteCodeRedisKey.GetKey(now.Day(), identityInfo.UserID, strconv.FormatUint(orgID, 10))
	code, err := o.redisCli.Get(key).Result()
	if err == redis.Nil {
		newCode := strutil.RandStr(5) + apistructs.CodeUserID(identityInfo.UserID)
		tommory := now.AddDate(0, 0, 1).Format("2006-01-02") + " 01:00:00"
		tommoryTime, _ := time.ParseInLocation("2006-01-02 15:04:05", tommory, time.Local)
		_, err := o.redisCli.Set(key, newCode, tommoryTime.Sub(now)).Result()
		if err != nil {
			return "", err
		}

		return newCode, nil
	} else if err != nil {
		return "", err
	}

	return code, nil
}

// SetNotifyConfig 设置通知配置
func (o *Org) SetNotifyConfig(orgID int64, notifyConfig apistructs.NotifyConfigUpdateRequestBody) error {
	org, err := o.db.GetOrg(orgID)
	if err != nil {
		return err
	}

	// 目前只开放配置语音短信通知的开关，之后会开发语音短信通知的其他配置
	org.Config.EnableMS = notifyConfig.Config.EnableMS

	return o.db.UpdateOrg(&org)
}

// GetNotifyConfig 获取通知配置
func (o *Org) GetNotifyConfig(orgID int64) (*apistructs.OrgConfig, error) {
	org, err := o.db.GetOrg(orgID)
	if err != nil {
		return nil, err
	}

	return &apistructs.OrgConfig{
		EnableMS: org.Config.EnableMS,
		// SMTPHost:                   org.Config.SMTPHost,
		// SMTPUser:                   org.Config.SMTPUser,
		// SMTPPassword:               org.Config.SMTPPassword,
		// SMTPPort:                   org.Config.SMTPPort,
		// SMTPIsSSL:                  org.Config.SMTPIsSSL,
		// SMSKeyID:                   org.Config.SMSKeyID,
		// SMSKeySecret:               org.Config.SMSKeySecret,
		// SMSSignName:                org.Config.SMSSignName,
		// SMSMonitorTemplateCode:     org.Config.SMSMonitorTemplateCode, // 监控单独的短信模版
		// VMSKeyID:                   org.Config.VMSKeyID,
		// VMSKeySecret:               org.Config.VMSKeySecret,
		// VMSMonitorTtsCode:          org.Config.VMSMonitorTtsCode, // 监控单独的语音模版
		// VMSMonitorCalledShowNumber: org.Config.VMSMonitorCalledShowNumber,
	}, nil
}

// DereferenceCluster 解除关联集群关系
func (o *Org) DereferenceCluster(userID string, req *apistructs.DereferenceClusterRequest) error {
	clusterInfo, err := o.bdl.GetCluster(req.Cluster)
	if err != nil {
		return err
	}
	if clusterInfo == nil {
		return errors.Errorf("不存在的集群%s", req.Cluster)
	}
	if err := o.db.DeleteOrgClusterRelationByClusterAndOrg(req.Cluster, req.OrgID); err != nil {
		return err
	}

	return nil
}
