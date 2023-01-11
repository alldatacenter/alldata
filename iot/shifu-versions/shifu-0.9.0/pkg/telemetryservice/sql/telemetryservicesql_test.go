package sql

import (
	"bytes"
	"encoding/json"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/telemetryservice/utils"
	"github.com/stretchr/testify/assert"
	"io"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	testclient "k8s.io/client-go/kubernetes/fake"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestBindSQLServiceHandler(t *testing.T) {
	testCases := []struct {
		desc        string
		requestBody *v1alpha1.TelemetryRequest
		expectResp  string
	}{
		{
			desc:       "testCase1 RequestBody is not a JSON",
			expectResp: "unexpected end of JSON input\n",
		},
		{
			desc:       "testCase2 wrong db type",
			expectResp: "Error to send to server\n",
			requestBody: &v1alpha1.TelemetryRequest{
				SQLConnectionSetting: &v1alpha1.SQLConnectionSetting{
					DBType: unitest.ToPointer(v1alpha1.DBType("wrongType")),
				},
			},
		},
		{
			desc:       "testCase3 db Type TDengine",
			expectResp: "Error to send to server\n",
			requestBody: &v1alpha1.TelemetryRequest{
				SQLConnectionSetting: &v1alpha1.SQLConnectionSetting{
					ServerAddress: unitest.ToPointer("testAddr"),
					DBType:        unitest.ToPointer(v1alpha1.DBTypeTDengine),
					UserName:      unitest.ToPointer("test"),
					Secret:        unitest.ToPointer("test"),
					DBName:        unitest.ToPointer("test"),
					DBTable:       unitest.ToPointer("test"),
				},
				RawData: []byte("test"),
			},
		},
	}
	for _, tC := range testCases {
		t.Run(tC.desc, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "test:80", bytes.NewBuffer([]byte("")))
			if tC.requestBody != nil {
				requestBody, err := json.Marshal(tC.requestBody)
				assert.Nil(t, err)
				req = httptest.NewRequest(http.MethodPost, "test:80", bytes.NewBuffer(requestBody))
			}

			rr := httptest.NewRecorder()
			BindSQLServiceHandler(rr, req)
			body, err := io.ReadAll(rr.Result().Body)
			assert.Nil(t, err)
			assert.Equal(t, tC.expectResp, string(body))

		})
	}

}

func TestInjectSecret(t *testing.T) {
	testNamespace := "test-namespace"
	testCases := []struct {
		name         string
		client       *testclient.Clientset
		ns           string
		setting      *v1alpha1.SQLConnectionSetting
		specUsername string
		specPassword string
	}{
		{
			name:   "case0 no secrets found",
			client: testclient.NewSimpleClientset(),
			ns:     testNamespace,
			setting: &v1alpha1.SQLConnectionSetting{
				UserName: unitest.ToPointer("origin-username"),
				Secret:   unitest.ToPointer("test-secret"),
			},
			specUsername: "origin-username",
			specPassword: "test-secret",
		},
		{
			name: "case1 have HTTP username secret",
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
			setting: &v1alpha1.SQLConnectionSetting{
				UserName: unitest.ToPointer("origin-username"),
				Secret:   unitest.ToPointer("test-secret"),
			},
			specUsername: "overwrite",
			specPassword: "test-secret",
		},
		{
			name: "case2 have HTTP password secret",
			client: testclient.NewSimpleClientset(&v1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test-secret",
					Namespace: testNamespace,
				},
				Data: map[string][]byte{
					"password": []byte("overwrite"),
				},
			}),
			ns: testNamespace,
			setting: &v1alpha1.SQLConnectionSetting{
				UserName: unitest.ToPointer("origin-username"),
				Secret:   unitest.ToPointer("test-secret"),
			},
			specUsername: "origin-username",
			specPassword: "overwrite",
		},
		{
			name: "case3 have HTTP both secrets",
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
			setting: &v1alpha1.SQLConnectionSetting{
				UserName: unitest.ToPointer("origin-username"),
				Secret:   unitest.ToPointer("test-secret"),
			},
			specUsername: "overwrite",
			specPassword: "overwrite",
		},
	}

	for _, c := range testCases {
		utils.SetClient(c.client, c.ns)
		injectSecret(c.setting)
		assert.Equal(t, c.specUsername, *c.setting.UserName)
		assert.Equal(t, c.specPassword, *c.setting.Secret)
	}
}
