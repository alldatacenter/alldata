route:
  group_by: ['alert']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 1h
  receiver: 'web.hook'
receivers:
- name: 'web.hook'
  webhook_configs:
  - url: '${cloudeonURL}/apiPre/alert/webhook'
    send_resolved: true
inhibit_rules:
  - source_match:
      alertLevel: '异常级别'
    target_match:
      alertLevel: '告警级别'
    equal: ['alert', 'dev', 'instance']
