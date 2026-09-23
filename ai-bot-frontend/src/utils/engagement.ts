import type { PostResponse, TopPostMetric } from '../api/types/post.ts';

const compact = new Intl.NumberFormat('mk-MK', { notation: 'compact', maximumFractionDigits: 1 });
const full = new Intl.NumberFormat('mk-MK');

/** 1234 → "1,2 илј.", 999 → "999", null → "—". */
export const formatCount = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '—';
  return value < 10_000 ? full.format(value) : compact.format(value);
};

export const formatFullCount = (value: number | null | undefined): string =>
  value === null || value === undefined ? 'непознато' : full.format(value);

export const metricValue = (post: PostResponse, metric: TopPostMetric): number | null => {
  switch (metric) {
    case 'LIKES': return post.likeCount;
    case 'REPOSTS': return post.repostCount;
    case 'REPLIES': return post.replyCount;
    case 'VIEWS': return post.viewCount;
    case 'ENGAGEMENT': return post.engagementScore;
  }
};

export const metricLabels: Record<TopPostMetric, string> = {
  ENGAGEMENT: 'Ангажман',
  LIKES: 'Лајкови',
  REPOSTS: 'Репостови',
  REPLIES: 'Одговори',
  VIEWS: 'Прегледи',
};

export const hasEngagement = (post: PostResponse): boolean =>
  [post.replyCount, post.repostCount, post.likeCount, post.viewCount].some(value => value !== null);
