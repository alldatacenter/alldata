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

package fileTree

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

const (
	OperationKeyClickBranchExpandChildren = "branchExpandChildren"

	mobileInitBranchKey = "mobile_init_branch"
	mobileInitKey       = "mobile_init"

	I18nLocalePrefixKey                = "wb.content.pipeline.file.tree."
	defaultPipelineI18nKey             = "defaultPipeline"
	addDefaultPipelineI18nKey          = "addDefaultPipeline"
	deleteI18nKey                      = "delete"
	isDeleteI18nKey                    = "isDelete"
	addDefaultI18nKey                  = "addDefault"
	canNotDeleteDefaultPipelineI18nKey = "canNotDeleteDefaultPipeline"
	addPipelineI18nKey                 = "addPipeline"
	openI18nKey                        = "open"

	mobileInitBranch   = "mobileInitBranch"
	mobileInitializing = "mobileInitializing"
)

// GetUserPermission  check Guest permission
func (ca *ComponentFileTree) CheckUserPermission() (bool, error) {
	bdl := ca.CtxBdl
	appId := bdl.InParams["appId"].(string)
	appID, err := strconv.ParseUint(appId, 10, 64)
	if err != nil {
		return false, err
	}
	access, err := bdl.Bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   bdl.Identity.UserID,
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: apistructs.NormalBranchResource,
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return true, err
	}
	if !access.Access {
		return true, nil
	}
	return false, nil
}

func (a *ComponentFileTree) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, globalState *apistructs.GlobalStateData) (err error) {
	a.Type = c.Type
	if event.Operation != apistructs.InitializeOperation && event.Operation != apistructs.RenderingOperation {
		err = a.unmarshal(c)
		if err != nil {
			return err
		}
	}

	defer func() {
		fail := a.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	err = a.SetBundle(bdl)
	if err != nil {
		return err
	}

	a.Disabled, err = a.CheckUserPermission()
	if err != nil {
		return err

	}

	if a.CtxBdl.InParams == nil {
		return fmt.Errorf("params is emprtt")
	}

	inParamsBytes, err := json.Marshal(a.CtxBdl.InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", a.CtxBdl.InParams, err)
	}

	var inParams InParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}

	project, err := GetOrgIdByProjectId(a.CtxBdl, inParams.ProjectId)
	if err != nil {
		return err
	}
	switch event.Operation {
	case apistructs.FileTreeAddDefaultOperationsKey:
		if err := a.handlerAddDefault(bdl, inParams, *project, event); err != nil {
			return err
		}
	case apistructs.FileTreeDeleteOperationKey:
		if err := a.handlerDelete(bdl, inParams, *project, event); err != nil {
			return err
		}
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		if err := a.handlerDefaultValue(bdl, inParams, *project); err != nil {
			return err
		}
	case apistructs.FileTreeSubmitOperationKey:
		err := a.handlerAddNodeResult(a.State.NodeFormModalAddNode)
		if err != nil {
			return err
		}
	case OperationKeyClickBranchExpandChildren:
		if err := a.handleClickBranchExpandChildren(bdl, inParams, *project, event); err != nil {
			return err
		}
	}
	return
}

func (a *ComponentFileTree) handlerAddNodeResult(NodeFormModalAddNode NodeFormModalAddNode) error {
	for index, v := range a.Data {
		if v.Title == NodeFormModalAddNode.Branch {
			a.Data[index].Children = append(a.Data[index].Children, a.getNodeByResult(NodeFormModalAddNode.Results))
			break
		}
	}

	selectKey, expandedKeys := findSelectedKeysExpandedKeys(a.Data, NodeFormModalAddNode.Results.Inode)
	a.State.SelectedKeys = selectKey
	a.State.ExpandedKeys = expandedKeys

	return nil
}

func (a *ComponentFileTree) getNodeByResult(result apistructs.UnifiedFileTreeNode) Data {
	i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)

	var node Data
	node.Title = result.Name
	node.Icon = "dm"
	node.IsLeaf = true
	node.Key = result.Inode
	var deleteOperation = DeleteOperation{
		Key:     "delete",
		Text:    i18nLocale.Get(I18nLocalePrefixKey + deleteI18nKey),
		Confirm: i18nLocale.Get(I18nLocalePrefixKey + isDeleteI18nKey),
		Reload:  true,
		Meta: DeleteOperationData{
			Key: result.Inode,
		},
		Disabled: a.Disabled,
	}
	node.Operations = map[string]interface{}{}
	node.Operations["delete"] = deleteOperation
	return node
}

func (a *ComponentFileTree) marshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(a.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	var data apistructs.ComponentData = map[string]interface{}{}
	data["treeData"] = a.Data
	c.Data = data
	c.State = state
	c.Type = a.Type
	return nil
}

func (a *ComponentFileTree) unmarshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state State
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	var data []Data
	dataJson, err := json.Marshal(c.Data["treeData"])
	if err != nil {
		return err
	}
	err = json.Unmarshal(dataJson, &data)
	if err != nil {
		return err
	}
	a.State = state
	a.Type = c.Type
	a.Data = data
	return nil
}

func (a *ComponentFileTree) handlerAddDefault(ctxBdl protocol.ContextBundle, inParams InParams, project apistructs.ProjectDTO, event apistructs.ComponentEvent) (err error) {
	data := event.OperationData
	operationData, err := json.Marshal(data)
	if err != nil {
		return err
	}
	var addDefaultOperations AddDefaultOperations
	err = json.Unmarshal(operationData, &addDefaultOperations)
	if err != nil {
		return err
	}

	key := strings.Trim(addDefaultOperations.Meta.Key, "")
	if key == "" {
		return fmt.Errorf("add node key can not be empty: %s", key)
	}

	decodeInode, err := base64.URLEncoding.DecodeString(key)
	if err != nil {
		return fmt.Errorf("decode key error: %s", key)
	}
	inode := string(decodeInode)

	if strings.Contains(inode, ".dice/pipelines/") || strings.Contains(inode, ".erda/pipelines/") || !strings.HasSuffix(inode, "/pipeline.yml") {
		return fmt.Errorf("add default key format error")
	}

	pinode := strings.ReplaceAll(inode, "/pipeline.yml", "")

	result, err := createDefault(ctxBdl, inParams.ProjectId, base64.URLEncoding.EncodeToString([]byte(pinode)), project.OrgID)
	if err != nil {
		return err
	}
	a.resetDefaultKey(result)
	selectKey, expandedKeys := findSelectedKeysExpandedKeys(a.Data, key)
	a.State.ExpandedKeys = expandedKeys
	a.State.SelectedKeys = selectKey
	return nil
}

func createDefault(ctxBdl protocol.ContextBundle, projectId string, pinode string, orgId uint64) (result *apistructs.UnifiedFileTreeNode, err error) {
	var req apistructs.UnifiedFileTreeNodeCreateRequest
	req.Scope = apistructs.FileTreeScopeProjectApp
	req.ScopeID = projectId
	req.Type = apistructs.UnifiedFileTreeNodeTypeFile
	req.Pinode = pinode
	req.Name = "pipeline.yml"
	req.UserID = ctxBdl.Identity.UserID
	return ctxBdl.Bdl.CreateFileTreeNodes(req, orgId)
}

func (a *ComponentFileTree) resetDefaultKey(result *apistructs.UnifiedFileTreeNode) {
	data := a.getDefaultYmlByPipelineYmlNode(result, "")
	for k, v := range a.Data {
		for childK, child := range v.Children {
			if child.Key == result.Inode {
				a.Data[k].Children[childK] = data
				return
			}
		}
	}

}

func (a *ComponentFileTree) handlerDelete(ctxBdl protocol.ContextBundle, inParams InParams, project apistructs.ProjectDTO, event apistructs.ComponentEvent) (err error) {
	data := event.OperationData
	operationData, err := json.Marshal(data)
	if err != nil {
		return err
	}
	var deleteOperation DeleteOperation
	err = json.Unmarshal(operationData, &deleteOperation)
	if err != nil {
		return err
	}
	key := strings.Trim(deleteOperation.Meta.Key, "")
	if key == "" {
		return fmt.Errorf("delete node key can not be empty: %s", key)
	}

	decodeInode, err := base64.URLEncoding.DecodeString(key)
	if err != nil {
		return fmt.Errorf("decode key error: %s", key)
	}
	inode := string(decodeInode)

	if !strings.Contains(inode, ".dice/pipelines/") && !strings.Contains(inode, ".erda/pipelines/") && strings.HasSuffix(inode, "/pipeline.yml") {
		return fmt.Errorf("cannot delete default node")
	}
	var req apistructs.UnifiedFileTreeNodeDeleteRequest
	req.ScopeID = inParams.ProjectId
	req.Scope = apistructs.FileTreeScopeProjectApp
	req.Inode = key
	req.UserID = ctxBdl.Identity.UserID
	_, err = ctxBdl.Bdl.DeleteFileTreeNodes(req, project.OrgID)
	if err != nil {
		return fmt.Errorf("delete node fail: %s", err)
	}

	for index, v := range a.Data {
		for childKey, child := range v.Children {
			if child.Key == key {
				a.Data[index].Children = append(a.Data[index].Children[:childKey], a.Data[index].Children[childKey+1:]...)
				break
			}
		}
	}

	selectKey, expandedKeys := findSelectedKeysExpandedKeys(a.Data, inParams.SelectedKeys)
	a.State.SelectedKeys = selectKey
	a.State.ExpandedKeys = expandedKeys
	return nil
}

func (a *ComponentFileTree) handleClickBranchExpandChildren(ctxBdl protocol.ContextBundle, inParams InParams, project apistructs.ProjectDTO, event apistructs.ComponentEvent) error {
	if event.OperationData == nil {
		return nil
	}
	var operationData ClickBranchNodeOperation
	b, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &operationData); err != nil {
		return err
	}
	if operationData.Meta.ParentKey == "" {
		return fmt.Errorf("no parent key in meta")
	}
	// 从 data 中找到匹配的目录节点
	for i, branchNode := range a.Data {
		if branchNode.Key != operationData.Meta.ParentKey {
			continue
		}
		subNodes, err := a.listBranchSubNodes(branchNode.Key, branchNode.Key, ctxBdl, inParams, project)
		if err != nil {
			return fmt.Errorf("failed to list branch sub nodes, err: %v", err)
		}
		a.Data[i].Children = subNodes
		a.State.ExpandedKeys = append(a.State.ExpandedKeys, branchNode.Key)
		break
	}
	return nil
}

func (a *ComponentFileTree) handlerDefaultValue(ctxBdl protocol.ContextBundle, inParams InParams, project apistructs.ProjectDTO) error {

	fileTreeData, err := a.getFileTreeData(ctxBdl, inParams, project)
	if err != nil {
		return err
	}

	// when the branch list here is empty and it is still a mobile application,
	// simulate a virtual branch and pipeline so that the front end can view the log of application initialization
	if len(fileTreeData) <= 0 {
		appID, err := strconv.ParseUint(inParams.AppId, 10, 64)
		if err != nil {
			return err
		}

		dto, err := ctxBdl.Bdl.GetApp(appID)
		if err != nil {
			return err
		}

		if dto.Mode == string(apistructs.ApplicationModeMobile) {
			i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)

			var node = Data{}
			node.Key = mobileInitBranchKey
			node.Icon = "fz"
			node.ClickToExpand = true
			node.IsLeaf = false
			node.Title = i18nLocale.Get(I18nLocalePrefixKey + mobileInitBranch)
			node.Selectable = false
			var clickToExpand = ClickBranchNodeOperation{
				Key:    OperationKeyClickBranchExpandChildren,
				Text:   i18nLocale.Get(I18nLocalePrefixKey + openI18nKey),
				Reload: true,
				Show:   false,
				Meta: ClickBranchNodeOperationMeta{
					ParentKey: mobileInitBranchKey,
				},
			}
			node.Operations = map[string]interface{}{}
			node.Operations["click"] = clickToExpand

			var childNode = Data{
				Key:        mobileInitKey,
				Title:      i18nLocale.Get(I18nLocalePrefixKey + mobileInitializing),
				Selectable: true,
				Icon:       "dm",
				IsLeaf:     true,
				Operations: nil,
			}
			node.Children = append(node.Children, childNode)
			fileTreeData = append(fileTreeData, node)
		}
	}

	selectKey, expandedKeys := findSelectedKeysExpandedKeys(fileTreeData, inParams.SelectedKeys)

	a.Type = "FileTree"
	a.Data = fileTreeData
	a.State = State{
		ExpandedKeys: expandedKeys,
		SelectedKeys: selectKey,
	}

	//forFileTree:
	//	for index, v := range a.Data {
	//		for childKey, child := range v.Children {
	//			for _, key := range selectKey {
	//				// 假如是默认流水线 且是虚的就创建
	//				if child.Key == key && child.Icon == "tj1" {
	//					node := a.Data[index].Children[childKey]
	//					pinode := base64.URLEncoding.EncodeToString([]byte(fmt.Sprintf("%s/%s/tree/%s", inParams.ProjectId, inParams.AppId, v.Title)))
	//					_, err := createDefault(a.CtxBdl, inParams.ProjectId, pinode, project.OrgID)
	//					if err != nil {
	//						return fmt.Errorf("create default pipeline error: %v", err)
	//					}
	//
	//					node.Title = "默认流水线"
	//					node.Icon = "dm"
	//					var deleteOperation = DeleteOperation{
	//						Key:         "delete",
	//						Text:        "删除",
	//						Disabled:    true,
	//						DisabledTip: "默认流水线无法删除",
	//					}
	//					node.Operations = []interface{}{deleteOperation}
	//					a.Data[index].Children[childKey] = node
	//					break forFileTree
	//				}
	//			}
	//		}
	//	}

	return nil
}

func (a *ComponentFileTree) getFileTreeData(ctxBdl protocol.ContextBundle, inParams InParams, project apistructs.ProjectDTO) ([]Data, error) {

	// 查询分支
	var req apistructs.UnifiedFileTreeNodeListRequest
	req.Scope = apistructs.FileTreeScopeProjectApp
	req.ScopeID = inParams.ProjectId
	req.Pinode = base64.URLEncoding.EncodeToString([]byte(fmt.Sprintf("%s/%s", inParams.ProjectId, inParams.AppId)))
	req.UserID = ctxBdl.Identity.UserID
	branchSlice, err := ctxBdl.Bdl.ListFileTreeNodes(req, project.OrgID)
	if err != nil {
		return nil, err
	}
	if len(branchSlice) == 0 {
		return nil, nil
	}

	var dirNodes []Data

	// 所有分支转换为目录节点
	for _, branch := range branchSlice {
		dirNodes = append(dirNodes, a.getNodeByBranch(branch))
	}
	if len(dirNodes) == 0 {
		return nil, nil
	}

	if len(inParams.SelectedKeys) > 0 {
		// 节点寻祖
		anReq := apistructs.UnifiedFileTreeNodeFindAncestorsRequest{
			Inode:   inParams.SelectedKeys,
			Scope:   apistructs.FileTreeScopeProjectApp,
			ScopeID: inParams.AppId,
		}
		anReq.UserID = ctxBdl.Identity.UserID
		ancestors, err := ctxBdl.Bdl.FindFileTreeNodeAncestors(anReq, project.OrgID)
		if err != nil {
			return nil, fmt.Errorf("failed to find ancestor nodes, err: %v", err)
		}
		logrus.Info("len ancestors: ", len(ancestors))
		for _, each := range ancestors {
			for i, eachone := range branchSlice {
				if each.Inode == eachone.Inode {
					logrus.Info("SelectedKeys: ", inParams.SelectedKeys, "   ancestors: ", each.Name, "    branchSlice: ", eachone.Name)
					subNodes, err := a.listBranchSubNodes(eachone.Name, each.Inode, ctxBdl, inParams, project)
					if err != nil {
						return nil, fmt.Errorf("failed to list branch sub nodes, err: %v", err)
					}
					dirNodes[i].Children = subNodes

					return dirNodes, nil
				}
			}
		}
	}

	// 展开第一个目录，查询子节点
	subNodes, err := a.listBranchSubNodes(branchSlice[0].Name, branchSlice[0].Inode, ctxBdl, inParams, project)
	if err != nil {
		return nil, fmt.Errorf("failed to list branch sub nodes, err: %v", err)
	}
	dirNodes[0].Children = subNodes

	return dirNodes, nil
}

func (a *ComponentFileTree) listBranchSubNodes(branch string, branchInode string, ctxBdl protocol.ContextBundle, inParams InParams, project apistructs.ProjectDTO) ([]Data, error) {
	// 解析出inode
	parsedBranchInode, err := decodeInode(branchInode)
	if err != nil {
		return nil, err
	}
	// subNodes
	var subNodes []Data
	// 将 pipeline.yml 转化为叶子节点
	childData, err := a.getDefaultChild(ctxBdl, inParams, project.OrgID, branchInode)
	if err != nil {
		return nil, err
	}
	subNodes = append(subNodes, childData)
	// .dice/pipelines 下的 yml 转化为其他叶子节点
	for _, v := range a.getOtherFolderChild(branch, ctxBdl, inParams, project.OrgID, parsedBranchInode) {
		if !strings.HasSuffix(v.Title, ".yml") {
			continue
		}
		subNodes = append(subNodes, *v)
	}
	return subNodes, nil
}

func findSelectedKeysExpandedKeys(fileTreeData []Data, selectedKeys string) ([]string, []string) {
	// 返回查询到的 key
	for _, v := range fileTreeData {
		for _, child := range v.Children {
			if child.Key == selectedKeys {
				if child.Icon == "tj1" {
					return []string{}, []string{v.Key}
				}
				//fileTreeData[key].Children[childKey].Selectable = true
				return []string{selectedKeys}, []string{v.Key}
			}
		}
	}

	// 没有找到就返回第一个 key
	for _, v := range fileTreeData {
		for _, child := range v.Children {
			if child.Icon == "tj1" {
				return []string{}, []string{v.Key}
			}
			//fileTreeData[key].Children[childKey].Selectable = true
			return []string{child.Key}, []string{v.Key}
		}
		return []string{}, []string{}
	}

	return []string{}, nil
}

func (a *ComponentFileTree) getOtherFolderChild(branch string, ctxBdl protocol.ContextBundle, inParams InParams, orgId uint64, parsedBranchInode string) []*Data {
	i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)
	// 查询分支下的 .dice/pipelines 下的 yml 文件
	var req apistructs.UnifiedFileTreeNodeListRequest
	req.Scope = apistructs.FileTreeScopeProjectApp
	req.ScopeID = inParams.ProjectId
	req.UserID = ctxBdl.Identity.UserID
	appID, err := strconv.ParseUint(inParams.AppId, 10, 64)
	if err != nil {
		return []*Data{}
	}
	parsedBranchInode += "/" + a.CtxBdl.Bdl.GetPipelineGittarFolder(req.UserID, appID, branch)
	req.Pinode = base64.URLEncoding.EncodeToString([]byte(parsedBranchInode))
	ymls, _ := ctxBdl.Bdl.ListFileTreeNodes(req, orgId)

	var childSlice = make([]*Data, 0)
	for _, v := range ymls {
		var deleteOperation = DeleteOperation{
			Key:     "delete",
			Text:    i18nLocale.Get(I18nLocalePrefixKey + deleteI18nKey),
			Confirm: i18nLocale.Get(I18nLocalePrefixKey + isDeleteI18nKey),
			Reload:  true,
			Meta: DeleteOperationData{
				Key: v.Inode,
			},
			Disabled: a.Disabled,
		}
		var child = Data{
			Key:        v.Inode,
			Title:      v.Name,
			Selectable: true,
			Icon:       "dm",
			IsLeaf:     true,
			Operations: map[string]interface{}{"delete": deleteOperation},
		}
		childSlice = append(childSlice, &child)
	}
	return childSlice
}

func (a *ComponentFileTree) getDefaultChild(ctxBdl protocol.ContextBundle, inParams InParams, orgId uint64, branchInode string) (Data, error) {
	// 查询分支下的 pipeline.yml
	var req apistructs.UnifiedFileTreeNodeListRequest
	req.Scope = apistructs.FileTreeScopeProjectApp
	req.ScopeID = inParams.ProjectId
	req.Pinode = branchInode
	req.UserID = ctxBdl.Identity.UserID
	pipelineYmls, err := ctxBdl.Bdl.ListFileTreeNodes(req, orgId)
	if err != nil {
		return Data{}, err
	}

	var defaultNode *apistructs.UnifiedFileTreeNode
	for _, v := range pipelineYmls {
		if v.Name == "pipeline.yml" {
			defaultNode = &v
			break
		}
	}
	branchInodeDecode, err := base64.URLEncoding.DecodeString(branchInode)
	if err != nil {
		return Data{}, err
	}

	var defaultKey = string(branchInodeDecode) + "/pipeline.yml"

	return a.getDefaultYmlByPipelineYmlNode(defaultNode, base64.URLEncoding.EncodeToString([]byte(defaultKey))), nil
}

func decodeInode(inode string) (string, error) {
	// 解析出inode
	pinodeBytes, err := base64.URLEncoding.DecodeString(inode)
	if err != nil {
		return "", fmt.Errorf("decode inode %s error: %v", inode, err)
	}
	branchInode := string(pinodeBytes)
	return branchInode, nil
}
func (a *ComponentFileTree) getDefaultYmlByPipelineYmlNode(defaultNode *apistructs.UnifiedFileTreeNode, defaultKey string) Data {
	i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)

	var node Data
	node.IsLeaf = true
	node.Operations = map[string]interface{}{}
	if defaultNode == nil {
		node.Icon = "tj1"
		node.Selectable = true
		node.Key = defaultKey
		node.Title = i18nLocale.Get(I18nLocalePrefixKey + addDefaultPipelineI18nKey)
		var addDefaultOperations = AddDefaultOperations{
			Key:    "addDefault",
			Text:   i18nLocale.Get(I18nLocalePrefixKey + addDefaultI18nKey),
			Reload: true,
			Show:   false,
			Meta: AddDefaultOperationData{
				Key: defaultKey,
			},
			Disabled: a.Disabled,
		}
		node.Operations["click"] = addDefaultOperations
	} else {
		node.Selectable = true
		node.Title = i18nLocale.Get(I18nLocalePrefixKey + defaultPipelineI18nKey)
		node.Icon = "dm"
		node.Key = defaultNode.Inode
		var deleteOperation = DeleteOperation{
			Key:         "delete",
			Text:        i18nLocale.Get(I18nLocalePrefixKey + deleteI18nKey),
			Disabled:    true,
			DisabledTip: i18nLocale.Get(I18nLocalePrefixKey + canNotDeleteDefaultPipelineI18nKey),
		}
		node.Operations["delete"] = deleteOperation
	}
	return node
}

func (a *ComponentFileTree) getNodeByBranch(branch apistructs.UnifiedFileTreeNode) Data {
	i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)

	var node Data
	node.Key = branch.Inode
	node.Icon = "fz"
	node.ClickToExpand = true
	node.IsLeaf = false
	node.Title = branch.Name
	node.Selectable = false
	var addNode = AddNodeOperation{
		Key:    "addNode",
		Text:   i18nLocale.Get(I18nLocalePrefixKey + addPipelineI18nKey),
		Reload: false,
		Command: AddNodeOperationCommand{
			Key:    "set",
			Target: "nodeFormModal",
			State: AddNodeOperationCommandState{
				Visible: true,
				FormData: AddNodeOperationCommandStateFormData{
					Branch: branch.Name,
				},
			},
		},
		Disabled: a.Disabled,
	}
	var clickToExpand = ClickBranchNodeOperation{
		Key:    OperationKeyClickBranchExpandChildren,
		Text:   i18nLocale.Get(I18nLocalePrefixKey + openI18nKey),
		Reload: true,
		Show:   false,
		Meta: ClickBranchNodeOperationMeta{
			ParentKey: branch.Inode,
		},
	}
	node.Operations = map[string]interface{}{}
	node.Operations["addNode"] = addNode
	node.Operations["click"] = clickToExpand
	return node
}

func GetOrgIdByProjectId(CtxBdl protocol.ContextBundle, projectId string) (*apistructs.ProjectDTO, error) {
	projectIdInt, err := strconv.Atoi(projectId)
	if err != nil {
		return nil, fmt.Errorf("projectId show be number type: %v", err)
	}
	project, err := CtxBdl.Bdl.GetProject(uint64(projectIdInt))
	if err != nil {
		return nil, fmt.Errorf("get project error: %v", err)
	}
	return project, nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentFileTree{}
}
