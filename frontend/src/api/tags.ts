import client from './client';
import type { Tag, TagTreeResponse, OfferTagRequest, ApiResponse } from '../types';
import { TagCategory } from '../types';


// все одобренные теги
// или фильтр по категории или по подстроке

export async function getTags(
  category?: TagCategory,
  search?: string
): Promise<Tag[]> {
  const params: Record<string, string> = {};
  if (category)
    params.category = category;
  if (search)
    params.search = search;

  const response = await client.get<ApiResponse<Tag[]>>('/tags', {params});
  return response.data.data!;
}

// Дерево тегов (родитель -> дети)
export async function getTagTree(): Promise<TagTreeResponse[]> {
  const response = await client.get<ApiResponse<TagTreeResponse[]>>('/tags/tree');
  return response.data.data!;
}

// Предложить новый тег (только для EMPLOYER)
export async function offerTag(data: OfferTagRequest): Promise<Tag> {
  const response = await client.post<ApiResponse<Tag>>('/tags/suggest', data);
  return response.data.data!;
}