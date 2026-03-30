import api from './client';
import type { ApiResponse } from '../types';

export interface UploadResponse {
  url: string;
  filename: string;
}

export async function uploadFile(file: File): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const res = await api.post<ApiResponse<UploadResponse>>('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data.data!;
}
