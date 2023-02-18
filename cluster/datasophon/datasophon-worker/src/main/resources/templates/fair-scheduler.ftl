<?xml version="1.0"?>
<allocations>
	<#list queueList as queue>
	<queue name="${queue.queueName}">
		<minResources>${queue.minResources}</minResources>
		<maxResources>${queue.maxResources}</maxResources>
		<maxRunningApps>${queue.appNum}</maxRunningApps>
		<maxAMShare>${queue.amShare}</maxAMShare>
		<weight>${queue.weight}</weight>
		<schedulingPolicy>${queue.schedulePolicy}</schedulingPolicy>
	</queue>
	</#list>
	<queuePlacementPolicy>
		<rule name="specified" create="false"/>
		<rule name="reject" />
	</queuePlacementPolicy>
</allocations>