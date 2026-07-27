export type SocialNetwork =
  | 'FACEBOOK'
  | 'INSTAGRAM'
  | 'X'
  | 'REDDIT'
  | 'TIKTOK'
  | 'YOUTUBE'
  | 'THREADS'
  | 'LINKEDIN';

export type SessionStatus = 'CREATED' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED';

export type TargetType = 'PROFILE' | 'HASHTAG' | 'KEYWORD' | 'FEED_URL';

export type BotActionType =
  | 'NAVIGATE'
  | 'CLICK'
  | 'TYPE'
  | 'SCROLL'
  | 'WAIT'
  | 'EXTRACT'
  | 'LOGIN'
  | 'FINISH';

export interface CreateTargetRequest {
  type: TargetType;
  value: string;
}

export interface CreateSessionRequest {
  socialNetwork: SocialNetwork;
  description: string;
  targets: CreateTargetRequest[];
  maxPosts: number;
  minMacedonianConfidence: number;
  includeText: boolean;
  includeImages: boolean;
  includeVideos: boolean;
}

export interface TargetResponse {
  id: number;
  type: TargetType;
  value: string;
}

export interface SessionResponse {
  id: number;
  socialNetwork: SocialNetwork;
  status: SessionStatus;
  description: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  targets: TargetResponse[];
  maxPosts: number;
  minMacedonianConfidence: number;
  includeText: boolean;
  includeImages: boolean;
  includeVideos: boolean;
}

export interface BotActionLogResponse {
  id: number;
  actionType: BotActionType;
  details: string | null;
  successful: boolean;
  occurredAt: string;
}
