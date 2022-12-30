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
            self.$brokerListDataTable = null;
            self.dialogInstance = new Dialog({
                successHook: $.proxy(self.initTable, self)
            });
            self.checkBoxInstance = new CheckBox();
            self.initTable();
            self.initEvent();
            dropBox.init();

        },
        initEvent: function () {
            var self = this;
            var selectedBrokerid = this.selectedBrokerid;

            $brokerList = $('#brokerList').on('click', '.js_check', function () {
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

            $('#brokerListSearch').off('keyup click').on('keyup click', function () {
                self.$brokerListDataTable.search(
                    $(this).val()
                ).draw();
            });

            $('body').on('click', '.js_reload', function () {
                self.dialogInstance.confirmBrokerInfo('reload', selectedBrokerid);
            }).on('click', '.js_online', function () {
                self.dialogInstance.confirmBrokerInfo('online', selectedBrokerid);
            }).on('click', '.js_offline', function () {
                self.dialogInstance.confirmBrokerInfo('offline', selectedBrokerid);
            }).on('click', '.js_delete', function () {
                self.dialogInstance.confirmBrokerInfo('delete', selectedBrokerid);
            }).on('click', '.js_reload_one', function () {
                self.dialogInstance.confirmBrokerInfo('reload', [$(this).attr('data-id')]);
            }).on('click', '.js_online_one', function () {
                self.dialogInstance.confirmBrokerInfo('online', [$(this).attr('data-id')]);
            }).on('click', '.js_offline_one', function () {
                self.dialogInstance.confirmBrokerInfo('offline', [$(this).attr('data-id')]);
            }).on('click', '.js_delete_one', function () {
                self.dialogInstance.confirmBrokerInfo('delete', [$(this).attr('data-id')]);
            }).on('click', '.js_add_one', function () { // 新增broker
                self.dialogInstance.addBrokerInfo('add');
            }).on('click', '.js_toggle_sub', function () { // 切换订阅开关
                var $this = $(this);
                var brokerId = $this.attr('data-id');
                self.checkBoxInstance.process('sub', $this, self.dialogInstance, brokerId);
            }).on('click', '.js_toggle_pub', function () { // 切换发布开关
                var $this = $(this);
                var brokerId = $this.attr('data-id');
                self.checkBoxInstance.process('pub', $this, self.dialogInstance, brokerId);
            }).off('click.js_all_check').on('click.js_all_check', '.js_all_check', function () { //表格全选
                var $this = $(this);
                var $checkBoxs = $brokerList.find('.js_check');

                var checkedClass = 'checked';
                var isCheck = $this.hasClass(checkedClass);
                if (isCheck) {
                    $this.removeClass(checkedClass);
                } else {
                    $this.addClass(checkedClass);
                }

                $checkBoxs.each(function () {
                    var $that = $(this);
                    var brokerId = $that.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);
                    if (isCheck) {
                        $that.removeClass(checkedClass);
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $that.addClass(checkedClass);
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });

            }).on('click', '#queryBtn', function () {
                var brokerIp = $('#brokerIpInput').val();

                self.initTable({
                    brokerIp: brokerIp,
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
            var self = this;
            var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_broker_run_status&"
                + $.param(opts);
            if (!self.$brokerListDataTable) {
                self.$brokerListDataTable = $('#brokerList').DataTable({
                    "ajax": {
                        "url": url,
                        "dataType": "jsonp"
                    },
                    "columns": [{
                        "data": null,
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return '<span class="checkbox  js_check" data-id="'
                                + full.brokerId
                                + '">' +
                                '    <i class="icon icon_check"></i>'
                                +
                                '    <label for=""></label>'
                                +
                                '    <input type="checkbox" checked>'
                                +
                                '</span>';
                        }
                    }, {
                        "data": "brokerId",
                        "className": 'h1',
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var html = '<a href="broker_detail.html?brokerId='
                                + data
                                + '" class="link" >'
                                + data
                                + '</a>';
                            return html;
                        }
                    }, {
                        "data": "brokerIp"
                    }, {
                        "data": "brokerPort"
                    }, {
                        "data": "manageStatus",
                        "orderable": false
                    }, {
                        "data": "runStatus",
                        "orderable": false
                    }, {
                        "data": "subStatus",
                        "orderable": false
                    }, {
                        "data": "acceptPublish",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var checked = data
                            === "true"
                                ? ' checked'
                                : '';
                            var js_toggle = full.manageStatus
                            === "online"
                            && full.runStatus
                            === "unRegister"
                                ? ''
                                : ' js_toggle_pub';
                            return '<span class="switch'
                                + js_toggle
                                + checked
                                + '" data-id="'
                                + full.brokerId
                                + '"><input type="checkbox" checked></span>';
                        }
                    }, {
                        "data": "acceptSubscribe",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            var checked = data
                            === "true"
                                ? ' checked'
                                : '';
                            var js_toggle = full.manageStatus
                            === "online"
                            && full.runStatus
                            === "unRegister"
                                ? ''
                                : ' js_toggle_sub';
                            return '<span class="switch'
                                + js_toggle
                                + checked
                                + '" data-id="'
                                + full.brokerId
                                + '"><input type="checkbox" checked></span>';
                        }
                    }, {
                        "data": "isConfChanged",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "isConfLoaded",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "isBrokerOnline",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "isBrokerOnline",
                        "orderable": false,
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return translation2Boolean[data];
                        }
                    }, {
                        "data": "brokerId",
                        "orderable": false,
                        "width": "152px",
                        "render": function (data,
                                            type,
                                            full,
                                            meta) {
                            return '<a href="javascript:;" class="link js_online_one" data-id="'
                                + full.brokerId
                                + '">上线</a><a href="javascript:;" class="link js_reload_one" data-id="'
                                + full.brokerId
                                + '">重载</a><a href="javascript:;" class="link js_offline_one" data-id="'
                                + full.brokerId
                                + '">下线</a><a href="javascript:;" class="link js_delete_one" data-id="'
                                + full.brokerId
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
                    "columnDefs": [{
                        "searchable": false,
                        "orderable": false,
                        "targets": 0
                    }],
                    // "columnDefs": [ {
                    //      "orderable":
                    //  false, "targets": [
                    // 0,1,2,3,4,5,6,7 ] }
                    // ],
                    "dom": '<"scroll-wp"rt><"pg-wp"ilp>'
                    //"dom":
                    // 'f<"scroll-wp"rt><"pg-wp"ilp>'
                });
            } else {
                self.$brokerListDataTable.ajax.url(url).load();
            }

            //$brokerListDataTable.search('134').draw()
        }
    };

    page.init();
})();