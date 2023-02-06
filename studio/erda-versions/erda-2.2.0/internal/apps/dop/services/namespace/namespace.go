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

package namespace

import (
	"encoding/json"
	"regexp"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
	"github.com/erda-project/erda/internal/apps/dop/types"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	DynamicNamespaceMaxLength = 32
	StaticNamespaceMaxLength  = 255
	NamespaceFormat           = "^[a-zA-Z0-9\\-]+$"
	NotDeleteValue            = "N"
)

// Namespace 命名空间参数
type Namespace struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

// Option 定义 Namespace 对象的配置选项
type Option func(*Namespace)

// New 新建 Namespace 实例
func New(options ...Option) *Namespace {
	o := &Namespace{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(o *Namespace) {
		o.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(c *Namespace) {
		c.bdl = bdl
	}
}

func (n *Namespace) GenerateAppExtraInfo(applicationID, projectID int64) string {
	workspaces := []apistructs.DiceWorkspace{
		types.DefaultWorkspace,
		types.DevWorkspace,
		types.TestWorkspace,
		types.StagingWorkspace,
		types.ProdWorkspace,
	}

	relatedNamespaces := make([]string, 0, len(workspaces)-1)
	var defaultNamespace string
	extra := make(map[string]string, len(workspaces))
	for _, v := range workspaces {
		key := strutil.Concat(string(v), ".configNamespace")
		value := strutil.Concat("app-", strconv.FormatInt(applicationID, 10), "-", string(v))
		extra[key] = value

		if v == types.DefaultWorkspace {
			defaultNamespace = value
		} else {
			relatedNamespaces = append(relatedNamespaces, value)
		}
	}

	// 创建 DEV/TEST/STAGING/PROD/DEFAULT namespace
	for _, v := range extra {
		namespaceCreateReq := apistructs.NamespaceCreateRequest{
			ProjectID: projectID,
			Dynamic:   true,
			Name:      v,
			IsDefault: strings.Contains(v, string(types.DefaultWorkspace)),
		}
		// 创建namespace
		_, err := n.Create(&namespaceCreateReq)
		if err != nil {
			logrus.Errorf(err.Error())
		}
	}

	// 创建 namespace relations
	relationCreateReq := apistructs.NamespaceRelationCreateRequest{
		RelatedNamespaces: relatedNamespaces,
		DefaultNamespace:  defaultNamespace,
	}

	if err := n.CreateRelation(&relationCreateReq); err != nil {
		logrus.Errorf(err.Error())
	}

	extraInfo, err := json.Marshal(extra)
	if err != nil {
		logrus.Errorf("failed to marshal extra info, (%v)", err)
	}
	return string(extraInfo)
}

// Create 创建 namespace
func (n *Namespace) Create(createReq *apistructs.NamespaceCreateRequest) (int64, error) {
	// check params
	if createReq.Name == "" {
		return 0, errors.New("invalid request, name is empty")
	}
	if createReq.ProjectID == 0 {
		return 0, errors.New("invalid request, projectID is empty")
	}

	// check namespace if already exist
	ns, err := n.db.GetNamespaceByName(createReq.Name)
	if err != nil {
		return 0, err
	}

	if ns != nil {
		logrus.Infof("namespace already exist")
		return ns.ID, nil
	}

	// check namespace length
	if createReq.Dynamic {
		if len(createReq.Name) > DynamicNamespaceMaxLength {
			return 0, errors.Errorf("namespace too long, namaspace: %s", createReq.Name)
		}
	} else {
		if len(createReq.Name) > StaticNamespaceMaxLength {
			return 0, errors.Errorf("namespace too long, namaspace: %s", createReq.Name)
		}
	}

	// check namespace format
	m, err := regexp.MatchString(NamespaceFormat, createReq.Name)
	if err != nil {
		return 0, errors.Errorf("failed to match namespace, namaspace: %s, parten: %s, (%+v)",
			createReq.Name, NamespaceFormat, err)
	}

	if !m {
		return 0, errors.Errorf("illegal namespace, namaspace: %s", createReq.Name)
	}

	// create namespace
	configInfo := &model.ConfigNamespace{
		Name:      createReq.Name,
		Dynamic:   createReq.Dynamic,
		ProjectID: strconv.FormatInt(createReq.ProjectID, 10),
		IsDefault: createReq.IsDefault,
		IsDeleted: NotDeleteValue,
	}

	err = n.db.UpdateOrAddNamespace(configInfo)
	if err != nil {
		return 0, err
	}

	return configInfo.ID, nil
}

// DeleteNamespace 删除 namespace
func (n *Namespace) DeleteNamespace(permission *permission.Permission, name string, identityInfo apistructs.IdentityInfo) error {
	// check namespace if exist
	ns, err := n.db.GetNamespaceByName(name)
	if err != nil {
		return err
	}

	if ns == nil {
		return errors.New("namespace not exist")
	}

	// 操作鉴权
	if !identityInfo.IsInternalClient() {
		appID, err := strconv.ParseUint(ns.ApplicationID, 10, 64)
		if err != nil {
			return err
		}
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  appID,
			Resource: apistructs.AppResource,
			Action:   apistructs.DeleteAction,
		}

		if access, err := n.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrDeleteProject.AccessDenied()
		}
	}

	// delete namespace and configs
	configItems, err := n.db.GetEnvConfigsByNamespaceID(ns.ID)
	if err != nil {
		return err
	}

	for _, config := range configItems {
		err = n.db.SoftDeleteEnvConfig(&config)
		if err != nil {
			return err
		}
	}

	// soft delete namespace
	err = n.db.SoftDeleteNamespace(ns)
	return err
}

// CreateRelation 创建 namespace 关联关系
func (n *Namespace) CreateRelation(createReq *apistructs.NamespaceRelationCreateRequest) error {
	// check params
	if createReq.DefaultNamespace == "" {
		return errors.New("invalid request, defaultNamespace is empty")
	}
	if createReq.RelatedNamespaces == nil || len(createReq.RelatedNamespaces) != 4 {
		return errors.New("invalid request, relationNamespace is invalid")
	}

	nss, err := n.db.GetNamespacesByNames(createReq.RelatedNamespaces)
	if err != nil {
		return err
	}

	if nss == nil || len(nss) != 4 {
		return errors.New("not exist namespace")
	}

	for _, ns := range nss {
		if ns.IsDefault {
			return errors.New("namespace attribute not match")
		}
	}

	defaultNs, err := n.db.GetNamespaceByName(createReq.DefaultNamespace)
	if err != nil {
		return err
	}

	if defaultNs == nil {
		return errors.New("not exist default namespace")
	}

	if !defaultNs.IsDefault {
		return errors.New("default namespace attribute not match")
	}

	for _, ns := range createReq.RelatedNamespaces {
		nsRelation, err := n.db.GetNamespaceRelationByName(ns)
		if err != nil {
			return errors.Wrapf(err, "failed to get namespace relation by name")
		}

		if nsRelation != nil {
			continue
		}

		// create namespace relation
		relationInfo := &model.ConfigNamespaceRelation{
			Namespace:        ns,
			DefaultNamespace: createReq.DefaultNamespace,
			IsDeleted:        NotDeleteValue,
		}

		err = n.db.UpdateOrAddNamespaceRelation(relationInfo)
		if err != nil {
			return err
		}

	}

	return nil
}

// DeleteRelation 删除 namespace 关联关系
func (n *Namespace) DeleteRelation(permission *permission.Permission, locale *i18n.LocaleResource, name, userID string) error {
	// check namespace if exist
	ns, err := n.db.GetNamespaceByName(name)
	if err != nil {
		return err
	}

	if ns == nil {
		return errors.New("namespace not exist")
	}

	// 操作鉴权
	appID, err := strconv.ParseUint(ns.ApplicationID, 10, 64)
	if err != nil {
		return err
	}
	req := apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: apistructs.AppResource,
		Action:   apistructs.DeleteAction,
	}
	if access, err := n.bdl.CheckPermission(&req); err != nil || !access.Access {
		return apierrors.ErrDeleteProject.AccessDenied()
	}

	// check namespace relation if exist
	nsRelations, err := n.db.GetNamespaceRelationsByDefaultName(name)
	if err != nil {
		return err
	}

	if nsRelations == nil {
		return errors.New("not exist namespace relation")
	}

	for _, nsRelation := range nsRelations {
		err = n.db.SoftDeleteNamespaceRelation(&nsRelation)
		if err != nil {
			return err
		}
	}

	return nil
}

// FixDataErr 修复命名空间不全
func (n *Namespace) FixDataErr(namespaceName, projectID string) error {
	// check namespace format
	m, err := regexp.MatchString(NamespaceFormat, namespaceName)
	if err != nil {
		return errors.Errorf("failed to match namespace, namaspace: %s, parten: %s, (%+v)",
			namespaceName, NamespaceFormat, err)
	}

	if !m {
		return errors.Errorf("illegal namespace, namaspace: %s", namespaceName)
	}

	nsSlice := strings.SplitN(namespaceName, "-", -1)
	if len(nsSlice) != 3 {
		return errors.Errorf("illegal namespace, namaspace: %s", namespaceName)
	}

	prefix := nsSlice[0] + "-" + nsSlice[1] + "-"
	nss, err := n.db.ListNamespaceByAppID(prefix)
	if err != nil {
		return err
	}

	if len(nss) == 0 {
		return errors.Errorf("can't find any namespace like %v", nsSlice[1])
	}

	nsMap := map[string]int{prefix + "STAGING": 1, prefix + "DEV": 1, prefix + "TEST": 1, prefix + "PROD": 1, prefix + "DEFAULT": 2}
	for _, v := range nss {
		if _, ok := nsMap[v.Name]; ok {
			delete(nsMap, v.Name)
		}
	}

	for k, v := range nsMap {
		var isDefautl bool
		if v == 2 {
			isDefautl = true
		}
		// create namespace
		configInfo := &model.ConfigNamespace{
			Name:      k,
			Dynamic:   true,
			ProjectID: projectID,
			IsDefault: isDefautl,
			IsDeleted: NotDeleteValue,
		}

		err = n.db.UpdateOrAddNamespace(configInfo)
		if err != nil {
			return err
		}
	}

	return nil
}
