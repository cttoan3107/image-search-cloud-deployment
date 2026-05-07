/*
  Global role of this file:

  - Entry point of the Vue application
  - Creates and mounts the app instance
  - Registers the router
*/
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router';

createApp(App).use(router).mount('#app')
