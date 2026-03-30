import api from './client';
import type { ApiResponse, ContactResponse, ContactRequestResponse } from '../types';

export async function sendContactRequest(userId: string): Promise<ContactRequestResponse> {
  const res = await api.post<ApiResponse<ContactRequestResponse>>(`/contacts/request/${userId}`);
  return res.data.data!;
}

export async function respondToContactRequest(
  requestId: string, status: 'ACCEPTED' | 'REJECTED'
): Promise<ContactRequestResponse> {
  const res = await api.put<ApiResponse<ContactRequestResponse>>(
    `/contacts/request/${requestId}`, { status }
  );
  return res.data.data!;
}

export async function getMyContacts(): Promise<ContactResponse[]> {
  const res = await api.get<ApiResponse<ContactResponse[]>>('/contacts');
  return res.data.data!;
}

export async function getIncomingContactRequests(): Promise<ContactRequestResponse[]> {
  const res = await api.get<ApiResponse<ContactRequestResponse[]>>('/contacts/requests');
  return res.data.data!;
}

export async function removeContact(contactRequestId: string): Promise<void> {
  await api.delete(`/contacts/${contactRequestId}`);
}