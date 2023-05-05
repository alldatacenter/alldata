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
	"path/filepath"
	"time"

	"github.com/xormplus/builder"
	"github.com/xormplus/xorm"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	"github.com/erda-project/erda/apistructs"
)

type PipelineDefinition struct {
	ID                string    `json:"id" xorm:"pk"`
	Location          string    `json:"location"`
	Name              string    `json:"name"`
	CostTime          int64     `json:"costTime"`
	Creator           string    `json:"creator"`
	Executor          string    `json:"executor"`
	SoftDeletedAt     uint64    `json:"softDeletedAt"`
	PipelineSourceId  string    `json:"pipelineSourceId"`
	Category          string    `json:"category"`
	Status            string    `json:"status"`
	StartedAt         time.Time `json:"startedAt,omitempty" xorm:"started_at"`
	EndedAt           time.Time `json:"endedAt,omitempty" xorm:"ended_at"`
	TimeCreated       time.Time `json:"timeCreated,omitempty" xorm:"created_at created"`
	TimeUpdated       time.Time `json:"timeUpdated,omitempty" xorm:"updated_at updated"`
	PipelineID        uint64    `json:"pipelineId"`
	TotalActionNum    int64     `json:"totalActionNum"`
	ExecutedActionNum int64     `json:"executedActionNum"`
	Ref               string    `json:"ref"`
}

func (PipelineDefinition) TableName() string {
	return "pipeline_definition"
}

func (client *Client) CreatePipelineDefinition(pipelineDefinition *PipelineDefinition, ops ...mysqlxorm.SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err = session.InsertOne(pipelineDefinition)
	return err
}

func (client *Client) UpdatePipelineDefinition(id string, pipelineDefinition *PipelineDefinition, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).AllCols().Update(pipelineDefinition)
	return err
}

func (client *Client) DeletePipelineDefinition(id string, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.Table(new(PipelineDefinition)).ID(id).Update(map[string]interface{}{"soft_deleted_at": time.Now().UnixNano() / 1e6})
	return err
}

func (client *Client) GetPipelineDefinition(id string, ops ...mysqlxorm.SessionOption) (*PipelineDefinition, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var pipelineDefinition PipelineDefinition
	var has bool
	var err error
	if has, _, err = session.Where("id = ? and soft_deleted_at = 0", id).GetFirst(&pipelineDefinition).GetResult(); err != nil {
		return nil, err
	}

	if !has {
		return nil, fmt.Errorf("the record not fount")
	}

	return &pipelineDefinition, nil
}

type PipelineDefinitionSource struct {
	PipelineDefinition `xorm:"extends"`

	SourceType string `json:"sourceType"`
	Remote     string `json:"remote"`
	Ref        string `json:"ref"`
	Path       string `json:"path"`
	FileName   string `json:"fileName"`
}

func (client *Client) ListPipelineDefinition(req *pb.PipelineDefinitionListRequest, ops ...mysqlxorm.SessionOption) ([]PipelineDefinitionSource, int64, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var (
		pipelineDefinitionSources []PipelineDefinitionSource
		err                       error
	)
	engine := session.Table("pipeline_definition").Alias("d").
		Select("d.*,s.source_type,s.remote,s.ref,s.path,s.name AS file_name").
		Join("LEFT", []string{"pipeline_source", "s"}, "d.pipeline_source_id = s.id AND s.soft_deleted_at = 0").
		Where("d.soft_deleted_at = 0")

	if req.Location == "" {
		return nil, 0, fmt.Errorf("the location is empty")
	}
	engine = engine.Where("d.location = ?", req.Location)
	if req.Remote != nil {
		engine = engine.In("s.remote", req.Remote)
	}
	if req.FuzzyName != "" {
		engine = engine.Where("d.name LIKE ?", "%"+req.FuzzyName+"%")
	}
	if req.Name != "" {
		engine = engine.Where("d.name = ?", req.Name)
	}
	if len(req.IdList) != 0 {
		engine = engine.In("d.id", req.IdList)
	}
	if len(req.Creator) != 0 {
		engine = engine.In("d.creator", req.Creator)
	}
	if len(req.Executor) != 0 {
		engine = engine.In("d.executor", req.Executor)
	}
	if len(req.Category) != 0 {
		engine = engine.In("d.category", req.Category)
	}
	if len(req.Ref) != 0 {
		engine = engine.In("s.ref", req.Ref)
	}
	if len(req.Status) != 0 {
		engine = engine.In("d.status", req.Status)
	}
	if len(req.SourceIDList) != 0 {
		engine = engine.In("d.pipeline_source_id", req.SourceIDList)
	}
	if len(req.TimeCreated) == 2 {
		if req.TimeCreated[0] != "" {
			engine = engine.Where("d.created_at >= ?", req.TimeCreated[0])
		}
		if req.TimeCreated[1] != "" {
			engine = engine.Where("d.created_at <= ?", req.TimeCreated[1])
		}
	}
	if len(req.TimeStarted) == 2 {
		if req.TimeStarted[0] != "" {
			engine = engine.Where("d.started_at >= ?", req.TimeStarted[0])
		}
		if req.TimeStarted[1] != "" {
			engine = engine.Where("d.started_at <= ?", req.TimeStarted[1])
		}
	}
	if !req.IsOthers {
		if len(req.FilePathWithNames) != 0 {
			cond := builder.NewCond()
			for i := 0; i < len(req.FilePathWithNames); i++ {
				cond = cond.Or(builder.Eq{"s.path": getFilePath(req.FilePathWithNames[i]), "s.name": filepath.Base(req.FilePathWithNames[i])})
			}
			sqlBuild, args, _ := builder.ToSQL(cond)
			engine = engine.Where(sqlBuild, args...)
		}
	} else {
		if len(req.FilePathWithNames) != 0 {
			for i := 0; i < len(req.FilePathWithNames); i++ {
				path := req.FilePathWithNames[i]
				if getFilePath(path) == "" {
					path = "/" + path
				}
				engine = engine.Where("CONCAT(s.path,'/',s.`name`) != ?", path)
			}
		}
	}

	countEngine := engine.Clone().Select("COUNT(*)")

	for _, v := range req.AscCols {
		engine = engine.Asc("d." + v)
	}
	for _, v := range req.DescCols {
		engine = engine.Desc("d." + v)
	}
	if err = engine.Limit(int(req.PageSize), int((req.PageNo-1)*req.PageSize)).
		Find(&pipelineDefinitionSources); err != nil {
		return nil, 0, err
	}

	total, err := client.CountPipelineDefinition(countEngine)
	if err != nil {
		return nil, 0, err
	}
	return pipelineDefinitionSources, total, nil
}

func (client *Client) CountPipelineDefinition(session *xorm.Session) (int64, error) {
	var (
		total int64
		err   error
	)

	total, err = session.Count(new(PipelineDefinitionSource))
	if err != nil {
		return 0, err
	}
	return total, nil
}

type PipelineDefinitionStatistics struct {
	Group      string
	FailedNum  uint64
	RunningNum uint64
	TotalNum   uint64
}

func (client *Client) StatisticsGroupByRemote(req *pb.PipelineDefinitionStatisticsRequest, ops ...mysqlxorm.SessionOption) ([]PipelineDefinitionStatistics, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var (
		list []PipelineDefinitionStatistics
		err  error
	)
	err = session.Table("pipeline_definition").Alias("d").
		Select(fmt.Sprintf("s.remote AS `group`,COUNT(*) AS total_num,COUNT( IF ( d.`status` = '%s' , 1, NULL) ) AS running_num,"+
			"COUNT(IF(DATE_SUB(CURDATE(), INTERVAL 1 DAY) <= d.started_at AND d.`status` = '%s',1,NULL)) AS failed_num",
			apistructs.PipelineStatusRunning, apistructs.PipelineStatusFailed)).
		Join("LEFT", []string{"pipeline_source", "s"}, "d.pipeline_source_id = s.id AND s.soft_deleted_at = 0").
		Where("d.soft_deleted_at = 0").
		Where("d.location = ?", req.GetLocation()).
		GroupBy("s.remote").
		Find(&list)
	return list, err
}

func (client *Client) StatisticsGroupByFilePath(req *pb.PipelineDefinitionStatisticsRequest, ops ...mysqlxorm.SessionOption) ([]PipelineDefinitionStatistics, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var (
		list []PipelineDefinitionStatistics
		err  error
	)
	engine := session.Table("pipeline_definition").Alias("d").
		Select(fmt.Sprintf("CONCAT(s.path,'/',s.`name`) AS `group`,COUNT(*) AS total_num,COUNT( IF ( d.`status` = '%s' , 1, NULL) ) AS running_num,"+
			"COUNT(IF(DATE_SUB(CURDATE(), INTERVAL 1 DAY) <= d.started_at AND d.`status` = '%s',1,NULL)) AS failed_num",
			apistructs.PipelineStatusRunning, apistructs.PipelineStatusFailed)).
		Join("LEFT", []string{"pipeline_source", "s"}, "d.pipeline_source_id = s.id AND s.soft_deleted_at = 0").
		Where("d.soft_deleted_at = 0").
		Where("d.location = ?", req.GetLocation())
	if len(req.GetRemotes()) != 0 {
		engine = engine.In("s.remote", req.GetRemotes())
	}
	err = engine.GroupBy("s.path,s.name").
		Find(&list)
	return list, err
}

func (p *PipelineDefinitionSource) Convert() *pb.PipelineDefinition {
	return &pb.PipelineDefinition{
		ID:                p.ID,
		Name:              p.Name,
		Creator:           p.Creator,
		Category:          p.Category,
		CostTime:          p.CostTime,
		Executor:          p.Executor,
		StartedAt:         timestamppb.New(p.StartedAt),
		EndedAt:           timestamppb.New(p.EndedAt),
		TimeCreated:       timestamppb.New(p.TimeCreated),
		TimeUpdated:       timestamppb.New(p.TimeUpdated),
		SourceType:        p.SourceType,
		PipelineSourceID:  p.PipelineSourceId,
		Remote:            p.Remote,
		Ref:               p.Ref,
		Path:              p.Path,
		FileName:          p.FileName,
		Status:            p.Status,
		PipelineID:        int64(p.PipelineID),
		TotalActionNum:    p.TotalActionNum,
		ExecutedActionNum: p.ExecutedActionNum,
	}
}

func (client *Client) ListUsedRef(req *pb.PipelineDefinitionUsedRefListRequest, ops ...mysqlxorm.SessionOption) (refs []string, err error) {
	session := client.NewSession(ops...)
	defer session.Close()
	engine := session.Table(PipelineDefinition{}.TableName()).Alias("d").
		Join("LEFT", []string{"pipeline_source", "s"}, "d.pipeline_source_id = s.id AND s.soft_deleted_at = 0").
		Cols("d.ref").
		Where("d.soft_deleted_at = 0").
		Where("d.location = ?", req.Location).
		GroupBy("d.ref")
	if len(req.GetRemotes()) != 0 {
		engine = engine.In("s.remote", req.GetRemotes())
	}
	err = engine.Find(&refs)
	return
}

func getFilePath(path string) string {
	dir := filepath.Dir(path)
	if dir == "." {
		return ""
	}
	return dir
}
