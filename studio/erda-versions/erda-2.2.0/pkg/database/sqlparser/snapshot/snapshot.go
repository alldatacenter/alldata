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

package snapshot

import (
	"bytes"
	"fmt"
	"os"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	"github.com/pingcap/parser/format"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"

	"github.com/erda-project/erda/pkg/database/sqlparser/table"
)

var (
	charsetWhiteEnv     = "PIPELINE_MIGRATION_CHARSET_WHITE"
	defaultCharsetWhite = []string{"utf8", "utf8mb4"}
)

// Snapshot maintains the structure of the database tables
type Snapshot struct {
	tables map[string]*table.Table
	from   *gorm.DB
}

// From snapshots the structure of the database tables, and returns the Snapshot.
// tx is the connection handler of the goal DB.
// ignore is the tables you do not want to snapshot.
func From(tx *gorm.DB, ignore ...string) (s *Snapshot, err error) {
	s = &Snapshot{tables: make(map[string]*table.Table), from: tx}
	ignores := make(map[string]bool, len(ignore))
	for _, ig := range ignore {
		ignores[ig] = true
	}

	defer func() {
		if err != nil {
			err = errors.Wrap(err, "failed to snapshot database schema structure")
		}
	}()

	tx = tx.Raw("SHOW TABLES")
	if err = tx.Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return s, nil
		}
		return nil, err
	}

	rows, err := tx.Rows()
	if err != nil {
		return nil, err
	}

	for rows.Next() {
		var (
			tableName string
			stmt      string
		)
		if err = rows.Scan(&tableName); err != nil {
			return nil, err
		}
		if _, ok := ignores[tableName]; ok {
			continue
		}

		tx := tx.Raw("SHOW CREATE TABLE " + tableName)
		if err = tx.Error; err != nil {
			return nil, err
		}
		if err = tx.Row().Scan(&tableName, &stmt); err != nil {
			return nil, err
		}
		// some character set can not be parsed, so trim them from the stmt
		stmt = TrimCharacterSetFromRawCreateTableSQL(stmt, CharsetWhite()...)
		// block format syntax can not be parsed, so trim it
		stmt = TrimBlockFormat(stmt)
		node, err := parser.New().ParseOneStmt(stmt, "", "")
		if err != nil {
			return nil, err
		}
		t := new(table.Table)
		t.Append(node.(ast.DDLNode))
		s.tables[tableName] = t
	}

	return s, err
}

// DDLNodes returns DDLs
func (s *Snapshot) DDLNodes() []ast.DDLNode {
	var nodes []ast.DDLNode
	for _, t := range s.tables {
		nodes = append(nodes, t.Nodes()...)
	}
	return nodes
}

// HasAnyTable returns true if there is any table in the Snapshot
func (s *Snapshot) HasAnyTable() bool {
	return len(s.tables) > 0
}

// TableNames returns all tables names
func (s *Snapshot) TableNames() []string {
	var names []string
	for k := range s.tables {
		names = append(names, k)
	}
	return names
}

// Dump dumps data from DB.
// tableName is the table you want to dump.
// lines is the numbers of lines you want to dump.
// Note: lines is an approximate number rather than an exact number.
// Note: the max data size is not more than 1<<16 (the MySQL default max placeholders size).
func (s *Snapshot) Dump(tableName string, lines uint64) ([]map[string]interface{}, uint64, error) {
	var (
		selectCount = fmt.Sprintf("SELECT COUNT(*) FROM `%s`", tableName)
		selectAll   = fmt.Sprintf("SELECT * FROM `%s` WHERE RAND() < ?", tableName)
		count       uint64
	)
	tx := s.from.Raw(selectCount)
	if err := tx.Error; err != nil {
		return nil, 0, errors.Wrapf(err, "failed to Raw(%s)", strconv.Quote(selectCount))
	}
	if err := tx.Row().Scan(&count); err != nil {
		return nil, 0, errors.Wrapf(err, "failed to Scan count from %s", strconv.Quote(selectCount))
	}
	logrus.WithField("tableName", tableName).
		WithField("maxSampleLines", lines).
		WithField("total", count).
		Infoln("*Snapshot.Dump")
	if count == 0 {
		return nil, count, nil
	}
	var rate = float64(lines) / float64(count)
	if err := tx.Raw(selectAll, rate).Error; err != nil {
		return nil, count, errors.Wrapf(err, "failed to Raw(%s)", strconv.Quote(selectAll))
	}
	rows, err := tx.Rows()
	if err != nil {
		return nil, count, errors.Wrapf(err, "failed to Rows from %s", strconv.Quote(selectAll))
	}
	defer rows.Close()

	var data []map[string]interface{}
	for rows.Next() {
		columns, _ := rows.Columns()
		values := make([]interface{}, len(columns))
		for i := 0; i < len(values); i++ {
			values[i] = &values[i]
		}
		if err := rows.Scan(values...); err != nil {
			return nil, count, errors.Wrapf(err, "failed to Scan %v, from %s", columns, strconv.Quote(selectAll))
		}
		var record = make(map[string]interface{})
		for i := 0; i < len(columns); i++ {
			record[columns[i]] = values[i]
		}
		data = append(data, record)
		if len(data) >= int(lines) {
			break
		}
		// (1<<16)-1 is the max placeholders size to insert to for mysql default
		if (len(data)+1)*len(columns) >= 1<<16 {
			break
		}
	}
	return data, count, nil
}

// RecoverTo recover the data structure and data to goal db
func (s *Snapshot) RecoverTo(tx *gorm.DB) error {
	var (
		l          = logrus.WithField("func", "*Snapshot.RecoverTo")
		nodes      = s.DDLNodes()
		installing = make(map[string]*ast.CreateTableStmt)
		installed  = make(map[string]*ast.CreateTableStmt)
		inserted   = make(map[string]*ast.CreateTableStmt)
	)

	for _, ddl := range nodes {
		create := ddl.(*ast.CreateTableStmt)
		installing[create.Table.Name.String()] = create
	}

	var (
		// createF is used to execute every CreateTableStmt .
		// createF can resolve foreign key dependencies recursively.
		createF func(*ast.CreateTableStmt) error
		// insertF dumps data and inserts into new db.
		insertF func(*ast.CreateTableStmt, uint64) (int, uint64, error)
	)
	createF = func(create *ast.CreateTableStmt) error {
		// skip installed table
		if _, ok := installed[create.Table.Name.String()]; ok {
			return nil
		}

		// resolve reference dependencies
		for _, constraint := range create.Constraints {
			if constraint.Tp == ast.ConstraintForeignKey {
				if alia, ok := installing[constraint.Refer.Table.Name.String()]; ok {
					if err := createF(alia); err != nil {
						return err
					}
				}
			}
		}

		// install
		var (
			buf = bytes.NewBuffer(nil)
		)
		TrimCollateOptionFromCreateTable(create)
		TrimCollateOptionFromCols(create)
		TrimConstraintCheckFromCreateTable(create)

		if err := create.Restore(&format.RestoreCtx{
			Flags:     format.DefaultRestoreFlags,
			In:        buf,
			JoinLevel: 0,
		}); err != nil {
			return errors.Wrap(err, "failed to Restore table definition")
		}

		if err := tx.Exec(buf.String()).Error; err != nil {
			return err
		}
		installed[create.Table.Name.String()] = create
		return nil
	}
	insertF = func(create *ast.CreateTableStmt, lines uint64) (int, uint64, error) {
		// skip inserted table
		if _, ok := inserted[create.Table.Name.String()]; ok {
			return 0, 0, nil
		}

		// resolve reference dependencies
		exLines := lines * 5
		for _, constraint := range create.Constraints {
			if constraint.Tp == ast.ConstraintForeignKey {
				if alia, ok := installing[constraint.Refer.Table.Name.String()]; ok {
					lines = exLines
					if n, count, err := insertF(alia, lines); err != nil {
						return n, count, err
					}
				}
			}
		}

		// insert
		inserted[create.Table.Name.String()] = create
		data, count, err := s.Dump(create.Table.Name.String(), lines)
		if err != nil {
			return 0, count, err
		}
		n := len(data)
		if n == 0 {
			return n, count, nil
		}
		if err = tx.Table(create.Table.Name.String()).Create(data).Error; err == nil {
			return n, count, nil
		}
		l.WithError(err).
			WithField("func", "*Snapshot.RecoverTo.insertF").
			WithField("tableName", create.Table.Name.String()).
			Warnln("failed to bulk insert, try to insert one by one item")
		n = 0
		for _, item := range data {
			if err := tx.Table(create.Table.Name.String()).Create(item).Error; err != nil {
				if IsCannotAddOrUpdateAChildRowError(err) {
					continue
				}
				return n, count, err
			}
			n++
		}

		return n, count, nil
	}

	bigTables := s.getBigTables()
	for _, create := range installing {
		if err := createF(create); err != nil {
			return err
		}
		if Sampling() {
			if _, ok := bigTables[create.Table.Name.String()]; ok {
				l.WithField("tableName", create.Table.Name.String()).
					Infoln("this is a big table, skip sampling")
				continue
			}
			now := time.Now()
			n, count, err := insertF(create, MaxSamplingSize())
			l.WithField("tableName", create.Table.Name.String()).
				WithField("timeCost", int(time.Now().Sub(now).Seconds())).
				Infof("collect %d/%d (sampling/total) lines", n, count)
			if err != nil {
				return err
			}
		}
	}

	return nil
}

func (s *Snapshot) getBigTables() map[string]struct{} {
	var l = logrus.WithField("func", "*Snapshot.getBigTables")
	var result = make(map[string]struct{})
	const dataLength = 2_000_000_000
	tx := s.from.Raw("SELECT table_name FROM information_schema.tables WHERE data_length > ?", dataLength)
	if err := tx.Error; err != nil {
		l.Errorln("failed to select table_name from information_schema.tables")
		return result
	}
	rows, err := tx.Rows()
	if err != nil {
		l.Errorln("failed to Rows select tabl_name from information_schema")
		return result
	}
	for rows.Next() {
		var tableName string
		if err := rows.Scan(&tableName); err != nil {
			l.Errorln("failed to Scan tableName")
			continue
		}
		result[tableName] = struct{}{}
	}
	return result
}

func TrimCollateOptionFromCols(create *ast.CreateTableStmt) {
	if create == nil {
		return
	}
	for i := range create.Cols {
		for j := len(create.Cols[i].Options) - 1; j >= 0; j-- {
			if create.Cols[i].Options[j].Tp == ast.ColumnOptionCollate {
				create.Cols[i].Options = append(create.Cols[i].Options[:j], create.Cols[i].Options[j+1:]...)
			}
		}
	}
}

func TrimCollateOptionFromCreateTable(create *ast.CreateTableStmt) {
	if create == nil {
		return
	}
	for i := len(create.Options) - 1; i >= 0; i-- {
		if create.Options[i].Tp == ast.TableOptionCollate {
			create.Options = append(create.Options[:i], create.Options[i+1:]...)
		}
	}
}

func TrimConstraintCheckFromCreateTable(create *ast.CreateTableStmt) {
	if create == nil {
		return
	}
	for i := len(create.Constraints) - 1; i >= 0; i-- {
		if create.Constraints[i].Tp == ast.ConstraintCheck {
			create.Constraints = append(create.Constraints[:i], create.Constraints[i+1:]...)
		}
	}
}

func TrimCharacterSetFromRawCreateTableSQL(create string, except ...string) string {
	pat := `(?i)(?:DEFAULT)* (?:CHARACTER SET|CHARSET)\s*=\s*(\w+)`
	re := regexp.MustCompile(pat)
	found := re.FindStringSubmatch(create)
	if len(found) == 0 {
		return create
	}
	for _, ex := range except {
		if strings.EqualFold(ex, found[len(found)-1]) {
			return create
		}
	}
	return re.ReplaceAllString(create, "")
}

func TrimBlockFormat(create string) string {
	return strings.ReplaceAll(create, "BLOCK_FORMAT=ENCRYPTED", "")
}

// ParseCreateTableStmt parses CreateTableStmt as *ast.CreateTableStmt node
func ParseCreateTableStmt(create string) (*ast.CreateTableStmt, error) {
	create = TrimCharacterSetFromRawCreateTableSQL(create, CharsetWhite()...)
	create = TrimBlockFormat(create)
	node, err := parser.New().ParseOneStmt(create, "", "")
	if err != nil {
		return nil, err
	}
	stmt, ok := node.(*ast.CreateTableStmt)
	if !ok {
		return nil, errors.Errorf("the text is not CreateTableStmt, text: %s", create)
	}
	return stmt, nil
}

func CharsetWhite() []string {
	v := os.Getenv(charsetWhiteEnv)
	if len(v) == 0 {
		return defaultCharsetWhite
	}
	return strings.Split(v, ",")
}
