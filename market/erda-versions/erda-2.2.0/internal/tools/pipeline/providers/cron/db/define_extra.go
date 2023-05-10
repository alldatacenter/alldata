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
	"github.com/pkg/errors"
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func (client *Client) GetDBClient() (db *xorm.Engine) {
	return client.DB()
}

func (client *Client) ListPipelineCrons(enable *bool, ops ...mysqlxorm.SessionOption) ([]PipelineCron, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	if enable != nil {
		session.Where("enable=?", *enable)
	}
	var crons []PipelineCron
	err := session.Find(&crons)
	return crons, err
}

// return: result, total, nil
func (client *Client) PagingPipelineCron(req *pb.CronPagingRequest, ops ...mysqlxorm.SessionOption) ([]PipelineCron, int64, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	result := make([]PipelineCron, 0)
	var total int64 = 0

	if req.PageNo < 0 {
		return nil, -1, errors.Errorf("invalid pageNo: %d", req.PageNo)
	}
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize < 0 {
		return nil, -1, errors.Errorf("invalid pageSize: %d", req.PageSize)
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}

	limitSQL := session.Desc("id")
	// source
	// 必须指定 sources 或 allSources
	if !req.AllSources && len(req.Sources) == 0 {
		return nil, -1, errors.Errorf("source is empty")
	}
	if !req.AllSources {
		limitSQL.In("pipeline_source", req.Sources)
	}
	// ymlName
	if len(req.YmlNames) != 0 {
		limitSQL.In("pipeline_yml_name", req.YmlNames)
	}

	if req.Enable != nil {
		limitSQL.Where("enable = ?", req.Enable.Value)
	}

	if req.PipelineDefinitionID != nil {
		limitSQL.In("pipeline_definition_id", req.PipelineDefinitionID)
	}

	if req.ClusterName != "" {
		limitSQL.Where("cluster_name = ?", req.ClusterName)
	}

	// LIMIT ${pageSize} OFFSET ${pageNo}
	var err error
	if req.GetAll {
		total, err = limitSQL.FindAndCount(&result)
	} else {
		total, err = limitSQL.Limit(int(req.PageSize), int((req.PageNo-1)*req.PageSize)).FindAndCount(&result)
	}
	if err != nil {
		return nil, -1, err
	}

	return result, total, nil
}

func (client *Client) GetPipelineCron(id interface{}, ops ...mysqlxorm.SessionOption) (cron PipelineCron, bool bool, err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	defer func() {
		err = errors.Wrapf(err, "failed to get pipeline cron by id [%v]", id)
	}()

	found, err := session.ID(id).Get(&cron)
	if err != nil {
		return PipelineCron{}, false, err
	}
	if !found {
		return PipelineCron{}, false, nil
	}
	return cron, true, nil
}

func (client *Client) UpdatePipelineCron(id interface{}, cron *PipelineCron, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).Update(cron)
	return errors.Wrapf(err, "failed to update pipeline cron, id [%v]", id)
}

func (client *Client) InsertOrUpdatePipelineCron(new_ *PipelineCron, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	// 寻找 v1
	queryV1 := &PipelineCron{
		ApplicationID:   new_.ApplicationID,
		Branch:          new_.Branch,
		PipelineYmlName: new_.PipelineYmlName,
	}
	v1Exist, err := session.Get(queryV1)
	if err != nil {
		return err
	}
	if queryV1.Extra.Version == "v2" {
		v1Exist = false
	}
	if v1Exist {
		new_.ID = queryV1.ID
		new_.Enable = queryV1.Enable
		return client.UpdatePipelineCron(new_.ID, new_)
	}

	// 寻找 v2
	queryV2 := &PipelineCron{
		PipelineSource:  new_.PipelineSource,
		PipelineYmlName: new_.PipelineYmlName,
	}
	v2Exist, err := session.Get(queryV2)
	if err != nil {
		return err
	}
	if v2Exist {
		new_.ID = queryV2.ID
		new_.Enable = queryV2.Enable
		return client.UpdatePipelineCron(new_.ID, new_)
	}
	return client.CreatePipelineCron(new_)
}

func (client *Client) CreatePipelineCron(cron *PipelineCron, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	if cron.ID == 0 {
		cron.ID = uuid.SnowFlakeIDUint64()
	}
	_, err := session.InsertOne(cron)
	return errors.Wrapf(err, "failed to create pipeline cron, applicationID [%d], branch [%s], expr [%s], enable [%v]", cron.ApplicationID, cron.Branch, cron.CronExpr, cron.Enable)
}

func (client *Client) DeletePipelineCron(id interface{}, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	if _, err := session.ID(id).Delete(&PipelineCron{}); err != nil {
		return errors.Errorf("failed to delete pipeline cron, id: %d, err: %v", id, err)
	}
	return nil
}

func (client *Client) UpdatePipelineCronWillUseDefault(id interface{}, cron *PipelineCron, columns []string, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).Cols(columns...).Update(cron)
	return errors.Wrapf(err, "failed to update pipeline cron, id [%v]", id)
}

func (client *Client) ListAllPipelineCrons(ops ...mysqlxorm.SessionOption) (crons []PipelineCron, err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	if err = session.Find(&crons); err != nil {
		return nil, err
	}
	return crons, nil
}

func (client *Client) IsCronExist(cron *PipelineCron, ops ...mysqlxorm.SessionOption) (bool bool, err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	existed, err := session.Get(cron)
	if err != nil {
		return false, err
	}
	return existed, nil
}
