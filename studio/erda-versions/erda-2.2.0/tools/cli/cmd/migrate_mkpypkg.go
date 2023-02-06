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
	"io/ioutil"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/database/sqlparser/pygrator"
	"github.com/erda-project/erda/tools/cli/command"
)

var MigrateMkPyPkg = command.Command{
	ParentName:     "Migrate",
	Name:           "mkpypkg",
	ShortHelp:      "generate python package",
	LongHelp:       "generate python package",
	Example:        "$ erda-cli migrate mkpypkg --filename=my_script.py --mysql-host localhost --mysql-port 3306 --mysql-username root --mysql-password *** --database erda --sandbox-port 3307",
	Hidden:         true,
	DontHideCursor: false,
	Args:           nil,
	Flags: append(
		mysqlFlags,
		command.StringFlag{
			Short:        "",
			Name:         "filename",
			Doc:          "python script filename",
			DefaultValue: "",
		},
		command.StringFlag{
			Short:        "",
			Name:         "requirements",
			Doc:          "requirements.txt file name",
			DefaultValue: "",
		},
		command.BoolFlag{
			Short:        "",
			Name:         "commit",
			Doc:          "",
			DefaultValue: false,
		},
	),
	Run: RunMigrateMkPyPkg,
}

type textFile struct {
	name string
	data []byte
}

func (t textFile) GetName() string {
	return t.name
}

func (t textFile) GetData() []byte {
	return t.data
}

func RunMigrateMkPyPkg(ctx *command.Context, host string, port int, username, password, database string, sandboxPort int,
	filename, requirements string, commit bool) (err error) {
	logrus.Infoln("Erda Migrator generate the python package")

	var developerScript = textFile{
		name: filename,
		data: nil,
	}

	developerScript.data, err = ioutil.ReadFile(filename)
	if err != nil {
		return errors.Wrap(err, "failed to read file")
	}

	var p = pygrator.Package{
		DeveloperScript: developerScript,
		Requirements:    nil,
		Settings: pygrator.Settings{
			Engine:   pygrator.MySQLConnectorEngine,
			User:     username,
			Password: password,
			Host:     host,
			Port:     port,
			Name:     database,
			TimeZone: pygrator.TimeZoneAsiaShanghai,
		},
		Commit: commit,
	}

	p.Requirements, err = ioutil.ReadFile(requirements)
	if err != nil {
		logrus.WithError(err).Warnln("failed to read requirements.txt, use default")
		p.Requirements = []byte(pygrator.BaseRequirements)
	}

	return p.Make()
}
