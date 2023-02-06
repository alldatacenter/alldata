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

package pipelinesvc

import (
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func TestPipelineSvc_BatchCreateLabels(t *testing.T) {
	db, mock, err := getEngine()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer db.Close()

	type fields struct {
		dbClient *dbclient.Client
	}
	type args struct {
		createReq *apistructs.PipelineLabelBatchInsertRequest
	}
	tests := []struct {
		name      string
		fields    fields
		args      args
		wantErr   bool
		wantValue uint64
	}{
		{
			name:   "test batch create labels",
			fields: fields{dbClient: &dbclient.Client{Engine: db}},
			args: args{createReq: &apistructs.PipelineLabelBatchInsertRequest{
				Labels: []apistructs.PipelineLabel{
					{
						Type:            "p_i",
						TargetID:        11807139039661,
						PipelineSource:  "erda",
						PipelineYmlName: "pipeline.yml",
						Key:             "foo",
						Value:           "foo",
					},
				},
			}},
			wantErr:   false,
			wantValue: uuid.SnowFlakeIDUint64(),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &PipelineSvc{
				dbClient: tt.fields.dbClient,
			}

			mock.ExpectExec("INSERT INTO `pipeline_labels`").
				WillReturnResult(sqlmock.NewResult(1, 1))
			if err = s.BatchCreateLabels(tt.args.createReq); (err != nil) != tt.wantErr {
				t.Errorf("BatchCreateLabels() error = %v, wantErr %v", err, tt.wantErr)
			}
			// we make sure that all expectations were met
			if err := mock.ExpectationsWereMet(); err != nil {
				t.Errorf("there were unfulfilled expectations: %s", err)
			}
		})
	}
}

func getEngine() (*xorm.Engine, sqlmock.Sqlmock, error) {
	db, mock, err := sqlmock.New()
	if err != nil {
		return nil, nil, err
	}

	engine, err := xorm.NewEngine("mysql", "root:123@/test?charset=utf8")
	if err != nil {
		return nil, nil, err
	}

	engine.DB().DB = db
	engine.ShowSQL(true)

	return engine, mock, nil
}
