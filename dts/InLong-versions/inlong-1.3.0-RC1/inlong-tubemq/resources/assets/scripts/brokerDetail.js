/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    var page = {
        init: function () {
            var brokerId = this.brokerId = $.getUrlParam('brokerId');
            var self = this;
            self.initEvent();
            self.showDetail();
            self.showNav(brokerId);
            self.$brokerDataTable = {};

            self.dialogInstance = new Dialog({
                successHook: $.proxy(self.showDetail, self)
            });
            self.checkBoxInstance = new CheckBox();
        },

        initEvent: function () {
            var self = this;
            var brokerId = self.brokerId;

            $('body').on('click', '.js_reload_one', function () {
                self.dialogInstance.confirmBrokerInfo('reload', [brokerId]);
            }).on('click', '.js_online_one', function () {
                self.dialogInstance.confirmBrokerInfo('online', [brokerId]);
            }).on('click', '.js_offline_one', function () {
                self.dialogInstance.confirmBrokerInfo('offline', [brokerId]);
            }).on('click', '.js_delete_one', function () {
                self.dialogInstance.confirmBrokerInfo('delete', [brokerId]);
            }).on('click', '.js_mod_one', function () {
                self.dialogInstance.addBrokerInfo('mod', brokerId);
            }).on('click', '.js_toggle_sub', function () { // 切换订阅开关
                var $this = $(this);
                self.checkBoxInstance.process('sub', $this, self.dialogInstance, brokerId);
            }).on('click', '.js_toggle_pub', function () { // 切换发布开关
                var $this = $(this);
                self.checkBoxInstance.process('pub', $this, self.dialogInstance, brokerId);
            });
        },

        showNav: function (navText) {
            $('#brokerNav').html(navText);
        },

        showDetail: function () {
            var self = this;
            self.showBrokerDetail(self.brokerId);
            self.showBrokerCurDetail(self.brokerId);
        },

        showBrokerDetail: function (brokerId) {
            var self = this;
            var url = G_CONFIG.HOST
                + "?type=op_query&method=admin_query_broker_run_status&withDetail=true&brokerId="
                + brokerId + "&callback=?";
            $.getJSON(url)
                .done(function (res) {
                    if (res.errCode === 0) {
                        var data = res.data[0];
                        var tableData = [];
                        var lastPushTopicList = data.BrokerSyncStatusInfo
                            ? data.BrokerSyncStatusInfo.lastPushBrokerTopicSetConfInfo : [];
                        var reportedTopicList = data.BrokerSyncStatusInfo
                            ? data.BrokerSyncStatusInfo.reportedBrokerTopicSetConfInfo : [];
                        var lastPushInfo = data.BrokerSyncStatusInfo
                            ? data.BrokerSyncStatusInfo.lastPushBrokerDefaultConfInfo : '';
                        var reportedInfo = data.BrokerSyncStatusInfo
                            ? data.BrokerSyncStatusInfo.reportedBrokerDefaultConfInfo : '';

                        self.initTopicTable('lastPush', lastPushTopicList);
                        self.initTopicTable('reported', reportedTopicList);
                        self.initDefInfo('lastPush', lastPushInfo);
                        self.initDefInfo('reported', lastPushInfo);
                        self.initCommonInfo(data);
                    }
                });
        },

        showBrokerCurDetail: function (brokerId) {
            var self = this;
            var url = G_CONFIG.HOST
                + "?type=op_query&method=admin_query_broker_configure&withTopic=true&brokerId="
                + brokerId + "&callback=?";
            $.getJSON(url)
                .done(function (res) {
                    if (res.errCode === 0) {
                        var data = res.data[0];
                        var tableData = [];
                        var curTopicList = data.topicSet;

                        self.initTopicTable('cur', curTopicList);
                        self.initDefInfo('cur', data);

                    }
                });
        },

        initCommonInfo: function (dataSet) {
            var translation2Boolean = {
                'true': '是',
                'false': '否',
                '-': '-'
            };
            var getCheckBox = function (data, className) {
                var checked = data === "true" ? ' checked' : '';
                className = className || '';
                return '<span class="switch' + checked + ' ' + className
                    + '"><input type="checkbox" checked></span>';
            }

            var data = dataSet.BrokerSyncStatusInfo || '';
            dataSet.currBrokerConfId = data.currBrokerConfId || '-';
            dataSet.lastPushBrokerConfId = data.lastPushBrokerConfId || '-';
            dataSet.reportedBrokerConfId = data.reportedBrokerConfId || '-';

            var html = '<div class="form-wp sep-2">' +
                '    <div class="row">' +
                '        <div class="tit">可发布:</div>' +
                '        <div class="cnt">' +
                getCheckBox(dataSet.acceptPublish, 'js_toggle_pub') +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">BrokerID:</div>' +
                '        <div class="cnt">' + dataSet.brokerId +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">BrokerIP:</div>' +
                '        <div class="cnt">' + dataSet.brokerIp +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">BrokerPort:</div>' +
                '        <div class="cnt">' + dataSet.brokerPort +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">当前管理状态:</div>' +
                '        <div class="cnt">' + dataSet.manageStatus +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">当前运行状态:</div>' +
                '        <div class="cnt">' + dataSet.runStatus +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">当前配置Id:</div>' +
                '        <div class="cnt">' + dataSet.currBrokerConfId +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">最后下发配置Id:</div>' +
                '        <div class="cnt">' + dataSet.lastPushBrokerConfId +
                '        </div>' +
                '    </div>' +
                '</div>' +
                '<div class="form-wp sep-2">' +
                '    <div class="row">' +
                '        <div class="tit">可订阅:</div>' +
                '        <div class="cnt">' +
                getCheckBox(dataSet.acceptSubscribe, 'js_toggle_sub') +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">配置是否已变更:</div>' +
                '        <div class="cnt">' + translation2Boolean[dataSet.isConfChanged] +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">变更是否已加载:</div>' +
                '        <div class="cnt">' + translation2Boolean[dataSet.isConfLoaded] +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">broker是否已注册:</div>' +
                '        <div class="cnt">' + translation2Boolean[dataSet.isBrokerOnline] +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">是否已上线:</div>' +
                '        <div class="cnt">' + translation2Boolean[dataSet.isBrokerOnline] +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">当前Broker版本:</div>' +
                '        <div class="cnt">' + dataSet.brokerVersion +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">最近上报配置Id:</div>' +
                '        <div class="cnt">' + dataSet.reportedBrokerConfId +
                '        </div>' +
                '    </div>' +
                '</div>';

            $('#commonInfo').html(html);
        },

        initDefInfo: function (type, dataSet) {
            var type2Dom = {
                'cur': '#curInfo',
                'lastPush': '#lastPushInfo',
                'reported': '#reportedInfo'
            }
            if (!(type in type2Dom)) {
                throw 'initTopicTable: invalid type'
            }
            var html = '<div class="form-wp sep-2">' +
                '    <div class="row">' +
                '        <div class="tit">acceptPublish：</div>' +
                '        <div class="cnt">' + dataSet.acceptPublish + '</div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">unflushThreshold：</div>' +
                '        <div class="cnt">' + dataSet.unflushThreshold + '</div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">numPartitions：</div>' +
                '        <div class="cnt">' + dataSet.numPartitions + '</div>' +
                '    </div>' +
                '</div>' +
                '<div class="form-wp sep-2">' +
                '    <div class="row">' +
                '        <div class="tit">acceptSubscribe：</div>' +
                '        <div class="cnt">' + dataSet.acceptSubscribe + '</div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">unflushInterval：</div>' +
                '        <div class="cnt">' + dataSet.unflushInterval + '</div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">deletePolicy：</div>' +
                '        <div class="cnt">' + dataSet.deletePolicy + '</div>' +
                '    </div>' +
                '</div>';
            var emptyHtml = '<div class="form-wp sep-2">' +
                '    <div class="row">' +
                '       暂无记录' +
                '    </div>' +
                '</div>';
            $(type2Dom[type]).html(dataSet ? html : emptyHtml);
        },

        initTopicTable: function (type, dataSet) {
            var type2Dom = {
                'cur': '#curTopicTable',
                'lastPush': '#lastPushTopicTable',
                'reported': '#reportedTopicTable'
            }
            if (!(type in type2Dom)) {
                throw 'initTopicTable: invalid type'
            }
            if (!this.$brokerDataTable[type]) {
                var dataTable = this.$brokerDataTable[type] = $(type2Dom[type]).DataTable({
                    data: dataSet,
                    columns: [{
                        "data": "topicName",
                        "orderable": false
                    }, {
                        "data": "numPartitions"
                    }, {
                        "data": "acceptPublish",
                        "orderable": false
                    }, {
                        "data": "acceptSubscribe",
                        "orderable": false
                    }, {
                        "data": "unflushThreshold"
                    }, {
                        "data": "unflushInterval"
                    }, {
                        "data": "deletePolicy",
                        "orderable": false
                    }],
                    language: {
                        searchPlaceholder: '请输入消费组或者Topic名称',
                        processing: "Loading...",
                        search: "搜索:",
                        //lengthMenu: "每页显示 _MENU_ 条",
                        lengthMenu: '每页显示 <select class="min">'
                        +
                        '<option value="10">10</option>'
                        +
                        '<option value="20">20</option>'
                        +
                        '<option value="30">30</option>'
                        +
                        '<option value="40">40</option>'
                        +
                        '<option value="50">50</option>'
                        +
                        '<option value="-1">全部</option>'
                        +
                        '</select> 条',
                        info: "当前显示由 _START_ 到 _END_ 条，共 _TOTAL_ 条记录， ",
                        infoEmpty: "",
                        //infoFiltered:
                        // "(_MAX_
                        // 条数据搜索结果)",
                        infoFiltered: "",
                        infoPostFix: "",
                        loadingRecords: "正在加载...",
                        zeroRecords: "暂无记录",
                        emptyTable: "暂无记录",
                        paginate: {
                            first: '<i class="i-first"></i>',
                            previous: '<i class="i-prev"></i>',
                            next: '<i class="i-next"></i>',
                            last: '<i class="i-last"></i>'
                        }
                    },
                    'pagingType': "full_numbers",
                    "dom": '<"scroll-wp"rt><"pg-wp"ilp>',
                    //"dom":
                    // 'f<"scroll-wp"rt><"pg-wp"ilp>'
                });

                $('#' + type + 'Search').on('keyup click', function () {
                    dataTable.search(
                        $(this).val()
                    ).draw();
                });
            } else {
                this.$brokerDataTable[type].clear().rows.add(dataSet).draw();
            }

        }
    };

    page.init();
})();