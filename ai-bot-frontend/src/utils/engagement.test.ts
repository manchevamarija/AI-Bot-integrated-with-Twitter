import { describe, expect, it } from 'vitest';
import type { PostResponse } from '../api/types/post.ts';
import { formatCount, hasEngagement, metricValue } from './engagement.ts';

const post = (overrides: Partial<PostResponse> = {}): PostResponse => ({
  id: 1,
  sessionId: 1,
  socialNetwork: 'X',
  externalId: '1',
  authorHandle: 'finki',
  content: 'Објава',
  sourceUrl: 'https://x.com/finki/status/1',
  postedAt: null,
  macedonianConfidence: 0.9,
  mediaItems: [],
  donationBatchId: null,
  replyCount: null,
  repostCount: null,
  likeCount: null,
  viewCount: null,
  engagementScore: null,
  ...overrides,
});

describe('engagement helpers', () => {
  it('shows an unknown counter as a dash, not as zero', () => {
    expect(formatCount(null)).toBe('—');
    expect(formatCount(0)).toBe('0');
  });

  it('keeps small numbers exact and shortens large ones', () => {
    expect(formatCount(987)).toBe('987');
    expect(formatCount(1_500_000)).not.toContain('500');
  });

  it('picks the counter that matches the selected ranking', () => {
    const ranked = post({ likeCount: 120, repostCount: 8, replyCount: 3, viewCount: 9000, engagementScore: 142 });
    expect(metricValue(ranked, 'LIKES')).toBe(120);
    expect(metricValue(ranked, 'REPOSTS')).toBe(8);
    expect(metricValue(ranked, 'REPLIES')).toBe(3);
    expect(metricValue(ranked, 'VIEWS')).toBe(9000);
    expect(metricValue(ranked, 'ENGAGEMENT')).toBe(142);
  });

  it('detects whether X reported any counters', () => {
    expect(hasEngagement(post())).toBe(false);
    expect(hasEngagement(post({ likeCount: 0 }))).toBe(true);
  });
});
