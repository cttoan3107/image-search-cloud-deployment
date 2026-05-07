<!--
  ImageGallery.vue
  Gallery page component of the application.
  Responsibilities:
  - Display all uploaded images
  - Load and display all available keywords
  - Allow users to filter images by:
      + keywords
      + file name
      + format
      + width
      + height
      + reaction (like / dislike)
  - Display reactions on each image card
  - Support pagination
  - Allow image deletion
  - Navigate to the image detail page
-->
<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import {
  deleteImage,
  getAllKeywords,
  getImages,
  searchImages,
  getImageReaction,
  type ImageWithReaction,
} from "./http-api"

const router = useRouter() // navigation
const images = ref<ImageWithReaction[]>([]) // image list
const availableKeywords = ref<string[]>([]) // all keywords
const selectedKeywords = ref<string[]>([]) // selected filters
const searchName = ref("") // filter by name
const searchFormat = ref("") // filter by format
const searchWidth = ref<number | null>(null) // filter by width
const searchHeight = ref<number | null>(null) // filter by height
const searchReaction = ref<"" | "LIKE" | "DISLIKE">("") // filter by reaction
const loading = ref(false) // loading state
const pageError = ref("") // error message
const noResult = ref(false) // no result flag
const currentPage = ref(1) // current page
const pageSize = ref(12) // items per page
const getImageSrc = (id: number) => `/images/${id}` // image URL

/*
  Check whether at least one filter is currently active.
  Used to:
  - show/hide the "Clear filters" button
  - decide whether to reload all images or search results after deletion
*/
const hasActiveFilters = computed(() => {
  return (
    selectedKeywords.value.length > 0 ||
    searchName.value.trim() !== "" ||
    searchFormat.value.trim() !== "" ||
    searchWidth.value !== null ||
    searchHeight.value !== null ||
    searchReaction.value !== ""
  )
})

/*
  Compute the total number of pages.
  Ensures at least 1 page exists even if the gallery is empty.
*/
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(images.value.length / pageSize.value))
})

/*
  Compute the images displayed on the current page.
  Uses the current page index and page size.
*/
const paginatedImages = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return images.value.slice(start, end)
})

/*
  Load the full gallery.

  Steps:
  - fetch all images
  - enrich them with reaction data
  - reset page state
*/
async function loadGallery() {
  loading.value = true
  pageError.value = ""
  noResult.value = false

  try {
    const data = await getImages()
    images.value = await attachReactions(data)
    currentPage.value = 1
    noResult.value = data.length === 0
  } catch (error) {
    pageError.value = "Unable to load images."
    images.value = []
  } finally {
    loading.value = false
  }
}

/*
  Load all keywords available in the system.
  These keywords are shown as suggestion chips.
*/
async function loadKeywords() {
  try {
    availableKeywords.value = await getAllKeywords()
  } catch (error) {
    availableKeywords.value = []
  }
}

/*
  Add a keyword to the selected filter list.
  Rules:
  - trim spaces
  - ignore empty values
  - avoid duplicates
*/
function addKeywordFilter(tag: string) {
  const normalized = tag.trim()
  if (!normalized) return
  if (!selectedKeywords.value.includes(normalized)) {
    selectedKeywords.value.push(normalized)
  }
}

/*
  Remove a keyword from the selected filter list.
*/
function removeKeywordFilter(tag: string) {
  selectedKeywords.value = selectedKeywords.value.filter((item) => item !== tag)
}

/*
  Handle manual keyword input from the user.
  Behavior:
  - user presses Enter or comma
  - split input by comma
  - add each keyword to selected filters
  - clear input field
*/
function handleKeywordInput(event: KeyboardEvent) {
  const target = event.target as HTMLInputElement
  if (event.key === "Enter" || event.key === ",") {
    event.preventDefault()
    const pieces = target.value
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean)

    pieces.forEach(addKeywordFilter)
    target.value = ""
  }
}

/*
  Go to the previous page.
  Also scroll back to top for better UX.
*/
function goToPreviousPage() {
  if (currentPage.value > 1) {
    currentPage.value -= 1
    window.scrollTo({ top: 0, behavior: "smooth" })
  }
}

/*
  Go to the next page.
  Also scroll back to top for better UX.
*/
function goToNextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value += 1
    window.scrollTo({ top: 0, behavior: "smooth" })
  }
}

/*
  Execute search with current filters.
  Steps:
  - build search parameters
  - fetch matching images
  - attach reaction info
  - reset pagination
*/
async function searchHandler() {
  loading.value = true
  pageError.value = ""
  noResult.value = false

  try {
    const result = await searchImages({
      tags: selectedKeywords.value,
      name: searchName.value.trim() || undefined,
      format: searchFormat.value.trim() || undefined,
      width: searchWidth.value,
      height: searchHeight.value,
      reaction: searchReaction.value || null
    })

    images.value = await attachReactions(result)
    currentPage.value = 1
    noResult.value = result.length === 0
  } catch (error) {
    pageError.value = "Search failed. Please try again."
    images.value = []
  } finally {
    loading.value = false
  }
}

/*
  Reset all search filters and reload full gallery.
*/
async function resetGallery() {
  selectedKeywords.value = []
  searchName.value = ""
  searchFormat.value = ""
  searchWidth.value = null
  searchHeight.value = null
  searchReaction.value = ""
  await loadGallery()
}

/*
  Enrich each image with its reaction state.
  For each image:
  - fetch current reaction
  - return image with "reaction" property
  - if request fails, fallback to null reaction
*/
async function attachReactions(imageList: { id: number; name: string }[]) {
  const enriched = await Promise.all(
    imageList.map(async (img) => {
      try {
        const data = await getImageReaction(img.id)
        return {
          ...img,
          reaction: data.reaction
        }
      } catch (error) {
        return {
          ...img,
          reaction: null
        }
      }
    })
  )
  return enriched
}

/*
  Delete an image after user confirmation.
  After deletion:
  - reload search results if filters are active
  - otherwise reload the full gallery
*/
async function deleteImageHandler(id: number) {
  const confirmed = window.confirm("Delete this image?")
  if (!confirmed) return

  try {
    await deleteImage(id)

    if (hasActiveFilters.value) {
      await searchHandler()
    } else {
      await loadGallery()
    }
  } catch (error) {
    pageError.value = "Unable to delete the image."
  }
}

/*
  Navigate to the image detail page.
*/
function openImageDetail(id: number) {
  router.push(`/image/${id}`)
}

/*
  Initial page loading:
  - load gallery
  - load keyword suggestions
*/
onMounted(async () => {
  await Promise.all([loadGallery(), loadKeywords()])
})
</script>

<template>
  <!-- Main gallery page -->
  <section class="gallery-page">
    <div class="gallery-shell">

      <!-- Page header -->
      <div class="gallery-header">
        <div>
          <p class="section-label">IMAGE LIBRARY</p>
          <h1 class="page-title">Gallery</h1>
          <p class="page-subtitle">
            Browse uploaded images and search by attributes.
          </p>
        </div>

        <!-- Reset all filters and reload gallery -->
        <button class="ghost-button" @click="resetGallery">
          Reset gallery
        </button>
      </div>

      <!-- Search / filter panel -->
      <div class="search-panel">
        <div class="search-grid">

          <!-- File name filter -->
          <label class="field">
            <span>File name</span>
            <input
              v-model="searchName"
              type="text"
              placeholder="Image name"
            />
          </label>

          <!-- Format filter -->
          <label class="field">
            <span>Format</span>
            <select v-model="searchFormat">
              <option value="">All formats</option>
              <option value="jpeg">JPEG</option>
              <option value="png">PNG</option>
            </select>
          </label>

          <!-- Reaction filter -->
          <label class="field">
            <span>Reaction</span>
            <select v-model="searchReaction">
              <option value="">All reactions</option>
              <option value="LIKE">Liked</option>
              <option value="DISLIKE">Disliked</option>
            </select>
          </label>

          <!-- Width filter -->
          <label class="field">
            <span>Width</span>
            <input
              v-model.number="searchWidth"
              type="number"
              min="0"
              placeholder="Width"
            />
          </label>

          <!-- Height filter -->
          <label class="field">
            <span>Height</span>
            <input
              v-model.number="searchHeight"
              type="number"
              min="0"
              placeholder="Height"
            />
          </label>
        </div>

        <!-- Keyword filter area -->
        <label class="field field-wide">
          <span>Keywords</span>
          <div class="keyword-box">

            <!-- Display selected keyword filters -->
            <div v-if="selectedKeywords.length > 0" class="selected-keywords">
              <button
                v-for="tag in selectedKeywords"
                :key="tag"
                type="button"
                class="selected-keyword"
                @click="removeKeywordFilter(tag)"
              >
                {{ tag }}
                <span>×</span>
              </button>
            </div>

            <!-- Keyword input -->
            <input
              type="text"
              placeholder="Type a keyword and press Enter"
              @keydown="handleKeywordInput"
            />
          </div>
        </label>

        <!-- Suggested keywords loaded from backend -->
        <div v-if="availableKeywords.length > 0" class="keyword-suggestions">
          <button
            v-for="tag in availableKeywords"
            :key="tag"
            type="button"
            class="suggestion-chip"
            @click="addKeywordFilter(tag)"
          >
            {{ tag }}
          </button>
        </div>

        <!-- Search actions -->
        <div class="search-actions">
          <button class="primary-button" @click="searchHandler">
            Search
          </button>

          <button
            v-if="hasActiveFilters"
            class="secondary-button"
            @click="resetGallery"
          >
            Clear filters
          </button>
        </div>
      </div>

      <!-- Error feedback -->
      <p v-if="pageError" class="feedback error-text">
        {{ pageError }}
      </p>

      <!-- Loading feedback -->
      <p v-if="loading" class="feedback soft-text">
        Loading images...
      </p>

      <!-- Empty result feedback -->
      <p v-if="noResult && !loading" class="feedback soft-text">
        No images found for the selected filters.
      </p>

      <!-- Gallery grid -->
      <div v-if="!loading && images.length > 0" class="gallery-grid">
        <article
          v-for="img in paginatedImages"
          :key="img.id"
          class="gallery-card"
        >
          <!-- Clickable image area -->
          <div class="gallery-image-wrap" @click="openImageDetail(img.id)">
            <img
              class="gallery-image"
              :src="getImageSrc(img.id)"
              :alt="img.name || `Image ${img.id}`"
            />

            <!-- Reaction badge -->
            <div v-if="img.reaction" class="reaction-badge">
              <span v-if="img.reaction === 'LIKE'">❤️</span>
              <span v-else-if="img.reaction === 'DISLIKE'">👎</span>
            </div>

            <!-- Hover overlay -->
            <div class="gallery-overlay">
              <span>Open image</span>
            </div>
          </div>

          <!-- Card footer -->
          <div class="gallery-meta">
            <div class="gallery-text" @click="openImageDetail(img.id)">
              <p class="gallery-name">
                {{ img.name || `Image #${img.id}` }}
              </p>
            </div>

            <!-- Delete action -->
            <button class="delete-button" @click="deleteImageHandler(img.id)">
              Delete
            </button>
          </div>
        </article>
      </div>

      <!-- Pagination controls -->
      <div
        v-if="!loading && images.length > 0"
        class="pagination-bar"
      >
        <button
          class="secondary-button"
          @click="goToPreviousPage"
          :disabled="currentPage === 1"
        >
          Previous
        </button>

        <p class="pagination-text">
          Page {{ currentPage }} / {{ totalPages }}
        </p>

        <button
          class="secondary-button"
          @click="goToNextPage"
          :disabled="currentPage === totalPages"
        >
          Next
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* Main page container */
.gallery-page {
  width: 100%;
  min-height: calc(100vh - 76px);
  padding: 36px 0 56px;
}

/* Main centered wrapper */
.gallery-shell {
  width: min(1340px, calc(100% - 40px));
  margin: 0 auto;
}

/* Header layout */
.gallery-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
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
.page-title {
  margin-bottom: 10px;
  font-size: clamp(2.2rem, 4vw, 3.4rem);
  line-height: 0.95;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #ebffff;
  text-transform: uppercase;
  text-shadow: 0 0 22px rgba(0, 255, 231, 0.08);
}

/* Subtitle */
.page-subtitle {
  max-width: 560px;
  color: rgba(214, 241, 255, 0.72);
  font-size: 1rem;
  line-height: 1.7;
}

/* Search panel card */
.search-panel {
  margin-bottom: 28px;
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(8, 17, 30, 0.82);
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.26),
    0 0 30px rgba(0, 255, 231, 0.04);
}

/* Grid layout for search fields */
.search-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

/* Generic field layout */
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Full-width field */
.field-wide {
  grid-column: 1 / -1;
  margin-top: 10px;
}

/* Field labels */
.field span {
  color: rgba(214, 241, 255, 0.72);
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* Inputs and selects */
.field input,
.field select {
  min-height: 50px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(255, 255, 255, 0.03);
  color: #eaffff;
}

/* Focus styles */
.field input:focus,
.field select:focus {
  outline: none;
  border-color: rgba(120, 246, 238, 0.45);
  box-shadow: 0 0 0 3px rgba(120, 246, 238, 0.08);
}

.field input:focus {
  outline: none;
  border-color: rgba(120, 246, 238, 0.45);
  box-shadow: 0 0 0 3px rgba(120, 246, 238, 0.08);
}

/* Keyword input container */
.keyword-box {
  min-height: 50px;
  padding: 8px;
  border-radius: 14px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(255, 255, 255, 0.03);
}

/* Keyword text input */
.keyword-box input {
  width: 100%;
  border: none;
  background: transparent;
  box-shadow: none;
  padding: 6px;
}

/* Remove focus shadow from keyword input */
.keyword-box input:focus {
  outline: none;
  box-shadow: none;
}

/* Selected keywords container */
.selected-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

/* Selected keyword pill */
.selected-keyword {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: auto;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(120, 246, 238, 0.18);
  background: rgba(5, 19, 31, 0.85);
  color: #d8fbff;
  font-size: 0.84rem;
}

/* "x" inside selected keyword */
.selected-keyword span {
  font-size: 0.9rem;
  line-height: 1;
}

/* Suggested keyword chips */
.keyword-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

/* Suggestion chip style */
.suggestion-chip {
  min-height: auto;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(120, 246, 238, 0.14);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(230, 252, 255, 0.82);
  font-size: 0.82rem;
}

/* Search action buttons layout */
.search-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

/* Shared button styles */
.primary-button,
.secondary-button,
.ghost-button,
.delete-button {
  min-height: 48px;
  padding: 0 18px;
  border-radius: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

/* Primary action button */
.primary-button {
  border: 1px solid rgba(111, 255, 245, 0.4);
  background: linear-gradient(135deg, rgba(17, 25, 47, 0.96), rgba(9, 44, 57, 0.96));
  color: #cffffd;
  box-shadow: 0 0 22px rgba(0, 255, 231, 0.1);
}

/* Secondary and ghost buttons */
.secondary-button,
.ghost-button {
  border: 1px solid rgba(120, 246, 238, 0.18);
  background: rgba(255, 255, 255, 0.03);
  color: rgba(230, 252, 255, 0.82);
}

/* Feedback text */
.feedback {
  margin: 16px 0 22px;
  font-size: 0.96rem;
}

/* Soft neutral text */
.soft-text {
  color: rgba(214, 241, 255, 0.68);
}

/* Error text */
.error-text {
  color: #ff7c9b;
}

/* Masonry-like gallery columns */
.gallery-grid {
  columns: 4 260px;
  column-gap: 18px;
}

/* Gallery card */
.gallery-card {
  break-inside: avoid;
  margin-bottom: 18px;
}

/* Image wrapper */
.gallery-image-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid rgba(120, 246, 238, 0.12);
  background: rgba(8, 17, 30, 0.9);
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.28),
    0 0 24px rgba(0, 255, 231, 0.04);
  cursor: pointer;
}

/* Thumbnail image */
.gallery-image {
  width: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.35s ease, filter 0.35s ease;
  filter: brightness(0.9) saturate(1.05);
}

/* Hover effect on image */
.gallery-card:hover .gallery-image {
  transform: scale(1.025);
  filter: brightness(1) saturate(1.1);
}

/* Hover overlay */
.gallery-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: end;
  justify-content: start;
  padding: 16px;
  background: linear-gradient(to top, rgba(4, 8, 15, 0.72), transparent 45%);
  opacity: 0;
  transition: opacity 0.25s ease;
}

/* Show overlay on hover */
.gallery-card:hover .gallery-overlay {
  opacity: 1;
}

/* Overlay text */
.gallery-overlay span {
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(120, 246, 238, 0.22);
  background: rgba(6, 15, 28, 0.82);
  color: #d8fbff;
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* Card footer layout */
.gallery-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 6px 0;
}

/* Clickable text area */
.gallery-text {
  min-width: 0;
  cursor: pointer;
}

/* Image name */
.gallery-name {
  color: #eaffff;
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.4;
  word-break: break-word;
}

/* Optional image ID text */
.gallery-id {
  margin-top: 4px;
  color: rgba(214, 241, 255, 0.58);
  font-size: 0.84rem;
}

/* Like / dislike badge displayed on image */
.reaction-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  min-width: 38px;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(6, 15, 28, 0.82);
  border: 1px solid rgba(120, 246, 238, 0.18);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.28);
  font-size: 1rem;
}

/* Delete button */
.delete-button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(255, 124, 155, 0.22);
  background: rgba(255, 124, 155, 0.06);
  color: #ff9bb0;
}

/* Responsive layout for medium screens */
@media (max-width: 1100px) {
  .search-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field-wide {
    grid-column: 1 / -1;
  }
}

/* Responsive layout for small screens */
@media (max-width: 700px) {
  .gallery-shell {
    width: min(100%, calc(100% - 24px));
  }

  .gallery-header {
    flex-direction: column;
    align-items: start;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-actions {
    flex-direction: column;
  }

  .primary-button,
  .secondary-button,
  .ghost-button {
    width: 100%;
  }

  .gallery-grid {
    columns: 2 160px;
    column-gap: 14px;
  }

  .gallery-meta {
    flex-direction: column;
    align-items: start;
  }

  .delete-button {
    width: 100%;
  }
}

/* Pagination bar */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 32px;
  width: 100%;
  clear: both;
}

/* Pagination text */
.pagination-text {
  color: rgba(214, 241, 255, 0.72);
  font-size: 0.95rem;
  min-width: 120px;
  text-align: center;
}

/* Disabled button state */
.secondary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>