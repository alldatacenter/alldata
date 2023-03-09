module github.com/koordinator-sh/koordinator

go 1.17

require (
	github.com/NVIDIA/go-nvml v0.11.6-0.0.20220823120812-7e2082095e82
	github.com/cakturk/go-netstat v0.0.0-20200220111822-e5b49efee7a5
	github.com/docker/docker v20.10.21+incompatible
	github.com/evanphx/json-patch v5.6.0+incompatible
	github.com/fsnotify/fsnotify v1.6.0
	github.com/gin-gonic/gin v1.8.1
	github.com/golang/groupcache v0.0.0-20210331224755-41bb18bfe9da
	github.com/golang/mock v1.6.0
	github.com/golang/protobuf v1.5.2
	github.com/google/go-cmp v0.5.9
	github.com/google/uuid v1.3.0
	github.com/hodgesds/perf-utils v0.5.1
	github.com/jedib0t/go-pretty/v6 v6.4.0
	github.com/k8stopologyawareschedwg/noderesourcetopology-api v0.0.12
	github.com/mohae/deepcopy v0.0.0-20170603005431-491d3605edfb
	github.com/onsi/ginkgo v1.16.4
	github.com/onsi/gomega v1.15.0
	github.com/openkruise/kruise-api v1.3.0
	github.com/patrickmn/go-cache v2.1.0+incompatible
	github.com/prashantv/gostub v1.1.0
	github.com/prometheus/client_golang v1.13.0
	github.com/spf13/cobra v1.6.1
	github.com/spf13/pflag v1.0.5
	github.com/stretchr/testify v1.8.1
	go.uber.org/atomic v1.10.0
	go.uber.org/multierr v1.6.0
	golang.org/x/crypto v0.0.0-20210711020723-a769d52b0f97
	golang.org/x/net v0.0.0-20220225172249-27dd8689420f
	golang.org/x/sys v0.0.0-20220908164124-27713097b956
	golang.org/x/time v0.0.0-20210723032227-1f47c861a9ac
	google.golang.org/grpc v1.38.0
	google.golang.org/protobuf v1.28.1
	gopkg.in/yaml.v2 v2.4.0
	gorm.io/driver/sqlite v1.3.6
	gorm.io/gorm v1.23.10
	k8s.io/api v0.22.6
	k8s.io/apimachinery v0.22.6
	k8s.io/apiserver v0.22.6
	k8s.io/client-go v0.22.6
	k8s.io/code-generator v0.22.6
	k8s.io/component-base v0.22.6
	k8s.io/component-helpers v0.22.6
	k8s.io/cri-api v0.22.6
	k8s.io/klog/v2 v2.10.0
	k8s.io/kube-scheduler v0.22.6
	k8s.io/kubectl v0.22.6
	k8s.io/kubelet v0.22.6
	k8s.io/kubernetes v1.22.6
	k8s.io/utils v0.0.0-20210819203725-bdf08cb9a70a
	sigs.k8s.io/controller-runtime v0.10.3
	sigs.k8s.io/scheduler-plugins v0.22.6
	sigs.k8s.io/yaml v1.3.0
)

require (
	cloud.google.com/go v0.65.0 // indirect
	github.com/Azure/azure-sdk-for-go v55.0.0+incompatible // indirect
	github.com/Azure/go-ansiterm v0.0.0-20210617225240-d185dfc1b5a1 // indirect
	github.com/Azure/go-autorest v14.2.0+incompatible // indirect
	github.com/Azure/go-autorest/autorest v0.11.18 // indirect
	github.com/Azure/go-autorest/autorest/adal v0.9.13 // indirect
	github.com/Azure/go-autorest/autorest/date v0.3.0 // indirect
	github.com/Azure/go-autorest/autorest/mocks v0.4.1 // indirect
	github.com/Azure/go-autorest/autorest/to v0.4.0 // indirect
	github.com/Azure/go-autorest/autorest/validation v0.1.0 // indirect
	github.com/Azure/go-autorest/logger v0.2.1 // indirect
	github.com/Azure/go-autorest/tracing v0.6.0 // indirect
	github.com/GoogleCloudPlatform/k8s-cloud-provider v0.0.0-20200415212048-7901bc822317 // indirect
	github.com/JeffAshton/win_pdh v0.0.0-20161109143554-76bb4ee9f0ab // indirect
	github.com/Microsoft/go-winio v0.4.17 // indirect
	github.com/Microsoft/hcsshim v0.8.23 // indirect
	github.com/NYTimes/gziphandler v1.1.1 // indirect
	github.com/PuerkitoBio/purell v1.1.1 // indirect
	github.com/PuerkitoBio/urlesc v0.0.0-20170810143723-de5bf2ad4578 // indirect
	github.com/armon/circbuf v0.0.0-20150827004946-bbbad097214e // indirect
	github.com/asaskevich/govalidator v0.0.0-20190424111038-f61b66f89f4a // indirect
	github.com/aws/aws-sdk-go v1.38.49 // indirect
	github.com/beorn7/perks v1.0.1 // indirect
	github.com/bits-and-blooms/bitset v1.2.0 // indirect
	github.com/blang/semver v3.5.1+incompatible // indirect
	github.com/cespare/xxhash/v2 v2.1.2 // indirect
	github.com/checkpoint-restore/go-criu/v5 v5.0.0 // indirect
	github.com/cilium/ebpf v0.6.2 // indirect
	github.com/clusterhq/flocker-go v0.0.0-20160920122132-2b8b7259d313 // indirect
	github.com/container-storage-interface/spec v1.5.0 // indirect
	github.com/containerd/cgroups v1.0.1 // indirect
	github.com/containerd/console v1.0.2 // indirect
	github.com/containerd/containerd v1.5.10 // indirect
	github.com/containerd/ttrpc v1.1.0 // indirect
	github.com/containernetworking/cni v0.8.1 // indirect
	github.com/coreos/go-semver v0.3.0 // indirect
	github.com/coreos/go-systemd/v22 v22.3.2 // indirect
	github.com/cyphar/filepath-securejoin v0.2.2 // indirect
	github.com/davecgh/go-spew v1.1.1 // indirect
	github.com/docker/distribution v2.8.1+incompatible // indirect
	github.com/docker/go-connections v0.4.0 // indirect
	github.com/docker/go-units v0.4.0 // indirect
	github.com/emicklei/go-restful v2.9.5+incompatible // indirect
	github.com/euank/go-kmsg-parser v2.0.0+incompatible // indirect
	github.com/felixge/httpsnoop v1.0.1 // indirect
	github.com/form3tech-oss/jwt-go v3.2.3+incompatible // indirect
	github.com/gin-contrib/sse v0.1.0 // indirect
	github.com/go-logr/logr v0.4.0 // indirect
	github.com/go-openapi/jsonpointer v0.19.5 // indirect
	github.com/go-openapi/jsonreference v0.19.6 // indirect
	github.com/go-openapi/swag v0.21.1 // indirect
	github.com/go-ozzo/ozzo-validation v3.5.0+incompatible // indirect
	github.com/go-playground/locales v0.14.0 // indirect
	github.com/go-playground/universal-translator v0.18.0 // indirect
	github.com/go-playground/validator/v10 v10.10.0 // indirect
	github.com/goccy/go-json v0.9.7 // indirect
	github.com/godbus/dbus/v5 v5.0.4 // indirect
	github.com/gofrs/uuid v4.0.0+incompatible // indirect
	github.com/gogo/protobuf v1.3.2 // indirect
	github.com/google/cadvisor v0.39.3 // indirect
	github.com/google/gofuzz v1.1.0 // indirect
	github.com/googleapis/gax-go/v2 v2.0.5 // indirect
	github.com/googleapis/gnostic v0.5.5 // indirect
	github.com/gophercloud/gophercloud v0.1.0 // indirect
	github.com/grpc-ecosystem/go-grpc-prometheus v1.2.0 // indirect
	github.com/grpc-ecosystem/grpc-gateway v1.16.0 // indirect
	github.com/heketi/heketi v10.3.0+incompatible // indirect
	github.com/imdario/mergo v0.3.12 // indirect
	github.com/inconshreveable/mousetrap v1.0.1 // indirect
	github.com/jinzhu/inflection v1.0.0 // indirect
	github.com/jinzhu/now v1.1.5 // indirect
	github.com/jmespath/go-jmespath v0.4.0 // indirect
	github.com/josharian/intern v1.0.0 // indirect
	github.com/json-iterator/go v1.1.12 // indirect
	github.com/karrick/godirwalk v1.16.1 // indirect
	github.com/leodido/go-urn v1.2.1 // indirect
	github.com/libopenstorage/openstorage v1.0.0 // indirect
	github.com/lithammer/dedent v1.1.0 // indirect
	github.com/mailru/easyjson v0.7.6 // indirect
	github.com/mattn/go-isatty v0.0.14 // indirect
	github.com/mattn/go-runewidth v0.0.13 // indirect
	github.com/mattn/go-sqlite3 v2.0.3+incompatible // indirect
	github.com/matttproud/golang_protobuf_extensions v1.0.2-0.20181231171920-c182affec369 // indirect
	github.com/mistifyio/go-zfs v2.1.2-0.20190413222219-f784269be439+incompatible // indirect
	github.com/mitchellh/mapstructure v1.1.2 // indirect
	github.com/moby/ipvs v1.0.1 // indirect
	github.com/moby/spdystream v0.2.0 // indirect
	github.com/moby/sys/mountinfo v0.4.1 // indirect
	github.com/moby/term v0.0.0-20210610120745-9d4ed1856297 // indirect
	github.com/modern-go/concurrent v0.0.0-20180306012644-bacd9c7ef1dd // indirect
	github.com/modern-go/reflect2 v1.0.2 // indirect
	github.com/morikuni/aec v1.0.0 // indirect
	github.com/mrunalp/fileutils v0.5.0 // indirect
	github.com/munnerz/goautoneg v0.0.0-20191010083416-a7dc8b61c822 // indirect
	github.com/mxk/go-flowrate v0.0.0-20140419014527-cca7078d478f // indirect
	github.com/nxadm/tail v1.4.8 // indirect
	github.com/opencontainers/go-digest v1.0.0 // indirect
	github.com/opencontainers/image-spec v1.0.2 // indirect
	github.com/opencontainers/runc v1.0.2 // indirect
	github.com/opencontainers/runtime-spec v1.0.3-0.20210326190908-1c3f411f0417 // indirect
	github.com/opencontainers/selinux v1.8.2 // indirect
	github.com/pelletier/go-toml/v2 v2.0.1 // indirect
	github.com/pkg/errors v0.9.1 // indirect
	github.com/pmezard/go-difflib v1.0.0 // indirect
	github.com/prometheus/client_model v0.2.0 // indirect
	github.com/prometheus/common v0.37.0 // indirect
	github.com/prometheus/procfs v0.8.0 // indirect
	github.com/quobyte/api v0.1.8 // indirect
	github.com/rivo/uniseg v0.2.0 // indirect
	github.com/rubiojr/go-vhd v0.0.0-20200706105327-02e210299021 // indirect
	github.com/seccomp/libseccomp-golang v0.9.1 // indirect
	github.com/sirupsen/logrus v1.8.1 // indirect
	github.com/storageos/go-api v2.2.0+incompatible // indirect
	github.com/stretchr/objx v0.5.0 // indirect
	github.com/syndtr/gocapability v0.0.0-20200815063812-42c35b437635 // indirect
	github.com/ugorji/go/codec v1.2.7 // indirect
	github.com/vishvananda/netlink v1.1.1-0.20201029203352-d40f9887b852 // indirect
	github.com/vishvananda/netns v0.0.0-20200728191858-db3c7e526aae // indirect
	github.com/vmware/govmomi v0.20.3 // indirect
	go.etcd.io/etcd/api/v3 v3.5.0 // indirect
	go.etcd.io/etcd/client/pkg/v3 v3.5.0 // indirect
	go.etcd.io/etcd/client/v3 v3.5.0 // indirect
	go.opencensus.io v0.22.4 // indirect
	go.opentelemetry.io/contrib v0.20.0 // indirect
	go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc v0.20.0 // indirect
	go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp v0.20.0 // indirect
	go.opentelemetry.io/otel v0.20.0 // indirect
	go.opentelemetry.io/otel/exporters/otlp v0.20.0 // indirect
	go.opentelemetry.io/otel/metric v0.20.0 // indirect
	go.opentelemetry.io/otel/sdk v0.20.0 // indirect
	go.opentelemetry.io/otel/sdk/export/metric v0.20.0 // indirect
	go.opentelemetry.io/otel/sdk/metric v0.20.0 // indirect
	go.opentelemetry.io/otel/trace v0.20.0 // indirect
	go.opentelemetry.io/proto/otlp v0.7.0 // indirect
	go.uber.org/zap v1.19.0 // indirect
	golang.org/x/mod v0.4.2 // indirect
	golang.org/x/oauth2 v0.0.0-20220223155221-ee480838109b // indirect
	golang.org/x/sync v0.0.0-20220601150217-0de741cfad7f // indirect
	golang.org/x/term v0.0.0-20210927222741-03fcf44c2211 // indirect
	golang.org/x/text v0.3.7 // indirect
	golang.org/x/tools v0.1.2 // indirect
	golang.org/x/xerrors v0.0.0-20200804184101-5ec99f83aff1 // indirect
	gomodules.xyz/jsonpatch/v2 v2.2.0 // indirect
	google.golang.org/api v0.30.0 // indirect
	google.golang.org/appengine v1.6.7 // indirect
	google.golang.org/genproto v0.0.0-20210602131652-f16073e35f0c // indirect
	gopkg.in/gcfg.v1 v1.2.0 // indirect
	gopkg.in/inf.v0 v0.9.1 // indirect
	gopkg.in/natefinch/lumberjack.v2 v2.0.0 // indirect
	gopkg.in/tomb.v1 v1.0.0-20141024135613-dd632973f1e7 // indirect
	gopkg.in/warnings.v0 v0.1.1 // indirect
	gopkg.in/yaml.v3 v3.0.1 // indirect
	k8s.io/apiextensions-apiserver v0.22.6 // indirect
	k8s.io/cloud-provider v0.22.6 // indirect
	k8s.io/csi-translation-lib v0.22.6 // indirect
	k8s.io/gengo v0.0.0-20201214224949-b6c5ce23f027 // indirect
	k8s.io/kube-openapi v0.0.0-20211109043538-20434351676c // indirect
	k8s.io/kube-proxy v0.0.0 // indirect
	k8s.io/legacy-cloud-providers v0.0.0 // indirect
	k8s.io/mount-utils v0.22.6 // indirect
	sigs.k8s.io/apiserver-network-proxy/konnectivity-client v0.0.27 // indirect
	sigs.k8s.io/structured-merge-diff/v4 v4.2.1 // indirect
)

replace (
	github.com/google/cadvisor => github.com/koordinator-sh/cadvisor v0.0.0-20220919031936-833eb74e858e
	golang.org/x/time => golang.org/x/time v0.3.0
	k8s.io/api => k8s.io/api v0.22.6
	k8s.io/apiextensions-apiserver => k8s.io/apiextensions-apiserver v0.22.6
	k8s.io/apimachinery => k8s.io/apimachinery v0.22.6
	k8s.io/apiserver => k8s.io/apiserver v0.22.6
	k8s.io/cli-runtime => k8s.io/cli-runtime v0.22.6
	k8s.io/client-go => k8s.io/client-go v0.22.6
	k8s.io/cloud-provider => k8s.io/cloud-provider v0.22.6
	k8s.io/cluster-bootstrap => k8s.io/cluster-bootstrap v0.22.6
	k8s.io/code-generator => k8s.io/code-generator v0.22.6
	k8s.io/component-base => k8s.io/component-base v0.22.6
	k8s.io/component-helpers => k8s.io/component-helpers v0.22.6
	k8s.io/controller-manager => k8s.io/controller-manager v0.22.6
	k8s.io/cri-api => k8s.io/cri-api v0.22.6
	k8s.io/csi-translation-lib => k8s.io/csi-translation-lib v0.22.6
	k8s.io/kube-aggregator => k8s.io/kube-aggregator v0.22.6
	k8s.io/kube-controller-manager => k8s.io/kube-controller-manager v0.22.6
	k8s.io/kube-proxy => k8s.io/kube-proxy v0.22.6
	k8s.io/kube-scheduler => k8s.io/kube-scheduler v0.22.6
	k8s.io/kubectl => k8s.io/kubectl v0.22.6
	k8s.io/kubelet => k8s.io/kubelet v0.22.6
	k8s.io/kubernetes => k8s.io/kubernetes v1.22.6
	k8s.io/legacy-cloud-providers => k8s.io/legacy-cloud-providers v0.22.6
	k8s.io/metrics => k8s.io/metrics v0.22.6
	k8s.io/mount-utils => k8s.io/mount-utils v0.22.6
	k8s.io/pod-security-admission => k8s.io/pod-security-admission v0.22.6
	k8s.io/sample-apiserver => k8s.io/sample-apiserver v0.22.6
	sigs.k8s.io/scheduler-plugins => sigs.k8s.io/scheduler-plugins v0.22.6
)
