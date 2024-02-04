<template>
  <component :is="borderBox" :title="borderTitle" :width="Number(borderStyle.width)" :height="Number(borderStyle.height)">
    <slot />
  </component>
</template>

<script>
import BorderBox0 from './borders/BorderBox0'
import BorderBox1 from './borders/BorderBox1'
import BorderBox2 from './borders/BorderBox2'
import BorderBox3 from './borders/BorderBox3'
import BorderBox4 from './borders/BorderBox4'
import BorderBox4Reverse from './borders/BorderBox4Reverse'
import BorderBox5 from './borders/BorderBox5'
import BorderBox5Reverse from './borders/BorderBox5Reverse'
import BorderBox6 from './borders/BorderBox6'
import BorderBox7 from './borders/BorderBox7'
import BorderBox8 from './borders/BorderBox8'
import BorderBox8Reverse from './borders/BorderBox8Reverse'
import BorderBox9 from './borders/BorderBox9'
import BorderBox10 from './borders/BorderBox10'
import BorderBox11 from './borders/BorderBox11'
import BorderBox12 from './borders/BorderBox12'
import BorderBox13 from './borders/BorderBox13'

export default {
  name: 'ScreenBorder',
  components: {
    BorderBox0,
    BorderBox1,
    BorderBox2,
    BorderBox3,
    BorderBox4, BorderBox4Reverse,
    BorderBox5, BorderBox5Reverse,
    BorderBox6,
    BorderBox7,
    BorderBox8, BorderBox8Reverse,
    BorderBox9,
    BorderBox10,
    BorderBox11,
    BorderBox12,
    BorderBox13
  },
  props: {
    borderBox: {
      type: String,
      required: true,
      default: 'BorderBox0'
    },
    borderTitle: {
      type: String,
      default: ''
    },
    borderStyle: {
      type: Object,
      default: () => {
        return {
          width: '0',
          height: '0'
        }
      }
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
