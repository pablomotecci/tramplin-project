import api from './client';
import type { ApiResponse } from '../types';

export interface ApplicantScore {
  userId: string;
  displayName: string;
  score: number;
}

export interface OpportunityScore {
  opportunityId: string;
  title: string;
  companyName: string;
  score: number;
}

export interface AssignmentPair {
  applicantId: string;
  applicantName: string;
  opportunityId: string;
  opportunityTitle: string;
  companyName: string;
  score: number;
}

export interface AssignmentResult {
  pairs: AssignmentPair[];
  totalScore: number;
  applicantCount: number;
  opportunityCount: number;
}

export async function getCandidatesForOpportunity(opportunityId: string): Promise<ApplicantScore[]> {
  const res = await api.get<ApiResponse<ApplicantScore[]>>(`/scoring/opportunity/${opportunityId}/candidates`);
  return res.data.data!;
}

export async function getRecommendationsForApplicant(): Promise<OpportunityScore[]> {
  const res = await api.get<ApiResponse<OpportunityScore[]>>('/scoring/applicant/recommendations');
  return res.data.data!;
}

export async function getMatchScore(applicantId: string, opportunityId: string): Promise<number> {
  const res = await api.get<ApiResponse<number>>('/scoring/match', {
    params: { applicantId, opportunityId }
  });
  return res.data.data!;
}

export async function getOptimalAssignment(): Promise<AssignmentResult> {
  const res = await api.get<ApiResponse<AssignmentResult>>('/scoring/assignment');
  return res.data.data!;
}