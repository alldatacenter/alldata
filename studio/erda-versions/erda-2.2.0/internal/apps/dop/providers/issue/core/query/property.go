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

package query

import (
	"fmt"
	"strconv"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

func (p *provider) GetProperties(req *pb.GetIssuePropertyRequest) ([]*pb.IssuePropertyIndex, error) {
	properties, err := p.db.GetIssueProperties(*req)
	if err != nil {
		return nil, err
	}
	propertyIndexes := BatchConvert(properties)
	propertyMap := make(map[int64][]string) // key: PropertyID  value: index
	// 只有公用字段会被任务类型模版使用
	if req.PropertyIssueType == pb.PropertyIssueTypeEnum_COMMON.String() {
		allProperties, err := p.db.GetIssueProperties(pb.GetIssuePropertyRequest{
			OrgID: req.OrgID,
		})
		if err != nil {
			return nil, err
		}
		for _, p := range allProperties {
			if p.PropertyIssueType != pb.PropertyIssueTypeEnum_COMMON.String() {
				propertyMap[p.Relation] = append(propertyMap[p.Relation], GetZhName(p.PropertyIssueType))
			}
		}
	}
	for i, v := range propertyIndexes {
		if req.PropertyIssueType == pb.PropertyIssueTypeEnum_COMMON.String() {
			propertyIndexes[i].RelatedIssue = strutil.DedupSlice(propertyMap[v.PropertyID])
		} else {
			propertyIndexes[i].RelatedIssue = append(propertyIndexes[i].RelatedIssue, GetZhName(v.PropertyIssueType.String()))
		}
		// 如果不是单选或者多选，不需要获取枚举值
		if common.IsOptions(v.PropertyType.String()) == false {
			continue
		}
		var (
			values []dao.IssuePropertyValue
			err    error
		)
		// 如果是COMMON类型获取自己的枚举值，如果不是则获取关联的COMMON类型的枚举值
		if v.PropertyIssueType == pb.PropertyIssueTypeEnum_COMMON {
			values, err = p.db.GetIssuePropertyValues(v.PropertyID)
		} else {
			values, err = p.db.GetIssuePropertyValues(v.Relation)
		}
		if err != nil {
			return nil, err
		}
		for index, val := range values {
			propertyIndexes[i].EnumeratedValues = append(propertyIndexes[i].EnumeratedValues, &pb.Enumerate{
				Index: int64(index),
				Id:    int64(val.ID),
				Name:  val.Name,
			})
		}
		if common.IsOptions(propertyIndexes[i].PropertyType.String()) && len(propertyIndexes[i].EnumeratedValues) > 0 && propertyIndexes[i].Required == true {
			propertyIndexes[i].Values = append(propertyIndexes[i].Values, propertyIndexes[i].EnumeratedValues[0].Id)
		}
	}
	return propertyIndexes, nil
}

func GetZhName(t string) string {
	switch t {
	case pb.IssueTypeEnum_REQUIREMENT.String():
		return "需求"
	case pb.IssueTypeEnum_TASK.String():
		return "任务"
	case pb.IssueTypeEnum_BUG.String():
		return "缺陷"
	case pb.IssueTypeEnum_EPIC.String():
		return "史诗"
	case pb.PropertyIssueTypeEnum_COMMON.String():
		return "公用"
	default:
		panic(fmt.Sprintf("invalid issue type: %s", string(t)))
	}
}

func Convert(is *dao.IssueProperty) *pb.IssuePropertyIndex {
	return &pb.IssuePropertyIndex{
		PropertyID:        int64(is.ID),
		ScopeType:         pb.ScopeTypeEnum_ScopeType(pb.ScopeTypeEnum_ScopeType_value[is.ScopeType]),
		ScopeID:           is.ScopeID,
		OrgID:             is.OrgID,
		Required:          is.Required,
		PropertyType:      pb.PropertyTypeEnum_PropertyType(pb.PropertyTypeEnum_PropertyType_value[is.PropertyType]),
		PropertyName:      is.PropertyName,
		DisplayName:       is.DisplayName,
		PropertyIssueType: pb.PropertyIssueTypeEnum_PropertyIssueType(pb.PropertyIssueTypeEnum_PropertyIssueType_value[is.PropertyIssueType]),
		Relation:          is.Relation,
		Index:             is.Index,
	}
}

func BatchConvert(properties []dao.IssueProperty) []*pb.IssuePropertyIndex {
	var response []*pb.IssuePropertyIndex
	for _, is := range properties {
		response = append(response, Convert(&is))
	}
	return response
}

func (pr *provider) CreatePropertyRelation(req *pb.CreateIssuePropertyInstanceRequest) error {
	var propertyInstances []dao.IssuePropertyRelation
	for _, p := range req.Property {
		// 如果是单选或多选，每条枚举值建立联系
		if common.IsOptions(p.PropertyType.String()) {
			// 必填项
			if p.Required == true && len(p.Values) == 0 {
				return apierrors.ErrCreateIssue.MissingParameter(fmt.Sprintf("必填字段\"%v\"未填写", p.PropertyID))
			}
			for _, v := range p.Values {
				propertyInstances = append(propertyInstances, dao.IssuePropertyRelation{
					OrgID:           req.OrgID,
					ProjectID:       req.ProjectID,
					IssueID:         req.IssueID,
					PropertyID:      p.PropertyID,
					PropertyValueID: v,
				})
			}
		} else {
			arbValue := GetArb(p)
			if p.Required == true && arbValue == "" {
				return apierrors.ErrCreateIssue.MissingParameter(fmt.Sprintf("必填字段\"%v\"未填写", p.PropertyID))
			}
			if arbValue == "" {
				continue
			}
			propertyInstances = append(propertyInstances, dao.IssuePropertyRelation{
				OrgID:          req.OrgID,
				ProjectID:      req.ProjectID,
				IssueID:        req.IssueID,
				PropertyID:     p.PropertyID,
				ArbitraryValue: arbValue,
			})
		}
	}
	for _, p := range req.Property {
		if err := pr.db.DeletePropertyRelationsByPropertyID(req.IssueID, p.PropertyID); err != nil {
			return err
		}
	}
	if err := pr.db.CreatePropertyRelations(propertyInstances); err != nil {
		return err
	}
	return nil
}

func GetArb(i *pb.IssuePropertyInstance) string {
	if s := i.ArbitraryValue.GetNumberValue(); s != 0 {
		return strconv.Itoa(int(s))
	}
	return i.ArbitraryValue.GetStringValue()
}

func (p *provider) GetBatchProperties(orgID int64, issuesType []string) ([]*pb.IssuePropertyIndex, error) {
	var (
		properties []*pb.IssuePropertyIndex
		err        error
	)
	if len(issuesType) != 1 {
		return nil, nil
	}
	req := &pb.GetIssuePropertyRequest{OrgID: orgID}
	switch issuesType[0] {
	case pb.IssueTypeEnum_TASK.String():
		req.PropertyIssueType = pb.PropertyIssueTypeEnum_TASK.String()
	case pb.IssueTypeEnum_BUG.String():
		req.PropertyIssueType = pb.PropertyIssueTypeEnum_BUG.String()
	case pb.IssueTypeEnum_REQUIREMENT.String():
		req.PropertyIssueType = pb.PropertyIssueTypeEnum_REQUIREMENT.String()
	case pb.IssueTypeEnum_EPIC.String():
		req.PropertyIssueType = pb.PropertyIssueTypeEnum_EPIC.String()
	}
	properties, err = p.GetProperties(req)
	if err != nil {
		return nil, err
	}
	return properties, nil
}
