import api from './client';
import type { ApiResponse, FavoriteResponse } from '../types';

export async function addFavorite(opportunityId: string): Promise<FavoriteResponse> {
  const res = await api.post<ApiResponse<FavoriteResponse>>(`/favorites/${opportunityId}`);
  return res.data.data!;
}

export async function removeFavoriteApi(opportunityId: string): Promise<void> {
  await api.delete(`/favorites/${opportunityId}`);
}

export async function getMyFavorites(): Promise<FavoriteResponse[]> {
  const res = await api.get<ApiResponse<FavoriteResponse[]>>('/favorites');
  return res.data.data!;
}