import api from "./client";
import type { ApiResponse } from "../types";

export interface PlatformStats {
    companiesCount: number;
    opportunitiesCount: number;
    internshipsCount: number;
    applicantsCount: number;
}


export async function getPlatformStats(): Promise<PlatformStats> {
    const res = await api.get<ApiResponse<PlatformStats>>('/stats/public');
  return res.data.data!;
}