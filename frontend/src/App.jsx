import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AuthProvider } from '@/context/AuthContext'
import { RutaProtegida } from '@/components/common/RutaProtegida'
import { LoginPage } from '@/features/auth/LoginPage'
import { MesasPage } from '@/features/mesas/MesasPage'
import { PedidoPage } from '@/features/pedido/PedidoPage'
import { ParrillaPage } from '@/features/parrilla/ParrillaPage'
import { CajaPage } from '@/features/caja/CajaPage'
import { CobrarPage } from '@/features/caja/CobrarPage'
import { AdminPage } from '@/features/admin/AdminPage'
import { ProductosPage } from '@/features/admin/ProductosPage'
import { CombosPage } from '@/features/admin/CombosPage'
import { UsuariosPage } from '@/features/admin/UsuariosPage'
import { MesasAdminPage } from '@/features/admin/MesasAdminPage'
import { VentasDiaPage } from '@/features/admin/VentasDiaPage'
import { ROLES } from '@/utils/constantes'

const TODOS = [ROLES.MOZO, ROLES.CAJA, ROLES.ADMIN]
const CAJA = [ROLES.CAJA, ROLES.ADMIN]
const SOLO_ADMIN = [ROLES.ADMIN]

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: true,
      staleTime: 10_000,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />

            <Route
              path="/mesas"
              element={
                <RutaProtegida roles={TODOS}>
                  <MesasPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/pedido/:mesaId"
              element={
                <RutaProtegida roles={TODOS}>
                  <PedidoPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/llevar/:pedidoId"
              element={
                <RutaProtegida roles={TODOS}>
                  <PedidoPage paraLlevar />
                </RutaProtegida>
              }
            />
            <Route
              path="/parrilla"
              element={
                <RutaProtegida roles={TODOS}>
                  <ParrillaPage />
                </RutaProtegida>
              }
            />

            <Route
              path="/caja"
              element={
                <RutaProtegida roles={CAJA}>
                  <CajaPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/cobrar/:pedidoId"
              element={
                <RutaProtegida roles={CAJA}>
                  <CobrarPage />
                </RutaProtegida>
              }
            />

            <Route
              path="/admin"
              element={
                <RutaProtegida roles={SOLO_ADMIN}>
                  <AdminPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/productos"
              element={
                <RutaProtegida roles={SOLO_ADMIN}>
                  <ProductosPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/combos"
              element={
                <RutaProtegida roles={SOLO_ADMIN}>
                  <CombosPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/usuarios"
              element={
                <RutaProtegida roles={SOLO_ADMIN}>
                  <UsuariosPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/mesas"
              element={
                <RutaProtegida roles={SOLO_ADMIN}>
                  <MesasAdminPage />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/ventas"
              element={
                <RutaProtegida roles={SOLO_ADMIN}>
                  <VentasDiaPage />
                </RutaProtegida>
              }
            />

            <Route path="*" element={<Navigate to="/mesas" replace />} />
          </Routes>

          {/* Desplazado para no taparse con la cabecera pegajosa. */}
          <Toaster
            position="top-center"
            richColors
            offset="5rem"
            toastOptions={{
              style: {
                fontSize: '1.0625rem',
                fontWeight: 600,
                padding: '1rem',
                borderRadius: '0.875rem',
              },
            }}
          />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
