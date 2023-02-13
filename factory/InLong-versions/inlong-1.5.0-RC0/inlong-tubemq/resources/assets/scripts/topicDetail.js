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
            var self = this;
            var topicName = self.topicName = $.getUrlParam('topicName');

            self.dialogInstance = new Dialog({
                successHook: $.proxy(self.showDetail, self)
            });
            self.checkBoxInstance = new CheckBox();
            self.$brokerDataTable = null;
            self.$consumerTable = null;

            self.initEvent();
            self.showNav(topicName);
            self.showDetail();
        },

        initEvent: function () {
            var self = this;
            var topicName = self.topicName;

            $('body').on('click', '.js_reload_one', function () {
                self.dialogInstance.confirmBrokerInfo('reload', [$(this).attr('data-brokerId')]);
            }).on('click', '.js_delete_one', function () {
                var brokerId = $(this).attr('data-brokerId');
                self.dialogInstance.confirmTopicInfo('delete', topicName, [brokerId]);
            }).on('click', '.js_mod_one', function () {
                self.dialogInstance.addTopicInfo('mod', topicName, $('#modTopic').data('topic'));
            }).on('click', '.js_delete_consumer_one', function () {
                var groupName = $(this).attr('data-groupName');
                self.dialogInstance.confirm('确认 <span class="hl">删除</span> 该消费组吗？',
                    function (passwd, formData) {
                        $.getJSON(G_CONFIG.HOST
                            + '?type=op_modify&method=admin_delete_allowed_consumer_group_info&topicName='
                            + topicName + '&groupName=' + groupName
                            + '&confModAuthToken=' + passwd
                            + '&callback=?').done(function (data) {
                            if (data.errCode === 0) {
                                self.dialogInstance.showTips('操作成功');
                                self.dialogInstance.successHook
                                && self.dialogInstance.successHook();
                            } else {
                                self.dialogInstance.showTips(data.errMsg);
                            }
                        });
                    });
            }).on('click', '.js_add_consumer_one', function () {
                self.dialogInstance.addConsumerGroup('add', topicName);
            }).on('click', '.js_toggle_auth', function () { // 切换订阅开关
                var $this = $(this);
                self.checkBoxInstance.process('setTopicAuth', $this, self.dialogInstance,
                    topicName);
            }).on('click', '.js_toggle_pub', function () { // 切换发布开关
                var $this = $(this);
                var checkedClass = 'checked';
                var state = $this.hasClass(checkedClass);
                var type = ['allowPub', 'disPub'][+state];
                self.dialogInstance.confirmTopicInfo(type, topicName);
            }).on('click', '.js_toggle_sub', function () { // 切换发布开关
                var $this = $(this);
                var checkedClass = 'checked';
                var state = $this.hasClass(checkedClass);
                var type = ['allowSub', 'disSub'][+state];
                self.dialogInstance.confirmTopicInfo(type, topicName);
            });
        },

        showNav: function (consumeGroup) {
            $('#topicNav').html(consumeGroup)
        },

        showDetail: function () {
            this.showTopicDetail(this.topicName);
            this.showConsumeGroupDetail(this.topicName);
        },

        showTopicDetail: function (topicName) {
            var self = this;
            var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_topic_info&topicName="
                + topicName + "&callback=?";
            $.getJSON(url)
                .done(function (res) {
                    if (res.errCode === 0) {
                        var data = res.data[0];
                        self.renderCommonInfo(data);
                        self.renderDefInfo(data.topicInfo[0]);
                        self.initBrokerTable(data.topicInfo);
                    }
                });
        },

        showConsumeGroupDetail: function (topicName) {
            var self = this;
            var url = G_CONFIG.HOST
                + "?type=op_query&method=admin_query_topic_authorize_control&topicName="
                + topicName + "&callback=?";
            $.getJSON(url)
                .done(function (res) {
                    if (res.errCode === 0) {
                        var data = res.data[0] || {};
                        self.initConsumerTable(data.authConsumeGroup);
                        $('#totalAuthControlCount').html(data.authConsumeGroup.length);
                    }
                });
        },

        renderDefInfo: function (dataSet) {
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
            $('#defInfo').html(dataSet ? html : emptyHtml);
            $('#modTopic').data('topic', dataSet);
        },

        renderCommonInfo: function (dataSet) {
            var translation2Boolean = {
                'true': '是',
                'false': '否',
                '-': '-'
            };
            var getChecked = function (data) {
                var checked = data === true ? '是' : '否';
                return checked;
            };
            var getCheckBox = function (data, className) {
                var checked = data === true ? ' checked' : '';
                className = className || '';
                return '<span class="switch' + checked + ' ' + className
                    + '"><input type="checkbox" checked></span>';
            };
            var getAuthBox = function (data, className) {
                var checked = data === true ? ' checked' : '';
                className = className || '';
                return '<span class="switch' + checked + ' ' + className
                    + '"><input type="checkbox" checked></span>';
            };

            var data = dataSet.BrokerSyncStatusInfo || '';
            dataSet.currBrokerConfId = data.currBrokerConfId || '-';
            dataSet.lastPushBrokerConfId = data.lastPushBrokerConfId || '-';
            dataSet.reportedBrokerConfId = data.reportedBrokerConfId || '-';

            var html = '<div class="form-wp sep-2">' +
                '    <div class="row">' +
                '        <div class="tit">topicName:</div>' +
                '        <div class="cnt">' + dataSet.topicName +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">配置分区数:</div>' +
                '        <div class="cnt">' + dataSet.totalCfgNumPart +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">可发布:</div>' +
                '        <div class="cnt">' +
                getCheckBox(dataSet.isSrvAcceptPublish, 'js_toggle_pub') +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">权限受控:</div>' +
                '        <div class="cnt">' + getAuthBox(dataSet.authData.enableAuthControl,
                    'js_toggle_auth') +
                '        </div>' +
                '    </div>' +
                '</div>' +
                '<div class="form-wp sep-2">' +

                '    <div class="row">' +
                '        <div class="tit">配置Broker数:</div>' +
                '        <div class="cnt">' + dataSet.infoCount +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">运行分区数:</div>' +
                '        <div class="cnt">' + dataSet.totalRunNumPartCount +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">可订阅:</div>' +
                '        <div class="cnt">' +
                getCheckBox(dataSet.isSrvAcceptSubscribe, 'js_toggle_pub') +
                '        </div>' +
                '    </div>' +
                '    <div class="row">' +
                '        <div class="tit">授权消费组个数:</div>' +
                '        <div class="cnt" id="totalAuthControlCount">0' +
                '        </div>' +
                '    </div>' +
                '</div>';

            $('#commonInfo').html(html);
        },

        initBrokerTable: function (dataSet) {
            var translation2Boolean = {
                'true': '是',
                'false': '否',
                '-': '-'
            };

            if (!this.$brokerDataTable) {
                var dataTable = this.$brokerDataTable = $('#brokerTable').DataTable({
                    data: dataSet,
                    columns: [{
                        "data": "brokerId",
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return full.brokerId
                                + '#'
                                + full.brokerIp
                                + ':'
                                + full.brokerPort;
                        }
                    }, {
                        "data": "runInfo.acceptPublish",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "runInfo.acceptSubscribe",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "runInfo.numPartitions"
                    }, {
                        "data": "runInfo.brokerManageStatus",
                        "orderable": false
                    }, {
                        "data": "numPartitions"
                    }, {
                        "data": "unflushThreshold"
                    }, {
                        "data": "unflushInterval"
                    }, {
                        "data": "deletePolicy",
                        "orderable": false
                    }, {
                        "data": "acceptPublish",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "acceptSubscribe",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": null,
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return '<a href="javascript:;" class="link js_mod_one"  data-brokerId="'
                                + full.brokerId
                                + '">编辑</a><a href="javascript:;" class="link js_reload_one"  data-brokerId="'
                                + full.brokerId
                                + '">重载</a><a href="javascript:;" class="link js_delete_one"  data-brokerId="'
                                + full.brokerId
                                + '">删除</a>';
                        }
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

                $('#brokerSearch').on('keyup click', function () {
                    dataTable.search(
                        $(this).val()
                    ).draw();
                });

            } else {
                this.$brokerDataTable.clear().rows.add(dataSet).draw();
            }

        },

        initConsumerTable: function (dataSet) {
            if (!this.$consumerTable) {
                var dataTable = this.$consumerTable = $('#consumerTable').DataTable({
                    data: dataSet,
                    columns: [{
                        "data": "groupName",
                        "orderable": false
                    }, {
                        "data": "createUser",
                        "orderable": false
                    }, {
                        "data": "createDate"
                    }, {
                        "data": null,
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return '<a href="javascript:;" class="link js_delete_consumer_one" data-groupname="'
                                + full.groupName
                                + '">删除</a>';
                        }
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
                    "dom": '<"scroll-wp"rt><"pg-wp"ilp>'
                    //"dom":
                    // 'f<"scroll-wp"rt><"pg-wp"ilp>'
                });
                $('#consumerSearch').on('keyup click', function () {
                    dataTable.search(
                        $(this).val()
                    ).draw();
                });

            } else {
                this.$consumerTable.clear().rows.add(dataSet).draw();
            }
        }
    };

    page.init();
})();