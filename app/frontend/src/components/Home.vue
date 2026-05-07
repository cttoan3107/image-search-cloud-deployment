<!--
  Home.vue

  Home page component of the application.

  Responsibilities:
  - Allow users to select and upload an image file
  - Communicate with backend via upload API
  - Display success or error messages after upload
  - Provide navigation to the gallery after successful upload
  - Present a styled landing page with visual composition
-->

<script setup lang="ts">
import { ref } from "vue"
import { uploadImage } from "./http-api"
import { useRouter } from "vue-router"

import centerImg from "./home-pic/center.jpg"
import codeImg from "./home-pic/code.jpg"
import ordiImg from "./home-pic/ordi.jpg"

const file = ref<File | null>(null)
const message = ref("")
const success = ref(false)
const showGalleryLink = ref(false)
const router = useRouter()

/* Handle file selection */
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  file.value = target.files?.[0] ?? null
}

/* Handle image upload */
const uploadFileHandler = async () => {
  if (!file.value) {
    message.value = "Select a file first."
    success.value = false
    return
  }

  try {
    await uploadImage(file.value)
    message.value = "Upload complete."
    success.value = true
    showGalleryLink.value = true
  } catch (error) {
    message.value = "Upload failed. Please use JPEG or PNG."
    success.value = false
  }
}
</script>

<template>
  <!-- Main homepage section -->
  <section class="home-page">
    <!-- Decorative ambient background effects -->
    <div class="ambient ambient-1"></div>
    <div class="ambient ambient-2"></div>
    <div class="ambient ambient-3"></div>

    <!-- Main page layout: text content + visual composition -->
    <div class="home-layout">
      <!-- Left content block -->
      <div class="home-copy">
        <!-- Section label -->
        <p class="section-label">IMAGE LIBRARY</p>

        <!-- Main title -->
        <h1 class="home-title">
          Upload your images
        </h1>

        <!-- Short project description -->
        <p class="home-description">
          Upload images to the system and explore them in the gallery. You can also search and find similar images.     
        </p>

        <!-- Upload form -->
        <div class="upload-panel">
          <!-- Custom file picker -->
          <label class="file-picker">
            <input type="file" @change="handleFileChange" />
            <span>{{ file ? file.name : "Choose file" }}</span>
          </label>

          <!-- Upload button -->
          <button class="upload-button" @click="uploadFileHandler">
            Upload
          </button>
        </div>

        <!-- Upload feedback message -->
        <p v-if="message" :class="success ? 'message-success' : 'message-error'">
          {{ message }}
        </p>

        <!-- Gallery navigation shown only after successful upload -->
        <p v-if="showGalleryLink" class="gallery-link">
          Archive updated.
          <a @click="router.push('/gallery')">Enter gallery</a>
        </p>
      </div>

      <!-- Right visual block -->
      <div class="home-visual">
        <div class="composition">
          <!-- Main decorative image -->
          <figure class="visual-card visual-main tilt-left">
            <img :src="centerImg" alt="Center visual" />
          </figure>

          <!-- Top decorative image -->
          <figure class="visual-card visual-top tilt-right">
            <img :src="codeImg" alt="Code visual" />
          </figure>

          <!-- Bottom decorative image -->
          <figure class="visual-card visual-bottom tilt-right">
            <img :src="ordiImg" alt="Retro computer visual" />
          </figure>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* Main homepage container */
.home-page {
  position: relative;
  width: 100%;
  min-height: calc(100vh - 76px);
  overflow: hidden;
  padding: 48px 0 64px;
}

/* Main two-column layout */
.home-layout {
  position: relative;
  z-index: 2;
  width: min(1340px, calc(100% - 64px));
  margin: 0 auto;
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 56px;
  align-items: center;
}

/* Left text/content container */
.home-copy {
  max-width: 520px;
}

/* Small section label */
.section-label {
  margin-bottom: 18px;
  color: #78f6ee;
  font-size: 0.78rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  opacity: 0.88;
}

/* Main title styling */
.home-title {
  margin-bottom: 22px;
  font-size: clamp(3rem, 6vw, 6.2rem);
  line-height: 0.92;
  font-weight: 800;
  letter-spacing: -0.05em;
  color: #ebffff;
  text-transform: uppercase;
  text-shadow:
    0 0 24px rgba(0, 255, 231, 0.12),
    0 0 60px rgba(124, 92, 255, 0.08);
}

/* Description text */
.home-description {
  max-width: 460px;
  margin-bottom: 34px;
  font-size: 1.05rem;
  line-height: 1.8;
  color: rgba(214, 241, 255, 0.72);
}

/* Upload controls container */
.upload-panel {
  display: flex;
  gap: 12px;
  align-items: stretch;
  max-width: 470px;
}

/* Custom file input wrapper */
.file-picker {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  min-height: 56px;
  padding: 0 18px;
  border-radius: 14px;
  border: 1px solid rgba(120, 246, 238, 0.2);
  background: rgba(8, 17, 30, 0.82);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.02) inset,
    0 0 24px rgba(0, 255, 231, 0.06);
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

/* Hover effect for file picker */
.file-picker:hover {
  border-color: rgba(120, 246, 238, 0.45);
  transform: translateY(-1px);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.03) inset,
    0 0 26px rgba(0, 255, 231, 0.12);
}

/* Invisible native file input */
.file-picker input[type="file"] {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

/* Selected file name display */
.file-picker span {
  display: block;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: rgba(221, 253, 255, 0.82);
  font-size: 0.95rem;
}

/* Upload button */
.upload-button {
  min-width: 132px;
  min-height: 56px;
  border-radius: 14px;
  border: 1px solid rgba(111, 255, 245, 0.4);
  background:
    linear-gradient(135deg, rgba(17, 25, 47, 0.96), rgba(9, 44, 57, 0.96));
  color: #cffffd;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  box-shadow:
    0 0 20px rgba(0, 255, 231, 0.12),
    0 0 40px rgba(124, 92, 255, 0.08);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

/* Hover effect for upload button */
.upload-button:hover {
  transform: translateY(-1px);
  border-color: rgba(111, 255, 245, 0.7);
  box-shadow:
    0 0 28px rgba(0, 255, 231, 0.16),
    0 0 48px rgba(124, 92, 255, 0.1);
}

/* Shared message styles */
.message-success,
.message-error {
  margin-top: 18px;
  font-size: 0.95rem;
  letter-spacing: 0.02em;
}

/* Success message color */
.message-success {
  color: #7effb8;
}

/* Error message color */
.message-error {
  color: #ff7c9b;
}

/* Gallery link container */
.gallery-link {
  margin-top: 10px;
  color: rgba(213, 239, 255, 0.66);
  font-size: 0.95rem;
}

/* Gallery link styling */
.gallery-link a {
  margin-left: 8px;
  color: #8cfbff;
  cursor: pointer;
  text-decoration: none;
  border-bottom: 1px solid rgba(140, 251, 255, 0.3);
}

/* Gallery link hover effect */
.gallery-link a:hover {
  color: #ffffff;
  border-bottom-color: rgba(255, 255, 255, 0.7);
}

/* Right visual container */
.home-visual {
  display: flex;
  justify-content: center;
}

/* Decorative image composition wrapper */
.composition {
  position: relative;
  width: min(100%, 760px);
  height: 760px;
}

/* Shared style for decorative cards */
.visual-card {
  position: absolute;
  overflow: hidden;
  background: #09111c;
  border: 1px solid rgba(134, 255, 245, 0.16);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.36),
    0 0 36px rgba(0, 255, 231, 0.06);
}

/* Image inside decorative cards */
.visual-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: contrast(1.05) saturate(1.08) brightness(0.86);
}

/* Main central image */
.visual-main {
  width: 56%;
  height: 80%;
  left: 18%;
  top: 8%;
  border-radius: 32px;
}

/* Top image */
.visual-top {
  width: 32%;
  height: 34%;
  left: 0;
  top: 2%;
  border-radius: 24px;
}

/* Bottom image */
.visual-bottom {
  width: 38%;
  height: 26%;
  right: 0;
  bottom: 2%;
  border-radius: 24px;
}

/* Rotation effect */
.tilt-left {
  transform: rotate(-5deg);
}

/* Rotation effect */
.tilt-right {
  transform: rotate(6deg);
}

/* Decorative scan line effect */
.scan-line {
  position: absolute;
  left: 8%;
  right: 8%;
  top: 52%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(127, 255, 247, 0.9), transparent);
  box-shadow:
    0 0 18px rgba(127, 255, 247, 0.5),
    0 0 40px rgba(127, 255, 247, 0.16);
  opacity: 0.6;
}

/* Shared ambient blur effect */
.ambient {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(90px);
  opacity: 0.55;
}

/* Ambient light 1 */
.ambient-1 {
  width: 360px;
  height: 360px;
  top: 4%;
  left: -40px;
  background: rgba(0, 255, 231, 0.12);
}

/* Ambient light 2 */
.ambient-2 {
  width: 320px;
  height: 320px;
  right: 8%;
  top: 12%;
  background: rgba(124, 92, 255, 0.12);
}

/* Ambient light 3 */
.ambient-3 {
  width: 420px;
  height: 420px;
  right: -80px;
  bottom: -40px;
  background: rgba(0, 255, 231, 0.08);
}

/* Tablet and medium screens */
@media (max-width: 1024px) {
  .home-layout {
    width: min(100%, calc(100% - 40px));
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .home-copy {
    max-width: 100%;
  }

  .home-description {
    max-width: 620px;
  }

  .composition {
    width: 100%;
    max-width: 720px;
    height: 620px;
    margin: 0 auto;
  }
}

/* Mobile layout */
@media (max-width: 640px) {
  .home-page {
    padding: 32px 0 48px;
  }

  .home-layout {
    width: min(100%, calc(100% - 24px));
    gap: 30px;
  }

  .home-title {
    font-size: 3rem;
  }

  .home-description {
    font-size: 1rem;
    line-height: 1.7;
  }

  .upload-panel {
    flex-direction: column;
    max-width: 100%;
  }

  .upload-button {
    width: 100%;
  }

  .composition {
    height: 440px;
  }

  .visual-main {
    width: 58%;
    height: 76%;
    left: 18%;
    top: 10%;
  }

  .visual-top {
    width: 34%;
    height: 28%;
  }

  .visual-bottom {
    width: 42%;
    height: 22%;
  }
}
</style>