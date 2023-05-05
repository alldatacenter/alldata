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
	"fmt"
	"reflect"
	"sort"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	"github.com/erda-project/erda/internal/apps/dop/services/issue"
	"github.com/erda-project/erda/internal/apps/dop/services/issuestate"
	"github.com/erda-project/erda/internal/apps/dop/services/iteration"
	"github.com/erda-project/erda/internal/apps/dop/services/testcase"
	"github.com/erda-project/erda/internal/apps/dop/services/testset"
	"github.com/erda-project/erda/pkg/strutil"
)

// TestPlan
type TestPlan struct {
	db            *dao.DBClient
	bdl           *bundle.Bundle
	testCaseSvc   *testcase.Service
	testSetSvc    *testset.Service
	autotest      *autotest.Service
	issueSvc      *issue.Issue
	issueStateSvc *issuestate.IssueState
	iterationSvc  *iteration.Iteration
}

// Option
type Option func(*TestPlan)

// New Initialize TestPlan service
func New(options ...Option) *TestPlan {
	svc := &TestPlan{}
	for _, op := range options {
		op(svc)
	}
	return svc
}

// WithDBClient Set db client
func WithDBClient(db *dao.DBClient) Option {
	return func(svc *TestPlan) {
		svc.db = db
	}
}

// WithBundle Set bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(svc *TestPlan) {
		svc.bdl = bdl
	}
}

// WithTestCase Set testCaseSvc service
func WithTestCase(testCaseSvc *testcase.Service) Option {
	return func(svc *TestPlan) {
		svc.testCaseSvc = testCaseSvc
	}
}

// WithTestSet Set testSet service
func WithTestSet(testSetSvc *testset.Service) Option {
	return func(svc *TestPlan) {
		svc.testSetSvc = testSetSvc
	}
}

func WithAutoTest(autotest *autotest.Service) Option {
	return func(svc *TestPlan) {
		svc.autotest = autotest
	}
}

func WithIssue(issueSvc *issue.Issue) Option {
	return func(svc *TestPlan) {
		svc.issueSvc = issueSvc
	}
}

func WithIssueState(issueStateSvc *issuestate.IssueState) Option {
	return func(svc *TestPlan) {
		svc.issueStateSvc = issueStateSvc
	}
}

func WithIterationSvc(iterationSvc *iteration.Iteration) Option {
	return func(svc *TestPlan) {
		svc.iterationSvc = iterationSvc
	}
}

// Create 创建测试计划
func (t *TestPlan) Create(req apistructs.TestPlanCreateRequest) (uint64, error) {
	// req params check
	if req.Name == "" {
		return 0, apierrors.ErrCreateTestPlan.MissingParameter("name")
	}
	if req.OwnerID == "" {
		return 0, apierrors.ErrCreateTestPlan.MissingParameter("ownerID")
	}
	req.PartnerIDs = strutil.DedupSlice(req.PartnerIDs, true)
	if len(req.PartnerIDs) == 0 {
		return 0, apierrors.ErrCreateTestPlan.MissingParameter("partnerIDs")
	}
	if req.ProjectID == 0 {
		return 0, apierrors.ErrCreateTestPlan.MissingParameter("projectID")
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := t.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectID,
			Resource: apistructs.TestPlanResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrCreateTestPlan.AccessDenied()
		}
	}

	// create testplan
	testPlan := dao.TestPlan{
		Name:        req.Name,
		ProjectID:   req.ProjectID,
		IterationID: req.IterationID,
		Status:      apistructs.TPStatusDoing,
		CreatorID:   req.UserID,
		UpdaterID:   req.UserID,
		Type:        apistructs.TestPlanTypeManual,
	}
	// 自动化测试计划
	if req.IsAutoTest {
		testPlan.Type = apistructs.TestPlanTypeAutoTest
		// 默认生成 inode
		inode, err := t.ensureAutoTestPlanInode(testPlan.ProjectID, testPlan.Name, req.IdentityInfo)
		if err != nil {
			return 0, apierrors.ErrCreateTestPlan.InternalError(err)
		}
		testPlan.Inode = inode
	}
	if err := t.db.CreateTestPlan(&testPlan); err != nil {
		return 0, apierrors.ErrCreateTestPlan.InternalError(err)
	}

	// create member
	var members []dao.TestPlanMember
	members = append(members, dao.TestPlanMember{
		TestPlanID: uint64(testPlan.ID),
		Role:       apistructs.TestPlanMemberRoleOwner,
		UserID:     req.OwnerID,
	})
	for _, partnerID := range req.PartnerIDs {
		members = append(members, dao.TestPlanMember{
			TestPlanID: uint64(testPlan.ID),
			Role:       apistructs.TestPlanMemberRolePartner,
			UserID:     partnerID,
		})
	}
	if err := t.db.BatchCreateTestPlanMembers(members); err != nil {
		return 0, apierrors.ErrCreateTestPlanMember.InternalError(err)
	}

	return uint64(testPlan.ID), nil
}

// Update 更新测试计划
func (t *TestPlan) Update(req apistructs.TestPlanUpdateRequest) error {
	// 参数校验
	req.PartnerIDs = strutil.DedupSlice(req.PartnerIDs, true)
	if req.Status != "" {
		if !req.Status.Valid() {
			return apierrors.ErrUpdateTestPlan.InvalidParameter(fmt.Sprintf("status: %s", req.Status))
		}
	}

	// 查询测试计划
	testPlan, err := t.db.GetTestPlan(req.TestPlanID)
	if err != nil {
		return apierrors.ErrUpdateTestPlan.InternalError(err)
	}
	if testPlan == nil {
		return apierrors.ErrUpdateTestPlan.NotFound()
	}

	// 更新测试计划
	if req.Name != "" {
		testPlan.Name = req.Name
	}
	if req.Status != "" {
		testPlan.Status = req.Status
	}
	if req.Summary != "" {
		testPlan.Summary = req.Summary
	}
	if req.TimestampSecStartedAt != nil {
		t := time.Unix(int64(*req.TimestampSecStartedAt), 0)
		testPlan.StartedAt = &t
	}
	if req.TimestampSecEndedAt != nil {
		t := time.Unix(int64(*req.TimestampSecEndedAt), 0)
		testPlan.EndedAt = &t
	}

	var isUpdateArchive bool
	if req.IsArchived != nil {
		if &testPlan.IsArchived != req.IsArchived {
			isUpdateArchive = true
		}
		testPlan.IsArchived = *req.IsArchived
	} else {
		testPlan.IterationID = req.IterationID
	}
	if err := t.db.UpdateTestPlan(testPlan); err != nil {
		return apierrors.ErrUpdateTestPlan.InternalError(err)
	}

	if req.OwnerID != "" || len(req.PartnerIDs) > 0 {
		members, err := t.db.ListTestPlanMembersByPlanID(req.TestPlanID)
		if err != nil {
			return apierrors.ErrListTestPlanMembers.InternalError(err)
		}
		var currentOwnerID string
		var currentPartnerIDs []string
		for _, mem := range members {
			if mem.Role.IsOwner() {
				currentOwnerID = mem.UserID
			}
			if mem.Role.IsPartner() {
				currentPartnerIDs = append(currentPartnerIDs, mem.UserID)
			}
		}

		// ownerID 不同时需要更新
		if req.OwnerID != "" && req.OwnerID != currentOwnerID {
			if err := t.db.OverwriteTestPlanOwner(req.TestPlanID, req.OwnerID); err != nil {
				return apierrors.ErrUpdateTestPlanMember.InternalError(err)
			}
		}

		// partnerIDs 不同时需要更新
		if len(req.PartnerIDs) > 0 {
			sort.Strings(currentPartnerIDs)
			sort.Strings(req.PartnerIDs)
			if !reflect.DeepEqual(currentPartnerIDs, req.PartnerIDs) {
				if err := t.db.OverwriteTestPlanPartners(req.TestPlanID, req.PartnerIDs); err != nil {
					return apierrors.ErrUpdateTestPlanMember.InternalError(err)
				}
			}
		}
	}

	if isUpdateArchive {
		return t.createAudit(testPlan, req)
	}
	return nil
}

func (t *TestPlan) createAudit(testPlan *dao.TestPlan, req apistructs.TestPlanUpdateRequest) error {
	now := strconv.FormatInt(time.Now().Unix(), 10)
	project, err := t.bdl.GetProject(testPlan.ProjectID)
	if err != nil {
		return err
	}
	audit := apistructs.Audit{
		UserID:       req.UserID,
		ScopeType:    apistructs.ProjectScope,
		ScopeID:      testPlan.ProjectID,
		OrgID:        project.OrgID,
		ProjectID:    testPlan.ProjectID,
		Result:       "success",
		StartTime:    now,
		EndTime:      now,
		TemplateName: apistructs.ArchiveTestplanTemplate,
		Context: map[string]interface{}{
			"projectName":  project.Name,
			"testPlanName": testPlan.Name,
		},
	}
	if !*req.IsArchived {
		audit.TemplateName = apistructs.UnarchiveTestPlanTemplate
	}
	return t.bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{
		Audit: audit,
	})
}

// Delete
func (t *TestPlan) Delete(identityInfo apistructs.IdentityInfo, testPlanID uint64) error {
	testPlan, err := t.db.GetTestPlan(testPlanID)
	if err != nil {
		return err
	}
	if testPlan == nil {
		return apierrors.ErrDeleteTestPlan.NotFound()
	}

	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := t.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanResource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrDeleteTestPlan.AccessDenied()
		}
	}

	// TODO 解除用例关联关系

	return t.db.DeleteTestPlan(testPlanID)
}

// Get 测试计划详情
func (t *TestPlan) Get(testPlanID uint64) (*apistructs.TestPlan, error) {
	testPlan, err := t.db.GetTestPlan(testPlanID)
	if err != nil {
		return nil, apierrors.ErrGetTestPlan.InternalError(err)
	}
	if testPlan == nil {
		return nil, apierrors.ErrGetTestPlan.NotFound()
	}
	// list member
	members, err := t.db.ListTestPlanMembersByPlanID(testPlanID)
	if err != nil {
		return nil, apierrors.ErrGetTestPlan.InternalError(err)
	}
	// list rels
	countMap, err := t.db.ListTestPlanCaseRelsCount([]uint64{testPlanID})
	if err != nil {
		return nil, apierrors.ErrGetTestPlan.InternalError(err)
	}

	convertedTp := t.Convert(testPlan, countMap[testPlanID], members...)

	if testPlan.IterationID != 0 {
		iterationDao, err := t.iterationSvc.Get(testPlan.IterationID)
		if err == nil {
			convertedTp.IterationName = iterationDao.Title
		}
	}

	return &convertedTp, nil
}

// List 测试计划分页查询
func (t *TestPlan) Paging(req apistructs.TestPlanPagingRequest) (*apistructs.TestPlanPagingResponseData, error) {
	// 参数校验
	if req.ProjectID == 0 {
		return nil, apierrors.ErrPagingTestPlans.MissingParameter("projectID")
	}
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}
	for _, status := range req.Statuses {
		if !status.Valid() {
			return nil, apierrors.ErrPagingTestPlans.InvalidParameter(fmt.Errorf("status: %s", status))
		}
	}

	// paging testplan
	total, list, err := t.db.PagingTestPlan(req)
	if err != nil {
		return nil, apierrors.ErrPagingTestPlans.InternalError(err)
	}

	// list relation partners
	var tpIDs []uint64
	for _, tp := range list {
		tpIDs = append(tpIDs, uint64(tp.ID))
	}
	memberMap, err := t.db.ListTestPlanMembersByPlanIDs(tpIDs)
	if err != nil {
		return nil, apierrors.ErrListTestPlanMembers.InternalError(err)
	}

	// list rels count
	relCountMap, err := t.db.ListTestPlanCaseRelsCount(tpIDs)
	if err != nil {
		return nil, apierrors.ErrPagingTestPlanCaseRels.InternalError(err)
	}

	testPlans := make([]apistructs.TestPlan, 0, len(list))
	var userIDs []string
	var iterationIDs []uint64
	for _, tp := range list {
		convertedTp := t.Convert(&tp, relCountMap[uint64(tp.ID)], memberMap[uint64(tp.ID)]...)
		testPlans = append(testPlans, convertedTp)
		userIDs = append(append(userIDs, convertedTp.OwnerID, convertedTp.CreatorID, convertedTp.UpdaterID), convertedTp.PartnerIDs...)
		iterationIDs = append(iterationIDs, tp.IterationID)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	iterationIDs = strutil.DedupUint64Slice(iterationIDs, true)

	iterationMap := make(map[uint64]string, len(iterationIDs))
	if len(iterationIDs) > 0 {
		iterations, _, err := t.iterationSvc.Paging(apistructs.IterationPagingRequest{
			PageNo:              1,
			PageSize:            999,
			ProjectID:           req.ProjectID,
			WithoutIssueSummary: true,
			IDs:                 iterationIDs,
		})
		if err != nil {
			return nil, apierrors.ErrPagingTestPlanCaseRels.InternalError(err)
		}
		for _, v := range iterations {
			iterationMap[v.ID] = v.Title
		}
	}

	for i := range testPlans {
		if testPlans[i].IterationID != 0 {
			testPlans[i].IterationName = iterationMap[testPlans[i].IterationID]
		}
	}

	return &apistructs.TestPlanPagingResponseData{
		Total:   total,
		List:    testPlans,
		UserIDs: userIDs,
	}, nil
}

// PagingTestPlanCaseRels 分页查询测试计划内测试用例
func (t *TestPlan) PagingTestPlanCaseRels(req apistructs.TestPlanCaseRelPagingRequest) (*apistructs.TestPlanCasePagingResponseData, error) {
	// query testplan if not queried yet
	if req.TestPlan == nil {
		_tp, err := t.Get(req.TestPlanID)
		if err != nil {
			return nil, err
		}
		req.TestPlan = _tp
	}

	// get paged planCaseRels
	rels, total, err := t.db.PagingTestPlanCaseRelations(req)
	if err != nil {
		return nil, err
	}

	if total == 0 {
		return &apistructs.TestPlanCasePagingResponseData{}, nil
	}

	// get testsets from paged testcases
	testSets, err := t.db.ListTestSets(apistructs.TestSetListRequest{
		Recycled:  false,
		ProjectID: &req.TestPlan.ProjectID,
		TestSetIDs: func() (tsIDs []uint64) {
			for _, rel := range rels {
				tsIDs = append(tsIDs, rel.TestSetID)
			}
			return
		}(),
		NoSubTestSets: true,
	})
	if err != nil {
		return nil, apierrors.ErrPagingTestPlanCaseRels.InternalError(err)
	}

	// list(testCase) -> list(testSet with testCases)
	mapOfTestSetIDAndDir := make(map[uint64]string)
	for _, ts := range testSets {
		mapOfTestSetIDAndDir[ts.ID] = ts.Directory
	}
	resultTestSetMap := make(map[uint64]apistructs.TestSetWithPlanCaseRels)
	var testSetIDOrdered []uint64

	// map: ts.ID -> TestSetWithCases ([]tc)
	for _, rel := range rels {
		// order by testSetID
		if _, ok := resultTestSetMap[rel.TestSetID]; !ok {
			testSetIDOrdered = append(testSetIDOrdered, rel.TestSetID)
		}
		// fulfill testSetWithCase
		tmp := resultTestSetMap[rel.TestSetID]
		tmp.Directory = mapOfTestSetIDAndDir[rel.TestSetID]
		tmp.TestSetID = rel.TestSetID
		tmp.TestCases = append(tmp.TestCases, rel.ConvertForPaging())
		resultTestSetMap[rel.TestSetID] = tmp
	}
	resultTestSets := make([]apistructs.TestSetWithPlanCaseRels, 0)
	for _, tsID := range testSetIDOrdered {
		if ts, ok := resultTestSetMap[tsID]; ok {
			resultTestSets = append(resultTestSets, ts)
		}
	}

	// get all user ids
	var allUserIDs []string
	for _, ts := range resultTestSets {
		for _, rel := range ts.TestCases {
			allUserIDs = append(allUserIDs, rel.CreatorID, rel.UpdaterID, rel.ExecutorID)
		}
	}
	allUserIDs = strutil.DedupSlice(allUserIDs, true)

	// result
	result := apistructs.TestPlanCasePagingResponseData{
		Total:    total,
		TestSets: resultTestSets,
		UserIDs:  allUserIDs,
	}

	return &result, nil
}

func (t *TestPlan) ListTestPlanCaseRels(req apistructs.TestPlanCaseRelListRequest) (rels []apistructs.TestPlanCaseRel, err error) {
	defer func() {
		if r := recover(); r != nil {
			logrus.Errorf("failed to list test plan case rels, err: %v", r)
			err = apierrors.ErrPagingTestPlanCaseRels.InternalError(fmt.Errorf("invalid request"))
		}
	}()
	dbRels, err := t.db.ListTestPlanCaseRels(req)
	if err != nil {
		return nil, apierrors.ErrPagingTestPlanCaseRels.InternalError(err)
	}
	// get testcases
	tcMap := make(map[uint64]*apistructs.TestCase)
	if !req.IDOnly {
		var tcIDs []uint64
		for _, dbRel := range dbRels {
			tcIDs = append(tcIDs, dbRel.TestCaseID)
		}
		tcIDs = strutil.DedupUint64Slice(tcIDs, true)
		tcs, _, err := t.testCaseSvc.ListTestCases(apistructs.TestCaseListRequest{
			IDs:                   tcIDs,
			AllowMissingProjectID: true,
			AllowEmptyTestSetIDs:  true,
			Recycled:              false,
			IDOnly:                false,
		})
		if err != nil {
			return nil, err
		}
		if len(tcs) == 0 {
			return rels, nil
		}
		for i := range tcs {
			tcMap[tcs[i].ID] = &tcs[i]
		}
	}
	// assign tc to rel
	for _, dbRel := range dbRels {
		rel := t.ConvertRel(&dbRel, tcMap[dbRel.TestCaseID])
		rels = append(rels, *rel)
	}
	return rels, nil
}

// ExecuteAPITest 执行接口测试
func (t *TestPlan) ExecuteAPITest(req apistructs.TestPlanAPITestExecuteRequest) (uint64, error) {
	if req.TestPlanID == 0 {
		return 0, apierrors.ErrTestPlanExecuteAPITest.MissingParameter("testPlanID")
	}
	//if req.EnvID == 0 {
	//	return 0, apierrors.ErrTestPlanExecuteAPITest.MissingParameter("envID")
	//}

	// query test plan
	tp, err := t.Get(req.TestPlanID)
	if err != nil {
		return 0, err
	}

	// 获取测试用例列表
	if len(req.TestCaseIDs) == 0 {
		rels, err := t.db.ListTestPlanCaseRels(apistructs.TestPlanCaseRelListRequest{
			TestPlanIDs:  []uint64{tp.ID},
			IDOnly:       false,
			IdentityInfo: req.IdentityInfo,
		})
		if err != nil {
			return 0, apierrors.ErrTestPlanExecuteAPITest.InternalError(err)
		}
		for _, rel := range rels {
			req.TestCaseIDs = append(req.TestCaseIDs, rel.TestCaseID)
		}
	}

	// 调用 qa api 执行 api 测试
	qaAPITestReq := apistructs.ApiTestsActionRequest{
		ProjectID:        int64(tp.ProjectID),
		TestPlanID:       int64(req.TestPlanID),
		ProjectTestEnvID: int64(req.EnvID),
		UsecaseIDs:       req.TestCaseIDs,
	}

	return t.testCaseSvc.ExecuteAPIs(qaAPITestReq)
}

// Convert
func (t *TestPlan) Convert(testPlan *dao.TestPlan, relsCount apistructs.TestPlanRelsCount, members ...dao.TestPlanMember) apistructs.TestPlan {
	result := apistructs.TestPlan{
		ID:          uint64(testPlan.ID),
		Name:        testPlan.Name,
		Status:      testPlan.Status,
		ProjectID:   testPlan.ProjectID,
		CreatorID:   testPlan.CreatorID,
		UpdaterID:   testPlan.UpdaterID,
		CreatedAt:   &testPlan.CreatedAt,
		UpdatedAt:   &testPlan.UpdatedAt,
		Summary:     testPlan.Summary,
		StartedAt:   testPlan.StartedAt,
		EndedAt:     testPlan.EndedAt,
		RelsCount:   relsCount,
		Type:        testPlan.Type,
		Inode:       testPlan.Inode,
		IsArchived:  testPlan.IsArchived,
		IterationID: testPlan.IterationID,
	}
	for _, mem := range members {
		if mem.Role.IsOwner() {
			result.OwnerID = mem.UserID
		}
		if mem.Role.IsPartner() {
			result.PartnerIDs = append(result.PartnerIDs, mem.UserID)
		}
	}
	return result
}

// ensureAutoTestPlanInode 保证创建 autotest 计划的节点
// 测试计划也使用目录树进行保存，根目录下直接挂该项目下的叶子节点，即计划的 pipeline
func (t *TestPlan) ensureAutoTestPlanInode(projectID uint64, planName string, identityInfo apistructs.IdentityInfo) (string, error) {
	scopeID := strconv.FormatUint(projectID, 10)
	// 查询根目录，若根节点不存在，则自动创建
	rootNodes, err := t.autotest.ListFileTreeNodes(apistructs.UnifiedFileTreeNodeListRequest{
		Scope:        string(apistructs.AutoTestsScopeProjectTestPlan),
		ScopeID:      scopeID,
		Pinode:       "0",
		IdentityInfo: identityInfo,
	})
	if err != nil {
		return "", err
	}
	// 获取根节点
	var rootNodeInode string
	if len(rootNodes) > 0 {
		rootNodeInode = rootNodes[0].Inode
	} else {
		// 创建根目录
		newRootNode, err := t.autotest.CreateFileTreeNode(apistructs.UnifiedFileTreeNodeCreateRequest{
			Type:         apistructs.UnifiedFileTreeNodeTypeDir,
			Scope:        string(apistructs.AutoTestsScopeProjectTestPlan),
			ScopeID:      scopeID,
			Name:         "testplan-root-dir",
			IdentityInfo: identityInfo,
		})
		if err != nil {
			return "", err
		}
		rootNodeInode = newRootNode.Inode
	}
	// 创建子文件节点
	planNode, err := t.autotest.CreateFileTreeNode(apistructs.UnifiedFileTreeNodeCreateRequest{
		Type:         apistructs.UnifiedFileTreeNodeTypeFile,
		Scope:        string(apistructs.AutoTestsScopeProjectTestPlan),
		ScopeID:      scopeID,
		Pinode:       rootNodeInode,
		Name:         planName,
		IdentityInfo: identityInfo,
	})
	if err != nil {
		return "", err
	}
	return planNode.Inode, nil
}
