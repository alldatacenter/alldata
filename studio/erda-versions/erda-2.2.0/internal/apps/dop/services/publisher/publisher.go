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

// Package publisher 封装Publisher资源相关操作
package publisher

import (
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	uuid2 "github.com/satori/go.uuid"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/apps/dop/services/nexussvc"
	"github.com/erda-project/erda/pkg/nexus"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Publisher 资源对象操作封装
type Publisher struct {
	db       *dao.DBClient
	uc       *ucauth.UCClient
	bdl      *bundle.Bundle
	nexusSvc *nexussvc.NexusSvc
}

// Option 定义 Publisher 对象的配置选项
type Option func(*Publisher)

// New 新建 Publisher 实例，通过 Publisher 实例操作企业资源
func New(options ...Option) *Publisher {
	p := &Publisher{}
	for _, op := range options {
		op(p)
	}
	return p
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(p *Publisher) {
		p.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(p *Publisher) {
		p.uc = uc
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(p *Publisher) {
		p.bdl = bdl
	}
}

func WithNexusSvc(svc *nexussvc.NexusSvc) Option {
	return func(p *Publisher) {
		p.nexusSvc = svc
	}
}

// Create 创建Publisher
func (p *Publisher) Create(userID string, createReq *apistructs.PublisherCreateRequest) (int64, error) {
	// 参数合法性检查
	if createReq.Name == "" {
		return 0, errors.Errorf("failed to create publisher(name is empty)")
	}
	if createReq.PublisherType == "" {
		return 0, errors.Errorf("failed to create publisher(publisherType is empty)")
	}
	if createReq.OrgID == 0 {
		return 0, errors.Errorf("failed to create publisher(org id is empty)")
	}

	publisher, err := p.db.GetPublisherByOrgAndName(int64(createReq.OrgID), createReq.Name)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return 0, err
		}
	}
	if publisher != nil {
		return 0, errors.Errorf("failed to create publisher(name already exists)")
	}

	// 企业只能创建一个
	count, err := p.db.GetOrgPublishersCount(createReq.OrgID)
	if err != nil {
		return 0, errors.Errorf("failed to get org publisher numbers, (%+v)", err)
	}

	if count > 0 {
		return 0, errors.New("one org limit one publisher")
	}

	// 添加Publisher至DB
	publisher = &model.Publisher{
		Name:          createReq.Name,
		Desc:          createReq.Desc,
		Logo:          createReq.Logo,
		OrgID:         int64(createReq.OrgID),
		UserID:        userID,
		PublisherType: createReq.PublisherType,
		PublisherKey:  GeneratePublisherKey(),
	}
	if err = p.db.CreatePublisher(publisher); err != nil {
		return 0, errors.Errorf("failed to insert publisher to db, err:%+v", err)
	}

	managerUsers, err := p.bdl.ListScopeManagersByScopeID(apistructs.ListScopeManagersByScopeIDRequest{
		ScopeType: apistructs.OrgScope,
		ScopeID:   int64(createReq.OrgID),
	})
	if err != nil {
		return 0, errors.Errorf("failed to get org managers, (%+v)", err)
	}
	managerIDs := []string{userID}
	for _, user := range managerUsers {
		managerIDs = append(managerIDs, user.UserID)
	}

	// 保证 nexus ${repoFormat}-hosted-repo 存在
	if err = p.ensureNexusHostedRepo(publisher); err != nil {
		return 0, err
	}

	return int64(publisher.ID), nil
}

// Update 更新Publisher
func (p *Publisher) Update(updateReq *apistructs.PublisherUpdateRequest) error {
	// 检查待更新的publisher是否存在
	publisher, err := p.db.GetPublisherByID(int64(updateReq.ID))
	if err != nil {
		return errors.Wrap(err, "failed to update publisher")
	}

	publisher.Desc = updateReq.Desc
	publisher.Logo = updateReq.Logo
	if err = p.db.UpdatePublisher(&publisher); err != nil {
		logrus.Warnf("failed to update publisher, (%v)", err)
		return errors.Errorf("failed to update publisher")
	}

	// 保证 nexus ${repoFormat}-hosted-repo 存在
	go func() {
		if err = p.ensureNexusHostedRepo(&publisher); err != nil {
			logrus.Errorf("Update publisher %v ensureNexusHostedRepo error %v", publisher, err)
		}
	}()

	return nil
}

// Delete 删除Publisher
func (p *Publisher) Delete(publisherID, orgID int64) error {
	// 检查Publisher下是否有内容，没有才可删除
	publishItemReq := &apistructs.QueryPublishItemRequest{
		PageSize:    0,
		PageNo:      0,
		PublisherId: publisherID,
		OrgID:       orgID,
	}

	if publishItemInfo, err := p.bdl.QueryPublishItems(publishItemReq); err != nil || publishItemInfo.Total > 0 {
		return errors.Errorf("failed to delete publisher(there exists publish items)")
	}

	if err := p.db.DeletePublisher(publisherID); err != nil {
		return errors.Errorf("failed to delete publisher, (%v)", err)
	}
	logrus.Infof("deleted publisher %d success", publisherID)

	return nil
}

// Get 获取Publisher
func (p *Publisher) Get(publisherID int64) (*apistructs.PublisherDTO, error) {
	publisher, err := p.db.GetPublisherByID(publisherID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to get publisher")
	}
	return p.convertToPublisherDTO(true, &publisher)
}

// ListAllPublishers 企业管理员可查看当前企业下所有Publisher，包括未加入的Publisher
func (p *Publisher) ListAllPublishers(userID string, params *apistructs.PublisherListRequest) (
	*apistructs.PagingPublisherDTO, error) {
	total, publishers, err := p.db.GetPublishersByOrgIDAndName(int64(params.OrgID), params)
	if err != nil {
		return nil, errors.Errorf("failed to get publishers, (%v)", err)
	}
	// 转换成所需格式
	publisherDTOs := make([]apistructs.PublisherDTO, 0, len(publishers))
	for i := range publishers {
		// 企业成员都是发布商成员
		publisherDTO, err := p.convertToPublisherDTO(true, &publishers[i])
		if err != nil {
			return nil, err
		}
		publisherDTOs = append(publisherDTOs, *publisherDTO)
	}

	return &apistructs.PagingPublisherDTO{Total: total, List: publisherDTOs}, nil
}

// ListJoinedPublishers 返回用户有权限的Publisher
func (p *Publisher) ListJoinedPublishers(userID string, params *apistructs.PublisherListRequest) (
	*apistructs.PagingPublisherDTO, error) {
	// 获取Publisher列表 publisher和org同级
	total, publishers, err := p.db.GetPublishersByOrgIDAndName(int64(params.OrgID), params)
	if err != nil {
		return nil, errors.Errorf("failed to get publishers, (%v)", err)
	}

	// 转换成所需格式
	publisherDTOs := make([]apistructs.PublisherDTO, 0, len(publishers))
	for i := range publishers {
		publisherDTO, err := p.convertToPublisherDTO(params.Joined, &publishers[i])
		if err != nil {
			return nil, err
		}
		publisherDTOs = append(publisherDTOs, *publisherDTO)
	}

	return &apistructs.PagingPublisherDTO{Total: total, List: publisherDTOs}, nil
}

func (p *Publisher) convertToPublisherDTO(joined bool, publisher *model.Publisher) (*apistructs.PublisherDTO, error) {
	// nexus repositories
	nexusRepos, err := p.nexusSvc.ListRepositories(apistructs.NexusRepositoryListRequest{
		PublisherID: &[]uint64{publisher.ID}[0],
		OrgID:       &[]uint64{uint64(publisher.OrgID)}[0],
	})
	if err != nil {
		return nil, err
	}

	// pipeline cm ns
	namespaces := []string{nexus.MakePublisherPipelineCmNs(uint64(publisher.ID))}

	return &apistructs.PublisherDTO{
		ID:                   uint64(publisher.ID),
		Name:                 publisher.Name,
		PublisherType:        publisher.PublisherType,
		PublisherKey:         publisher.PublisherKey,
		OrgID:                uint64(publisher.OrgID),
		Creator:              publisher.UserID,
		Logo:                 publisher.Logo,
		Desc:                 publisher.Desc,
		Joined:               joined,
		CreatedAt:            publisher.CreatedAt,
		UpdatedAt:            publisher.UpdatedAt,
		NexusRepositories:    nexusRepos,
		PipelineCmNamespaces: namespaces,
	}, nil
}

// GeneratePublisherKey 生成publisherKey
func GeneratePublisherKey() string {
	return strings.Replace(uuid2.NewV4().String(), "-", "", -1)
}
