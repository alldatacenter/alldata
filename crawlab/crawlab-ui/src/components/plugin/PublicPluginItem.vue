<template>
  <div
    class="public-plugin-item"
    :class="[
        active ? 'active' : '',
        clickable ? 'clickable' : '',
      ]"
    @click="onClick"
  >
    <div class="container">
      <div class="image-wrapper">
        <img class="image" :src="defaultLogo"/>
      </div>
      <div class="content-wrapper">
        <div class="content">
          <div class="name">{{ plugin?.name }}</div>
          <div class="description">{{ plugin?.description }}</div>
        </div>
      </div>
      <div class="actions-wrapper">
        <cl-label-button
          class-name="install-btn"
          :type="type"
          :icon="icon"
          :loading="loading"
          :label="label"
          :disabled="disabled"
          @click="onInstall"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import defaultLogo from '@/assets/svg/logo';
import {useI18n} from 'vue-i18n';
import {
  PLUGIN_STATUS_INSTALLING,
} from '@/constants/plugin';

export default defineComponent({
  name: 'PublicPluginItem',
  props: {
    plugin: {
      type: Object as PropType<PublicPlugin>,
    },
    active: {
      type: Boolean,
      default: false,
    },
    clickable: {
      type: Boolean,
      default: false,
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
    'click',
    'install',
  ],
  setup(props: PublicPluginItemProps, {emit}) {
    const {t} = useI18n();

    const type = computed<string>(() => {
      const {status, installed} = props;
      if (installed) {
        return 'success';
      } else if (status === PLUGIN_STATUS_INSTALLING) {
        return 'warning';
      } else {
        return 'primary';
      }
    });

    const icon = computed<Icon | undefined>(() => {
      const {status, installed} = props;
      if (installed) {
        return ['fa', 'check'];
      } else if (status === PLUGIN_STATUS_INSTALLING) {
        return;
      } else {
        return ['fa', 'download'];
      }
    });

    const loading = computed<boolean>(() => {
      const {status} = props;
      return status === PLUGIN_STATUS_INSTALLING;
    });

    const label = computed<string>(() => {
      const {status, installed} = props;
      if (installed) {
        return t('components.plugin.status.label.installed');
      } else if (status === PLUGIN_STATUS_INSTALLING) {
        return t('components.plugin.status.label.installing');
      } else {
        return t('common.actions.install');
      }
    });

    const disabled = computed<boolean>(() => {
      const {status, installed} = props;
      if (installed) {
        return true;
      } else if (status === PLUGIN_STATUS_INSTALLING) {
        return true;
      } else {
        return false;
      }
    });

    const onClick = () => {
      const {clickable} = props;
      if (!clickable) return;
      emit('click');
    };

    const onInstall = () => {
      emit('install');
    };

    return {
      type,
      icon,
      loading,
      label,
      disabled,
      onClick,
      onInstall,
      defaultLogo,
      t,
    };
  },
});
</script>

<style scoped lang="scss">
.public-plugin-item {
  width: 100%;
  height: 100px;
  padding: 10px 10px 20px 10px;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: rgba(64, 158, 255, 0.1);
    }

    &.active {
      background: rgba(64, 158, 255, 0.1);
    }
  }

  .container {
    display: flex;
    align-items: center;
    height: 100%;
    width: 100%;

    .image-wrapper {
      flex: 0 0 60px;
      height: 100%;
      display: flex;
      align-items: center;

      .image {
        width: 100%;
      }
    }

    .content-wrapper {
      flex: 1 0 auto;
      width: calc(100% - 60px - 120px - 40px);
      height: 100%;
      margin-left: 20px;
      display: flex;

      .content {
        width: 100%;

        .name {
          font-size: 20px;
          line-height: 160%;
          font-weight: 500;
          color: var(--cl-info-color);
        }

        .description {
          color: var(--cl-info-medium-color);
          width: 100%;
          word-break: break-word;
        }
      }
    }

    .actions-wrapper {
      width: 120px;
      margin-left: 20px;
      flex: 0 0 80px;
      align-items: center;
      justify-content: center;
    }
  }
}
</style>
