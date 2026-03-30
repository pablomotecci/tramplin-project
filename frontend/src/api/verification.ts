import api from './client';
import type { ApiResponse, VerificationRequestResponse, CreateVerificationRequest } from '../types';

export async function submitVerificationRequest(
  data: CreateVerificationRequest
): Promise<VerificationRequestResponse> {
  const res = await api.post<ApiResponse<VerificationRequestResponse>>('/verification/request', data);
  return res.data.data!;
}

export async function getMyVerificationRequest(): Promise<VerificationRequestResponse> {
  const res = await api.get<ApiResponse<VerificationRequestResponse>>('/verification/my');
  return res.data.data!;
}

export async function getVerificationHistory(): Promise<VerificationRequestResponse[]> {
  const res = await api.get<ApiResponse<VerificationRequestResponse[]>>('/verification/my/history');
  return res.data.data!;
}