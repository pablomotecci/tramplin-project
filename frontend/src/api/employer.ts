// API функции для ЛК работодателя

import api from './client';
import type { ApiResponse, CompanyProfileResponse, UpdateCompanyRequest, } from '../types';

// Получить профиль компании
export async function getCompanyProfile(): Promise<CompanyProfileResponse> {
  const response = await api.get<ApiResponse<CompanyProfileResponse>>(
    '/profile/employer'
  );
  return response.data.data!;
}

// Получить публичный профиль компании по ID
export async function getCompanyPublicProfile(companyId: string): Promise<CompanyProfileResponse> {
  const response = await api.get<ApiResponse<CompanyProfileResponse>>(
    `/employers/${companyId}/public`
  );
  return response.data.data!;
}

// Обновить профиль компании
export async function updateCompanyProfile(
  data: UpdateCompanyRequest
): Promise<CompanyProfileResponse> {
  const response = await api.put<ApiResponse<CompanyProfileResponse>>(
    '/profile/employer',
    data
  );
  return response.data.data!;
}