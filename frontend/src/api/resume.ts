import api from './client';
import type {
  ApiResponse,
  ResumeExperience, ResumeProject, ResumeEducation, ResumeBundle,
  ResumeExperienceInput, ResumeProjectInput, ResumeEducationInput,
} from '../types';

const BASE = '/profile/applicant/resume';

export interface ReorderItem { id: string; displayOrder: number; }

// Опыт работы
export async function getExperiences(): Promise<ResumeExperience[]> {
  const res = await api.get<ApiResponse<ResumeExperience[]>>(`${BASE}/experiences`);
  return res.data.data!;
}
export async function createExperience(data: ResumeExperienceInput): Promise<ResumeExperience> {
  const res = await api.post<ApiResponse<ResumeExperience>>(`${BASE}/experiences`, data);
  return res.data.data!;
}
export async function updateExperience(id: string, data: ResumeExperienceInput): Promise<ResumeExperience> {
  const res = await api.put<ApiResponse<ResumeExperience>>(`${BASE}/experiences/${id}`, data);
  return res.data.data!;
}
export async function deleteExperience(id: string): Promise<void> {
  await api.delete(`${BASE}/experiences/${id}`);
}
export async function reorderExperiences(items: ReorderItem[]): Promise<void> {
  await api.put(`${BASE}/experiences/reorder`, { items });
}

// Проекты
export async function getProjects(): Promise<ResumeProject[]> {
  const res = await api.get<ApiResponse<ResumeProject[]>>(`${BASE}/projects`);
  return res.data.data!;
}
export async function createProject(data: ResumeProjectInput): Promise<ResumeProject> {
  const res = await api.post<ApiResponse<ResumeProject>>(`${BASE}/projects`, data);
  return res.data.data!;
}
export async function updateProject(id: string, data: ResumeProjectInput): Promise<ResumeProject> {
  const res = await api.put<ApiResponse<ResumeProject>>(`${BASE}/projects/${id}`, data);
  return res.data.data!;
}
export async function deleteProject(id: string): Promise<void> {
  await api.delete(`${BASE}/projects/${id}`);
}
export async function reorderProjects(items: ReorderItem[]): Promise<void> {
  await api.put(`${BASE}/projects/reorder`, { items });
}

// Образование
export async function getEducation(): Promise<ResumeEducation[]> {
  const res = await api.get<ApiResponse<ResumeEducation[]>>(`${BASE}/education`);
  return res.data.data!;
}
export async function createEducation(data: ResumeEducationInput): Promise<ResumeEducation> {
  const res = await api.post<ApiResponse<ResumeEducation>>(`${BASE}/education`, data);
  return res.data.data!;
}
export async function updateEducation(id: string, data: ResumeEducationInput): Promise<ResumeEducation> {
  const res = await api.put<ApiResponse<ResumeEducation>>(`${BASE}/education/${id}`, data);
  return res.data.data!;
}
export async function deleteEducation(id: string): Promise<void> {
  await api.delete(`${BASE}/education/${id}`);
}
export async function reorderEducation(items: ReorderItem[]): Promise<void> {
  await api.put(`${BASE}/education/reorder`, { items });
}

// Публичное резюме
export async function getPublicResume(userId: string): Promise<ResumeBundle> {
  const res = await api.get<ApiResponse<ResumeBundle>>(`/profile/applicant/${userId}/resume`);
  return res.data.data!;
}