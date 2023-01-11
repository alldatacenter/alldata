package deviceshifuplc4x

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	plc4go "github.com/apache/plc4x/plc4go/pkg/api"
	"github.com/apache/plc4x/plc4go/pkg/api/drivers"
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/deviceshifu/utils"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"golang.org/x/net/context"
)

// DeviceShifu deviceshifu and socketConnection for Socket
type DeviceShifu struct {
	base *deviceshifubase.DeviceShifuBase
	conn *plc4go.PlcConnectionConnectResult
}

// New new socket deviceshifu
func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifu, error) {
	base, mux, err := deviceshifubase.New(deviceShifuMetadata)
	if err != nil {
		return nil, err
	}

	ds := &DeviceShifu{base: base}

	var connectionResult plc4go.PlcConnectionConnectResult
	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		if *base.EdgeDevice.Spec.Protocol != v1alpha1.ProtocolPLC4X {
			return nil, fmt.Errorf("Protocol not support in plc4x")
		}

		protocol := base.EdgeDevice.Spec.ProtocolSettings.PLC4XSetting.Protocol

		driverManager := plc4go.NewPlcDriverManager()

		switch *protocol {
		case v1alpha1.Plc4xProtocolS7:
			drivers.RegisterS7Driver(driverManager)
		case v1alpha1.Plc4xProtocolADS:
			drivers.RegisterAdsDriver(driverManager)
		case v1alpha1.Plc4xProtocolBACnet:
			drivers.RegisterBacnetDriver(driverManager)
		case v1alpha1.Plc4xProtocolCBus:
			drivers.RegisterCBusDriver(driverManager)
		case v1alpha1.Plc4xProtocolEip:
			drivers.RegisterEipDriver(driverManager)
		case v1alpha1.Plc4xProtocolKnx:
			drivers.RegisterKnxDriver(driverManager)
		case v1alpha1.Plc4xProtocolModbusAscii:
			drivers.RegisterModbusAsciiDriver(driverManager)
		case v1alpha1.Plc4xProtocolModbusRTU:
			drivers.RegisterModbusRtuDriver(driverManager)
		case v1alpha1.Plc4xProtocolModbusTcp:
			drivers.RegisterModbusTcpDriver(driverManager)
		}

		address := string(*protocol) + "://" + *base.EdgeDevice.Spec.Address

		connectionRequestChanel := driverManager.GetConnection(address)

		// Wait for the driver to connect (or not)
		connectionResult = <-connectionRequestChanel

		// Check if something went wrong
		if connectionResult.GetErr() != nil {
			logger.Errorf("Error connecting to PLC: %s", connectionResult.GetErr().Error())
			return nil, err
		}

		// If all was ok, get the connection instance
		connection := connectionResult.GetConnection()

		if !connection.IsConnected() {
			logger.Errorf("Cannot Connected to %v", *base.EdgeDevice.Spec.Address)
			return nil, fmt.Errorf("Cannot Connect to %v", *base.EdgeDevice.Spec.Address)
		}

		ds.conn = &connectionResult

		logger.Infof("Connected to '%v'", *base.EdgeDevice.Spec.Address)
		mux.HandleFunc("/read", ds.readCommandHandlerPlc4x())
		mux.HandleFunc("/write", ds.writeCommandHandlerPlc4x())

	}
	deviceshifubase.BindDefaultHandler(mux)

	ds.base.UpdateEdgeDeviceResourcePhase(v1alpha1.EdgeDevicePending)
	return ds, nil
}

func (ds *DeviceShifu) writeCommandHandlerPlc4x() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		timeout := time.Duration(*ds.base.DeviceShifuConfig.Instructions.InstructionSettings.DefaultTimeoutSeconds)

		params, err := utils.ParseHTTPGetParams(r.URL.String())
		if err != nil {
			logger.Errorf("Error when parse request url, error: %v", err)
			http.Error(w, "Error when parse request url, error: "+err.Error(), http.StatusBadRequest)
			return
		}
		request := (*ds.conn).GetConnection().WriteRequestBuilder()

		for key, value := range params {
			fieldId := fmt.Sprintf("field_%s", key)
			request = request.AddQuery(fieldId, key, value)
		}

		plc4xRequest, err := request.Build()
		if err != nil {
			logger.Errorf("Error when build request by params, error: %v", err)
			http.Error(w, "Error when build request with params, error: "+err.Error(), http.StatusBadRequest)
			return
		}

		ctx, cancel := context.WithTimeout(context.Background(), time.Second*timeout)
		defer cancel()
		resultData := <-plc4xRequest.ExecuteWithContext(ctx)

		if err := resultData.GetErr(); err != nil {
			logger.Errorf("Got Error on execute request, error: %v", err)
			http.Error(w, "Got Error on execute request, error: "+err.Error(), http.StatusInternalServerError)
			return
		}
	}
}

func (ds *DeviceShifu) readCommandHandlerPlc4x() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		logger.Infof("Url: %v", r.RequestURI)
		timeout := time.Duration(*ds.base.DeviceShifuConfig.Instructions.InstructionSettings.DefaultTimeoutSeconds)

		params, err := utils.ParseHTTPGetParams(r.RequestURI)
		if err != nil {
			logger.Errorf("Error when parse request url, error: %v", err)
			http.Error(w, "Error when parse request url, error: "+err.Error(), http.StatusBadRequest)
			return
		}
		request := (*ds.conn).GetConnection().ReadRequestBuilder()

		logger.Infof("Params: %v", params)

		for key := range params {
			fieldId := fmt.Sprintf("field_%s", key)
			request = request.AddQuery(fieldId, key)
		}

		plc4xRequest, err := request.Build()
		if err != nil {
			logger.Errorf("Error when build request by params, error: %v", err)
			http.Error(w, "Error when build request with params, error: "+err.Error(), http.StatusBadRequest)
			return
		}

		ctx, cancel := context.WithTimeout(context.Background(), time.Second*timeout)
		defer cancel()
		resultData := <-plc4xRequest.ExecuteWithContext(ctx)

		if err := resultData.GetErr(); err != nil {
			logger.Errorf("Got Error on execute request, error: %v", err)
			http.Error(w, "Got Error on execute request, error: "+err.Error(), http.StatusInternalServerError)
			return
		}

		resultMap := make(map[string]string, len(params))
		for _, field := range resultData.GetResponse().GetFieldNames() {
			value := resultData.GetResponse().GetValue(field).String()
			resultMap[field] = value
		}

		result, err := json.Marshal(resultMap)
		if err != nil {
			logger.Errorf("Error when marshal result to []byte, error: ", err)
			http.Error(w, "Error when marshal result to []byte, error:"+err.Error(), http.StatusInternalServerError)
			return
		}

		_, err = w.Write(result)
		if err != nil {
			logger.Errorf("Error when write data into response, error: %v", err)
			http.Error(w, "Error when write data into response, error: "+err.Error(), http.StatusInternalServerError)
		}
	}
}

func (ds *DeviceShifu) collectPLC4XTelemetry() (bool, error) {
	timeout := time.Duration(*ds.base.DeviceShifuConfig.Telemetries.DeviceShifuTelemetrySettings.DeviceShifuTelemetryTimeoutInMilliseconds)

	conn := (*ds.conn).GetConnection()
	pingResult := conn.Ping()

	select {
	case <-time.After(timeout * time.Millisecond):
		return false, fmt.Errorf("timeout ping to device")
	case result := <-pingResult:
		if err := result.GetErr(); err != nil {
			return false, err
		}
	}

	return true, nil
}

// Start start socket telemetry
func (ds *DeviceShifu) Start(stopCh <-chan struct{}) error {
	return ds.base.Start(stopCh, ds.collectPLC4XTelemetry)
}

// Stop stop http server
func (ds *DeviceShifu) Stop() error {
	return ds.base.Stop()
}
