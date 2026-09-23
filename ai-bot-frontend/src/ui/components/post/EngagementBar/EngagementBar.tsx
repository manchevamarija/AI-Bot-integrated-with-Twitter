import { Stack, Tooltip, Typography } from '@mui/material';
import FavoriteBorderRoundedIcon from '@mui/icons-material/FavoriteBorderRounded';
import RepeatRoundedIcon from '@mui/icons-material/RepeatRounded';
import ChatBubbleOutlineRoundedIcon from '@mui/icons-material/ChatBubbleOutlineRounded';
import BarChartRoundedIcon from '@mui/icons-material/BarChartRounded';
import type { PostResponse } from '../../../../api/types/post.ts';
import { formatCount, formatFullCount, hasEngagement } from '../../../../utils/engagement.ts';

interface EngagementBarProps {
  post: PostResponse;
  size?: 'small' | 'medium';
}

/**
 * The same four counters X shows under a post, in the same order, so the
 * numbers are easy to compare with the original.
 */
const EngagementBar = ({ post, size = 'small' }: EngagementBarProps) => {
  if (!hasEngagement(post)) {
    return (
      <Typography variant='caption' color='text.disabled'>
        X не прикажа бројачи за оваа објава
      </Typography>
    );
  }

  const items = [
    { label: 'Одговори', value: post.replyCount, Icon: ChatBubbleOutlineRoundedIcon },
    { label: 'Репостови', value: post.repostCount, Icon: RepeatRoundedIcon },
    { label: 'Лајкови', value: post.likeCount, Icon: FavoriteBorderRoundedIcon },
    { label: 'Прегледи', value: post.viewCount, Icon: BarChartRoundedIcon },
  ];

  return (
    <Stack
      direction='row'
      component='ul'
      aria-label='Ангажман на X'
      sx={{ listStyle: 'none', m: 0, p: 0, gap: size === 'small' ? 2 : 3, flexWrap: 'wrap' }}
    >
      {items.map(({ label, value, Icon }) => (
        <Tooltip key={label} title={`${label}: ${formatFullCount(value)}`}>
          <Stack
            component='li'
            direction='row'
            spacing={0.6}
            sx={{ alignItems: 'center', color: 'text.secondary' }}
            aria-label={`${label}: ${formatFullCount(value)}`}
          >
            <Icon sx={{ fontSize: size === 'small' ? 17 : 21 }}/>
            <Typography
              variant={size === 'small' ? 'caption' : 'body1'}
              sx={{ fontWeight: 650, fontVariantNumeric: 'tabular-nums', color: 'text.primary' }}
            >
              {formatCount(value)}
            </Typography>
          </Stack>
        </Tooltip>
      ))}
    </Stack>
  );
};

export default EngagementBar;
