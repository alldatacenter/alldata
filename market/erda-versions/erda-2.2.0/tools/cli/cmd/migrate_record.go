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

package cmd

import (
	"database/sql"
	"fmt"
	"path/filepath"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
	"github.com/erda-project/erda/tools/cli/command"
)

const recordSQLPat = `INSERT INTO schema_migration_history (
    service_name, 
    filename, 
    checksum, 
    installed_by,
    installed_on, 
    language_type,
    reversed,
    created_at,
    updated_at
) VALUES (
    '%s', 
    '%s', 
    '%s', 
    '',
    'erda-cli migrate record', 
    '%s',
    '',
    NOW(),
    NOW()
)`

var MigrateRecord = command.Command{
	ParentName:     "Migrate",
	Name:           "record",
	ShortHelp:      "manually insert the migration record",
	LongHelp:       "manually isnert the migration record",
	Example:        "$ erda-cli migrate record --filename a.sql",
	Hidden:         false,
	DontHideCursor: false,
	Args:           nil,
	Flags: append(mysqlFlags,
		command.StringFlag{
			Short:        "",
			Name:         "module",
			Doc:          "the recording script module name",
			DefaultValue: "",
		},
		command.StringFlag{
			Short:        "",
			Name:         "filename",
			Doc:          "the recording script filename",
			DefaultValue: "",
		},
		command.BoolFlag{
			Short:        "",
			Name:         "dry",
			Doc:          "dry run",
			DefaultValue: false,
		},
	),
	Run: RunMigrateRecord,
}

func RunMigrateRecord(ctx *command.Context, host string, port int, username, password, database string, sandboxPort int,
	module, filename string, dry bool) error {
	script, err := migrator.NewScript(".", filepath.Join(module, filename))
	if err != nil {
		return errors.Wrap(err, "failed to new file as a script")
	}

	insert := fmt.Sprintf(recordSQLPat, module, filename, script.Checksum(), script.Type)
	fmt.Println("-- ---------------------------------------------------------------------------")
	if dry {
		fmt.Println("-- This is the record SQL, you can copy it and execute on your MySQL server --")
	} else {
		fmt.Println("-- This is the record SQL, the tool will execute it on your MySQL server -----")
	}
	fmt.Println(insert, ";")
	fmt.Println("-- ---------------------------------------------------------------------------")
	if dry {
		return nil
	}

	dsn := migrator.DSNParameters{
		Username:  username,
		Password:  password,
		Host:      host,
		Port:      port,
		Database:  database,
		ParseTime: true,
		Timeout:   time.Second * 150,
	}.Format(true)
	errMsg := "failed to connect to your MySQL server," +
		"you can run the command with '--dry' to print the SQL " +
		"then copy it and execute on your MySQL server manually"
	open, err := sql.Open("mysql", dsn)
	if err != nil {
		return errors.Wrap(err, errMsg)
	}
	execution, err := open.Prepare(insert)
	if err != nil {
		return errors.Wrap(err, errMsg)
	}
	result, err := execution.Exec()
	if err != nil {
		return errors.Wrap(err, errMsg)
	}
	lastInsertID, _ := result.LastInsertId()
	rowsAffected, _ := result.RowsAffected()
	logrus.WithField("last insert id", lastInsertID).
		WithField("rows affected", rowsAffected).
		Infoln("success!")

	return nil
}
