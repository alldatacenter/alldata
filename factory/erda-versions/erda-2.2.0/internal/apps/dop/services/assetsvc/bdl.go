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

// 在 bundle 能力上的再封装

package assetsvc

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/uc"
)

// 通知消息中的 action 参数
type Action string

func status2Action(status apistructs.ContractStatus) Action {
	switch status {
	case apistructs.ContractApproving:
		return "正在审批"
	case apistructs.ContractApproved:
		return "通过了"
	case apistructs.ContractDisapproved:
		return "拒绝了"
	case apistructs.ContractUnapproved:
		return "撤销了"
	default:
		return ""
	}
}

func (svc *Service) CheckClientIDSecret(orgID, userID, clientID, clientSecret string) error {
	credentials, err := bdl.Bdl.GetClientCredentials(orgID, userID, clientID)
	if err != nil {
		return err
	}
	if credentials.ClientSecret != clientSecret {
		return errors.New("clientID mismatch clientSecret")
	}

	return nil
}

func (svc *Service) EmailNotify(title, templateName string, params map[string]string, locale string, orgID uint64, userIDs []string) error {
	users, err := uc.GetUsers(userIDs)
	if err != nil {
		return errors.Wrap(err, "failed to GetUsers")
	}
	var emails []string
	for _, v := range users {
		if v.Email != nil && *v.Email != "" {
			emails = append(emails, *v.Email)
		}
	}
	if params != nil {
		params["title"] = title
	}
	return bdl.Bdl.CreateEmailNotify(templateName, params, locale, orgID, emails)
}

func (svc *Service) MboxNotify(title, templateName string, params map[string]string, locale string, orgID uint64, users []string) error {
	if params != nil {
		params["title"] = title
	}
	return bdl.Bdl.CreateMboxNotify(templateName, params, locale, orgID, users)
}

func (svc *Service) GetEndpointDomains(orgID, userID, endpointID string) []string {
	endpoint, err := bdl.Bdl.GetEndpoint(orgID, userID, endpointID)
	if err != nil {
		return nil
	}
	return endpoint.BindDomain
}

func (svc *Service) createOrUpdateClientLimits(orgID, userID, endpointID, clientID string, contractID uint64) (err error) {
	var contract apistructs.ContractModel
	if err = svc.FirstRecord(&contract, map[string]interface{}{"id": contractID}); err != nil {
		return err
	}
	if contract.Status.ToLower() != apistructs.ContractApproved {
		return nil
	}

	switch curSLAID := contract.CurSLAID; {
	case curSLAID == nil:
		// 无当前 SLA, 设置 1 次/天 的流量限制
		return bdl.Bdl.CreateOrUpdateClientLimits(orgID, userID, clientID, endpointID, OnceADayLimitType())
	case *curSLAID == 0:
		// 设置无流量限制
		return bdl.Bdl.CreateOrUpdateClientLimits(orgID, userID, clientID, endpointID, nil)
	}

	// 正常情况, 按 SLA 流量规则设置流量限制
	var limitModel apistructs.SLALimitModel
	if err = svc.FirstRecord(&limitModel, map[string]interface{}{"sla_id": *contract.CurSLAID}); err != nil {
		return err
	} // 目前支持一条流量限制, 所以用 FirstRecord

	return bdl.Bdl.CreateOrUpdateClientLimits(orgID, userID, clientID, endpointID,
		svc.limitModels2Types([]*apistructs.SLALimitModel{&limitModel}))
}

func (svc *Service) limitModels2Types(models []*apistructs.SLALimitModel) []apistructs.LimitType {
	var limitTypes = make([]apistructs.LimitType, len(models))
	for i, v := range models {
		limitType := apistructs.LimitType{
			Day:    nil,
			Hour:   nil,
			Minute: nil,
			Second: nil,
		}
		times := int(v.Limit)
		switch v.Unit {
		case apistructs.DurationSecond:
			limitType.Second = &times
		case apistructs.DurationMinute:
			limitType.Minute = &times
		case apistructs.DurationHour:
			limitType.Hour = &times
		case apistructs.DurationDay:
			limitType.Day = &times
		default:
			continue
		}

		limitTypes[i] = limitType
	}
	return limitTypes
}

// SLA 影响到的客户端列表
func (svc *Service) slaAffectClients(slaID uint64) ([]*apistructs.ClientModel, []*apistructs.ContractModel, error) {
	// 查出所有受影响的客户端
	var (
		contracts       []*apistructs.ContractModel
		clientPrimaries []uint64
		clients         []*apistructs.ClientModel
	)
	if err := svc.ListRecords(&contracts, map[string]interface{}{
		"cur_sla_id": slaID,
	}); err != nil {
		return nil, nil, errors.Wrap(err, "failed to ListRecords contracts")
	}

	for _, contract := range contracts {
		clientPrimaries = append(clientPrimaries, contract.ClientID)
	}

	if err := dbclient.Sq().Where("id IN (?)", clientPrimaries).
		Find(&clients).
		Error; err != nil {
		return nil, nil, errors.Wrap(err, "failed to Find clients")
	}

	return clients, contracts, nil
}
