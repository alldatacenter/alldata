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

package dao_test

import (
	"os"
	"path/filepath"
	"strconv"
	"testing"

	"gorm.io/driver/sqlite"
	"gorm.io/gorm"

	"github.com/erda-project/erda/internal/core/project/dao"
)

var dsn = filepath.Join(os.TempDir(), "gorm.db")

type User struct {
	Age  int64 `gorm:"type:BIGINT"`
	Name string
}

func TestTX_Create(t *testing.T) {
	openDB(t)
	defer closeDB()

	var users []User
	for i := 0; i < 10; i++ {
		users = append(users, User{Age: int64(i), Name: "dspo-" + strconv.Itoa(i)})
	}

	if err := dao.Q().DB().AutoMigrate(new(User)); err != nil {
		t.Fatalf("failed to migrate user: %v", err)
	}

	// test create
	if err := dao.Q().Create(&User{Age: 10, Name: "dspo-10"}); err != nil {
		t.Fatalf("failed to Create: %v", err)
	}
	total, err := dao.Q().List(new([]User))
	if err != nil {
		t.Fatalf("failed to List")
	}
	if total != 1 {
		t.Fatalf("total should be %v", 1)
	}

	// test create in batches
	if err := dao.Q().CreateInBatches(users, 2); err != nil {
		t.Fatalf("failed to CreateInBatches: %v", err)
	}
	total, err = dao.Q().List(new([]User))
	if err != nil {
		t.Fatalf("failed to List")
	}
	if total != 11 {
		t.Fatalf("total should be %v", 11)
	}

	// test Option Where
	var user User
	ok, err := dao.Q().Get(&user, dao.Where("name = ?", "dspo-0"))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if !ok {
		t.Fatalf("ok should be true")
	}
	ok, err = dao.Q().Get(&user, dao.Where("age = ? AND name = ?", 0, "dspo-0"))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if !ok {
		t.Fatalf("ok shoule be true")
	}
	ok, err = dao.Q().Get(&user, dao.Where("age > ?", 15))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if ok {
		t.Fatalf("ok should be false")
	}

	// test Option Wheres
	ok, err = dao.Q().Get(&user, dao.Wheres(map[string]interface{}{"name": "dspo-0"}))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if !ok {
		t.Fatalf("ok should be true")
	}
	if user.Age != 0 || user.Name != "dspo-0" {
		t.Fatalf("user record error")
	}
	ok, err = dao.Q().Get(&user, dao.Wheres(User{Name: "dspo-1"}))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if !ok {
		t.Fatal("ok should be true")
	}
	if user.Age != 1 || user.Name != "dspo-1" {
		t.Fatal("user record error")
	}

	// test Option WhereColum.IS
	var name = dao.Col("name")
	ok, err = dao.Q().Get(&user, name.Is(nil))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if ok {
		t.Fatal("ok should be true")
	}

	ok, err = dao.Q().Get(&user, name.Is("dspo-2"))
	if err != nil {
		t.Fatalf("failed to Get: %v", err)
	}
	if !ok {
		t.Fatalf("ok should be true")
	}

	// test Option WhereColumn.In
	var users2 []User
	total, err = dao.Q().List(&users2, name.In("dspo-0", "dspo-1", "dspo-3", "dspo-15"))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 3 {
		t.Fatalf("total is expected to be %v, got %v", 3, total)
	}

	// test Option WhereColumn.InMap
	total, err = dao.Q().List(&users2, name.InMap(map[interface{}]struct{}{
		"dspo-4":  {},
		"dspo-5":  {},
		"dspo-15": {},
	}))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 2 {
		t.Fatalf("total is expected to be: %v, got: %v", 2, total)
	}

	// test Option WhereColumn.Like
	total, err = dao.Q().List(&users2, name.Like("dspo-%"))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 11 {
		t.Fatalf("total is expected: %v, got: %v", 11, total)
	}

	// test Option WhereColumn.GreaterThan
	var age = dao.Col("age")
	total, err = dao.Q().List(&users2, age.GreaterThan(8))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 2 {
		t.Fatalf("total is expected: %v, got: %v", 1, total)
	}

	// test Option WhereColumn.EqGreaterThan
	total, err = dao.Q().List(&users2, age.EqGreaterThan(8))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 3 {
		t.Fatalf("total is expected: %v, got: %v", 2, total)
	}

	// test Option WhereColumn.LessThan
	total, err = dao.Q().List(&users2, age.LessThan(8))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 8 {
		t.Fatalf("total is expected: %v, got: %v", 8, total)
	}

	// test Option WhereColumn.EqLessThan
	total, err = dao.Q().List(&users2, age.EqLessThan(8))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 9 {
		t.Fatalf("total is expected to be: %v, got: %v", 9, total)
	}

	// test Option Column.DESC
	_, err = dao.Q().List(&users2, age.DESC())
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if users2[0].Name != "dspo-10" {
		t.Fatalf("the first user's name is expected to be: %s, got: %s", "dspo-10", users2[0].Name)
	}

	// test Option WhereValue.In
	total, err = dao.Q().List(&users2, dao.Value("dspo-1").In("name", "age"))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 1 {
		t.Fatalf("total is expected to be: %v, got: %v", 1, total)
	}

	// test Option Paging
	total, err = dao.Q().List(&users2, age.GreaterThan(0), dao.Paging(-1, 0))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 10 {
		t.Fatalf("total is expected: %v, got: %v", 10, total)
	}
	if len(users2) != 10 {
		t.Fatalf("length of users2 is expected to be: %v, got: %v", 0, len(users2))
	}

	total, err = dao.Q().List(&users2, age.GreaterThan(0), dao.Paging(5, 1))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 10 {
		t.Fatalf("failed to List: %v", err)
	}
	if len(users2) != 5 {
		t.Fatalf("length of users2 is expected to be: %v, got: %v", 5, len(users2))
	}

	// test Option OrderBy
	total, err = dao.Q().List(&users2, dao.OrderBy("age", dao.DESC))
	if err != nil {
		t.Fatalf("failed to List: %v", err)
	}
	if total != 11 {
		t.Fatalf("total is expected to be: %v, got: %v", 11, total)
	}
	if users2[0].Name != "dspo-10" {
		t.Fatalf("the first record's Name is expected to be: %s, got: %s", "dspo-10", users2[0].Name)
	}

	// test update
	if err := dao.Q().Updates(&user, map[string]interface{}{"name": "cmc-10"}, name.Is(1)); err != nil {
		t.Fatalf("failed to Updates: %v", err)
	}
	t.Logf("user: %+v", user)
}

func openDB(t *testing.T) {
	db, err := gorm.Open(sqlite.Open(dsn), &gorm.Config{})
	if err != nil {
		t.Fatalf("failed to open %s: %v", dsn, err)
	}
	dao.Init(db.Debug())
}

func closeDB() {
	os.Remove(dsn)
}
