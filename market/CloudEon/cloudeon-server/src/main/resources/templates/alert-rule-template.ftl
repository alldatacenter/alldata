groups:
  - name: ${serviceName}
    rules:
    <#list ruleList as rule>
    -  alert: ${rule.ruleName}
       expr: ${rule.promql}
       labels:
           alertLevel: ${rule.alertLevel}
           clusterId: ${rule.clusterId}
           serviceRoleName: ${rule.stackRoleName}
           serviceName: ${rule.stackServiceName}
       annotations:
           alertAdvice: ${rule.alertAdvice}
           alertInfo:  "${rule.alertInfo}"
    </#list>