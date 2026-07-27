import { Alert, Box, Button, CircularProgress, Pagination, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import type { PostFilter } from '../../../../api/types/post.ts';
import usePosts from '../../../../hooks/usePosts.ts';
import PostFilters from '../../../components/post/PostFilters/PostFilters.tsx';
import PostGrid from '../../../components/post/PostGrid/PostGrid.tsx';

const PostsPage = () => {
  const [filter, setFilter] = useState<PostFilter>({ minMacedonianConfidence: 0.5 });
  const [page, setPage] = useState<number>(0);

  const { posts, loading, error, reload, onDelete } = usePosts(filter, page, 12);

  return (
    <Stack spacing={0}>
      <Box sx={{ mb: 3 }}>
        <Typography variant='h4'>Извлечени објави</Typography>
        <Typography color='text.secondary' sx={{ mt: 0.5 }}>Прегледај, филтрирај и провери ја содржината пред донирање.</Typography>
      </Box>
      <PostFilters filter={filter} onChange={(next) => { setFilter(next); setPage(0); }}/>
      {error && (
        <Alert
          severity='error'
          action={<Button color='inherit' size='small' onClick={() => void reload()}>Повтори</Button>}
          sx={{ mb: 2 }}
        >
          {error}
        </Alert>
      )}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress/>
        </Box>
      )}
      {!loading && !error && (!posts || posts.content.length === 0) && (
        <Typography color='text.secondary'>
          Нема објави што одговараат на избраните филтри.
        </Typography>
      )}
      {!loading && posts && <>
        <Typography variant='body2' color='text.secondary' sx={{ mb: 2 }}>
          Пронајдени се <strong>{posts.totalElements}</strong> објави
        </Typography>
        <PostGrid posts={posts.content} onDelete={onDelete}/>
        {posts.totalPages > 1 && <Pagination sx={{ mt: 4, alignSelf: 'center' }} color='primary' page={page + 1} count={posts.totalPages} onChange={(_, value) => setPage(value - 1)}/>}
      </>}
    </Stack>
  );
};

export default PostsPage;
