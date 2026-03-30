import api from './client';
import type { ApiResponse } from '../types';

export type Visibility = 'ALL' | 'CONTACTS_ONLY' | 'EMPLOYERS_ONLY' | 'NOBODY';

export interface PrivacySettings {
  profileVisibility: Visibility;
  resumeVisibility: Visibility;
  applicationsVisibility: Visibility;
  contactsVisibility: Visibility;
}

export async function getPrivacySettings(): Promise<PrivacySettings> {
  const res = await api.get<ApiResponse<PrivacySettings>>('/privacy-settings');
  return res.data.data!;
}

export async function updatePrivacySettings(data: Partial<PrivacySettings>): Promise<PrivacySettings> {
  const res = await api.put<ApiResponse<PrivacySettings>>('/privacy-settings', data);
  return res.data.data!;
}