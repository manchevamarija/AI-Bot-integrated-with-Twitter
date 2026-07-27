import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { PostFilter } from '../../../../api/types/post.ts';
import PostFilters from './PostFilters.tsx';

describe('PostFilters', () => {
  it('reports a text search without losing the existing filter values', async () => {
    const onChange = vi.fn();
    const filter: PostFilter = {
      sessionId: 68,
      minMacedonianConfidence: 0.5,
      donated: false,
    };

    render(<PostFilters filter={filter} onChange={onChange}/>);
    fireEvent.change(screen.getByLabelText(/текст или автор/i), {
      target: { value: 'Скопје' },
    });

    expect(onChange).toHaveBeenCalledWith({
      ...filter,
      search: 'Скопје',
    });
  });

  it('converts the session id input to a number', async () => {
    const onChange = vi.fn();

    render(<PostFilters filter={{}} onChange={onChange}/>);
    fireEvent.change(screen.getByLabelText(/ID на сесија/i), {
      target: { value: '42' },
    });

    expect(onChange).toHaveBeenCalledWith({ sessionId: 42 });
  });
});
