import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import StartSessionDialog from './StartSessionDialog.tsx';

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
});
