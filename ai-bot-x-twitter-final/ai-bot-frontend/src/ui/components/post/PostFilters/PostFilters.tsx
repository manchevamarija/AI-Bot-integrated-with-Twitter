import {
  Box, InputAdornment, MenuItem, Paper, Slider, Stack, TextField, Typography,
} from '@mui/material';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import FilterAltRoundedIcon from '@mui/icons-material/FilterAltRounded';
import type { PostFilter } from '../../../../api/types/post.ts';

interface PostFiltersProps {
  filter: PostFilter;
  onChange: (filter: PostFilter) => void;
}

const PostFilters = ({ filter, onChange }: PostFiltersProps) => (
  <Paper variant='outlined' sx={{ p: 2, mb: 3 }}>
    <Stack direction='row' spacing={1} sx={{ mb: 1.5, alignItems: 'center' }}>
      <FilterAltRoundedIcon color='primary' fontSize='small'/>
      <Typography sx={{ fontWeight: 750 }}>Филтри</Typography>
    </Stack>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr 1.6fr' }, gap: 2, alignItems: 'center' }}>
      <TextField
        size='small'
        label='Текст или автор'
        value={filter.search ?? ''}
        placeholder='Пребарај Скопје или автор'
        slotProps={{ input: { startAdornment: <InputAdornment position='start'><SearchRoundedIcon fontSize='small'/></InputAdornment> } }}
        onChange={(event) => onChange({ ...filter, search: event.target.value || undefined })}
      />
      <TextField
        size='small'
        type='number'
        label='ID на сесија'
        value={filter.sessionId ?? ''}
        onChange={(event) => onChange({ ...filter, sessionId: event.target.value ? Number(event.target.value) : undefined })}
      />
      <TextField
        select
        size='small'
        label='Донација'
        value={filter.donated === undefined ? '' : String(filter.donated)}
        onChange={(event) => onChange({ ...filter, donated: event.target.value === '' ? undefined : event.target.value === 'true' })}
      >
        <MenuItem value=''>Сите</MenuItem>
        <MenuItem value='false'>Достапни</MenuItem>
        <MenuItem value='true'>Донирани</MenuItem>
      </TextField>
      <Box sx={{ px: 1 }}>
        <Typography variant='caption' color='text.secondary'>
          Минимум македонски: <strong>{Math.round((filter.minMacedonianConfidence ?? 0) * 100)}%</strong>
        </Typography>
        <Slider
          size='small'
          value={(filter.minMacedonianConfidence ?? 0) * 100}
          onChange={(_, value) => onChange({ ...filter, minMacedonianConfidence: Number(value) / 100 })}
        />
      </Box>
    </Box>
  </Paper>
);

export default PostFilters;
