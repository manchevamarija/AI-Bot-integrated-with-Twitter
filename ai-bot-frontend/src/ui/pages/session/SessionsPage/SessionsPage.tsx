import { Box, Button, CircularProgress, Grid, Stack, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useState } from 'react';
import useSessions from '../../../../hooks/useSessions.ts';
import SessionCard from '../../../components/session/SessionCard/SessionCard.tsx';
import StartSessionDialog from '../../../components/session/StartSessionDialog/StartSessionDialog.tsx';

/**
 * The bot control panel. Data flows from useSessions to the session
 * dialog and cards.
 */
const SessionsPage = () => {
  const { sessions, loading } = useSessions();

  const [newSessionDialogOpen, setNewSessionDialogOpen] = useState<boolean>(false);

  return (
    <Stack spacing={3}>
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress/>
        </Box>
      )}
      {!loading &&
       <>
         <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, gap: 2, flexDirection: { xs: 'column', sm: 'row' } }}>
           <Box>
             <Typography variant='h4'>Сесии за извлекување</Typography>
             <Typography color='text.secondary' sx={{ mt: 0.5 }}>Управувај со live пребарувањата и следи го движењето на ботот.</Typography>
           </Box>
           <Button variant='contained' startIcon={<AddIcon/>} onClick={() => setNewSessionDialogOpen(true)}>
             Нова сесија
           </Button>
         </Box>
         {sessions.length === 0 && (
           <Typography color='text.secondary'>
             Сè уште нема сесии. Креирај ја првата за да го активираш ботот.
           </Typography>
         )}
         <Grid container spacing={2}>
           {sessions.map((session) => (
             <Grid key={session.id} size={{ xs: 12, sm: 6, md: 4 }}>
               <SessionCard session={session}/>
             </Grid>
           ))}
         </Grid>
         <StartSessionDialog
           open={newSessionDialogOpen}
           onClose={() => setNewSessionDialogOpen(false)}
         />
       </>}
    </Stack>
  );
};

export default SessionsPage;

