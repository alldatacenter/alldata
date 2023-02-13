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

package nodes

import (
	"errors"
	"fmt"
	"sort"
	"strconv"
	"time"

	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/strutil"
)

func (n *Nodes) Sync(rs []dbclient.Record) ([]dbclient.Record, error) {
	result := []dbclient.Record{}
	for _, record := range rs {
		if record.Status == dbclient.StatusTypeSuccess || record.Status == dbclient.StatusTypeFailed ||
			record.Status == dbclient.StatusTypeUnknown {
			result = append(result, record)
			continue
		}

		dto, err := n.bdl.GetPipeline(record.PipelineID)
		if err != nil && strutil.Contains(err.Error(), "not found") {
			// this record can't select in pipelines, reset to status unknown.
			record.Status = dbclient.StatusTypeUnknown
			if err := n.db.RecordsWriter().Update(record); err != nil {
				logrus.Error(err.Error())
			}
		} else if err != nil {
			errstr := fmt.Sprintf("failed to get pipeline info: %v", err)
			logrus.Errorf(errstr)
			return nil, errors.New(errstr)
		} else {
			if len(dto.PipelineStages) == 0 {
				errstr := fmt.Sprintf("len(dto.PipelineStages) == 0, pipelineid: %d", record.PipelineID)
				logrus.Errorf(errstr)
				return nil, errors.New(errstr)
			}
			if len(dto.PipelineStages[0].PipelineTasks) == 0 {
				errstr := fmt.Sprintf("len(dto.PipelineStages[0].PipelineTasks) == 0, pipelineid: %d", record.PipelineID)
				logrus.Errorf(errstr)
				return nil, errors.New(errstr)
			}
			task := dto.PipelineStages[0].PipelineTasks[0]
			if task.Status.IsEndStatus() {
				if task.Status.IsSuccessStatus() {
					record.Status = dbclient.StatusTypeSuccess
				} else {
					record.Status = dbclient.StatusTypeFailed
				}
				if err := n.db.RecordsWriter().Update(record); err != nil {
					errstr := fmt.Sprintf("failed to update record: %v", err)
					logrus.Errorf(errstr)
					return nil, err
				}
			}
		}
		// TODO: add metafile detail into record
		result = append(result, record)
	}
	return result, nil
}

type RecordWithPipeline struct {
	dbclient.Record
	*apistructs.PipelineDetailDTO
}

func (n *Nodes) Merge(rs []dbclient.Record) ([]RecordWithPipeline, error) {
	r := []RecordWithPipeline{}
	for _, record := range rs {
		if record.PipelineID == 0 {
			r = append(r, RecordWithPipeline{Record: record})
			continue
		}
		dto, err := n.bdl.GetPipeline(record.PipelineID)
		if err != nil && strutil.Contains(err.Error(), "not found") {
			logrus.Warnf("not found pipeline: %d", record.PipelineID)
			r = append(r, RecordWithPipeline{Record: record})
			continue
		}
		if err != nil {
			return nil, err
		}
		if len(dto.PipelineStages) == 0 {
			errstr := fmt.Sprintf("len(dto.PipelineStages) == 0, pipelineid: %d", record.PipelineID)
			logrus.Errorf(errstr)
			return nil, errors.New(errstr)
		}
		if len(dto.PipelineStages[0].PipelineTasks) == 0 {
			errstr := fmt.Sprintf("len(dto.PipelineStages[0].PipelineTasks) == 0, pipelineid: %d", record.PipelineID)
			logrus.Errorf(errstr)
			return nil, errors.New(errstr)
		}
		status := dbclient.StatusTypeSuccess
		for _, stage := range dto.PipelineStages {
			for _, task := range stage.PipelineTasks {
				if task.Status.IsFailedStatus() {
					status = dbclient.StatusTypeFailed
					break
				}
				if !task.Status.IsSuccessStatus() {
					status = dbclient.StatusTypeProcessing
					break
				}
			}
		}

		if status == dbclient.StatusTypeProcessing && record.CreatedAt.Before(time.Now().Add(-6*time.Hour)) {
			status = dbclient.StatusTypeFailed
			record.Detail = "timeout: 6h"
		}

		record.Status = status

		if err := n.db.RecordsWriter().Update(record); err != nil {
			errstr := fmt.Sprintf("failed to update record: %v", err)
			logrus.Errorf(errstr)
			return nil, err
		}
		r = append(r, RecordWithPipeline{Record: record, PipelineDetailDTO: dto})
	}
	return r, nil
}

func (n *Nodes) Query(req apistructs.RecordRequest) (*apistructs.RecordsResponseData, error) {
	reader := n.db.RecordsReader()
	if len(req.RecordIDs) > 0 {
		reader.ByIDs(req.RecordIDs...)
	}
	if len(req.ClusterNames) > 0 {
		reader.ByClusterNames(req.ClusterNames...)
	}
	if len(req.Statuses) > 0 {
		reader.ByStatuses(req.Statuses...)
	}
	if len(req.UserIDs) > 0 {
		reader.ByUserIDs(req.UserIDs...)
	}
	if len(req.RecordTypes) > 0 {
		reader.ByRecordTypes(req.RecordTypes...)
	}
	if len(req.PipelineIDs) > 0 {
		reader.ByPipelineIDs(req.PipelineIDs...)
	}
	if req.OrgID != "" {
		reader.ByOrgID(req.OrgID)
	}
	total, err := reader.Count()
	if err != nil {
		err := fmt.Errorf("failed to get total num of records: %v", err)
		logrus.Error(err.Error())
		return nil, err
	}
	records, err := reader.PageNum((req.PageNo - 1) * req.PageSize).PageSize(req.PageSize).Do()
	if err != nil {
		err := fmt.Errorf("failed to query records: %v", err)
		logrus.Error(err.Error())
		return nil, err
	}

	nodesRecords := []dbclient.Record{}
	otherRecords := []dbclient.Record{}
	for _, record := range records {
		if record.RecordType == dbclient.RecordTypeAddNodes || record.RecordType == dbclient.RecordTypeRmNodes {
			nodesRecords = append(nodesRecords, record)
		} else {
			otherRecords = append(otherRecords, record)
		}
	}
	recordsWithPipeline, err := n.Merge(records)
	if err != nil {
		err := fmt.Errorf("failed to sync records: %v", err)
		logrus.Error(err.Error())
		return nil, err
	}

	resultData := []apistructs.RecordData{}
	for _, r := range recordsWithPipeline {
		orgid, err := strconv.ParseUint(r.OrgID, 10, 64)
		if err != nil {
			err := fmt.Errorf("failed to parse record field orgid: %v", err)
			logrus.Error(err.Error())
			return nil, err
		}

		resultData = append(resultData, apistructs.RecordData{
			CreateTime:    r.CreatedAt,
			RecordID:      strconv.FormatUint(r.Record.ID, 10),
			RecordType:    string(r.RecordType),
			RawRecordType: string(r.RecordType),
			UserID:        r.UserID,
			OrgID:         orgid,
			ClusterName:   r.ClusterName,
			Status:        string(r.Status),
			Detail:        r.Detail,

			PipelineDetail: r.PipelineDetailDTO,
		})
	}

	resultUserIDs := []string{}
	for _, r := range resultData {
		resultUserIDs = append(resultUserIDs, r.UserID)
	}

	sort.Sort(sortRecords(resultData))
	result := apistructs.RecordsResponseData{
		UserInfoHeader: apistructs.UserInfoHeader{
			UserIDs: strutil.DedupSlice(resultUserIDs, true),
		},
		Data: apistructs.RecordsData{
			Total: total,
			List:  resultData,
		},
	}
	return &result, nil
}

func (n *Nodes) UpdateCronJobRecord(req apistructs.RecordUpdateRequest) error {

	var plr apistructs.PipelinePageListRequest

	pageSize := req.PageSize
	plr.PageSize = pageSize
	plr.Sources = append(plr.Sources, apistructs.PipelineSourceOps)
	plr.YmlNames = append(plr.YmlNames, fmt.Sprintf("%s-%s.yml", apistructs.DeleteEssNodesCronPrefix, req.ClusterName))
	// list pipelines
	rsp, err := n.bdl.PageListPipeline(plr)
	if err != nil {
		logrus.Errorf("list pipeline failed, request: %v, error: %v", req, err)
		return err
	}
	if rsp == nil || rsp.Total == 0 {
		logrus.Infof("list pipeline , empty response, cluster name: %v, request: %v", req.ClusterName, plr)
		return nil
	}
	// construct pipeline records status
	var pStatus []pipelineStatus
	var pipelineIDs []string
	for _, p := range rsp.Pipelines {
		if p.Status == apistructs.PipelineStatusAnalyzed {
			// omit analyzed status, because parent pipeline is always in analyzed status
			continue
		}
		pipelineID := p.ID
		pipelineIDs = append(pipelineIDs, strconv.FormatUint(pipelineID, 10))
		status := dbclient.StatusTypeSuccess
		if p.Status.IsFailedStatus() {
			status = dbclient.StatusTypeFailed
		}
		if !p.Status.IsSuccessStatus() {
			status = dbclient.StatusTypeProcessing
		}
		pStatus = append(pStatus, pipelineStatus{PipelineID: pipelineID, Status: status})
	}
	logrus.Debugf("cron pipeline status: %v", pStatus)
	// check whether pipeline exist in ops_record
	reader := n.db.RecordsReader()
	if req.OrgID != "" {
		reader.ByOrgID(req.OrgID)
	}
	if req.ClusterName != "" {
		reader.ByClusterNames([]string{req.ClusterName}...)
	}
	if req.UserID != "" {
		reader.ByUserIDs([]string{req.UserID}...)
	}
	if req.RecordType != "" {
		reader.ByRecordTypes([]string{string(dbclient.RecordTypeDeleteEssNodes)}...)
	}
	if len(pipelineIDs) > 0 {
		reader.ByPipelineIDs(pipelineIDs...)
	}
	idSet := set.New()
	recordMap := make(map[uint64]*dbclient.Record)
	records, err := reader.PageNum(0).PageSize(pageSize).Do()
	if err != nil {
		err := fmt.Errorf("ess cron job, failed to query records: %v", err)
		logrus.Error(err.Error())
		return err
	}
	for i, r := range records {
		idSet.Insert(r.PipelineID)
		recordMap[r.PipelineID] = &records[i]
	}
	logrus.Debugf("current pipeline status: %v", records)
	for _, s := range pStatus {
		if idSet.Has(s.PipelineID) {
			// update
			if recordMap[s.PipelineID].Status != s.Status {
				recordMap[s.PipelineID].Status = s.Status
				if err := n.db.RecordsWriter().Update(*recordMap[s.PipelineID]); err != nil {
					logrus.Errorf("ess cron job, update record failed, error: %v", err)
					return err
				}
			}
		} else {
			// insert
			_, err = n.db.RecordsWriter().Create(&dbclient.Record{
				RecordType:  dbclient.RecordTypeDeleteEssNodes,
				UserID:      req.UserID,
				OrgID:       req.OrgID,
				ClusterName: req.ClusterName,
				Status:      s.Status,
				Detail:      "",
				PipelineID:  s.PipelineID,
			})
			if err != nil {
				logrus.Errorf("ess cron job, insert record failed, error: %v", err)
				return err
			}
		}
	}

	return nil
}

type sortRecords []apistructs.RecordData

func (r sortRecords) Len() int {
	return len(r)
}

func (r sortRecords) Less(i, j int) bool {
	return r[i].CreateTime.After(r[j].CreateTime)
}

func (r sortRecords) Swap(i, j int) {
	r[i], r[j] = r[j], r[i]
}

type pipelineStatus struct {
	PipelineID uint64
	Status     dbclient.StatusType
}
