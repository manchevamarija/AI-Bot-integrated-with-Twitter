import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.tsx';
import AuthProvider from './providers/authProvider.tsx';
import SnackbarProvider from './providers/snackbarProvider.tsx';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#D7263D', dark: '#B61E32', light: '#FBE8EB' },
    secondary: { main: '#151515' },
    success: { main: '#151515' },
    warning: { main: '#D7263D' },
    error: { main: '#B61E32' },
    background: { default: '#FCFCFA', paper: '#FFFFFF' },
    text: { primary: '#151515', secondary: '#666666' },
  },
  typography: {
    fontFamily: '"Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
    h3: { fontWeight: 700, letterSpacing: '-0.035em' },
    h4: { fontWeight: 700, letterSpacing: '-0.03em' },
    h5: { fontWeight: 700, letterSpacing: '-0.02em' },
    h6: { fontWeight: 700 },
    button: {
      textTransform: 'none',
      fontWeight: 600,
      fontSize: '0.9rem',
      letterSpacing: '0.01em',
    },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #ECE3D3',
          boxShadow: 'none',
          backgroundImage: 'none',
        },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          minHeight: 42,
          borderRadius: 999,
          paddingInline: 22,
          borderWidth: 1,
          transition: 'background-color 160ms ease, color 160ms ease, border-color 160ms ease, transform 160ms ease',
          '& .MuiButton-startIcon, & .MuiButton-endIcon': {
            transition: 'transform 140ms ease',
          },
          '&:hover .MuiButton-endIcon': {
            transform: 'translateX(2px)',
          },
          '&:focus-visible': {
            outline: '2px solid #151515',
            outlineOffset: 3,
          },
          '&.MuiButton-containedPrimary': {
            color: '#fff',
            backgroundColor: '#D7263D',
            border: '1px solid #D7263D',
            '&:hover': {
              backgroundColor: '#B61E32',
              borderColor: '#B61E32',
              transform: 'translateY(-1px)',
            },
          },
          '&.MuiButton-containedSecondary': {
            color: '#fff',
            backgroundColor: '#151515',
            border: '1px solid #151515',
            '&:hover': { backgroundColor: '#333333', transform: 'translateY(-1px)' },
          },
          '&.MuiButton-containedSuccess': {
            color: '#fff',
            backgroundColor: '#151515',
            border: '1px solid #151515',
            '&:hover': { backgroundColor: '#333333', borderColor: '#333333' },
          },
          '&.MuiButton-outlined': {
            color: '#151515',
            backgroundColor: 'transparent',
            borderColor: '#151515',
            '&:hover': {
              color: '#fff',
              backgroundColor: '#151515',
              borderColor: '#151515',
              transform: 'translateY(-1px)',
            },
          },
          '&.MuiButton-outlinedError': {
            color: '#B61E32',
            borderColor: '#D7263D',
            '&:hover': {
              color: '#fff',
              backgroundColor: '#D7263D',
              borderColor: '#D7263D',
            },
          },
          '&.MuiButton-text': {
            minHeight: 36,
            paddingInline: 10,
            color: '#151515',
            '&:hover': {
              color: '#D7263D',
              backgroundColor: '#FBE8EB',
            },
          },
          '&.Mui-disabled': {
            color: '#999999',
            backgroundColor: '#F2F0EB',
            borderColor: '#DED8CD',
          },
          '&.MuiButton-sizeSmall': {
            minHeight: 34,
            paddingInline: 12,
            fontSize: '0.82rem',
          },
        },
      },
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 600, borderRadius: 999 } } },
    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 16, border: '1px solid #ECE3D3', boxShadow: '0 24px 70px rgba(21,21,21,.16)' },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: { root: { borderRadius: 10, backgroundColor: '#fff' } },
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline/>
      <AuthProvider>
        <SnackbarProvider>
          <App/>
        </SnackbarProvider>
      </AuthProvider>
    </ThemeProvider>
  </StrictMode>
);
