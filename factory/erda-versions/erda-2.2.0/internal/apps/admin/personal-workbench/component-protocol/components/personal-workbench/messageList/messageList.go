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

package messageList

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/list"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/list/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common/gshelper"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
	"github.com/erda-project/erda/pkg/strutil"
	rt "github.com/erda-project/erda/pkg/time/readable_time"
)

const (
	CompMessageList = "messageList"

	DefaultPageNo   uint64 = 1
	DefaultPageSize uint64 = 10
)

type MessageList struct {
	impl.DefaultList

	sdk   *cptype.SDK
	bdl   *bundle.Bundle
	wbSvc *workbench.Workbench

	identity  apistructs.Identity
	filterReq apistructs.WorkbenchMsgRequest
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, CompMessageList, func() servicehub.Provider {
		return &MessageList{}
	})
}

func (l *MessageList) Initialize(sdk *cptype.SDK) {}

func (l *MessageList) Finalize(sdk *cptype.SDK) {
	sdk.SetUserIDs(l.StdDataPtr.UserIDs)
}

func (l *MessageList) BeforeHandleOp(sdk *cptype.SDK) {
	// get svc info
	l.sdk = sdk
	l.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	l.wbSvc = sdk.Ctx.Value(types.WorkbenchSvc).(*workbench.Workbench)

	// set component version
	l.sdk.Comp.Version = "2"

	// get identity info
	l.identity = apistructs.Identity{
		UserID: sdk.Identity.UserID,
		OrgID:  sdk.Identity.OrgID,
	}

	// get global related stat info
	gh := gshelper.NewGSHelper(sdk.GlobalState)
	tp, _ := gh.GetMsgTabName()

	// construct filter info, check & set default value
	l.filterReq = apistructs.WorkbenchMsgRequest{
		Type: tp,
		PageRequest: apistructs.PageRequest{
			PageNo:   DefaultPageNo,
			PageSize: DefaultPageSize,
		},
	}
}

func (l *MessageList) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		l.StdDataPtr = l.doFilter()
		return nil
	}
}

func (l *MessageList) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return l.RegisterInitializeOp()
}

// RegisterChangePage when change page, filter needed
func (l *MessageList) RegisterChangePage(opData list.OpChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {

		if opData.ClientData.PageNo > 0 {
			l.filterReq.PageNo = opData.ClientData.PageNo
		}
		if opData.ClientData.PageSize > 0 {
			l.filterReq.PageSize = opData.ClientData.PageSize
		}
		l.StdDataPtr = l.doFilter()
		return nil
	}
}

// RegisterItemStarOp when item stared, unnecessary here
func (l *MessageList) RegisterItemStarOp(opData list.OpItemStar) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (l *MessageList) RegisterItemClickGotoOp(opData list.OpItemClickGoto) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}
func (l *MessageList) RegisterBatchOp(opData list.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {

		return nil
	}
}

// RegisterItemClickOp get client data, and set message read
func (l *MessageList) RegisterItemClickOp(opData list.OpItemClick) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		idNum, err := strconv.Atoi(opData.ClientData.DataRef.ID)
		if err != nil {
			logrus.Errorf("parse message client data failed, id: %v, error: %v", opData.ClientData.DataRef.ID, err)
			return nil
		}
		id := int64(idNum)
		orgID, err := strconv.ParseUint(l.identity.OrgID, 10, 64)
		if err != nil {
			panic(err)
		}
		switch opData.ClientData.OperationRef.ID {
		case "approved", "denied":
			b, err := json.Marshal(map[string]interface{}{"status": opData.ClientData.OperationRef.ID})
			if err != nil {
				panic(err)
			}
			if _, err := l.bdl.UpdateApprove(orgID, l.identity.UserID, id, bytes.NewReader(b)); err != nil {
				panic(err)
			}
		case "approveDeploy", "rejectDeploy":
			if err := l.bdl.UpdateManualApproval(l.identity.OrgID, &apistructs.UpdateApproval{
				Id:     id,
				Reject: opData.ClientData.OperationRef.ID == "rejectDeploy",
			}); err != nil {
				panic(err)
			}
		default:
			req := apistructs.SetMBoxReadStatusRequest{
				IDs: []int64{id},
			}
			err = l.bdl.SetMBoxReadStatus(l.identity, &req)
			if err != nil {
				logrus.Errorf("set mbox read status filed, id: %v, error: %v", id, err)
				return nil
			}
		}
		l.StdDataPtr = l.doFilter()
		return nil
	}
}

func (l *MessageList) doFilter() (data *list.Data) {
	data = &list.Data{}

	switch l.filterReq.Type {
	case apistructs.WorkbenchItemUnreadMes:
		return l.doFilterMsg()
	case apistructs.WorkbenchItemApproveRequest:
		return l.doFilterApproval()
	default:
		logrus.Errorf("item [%v] not support", l.filterReq.Type)
	}
	return
}

func (l *MessageList) doFilterMsg() (data *list.Data) {
	data = &list.Data{}

	// list unread message
	req := apistructs.QueryMBoxRequest{PageNo: int64(l.filterReq.PageNo),
		PageSize: int64(l.filterReq.PageSize),
		Status:   apistructs.MBoxUnReadStatus,
		Type:     apistructs.MBoxTypeIssue,
	}
	ms, err := l.bdl.ListMbox(l.identity, req)
	if err != nil {
		logrus.Errorf("list unread messages failed, identity: %+v,request: %+v, error:%v", l.identity, req, err)
		return
	}

	data = &list.Data{
		PageNo:   l.filterReq.PageNo,
		PageSize: l.filterReq.PageSize,
		Total:    uint64(ms.Total),
		Operations: map[cptype.OperationKey]cptype.Operation{
			list.OpChangePage{}.OpKey(): cputil.NewOpBuilder().Build(),
		},
	}

	var ids []uint64
	for _, v := range ms.List {
		id, err := getIssueID(v.DeduplicateID)
		if err != nil {
			logrus.Warnf("get issue id failed, error: %v", err)
		}
		ids = append(ids, id)
	}
	streamMap, err := l.wbSvc.ListIssueStreams(ids, 0)
	if err != nil {
		logrus.Warnf("list issue streams failed, ids: %v, error: %v", ids, err)
	}

	for _, p := range ms.List {
		var stream apistructs.IssueStream
		issueID, _ := getIssueID(p.DeduplicateID)
		if c, ok := streamMap[issueID]; ok {
			stream = c
		} else {
			continue
		}

		item := list.Item{
			ID:           strconv.FormatInt(p.ID, 10),
			Title:        p.Title,
			TitleSummary: strconv.FormatInt(p.UnreadCount, 10),
			MainState:    &list.StateInfo{Status: common.UnreadMsgStatus},
			Description:  stream.Content,
			Selectable:   true,
			ColumnsInfo: map[string]interface{}{
				"users": []string{stream.Operator},
				"text": []map[string]string{{
					"tip":  stream.UpdatedAt.Format("2006-01-02"),
					"text": l.getReadableTimeText(stream.UpdatedAt),
				}},
			},
			Operations: genClickGotoServerData(p),
		}
		if stream.Operator == "" {
			logrus.Errorf("stream operator is empty, content: %+v", stream)
		} else {
			data.UserIDs = append(data.UserIDs, stream.Operator)
		}
		data.List = append(data.List, item)
	}
	data.UserIDs = strutil.DedupSlice(data.UserIDs)
	return
}

func (l *MessageList) getReadableTimeText(t time.Time) string {
	r := rt.Readable(t).String()
	s := strings.Split(r, " ")
	if len(s) == 2 {
		return l.sdk.I18n(r)
	} else {
		return fmt.Sprintf("%v %v", s[0], l.sdk.I18n(strings.Join(s[1:], " ")))
	}
}

func getIssueID(deduplicateID string) (uint64, error) {
	if deduplicateID == "" {
		return 0, errors.New("deduplicate id is nil")
	}
	sli := strings.SplitN(deduplicateID, "-", 2)
	idStr := sli[1]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		logrus.Errorf("parse deduplicateID failed, content: %v, error: %v", deduplicateID, err)
		return 0, err
	}
	return uint64(id), nil
}

func genClickGotoServerData(i *apistructs.MBox) (data map[cptype.OperationKey]cptype.Operation) {
	data = make(map[cptype.OperationKey]cptype.Operation)

	sd := list.OpItemBasicServerData{}

	// content check
	if strings.HasPrefix(i.Content, "http") {
		sd = list.OpItemBasicServerData{
			JumpOut: true,
			// url link
			Target: i.Content,
		}
	} else {
		logrus.Errorf("content not prefix with http, mbox id: %v", i.ID)
		return

	}
	data = map[cptype.OperationKey]cptype.Operation{
		list.OpItemClick{}.OpKey(): cputil.NewOpBuilder().Build(),
		list.OpItemClickGoto{}.OpKey(): cputil.NewOpBuilder().
			WithSkipRender(true).
			WithServerDataPtr(sd).Build(),
	}
	return
}
