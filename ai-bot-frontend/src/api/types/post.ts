import type { SocialNetwork } from './session.ts';

export type MediaType = 'IMAGE' | 'VIDEO';

export interface MediaItemResponse {
  id: number;
  type: MediaType;
  sourceUrl: string;
  storagePath: string | null;
}

export interface PostResponse {
  id: number;
  sessionId: number;
  socialNetwork: SocialNetwork;
  externalId: string | null;
  authorHandle: string | null;
  content: string | null;
  sourceUrl: string | null;
  postedAt: string | null;
  macedonianConfidence: number | null;
  mediaItems: MediaItemResponse[];
  donationBatchId: number | null;
  /** Counters read from X; null means X did not show the counter. */
  replyCount: number | null;
  repostCount: number | null;
  likeCount: number | null;
  viewCount: number | null;
  /** likes + 2 × reposts + 2 × replies */
  engagementScore: number | null;
}

export type TopPostMetric = 'ENGAGEMENT' | 'LIKES' | 'REPOSTS' | 'REPLIES' | 'VIEWS';

export interface TopPostsQuery {
  metric: TopPostMetric;
  limit: number;
  sessionId?: number;
  minMacedonianConfidence?: number;
}

/**
 * Optional filters for browsing posts — mirrors the request params of
 * GET /api/posts. Omitted fields are not sent.
 */
export interface PostFilter {
  sessionId?: number;
  socialNetwork?: SocialNetwork;
  minMacedonianConfidence?: number;
  donated?: boolean;
  search?: string;
}

/**
 * Spring Data Page<T> as serialised by the backend.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface SessionStatistics {
  totalPosts: number;
  macedonianPosts: number;
  postsWithMedia: number;
  imagePosts: number;
  videoPosts: number;
  donatedPosts: number;
  totalLikes: number;
  totalReposts: number;
  totalReplies: number;
  totalViews: number;
}
