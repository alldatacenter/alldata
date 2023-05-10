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
// limitations under the License.o

package handler_test

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/google/uuid"
	"google.golang.org/grpc/metadata"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-infra/providers/mysql/v2/plugins/fields"
	"github.com/erda-project/erda-proto-go/apps/gallery/pb"
	"github.com/erda-project/erda/internal/apps/gallery/handler"
	"github.com/erda-project/erda/internal/apps/gallery/model"
	"github.com/erda-project/erda/internal/apps/gallery/types"
)

func TestListOpusTypes(t *testing.T) {
	types := handler.ListOpusTypes(transport.WithHeader(context.Background(), metadata.New(map[string]string{
		"lang": "en-us",
	})), &MockTran{})
	data, _ := json.MarshalIndent(types, "", "  ")
	t.Log(string(data))
}

func TestAdjustPaging(t *testing.T) {
	var cases = []struct {
		Size       int32
		No         int32
		ResultSize int
		ResultNo   int
	}{
		{Size: 0, No: 0},
		{Size: 20, No: 5},
		{Size: 2000, No: 10},
	}
	for _, case_ := range cases {
		if size, no := handler.AdjustPaging(case_.Size, case_.No); size != case_.ResultSize || no != case_.ResultNo {
			t.Logf("error happened, expected: %d, %d, autual: %d, %d", case_.ResultSize, case_.No, size, no)
		}
	}
}

func TestPrepareListOpusesOptions(t *testing.T) {
	var cases = []struct {
		Type     string
		Name     string
		OrgID    int64
		Keyword  string
		PageSize int
		PageNo   int
	}{
		{Type: "erda/extensions/addon", Name: "mysql", OrgID: 1, Keyword: "mysql", PageSize: 10, PageNo: 2},
		{OrgID: 1, PageSize: 10, PageNo: 2},
	}
	var results = []struct {
		SQL  string
		Vars []interface{}
	}{
		{
			SQL:  "SELECT * FROM `erda_gallery_opus` WHERE type = ? AND name = ? AND (display_name LIKE ? OR display_name_i18n LIKE ? OR summary LIKE ? OR summary_i18n LIKE ?) AND (org_id = ? OR level = ?) AND (`erda_gallery_opus`.`deleted_at` = ? OR `erda_gallery_opus`.`deleted_at` IS NULL) ORDER BY type DESC,name DESC,updated_at DESC LIMIT 10 OFFSET 10",
			Vars: []interface{}{cases[0].Type, cases[0].Name, "%mysql%", "%mysql%", "%mysql%", "%mysql%", cases[0].OrgID, types.OpusLevelSystem, time.Unix(0, 0)},
		}, {
			SQL:  "SELECT * FROM `erda_gallery_opus` WHERE (org_id = ? OR level = ?) AND (`erda_gallery_opus`.`deleted_at` = ? OR `erda_gallery_opus`.`deleted_at` IS NULL) ORDER BY type DESC,name DESC,updated_at DESC LIMIT 10 OFFSET 10",
			Vars: []interface{}{cases[1].OrgID, types.OpusLevelSystem, time.Unix(0, 0)},
		},
	}
	dbname := filepath.Join(os.TempDir(), "gorm.db")
	db, err := gorm.Open(sqlite.Open(dbname), new(gorm.Config))
	if err != nil {
		t.Fatalf("failed to connect database: %v", err)
	}
	defer os.Remove(dbname)
	for i, case_ := range cases {
		options := handler.PrepareListOpusesOptions(case_.OrgID, case_.Type, case_.Name, case_.Keyword, case_.PageSize, case_.PageNo)
		db := db.Debug().Session(&gorm.Session{DryRun: true})
		for _, opt := range options {
			db = opt.With(db)
		}
		db = db.Find(new(model.Opus))
		if results[i].SQL != db.Statement.SQL.String() {
			t.Fatalf("SQL error, expected:\n%s\nactual:\n%s\n", results[i].SQL, db.Statement.SQL.String())
		}
		if len(results[i].Vars) != len(db.Statement.Vars) {
			t.Fatalf("length of vars error, expected: %d, actual: %d, actuals: %v", len(results[i].Vars), len(db.Statement.Vars), db.Statement.Vars)
		}
		for j := range results[i].Vars {
			if results[i].Vars[j] != db.Statement.Vars[j] {
				t.Fatalf("vars values not equal, [%d][%d], expectd: %v, actual: %v, actuals: %v", i, j, results[i].Vars[j], db.Statement.Vars[j], db.Statement.Vars)
			}
		}
	}
	options := handler.PrepareListOpusesOptions(1, "", "", "", 10, 2)
	db2 := db.Debug().Session(&gorm.Session{DryRun: true})
	for _, opt := range options {
		db2 = opt.With(db2)
	}
	db2 = db2.Find(new(model.Opus))
}

func TestPrepareListOpusesKeywordFilterOption(t *testing.T) {
	var (
		keyword  = "mysql"
		versions = []*model.OpusVersion{new(model.OpusVersion), new(model.OpusVersion)}
		sql      = "SELECT * FROM `erda_gallery_opus` WHERE (name LIKE ? OR display_name LIKE ? OR id IN (?,?)) AND (`erda_gallery_opus`.`deleted_at` = ? OR `erda_gallery_opus`.`deleted_at` IS NULL)"
		vars     = []interface{}{"%mysql%", "%mysql%"}
	)
	for i := range versions {
		opusID := uuid.New().String()
		versions[i].OpusID = opusID
		vars = append(vars, opusID)
	}
	vars = append(vars, time.Unix(0, 0))
	dbname := filepath.Join(os.TempDir(), "gorm.db")
	db, err := gorm.Open(sqlite.Open(dbname), new(gorm.Config))
	if err != nil {
		t.Fatalf("failed to connect dbtabase: %v", err)
	}
	defer os.Remove(dbname)
	db = handler.PrepareListOpusesKeywordFilterOption(keyword, versions).With(db)
	db = db.Debug().Session(&gorm.Session{DryRun: true}).Find(new(model.Opus))
	if db.Statement.SQL.String() != sql {
		t.Fatalf("sql error, expected: %s\n, actual: %s\n", sql, db.Statement.SQL.String())
	}
	for i := range vars {
		if vars[i] != db.Statement.Vars[i] {
			t.Fatalf("vars[%d] not equal, expected: %s,\nactual: %s\n", i, vars[i], db.Statement.Vars[i])
		}
	}
	t.Log(db.Statement.SQL.String())
	t.Log(db.Statement.Vars)
}

func TestPrepareListVersionsInOpusesIDsOption(t *testing.T) {
	dbname := filepath.Join(os.TempDir(), "gorm.db")
	db, err := gorm.Open(sqlite.Open(dbname), new(gorm.Config))
	if err != nil {
		t.Fatalf("failed to connect dbtabase: %v", err)
	}
	defer os.Remove(dbname)

	var (
		opuses = []*model.Opus{new(model.Opus), new(model.Opus)}
		sql    = "SELECT * FROM `erda_gallery_opus_version` WHERE opus_id IN (?,?) AND (`erda_gallery_opus_version`.`deleted_at` = ? OR `erda_gallery_opus_version`.`deleted_at` IS NULL)"
		vars   = []interface{}{fields.UUID{String: uuid.New().String(), Valid: true}, fields.UUID{String: uuid.New().String(), Valid: true}, time.Unix(0, 0)}
	)
	for i := range opuses {
		opuses[i].ID = vars[i].(fields.UUID)
	}

	db = handler.PrepareListVersionsInOpusesIDsOption(opuses).With(db)
	db = db.Debug().Session(&gorm.Session{DryRun: true}).Find(new(model.OpusVersion))
	if db.Statement.SQL.String() != sql {
		t.Fatalf("sql not equal, expected: %s\n, actual: %s\n", sql, db.Statement.SQL.String())
	}
	t.Log(db.Statement.SQL.String())
	t.Log(db.Statement.Vars)
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}

func TestComposeListOpusResp(t *testing.T) {
	var (
		total   int64 = 1
		timeNow       = time.Now()
		opus          = model.Opus{
			Model:       model.Model{ID: fields.UUID{String: uuid.New().String(), Valid: true}},
			Level:       "sys",
			Type:        "erda/extension/addon",
			Name:        "mysql",
			DisplayName: "mysql",
		}
	)
	opus.DefaultVersionID = uuid.New().String()
	opus.LatestVersionID = uuid.New().String()
	opus.CreatedAt = timeNow
	opus.UpdatedAt = timeNow
	resp := handler.ComposeListOpusResp(transport.WithHeader(context.Background(), metadata.New(map[string]string{
		"lang": "en-us",
	})), total, []*model.Opus{&opus}, &MockTran{})
	t.Logf("%v", resp)
}

// need not do unit test
func TestComposeListOpusVersionRespWithOpus(t *testing.T) {
	handler.ComposeListOpusVersionRespWithOpus("en-us", new(pb.ListOpusVersionsResp), new(model.Opus))
}

// need not do unit test
func TestComposeListOpusVersionRespWithVersions(t *testing.T) {
	_ = handler.ComposeListOpusVersionRespWithVersions("en-us", new(pb.ListOpusVersionsResp), []*model.OpusVersion{new(model.OpusVersion)})
}

// need not do unit test
func TestComposeListOpusVersionRespWithPresentations(t *testing.T) {
	var id = uuid.New().String()
	var resp = &pb.ListOpusVersionsResp{Data: &pb.ListOpusVersionsRespData{Versions: []*pb.ListOpusVersionRespDataVersion{{Id: id}}}}
	var presentation = &model.OpusPresentation{VersionID: id}
	handler.ComposeListOpusVersionRespWithPresentations("en-us", resp, []*model.OpusPresentation{presentation})
}

// need not do unit test
func TestComposeListOpusVersionRespWithReadmes(t *testing.T) {
	var id = uuid.New().String()
	var resp = &pb.ListOpusVersionsResp{
		Data: &pb.ListOpusVersionsRespData{
			Versions: []*pb.ListOpusVersionRespDataVersion{{Id: id}},
		},
	}
	handler.ComposeListOpusVersionRespWithReadmes(resp, types.LangEn.String(), []*model.OpusReadme{{
		VersionID: id,
		Lang:      types.LangEn.String(),
		LangName:  types.LangTypes[types.LangEn],
		Text:      "xxx",
	}})
	handler.ComposeListOpusVersionRespWithReadmes(resp, types.LangEn.String(), []*model.OpusReadme{{
		VersionID: id,
		Lang:      types.LangUnknown.String(),
		LangName:  types.LangTypes[types.LangUnknown],
		Text:      "xxx",
	}})
	handler.ComposeListOpusVersionRespWithReadmes(resp, types.LangEn.String(), []*model.OpusReadme{{
		VersionID: id,
		Lang:      "other",
		LangName:  "other",
		Text:      "xxx",
	}})
	handler.ComposeListOpusVersionRespWithReadmes(resp, types.LangEn.String(), []*model.OpusReadme{{
		VersionID: "xxx-yyy",
	}})
}

// need not do unit test
func TestGenOpusUpdates(t *testing.T) {
	s := "xxx"
	handler.GenOpusUpdates(s, s, s, s, s, s, s, true)
}

// need not do unit test
func TestGenPresentationFromReq(t *testing.T) {
	handler.GenPresentationFromReq("", "", model.Common{}, new(pb.PutOnExtensionsReq))
}
