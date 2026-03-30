import client from './client';
import type { ApplicationResponse, CreateApplicationRequest, UpdateApplicationStatusRequest, PaginatedResponse, ApiResponse } from '../types';
import { ApplicationStatus } from '../types';


// Соискатель откликается на вакансию
export async function createApplication(
  data: CreateApplicationRequest
): Promise<ApplicationResponse> {
  const response = await client.post<ApiResponse<ApplicationResponse>>(
    '/applications',
    data
  );
  return response.data.data!;
}

// Мои отклики, для соискателя
export async function getMyApplications(
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<ApplicationResponse>> {
  const response = await client.get<ApiResponse<PaginatedResponse<ApplicationResponse>>>(
    '/applications/my',
    { params: { page, size } }
  );
  return response.data.data!;
}

// Входящие отклики, для работодателя
export async function getIncomingApplications(
  page: number = 0,
  size: number = 20,
  status?: ApplicationStatus
): Promise<PaginatedResponse<ApplicationResponse>> {
  const params: Record<string, any> = { page, size };
  if (status) params.status = status;
  const response = await client.get<ApiResponse<PaginatedResponse<ApplicationResponse>>>(
    '/applications/incoming',
    { params }
  );
  return response.data.data!;
}

// Детали отклика
export async function getApplicationById(
  id: string
): Promise<ApplicationResponse> {
  const response = await client.get<ApiResponse<ApplicationResponse>>(
    `/applications/${id}`
  );
  return response.data.data!;
}

// Работодатель меняет статус отклика
export async function updateApplicationStatus(
  id: string,
  data: UpdateApplicationStatusRequest
): Promise<ApplicationResponse> {
  const response = await client.patch<ApiResponse<ApplicationResponse>>(
    `/applications/${id}/status`,
    data
  );
  return response.data.data!;
}