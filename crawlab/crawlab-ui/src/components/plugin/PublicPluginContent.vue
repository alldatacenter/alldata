<template>
  <div class="public-plugin-content">
    <div class="top">
      <cl-public-plugin-item
        :plugin="plugin"
        :status="status"
        :installed="installed"
        @install="() => $emit('install')"
      />
    </div>
    <div class="content">
      <div class="info">
        <span class="label">
          {{ t('components.plugin.install.author') }}:
        </span>
        <span class="value">
          <a :href="info.repo?.owner?.html_url" target="_blank">
            {{ info.repo?.owner?.login }}
          </a>
        </span>
      </div>
      <div class="info">
        <span class="label">
          {{ t('components.plugin.install.repoUrl') }}:
        </span>
        <span class="value">
          <a :href="info.repo?.html_url" target="_blank">
            {{ info.repo?.html_url }}
          </a>
        </span>
      </div>
      <div class="info">
        <span class="label">
          {{ t('components.plugin.install.pushedAt') }}:
        </span>
        <span class="value">
          <cl-time :time="info.repo?.pushed_at"/>
        </span>
      </div>
      <div class="info">
        <span class="label">
          {{ t('components.plugin.install.updatedAt') }}:
        </span>
        <span class="value">
          <cl-time :time="info.repo?.updated_at"/>
        </span>
      </div>
      <div class="readme" v-html="readme"/>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {useI18n} from 'vue-i18n';
import {Converter} from 'showdown';

export default defineComponent({
  name: 'PublicPluginContent',
  props: {
    plugin: {
      type: Object as PropType<PublicPlugin>,
    },
    info: {
      type: Object as PropType<PublicPluginInfo>,
    },
    status: {
      type: String,
    },
    installed: {
      type: Boolean,
      default: false,
    },
  },
  emits: [
    'install',
  ],
  setup(props: PublicPluginContentProps, {emit}) {
    // i18n
    const {t} = useI18n();

    // markdown-to-text converter
    const converter = new Converter();

    const readme = computed<string>(() => {
      return converter.makeHtml(props.info?.readme || '');
    });

    return {
      t,
      readme,
    };
  },
});
</script>

<style scoped lang="scss">
.public-plugin-content {
  .top {
    height: 100px;
  }

  .content {
    width: 100%;
    padding: 10px 20px;

    .readme {
    }

    .info {
      margin-bottom: 10px;
      font-size: 16px;
      display: flex;
      align-items: center;
      width: 100%;

      .label {
        font-weight: 500;
        flex: 0 0 120px;
      }

      .value {
        flex: 1 0 auto;

        a {
          color: var(--cl-primary-color);

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }
  }
}
</style>
