import {
  Box, Button, Card, CardActions, CardContent, Chip, Divider, Stack, Typography,
} from '@mui/material';
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded';
import StopRoundedIcon from '@mui/icons-material/StopRounded';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import XIcon from '@mui/icons-material/X';
import { useNavigate } from 'react-router';
import type { SessionResponse } from '../../../../api/types/session.ts';
import useSessions from '../../../../hooks/useSessions.ts';

interface SessionCardProps {
  session: SessionResponse;
}

const statusMeta = {
  CREATED: { label: 'Подготвена', color: 'default' },
  RUNNING: { label: 'Во тек', color: 'success' },
  PAUSED: { label: 'Паузирана', color: 'warning' },
  COMPLETED: { label: 'Завршена', color: 'primary' },
  FAILED: { label: 'Неуспешна', color: 'error' },
} as const;

const SessionCard = ({ session }: SessionCardProps) => {
  const navigate = useNavigate();
  const { onStart, onStop } = useSessions();
  const status = statusMeta[session.status];
  const canStart = session.status === 'CREATED' || session.status === 'PAUSED';
  const canStop = session.status === 'RUNNING';

  return (
    <Card sx={{
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      transition: 'transform .2s ease, box-shadow .2s ease',
      '&:hover': { transform: 'translateY(-3px)', boxShadow: '0 18px 40px rgba(21,21,21,.1)' },
    }}>
      <CardContent sx={{ flexGrow: 1, p: 2.5 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2 }}>
          <Stack direction='row' spacing={1.2} sx={{ alignItems: 'center' }}>
            <Box sx={{ display: 'grid', placeItems: 'center', width: 40, height: 40, borderRadius: '50%', bgcolor: '#151515', color: 'white' }}>
              <XIcon fontSize='small'/>
            </Box>
            <Box>
              <Typography variant='h6'>Сесија #{session.id}</Typography>
              <Typography variant='caption' color='text.secondary'>X / Twitter</Typography>
            </Box>
          </Stack>
          <Chip label={status.label} color={status.color} size='small' variant={session.status === 'CREATED' ? 'outlined' : 'filled'}/>
        </Box>

        <Typography sx={{ mt: 2.2, fontWeight: 650 }}>{session.description}</Typography>
        <Stack direction='row' sx={{ mt: 1.5, gap: 0.8, flexWrap: 'wrap' }}>
          {session.targets.map((target) => (
            <Chip
              key={`${target.type}-${target.value}`}
              label={`${target.type === 'HASHTAG' ? '#' : ''}${target.value.replace(/^#/, '')}`}
              size='small'
              sx={{ bgcolor: '#FBE8EB', color: '#B61E32' }}
            />
          ))}
        </Stack>

        <Divider sx={{ my: 2 }}/>
        <Stack direction='row' spacing={0.8} sx={{ alignItems: 'center', color: 'text.secondary' }}>
          <AccessTimeRoundedIcon sx={{ fontSize: 17 }}/>
          <Typography variant='caption'>
            {session.startedAt
              ? `Старт: ${new Date(session.startedAt).toLocaleString('mk-MK')}`
              : 'Сè уште не е стартувана'}
          </Typography>
        </Stack>
      </CardContent>

      <CardActions sx={{ px: 2.5, pb: 2.2, pt: 0, gap: 0.5 }}>
        <Button size='small' endIcon={<ArrowForwardRoundedIcon/>} onClick={() => navigate(`/sessions/${session.id}`)}>
          Детали
        </Button>
        <Box sx={{ flexGrow: 1 }}/>
        {canStart && (
          <Button size='small' variant='contained' color='success' startIcon={<PlayArrowRoundedIcon/>} onClick={() => onStart(session.id)}>
            Старт
          </Button>
        )}
        {canStop && (
          <Button size='small' variant='outlined' color='error' startIcon={<StopRoundedIcon/>} onClick={() => onStop(session.id)}>
            Стоп
          </Button>
        )}
      </CardActions>
    </Card>
  );
};

export default SessionCard;
