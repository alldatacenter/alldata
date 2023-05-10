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
	"reflect"
	"testing"

	"github.com/jinzhu/gorm"
)

func TestMSPProjectDB_Query(t *testing.T) {
	mysqldb, _, _ := MockInit(MYSQL)
	type fields struct {
		DB *gorm.DB
	}
	type args struct {
		id string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *MSPProject
		wantErr bool
	}{
		{name: "case1", fields: fields{DB: mysqldb}, args: args{id: "1"}, want: nil, wantErr: true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			db := &MSPProjectDB{
				DB: tt.fields.DB,
			}
			got, err := db.Query(tt.args.id)
			if (err != nil) != tt.wantErr {
				t.Errorf("Query() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Query() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMSPProjectDB_Delete(t *testing.T) {
	mysqldb, _, _ := MockInit(MYSQL)
	type fields struct {
		DB *gorm.DB
	}
	type args struct {
		id string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *MSPProject
		wantErr bool
	}{
		{name: "case1", fields: fields{DB: mysqldb}, args: args{id: "1"}, want: nil, wantErr: true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			db := &MSPProjectDB{
				DB: tt.fields.DB,
			}
			got, err := db.Delete(tt.args.id)
			if (err != nil) != tt.wantErr {
				t.Errorf("Delete() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Delete() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMSPProjectDB_Update(t *testing.T) {
	mysqldb, _, _ := MockInit(MYSQL)
	type fields struct {
		DB *gorm.DB
	}
	type args struct {
		project *MSPProject
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *MSPProject
		wantErr bool
	}{
		{name: "case1", fields: fields{DB: mysqldb}, args: args{project: nil}, want: nil, wantErr: true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			db := &MSPProjectDB{
				DB: tt.fields.DB,
			}
			got, err := db.Update(tt.args.project)
			if (err != nil) != tt.wantErr {
				t.Errorf("Update() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Update() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMSPProjectDB_Create(t *testing.T) {
	mysqldb, _, _ := MockInit(MYSQL)
	type fields struct {
		DB *gorm.DB
	}
	type args struct {
		project *MSPProject
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *MSPProject
		wantErr bool
	}{
		{name: "case1", fields: fields{DB: mysqldb}, args: args{project: nil}, want: nil, wantErr: true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			db := &MSPProjectDB{
				DB: tt.fields.DB,
			}
			got, err := db.Create(tt.args.project)
			if (err != nil) != tt.wantErr {
				t.Errorf("Create() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Create() got = %v, want %v", got, tt.want)
			}
		})
	}
}
