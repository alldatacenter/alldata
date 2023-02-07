import {createApp} from 'vue';
import CrawlabUI from 'crawlab-ui';
import 'crawlab-ui/dist/crawlab-ui.css';
import App from '@/ComponentExample.vue';

// console.log(CrawlabUI);

const app = createApp(App);
app.use(CrawlabUI)
  .mount('#app');
