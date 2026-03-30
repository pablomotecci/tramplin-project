// API функции для ЛК соискателя.

import api from './client';
import type {
  ApiResponse,
  ApplicantProfileResponse,
  UpdateApplicantRequest,
} from '../types';

// Получить свой профиль соискателя
export async function getApplicantProfile(): Promise<ApplicantProfileResponse> {
  const response = await api.get<ApiResponse<ApplicantProfileResponse>>(
    '/profile/applicant'
  );
  return response.data.data!;
}

// Обновить свой профиль соискателя
export async function updateApplicantProfile(
  data: UpdateApplicantRequest
): Promise<ApplicantProfileResponse> {
  const response = await api.put<ApiResponse<ApplicantProfileResponse>>(
    '/profile/applicant',
    data
  );
  return response.data.data!;
}


// Получить теги(навыки) соискателя
export async function getApplicantTags(): Promise<string[]> {
  const response = await api.get<ApiResponse<string[]>>('/profile/applicant/tags');
  return response.data.data!;
}

// Обновить теги соискателя
export async function updateApplicantTags(tagIds: string[]): Promise<void> {
  await api.put('/profile/applicant/tags', { tagIds });
}

// Просмотр профиля другого соискателя (с учётом приватности)
export async function getApplicantById(userId: string): Promise<ApplicantProfileResponse> {
  const response = await api.get<ApiResponse<ApplicantProfileResponse>>(
    `/profile/applicant/${userId}`
  );
  return response.data.data!;
}


export async function getApplicantApplications(userId: string): Promise<any[]> {
  const response = await api.get<ApiResponse<any[]>>(`/profile/applicant/${userId}/applications`);
  return response.data.data!;
}

export async function getApplicantContacts(userId: string): Promise<any[]> {
  const response = await api.get<ApiResponse<any[]>>(`/profile/applicant/${userId}/contacts`);
  return response.data.data!;
}