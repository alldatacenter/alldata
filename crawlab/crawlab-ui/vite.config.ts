import {resolve} from 'path';
import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import dynamicImport from 'vite-plugin-dynamic-import';

export default defineConfig({
  build: {
    lib: {
      name: 'crawlab-ui',
      entry: resolve(__dirname, 'src/index.ts'),
      fileName: 'crawlab-ui',
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled
      // into your library
      external: [
        'vue',
        // 'vue-router',
        // 'vue-i18n',
        // 'vuex',
        // 'axios',
        // 'element-plus',
        // '@element/icons',
        // '@fortawesome/fontawesome-svg-core',
        // '@fortawesome/free-brands-svg-icons',
        // '@fortawesome/free-regular-svg-icons',
        // '@fortawesome/free-solid-svg-icons',
        // '@fortawesome/vue-fontawesome',
        // 'codemirror',
        // 'echarts',
        // 'atom-material-icons',
      ],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: 'Vue',
          //   'vue-router': 'VueRouter',
          //   'vue-i18n': 'VueI18n',
          //   vuex: 'Vuex',
          //   axios: 'axios',
          // 'element-plus': 'ElementPlus',
          //   '@element/icons': 'ElementIcons',
          //   '@fortawesome/fontawesome-svg-core': 'FontAwesomeSvgCore',
          //   '@fortawesome/free-brands-svg-icons': 'FontAwesomeBrandsSvgIcons',
          //   '@fortawesome/free-regular-svg-icons': 'FontAwesomeRegularSvgIcons',
          //   '@fortawesome/free-solid-svg-icons': 'FontAwesomeSolidSvgIcons',
          //   '@fortawesome/vue-fontawesome': 'FontAwesomeVue',
          //   codemirror: 'CodeMirror',
          //   echarts: 'echarts',
          //   'atom-material-icons': 'AtomMaterialIcons',
        }
      }
    },
  },
  resolve: {
    alias: [
      {find: '@', replacement: resolve(__dirname, 'src')},
      {find: 'vue', replacement: resolve('./node_modules/vue/dist/vue.esm-bundler.js')},
      // {find: 'vue-router', replacement: 'vue-router/dist/vue-router.esm-bundler.js'},
      // {find: 'vue-i18n', replacement: 'vue-i18n/dist/vue-i18n.esm-bundler.js'},
      // {find: 'vuex', replacement: 'vuex/dist/vuex.global.prod.js'},
      // {find: 'axios', replacement: 'axios/dist/axios.min.js'},
      // {find: 'element-plus$', replacement: 'element-plus/dist/index.full.min.mjs'},
      // {find: '@element/icons', replacement: 'element-plus/lib/components/base/icon.js'},
      // {find: 'codemirror', replacement: 'codemirror/lib/codemirror.js'},
      // {find: 'echarts', replacement: 'echarts/dist/echarts.min.js'},
      // {find: 'atom-material-icons', replacement: 'atom-material-icons/lib/atom-material-icons.js'},
    ],
    extensions: [
      '.js',
      '.ts',
      '.jsx',
      '.tsx',
      '.json',
      '.vue',
      '.scss',
    ]
  },
  plugins: [
    vue(),
    dynamicImport(),
  ],
  server: {
    cors: true,
  },
});
