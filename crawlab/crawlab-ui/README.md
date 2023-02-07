# Crawlab-UI

This is the UI components and modules to support the frontend development
for [Crawlab](https://github.com/crawlab-team/crawlab).

## How to Install

Use `npm` or `yarn` to install `crawlab-ui`.

```
# npm
npm install crawlab-ui -S

# or use yarn
yarn add crawlab-ui -S
```

## How to Use

It is similar to [Element-Plus](https://github.com/element-plus/element-plus), you can import components from Crawlab-UI. Crawlab-UI is built based on Element-Plus so that you can comfortably use it with Element-Plus.

### Use Globally Installed Components

Below is an example of entry file (main.ts) using Crawlab-UI globally.

```ts
import {createApp} from 'vue';
import CrawlabUI from 'crawlab-ui';

const app = createApp(App);
app
  .use(CrawlabUI)  // install globally
  .mount('#app');
```

Below is an example of using globally installed Crawlab-UI in a Vue 3 component.

```vue
<template>
  <cl-form :model="form">
    <cl-form-item :span="2" label="Key" prop="key" required>
      <el-input v-model="form.key"/>
    </cl-form-item>
    <cl-form-item :span="2" label="Value" prop="value" required>
        <el-input v-model="form.value"/>
    </cl-form-item>
  </cl-form>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';

export default defineComponent({
  setup() {
    const form = ref({
      key: 'test-key',
      value: 'test-value',
    });
    return {
      form,
    };
  },
});
</script>
```

### Use Standalone Components

Below is an example of using standalone components in a Vue 3 component.

```vue
<template>
  <cl-form :model="form">
    <cl-form-item :span="2" label="Key" prop="key" required>
      <el-input v-model="form.key"/>
    </cl-form-item>
    <cl-form-item :span="2" label="Value" prop="value" required>
      <el-input v-model="form.value"/>
    </cl-form-item>
  </cl-form>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {ClForm, ClFormItem} from 'crawlab-ui';

export default defineComponent({
  components: {
    ClForm,
    ClFormItem,
  },
  setup() {
    const form = ref({
      key: 'test-key',
      value: 'test-value',
    });
    return {
      form,
    };
  },
});
</script>
```

### Use Web Application

Crawlab-UI has a built-in web application for Crawlab frontend. You can simply use it to start Crawlab frontend Vue 3 SPA.

```ts
// index.ts or index.js or other entry file
import {createApp} from 'crawlab-ui';
createApp();
```

And that's it! After you build or start serving it, you can view the Crawlab frontend web application in the browser.

## Development
