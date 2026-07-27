import {
  Alert, Avatar, Box, Button, Card, CardContent, Chip, CircularProgress, Grid, Stack, Typography,
} from '@mui/material';
import ArrowOutwardRoundedIcon from '@mui/icons-material/ArrowOutwardRounded';
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import postApi from '../../../../api/postApi.ts';
import type { PostResponse } from '../../../../api/types/post.ts';

type Mp4VideoPlayerProps = {
  postId: number;
  mediaId: number;
  poster?: string;
};

const Mp4VideoPlayer = ({ postId, mediaId, poster }: Mp4VideoPlayerProps) => {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [videoError, setVideoError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl: string | null = null;

    postApi.loadVideo(postId, mediaId)
      .then((response) => response.data.arrayBuffer())
      .then((content) => {
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(new Blob([content], { type: 'video/mp4' }));
        setBlobUrl(objectUrl);
      })
      .catch((reason: unknown) => {
        if (controller.signal.aborted) return;
        console.error('Unable to load X MP4 media', reason);
        setVideoError('Видеото не може да се вчита од X.');
      });

    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [postId, mediaId]);

  if (videoError) {
    return <Alert severity='warning'>{videoError}</Alert>;
  }

  if (!blobUrl) {
    return (
      <Box
        sx={{
          minHeight: 280,
          display: 'grid',
          placeItems: 'center',
          bgcolor: '#151515',
          borderRadius: 3,
          backgroundImage: poster ? `url("${poster}")` : undefined,
          backgroundPosition: 'center',
          backgroundSize: 'cover',
        }}
      >
        <CircularProgress sx={{ color: '#fff' }}/>
      </Box>
    );
  }

  return (
    <Box
      key={blobUrl}
      component='video'
      controls
      preload='auto'
      playsInline
      poster={poster}
      sx={{ width: '100%', maxHeight: 520, borderRadius: 3, bgcolor: '#151515' }}
    >
      <source src={blobUrl} type='video/mp4'/>
      Вашиот прелистувач не поддржува MP4 видео.
    </Box>
  );
};

const PostDetailsPage = () => {
  const { id } = useParams<{ id: string }>();
  const [post, setPost] = useState<PostResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    postApi.findById(id)
      .then((response) => setPost(response.data))
      .catch(() => setError('Објавата не може да се вчита.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Box sx={{ display: 'grid', placeItems: 'center', minHeight: 300 }}><CircularProgress/></Box>;
  if (error || !post) return <Alert severity='error'>{error ?? 'Објавата не е пронајдена.'}</Alert>;

  const handle = post.authorHandle ?? 'unknown';
  const confidence = Math.round((post.macedonianConfidence ?? 0) * 100);
  const images = post.mediaItems.filter((media) => media.type === 'IMAGE');
  const video = post.mediaItems.find((media) => media.type === 'VIDEO');

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant='h4'>Детали за објава</Typography>
        <Typography color='text.secondary'>Проверка на содржината, медиумите и изворот пред донирање.</Typography>
      </Box>
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Card>
            <CardContent sx={{ p: { xs: 2.5, md: 3.5 } }}>
              <Stack direction='row' spacing={1.5} sx={{ alignItems: 'center' }}>
                <Avatar sx={{ width: 48, height: 48, bgcolor: '#151515' }}>{handle.slice(0, 2).toUpperCase()}</Avatar>
                <Box>
                  <Typography variant='h6'>@{handle}</Typography>
                  <Typography variant='body2' color='text.secondary'>
                    {post.postedAt ? new Date(post.postedAt).toLocaleString('mk-MK') : `Извлечена во сесија #${post.sessionId}`}
                  </Typography>
                </Box>
              </Stack>

              <Typography sx={{ whiteSpace: 'pre-wrap', my: 3, lineHeight: 1.8, fontSize: '1.05rem' }}>{post.content}</Typography>

              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 2 }}>
                {images.map((media) => (
                  <Box
                    key={media.id}
                    component='img'
                    src={media.sourceUrl}
                    alt={`Медиум од ${handle}`}
                    sx={{ width: '100%', maxHeight: 520, objectFit: 'cover', borderRadius: 3 }}
                  />
                ))}
                {video && (
                  <Mp4VideoPlayer
                    key={`${post.id}-${video.id}`}
                    postId={post.id}
                    mediaId={video.id}
                    poster={images[0]?.sourceUrl}
                  />
                )}
              </Box>

              {post.sourceUrl && (
                <Button sx={{ mt: 3 }} variant='outlined' href={post.sourceUrl} target='_blank' rel='noreferrer' endIcon={<ArrowOutwardRoundedIcon/>}>
                  Отвори ја оригиналната објава на X
                </Button>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Stack direction='row' spacing={1} sx={{ alignItems: 'center', mb: 2 }}>
                <ArticleRoundedIcon color='primary'/>
                <Typography variant='h6'>Метаподатоци</Typography>
              </Stack>
              <Typography variant='caption' color='text.secondary'>МАКЕДОНСКА СОДРЖИНА</Typography>
              <Typography variant='h3' color='text.primary'>{confidence}%</Typography>
              <Stack spacing={1.5} sx={{ mt: 3 }}>
                <Box>
                  <Typography variant='caption' color='text.secondary'>X ID</Typography>
                  <Typography variant='body2' sx={{ fontFamily: 'monospace', overflowWrap: 'anywhere' }}>{post.externalId ?? '—'}</Typography>
                </Box>
                <Box>
                  <Typography variant='caption' color='text.secondary'>СЕСИЈА</Typography>
                  <Typography variant='body2'>#{post.sessionId}</Typography>
                </Box>
                <Box>
                  <Typography variant='caption' color='text.secondary'>ДОНАЦИЈА</Typography><br/>
                  <Chip
                    size='small'
                    color={post.donationBatchId ? 'success' : 'default'}
                    label={post.donationBatchId ? `Batch #${post.donationBatchId}` : 'Не е донирана'}
                  />
                </Box>
                <Box>
                  <Typography variant='caption' color='text.secondary'>МЕДИУМИ</Typography>
                  <Typography variant='body2'>{post.mediaItems.length}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Stack>
  );
};

export default PostDetailsPage;
