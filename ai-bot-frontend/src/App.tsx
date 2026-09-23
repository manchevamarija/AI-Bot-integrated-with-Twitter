import './App.css';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Outlet, Route, Routes } from 'react-router';
import { Box, CircularProgress } from '@mui/material';
import Layout from './ui/components/layout/Layout/Layout.tsx';
import ProtectedRoute from './ui/components/routing/ProtectedRoute/ProtectedRoute.tsx';
import SessionsProvider from './providers/sessionsProvider.tsx';

const HomePage = lazy(() => import('./ui/pages/home/HomePage/HomePage.tsx'));
const RegisterPage = lazy(() => import('./ui/pages/auth/RegisterPage/RegisterPage.tsx'));
const LoginPage = lazy(() => import('./ui/pages/auth/LoginPage/LoginPage.tsx'));
const SessionsPage = lazy(() => import('./ui/pages/session/SessionsPage/SessionsPage.tsx'));
const SessionDetailsPage = lazy(() => import('./ui/pages/session/SessionDetailsPage/SessionDetailsPage.tsx'));
const PostsPage = lazy(() => import('./ui/pages/post/PostsPage/PostsPage.tsx'));
const PostDetailsPage = lazy(() => import('./ui/pages/post/PostDetailsPage/PostDetailsPage.tsx'));
const TopPostsPage = lazy(() => import('./ui/pages/post/TopPostsPage/TopPostsPage.tsx'));
const DonationsPage = lazy(() => import('./ui/pages/donation/DonationsPage/DonationsPage.tsx'));

const PageLoader = () => (
  <Box
    role='status'
    aria-label='Се вчитува страницата'
    sx={{ minHeight: '45vh', display: 'grid', placeItems: 'center' }}
  >
    <CircularProgress size={32}/>
  </Box>
);

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader/>}>
        <Routes>
          <Route path='/register' element={<RegisterPage/>}/>
          <Route path='/login' element={<LoginPage/>}/>
          <Route path='/' element={<Layout/>}>
            <Route index element={<HomePage/>}/>
            <Route element={<ProtectedRoute/>}>
              <Route element={<SessionsProvider><Outlet/></SessionsProvider>}>
                <Route path='sessions' element={<SessionsPage/>}/>
                <Route path='sessions/:id' element={<SessionDetailsPage/>}/>
              </Route>
              <Route path='posts' element={<PostsPage/>}/>
              <Route path='posts/:id' element={<PostDetailsPage/>}/>
              <Route path='top' element={<TopPostsPage/>}/>
              <Route path='donations' element={<DonationsPage/>}/>
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
