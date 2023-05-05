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
	"fmt"
	"net/http"
	"strconv"

	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) RecordTypeList(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	return mkResponse(apistructs.RecordTypeListResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: []apistructs.RecordTypeData{
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeImportKubernetesCluster)),
				RawRecordType: string(dbclient.RecordTypeImportKubernetesCluster),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeRmNodes)),
				RawRecordType: string(dbclient.RecordTypeRmNodes),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeDeleteNodes)),
				RawRecordType: string(dbclient.RecordTypeDeleteNodes),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateClusterCredential)),
				RawRecordType: string(dbclient.RecordTypeCreateClusterCredential),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeResetClusterCredential)),
				RawRecordType: string(dbclient.RecordTypeResetClusterCredential),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeDeleteEssNodes)),
				RawRecordType: string(dbclient.RecordTypeDeleteEssNodes),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeDeleteEssNodesCronJob)),
				RawRecordType: string(dbclient.RecordTypeDeleteEssNodesCronJob),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeAddNodes)),
				RawRecordType: string(dbclient.RecordTypeAddNodes),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeAddEssNodes)),
				RawRecordType: string(dbclient.RecordTypeAddEssNodes),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeSetLabels)),
				RawRecordType: string(dbclient.RecordTypeSetLabels),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeUpgradeEdgeCluster)),
				RawRecordType: string(dbclient.RecordTypeUpgradeEdgeCluster),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeAddAliCSECluster)),
				RawRecordType: string(dbclient.RecordTypeAddAliACKECluster),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeAddAliCSManagedCluster)),
				RawRecordType: string(dbclient.RecordTypeAddAliCSManagedCluster),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeAddAliECSECluster)),
				RawRecordType: string(dbclient.RecordTypeAddAliECSECluster),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeAddAliNodes)),
				RawRecordType: string(dbclient.RecordTypeAddAliNodes),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeOfflineEdgeCluster)),
				RawRecordType: string(dbclient.RecordTypeOfflineEdgeCluster),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateAliCloudMysql)),
				RawRecordType: string(dbclient.RecordTypeCreateAliCloudMysql),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateAliCloudMysqlDB)),
				RawRecordType: string(dbclient.RecordTypeCreateAliCloudMysqlDB),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateAliCloudOns)),
				RawRecordType: string(dbclient.RecordTypeCreateAliCloudOns),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateAliCloudOnsTopic)),
				RawRecordType: string(dbclient.RecordTypeCreateAliCloudOnsTopic),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateAliCloudRedis)),
				RawRecordType: string(dbclient.RecordTypeCreateAliCloudRedis),
			},
			{
				RecordType:    i18n.Sprintf(string(dbclient.RecordTypeCreateAliCloudOss)),
				RawRecordType: string(dbclient.RecordTypeCreateAliCloudOss),
			},
		},
	})
}

func (e *Endpoints) Query(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	i18nPrinter := ctx.Value("i18nPrinter").(*message.Printer)

	recordIDs := strutil.Split(r.URL.Query().Get("recordIDs"), ",", true)
	clusterNames := strutil.Split(r.URL.Query().Get("clusterName"), ",", true)
	statuses := strutil.Split(r.URL.Query().Get("status"), ",", true)
	userIDs := strutil.Split(r.URL.Query().Get("userIDs"), ",", true)
	recordTypes := strutil.Split(r.URL.Query().Get("recordType"), ",", true)
	pageSize := r.URL.Query().Get("pageSize")
	pageNo := r.URL.Query().Get("pageNo")
	orgID := r.Header.Get("Org-ID")
	scope := r.URL.Query().Get("scope")

	pageSize_ := 100
	if pageSize != "" {
		var err error
		pageSize_, err = strconv.Atoi(pageSize)
		if err != nil {
			errstr := fmt.Sprintf("failed to parse 'pageSize' arg")
			return mkResponse(apistructs.RecordsResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errstr},
				},
			})
		}
	}
	pageNo_ := 0
	if pageNo != "" {
		var err error
		pageNo_, err = strconv.Atoi(pageNo)
		if err != nil {
			errstr := fmt.Sprintf("failed to parse 'pageNo' arg")
			return mkResponse(apistructs.RecordsResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errstr},
				},
			})
		}
	}

	var req apistructs.RecordRequest
	// if scope is system, search log in all org
	if scope == "system" {
		req.OrgID = ""
	} else {
		req.OrgID = orgID
	}
	req.PageSize = pageSize_
	req.PageNo = pageNo_
	req.RecordIDs = recordIDs
	req.ClusterNames = clusterNames
	req.RecordTypes = recordTypes
	req.Statuses = statuses
	req.UserIDs = userIDs

	rsp, err := e.nodes.Query(req)
	if err != nil {
		return mkResponse(apistructs.RecordsResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	result := apistructs.RecordsResponse{
		Header:         apistructs.Header{Success: true},
		UserInfoHeader: rsp.UserInfoHeader,
		Data:           rsp.Data,
	}

	// i18n update
	for i, r := range result.Data.List {
		data := &result.Data.List[i]
		data.RecordType = i18nPrinter.Sprintf(r.RecordType)
		if data.PipelineDetail != nil {
			for i, stage := range r.PipelineDetail.PipelineStages {
				for j, task := range stage.PipelineTasks {
					taskName := task.Name
					data.PipelineDetail.PipelineStages[i].PipelineTasks[j].Name = i18nPrinter.Sprintf(taskName)
				}
			}
		}
	}
	return mkResponse(result)
}
