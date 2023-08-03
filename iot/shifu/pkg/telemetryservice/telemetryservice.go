package telemetryservice

import (
	"net/http"
	"os"

	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/edgenesis/shifu/pkg/telemetryservice/minio"
	"github.com/edgenesis/shifu/pkg/telemetryservice/mqtt"
	"github.com/edgenesis/shifu/pkg/telemetryservice/sql"
)

var serverListenPort = os.Getenv("SERVER_LISTEN_PORT")

func New(stop <-chan struct{}) {
	mux := http.NewServeMux()
	mux.HandleFunc(v1alpha1.TelemetryServiceURIMQTT, mqtt.BindMQTTServicehandler)
	mux.HandleFunc(v1alpha1.TelemetryServiceURISQL, sql.BindSQLServiceHandler)
	mux.HandleFunc(v1alpha1.TelemetryServiceURIMinIO, minio.BindMinIOServiceHandler)
	err := Start(stop, mux, serverListenPort)
	if err != nil {
		logger.Errorf("Error when telemetryService Running, error: %v", err)
	}
}

func Start(stop <-chan struct{}, mux *http.ServeMux, addr string) error {
	var errChan = make(chan error, 1)
	server := http.Server{
		Addr:    addr,
		Handler: mux,
	}

	go func() {
		err := server.ListenAndServe()
		if err != nil {
			logger.Errorf("Error when server running, error: %v", err)
			errChan <- err
		}
	}()

	logger.Infof("Listening at %#v", addr)
	select {
	case err := <-errChan:
		return err
	case <-stop:
		return server.Close()
	}
}
