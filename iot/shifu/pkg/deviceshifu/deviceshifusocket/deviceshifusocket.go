package deviceshifusocket

import (
	"bufio"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/utils"
	"github.com/edgenesis/shifu/pkg/logger"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
)

// DeviceShifu deviceshifu and socketConnection for Socket
type DeviceShifu struct {
	base             *deviceshifubase.DeviceShifuBase
	socketConnection *net.Conn
}

// HandlerMetaData MetaData for Socket Handler
type HandlerMetaData struct {
	edgeDeviceSpec v1alpha1.EdgeDeviceSpec
	instruction    string
	properties     *deviceshifubase.DeviceShifuInstruction
	connection     *net.Conn
}

// New new socket deviceshifu
func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifu, error) {
	base, mux, err := deviceshifubase.New(deviceShifuMetadata)
	if err != nil {
		return nil, err
	}
	var socketConnection net.Conn

	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		switch protocol := *base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolSocket:
			connectionType := base.EdgeDevice.Spec.ProtocolSettings.SocketSetting.NetworkType
			if connectionType == nil || *connectionType != "tcp" {
				// todo need to validate in crd ( kubebuilder )
				return nil, fmt.Errorf("Sorry!, Shifu currently only support TCP Socket")
			}

			socketConnection, err := net.Dial(*connectionType, *base.EdgeDevice.Spec.Address)
			if err != nil {
				return nil, fmt.Errorf("Cannot connect to %v", *base.EdgeDevice.Spec.Address)
			}

			logger.Infof("Connected to '%v'", *base.EdgeDevice.Spec.Address)
			for instruction, properties := range base.DeviceShifuConfig.Instructions.Instructions {
				HandlerMetaData := &HandlerMetaData{
					base.EdgeDevice.Spec,
					instruction,
					properties,
					&socketConnection,
				}

				mux.HandleFunc("/"+instruction, deviceCommandHandlerSocket(HandlerMetaData))
			}
		}
	}

	ds := &DeviceShifu{base: base, socketConnection: &socketConnection}
	ds.base.UpdateEdgeDeviceResourcePhase(v1alpha1.EdgeDevicePending)
	deviceshifubase.BindDefaultHandler(mux)

	return ds, nil
}

func deviceCommandHandlerSocket(HandlerMetaData *HandlerMetaData) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var settings = HandlerMetaData.edgeDeviceSpec.ProtocolSettings.SocketSetting
		headerContentType := r.Header.Get("Content-Type")
		if headerContentType != "application/json" {
			http.Error(w, "content-type is not application/json", http.StatusBadRequest)
			logger.Errorf("content-type is not application/json")
			return
		}

		var socketRequest RequestBody
		err := json.NewDecoder(r.Body).Decode(&socketRequest)
		if err != nil {
			logger.Errorf("error decode: %v", socketRequest)
			http.Error(w, "error decode JSON "+err.Error(), http.StatusBadRequest)
			return
		}

		logger.Infof("After decode socket request: '%v', timeout:'%v'", socketRequest.Command, socketRequest.Timeout)
		connection := HandlerMetaData.connection
		timeout := socketRequest.Timeout
		if timeout > 0 {
			err := (*connection).SetDeadline(time.Now().Add(time.Duration(timeout) * time.Second))
			if err != nil {
				logger.Errorf("cannot send deadline to socket, error: %v", err)
				http.Error(w, "Failed to send deadline to socket, error:  "+err.Error(), http.StatusBadRequest)
				return
			}
		}

		command, err := decodeCommand(socketRequest.Command, *settings.Encoding)
		if err != nil {
			logger.Errorf("cannot decode Command from body, error: %v", err)
			http.Error(w, "Failed to decode Command from body, error:  "+err.Error(), http.StatusBadRequest)
			return
		}

		logger.Infof("Sending %v", command)
		_, err = (*connection).Write(command)
		if err != nil {
			logger.Errorf("cannot write command into socket, error: %v", err)
			http.Error(w, "Failed to send message to socket, error: "+err.Error(), http.StatusInternalServerError)
			return
		}

		message := make([]byte, *settings.BufferLength)
		n, _ := bufio.NewReader(*connection).Read(message)
		if n <= 0 {
			logger.Errorf("Error when Read data from connection to buffer")
			http.Error(w, "Failed to Read data from connection to buffer, for Read 0 byte from buffer", http.StatusInternalServerError)
			return
		}

		outputMessage, err := encodeMessage(message, *settings.Encoding)
		if err != nil {
			logger.Errorf("Error when encode message with Encoding, Encoding %v, error: %v", *settings.Encoding, err)
			http.Error(w, "Failed to encode message with Encoding,  Encoding "+string(*settings.Encoding)+", error: "+err.Error(), http.StatusBadRequest)
			return
		}
		handlerInstruction := HandlerMetaData.instruction
		instructionFuncName, shouldUsePythonCustomProcessing := deviceshifubase.CustomInstructionsPython[handlerInstruction]
		logger.Infof("Instruction %v is custom: %v", handlerInstruction, shouldUsePythonCustomProcessing)
		if shouldUsePythonCustomProcessing {
			logger.Infof("Instruction %v has a python customized handler configured.\n", handlerInstruction)
			outputMessage = utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, outputMessage, deviceshifubase.PythonScriptDir)
		}

		returnMessage := ReturnBody{
			Message: outputMessage,
			Status:  http.StatusOK,
		}

		w.WriteHeader(http.StatusOK)
		w.Header().Set("Content-Type", "application/json")
		err = json.NewEncoder(w).Encode(returnMessage)
		if err != nil {
			logger.Errorf("Failed encode message to json, error: %v" + err.Error())
			http.Error(w, "Failed encode message to json, error: "+err.Error(), http.StatusBadRequest)
		}
	}
}

// TODO: update configs
// TODO: update status based on telemetry

func (ds *DeviceShifu) collectSocketTelemetry() (bool, error) {
	if ds.base.EdgeDevice.Spec.Address == nil {
		return false, fmt.Errorf("Device %v does not have an address", ds.base.Name)
	}

	if ds.base.EdgeDevice.Spec.Protocol != nil {
		switch protocol := *ds.base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolSocket:
			conn, err := net.Dial("tcp", *ds.base.EdgeDevice.Spec.Address)
			if err != nil {
				logger.Errorf("error checking telemetry: error: %v", err.Error())
				return false, err
			}

			defer conn.Close()
			return true, nil
		default:
			logger.Warnf("EdgeDevice protocol %v not supported in deviceshifu", protocol)
			return false, nil
		}
	}
	return true, nil
}

// Start start socket telemetry
func (ds *DeviceShifu) Start(stopCh <-chan struct{}) error {
	return ds.base.Start(stopCh, ds.collectSocketTelemetry)
}

// Stop stop http server
func (ds *DeviceShifu) Stop() error {
	return ds.base.Stop()
}

func decodeCommand(input string, encode v1alpha1.Encoding) ([]byte, error) {
	var output []byte
	var err error

	switch encode {
	case v1alpha1.HEX:
		output, err = hex.DecodeString(input)
	case v1alpha1.UTF8:
		fallthrough
	default:
		output = []byte(input)
	}
	return output, err
}

func encodeMessage(input []byte, encode v1alpha1.Encoding) (string, error) {
	var output string
	var err error

	switch encode {
	case v1alpha1.HEX:
		output = hex.EncodeToString(input)
	case v1alpha1.UTF8:
		fallthrough
	default:
		output = string(input)
	}
	return output, err
}
