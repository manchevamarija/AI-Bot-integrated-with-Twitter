import {
  Alert,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Slider,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import useSessions from '../../../../hooks/useSessions.ts';
import type { TargetType } from '../../../../api/types/session.ts';
import { MAX_TARGET_LENGTH, xSearchUrl, type SearchOrder } from '../../../../utils/xSearch.ts';

interface StartSessionDialogProps {
  open: boolean;
  onClose: () => void;
}

const labels: Record<TargetType, string> = {
  KEYWORD: 'Клучен збор',
  HASHTAG: 'Хаштаг',
  PROFILE: 'X профил',
  FEED_URL: 'Директен URL',
};

const StartSessionDialog = ({ open, onClose }: StartSessionDialogProps) => {
  const { onCreate } = useSessions();
  const [description, setDescription] = useState('Македонска содржина од X');
  const [type, setType] = useState<TargetType>('KEYWORD');
  const [value, setValue] = useState('Скопје, Македонија');
  const [order, setOrder] = useState<SearchOrder>('live');
  const [maxPosts, setMaxPosts] = useState(30);
  const [minConfidence, setMinConfidence] = useState(50);
  const [includeText, setIncludeText] = useState(true);
  const [includeImages, setIncludeImages] = useState(true);
  const [includeVideos, setIncludeVideos] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const submit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const values = type === 'KEYWORD' || type === 'HASHTAG'
        ? value.split(',').map(item => item.trim()).filter(Boolean)
        : [value.trim()];
      const uniqueValues = [...new Map(
        values.map(item => [item.toLocaleLowerCase('mk'), item]),
      ).values()];
      const searchable = type === 'KEYWORD' || type === 'HASHTAG';
      // Popular results are a direct X search URL, so the backend and the
      // bot need no new target type: FEED_URL already navigates anywhere on X.
      const targets = searchable && order === 'top'
        ? uniqueValues.map(item => ({ type: 'FEED_URL' as TargetType, value: xSearchUrl(item, type, 'top') }))
        : uniqueValues.map(item => ({ type, value: item }));
      if (targets.some(target => target.value.length > MAX_TARGET_LENGTH)) {
        setError('Некој збор е предолг за пребарување на популарни објави. Скрати го.');
        return;
      }
      await onCreate({
        socialNetwork: 'X',
        description: description.trim(),
        targets,
        maxPosts,
        minMacedonianConfidence: minConfidence / 100,
        includeText,
        includeImages,
        includeVideos,
      });
      onClose();
    } catch {
      setError('Сесијата не може да се креира. Проверете дали backend-от работи.');
    } finally {
      setSubmitting(false);
    }
  };

  const noContentSelected = !includeText && !includeImages && !includeVideos;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth='sm'>
      <DialogTitle>Нова сесија за извлекување</DialogTitle>
      <DialogContent>
        <Alert severity='info' sx={{ mt: 1 }}>
          За повеќе клучни зборови или хаштагови, одделете ги вредностите со запирка.
        </Alert>
        {error && <Alert severity='error' sx={{ mt: 2 }}>{error}</Alert>}
        <TextField margin='normal' fullWidth label='Социјална мрежа' value='X / Twitter' disabled />
        <TextField
          margin='normal'
          fullWidth
          label='Опис'
          value={description}
          onChange={event => setDescription(event.target.value)}
        />
        <TextField
          select
          margin='normal'
          fullWidth
          label='Тип на пребарување'
          value={type}
          onChange={event => setType(event.target.value as TargetType)}
        >
          {(Object.keys(labels) as TargetType[]).map(targetType => (
            <MenuItem key={targetType} value={targetType}>{labels[targetType]}</MenuItem>
          ))}
        </TextField>
        <TextField
          margin='normal'
          fullWidth
          label='Вредности'
          value={value}
          helperText={type === 'KEYWORD' || type === 'HASHTAG'
            ? 'Пример: Скопје, Македонија, Охрид'
            : 'Внесете една вредност'}
          onChange={event => setValue(event.target.value)}
        />

        {(type === 'KEYWORD' || type === 'HASHTAG') && (
          <Stack spacing={0.8} sx={{ mt: 1.5 }}>
            <Typography variant='body2'>Кои објави да се бараат</Typography>
            <ToggleButtonGroup
              exclusive
              size='small'
              value={order}
              aria-label='Кои објави да се бараат'
              onChange={(_, next: SearchOrder | null) => next && setOrder(next)}
            >
              <ToggleButton value='live' sx={{ px: 2, textTransform: 'none' }}>Најнови</ToggleButton>
              <ToggleButton value='top' sx={{ px: 2, textTransform: 'none' }}>Најпопуларни</ToggleButton>
            </ToggleButtonGroup>
            <Typography variant='caption' color='text.secondary'>
              {order === 'top'
                ? 'Јазичето „Top“ на X: објави со најмногу лајкови, репостови и одговори.'
                : 'Јазичето „Latest“ на X: најсвежите објави, обично со помалку реакции.'}
            </Typography>
          </Stack>
        )}

        <Stack spacing={0.5} sx={{ mt: 2 }}>
          <Typography variant='body2'>Максимум објави: {maxPosts}</Typography>
          <Slider
            value={maxPosts}
            min={5}
            max={100}
            step={5}
            marks
            onChange={(_, next) => setMaxPosts(next as number)}
          />
          <Typography variant='body2'>
            Минимум македонски јазик: {minConfidence}%
          </Typography>
          <Slider
            value={minConfidence}
            min={0}
            max={100}
            step={5}
            onChange={(_, next) => setMinConfidence(next as number)}
          />
        </Stack>

        <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ mt: 1 }}>
          <FormControlLabel
            control={<Checkbox checked={includeText} onChange={event => setIncludeText(event.target.checked)} />}
            label='Текст'
          />
          <FormControlLabel
            control={<Checkbox checked={includeImages} onChange={event => setIncludeImages(event.target.checked)} />}
            label='Слики'
          />
          <FormControlLabel
            control={<Checkbox checked={includeVideos} onChange={event => setIncludeVideos(event.target.checked)} />}
            label='Видео'
          />
        </Stack>
        {noContentSelected && (
          <Alert severity='warning'>Изберете барем еден тип содржина.</Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Откажи</Button>
        <Button
          variant='contained'
          disabled={!value.trim() || submitting || noContentSelected}
          onClick={() => void submit()}
        >
          {submitting ? 'Се креира…' : 'Креирај'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StartSessionDialog;
