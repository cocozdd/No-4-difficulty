import request from './request';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  password: string;
  nickname: string;
  phone: string;
}

export interface AuthResponse {
  token: string;
  role: string;
  nickname: string;
}

export interface ProfileResponse {
  id: number;
  username: string;
  nickname: string;
  role: string;
  avatarUrl?: string;
  phone?: string;
}

export const login = (payload: LoginPayload) =>
  request.post<AuthResponse>('/auth/login', payload);

export const register = (payload: RegisterPayload) =>
  request.post<AuthResponse>('/auth/register', payload);

export const fetchProfile = () => request.get<ProfileResponse>('/auth/profile');
