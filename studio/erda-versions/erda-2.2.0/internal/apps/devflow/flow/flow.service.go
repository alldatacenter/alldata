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

package flow

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"path/filepath"
	"sort"
	"strconv"
	"strings"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/apps/devflow/flow/pb"
	issuerelationpb "github.com/erda-project/erda-proto-go/apps/devflow/issuerelation/pb"
	rulepb "github.com/erda-project/erda-proto-go/dop/devflowrule/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/devflowrule"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/apps/dop/services/filetree"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const (
	MultiBranchFlowType  = "multi_branch"
	SingleBranchFlowType = "single_branch"
)

type Service struct {
	p             *provider
	fileTree      *filetree.GittarFileTree
	branchRuleSvc *branchrule.BranchRule
	permission    *permission.Permission
}

func (p *Service) WithGittarFileTree(fileTree *filetree.GittarFileTree) {
	if p == nil {
		return
	}
	p.fileTree = fileTree
}

func (p *Service) WithBranchRule(branchRuleSvc *branchrule.BranchRule) {
	if p == nil {
		return
	}
	p.branchRuleSvc = branchRuleSvc
}

func (p *Service) WithPermission(permission *permission.Permission) {
	if p == nil {
		return
	}
	p.permission = permission
}

const issueRelationType = "devflow"
const gittarPrefixOpenApi = "/wb/"

const mrOpenState = "open"
const mrMergedState = "merged"

func (s *Service) CreateFlowNode(ctx context.Context, req *pb.CreateFlowNodeRequest) (*pb.CreateFlowNodeResponse, error) {
	app, err := s.p.bdl.GetApp(req.AppID)
	if err != nil {
		return nil, err
	}

	var repoPath = gittarPrefixOpenApi + app.ProjectName + "/" + app.Name
	exists, err := s.JudgeBranchIsExists(ctx, repoPath, req.SourceBranch)
	if err != nil {
		return nil, err
	}
	// auto create sourceBranch
	if !exists {
		err := s.p.bdl.CreateGittarBranch(repoPath, apistructs.GittarCreateBranchRequest{
			Name: req.SourceBranch,
			Ref:  req.TargetBranch,
		}, apis.GetOrgID(ctx), apis.GetUserID(ctx))
		if err != nil {
			return nil, err
		}
	}

	// find branch merge
	result, err := s.p.bdl.ListMergeRequest(req.AppID, apis.GetUserID(ctx), apistructs.GittarQueryMrRequest{
		TargetBranch: req.TargetBranch,
		SourceBranch: req.SourceBranch,
		Page:         1,
		Size:         999,
	})
	if err != nil {
		return nil, err
	}
	mergeInfoList := result.List

	var mergeResult *apistructs.MergeRequestInfo
	for _, merge := range mergeInfoList {
		if merge.State == mrOpenState || merge.State == mrMergedState {
			mergeResult = merge
			break
		}
	}

	var mergeID int64
	var repoMergeID int
	if mergeResult == nil {
		// auto create merge
		mergeInfo, err := s.p.bdl.CreateMergeRequest(req.AppID, apis.GetUserID(ctx), apistructs.GittarCreateMergeRequest{
			TargetBranch:       req.TargetBranch,
			SourceBranch:       req.SourceBranch,
			Title:              "auto create devflow merge",
			Description:        "auto create devflow merge",
			RemoveSourceBranch: false,
			AssigneeID:         apis.GetUserID(ctx),
		})
		if err != nil {
			return nil, err
		}
		mergeID = mergeInfo.Id
		repoMergeID = mergeInfo.RepoMergeId
	} else {
		mergeID = mergeResult.Id
		repoMergeID = mergeResult.RepoMergeId
	}

	var extra = pb.IssueRelationExtra{
		AppID:       req.AppID,
		RepoMergeID: uint64(repoMergeID),
	}

	// create relation
	_, err = s.p.IssueRelation.Create(ctx, &issuerelationpb.CreateIssueRelationRequest{
		Type:     issueRelationType,
		IssueID:  req.IssueID,
		Relation: strconv.FormatInt(mergeID, 10),
		Extra:    jsonparse.JsonOneLine(&extra),
	})
	if err != nil {
		return nil, err
	}

	return &pb.CreateFlowNodeResponse{
		RepoMergeID: uint64(repoMergeID),
		MergeID:     uint64(mergeID),
	}, nil
}

func (s *Service) OperationMerge(ctx context.Context, req *pb.OperationMergeRequest) (*pb.OperationMergeResponse, error) {
	if req.Enable == nil {
		return nil, fmt.Errorf("enable can not empty")
	}

	extra, err := s.getRelationExtra(ctx, req.MergeID)
	if err != nil {
		return nil, err
	}

	appDto, err := s.p.bdl.GetApp(extra.AppID)
	if err != nil {
		return nil, err
	}

	mrInfo, err := s.p.bdl.GetMergeRequestDetail(extra.AppID, apis.GetUserID(ctx), extra.RepoMergeID)
	if err != nil {
		return nil, err
	}

	tempBranch, err := s.getTempBranchFromFlowRule(ctx, appDto.ProjectID, mrInfo)
	if err != nil {
		return nil, err
	}
	if tempBranch == "" {
		return nil, fmt.Errorf("tempBranch can not empty")
	}

	operationReq := apistructs.GittarMergeOperationTempBranchRequest{
		MergeID:    req.MergeID,
		TempBranch: tempBranch,
	}
	// Merged into tempBranch success will set isJoinTempBranch = true
	if !req.Enable.Value {
		operationReq.IsJoinTempBranch = &req.Enable.Value
	}
	err = s.p.bdl.OperationTempBranch(extra.AppID, apis.GetUserID(ctx), operationReq)
	if err != nil {
		return nil, err
	}

	if req.Enable.Value {
		err = s.JoinTempBranch(ctx, tempBranch, appDto, mrInfo)
		if err != nil {
			return nil, err
		}
		return &pb.OperationMergeResponse{}, nil
	}

	// Is already removed
	if !mrInfo.IsJoinTempBranch {
		return &pb.OperationMergeResponse{}, nil
	}
	err = s.RejoinTempBranch(ctx, tempBranch, mrInfo.TargetBranch, appDto)
	if err != nil {
		return nil, err
	}

	return &pb.OperationMergeResponse{}, nil
}

func (s *Service) getTempBranchFromFlowRule(ctx context.Context, projectID uint64, mrInfo *apistructs.MergeRequestInfo) (string, error) {
	flowRule, err := s.p.DevFlowRule.GetFlowByRule(ctx, devflowrule.GetFlowByRuleRequest{
		ProjectID:    projectID,
		FlowType:     MultiBranchFlowType,
		TargetBranch: mrInfo.TargetBranch,
		ChangeBranch: mrInfo.SourceBranch,
	})
	if err != nil {
		return "", err
	}

	if flowRule != nil {
		return flowRule.AutoMergeBranch, nil
	}
	return "", nil
}

func (s *Service) GetTempBranchFromDevFlowRules(ctx context.Context, targetBranch string, projectID uint64) (string, error) {
	result, err := s.p.DevFlowRule.GetDevFlowRulesByProjectID(ctx, &rulepb.GetDevFlowRuleRequest{
		ProjectID: projectID,
	})
	if err != nil {
		return "", err
	}
	for _, v := range result.Data.Flows {
		if targetBranch == v.TargetBranch {
			return v.AutoMergeBranch, nil
		}
	}
	return "", nil
}

func (s *Service) JoinTempBranch(ctx context.Context, tempBranch string, appDto *apistructs.ApplicationDTO, mrInfo *apistructs.MergeRequestInfo) error {
	repoPath := gittarPrefixOpenApi + appDto.ProjectName + "/" + appDto.Name

	// Idempotent create tempBranch
	if err := s.IdempotentCreateBranch(ctx, repoPath, mrInfo.TargetBranch, tempBranch); err != nil {
		return err
	}

	// Merge sourceBranch to tempBranch
	return s.MergeToTempBranch(ctx, tempBranch, appDto.ID, mrInfo)
}

func (s *Service) RejoinTempBranch(ctx context.Context, tempBranch, targetBranch string, appDto *apistructs.ApplicationDTO) error {
	// List merge requests to find the joined mr
	result, err := s.p.bdl.ListMergeRequest(appDto.ID, apis.GetUserID(ctx), apistructs.GittarQueryMrRequest{
		TargetBranch: targetBranch,
		Page:         1,
		Size:         999,
	})
	if err != nil {
		return err
	}
	mrList := result.List

	repoPath := gittarPrefixOpenApi + appDto.ProjectName + "/" + appDto.Name
	// Delete tempBranch
	if err = s.IdempotentDeleteBranch(ctx, repoPath, tempBranch); err != nil {
		return err
	}

	// Idempotent create tempBranch
	if err = s.IdempotentCreateBranch(ctx, repoPath, targetBranch, tempBranch); err != nil {
		return err
	}

	// Merge the branch to tempBranch
	for _, v := range mrList {
		if !v.IsJoinTempBranch {
			continue
		}
		if v.State != mrOpenState {
			continue
		}
		if err = s.MergeToTempBranch(ctx, tempBranch, appDto.ID, v); err != nil {
			s.p.Log.Errorf("failed to MergeWithBranch, err: %v", err)
		}
	}
	return nil
}

func (s *Service) MergeToTempBranch(ctx context.Context, tempBranch string, appID uint64, mrInfo *apistructs.MergeRequestInfo) error {
	_, err := s.p.bdl.MergeWithBranch(apis.GetUserID(ctx), apistructs.GittarMergeWithBranchRequest{
		SourceBranch: mrInfo.SourceBranch,
		TargetBranch: tempBranch,
		AppID:        appID,
	})

	req := apistructs.GittarMergeOperationTempBranchRequest{
		MergeID: uint64(mrInfo.Id),
	}
	if err != nil {
		req.JoinTempBranchStatus = apistructs.JoinTempBranchFailedStatus + err.Error()
	} else {
		req.JoinTempBranchStatus = apistructs.JoinTempBranchSuccessStatus
		isJoinTempBranch := true
		req.IsJoinTempBranch = &isJoinTempBranch
	}
	operationErr := s.p.bdl.OperationTempBranch(appID, apis.GetUserID(ctx), req)
	if operationErr != nil {
		s.p.Log.Errorf("failed to OperationTempBranch, err: %v", operationErr.Error())
	}
	return err
}

func (s *Service) IdempotentCreateBranch(ctx context.Context, repoPath, targetBranch, newBranch string) error {
	exists, err := s.JudgeBranchIsExists(ctx, repoPath, newBranch)
	if err != nil {
		return err
	}
	if exists {
		return nil
	}
	err = s.p.bdl.CreateGittarBranch(repoPath, apistructs.GittarCreateBranchRequest{
		Name: newBranch,
		Ref:  targetBranch,
	}, apis.GetOrgID(ctx), apis.GetUserID(ctx))
	return err
}

func (s *Service) IdempotentDeleteBranch(ctx context.Context, repoPath, branch string) error {
	exists, err := s.JudgeBranchIsExists(ctx, repoPath, branch)
	if err != nil {
		return err
	}
	if !exists {
		return nil
	}
	err = s.p.bdl.DeleteGittarBranch(repoPath, apis.GetOrgID(ctx), branch, apis.GetUserID(ctx))
	return err
}

func (s *Service) JudgeBranchIsExists(ctx context.Context, repoPath, branch string) (has bool, err error) {
	branches, err := s.p.bdl.GetGittarBranchesV2(repoPath, apis.GetOrgID(ctx), true, apis.GetUserID(ctx))
	if err != nil {
		return false, err
	}

	for _, v := range branches {
		if v == branch {
			return true, nil
		}
	}
	return false, nil
}

func (s *Service) DeleteFlowNode(ctx context.Context, req *pb.DeleteFlowNodeRequest) (*pb.DeleteFlowNodeResponse, error) {
	if req.MergeID <= 0 {
		return nil, fmt.Errorf("mergeID can not empty")
	}

	if req.IssueID <= 0 {
		return nil, fmt.Errorf("issueID can not empty")
	}

	issueRelations, err := s.p.IssueRelation.List(ctx, &issuerelationpb.ListIssueRelationRequest{
		Type: issueRelationType,
		Relations: []string{
			strconv.FormatUint(req.MergeID, 10),
		},
		IssueIDs: []uint64{req.IssueID},
	})
	if err != nil {
		return nil, err
	}

	data := issueRelations.Data
	var relationID string
	for _, relation := range data {
		if relation.IssueID == req.IssueID && relation.Relation == strconv.FormatUint(req.MergeID, 10) {
			relationID = relation.ID
		}
	}

	if relationID == "" {
		return &pb.DeleteFlowNodeResponse{}, nil
	}

	_, err = s.p.IssueRelation.Delete(ctx, &issuerelationpb.DeleteIssueRelationRequest{
		RelationID: relationID,
	})
	if err != nil {
		return nil, err
	}

	return &pb.DeleteFlowNodeResponse{}, nil
}

// Reconstruction todo impl
func (s *Service) Reconstruction(ctx context.Context, req *pb.ReconstructionRequest) (*pb.ReconstructionResponse, error) {

	return &pb.ReconstructionResponse{}, nil
}

func (s *Service) buildRelationQueryRequest(req *pb.GetDevFlowInfoRequest) (*issuerelationpb.ListIssueRelationRequest, error) {
	query := &issuerelationpb.ListIssueRelationRequest{
		Type: issueRelationType,
	}
	if req.IssueID > 0 {
		query.IssueIDs = append(query.IssueIDs, req.IssueID)
	}
	if req.MergeID > 0 {
		query.Relations = append(query.Relations, strconv.FormatUint(req.MergeID, 10))
	}
	if len(query.Relations) == 0 && len(query.IssueIDs) == 0 {
		return nil, fmt.Errorf("relation and issue cannot be empty at the same time")
	}
	if req.ProjectID == 0 {
		return nil, fmt.Errorf("projectID can not empty")
	}
	return query, nil
}

func (s *Service) GetDevFlowInfo(ctx context.Context, req *pb.GetDevFlowInfoRequest) (*pb.GetDevFlowInfoResponse, error) {
	query, err := s.buildRelationQueryRequest(req)
	if err != nil {
		return nil, err
	}

	// Find issueRelations
	result, err := s.p.IssueRelation.List(ctx, query)
	if err != nil {
		return nil, err
	}
	relations := result.Data
	if len(relations) == 0 {
		return &pb.GetDevFlowInfoResponse{Status: getStatusDevFlowInfos(nil)}, nil
	}

	appInfoMap, err := s.getAllAppInfoMapByProjectID(ctx, req.ProjectID)
	if err != nil {
		return nil, err
	}

	rules, err := s.branchRuleSvc.Query(apistructs.ProjectScope, int64(req.ProjectID))
	if err != nil {
		return nil, apierrors.ErrGetGittarRepoFile.InternalError(err)
	}

	var (
		relationExtraMap          = map[string]*pb.IssueRelationExtra{}
		relationMap               = map[string]*issuerelationpb.IssueRelation{}
		relationMergeInfoMap      = map[string]*apistructs.MergeRequestInfo{}
		appMergeInfoMap           = make(map[uint64][]apistructs.MergeRequestInfo)
		needQueryTempBranchAppMap = map[uint64]uint64{}

		devFlowInfos []*pb.DevFlowInfo
	)

	work := limit_sync_group.NewWorker(5)
	for index := range relations {
		work.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			index := i[0].(int)
			relation := relations[index]

			var extra pb.IssueRelationExtra
			err = json.Unmarshal([]byte(relation.Extra), &extra)
			if err != nil {
				return fmt.Errorf("failed to unmarshal extra, err: %v", err)
			}

			err = s.permission.CheckAppAction(apistructs.IdentityInfo{UserID: apis.GetUserID(ctx)}, extra.AppID, apistructs.GetAction)
			if err != nil {
				var devFlowInfo = &pb.DevFlowInfo{}
				devFlowInfo.DevFlowNode = &pb.DevFlowNode{
					RepoMergeID: extra.RepoMergeID,
					AppID:       extra.AppID,
					AppName:     appInfoMap[extra.AppID].Name,
					TempBranch:  "",
					IssueID:     relation.IssueID,
				}
				devFlowInfo.HasPermission = false

				locker.Lock()
				devFlowInfos = append(devFlowInfos, devFlowInfo)
				locker.Unlock()
				return nil
			}

			mrInfo, err := s.p.bdl.GetMergeRequestDetail(extra.AppID, apis.GetUserID(ctx), extra.RepoMergeID)
			if err != nil {
				return err
			}
			if mrInfo.State != mrMergedState && mrInfo.State != mrOpenState {
				go func() {
					_, err := s.p.IssueRelation.Delete(ctx, &issuerelationpb.DeleteIssueRelationRequest{
						RelationID: relation.ID,
					})
					if err != nil {
						s.p.Log.Errorf("delete not open mr relation error %v", err)
					}
				}()
				return nil
			}

			locker.Lock()
			defer locker.Unlock()
			relationMap[relation.ID] = relation
			relationExtraMap[relation.ID] = &extra
			relationMergeInfoMap[relation.ID] = mrInfo
			appMergeInfoMap[extra.AppID] = append(appMergeInfoMap[extra.AppID], *mrInfo)
			needQueryTempBranchAppMap[extra.AppID] = extra.AppID
			return nil
		}, index)
	}
	if work.Do().Error() != nil {
		return nil, work.Error()
	}

	var (
		appTempBranchChangeBranchListMap = map[string][]*pb.ChangeBranch{}
		appTempBranchCommitMap           = map[string]*apistructs.Commit{}
		sourceBranchBaseCommitMap        = make(map[string]*apistructs.Commit)
		mrTempBranchMap                  = make(map[int64]string)
	)
	for _, appID := range needQueryTempBranchAppMap {
		work.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			appID := i[0].(uint64)
			appDto := appInfoMap[appID]
			for _, v := range appMergeInfoMap[appID] {
				repoPath := filepath.Join(gittarPrefixOpenApi, appDto.ProjectName, appDto.Name)
				tempBranch, err := s.getTempBranchFromFlowRule(ctx, appDto.ProjectID, &v)
				if err != nil {
					return err
				}
				var baseCommit *apistructs.Commit
				if tempBranch != "" {
					if err = s.IdempotentCreateBranch(ctx, repoPath, v.TargetBranch, tempBranch); err != nil {
						return err
					}
					if v.IsJoinTempBranch {
						baseCommit, err = s.p.bdl.GetMergeBase(apis.GetUserID(ctx), apistructs.GittarMergeBaseRequest{
							SourceBranch: v.SourceBranch,
							TargetBranch: tempBranch,
							AppID:        appDto.ID,
						})
						if err != nil {
							return err
						}
					}
				}

				if _, ok := appTempBranchCommitMap[fmt.Sprintf("%d%s", appID, tempBranch)]; !ok {
					var commit *apistructs.Commit
					if tempBranch != "" {
						commit, err = s.p.bdl.ListGittarCommit(repoPath, tempBranch, apis.GetUserID(ctx), apis.GetOrgID(ctx))
						if err != nil {
							return err
						}
					}

					locker.Lock()
					appTempBranchCommitMap[fmt.Sprintf("%d%s", appID, tempBranch)] = commit
					locker.Unlock()
				}

				locker.Lock()
				mrTempBranchMap[v.Id] = tempBranch
				sourceBranchBaseCommitMap[fmt.Sprintf("%d%s", appID, v.SourceBranch)] = baseCommit
				if tempBranch != "" && v.IsJoinTempBranch {
					appTempBranchChangeBranchListMap[fmt.Sprintf("%d%s", appID, tempBranch)] = append(appTempBranchChangeBranchListMap[fmt.Sprintf("%d%s", appID, tempBranch)], &pb.ChangeBranch{
						Commit:      CommitConvert(baseCommit),
						BranchName:  v.SourceBranch,
						RepoMergeID: uint64(v.RepoMergeId),
					})
				}
				locker.Unlock()
			}
			return nil
		}, appID)
	}
	if work.Do().Error() != nil {
		return nil, work.Error()
	}

	for relationID := range relationMap {
		work.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			relationID := i[0].(string)
			relation := relationMap[relationID]

			extra := relationExtraMap[relationID]
			mrInfo := relationMergeInfoMap[relationID]
			appDto := appInfoMap[extra.AppID]
			repoPath := filepath.Join(gittarPrefixOpenApi, appDto.ProjectName, appDto.Name)

			var (
				commitID        string
				infos           []*pb.PipelineStepInfo
				node            *apistructs.UnifiedFileTreeNode
				hasOnPushBranch bool
				err             error
			)

			tempBranch := mrTempBranchMap[mrInfo.Id]
			if mrInfo.IsJoinTempBranch {
				if appTempBranchCommitMap[fmt.Sprintf("%d%s", appDto.ID, tempBranch)] != nil {
					commitID = appTempBranchCommitMap[fmt.Sprintf("%d%s", appDto.ID, tempBranch)].ID
				}

				if tempBranch != "" && commitID != "" {
					infos, node, hasOnPushBranch, err = s.listPipelineStepInfo(ctx, &appDto, tempBranch, diceworkspace.GetValidBranchByGitReference(tempBranch, rules).Workspace, commitID)
					if err != nil {
						return err
					}
				}
			}
			branchDetail, err := s.p.bdl.GetGittarBranchDetail(repoPath, apis.GetOrgID(ctx), mrInfo.SourceBranch, apis.GetUserID(ctx))
			if err != nil {
				return err
			}

			var devFlowInfo = &pb.DevFlowInfo{}
			devFlowInfo.ChangeBranch = appTempBranchChangeBranchListMap[fmt.Sprintf("%d%s", appDto.ID, tempBranch)]
			devFlowInfo.Commit = commitID
			devFlowInfo.PipelineStepInfos = infos
			devFlowInfo.HasPermission = true
			if mrInfo.IsJoinTempBranch {
				devFlowInfo.PInode = base64.URLEncoding.EncodeToString([]byte(fmt.Sprintf("%v/%v/tree/%v", appInfoMap[extra.AppID].ProjectID, extra.AppID, tempBranch)))
			}
			if node != nil {
				devFlowInfo.Inode = node.Inode
			}
			devFlowInfo.HasOnPushBranch = hasOnPushBranch

			devFlowInfo.DevFlowNode = &pb.DevFlowNode{
				RepoMergeID:          extra.RepoMergeID,
				MergeID:              uint64(mrInfo.Id),
				AppID:                extra.AppID,
				AppName:              appInfoMap[extra.AppID].Name,
				TargetBranch:         mrInfo.TargetBranch,
				SourceBranch:         mrInfo.SourceBranch,
				JoinTempBranchStatus: mrInfo.JoinTempBranchStatus,
				TempBranch:           tempBranch,
				IssueID:              relation.IssueID,
				IsJoinTempBranch:     mrInfo.IsJoinTempBranch,
				Commit:               CommitConvert(branchDetail.Commit),
				BaseCommit:           CommitConvert(sourceBranchBaseCommitMap[fmt.Sprintf("%d%s", extra.AppID, mrInfo.SourceBranch)]),
				CanJoin:              canJoin(branchDetail.Commit, sourceBranchBaseCommitMap[fmt.Sprintf("%d%s", extra.AppID, mrInfo.SourceBranch)]),
			}

			locker.Lock()
			devFlowInfos = append(devFlowInfos, devFlowInfo)
			locker.Unlock()
			return nil
		}, relationID)
	}
	err = work.Do().Error()
	if err != nil {
		return nil, err
	}
	sort.Slice(devFlowInfos, func(i, j int) bool {
		if devFlowInfos[i].DevFlowNode.AppID != devFlowInfos[j].DevFlowNode.AppID {
			return devFlowInfos[i].DevFlowNode.AppID > devFlowInfos[j].DevFlowNode.AppID
		}
		return devFlowInfos[i].DevFlowNode.RepoMergeID > devFlowInfos[j].DevFlowNode.RepoMergeID
	})

	return &pb.GetDevFlowInfoResponse{
		DevFlowInfos: devFlowInfos,
		Status:       getStatusDevFlowInfos(devFlowInfos),
	}, nil
}

func (s *Service) getAllAppInfoMapByProjectID(ctx context.Context, projectID uint64) (map[uint64]apistructs.ApplicationDTO, error) {
	var appInfoMap = map[uint64]apistructs.ApplicationDTO{}
	orgID, err := apis.GetIntOrgID(ctx)
	if err != nil {
		return nil, err
	}
	appListResult, err := s.p.bdl.GetAppsByProjectSimple(projectID, uint64(orgID), apis.GetUserID(ctx))
	if err != nil {
		return nil, err
	}
	for _, app := range appListResult.List {
		appInfoMap[app.ID] = app
	}
	return appInfoMap, nil
}

func canJoin(commit, baseCommit *apistructs.Commit) bool {
	if commit == nil {
		return false
	}
	if baseCommit == nil {
		return true
	}
	if commit.ID == baseCommit.ID {
		return false
	}
	return true
}

func CommitConvert(commit *apistructs.Commit) *pb.Commit {
	if commit == nil {
		return nil
	}
	var author, committer *pb.Signature
	if commit.Author != nil {
		author = &pb.Signature{
			Email: commit.Author.Email,
			Name:  commit.Author.Name,
			When:  timestamppb.New(commit.Author.When),
		}
	}
	if commit.Committer != nil {
		committer = &pb.Signature{
			Email: commit.Committer.Email,
			Name:  commit.Committer.Name,
			When:  timestamppb.New(commit.Committer.When),
		}
	}
	return &pb.Commit{
		ID:            commit.ID,
		Author:        author,
		Committer:     committer,
		CommitMessage: commit.CommitMessage,
		ParentSha:     commit.ParentSha,
	}
}

func (s *Service) queryAllPipelineYmlAndDoFunc(ctx context.Context, appDto *apistructs.ApplicationDTO, branch string,
	do func(ctx context.Context, locker *limit_sync_group.Locker, content string, node *apistructs.UnifiedFileTreeNode) error) error {

	ymlNodes, err := s.getAllPipelineYml(ctx, appDto, branch)
	if err != nil {
		return err
	}

	if len(ymlNodes) > 0 {
		ymlNodes = ymlNodes[:1]
	}
	worker := limit_sync_group.NewWorker(5)
	for index := range ymlNodes {
		worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			index := i[0].(int)
			node := ymlNodes[index]

			content, err := s.getPipelineYmlNodeContent(ctx, appDto, branch, node)
			if err != nil {
				return err
			}

			return do(ctx, locker, content, node)
		}, index)
	}
	return worker.Do().Error()
}

func (s *Service) listPipelineStepInfo(ctx context.Context, appDto *apistructs.ApplicationDTO, branch string, workSpace string, commit string) ([]*pb.PipelineStepInfo, *apistructs.UnifiedFileTreeNode, bool, error) {
	var (
		infos           []*pb.PipelineStepInfo
		treeNode        *apistructs.UnifiedFileTreeNode
		HasOnPushBranch bool
	)
	err := s.queryAllPipelineYmlAndDoFunc(ctx, appDto, branch, func(ctx context.Context, locker *limit_sync_group.Locker, content string, node *apistructs.UnifiedFileTreeNode) error {
		pipelineYml, err := pipelineyml.New([]byte(content))
		if err != nil {
			s.p.Log.Errorf("failed to parse %v yaml:%v \n err:%v", content, pipelineYml, err)
			return nil
		}

		treeNode = node
		HasOnPushBranch = pipelineYml.HasOnPushBranch(branch)
		if !HasOnPushBranch {
			return nil
		}

		inodeBytes, err := base64.URLEncoding.DecodeString(node.Inode)
		if err != nil {
			return err
		}

		var stepInfo = pb.PipelineStepInfo{
			YmlName: node.Name,
		}

		ymlName := getFullYmlName(string(inodeBytes), node.Name)
		result, err := s.p.bdl.PageListPipeline(apistructs.PipelinePageListRequest{
			PageNo:   1,
			PageSize: 1,
			Sources: []apistructs.PipelineSource{
				apistructs.PipelineSourceDice,
			},
			YmlNames: []string{
				fmt.Sprintf("%v/%v/%v/%v", appDto.ID, workSpace, branch, ymlName),
			},
			DescCols: []string{"id"},
		})
		if err != nil {
			s.p.Log.Errorf("failed to list ymlName %v branch %v pipeline info err: %v", ymlName, branch, err)
		} else {
			if len(result.Pipelines) > 0 && result.Pipelines[0].Commit == commit {
				stepInfo.PipelineID = result.Pipelines[0].ID
				stepInfo.Status = result.Pipelines[0].Status.String()
			}
		}

		locker.Lock()
		infos = append(infos, &stepInfo)
		locker.Unlock()
		return nil
	})
	if err != nil {
		return nil, nil, false, err
	}
	return infos, treeNode, HasOnPushBranch, nil
}

func getFullYmlName(parseInode string, ymlName string) string {
	if ymlName != apistructs.DefaultPipelineYmlName {
		if strings.Contains(parseInode, apistructs.DicePipelinePath) {
			ymlName = apistructs.DicePipelinePath + "/" + ymlName
		} else {
			ymlName = apistructs.ErdaPipelinePath + "/" + ymlName
		}
	}
	return ymlName
}

func (s *Service) getPipelineYmlNodeContent(ctx context.Context, appDto *apistructs.ApplicationDTO, branch string, node *apistructs.UnifiedFileTreeNode) (string, error) {

	inodeBytes, err := base64.URLEncoding.DecodeString(node.Inode)
	if err != nil {
		return "", apierrors.ErrGetGittarFileTreeNode.InternalError(err)
	}

	ymlName := getFullYmlName(string(inodeBytes), node.Name)
	inode := fmt.Sprintf("%v/%v/blob/%v/%v", appDto.ProjectName, appDto.Name, branch, ymlName)

	content, err := s.p.bdl.GetGittarBlobNode(gittarPrefixOpenApi+inode, apis.GetOrgID(ctx), apis.GetUserID(ctx))
	if err != nil {
		return "", apierrors.ErrGetGittarFileTreeNode.InternalError(err)
	}
	return content, nil
}

func getStatusDevFlowInfos(devFlowInfos []*pb.DevFlowInfo) string {
	if len(devFlowInfos) == 0 {
		return "none"
	}

	var pipelineNotRunNum = 0
	var pipelineFailedNum = 0
	var pipelineSuccessNum = 0
	var pipelineNum = 0

	for _, info := range devFlowInfos {
		if info.DevFlowNode.JoinTempBranchStatus == apistructs.JoinTempBranchFailedStatus {
			return "mergeFailed"
		}
		for _, pipelineInfo := range info.PipelineStepInfos {
			if pipelineInfo.Status == "" {
				pipelineNotRunNum++
			}
			if apistructs.PipelineStatus(pipelineInfo.Status).IsFailedStatus() {
				pipelineFailedNum++
			}
			if apistructs.PipelineStatus(pipelineInfo.Status).IsSuccessStatus() {
				pipelineSuccessNum++
			}
		}
		pipelineNum++
	}

	if pipelineNotRunNum > 0 {
		return "pipelineNotRun"
	}
	if pipelineFailedNum > 0 {
		return "pipelineFiled"
	}
	if pipelineSuccessNum != pipelineNum {
		return "pipelineRunning"
	}
	return "success"
}

func (s *Service) getAllPipelineYml(ctx context.Context, app *apistructs.ApplicationDTO, branch string) ([]*apistructs.UnifiedFileTreeNode, error) {
	orgID, err := strconv.ParseInt(apis.GetOrgID(ctx), 10, 64)
	if err != nil {
		return nil, err
	}
	var nodes []*apistructs.UnifiedFileTreeNode

	var defaultYmlPath = fmt.Sprintf("%v/%v/tree/%v", app.ProjectID, app.ID, branch)
	pipelineTreeData, err := s.fileTree.ListFileTreeNodes(apistructs.UnifiedFileTreeNodeListRequest{
		Scope:   apistructs.FileTreeScopeProjectApp,
		ScopeID: strconv.FormatUint(app.ProjectID, 10),
		Pinode:  base64.URLEncoding.EncodeToString([]byte(defaultYmlPath)),
		IdentityInfo: apistructs.IdentityInfo{
			UserID: apis.GetUserID(ctx),
		},
	}, uint64(orgID))
	if err != nil {
		s.p.Log.Errorf("failed to get %v path yml error %v", apistructs.DefaultPipelineYmlName, err)
		return nil, err
	}

	for _, node := range pipelineTreeData {
		if node.Name == apistructs.DefaultPipelineYmlName {
			nodes = append(nodes, node)
			break
		}
	}
	return nodes, nil
}

func (s *Service) getRelationExtra(ctx context.Context, mergeID uint64) (*pb.IssueRelationExtra, error) {
	resp, err := s.p.IssueRelation.List(ctx, &issuerelationpb.ListIssueRelationRequest{
		Type: issueRelationType,
		Relations: []string{
			strconv.FormatInt(int64(mergeID), 10),
		},
	})
	if err != nil {
		return nil, err
	}
	if len(resp.Data) == 0 {
		return nil, fmt.Errorf("not find this devflow merge id %v", mergeID)
	}

	var extra pb.IssueRelationExtra
	err = json.Unmarshal([]byte(resp.Data[0].Extra), &extra)
	if err != nil {
		return nil, fmt.Errorf("unmarshal extra %v error %v", resp.Data[0].Extra, err)
	}
	return &extra, nil
}
