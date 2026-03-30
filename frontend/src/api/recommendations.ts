import api from "./client";
import type { ApiResponse } from "../types";

export interface RecommendationResponse {
  id: string;
  recommenderName: string;
  recommendedName: string;
  opportunityTitle: string;
  companyName: string;
  message: string | null;
  createdAt: string;
}

export async function createRecommendation(data: {
  recommendedId: string;
  opportunityId: string;
  message?: string;
}): Promise<RecommendationResponse> {
  const res = await api.post<ApiResponse<RecommendationResponse>>('/recommendations', data);
  return res.data.data!;
}

export async function getMyRecommendations(): Promise<RecommendationResponse[]> {
  const res = await api.get<ApiResponse<RecommendationResponse[]>>('/recommendations/my');
  return res.data.data!;
}

export async function getRecommendationsForMe(): Promise<RecommendationResponse[]> {
  const res = await api.get<ApiResponse<RecommendationResponse[]>>('/recommendations/for-me');
  return res.data.data!;
}

export async function getRecommendationsByOpportunity(opportunityId: string): Promise<RecommendationResponse[]> {
  const res = await api.get<ApiResponse<RecommendationResponse[]>>(
    `/recommendations/opportunity/${opportunityId}`
  );
  return res.data.data!;
}