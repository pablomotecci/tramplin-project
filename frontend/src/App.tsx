import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './hooks/useAuth';
import { Header } from './components/layout/Header';
import { ProtectedRoute } from './components/route/ProtectedRoute';
import { HomePage } from './pages/HomePage';
import { ApplicantDashboard } from './pages/ApplicantDashboard';
import { EmployerDashboard } from './pages/EmployerDashboard';
import { CuratorDashboard } from './pages/CuratorDashboard';
import CreateOpportunity from './pages/CreateOpportunity';
import OpportunityPage from './pages/OpportunityPage';
import EditOpportunity from './pages/EditOpportunity';
import CompanyProfile from './pages/CompanyProfile';
import ApplicantProfile from './pages/ApplicantProfile';
import NotFoundPage from './pages/NotFoundPage';
import { Footer } from './components/layout/Footer';
import { ToastProvider } from './components/ui/Toast';

/*
Корневой компонент приложения
 
  AuthProvider — оборачивает всё приложение, даёт доступ к useAuth()
  любой компонент здесь может узнать вошел ли пользователь, 
  какая у него роль и вызвать функции login logout

Routes — маршруты страниц
  /profile — личный кабинет соискателя
  /company — личный кабинет работодателя
*/

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
        <Header />
        <Routes>
          <Route path="/" element={<HomePage />} />

          <Route
            path="/profile"
            element={
              <ProtectedRoute allowedRoles={['APPLICANT']}>
                <ApplicantDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/company"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <EmployerDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/curator"
            element={
              <ProtectedRoute allowedRoles={['CURATOR', 'ADMIN']}>
                <CuratorDashboard />
              </ProtectedRoute>
            }
          />
          

          <Route
            path="/company/opportunities/new"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <CreateOpportunity />
              </ProtectedRoute>
            }
          />

          <Route
            path="/company/opportunities/edit/:id"
            element={
              <ProtectedRoute allowedRoles={['EMPLOYER']}>
                <EditOpportunity />
              </ProtectedRoute>
            }
          />

          <Route path="/opportunities/:id" element={<OpportunityPage />} />
          <Route path="/company/:id" element={<CompanyProfile />} />
          <Route path="/applicant/:id" element={<ApplicantProfile />} />
          <Route path="*" element={<NotFoundPage />} />

        </Routes>
        <Footer />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;