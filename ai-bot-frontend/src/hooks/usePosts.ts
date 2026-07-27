import { useCallback, useEffect, useState } from 'react';
import type { PageResponse, PostFilter, PostResponse } from '../api/types/post.ts';
import postApi from '../api/postApi.ts';
import useSnackbar from './useSnackbar.ts';

const usePosts = (filter: PostFilter, page: number, size: number) => {
  const { showSnackbar } = useSnackbar();
  const [posts, setPosts] = useState<PageResponse<PostResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await postApi.findAll(filter, page, size);
      setPosts(response.data);
      setError(null);
    } catch {
      setError('Објавите не може да се вчитаат. Провери дали backend-от работи.');
    } finally {
      setLoading(false);
    }
  }, [filter, page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  const onDelete = async (id: number) => {
    try {
      await postApi.delete(String(id));
      showSnackbar('Објавата е избришана.');
      await load();
    } catch (cause) {
      showSnackbar('Објавата не може да се избрише.', 'error');
      throw cause;
    }
  };

  return { posts, loading, error, reload: load, onDelete };
};

export default usePosts;
