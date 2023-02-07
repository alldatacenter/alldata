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
    var dropBox = {
        init: function () {
            var $dropBoxMenu = $('#dropBoxMenu');
            $('#dropBox').hover(function () {
                $dropBoxMenu.show();
            }, function () {
                $dropBoxMenu.hide();
            });
        }
    };

    var page = {
        init: function () {
            var self = this;
            self.selectedBrokerid = [];

            self.dialogInstance = new Dialog({
                successHook: $.proxy(self.initTable, self)
            });
            self.checkBoxInstance = new CheckBox();
            self.$topicListDataTable = null;

            self.initTable();
            self.initEvent();
            dropBox.init();
        },
        initEvent: function () {
            var self = this;
            var selectedBrokerid = this.selectedBrokerid;

            $('#topicList').on('click', '.js_check', function () {
                var $this = $(this);
                var brokerId = $this.attr('data-id');
                var idx = $.inArray(brokerId, selectedBrokerid);

                var checkedClass = 'checked';

                if ($this.hasClass(checkedClass)) {
                    $this.removeClass(checkedClass);
                    // Remove from the 'open' array
                    selectedBrokerid.splice(idx, 1);
                } else {
                    $this.addClass(checkedClass);

                    // Add to the 'open' array
                    if (idx === -1) {
                        selectedBrokerid.push(brokerId);
                    }
                }
            });

            $('body').on('click', '.js_delete_one', function () {
                self.dialogInstance.confirmTopicInfo('delete', $(this).attr('data-topicName'));
            }).on('click', '.js_add_one', function () {
                self.dialogInstance.addTopicInfo('add');
            }).on('click', '.js_toggle_auth', function () { // 切换发布开关
                var $this = $(this);
                var topicName = $this.attr('data-topicName');
                self.checkBoxInstance.process('setTopicAuth', $this, self.dialogInstance,
                    topicName);
            }).on('click', '.js_toggle_pub', function () { // 切换发布开关
                var $this = $(this);
                var checkedClass = 'checked';
                var state = $this.hasClass(checkedClass);
                var type = ['allowPub', 'disPub'][+state];
                self.dialogInstance.confirmTopicInfo(type, $this.attr('data-topicName'));
            }).on('click', '.js_toggle_sub', function () { // 切换发布开关
                var $this = $(this);
                var checkedClass = 'checked';
                var state = $this.hasClass(checkedClass);
                var type = ['allowSub', 'disSub'][+state];
                self.dialogInstance.confirmTopicInfo(type, $this.attr('data-topicName'));
            }).on('click', '#queryBtn', function () {
                var topicName = $('#topicNameInput').val();

                self.initTable({
                    topicName: topicName,
                });
            });
        },
        initTable: function (opts) {
            opts = opts || {};
            var translation2Boolean = {
                'true': '是',
                'false': '否',
                '-': '-'
            };
            var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_topic_broker_config_info&" + $.param(
                    opts);

            if (!this.$topicListDataTable) {
                this.$topicListDataTable = $('#topicList').DataTable({
                    "ajax": {
                        "url": url,
                        "dataType": "jsonp"
                    },
                    "columns": [{
                        "data": "topicName",
                        "className": 'h1',
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var html = '<a href="topic_detail.html?topicName='
                                + data
                                + '" class="link" >'
                                + data
                                + '</a>';
                            return html;
                        }
                    }, {
                        "data": "brokerTotalCfgCnt"
                    }, {
                        "data": "partTotalCfgCnt"
                    }, {
                        "data": "partTotalRunCnt"
                    }, {
                        "data": "topicSrvAccPubStatus",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var checked = data
                            === true
                                ? ' checked'
                                : '';
                            return '<span class="switch'
                                + checked
                                + ' js_toggle_pub" data-topicName="'
                                + full.topicName
                                + '"><input type="checkbox" checked></span>';
                        }
                    }, {
                        "data": "topicSrvAccSubStatus",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var checked = data
                            === true
                                ? ' checked'
                                : '';
                            return '<span class="switch'
                                + checked
                                + ' js_toggle_sub" data-topicName="'
                                + full.topicName
                                + '"><input type="checkbox" checked></span>';
                        }
                    }, {
                        "data": "enableAuthControl",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var checked = data
                            === true
                                ? ' checked'
                                : '';
                            return '<span class="switch'
                                + checked
                                + ' js_toggle_auth" data-topicName="'
                                + full.topicName
                                + '"><input type="checkbox" checked></span>';
                        }
                    }, {
                        "data": null,
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return '<a href="javascript:;" class="link js_delete_one" data-topicName="'
                                + full.topicName
                                + '">删除</a>';
                        }
                    }],
                    language: {
                        searchPlaceholder: '请输入消费组或者Topic名称',
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
                        '<option value="-1">All</option>'
                        +
                        '</select> 条',
                        info: "当前显示第 _START_ 到 _END_ 条，共 _TOTAL_ 条记录， ",
                        infoEmpty: "",
                        infoFiltered: "(_MAX_ 条数据的搜索结果) ",
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

                $('#topicListSearch').on('keyup click', function () {
                    $topicListDataTable.search(
                        $(this).val()
                    ).draw();
                });

            } else {
                this.$topicListDataTable.ajax.url(url).load();
            }

        }
    };

    page.init();
})();