package tdengine

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	_ "github.com/taosdata/driver-go/v3/taosRestful"
)

type DBHelper struct {
	DB       *sql.DB
	Settings *v1alpha1.SQLConnectionSetting
}

func SendToTDengine(ctx context.Context, rawData []byte, sqlcs *v1alpha1.SQLConnectionSetting) error {
	db := &DBHelper{Settings: sqlcs}

	err := db.connectToTDengine(ctx)
	if err != nil {
		logger.Errorf("Error to Connect to tdengine, error %v", err.Error())
		return err
	}

	err = db.insertDataToDB(ctx, rawData)
	if err != nil {
		logger.Errorf("Error to Insert rawData to DB, errror: %v", err.Error())
		return err
	}

	return nil
}

func (db *DBHelper) connectToTDengine(ctx context.Context) error {
	var err error
	taosUri := constructTDengineUri(db.Settings)
	db.DB, err = sql.Open("taosRestful", taosUri)
	logger.Infof("Try connect to tdengine %v", *db.Settings.DBName)
	return err
}

func (db *DBHelper) insertDataToDB(ctx context.Context, rawData []byte) error {
	result, err := db.DB.Exec(fmt.Sprintf("Insert Into %s Values('%s','%s')", *db.Settings.DBTable, time.Now().Format("2006-01-02 15:04:05"), string(rawData)))
	if err != nil {
		logger.Errorf("Error to Insert RawData to db, error: %v", err)
		return err
	}

	id, err := result.RowsAffected()
	if err != nil {
		logger.Errorf("Error to get RowsAffected, error: %v", err)
		return err
	} else if id <= 0 {
		logger.Errorf("Data insert failed, for RowsAffected is equal or lower than 0")
		return errors.New("insert Failed")
	}

	logger.Infof("Successfully Insert Data %v to DB %v", string(rawData), db.Settings.DBName)
	return nil
}

// constructTDengineUri  example: root:taosdata@http(localhost:6041)/test
func constructTDengineUri(sqlcs *v1alpha1.SQLConnectionSetting) string {
	return fmt.Sprintf("%s:%s@http(%s)/%s", *sqlcs.UserName, *sqlcs.Secret, *sqlcs.ServerAddress, *sqlcs.DBName)
}
