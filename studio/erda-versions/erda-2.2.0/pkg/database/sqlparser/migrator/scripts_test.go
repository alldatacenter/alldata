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

package migrator_test

import (
	"path/filepath"
	"testing"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
)

type parameter struct {
	workdir      string
	migrationDir string
}

func (p parameter) MySQLParameters() *migrator.DSNParameters {
	return nil
}

func (p parameter) SandboxParameters() *migrator.DSNParameters {
	return nil
}

func (p parameter) Database() string {
	return ""
}

func (p parameter) MigrationDir() string {
	return p.migrationDir
}

func (p parameter) Modules() []string {
	return nil
}

func (p parameter) Workdir() string {
	return p.workdir
}

func (p parameter) DebugSQL() bool {
	return false
}

func (p parameter) NeedErdaMySQLLint() bool {
	return false
}

func (p parameter) LintConfig() map[string]sqllint.Config {
	return make(map[string]sqllint.Config)
}

func TestNewScripts(t *testing.T) {
	var p = parameter{
		workdir:      "..",
		migrationDir: "testdata/new_scripts_test_data",
	}
	var (
		serviceB      = "service_b"
		serviceBBase0 = "testdata/new_scripts_test_data/service_b/this_is_base.sql"
	)

	scripts, err := migrator.NewScripts(p)
	if err != nil {
		t.Fatal(err)
	}

	for name, service := range scripts.Services {
		t.Log(name)
		for _, filename := range service.Filenames() {
			t.Log("\t", filename)
		}
	}

	// assert
	if first := scripts.Services[serviceB].Filenames()[0]; first != filepath.Base(serviceBBase0) {
		t.Fatal("base file error")
	}
}

func TestNewScripts2(t *testing.T) {
	var p = parameter{
		workdir:      "..",
		migrationDir: "testdata/new_scripts_test_data2",
	}

	_, err := migrator.NewScripts(p)
	if err == nil {
		t.Fatal("fails")
	}
}

func TestScripts_AlterPermissionLint(t *testing.T) {
	for _, migDir := range []string{
		"testdata/alter_permission_test_data",
		"testdata/alter_permission_test_data2",
		"testdata/alter_permission_test_data3",
	} {
		var p = parameter{
			workdir:      "..",
			migrationDir: migDir,
		}
		scripts, err := migrator.NewScripts(p)
		if err != nil {
			t.Fatal(err)
		}
		// assert: if AlterPermissionLint returns err == nil, test fails
		if err = scripts.AlterPermissionLint(); err == nil {
			t.Fatal(err)
		} else {
			t.Log(err)
		}
	}
}
