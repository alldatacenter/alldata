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

package libreference

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// LibReference 库引用 service
type LibReference struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

// Option 库引用选项
type Option func(*LibReference)

// New 初始化库引用 service
func New(options ...Option) *LibReference {
	lr := &LibReference{}
	for _, op := range options {
		op(lr)
	}
	return lr
}

// WithDBClient 设置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(lr *LibReference) {
		lr.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(lr *LibReference) {
		lr.bdl = bdl
	}
}

// Create 创建库引用
func (l *LibReference) Create(createReq *apistructs.LibReferenceCreateRequest) (uint64, error) {
	// 参数校验
	if createReq.AppID == 0 {
		return 0, apierrors.ErrCreateLibReference.MissingParameter("appID")
	}
	if createReq.LibID == 0 {
		return 0, apierrors.ErrCreateLibReference.MissingParameter("libID")
	}
	if createReq.LibName == "" {
		return 0, apierrors.ErrCreateLibReference.MissingParameter("libName")
	}

	if !createReq.IsInternalClient() {
		// Authorize
		access, err := l.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   createReq.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  createReq.AppID,
			Resource: apistructs.LibReferenceResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrCreateLibReference.AccessDenied()
		}
	}

	// 检查库引用是否已存在
	listReq := &apistructs.LibReferenceListRequest{
		AppID:    createReq.AppID,
		LibID:    createReq.LibID,
		PageNo:   1,
		PageSize: 1,
	}
	_, libReferences, err := l.db.ListLibReference(listReq)
	if err != nil {
		return 0, apierrors.ErrCreateLibReference.InternalError(err)
	}
	if len(libReferences) > 0 {
		return 0, apierrors.ErrCreateLibReference.AlreadyExists()
	}

	// 创建审批流
	approvalCreateReq := apistructs.ApproveCreateRequest{
		Title:    fmt.Sprintf("应用 %s 关联库 %s 申请", createReq.AppName, createReq.LibName),
		Type:     apistructs.ApproveLibReference,
		Priority: "middle",
		TargetID: createReq.AppID,
		EntityID: createReq.LibID,
		OrgID:    createReq.OrgID,
		Desc:     createReq.LibDesc,
	}

	newApproval, err := l.bdl.CreateApprove(&approvalCreateReq, createReq.UserID)
	if err != nil {
		return 0, err
	}

	// 创建库引用
	libReference := &dao.LibReference{
		AppID:          createReq.AppID,
		LibID:          createReq.LibID,
		LibName:        createReq.LibName,
		LibDesc:        createReq.LibDesc,
		ApprovalID:     newApproval.ID,
		ApprovalStatus: apistructs.ApprovalStatusPending,
		Creator:        createReq.UserID,
	}
	if err := l.db.CreateLibReference(libReference); err != nil {
		return 0, err
	}

	return uint64(libReference.ID), nil
}

// UpdateApprovalStatus 更新库引用审批状态
func (l *LibReference) UpdateApprovalStatus(approvalID uint64, approvalStatus apistructs.ApprovalStatus) error {
	return l.db.UpdateApprovalStatusByApprovalID(approvalID, approvalStatus)
}

// Delete 删除库引用
func (l *LibReference) Delete(identityInfo apistructs.IdentityInfo, libReferenceID uint64) error {
	// 检查库引用是否存在
	libReference, err := l.db.GetLibReference(libReferenceID)
	if err != nil {
		return err
	}
	if libReference == nil {
		return apierrors.ErrDeleteLibReference.NotFound()
	}

	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := l.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  libReference.AppID,
			Resource: apistructs.LibReferenceResource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrDeleteLibReference.AccessDenied()
		}
	}

	// 待审批库引用不能删除
	if libReference.ApprovalStatus == apistructs.ApprovalStatusPending {
		return apierrors.ErrDeleteLibReference.InvalidState("approvalStatus")
	}

	return l.db.DeleteLibReference(libReferenceID)
}

// List 库引用列表
func (l *LibReference) List(listReq *apistructs.LibReferenceListRequest) (*apistructs.LibReferenceListResponseData, error) {
	if listReq.PageNo == 0 {
		listReq.PageNo = 1
	}
	if listReq.PageSize == 0 {
		listReq.PageSize = 20
	}

	if !listReq.IsInternalClient() {
		// Authorize
		access, err := l.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   listReq.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  listReq.AppID,
			Resource: apistructs.LibReferenceResource,
			Action:   apistructs.ListAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrListLibReference.AccessDenied()
		}
	}

	total, lrs, err := l.db.ListLibReference(listReq)
	if err != nil {
		return nil, err
	}
	libReferences := make([]apistructs.LibReference, 0, len(lrs))
	for _, lr := range lrs {
		libReferences = append(libReferences, *l.Convert(&lr))
	}

	return &apistructs.LibReferenceListResponseData{
		Total: total,
		List:  libReferences,
	}, nil
}

// Convert 库引用数据结构转换
func (l *LibReference) Convert(libReference *dao.LibReference) *apistructs.LibReference {
	return &apistructs.LibReference{
		ID:             uint64(libReference.ID),
		AppID:          libReference.AppID,
		LibID:          libReference.LibID,
		LibName:        libReference.LibName,
		LibDesc:        libReference.LibDesc,
		ApprovalID:     libReference.ApprovalID,
		ApprovalStatus: libReference.ApprovalStatus,
		Creator:        libReference.Creator,
		CreatedAt:      &libReference.CreatedAt,
		UpdatedAt:      &libReference.UpdatedAt,
	}
}
