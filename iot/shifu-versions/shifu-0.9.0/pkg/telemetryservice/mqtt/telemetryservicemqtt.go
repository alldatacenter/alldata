package mqtt

import (
	"encoding/json"
	"fmt"
	"github.com/edgenesis/shifu/pkg/telemetryservice/utils"
	"io"
	"net/http"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
)

func BindMQTTServicehandler(w http.ResponseWriter, r *http.Request) {
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

	injectSecret(request.MQTTSetting)

	client, err := connectToMQTT(request.MQTTSetting)
	if err != nil {
		logger.Errorf("Error to connect to mqtt server, error: %#v", err)
		http.Error(w, "Error to connect to server", http.StatusBadRequest)
		return
	}
	defer (*client).Disconnect(0)

	token := (*client).Publish(*request.MQTTSetting.MQTTTopic, 1, false, request.RawData)
	if token.Error() != nil {
		logger.Errorf("Error when publish Data to MQTTServer, error: %#v", err.Error())
		http.Error(w, "Error to publish a message to server", http.StatusBadRequest)
		return
	}
	logger.Infof("Info: Success To publish a message %v to %v", string(request.RawData), request.MQTTSetting.MQTTServerAddress)
}

func connectToMQTT(settings *v1alpha1.MQTTSetting) (*mqtt.Client, error) {
	opts := mqtt.NewClientOptions()
	opts.AddBroker(fmt.Sprintf("tcp://%s", *settings.MQTTServerAddress))
	opts.SetClientID("shifu-service")
	opts.SetDefaultPublishHandler(messagePubHandler)
	opts.OnConnect = connectHandler
	opts.OnConnectionLost = connectLostHandler
	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		logger.Errorf("Error when connect to server error: %v", token.Error())
		return nil, token.Error()
	}
	logger.Infof("Connect to %v success!", *settings.MQTTServerAddress)
	return &client, nil
}

var messagePubHandler mqtt.MessageHandler = func(client mqtt.Client, msg mqtt.Message) {
	logger.Infof("MESSAGE_STR updated")
}

var connectHandler mqtt.OnConnectHandler = func(client mqtt.Client) {
	logger.Infof("Connected")
}

var connectLostHandler mqtt.ConnectionLostHandler = func(client mqtt.Client, err error) {
	logger.Infof("Connect lost: %v", err)
}

func injectSecret(setting *v1alpha1.MQTTSetting) {
	if setting == nil {
		logger.Warn("empty telemetry service setting.")
		return
	}
	if setting.MQTTServerSecret == nil {
		logger.Warn("empty secret setting.")
		return
	}
	secret, err := utils.GetSecret(*setting.MQTTServerSecret)
	if err != nil {
		logger.Errorf("unable to get secret for telemetry %v, error: %v", *setting.MQTTServerSecret, err)
		return
	}
	pwd, exist := secret[deviceshifubase.PasswordSecretField]
	if !exist {
		logger.Errorf("the %v field not found in telemetry secret", deviceshifubase.PasswordSecretField)
	} else {
		*setting.MQTTServerSecret = pwd
		logger.Info("MQTTServerSecret load from secret")
	}
}
