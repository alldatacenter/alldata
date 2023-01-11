package main

/*
mockHTTPClient, using this file will send a message to telemetryService
*/
import (
	"bytes"
	"encoding/json"
	"net/http"
	"os"

	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
)

var (
	targetServer     = "http://" + os.Getenv("TARGET_SERVER_ADDRESS")
	targetMqttServer = os.Getenv("TARGET_MQTT_SERVER_ADDRESS")
	targetSqlServer  = os.Getenv("TARGET_SQL_SERVER_ADDRESS")
)

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/mqtt", sendToMQTT)
	mux.HandleFunc("/sql", sendToTDengine)
	http.ListenAndServe(":9090", mux)

}

func sendToMQTT(w http.ResponseWriter, r *http.Request) {
	defaultTopic := "/test"
	req := &v1alpha1.TelemetryRequest{
		RawData: []byte("testData"),
		MQTTSetting: &v1alpha1.MQTTSetting{
			MQTTTopic:         &defaultTopic,
			MQTTServerAddress: &targetMqttServer,
		},
	}
	if err := sendRequest(req, "/mqtt"); err != nil {
		http.Error(w, "send failed", http.StatusInternalServerError)
	}
}

func sendToTDengine(w http.ResponseWriter, r *http.Request) {
	req := &v1alpha1.TelemetryRequest{
		SQLConnectionSetting: &v1alpha1.SQLConnectionSetting{
			ServerAddress: &targetSqlServer,
			UserName:      toPointer("root"),
			Secret:        toPointer("taosdata"),
			DBName:        toPointer("shifu"),
			DBTable:       toPointer("testsubtable"),
			DBType:        toPointer(v1alpha1.DBTypeTDengine),
		},
		RawData: []byte("testData"),
	}
	sendRequest(req, "/sql")
}

func sendRequest(request *v1alpha1.TelemetryRequest, path string) error {
	requestBody, err := json.Marshal(request)
	if err != nil {
		return err
	}

	_, err = http.DefaultClient.Post(targetServer+path, "application/json", bytes.NewBuffer(requestBody))
	return err
}

func toPointer[T any](v T) *T {
	return &v
}
