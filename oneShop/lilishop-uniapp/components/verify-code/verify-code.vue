<template>
	<view class="xt__verify-code">
		<!-- 输入框 -->
		<input
			id="xt__input"
			:value="code"
			class="xt__input"
			:focus="isFocus"
			:password="isPassword"
			:type="inputType"
			:maxlength="size"
			@input="input"
			@focus="inputFocus"
			@blur="inputBlur"
		/>

		<!-- 光标 -->
		<view
			id="xt__cursor"
			v-if="cursorVisible && type !== 'middle'"
			class="xt__cursor"
			:style="{ left: codeCursorLeft[code.length] + 'px', height: cursorHeight + 'px', backgroundColor: cursorColor }"
		></view>

		<!-- 输入框 - 组 -->
		<view id="xt__input-ground" class="xt__input-ground">
			<template v-for="(item, index) in size">
				<view
					:key="index"
					:style="{ borderColor: code.length === index && cursorVisible ? boxActiveColor : boxNormalColor }"
					:class="['xt__box', `xt__box-${type + ''}`, `xt__box::after`]"
				>
					<view :style="{ borderColor: boxActiveColor }" class="xt__middle-line" v-if="type === 'middle' && !code[index]"></view>
					<text class="xt__code-text">{{ code[index] | codeFormat(isPassword) }}</text>
				</view>
			</template>
		</view>
	</view>
</template>
<script>
/**
 * @description 输入验证码组件
 * @property {string} type = [box|middle|bottom] - 显示类型 默认：box -eg:bottom
 * @property {string} inputType = [text|number] - 输入框类型 默认：number -eg:number
 * @property {number} size = [4|6] - 支持的验证码数量 默认：6 -eg:6
 * @property {boolean} isFocus - 是否立即聚焦 默认：true
 * @property {boolean} isPassword - 是否以密码形式显示 默认false -eg:false
 * @property {string} cursorColor - 光标颜色 默认：#cccccc
 * @property {string} boxNormalColor - 光标未聚焦到的框的颜色 默认：#cccccc
 * @property {string} boxActiveColor - 光标聚焦到的框的颜色 默认：#000000
 * @event {Function(data)} confirm - 输入完成
 */
export default {
	name: 'xt-verify-code',
	props: {
		value: {
			type: String,
			default: () => ''
		},
		type: {
			type: String,
			default: () => 'box'
		},
		inputType: {
			type: String,
			default: () => 'number'
		},
		size: {
			type: Number,
			default: () => 6
		},
		isFocus: {
			type: Boolean,
			default: () => true
		},
		isPassword: {
			type: Boolean,
			default: () => false
		},
		cursorColor: {
			type: String,
			default: () => '#cccccc'
		},
		boxNormalColor: {
			type: String,
			default: () => '#cccccc'
		},
		boxActiveColor: {
			type: String,
			default: () => '#000000'
		}
	},
	model: {
		prop: 'value',
		event: 'input'
	},
	data() {
		return {
			cursorVisible: false,
			cursorHeight: 35,
			code: '', // 输入的验证码
			codeCursorLeft: [] // 向左移动的距离数组
		};
	},
	created() {
		this.cursorVisible = this.isFocus;
	},
	mounted() {
		this.init();
	},
	methods: {
		/**
		 * @description 初始化
		 */
		init() {
			this.getCodeCursorLeft();
			this.setCursorHeight();
		},
		/**
		 * @description 获取元素节点
		 * @param {string} elm - 节点的id、class 相当于 document.querySelect的参数 -eg: #id
		 * @param {string} type = [single|array] - 单个元素获取多个元素 默认是单个元素
		 * @param {Function} callback - 回调函数
		 */
		getElement(elm, type = 'single', callback) {
			uni
				.createSelectorQuery()
				.in(this)
				[type === 'array' ? 'selectAll' : 'select'](elm)
				.boundingClientRect()
				.exec(data => {
					callback(data[0]);
				});
		},
		/**
		 * @description 计算光标的高度
		 */
		setCursorHeight() {
			this.getElement('.xt__box', 'single', boxElm => {
				this.cursorHeight = boxElm.height * 0.6;
			});
		},
		/**
		 * @description 获取光标在每一个box的left位置
		 */
		getCodeCursorLeft() {
			// 获取父级框的位置信息
			this.getElement('#xt__input-ground', 'single', parentElm => {
				const parentLeft = parentElm.left;
				// 获取各个box信息
				this.getElement('.xt__box', 'array', elms => {
					this.codeCursorLeft = [];
					elms.forEach(elm => {
						this.codeCursorLeft.push(elm.left - parentLeft + elm.width / 2);
					});
				});
			});
		},

		// 输入框输入变化的回调
		input(e) {
			const value = e.detail.value;
			this.cursorVisible = value.length !== this.size;
			this.$emit('input', value);
			this.inputSuccess(value);
		},

		// 输入完成回调
		inputSuccess(value) {
			if (value.length === this.size) {
				this.$emit('confirm', value);
			}
		},
		// 输入聚焦
		inputFocus() {
			this.cursorVisible = this.code.length !== this.size;
		},
		// 输入失去焦点
		inputBlur() {
			this.cursorVisible = false;
		}
	},
	watch: {
		value(val) {
			this.code = val;
		}
	},
	filters: {
		codeFormat(val, isPassword) {
			let value = '';
			if (val) {
				value = isPassword ? '*' : val;
			}
			return value;
		}
	}
};
</script>
<style lang="scss" scoped>
.xt__verify-code {
	position: relative;
	width: 100%;
	box-sizing: border-box;

	.xt__input {
		height: 100%;
		width: 200%;
		position: absolute;
		left: -100%;
		z-index: 1;
	}
	.xt__cursor {
		position: absolute;
		top: 50%;
		transform: translateY(-50%);
		display: inline-block;
		width: 2px;
		animation-name: cursor;
		animation-duration: 0.8s;
		animation-iteration-count: infinite;
	}

	.xt__input-ground {
		display: flex;
		justify-content: space-between;
		align-items: center;
		width: 100%;
		box-sizing: border-box;
		.xt__box {
			position: relative;
			display: inline-block;
			width: 76rpx;
			height: 112rpx;
			&-bottom {
				border-bottom-width: 2px;
				border-bottom-style: solid;
			}

			&-box {
				border-width: 2px;
				border-style: solid;
			}

			&-middle {
				border: none;
			}

			.xt__middle-line {
				position: absolute;
				top: 50%;
				left: 50%;
				width: 50%;
				transform: translate(-50%, -50%);
				border-bottom-width: 2px;
				border-bottom-style: solid;
			}

			.xt__code-text {
				position: absolute;
				top: 50%;
				left: 50%;
				font-size:52rpx;
				transform: translate(-50%, -50%);
			}
		}
	}
}

@keyframes cursor {
	0% {
		opacity: 1;
	}

	100% {
		opacity: 0;
	}
}
</style>
