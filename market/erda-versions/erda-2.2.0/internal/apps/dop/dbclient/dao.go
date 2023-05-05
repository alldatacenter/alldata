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

package dbclient

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/pkg/database/cimysql"
)

func FindTPRecord(tp *TPRecordDO) (*TPRecordDO, error) {
	success, err := cimysql.Engine.Get(tp)
	if err != nil {
		return nil, errors.Wrap(err, "find tp record")
	}

	if !success {
		return nil, errors.New("not found in db")
	}

	return tp, nil
}

func FindTPRecordById(id uint64) (*TPRecordDO, error) {
	r := NewTPRecordDO()
	r.ID = id

	r, err := FindTPRecord(r)
	if err != nil {
		return nil, errors.Wrapf(err, "find tp record by id=%d", id)
	}
	return r, nil
}

func FindTPRecordByCommitId(commitID string) (*TPRecordDO, error) {
	r := NewTPRecordDO()
	r.CommitID = commitID

	success, err := cimysql.Engine.Get(r)
	if err != nil {
		return nil, errors.Errorf("failed to find record, commitID: %s, (%+v)", commitID, err)
	}

	if !success {
		return nil, errors.Errorf("failed to find record, commitID: %s", commitID)
	}

	return r, nil
}

func FindTPRecordPagingByAppID(req *pb.TestRecordPagingRequest) (*Paging, error) {
	var list []*TPRecordDO
	total, err := cimysql.Engine.Select("id,name,branch,operator_name,totals,type,created_at,coverage_report").Where("app_id = ?", req.ApplicationId).
		Limit(int(req.PageSize), (int(req.PageNo)-1)*int(req.PageSize)).Desc("id").FindAndCount(&list)
	if err != nil {
		return nil, err
	}

	return &Paging{
		Total: total,
		List:  list,
	}, nil
}

func InsertTPRecord(r *TPRecordDO) (*TPRecordDO, error) {
	var err error
	var affected int64

	if affected, err = cimysql.Engine.InsertOne(r); err != nil {
		return nil, err
	}
	if affected != 1 {
		return nil, errors.Errorf("failed to insert.")
	}
	return r, nil
}

func InsertTPRecords(rs ...*TPRecordDO) error {
	var (
		affected int64
		err      error
	)

	session := cimysql.Engine.NewSession()
	defer sessionClose(session, err)

	err = session.Begin()
	if err != nil {
		return err
	}

	if affected, err = session.Insert(rs); err != nil {
		return err
	}

	if affected != int64(len(rs)) {
		return errors.Errorf("failed to insert")
	}

	return nil
}

func sessionClose(session *xorm.Session, err error) {
	var e error
	if session != nil {
		if err != nil {
			if e = session.Rollback(); e != nil {
				logrus.Error(e)
			}
		} else {
			if e = session.Commit(); e != nil {
				logrus.Error(e)
			}
		}
		session.Close()
	}
}

func FindLatestSonarByAppID(appID int64) (*QASonar, error) {
	r := &QASonar{}
	r.ApplicationID = appID

	success, err := cimysql.Engine.Desc("updated_at").Get(r)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to find sonar records, appID: %d", appID)
	}

	// if no records in db，it is normal and cannot return errors
	if !success {
		return nil, nil
	}

	return r, nil
}
