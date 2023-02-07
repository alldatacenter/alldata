<template>
  <DemoLayout active-name="basic">
    <Form>
      <cl-form-item
        :span="4"
        label="Form"
      >
        {{ form }}
      </cl-form-item>
      <cl-form-item
        :span="2"
        :offset="2"
        label="Names"
      >
        <InputList
          v-model="form.names"
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :offset="2"
        label="Ids"
      >
        <InputList
          v-model="form.ids"
          disabled
        />
      </cl-form-item>
      <cl-form-item
        :span="2"
        :offset="2"
        label="Empty"
      >
        <InputList
          v-model="form.empty"
        />
      </cl-form-item>
    </Form>
  </DemoLayout>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';

export default defineComponent({
  name: 'DemoInputList',
  setup() {
    const form = ref({
      names: [
        'Alpha',
        'Beta',
        'Gamma',
      ],
      ids: [
        '1',
        '2',
        '3,'
      ]
    });

    return {
      form,
    };
  }
});
</script>

<style lang="scss" scoped>
.form {
  margin: 20px;
}
</style>
