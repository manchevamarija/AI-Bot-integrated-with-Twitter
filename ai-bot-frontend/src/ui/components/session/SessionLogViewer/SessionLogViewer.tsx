import {
  Alert, Box, Chip, Paper, Stack, Typography,
} from '@mui/material';
import TravelExploreRoundedIcon from '@mui/icons-material/TravelExploreRounded';
import HourglassTopRoundedIcon from '@mui/icons-material/HourglassTopRounded';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import SouthRoundedIcon from '@mui/icons-material/SouthRounded';
import FlagRoundedIcon from '@mui/icons-material/FlagRounded';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import type { BotActionLogResponse, BotActionType } from '../../../../api/types/session.ts';

interface SessionLogViewerProps {
  logs: BotActionLogResponse[];
}

const icons: Partial<Record<BotActionType, typeof TravelExploreRoundedIcon>> = {
  NAVIGATE: TravelExploreRoundedIcon,
  WAIT: HourglassTopRoundedIcon,
  EXTRACT: DownloadRoundedIcon,
  SCROLL: SouthRoundedIcon,
  FINISH: FlagRoundedIcon,
};

const labels: Partial<Record<BotActionType, string>> = {
  NAVIGATE: 'Отворање на X',
  WAIT: 'Вчитување',
  EXTRACT: 'Извлекување објави',
  SCROLL: 'Вчитување повеќе',
  FINISH: 'Завршување',
  LOGIN: 'Најава',
  CLICK: 'Интеракција',
  TYPE: 'Внесување',
};

const fallbackPrefix = 'Claude API unavailable; using safe read-only browser fallback';

const SessionLogViewer = ({ logs }: SessionLogViewerProps) => {
  const fallback = logs.some((log) => log.details?.includes(fallbackPrefix));

  return (
    <Stack spacing={2}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant='h5'>Тек на ботот</Typography>
          <Typography color='text.secondary' variant='body2'>Хронолошки запис на live browser активностите.</Typography>
        </Box>
        {logs.length > 0 && <Chip label={`${logs.length} чекори`} size='small' variant='outlined'/>}
      </Box>

      {fallback && (
        <Alert severity='info'>
          Live browser режим — извлекувањето продолжува со безбедната read-only стратегија.
        </Alert>
      )}

      {!logs.length && (
        <Paper variant='outlined' sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
          Нема запишани активности. Стартувај ја сесијата за да се појави live trace.
        </Paper>
      )}

      <Stack spacing={1.2}>
        {logs.map((log, index) => {
          const Icon = icons[log.actionType] ?? TravelExploreRoundedIcon;
          const cleaned = (log.details ?? '')
            .replace(fallbackPrefix, 'Безбедна read-only стратегија')
            .replace(/^Live browser:\s*/, '');
          return (
            <Paper
              key={log.id}
              variant='outlined'
              sx={{ p: 2, display: 'flex', gap: 1.7, alignItems: 'flex-start', borderColor: log.successful ? '#ECE3D3' : '#F4B8C0' }}
            >
              <Box sx={{
                display: 'grid', placeItems: 'center', minWidth: 38, height: 38, borderRadius: 2.5,
                bgcolor: log.successful ? '#F2F0EB' : '#FBE8EB',
                color: log.successful ? '#151515' : '#B61E32',
              }}>
                <Icon fontSize='small'/>
              </Box>
              <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                <Stack direction='row' spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                  <Typography sx={{ fontWeight: 750 }}>{labels[log.actionType] ?? log.actionType}</Typography>
                  <Typography variant='caption' color='text.secondary'>Чекор {index + 1}</Typography>
                </Stack>
                <Typography variant='body2' color='text.secondary' sx={{ mt: 0.4, overflowWrap: 'anywhere' }}>
                  {cleaned || 'Акцијата е извршена.'}
                </Typography>
                <Typography variant='caption' color='text.disabled'>
                  {new Date(log.occurredAt).toLocaleString('mk-MK')}
                </Typography>
              </Box>
              <Chip
                size='small'
                icon={log.successful ? <CheckRoundedIcon/> : <ErrorOutlineRoundedIcon/>}
                color={log.successful ? 'success' : 'error'}
                label={log.successful ? 'Успешно' : 'Грешка'}
                variant='outlined'
              />
            </Paper>
          );
        })}
      </Stack>
    </Stack>
  );
};

export default SessionLogViewer;
