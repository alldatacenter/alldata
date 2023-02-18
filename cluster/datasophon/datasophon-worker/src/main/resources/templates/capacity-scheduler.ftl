<configuration>

  <property>
    <name>yarn.scheduler.capacity.maximum-applications</name>
    <value>10000</value>
    <description>
      Maximum number of applications that can be pending and running.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.maximum-am-resource-percent</name>
    <value>0.5</value>
    <description>
      Maximum percent of resources in the cluster which can be used to run 
      application masters i.e. controls number of concurrent running
      applications.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.resource-calculator</name>
    <value>org.apache.hadoop.yarn.util.resource.DominantResourceCalculator</value>
    <description>
      The ResourceCalculator implementation to be used to compare 
      Resources in the scheduler.
      The default i.e. DefaultResourceCalculator only uses Memory while
      DominantResourceCalculator uses dominant-resource to compare 
      multi-dimensional resources such as Memory, CPU etc.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.queues</name>
    <value><#list queueList as queue>${queue.queueName}<#sep>,</#list></value>
    <description>
      The queues at the this level (root is the root queue).
    </description>
  </property>
<#list queueList as queue>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.capacity</name>
    <value>${queue.capacity}</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.user-limit-factor</name>
    <value>1</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.state</name>
    <value>RUNNING</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.acl_submit_applications</name>
    <value>${queue.aclUsers}</value>
  </property>
  <#if queue.nodeLabel != "default">
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.accessible-node-labels</name>
    <value>${queue.nodeLabel}</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.accessible-node-labels.${queue.nodeLabel}.capacity</name>
    <value>100</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.accessible-node-labels.${queue.nodeLabel}.capacity</name>
    <value>100</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.default-node-label-expression</name>
    <value>${queue.nodeLabel}</value>
  </property>
  </#if>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.acl_administer_queue</name>
    <value>*</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.acl_application_max_priority</name>
    <value>*</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.maximum-application-lifetime</name>
    <value>-1</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.${queue.queueName}.default-application-lifetime</name>
    <value>-1</value>
  </property>
</#list>
  <property>
    <name>yarn.scheduler.capacity.node-locality-delay</name>
    <value>40</value>
    <description>
      Number of missed scheduling opportunities after which the CapacityScheduler 
      attempts to schedule rack-local containers.
      When setting this parameter, the size of the cluster should be taken into account.
      We use 40 as the default value, which is approximately the number of nodes in one rack.
      Note, if this value is -1, the locality constraint in the container request
      will be ignored, which disables the delay scheduling.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.rack-locality-additional-delay</name>
    <value>-1</value>
    <description>
      Number of additional missed scheduling opportunities over the node-locality-delay
      ones, after which the CapacityScheduler attempts to schedule off-switch containers,
      instead of rack-local ones.
      Example: with node-locality-delay=40 and rack-locality-delay=20, the scheduler will
      attempt rack-local assignments after 40 missed opportunities, and off-switch assignments
      after 40+20=60 missed opportunities.
      When setting this parameter, the size of the cluster should be taken into account.
      We use -1 as the default value, which disables this feature. In this case, the number
      of missed opportunities for assigning off-switch containers is calculated based on
      the number of containers and unique locations specified in the resource request,
      as well as the size of the cluster.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.queue-mappings</name>
    <value></value>
    <description>
      A list of mappings that will be used to assign jobs to queues
      The syntax for this list is [u|g]:[name]:[queue_name][,next mapping]*
      Typically this list will be used to map users to queues,
      for example, u:%user:%user maps all users to queues with the same name
      as the user.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.queue-mappings-override.enable</name>
    <value>false</value>
    <description>
      If a queue mapping is present, will it override the value specified
      by the user? This can be used by administrators to place jobs in queues
      that are different than the one specified by the user.
      The default is false.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.per-node-heartbeat.maximum-offswitch-assignments</name>
    <value>1</value>
    <description>
      Controls the number of OFF_SWITCH assignments allowed
      during a node's heartbeat. Increasing this value can improve
      scheduling rate for OFF_SWITCH containers. Lower values reduce
      "clumping" of applications on particular nodes. The default is 1.
      Legal values are 1-MAX_INT. This config is refreshable.
    </description>
  </property>


  <property>
    <name>yarn.scheduler.capacity.application.fail-fast</name>
    <value>false</value>
    <description>
      Whether RM should fail during recovery if previous applications'
      queue is no longer valid.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.workflow-priority-mappings</name>
    <value></value>
    <description>
      A list of mappings that will be used to override application priority.
      The syntax for this list is
      [workflowId]:[full_queue_name]:[priority][,next mapping]*
      where an application submitted (or mapped to) queue "full_queue_name"
      and workflowId "workflowId" (as specified in application submission
      context) will be given priority "priority".
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.workflow-priority-mappings-override.enable</name>
    <value>false</value>
    <description>
      If a priority mapping is present, will it override the value specified
      by the user? This can be used by administrators to give applications a
      priority that is different than the one specified by the user.
      The default is false.
    </description>
  </property>

</configuration>
