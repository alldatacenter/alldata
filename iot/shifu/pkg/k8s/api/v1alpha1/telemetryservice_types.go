/*
Copyright 2021.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// telemetry Service handler
const (
	TelemetryServiceURIMQTT  = "/mqtt"
	TelemetryServiceURISQL   = "/sql"
	TelemetryServiceURIMinIO = "/minio"
)

// HTTPSetting defines HTTP specific settings when connecting to an EdgeDevice
type HTTPSetting struct {
	Username *string `json:"username,omitempty"`
	Password *string `json:"password,omitempty"`
}

type MinIOSetting struct {
	Secret           *string `json:"Secret,omitempty"`
	AccessKey        *string `json:"AccessKey,omitempty"`
	SecretKey        *string `json:"SecretKey,omitempty"`
	RequestTimeoutMS *int64  `json:"RequestTimeoutMS,omitempty"`
	//+kubebuilder:validation:Required
	Bucket *string `json:"Bucket,omitempty"`
	//+kubebuilder:validation:Required
	FileExtension *string `json:"FileExtension,omitempty"`
	//+kubebuilder:validation:Required
	ServerAddress *string `json:"ServerAddress,omitempty"`
}

// ServiceSettings defines service settings on telemetry
type ServiceSettings struct {
	HTTPSetting  *HTTPSetting          `json:"HTTPSetting,omitempty"`
	MQTTSetting  *MQTTSetting          `json:"MQTTSetting,omitempty"`
	SQLSetting   *SQLConnectionSetting `json:"SQLSetting,omitempty"`
	MinIOSetting *MinIOSetting         `json:"MinIOSetting,omitempty"`
}

// TelemetryServiceSpec defines the desired state of TelemetryService
type TelemetryServiceSpec struct {
	// INSERT ADDITIONAL SPEC FIELDS - desired state of cluster
	// Important: Run "make" to regenerate code after modifying this file

	TelemetrySeriveEndpoint *string            `json:"telemetrySeriveEndpoint,omitempty"`
	ServiceSettings         *ServiceSettings   `json:"serviceSettings,omitempty"`
	CustomMetadata          *map[string]string `json:"customMetadata,omitempty"`
}

type Type string

type TelemetryRequest struct {
	RawData              []byte                `json:"rawData,omitempty"`
	MQTTSetting          *MQTTSetting          `json:"mqttSetting,omitempty"`
	SQLConnectionSetting *SQLConnectionSetting `json:"sqlConnectionSetting,omitempty"`
	MinIOSetting         *MinIOSetting         `json:"minIOSetting,omitempty"`
}

// TelemetryServiceStatus defines the observed state of TelemetryService
type TelemetryServiceStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file
	TelemetryServicePhase *EdgeDevicePhase `json:"telemetryservicephase,omitempty"`
}

//+kubebuilder:object:root=true
//+kubebuilder:subresource:status

// TelemetryService is the Schema for the telemetryservices API
type TelemetryService struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   TelemetryServiceSpec   `json:"spec,omitempty"`
	Status TelemetryServiceStatus `json:"status,omitempty"`
}

//+kubebuilder:object:root=true

// TelemetryServiceList contains a list of TelemetryService
type TelemetryServiceList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []TelemetryService `json:"items"`
}

func init() {
	SchemeBuilder.Register(&TelemetryService{}, &TelemetryServiceList{})
}

type SQLConnectionSetting struct {
	// +kubebuilder:validation:Required
	ServerAddress *string `json:"serverAddress,omitempty"`
	UserName      *string `json:"username,omitempty"`
	Secret        *string `json:"secret,omitempty"`
	DBName        *string `json:"dbName,omitempty"`
	DBTable       *string `json:"dbTable,omitempty"`
	DBType        *DBType `json:"dbtype,omitempty"`
}

type DBType string

const (
	DBTypeTDengine DBType = "TDengine"
)
