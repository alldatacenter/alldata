jQuery.fn.pagination = function(maxentries, opts) {
	opts = jQuery.extend({
				items_per_page : 10, // 每页显示多少条记录
				current_page : 0,      //当前页码
				num_display_entries : 4, // 中间显示页码的个数
				num_edge_entries : 2, // 末尾显示页码的个数
				link_to : "javascript:;",         //页码点击后的链接
				prev_text : "上一页",   //上一页的文字
				next_text : "下一页",	   //下一页的文字
				ellipse_text : "...",  //页码之间的省略号
				display_msg : true, // 是否显示记录信息
				prev_show_always : true, //是否总是显示最前页
				next_show_always : true,//是否总是显示最后页
				setPageNo:false,//是否显示跳转第几页
				callback : function() {
					return false;
				} // 回调函数
			}, opts || {});

	return this.each(function() {
		// 总页数
		function numPages() {
			return Math.ceil(maxentries / opts.items_per_page);
		}
		/**
		 * 计算页码
		 */
		function getInterval() {
			var ne_half = Math.ceil(opts.num_display_entries / 2);
			var np = numPages();
			var upper_limit = np - opts.num_display_entries;
			var start = current_page > ne_half ? Math.max(Math.min(current_page
									- ne_half, upper_limit), 0) : 0;
			var end = current_page > ne_half ? Math.min(current_page + ne_half,
					np) : Math.min(opts.num_display_entries, np);
			return [start, end];
		}

		/**
		 * 点击事件
		 */
		function pageSelected(page_id, evt) {
			var page_id = parseInt(page_id);
			current_page = page_id;
			drawLinks();
			var continuePropagation = opts.callback(page_id, panel);
			if (!continuePropagation) {
				if (evt.stopPropagation) {
					evt.stopPropagation();
				} else {
					evt.cancelBubble = true;
				}
			}
			return continuePropagation;
		}

		/**
		 * 链接
		 */
		function drawLinks() {
			panel.empty();
			var interval = getInterval();
			var np = numPages();
			var getClickHandler = function(page_id) {
				return function(evt) {
					return pageSelected(page_id, evt);
				}
			}
			var appendItem = function(page_id, appendopts) {
				page_id = page_id < 0 ? 0 : (page_id < np ? page_id : np-1);
				appendopts = jQuery.extend({
							text : page_id+1,
							classes : ""
						}, appendopts || {});
				if (page_id == current_page) {
					var lnk = $("<span class='current'>" + (appendopts.text)
							+ "</span>");
				} else {
					var lnk = $("<a>" + (appendopts.text) + "</a>").bind(
							"click", getClickHandler(page_id)).attr('href',
							opts.link_to.replace(/__id__/, page_id));

				}
				if (appendopts.classes) {
					lnk.addClass(appendopts.classes);
				}
				panel.append(lnk);
			}
			// 上一页
			if (opts.prev_text && (current_page > 0 || opts.prev_show_always)) {
				appendItem(current_page - 1, {
							text : opts.prev_text,
							classes : "prev"
						});
			}
			// 点点点
			if (interval[0] > 0 && opts.num_edge_entries > 0) {
				var end = Math.min(opts.num_edge_entries, interval[0]);
				for (var i = 0; i < end; i++) {
					appendItem(i);
				}
				if (opts.num_edge_entries < interval[0] && opts.ellipse_text) {
					jQuery("<span>" + opts.ellipse_text + "</span>")
							.appendTo(panel);
				}
			}
			// 中间的页码
			for (var i = interval[0]; i < interval[1]; i++) {
				appendItem(i);
			}
			// 最后的页码
			if (interval[1] < np && opts.num_edge_entries > 0) {
				if (np - opts.num_edge_entries > interval[1]
						&& opts.ellipse_text) {
					jQuery("<span>" + opts.ellipse_text + "</span>")
							.appendTo(panel);
				}
				var begin = Math.max(np - opts.num_edge_entries, interval[1]);
				for (var i = begin; i < np; i++) {
					appendItem(i);
				}

			}
			// 下一页
			if (opts.next_text
					&& (current_page < np - 1 || opts.next_show_always)) {
				appendItem(current_page + 1, {
							text : opts.next_text,
							classes : "next"
						});
			}
			// 记录显示
			if (opts.display_msg) {
				if(!maxentries){
					panel
						.append('<div class="pxofy">暂时无数据可以显示</div>');
				}else{
				panel
						.append('<div class="pxofy">显示第&nbsp;'
								+ ((current_page * opts.items_per_page) + 1)
								+ '&nbsp;条到&nbsp;'
								+ (((current_page + 1) * opts.items_per_page) > maxentries
										? maxentries
										: ((current_page + 1) * opts.items_per_page))
								+ '&nbsp;条记录，总共&nbsp;' + maxentries + '&nbsp;条</div>');
				}
			}
			//设置跳到第几页
			if(opts.setPageNo){
				  panel.append("<div class='goto'><span class='text'>跳转到</span><input type='text'/><span class='page'>页</span><button type='button' class='ue-button long2'>确定</button></div>");	
			}
		}

		// 当前页
		var current_page = opts.current_page;
		maxentries = ( maxentries < 0) ? 0 : maxentries;
		opts.items_per_page = (!opts.items_per_page || opts.items_per_page < 0)
				? 1
				: opts.items_per_page;
		var panel = jQuery(this);
		this.selectPage = function(page_id) {
			pageSelected(page_id);
		}
		this.prevPage = function() {
			if (current_page > 0) {
				pageSelected(current_page - 1);
				return true;
			} else {
				return false;
			}
		}
		this.nextPage = function() {
			if (current_page < numPages() - 1) {
				pageSelected(current_page + 1);
				return true;
			} else {
				return false;
			}
		}
		
		if(maxentries==0){
			panel.append('<span class="current prev">'+opts.prev_text+'</span><span class="current next">'+opts.next_text+'</span><div class="pxofy">暂时无数据可以显示</div>');
		}else{
			drawLinks();
		}
		$(this).find(".goto button").live("click",function(evt){
			var setPageNo = $(this).parent().find("input").val();
			if(setPageNo!=null && setPageNo!=""&&setPageNo>0&&setPageNo<=numPages()){
				pageSelected(setPageNo-1, evt);
			}
		});		
	});
}
