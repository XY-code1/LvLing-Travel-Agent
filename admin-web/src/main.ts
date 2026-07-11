import 'element-plus/dist/index.css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import './styles/index.scss';

import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import { createPinia } from 'pinia';
import { createApp } from 'vue';

import App from './App.vue';
import router from './router';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus, { locale: zhCn });
app.mount('#app');
