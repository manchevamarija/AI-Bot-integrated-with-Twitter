import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import StartSessionDialog from './StartSessionDialog.tsx';
import { xSearchUrl } from '../../../../utils/xSearch.ts';

const onCreate = vi.fn();

vi.mock('../../../../hooks/useSessions.ts', () => ({
  default: () => ({ onCreate }),
}));

describe('StartSessionDialog', () => {
  beforeEach(() => {
    onCreate.mockReset();
    onCreate.mockResolvedValue(undefined);
  });

  it('creates one target per comma-separated keyword and removes duplicates', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(<StartSessionDialog open onClose={onClose}/>);

    const values = screen.getByLabelText(/вредности/i);
    await user.clear(values);
    await user.type(values, 'Скопје, Македонија, скопје');
    await user.click(screen.getByRole('button', { name: /креирај/i }));

    await waitFor(() => expect(onCreate).toHaveBeenCalledOnce());
    expect(onCreate).toHaveBeenCalledWith(expect.objectContaining({
      socialNetwork: 'X',
      targets: [
        { type: 'KEYWORD', value: 'скопје' },
        { type: 'KEYWORD', value: 'Македонија' },
      ],
      maxPosts: 30,
      minMacedonianConfidence: 0.5,
      includeText: true,
      includeImages: true,
      includeVideos: true,
    }));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('turns keywords into X "Top" search URLs when popular posts are chosen', async () => {
    const user = userEvent.setup();

    render(<StartSessionDialog open onClose={vi.fn()}/>);

    const values = screen.getByLabelText(/вредности/i);
    await user.clear(values);
    await user.type(values, 'Скопје');
    await user.click(screen.getByRole('button', { name: /најпопуларни/i }));
    await user.click(screen.getByRole('button', { name: /креирај/i }));

    await waitFor(() => expect(onCreate).toHaveBeenCalledOnce());
    expect(onCreate).toHaveBeenCalledWith(expect.objectContaining({
      targets: [{
        type: 'FEED_URL',
        value: `https://x.com/search?q=${encodeURIComponent('Скопје')}&src=typed_query&f=top`,
      }],
    }));
  });

  it('adds the hashtag sign for popular hashtag searches', () => {
    expect(xSearchUrl('Македонија', 'HASHTAG', 'top'))
      .toBe(`https://x.com/search?q=${encodeURIComponent('#Македонија')}&src=typed_query&f=top`);
  });
});
