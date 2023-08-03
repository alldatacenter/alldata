package minio

import (
	"bytes"
	"encoding/json"
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifuhttp"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/telemetryservice/utils"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/stretchr/testify/assert"
	"io"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	testclient "k8s.io/client-go/kubernetes/fake"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestBindMinIOServiceHandler(t *testing.T) {
	testCases := []struct {
		name        string
		requestBody *v1alpha1.TelemetryRequest
		expectResp  string
		deviceName  string
	}{
		{
			name:       "testCase1 RequestBody is not a JSON",
			expectResp: "Unexpected end of JSON input\n",
		},
		{
			name:        "testCase2 MinIOSetting is nil",
			expectResp:  "MinIOSetting cant be nil\n",
			requestBody: &v1alpha1.TelemetryRequest{},
		},
		{
			name:       "testCase3 missing parameter",
			expectResp: "Bucket or EndPoint or FileExtension cant be nil\n",
			requestBody: &v1alpha1.TelemetryRequest{
				MinIOSetting: &v1alpha1.MinIOSetting{
					Bucket: unitest.ToPointer("test-bucket"),
				},
			},
		},
		{
			name:       "testCase4 no secret",
			expectResp: "Fail to get AccessKey/username or SecretKey/password\n",
			requestBody: &v1alpha1.TelemetryRequest{
				MinIOSetting: &v1alpha1.MinIOSetting{
					Bucket:        unitest.ToPointer("test-bucket"),
					ServerAddress: unitest.ToPointer("test-end-point"),
					FileExtension: unitest.ToPointer("test-extension"),
				},
				RawData: []byte("test"),
			},
		},
		{
			name:       "testCase5 no device name",
			expectResp: "Fail to get device name from header\n",
			requestBody: &v1alpha1.TelemetryRequest{
				MinIOSetting: &v1alpha1.MinIOSetting{
					Bucket:        unitest.ToPointer("test-bucket"),
					ServerAddress: unitest.ToPointer("test-end-point"),
					FileExtension: unitest.ToPointer("test-extension"),
					AccessKey:     unitest.ToPointer("AccessKey"),
					SecretKey:     unitest.ToPointer("SecretKey"),
				},
				RawData: []byte("test"),
			},
		},
		{
			name:       "testCase6 with device name",
			expectResp: "upload object fail\n",
			requestBody: &v1alpha1.TelemetryRequest{
				MinIOSetting: &v1alpha1.MinIOSetting{
					Bucket:        unitest.ToPointer("test-bucket"),
					ServerAddress: unitest.ToPointer("test-end-point"),
					FileExtension: unitest.ToPointer("test-extension"),
					AccessKey:     unitest.ToPointer("AccessKey"),
					SecretKey:     unitest.ToPointer("SecretKey"),
				},
				RawData: []byte("test"),
			},
			deviceName: "test-device",
		},
	}
	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "test:123", bytes.NewBuffer([]byte("")))
			if testCase.requestBody != nil {
				requestBody, err := json.Marshal(testCase.requestBody)
				assert.Nil(t, err)
				req = httptest.NewRequest(http.MethodPost, "test:123", bytes.NewBuffer(requestBody))
			}
			if testCase.deviceName != "" {
				req.Header = map[string][]string{
					deviceshifuhttp.DeviceNameHeaderField: {testCase.deviceName},
				}
			}
			recoder := httptest.NewRecorder()
			BindMinIOServiceHandler(recoder, req)
			body, err := io.ReadAll(recoder.Result().Body)
			assert.Nil(t, err)
			assert.Equal(t, testCase.expectResp, string(body))
		})
	}
}

func TestInjectSecret(t *testing.T) {
	testNamespace := "test-namespace"
	testCases := []struct {
		name            string
		client          *testclient.Clientset
		ns              string
		setting         *v1alpha1.MinIOSetting
		expectAccessKey *string
		expectSecretKey *string
	}{
		{
			name:    "case1 no secret",
			client:  testclient.NewSimpleClientset(),
			ns:      testNamespace,
			setting: &v1alpha1.MinIOSetting{},
		},
		{
			name:   "case2 no secrets found",
			client: testclient.NewSimpleClientset(),
			ns:     testNamespace,
			setting: &v1alpha1.MinIOSetting{
				Secret: unitest.ToPointer("test-secret"),
			},
		},
		{
			name: "case3 with secret but no data",
			client: testclient.NewSimpleClientset(&v1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test-secret",
					Namespace: testNamespace,
				},
				Data: map[string][]byte{},
			}),
			ns: testNamespace,
			setting: &v1alpha1.MinIOSetting{
				Secret: unitest.ToPointer("test-secret"),
			},
		},
		{
			name: "case4 have secretId",
			client: testclient.NewSimpleClientset(&v1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test-secret",
					Namespace: testNamespace,
				},
				Data: map[string][]byte{
					"username": []byte("overwrite"),
				},
			}),
			ns: testNamespace,
			setting: &v1alpha1.MinIOSetting{
				Secret: unitest.ToPointer("test-secret"),
			},
			expectAccessKey: unitest.ToPointer("overwrite"),
		},
		{
			name: "case5 have id and key",
			client: testclient.NewSimpleClientset(&v1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test-secret",
					Namespace: testNamespace,
				},
				Data: map[string][]byte{
					"username": []byte("overwrite"),
					"password": []byte("overwrite"),
				},
			}),
			ns: testNamespace,
			setting: &v1alpha1.MinIOSetting{
				Secret: unitest.ToPointer("test-secret"),
			},
			expectAccessKey: unitest.ToPointer("overwrite"),
			expectSecretKey: unitest.ToPointer("overwrite"),
		},
	}

	for _, c := range testCases {
		utils.SetClient(c.client, c.ns)
		injectSecret(c.setting)
		if c.setting != nil {
			assert.Equal(t, c.expectAccessKey, c.setting.AccessKey)
			assert.Equal(t, c.expectSecretKey, c.setting.SecretKey)
		}
	}
}

func TestUploadObject(t *testing.T) {
	client, err := minio.New("test-end-point", &minio.Options{
		Creds: credentials.NewStaticV4("test-api-id", "test-api-key", ""),
	})
	if err != nil {
		t.Error("Fail to create test client")
	}
	err = uploadObject(client, "test-bucket", "test-file-name", []byte{})
	assert.Equal(t, "upload object fail", err.Error())
}
