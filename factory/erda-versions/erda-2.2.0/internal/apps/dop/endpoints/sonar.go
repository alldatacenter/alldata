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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/database/cimysql"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// sonar 分析结果的问题类型
const (
	Bugs             = "bugs"
	Coverage         = "coverage"
	Vulnerabilities  = "vulnerabilities"
	CodeSmells       = "codeSmells"
	Duplications     = "duplications"
	IssueStatistics  = "issuesStatistics"
	SonarMetricsName = "sonar_metrics_statistics"
)

// 工单对应的pagesize
const (
	PAGE = 300
)

// API 返回对应的错误类型
const (
	SonarIssue      = "SONAR_ISSUE"
	SonarIssueStore = "SONAR_ISSUE_STORE"
)

// CompareData 将新产生的 sonar 问题与上次分析产生的问题进行对比的数据
type CompareData struct {
	Path    string `json:"path"`
	Message string `json:"message"`
	Rule    string `json:"rule"`
	Code    string `json:"code"`
	//Line    int    `json:"line"`
	TextRange TextRange `json:"textRange"`
}

type TextRange struct {
	EndLine     int `json:"endLine"`
	EndOffset   int `json:"endOffset"`
	StartLine   int `json:"startLine"`
	StartOffset int `json:"startOffset"`
}

// SonarIssuesStore 储存Sonar 分析产生的结果数据
func (e *Endpoints) SonarIssuesStore(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if r.ContentLength == 0 {
		return apierrors.ErrStoreSonarIssue.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.SonarStoreRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrStoreSonarIssue.InvalidParameter(err).ToResp(), nil
	}

	// deal bugs, codeSmells, vulnerabilities tickets;
	// close tickets for resolved issues, create tickets for new issues
	// if has error, skip
	go func() {
		if err := e.dealTickets(&req, Bugs); err != nil {
			logrus.Warning(err)
		}
	}()

	go func() {
		if err := e.dealTickets(&req, CodeSmells); err != nil {
			logrus.Warning(err)
		}
	}()

	go func() {
		if err := e.dealTickets(&req, Vulnerabilities); err != nil {
			logrus.Warning(err)
		}
	}()

	resp, err := storeIssues(&req, e.bdl)
	if err != nil {
		return apierrors.ErrStoreSonarIssue.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp)
}

// SonarIssues 根据参数类型，获取 Sonar 的结果信息
func (e *Endpoints) SonarIssues(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		sonars = []dbclient.QASonar{}
		err    error
	)

	var req apistructs.SonarIssueGetRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrGetSonarIssue.InvalidParameter(err).ToResp(), nil
	}

	if req.AppID > 0 {
		err = cimysql.Engine.Where("app_id = ?", req.AppID).Desc("updated_at").Limit(1, 0).Find(&sonars)
		if err != nil {
			return apierrors.ErrGetSonarIssue.InternalError(err).ToResp(), nil
		}

		issueStatistics := statistics(sonars, false)

		return httpserver.OkResp(issueStatistics)
	}

	err = cimysql.Engine.Where("commit_id = ?", req.Key).Find(&sonars)
	if err != nil {
		return apierrors.ErrGetSonarIssue.InternalError(err).ToResp(), nil
	}

	var data interface{}

	switch req.Type {
	case IssueStatistics:
		data = statistics(sonars, true)
	case Bugs:
		data = composeIssues(Bugs, sonars)
	case Vulnerabilities:
		data = composeIssues(Vulnerabilities, sonars)
	case Coverage:
		data = composeIssuesTree(Coverage, sonars)
	case Duplications:
		data = composeIssuesTree(Duplications, sonars)
	case CodeSmells:
		data = composeIssues(CodeSmells, sonars)
	default:
		return apierrors.ErrGetSonarIssue.InvalidParameter("type").ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func storeIssues(sonarStore *apistructs.SonarStoreRequest, bdl *bundle.Bundle) (dbclient.QASonar, error) {
	sonar := dbclient.QASonar{
		Key: sonarStore.Key,
	}

	success, err := cimysql.Engine.Get(&sonar)
	if err != nil {
		logrus.Warningf("Get sonar issues info by key:%s from db failed, err:%v", sonar.Key, err)
	}

	issuesStatistics, err := json.Marshal(sonarStore.IssuesStatistics)
	if err != nil {
		logrus.Warningf("Marshal issuesStatistics:%v failed, err:%v", sonarStore.IssuesStatistics, err)
	} else {
		sonar.IssuesStatistics = string(issuesStatistics)
	}

	bugs, err := json.Marshal(sonarStore.Bugs)
	if err != nil {
		logrus.Warningf("Marshal bugs:%v failed, err:%v", sonarStore.Bugs, err)
	} else {
		sonar.Bugs = string(bugs)
	}

	codeSmells, err := json.Marshal(sonarStore.CodeSmells)
	if err != nil {
		logrus.Warningf("Marshal codeSmells:%v failed, err:%v", sonarStore.CodeSmells, err)
	} else {
		sonar.CodeSmells = string(codeSmells)
	}

	vulnerabilities, err := json.Marshal(sonarStore.Vulnerabilities)
	if err != nil {
		logrus.Warningf("Marshal vulnerabilities:%v failed, err:%v", sonarStore.Vulnerabilities, err)
	} else {
		sonar.Vulnerabilities = string(vulnerabilities)
	}

	coverage, err := json.Marshal(sonarStore.Coverage)
	if err != nil {
		logrus.Warningf("Marshal coverage:%v failed, err:%v", sonarStore.Coverage, err)
	} else {
		sonar.Coverage = string(coverage)
	}

	duplications, err := json.Marshal(sonarStore.Duplications)
	if err != nil {
		logrus.Warningf("Marshal duplications:%v failed, err:%v", sonarStore.Duplications, err)
	} else {
		sonar.Duplications = string(duplications)
	}

	sonar.CommitID = sonarStore.CommitID
	sonar.OperatorID = sonarStore.OperatorID
	sonar.ProjectID = sonarStore.ProjectID
	sonar.BuildID = sonarStore.BuildID
	sonar.ApplicationName = sonarStore.ApplicationName
	sonar.ApplicationID = sonarStore.ApplicationID
	sonar.GitRepo = sonarStore.GitRepo
	sonar.Branch = sonarStore.Branch
	sonar.LogID = sonarStore.LogID

	if success { // update
		if _, err = cimysql.Engine.ID(sonar.ID).Update(&sonar); err != nil {
			return dbclient.QASonar{}, errors.Errorf("failed to update sonar info, sonar: %+v, (%+v)",
				sonar, err)
		}
	} else { // insert
		_, err = cimysql.Engine.InsertOne(&sonar)
		if err != nil {
			return dbclient.QASonar{}, errors.Errorf("failed to insert sonar info, sonar: %+v, (%+v)",
				sonar, err)
		}
	}

	go MetricsSonar(sonarStore, bdl)

	return sonar, nil
}

func MetricsSonar(sonarStore *apistructs.SonarStoreRequest, bdl *bundle.Bundle) {
	if sonarStore == nil || bdl == nil {
		return
	}

	var metrics []apistructs.Metric
	var metric = &apistructs.Metric{}
	addDefaultTagAndField(sonarStore, metric, bdl)
	bugs, _ := strconv.ParseFloat(sonarStore.IssuesStatistics.Bugs, 64)
	coverage, _ := strconv.ParseFloat(sonarStore.IssuesStatistics.Coverage, 64)
	vulnerabilities, _ := strconv.ParseFloat(sonarStore.IssuesStatistics.Vulnerabilities, 64)
	codeSmells, _ := strconv.ParseFloat(sonarStore.IssuesStatistics.CodeSmells, 64)
	duplications, _ := strconv.ParseFloat(sonarStore.IssuesStatistics.Duplications, 64)
	metric.Fields["bugs_num"] = bugs
	metric.Fields["coverage"] = coverage
	metric.Fields["vulnerabilities"] = vulnerabilities
	metric.Fields["codeSmells"] = codeSmells
	metric.Fields["duplications"] = duplications
	metrics = append(metrics, *metric)

	doMetrics(metrics, bdl)
}

func addDefaultTagAndField(sonarStore *apistructs.SonarStoreRequest, metric *apistructs.Metric, bdl *bundle.Bundle) {
	metric.Timestamp = time.Now().UnixNano()
	metric.Name = SonarMetricsName
	metric.Fields = map[string]interface{}{}
	metric.Tags = map[string]string{}
	metric.Tags["app_id"] = strconv.Itoa(int(sonarStore.ApplicationID))
	metric.Tags["operator_id"] = sonarStore.OperatorID
	metric.Tags["project_id"] = strconv.Itoa(int(sonarStore.ProjectID))
	metric.Tags["commit_id"] = sonarStore.CommitID
	metric.Tags["branch"] = sonarStore.Branch
	metric.Tags["git_repo"] = sonarStore.GitRepo
	metric.Tags["build_id"] = strconv.Itoa(int(sonarStore.BuildID))
	metric.Tags["log_id"] = sonarStore.LogID
	metric.Tags["app_name"] = sonarStore.ApplicationName
	metric.Tags["project_name"] = sonarStore.ProjectName
	metric.Tags["_meta"] = "true"
	metric.Tags["_metric_scope"] = "org"
	metric.Fields["num"] = 1

	project, err := bdl.GetProject(uint64(sonarStore.ProjectID))
	if err != nil {
		logrus.Errorf("addDefaultTagAndField get project err: %v", err)
		return
	}
	org, err := bdl.GetOrg(project.OrgID)
	if err != nil {
		logrus.Errorf("addDefaultTagAndField get org err: %v", err)
		return
	}
	metric.Tags["_metric_scope_id"] = org.Name
	metric.Tags["org_name"] = org.Name
}

func doMetrics(metric []apistructs.Metric, bdl *bundle.Bundle) {
	logrus.Info(" doMetrics CollectMetrics start ")
	metricsObject := apistructs.Metrics{}
	metricsObject.Metric = metric
	var count = 1
	for count < 3 {
		err := bdl.CollectMetrics(&metricsObject)
		if err != nil {
			logrus.Errorf(" doMetrics CollectMetrics error %v", err)
			count++
			time.Sleep(time.Minute)
			if count >= 3 {
				logrus.Errorf(" doMetrics CollectMetrics lost data %+v", metricsObject)
			}
			continue
		}
		break
	}
	logrus.Info(" doMetrics CollectMetrics end ")
}

func statistics(sonars []dbclient.QASonar, bUTPassed bool) apistructs.TestIssuesStatistics {
	issues := apistructs.TestIssuesStatistics{
		Bugs:            "0",
		CodeSmells:      "0",
		Vulnerabilities: "0",
		Coverage:        "0.0",
		Duplications:    "0.0",
		Rating:          &apistructs.TestIssueStatisticsRating{},
	}

	if len(sonars) == 0 {
		return issues
	}

	commitID := sonars[0].CommitID
	issues.CommitID = commitID
	issues.Branch = sonars[0].Branch
	issues.Time = sonars[0].UpdatedAt

	if bUTPassed {
		UT, err := getUtPassed(commitID)
		if err != nil {
			logrus.Warning(err)
		}
		issues.UT = UT
	}

	for i := range sonars {
		if commitID != sonars[i].CommitID {
			continue
		}

		issuesTmp := apistructs.TestIssuesStatistics{}
		err := json.Unmarshal([]byte(sonars[i].IssuesStatistics), &issuesTmp)
		if err != nil {
			logrus.Warningf("failed to unmarshal statistics, (%+v)", err)
			continue
		}

		if issuesTmp.Bugs != "" {
			if issues.Bugs, err = plusInt64(issues.Bugs, issuesTmp.Bugs); err != nil {
				logrus.Warning(err)
			}
		}

		if issuesTmp.CodeSmells != "" {
			if issues.CodeSmells, err = plusInt64(issues.CodeSmells, issuesTmp.CodeSmells); err != nil {
				logrus.Warning(err)
			}
		}

		if issuesTmp.Vulnerabilities != "" {
			if issues.Vulnerabilities, err = plusInt64(issues.Vulnerabilities, issuesTmp.Vulnerabilities); err != nil {
				logrus.Warning(err)
			}
		}

		if issuesTmp.Coverage != "" {
			if issues.Coverage, err = plusFloat64(issues.Coverage, issuesTmp.Coverage); err != nil {
				logrus.Warning(err)
			}
		}

		if issuesTmp.Duplications != "" {
			if issues.Duplications, err = plusFloat64(issues.Duplications, issuesTmp.Duplications); err != nil {
				logrus.Warning(err)
			}
		}

		if issuesTmp.SonarKey != "" {
			if issues.SonarKey == "" {
				issues.SonarKey = issuesTmp.SonarKey
			} else {
				issues.SonarKey = fmt.Sprint(issues.SonarKey, ",", issuesTmp.SonarKey)
			}
		}

		if issuesTmp.Path != "" {
			if issues.Path == "" {
				issues.Path = issuesTmp.Path
			} else {
				issues.Path = fmt.Sprint(issues.Path, ",", issuesTmp.Path)
			}
		}

		if issuesTmp.Rating != nil {
			issues.Rating = issuesTmp.Rating
		}
	}

	return issues
}

func getUtPassed(commitID string) (string, error) {
	record, err := dbclient.FindTPRecordByCommitId(commitID)
	if err != nil {
		return "", err
	}

	var (
		ok     bool
		passed int64
		skiped int64
	)
	totals := record.Totals
	if passed, ok = totals.Statuses["passed"]; !ok {
		return "", errors.New("get passed error")
	}
	if skiped, ok = totals.Statuses["skipped"]; !ok {
		return "", errors.New("get skipped error")
	}

	ret := ((float64(passed) + float64(skiped)) * 100) / float64(totals.Tests)

	return fmt.Sprintf("%.2f", ret), nil
}

func composeIssues(issuestype string, sonars []dbclient.QASonar) []*apistructs.TestIssues {
	issues := []*apistructs.TestIssues{}
	var dbData []byte

	for i := range sonars {
		switch issuestype {
		case Bugs:
			dbData = []byte(sonars[i].Bugs)
		case CodeSmells:
			dbData = []byte(sonars[i].CodeSmells)
		case Vulnerabilities:
			dbData = []byte(sonars[i].Vulnerabilities)
		}

		issuesTmp := []*apistructs.TestIssues{}
		err := json.Unmarshal(dbData, &issuesTmp)
		if err != nil {
			logrus.Warningf("Unmarshal %s error:%v", err, issuestype)
			continue
		}

		issues = append(issues, issuesTmp...)
	}

	return issues
}

func composeIssuesTree(issuestype string, sonars []dbclient.QASonar) []*apistructs.TestIssuesTree {
	issuesTree := []*apistructs.TestIssuesTree{}
	var dbData []byte

	for i := range sonars {
		switch issuestype {
		case Coverage:
			dbData = []byte(sonars[i].Coverage)
		case Duplications:
			dbData = []byte(sonars[i].Duplications)
		}

		issuesTmp := []*apistructs.TestIssuesTree{}
		err := json.Unmarshal(dbData, &issuesTmp)
		if err != nil {
			logrus.Warningf("Unmarshal %s error:%v", err, issuestype)
			continue
		}

		issuesTree = append(issuesTree, issuesTmp...)
	}

	return issuesTree
}

func plusInt64(s1, s2 string) (string, error) {
	var (
		d1  int64
		d2  int64
		err error
	)

	if d1, err = strconv.ParseInt(s1, 10, 64); err != nil {
		return "", err
	}

	if d2, err = strconv.ParseInt(s2, 10, 64); err != nil {
		return "", err
	}

	return strconv.FormatInt(d1+d2, 10), err
}

func plusFloat64(s1, s2 string) (string, error) {
	var (
		d1  float64
		d2  float64
		err error
	)

	if d1, err = strconv.ParseFloat(s1, 64); err != nil {
		return "", err
	}

	if d2, err = strconv.ParseFloat(s2, 64); err != nil {
		return "", err
	}

	return strconv.FormatFloat(d1+d2, 'f', -1, 64), err
}

func map2List(m map[CompareData]*apistructs.TestIssues) (sl []*apistructs.TestIssues) {
	if len(m) == 0 {
		return
	}

	for _, v := range m {
		sl = append(sl, v)
	}

	return
}

func differenceSet(m1, m2 map[CompareData]*apistructs.TestIssues) []*apistructs.TestIssues {
	m3 := make(map[CompareData]*apistructs.TestIssues)
	for k, v := range m1 {
		m3[k] = v
	}

	for k := range m3 {
		if _, ok := m2[k]; ok {
			delete(m3, k)
		}
	}

	return map2List(m3)
}

func makeOldIssues(appID int64, issueType string) (map[CompareData]*apistructs.TestIssues, error) {
	var (
		qaSonar      *dbclient.QASonar
		issues       []*apistructs.TestIssues
		err          error
		issueContent string
	)

	if qaSonar, err = dbclient.FindLatestSonarByAppID(appID); err != nil {
		return nil, err
	}

	if qaSonar == nil {
		return nil, nil
	}

	switch issueType {
	case Bugs:
		issueContent = qaSonar.Bugs
	case CodeSmells:
		issueContent = qaSonar.CodeSmells
	case Vulnerabilities:
		issueContent = qaSonar.Vulnerabilities
	}

	if issueContent == "" {
		return nil, nil
	}

	if err = json.Unmarshal([]byte(issueContent), &issues); err != nil {
		return nil, errors.Wrapf(err, "failed to unmarshal %s", issueType)
	}

	return makeCompareIssues(issues), nil
}

func makeCompareIssues(issues []*apistructs.TestIssues) map[CompareData]*apistructs.TestIssues {
	var compareIssues = make(map[CompareData]*apistructs.TestIssues)

	for _, s := range issues {
		compareData := CompareData{}

		if len(s.Code) > 0 {
			compareData.Code = s.Code[0]
		}
		compareData.Message = s.Message
		compareData.Path = s.Path
		compareData.Rule = s.Rule
		compareData.TextRange = TextRange{
			EndLine:     s.TextRange.EndLine,
			EndOffset:   s.TextRange.EndOffset,
			StartLine:   s.TextRange.StartLine,
			StartOffset: s.TextRange.StartOffset,
		}
		compareIssues[compareData] = s
	}

	return compareIssues
}

func (e *Endpoints) dealTickets(so *apistructs.SonarStoreRequest, issueType string) error {
	var (
		// nIssues map[CompareData]*apistructs.TestIssues
		// oIssues map[CompareData]*apistructs.TestIssues
		// nl       []*apistructs.TestIssues
		// ol       []*apistructs.TestIssues
		tmpIssue []*apistructs.TestIssues
		err      error
	)

	switch issueType {
	case Bugs:
		tmpIssue = so.Bugs
	case CodeSmells:
		tmpIssue = so.CodeSmells
	case Vulnerabilities:
		tmpIssue = so.Vulnerabilities
	}
	// get new issues
	// nIssues = makeCompareIssues(tmpIssue)

	// // get already issues
	// if oIssues, err = makeOldIssues(so.ApplicationID, issueType); err != nil {
	// 	return err
	// }

	// compute new issues, then create tickets
	// nl = differenceSet(nIssues, oIssues)
	// if len(nl) > 0 {
	// 	e.createTicket(nl, so, issueType)
	// }

	//logrus.Infof("create new ticket, issues: %+v", nl)

	// compute resolved bugs, then close tickets
	// ol = differenceSet(oIssues, nIssues)
	// if len(ol) > 0 {
	// 	err = e.closeTicket(ol, so.ApplicationID, issueType)
	// 	if err != nil {
	// 		return err
	// 	}
	// }

	//logrus.Infof("close resolved ticket, issues: %+v", nl)
	err = e.ticket.Delete(strconv.FormatInt(so.ApplicationID, 10), string(convert2TicketType(issueType)), "application")
	e.createTicket(tmpIssue, so, issueType)

	return err
}

func (e *Endpoints) closeTicket(issues []*apistructs.TestIssues, appID int64, issueType string) error {
	var (
		ticketList []apistructs.Ticket
		err        error
	)

	for _, issue := range issues {
		req := apistructs.TicketListRequest{
			Type:       []apistructs.TicketType{convert2TicketType(issueType)},
			Priority:   getTicketPriority(issueType),
			Status:     apistructs.TicketOpen,
			TargetType: apistructs.TicketApp,
			TargetID:   strconv.FormatInt(appID, 10),
			PageSize:   PAGE,
		}

		if _, ticketList, err = e.ticket.List(&req); err != nil {
			return err
		}

		for i := range ticketList {
			if ticketList[i].Title != issue.Message || ticketList[i].Creator != apistructs.TicketUserQA {
				continue
			}
			if p, ok := ticketList[i].Label["path"]; !ok || p != issue.Path {
				continue
			}
			if r, ok := ticketList[i].Label["rule"]; !ok || r != issue.Rule {
				continue
			}
			if l, ok := ticketList[i].Label["line"]; !ok || l != strconv.Itoa(issue.Line) {
				continue
			}

			// if close ticket failed, skip this error
			if err = e.ticket.Close(e.permission, nil, ticketList[i].TicketID, user.ID(apistructs.TicketUserQA)); err != nil {
				logrus.Warning(err)
				continue
			}
			logrus.Infof("successed to close ticket, ticketID:%d", ticketList[i].TicketID)
		}
	}

	return nil
}

func (e *Endpoints) createTicket(issues []*apistructs.TestIssues, sonar *apistructs.SonarStoreRequest, issueType string) {
	for _, issue := range issues {
		extra := make(map[string]interface{})
		extra["path"] = issue.Path
		extra["message"] = fmt.Sprintf("%s (line %s in %s)", issue.Message, strconv.Itoa(issue.Line), issue.Path)
		extra["rule"] = issue.Rule
		extra["severity"] = issue.Severity
		extra["status"] = issue.Status
		extra["branch"] = sonar.Branch
		extra["startLine"] = strconv.Itoa(issue.TextRange.StartLine)
		extra["endLine"] = strconv.Itoa(issue.TextRange.EndLine)
		extra["line"] = strconv.Itoa(issue.Line)

		extra["lineCode"] = ""
		if len(issue.Code) > 0 {
			extra["lineCode"] = issue.Code[0]
		}

		// render code to markdown
		extra["code"] = fmt.Sprintf("```%s\n%s\n```", getCodeLang(issue.Path), strings.Join(issue.Code, "\n"))

		req := &apistructs.TicketCreateRequest{
			Title:      fmt.Sprintf("%s (line %s in %s)", issue.Message, strconv.Itoa(issue.Line), issue.Path),
			Type:       convert2TicketType(issueType),
			Priority:   getTicketPriority(issueType),
			UserID:     apistructs.TicketUserQA,
			Label:      extra,
			TargetType: apistructs.TicketApp,
			TargetID:   strconv.FormatInt(sonar.ApplicationID, 10),
		}

		ticketID, err := e.ticket.Create(user.ID(req.UserID), uuid.UUID(), req)
		if err != nil {
			logrus.Warningf("failed to create ticket, req: %+v, (%+v)", req, err)
			continue
		}

		logrus.Infof("successed to create ticket, req:%+v, ticketID: %d", req, ticketID)
	}
}

func getCodeLang(path string) string {
	if strings.HasSuffix(path, ".go") {
		return "go"
	}

	if strings.HasSuffix(path, ".java") || strings.HasSuffix(path, ".kt") {
		return "java"
	}

	if strings.HasSuffix(path, ".js") {
		return "js"
	}

	return "\n"
}

func getTicketPriority(issueType string) apistructs.TicketPriority {
	switch issueType {
	case Bugs:
		return apistructs.TicketHigh
	case CodeSmells:
		return apistructs.TicketMedium
	case Vulnerabilities:
		return apistructs.TicketLow
	}

	return apistructs.TicketLow
}

func convert2TicketType(issueType string) apistructs.TicketType {
	switch issueType {
	case Bugs:
		return apistructs.TicketBug
	case CodeSmells:
		return apistructs.TicketCodeSmell
	case Vulnerabilities:
		return apistructs.TicketVulnerability
	}

	return ""
}

// GetSonarCredential
// 该 API 只允许 token 访问，校验已由 openapi checkToken 完成
func (e *Endpoints) GetSonarCredential(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var sonarAddr string
	clusterName := r.URL.Query().Get("clusterName")
	switch clusterName {
	case conf.DiceClusterName():
		sonarAddr = httpclientutil.WrapHttp(conf.SonarAddr()) // sonar-scanner need protocol
	default:
		sonarAddr = conf.SonarPublicURL()
	}

	sonarCredential := apistructs.SonarCredential{
		Server: sonarAddr,
		Token:  conf.SonarAdminToken(),
	}

	return httpserver.OkResp(sonarCredential)
}
