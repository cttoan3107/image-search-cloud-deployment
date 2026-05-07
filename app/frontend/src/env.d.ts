/*
  Global role of this file:
  - Provides TypeScript declarations for Vue files (*.vue)
  - Allows importing Vue components without type errors
*/

/// <reference types="vite/client" />

declare module "*.vue" {
  import { DefineComponent } from "vue"
  const component: DefineComponent<{}, {}, any>
  export default component
}