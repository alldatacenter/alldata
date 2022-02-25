/*!
* flexibleArea.js v1.0
* A jQuery plugin that dynamically updates textarea's height to fit the content.
* http://flaviusmatis.github.com/flexibleArea.js/
*
* Copyright 2012, Flavius Matis
* Released under the MIT license.
* http://flaviusmatis.github.com/license.html
*/

(function($){
	var methods = {
		init : function() {

			var styles = [
				'paddingTop',
				'paddingRight',
				'paddingBottom',
				'paddingLeft',
				'fontSize',
				'lineHeight',
				'fontFamily',
				'width',
				'fontWeight',
				'border-top-width',
				'border-right-width',
				'border-bottom-width',
				'border-left-width'
			];

			return this.each( function() {

				if (this.type !== 'textarea')	return false;
					
				var $textarea = $(this).css({'resize': 'none', overflow: 'hidden'});
				
				var	$clone = $('<div></div>').css({
					'position' : 'absolute',
					'display' : 'none',
					'word-wrap' : 'break-word',
					'white-space' : 'pre-wrap',
					'border-style' : 'solid'
				}).appendTo(document.body);

				// Apply textarea styles to clone
				for (var i=0; i < styles.length; i++) {
					$clone.css(styles[i],$textarea.css(styles[i]));
				}

				var textareaHeight = parseInt($textarea.css('height'), 10);
				var lineHeight = parseInt($textarea.css('line-height'), 10) || parseInt($textarea.css('font-size'), 10);
				var minheight = lineHeight * 2 > textareaHeight ? lineHeight * 2 : textareaHeight;
				var maxheight = parseInt($textarea.css('max-height'), 10) > -1 ? parseInt($textarea.css('max-height'), 10) : Number.MAX_VALUE;

				function updateHeight() {
					var textareaContent = $textarea.val().replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/&/g, '&amp;').replace(/\n/g, '<br/>');
					// Adding an extra white space to make sure the last line is rendered.
					$clone.html(textareaContent + '&nbsp;');
					setHeightAndOverflow();
				}

				function setHeightAndOverflow(){
					var cloneHeight = $clone.height() + lineHeight;
					var overflow = 'hidden';
					var height = cloneHeight;
					if (cloneHeight > maxheight) {
						height = maxheight;
						overflow = 'auto';
					} else if (cloneHeight < minheight) {
						height = minheight;
					}
					if ($textarea.height() !== height) {
						$textarea.css({'overflow': overflow, 'height': height + 'px'});
					}
				}

				// Update textarea size on keyup, change, cut and paste
				$textarea.bind('keyup change cut paste', function(){
					updateHeight();
				});

				// Update textarea on window resize
				$(window).bind('resize', function (){
					var cleanWidth = parseInt($textarea.width(), 10);
					if ($clone.width() !== cleanWidth) {
						$clone.css({'width': cleanWidth + 'px'});
						updateHeight();
					}
				});

				// Update textarea on blur
				$textarea.bind('blur',function(){
					setHeightAndOverflow()
				});

				// Update textarea when needed
				$textarea.bind('updateHeight', function(){
					updateHeight();
				});

				// Wait until DOM is ready to fix IE7+ stupid bug
				$(function(){
					updateHeight();
				});
				
			});
			
		}
	};

	$.fn.flexible = function(method) {

		// Method calling logic
		if (methods[method]) {
			return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
		} else if (typeof method === 'object' || ! method) {
			return methods.init.apply(this, arguments);
		} else {
			$.error('Method ' + method + ' does not exist on jQuery.easyModal');
		}

	};

})(jQuery);