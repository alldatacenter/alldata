import os
import sys

sys.path.insert(1, os.path.join(sys.path[0], "../../helpers"))
from helpers import helm_template
import yaml

name = "release-name-kibana"
elasticsearchHosts = "http://elasticsearch-master:9200"


def test_defaults():
    config = """
    """

    r = helm_template(config)

    assert name in r["deployment"]
    assert name in r["service"]

    s = r["service"][name]["spec"]
    assert s["ports"][0]["port"] == 5601
    assert s["ports"][0]["name"] == "http"
    assert s["ports"][0]["protocol"] == "TCP"
    assert s["ports"][0]["targetPort"] == 5601

    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["name"] == "kibana"
    assert c["image"].startswith("docker.elastic.co/kibana/kibana:")
    assert c["ports"][0]["containerPort"] == 5601

    assert c["env"][0]["name"] == "ELASTICSEARCH_HOSTS"
    assert c["env"][0]["value"] == elasticsearchHosts

    assert c["env"][1]["name"] == "SERVER_HOST"
    assert c["env"][1]["value"] == "0.0.0.0"

    assert 'http "/app/kibana"' in c["readinessProbe"]["exec"]["command"][-1]

    # Empty customizable defaults
    assert "imagePullSecrets" not in r["deployment"][name]["spec"]["template"]["spec"]
    assert "tolerations" not in r["deployment"][name]["spec"]["template"]["spec"]
    assert "nodeSelector" not in r["deployment"][name]["spec"]["template"]["spec"]
    assert "ingress" not in r

    assert r["deployment"][name]["spec"]["strategy"]["type"] == "Recreate"

    # Make sure that the default 'annotation' dictionary is empty
    assert "annotations" not in r["service"][name]["metadata"]

    # Make sure that the default 'loadBalancerSourceRanges' list is empty
    assert "loadBalancerSourceRanges" not in r["service"][name]["spec"]

    # Make sure that the default 'loadBalancerIP' string is empty
    assert "loadBalancerIP" not in r["service"][name]["spec"]

    assert "hostAliases" not in r["deployment"][name]["spec"]["template"]["spec"]


def test_overriding_the_elasticsearch_hosts():
    config = """
    elasticsearchHosts: 'http://hello.world'
    """

    r = helm_template(config)

    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["env"][0]["name"] == "ELASTICSEARCH_HOSTS"
    assert c["env"][0]["value"] == "http://hello.world"


def test_overriding_the_elasticsearch_url():
    config = """
    elasticsearchURL: 'http://hello.world'
    """

    r = helm_template(config)

    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["env"][0]["name"] == "ELASTICSEARCH_URL"
    assert c["env"][0]["value"] == "http://hello.world"


def test_overriding_the_port():
    config = """
    httpPort: 5602
    """

    r = helm_template(config)

    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["ports"][0]["containerPort"] == 5602

    assert r["service"][name]["spec"]["ports"][0]["targetPort"] == 5602


def test_adding_env_from():
    config = """
envFrom:
- secretRef:
    name: secret-name
"""
    r = helm_template(config)
    secretRef = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0][
        "envFrom"
    ][0]["secretRef"]
    assert secretRef == {"name": "secret-name"}


def test_adding_image_pull_secrets():
    config = """
imagePullSecrets:
  - name: test-registry
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["spec"]["imagePullSecrets"][0]["name"]
        == "test-registry"
    )


def test_adding_tolerations():
    config = """
tolerations:
- key: "key1"
  operator: "Equal"
  value: "value1"
  effect: "NoExecute"
  tolerationSeconds: 3600
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["spec"]["tolerations"][0]["key"]
        == "key1"
    )


def test_adding_a_node_selector():
    config = """
nodeSelector:
  disktype: ssd
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["spec"]["nodeSelector"]["disktype"]
        == "ssd"
    )


def test_adding_an_affinity_rule():
    config = """
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: app
          operator: In
          values:
          - kibana
      topologyKey: kubernetes.io/hostname
"""

    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["spec"]["affinity"][
            "podAntiAffinity"
        ]["requiredDuringSchedulingIgnoredDuringExecution"][0]["topologyKey"]
        == "kubernetes.io/hostname"
    )


def test_adding_a_extra_container():
    config = """
extraContainers: |
  - name: do-something
    image: busybox
    command: ['do', 'something']
"""
    r = helm_template(config)
    extraContainer = r["deployment"][name]["spec"]["template"]["spec"]["containers"]
    assert {
        "name": "do-something",
        "image": "busybox",
        "command": ["do", "something"],
    } in extraContainer


def test_adding_a_extra_init_container():
    config = """
extraInitContainers: |
  - name: do-something
    image: busybox
    command: ['do', 'something']
"""
    r = helm_template(config)
    extraInitContainer = r["deployment"][name]["spec"]["template"]["spec"][
        "initContainers"
    ]
    assert {
        "name": "do-something",
        "image": "busybox",
        "command": ["do", "something"],
    } in extraInitContainer


def test_adding_an_ingress_rule():
    config = """
ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
  path: /
  hosts:
    - kibana.elastic.co
  tls:
  - secretName: elastic-co-wildcard
    hosts:
     - kibana.elastic.co
"""

    r = helm_template(config)
    assert name in r["ingress"]
    i = r["ingress"][name]["spec"]
    assert i["tls"][0]["hosts"][0] == "kibana.elastic.co"
    assert i["tls"][0]["secretName"] == "elastic-co-wildcard"

    assert i["rules"][0]["host"] == "kibana.elastic.co"
    assert i["rules"][0]["http"]["paths"][0]["path"] == "/"
    assert i["rules"][0]["http"]["paths"][0]["backend"]["serviceName"] == name
    assert i["rules"][0]["http"]["paths"][0]["backend"]["servicePort"] == 5601


def test_adding_an_ingress_rule_wildcard():
    config = """
ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
  path: /
  hosts:
    - kibana.elastic.co
  tls:
  - secretName: elastic-co-wildcard
    hosts:
     - "*.elastic.co"
"""

    r = helm_template(config)
    assert name in r["ingress"]
    i = r["ingress"][name]["spec"]
    assert i["tls"][0]["hosts"][0] == "*.elastic.co"
    assert i["tls"][0]["secretName"] == "elastic-co-wildcard"

    assert i["rules"][0]["host"] == "kibana.elastic.co"
    assert i["rules"][0]["http"]["paths"][0]["path"] == "/"
    assert i["rules"][0]["http"]["paths"][0]["backend"]["serviceName"] == name
    assert i["rules"][0]["http"]["paths"][0]["backend"]["servicePort"] == 5601


def test_override_the_default_update_strategy():
    config = """
updateStrategy:
  type: "RollingUpdate"
  rollingUpdate:
    maxUnavailable: 1
    maxSurge: 1
"""

    r = helm_template(config)
    assert r["deployment"][name]["spec"]["strategy"]["type"] == "RollingUpdate"
    assert (
        r["deployment"][name]["spec"]["strategy"]["rollingUpdate"]["maxUnavailable"]
        == 1
    )
    assert r["deployment"][name]["spec"]["strategy"]["rollingUpdate"]["maxSurge"] == 1


def test_using_a_name_override():
    config = """
nameOverride: overrider
"""

    r = helm_template(config)
    assert "overrider-kibana" in r["deployment"]


def test_setting_a_custom_service_account():
    config = """
serviceAccount: notdefault
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["spec"]["serviceAccount"]
        == "notdefault"
    )


def test_setting_pod_security_context():
    config = """
podSecurityContext:
  runAsUser: 1001
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["spec"]["securityContext"][
            "runAsUser"
        ]
        == 1001
    )


def test_adding_in_kibana_config():
    config = """
kibanaConfig:
  kibana.yml: |
    key:
      nestedkey: value
    dot.notation: test

  other-config.yml: |
    hello = world
"""
    r = helm_template(config)
    c = r["configmap"][name + "-config"]["data"]

    assert "kibana.yml" in c
    assert "other-config.yml" in c

    assert "nestedkey: value" in c["kibana.yml"]
    assert "dot.notation: test" in c["kibana.yml"]

    assert "hello = world" in c["other-config.yml"]

    d = r["deployment"][name]["spec"]["template"]["spec"]

    assert {"configMap": {"name": name + "-config"}, "name": "kibanaconfig"} in d[
        "volumes"
    ]
    assert {
        "mountPath": "/usr/share/kibana/config/kibana.yml",
        "name": "kibanaconfig",
        "subPath": "kibana.yml",
    } in d["containers"][0]["volumeMounts"]
    assert {
        "mountPath": "/usr/share/kibana/config/other-config.yml",
        "name": "kibanaconfig",
        "subPath": "other-config.yml",
    } in d["containers"][0]["volumeMounts"]

    assert (
        "configchecksum"
        in r["deployment"][name]["spec"]["template"]["metadata"]["annotations"]
    )


def test_changing_the_protocol():
    config = """
protocol: https
"""
    r = helm_template(config)
    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert "https://" in c["readinessProbe"]["exec"]["command"][-1]


def test_changing_the_health_check_path():
    config = """
healthCheckPath: "/kibana/app/kibana"
"""
    r = helm_template(config)
    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]

    assert 'http "/kibana/app/kibana"' in c["readinessProbe"]["exec"]["command"][-1]


def test_priority_class_name():
    config = """
priorityClassName: ""
"""
    r = helm_template(config)
    spec = r["deployment"][name]["spec"]["template"]["spec"]
    assert "priorityClassName" not in spec

    config = """
priorityClassName: "highest"
"""
    r = helm_template(config)
    priority_class_name = r["deployment"][name]["spec"]["template"]["spec"][
        "priorityClassName"
    ]
    assert priority_class_name == "highest"


def test_service_labels():
    config = ""

    r = helm_template(config)

    assert "label1" not in r["service"][name]["metadata"]["labels"]

    config = """
    service:
      labels:
        label1: value1
    """

    r = helm_template(config)

    assert r["service"][name]["metadata"]["labels"]["label1"] == "value1"


def test_service_annotations():
    config = """
service:
  annotations:
    cloud.google.com/load-balancer-type: "Internal"
    """
    r = helm_template(config)
    s = r["service"][name]["metadata"]["annotations"][
        "cloud.google.com/load-balancer-type"
    ]
    assert s == "Internal"

    config = """
service:
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-internal: 0.0.0.0/0
    """
    r = helm_template(config)
    s = r["service"][name]["metadata"]["annotations"][
        "service.beta.kubernetes.io/aws-load-balancer-internal"
    ]
    assert s == "0.0.0.0/0"


def test_service_load_balancer_source_ranges():
    config = """
service:
  loadBalancerSourceRanges:
    - 0.0.0.0/0
    """
    r = helm_template(config)
    l = r["service"][name]["spec"]["loadBalancerSourceRanges"][0]
    assert l == "0.0.0.0/0"

    config = """
service:
  loadBalancerSourceRanges:
    - 192.168.0.0/24
    - 192.168.1.0/24
    """
    r = helm_template(config)
    l = r["service"][name]["spec"]["loadBalancerSourceRanges"][0]
    assert l == "192.168.0.0/24"
    l = r["service"][name]["spec"]["loadBalancerSourceRanges"][1]
    assert l == "192.168.1.0/24"


def test_adding_a_nodePort():
    config = ""

    r = helm_template(config)

    assert "nodePort" not in r["service"][name]["spec"]["ports"][0]

    config = """
    service:
      nodePort: 30001
    """

    r = helm_template(config)

    assert r["service"][name]["spec"]["ports"][0]["nodePort"] == 30001


def test_override_the_serverHost():
    config = """
    serverHost: "localhost"
    """

    r = helm_template(config)

    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["env"][1]["name"] == "SERVER_HOST"
    assert c["env"][1]["value"] == "localhost"


def test_adding_pod_annotations():
    config = """
podAnnotations:
  iam.amazonaws.com/role: es-role
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["spec"]["template"]["metadata"]["annotations"][
            "iam.amazonaws.com/role"
        ]
        == "es-role"
    )


def test_override_imagePullPolicy():
    config = ""

    r = helm_template(config)
    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["imagePullPolicy"] == "IfNotPresent"

    config = """
    imagePullPolicy: Always
    """

    r = helm_template(config)
    c = r["deployment"][name]["spec"]["template"]["spec"]["containers"][0]
    assert c["imagePullPolicy"] == "Always"


def test_adding_pod_labels():
    config = """
labels:
  app.kubernetes.io/name: kibana
"""
    r = helm_template(config)
    assert (
        r["deployment"][name]["metadata"]["labels"]["app.kubernetes.io/name"]
        == "kibana"
    )
    assert (
        r["deployment"][name]["spec"]["template"]["metadata"]["labels"][
            "app.kubernetes.io/name"
        ]
        == "kibana"
    )


def test_service_to_pod_label_selectors():
    config = ""

    r = helm_template(config)

    assert all(
        l in r["deployment"][name]["spec"]["template"]["metadata"]["labels"].items()
        for l in r["service"][name]["spec"]["selector"].items()
    )


def test_service_to_pod_label_selectors_with_custom_labels():
    config = """
labels:
  app.kubernetes.io/name: kibana
"""

    r = helm_template(config)

    assert all(
        l in r["deployment"][name]["spec"]["template"]["metadata"]["labels"].items()
        for l in r["service"][name]["spec"]["selector"].items()
    )


def test_adding_a_secret_mount_with_subpath():
    config = """
secretMounts:
  - name: elastic-certificates
    secretName: elastic-certs
    path: /usr/share/elasticsearch/config/certs
    subPath: cert.crt
"""
    r = helm_template(config)
    d = r["deployment"][name]["spec"]["template"]["spec"]
    assert d["containers"][0]["volumeMounts"][-1] == {
        "mountPath": "/usr/share/elasticsearch/config/certs",
        "subPath": "cert.crt",
        "name": "elastic-certificates",
    }


def test_adding_a_secret_mount_without_subpath():
    config = """
secretMounts:
  - name: elastic-certificates
    secretName: elastic-certs
    path: /usr/share/elasticsearch/config/certs
"""
    r = helm_template(config)
    d = r["deployment"][name]["spec"]["template"]["spec"]
    assert d["containers"][0]["volumeMounts"][-1] == {
        "mountPath": "/usr/share/elasticsearch/config/certs",
        "name": "elastic-certificates",
    }


def test_adding_lifecycle_events():
    config = """
lifecycle:
    postStart:
        exec:
            command: ['/bin/true']
"""
    r = helm_template(config)
    d = r["deployment"][name]["spec"]["template"]["spec"]
    p = d["containers"][0]["lifecycle"]["postStart"]
    assert p["exec"]["command"][0] == "/bin/true"


def test_setting_fullnameOverride():
    config = """
fullnameOverride: 'kibana-custom'
"""
    r = helm_template(config)

    custom_name = "kibana-custom"
    assert custom_name in r["deployment"]
    assert custom_name in r["service"]

    assert r["service"][custom_name]["spec"]["ports"][0]["port"] == 5601
    assert (
        r["deployment"][custom_name]["spec"]["template"]["spec"]["containers"][0][
            "name"
        ]
        == "kibana"
    )


def test_adding_loadBalancerIP():
    config = """
    service:
      loadBalancerIP: 12.5.11.79
    """

    r = helm_template(config)

    assert r["service"][name]["spec"]["loadBalancerIP"] == "12.5.11.79"


def test_service_port_name():
    r = helm_template("")

    config = """
    service:
      httpPortName: istio
    """

    r = helm_template(config)

    assert r["service"][name]["spec"]["ports"][0]["name"] == "istio"


def test_hostaliases():
    config = """
hostAliases:
- ip: "127.0.0.1"
  hostnames:
  - "foo.local"
  - "bar.local"
"""
    r = helm_template(config)
    hostAliases = r["deployment"][name]["spec"]["template"]["spec"]["hostAliases"]
    assert {"ip": "127.0.0.1", "hostnames": ["foo.local", "bar.local"]} in hostAliases
