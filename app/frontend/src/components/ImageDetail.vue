<!--
  ImageDetail.vue

  Image detail page component of the application.

  Responsibilities:
  - Display a selected image and its metadata
  - Allow the user to rename the image
  - Allow keyword management (add / delete)
  - Allow user reactions (like / dislike)
  - Download the current image
  - Load and display similar images
  - React to route changes when the user opens another image
-->

<script setup lang="ts">
/* Import Vue Composition API utilities */
import { onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  addKeyword as addKeywordApi,
  deleteKeyword as deleteKeywordApi,
  findSimilarImages,
  getMetadata,
  getImageReaction,
  likeImage,
  renameImage,
  dislikeImage,
  type Metadata,
  type SimilarImage
} from "./http-api"

const route = useRoute()
const router = useRouter()
const newName = ref("")
const id = ref(Number(route.params.id))
const metadata = ref<Metadata | null>(null)
const similarImages = ref<SimilarImage[]>([])
const newKeyword = ref("")
const pageError = ref("")
const descriptor = ref("gradient")
const k = ref(5)
const loadingSimilar = ref(false)
const reaction = ref<"LIKE" | "DISLIKE" | null>(null)
const getImageSrc = (imageId: number) => `/images/${imageId}`

/*
  Download the current image.
  Steps:
  - Fetch the image from the backend
  - Convert response to blob
  - Create a temporary browser URL
  - Create a temporary <a> tag
  - Trigger automatic download
  - Clean up generated resources
*/
async function downloadImage() {
  try {
    const response = await fetch(getImageSrc(id.value))
    if (!response.ok) {
      throw new Error("Download failed")
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement("a")

    /* Determine extension based on metadata type */
    const extension =
      metadata.value?.type?.toLowerCase().includes("png") ? "png" : "jpg"

    link.href = url
    link.download = metadata.value?.name || `image-${id.value}.${extension}`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    pageError.value = "Unable to download image."
  }
}

/*
  Load image metadata from the backend.
  On success:
  - Store metadata
  On failure:
  - Show error message
  - Reset metadata
*/
async function loadMetadata() {
  try {
    pageError.value = ""
    metadata.value = await getMetadata(id.value)
  } catch (error) {
    pageError.value = "Unable to load image metadata."
    metadata.value = null
  }
}

/*
  Load similar images from the backend.
  Uses:
  - current image ID
  - selected descriptor
  - selected number of results
*/
async function loadSimilarImages() {
  try {
    loadingSimilar.value = true
    pageError.value = ""
    similarImages.value = await findSimilarImages(id.value, descriptor.value, k.value)
  } catch (error) {
    pageError.value = "Unable to load similar images."
    similarImages.value = []
  } finally {
    loadingSimilar.value = false
  }
}

/*
  Add a new keyword to the current image.
  Steps:
  - Trim input
  - Ignore empty input
  - Call backend API
  - Reload metadata to refresh displayed keyword list
*/
async function addKeyword() {
  const trimmed = newKeyword.value.trim()
  if (!trimmed) return

  try {
    await addKeywordApi(id.value, trimmed)
    newKeyword.value = ""
    await loadMetadata()
  } catch (error) {
    pageError.value = "Unable to add keyword."
  }
}

/*
  Delete a keyword from the current image.
  After deletion:
  - Reload metadata to refresh displayed keyword list
*/
async function deleteKeyword(keyword: string) {
  try {
    await deleteKeywordApi(id.value, keyword)
    await loadMetadata()
  } catch (error) {
    pageError.value = "Unable to delete keyword."
  }
}

/*
  Rename the current image.
  Steps:
  - Trim the new name
  - Ignore empty input
  - Call backend rename API
  - Reload metadata to reflect the new name
  - Clear input field
*/
async function handleRename() {
  const trimmed = newName.value.trim()
  if (!trimmed) return

  try {
    pageError.value = ""
    await renameImage(id.value, trimmed)

    await loadMetadata()
    newName.value = ""
  } catch (error) {
    pageError.value = "Unable to rename image."
  }
}

/*
  Open another image detail page.
  Used when clicking on a similar image card.
*/
function openSimilarImage(imageId: number) {
  router.push(`/image/${imageId}`)
}

/*
  Load the current reaction for this image.
  - LIKE
  - DISLIKE
  - null
*/
async function loadReaction() {
  try {
    const data = await getImageReaction(id.value)
    reaction.value = data.reaction
  } catch (error) {
    pageError.value = "Unable to load reaction."
    reaction.value = null
  }
}

/* Send a "like" reaction, then reload current reaction state.*/
async function handleLike() {
  try {
    await likeImage(id.value)
    await loadReaction()
  } catch (error) {
    pageError.value = "Unable to like image."
  }
}

/* Send a "dislike" reaction, then reload current reaction state.*/
async function handleDislike() {
  try {
    await dislikeImage(id.value)
    await loadReaction()
  } catch (error) {
    pageError.value = "Unable to dislike image."
  }
}

/*
  Initialize all page data.
  Loads in parallel:
  - metadata
  - reaction
  - similar images
*/
async function initializePage() {
  await Promise.all([
    loadMetadata(),
    loadReaction(),
    loadSimilarImages()
  ])
}

/*
  Watch the route image ID.
  Purpose:
  - When user clicks a similar image and route changes,
    reload all page data for the new image
*/
watch(
  () => route.params.id,
  async (newId) => {
    id.value = Number(newId)
    await initializePage()
  }
)

/* Load all page data when component is first mounted.*/
onMounted(async () => {
  await initializePage()
})
</script>

<template>
  <!-- Main detail page container -->
  <section class="detail-page">
    <div class="detail-shell">

      <!-- Button to return to gallery page -->
      <button class="back-link" @click="router.push('/gallery')">
        ← Back to gallery
      </button>

      <!-- Global page error display -->
      <p v-if="pageError" class="error-text">
        {{ pageError }}
      </p>

      <!-- Main two-column layout:
           left = image
           right = metadata and controls -->
      <div class="detail-grid">
        <div class="image-panel">
          <div class="image-frame">
            <!-- Main displayed image -->
            <img
              :src="getImageSrc(id)"
              :alt="metadata?.name || `Image ${id}`"
              class="main-image"
            />
          </div>
        </div>

        <div class="info-panel">
          <p class="section-label">IMAGE DETAILS</p>

          <!-- Title + download button -->
          <div class="title-row">
            <h1 class="detail-title">
              {{ metadata?.name || `Image #${id}` }}
            </h1>

            <button
              class="icon-btn small"
              @click="downloadImage"
              aria-label="Download image"
              title="Download image"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.6"
                stroke-linecap="round"
                stroke-linejoin="round"
                class="download-svg"
              >
                <path d="M12 3v12" />
                <path d="M7 10l5 5 5-5" />
                <path d="M5 21h14" />
              </svg>
            </button>
          </div>

          <p class="detail-description">
            View metadata, manage keywords, and explore similar images.
          </p>

          <!-- Metadata display block -->
          <div class="meta-block">
            <div class="meta-row">
              <span>Name</span>
              <strong>{{ metadata?.name || "—" }}</strong>
            </div>

            <div class="meta-row">
              <span>Type</span>
              <strong>{{ metadata?.type || "—" }}</strong>
            </div>

            <div class="meta-row">
              <span>Size</span>
              <strong>{{ metadata?.size || "—" }}</strong>
            </div>

            <div class="meta-row">
              <span>ID</span>
              <strong>{{ id }}</strong>
            </div>
          </div>

          <!-- Rename image section -->
          <div class="rename-section">
            <h2>Rename Image</h2>

            <div class="rename-form">
              <input
                v-model="newName"
                type="text"
                placeholder="Enter new file name (e.g. cat.jpg)"
                @keyup.enter="handleRename"
              />

              <button class="primary-button" @click="handleRename">
                Rename
              </button>
            </div>
          </div>

          <!-- Reaction section -->
          <div class="reaction-section">
            <button
              @click="handleLike"
              :class="{ active: reaction === 'LIKE' }"
            >
              ❤️ Like
            </button>

            <button
              @click="handleDislike"
              :class="{ active: reaction === 'DISLIKE' }"
            >
              👎 Dislike
            </button>
          </div>

          <!-- Keyword management section -->
          <div class="keyword-section">
            <div class="panel-head">
              <h2>Keywords</h2>
            </div>

            <!-- Existing keywords -->
            <div v-if="metadata?.keywords?.length" class="keywords">
              <div
                v-for="keyword in metadata.keywords"
                :key="keyword"
                class="keyword-chip"
              >
                <span>{{ keyword }}</span>

                <!-- Delete keyword button -->
                <button class="chip-delete" @click="deleteKeyword(keyword)">
                  ×
                </button>
              </div>
            </div>

            <!-- Message if there are no keywords -->
            <p v-else class="soft-text">No keywords available.</p>

            <!-- Add keyword form -->
            <div class="keyword-form">
              <input
                v-model="newKeyword"
                type="text"
                placeholder="Add a keyword"
                @keyup.enter="addKeyword"
              />
              <button class="primary-button" @click="addKeyword">
                Add
              </button>
            </div>
          </div>

          <!-- Controls for similar image search -->
          <div class="similar-controls">
            <div class="field-inline">
              <label for="descriptor">Descriptor</label>
              <select id="descriptor" v-model="descriptor">
                <option value="gradient">Gradient</option>
                <option value="hs">HS</option>
                <option value="rgb">RGB</option>
              </select>
            </div>

            <div class="field-inline small-field">
              <label for="k">Results</label>
              <input id="k" v-model.number="k" type="number" min="1" />
            </div>

            <button class="secondary-button" @click="loadSimilarImages">
              Refresh similar
            </button>
          </div>
        </div>
      </div>

      <!-- Similar images section -->
      <div class="similar-section">
        <div class="similar-head">
          <h2>Similar Images</h2>
          <p>Open a related image to continue browsing.</p>
        </div>

        <!-- Loading state wait utill result -->
        <p v-if="loadingSimilar" class="soft-text similar-empty">
          Loading similar images...
        </p>

        <!-- Similar results -->
        <div v-else-if="similarImages.length" class="similar-grid">
          <article
            v-for="img in similarImages"
            :key="img.id"
            class="similar-card"
            @click="openSimilarImage(img.id)"
          >
            <div class="similar-image-wrap">
              <img
                :src="getImageSrc(img.id)"
                :alt="img.name || `Similar image ${img.id}`"
              />
            </div>

            <div class="similar-meta">
              <p class="similar-name">
                {{ img.name || `Image #${img.id}` }}
              </p>
              <p class="similar-score">
                Score: {{ img.score.toFixed(3) }}
              </p>
            </div>
          </article>
        </div>

        <!-- Empty state -->
        <p v-else class="soft-text similar-empty">
          No similar images found.
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* Main page container */
.detail-page {
  width: 100%;
  min-height: calc(100vh - 76px);
  padding: 36px 0 56px;
}

/* Main centered wrapper */
.detail-shell {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
}

/* Back navigation button */
.back-link {
  margin-bottom: 20px;
  padding: 0;
  border: none;
  background: transparent;
  color: #8cfbff;
  font-size: 0.95rem;
  letter-spacing: 0.04em;
}

/* Error message */
.error-text {
  margin-bottom: 16px;
  color: #ff7c9b;
}

/* Two-column page layout */
.detail-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 28px;
  align-items: start;
}

/* Title row with action button */
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Generic icon button */
.icon-btn {
  padding: 0;
  border-radius: 50%;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(8, 17, 30, 0.82);
  color: #eaffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

/* Hover effect for icon button */
.icon-btn:hover {
  transform: translateY(-2px);
  border-color: rgba(111, 255, 245, 0.55);
  background: rgba(255, 255, 255, 0.04);
}

/* Small circular icon button size */
.icon-btn.small {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

/* Download icon size */
.download-svg {
  width: 20px;
  height: 20px;
}

/* Shared card styles */
.image-frame,
.info-panel,
.similar-section {
  border-radius: 26px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(8, 17, 30, 0.82);
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.26),
    0 0 30px rgba(0, 255, 231, 0.04);
}

/* Main image wrapper */
.image-frame {
  padding: 18px;
}

/* Main image style */
.main-image {
  width: 100%;
  display: block;
  border-radius: 18px;
  object-fit: cover;
  filter: brightness(0.93) saturate(1.05);
}

/* Right information panel */
.info-panel {
  padding: 24px;
}

/* Small section label */
.section-label {
  margin-bottom: 12px;
  color: #78f6ee;
  font-size: 0.78rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

/* Main page title */
.detail-title {
  margin-bottom: 0;
  font-size: clamp(2rem, 3.4vw, 3rem);
  line-height: 0.98;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #ebffff;
  text-transform: uppercase;
}

/* Supporting description text */
.detail-description {
  margin-top: 12px;
  margin-bottom: 24px;
  color: rgba(214, 241, 255, 0.72);
  line-height: 1.7;
}

/* Metadata block layout */
.meta-block {
  display: grid;
  gap: 12px;
  margin-bottom: 24px;
}

/* Single metadata row */
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 48px;
  padding: 0 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(120, 246, 238, 0.08);
}

/* Metadata label */
.meta-row span {
  color: rgba(214, 241, 255, 0.62);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.82rem;
}

/* Metadata value */
.meta-row strong {
  color: #eaffff;
  font-weight: 700;
  text-align: right;
  word-break: break-word;
}

/* Keyword section spacing */
.keyword-section {
  margin-bottom: 24px;
}

/* Section titles */
.panel-head h2,
.similar-head h2 {
  margin-bottom: 12px;
  color: #ebffff;
  font-size: 1.2rem;
  font-weight: 700;
}

/* Keywords list layout */
.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

/* Single keyword chip */
.keyword-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(255, 255, 255, 0.03);
  color: #d8fbff;
}

/* Delete button inside keyword chip */
.chip-delete {
  min-height: auto;
  padding: 0;
  border: none;
  background: transparent;
  color: #ff9bb0;
  font-size: 1rem;
  line-height: 1;
}

/* Form layout for adding keywords */
.keyword-form {
  display: flex;
  gap: 12px;
}

/* Input/select shared style */
.keyword-form input,
.field-inline input,
.field-inline select {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(255, 255, 255, 0.03);
  color: #eaffff;
}

/* Keyword input grows to fill available space */
.keyword-form input {
  flex: 1;
}

/* Shared button styles */
.primary-button,
.secondary-button {
  min-height: 48px;
  padding: 0 18px;
  border-radius: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

/* Main action button */
.primary-button {
  border: 1px solid rgba(111, 255, 245, 0.4);
  background: linear-gradient(135deg, rgba(17, 25, 47, 0.96), rgba(9, 44, 57, 0.96));
  color: #cffffd;
  box-shadow: 0 0 22px rgba(0, 255, 231, 0.1);
}

/* Secondary action button */
.secondary-button {
  border: 1px solid rgba(120, 246, 238, 0.18);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(230, 252, 255, 0.82);
}

/* Similar-image controls layout */
.similar-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: end;
}

/* Field container */
.field-inline {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Field label */
.field-inline label {
  color: rgba(214, 241, 255, 0.72);
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* Smaller width field */
.small-field {
  width: 120px;
}

/* Secondary text */
.soft-text {
  color: rgba(214, 241, 255, 0.68);
}

/* Similar section container */
.similar-section {
  margin-top: 30px;
  padding: 24px;
}

/* Similar section header spacing */
.similar-head {
  margin-bottom: 18px;
}

/* Similar section subtitle */
.similar-head p {
  color: rgba(214, 241, 255, 0.68);
}

/* Grid for similar image cards */
.similar-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

/* Clickable similar-image card */
.similar-card {
  cursor: pointer;
}

/* Similar image frame */
.similar-image-wrap {
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(120, 246, 238, 0.12);
  background: rgba(255, 255, 255, 0.03);
}

/* Similar image style */
.similar-image-wrap img {
  width: 100%;
  display: block;
  aspect-ratio: 1 / 1.15;
  object-fit: cover;
  transition: transform 0.3s ease, filter 0.3s ease;
  filter: brightness(0.9) saturate(1.04);
}

/* Hover animation on similar image */
.similar-card:hover .similar-image-wrap img {
  transform: scale(1.03);
  filter: brightness(1) saturate(1.08);
}

/* Similar card text block */
.similar-meta {
  padding: 10px 4px 0;
}

/* Similar image name */
.similar-name {
  color: #eaffff;
  font-size: 0.92rem;
  font-weight: 600;
  line-height: 1.4;
  word-break: break-word;
}

/* Similarity score text */
.similar-score {
  margin-top: 4px;
  color: rgba(214, 241, 255, 0.58);
  font-size: 0.84rem;
}

/* Empty state spacing */
.similar-empty {
  margin-top: 12px;
}

/* Rename section spacing */
.rename-section {
  margin-bottom: 24px;
}

/* Rename section title */
.rename-section h2 {
  margin-bottom: 12px;
  color: #ebffff;
  font-size: 1.2rem;
  font-weight: 700;
}

/* Rename form layout */
.rename-form {
  display: flex;
  gap: 12px;
}

/* Rename input */
.rename-form input {
  flex: 1;
  min-height: 48px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(255, 255, 255, 0.03);
  color: #eaffff;
}

/* Responsive layout for medium screens */
@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .similar-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

/* Active state for like/dislike buttons */
.reaction-section button.active {
  font-weight: bold;
  border: 2px solid currentColor;
}

/* Responsive layout for small screens */
@media (max-width: 700px) {
  .detail-shell {
    width: min(100%, calc(100% - 24px));
  }

  .keyword-form {
    flex-direction: column;
  }

  .keyword-form button {
    width: 100%;
  }

  .similar-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .small-field {
    width: 100%;
  }

  .similar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>