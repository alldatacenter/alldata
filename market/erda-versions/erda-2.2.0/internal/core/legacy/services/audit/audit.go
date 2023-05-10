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

package audit

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"strings"
	"text/template"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/cron"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Audit 成员操作封装
type Audit struct {
	db    *dao.DBClient
	uc    *ucauth.UCClient
	cron  *cron.Cron
	trans i18n.Translator
}

// Option 定义 Member 对象配置选项
type Option func(*Audit)

// New 新建 Audit 实例
func New(options ...Option) *Audit {
	audit := &Audit{
		cron: cron.New(),
	}
	for _, op := range options {
		op(audit)
	}
	//启动定时任务
	audit.startCronJob()
	return audit
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(a *Audit) {
		a.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(a *Audit) {
		a.uc = uc
	}
}

// WithTrans sets the i18n.Translator
func WithTrans(trans i18n.Translator) Option {
	return func(a *Audit) {
		a.trans = trans
	}
}

// Create 创建审计事件
func (a *Audit) Create(req apistructs.AuditCreateRequest) error {
	audit, err := convertAuditCreateReq2Model(req.Audit)
	if err != nil {
		return err
	}

	if err := a.db.CreateAudit(audit); err != nil {
		return err
	}

	return nil
}

// BatchCreateAudit 批量创建审计
func (a *Audit) BatchCreateAudit(reqs []apistructs.Audit) error {
	var audits []model.Audit
	for _, req := range reqs {
		audit, err := convertAuditCreateReq2Model(req)
		if err != nil {
			return err
		}
		audits = append(audits, *audit)
	}

	if err := a.db.BatchCreateAudit(audits); err != nil {
		return err
	}
	return nil
}

// List 通过参数过滤事件
func (a *Audit) List(param *apistructs.AuditsListRequest) (int, []model.Audit, error) {
	return a.db.GetAuditsByParam(param)
}

// UpdateAuditCleanCron 更新审计事件周期
func (a *Audit) UpdateAuditCleanCron(orgID, interval int64) error {
	return a.db.UpdateAuditCleanCron(orgID, interval)
}

// ExportExcel 导出审计到excel
func (a *Audit) ExportExcel(ctx context.Context, audits []model.Audit) (io.Reader, string, error) {
	table, err := a.convertAuditsToExcelList(ctx, audits)
	if err != nil {
		return nil, "", err
	}
	tableName := "audits"
	buf := bytes.NewBuffer([]byte{})
	if err := excel.ExportExcel(buf, table, tableName); err != nil {
		return nil, "", err
	}
	return buf, tableName, nil
}

func (a *Audit) convertAuditsToExcelList(ctx context.Context, audits []model.Audit) ([][]string, error) {
	r := [][]string{{"操作时间", "操作者", "操作", "客户端ip"}}
	userIDNameMap := make(map[string]string)
	for _, audit := range audits {
		userIDNameMap[audit.UserID] = ""
		r = append(r, append(append([]string{audit.StartTime.Format("2006-01-02 15:04:05"), audit.UserID,
			a.getContent(ctx, audit), audit.ClientIP})))
	}

	var userIDs []string
	for k := range userIDNameMap {
		userIDs = append(userIDs, k)
	}

	users, err := a.uc.FindUsers(userIDs)
	if err != nil {
		return nil, err
	}
	for _, u := range users {
		userIDNameMap[u.ID] = u.Nick
	}

	// 把r里的userID替换成userName
	l := len(r)
	for i := 1; i < l; i++ {
		if r[i][1] != "" {
			if name, ok := userIDNameMap[r[i][1]]; ok {
				r[i][1] = name
			}
		}
	}

	return r, nil
}

func (a *Audit) getContent(ctx context.Context, audit model.Audit) string {
	langCodes, _ := ctx.Value("lang_codes").(i18n.LanguageCodes)
	var locale string
	if len(langCodes) != 0 {
		locale = langCodes[0].Code
	}
	if strings.Contains(locale, "zh") {
		locale = "zh"
	}
	if strings.Contains(locale, "en") {
		locale = "en"
	}

	ct := conf.AuditTemplate()[audit.TemplateName].Success[locale]
	logrus.Debugf("audit template is: %s", ct)
	tpl, err := template.New("c").Parse(ct)
	if err != nil {
		return ""
	}

	// 处理下context里的内容：1.审计外部结构的value放入context里 2.context里的结构体处理下 3.做一些翻译
	context := make(map[string]interface{})
	if err = json.Unmarshal([]byte(audit.Context), &context); err != nil {
		return ""
	}

	context["scopeType"] = func() interface{} {
		switch audit.ScopeType {
		case apistructs.ProjectScope:
			s := a.trans.Text(langCodes, "PROJECT")
			if _, ok := context["projectName"]; ok {
				s += " " + context["projectName"].(string)
			}
			return s
		case apistructs.AppScope:
			s := a.trans.Text(langCodes, "APPLICATION")
			if _, ok := context["projectName"]; ok {
				s += " " + context["projectName"].(string)
			}
			if _, ok := context["appName"]; ok {
				s += " / " + context["appName"].(string)
			}
			return s
		}
		return audit.ScopeType
	}()
	if issueType, ok := context["issueType"]; ok {
		context["issueType"] = apistructs.IssueType(issueType.(string)).GetZhName()
	}
	if userInfo, ok := context["users"]; ok {
		var users []string
		for _, v := range userInfo.([]interface{}) {
			users = append(users, v.(map[string]interface{})["nick"].(string))
		}
		context["users"] = strings.Join(users, ",")
	}

	var content bytes.Buffer
	if err := tpl.Execute(&content, context); err != nil {
		return ""
	}

	return content.String()
}

// GetAuditCleanCron 获取审计事件周期
func (a *Audit) GetAuditCleanCron(orgID int64) (*apistructs.AuditListCleanCronResponseData, error) {
	audit, err := a.db.GetAuditCleanCron(orgID)
	if err != nil {
		return nil, err
	}
	var interval uint64
	if audit.Config.AuditInterval == 0 {
		// 当天创建的企业，还没有被定时任务初始化过的清理周期，先转化成7
		interval = 7
	} else {
		interval = uint64(-audit.Config.AuditInterval)
	}

	return &apistructs.AuditListCleanCronResponseData{
		Interval: interval,
	}, nil
}

func (a *Audit) startCronJob() {
	a.cron.AddFunc(conf.AuditCleanCron(), a.cronCleanAudit)
	a.cron.AddFunc(conf.AuditArchiveCron(), a.cronArchiveAudit)
	a.cron.Start()
}

func (a *Audit) getCleanAuditConfig() map[int][]uint64 {
	//获取企业审计清理周期配置
	auditSettings, err := a.db.GetAuditSettings()
	if err != nil {
		logrus.Errorf(err.Error())
	}

	// key 是清理周期，value是多个企业id，按周期把企业分组
	intervalOrgsMap := make(map[int][]uint64)
	for _, setting := range auditSettings {
		interval := int(setting.Config.AuditInterval)
		intervalOrgsMap[interval] = append(intervalOrgsMap[interval], setting.ID)
	}

	// 清理周期为0天的企业，初始化为7天
	orgIDs := intervalOrgsMap[0]
	if orgIDs != nil {
		if err := a.db.InitOrgAuditInterval(orgIDs); err != nil {
			logrus.Errorf(err.Error())
		}
		intervalOrgsMap[-7] = append(intervalOrgsMap[-7], orgIDs...)
		delete(intervalOrgsMap, 0)
	}

	return intervalOrgsMap
}

func (a *Audit) cronCleanAudit() {
	intervalOrgsMap := a.getCleanAuditConfig()
	// 软删除企业审计事件
	for interval, orgs := range intervalOrgsMap {
		startAt := time.Now().AddDate(0, 0, interval)
		// delete org audit one by one to avoid very long IN clause
		for _, org := range orgs {
			if err := a.db.DeleteAuditsByTimeAndOrg(startAt, org); err != nil {
				logrus.Errorf(err.Error())
			}
		}
	}
	// 软删除系统审计事件
	startAt := time.Now().AddDate(0, 0, conf.SysAuditCleanInterval())
	if err := a.db.DeleteAuditsByTimeAndSys(startAt); err != nil {
		logrus.Errorf(err.Error())
	}
}

func (a *Audit) cronArchiveAudit() {
	// 归档已经软删除的审计事件
	if err := a.db.ArchiveAuditsByTimeAndOrg(); err != nil {
		logrus.Errorf(err.Error())
	}
}

func convertAuditCreateReq2Model(req apistructs.Audit) (*model.Audit, error) {
	context, err := json.Marshal(req.Context)
	if err != nil {
		return nil, err
	}
	startAt, err := time.ParseInLocation("2006-01-02 15:04:05", req.StartTime, time.Local)
	if err != nil {
		return nil, err
	}
	endAt, err := time.ParseInLocation("2006-01-02 15:04:05", req.EndTime, time.Local)
	if err != nil {
		return nil, err
	}
	audit := &model.Audit{
		StartTime:    startAt,
		EndTime:      endAt,
		UserID:       req.UserID,
		ScopeType:    req.ScopeType,
		ScopeID:      req.ScopeID,
		FDPProjectID: req.FDPProjectID,
		AppID:        req.AppID,
		OrgID:        req.OrgID,
		ProjectID:    req.ProjectID,
		Context:      string(context),
		TemplateName: req.TemplateName,
		AuditLevel:   req.AuditLevel,
		Result:       req.Result,
		ErrorMsg:     req.ErrorMsg,
		ClientIP:     req.ClientIP,
		UserAgent:    req.UserAgent,
	}

	return audit, nil
}
