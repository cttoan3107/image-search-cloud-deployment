/*
  http-api.ts

  Centralized API service for the frontend.

  Responsibilities:
  - Define all data models (interfaces)
  - Handle HTTP communication with the backend
  - Provide reusable functions for image operations:
      + upload, delete, rename
      + metadata & keywords management
      + similarity search
      + filtering/search
      + like/dislike reactions
*/

import axios from "axios"

/*Interfaces (Data Models)*/

/* Basic image representation */
export interface Image {
  id: number
  name: string
}

/* Metadata associated with an image */
export interface Metadata {
  name: string
  type: string
  size: string
  keywords: string[]
}

/* Representation of a similar image with a similarity score */
export interface SimilarImage {
  id: number
  name?: string
  score: number
}

/* Filters used when searching images */
export interface SearchFilters {
  tags?: string[]
  name?: string
  format?: string
  width?: number | null
  height?: number | null
  reaction?: "LIKE" | "DISLIKE" | null
}

/* Axios instance configured with base URL */
const api = axios.create({
  baseURL: "/"
})

/* Response format for image reaction (like/dislike) */
export interface ImageReactionResponse {
  imageId: number
  reaction: "LIKE" | "DISLIKE" | null
}

/* Extended image type including reaction */
export type ImageWithReaction = Image & {
  reaction?: "LIKE" | "DISLIKE" | null
}

/* Image CRUD Operations */

/* Fetch all images */
export async function getImages(): Promise<Image[]> {
  const resp = await api.get("images")
  return resp.data
}

/* Upload a new image using multipart/form-data */
export async function uploadImage(file: File): Promise<void> {
  const formData = new FormData()
  formData.append("file", file)

  await api.post("images", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  })
}

/* Delete an image */
export async function deleteImage(id: number): Promise<void> {
  await api.delete(`images/${id}`)
}

/* Image Similarity */

/*
  Find similar images
  Parameters:
  - id: reference image
  - descriptor: algorithm used (e.g., color, texture...)
  - k: number of results

  Returns:
  - List of similar images with similarity scores
*/
export async function findSimilarImages(
  id: number,
  descriptor: string,
  k: number
): Promise<SimilarImage[]> {
  const resp = await api.get(`images/${id}/similar`, {
    params: {
      number: k,
      descriptor
    }
  })
  return resp.data
}

/*Metadata & Keywords */
/* Get metadata of a specific image */
export async function getMetadata(id: number): Promise<Metadata> {
  const resp = await api.get(`images/${id}/metadata`)
  const data = resp.data

  /* Normalize backend response (handle missing fields) */
  return {
    name: data.Name ?? "",
    type: data.Type ?? "",
    size: data.Size ?? "",
    keywords: data.Keywords ?? []
  }
}

/* Add a keyword to an image */
export async function addKeyword(id: number, tag: string): Promise<void> {
  await api.put(`images/${id}/keywords`, null, {
    params: { tag }
  })
}

/* Remove a keyword from an image */
export async function deleteKeyword(id: number, tag: string): Promise<void> {
  await api.delete(`images/${id}/keywords`, {
    params: { tag }
  })
}

/* Get all existing keywords in the system */
export async function getAllKeywords(): Promise<string[]> {
  const resp = await api.get("images/keywords")
  return resp.data
}

/* Search & Filtering */
/*
  Search images using multiple filters
  Steps:
  1. Build query parameters dynamically
  2. Call backend search endpoint (returns image IDs)
  3. Fetch all images
  4. Filter images locally based on returned IDs
*/
export async function searchImages(filters: SearchFilters): Promise<Image[]> {
  const params = new URLSearchParams()

  /* Add filters only if they exist */
  if (filters.name?.trim()) params.append("name", filters.name.trim())
  if (filters.format?.trim()) params.append("format", filters.format.trim())
  if (filters.width != null) params.append("width", String(filters.width))
  if (filters.height != null) params.append("height", String(filters.height))
  if (filters.reaction) params.append("reaction", filters.reaction)

  /* Handle multiple keywords */
  if (filters.tags && filters.tags.length > 0) {
    for (const tag of filters.tags) {
      const trimmed = tag.trim()
      if (trimmed) {
        params.append("keywords", trimmed)
      }
    }
  }

  /* Perform two API calls in parallel */
  const [searchResp, allImagesResp] = await Promise.all([
    api.get<number[]>("images/search", { params }),
    api.get<Image[]>("images")
  ])

  /* Convert IDs into a Set for faster lookup */
  const ids = new Set(searchResp.data)

  /* Return only matching images */
  return allImagesResp.data.filter((img) => ids.has(img.id))
}

/*Reactions (Like / Dislike)*/
/* Get current reaction of an image */
export async function getImageReaction(id: number): Promise<ImageReactionResponse> {
  const resp = await api.get(`images/${id}/reaction`)
  return resp.data
}
/* Send a "like" reaction */
export async function likeImage(id: number): Promise<void> {
  await api.post(`images/${id}/like`)
}
/* Send a "dislike" reaction */
export async function dislikeImage(id: number): Promise<void> {
  await api.post(`images/${id}/dislike`)
}

/* Rename an image */
export async function renameImage(id: number, newName: string): Promise<void> {
  await api.put(`images/${id}/name`, null, {
    params: { newName }
  })
}