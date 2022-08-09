<template>
	<default-page v-if="type" :type="type" title="您的设备已断网" :isBtn="false" />
</template>

<script>
	import defaultPage from '@/components/default-page/default-page.vue';
	export default {
		components: {
			defaultPage
		},
		data() {
			return {
				type: undefined
			}
		},
		onLoad(options) {
			
		}
	}
</script>

<style>

</style>
