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

package mysqlhelper

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"net"
	"os"
	"strings"
	"time"

	"github.com/go-sql-driver/mysql"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/clusterdialer"
)

type Request struct {
	Sqls       []string
	Host       string
	Url        string
	User       string
	Password   string
	ClusterKey string
	CreateDbs  []string
}

func (r Request) addr() (string, string) {
	host, port, _ := net.SplitHostPort(strings.Replace(r.Url, "jdbc:mysql://", "", -1))
	return host, port
}

func (r Request) dsn(proto string) string {
	host, port := r.addr()
	return fmt.Sprintf("%s:%s@%s(%s:%s)/?charset=utf8mb4,utf8&parseTime=true&multiStatements=true", r.User, r.Password, proto, host, port)
}

func (r Request) isRemoteCluster() bool {
	currentClusterKey := os.Getenv("DICE_CLUSTER_NAME")
	if r.ClusterKey == "" || currentClusterKey == r.ClusterKey {
		return false
	}
	return true
}

func (r Request) dbOpen() (*sql.DB, error) {
	proto := "tcp"
	if r.isRemoteCluster() {
		proto = fmt.Sprintf("tcp-%s", r.ClusterKey)
		mysql.RegisterDialContext(proto, mysql.DialContextFunc(clusterdialer.DialContextTCP(r.ClusterKey)))
	}
	logrus.Debugf("mysql dsn is:%s", r.dsn(proto))
	db, err := sql.Open("mysql", r.dsn(proto))
	if err != nil {
		return nil, err
	}
	return db, nil
}

func (r Request) Exec() error {
	db, err := r.dbOpen()
	if err != nil {
		return err
	}
	defer db.Close()
	for _, dbName := range r.CreateDbs {
		_, err = db.Exec("CREATE DATABASE IF NOT EXISTS " + dbName)
		if err != nil {
			return err
		}
	}
	for _, sql := range r.Sqls {
		ctx, cf := context.WithTimeout(context.Background(), time.Minute)
		defer cf()
		_, err = db.ExecContext(ctx, sql)
		if err != nil {
			return err
		}
	}
	return err
}

type SlaveState struct {
	IORunning  string
	SQLRunning string
}

func (r Request) GetSlaveState() (*SlaveState, error) {
	db, err := r.dbOpen()
	if err != nil {
		return nil, err
	}
	defer db.Close()
	ctx, cf := context.WithTimeout(context.Background(), time.Minute)
	defer cf()
	rows, err := db.QueryContext(ctx, "show slave status")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	if !rows.Next() {
		return nil, errors.New("no rows")
	}
	cols, err := rows.Columns()
	if err != nil {
		return nil, err
	}
	arr := make([]interface{}, len(cols))
	for i := range arr {
		if cols[i] == "Slave_IO_Running" {
			arr[i] = new(string)
		} else if cols[i] == "Slave_SQL_Running" {
			arr[i] = new(string)
		} else {
			arr[i] = new(interface{})
		}
	}
	err = rows.Scan(arr...)
	if err != nil {
		return nil, err
	}
	var state SlaveState
	for i, col := range cols {
		if col == "Slave_IO_Running" {
			state.IORunning = *(arr[i].(*string))
		} else if col == "Slave_SQL_Running" {
			state.SQLRunning = *(arr[i].(*string))
		}
	}
	return &state, nil
}
