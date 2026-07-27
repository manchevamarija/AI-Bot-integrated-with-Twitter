import {
  Box, Button, Card, CardActions, CardContent, Chip, Divider, Stack, Typography,
} from '@mui/material';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import CloudUploadRoundedIcon from '@mui/icons-material/CloudUploadRounded';
import Inventory2RoundedIcon from '@mui/icons-material/Inventory2Rounded';
import type { DonationBatchResponse, DonationStatus } from '../../../../api/types/donation.ts';

interface DonationBatchCardProps {
  batch: DonationBatchResponse;
  onApprove: (id: number) => Promise<void>;
  onSubmit: (id: number) => Promise<void>;
}

const meta: Record<DonationStatus, { label: string; color: 'default' | 'primary' | 'success' | 'error' | 'warning' }> = {
  DRAFT: { label: 'Нацрт', color: 'default' },
  APPROVED: { label: 'Одобрена', color: 'primary' },
  SUBMITTED: { label: 'Испратена', color: 'primary' },
  ACCEPTED: { label: 'Прифатена', color: 'success' },
  REJECTED: { label: 'Одбиена', color: 'error' },
  FAILED: { label: 'Неуспешна', color: 'error' },
};

const DonationBatchCard = ({ batch, onApprove, onSubmit }: DonationBatchCardProps) => {
  const status = meta[batch.status];
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ p: 2.5, flexGrow: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Stack direction='row' spacing={1.2} sx={{ alignItems: 'center' }}>
            <Box sx={{ display: 'grid', placeItems: 'center', width: 42, height: 42, borderRadius: '50%', bgcolor: '#FBE8EB', color: '#D7263D' }}>
              <Inventory2RoundedIcon/>
            </Box>
            <Box>
              <Typography variant='h6'>Донација #{batch.id}</Typography>
              <Typography variant='caption' color='text.secondary'>
                {new Date(batch.createdAt).toLocaleString('mk-MK')}
              </Typography>
            </Box>
          </Stack>
          <Chip label={status.label} color={status.color} size='small'/>
        </Box>

        <Box sx={{ mt: 2.5, p: 2, borderRadius: 2, bgcolor: '#FBF6EE', borderLeft: '3px solid #D7263D' }}>
          <Typography variant='caption' color='text.secondary'>СОДРЖИНА</Typography>
          <Typography variant='h5'>{batch.postIds.length} објави</Typography>
        </Box>

        {batch.submittedAt && (
          <Typography variant='caption' color='text.secondary' sx={{ display: 'block', mt: 1.5 }}>
            Испратена: {new Date(batch.submittedAt).toLocaleString('mk-MK')}
          </Typography>
        )}
      </CardContent>
      <Divider/>
      <CardActions sx={{ px: 2.5, py: 1.5 }}>
        <Button
          startIcon={<CheckCircleRoundedIcon/>}
          disabled={batch.status !== 'DRAFT'}
          onClick={() => void onApprove(batch.id)}
        >
          Провери и одобри
        </Button>
        <Box sx={{ flexGrow: 1 }}/>
        <Button
          variant='contained'
          startIcon={<CloudUploadRoundedIcon/>}
          disabled={batch.status !== 'APPROVED'}
          onClick={() => void onSubmit(batch.id)}
        >
          Испрати
        </Button>
      </CardActions>
    </Card>
  );
};

export default DonationBatchCard;
