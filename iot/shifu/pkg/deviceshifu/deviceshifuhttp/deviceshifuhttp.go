package deviceshifuhttp

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/utils"
	"github.com/edgenesis/shifu/pkg/logger"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"

	"k8s.io/client-go/rest"
)

// DeviceShifuHTTP deviceshifu for HTTP
type DeviceShifuHTTP struct {
	base *deviceshifubase.DeviceShifuBase
}

// HandlerMetaData MetaData for HTTPhandler
type HandlerMetaData struct {
	edgeDeviceSpec v1alpha1.EdgeDeviceSpec
	instruction    string
	properties     *deviceshifubase.DeviceShifuInstruction
}

// CommandlineHandlerMetadata MetaData for HTTPCommandline handler
type CommandlineHandlerMetadata struct {
	edgeDeviceSpec  v1alpha1.EdgeDeviceSpec
	instruction     string
	properties      *deviceshifubase.DeviceShifuInstruction
	driverExecution string
}

var (
	instructionSettings *deviceshifubase.DeviceShifuInstructionSettings
)

const (
	DeviceNameHeaderField = "Device-Name"
)

// New This function creates a new Device Shifu based on the configuration
func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifuHTTP, error) {
	if deviceShifuMetadata.Namespace == "" {
		return nil, fmt.Errorf("DeviceShifuHTTP's namespace can't be empty")
	}

	base, mux, err := deviceshifubase.New(deviceShifuMetadata)
	if err != nil {
		return nil, err
	}

	instructionSettings = base.DeviceShifuConfig.Instructions.InstructionSettings
	if instructionSettings == nil {
		instructionSettings = &deviceshifubase.DeviceShifuInstructionSettings{}
	}

	if instructionSettings.DefaultTimeoutSeconds == nil {
		var defaultTimeoutSeconds = deviceshifubase.DeviceDefaultGlobalTimeoutInSeconds
		instructionSettings.DefaultTimeoutSeconds = &defaultTimeoutSeconds
	}

	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		// switch for different Shifu Protocols
		switch protocol := *base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolHTTP:
			for instruction, properties := range base.DeviceShifuConfig.Instructions.Instructions {
				HandlerMetaData := &HandlerMetaData{
					base.EdgeDevice.Spec,
					instruction,
					properties,
				}
				handler := DeviceCommandHandlerHTTP{base.RestClient, HandlerMetaData}
				mux.HandleFunc("/"+instruction, handler.commandHandleFunc())
			}
		case v1alpha1.ProtocolHTTPCommandline:
			driverExecution := base.DeviceShifuConfig.DriverProperties.DriverExecution
			if driverExecution == "" {
				return nil, fmt.Errorf("driverExecution cannot be empty")
			}

			for instruction, properties := range base.DeviceShifuConfig.Instructions.Instructions {
				CommandlineHandlerMetadata := &CommandlineHandlerMetadata{
					base.EdgeDevice.Spec,
					instruction,
					properties,
					base.DeviceShifuConfig.DriverProperties.DriverExecution,
				}

				handler := DeviceCommandHandlerHTTPCommandline{base.RestClient, CommandlineHandlerMetadata}
				mux.HandleFunc("/"+instruction, handler.commandHandleFunc())
			}
		default:
			logger.Errorf("EdgeDevice protocol %v not supported in deviceShifu_http_http", protocol)
			return nil, errors.New("wrong protocol not supported in deviceShifu_http_http")
		}
	}
	deviceshifubase.BindDefaultHandler(mux)

	ds := &DeviceShifuHTTP{base: base}

	ds.base.UpdateEdgeDeviceResourcePhase(v1alpha1.EdgeDevicePending)
	return ds, nil
}

// DeviceCommandHandlerHTTP handler for http
type DeviceCommandHandlerHTTP struct {
	client          *rest.RESTClient
	HandlerMetaData *HandlerMetaData
}

// This function is to create a URL containing directives from the requested URL
// e.g.:
// if we have http://localhost:8081/start?time=10:00:00&target=machine1&target=machine2
// and our address is http://localhost:8088 and instruction is start
// then we will get this URL string:
// http://localhost:8088/start?time=10:00:00&target=machine1&target=machine2
func createURIFromRequest(address string, handlerInstruction string, r *http.Request) string {

	queryStr := "?"

	for queryName, queryValues := range r.URL.Query() {
		for _, queryValue := range queryValues {
			queryStr += queryName + "=" + queryValue + "&"
		}
	}

	queryStr = strings.TrimSuffix(queryStr, "&")

	if queryStr == "?" {
		return "http://" + address + "/" + handlerInstruction
	}

	return "http://" + address + "/" + handlerInstruction + queryStr
}

// This function executes the instruction by requesting the url returned by createURIFromRequest
func (handler DeviceCommandHandlerHTTP) commandHandleFunc() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		handlerProperties := handler.HandlerMetaData.properties
		handlerInstruction := handler.HandlerMetaData.instruction
		handlerEdgeDeviceSpec := handler.HandlerMetaData.edgeDeviceSpec
		handlerHTTPClient := handler.client.Client

		if handlerProperties != nil {
			// TODO: handle validation compile
			for _, instructionProperty := range handlerProperties.DeviceShifuInstructionProperties {
				logger.Infof("Properties of command: %v %v", handlerInstruction, instructionProperty)
			}
		}

		var (
			resp              *http.Response
			httpErr, parseErr error
			requestBody       []byte
			ctx               context.Context
			cancel            context.CancelFunc
			timeout           = *instructionSettings.DefaultTimeoutSeconds
			reqType           = r.Method
		)

		logger.Infof("handling instruction '%v' to '%v' with request type %v", handlerInstruction, *handlerEdgeDeviceSpec.Address, reqType)

		timeoutStr := r.URL.Query().Get(deviceshifubase.DeviceInstructionTimeoutURIQueryStr)
		if timeoutStr != "" {
			timeout, parseErr = strconv.ParseInt(timeoutStr, 10, 64)
			if parseErr != nil {
				http.Error(w, parseErr.Error(), http.StatusBadRequest)
				logger.Errorf("timeout URI parsing error" + parseErr.Error())
				return
			}
		}

		switch reqType {
		case http.MethodPost:
			requestBody, parseErr = io.ReadAll(r.Body)
			if parseErr != nil {
				http.Error(w, "Error on parsing body", http.StatusBadRequest)
				logger.Errorf("Error on parsing body" + parseErr.Error())
				return
			}

			fallthrough
		case http.MethodGet:
			// for shifu.cloud timeout=0 is emptyomit
			// for hikivison's rtsp stream need never timeout
			if timeout <= 0 {
				ctx, cancel = context.WithCancel(context.Background())
			} else {
				ctx, cancel = context.WithTimeout(context.Background(), time.Duration(timeout)*time.Second)
			}

			defer cancel()
			httpURL := createURIFromRequest(*handlerEdgeDeviceSpec.Address, handlerInstruction, r)
			req, reqErr := http.NewRequestWithContext(ctx, reqType, httpURL, bytes.NewBuffer(requestBody))
			if reqErr != nil {
				http.Error(w, reqErr.Error(), http.StatusBadRequest)
				logger.Errorf("error creating HTTP request" + reqErr.Error())
				return
			}

			utils.CopyHeader(req.Header, r.Header)
			resp, httpErr = handlerHTTPClient.Do(req)
			if httpErr != nil {
				http.Error(w, httpErr.Error(), http.StatusServiceUnavailable)
				logger.Errorf("HTTP error" + httpErr.Error())
				return
			}
		default:
			http.Error(w, "not supported yet", http.StatusBadRequest)
			logger.Errorf("Request type %s is not supported yet!", reqType)
			return
		}

		if resp != nil {
			// Handling deviceshifu stuck when responseBody is a stream
			instructionFuncName, shouldUsePythonCustomProcessing := deviceshifubase.CustomInstructionsPython[handlerInstruction]
			if !shouldUsePythonCustomProcessing {
				utils.CopyHeader(w.Header(), resp.Header)
				_, err := io.Copy(w, resp.Body)
				if err != nil {
					logger.Errorf("cannot copy requestBody from requestBody, error: %s", err.Error())
				}
				return
			}

			respBody, readErr := io.ReadAll(resp.Body)
			if readErr != nil {
				logger.Errorf("error when read requestBody from responseBody, err: %s", readErr.Error())
				return
			}

			rawRespBodyString := string(respBody)
			logger.Infof("Instruction %s is custom: %s", handlerInstruction, shouldUsePythonCustomProcessing)
			w.Header().Set("Content-Type", "application/json")
			rawRespBodyString = utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, rawRespBodyString, deviceshifubase.PythonScriptDir)
			_, writeErr := io.WriteString(w, rawRespBodyString)
			if writeErr != nil {
				logger.Errorf("Failed to write response %s", rawRespBodyString)
			}
			return
		}

		// TODO: For now, just write tht instruction to the response
		logger.Warnf("resp is nil")
		_, err := w.Write([]byte(handlerInstruction))
		if err != nil {
			logger.Errorf("cannot write instruction into response's body, err: %s", err.Error())
		}
	}
}

// this function gathers the instruction name and its arguments from user input via HTTP and create the direct call command
// "flags_no_parameter" is a special key where it contains all flags
// e.g.:
// if we have localhost:8081/start?time=10:00:00&flags_no_parameter=-a,-c,--no-dependency&target=machine2
// and our driverExecution is "/usr/local/bin/python /usr/src/driver/python-car-driver.py"
// then we will get this command string:
// /usr/local/bin/python /usr/src/driver/python-car-driver.py start time=10:00:00 target=machine2 -a -c --no-dependency
// which is exactly what we need to run if we are operating directly on the device
func createHTTPCommandlineRequestString(r *http.Request, driverExecution string, instruction string) string {
	values := r.URL.Query()
	requestStr := ""
	flagsStr := ""
	for parameterName, parameterValues := range values {
		if parameterName == "flags_no_parameter" {
			if len(parameterValues) == 1 {
				flagsStr = " " + strings.Replace(parameterValues[0], ",", " ", -1)
			} else {
				for _, parameterValue := range parameterValues {
					flagsStr += " " + parameterValue
				}
			}
		} else {
			if len(parameterValues) < 1 {
				continue
			}

			requestStr += " " + parameterName + "="
			for _, parameterValue := range parameterValues {
				requestStr += parameterValue
			}
		}
	}

	if instruction == deviceshifubase.DeviceDefaultCMDDoNotExec {
		return strings.TrimSpace(flagsStr)
	} else if instruction == deviceshifubase.DeviceDefaultCMDStubHealth {
		return "ls"
	}

	return driverExecution + " " + instruction + requestStr + flagsStr
}

// DeviceCommandHandlerHTTPCommandline handler for http commandline
type DeviceCommandHandlerHTTPCommandline struct {
	client                     *rest.RESTClient
	CommandlineHandlerMetadata *CommandlineHandlerMetadata
}

func (handler DeviceCommandHandlerHTTPCommandline) commandHandleFunc() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		driverExecution := handler.CommandlineHandlerMetadata.driverExecution
		handlerProperties := handler.CommandlineHandlerMetadata.properties
		handlerInstruction := handler.CommandlineHandlerMetadata.instruction
		handlerEdgeDeviceSpec := handler.CommandlineHandlerMetadata.edgeDeviceSpec
		handlerHTTPClient := handler.client.Client

		if handlerProperties != nil {
			// TODO: handle validation compile
			for _, instructionProperty := range handlerProperties.DeviceShifuInstructionProperties {
				logger.Infof("Properties of command: %v %v", handlerInstruction, instructionProperty)
			}
		}

		var (
			resp              *http.Response
			httpErr, parseErr error
			ctx               context.Context
			cancel            context.CancelFunc
			timeout                 = *instructionSettings.DefaultTimeoutSeconds
			reqType                 = http.MethodPost // For command line interface, we only use POST
			toleration        int64 = 1
		)

		logger.Infof("handling instruction '%v' to '%v'", handlerInstruction, *handlerEdgeDeviceSpec.Address)
		timeoutStr := r.URL.Query().Get(deviceshifubase.DeviceInstructionTimeoutURIQueryStr)
		if timeoutStr != "" {
			timeout, parseErr = strconv.ParseInt(timeoutStr, 10, 64)
			if parseErr != nil {
				http.Error(w, parseErr.Error(), http.StatusBadRequest)
				logger.Infof("timeout URI parsing error %v", parseErr.Error())
				return
			}
		}

		tolerationStr := r.URL.Query().Get(deviceshifubase.PowerShellStubTimeoutTolerationStr)
		if tolerationStr != "" {
			toleration, parseErr = strconv.ParseInt(tolerationStr, 10, 64)
			if parseErr != nil {
				http.Error(w, parseErr.Error(), http.StatusBadRequest)
				logger.Infof("timeout URI parsing error %v", parseErr.Error())
				return
			}
		}

		commandString := createHTTPCommandlineRequestString(r, driverExecution, handlerInstruction)
		// we are passing the 'cmdTimeout' param to HTTP PowerShell stub to control the execution timeout
		postAddressString := "http://" + *handlerEdgeDeviceSpec.Address + "/?" +
			deviceshifubase.PowerShellStubTimeoutStr + "=" + timeoutStr
		logger.Infof("posting HTTP command line '%v' to '%v'", commandString, postAddressString)

		if timeout <= 0 {
			ctx, cancel = context.WithCancel(context.Background())
		} else {
			// Special handle for HTTP stub since the connection b/w deviceShifu and stub will have some
			// latency, cancelling the PowerShell execution and context at the same time will result in context
			// cancel without an return value
			ctx, cancel = context.WithTimeout(context.Background(), time.Duration(timeout+toleration)*time.Second)
		}

		defer cancel()
		req, reqErr := http.NewRequestWithContext(ctx, reqType, postAddressString, bytes.NewBuffer([]byte(commandString)))
		if reqErr != nil {
			http.Error(w, reqErr.Error(), http.StatusBadRequest)
			logger.Errorf("error creating HTTP request %v", reqErr.Error())
			return
		}

		req.Header.Set("Content-Type", "text/plain")
		resp, httpErr = handlerHTTPClient.Do(req)
		if httpErr != nil {
			http.Error(w, httpErr.Error(), http.StatusServiceUnavailable)
			logger.Errorf("HTTP error, %v", httpErr.Error())
			return
		}

		if resp != nil {
			utils.CopyHeader(w.Header(), resp.Header)
			w.WriteHeader(resp.StatusCode)

			respBody, readErr := io.ReadAll(resp.Body)
			if readErr != nil {
				logger.Errorf("error when read requestBody from responseBody, err: %v", readErr)
			}

			rawRespBodyString := string(respBody)
			instructionFuncName, shouldUsePythonCustomProcessing := deviceshifubase.CustomInstructionsPython[handlerInstruction]
			respBodyString := rawRespBodyString
			logger.Infof("Instruction %v is custom: %v", handlerInstruction, shouldUsePythonCustomProcessing)
			if shouldUsePythonCustomProcessing {
				logger.Infof("Instruction %v has a python customized handler configured.\n", handlerInstruction)
				respBodyString = utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, rawRespBodyString, deviceshifubase.PythonScriptDir)
			}
			_, writeErr := io.WriteString(w, respBodyString)
			if writeErr != nil {
				logger.Errorf("Failed to write response %v", respBodyString)
			}
			return
		}

		// TODO: For now, if response is nil without error, just write the instruction to the response
		logger.Warnf("resp is nil")
		_, err := w.Write([]byte(handlerInstruction))
		if err != nil {
			logger.Errorf("cannot write instruction into responseBody")
		}
	}
}

// TODO: update configs

func (ds *DeviceShifuHTTP) collectHTTPTelemtries() (bool, error) {
	telemetryCollectionResult := false
	if ds.base.EdgeDevice.Spec.Protocol != nil {
		switch protocol := *ds.base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolHTTP, v1alpha1.ProtocolHTTPCommandline:
			telemetries := ds.base.DeviceShifuConfig.Telemetries.DeviceShifuTelemetries
			deviceName := ds.base.EdgeDevice.Name
			for telemetry, telemetryProperties := range telemetries {
				if ds.base.EdgeDevice.Spec.Address == nil {
					return false, fmt.Errorf("Device %v does not have an address", ds.base.Name)
				}

				if telemetryProperties.DeviceShifuTelemetryProperties.DeviceInstructionName == nil {
					return false, fmt.Errorf("Device %v telemetry %v does not have an instruction name", ds.base.Name, telemetry)
				}

				var (
					ctx     context.Context
					cancel  context.CancelFunc
					timeout = *ds.base.DeviceShifuConfig.Telemetries.DeviceShifuTelemetrySettings.DeviceShifuTelemetryTimeoutInMilliseconds
				)

				if timeout == 0 {
					ctx, cancel = context.WithCancel(context.TODO())
				} else {
					ctx, cancel = context.WithTimeout(context.TODO(), time.Duration(timeout)*time.Millisecond)
				}

				defer cancel()
				address := *ds.base.EdgeDevice.Spec.Address
				instruction := *telemetryProperties.DeviceShifuTelemetryProperties.DeviceInstructionName
				req, ReqErr := http.NewRequestWithContext(ctx, http.MethodGet, "http://"+address+"/"+instruction, nil)
				if ReqErr != nil {
					logger.Errorf("error checking telemetry: %v, error: %v", telemetry, ReqErr.Error())
					return false, ReqErr
				}

				resp, err := ds.base.RestClient.Client.Do(req)
				if err != nil {
					logger.Errorf("error checking telemetry: %v, error: %v", telemetry, err.Error())
					return false, err
				}

				if resp != nil {
					if resp.StatusCode >= http.StatusOK && resp.StatusCode < http.StatusMultipleChoices {
						instructionFuncName, pythonCustomExist := deviceshifubase.CustomInstructionsPython[instruction]
						if pythonCustomExist {
							respBody, readErr := io.ReadAll(resp.Body)
							if readErr != nil {
								logger.Errorf("error when read requestBody from responseBody, err: %v", readErr)
							}

							rawRespBodyString := string(respBody)
							logger.Infof("Instruction %v is custom: %v, has a python customized handler configured.\n", instruction, pythonCustomExist)
							respBodyString := utils.ProcessInstruction(deviceshifubase.PythonHandlersModuleName, instructionFuncName, rawRespBodyString, deviceshifubase.PythonScriptDir)
							resp = &http.Response{
								Header: make(http.Header),
								Body:   io.NopCloser(strings.NewReader(respBodyString)),
							}
						}

						telemetryCollectionService, exist := deviceshifubase.TelemetryCollectionServiceMap[telemetry]
						if exist && *telemetryCollectionService.TelemetrySeriveEndpoint != "" {
							resp.Header.Add(DeviceNameHeaderField, deviceName)
							err = deviceshifubase.PushTelemetryCollectionService(&telemetryCollectionService, resp)
							if err != nil {
								return false, err
							}
						}

						telemetryCollectionResult = true
						continue
					}
				}

				return false, nil
			}
		default:
			logger.Warnf("EdgeDevice protocol %v not supported in deviceshifu_http_http", protocol)
			return false, nil
		}
	}

	return telemetryCollectionResult, nil
}

// Start start http telemetry
func (ds *DeviceShifuHTTP) Start(stopCh <-chan struct{}) error {
	return ds.base.Start(stopCh, ds.collectHTTPTelemtries)
}

// Stop stop http server
func (ds *DeviceShifuHTTP) Stop() error {
	return ds.base.Stop()
}
