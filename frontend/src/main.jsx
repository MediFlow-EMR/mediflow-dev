import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import router from './routes.jsx';
import './styles/global.css';
import './styles/theme.css';
import './styles/ui.css';
import { applyTheme, readTheme } from './utils/theme';

applyTheme(readTheme());

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.Suspense fallback={<div role="status" className="route-loading">화면을 불러오는 중…</div>}>
        <RouterProvider router={router} />
    </React.Suspense>
);