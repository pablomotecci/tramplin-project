import api from "./client";
import type { ApiResponse, PaginatedResponse, UserManagementResponse, CreateCuratorRequest, VerificationRequestResponse, RejectVerificationRequest, ModerationLogResponse, ModerationActionRequest, Tag, OfferTagRequest, UserRole, AccountStatus, } from "../types";

export async function getUsers(
  page = 0,
  size = 20,
  role?: UserRole,
  status?: AccountStatus
): Promise<PaginatedResponse<UserManagementResponse>> {
  const params: Record<string, any> = { page, size };
  if (role) params.role = role;
  if (status) params.status = status;
  const res = await api.get<ApiResponse<PaginatedResponse<UserManagementResponse>>>(
    '/curator/users',
    { params }
  );
  return res.data.data!;
}


export async function createCurator(
  data: CreateCuratorRequest
): Promise<UserManagementResponse> {
  const res = await api.post<ApiResponse<UserManagementResponse>>(
    '/curator/users/curators',
    data
  );
  return res.data.data!;
}


// Верификация компаний
export async function getPendingVerifications(
  page = 0,
  size = 20
): Promise<PaginatedResponse<VerificationRequestResponse>> {
  const res = await api.get<ApiResponse<PaginatedResponse<VerificationRequestResponse>>>(
    '/verification/pending',
    { params: { page, size } }
  );
  return res.data.data!;
}



export async function approveVerification(
  id: string
): Promise<VerificationRequestResponse> {
  const res = await api.put<ApiResponse<VerificationRequestResponse>>(
    `/verification/${id}/approve`
  );
  return res.data.data!;
}


export async function rejectVerification(
  id: string,
  data: RejectVerificationRequest
): Promise<VerificationRequestResponse> {
  const res = await api.put<ApiResponse<VerificationRequestResponse>>(
    `/verification/${id}/reject`,
    data
  );
  return res.data.data!;
}


// Модерация
export async function hideOpportunity(
  id: string,
  data?: ModerationActionRequest
): Promise<ModerationLogResponse> {
  const res = await api.put<ApiResponse<ModerationLogResponse>>(
    `/moderation/opportunities/${id}/hide`,
    data || {}
  );
  return res.data.data!;
}


export async function unhideOpportunity(
  id: string,
  data?: ModerationActionRequest
): Promise<ModerationLogResponse> {
  const res = await api.put<ApiResponse<ModerationLogResponse>>(
    `/moderation/opportunities/${id}/unhide`,
    data || {}
  );
  return res.data.data!;
}


export async function blockUser(
  id: string,
  data: ModerationActionRequest
): Promise<ModerationLogResponse> {
  const res = await api.put<ApiResponse<ModerationLogResponse>>(
    `/moderation/users/${id}/block`,
    data
  );
  return res.data.data!;
}


export async function unblockUser(
  id: string,
  data?: ModerationActionRequest
): Promise<ModerationLogResponse> {
  const res = await api.put<ApiResponse<ModerationLogResponse>>(
    `/moderation/users/${id}/unblock`,
    data || {}
  );
  return res.data.data!;
}



export async function getModerationLogs(
  page = 0,
  size = 20
): Promise<PaginatedResponse<ModerationLogResponse>> {
  const res = await api.get<ApiResponse<PaginatedResponse<ModerationLogResponse>>>(
    '/moderation/logs',
    { params: { page, size } }
  );
  return res.data.data!;
}


// Управление тегами куратором
export async function getPendingTags(
  page = 0,
  size = 20
): Promise<PaginatedResponse<Tag>> {
  const res = await api.get<ApiResponse<PaginatedResponse<Tag>>>(
    '/curator/tags/pending',
    { params: { page, size } }
  );
  return res.data.data!;
}



export async function approveTag(id: string): Promise<Tag> {
  const res = await api.put<ApiResponse<Tag>>(`/curator/tags/${id}/approve`);
  return res.data.data!;
}



export async function rejectTag(id: string): Promise<void> {
  await api.delete(`/curator/tags/${id}/reject`);
}

export async function createTagByCurator(data: OfferTagRequest): Promise<Tag> {
  const res = await api.post<ApiResponse<Tag>>('/curator/tags', data);
  return res.data.data!;
}


// изменить статус пользователя(блокировка/разблокировка)
export async function changeUserStatus(
  userId: string, status: AccountStatus
): Promise<UserManagementResponse> {
  const res = await api.put<ApiResponse<UserManagementResponse>>(
    `/curator/users/${userId}/status`,
    null,
    { params: { status } }
  );
  return res.data.data!;
}

// Сбросить пароль пользователя(куратор получает временный пароль)
export async function resetUserPassword(userId: string): Promise<{ userId: string; temporaryPassword: string }> {
  const res = await api.post<ApiResponse<{ userId: string; temporaryPassword: string }>>(
    `/curator/users/${userId}/reset-password`
  );
  return res.data.data!;
}

// редактирует профиль соискателя
export async function editApplicantProfile(
  userId: string, data: any
): Promise<any> {
  const res = await api.put<ApiResponse<any>>(
    `/curator/users/${userId}/applicant-profile`,
    data
  );
  return res.data.data!;
}

//  Редактирует профиль компании
export async function editCompanyProfile(
  userId: string, data: any
): Promise<any> {
  const res = await api.put<ApiResponse<any>>(
    `/curator/users/${userId}/company-profile`,
    data
  );
  return res.data.data!;
}