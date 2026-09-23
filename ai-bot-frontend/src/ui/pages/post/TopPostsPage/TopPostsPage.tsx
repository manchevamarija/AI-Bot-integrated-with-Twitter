import {
  Alert, Box, Button, CircularProgress, FormControlLabel, MenuItem, Paper, Stack, Switch,
  TextField, ToggleButton, ToggleButtonGroup, Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link } from 'react-router';
import type { TopPostMetric } from '../../../../api/types/post.ts';
import useTopPosts from '../../../../hooks/useTopPosts.ts';
import { metricLabels, metricValue } from '../../../../utils/engagement.ts';
import TopPostRow from '../../../components/post/TopPostRow/TopPostRow.tsx';

const metrics = Object.keys(metricLabels) as TopPostMetric[];

const metricHints: Record<TopPostMetric, string> = {
  ENGAGEMENT: 'Лајкови + 2 × репостови + 2 × одговори. Репост и одговор бараат повеќе труд од лајк.',
  LIKES: 'Објавите со најмногу лајкови на X.',
  REPOSTS: 'Објавите што најмногу се споделувале.',
  REPLIES: 'Објавите што поттикнале најмногу разговор.',
  VIEWS: 'Колку пати X ја прикажал објавата. Не е дел од ангажманот.',
};

const TopPostsPage = () => {
  const [metric, setMetric] = useState<TopPostMetric>('ENGAGEMENT');
  const [limit, setLimit] = useState(10);
  const [sessionId, setSessionId] = useState<number | undefined>(undefined);
  const [macedonianOnly, setMacedonianOnly] = useState(true);

  const { posts, loading, error, reload } = useTopPosts({
    metric,
    limit,
    sessionId,
    minMacedonianConfidence: macedonianOnly ? 0.5 : undefined,
  });
  const leaderValue = posts.length ? metricValue(posts[0], metric) ?? 0 : 0;

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant='h4'>Најдобри објави</Typography>
        <Typography color='text.secondary' sx={{ mt: 0.5, maxWidth: '64ch' }}>
          Рангирање на извлечените објави според реалните бројачи од X. Се рангираат само
          објави за кои X прикажал бројка.
        </Typography>
      </Box>

      <Paper variant='outlined' sx={{ p: 2 }}>
        <Stack spacing={2}>
          <ToggleButtonGroup
            exclusive
            size='small'
            value={metric}
            aria-label='Рангирај според'
            onChange={(_, next: TopPostMetric | null) => next && setMetric(next)}
            sx={{ overflowX: 'auto', maxWidth: '100%', alignSelf: 'flex-start' }}
          >
            {metrics.map(item => (
              <ToggleButton key={item} value={item} sx={{ px: 2, textTransform: 'none', fontWeight: 650 }}>
                {metricLabels[item]}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
          <Typography variant='body2' color='text.secondary'>{metricHints[metric]}</Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '160px 120px 1fr' }, gap: 2, alignItems: 'center' }}>
            <TextField
              size='small'
              type='number'
              label='ID на сесија'
              value={sessionId ?? ''}
              onChange={event => setSessionId(event.target.value ? Number(event.target.value) : undefined)}
            />
            <TextField
              select
              size='small'
              label='Прикажи'
              value={limit}
              onChange={event => setLimit(Number(event.target.value))}
            >
              {[5, 10, 20, 50].map(size => <MenuItem key={size} value={size}>Топ {size}</MenuItem>)}
            </TextField>
            <FormControlLabel
              control={<Switch checked={macedonianOnly} onChange={event => setMacedonianOnly(event.target.checked)}/>}
              label='Само македонски (≥ 50%)'
            />
          </Box>
        </Stack>
      </Paper>

      {error && (
        <Alert
          severity='error'
          action={<Button color='inherit' size='small' onClick={() => void reload()}>Повтори</Button>}
        >
          {error}
        </Alert>
      )}

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress/>
        </Box>
      )}

      {!loading && !error && posts.length === 0 && (
        <Paper variant='outlined' sx={{ p: 4 }}>
          <Typography sx={{ fontWeight: 700 }}>Нема објави со бројачи за овие филтри.</Typography>
          <Typography color='text.secondary' sx={{ mt: 0.5 }}>
            Бројачите се читаат при извлекување, па постарите објави немаат вредности.
            Стартувај нова сесија за да се пополни рангирањето.
          </Typography>
          <Button component={Link} to='/sessions' variant='contained' sx={{ mt: 2 }}>Оди на сесии</Button>
        </Paper>
      )}

      {!loading && posts.length > 0 && (
        <Paper variant='outlined' sx={{ px: { xs: 2, md: 3 } }}>
          <Box component='ol' sx={{ m: 0, p: 0 }}>
            {posts.map((post, index) => (
              <TopPostRow
                key={post.id}
                post={post}
                rank={index + 1}
                metric={metric}
                leaderValue={leaderValue}
              />
            ))}
          </Box>
        </Paper>
      )}
    </Stack>
  );
};

export default TopPostsPage;
