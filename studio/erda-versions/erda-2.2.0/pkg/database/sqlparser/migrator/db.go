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

package migrator

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/sirupsen/logrus"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

func (mig *Migrator) DB() *gorm.DB {
	if mig.db != nil {
		return mig.db
	}

	var (
		err         error
		dsn         = mig.MySQLParameters().Format(false)
		showSchemas = fmt.Sprintf("SHOW SCHEMAS LIKE '%s'", mig.MySQLParameters().Database)
		stmt        = "CREATE DATABASE IF NOT EXISTS " + mig.dbSettings.Name + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
	)

	// initF shows schemas like the database, if the database is not exists, create it
	initF := func(db *sql.DB) error {
		rows, err := db.Query(showSchemas)
		if err != nil {
			return err
		}
		if rows.Next() {
			return nil
		}
		if _, err := db.Exec(stmt); err != nil {
			return err
		}
		return nil
	}
	if err = mig.initConnToDB(dsn, RetryTimeout(mig.RetryTimeout()), initF); err != nil {
		logrus.WithError(err).Fatalln("failed to init connection to MySQL Server")
	}

	dsn = mig.MySQLParameters().Format(true)
	mig.db, err = gorm.Open(mysql.Open(dsn), &gorm.Config{DisableForeignKeyConstraintWhenMigrating: true})
	if err != nil {
		logrus.WithError(err).WithField("DSN", dsn).Fatalln("failed to open MySQL connection")
	}
	// set the gorm logger SQL collector
	mig.db.Logger, err = gormutil.NewSQLCollector(mig.collectorFilename, nil)
	if err != nil {
		logrus.WithError(err).WithField("SQL collector filename", mig.collectorFilename).
			Fatalln("failed to set SQL collector")
		return nil
	}

	if mig.Parameters.DebugSQL() {
		mig.db = mig.db.Debug()
	}

	return mig.db
}

// ClearSandbox
// if you want to do a new migration in a clean sandbox,
// you should ClearSandbox to clear all changes on last migration
func (mig *Migrator) ClearSandbox() {
	mig.sandbox = nil
}

func (mig *Migrator) SandBox() *gorm.DB {
	if mig.sandbox != nil {
		return mig.sandbox
	}

	var (
		err            error
		createDatabase = "CREATE DATABASE IF NOT EXISTS " + mig.sandboxSettings.Name + " DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci"
		dropDatabase   = "DROP SCHEMA IF EXISTS " + mig.sandboxSettings.Name
		dsn            = mig.SandboxParameters().Format(false)
	)

	initF := func(db *sql.DB) error {
		if _, err := db.Exec(dropDatabase); err != nil {
			return err
		}
		if _, err := db.Exec(createDatabase); err != nil {
			return err
		}
		return nil
	}
	if err = mig.initConnToDB(dsn, RetryTimeout(mig.RetryTimeout()), initF); err != nil {
		logrus.WithError(err).Fatalln("failed to init connection to the sandbox")
		return nil
	}

	dsn = mig.SandboxParameters().Format(true)
	mig.sandbox, err = gorm.Open(mysql.Open(dsn), &gorm.Config{DisableForeignKeyConstraintWhenMigrating: true})
	if err != nil {
		logrus.WithError(err).WithField("DSN", dsn).Fatalln("failed to open connection to sandbox")
	}
	mig.sandbox.Logger = logger.New(
		log.New(os.Stdout, "\r\n", log.Ltime),
		logger.Config{SlowThreshold: 200 * time.Millisecond, LogLevel: logger.Error, Colorful: true},
	)

	if mig.Parameters.DebugSQL() {
		mig.sandbox = mig.sandbox.Debug()
	}

	return mig.sandbox
}

func (mig *Migrator) initConnToDB(dsn string, timeout time.Duration, initF func(db *sql.DB) error) (err error) {
	var (
		open *sql.DB
	)
	defer func() {
		if open != nil {
			_ = open.Close()
		}
	}()

	for now := time.Now(); time.Since(now) < timeout; time.Sleep(time.Second * 3) {
		loopLog := logrus.WithField("DSN", dsn).WithField("left time", timeout.Seconds()-time.Since(now).Seconds())
		open, err = sql.Open("mysql", dsn)
		if err != nil {
			loopLog.WithError(err).Warnln("failed to connect to the MySQL Server, it will try again in 3 seconds")
			continue
		}
		if err = open.Ping(); err != nil {
			loopLog.WithError(err).Warnln("failed to Ping the MySQL Server, it will try again in 3 seconds")
			continue
		}
		if err = initF(open); err != nil {
			loopLog.WithError(err).Errorln("failed to do initiation on the database")
			return err
		}

		return nil
	}

	logrus.WithError(err).WithField("DSN", dsn).Fatalln("failed to dial the MySQL Server")
	return err
}
