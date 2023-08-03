package sql

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/telemetryservice/utils"
	"io"
	"net/http"

	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/edgenesis/shifu/pkg/telemetryservice/sql/tdengine"
)

func BindSQLServiceHandler(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		logger.Errorf("Error when Read Data From Body, error: %v", err)
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	logger.Infof("requestBody: %s", string(body))
	request := v1alpha1.TelemetryRequest{}

	err = json.Unmarshal(body, &request)
	if err != nil {
		logger.Errorf("Error to Unmarshal request body to struct")
		http.Error(w, "unexpected end of JSON input", http.StatusBadRequest)
		return
	}

	injectSecret(request.SQLConnectionSetting)

	switch *request.SQLConnectionSetting.DBType {
	case v1alpha1.DBTypeTDengine:
		err = tdengine.SendToTDengine(context.TODO(), request.RawData, request.SQLConnectionSetting)
	default:
		err = fmt.Errorf("UnSupport DB Type")
	}

	if err != nil {
		logger.Errorf("Error to Send to SQL Server, error: %s", err.Error())
		http.Error(w, "Error to send to server", http.StatusBadRequest)
	}
}

func injectSecret(setting *v1alpha1.SQLConnectionSetting) {
	if setting == nil {
		logger.Warn("empty telemetry service setting.")
		return
	}
	if setting.Secret == nil {
		logger.Warn("empty secret setting.")
		return
	}
	secret, err := utils.GetSecret(*setting.Secret)
	if err != nil {
		logger.Errorf("unable to get secret for telemetry %v, error: %v", *setting.Secret, err)
		return
	}
	pwd, exist := secret[deviceshifubase.PasswordSecretField]
	if !exist {
		logger.Errorf("the %v field not found in telemetry secret", deviceshifubase.PasswordSecretField)
	} else {
		*setting.Secret = pwd
		logger.Info("SQLSetting.Secret load from secret")
	}
	username, exist := secret[deviceshifubase.UsernameSecretField]
	if !exist {
		logger.Errorf("the %v field not found in telemetry secret", deviceshifubase.UsernameSecretField)
		if setting.UserName == nil {
			setting.UserName = new(string)
		}
	} else {
		setting.UserName = &username
		logger.Info("SQLSetting.UserName load from secret")
	}
}
