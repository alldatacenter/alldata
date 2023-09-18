apiVersion: 1
providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    updateIntervalSeconds: 10
    options:
      path: /opt/edp/${service.serviceName}/conf/provisioning/dashboards/grafana