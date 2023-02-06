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

package filetree

import (
	"encoding/base64"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/apps/dop/services/pipeline"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/strutil"
)

const gittarPrefixOpenApi = "/wb/"

const gittarEntryTreeType = "tree"
const gittarEntryBlobType = "blob"

// Pipeline pipeline 结构体
type GittarFileTree struct {
	bdl           *bundle.Bundle
	branchRuleSve *branchrule.BranchRule
}

// Option Pipeline 配置选项
type Option func(*GittarFileTree)

// New Pipeline service
func New(options ...Option) *GittarFileTree {
	r := &GittarFileTree{}
	for _, op := range options {
		op(r)
	}
	return r
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(f *GittarFileTree) {
		f.bdl = bdl
	}
}

func WithBranchRule(svc *branchrule.BranchRule) Option {
	return func(f *GittarFileTree) {
		f.branchRuleSve = svc
	}
}

func (svc *GittarFileTree) ListFileTreeNodes(req apistructs.UnifiedFileTreeNodeListRequest, orgID uint64) ([]*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrListGittarFileTreeNodes.InvalidParameter(err)
	}

	// 项目id转化
	projectID, err := strconv.ParseInt(req.ScopeID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
	}

	var list []*apistructs.UnifiedFileTreeNode
	// 假如 pinode 是根节点，就根据 scopeID 查询项目下都应用列表
	if req.Pinode == apistructs.RootPinode {
		// 查询项目名称
		project, err := svc.bdl.GetProject(uint64(projectID))
		if err != nil {
			return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
		}
		// 查询项目下都应用列表
		apps, err := svc.bdl.GetAppsByProjectSimple(uint64(projectID), orgID, req.UserID)
		if err != nil {
			return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
		}
		if apps == nil || apps.List == nil || len(apps.List) <= 0 {
			return nil, nil
		}
		// 将应用转化为第一层的目录树
		for _, app := range apps.List {
			list = append(list, appConvertToUnifiedFileTreeNode(&app, req.Scope, req.ScopeID, strconv.Itoa(int(project.ID))))
		}
		return list, nil
	}
	// 不是就根据 pinode 查询其在 gittar 的目录
	// pinode 反base64 获取父类的路径
	pinodeBytes, err := base64.URLEncoding.DecodeString(req.Pinode)
	if err != nil {
		return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
	}
	pinode := string(pinodeBytes)
	pathSplit := strings.Split(pinode, "/")
	length := len(pathSplit)
	// 因为分支是 feature/sss/sss 这种模式根据 / 分割就会有问题
	branchExcessLength := getBranchExcessLength(pinode)

	if length < 2 {
		return nil, fmt.Errorf("not find nodes: pinode length error")
	}

	// 第一个为projectID的字符串, 获取其名称
	// 第二个为appId的字符串, 获取其名称
	appID, err := strconv.ParseUint(pathSplit[1], 10, 64)
	if err != nil {
		return nil, fmt.Errorf("invalid appID: %s, err: %v", pathSplit[1], err)
	}
	app, err := svc.bdl.GetApp(appID)
	if err != nil {
		return nil, fmt.Errorf("failed to get app, appID: %d, err: %v", appID, err)
	}

	var realPinode = app.ProjectName + "/" + app.Name
	for i := 2; i < len(pathSplit); i++ {
		realPinode += "/" + pathSplit[i]
	}

	// 根据 / 分割判定长度为 2 的时候，代表需要查询分支列表
	if length == 2 {
		branchs, err := svc.bdl.GetGittarBranchesV2(gittarPrefixOpenApi+realPinode, strconv.Itoa(int(orgID)), true, req.UserID)
		if err != nil {
			return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
		}
		if branchs == nil || len(branchs) <= 0 {
			return nil, nil
		}

		for _, branch := range branchs {
			// 过滤掉不符合分支规则
			_, err := svc.GetWorkspaceByBranch(pathSplit[0], branch)
			if err != nil {
				continue
			}
			list = append(list, branchConvertToUnifiedFileTreeNode(branch, req.Scope, req.ScopeID, pinode, req.Pinode))
		}
		return list, nil
	}
	if length > 3+branchExcessLength {
		// 长度大于 3 就表达查询子节点了 /projectName/appName/tree/branchName
		treeData, err := svc.bdl.GetGittarTreeNode(gittarPrefixOpenApi+realPinode, strconv.Itoa(int(orgID)), true, req.UserID)
		if err != nil {
			return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
		}
		if treeData == nil || len(treeData.Entries) <= 0 {
			return nil, nil
		}

		for _, node := range treeData.Entries {
			if strings.Contains(node.Name, "/") {
				node.Name = strings.Split(node.Name, "/")[0]
			}

			// 代表是分支下, 过滤出 pipeline.yml 和 .dice
			if length == 4+branchExcessLength {
				// 假如不是 tree  yml 结构的文件
				if node.Type != gittarEntryTreeType && node.Name != "pipeline.yml" {
					continue
				}
				if node.Type == gittarEntryTreeType && node.Name != ".dice" && node.Name != ".erda" {
					continue
				}
			}
			// 代表在.dice 目录下, 文件全部过滤，只有pipelines
			if length == 5+branchExcessLength {
				if node.Type != gittarEntryTreeType {
					continue
				}
				if node.Type == gittarEntryTreeType && node.Name != "pipelines" {
					continue
				}
			}
			// 长度大于 5 就代表过滤出 .yml 就行了
			if length > 5+branchExcessLength {
				if node.Type != gittarEntryTreeType && !strings.HasSuffix(node.Name, ".yml") {
					continue
				}
			}

			// Filter does not match pipeline category rules
			if notMatchInPipelineCategory(apistructs.PipelineCategory(req.PipelineCategoryKey), treeData.Path, node) {
				continue
			}
			list = append(list, entryConvertToUnifiedFileTreeNode(&node, req.Scope, req.ScopeID, pinode, req.Pinode))
		}
	}

	return list, nil
}

func notMatchInPipelineCategory(pipelineCategoryKey apistructs.PipelineCategory, path string, node apistructs.TreeEntry) bool {
	if pipelineCategoryKey == "" {
		return false
	}
	if pipelineCategoryKey != "" {
		switch pipelineCategoryKey {
		case apistructs.CategoryOthers:
			if node.Type == gittarEntryBlobType &&
				(strutil.InSlice(filepath.Join(path, node.Name), apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildArtifact]) ||
					strutil.InSlice(filepath.Join(path, node.Name), apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildDeploy]) ||
					strutil.InSlice(filepath.Join(path, node.Name), apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildCombineArtifact]) ||
					strutil.InSlice(filepath.Join(path, node.Name), apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildIntegration])) {
				return true
			}
		default:
			if node.Type == gittarEntryTreeType && node.Name == ".dice" || strings.HasSuffix(path, ".dice") {
				return true
			}
			if node.Type == gittarEntryBlobType && !strutil.InSlice(filepath.Join(path, node.Name), apistructs.CategoryKeyRuleMap[apistructs.PipelineCategory(pipelineCategoryKey)]) {
				return true
			}
		}
	}
	return false
}

func (svc *GittarFileTree) GetFileTreeNode(req apistructs.UnifiedFileTreeNodeGetRequest, orgID uint64) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrGetGittarFileTreeNode.InvalidParameter(err)
	}

	// pinode 反base64 获取父类的路径
	inodeBytes, err := base64.URLEncoding.DecodeString(req.Inode)
	if err != nil {
		return nil, apierrors.ErrGetGittarFileTreeNode.InternalError(err)
	}
	inode := string(inodeBytes)
	inode = strings.Replace(inode, "/"+gittarEntryTreeType, "/"+gittarEntryBlobType, 1)
	pathSplit := strings.Split(inode, "/")

	// 因为分支是 feature/sss/sss 这种模式根据 / 分割就会有问题
	branchExcessLength := getBranchExcessLength(inode)

	// 第一个为projectID的字符串, 获取其名称
	// 第二个为appId的字符串, 获取其名称
	appID, err := strconv.ParseUint(pathSplit[1], 10, 64)
	if err != nil {
		return nil, fmt.Errorf("invalid appID: %s, err: %v", pathSplit[1], err)
	}
	app, err := svc.bdl.GetApp(appID)
	if err != nil {
		return nil, fmt.Errorf("failed to get app, appID: %d, err: %v", appID, err)
	}
	realinode, treeRepo := app.ProjectName+"/"+app.Name, app.ProjectName+"/"+app.Name
	for i := 2; i < len(pathSplit); i++ {
		realinode += "/" + pathSplit[i]

		if i == 2 && pathSplit[i] == gittarEntryBlobType {
			treeRepo += "/" + gittarEntryTreeType
		} else {
			treeRepo += "/" + pathSplit[i]
		}

	}
	orgIDStr := strconv.Itoa(int(orgID))
	// 获取文件内容
	context, err := svc.bdl.GetGittarBlobNode(gittarPrefixOpenApi+realinode, orgIDStr, req.UserID)
	if err != nil {
		return nil, apierrors.ErrGetGittarFileTreeNode.InternalError(err)
	}

	// 获取文件commit信息
	commitMessage, err := svc.bdl.GetGittarTree(gittarPrefixOpenApi+treeRepo, orgIDStr, req.UserID)
	if err != nil {
		return nil, apierrors.ErrGetGittarFileTreeNode.InternalError(err)
	}

	// 获取 snippetConfigName
	snippetConfigName := ""
	for i := 4 + branchExcessLength; i < len(pathSplit); i++ {
		snippetConfigName += "/" + pathSplit[i]
	}

	// 根据分支获取 workspace
	branch := getBranchStr(inode)
	workspace, err := svc.GetWorkspaceByBranch(pathSplit[0], branch)
	if err != nil {
		return nil, err
	}

	return &apistructs.UnifiedFileTreeNode{
		Type:      apistructs.UnifiedFileTreeNodeTypeFile,
		Inode:     req.Inode,
		UpdaterID: commitMessage.Commit.Committer.Name,
		UpdatedAt: commitMessage.Commit.Committer.When,
		Desc:      commitMessage.Commit.CommitMessage,
		Name:      strings.ReplaceAll(strings.ReplaceAll(pathSplit[len(pathSplit)-1], ".", "-"), "/", "-"),
		Meta: map[string]interface{}{
			apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml: context,
			apistructs.AutoTestFileTreeNodeMetaKeySnippetAction: apistructs.PipelineYmlAction{
				SnippetConfig: &apistructs.SnippetConfig{
					Name:   snippetConfigName,
					Source: apistructs.SnippetSourceLocal,
					Labels: map[string]string{
						apistructs.LabelGittarYmlPath: pipeline.GetGittarYmlNamesLabels(app.Name, workspace, branch, snippetConfigName[1:]),
					},
				},
				Type: "snippet",
			},
		},
	}, nil
}

func (svc *GittarFileTree) GetWorkspaceByBranch(projectIDStr, branch string) (string, error) {
	projectID, err := strconv.Atoi(projectIDStr)
	if err != nil {
		return "", err
	}

	rules, err := svc.branchRuleSve.Query(apistructs.ProjectScope, int64(projectID))
	if err != nil {
		return "", err
	}
	// get workspace by branch
	wsByBranch, err := diceworkspace.GetByGitReference(branch, rules)
	if err != nil {
		return "", err
	}
	return wsByBranch.String(), nil
}

func (svc *GittarFileTree) DeleteFileTreeNode(req apistructs.UnifiedFileTreeNodeDeleteRequest, orgID uint64, userID string) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrDeleteGittarFileTreeNode.InvalidParameter(err)
	}

	// 解析
	inodeBytes, err := base64.URLEncoding.DecodeString(req.Inode)
	if err != nil {
		return nil, apierrors.ErrDeleteGittarFileTreeNode.InvalidParameter(err)
	}
	inode := string(inodeBytes)
	inodeSplit := strings.Split(inode, "/")

	// 因为分支是 feature/sss/sss 这种模式根据 / 分割就会有问题
	branchExcessLength := getBranchExcessLength(inode)

	var length = len(inodeSplit)
	if length < 4+branchExcessLength {
		return nil, fmt.Errorf("wrong format inode error: length too short")
	}

	appID, err := strconv.ParseUint(inodeSplit[1], 10, 64)
	if err != nil {
		return nil, fmt.Errorf("invalid appID: %s, err: %v", inodeSplit[1], err)
	}
	app, err := svc.bdl.GetApp(appID)
	if err != nil {
		return nil, fmt.Errorf("failed to get app, appID: %d, err: %v", appID, err)
	}

	// path 是给 gittar commit 用的地址，地址是除了 项目名分支名和应用名
	var path = ""
	for i := 4 + branchExcessLength; i < length; i++ {
		path += inodeSplit[i] + "/"
	}
	path = path[:len(path)-1]

	// 这里认定以 .yml 结尾的就是文件，其他都是文件夹
	var pathType string
	if strings.HasSuffix(path, ".yml") {
		pathType = "blob"
	} else {
		pathType = "tree"
	}

	var request = apistructs.GittarCreateCommitRequest{
		Branch:  getBranchStr(inode),
		Message: "delete file" + path,
		Actions: []apistructs.EditActionItem{
			{
				Action:   "delete",
				PathType: pathType,
				Path:     path,
			},
		},
	}
	resp, err := svc.bdl.CreateGittarCommitV2(fmt.Sprintf("wb/%s/%s", app.ProjectName, app.Name), request, int(orgID), userID)
	if err != nil {
		return nil, err
	}

	if !resp.Success {
		return nil, fmt.Errorf("delete filetree node error: %v", resp.Error)
	}
	return nil, nil
}

func (svc *GittarFileTree) FuzzySearchFileTreeNodes(req apistructs.UnifiedFileTreeNodeFuzzySearchRequest, orgID uint64) ([]apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrFuzzySearchGittarFileTreeNodes.InvalidParameter(err)
	}

	// 项目id转化
	projectID, err := strconv.ParseInt(req.ScopeID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
	}

	// 查询项目下都应用列表
	apps, err := svc.bdl.GetAppsByProjectSimple(uint64(projectID), orgID, req.UserID)
	if err != nil {
		return nil, apierrors.ErrListGittarFileTreeNodes.InternalError(err)
	}
	if len(apps.List) <= 0 {
		return nil, nil
	}
	projectName := apps.List[0].ProjectName

	var results []apistructs.UnifiedFileTreeNode
	// 异步设置值
	if req.FromPinode == "" {
		results, err = svc.searchGittarYmlList(apps.List, orgID, projectName, "", "", projectID, req.UserID)
		if err != nil {
			return nil, err
		}
	} else {
		inodeBytes, err := base64.URLEncoding.DecodeString(req.FromPinode)
		if err != nil {
			return nil, apierrors.ErrGetGittarFileTreeNode.InternalError(err)
		}

		pathSplit := strings.Split(string(inodeBytes), "/")
		length := len(pathSplit)

		var filterAppName string
		var filterBranch string
		if length == 2 {
			// 第二个为appId的字符串, 获取其名称
			appID, err := strconv.ParseUint(pathSplit[1], 10, 64)
			if err != nil {
				return nil, fmt.Errorf("invalid appID: %s, err: %v", pathSplit[1], err)
			}
			app, err := svc.bdl.GetApp(appID)
			if err != nil {
				return nil, fmt.Errorf("failed to get app, appID: %d, err: %v", appID, err)
			}
			filterAppName = app.Name
		} else if length >= 3 {
			filterBranch = getBranchStr(string(inodeBytes))
		}

		results, err = svc.searchGittarYmlList(apps.List, orgID, projectName, filterAppName, filterBranch, projectID, req.UserID)
		if err != nil {
			return nil, err
		}
	}

	// 过滤查询
	var filterResult []apistructs.UnifiedFileTreeNode
	for _, v := range results {
		if !strings.Contains(v.Name, req.Fuzzy) {
			continue
		}
		filterResult = append(filterResult, v)
	}

	return filterResult, nil
}

func (svc *GittarFileTree) CreateFileTreeNode(req apistructs.UnifiedFileTreeNodeCreateRequest, orgID uint64) (*apistructs.UnifiedFileTreeNode, error) {
	// 创建前校验
	node, err := svc.validateNodeBeforeCreate(req)
	if err != nil {
		return nil, apierrors.ErrCreateGittarFileTreeNode.InvalidParameter(err)
	}

	// 解析
	pinodeBytes, err := base64.URLEncoding.DecodeString(node.Pinode)
	if err != nil {
		return nil, apierrors.ErrCreateGittarFileTreeNode.InvalidParameter(err)
	}

	pinode := string(pinodeBytes)
	pinodeSplit := strings.Split(pinode, "/")

	// 因为分支是 feature/sss/sss 这种模式根据 / 分割就会有问题
	branchExcessLength := getBranchExcessLength(pinode)

	// 父节点一定是 /project/app/blob(tree)/branch
	var length = len(pinodeSplit)
	if length < 4+branchExcessLength {
		return nil, fmt.Errorf("wrong format pinode error: length too short")
	}

	// 获取各个信息
	appID := pinodeSplit[1]
	branch := getBranchStr(pinode)

	// 获取project的name 和app的name
	appIDUint, err := strconv.ParseUint(appID, 10, 64)
	if err != nil {
		return nil, fmt.Errorf("invalid appID: %s, err: %v", appID, err)
	}
	app, err := svc.bdl.GetApp(appIDUint)
	if err != nil {
		return nil, fmt.Errorf("failed to get app, appID: %v, err: %v", appID, err)
	}

	// 假如是创建文件
	var request = apistructs.GittarCreateCommitRequest{}
	if node.Type == apistructs.UnifiedFileTreeNodeTypeDir {
		// .dice 只能是 pinode 为 /project/app/blob(tree)/branch
		if length == 4+branchExcessLength && node.Name != ".dice" && node.Name != ".erda" {
			return nil, fmt.Errorf("error create folder： only '.erda' or '.dice' files can be created under the branch")
		}
		// pipelines 只能是 pinode 为 /project/app/blob(tree)/branch/.dice
		if length == 5+branchExcessLength && node.Name != "pipelines" {
			return nil, fmt.Errorf("error create folder： only 'pipelines' files can be created under the .erda or .dice")
		}

		var path = ""
		for i := 4 + branchExcessLength; i < length; i++ {
			path += pinodeSplit[i] + "/"
		}
		path += node.Name

		// 否则文件夹就随便创建
		request.Branch = branch
		request.Actions = []apistructs.EditActionItem{
			{
				Action:   "add",
				Path:     path,
				PathType: gittarEntryTreeType,
			},
		}
		request.Message = "add new directory"
	} else {
		// 这里就是文件创建逻辑，目前只能创建.yml文件
		if !strings.HasSuffix(node.Name, ".yml") {
			return nil, fmt.Errorf("only '.yml' suffix file can be created")
		}

		request.Branch = branch
		request.Actions = []apistructs.EditActionItem{}
		request.Message = "add " + node.Name
		// set default yml content
		action := apistructs.EditActionItem{
			Action:   "add",
			PathType: gittarEntryBlobType,
			Content:  "version: \"1.1\"\nstages: []\n",
		}

		// 假如长度为4，那就是应用的根目录下，就只能创建pipeline.yml文件了
		if length == 4+branchExcessLength {
			if node.Name != "pipeline.yml" {
				return nil, fmt.Errorf("only 'pipeline.yml' can be created under the branch")
			}
			action.Path = node.Name
			request.Actions = append(request.Actions, action)
		} else {
			// 否则长度就要大于6  /project/app/blob(tree)/branch/.dice/pipelines 的父目录下才能创建文件
			if length >= 6+branchExcessLength {
				// 获取除了branch的path
				var path = ""
				for i := 4 + branchExcessLength; i < length; i++ {
					path += pinodeSplit[i] + "/"
				}
				path += node.Name

				action.Path = path
				request.Actions = append(request.Actions, action)
			} else {
				return nil, fmt.Errorf("error create file： create .yml files can only be in the .dice/pipelines folder")
			}
		}

	}

	// 创建文件
	resp, err := svc.bdl.CreateGittarCommitV2(fmt.Sprintf("wb/%s/%s", app.ProjectName, app.Name), request, int(orgID), req.UserID)
	if err != nil {
		return nil, err
	}
	if !resp.Success {
		return nil, fmt.Errorf("create filetree node error: %v", resp.Error)
	}

	if node.Type == apistructs.UnifiedFileTreeNodeTypeDir {
		return &apistructs.UnifiedFileTreeNode{
			Type:      apistructs.UnifiedFileTreeNodeTypeDir,
			Inode:     base64.URLEncoding.EncodeToString([]byte(pinode + "/" + node.Name)),
			Pinode:    node.Pinode,
			Name:      node.Name,
			Scope:     node.Scope,
			ScopeID:   node.ScopeID,
			Desc:      node.Desc,
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
			CreatorID: node.CreatorID,
			UpdaterID: node.UpdaterID,
		}, nil
	}

	var treeNodeGetRequest = apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:   base64.URLEncoding.EncodeToString([]byte(pinode + "/" + node.Name)),
		ScopeID: node.ScopeID,
		Scope:   node.Scope,
	}
	treeNodeGetRequest.IdentityInfo = req.IdentityInfo
	return svc.GetFileTreeNode(treeNodeGetRequest, orgID)
}

func (svc *GittarFileTree) validateNodeBeforeCreate(req apistructs.UnifiedFileTreeNodeCreateRequest) (*apistructs.AutoTestFileTreeNode, error) {
	// 构造 node
	node := apistructs.AutoTestFileTreeNode{
		Type:      req.Type,
		Scope:     req.Scope,
		ScopeID:   req.ScopeID,
		Pinode:    req.Pinode,
		Name:      req.Name,
		Desc:      req.Desc,
		CreatorID: req.IdentityInfo.UserID,
		UpdaterID: req.IdentityInfo.UserID,
	}
	// 参数校验
	if !req.Type.Valid() {
		return nil, fmt.Errorf("invalid node type: %s", req.Type.String())
	}
	if req.Type.IsDir() {
		if req.Pinode == "" {
			return nil, fmt.Errorf("pinode can not be empty")
		} else {
			// non-root dir
			if err := req.ValidateNonRootDir(); err != nil {
				return nil, err
			}
		}
	}
	if req.Type.IsFile() {
		if err := req.ValidateFile(); err != nil {
			return nil, err
		}
	}
	// 字段最大长度校验
	if err := strutil.Validate(node.Name, strutil.MaxLenValidator(apistructs.MaxSetNameLen)); err != nil {
		return nil, err
	}
	if err := strutil.Validate(node.Desc, strutil.MaxLenValidator(apistructs.MaxSetDescLen)); err != nil {
		return nil, err
	}

	// 分配 inode
	node.Inode = uuid.SnowFlakeID()
	return &node, nil
}

// 查询项目应用分支的节点内容列表
// apps 项目下的 app 列表
// orgID 企业id 用作获取yml内容的
// projectName 项目的名称
// filterAppName 应用名称，没有传递就是查询项目下所有的app节点内容
// filterBranchName 分支名称 没有传递就是查询所有的分支节点内容
// projectID 项目的id
func (svc *GittarFileTree) searchGittarYmlList(apps []apistructs.ApplicationDTO, orgID uint64, projectName string,
	filterAppName string, filterBranchName string, projectID int64, userID string) ([]apistructs.UnifiedFileTreeNode, error) {

	var wait sync.WaitGroup
	var results []apistructs.UnifiedFileTreeNode
	var warn error

	for _, app := range apps {
		// 不为空且不相等就查询全部
		if filterAppName != "" && app.Name != filterAppName {
			continue
		}

		wait.Add(1)
		// 异步查询 app 中的分支
		go func(app apistructs.ApplicationDTO) {
			defer wait.Done()

			list, err := svc.searchBranch(app, orgID, projectName, filterBranchName, projectID, userID)
			if err != nil {
				warn = err
				return
			}
			if list == nil || len(list) <= 0 {
				return
			}
			results = append(results, list...)
		}(app)

	}

	wait.Wait()

	if warn != nil {
		return nil, warn
	}
	return results, nil
}

// 根据分支查询
// branchName 用作过滤
func (svc *GittarFileTree) searchBranch(app apistructs.ApplicationDTO, orgID uint64, projectName string, branchName string, projectID int64, userID string) ([]apistructs.UnifiedFileTreeNode, error) {
	var warn error
	var results []apistructs.UnifiedFileTreeNode
	var realPinode = projectName + "/" + app.Name
	// 查询应用下的分支
	branchs, err := svc.bdl.GetGittarBranchesV2(gittarPrefixOpenApi+realPinode, strconv.Itoa(int(orgID)), true, userID)
	if err != nil {
		return nil, err
	}
	if branchs == nil || len(branchs) <= 0 {
		return nil, nil
	}

	// 异步查询分支中的 yml
	var branchGroupWait sync.WaitGroup
	for _, branch := range branchs {

		// 分支不为空且不想等就跳过
		if branchName != "" && branch != branchName {
			continue
		}

		branchGroupWait.Add(1)
		go func(branch string) {
			defer branchGroupWait.Done()

			list, err := svc.searchYmlContext(app, orgID, projectName, branch, projectID, userID)
			if err != nil {
				warn = err
				return
			}
			if list == nil || len(list) <= 0 {
				return
			}
			results = append(results, list...)
		}(branch)
	}

	// 等待 branch 查询
	branchGroupWait.Wait()

	if warn != nil {
		return nil, warn
	}

	return results, nil
}

func (svc *GittarFileTree) GetGittarFileByPipelineId(pipelineId uint64, orgID uint64, identity apistructs.IdentityInfo) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	pipelineDetail, err := svc.bdl.GetPipeline(pipelineId)
	if err != nil {
		return nil, err
	}

	labels := pipelineDetail.Labels
	appID := labels[apistructs.LabelAppID]
	projectID := labels[apistructs.LabelProjectID]
	branch := labels[apistructs.LabelBranch]

	var ymlName string
	// the name starting with dice-deploy-release is the pipeline deployed by the deployment center，
	// Users do not have an address to see the details of the pipeline, and can only simulate to the pipeline.yml of the corresponding branch first，
	// todo remove the code when developing other pipeline detail interfaces later
	if strings.HasPrefix(pipelineDetail.YmlName, "dice-deploy-release") {
		ymlName = "pipeline.yml"
	} else {
		names := strings.Split(pipelineDetail.YmlName, branch)
		ymlName = pipelineDetail.YmlName
		if len(names) >= 2 {
			ymlName = names[1]
		}
		ymlName = strings.TrimPrefix(ymlName, "/")
	}

	inode := fmt.Sprintf("%s/%s/tree/%s/%s", projectID, appID, branch, ymlName)
	base64Inode := base64.URLEncoding.EncodeToString([]byte(inode))

	var req apistructs.UnifiedFileTreeNodeGetRequest
	req.Inode = base64Inode
	req.IdentityInfo = identity
	return svc.GetFileTreeNode(req, orgID)
}

// 根据分支下的yml列表，查询所有的内容
// branch 是分支名称
func (svc *GittarFileTree) searchYmlContext(app apistructs.ApplicationDTO, orgID uint64, projectName string, branch string, projectID int64, userID string) ([]apistructs.UnifiedFileTreeNode, error) {

	var results []apistructs.UnifiedFileTreeNode
	var warn error
	var wait sync.WaitGroup

	// 查询分支下所有的yml列表
	list := pipeline.GetPipelineYmlList(apistructs.CICDPipelineYmlListRequest{
		AppID:  int64(app.ID),
		Branch: branch,
	}, svc.bdl, userID)

	if list == nil && len(list) <= 0 {
		return nil, nil
	}

	for _, ymlPath := range list {
		wait.Add(1)

		go func(ymlPath string) {
			defer wait.Done()
			searchINode := projectName + "/" + app.Name + "/" + gittarEntryBlobType + "/" + branch + "/" + ymlPath
			// 获取文件内容
			context, err := svc.bdl.GetGittarBlobNode(gittarPrefixOpenApi+searchINode, strconv.Itoa(int(orgID)), userID)
			if err != nil {
				warn = err
				return
			}

			var pathSplit = strings.Split(ymlPath, "/")

			inode := strconv.Itoa(int(projectID)) + "/" + strconv.Itoa(int(app.ID)) + "/" + gittarEntryBlobType + "/" + branch + "/" + ymlPath
			results = append(results, apistructs.UnifiedFileTreeNode{
				Name:  pathSplit[len(pathSplit)-1],
				Type:  apistructs.UnifiedFileTreeNodeTypeFile,
				Inode: base64.URLEncoding.EncodeToString([]byte(inode)),
				Meta: map[string]interface{}{
					apistructs.AutoTestFileTreeNodeMetaKeyPipelineYml: context,
					apistructs.AutoTestFileTreeNodeMetaKeySnippetAction: apistructs.SnippetConfig{
						Name:   "/" + ymlPath,
						Source: apistructs.SnippetSourceLocal,
						Labels: map[string]string{
							apistructs.LabelAppID:  strconv.Itoa(int(app.ID)),
							apistructs.LabelBranch: branch,
						},
					},
				},
			})

		}(ymlPath)
	}

	wait.Wait()
	if warn != nil {
		return nil, warn
	}

	return results, nil
}

func (svc *GittarFileTree) FindFileTreeNodeAncestors(req apistructs.UnifiedFileTreeNodeFindAncestorsRequest) ([]apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrFindGittarFileTreeNodeAncestors.InvalidParameter(err)
	}

	var results []apistructs.UnifiedFileTreeNode
	inodeBytes, err := base64.URLEncoding.DecodeString(req.Inode)
	if err != nil {
		return nil, apierrors.ErrFindGittarFileTreeNodeAncestors.InvalidParameter(err)
	}
	branch := getBranchStr(string(inodeBytes))
	err = recursivelyFindAncestors(req.Inode, &results, branch)
	if err != nil {
		return nil, apierrors.ErrFindGittarFileTreeNodeAncestors.InvalidParameter(err)
	}
	return results, nil
}

func recursivelyFindAncestors(inode string, ancestors *[]apistructs.UnifiedFileTreeNode, branch string) error {
	// inode == 0 或者 ""
	if inode == "" || inode == "MA==" {
		return nil
	}

	// 解析
	inodeBytes, err := base64.URLEncoding.DecodeString(inode)
	if err != nil {
		return apierrors.ErrFindGittarFileTreeNodeAncestors.InvalidParameter(err)
	}
	inodeStr := string(inodeBytes)
	// 把分支用占位符替换掉
	inodeStr = strings.Replace(inodeStr, branch, "${branch}", 1)

	inodeSplit := strings.Split(inodeStr, "/")
	var length = len(inodeSplit)
	var pinode = ""
	// 把 a/b/tree 中的 tree 去除
	if length == 3 {
		inode = base64.URLEncoding.EncodeToString([]byte(inodeSplit[0] + "/" + inodeSplit[1]))
		length = 2
	}
	// 去除最后一个文件名或者文件夹名称
	for i := 0; i < length-1; i++ {
		pinode = pinode + inodeSplit[i] + "/"
	}
	// 把最后面的 / 去处
	if strings.HasSuffix(pinode, "/") {
		pinode = pinode[:len(pinode)-1]
	}

	// 长度为 3 的时候去除 tree
	pinodeSplit := strings.Split(pinode, "/")
	if len(pinodeSplit) == 3 {
		pinode = strings.ReplaceAll(pinode, "/tree", "")
	}

	if pinode == "" {
		pinode = "0"
	}

	// 把分支用占位符替换回来
	pinode = strings.Replace(pinode, "${branch}", branch, 1)
	pinode = base64.URLEncoding.EncodeToString([]byte(pinode))
	*ancestors = append(*ancestors, apistructs.UnifiedFileTreeNode{
		Inode:  inode,
		Pinode: pinode,
	})
	err = recursivelyFindAncestors(pinode, ancestors, branch)
	return err
}

func branchConvertToUnifiedFileTreeNode(branch string, scope, scopeID, decodePInode, pinode string) *apistructs.UnifiedFileTreeNode {

	// 根据绝对路径base64下，转化成一个唯一id
	inode := decodePInode + "/" + gittarEntryTreeType + "/" + branch
	encryptINode := base64.URLEncoding.EncodeToString([]byte(inode))

	return &apistructs.UnifiedFileTreeNode{
		Type:    apistructs.UnifiedFileTreeNodeTypeDir,
		Scope:   scope,
		ScopeID: scopeID,
		Inode:   encryptINode,
		Pinode:  pinode,
		Name:    branch,
	}
}

func entryConvertToUnifiedFileTreeNode(entry *apistructs.TreeEntry, scope, scopeID, decodePInode, pinode string) *apistructs.UnifiedFileTreeNode {

	// 根据绝对路径base64下，转化成一个唯一id
	inode := decodePInode + "/" + entry.Name
	encryptINode := base64.URLEncoding.EncodeToString([]byte(inode))

	node := &apistructs.UnifiedFileTreeNode{
		Scope:   scope,
		ScopeID: scopeID,
		Inode:   encryptINode,
		Pinode:  pinode,
		Name:    entry.Name,
	}
	if entry.Type == gittarEntryTreeType {
		node.Type = apistructs.UnifiedFileTreeNodeTypeDir
	} else {
		node.Type = apistructs.UnifiedFileTreeNodeTypeFile
	}

	return node
}

func appConvertToUnifiedFileTreeNode(app *apistructs.ApplicationDTO, scope, scopeID, projectID string) *apistructs.UnifiedFileTreeNode {

	// 根据绝对路径base64下，转化成一个唯一id
	inode := projectID + "/" + strconv.Itoa(int(app.ID))
	encryptINode := base64.URLEncoding.EncodeToString([]byte(inode))

	return &apistructs.UnifiedFileTreeNode{
		Type:      apistructs.UnifiedFileTreeNodeTypeDir,
		Scope:     scope,
		ScopeID:   scopeID,
		Inode:     encryptINode,
		Pinode:    apistructs.RootPinode,
		Name:      app.Name,
		Desc:      app.Desc,
		CreatorID: app.Creator,
		UpdaterID: app.Creator,
		CreatedAt: app.CreatedAt,
		UpdatedAt: app.UpdatedAt,
		Meta:      nil,
	}
}

func getBranchExcessLength(name string) int {
	return len(strings.Split(getBranchStr(name), "/")) - 1
}

func getBranchStr(name string) string {
	splitValue := strings.SplitN(name, "/", 4)
	if len(splitValue) != 4 {
		return name
	}

	rightNameValue := splitValue[3]

	var afterIndex = -1
	if strings.HasSuffix(rightNameValue, "/.dice") {
		afterIndex = strings.LastIndex(rightNameValue, "/.dice")
	} else {
		afterIndex = strings.LastIndex(rightNameValue, "/.dice/")
	}

	if afterIndex == -1 {
		if strings.HasSuffix(rightNameValue, "/.erda") {
			afterIndex = strings.LastIndex(rightNameValue, "/.erda")
		} else {
			afterIndex = strings.LastIndex(rightNameValue, "/.erda/")
		}
	}

	if afterIndex == -1 {
		afterIndex = strings.LastIndex(rightNameValue, "/pipeline.yml")
	}

	if afterIndex == -1 {
		return rightNameValue
	}
	return rightNameValue[:afterIndex]
}
