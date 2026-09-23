import {
  Avatar, Box, Button, Card, CardActions, CardContent, Chip, Dialog, DialogActions,
  DialogContent, DialogContentText, DialogTitle, Divider, Stack, Tooltip, Typography,
} from '@mui/material';
import ArrowOutwardRoundedIcon from '@mui/icons-material/ArrowOutwardRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import ImageRoundedIcon from '@mui/icons-material/ImageRounded';
import VideocamRoundedIcon from '@mui/icons-material/VideocamRounded';
import { Link } from 'react-router';
import type { PostResponse } from '../../../../api/types/post.ts';
import { useState } from 'react';
import EngagementBar from '../EngagementBar/EngagementBar.tsx';

interface PostCardProps {
  post: PostResponse;
  onDelete: (id: number) => Promise<void>;
}

const PostCard = ({ post, onDelete }: PostCardProps) => {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const confidence = post.macedonianConfidence ?? 0;
  const image = post.mediaItems.find((media) => media.type === 'IMAGE');
  const hasVideo = post.mediaItems.some((media) => media.type === 'VIDEO');
  const handle = post.authorHandle || 'непознат автор';

  return (
    <Card sx={{
      height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden',
      transition: 'transform .2s ease, box-shadow .2s ease',
      '&:hover': { transform: 'translateY(-3px)', boxShadow: '0 18px 40px rgba(21,21,21,.1)' },
    }}>
      {image && (
        <Box
          component='img'
          src={image.sourceUrl}
          alt={`Медиум од ${handle}`}
          sx={{ width: '100%', height: 190, objectFit: 'cover', bgcolor: '#F2F0EB' }}
        />
      )}
      <CardContent sx={{ flexGrow: 1, p: 2.5 }}>
        <Stack direction='row' spacing={1.2} sx={{ alignItems: 'center' }}>
          <Avatar sx={{ width: 38, height: 38, bgcolor: '#151515', fontSize: 15 }}>
            {handle.replace('@', '').slice(0, 2).toUpperCase()}
          </Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography noWrap sx={{ fontWeight: 750 }}>@{handle.replace('@', '')}</Typography>
            <Typography variant='caption' color='text.secondary'>
              {post.postedAt ? new Date(post.postedAt).toLocaleString('mk-MK') : `Сесија #${post.sessionId}`}
            </Typography>
          </Box>
        </Stack>

        <Typography variant='body2' sx={{
          mt: 2, lineHeight: 1.65, color: 'text.primary',
          display: '-webkit-box', WebkitLineClamp: image ? 4 : 7, WebkitBoxOrient: 'vertical', overflow: 'hidden',
        }}>
          {post.content || 'Објава без текстуална содржина.'}
        </Typography>

        <Stack direction='row' sx={{ mt: 2, gap: 0.8, flexWrap: 'wrap' }}>
          <Chip size='small' color='secondary' label={`MK ${Math.round(confidence * 100)}%`}/>
          {image && <Chip size='small' icon={<ImageRoundedIcon/>} label='Слика' variant='outlined'/>}
          {hasVideo && <Chip size='small' icon={<VideocamRoundedIcon/>} label='Видео' variant='outlined'/>}
          {post.donationBatchId && <Chip size='small' color='success' icon={<CheckCircleRoundedIcon/>} label='Донирана' variant='outlined'/>}
        </Stack>

        <Box sx={{ mt: 1.8 }}>
          <EngagementBar post={post}/>
        </Box>
      </CardContent>
      <Divider/>
      <CardActions sx={{ px: 2, py: 1.3 }}>
        <Button size='small' component={Link} to={`/posts/${post.id}`} startIcon={<VisibilityRoundedIcon/>}>Детали</Button>
        {post.sourceUrl && (
          <Button size='small' href={post.sourceUrl} target='_blank' rel='noreferrer' endIcon={<ArrowOutwardRoundedIcon/>}>Отвори X</Button>
        )}
        <Box sx={{ flexGrow: 1 }}/>
        <Tooltip
          title={post.donationBatchId !== null
            ? 'Донираната објава не може да се избрише бидејќи е дел од евидентирана донација.'
            : 'Избриши ја објавата од локалниот корпус'}
        >
          <span>
            <Button
              size='small'
              color='error'
              disabled={post.donationBatchId !== null}
              onClick={() => setConfirmOpen(true)}
              aria-label='Избриши објава'
            >
              <DeleteOutlineRoundedIcon fontSize='small'/>
            </Button>
          </span>
        </Tooltip>
      </CardActions>
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Избриши ја објавата?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Објавата од @{handle.replace('@', '')} ќе биде отстранета од локалниот корпус. Ова не ја брише оригиналната објава на X.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Откажи</Button>
          <Button
            color='error'
            variant='contained'
            onClick={() => void onDelete(post.id).then(() => setConfirmOpen(false))}
          >
            Избриши
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

export default PostCard;
