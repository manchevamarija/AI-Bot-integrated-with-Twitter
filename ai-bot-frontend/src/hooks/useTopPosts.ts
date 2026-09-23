import { useCallback, useEffect, useState } from 'react';
import type { PostResponse, TopPostsQuery } from '../api/types/post.ts';
import postApi from '../api/postApi.ts';

const useTopPosts = (query: TopPostsQuery) => {
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { metric, limit, sessionId, minMacedonianConfidence } = query;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await postApi.top({ metric, limit, sessionId, minMacedonianConfidence });
      setPosts(response.data);
      setError(null);
    } catch {
      setError('Најдобрите објави не може да се вчитаат. Провери дали backend-от работи.');
    } finally {
      setLoading(false);
    }
  }, [metric, limit, sessionId, minMacedonianConfidence]);

  useEffect(() => {
    void load();
  }, [load]);

  return { posts, loading, error, reload: load };
};

export default useTopPosts;
