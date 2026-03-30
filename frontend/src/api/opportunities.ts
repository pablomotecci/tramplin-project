import client from './client';
import type { OpportunityResponse, OpportunityRequest, OpportunityMapCard, OpportunityFilters, PaginatedResponse, ApiResponse } from '../types';

export async function getOpportunities(
    filters?: OpportunityFilters
): Promise<PaginatedResponse<OpportunityResponse>> {
    const params: Record<string, string | number> = {};

    if (filters?.type) 
        params.type = filters.type;
    if (filters?.workFormat) 
        params.workFormat = filters.workFormat;
    if (filters?.city) 
        params.city = filters.city;
    if (filters?.salaryMin) 
        params.salaryMin = filters.salaryMin;
    if (filters?.search) 
        params.search = filters.search;
    if (filters?.page !== undefined) 
        params.page = filters.page;
    if (filters?.size) 
        params.size = filters.size; 

    if (filters?.tagIds && filters.tagIds.length > 0) {
        (params as any).tagIds = filters.tagIds;
    }


    const response = await client.get<ApiResponse<PaginatedResponse<OpportunityResponse>>>(
        '/opportunities', {params}
    );
    return response.data.data!;
}



// Для маркеров на карте

export async function getOpportunitiesForMap(
  bounds: { swLat: number; swLng: number; neLat: number; neLng: number },
  tagIds?: string[]
): Promise<OpportunityMapCard[]> {
  const params: Record<string, any> = { ...bounds };
  if (tagIds && tagIds.length > 0) {
    params.tagIds = tagIds;
  }
  const response = await client.get<ApiResponse<OpportunityMapCard[]>>(
    '/opportunities/map',
    { params }
  );
  return response.data.data!;
}



// запрос на получение 1 карточки по id
export async function getOpportunityById(id: string): Promise<OpportunityResponse> {
    const response = await client.get<ApiResponse<OpportunityResponse>>(
        `/opportunities/${id}`
    );
    return response.data.data!;
}


export async function createOpportunity(
    data: OpportunityRequest
): Promise<OpportunityResponse> {
    const response = await client.post<ApiResponse<OpportunityResponse>>(
        '/opportunities', data
    );
    return response.data.data!;
}



export async function updateOpportunity(
    id: string,
    data: OpportunityRequest
): Promise<OpportunityResponse> {
    const response = await client.patch<ApiResponse<OpportunityResponse>>(
        `/opportunities/${id}`, data
    );
    return response.data.data!;
}



export async function deleteOpportunity(id: string): Promise<void> {
    await client.delete(`/opportunities/${id}`);
}


// Возможности для ЛК преподавателя
export async function getMyOpportunities(): Promise<OpportunityResponse[]> {
    const response = await client.get<ApiResponse<OpportunityResponse[]>>('/opportunities/my');
    return response.data.data!;
}



export async function changeOpportunityStatus(
    id: string, status: 'ACTIVE' | 'CLOSED' | 'DRAFT'
): Promise<OpportunityResponse> {
    const response = await client.patch<ApiResponse<OpportunityResponse>>(
        `/opportunities/${id}/status`,
        null,
        { params: { status } }
    );
    return response.data.data!;
}
