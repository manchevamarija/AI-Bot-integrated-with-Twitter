import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import type { PostResponse } from '../../../../api/types/post.ts';
import TopPostRow from './TopPostRow.tsx';

const post: PostResponse = {
  id: 7,
  sessionId: 3,
  socialNetwork: 'X',
  externalId: '777',
  authorHandle: 'finki',
  content: 'Денес во Скопје се одржа хакатон.',
  sourceUrl: 'https://x.com/finki/status/777',
  postedAt: null,
  macedonianConfidence: 0.82,
  mediaItems: [],
  donationBatchId: null,
  replyCount: 4,
  repostCount: 10,
  likeCount: 250,
  viewCount: 9876,
  engagementScore: 278,
};

describe('TopPostRow', () => {
  it('shows the rank and the value of the selected metric', () => {
    render(
      <MemoryRouter>
        <ol><TopPostRow post={post} rank={1} metric='LIKES' leaderValue={250}/></ol>
      </MemoryRouter>,
    );

    expect(screen.getByLabelText('Место 1')).toHaveTextContent('1');
    expect(screen.getByText('лајкови')).toBeInTheDocument();
    expect(screen.getByLabelText('Лајкови: 250')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /отвори x/i })).toHaveAttribute('href', post.sourceUrl);
  });
});
