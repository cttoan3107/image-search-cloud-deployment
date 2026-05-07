/*
  router.ts

  Vue Router configuration file.

  Responsibilities:
  - Define all application routes (pages)
  - Map URL paths to Vue components
  - Handle navigation between pages
  - Enable dynamic routing (e.g., image detail with ID)
*/

import { createRouter, createWebHashHistory } from "vue-router"

/* Import page components */
import Home from "./components/Home.vue"
import ImageGallery from "./components/ImageGallery.vue"
import ImageDetail from "./components/ImageDetail.vue"

/* Define all routes of the application */
const routes = [
  {
    /* Home page route */
    path: "/",
    name: "home",
    component: Home
  },
  {
    /* Gallery page route (list of images) */
    path: "/gallery",
    name: "gallery",
    component: ImageGallery
  },
  {
    /* Image detail page with dynamic parameter (id) */
    path: "/image/:id",
    name: "image-detail",
    component: ImageDetail,
    props: true
  }
]

/* Create router instance */
const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router