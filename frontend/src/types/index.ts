// Все TS-типы


export type UserRole = 'APPLICANT' | 'EMPLOYER' | 'CURATOR' | 'ADMIN';

export type AccountStatus = 'ACTIVE' | 'PENDING_VERIFICATION' | 'BLOCKED';

export type VerificationStatus = 'UNVERIFIED' | 'PENDING' | 'VERIFIED' | 'REJECTED';


export interface RegisterRequest {
  email: string;
  displayName: string;
  password: string;
  role: 'APPLICANT' | 'EMPLOYER';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  userId: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
  accessToken: string;
  refreshToken: string;
}

// Профиль соискателя
export interface ApplicantProfileResponse {
  userId: string;
  firstName: string;
  lastName: string;
  middleName: string | null;
  university: string | null;
  course: number | null;
  graduationYear: number | null;
  bio: string | null;
  avatarUrl: string | null;
  phone: string | null;
  portfolioUrl: string | null;
  githubUrl: string | null;
  skillsSummary: string | null;
  tags: string[];
}

export interface UpdateApplicantRequest {
  firstName: string;
  lastName: string;
  middleName?: string;
  university?: string;
  course?: number;
  graduationYear?: number;
  bio?: string;
  phone?: string;
  portfolioUrl?: string;
  githubUrl?: string;
  skillsSummary?: string;
}

// Профиль компании
export interface CompanyProfileResponse {
  userId: string;
  companyName: string;
  description: string | null;
  industry: string | null;
  inn: string | null;
  websiteUrl: string | null;
  logoUrl: string | null;
  city: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  officePhotos: string[] | null;
  videoUrl: string | null;
  verificationStatus: VerificationStatus;
}

export interface UpdateCompanyRequest {
  companyName: string;
  description?: string;
  industry?: string;
  inn?: string;
  websiteUrl?: string;
  city?: string;
  address?: string;
  phone?: string;
  email?: string;
  officePhotos?: string[];
  videoUrl?: string;
}

// Единая обёртка API ответов
export interface ApiErrorDetail {
  code: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiErrorDetail;
}

export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: AccountStatus;
  createdAt: string;
}





// КАРТОЧКИ ВОЗМОЖНОСТЕЙ

export enum OpportunityType {
  VACANCY = 'VACANCY',
  INTERNSHIP = 'INTERNSHIP',
  MENTORSHIP = 'MENTORSHIP',
  EVENT = 'EVENT',
}

/*

ФОРМАТ РАБОТЫ

  OFFICE - маркер на карте по точному адресу 
  HYBRID - маркер на карте по адресу офиса
  REMOTE - маркер по городу работодателя

*/

export enum WorkFormat {
  OFFICE = 'OFFICE',
  HYBRID = 'HYBRID',
  REMOTE = 'REMOTE',
}


export enum OpportunityStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  CLOSED = 'CLOSED',
  ON_MODERATION = 'ON_MODERATION',
}


export enum TagCategory {
  LANGUAGE = 'LANGUAGE',
  FRAMEWORK = 'FRAMEWORK',
  LEVEL = 'LEVEL',
  SPECIALIZATION = 'SPECIALIZATION',
  EMPLOYMENT_TYPE = 'EMPLOYMENT_TYPE',
  TOOL = 'TOOL',
  DATABASE = 'DATABASE',
}


export interface Tag {
  id: string;
  name: string;
  category: TagCategory;
  approved: boolean;
  parentId: string | null;
  parentName: string | null;
}


export interface TagTreeResponse {
  id: string;
  name: string;
  category: TagCategory;
  children: TagTreeResponse[];
}


export interface OfferTagRequest {
  name: string;
  category: TagCategory;
  parentId?: string;
}



export interface CompanySummary {
  id: string;
  companyName: string;
  logoUrl: string | null;
  industry: string | null;
  city: string | null;
}


// ПОЛНАЯ КАРТОЧКА ВОЗМОЖНОСТИ

export interface OpportunityResponse {
  id: string;
  title: string;
  description: string;
  type: OpportunityType;
  workFormat: WorkFormat;       // офис/гибрид/удаленка
  status: OpportunityStatus;

  // Месторасположение
  city: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;

  salaryMin: number | null;
  salaryMax: number | null;

  publishedAt: string | null;
  expiresAt: string | null;
  eventDate: string | null;


  contactEmail: string | null;
  contactPhone: string | null;
  contactUrl: string | null;

  employerId: string;
  companyName: string;
  logoUrl: string | null;

  tags: string[]; // массив имен тегов

}



/* 
Короткая версия карточки для маркера на карте
Отобразится контент при наведении на маркер
*/

export interface OpportunityMapCard {
  id: string;
  title: string;
  companyName: string;
  logoUrl: string | null;
  type: OpportunityType;
  workFormat: WorkFormat;
  city: string;
  latitude: number | null;
  longitude: number | null;
  salaryMin: number | null;
  salaryMax: number | null;
  tags: string[];
}



// Запрос на создание/редактирование карточки (POST /opportunities / PUT /opportunities)

export interface OpportunityRequest {
  title: string;
  type: OpportunityType;
  workFormat: WorkFormat;
  description: string;
  city: string;
  address?: string | null;
  salaryMin?: number | null;
  salaryMax?: number | null;
  eventDate?: string | null;
  expiresAt?: string | null;
  tagIds?: string[];
  contactPhone?: string | null;
  contactEmail: string | null;
  contactUrl?: string | null; 
}


export interface PaginatedResponse<T> {
  content: T[];           // массив элементов текущей страницы
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}


export interface OpportunityFilters {
  type?: OpportunityType;
  workFormat?: WorkFormat;
  city?: string;
  salaryMin?: number;
  tagIds?: string[];
  page?: number;
  size?: number;
  search?: string;
}


// Отклики
export enum ApplicationStatus {
  PENDING = 'PENDING',
  REVIEWED = 'REVIEWED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  RESERVED = 'RESERVED',
}


export interface ApplicationResponse {
  id: string;
  status: ApplicationStatus;
  coverLetter: string | null;
  createdAt: string;
  updatedAt: string;

  // для соискателя, что за вакансия
  opportunityId: string;
  opportunityTitle: string;
  companyName: string;

  // для работодателя, кто откликнулся
  applicantId: string;
  applicantFirstName: string;
  applicantLastName:string;
  applicantEmail: string;
}


export interface CreateApplicationRequest {
  opportunityId: string;
  coverLetter?: string;
}

export interface UpdateApplicationStatusRequest {
  status: ApplicationStatus;
}


//Контакты
export enum ContactRequestStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
}


export interface ContactRequestResponse {
  id: string;
  senderId: string;
  senderDisplayName: string;
  senderEmail: string;
  status: ContactRequestStatus;
  createdAt: string;
}

export interface ContactResponse {
  contactRequestId: string;
  userId: string;
  displayName: string;
  email: string;
  connectedAt: string;
}


export interface FavoriteResponse {
  id: string;
  opportunityId: string;
  opportunityTitle: string;
  companyName: string;
  createdAt: string;
}





// Управление пользователями
export interface UserManagementResponse {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: AccountStatus;
  createdAt: string;
}


export interface CreateCuratorRequest {
  email: string;
  displayName: string;
  password: string;
}


export type VerificationRequestStatus = | 'PENDING' | 'INN_VERIFIED' | 'EMAIL_VERIFIED' | 'APPROVED' | 'REJECTED';

export interface VerificationRequestResponse {
  id: string;
  employerId: string;
  companyName: string;
  inn: string;
  companyDomain: string;
  corporateEmail: string;
  status: VerificationRequestStatus;
  rejectionReason: string | null;
  createdAt: string;
}


export interface CreateVerificationRequest {
  inn: string;
  companyDomain: string;
  corporateEmail: string;
}

export interface RejectVerificationRequest {
  rejectionReason: string;
}

// Модерация
export type ModerationAction = 'EDIT' | 'HIDE' | 'UNHIDE' | 'BLOCK_USER' | 'UNBLOCK_USER' | 'DELETE';
export type TargetType = 'OPPORTUNITY' | 'USER' | 'COMPANY';

export interface ModerationActionRequest {
  reason?: string;
  details?: string;
}


export interface ModerationLogResponse {
  id: string;
  curatorId: string;
  curatorName: string;
  action: ModerationAction;
  targetType: TargetType;
  targetId: string;
  reason: string | null;
  details: string | null;
  createdAt: string;
}