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

package db

import (
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type ArchiveDeleteRequest struct {
	Statuses       []string
	NotStatuses    []string
	EndTimeCreated time.Time
}

func (client *Client) CreatePipelineArchive(archive *spec.PipelineArchive, ops ...dbclient.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.InsertOne(archive)
	return err
}

func (client *Client) GetPipelineArchiveByPipelineID(pipelineID uint64, ops ...dbclient.SessionOption) (spec.PipelineArchive, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	archive := spec.PipelineArchive{PipelineID: pipelineID}
	exist, err := session.Get(&archive)
	return archive, exist, err
}

func (client *Client) GetPipelineFromArchive(pipelineID uint64, ops ...dbclient.SessionOption) (spec.Pipeline, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	archive, exist, err := client.GetPipelineArchiveByPipelineID(pipelineID, ops...)
	if err != nil {
		return spec.Pipeline{}, false, err
	}
	if !exist {
		return spec.Pipeline{}, false, nil
	}

	return archive.Content.Pipeline, true, nil
}

// GetPipelineIncludeArchived return: pipeline, exist, findFromArchive, error
func (client *Client) GetPipelineIncludeArchived(pipelineID uint64, ops ...dbclient.SessionOption) (spec.Pipeline, bool, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	p, exist, err := client.GetPipelineWithExistInfo(pipelineID, ops...)
	if err != nil {
		return spec.Pipeline{}, false, false, err
	}
	if exist {
		return p, true, false, nil
	}

	// find from archive
	ap, findFromArchive, err := client.GetPipelineFromArchive(pipelineID, ops...)
	if err != nil {
		return spec.Pipeline{}, false, false, err
	}
	return ap, findFromArchive, findFromArchive, err
}

func (client *Client) GetPipelineTasksFromArchive(pipelineID uint64, ops ...dbclient.SessionOption) ([]spec.PipelineTask, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	archive, _, err := client.GetPipelineArchiveByPipelineID(pipelineID, ops...)
	return archive.Content.PipelineTasks, err
}

// GetPipelineTasksIncludeArchived return: tasks, findFromArchive, error
func (client *Client) GetPipelineTasksIncludeArchived(pipelineID uint64, ops ...dbclient.SessionOption) ([]spec.PipelineTask, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	tasks, err := client.ListPipelineTasksByPipelineID(pipelineID, ops...)
	if err != nil {
		return nil, false, err
	}
	if len(tasks) > 0 {
		return tasks, false, nil
	}

	// find from archive
	tasks, err = client.GetPipelineTasksFromArchive(pipelineID, ops...)
	if err != nil {
		return nil, false, err
	}
	return tasks, true, nil
}

func (client *Client) ArchivePipeline(pipelineID uint64) (_ uint64, err error) {
	// tx
	txSession := client.NewSession()
	defer txSession.Close()
	if err := txSession.Begin(); err != nil {
		return 0, err
	}
	defer func() {
		if err != nil {
			rbErr := txSession.Rollback()
			if rbErr != nil {
				logrus.Errorf("[alert] failed to rollback when archivePipeline failed, pipelineID: %d, rollbackErr: %v",
					pipelineID, rbErr)
			}
			return
		}
		cmErr := txSession.Commit()
		if cmErr != nil {
			logrus.Errorf("[alert] failed to commit when archivePipeline success, pipelineID: %d, commitErr: %v",
				pipelineID, cmErr)
		}
	}()

	ops := dbclient.WithTxSession(txSession.Session)

	// pipeline
	p, err := client.GetPipeline(pipelineID, ops)
	if err != nil {
		return 0, err
	}
	// 校验当前流水线是否可被归档
	can, reason := p.CanArchive()
	if !can {
		return 0, fmt.Errorf("cannot archive, reason: %s", reason)
	}
	// pipeline_labels
	labels, err := client.ListLabelsByPipelineID(pipelineID, ops)
	if err != nil {
		return 0, err
	}
	// pipeline_stages
	stages, err := client.ListPipelineStageByPipelineID(pipelineID, ops)
	if err != nil {
		return 0, err
	}
	// pipeline_tasks
	tasks, err := client.ListPipelineTasksByPipelineID(pipelineID, ops)
	if err != nil {
		return 0, err
	}
	// pipeline_reports
	reports, err := client.BatchListPipelineReportsByPipelineID([]uint64{pipelineID}, nil, ops)
	if err != nil {
		return 0, err
	}

	archive := spec.PipelineArchive{
		PipelineID:      pipelineID,
		PipelineSource:  p.PipelineSource,
		PipelineYmlName: p.PipelineYmlName,
		Status:          p.Status,
		DiceVersion:     version.Version,
		Content: spec.PipelineArchiveContent{
			Pipeline:        p,
			PipelineLabels:  labels,
			PipelineStages:  stages,
			PipelineTasks:   tasks,
			PipelineReports: reports[pipelineID],
		},
	}

	// check
	if !p.Extra.CompleteReconcilerGC {
		for _, task := range tasks {
			// uuid 不为空，表示已经在实质上调用了 executor 创建了资源，需要 gc namespace
			if task.Extra.UUID != "" {
				return 0, err
			}
		}
	}

	// create
	if err := client.CreatePipelineArchive(&archive, ops); err != nil {
		if err := txSession.Rollback(); err != nil {
			logrus.Errorf("[alert] failed to rollback when CreatePipelineArchive failed, err: %v", err)
		}
		return 0, err
	}

	// delete
	if err := client.DeletePipelineRelated(pipelineID, ops); err != nil {
		return 0, err
	}

	return archive.ID, nil
}

func tableFieldName(tableName string, field string) string {
	return fmt.Sprintf("%v.%v", tableName, field)
}

func (client *Client) DeletePipelineArchives(req ArchiveDeleteRequest, ops ...dbclient.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()
	if req.EndTimeCreated.IsZero() {
		return errors.New("invalid param: endTimeCreated")
	}
	baseSQL := session.Table(&spec.PipelineArchive{}).
		Where(tableFieldName((&spec.PipelineArchive{}).TableName(), "time_created")+" <= ?", req.EndTimeCreated)
	if len(req.Statuses) > 0 {
		baseSQL = baseSQL.In(tableFieldName((&spec.PipelineArchive{}).TableName(), "status"), req.Statuses)
	}
	if len(req.NotStatuses) > 0 {
		baseSQL = baseSQL.NotIn(tableFieldName((&spec.PipelineArchive{}).TableName(), "status"), req.NotStatuses)
	}
	if _, err := baseSQL.Delete(&spec.PipelineArchive{}); err != nil {
		return err
	}
	return nil
}
