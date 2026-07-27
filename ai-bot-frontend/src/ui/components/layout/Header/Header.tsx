import './Header.css';
import {
  AppBar, Avatar, Box, Button, Drawer, IconButton, List, ListItem,
  ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import TravelExploreRoundedIcon from '@mui/icons-material/TravelExploreRounded';
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded';
import VolunteerActivismRoundedIcon from '@mui/icons-material/VolunteerActivismRounded';
import { Link, NavLink } from 'react-router';
import { useState } from 'react';
import AuthToggle from '../../auth/AuthToggle/AuthToggle.tsx';
import useAuth from '../../../../hooks/useAuth.ts';
import type { Role } from '../../../../api/types/user.ts';

interface Page {
  path: string;
  name: string;
  authenticated: boolean;
  icon: typeof HomeRoundedIcon;
  role?: Role;
}

const pages: Page[] = [
  { path: '/', name: 'Почетна', authenticated: false, icon: HomeRoundedIcon },
  { path: '/sessions', name: 'Сесии', authenticated: true, icon: TravelExploreRoundedIcon },
  { path: '/posts', name: 'Објави', authenticated: true, icon: ArticleRoundedIcon },
  { path: '/donations', name: 'Донации', authenticated: true, icon: VolunteerActivismRoundedIcon },
];

const Header = () => {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const { isLoggedIn, user } = useAuth();
  const visiblePages = pages.filter((page) =>
    (!page.authenticated || isLoggedIn) &&
    (!page.role || (user?.roles.includes(page.role) ?? false))
  );

  return (
    <>
      <AppBar position='sticky' color='inherit' elevation={0} className='app-header'>
        <Toolbar className='app-toolbar'>
          <IconButton
            edge='start'
            aria-label='Отвори мени'
            sx={{ display: { md: 'none' } }}
            onClick={() => setDrawerOpen(true)}
          >
            <MenuIcon/>
          </IconButton>

          <Box
            className='brand brand-link'
            component={Link}
            to='/'
            aria-label='Оди на почетна страница'
          >
            <Avatar className='brand-mark'><TravelExploreRoundedIcon fontSize='small'/></Avatar>
            <Box>
              <Typography className='brand-name'>AI BOT СО X</Typography>
              <Typography className='brand-caption'>Интеграција за македонски корпус</Typography>
            </Box>
          </Box>

          <Box className='desktop-nav'>
            {visiblePages.map((page) => (
              <Button
                key={page.path}
                component={NavLink}
                to={page.path}
                end={page.path === '/'}
                className='nav-button'
              >
                {page.name}
              </Button>
            ))}
          </Box>

          <Box sx={{ ml: 'auto' }}><AuthToggle/></Box>
        </Toolbar>
      </AppBar>

      <Drawer anchor='left' open={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Box sx={{ width: 280, p: 2 }} role='presentation'>
          <Box
            className='brand drawer-brand brand-link'
            component={Link}
            to='/'
            aria-label='Оди на почетна страница'
            onClick={() => setDrawerOpen(false)}
          >
            <Avatar className='brand-mark'><TravelExploreRoundedIcon fontSize='small'/></Avatar>
            <Typography className='brand-name'>AI BOT СО X</Typography>
          </Box>
          <List>
            {visiblePages.map((page) => {
              const Icon = page.icon;
              return (
                <ListItem key={page.path} disablePadding>
                  <ListItemButton
                    component={NavLink}
                    to={page.path}
                    onClick={() => setDrawerOpen(false)}
                    sx={{ borderRadius: 0, mb: 0.5 }}
                  >
                    <ListItemIcon><Icon color='primary'/></ListItemIcon>
                    <ListItemText primary={page.name}/>
                  </ListItemButton>
                </ListItem>
              );
            })}
          </List>
        </Box>
      </Drawer>
    </>
  );
};

export default Header;
