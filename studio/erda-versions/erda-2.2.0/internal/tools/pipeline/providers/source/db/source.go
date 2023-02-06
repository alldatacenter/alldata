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

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

type PipelineSource struct {
	ID          string `json:"id" xorm:"pk"`
	SourceType  string `json:"sourceType"`
	Remote      string `json:"remote"`
	Ref         string `json:"ref"`
	Path        string `json:"path"`
	Name        string `json:"name"`
	PipelineYml string `json:"pipelineYml"`

	VersionLock   uint64    `json:"versionLock" xorm:"version_lock version"`
	SoftDeletedAt uint64    `json:"softDeletedAt"`
	CreatedAt     time.Time `json:"timeCreated,omitempty" xorm:"created_at created"`
	UpdatedAt     time.Time `json:"timeUpdated,omitempty" xorm:"updated_at updated"`
}

type PipelineSourceUnique struct {
	SourceType string   `json:"sourceType"`
	Remote     string   `json:"remote"`
	Ref        string   `json:"ref"`
	Path       string   `json:"path"`
	Name       string   `json:"name"`
	IDList     []string `json:"idList"`
}

func (PipelineSource) TableName() string {
	return "pipeline_source"
}

func (client *Client) CreatePipelineSource(pipelineSource *PipelineSource, ops ...mysqlxorm.SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	pipelineSource.ID = uuid.New()
	_, err = session.InsertOne(pipelineSource)
	return err
}

func (client *Client) UpdatePipelineSource(id string, pipelineSource *PipelineSource, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).AllCols().Update(pipelineSource)
	return err
}

func (client *Client) DeletePipelineSource(id string, pipelineSource *PipelineSource, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()
	_, err := session.ID(id).Cols("soft_deleted_at").Update(pipelineSource)
	return err
}

func (client *Client) GetPipelineSource(id string, ops ...mysqlxorm.SessionOption) (*PipelineSource, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var pipelineSource PipelineSource
	var has bool
	var err error
	if has, _, err = session.Where("id = ? and soft_deleted_at = 0", id).
		GetFirst(&pipelineSource).GetResult(); err != nil {
		return nil, err
	}

	if !has {
		return nil, fmt.Errorf("the record not fount")
	}

	return &pipelineSource, nil
}

func (client *Client) GetPipelineSourceByUnique(unique *PipelineSourceUnique, ops ...mysqlxorm.SessionOption) ([]PipelineSource, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var (
		pipelineSources []PipelineSource
		err             error
	)
	if err = session.
		Where("source_type = ?", unique.SourceType).
		Where("remote = ?", unique.Remote).
		Where("ref = ?", unique.Ref).
		Where("path = ?", unique.Path).
		Where("name = ?", unique.Name).
		Where("soft_deleted_at = 0").
		Find(&pipelineSources); err != nil {
		return nil, err
	}
	return pipelineSources, nil
}

func (client *Client) ListPipelineSource(idList []string, ops ...mysqlxorm.SessionOption) ([]PipelineSource, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var pipelineSource []PipelineSource
	if err := session.In("id", idList).
		Find(&pipelineSource); err != nil {
		return nil, err
	}

	return pipelineSource, nil
}

func (p *PipelineSource) Convert() *pb.PipelineSource {
	return &pb.PipelineSource{
		ID:          p.ID,
		SourceType:  p.SourceType,
		Remote:      p.Remote,
		Ref:         p.Ref,
		Path:        p.Path,
		Name:        p.Name,
		PipelineYml: p.PipelineYml,
		VersionLock: p.VersionLock,
		TimeCreated: timestamppb.New(p.CreatedAt),
		TimeUpdated: timestamppb.New(p.UpdatedAt),
	}
}
