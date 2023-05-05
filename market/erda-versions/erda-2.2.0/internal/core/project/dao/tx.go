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

package dao

import (
	"errors"

	"gorm.io/gorm"
)

var q *TX

var InvalidTransactionError = errors.New("invalid transaction, it is already committed or roll backed")

type TX struct {
	Error error

	tx    *gorm.DB
	inTx  bool
	valid bool
}

func Init(db *gorm.DB) {
	if q == nil {
		q = &TX{
			tx:    db,
			valid: true,
		}
	}
}

func Q() *TX {
	if q == nil || q.tx == nil {
		panic("q is not init")
	}
	return &TX{
		tx:    q.tx,
		valid: true,
	}
}

func Begin() *TX {
	return &TX{
		tx:    Q().DB().Begin(),
		valid: true,
		inTx:  true,
	}
}

func (tx *TX) Create(i interface{}) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	tx.Error = tx.tx.Create(i).Error
	return tx.Error
}

func (tx *TX) CreateInBatches(i interface{}, size int) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	tx.Error = tx.tx.CreateInBatches(i, size).Error
	return tx.Error
}

func (tx *TX) Delete(i interface{}, options ...Option) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	var db = tx.DB()
	for _, opt := range options {
		db = opt(db)
	}
	return db.Delete(i).Error
}

func (tx *TX) Updates(i, v interface{}, options ...Option) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	var db = tx.DB()
	for _, opt := range options {
		db = opt(db)
	}
	return db.Model(i).Updates(v).Error
}

func (tx *TX) UpdateColumns(i interface{}, options ...Option) error {
	if tx.inTx && !tx.valid {
		return InvalidTransactionError
	}
	var db = tx.DB()
	db = db.Model(i)
	for _, opt := range options {
		db = opt(db)
	}
	return db.Error
}

func (tx *TX) List(i interface{}, options ...Option) (int64, error) {
	var total int64
	var db = tx.DB()
	for _, opt := range options {
		db = opt(db)
	}

	err := db.Find(i).Count(&total).Error
	if err == nil {
		return total, nil
	}
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return 0, nil
	}
	return 0, err
}

func (tx *TX) Get(i interface{}, options ...Option) (bool, error) {
	var db = tx.DB()
	for _, opt := range options {
		db = opt(db)
	}

	err := db.First(i).Error
	if err == nil {
		return true, nil
	}
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return false, nil
	}
	return false, err
}

func (tx *TX) CommitOrRollback() {
	if tx.inTx && !tx.valid {
		return
	}
	if tx.Error == nil {
		tx.tx.Commit()
	} else {
		tx.tx.Rollback()
	}
	tx.valid = false
}

func (tx *TX) DB() *gorm.DB {
	return tx.tx
}
