# Erda Changelog 1.x

<table>
<tr>
  <th title="Current">1.5<sup>Current</sup></th>
  <th title="Current">1.4</th>
  <th title="Current">1.3</th>
  <th title="Current">1.2</th>
  <th title="Current">1.1</th>
  <th title="Current">1.0</th>
</tr>
<tr>
  <td valign="top">
    <a href="#v150">1.5.0</a><br/>
  </td>
  <td valign="top">
    <a href="#v140">1.4.0</a><br/>
  </td>
  <td valign="top">
    <a href="#v131">1.3.1</a><br/>
    <a href="#v130">1.3.0</a><br/>
  </td>
  <td valign="top">
    <a href="#v121">1.2.1</a><br/>
    <a href="#v120">1.2.0</a><br/>
  </td>
  <td valign="top">
    <a href="#v111">1.1.1</a><br/>
    <a href="#v110">1.1.0</a><br/>
  </td>
  <td valign="top">
    <a href="#v101">1.0.1</a><br/>
    <a href="#v100">1.0.0</a><br/>
  </td>
</tr>
</table>

## v1.5.0

`2021-12-31`

### New Features

* (228759) [Monitoring] Support new visual topology in microservice governance. [#2229](https://github.com/erda-project/erda-ui/pull/2229) [#2256](https://github.com/erda-project/erda-ui/pull/2256)
* (230877) [Efficiency measure] Support efficiency measure of requirements and tasks. [#3313](https://github.com/erda-project/erda/pull/3313) [#3365](https://github.com/erda-project/erda/pull/3365) [#3378](https://github.com/erda-project/erda/pull/3378)
* (238380) [Collaboration] Support Gantt chart for project management. [#3259](https://github.com/erda-project/erda/pull/3259)
* (251153) [Monitoring] Support flame graph in tracing details. [#2085](https://github.com/erda-project/erda-ui/pull/2085)
* (222543) [Testing] Support management of test space status and basic information. [#2758](https://github.com/erda-project/erda/pull/2758)
* (222561) [Testing] Support new label, reference, enable/disable, copy and parallel orchestration for test scene set. [#266](https://github.com/erda-project/erda-actions/pull/266) [#2453](https://github.com/erda-project/erda/pull/2453) [#2989](https://github.com/erda-project/erda/pull/2989)
* (224168) [Monitoring] Support trace/error and log data storage via ES. [#60](https://github.com/erda-project/erda-analyzer/pull/60) [#2633](https://github.com/erda-project/erda/pull/2633)
* (231090) [Monitoring] Support alarm notification in levels and by SMS. [#59](https://github.com/erda-project/erda-analyzer/pull/59) [#2378](https://github.com/erda-project/erda/pull/2378)
* (261239) [Notification] Support notification channels of phone and email. [#2219](https://github.com/erda-project/erda-ui/pull/2219)
* (232241) [Notification] Support notification of DingTalk message. [#3129](https://github.com/erda-project/erda/pull/3129)
* (240364) Support viewing logs of Spark jobs. [#3275](https://github.com/erda-project/erda/pull/3275)
* (247411) [Cloud management] Add container resource menu. [#1983](https://github.com/erda-project/erda-ui/pull/1983)
* (251136) [Monitoring] Support new homepage for the microservice platform. [#3154](https://github.com/erda-project/erda/pull/3154)
* (251135) [Monitoring] Adjust microservice platform menu. [#3093](https://github.com/erda-project/erda/pull/3093)
* (251137) [Monitoring] Support new service list and topology in service monitoring. [#3336](https://github.com/erda-project/erda/pull/3336) [#2388](https://github.com/erda-project/erda-ui/pull/2388)
* (252051) [Collaboration] Support new visual design for issues. [#134](https://github.com/erda-project/erda-infra/pull/134) [#2209](https://github.com/erda-project/erda-ui/pull/2209)
* (263984) Optimize structure of the project collaboration page. [#141](https://github.com/erda-project/erda-infra/pull/141) [#142](https://github.com/erda-project/erda-infra/pull/142) [#143](https://github.com/erda-project/erda-infra/pull/143) [#144](https://github.com/erda-project/erda-infra/pull/144) [#3394](https://github.com/erda-project/erda/pull/3394) [#3401](https://github.com/erda-project/erda/pull/3401) [#3404](https://github.com/erda-project/erda/pull/3404) [#3405](https://github.com/erda-project/erda/pull/3405)
* (264027) Support buildlkitd instead of dockerd for erda packaging and building.
* (264280) Optimize progress percentage statistics of requirements. [#3360](https://github.com/erda-project/erda/pull/3360)
* (267570) Support scalable card component. [#2341](https://github.com/erda-project/erda-ui/pull/2341)

### Bug Fixes

* (204238) The UI release takes too long. [#1220](https://github.com/erda-project/erda-ui/pull/1220)
* (205361) The dashboard can be saved when the required chart type is not selected. [#1952](https://github.com/erda-project/erda-ui/pull/1952)
* (209158) The release action in pipeline failed to report metadata. [#3043](https://github.com/erda-project/erda/pull/3043)
* (209165) [Addon] It is not checked whether there is an addon in the project to be deleted. [#1615](https://github.com/erda-project/erda/pull/1615) [#3419](https://github.com/erda-project/erda/pull/3419) [#3450](https://github.com/erda-project/erda/pull/3450)
* (210151) [Automated testing] If the interface is configured with a loop, the loop strategy should be expanded and displayed by default. [#3020](https://github.com/erda-project/erda/pull/3020)
* (222146) Pipeline cache conflicts. [#3318](https://github.com/erda-project/erda/pull/3318)
* (223455) [Automated testing] After turning the page and editing an execution plan, the page reloads and turns to the first page. [#3137](https://github.com/erda-project/erda/pull/3137)
* (226938) The time fields are incomplete in the exported Excel file of project collaboration, such as closed time. [#2882](https://github.com/erda-project/erda/pull/2882)
* (227823) The error message that occurs when relating repeated issues is unclear. [#2990](https://github.com/erda-project/erda/pull/2990)
* (228378) The timeout configuration is unavailable as there is no timeout input field in the action of test plan. [#3125](https://github.com/erda-project/erda/pull/3125)
* (231292) The support account only has permissions of audit log in the org center. [#2159](https://github.com/erda-project/erda-ui/pull/2159)
* (234806) [DOP] When referencing test cases in manual testing, selecting all only works with cases on the current page. [#1984](https://github.com/erda-project/erda-ui/pull/1984)
* (237839) [Automated testing] The orchestration relationship of parallel orchestrated interfaces or scene sets cannot be viewed in the execution details. [#2892](https://github.com/erda-project/erda/pull/2892)
* (240758) [Deployments] An extra runtime card appears in the deployments when updating deployment via artifact. [#3340](https://github.com/erda-project/erda/pull/3340)
* (246603) The pipeline execution details do not match the selected branch. [#2051](https://github.com/erda-project/erda-ui/pull/2051)
* (246738) Parameter details requires interaction optimization. [#2161](https://github.com/erda-project/erda-ui/pull/2161)
* (247460) The test plan cannot be filtered by iteration. [#1957](https://github.com/erda-project/erda-ui/pull/1957) [#3140](https://github.com/erda-project/erda/pull/3140)
* (247463) The plan name column is too narrow to see full information. [#3032](https://github.com/erda-project/erda/pull/3032)
* (247656) The environment order is inconsistent on different pages. [#1968](https://github.com/erda-project/erda-ui/pull/1968)
* (248026) [Manual testing] The test cases under test subset loads incorrectly. [#3076](https://github.com/erda-project/erda/pull/3076) [#3099](https://github.com/erda-project/erda/pull/3099)
* (248120) The scaled out value of the instance is inconsistent with the limit value displayed in monitoring. [#2004](https://github.com/erda-project/erda-ui/pull/2004)
* (248127) The total number of interfaces, execution rate and pass rate are missed in the automated test case. [#3041](https://github.com/erda-project/erda/pull/3041)
* (248918) Project issue information displays in the personal dashboard though no project is joined. [#3092](https://github.com/erda-project/erda/pull/3092)
* (248931) The organization announcement information is not updated as organization changes. [#2008](https://github.com/erda-project/erda-ui/pull/2008)
* (248966) [Log query] When entering the real-time tail page, the query conditions brought from the log query page should be bracketed. [#172](https://github.com/erda-project/erda-ui-enterprise/pull/172)
* (248985) [Active monitoring] The URL cannot be modified when editing. [#1986](https://github.com/erda-project/erda-ui/pull/1986)
* (249009) The percentage of used quota is wrong. [#1997](https://github.com/erda-project/erda-ui/pull/1997)
* (249051) [Log query] On the real-time tail page, the label filter button should be displayed. [#172](https://github.com/erda-project/erda-ui-enterprise/pull/172)
* (249116) Failed to import tasks with a 502 error. [#3136](https://github.com/erda-project/erda/pull/3136)
* (249137) [Pipeline] The pipeline gets a panic on erda cloud. [#3126](https://github.com/erda-project/erda/pull/3126)
* (249463) Failed to deploy across cluster. [#266](https://github.com/erda-project/erda-actions/pull/266)
* (249480) [Automated testing] The execution pass rate in test plan is not precise to decimals. [#3065](https://github.com/erda-project/erda/pull/3065)
* (249529) [Alarm] The alarm level in alarm rules and notification objects is incorrect after the old data is upgraded. [#2001](https://github.com/erda-project/erda-ui/pull/2001)
* (249553) [Alarm] Enter the alarm list page, edit the alarm rule for the first time, and the value field of the filter rule cannot be edited. [#2001](https://github.com/erda-project/erda-ui/pull/2001)
* (249566) [Notification Channel] When adding a notification channel, the AccessKeyId and AccessKeySecret fields have default values. [#2001](https://github.com/erda-project/erda-ui/pull/2001)
* (249626) [Test space] Failed to query when data exceeds one page. [#3064](https://github.com/erda-project/erda/pull/3064)
* (249629) [Test space] There are two scroll bars on the page. [#2018](https://github.com/erda-project/erda-ui/pull/2018)
* (249666) Unable to view logs on V1.3. [#3416](https://github.com/erda-project/erda/pull/3416)
* (249684) [Service analysis] After using ES to store trace/error and log data, the charts in the service overview show incorrectly. [#2007](https://github.com/erda-project/erda-ui/pull/2007)
* (249688) [O&M dashboard] The data of old dashboard cannot show. [#2007](https://github.com/erda-project/erda-ui/pull/2007)
* (249689) [Deployment log] After using ES to store trace/error and log data, the log of runtime deployment cannot show. [#3073](https://github.com/erda-project/erda/pull/3073) [#3077](https://github.com/erda-project/erda/pull/3077)
* (249764) [Upgrade test] The audit information is incorrect when the quota is configured as 0 before upgrade. [#3349](https://github.com/erda-project/erda/pull/3349)
* (249796) [Alarm] Mobile phone is optional in the notification method of notification object in the alarm strategy. [#2021](https://github.com/erda-project/erda-ui/pull/2021)
* (249887) There is a scroll bar on the project quota page. [#2156](https://github.com/erda-project/erda-ui/pull/2156)
* (249894) The project name is displayed incompletely. [#2154](https://github.com/erda-project/erda-ui/pull/2154)
* (249948) The search experience of the backend management requires optimization. [#183](https://github.com/erda-project/erda-ui-enterprise/pull/183)
* (250216) [Alarm] When adding multiple notification groups in an alarm strategy, only the first one is displayed in the list. [#2023](https://github.com/erda-project/erda-ui/pull/2023)
* (250275) [Alarm] The old custom alarm rules are not compatible. [#3088](https://github.com/erda-project/erda/pull/3088)
* (250388) [Alarm] It goes wrong when selecting all for comparison method in filter rules. [#3102](https://github.com/erda-project/erda/pull/3102)
* (250393) [Alarm] It goes wrong when selecting not match for comparison method in filter rules. [#78](https://github.com/erda-project/erda-analyzer/pull/78) [#86](https://github.com/erda-project/erda-analyzer/pull/86)
* (250611) [Error analysis] No data on the error analysis page. [#77](https://github.com/erda-project/erda-analyzer/pull/77)
* (250655) [Project collaboration] The view mode of markdown requires adjustment. [#2075](https://github.com/erda-project/erda-ui/pull/2075)
* (250657) [Notification channel] The creator field is displayed as the account in the notification channel list. [#3216](https://github.com/erda-project/erda/pull/3216)
* (250658) [Alarm] The page of adding a new alarm strategy shows incorrectly. [#2050](https://github.com/erda-project/erda-ui/pull/2050)
* (250660) The page of adding a new dashboard shows incorrectly. [#2048](https://github.com/erda-project/erda-ui/pull/2048)
* (250877) Failed to turn the page of project members. [#2052](https://github.com/erda-project/erda-ui/pull/2052)
* (250883) [Test space] The dot color should change according to the test status. [#3143](https://github.com/erda-project/erda/pull/3143)
* (251601) The description and default value of the get parameter in API management are not displayed after release. [#2078](https://github.com/erda-project/erda-ui/pull/2078)
* (251616) [Pipeline] The cron compensator still creates pipeline. [#3119](https://github.com/erda-project/erda/pull/3119)
* (252139) The interface execution rate and pass rate exceed 100% in the execution details of test plan. [#3126](https://github.com/erda-project/erda/pull/3126)
* (252476) After cancelling sorting order, the resources rank in descending. [#2107](https://github.com/erda-project/erda-ui/pull/2107)
* (252574) Failed to switch to a joined organization as admin. [#3182](https://github.com/erda-project/erda/pull/3182)
* (252654) In Microservice Platform > Diagnose Analysis > Error Insight, the page of error details crashes. [#2079](https://github.com/erda-project/erda-ui/pull/2079)
* (252901) Failed to execute the automated test case, but the execution result is empty. [#3161](https://github.com/erda-project/erda/pull/3161)
* (256203) [Alarm] Alarms are ignored as multiple notification methods are configured. [#85](https://github.com/erda-project/erda-analyzer/pull/85)
* (256222) [Execution plan] The added test plan is not displayed. [#3165](https://github.com/erda-project/erda/pull/3165)
* (256289) [Notification channel] The switching tab of notification channel switching requires improvement. [#2112](https://github.com/erda-project/erda-ui/pull/2112)
* (256376) [Execution plan] Test scenes are not parallel. [#3166](https://github.com/erda-project/erda/pull/3166)
* (256434) [Microservice governance] The page title shows incorrectly on the microservice homepage. [#2118](https://github.com/erda-project/erda-ui/pull/2118)
* (256435) The resource allocation rate and allocated in the cluster list are wrong. [#3173](https://github.com/erda-project/erda/pull/3173)
* (256437) Failed to go to the page of token management. [#2116](https://github.com/erda-project/erda-ui/pull/2116)
* (256535) After switching organization, the cluster list shows the clusters of the previous organization. [#3173](https://github.com/erda-project/erda/pull/3173)
* (256550) The ascending and descending order of quota usage is reversed. [#3173](https://github.com/erda-project/erda/pull/3173)
* (256552) Failed to copy scenes to an empty scene set. [#3170](https://github.com/erda-project/erda/pull/3170)
* (256557) Only K8s clusters are watermarked in the cluster list. [#2121](https://github.com/erda-project/erda-ui/pull/2121)
* (256579) [Microservice governance] The number of services in msp project is wrong on the microservice homepage. [#3181](https://github.com/erda-project/erda/pull/3181)
* (256635) [Execution plan] Failed to get the latest execution result when a scene is referenced by multiple scene sets. [#3166](https://github.com/erda-project/erda/pull/3166)
* (256636) No data in resource monitoring of pod details. [#3169](https://github.com/erda-project/erda/pull/3169)
* (256648) No data in resource monitoring when clicking node in the pod details to enter the node details page. [#3174](https://github.com/erda-project/erda/pull/3174)
* (256721) [Alarm] The original notification method of DingTalk should be changed to DingTalk group in custom alarm rules. [#3177](https://github.com/erda-project/erda/pull/3177) [#3195](https://github.com/erda-project/erda/pull/3195)
* (256805) The name and value in the cluster list do not match. [#3179](https://github.com/erda-project/erda/pull/3179)
* (256807) The value of memory quota in the cluster list is wrong. [#3179](https://github.com/erda-project/erda/pull/3179)
* (256994) No data of resource statistics occasionally in the cluster list. [#3184](https://github.com/erda-project/erda/pull/3184)
* (257036) Failed to view container resources when switching to terminus-dev cluster. [#3185](https://github.com/erda-project/erda/pull/3185)
* (257042) [Microservice homepage] The number of services in the project shows 0. [#3196](https://github.com/erda-project/erda/pull/3196)
* (257182) An error occurs when editing scenes of automated testing. [#3199](https://github.com/erda-project/erda/pull/3199)
* (257257) [Test management] The statistics page requires performance optimization. [#3222](https://github.com/erda-project/erda/pull/3222) [#3230](https://github.com/erda-project/erda/pull/3230)
* (257960) [Project collaboration] Performance optimization is required. [#3208](https://github.com/erda-project/erda/pull/3208)
* (257968) [Test report] Performance optimization is required. [#3208](https://github.com/erda-project/erda/pull/3208)
* (257975) The error message that occurs when deleting a project requires optimization. [#3362](https://github.com/erda-project/erda/pull/3362)
* (258142) [Application development] Performance optimization is required. [#3256](https://github.com/erda-project/erda/pull/3256)
* (258272) The statistics page is moved up automatically after filtering. [#2167](https://github.com/erda-project/erda-ui/pull/2167)
* (258275) The statistics chart of automated testing shows wrong. [#3233](https://github.com/erda-project/erda/pull/3233)
* (258315) [Automated testing] Each loop result is printed when viewing the execution result of an interface with loop. [#3270](https://github.com/erda-project/erda/pull/3270)
* (259829) The automated test plan references a scene set. The referenced scene set executes successfully but the referencing scene set is failed. [#3244](https://github.com/erda-project/erda/pull/3244)
* (260508) An error occurs when saving the edited pipeline. [#3262](https://github.com/erda-project/erda/pull/3262)
* (260709) [Gantt chart] An error occurs when selecting multiple iterations. [#2199](https://github.com/erda-project/erda-ui/pull/2199)
* (260722) [Gantt chart] The historical data lacks start time. [#3289](https://github.com/erda-project/erda/pull/3289)
* (260727) [Gantt chart] The tasks already included should be excluded. [#3286](https://github.com/erda-project/erda/pull/3286)
* (260744) [Gantt chart] Failed to click to view details of the included tasks. [#2200](https://github.com/erda-project/erda-ui/pull/2200)
* (260745) [Gantt chart] The default iteration is wrong when there is no iteration in progress. [#3284](https://github.com/erda-project/erda/pull/3284)
* (260843) [Gantt chart] The page is slow with a lot of requirements. [#2205](https://github.com/erda-project/erda-ui/pull/2205)
* (260845) [Gantt chart] Manual refresh is required after removing the relation. [#2260](https://github.com/erda-project/erda-ui/pull/2260)
* (260939) [Gantt chart] An error occurs occasionally when viewing plans. [#2202](https://github.com/erda-project/erda-ui/pull/2202)
* (260946) [Custom dashboard] The metrics group is unavailable when creating a new dashboard. [#2204](https://github.com/erda-project/erda-ui/pull/2204)
* (261267) The Gantt chart date is inconsistent with that of issues. [#2211](https://github.com/erda-project/erda-ui/pull/2211)
* (261274) The old data at 00:00 of Gantt chart needs to be fixed. [#2211](https://github.com/erda-project/erda-ui/pull/2211)
* (261286) The Gantt chart filtering requires optimization. [#3276](https://github.com/erda-project/erda/pull/3276)
* (261331) The list of tasks included in the requirement does not show the status. [#2231](https://github.com/erda-project/erda-ui/pull/2231)
* (261389) The UXD of Gantt chart requires optimization. [#2269](https://github.com/erda-project/erda-ui/pull/2269)
* (261398) [Automated testing] No secondary confirmation when deleting scenes. [#3277](https://github.com/erda-project/erda/pull/3277)
* (261505) The iteration oder is not fixes in Gantt chart. [#3282](https://github.com/erda-project/erda/pull/3282)
* (261837) [Gantt chart] The filled in time is inconsistent with that of timeline. [#2220](https://github.com/erda-project/erda-ui/pull/2220)
* (261852) The date is not displayed when the mouse hovers on Gantt chart. [#2220](https://github.com/erda-project/erda-ui/pull/2220)
* (262206) The pod list in the DaemonSet details page shows incorrectly. [#3290](https://github.com/erda-project/erda/pull/3290)
* (262534) [Deployments] The domain name set for the service does not take effect. [#3322](https://github.com/erda-project/erda/pull/3322) [#3323](https://github.com/erda-project/erda/pull/3323)
* (262586) [Automated testing] The button on the interface add/edit page is wrong. [#2224](https://github.com/erda-project/erda-ui/pull/2224)
* (262614) The data of issue due today and tomorrow on the homepage is wrong. [#3354](https://github.com/erda-project/erda/pull/3354)
* (262679) [Automated test Platforming] Failed to view the configuration sheet log when executing a single test scene. [#3304](https://github.com/erda-project/erda/pull/3304)
* (263116) [Automated testing] The pagination in execution details is incorrect. [#3371](https://github.com/erda-project/erda/pull/3371)
* (263172) Failed to deploy API gateway in the testing environment. [#3331](https://github.com/erda-project/erda/pull/3331)
* (263196) Create a bug with one click in ticket, and the page crashes when clicking the related issues in bug details. [#2236](https://github.com/erda-project/erda-ui/pull/2236)
* (263214) [Test environment] The admin cannot access the operation management platform. [#2243](https://github.com/erda-project/erda-ui/pull/2243)
* (263260) [Service list] The error rate of the services in the list always shows 0. [#3339](https://github.com/erda-project/erda/pull/3339)
* (263281) [Service list] The average delay of service apm-demo-dubbo is inconsistent with that in topology. [#2242](https://github.com/erda-project/erda-ui/pull/2242)
* (263293) The efficiency measure in test environment requires optimization. [#2254](https://github.com/erda-project/erda-ui/pull/2254)
* (263359) [Topology] The page crashes after clicking the gateway node and external service node in the topology. [#2244](https://github.com/erda-project/erda-ui/pull/2244)
* (263409) [Service list] An error occurs when entering the service list of a msp project. [#2245](https://github.com/erda-project/erda-ui/pull/2245)
* (263474) [Automated testing] The interface execution takes 5 hours and the loop termination condition does not work. [#3329](https://github.com/erda-project/erda/pull/3329)
* (263572) The status not required in the process should not be displayed. [#2255](https://github.com/erda-project/erda-ui/pull/2255)
* (263575) Icon is required for issue complexity. [#2270](https://github.com/erda-project/erda-ui/pull/2270)
* (263634) dice.yml error leads to orchestrator panic. [#3351](https://github.com/erda-project/erda/pull/3351)
* (263635) [Gantt chart] Manual refresh is required after filtering by assignee. [#3335](https://github.com/erda-project/erda/pull/3335)
* (263765) [Gantt chart] The dragged date is incorrect. [#2269](https://github.com/erda-project/erda-ui/pull/2269)
* (263766) The refresh button does not work on the project collaboration page. [#2271](https://github.com/erda-project/erda-ui/pull/2271)
* (264118) [Service analysis] The data of RPC call is incorrect. [#2279](https://github.com/erda-project/erda-ui/pull/2279)
* (264469) [Automated testing] The later interface cannot get the output parameters of the previous interface when executing the plan. [#3402](https://github.com/erda-project/erda/pull/3402)
* (264516) The addon oversold ratio is invalid in test environment. [#3383](https://github.com/erda-project/erda/pull/3383)
* (265589) [Microservice governance] The HTTP status chart of service node in the topology is incorrect. [#3397](https://github.com/erda-project/erda/pull/3397)
* (265590) [Gantt chart] An error occurs when dragging the date. [#3390](https://github.com/erda-project/erda/pull/3390)
* (265633) The environment variables are not injected when running workflow via SPRAK-OPERATOR. [#3392](https://github.com/erda-project/erda/pull/3392)
* (265656) [Topology] The number of unhealthy services and free services is wrong in topology analysis. [#2297](https://github.com/erda-project/erda-ui/pull/2297)
* (265684) [Topology] MySQL and Redis nodes are not displayed in the topology. [#3414](https://github.com/erda-project/erda/pull/3414)
* (265721) [Topology] The HTTP status chart of services node details is not displayed completely with a certain number of status. [#2297](https://github.com/erda-project/erda-ui/pull/2297)
* (265728) [Topology] The services in topology overview and topology analysis include external nodes. [#2297](https://github.com/erda-project/erda-ui/pull/2297)
* (265844) The link in DingTalk notifications is incorrect. [#3400](https://github.com/erda-project/erda/pull/3400)
* (265949) The application cannot be scaled out. [#3398](https://github.com/erda-project/erda/pull/3398)
* (266007) The scroll bar style in test statistics is wrong. [#2304](https://github.com/erda-project/erda-ui/pull/2304)
* (266027) The service list is empty when adding service instance for addon. [#2305](https://github.com/erda-project/erda-ui/pull/2305)
* (266168) The organization member list requires optimization. [#2343](https://github.com/erda-project/erda-ui/pull/2343)
* (266407) [Service list] The average throughput and average delay in the chart is inconsistent with that in the list. [#3414](https://github.com/erda-project/erda/pull/3414) [#3445](https://github.com/erda-project/erda/pull/3445)
* (266408) [Service list] The services in the unhealthy service list are not filtered. [#3414](https://github.com/erda-project/erda/pull/3414)
* (266409) [Service monitoring] The abscissa axis time in the chart is incorrect when the query time is longer than one hour. [#3414](https://github.com/erda-project/erda/pull/3414)
* (266410) [Service list] The unhealthy services in the unhealthy service ranking is inconsistent with that in topology. [#3414](https://github.com/erda-project/erda/pull/3414)
* (266411) [Service list] An error occurs on the service list page. [#3414](https://github.com/erda-project/erda/pull/3414)
* (266412) [Service monitoring] The time unit in the slow response API and slow response SQL is unreasonable. [#3414](https://github.com/erda-project/erda/pull/3414)
* (266417) [Service monitoring] No unit for the response time. [#2316](https://github.com/erda-project/erda-ui/pull/2316)
* (266446) [Filter] No profile photo in kanban. [#3412](https://github.com/erda-project/erda/pull/3412)
* (266447) [Project collaboration] An error occurs when turning page or refreshing list. [#3413](https://github.com/erda-project/erda/pull/3413)
* (266448) [Project collaboration] Add conditions to the custom filter conditions, the filter status turns to changed, but the added conditions are not displayed after clicking filter. [#2320](https://github.com/erda-project/erda-ui/pull/2320)
* (266449) [Project collaboration] The filter conditions in the issue list cannot be displayed normally. [#2321](https://github.com/erda-project/erda-ui/pull/2321)
* (266450) [Project collaboration] Add two filter items after selecting all, click cancel and open again, then the two items are still displayed. [#2322](https://github.com/erda-project/erda-ui/pull/2322)
* (266452) [Project collaboration] No load more in kanban. [#3412](https://github.com/erda-project/erda/pull/3412)
* (266454) [Project collaboration] There are two scroll bars in kanban. [#2323](https://github.com/erda-project/erda-ui/pull/2323)
* (266455) [Project collaboration] The kanban filter in iteration requires modification. [#3412](https://github.com/erda-project/erda/pull/3412)
* (266456) [Project collaboration] The delete icon (x) style requires adjustment. [#2322](https://github.com/erda-project/erda-ui/pull/2322)
* (266458) [Project collaboration] The start date and end date cannot be the same in the kanban filter. [#2327](https://github.com/erda-project/erda-ui/pull/2327)
* (266459) [Project collaboration] Severity should be changed to complexity in requirement filter. [#3469](https://github.com/erda-project/erda/pull/3469)
* (266460) [Project collaboration] Logic error of filter interval in kanban. [#2327](https://github.com/erda-project/erda-ui/pull/2327)
* (266461) [Project collaboration] The filter cursor requires optimization. [#2326](https://github.com/erda-project/erda-ui/pull/2326)
* (266463) [Project collaboration] The labels are not displayed completely. [#2328](https://github.com/erda-project/erda-ui/pull/2328)
* (266466) [Service monitoring] The overview page shows incorrectly. [#2336](https://github.com/erda-project/erda-ui/pull/2336)
* (266908) [Artifact management] No artifact displayed when quickly updating from artifacts. [#2358](https://github.com/erda-project/erda-ui/pull/2358)
* (267323) [Service list] The page number of unhealthy services page and no traffic services page is incorrect. [#3488](https://github.com/erda-project/erda/pull/3488)
* (267696) [Org center] The project information page of msp projects is wrong. [#2351](https://github.com/erda-project/erda-ui/pull/2351)
* (267958) [Monitoring] The page of notification channel and access configuration of msp projects does not respond. [#3458](https://github.com/erda-project/erda/pull/3458)
* (267968) [Microservice governance] An error occurs when returning to the microservice homepage from the service list page. [#2363](https://github.com/erda-project/erda-ui/pull/2363)
* (267978) [Tracing] No data returned when querying by dubboMethod. [#3490](https://github.com/erda-project/erda/pull/3490) [#2405](https://github.com/erda-project/erda-ui/pull/2405)
* (267990) Assignee style error. [#2362](https://github.com/erda-project/erda-ui/pull/2362)
* (268044) Click the related requirement or task but no issue is found. [#2365](https://github.com/erda-project/erda-ui/pull/2365)
* (268052) The requirement deadline should change as its included task is removed. [#3470](https://github.com/erda-project/erda/pull/3470)
* (269998) Failed to change the issue status in the issue list. [#3515](https://github.com/erda-project/erda/pull/3515)


# v1.4.0

`2021-11-16`

### New Features

* Support parallel scene sets in automated testing. [#2173](https://github.com/erda-project/erda/pull/2173)
* Support scene set importing and exporting in automated testing. [#2470](https://github.com/erda-project/erda/pull/2470)
* Support step copying and pasting in automated testing. [#2481](https://github.com/erda-project/erda/pull/2481)
* Support step enabling and disabling in automated testing. [#2453](https://github.com/erda-project/erda/pull/2453)
* Accelerate the loading of manual test related pages. [#2910](https://github.com/erda-project/erda/pull/2910)
* Support issue dashboard with history data displayed in bar chart, pie chart, etc. [#2294](https://github.com/erda-project/erda/pull/2294) [#2462](https://github.com/erda-project/erda/pull/2462)
* Support setting resource quotas according to the project's workspace granularity in the management center. [#2283](https://github.com/erda-project/erda/pull/2283)
* Support resource usage ranking of projects in the cloud management platform. [#2525](https://github.com/erda-project/erda/pull/2525)
* Support sending SMS alerts through custom notification channels in microservice and cloud management platforms. [#2460](https://github.com/erda-project/erda/pull/2460)
* Support Elasticsearch as a backend storage in the microservice platform. [#2861](https://github.com/erda-project/erda/pull/2861)
* Support automatically adding the .yml suffix to the file name when user creates a pipeline in the DevOps platform. [#2685](https://github.com/erda-project/erda/pull/2685)
* Support K8s versions below 1.16 in the Kubernetes dashboard of cloud management platform.[#2852](https://github.com/erda-project/erda/pull/2852)
* Support dynamic configuration search depth of git search interface in the DevOps platform.[#2872](https://github.com/erda-project/erda/pull/2872)
* Optimize the alarm trigger conditions and alarm expressions in the microservice platform. [#2739](https://github.com/erda-project/erda/pull/2739)
* Support service analysis of microservice&DevOps projects in the microservice platform.[#2782](https://github.com/erda-project/erda/pull/2782) [#2833](https://github.com/erda-project/erda/pull/2833)



### Bug Fixes

* Fix the bug that action will not automatically synchronize the latest version of GitHub. [#2507](https://github.com/erda-project/erda/pull/2507)
* Fix the bug that it is not checked whether there is a cycle in the scene set when it is moved. [#2309](https://github.com/erda-project/erda/pull/2309)
* Fix the bug that the execution action of automated test plan cannot monitor whether the plan is executed. [#2407](https://github.com/erda-project/erda/pull/2407)
* Fix the bug that the .yml suffix is not added when creating a pipeline. [#2685](https://github.com/erda-project/erda/pull/2685)
* Fix the bug of incorrect calculation of execution time of pipeline loop task. [#2816](https://github.com/erda-project/erda/pull/2816)
* Fix the bug that the pipeline with the same ID is scheduled repeatedly. [#2921](https://github.com/erda-project/erda/pull/2921)
* Fix the bug of slower requests as automated testing tasks apply for a large number of tokens. [#2991](https://github.com/erda-project/erda/pull/2991)
* Fix the bug that application deletion failed without returning an error message. [#2613](https://github.com/erda-project/erda/pull/2613)
* Fix the bug that add a unique index to the application table to avoid applications with the same name. [#2611](https://github.com/erda-project/erda/pull/2611)
* Optimize the audit message for org update. [#2706](https://github.com/erda-project/erda/pull/2706)
* Fix the bug that the parent context is recycled from which the child context gets data, causing the gittar component to panic. [#2348](https://github.com/erda-project/erda/pull/2348)
* Optimize the API statistics of automated testing. [#2806](https://github.com/erda-project/erda/pull/2806)
* Fix the bug that the execution details of the scene set in the automated testing cannot show the execution environment at the time. [#2529](https://github.com/erda-project/erda/pull/2529)
* Fix the bug that when click to retry pipeline timing tasks, the trigger time will not change. [#2560](https://github.com/erda-project/erda/pull/2560)
* Fix the bug that the imported scene set contains configuration sheet and an eoor occurs when click to view details. [#2609](https://github.com/erda-project/erda/pull/2609)
* Fix the bug that the status in the test space records of importing and exporting is inconsistent with that in the test space list. [#2624](https://github.com/erda-project/erda/pull/2624)
* Fix the bug that the execution time of steps in automated testing is 00:00. [#2650](https://github.com/erda-project/erda/pull/2650)
* Fix the bug that in case of multiple instances in the pipeline, the number quried by queue manager is inconsistent. [#2742](https://github.com/erda-project/erda/pull/2742)
* Fix the bug that branch variables are not injected in the pipeline. [#2797](https://github.com/erda-project/erda/pull/2797)
* Fix the bug of incorrect issue status. [#2268](https://github.com/erda-project/erda/pull/2268)
* Fix the bug of incorrect filtering result. [#2504](https://github.com/erda-project/erda/pull/2504)
* Fix the bug that an error occurs when the namespace does not have aliyun secret in scheduler. [#2456](https://github.com/erda-project/erda/pull/2456)
* Fix the bug that the filtering rules of custom alarm created in msp and cmp are incorrect. [#2860](https://github.com/erda-project/erda/pull/2860)
* Fix the bug that span of tracing is missing in the microservice platform. [2849](https://github.com/erda-project/erda/pull/2849), [2820](https://github.com/erda-project/erda/pull/2820)
* Fix the bug that log index cache gets overwritten when multi esurls exist in Erda cluster. [#2887](https://github.com/erda-project/erda/pull/2887)
* Modify the git-push address of mobile template in the DevOps platform. [#2808](https://github.com/erda-project/erda/pull/2808)
* Fix the bug that pipeline does not reset the execution start time of the cyclic task in the DevOps platform. [#2816](https://github.com/erda-project/erda/pull/2816)
* Fix the bug that write data to etcd after handleServiceGroup function in scheduler. [#2604](https://github.com/erda-project/erda/pull/2604)
* Fix the bug that the execution of test plan leaves out archived plans in the DevOps platform. [#2663](https://github.com/erda-project/erda/pull/2663)
* Fix the bug that one of the tasks in the pipeline of automated testing may be in execution after canceling in the DevOps platform. [#2684](https://github.com/erda-project/erda/pull/2684)
* Fix the bug that the environment variables of container resource are not updated when scaling the service group in the DevOps platform. [#2672](https://github.com/erda-project/erda/pull/2672)
* Fix the bug that admin account queries all organizations in the DevOps platform. [#2692](https://github.com/erda-project/erda/pull/2692)




# v1.3.1

`2021-10-15`

### New Features
* The DevOps platform now supports code coverage dashboard & bugs dashboard.[#2342](https://github.com/erda-project/erda/pull/2342)
* Optimize HTTP active monitoring in the microservice platform [#2377](https://github.com/erda-project/erda/pull/2377)
* The scenario set of the automated test platform now supports parallel execution.[#2412](https://github.com/erda-project/erda/pull/2412)
* Optimize load speed of k8s dashboard's nodes list. [#2355](https://github.com/erda-project/erda/pull/2355)

### Bug Fixes
* Fix the bug that batch cluster upgrade has wrong permission.[#2308](https://github.com/erda-project/erda/pull/2308)
* Fix the bug that cluster-agent module missing privileged param.[#2367](https://github.com/erda-project/erda/pull/2367)
* Fix the bug that there is no user information in the notification group of the microservice platform.[#2393](https://github.com/erda-project/erda/pull/2393)
* Fix the bug that start same k8s dashboard sever redundantly when watch multi clusters. [#2366](https://github.com/erda-project/erda/pull/2366)

# v1.3.0

`2021-09-30`

### New Features

* Cloud management module add k8s dashboard。[#1542](https://github.com/erda-project/erda/pull/1542) [#1585](https://github.com/erda-project/erda/pull/1582) [#1703](https://github.com/erda-project/erda/pull/1703)
* Add admin role and system-auditor role.[#1031](https://github.com/erda-project/erda-ui/pull/1031)
* Projects collaborate with item creators and handlers to increase the ability to modify item types.[#1347](https://github.com/erda-project/erda-ui/pull/1347) [#2090](https://github.com/erda-project/erda/pull/2090)
* Adjust audit log max retention days to 180 days.[#2142](https://github.com/erda-project/erda/pull/2142)
* Support read all unread mbox with one click.[#1593](https://github.com/erda-project/erda/pull/1593)
* Add execute-time and pass-rate in autotest-plan table component.[#1684](https://github.com/erda-project/erda/pull/1684)
* Add audit for runtime deploy operate.[#1653](https://github.com/erda-project/erda/pull/1653)
* Add application filter in authorize modal.[#1371](https://github.com/erda-project/erda-ui/pull/1371)
* Pipeline actions support multi containers monitor.[#1585](https://github.com/erda-project/erda/pull/1585) [#1777](https://github.com/erda-project/erda/pull/1777)
* Improvement of the Api-Design module.[#1632](https://github.com/erda-project/erda/pull/1632) [#1575](https://github.com/erda-project/erda/pull/1575)
* Improvment on security of gittar access.[#1607](https://github.com/erda-project/erda/pull/1607) [#1663](https://github.com/erda-project/erda/pull/1663)
* Improvement on MicroService module, support opentracing integration.[#1829](https://github.com/erda-project/erda/pull/1829)
* MicroService module support member management.[#1659](https://github.com/erda-project/erda/pull/1659)
* Improvement on request-tracing feature.[#1553](https://github.com/erda-project/erda/pull/1553)
* Add inspection for MQ requests.[#1676](https://github.com/erda-project/erda/pull/1676)
* Improvement on Log Query, support AND, OR operator.[#1960](https://github.com/erda-project/erda/pull/1960)
* Add new log analytics addon.[#1547](https://github.com/erda-project/erda/pull/1547)
* Change log max lines limit to 5000.[#1348](https://github.com/erda-project/erda-ui/pull/1348)


### Bug Fixes

* Fix the bug that non-exist branch page loop request error.[#1090](https://github.com/erda-project/erda-ui/pull/1090)
* Fix the bug that action form edit struct-array error.[#1132](https://github.com/erda-project/erda-ui/pull/1132)
* Fix the bug that project-pipeline pageNo change error.[#1211](https://github.com/erda-project/erda-ui/pull/1211)
* Fix the bug that node information arrangement style bug of clusters management nodes detail.[#1322](https://github.com/erda-project/erda-ui/pull/1322)
* Fix the bug that scene sets would display Chinese in English mode.[#1330](https://github.com/erda-project/erda-ui/pull/1330)
* Fix the issue that add default value for enumerated custom fields when quick create issue.[#1351](https://github.com/erda-project/erda-ui/pull/1351)
* Fix the bug that Ellipsis tooltip error.[#1353](https://github.com/erda-project/erda-ui/pull/1353)
* Fix the bug that api-design missing url params when click left menu.[#1375](https://github.com/erda-project/erda-ui/pull/1375)
* Fix the issue that add placeholder for contractive-filter / adjust backlog filter item.[#1384](https://github.com/erda-project/erda-ui/pull/1384)
* Fix the bugs of Form validation on the API design page and the display bug of the response params example.[#1395](https://github.com/erda-project/erda-ui/pull/1395)
* Modify the error message returned。[#1709](https://github.com/erda-project/erda/pull/1709)
* Fix the issue that autotest step input param do not support '.'.[#2065](https://github.com/erda-project/erda/pull/2065)
* Fix the bug that menu of AppMonitor display error.[#2077](https://github.com/erda-project/erda/pull/2077) [#2084](https://github.com/erda-project/erda/pull/2084)
* Fix the issue that get execute env from report env.[#2088](https://github.com/erda-project/erda/pull/2088)
* Modify micro_service dop role-list.[#2135](https://github.com/erda-project/erda/pull/2135)
* Support cms for pipeline with cron enabled.[#1741](https://github.com/erda-project/erda/pull/1741)

### Refactor

* Refactor the uc component, support intergrate with kratos.[#1460](https://github.com/erda-project/erda/pull/1460)
* Fix single point problem of core components.
* Refactor the OpenApi，support declare grpc api expose to OpenApi.[#1584](https://github.com/erda-project/erda/pull/1584)
* Add etcd distributed lock.[#1548](https://github.com/erda-project/erda/pull/1548)
* Refactor api of the Hepa to grpc.[#1744](https://github.com/erda-project/erda/pull/1744)
* Gittar remove skipAuth.[#1856](https://github.com/erda-project/erda/pull/1856)
* Rename worker cluster tag.[#2124](https://github.com/erda-project/erda/pull/2124)

# v1.2.1

`2021-08-23`

### Bug Fixes

* Fix the issue of table style for manual test case. [#953](https://github.com/erda-project/erda-ui/pull/953)
* Fix the bug that occured when switching the source type for the first time after resetting the form while adding tags. [#957](https://github.com/erda-project/erda-ui/pull/957)
* Fix the bug of regular expression of repository address field. [#958](https://github.com/erda-project/erda-ui/pull/958)
* Fix the issue of purple label without background color. [#962](https://github.com/erda-project/erda-ui/pull/962)
* Fix the bug that the empty page icon is not displayed when there is no branch on the API design page. [#970](https://github.com/erda-project/erda-ui/pull/970)
* Fix the bug of tracking details type.[#975](https://github.com/erda-project/erda-ui/pull/975)
* Fix the bug that cluster_name and application_id do not take effect when they exist in custom filter rules. [#1459](https://github.com/erda-project/erda/pull/1459)
* Fix the issue that the ES index of log analysis is not scrolling.[#1464](https://github.com/erda-project/erda/pull/1464)[#1465](https://github.com/erda-project/erda/pull/1465)
* Fix the issue of memory leak when getting the instance list.[#1493](https://github.com/erda-project/erda/pull/1493)
* Support getting the specified pod when obtaining the pod status list.[#1495](https://github.com/erda-project/erda/pull/1495)

# v1.2.0

`2021-08-16`

### New Features

* Optimize overview and project list in MSP. [#809](https://github.com/erda-project/erda-ui/pull/809)
* Support sending test messages when configuring DingTalk notifications. [#777](https://github.com/erda-project/erda-ui/pull/777)
* Support importing and exporting automation test sets. [#749](https://github.com/erda-project/erda-ui/pull/749)
* Enable more features of multi-cloud management platform for free users. [#759](https://github.com/erda-project/erda-ui/pull/759)
* Optimize the way to add EDAS clusters. [#750](https://github.com/erda-project/erda-ui/pull/750)
* Optimize markdown editor interaction and style. [#853](https://github.com/erda-project/erda-ui/pull/853)
* Optimize pipeline log style. [#802](https://github.com/erda-project/erda-ui/pull/802)
* Optimize pipeline notification content. [#1189](https://github.com/erda-project/erda/pull/1189)
* Optimize the execution logic of automation test cases. [#1103](https://github.com/erda-project/erda/pull/1103)
* Support filtering test case executor by unassigned person in the test plan. [#732](https://github.com/erda-project/erda/pull/732)
* Add precheck for existence of package-lock.json before packaging front-end applicaiton. [#1116](https://github.com/erda-project/erda/pull/1116)

### Bug Fixes

* Fix a bug of cluster parameter in the project pipeline. [#737](https://github.com/erda-project/erda-ui/pull/737)
* Fix the bug of data duplication in repo file comparison. [#744](https://github.com/erda-project/erda-ui/pull/744)
* Modify markdown editor style. [#763](https://github.com/erda-project/erda-ui/pull/763)
* Fix a bug of env parameter in the project pipeline. [#765](https://github.com/erda-project/erda-ui/pull/765)
* Fix the style issue when dragging and droppping Nusi component tree. [#769](https://github.com/erda-project/erda-ui/pull/769)
* Fix the error of operation key value in action form. [#771](https://github.com/erda-project/erda-ui/pull/771)
* Fix the mandatory verification error of custom labels in the form. [#778](https://github.com/erda-project/erda-ui/pull/778)
* Fix the button style issue of markdown editor. [#782](https://github.com/erda-project/erda-ui/pull/782)
* Fix the issue of yml editor add node disappearance and actionForm componentization data error. [#781](https://github.com/erda-project/erda-ui/pull/781)
* Fix the bug of the drop-down width when selecting artifact ID in deployment center.[#827](https://github.com/erda-project/erda-ui/pull/827)
* Add registration command and retry initialization operations for EDAS cluster. [#840](https://github.com/erda-project/erda-ui/pull/840)
* Fix the bug of extension service form in project. [#863](https://github.com/erda-project/erda-ui/pull/863)
* Fix the possible crash bug when adding members using nicknames with special characters. [#862](https://github.com/erda-project/erda-ui/pull/862)
* Fix the bug that projectId is missing in the request application list. [#873](https://github.com/erda-project/erda-ui/pull/873)
* Fix the bug that Git repositories can be cloned without account password.[#877](https://github.com/erda-project/erda-ui/pull/877)
* Fix the bug that the text prompt is incomplete caused by invalid form in trace debugging. [#857](https://github.com/erda-project/erda-ui/pull/857)
* Fix the data error of related issues after changing issue to backlog. [#902](https://github.com/erda-project/erda-ui/pull/902)
* Fix the bug that two scroll bars appear when scrolling the item in backlog table. [#839](https://github.com/erda-project/erda-ui/pull/839)
* Fix the API error reported after deleting files in repo. [#910](https://github.com/erda-project/erda-ui/pull/910)
* Fix the error occured when initializing pipeline action form. [#912](https://github.com/erda-project/erda-ui/pull/912)
* Fix some table issues: column width too long or insufficient, table exceeds the page and uniform overflow omission. [#736](https://github.com/erda-project/erda-ui/pull/736)
* Fix the style issue that the item name of extended query column is too long. [#739](https://github.com/erda-project/erda-ui/pull/739)
* Fix the bug that required fields in project collaboration are not marked as required.[#746](https://github.com/erda-project/erda-ui/pull/746)
* Add width to the table in OrgCenter > Projects. [#755](https://github.com/erda-project/erda-ui/pull/755)
* Fix the bug of style validation occured when adding issue in Issues > Backlog. [#757](https://github.com/erda-project/erda-ui/pull/757)
* Fix the bug that in Multi-Cloud Management Platform > Alarm Record, click a record for details, then all list items are expanded when clicking the expand button before the list items.[#754](https://github.com/erda-project/erda-ui/pull/754)
* Fix the bug that when editing issues, the month in datepicker cannot be changed.[#761](https://github.com/erda-project/erda-ui/pull/761)
* Increase the width of the member table. [#767](https://github.com/erda-project/erda-ui/pull/767)
* Fix the bug that the row representing the folder in the test case table shows an extra column for the interface pass rate. [#768](https://github.com/erda-project/erda-ui/pull/768)
* Increase the width of the test case table. [#774](https://github.com/erda-project/erda-ui/pull/774)
* Add a mouse-over style to tables with row click events. [#766](https://github.com/erda-project/erda-ui/pull/766)
* Adjust the width of the related issue table. [#775](https://github.com/erda-project/erda-ui/pull/775)
* Fix the loop refresh issue when the path is /-. [#780](https://github.com/erda-project/erda-ui/pull/780)
* Fix the bug that the left arrow faces wrongly when the tree on the left side of the test case is expanded, and the parent node occasionally collapses when the child node is clicked.[#773](https://github.com/erda-project/erda-ui/pull/773)
* Fix the bug that some rows in the test case table go beyond the right side. [#790](https://github.com/erda-project/erda-ui/pull/790)
* Fix the incorrect address of application repository in application settings. [#797](https://github.com/erda-project/erda-ui/pull/797)
* Fix the issue that the color of alert list icon is black. [#808](https://github.com/erda-project/erda-ui/pull/808)
* Fix the issue that error occured when editing custom addon of extended service. [#812](https://github.com/erda-project/erda-ui/pull/812)
* Fix the issue that the text field of trace debugging body is too long to see the tabs above. [#820](https://github.com/erda-project/erda-ui/pull/820)
* Fix the bug that logs cannot be downloaded when using the default duration. [#842](https://github.com/erda-project/erda-ui/pull/842)
* Fix the bug that the search box does not display when the data is empty. [#906](https://github.com/erda-project/erda-ui/pull/906)
* Migrate Affix and InputNumber from Nusi to Antd.[#779](https://github.com/erda-project/erda-ui/pull/779)
* Change the grouping rules to mandatory when creating custom alarm rules. [#752](https://github.com/erda-project/erda-ui/pull/752)
* Fix the issue that the setting of runtime rollback number does not take effect.

### Refactor

* The interfaces of msp and monitor modules are all defined by proto.
* Optimize Quick-Start for one-click deployment of Erda Standalone mode on the local machine. [#1242](https://github.com/erda-project/erda/pull/1242)

# v1.1.1

`2021-8-5`

### Bug Fixes

* EDAS and K8S use the same agent now. ([#1277](https://github.com/erda-project/erda/pull/1277))
* Enable cloud management platform for free users. ([#810](https://github.com/erda-project/erda-ui/pull/810))
* Fixed the issue that error occured when editing custom addon of extended service. ([#813](https://github.com/erda-project/erda-ui/pull/813))

# v1.1.0

`2021-07-28`

### New Features

- Support existing clusters importing by users ([#806](https://github.com/erda-project/erda/pull/806))
- Support subscription to issue changes, to receive notifications timely when followed issue is modified ([#451](https://github.com/erda-project/erda-ui/pull/451))
- Support asynchronous import and export of manual test cases ([#380](https://github.com/erda-project/erda-ui/pull/380))
- Support auto page refresh for automated test plan ([#446](https://github.com/erda-project/erda-ui/pull/446))
- Support bug closed time viewing and filtering ([#445](https://github.com/erda-project/erda/pull/445))
- Add project-level applications to realize rapid project migration ([#350](https://github.com/erda-project/erda-ui/pull/350))
- Support page turning in Issues > Backlog ([#395](https://github.com/erda-project/erda-ui/pull/395))
- Optimize clone address of code repository ([#155](https://github.com/erda-project/erda-ui/pull/155))
- Optimize size of sliding window and description area for issue editing ([#314](https://github.com/erda-project/erda-ui/pull/314))
- Support size adjusting for table pagination ([#1031](https://github.com/erda-project/erda/pull/1031))
- Turn enter search to delayed auto search for personal dashboard ([#324](https://github.com/erda-project/erda-ui/pull/324))
- Optimize the downloaded file name and suffix format of container log: service name_timestamp.log ([#684](https://github.com/erda-project/erda/pull/684/files))
- Upgrade logo ([#688](https://github.com/erda-project/erda-ui/pull/688))

### Bug Fixes

- Safari page crashes when access Code Repository > Commit History ([#384](https://github.com/erda-project/erda-ui/pull/384))
- The list order remains unchanged after viewing MR ([#661](https://github.com/erda-project/erda/pull/661))
- The certificate file is uploaded but its name is not displayed ([#639](https://github.com/erda-project/erda-ui/pull/639))
- Canceling the edit of merge request will clear the comparison result ([#638](https://github.com/erda-project/erda-ui/pull/638))
- Failed to download files in code repository ([#588](https://github.com/erda-project/erda-ui/pull/588))
- The pipeline node shows the previously failed error ([#422](https://github.com/erda-project/erda-ui/pull/422))
- Members will automatically log out if exit the organization ([#347](https://github.com/erda-project/erda-ui/pull/347))

### Refactor

- Split out new platform services of dop, msp, cmp, ecp and admin
  - dop ([#392](https://github.com/erda-project/erda-ui/pull/392))
  - msp ([#407](https://github.com/erda-project/erda-ui/pull/407))
  - cmp ([#416](https://github.com/erda-project/erda-ui/pull/416))
  - ecp ([#419](https://github.com/erda-project/erda-ui/pull/419))
- Remove components of qa, apim, cmdb, ops and tmc
- Add core components of cluster manager
- Add cluster-dialer instead of soldier to handle inter-cluster communication
- Add a new way to define an interface using protobuf protocol, and the msp component has been migrated

# v1.0.1

`2021-07-08`

### New features

- Support individuals to create organization if he/she doesn't belong to any organizations ([#592](https://github.com/erda-project/erda/pull/592))
- Support scaling application without restarting the existing instances ([#644](https://github.com/erda-project/erda/pull/644)) ([#645](https://github.com/erda-project/erda/pull/645))

### Bug Fixes

- Fix the issue that custom stages were created with null value ([#588](https://github.com/erda-project/erda/pull/588)) ([#606](https://github.com/erda-project/erda/pull/606))
- Keep the creator, assignee, create time and man hour unchanged when issue type is switched ([#610](https://github.com/erda-project/erda/pull/610)) ([#612](https://github.com/erda-project/erda/pull/612))
- Fix the logic error of job deletion under the specified namespace ([#632](https://github.com/erda-project/erda/pull/632)) ([#636](https://github.com/erda-project/erda/pull/636))
- Add guest permissions for dashboard and ticket ([#701](https://github.com/erda-project/erda/pull/701)) ([#705](https://github.com/erda-project/erda/pull/705))
- Reset flags before loop for wait step when timed out ([#715](https://github.com/erda-project/erda/pull/715))

# v1.0.0

`2021-06-09`

Erda v1.0.0 is released!

Start your Erda journey in two ways:

- [Quick start in your local machine](https://github.com/erda-project/erda/blob/master/docs/guides/quickstart/quickstart-full.md)
- [Install with K8s](https://github.com/erda-project/erda/blob/master/docs/guides/deploy/How-to-install-Erda.md)
