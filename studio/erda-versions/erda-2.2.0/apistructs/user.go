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

package apistructs

import (
	"encoding/json"
	"strconv"
	"time"
)

// /api/users/<userId>
// method: get
// 获取用户
type UserGetRequest struct {
	UserId string `path:"userId"`
}

// /api/users/<userId>
// method: get
type UserGetResponse struct {
	Header
	Data UserProfile `json:"data"`
}

type UserProfile struct {
	Id        uint64    `json:"id"`
	Email     string    `json:"email"`
	Mobile    string    `json:"mobile"`
	Nick      string    `json:"nick"`
	Avatar    string    `json:"avatar"`
	Status    string    `json:"status"`
	OrgId     uint64    `json:"orgId"`
	OrgName   string    `json:"orgName"`
	OrgLogo   string    `json:"orgLogo"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
	OpenToken string    `json:"openToken"`

	// 用户权限列表
	Authorizes []Authorize `json:"authorizes"`

	// 用户角色列表
	Roles []UserRole `json:"roles"`
}

type Authorize struct {
	// 权限key
	Key      string `json:"key"`
	TargetId string `json:"targetId"`
}

type UserRole struct {
	// 角色key
	RoleKey  string `json:"roleKey"`
	UserId   string `json:"userId"`
	TargetId string `json:"targetId"`
	Role     Role   `json:"key"`
}

type Role struct {
	Key string `json:"key"`

	// 范围
	Scope       string `json:"scope"`
	Name        string `json:"name"`
	Description string `json:"description"`

	// 权限列表
	Permissions []string  `json:"permissions"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

type User struct {
	Avatar    string    `json:"avatar"`
	CreatedAt time.Time `json:"createdAt"`
	Email     string    `json:"email"`
	Id        string    `json:"id"`
	Mobile    string    `json:"mobile"`
	Nick      string    `json:"nick"`
	Status    string    `json:"status"`

	// 三方用户如wechat,qq等
	ThirdPart string `json:"thirdPart"`

	// 三方用户的id
	ThirdUid  string    `json:"thirdUid"`
	UpdatedAt time.Time `json:"updatedAt"`
}

type SimpleUser struct {
	Id      uint64 `json:"id"`
	Email   string `json:"email"`
	Mobile  string `json:"mobile"`
	Nick    string `json:"nick"`
	Avatar  string `json:"avatar"`
	Status  string `json:"status"`
	OrgId   uint64 `json:"orgId"`
	OrgRole string `json:"orgRole"`
	OrgName string `json:"orgName"`
	OrgLogo string `json:"orgLogo"`
}

// UserListRequest 用户批量查询请求
type UserListRequest struct {
	// 查询关键字，可根据用户名/手机号/邮箱模糊匹配
	Query string `query:"q" schema:"q"`

	// 用户信息是否明文
	Plaintext bool `query:"plaintext" schema:"plaintext"`

	// 支持批量查询，传参形式: userID=xxx&userID=yyy
	UserIDs []string `query:"userID" schema:"userID"`
}

// UserListResponse 用户批量查询响应
type UserListResponse struct {
	Header
	Data UserListResponseData `json:"data"`
}

type UserIDResponse struct {
	Header
	Data string `json:"data"`
}

// UserListResponseData 用户批量查询响应数据
type UserListResponseData struct {
	Users []UserInfo `json:"users"`
}

// UserCurrentResponse 当前用户信息
type UserCurrentResponse struct {
	Header
	Data UserInfo `json:"data"`
}

// UserInfo 返回用户数据格式
type UserInfo struct {
	ID          string `json:"id"`
	Name        string `json:"name"`
	Nick        string `json:"nick"`
	Avatar      string `json:"avatar"`
	Phone       string `json:"phone"`
	Email       string `json:"email"`
	Token       string `json:"token"`
	LastLoginAt string `json:"lastLoginAt"`
	PwdExpireAt string `json:"pwdExpireAt"`
	Source      string `json:"source"`
}

// UserInfoExt 用户信息扩展
type UserInfoExt struct {
	UserInfo
	Locked bool `json:"locked"`
}

// UserPagingRequest 用户分页请求
type UserPagingRequest struct {
	Name     string `query:"name"`
	Nick     string `query:"nick"`
	Phone    string `query:"phone"`
	Email    string `query:"email"`
	Locked   *int   `query:"locked"`
	Source   string `query:"source"`
	PageNo   int    `query:"pageNo"`
	PageSize int    `query:"pageSize"`
}

// UserPagingResponse 用户分页结果
type UserPagingResponse struct {
	Header
	Data *UserPagingData `json:"data"`
}

// UserPagingData 用户分页数据
type UserPagingData struct {
	Total int           `json:"total"`
	List  []UserInfoExt `json:"list"`
}

// UserCreateItem 用户创建数据结构
type UserCreateItem struct {
	Name     string `json:"name"`
	Nick     string `json:"nick"`
	Phone    string `json:"phone"`
	Email    string `json:"email"`
	Password string `json:"password"`
}

// UserCreateRequest 用户创建请求
type UserCreateRequest struct {
	Users []UserCreateItem `json:"users"`
}

// UserCreateResponse 用户创建响应
type UserCreateResponse struct {
	Header
}

// UserUpdateInfoRequset 更新用户信息请求
type UserUpdateInfoRequset struct {
	UserID string `json:"userId"`
	Name   string `json:"name"`
	Nick   string `json:"nick"`
	Mobile string `json:"mobile"`
	Email  string `json:"email"`
}

// UserUpdateInfoResponse 更新用户信息响应
type UserUpdateInfoResponse struct {
	Header
}

// UserUpdateLoginMethodRequest 更新用户登录方式请求
type UserUpdateLoginMethodRequest struct {
	ID     string `json:"id"`
	Source string `json:"source"`
}

// UserUpdateLoginMethodResponse 更新用户录方式响应
type UserUpdateLoginMethodResponse struct {
	Header
}

// UserListLoginMethodResponse list用户录方式响应
type UserListLoginMethodResponse struct {
	Header
	Data []UserListLoginMethodData `json:"data"`
}

// UserListLoginMethodData list用户录方式响应
type UserListLoginMethodData struct {
	DisplayName string `json:"displayName"`
	Value       string `json:"value"`
}

// UserBatchUpdateLoginMethodRequest 批量用户更新登录请求
type UserBatchUpdateLoginMethodRequest struct {
	UserIDs []string `json:"userIDs"`
	Source  string   `json:"source"`
}

// UserBatchUpdateLoginMethodResponse 批量用户更新登录方式响应
type UserBatchUpdateLoginMethodResponse struct {
	Header
}

// UserFreezeRequest 用户冻结请求
type UserFreezeRequest struct {
	UserID string `path:"userID"`
}

// UserFreezeResponse 用户冻结响应
type UserFreezeResponse struct {
	Header
}

// UserBatchFreezeRequest 用户批量冻结请求
type UserBatchFreezeRequest struct {
	UserIDs []string `json:"userIDs"`
}

// UserBatchFreezeResponse 用户批量冻结响应
type UserBatchFreezeResponse struct {
	Header
}

// UserFreezeRequest 用户解冻请求
type UserUnfreezeRequest struct {
	UserID string `path:"userID"`
}

// UserUnfreezeResponse 用户解冻响应
type UserUnfreezeResponse struct {
	Header
}

// UserBatchUnFreezeRequest 用户批量解冻请求
type UserBatchUnFreezeRequest struct {
	UserIDs []string `json:"userIDs"`
}

// UserBatchUnFreezeResponse 用户批量解冻响应
type UserBatchUnFreezeResponse struct {
	Header
}

// PwdSecurityConfig 密码安全配置
type PwdSecurityConfig struct {
	// 密码错误弹出图片验证码次数
	CaptchaChallengeNumber int `json:"captchaChallengeNumber"`
	// 连续密码错误次数
	ContinuousPwdErrorNumber int `json:"continuousPwdErrorNumber"`
	// 24小时内累计密码错误次数
	MaxPwdErrorNumber int `json:"maxPwdErrorNumber"`
	// 强制重密码周期,单位:月
	ResetPassWordPeriod int `json:"resetPassWordPeriod"`
}

// PwdSecurityConfigGetResponse 密码安全配置查询结果
type PwdSecurityConfigGetResponse struct {
	Header
	Data *PwdSecurityConfig `json:"data"`
}

// PwdSecurityConfigUpdateRequest 密码安全配置更新请求
type PwdSecurityConfigUpdateRequest struct {
	PwdSecurityConfig
}

// PwdSecurityConfigUpdateResponse 密码安全配置更新结果
type PwdSecurityConfigUpdateResponse struct {
	Header
}

// UCAuditsListRequest UC List审计事件请求
type UCAuditsListRequest struct {
	Event     string `json:"event,omitempty"`
	EventType string `json:"eventType,omitempty"`
	IP        string `json:"ip,omitempty"`
	LastID    int64  `json:"lastId,omitempty"`
	Size      uint64 `json:"size"`
	UserID    string `json:"userId,omitempty"`
	EventTime int64  `json:"eventTime,omitempty"`
}

// UCAuditsListResponse UC List审计事件响应
type UCAuditsListResponse struct {
	Result []UCAudit `json:"result"`
}

// UCAudit uc审计事件
type UCAudit struct {
	ID           int64
	TenantID     int64           `json:"tenantId"`
	UserID       int64           `json:"userId"`
	EventType    string          `json:"eventType"`
	Event        string          `json:"event"`
	EventTime    int64           `json:"eventTime"`
	Extra        string          `json:"extra"`
	MacAddress   string          `json:"macAddress"`
	OperatorInfo UCAuditUserInfo `json:"operatorInfo"` // uc的操作人信息
	IP           string          `json:"ip"`
	UserInfo     UCAuditUserInfo `json:"userInfo"` // uc的被操作人信息
}

// UCExtra 更新用户信息时，uc审计返回的extra里有请求uc的request，
// 需要根据requset里的信息，判断这次更新的是什么用户信息
type UCExtra struct {
	Request  UCRequest       `json:"request"`
	UserInfo UCAuditUserInfo `json:"user"`
}

type UCRequest struct {
	Source   string `json:"source"`
	NickName string `json:"nickname"`
}

// UCAuditUserInfo 返回用户数据格式
type UCAuditUserInfo struct {
	ID       int64  `json:"id"`
	UserName string `json:"username"`
	Nick     string `json:"nickname"`
	Avatar   string `json:"avatar"`
	Mobile   string `json:"mobile"`
	Email    string `json:"email"`
	Token    string `json:"token"`
}

// Convert2SysUCAuditBatchCreateRequest 转换成批量创建dice审计请求
func (ua *UCAuditsListResponse) Convert2SysUCAuditBatchCreateRequest() (AuditBatchCreateRequest, []string, []int64) {
	var results AuditBatchCreateRequest
	var deDupUserIDs = make(map[string]string)
	var ucIDs []int64
	for _, audit := range ua.Result {
		template := audit.getTemplateInfo()
		if template == nil {
			continue
		}
		t := audit.getFormartEventTime()
		userID := strconv.FormatInt(audit.UserID, 10)
		operatorID := strconv.FormatInt(audit.OperatorInfo.ID, 10)
		results.Audits = append(results.Audits, Audit{
			UserID:       operatorID,
			ScopeType:    SysScope,
			ScopeID:      1,
			TemplateName: template.TemplateName,
			Context:      map[string]interface{}{"userName": audit.getUserName(), "nickName": audit.getNickName()},
			Result:       template.Result,
			ErrorMsg:     template.ErrorMsg,
			StartTime:    t,
			EndTime:      t,
			ClientIP:     audit.IP,
		},
		)
		deDupUserIDs[userID] = ""
		ucIDs = append(ucIDs, audit.ID)
	}

	var userIDs []string
	for userID := range deDupUserIDs {
		userIDs = append(userIDs, userID)
	}

	return results, userIDs, ucIDs
}

func (a *UCAudit) getUserName() string {
	userName := a.UserInfo.UserName
	if userName == "" {
		var extra UCExtra
		json.Unmarshal([]byte(a.Extra), &extra)
		userName = extra.UserInfo.UserName
	}

	return userName
}

func (a *UCAudit) getNickName() string {
	nickName := a.UserInfo.Nick
	if nickName == "" {
		var extra UCExtra
		json.Unmarshal([]byte(a.Extra), &extra)
		nickName = extra.UserInfo.Nick
	}

	return nickName
}

// getFormartEventTime 获取字符串类型，格式为 ‘2006-01-02 15:04:05’ 的时间
func (a *UCAudit) getFormartEventTime() string {
	// 事件发生时间不能为空，转换成字符串
	if a.EventTime == 0 {
		return ""
	}
	//毫秒unix
	return strconv.FormatInt(a.EventTime/1e3, 10)
}

// UCTemplateInfo 用于uc模版转换成dice的模版
type UCTemplateInfo struct {
	TemplateName TemplateName
	Result       Result
	ErrorMsg     string
}

// EventTypeTempMap uc事件模版对应的模版名
var EventTypeTempMap = map[string]UCTemplateInfo{
	"LOGIN":             {TemplateName: LoginTemplate, Result: SuccessfulResult},
	"LOG_OUT":           {TemplateName: LogoutTemplate, Result: SuccessfulResult},
	"UPDATE_PASSWORD":   {TemplateName: UpdatePasswordTemplate, Result: SuccessfulResult},
	"SIGN_UP":           {TemplateName: RegisterUserTemplate, Result: SuccessfulResult},
	"CREATE_USER":       {TemplateName: CreateUserTemplate, Result: SuccessfulResult},
	"DISABLE":           {TemplateName: DisableUserTemplate, Result: SuccessfulResult},
	"ENABLE":            {TemplateName: EnableUserTemplate, Result: SuccessfulResult},
	"FREEZE":            {TemplateName: FreezeUserTemplate, Result: SuccessfulResult},
	"UN_FREEZE":         {TemplateName: UnfreezeUserTemplate, Result: SuccessfulResult},
	"DESTROY":           {TemplateName: DestroyUserTemplate, Result: SuccessfulResult},
	"UPDATE_LOGIN_TYPE": {TemplateName: UpdateUserLoginTypeTemplateName, Result: SuccessfulResult},
	"RESET_MOBILE":      {TemplateName: UpdateUserTelTemplate, Result: SuccessfulResult},
	"RESET_EMAIL":       {TemplateName: UpdateUserMailTemplate, Result: SuccessfulResult},
	"BIND_EMAIL":        {TemplateName: UpdateUserMailTemplate, Result: SuccessfulResult},
	"UN_BIND_EMAIL":     {TemplateName: UpdateUserMailTemplate, Result: SuccessfulResult},
	"TEMP_LOCK":         {TemplateName: FreezedSinceLoginFailedTemplateName, Result: SuccessfulResult},
	"WRONG_PASSWORD":    {TemplateName: LoginTemplate, Result: FailureResult, ErrorMsg: "Wrong PassWord"},
}

// uc eventType 和 dice 审计模版名没有对应上，需要转换一次
func (a *UCAudit) getTemplateInfo() *UCTemplateInfo {
	// 改用户的任何信息，uc返回的eventType都是UPDATE_USER_INFO，作区分的方案是根据uc审计事件返回里的extra里的requst做区分
	// 这个request，是dice调用uc更新用户信息时的requst请求体，当request里含有什么字段时，则代表更新了什么内容
	// 这也意味着，之后有新的调用uc更新用户信息的接口时，需要注意只传修改的内容，且一次只能修改一个字段，即只能patch
	// 而修改手机号，是直接走的uc的前端，是全量的信息，dice无法判断，商量后uc会对修改手机号产生新的eventType
	if a.EventType == "UPDATE_USER_INFO" {
		var extra UCExtra
		json.Unmarshal([]byte(a.Extra), &extra)
		// source 不为空说明dice请求的更新用户信息接口是更新的登录方式
		if extra.Request.Source != "" {
			v := EventTypeTempMap["UPDATE_LOGIN_TYPE"]
			return &v
		}
		return nil
	}

	if v, ok := EventTypeTempMap[a.EventType]; ok {
		return &v
	}

	return nil
}
