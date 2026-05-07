<!--
  Gallery.vue
  Note:
  - This component was provided as part of the project base
  - This part was not implemented by me.
  - I did not actively work on this page.
-->
<script setup lang="ts">
import { ref, onMounted } from "vue"
import { getImages } from "./http-api"

type Image = {
  id: number
  name: string
}

const images = ref<Image[]>([])

onMounted(async () => {
  images.value = await getImages()
})
</script>

<template>
  <div class="gallery-page">
    <h2>Image Gallery</h2>
    <div class="gallery">
      <div v-for="img in images" :key="img.id" class="image-card">
        <img :src="`/images/${img.id}`" :alt="img.name" />
        <p>{{ img.name }}</p>
      </div>
    </div>
  </div>
</template>

<style>
.gallery-page {
  margin-top: 100px;
}

.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 25px;
  margin-top: 40px;
  padding: 0 40px;
}

.image-card {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transition: transform 0.2s ease;
}

.image-card:hover {
  transform: translateY(-5px);
}

.image-card img {
  width: 100%;
  height: 260px;
  object-fit: cover;
  display: block;
}

.image-card p {
  padding: 12px;
  font-weight: 500;
  font-size: 16px;
}
</style>