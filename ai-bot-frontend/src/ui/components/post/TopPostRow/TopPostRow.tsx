import { Avatar, Box, Button, Chip, Stack, Typography } from '@mui/material';
import ArrowOutwardRoundedIcon from '@mui/icons-material/ArrowOutwardRounded';
import { Link } from 'react-router';
import type { PostResponse, TopPostMetric } from '../../../../api/types/post.ts';
import { formatCount, metricLabels, metricValue } from '../../../../utils/engagement.ts';
import EngagementBar from '../EngagementBar/EngagementBar.tsx';

interface TopPostRowProps {
  post: PostResponse;
  rank: number;
  metric: TopPostMetric;
  /** Value of the first-ranked post, used to size the comparison bar. */
  leaderValue: number;
  compact?: boolean;
}

const TopPostRow = ({ post, rank, metric, leaderValue, compact = false }: TopPostRowProps) => {
  const value = metricValue(post, metric) ?? 0;
  const share = leaderValue > 0 ? Math.max(4, Math.round((value / leaderValue) * 100)) : 0;
  const handle = (post.authorHandle || 'непознат').replace('@', '');
  const leader = rank === 1;

  return (
    <Box
      component='li'
      sx={{
        listStyle: 'none',
        display: 'grid',
        gridTemplateColumns: { xs: '44px 1fr', sm: compact ? '44px 1fr 110px' : '64px 1fr 150px' },
        columnGap: { xs: 1.5, sm: 2.5 },
        rowGap: 1.5,
        py: compact ? 2 : 2.8,
        borderBottom: '1px solid #ECE3D3',
        '&:last-of-type': { borderBottom: 'none' },
      }}
    >
      <Typography
        aria-label={`Место ${rank}`}
        sx={{
          fontSize: compact ? 28 : leader ? 52 : 40,
          lineHeight: 1,
          fontWeight: 800,
          letterSpacing: '-0.05em',
          fontVariantNumeric: 'tabular-nums',
          color: leader ? 'primary.main' : 'text.primary',
          pt: 0.3,
        }}
      >
        {rank}
      </Typography>

      <Box sx={{ minWidth: 0 }}>
        <Stack direction='row' spacing={1} sx={{ alignItems: 'center' }}>
          <Avatar sx={{ width: 28, height: 28, bgcolor: '#151515', fontSize: 12 }}>
            {handle.slice(0, 2).toUpperCase()}
          </Avatar>
          <Typography noWrap sx={{ fontWeight: 750 }}>@{handle}</Typography>
          <Chip size='small' color='secondary' label={`MK ${Math.round((post.macedonianConfidence ?? 0) * 100)}%`}/>
        </Stack>
        <Typography
          variant='body2'
          sx={{
            mt: 1,
            lineHeight: 1.6,
            maxWidth: '68ch',
            display: '-webkit-box',
            WebkitLineClamp: compact ? 2 : 3,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {post.content || 'Објава без текстуална содржина.'}
        </Typography>
        {!compact && (
          <Stack direction='row' sx={{ mt: 1.5, gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
            <EngagementBar post={post}/>
            <Box sx={{ flexGrow: 1 }}/>
            <Button size='small' component={Link} to={`/posts/${post.id}`}>Детали</Button>
            {post.sourceUrl && (
              <Button size='small' href={post.sourceUrl} target='_blank' rel='noreferrer' endIcon={<ArrowOutwardRoundedIcon/>}>
                Отвори X
              </Button>
            )}
          </Stack>
        )}
      </Box>

      <Box sx={{ gridColumn: { xs: '2', sm: 'auto' }, textAlign: { xs: 'left', sm: 'right' } }}>
        <Typography sx={{ fontSize: compact ? 20 : 28, fontWeight: 800, lineHeight: 1.1, fontVariantNumeric: 'tabular-nums' }}>
          {formatCount(value)}
        </Typography>
        <Typography variant='caption' color='text.secondary'>{metricLabels[metric].toLowerCase()}</Typography>
        <Box
          role='presentation'
          sx={{ mt: 1, height: 4, bgcolor: '#F2F0EB', borderRadius: 2, overflow: 'hidden' }}
        >
          <Box sx={{ width: `${share}%`, height: '100%', bgcolor: leader ? 'primary.main' : '#151515' }}/>
        </Box>
        {compact && (
          <Button size='small' component={Link} to={`/posts/${post.id}`} sx={{ mt: 0.5, px: 0, minHeight: 32 }}>
            Детали
          </Button>
        )}
      </Box>
    </Box>
  );
};

export default TopPostRow;
