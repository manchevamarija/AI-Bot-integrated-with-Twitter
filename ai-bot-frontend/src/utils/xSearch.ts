import type { TargetType } from '../api/types/session.ts';

export type SearchOrder = 'live' | 'top';

export const MAX_TARGET_LENGTH = 200;


/**
 * X search URL for a keyword or hashtag. "top" is X's popular tab, which
 * surfaces posts with the most engagement instead of the newest ones.
 */
export const xSearchUrl = (value: string, type: TargetType, order: SearchOrder) => {
  const query = type === 'HASHTAG' && !value.startsWith('#') ? `#${value}` : value;
  return `https://x.com/search?q=${encodeURIComponent(query)}&src=typed_query&f=${order}`;
};
