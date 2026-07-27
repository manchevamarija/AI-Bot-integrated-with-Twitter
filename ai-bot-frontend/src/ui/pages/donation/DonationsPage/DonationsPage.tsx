import {
  Alert, Box, Button, Card, CardContent, CircularProgress, Grid, Stack, Typography,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DraftsRoundedIcon from '@mui/icons-material/DraftsRounded';
import CloudDoneRoundedIcon from '@mui/icons-material/CloudDoneRounded';
import VerifiedRoundedIcon from '@mui/icons-material/VerifiedRounded';
import { useState } from 'react';
import useDonations from '../../../../hooks/useDonations.ts';
import DonationBatchCard from '../../../components/donation/DonationBatchCard/DonationBatchCard.tsx';
import SubmitDonationDialog from '../../../components/donation/SubmitDonationDialog/SubmitDonationDialog.tsx';

const DonationsPage = () => {
  const { donations, loading, error, reload, onCreate, onApprove, onSubmit } = useDonations();
  const [newBatchDialogOpen, setNewBatchDialogOpen] = useState(false);

  const stats = [
    { label: 'Нацрти', value: donations.filter((item) => item.status === 'DRAFT').length, icon: DraftsRoundedIcon, color: '#151515', bg: '#F2F0EB' },
    { label: 'Испратени', value: donations.filter((item) => item.status === 'SUBMITTED').length, icon: CloudDoneRoundedIcon, color: '#D7263D', bg: '#FBE8EB' },
    { label: 'Прифатени', value: donations.filter((item) => item.status === 'ACCEPTED').length, icon: VerifiedRoundedIcon, color: '#FFFFFF', bg: '#151515' },
  ];

  if (loading) return <Box sx={{ display: 'grid', placeItems: 'center', minHeight: 300 }}><CircularProgress/></Box>;

  return (
    <Stack spacing={3}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
        <Box>
          <Typography variant='h4'>Донации за Везилка</Typography>
          <Typography color='text.secondary' sx={{ mt: 0.5 }}>
            Прегледај ги избраните објави, одобри ја донацијата и испрати ја со зачуван извор.
          </Typography>
        </Box>
        <Button variant='contained' startIcon={<AddRoundedIcon/>} onClick={() => setNewBatchDialogOpen(true)}>
          Нова донација
        </Button>
      </Box>

      {error && (
        <Alert
          severity='error'
          action={<Button color='inherit' size='small' onClick={() => void reload()}>Повтори</Button>}
        >
          {error}
        </Alert>
      )}

      <Grid container spacing={2}>
        {stats.map(({ label, value, icon: Icon, color, bg }) => (
          <Grid key={label} size={{ xs: 12, sm: 4 }}>
            <Card>
              <CardContent sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box><Typography color='text.secondary'>{label}</Typography><Typography variant='h4'>{value}</Typography></Box>
                <Box sx={{ display: 'grid', placeItems: 'center', width: 48, height: 48, borderRadius: '50%', color, bgcolor: bg }}><Icon/></Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {donations.length === 0 ? (
        <Alert severity='info'>Нема подготвени донации. Избери проверени македонски објави за да ја создадеш првата донација.</Alert>
      ) : (
        <Grid container spacing={2.5}>
          {donations.map((batch) => (
            <Grid key={batch.id} size={{ xs: 12, md: 6 }}>
              <DonationBatchCard batch={batch} onApprove={onApprove} onSubmit={onSubmit}/>
            </Grid>
          ))}
        </Grid>
      )}

      <SubmitDonationDialog
        open={newBatchDialogOpen}
        onClose={() => setNewBatchDialogOpen(false)}
        onCreate={(postIds) => onCreate({ postIds })}
      />
    </Stack>
  );
};

export default DonationsPage;
