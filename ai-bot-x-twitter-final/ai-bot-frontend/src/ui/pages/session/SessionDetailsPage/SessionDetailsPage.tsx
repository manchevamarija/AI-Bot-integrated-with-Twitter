import {
  Alert, Box, Button, Card, CardContent, Chip, Grid, Skeleton, Stack, Typography,
} from '@mui/material';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import type { SessionStatus } from '../../../../api/types/session.ts';
import { useParams } from 'react-router';
import useSessionDetails from '../../../../hooks/useSessionDetails.ts';
import SessionLogViewer from '../../../components/session/SessionLogViewer/SessionLogViewer.tsx';
import postApi from '../../../../api/postApi.ts';

const statusMeta: Record<SessionStatus, {
  label: string;
  color: 'default' | 'success' | 'warning' | 'primary' | 'error';
}> = {
  CREATED: { label: 'Подготвена', color: 'default' },
  RUNNING: { label: 'Во тек', color: 'success' },
  PAUSED: { label: 'Паузирана', color: 'warning' },
  COMPLETED: { label: 'Завршена', color: 'primary' },
  FAILED: { label: 'Неуспешна', color: 'error' },
};

const SessionDetailsPage = () => {
  const { id = '' } = useParams<{ id: string }>();
  const { session, logs, statistics, loading, error, reload } = useSessionDetails(id);

  const download = async (format: 'json' | 'csv') => {
    const response = await postApi.export(id, format);
    const url = URL.createObjectURL(response.data);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `x-session-${id}.${format}`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  if (loading && !session) return <Skeleton variant='rounded' height={220}/>;
  if (error && !session) {
    return (
      <Alert severity='error' action={<Button color='inherit' onClick={() => void reload()}>Повтори</Button>}>
        {error}
      </Alert>
    );
  }
  if (!session) return null;

  const status = statusMeta[session.status];
  const statItems = [
    ['Вкупно објави', statistics?.totalPosts ?? 0],
    ['Македонски ≥ 50%', statistics?.macedonianPosts ?? 0],
    ['Со медиуми', statistics?.postsWithMedia ?? 0],
    ['Слики', statistics?.imagePosts ?? 0],
    ['Видеа', statistics?.videoPosts ?? 0],
    ['Донирани', statistics?.donatedPosts ?? 0],
  ] as const;

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        sx={{ justifyContent: 'space-between', gap: 2 }}
      >
        <Box>
          <Stack
            direction='row'
            spacing={1.5}
            sx={{ alignItems: 'center', flexWrap: 'wrap' }}
          >
            <Typography variant='h4'>Сесија #{id}</Typography>
            <Chip label={status.label} color={status.color}/>
          </Stack>
          <Typography color='text.secondary' sx={{ mt: 0.6 }}>
            Реална јавна содржина извлечена од X
          </Typography>
        </Box>
        <Stack direction='row' spacing={1}>
          <Button startIcon={<DownloadRoundedIcon/>} variant='outlined' onClick={() => void download('json')}>
            JSON
          </Button>
          <Button startIcon={<DownloadRoundedIcon/>} variant='outlined' onClick={() => void download('csv')}>
            CSV
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity='warning'>{error}</Alert>}

      <Card>
        <CardContent sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 7 }}>
              <Typography variant='overline' color='text.secondary'>ОПИС</Typography>
              <Typography variant='h6'>{session.description || 'Без опис'}</Typography>
              <Typography variant='overline' color='text.secondary' sx={{ display: 'block', mt: 2 }}>
                ЦЕЛИ
              </Typography>
              <Stack direction='row' sx={{ gap: 1, flexWrap: 'wrap' }}>
                {session.targets.map(target => (
                  <Chip key={target.id} label={`${target.type}: ${target.value}`}/>
                ))}
              </Stack>
            </Grid>
            <Grid size={{ xs: 12, md: 5 }}>
              <Stack spacing={1} sx={{ p: 2, bgcolor: '#FBF6EE' }}>
                <Typography variant='body2'>Максимум: <strong>{session.maxPosts} објави</strong></Typography>
                <Typography variant='body2'>
                  Македонски праг: <strong>{Math.round(session.minMacedonianConfidence * 100)}%</strong>
                </Typography>
                <Typography variant='body2'>
                  Содржина: {[session.includeText && 'текст', session.includeImages && 'слики', session.includeVideos && 'видео']
                    .filter(Boolean).join(', ')}
                </Typography>
                <Typography variant='body2'>
                  Почеток: {session.startedAt ? new Date(session.startedAt).toLocaleString('mk-MK') : '—'}
                </Typography>
                <Typography variant='body2'>
                  Крај: {session.finishedAt ? new Date(session.finishedAt).toLocaleString('mk-MK') : '—'}
                </Typography>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Grid container spacing={1.5}>
        {statItems.map(([label, value]) => (
          <Grid key={label} size={{ xs: 6, md: 2 }}>
            <Card variant='outlined'>
              <CardContent>
                <Typography variant='h5'>{value}</Typography>
                <Typography variant='caption' color='text.secondary'>{label}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <SessionLogViewer logs={logs}/>
    </Stack>
  );
};

export default SessionDetailsPage;
