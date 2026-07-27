import {
  Box, Button, Card, CardContent, Grid, LinearProgress, Stack, Typography,
} from '@mui/material';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import ArticleOutlinedIcon from '@mui/icons-material/ArticleOutlined';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import FavoriteBorderRoundedIcon from '@mui/icons-material/FavoriteBorderRounded';
import TravelExploreRoundedIcon from '@mui/icons-material/TravelExploreRounded';
import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import postApi from '../../../../api/postApi.ts';
import donationApi from '../../../../api/donationApi.ts';
import sessionApi from '../../../../api/sessionApi.ts';
import useAuth from '../../../../hooks/useAuth.ts';

const AnimatedNumber = ({ value }: { value: number }) => {
  const [displayValue, setDisplayValue] = useState(0);

  useEffect(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reducedMotion || value === 0) {
      const frame = requestAnimationFrame(() => setDisplayValue(value));
      return () => cancelAnimationFrame(frame);
    }

    const duration = 750;
    const startedAt = performance.now();
    let frame = 0;

    const animate = (now: number) => {
      const progress = Math.min((now - startedAt) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplayValue(Math.round(value * eased));
      if (progress < 1) frame = requestAnimationFrame(animate);
    };

    frame = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frame);
  }, [value]);

  return <>{displayValue.toLocaleString('mk-MK')}</>;
};

const HomePage = () => {
  const { isLoggedIn } = useAuth();
  const [stats, setStats] = useState({ posts: 0, macedonian: 0, donations: 0, latest: '—' });

  useEffect(() => {
    if (!localStorage.getItem('token')) return;
    Promise.all([
      postApi.findAll({}, 0, 1),
      postApi.findAll({ minMacedonianConfidence: 0.5 }, 0, 1),
      donationApi.findAll(),
      sessionApi.findAll(),
    ]).then(([allPosts, mkPosts, donations, sessions]) => {
      const latest = [...sessions.data].sort((a, b) => b.id - a.id)[0];
      setStats({
        posts: allPosts.data.totalElements,
        macedonian: mkPosts.data.totalElements,
        donations: donations.data.filter((batch) => ['SUBMITTED', 'ACCEPTED'].includes(batch.status)).length,
        latest: latest ? `#${latest.id} · ${latest.status}` : '—',
      });
    }).catch(() => undefined);
  }, []);

  const mkShare = stats.posts ? Math.round((stats.macedonian / stats.posts) * 100) : 0;
  const cards = [
    { index: '01', label: 'Извлечени објави', value: stats.posts, icon: ArticleOutlinedIcon },
    { index: '02', label: 'Македонски ≥ 50%', value: stats.macedonian, icon: CheckCircleOutlineRoundedIcon },
    { index: '03', label: 'Испратени донации', value: stats.donations, icon: FavoriteBorderRoundedIcon },
    { index: '04', label: 'Последна сесија', value: stats.latest, icon: TravelExploreRoundedIcon },
  ];

  return (
    <Stack spacing={{ xs: 5, md: 7 }}>
      <Box
        component='section'
        sx={{
          py: { xs: 5, md: 8 },
          borderTop: '1px solid #ECE3D3',
          borderBottom: '1px solid #ECE3D3',
        }}
      >
        <Grid container spacing={{ xs: 5, md: 8 }} sx={{ alignItems: 'end' }}>
          <Grid size={{ xs: 12, md: 8 }}>
            <Typography
              variant='overline'
              sx={{ color: 'primary.main', fontWeight: 750, letterSpacing: '.14em' }}
            >
              X / МАКЕДОНСКИ ЈАЗИК / ОТВОРЕНИ ПОДАТОЦИ
            </Typography>
            <Typography
              component='h1'
              sx={{
                mt: 2,
                maxWidth: 930,
                fontSize: { xs: '2.65rem', sm: '3.8rem', md: '5.25rem' },
                lineHeight: .98,
                fontWeight: 600,
                letterSpacing: '-.055em',
              }}
            >
              Македонската содржина, собрана со контекст.
            </Typography>
            <Typography
              sx={{
                mt: 3,
                maxWidth: 720,
                color: 'text.secondary',
                fontSize: { xs: '1rem', md: '1.14rem' },
                lineHeight: 1.7,
              }}
            >
              Систем за пронаоѓање реални јавни објави од X, препознавање
              македонски текст и подготовка на проверливи донации за
              doniraj.vezilka.ai.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mt: 4 }}>
              <Button
                component={Link}
                to={isLoggedIn ? '/sessions' : '/login'}
                variant='contained'
                endIcon={<ArrowForwardRoundedIcon/>}
              >
                {isLoggedIn ? 'Започни ново пребарување' : 'Најави се'}
              </Button>
              {isLoggedIn && (
                <Button component={Link} to='/posts' variant='outlined' color='secondary'>
                  Прегледај ги објавите
                </Button>
              )}
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <Box sx={{ pl: { md: 4 }, borderLeft: { md: '1px solid #ECE3D3' } }}>
              <Typography variant='overline' sx={{ color: 'text.secondary', letterSpacing: '.12em' }}>
                КВАЛИТЕТ НА КОРПУСОТ
              </Typography>
              <Typography sx={{ mt: 1, fontSize: { xs: '3.5rem', md: '5rem' }, lineHeight: 1, fontWeight: 550 }}>
                {mkShare}<Box component='span' sx={{ color: 'primary.main' }}>%</Box>
              </Typography>
              <Typography color='text.secondary' sx={{ mt: 1, mb: 2.5 }}>
                од објавите се над јазичниот праг од 50%
              </Typography>
              <LinearProgress
                variant='determinate'
                value={mkShare}
                sx={{
                  height: 4,
                  bgcolor: '#F2F0EB',
                  '& .MuiLinearProgress-bar': { bgcolor: 'primary.main' },
                }}
              />
            </Box>
          </Grid>
        </Grid>
      </Box>

      {isLoggedIn && (
        <Box component='section'>
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: { xs: 'start', md: 'end' },
              flexDirection: { xs: 'column', md: 'row' },
              gap: 1.5,
              mb: 3,
            }}
          >
            <Box>
              <Typography variant='overline' sx={{ color: 'primary.main', fontWeight: 700, letterSpacing: '.14em' }}>
                ПРЕГЛЕД
              </Typography>
              <Typography variant='h4' sx={{ mt: .5 }}>
                Состојба на системот
              </Typography>
            </Box>
            <Typography color='text.secondary' sx={{ fontSize: '.92rem' }}>
              Ажурирани податоци од вашите сесии
            </Typography>
          </Box>

          <Grid container spacing={2}>
            {cards.map(({ index, label, value, icon: Icon }) => (
              <Grid
                key={label}
                size={{ xs: 12, sm: 6, lg: 3 }}
              >
                <Card
                  sx={{
                    height: '100%',
                    position: 'relative',
                    overflow: 'hidden',
                    bgcolor: '#fff',
                    color: 'text.primary',
                    borderColor: '#ECE3D3',
                    '&::before': {
                      content: '""',
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      width: '100%',
                      height: 4,
                      bgcolor: '#D7263D',
                    },
                  }}
                >
                  <CardContent
                    sx={{
                      p: 3,
                      minHeight: 174,
                      display: 'flex',
                      flexDirection: 'column',
                      '&:last-child': { pb: 3 },
                    }}
                  >
                    <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        color: 'text.secondary',
                      }}
                    >
                      <Typography variant='caption' sx={{ letterSpacing: '.1em' }}>{index}</Typography>
                      <Box
                        sx={{
                          width: 38,
                          height: 38,
                          display: 'grid',
                          placeItems: 'center',
                          bgcolor: '#FBF6EE',
                          color: '#666666',
                        }}
                      >
                        <Icon fontSize='small'/>
                      </Box>
                    </Box>
                    <Typography
                      sx={{
                        mt: 'auto',
                        pt: 2,
                        color: 'text.primary',
                        fontSize: typeof value === 'number' ? '2.75rem' : '1.65rem',
                        lineHeight: 1.05,
                        fontWeight: 550,
                        letterSpacing: '-.035em',
                      }}
                    >
                      {typeof value === 'number' ? <AnimatedNumber value={value}/> : value}
                    </Typography>
                    <Typography
                      color='text.secondary'
                      sx={{ mt: 1 }}
                    >
                      {label}
                    </Typography>
                    {typeof value === 'number' && (
                      <Box sx={{ mt: 2.25, height: 3, bgcolor: '#F2F0EB', overflow: 'hidden' }}>
                        <Box
                          sx={{
                            height: '100%',
                            width: `${Math.max(12, Math.min(100, value))}%`,
                            bgcolor: '#D7263D',
                            transformOrigin: 'left',
                            animation: 'metricGrow .8s ease-out both',
                            '@keyframes metricGrow': {
                              from: { transform: 'scaleX(0)' },
                              to: { transform: 'scaleX(1)' },
                            },
                            '@media (prefers-reduced-motion: reduce)': {
                              animation: 'none',
                            },
                          }}
                        />
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>
      )}
    </Stack>
  );
};

export default HomePage;
