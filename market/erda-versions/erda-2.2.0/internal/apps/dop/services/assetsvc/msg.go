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

package assetsvc

import (
	"context"
	"fmt"
	"path"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/services/uc"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	contractProveTitle    = "API 调用申请处理结果"
	contractProveTitleEn  = "The Processing Result of Applying for API Contract"
	contractProveTemplate = `
管理员处理了您的 API 调用申请，详细信息如下：

- API 名称：{{assetName}}
- 客户端名：{{clientDisplayName}}({{clientName}})
- 客户端ID：{{clientID}}
- **处理结果: {{result}}** 

[点击此处快速跳转至申请列表查看详情]({{path}})
`
	contractProveTemplateEn = `
The manager has processed your API contract request, the more details:

- API Name：{{assetName}}
- Client Name：{{clientDisplayName}}({{clientName}})
- Client ID：{{clientID}}
- **Result: {{result}}** 

[Click here to see the request list]({{path}})
`

	requestAccessTitle    = "API 调用申请处理"
	requestAccessTitleEn  = "Please Process the Applying for API Contract"
	requestAccessTemplate = ` 
用户发起对 API 调用申请，{{do}}。
- 申请事项: {{requestItem}}
- 申请用户：{{userName}}
- API 名称: {{assetName}} 

[点击此处快速跳转至访问管理页面处理]({{path}})
`
	requestAccessTemplateEn = ` 
The User want to use the API，{{do}}。
- Item: {{requestItem}}
- User Name：{{userName}}
- API Asset Name: {{assetName}} 

[Click here to see more]({{path}})
`

	versionDeprecatedTitle    = "【注意】您申请调用的 API 版本已被停用"
	versionDeprecatedTitleEn  = "[Note] The API version you were using has been disabled"
	versionDeprecatedTemplate = `
您好，您正在调用的 API 版本已被停用，
请及时申请最新版本调用，如有疑问可联系 API 管理员进行具体咨询，谢谢！

详细信息：
- API 名称：{{assetName}}
- API 版本：{{apiVersion}} {{major}}.{{minor}}.{{patch}}
- 客户端名：{{clientDisplayName}}({{clientName}})
- 客户端ID ：{{clientID}}
- 更新状态：已停用
`
	versionDeprecatedTemplateEn = `
The API version you were using has been disabled.
Please reqeust the newest version.
You can contact the manager for more information.
Thank you.

Details：
- API Name：{{assetName}}
- API Version：{{apiVersion}} {{major}}.{{minor}}.{{patch}}
- Client Name：{{clientDisplayName}}({{clientName}})
- Client ID ：{{clientID}}
- Status：disable
`

	versionReAvailableTitle    = "您申请调用的 API 版本已恢复可用"
	versionReAvailableTitleEn  = "The API version you were using has been restored"
	versionReAvailableTemplate = `
您好，您正在调用的 API 版本已经恢复为可用，
如已经更换为其他版本，请忽略此信息，谢谢！

详细信息：
- API 名称：{{assetName}}
- API 版本：{{apiVersion}} {{major}}.{{minor}}.{{patch}}
- 客户端名：{{clientDisplayName}}({{clientName}})
- 客户端ID ：{{clientID}}
更新状态：已恢复可用
`
	versionReAvailableTemplateEn = `
	The API version you were using has been restored，you can use it again.
If you switch to some other version, please ignore this message.
Thank you！

Details：
- API Name：{{assetName}}
- API Version：{{apiVersion}} {{major}}.{{minor}}.{{patch}}
- Client Name：{{clientDisplayName}}({{clientName}})
- Client ID ：{{clientID}}
Status：able
`
)

var msgI18ns = map[string]msgI18n{
	"contractProveTitle":         {Cn: contractProveTitle, En: contractProveTitleEn},
	"contractProveTemplate":      {Cn: contractProveTemplate, En: contractProveTemplateEn},
	"requestAccessTitle":         {Cn: requestAccessTitle, En: requestAccessTitleEn},
	"requestAccessTemplate":      {Cn: requestAccessTemplate, En: requestAccessTemplateEn},
	"versionDeprecatedTitle":     {Cn: versionDeprecatedTitle, En: versionDeprecatedTitleEn},
	"versionDeprecatedTemplate":  {Cn: versionDeprecatedTemplate, En: versionDeprecatedTemplateEn},
	"versionReAvailableTitle":    {Cn: versionReAvailableTitle, En: versionReAvailableTitleEn},
	"versionReAvailableTemplate": {versionReAvailableTemplate, versionReAvailableTemplateEn},
	"empty":                      {Cn: "空", En: "Empty"},
	"slaChangedTobe":             {Cn: "SLA 更变为 %s", En: "SLA is changed to be %s"},
	"approved":                   {Cn: "同意调用", En: "approved"},
	"revokeContract":             {Cn: "撤销调用权限", En: "revoked the contract"},
	"refused":                    {Cn: "拒绝调用", En: "refused"},
	"approving":                  {Cn: "正在审批", En: "approving"},
	"slaUpdated":                 {Cn: "管理员更新了 SLA *%s*, 请及时查看", En: "the manager updated the SLA, please check it in time"},
	"applyToSLA":                 {Cn: "申请使用名称为 %s 的 SLA", En: "apply to the SLA %s"},
	"applyToAPI":                 {Cn: "申请调用 API %s %s", En: "apply to the API %s %s"},
	"pleaseProcessIt":            {Cn: "请及时处理", En: "please process it in time"},
	"noNeedToProcessIt":          {Cn: "已自动通过，无须处理", En: "it is approved automatically, no additional processing is required"},
}

type ApprovalResult string
type RequestItem string

type msgI18n struct {
	Cn, En string
}

func (n msgI18n) Get(locale string) string {
	if locale == "zh-CN" {
		return n.Cn
	}
	return n.En
}

// approval results
func (svc *Service) ApprovalResultSLAUpdated(ctx context.Context, name, locale string) ApprovalResult {
	if name == "" {
		name = msgI18ns["empty"].Get(locale)
	}
	return ApprovalResult(fmt.Sprintf(msgI18ns["slaChangedTobe"].Get(locale), name))
}

func (svc *Service) ManagerProvedContract(ctx context.Context, locale string) ApprovalResult {
	return ApprovalResult(msgI18ns["approved"].Get(locale))
}

func (svc *Service) ApprovalResultWhileDelete(ctx context.Context, status apistructs.ContractStatus, locale string) ApprovalResult {
	if status == apistructs.ContractApproved {
		return ApprovalResult(msgI18ns["revokeContract"].Get(locale))
	}
	return ApprovalResult(msgI18ns["refused"].Get(locale))
}

func (svc *Service) ApprovalResultFromStatus(ctx context.Context, status apistructs.ContractStatus, locale string) ApprovalResult {
	switch status {
	case apistructs.ContractApproving:
		return ApprovalResult(msgI18ns["approving"].Get(locale))
	case apistructs.ContractApproved:
		return ApprovalResult(msgI18ns["approved"].Get(locale))
	case apistructs.ContractDisapproved:
		return ApprovalResult(msgI18ns["refused"].Get(locale))
	case apistructs.ContractUnapproved:
		return ApprovalResult(msgI18ns["revokeContract"].Get(locale))
	default:
		return ""
	}
}

func (svc *Service) ManagerRewriteSLA(ctx context.Context, name, locale string) ApprovalResult {
	return ApprovalResult(fmt.Sprintf(msgI18ns["slaUpdated"].Get(locale), name))
}

// RequestItemSLA
func (svc *Service) RequestItemSLA(ctx context.Context, name, locale string) RequestItem {
	return RequestItem(fmt.Sprintf(msgI18ns["applyToSLA"].Get(locale), name))
}

func (svc *Service) RequestItemAPI(ctx context.Context, name, swaggerVersion, locale string) RequestItem {
	return RequestItem(fmt.Sprintf(msgI18ns["applyToAPI"].Get(locale), name, swaggerVersion))
}

// 异步发站内信和邮件, 通知 access 管理员
func (svc *Service) contractMsgToManager(ctx context.Context, orgID uint64, contractUserID string,
	asset *apistructs.APIAssetsModel, access *apistructs.APIAccessesModel, item RequestItem,
	autoed bool) {

	var username = contractUserID
	users, err := uc.GetUsers([]string{contractUserID, access.CreatorID})
	if err != nil {
		logrus.Errorf("failed to GetUsers, err: %v", err)
	}
	if user, ok := users[contractUserID]; ok {
		username = user.Nick
	}

	org, err := bdl.Bdl.GetOrg(orgID)
	if err != nil {
		logrus.Errorf("failed to GetOrg, err: %v", err)
		return
	}

	do := msgI18ns["pleaseProcessIt"].Get(org.Locale)
	if autoed {
		do = msgI18ns["noNeedToProcessIt"].Get(org.Locale)
	}

	go func() {
		wildDomain := conf.WildDomain()
		host := fmt.Sprintf("%s-org.%s", org.Name, wildDomain)
		pathName := accessDetailPath(org.Name, access.ID)
		forwardURL := path.Join(host, pathName)
		params := map[string]string{
			"requestItem": string(item),
			"userName":    username,
			"assetName":   access.AssetName,
			"path":        "https://" + forwardURL,
			"do":          do,
		}
		if err := svc.EmailNotify(
			msgI18ns["requestAccessTitle"].Get(org.Locale),
			msgI18ns["requestAccessTemplate"].Get(org.Locale),
			params,
			org.Locale,
			orgID,
			strutil.DedupSlice([]string{access.CreatorID, asset.CreatorID}),
		); err != nil {
			logrus.Errorf("failed to send email notify, err: %v", err)
		}
	}()
	go func() {
		params := map[string]string{
			"requestItem": string(item),
			"userName":    username,
			"assetName":   access.AssetName,
			"path":        accessDetailPath(org.Name, access.ID),
			"do":          do,
		}
		if err := svc.MboxNotify(
			msgI18ns["requestAccessTitle"].Get(org.Locale),
			msgI18ns["requestAccessTemplate"].Get(org.Locale),
			params,
			org.Locale,
			orgID,
			strutil.DedupSlice([]string{access.CreatorID, asset.CreatorID}),
		); err != nil {
			logrus.Errorf("failed to send mbox notify, err: %v", err)
		}
	}()
}

// 异步发送站内信和邮件, 通知调用申请人
func (svc *Service) contractMsgToUser(orgID uint64, contractUserID, assetName string, client *apistructs.ClientModel, result ApprovalResult) {
	org, err := bdl.Bdl.GetOrg(orgID)
	if err != nil {
		logrus.Errorf("failed to GetOrg, err: %v", err)
		return
	}

	go func() {
		wildDomain := conf.WildDomain()
		host := fmt.Sprintf("%s-org.%s", org.Name, wildDomain)
		pathName := myClientsPath(org.Name, client.ID)
		forwardURL := path.Join(host, pathName)
		if err := svc.EmailNotify(
			msgI18ns["contractProveTitle"].Get(org.Locale),
			msgI18ns["contractProveTemplate"].Get(org.Locale),
			map[string]string{
				"result":            string(result),
				"assetName":         assetName,
				"clientDisplayName": client.DisplayName,
				"clientName":        client.Name,
				"clientID":          client.ClientID,
				"path":              "https://" + forwardURL,
			},
			org.Locale,
			orgID,
			[]string{contractUserID},
		); err != nil {
			logrus.Errorf("failed to send email notify, err: %v", err)
		}
	}()

	go func() {
		if err := svc.MboxNotify(
			msgI18ns["contractProveTitle"].Get(org.Locale),
			msgI18ns["contractProveTemplate"].Get(org.Locale),
			map[string]string{
				"result":            string(result),
				"assetName":         assetName,
				"clientDisplayName": client.DisplayName,
				"clientName":        client.Name,
				"clientID":          client.ClientID,
				"path":              myClientsPath(org.Name, client.ID),
			},
			org.Locale,
			orgID,
			[]string{contractUserID},
		); err != nil {
			logrus.Errorf("failed to send mbox notify, err: %v", err)
		}
	}()
}

func (svc *Service) updateVersionMsgToUser(orgID uint64, contractUserID, assetName string,
	version *apistructs.APIAssetVersionsModel, client *apistructs.ClientModel) {
	org, err := bdl.Bdl.GetOrg(orgID)
	if err != nil {
		logrus.Errorf("failed to GetOrg, err: %v", err)
		return
	}

	major := strconv.FormatUint(version.Major, 10)
	minor := strconv.FormatUint(version.Minor, 10)
	patch := strconv.FormatUint(version.Patch, 10)

	title := msgI18ns["versionDeprecatedTitle"].Get(org.Locale)
	template := msgI18ns["versionDeprecatedTemplate"].Get(org.Locale)
	if !version.Deprecated {
		title = msgI18ns["versionReAvailableTitle"].Get(org.Locale)
		template = msgI18ns["versionReAvailableTemplate"].Get(org.Locale)
	}

	go func() {
		if err := svc.EmailNotify(
			title,
			template,
			map[string]string{
				"assetName":         assetName,
				"apiVersion":        version.SwaggerVersion,
				"major":             major,
				"minor":             minor,
				"patch":             patch,
				"clientDisplayName": client.DisplayName,
				"clientName":        client.Name,
				"clientID":          client.ClientID,
			},
			org.Locale,
			orgID,
			[]string{contractUserID},
		); err != nil {
			logrus.Errorf("failed to send email notify, err: %v", err)
		}
	}()

	go func() {
		if err := svc.MboxNotify(
			title,
			template,
			map[string]string{
				"assetName":         assetName,
				"apiVersion":        version.SwaggerVersion,
				"major":             major,
				"minor":             minor,
				"patch":             patch,
				"clientDisplayName": client.DisplayName,
				"clientName":        client.Name,
				"clientID":          client.ClientID,
			},
			org.Locale,
			orgID,
			[]string{contractUserID},
		); err != nil {
			logrus.Errorf("failed to send email notify, err: %v", err)
		}
	}()
}
