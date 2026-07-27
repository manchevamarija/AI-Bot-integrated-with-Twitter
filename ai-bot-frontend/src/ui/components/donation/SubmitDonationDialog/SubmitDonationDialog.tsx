import {
  Alert, Avatar, Box, Button, Checkbox, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, FormControlLabel, Paper, Stack, Typography,
} from '@mui/material';
import CloudUploadRoundedIcon from '@mui/icons-material/CloudUploadRounded';
import { useEffect, useState } from 'react';
import postApi from '../../../../api/postApi.ts';
import type { PostResponse } from '../../../../api/types/post.ts';

interface SubmitDonationDialogProps {
  open: boolean;
  onClose: () => void;
  onCreate: (postIds: number[]) => Promise<void>;
}

const SubmitDonationDialog = ({ open, onClose, onCreate }: SubmitDonationDialogProps) => {
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [selected, setSelected] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setLoading(true);
    postApi.findAll({ donated: false, minMacedonianConfidence: 0.5 }, 0, 100)
      .then((response) => setPosts(response.data.content))
      .catch(() => setError('Достапните објави не може да се вчитаат.'))
      .finally(() => setLoading(false));
  }, [open]);

  const submit = async () => {
    setSubmitting(true);
    try {
      await onCreate(selected);
      setSelected([]);
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  const allSelected = posts.length > 0 && selected.length === posts.length;

  return (
    <Dialog open={open} onClose={submitting ? undefined : onClose} fullWidth maxWidth='md'>
      <DialogTitle sx={{ pb: 1 }}>
        <Stack direction='row' spacing={1.2} sx={{ alignItems: 'center' }}>
          <Box sx={{ display: 'grid', placeItems: 'center', width: 42, height: 42, borderRadius: '50%', bgcolor: '#FBE8EB', color: '#D7263D' }}>
            <CloudUploadRoundedIcon/>
          </Box>
          <Box>
            <Typography variant='h6'>Подготви нова донација</Typography>
            <Typography variant='body2' color='text.secondary'>Избери проверени објави со најмалку 50% македонска содржина.</Typography>
          </Box>
        </Stack>
      </DialogTitle>
      <DialogContent>
        {error && <Alert severity='error' sx={{ mb: 2 }}>{error}</Alert>}
        {loading && <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}><CircularProgress/></Box>}
        {!loading && !error && posts.length === 0 && (
          <Alert severity='info'>Нема недонирани македонски објави. Прво изврши extraction сесија.</Alert>
        )}
        {!loading && posts.length > 0 && (
          <>
            <Paper variant='outlined' sx={{ px: 2, py: 1, mb: 1.5, display: 'flex', alignItems: 'center' }}>
              <FormControlLabel
                control={<Checkbox checked={allSelected} indeterminate={selected.length > 0 && !allSelected}
                  onChange={(_, checked) => setSelected(checked ? posts.map((post) => post.id) : [])}/>}
                label='Избери ги сите'
              />
              <Box sx={{ flexGrow: 1 }}/>
              <Chip label={`${selected.length} избрани`} color={selected.length ? 'primary' : 'default'} size='small'/>
            </Paper>
            <Stack divider={<Divider flexItem/>} sx={{ maxHeight: 430, overflowY: 'auto' }}>
              {posts.map((post) => {
                const checked = selected.includes(post.id);
                const handle = post.authorHandle ?? 'unknown';
                return (
                  <Box key={post.id} sx={{ display: 'flex', gap: 1.5, py: 1.5, pr: 1, bgcolor: checked ? '#FBE8EB' : 'transparent' }}>
                    <Checkbox
                      checked={checked}
                      onChange={(_, next) => setSelected((current) => next ? [...current, post.id] : current.filter((id) => id !== post.id))}
                    />
                    <Avatar sx={{ width: 34, height: 34, bgcolor: '#151515', fontSize: 13 }}>{handle.slice(0, 2).toUpperCase()}</Avatar>
                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                      <Stack direction='row' spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                        <Typography variant='body2' sx={{ fontWeight: 750 }}>@{handle}</Typography>
                        <Chip size='small' color='success' variant='outlined' label={`MK ${Math.round((post.macedonianConfidence ?? 0) * 100)}%`}/>
                      </Stack>
                      <Typography variant='body2' color='text.secondary' sx={{
                        mt: 0.5, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
                      }}>
                        {post.content}
                      </Typography>
                    </Box>
                  </Box>
                );
              })}
            </Stack>
          </>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} disabled={submitting}>Откажи</Button>
        <Button variant='contained' disabled={selected.length === 0 || submitting} onClick={() => void submit()}>
          {submitting ? 'Се подготвува…' : `Креирај нацрт (${selected.length})`}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default SubmitDonationDialog;
