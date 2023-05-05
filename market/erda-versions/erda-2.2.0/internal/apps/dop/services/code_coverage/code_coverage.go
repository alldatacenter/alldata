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

package code_coverage

import (
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/environment"
)

const SourcecovAddonName = "sourcecov"

type CodeCoverage struct {
	db *dao.DBClient

	bdl       *bundle.Bundle
	envConfig *environment.EnvConfig
}

type Option func(*CodeCoverage)

func New(options ...Option) *CodeCoverage {
	c := &CodeCoverage{}
	for _, op := range options {
		op(c)
	}
	return c
}

func WithDBClient(db *dao.DBClient) Option {
	return func(c *CodeCoverage) {
		c.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(c *CodeCoverage) {
		c.bdl = bdl
	}
}

func WithEnvConfig(envConfig *environment.EnvConfig) Option {
	return func(e *CodeCoverage) {
		e.envConfig = envConfig
	}
}

// Start Start record
func (svc *CodeCoverage) Start(req apistructs.CodeCoverageStartRequest) error {
	// check permission
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectID,
			Resource: "codeCoverage",
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrStartCodeCoverageExecRecord.AccessDenied()
		}
	}

	if err := svc.JudgeRunningRecordExist(req.ProjectID, req.Workspace); err != nil {
		return err
	}

	record := dao.CodeCoverageExecRecord{
		ProjectID:     req.ProjectID,
		Status:        apistructs.RunningStatus,
		ReportStatus:  apistructs.RunningStatus,
		TimeBegin:     time.Now(),
		StartExecutor: req.UserID,
		Workspace:     req.Workspace,
		TimeEnd:       time.Date(1000, 01, 01, 0, 0, 0, 0, time.Local),
		ReportTime:    time.Date(1000, 01, 01, 0, 0, 0, 0, time.Local),
	}
	tx := svc.db.Begin()
	if err := tx.Debug().Create(&record).Error; err != nil {
		tx.Rollback()
		return err
	}
	tx.Commit()
	return nil
}

// End End record
func (svc *CodeCoverage) End(req apistructs.CodeCoverageUpdateRequest) error {
	record, err := svc.db.GetCodeCoverageByID(req.ID)
	if err != nil {
		return err
	}

	// check permission
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  record.ProjectID,
			Resource: "codeCoverage",
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrEndCodeCoverageExecRecord.AccessDenied()
		}
	}

	if record.Status != apistructs.ReadyStatus {
		return errors.New("the pre status is not ready")
	}
	record.Status = apistructs.EndingStatus
	record.TimeEnd = time.Now()
	record.EndExecutor = req.UserID
	if err := svc.db.UpdateCodeCoverage(record); err != nil {
		return err
	}

	return nil
}

// Cancel Cancel record
func (svc *CodeCoverage) Cancel(req apistructs.CodeCoverageCancelRequest) error {
	// check permission
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectID,
			Resource: "codeCoverage",
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrCancelCodeCoverageExecRecord.AccessDenied()
		}
	}
	record := dao.CodeCoverageExecRecord{
		Status:       apistructs.CancelStatus,
		ReportStatus: apistructs.CancelStatus,
		TimeEnd:      time.Now(),
	}

	return svc.db.CancelCodeCoverage(req.ProjectID, req.Workspace, &record)
}

// ReadyCallBack Record ready callBack
func (svc *CodeCoverage) ReadyCallBack(req apistructs.CodeCoverageUpdateRequest) error {
	record, err := svc.db.GetCodeCoverageByID(req.ID)
	if err != nil {
		return err
	}
	if record.Status == apistructs.CancelStatus {
		return nil
	}

	if record.Status != apistructs.RunningStatus {
		return errors.New("the pre status is not running")
	}
	record.Status = apistructs.ReadyStatus
	record.Msg = req.Msg

	return svc.db.UpdateCodeCoverage(record)
}

// EndCallBack Record end callBack
func (svc *CodeCoverage) EndCallBack(req apistructs.CodeCoverageUpdateRequest) error {
	record, err := svc.db.GetCodeCoverageByID(req.ID)
	if err != nil {
		return err
	}

	status := apistructs.CodeCoverageExecStatus(req.Status)
	if status != apistructs.FailStatus && status != apistructs.SuccessStatus {
		return errors.New("the status is not fail or success")
	}
	if record.Status == apistructs.CancelStatus {
		return nil
	}

	project, err := svc.bdl.GetProject(record.ProjectID)
	if err != nil {
		return err
	}
	record.Status = status
	record.Msg = req.Msg
	if status == apistructs.FailStatus {
		record.ReportStatus = apistructs.FailStatus
	}

	if req.ReportXmlUUID != "" {

		f, err := svc.bdl.DownloadDiceFile(req.ReportXmlUUID)
		if err != nil {
			return err
		}
		defer f.Close()

		tempAddr, err := ioutil.TempDir("", "jacoco_xml_tar_gz")
		if err != nil {
			return err
		}

		var xmlTarFileName = "_project_xml.tar.gz"
		err = simpleRun("", "bash", "-c", fmt.Sprintf("cd %v && touch %v", tempAddr, xmlTarFileName))
		if err != nil {
			return err
		}

		fileBytes, err := ioutil.ReadAll(f)
		if err != nil {
			return err
		}

		err = ioutil.WriteFile(fmt.Sprintf("%v/%v", tempAddr, xmlTarFileName), fileBytes, 0777)
		if err != nil {
			return err
		}

		err = simpleRun("", "bash", "-c", fmt.Sprintf("cd %v && tar -xzf %v", tempAddr, xmlTarFileName))
		if err != nil {
			return err
		}

		file, err := os.Open(fmt.Sprintf("%v/%v", tempAddr, "_project_xml"))
		if err != nil {
			return err
		}

		all, err := ioutil.ReadAll(file)
		if err != nil {
			return err
		}
		analyzeJson, coverage, err := getAnalyzeJson(project.ID, project.DisplayName, all)
		if err != nil {
			return err
		}
		record.ReportContent = analyzeJson
		record.Coverage = coverage
	}

	return svc.db.UpdateCodeCoverage(record)
}

func simpleRun(dir string, name string, arg ...string) error {
	fmt.Fprintf(os.Stdout, "Run: %s, %v\n", name, arg)
	cmd := exec.Command(name, arg...)
	if dir != "" {
		cmd.Path = dir
	}
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

// ReportCallBack Record report callBack
func (svc *CodeCoverage) ReportCallBack(req apistructs.CodeCoverageUpdateRequest) error {
	record, err := svc.db.GetCodeCoverageByID(req.ID)
	if err != nil {
		return err
	}
	reportStatus := apistructs.CodeCoverageExecStatus(req.Status)
	if reportStatus != apistructs.FailStatus && reportStatus != apistructs.SuccessStatus {
		return errors.New("the status is not fail or success")
	}
	if record.ReportStatus == apistructs.CancelStatus {
		return nil
	}

	record.ReportStatus = reportStatus
	record.ReportMsg = req.Msg
	record.ReportUrl = req.ReportTarUrl
	record.ReportTime = time.Now()

	return svc.db.UpdateCodeCoverage(record)
}

// ListCodeCoverageRecord list code coverage record
func (svc *CodeCoverage) ListCodeCoverageRecord(req apistructs.CodeCoverageListRequest) (data apistructs.CodeCoverageExecRecordData, err error) {
	var (
		records []dao.CodeCoverageExecRecordShort
		list    []apistructs.CodeCoverageExecRecordDto
		total   uint64
	)
	records, total, err = svc.db.ListCodeCoverage(req)
	if err != nil {
		return
	}
	for _, v := range records {
		list = append(list, v.Covert())
	}
	return apistructs.CodeCoverageExecRecordData{
		Total: total,
		List:  list,
	}, nil
}

// GetCodeCoverageRecord Get code coverage record
func (svc *CodeCoverage) GetCodeCoverageRecord(id uint64) (*apistructs.CodeCoverageExecRecordDto, error) {
	record, err := svc.db.GetCodeCoverageByID(id)
	if err != nil {
		return nil, err
	}
	return record.Covert(), nil
}

// JudgeRunningRecordExist Judge running record exist
func (svc *CodeCoverage) JudgeRunningRecordExist(projectID uint64, workspace string) error {
	records, err := svc.db.ListCodeCoverageByStatus(projectID, apistructs.WorkingStatus, workspace)
	if err != nil {
		return err
	}
	if len(records) > 0 {
		return errors.New("there is already running record")
	}
	return nil
}

func (svc *CodeCoverage) JudgeCanEnd(projectID uint64, workspace string) (bool, error) {
	records, err := svc.db.ListCodeCoverageByStatus(projectID, []apistructs.CodeCoverageExecStatus{apistructs.ReadyStatus}, workspace)
	if err != nil {
		return false, err
	}
	if len(records) > 0 {
		return true, nil
	}

	return false, nil
}

func (svc *CodeCoverage) JudgeSourcecovAddon(projectID uint64, orgID uint64, workspace string) (bool, error) {
	resp, err := svc.bdl.ListAddonByProjectID(int64(projectID), int64(orgID))
	if err != nil {
		return false, err
	}

	var find = false
	for _, addon := range resp.Data {
		if addon.AddonName != SourcecovAddonName || addon.Workspace != workspace {
			continue
		}
		if addon.AttachCount <= 0 {
			continue
		}
		find = true
	}

	if find {
		return true, nil
	}

	return false, nil
}

func (svc *CodeCoverage) GetCodeCoverageRecordStatus(projectID uint64, workspace string) (*apistructs.CodeCoverageExecRecordDetail, error) {
	var req = apistructs.CodeCoverageListRequest{
		PageSize:  1,
		PageNo:    1,
		ProjectID: projectID,
		Workspace: workspace,
	}
	records, _, err := svc.db.ListCodeCoverage(req)
	if err != nil {
		return nil, err
	}

	if len(records) <= 0 {
		return nil, fmt.Errorf("not have records")
	}

	setting, err := svc.db.GetCodeCoverageSettingByProjectID(projectID, workspace)
	if err != nil {
		return nil, err
	}

	var detail = apistructs.CodeCoverageExecRecordDetail{
		ProjectID: projectID,
		PlanID:    records[0].ID,
		Status:    records[0].Status.String(),
	}

	if setting != nil {
		detail.MavenSetting = setting.MavenSetting
		detail.Includes = setting.Includes
		detail.Excludes = setting.Excludes
	}

	return &detail, nil
}

func (svc *CodeCoverage) GetCodeCoverageSetting(projectID uint64, workspace string) (*apistructs.CodeCoverageSetting, error) {
	setting, err := svc.db.GetCodeCoverageSettingByProjectID(projectID, workspace)
	if err != nil {
		return nil, err
	}

	if setting == nil {
		return &apistructs.CodeCoverageSetting{}, nil
	}

	return &apistructs.CodeCoverageSetting{
		ID:           setting.ID,
		ProjectID:    setting.ProjectID,
		MavenSetting: setting.MavenSetting,
		Includes:     setting.Includes,
		Excludes:     setting.Excludes,
		Workspace:    setting.Workspace,
	}, nil
}

func (svc *CodeCoverage) SaveCodeCoverageSetting(saveSettingRequest apistructs.SaveCodeCoverageSettingRequest) (*apistructs.CodeCoverageSetting, error) {
	// check permission
	if !saveSettingRequest.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   saveSettingRequest.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  saveSettingRequest.ProjectID,
			Resource: "codeCoverage",
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErSaveCodeCoverageSetting.AccessDenied()
		}
	}

	list, err := svc.ListCodeCoverageRecord(apistructs.CodeCoverageListRequest{
		ProjectID: saveSettingRequest.ProjectID,
		PageNo:    1,
		PageSize:  1,
		Workspace: saveSettingRequest.Workspace,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to detect whether a plan is executing, error %v", err)
	}

	if len(list.List) > 0 {
		record := list.List[0]
		if record.Status == apistructs.RunningStatus.String() || record.Status == apistructs.ReadyStatus.String() {
			return nil, fmt.Errorf("there are plans was running")
		}

		if record.Status == apistructs.EndingStatus.String() {
			return nil, fmt.Errorf("there are plans was running")
		}
	}

	setting, err := svc.db.GetCodeCoverageSettingByProjectID(saveSettingRequest.ProjectID, saveSettingRequest.Workspace)
	if err != nil {
		return nil, err
	}
	if setting == nil {
		setting = &dao.CodeCoverageSetting{}
	}
	setting.Excludes = saveSettingRequest.Excludes
	setting.Includes = saveSettingRequest.Includes
	setting.MavenSetting = saveSettingRequest.MavenSetting
	if setting.ID <= 0 {
		setting.ProjectID = saveSettingRequest.ProjectID
		setting.Workspace = saveSettingRequest.Workspace
	}

	saveSetting, err := svc.db.SaveCodeCoverageSettingByProjectID(setting)
	if err != nil {
		return nil, err
	}

	return &apistructs.CodeCoverageSetting{
		ID:           saveSetting.ID,
		ProjectID:    saveSetting.ProjectID,
		MavenSetting: saveSetting.MavenSetting,
		Includes:     saveSetting.Includes,
		Excludes:     saveSetting.Excludes,
		Workspace:    saveSetting.Workspace,
	}, nil
}
