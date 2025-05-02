<template>
  <div>
    <svg-icon icon-class="doc" @click="click" />
  </div>
</template>

<script>
export default {
  name: 'Doc',
  methods: {
    click() {
      window.open('https://alldata-document.readthedocs.io/zh/latest/README.html/', '_blank')
    }
  }
}
</script>
