import { useCallback, useEffect, useRef, useState } from 'react';
import type { BotActionLogResponse, SessionResponse } from '../api/types/session.ts';
import type { SessionStatistics } from '../api/types/post.ts';
import sessionApi from '../api/sessionApi.ts';
import postApi from '../api/postApi.ts';

const POLL_INTERVAL_MS = 2_000;

const useSessionDetails = (id: string) => {
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [logs, setLogs] = useState<BotActionLogResponse[]>([]);
  const [statistics, setStatistics] = useState<SessionStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const timerRef = useRef<number | undefined>(undefined);

  const load = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    window.clearTimeout(timerRef.current);

    try {
      const [sessionResponse, logsResponse, statisticsResponse] = await Promise.all([
        sessionApi.findById(id),
        sessionApi.findLogs(id),
        postApi.statistics(id),
      ]);
      setSession(sessionResponse.data);
      setLogs(logsResponse.data);
      setStatistics(statisticsResponse.data);
      setError(null);

      if (sessionResponse.data.status === 'RUNNING') {
        timerRef.current = window.setTimeout(() => void load(false), POLL_INTERVAL_MS);
      }
    } catch {
      setError('Сесијата не може да се вчита. Проверете дали backend-от работи.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load(true);
    return () => window.clearTimeout(timerRef.current);
  }, [load]);

  return {
    session,
    logs,
    statistics,
    loading,
    error,
    reload: () => load(true),
  };
};

export default useSessionDetails;
