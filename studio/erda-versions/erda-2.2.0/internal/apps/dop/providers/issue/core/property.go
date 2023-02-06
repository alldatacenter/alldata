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

package core

import (
	"context"
	"strconv"
	"strings"

	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func (i *IssueService) CreateIssueProperty(ctx context.Context, req *pb.CreateIssuePropertyRequest) (*pb.CreateIssuePropertyResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssuePropertyValue.NotLogin()
	}
	req.IdentityInfo = identityInfo

	properties, err := i.db.GetIssueProperties(pb.GetIssuePropertyRequest{
		OrgID:             req.OrgID,
		PropertyIssueType: req.PropertyIssueType.String(),
	})
	var startIndex int64 = 0
	for _, v := range properties {
		if v.Index >= startIndex {
			startIndex = v.Index + 1
		}
	}

	if err != nil {
		return nil, err
	}
	issueProperty := &dao.IssueProperty{
		ScopeID:           req.ScopeID,
		ScopeType:         req.ScopeType.String(),
		OrgID:             req.OrgID,
		PropertyName:      req.PropertyName,
		DisplayName:       req.PropertyName, // 两个name相同
		PropertyType:      req.PropertyType.String(),
		Required:          req.Required,
		PropertyIssueType: req.PropertyIssueType.String(),
		Relation:          req.Relation,
		Index:             startIndex,
	}
	// 校验字段属性
	if issueProperty.PropertyType == "" {
		return nil, apierrors.ErrCreateIssueProperty.MissingParameter("PropertyType")
	}
	if issueProperty.PropertyIssueType != pb.PropertyIssueTypeEnum_COMMON.String() && issueProperty.Relation <= 0 {
		return nil, apierrors.ErrCreateIssueProperty.MissingParameter("relation")
	}
	if issueProperty.PropertyName == "" {
		return nil, apierrors.ErrCreateIssueProperty.MissingParameter("PropertyName")
	}
	if len(req.PropertyName) > 100 {
		return nil, apierrors.ErrCreateIssueProperty.InvalidParameter("PropertyName is longer than 100")
	}
	// 重名检测
	propertyName, err := i.GetByName(req.OrgID, req.PropertyName, req.PropertyIssueType.String())
	if err != nil {
		return nil, apierrors.ErrCreateIssueProperty.InternalError(err)
	}
	if propertyName != nil {
		return nil, apierrors.ErrCreateIssueProperty.AlreadyExists()
	}
	// 校验字段枚举值属性
	var value []dao.IssuePropertyValue
	for _, val := range req.EnumeratedValues {
		if len(val.Name) > 100 {
			err = apierrors.ErrCreateIssueProperty.InvalidParameter("EnumeratedName is longer than 100")
			return nil, err
		}
		value = append(value, dao.IssuePropertyValue{
			PropertyID: int64(issueProperty.ID),
			Name:       val.Name,
		})
	}
	// 添加字段数据
	if err := i.db.CreateIssueProperty(issueProperty); err != nil {
		return nil, err
	}
	propertyIndex := &pb.IssuePropertyIndex{
		PropertyID:        int64(issueProperty.ID),
		ScopeID:           issueProperty.ScopeID,
		ScopeType:         req.ScopeType,
		OrgID:             issueProperty.OrgID,
		PropertyName:      issueProperty.PropertyName,
		DisplayName:       issueProperty.DisplayName,
		PropertyType:      req.PropertyType,
		Required:          issueProperty.Required,
		PropertyIssueType: req.PropertyIssueType,
		Relation:          issueProperty.Relation,
		Index:             issueProperty.Index,
	}
	// 如果不是创建公有字段,或者公有字段的类型不是单选、多选 则不需要向数据库添加枚举值
	if issueProperty.PropertyIssueType != pb.PropertyIssueTypeEnum_COMMON.String() {
		return &pb.CreateIssuePropertyResponse{Data: propertyIndex}, nil
	} else if common.IsOptions(issueProperty.PropertyIssueType) == false {
		return &pb.CreateIssuePropertyResponse{Data: propertyIndex}, nil
	}
	// 添加字段枚举值数据
	for i := range value {
		value[i].PropertyID = int64(issueProperty.ID)
	}
	if err = i.db.CreateIssuePropertyValues(value); err != nil {
		return nil, err
	}
	for i, v := range value {
		propertyIndex.EnumeratedValues = append(propertyIndex.EnumeratedValues, &pb.Enumerate{
			Id:    int64(v.ID),
			Name:  v.Name,
			Index: int64(i),
		})
	}
	return &pb.CreateIssuePropertyResponse{Data: propertyIndex}, nil
}

func (i *IssueService) DeleteIssueProperty(ctx context.Context, req *pb.DeleteIssuePropertyRequest) (*pb.DeleteIssuePropertyResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrDeleteIssuePropertyValue.NotLogin()
	}
	req.IdentityInfo = identityInfo

	property, err := i.db.GetIssuePropertyByID(req.PropertyID)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, apierrors.ErrGetIssueProperty.NotFound()
		}
		return nil, apierrors.ErrGetIssueProperty.InternalError(err)
	}
	p := query.Convert(property)

	// 如果该字段被关联，不能删除
	property, err = i.GetByRelation(p.PropertyID)
	if err != nil {
		return nil, apierrors.ErrDeleteIssueProperty.InternalError(err)
	}
	if property != nil {
		return nil, apierrors.ErrDeleteIssueProperty.InvalidParameter("该字段已被其他字段引用")
	}

	// 删除用了该字段的全部事件实例
	if err := i.db.DeletePropertyRelationsByPropertyID(-1, p.PropertyID); err != nil {
		return nil, err
	}
	// 删除事项字段的枚举值
	if err := i.db.DeleteIssuePropertyValuesByPropertyID(p.PropertyID); err != nil {
		return nil, err
	}
	// 删除事项字段
	if err := i.db.DeleteIssueProperty(p.OrgID, p.PropertyIssueType.String(), req.PropertyID, p.Index); err != nil {
		return nil, err
	}
	return &pb.DeleteIssuePropertyResponse{Data: p}, nil
}

func (i *IssueService) UpdateIssueProperty(ctx context.Context, req *pb.UpdateIssuePropertyRequest) (*pb.UpdateIssuePropertyResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssuePropertyValue.NotLogin()
	}
	req.IdentityInfo = identityInfo

	oldIssueProperty, err := i.db.GetIssuePropertyByID(req.PropertyID)
	if err != nil {
		return nil, err
	}
	// 限定可修改的字段
	issueProperty := &dao.IssueProperty{
		BaseModel: dbengine.BaseModel{
			ID: oldIssueProperty.ID,
		},
		ScopeID:           oldIssueProperty.ScopeID,
		ScopeType:         oldIssueProperty.ScopeType,
		OrgID:             oldIssueProperty.OrgID,
		PropertyName:      req.PropertyName,
		DisplayName:       req.DisplayName,
		PropertyType:      req.PropertyType.String(),
		Required:          req.Required,
		PropertyIssueType: oldIssueProperty.PropertyIssueType,
		Index:             oldIssueProperty.Index,
		Relation:          oldIssueProperty.Relation,
	}
	// 字段类型转换的校验
	if IsCanChange(oldIssueProperty.PropertyType, issueProperty.PropertyType) != true {
		return nil, apierrors.ErrUpdateIssueProperty.InvalidParameter("非法的PropertyType改变")
	}
	// 重名检测
	propertyName, err := i.GetByName(req.OrgID, req.PropertyName, req.PropertyIssueType.String())
	if err != nil {
		return nil, apierrors.ErrUpdateIssueProperty.InternalError(err)
	}
	if propertyName != nil && propertyName.ID != issueProperty.ID {
		return nil, apierrors.ErrUpdateIssueProperty.AlreadyExists()
	}
	// 如果字段已经被使用，不能修改
	propertyRelated, err := i.GetByRelation(req.PropertyID)
	if err != nil {
		return nil, apierrors.ErrUpdateIssueProperty.InternalError(err)
	}
	if propertyRelated != nil {
		return nil, apierrors.ErrUpdateIssueProperty.InvalidParameter("被引用的字段禁止修改")
	}
	// 更新字段
	if err := i.db.UpdateIssueProperty(issueProperty); err != nil {
		return nil, err
	}
	// 删除该字段原有枚举值
	if err := i.db.DeleteIssuePropertyValuesByPropertyID(req.PropertyID); err != nil {
		return nil, err
	}
	var values []dao.IssuePropertyValue
	for _, v := range req.EnumeratedValues {
		values = append(values, dao.IssuePropertyValue{
			PropertyID: req.PropertyID,
			Name:       v.Name,
		})
	}
	// 如果是单选或者多选,添加该字段新枚举值
	if common.IsOptions(issueProperty.PropertyType) {
		if err := i.db.CreateIssuePropertyValues(values); err != nil {
			return nil, err
		}
	}
	response := query.Convert(issueProperty)
	for i, v := range values {
		response.EnumeratedValues = append(response.EnumeratedValues, &pb.Enumerate{
			Id:    int64(v.ID),
			Name:  v.Name,
			Index: int64(i),
		})
	}

	return &pb.UpdateIssuePropertyResponse{Data: response}, nil
}

func (i *IssueService) GetIssueProperty(ctx context.Context, req *pb.GetIssuePropertyRequest) (*pb.GetIssuePropertyResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrGetIssueProperty.NotLogin()
	}
	req.IdentityInfo = identityInfo

	property, err := i.query.GetProperties(req)
	if err != nil {
		return nil, err
	}
	return &pb.GetIssuePropertyResponse{Data: property}, nil
}

// GetByName 根据 name 获取 property 详情
func (i *IssueService) GetByName(orgID int64, name string, propertyIssueType string) (*dao.IssueProperty, error) {
	property, err := i.db.GetIssuePropertyByName(orgID, name, propertyIssueType)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, nil
	}
	return property, nil
}

func (i *IssueService) GetByRelation(id int64) (*dao.IssueProperty, error) {
	property, err := i.db.GetIssuePropertiesByRelation(id)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, nil
	}
	return property, nil
}

// 字段类型转换的校验
func IsCanChange(pt, newpt string) bool {
	if pt != newpt {
		// 如果都不是选择型，允许转换
		if common.IsOptions(pt) == false && common.IsOptions(newpt) == false {
			return true
		}
		// 如果单选变多选，允许转换
		if pt == pb.PropertyTypeEnum_Select.String() && (newpt == pb.PropertyTypeEnum_MultiSelect.String() || newpt == pb.PropertyTypeEnum_CheckBox.String()) {
			return true
		}
		// 多选互相转换,允许转换
		if (pt == pb.PropertyTypeEnum_MultiSelect.String() || pt == pb.PropertyTypeEnum_CheckBox.String()) && (newpt == pb.PropertyTypeEnum_MultiSelect.String() || newpt == pb.PropertyTypeEnum_CheckBox.String()) {
			return false
		}
	}
	return true
}

func (i *IssueService) UpdateIssuePropertiesIndex(ctx context.Context, req *pb.UpdateIssuePropertiesIndexRequest) (*pb.UpdateIssuePropertiesIndexResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrUpdateIssueProperty.NotLogin()
	}
	req.IdentityInfo = identityInfo

	var propertiesIndex []dao.IssueProperty
	for _, issueProperty := range req.Data {
		propertiesIndex = append(propertiesIndex, dao.IssueProperty{
			BaseModel: dbengine.BaseModel{
				ID: uint64(issueProperty.PropertyID),
			},
			ScopeID:           issueProperty.ScopeID,
			ScopeType:         issueProperty.ScopeType.String(),
			OrgID:             issueProperty.OrgID,
			PropertyName:      issueProperty.PropertyName,
			PropertyType:      issueProperty.PropertyType.String(),
			Required:          issueProperty.Required,
			DisplayName:       issueProperty.DisplayName,
			PropertyIssueType: issueProperty.PropertyIssueType.String(),
			Relation:          issueProperty.Relation,
			Index:             issueProperty.Index,
		})
	}
	if err := i.db.UpdateIssuePropertiesIndex(propertiesIndex); err != nil {
		return nil, err
	}
	return &pb.UpdateIssuePropertiesIndexResponse{Data: query.BatchConvert(propertiesIndex)}, nil
}

func (i *IssueService) GetIssuePropertyUpdateTime(ctx context.Context, req *pb.GetIssuePropertyUpdateTimeRequest) (*pb.GetIssuePropertyUpdateTimeResponse, error) {
	properties, err := i.db.GetIssuePropertiesByTime(req.OrgID)
	if err != nil {
		return nil, err
	}
	var updateAt pb.IssuePropertyUpdateTimes
	for _, v := range properties {
		switch v.PropertyIssueType {
		case pb.PropertyIssueTypeEnum_TASK.String():
			updateAt.Task = v.UpdatedAt.Format("2006-01-02 15:04:05")
		case pb.PropertyIssueTypeEnum_BUG.String():
			updateAt.Bug = v.UpdatedAt.Format("2006-01-02 15:04:05")
		case pb.PropertyIssueTypeEnum_EPIC.String():
			updateAt.Epic = v.UpdatedAt.Format("2006-01-02 15:04:05")
		case pb.PropertyIssueTypeEnum_REQUIREMENT.String():
			updateAt.Requirement = v.UpdatedAt.Format("2006-01-02 15:04:05")
		}
	}
	return &pb.GetIssuePropertyUpdateTimeResponse{Data: &updateAt}, nil
}

func (i *IssueService) CreateIssuePropertyInstance(ctx context.Context, req *pb.CreateIssuePropertyInstanceRequest) (*pb.CreateIssuePropertyInstanceResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssueProperty.NotLogin()
	}
	req.IdentityInfo = identityInfo

	issueModel, err := i.db.GetIssue(req.IssueID)
	if err != nil {
		return nil, apierrors.ErrCreateIssueProperty.InvalidParameter(err)
	}

	if !apis.IsInternalClient(ctx) {
		// issue 创建 校验用户在 当前 project 下是否拥有 CREATE ${ISSUE_TYPE} 权限
		if req.ProjectID == 0 {
			return nil, apierrors.ErrCreateIssueProperty.MissingParameter("projectID")
		}
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issueModel.ProjectID,
			Resource: "issue-" + strings.ToLower(issueModel.Type),
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return nil, apierrors.ErrCreateIssueProperty.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrCreateIssueProperty.AccessDenied()
		}
	}

	if err := i.query.CreatePropertyRelation(req); err != nil {
		return nil, err
	}
	return &pb.CreateIssuePropertyInstanceResponse{Data: req.IssueID}, nil
}

func (i *IssueService) GetIssuePropertyInstance(ctx context.Context, req *pb.GetIssuePropertyInstanceRequest) (*pb.GetIssuePropertyInstanceResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrGetIssueProperty.NotLogin()
	}
	req.IdentityInfo = identityInfo

	issueModel, err := i.db.GetIssue(req.IssueID)
	if err != nil {
		return nil, apierrors.ErrGetIssueProperty.InvalidParameter(err)
	}

	if !apis.IsInternalClient(ctx) {
		// issue 创建 校验用户在 当前 project 下是否拥有 CREATE ${ISSUE_TYPE} 权限
		if issueModel.ProjectID == 0 {
			return nil, apierrors.ErrGetIssueProperty.MissingParameter("projectID")
		}
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issueModel.ProjectID,
			Resource: "issue-" + strings.ToLower(issueModel.Type),
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return nil, apierrors.ErrGetIssueProperty.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrGetIssueProperty.AccessDenied()
		}
	}

	var instances []pb.IssuePropertyInstance
	mp := make(map[int64]int)
	// 获取该事件类型配置的全部自定义字段
	properties, err := i.query.GetProperties(&pb.GetIssuePropertyRequest{
		OrgID:             req.OrgID,
		PropertyIssueType: req.PropertyIssueType,
	})
	if err != nil {
		return nil, err
	}
	// 构建property到instances的映射，instances中存放自定义字段信息（不含值）
	for i, pro := range properties {
		instances = append(instances, pb.IssuePropertyInstance{
			PropertyID:        pro.PropertyID,
			ScopeID:           pro.ScopeID,
			ScopeType:         pro.ScopeType,
			OrgID:             pro.OrgID,
			PropertyName:      pro.PropertyName,
			DisplayName:       pro.DisplayName,
			PropertyType:      pro.PropertyType,
			Required:          pro.Required,
			PropertyIssueType: pro.PropertyIssueType,
			Relation:          pro.Relation,
			Index:             pro.Index,
			EnumeratedValues:  pro.EnumeratedValues,
			Values:            pro.Values,
			RelatedIssue:      pro.RelatedIssue,
		})
		mp[pro.PropertyID] = i
	}
	// 填充instances每个自定义字段的值
	relations, err := i.db.GetPropertyRelationByID(req.IssueID)
	if err != nil {
		return nil, err
	}
	for _, v := range relations {
		if mp[v.PropertyID] >= len(instances) {
			return nil, apierrors.ErrGetIssue.InvalidState("找不到使用的自定义字段")
		}
		if common.IsOptions(instances[mp[v.PropertyID]].PropertyType.String()) == false {
			instances[mp[v.PropertyID]].ArbitraryValue = structpb.NewStringValue(v.ArbitraryValue)
			continue
		}
		instances[mp[v.PropertyID]].PropertyEnumeratedValues = append(
			instances[mp[v.PropertyID]].PropertyEnumeratedValues, &pb.PropertyEnumerate{Id: v.PropertyValueID})
	}

	res, err := ConvertRelations(req.IssueID, instances)
	if err != nil {
		return nil, err
	}

	return &pb.GetIssuePropertyInstanceResponse{Data: res}, nil
}

func ConvertRelations(issueID int64, relations []pb.IssuePropertyInstance) (*pb.IssueAndPropertyAndValue, error) {
	res := &pb.IssueAndPropertyAndValue{
		IssueID: issueID,
	}
	for i, v := range relations {
		var arbitraryValue interface{}
		// 判断出参应该是数字还是字符串
		if v.PropertyType == pb.PropertyTypeEnum_Number && v.ArbitraryValue != nil {
			if val := v.ArbitraryValue.GetNumberValue(); val > 0 {
				arbitraryValue = val
			} else {
				arbitraryValue = v.ArbitraryValue.GetStringValue()
			}
		} else {
			arbitraryValue = v.ArbitraryValue
		}

		var arbi *structpb.Value
		if v.ArbitraryValue != nil {
			a, ok := arbitraryValue.(*structpb.Value)
			if ok {
				arbi = a
			} else {
				arb, err := structpb.NewValue(arbitraryValue)
				if err != nil {
					return nil, err
				}
				arbi = arb
			}
		}

		res.Property = append(res.Property, &pb.IssuePropertyExtraProperty{
			PropertyID:       v.PropertyID,
			PropertyType:     v.PropertyType,
			PropertyName:     v.PropertyName,
			Required:         v.Required,
			DisplayName:      v.DisplayName,
			ArbitraryValue:   arbi,
			EnumeratedValues: v.EnumeratedValues,
		})
		for _, val := range v.PropertyEnumeratedValues {
			res.Property[i].Values = append(res.Property[i].Values, val.Id)
		}
	}
	return res, nil
}

func GetArb(i *pb.IssuePropertyInstance) string {
	if s := i.ArbitraryValue.GetNumberValue(); s != 0 {
		return strconv.Itoa(int(s))
	}
	return i.ArbitraryValue.GetStringValue()
}
