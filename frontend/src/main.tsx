import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles/global.css';

/*
  точка входа React приложения
  StrictMode включает дополнительные проверки в dev режиме
*/


createRoot(document.getElementById('root')!).render(
  <StrictMode>   
    <App />
  </StrictMode>
);
