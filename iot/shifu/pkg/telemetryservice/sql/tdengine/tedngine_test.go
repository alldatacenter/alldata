package tdengine

import (
	"context"
	"database/sql"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/stretchr/testify/assert"
)

func TestConstructTDengineUri(t *testing.T) {
	testCases := []struct {
		desc   string
		Input  v1alpha1.SQLConnectionSetting
		output string
	}{
		{
			desc: "test",
			Input: v1alpha1.SQLConnectionSetting{
				UserName:      unitest.ToPointer("testUser"),
				Secret:        unitest.ToPointer("testPassword"),
				ServerAddress: unitest.ToPointer("testAddress"),
				DBName:        unitest.ToPointer("testDB"),
			},
			output: "testUser:testPassword@http(testAddress)/testDB",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.desc, func(t *testing.T) {
			result := constructTDengineUri(&tC.Input)
			assert.Equal(t, tC.output, result)
		})
	}
}

func TestInsertDataToDB(t *testing.T) {
	testCases := []struct {
		desc         string
		expectSQL    string
		rawData      []byte
		dbHelper     *DBHelper
		expectResult sql.Result
		expectErr    string
		preCloseDB   bool
	}{
		{
			desc:      "testCases 1 insert Successfully",
			expectSQL: "Insert Into testTable",
			rawData:   []byte("testData"),
			dbHelper: &DBHelper{
				Settings: &v1alpha1.SQLConnectionSetting{
					DBName:  unitest.ToPointer("testDB"),
					DBTable: unitest.ToPointer("testTable"),
				},
			},
			expectResult: sqlmock.NewResult(1, 1),
		},
		{
			desc:    "testCases2 without DBName",
			rawData: []byte("testData"),
			dbHelper: &DBHelper{
				Settings: &v1alpha1.SQLConnectionSetting{
					DBName:  unitest.ToPointer("testDB"),
					DBTable: unitest.ToPointer(""),
				},
			},
			preCloseDB: true,
			expectErr:  "sql: database is closed",
		},
		{
			desc:      "testCases 3 LastInsertId = 0",
			expectSQL: "Insert Into testTable",
			rawData:   []byte("testData"),
			dbHelper: &DBHelper{
				Settings: &v1alpha1.SQLConnectionSetting{
					DBName:  unitest.ToPointer("testDB"),
					DBTable: unitest.ToPointer("testTable"),
				},
			},
			expectResult: sqlmock.NewResult(0, 0),
			expectErr:    "insert Failed",
		},
	}
	for _, tC := range testCases {
		t.Run(tC.desc, func(t *testing.T) {
			db, sm, err := sqlmock.New()
			assert.Nil(t, err)
			if tC.preCloseDB {
				db.Close()
			} else {
				defer db.Close()
			}

			sm.ExpectExec(tC.expectSQL).WillReturnResult(tC.expectResult)
			helper := DBHelper{DB: db, Settings: tC.dbHelper.Settings}
			err = helper.insertDataToDB(context.TODO(), tC.rawData)
			if tC.expectErr == "" {
				assert.Nil(t, err)
			} else {
				assert.NotNil(t, err)
				assert.Equal(t, tC.expectErr, err.Error())
			}
		})
	}
}

// just for cover the code
func TestConnectTdengine(t *testing.T) {
	db := &DBHelper{
		Settings: &v1alpha1.SQLConnectionSetting{
			UserName:      unitest.ToPointer("testUser"),
			Secret:        unitest.ToPointer("testSecret"),
			ServerAddress: unitest.ToPointer("testAddress"),
			DBName:        unitest.ToPointer("testDB"),
		},
	}
	err := db.connectToTDengine(context.TODO())
	assert.Nil(t, err)
}

func TestSendToTDengine(t *testing.T) {
	settings := &v1alpha1.SQLConnectionSetting{
		UserName:      unitest.ToPointer("testUser"),
		Secret:        unitest.ToPointer("testSecret"),
		ServerAddress: unitest.ToPointer("1.2.3.4"),
		DBName:        unitest.ToPointer("testDB"),
		DBTable:       unitest.ToPointer("testTable"),
	}
	expectErr := "invalid DSN: network address not terminated (missing closing brace)"
	err := SendToTDengine(context.TODO(), []byte("test"), settings)
	assert.Equal(t, expectErr, err.Error())
}
