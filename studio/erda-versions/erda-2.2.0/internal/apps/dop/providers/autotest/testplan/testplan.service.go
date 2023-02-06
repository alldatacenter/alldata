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

package testplan

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/dop/autotest/testplan/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/autotest/testplan/db"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/pkg/time/mysql_time"
)

type TestPlanService struct {
	p   *provider
	db  db.TestPlanDB
	bdl *bundle.Bundle

	autoTestSvc *autotestv2.Service
}

func (s *TestPlanService) WithAutoTestSvc(sv *autotestv2.Service) {
	s.autoTestSvc = sv
}

func (s *TestPlanService) UpdateTestPlanByHook(ctx context.Context, req *pb.TestPlanUpdateByHookRequest) (*pb.TestPlanUpdateByHookResponse, error) {
	logrus.Info("start testplan execute callback")
	apiTotalNum, err := s.calcTotalApiNum(req)
	if err != nil {
		logrus.Errorf("failed to calcTotalApiNum,err: %s", err.Error())
	}
	req.Content.ApiTotalNum = apiTotalNum
	// not include ref set
	req.Content.ApiExecNum = req.Content.ApiExecNum - req.Content.ApiRefExecNum
	req.Content.ApiSuccessNum = req.Content.ApiSuccessNum - req.Content.ApiRefSuccessNum
	req.Content.PassRate = calcRate(req.Content.ApiSuccessNum, req.Content.ApiTotalNum)
	req.Content.ExecuteRate = calcRate(req.Content.ApiExecNum, req.Content.ApiTotalNum)

	if req.Content.StepAPIType == apistructs.AutoTestPlan {
		if req.Content.TestPlanID == 0 {
			return nil, apierrors.ErrUpdateTestPlan.MissingParameter("testPlanID")
		}
		req.Content.ExecuteDuration = getCostTime(req.Content.CostTimeSec)

		go func() {
			err := s.ProcessEvent(req.Content)
			if err != nil {
				logrus.Errorf("failed to ProcessEvent, err: %s", err.Error())
			}
		}()

		fields := make(map[string]interface{}, 0)
		fields["execute_time"] = parseExecuteTime(req.Content.ExecuteTime)
		fields["execute_api_num"] = req.Content.ApiExecNum
		fields["success_api_num"] = req.Content.ApiSuccessNum
		fields["total_api_num"] = req.Content.ApiTotalNum
		fields["cost_time_sec"] = req.Content.CostTimeSec
		fields["execute_rate"] = req.Content.ExecuteRate
		fields["pass_rate"] = req.Content.PassRate

		if err = s.db.UpdateTestPlanV2(req.Content.TestPlanID, fields); err != nil {
			return nil, err
		}
	}

	// scene has api exec content
	if req.Content.StepAPIType == apistructs.StepTypeScene.String() {
		if err = s.BatchCreateTestPlanExecHistory(req); err != nil {
			return nil, err
		}
	} else {
		if err = s.createTestPlanExecHistory(req); err != nil {
			return nil, err
		}
	}

	return &pb.TestPlanUpdateByHookResponse{Data: req.Content.TestPlanID}, nil
}

func (s *TestPlanService) BatchCreateTestPlanExecHistory(req *pb.TestPlanUpdateByHookRequest) error {
	contents := make([]*pb.Content, 0)
	contents = append(contents, req.Content)
	for _, v := range req.Content.SubContents {
		contents = append(contents, v)
	}

	testPlan, err := s.db.GetTestPlan(req.Content.TestPlanID)
	if err != nil {
		return err
	}
	iterationID := req.Content.IterationID
	if iterationID == 0 {
		iterationID = testPlan.IterationID
	}
	project, err := s.bdl.GetProject(testPlan.ProjectID)
	if err != nil {
		return err
	}

	execHistories := make([]db.AutoTestExecHistory, 0)
	for _, v := range contents {
		if v.StepAPIType != apistructs.StepTypeScene.String() {
			v.ApiTotalNum = 1
			v.ApiExecNum = 1
			v.PassRate = calcRate(v.ApiSuccessNum, v.ApiTotalNum)
			v.ExecuteRate = calcRate(v.ApiExecNum, v.ApiTotalNum)
		}

		executeTime := parseExecuteTime(v.ExecuteTime)
		if executeTime == nil {
			executeTime = mysql_time.GetMysqlDefaultTime()
		}

		timeBegin := parseExecuteTime(v.TimeBegin)
		if timeBegin == nil {
			timeBegin = mysql_time.GetMysqlDefaultTime()
		}
		timeEnd := parseExecuteTime(v.TimeEnd)
		if timeEnd == nil {
			timeEnd = mysql_time.GetMysqlDefaultTime()
		}
		execHistory := db.AutoTestExecHistory{
			CreatorID:     v.CreatorID,
			ProjectID:     testPlan.ProjectID,
			SpaceID:       testPlan.SpaceID,
			IterationID:   iterationID,
			PlanID:        v.TestPlanID,
			SceneID:       v.SceneID,
			SceneSetID:    v.SceneSetID,
			StepID:        v.StepID,
			ParentPID:     v.ParentID,
			Type:          apistructs.StepAPIType(v.StepAPIType),
			Status:        apistructs.PipelineStatus(v.Status),
			PipelineYml:   v.PipelineYml,
			ExecuteApiNum: v.ApiExecNum,
			SuccessApiNum: v.ApiSuccessNum,
			PassRate:      v.PassRate,
			ExecuteRate:   v.ExecuteRate,
			TotalApiNum:   v.ApiTotalNum,
			ExecuteTime:   *executeTime,
			CostTimeSec:   v.CostTimeSec,
			OrgID:         project.OrgID,
			TimeBegin:     *timeBegin,
			TimeEnd:       *timeEnd,
			PipelineID:    v.PipelineID,
		}
		execHistories = append(execHistories, execHistory)
	}
	return s.db.BatchCreateAutoTestExecHistory(execHistories)
}

func (s *TestPlanService) createTestPlanExecHistory(req *pb.TestPlanUpdateByHookRequest) error {
	testPlan, err := s.db.GetTestPlan(req.Content.TestPlanID)
	if err != nil {
		return err
	}
	iterationID := req.Content.IterationID
	if iterationID == 0 {
		iterationID = testPlan.IterationID
	}
	executeTime := parseExecuteTime(req.Content.ExecuteTime)
	if executeTime == nil {
		executeTime = mysql_time.GetMysqlDefaultTime()
	}

	timeBegin := parseExecuteTime(req.Content.TimeBegin)
	if timeBegin == nil {
		timeBegin = mysql_time.GetMysqlDefaultTime()
	}
	timeEnd := parseExecuteTime(req.Content.TimeEnd)
	if timeEnd == nil {
		timeEnd = mysql_time.GetMysqlDefaultTime()
	}

	project, err := s.bdl.GetProject(testPlan.ProjectID)
	if err != nil {
		return err
	}

	execHistory := db.AutoTestExecHistory{
		CreatorID:     req.Content.CreatorID,
		ProjectID:     testPlan.ProjectID,
		SpaceID:       testPlan.SpaceID,
		IterationID:   iterationID,
		PlanID:        req.Content.TestPlanID,
		SceneID:       req.Content.SceneID,
		SceneSetID:    req.Content.SceneSetID,
		StepID:        req.Content.StepID,
		ParentPID:     req.Content.ParentID,
		Type:          apistructs.StepAPIType(req.Content.StepAPIType),
		Status:        apistructs.PipelineStatus(req.Content.Status),
		PipelineYml:   req.Content.PipelineYml,
		ExecuteApiNum: req.Content.ApiExecNum,
		SuccessApiNum: req.Content.ApiSuccessNum,
		PassRate:      req.Content.PassRate,
		ExecuteRate:   req.Content.ExecuteRate,
		TotalApiNum:   req.Content.ApiTotalNum,
		ExecuteTime:   *executeTime,
		CostTimeSec:   req.Content.CostTimeSec,
		OrgID:         project.OrgID,
		TimeBegin:     *timeBegin,
		TimeEnd:       *timeEnd,
		PipelineID:    req.Content.PipelineID,
	}
	return s.db.CreateAutoTestExecHistory(&execHistory)
}

// parseExecuteTime parse string to time, if err return nil
func parseExecuteTime(value string) *time.Time {
	t, err := time.ParseInLocation("2006-01-02 15:04:05", value, time.Local)
	if err != nil {
		logrus.Errorf("failed to parse ExecuteTime,err: %s", err.Error())
		return nil
	}
	return &t
}

func (s *TestPlanService) ProcessEvent(req *pb.Content) error {
	eventName := "autotest-plan-execute"

	testPlan, err := s.autoTestSvc.GetTestPlanV2(req.TestPlanID, apistructs.IdentityInfo{
		InternalClient: "dop",
	})
	if err != nil {
		return err
	}
	project, err := s.bdl.GetProject(testPlan.ProjectID)
	if err != nil {
		return err
	}
	org, err := s.bdl.GetOrg(project.OrgID)
	if err != nil {
		return err
	}
	orgID := strconv.FormatUint(project.OrgID, 10)
	projectID := strconv.FormatUint(project.ID, 10)
	notifyDetails, err := s.bdl.QueryNotifiesBySource(orgID, "project", projectID, eventName, "")

	for _, notifyDetail := range notifyDetails {
		if notifyDetail.NotifyGroup == nil {
			continue
		}
		notifyItem := notifyDetail.NotifyItems[0]
		params := map[string]string{
			"org_name":         org.Name,
			"project_name":     project.Name,
			"plan_name":        testPlan.Name,
			"pass_rate":        fmt.Sprintf("%.2f", req.PassRate),
			"execute_duration": req.ExecuteDuration,
			"api_total_num":    fmt.Sprintf("%d", req.ApiTotalNum),
		}
		marshal, _ := json.Marshal(params)
		logrus.Debugf("testplan params :%s", string(marshal))

		eventboxReqContent := apistructs.GroupNotifyContent{
			SourceName:            "",
			SourceType:            "project",
			SourceID:              projectID,
			NotifyName:            notifyDetail.Name,
			NotifyItemDisplayName: notifyItem.DisplayName,
			Channels:              []apistructs.GroupNotifyChannel{},
			Label:                 notifyItem.Label,
			CalledShowNumber:      notifyItem.CalledShowNumber,
			OrgID:                 int64(project.OrgID),
		}

		err = s.bdl.CreateGroupNotifyEvent(apistructs.EventBoxGroupNotifyRequest{
			Sender:        "adapter",
			GroupID:       notifyDetail.NotifyGroup.ID,
			Channels:      notifyDetail.Channels,
			NotifyItem:    notifyItem,
			NotifyContent: &eventboxReqContent,
			Params:        params,
		})
		if err != nil {
			return err
		}
	}
	return nil
}

func convertUTCTime(tm string) (time.Time, error) {
	executeTime, err := time.Parse("2006-01-02 15:04:05", tm)
	if err != nil {
		return time.Time{}, err
	}
	// executeTime is UTC
	m, err := time.ParseDuration("-8h")
	if err != nil {
		return time.Time{}, err
	}
	return executeTime.Add(m), nil
}

// getCostTime the format of time is "00:00:00"
// id is not end status or err return "-"
func getCostTime(costTimeSec int64) string {
	if costTimeSec < 0 {
		return "-"
	}
	return time.Unix(costTimeSec, 0).In(time.UTC).Format("15:04:05")
}

func (s *TestPlanService) calcTotalApiNum(req *pb.TestPlanUpdateByHookRequest) (int64, error) {
	switch req.Content.StepAPIType {
	case apistructs.AutoTestPlan:
		planSteps, err := s.db.ListTestPlanByPlanID(req.Content.TestPlanID)
		if err != nil {
			return 0, err
		}
		sceneIDs := s.getSceneIDsNotIncludeRef(func() []uint64 {
			setIDs := make([]uint64, 0, len(planSteps))
			for _, v := range planSteps {
				setIDs = append(setIDs, v.SceneSetID)
			}
			return setIDs
		}()...)
		return s.countApiBySceneIDRepeat(sceneIDs...)
	case apistructs.AutotestSceneSet:
		sceneIDs := s.getSceneIDsNotIncludeRef(req.Content.SceneSetID)
		return s.countApiBySceneIDRepeat(sceneIDs...)
	case apistructs.StepTypeScene.String():
		return s.countApiBySceneIDRepeat(req.Content.SceneID)
	case apistructs.StepTypeAPI.String(), apistructs.StepTypeWait.String(),
		apistructs.StepTypeCustomScript.String(), apistructs.StepTypeConfigSheet.String():
		return 1, nil
	}
	return 0, nil
}

func (s *TestPlanService) countApiBySceneIDRepeat(sceneID ...uint64) (total int64, err error) {
	apiCounts, err := s.db.CountApiBySceneID(sceneID...)
	if err != nil {
		return 0, err
	}
	apiCountMap := make(map[uint64]int64)
	for _, v := range apiCounts {
		apiCountMap[v.SceneID] = v.Count
	}
	for _, v := range sceneID {
		total += apiCountMap[v]
	}
	return
}

func (s *TestPlanService) getSceneIDsNotIncludeRef(setID ...uint64) (sceneIDs []uint64) {
	setIDCountMap := make(map[uint64]int)           // key: setID, value: the count of setID
	sceneMap := make(map[uint64][]db.AutoTestScene) // key: setID, value: []db.AutoTestScene

	for _, v := range setID {
		setIDCountMap[v] = setIDCountMap[v] + 1
	}
	scenes, err := s.db.ListSceneBySceneSetID(setID...)
	if err != nil {
		return
	}
	for _, v := range scenes {
		sceneMap[v.SetID] = append(sceneMap[v.SetID], v)
	}
	for k, v := range setIDCountMap {
		if v > 1 {
			for i := 0; i < v-1; i++ {
				scenes = append(scenes, sceneMap[k]...)
			}
		}
	}

	for _, v := range scenes {
		// not include reference
		if v.RefSetID != 0 {
			continue
		}
		sceneIDs = append(sceneIDs, v.ID)
	}
	return
}

func (s *TestPlanService) getSceneIDsIncludeRef(setRefMap map[uint64]uint64, setID ...uint64) (sceneIDs []uint64) {
	setIDCountMap := make(map[uint64]int)           // key: setID, value: the count of setID
	sceneSetMap := make(map[uint64]uint64)          // key: sceneID, value: setID
	sceneMap := make(map[uint64][]db.AutoTestScene) // key: setID, value: []db.AutoTestScene

	for _, v := range setID {
		setIDCountMap[v] = setIDCountMap[v] + 1
	}
	scenes, err := s.db.ListSceneBySceneSetID(setID...)
	if err != nil {
		return
	}
	for _, v := range scenes {
		sceneMap[v.SetID] = append(sceneMap[v.SetID], v)
		sceneSetMap[v.ID] = v.SetID
	}
	for k, v := range setIDCountMap {
		if v > 1 {
			for i := 0; i < v-1; i++ {
				scenes = append(scenes, sceneMap[k]...)
			}
		}
	}

	for _, v := range scenes {
		if v.RefSetID == 0 {
			sceneIDs = append(sceneIDs, v.ID)
			continue
		}
		// check if has circular reference
		// 1. reference itself
		if v.RefSetID == sceneSetMap[v.ID] {
			return
		}
		// 2. circular reference
		if setRefMap[v.RefSetID] == sceneSetMap[v.ID] {
			return
		}
		setRefMap[sceneSetMap[v.ID]] = v.RefSetID

		ids := s.getSceneIDsIncludeRef(setRefMap, v.RefSetID)
		sceneIDs = append(sceneIDs, ids...)
	}
	return
}

func calcRate(num, totalNum int64) float64 {
	if totalNum == 0 {
		return 0
	}
	return float64(num) / float64(totalNum) * 100
}
