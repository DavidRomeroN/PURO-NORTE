import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Flame } from 'lucide-react'
import { mensajeDeError } from '@/api/axiosClient'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/hooks/useAuth'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function alEnviar(evento) {
    evento.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await login(usuario.trim(), password)
      navigate('/mesas', { replace: true })
    } catch (fallo) {
      setError(mensajeDeError(fallo))
      setEnviando(false)
    }
  }

  return (
    <div className="min-h-dvh bg-carbon lg:grid lg:grid-cols-2">
      {/* En escritorio la marca ocupa su propia mitad; en celular se reduce a la cabecera. */}
      <section className="relative flex flex-col justify-center overflow-hidden px-6 py-12 lg:px-14">
        <div
          aria-hidden="true"
          className="absolute -left-24 top-1/3 size-[28rem] rounded-full bg-brasa-600/25 blur-3xl"
        />
        <div className="relative">
          <span className="flex size-16 items-center justify-center rounded-app-lg bg-brasa-600 shadow-brasa">
            <Flame size={34} className="text-white" fill="currentColor" />
          </span>
          <h1 className="mt-6 text-4xl font-extrabold leading-none text-white lg:text-6xl">
            Puro Norte
          </h1>
          <p className="mt-3 max-w-sm text-lg text-white/70 lg:text-xl">
            Anticuchería. Toma pedidos, cobra y emite boletas desde el celular.
          </p>
        </div>
      </section>

      <section className="flex items-start justify-center rounded-t-[2rem] bg-fondo px-5 py-10 lg:items-center lg:rounded-none lg:px-14">
        <div className="w-full max-w-sm">
          <h2 className="text-2xl font-extrabold text-carbon">Ingresa para empezar el turno</h2>

          <form onSubmit={alEnviar} className="mt-6 flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <Label htmlFor="usuario">Usuario</Label>
              <Input
                id="usuario"
                name="usuario"
                value={usuario}
                onChange={(e) => setUsuario(e.target.value)}
                autoCapitalize="none"
                autoCorrect="off"
                autoComplete="username"
                spellCheck="false"
                required
              />
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Contraseña</Label>
              <div className="relative">
                <Input
                  id="password"
                  name="password"
                  type={verPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  className="pr-14"
                  required
                />
                <button
                  type="button"
                  onClick={() => setVerPassword((visible) => !visible)}
                  aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  className="absolute right-1 top-1/2 flex min-h-12 min-w-12 -translate-y-1/2 items-center justify-center rounded-app text-tinta transition-colors hover:bg-hundido hover:text-carbon"
                >
                  {verPassword ? <EyeOff size={24} /> : <Eye size={24} />}
                </button>
              </div>
            </div>

            {error ? (
              <p
                role="alert"
                className="rounded-app border-2 border-alerta/30 bg-alerta-suave px-4 py-3 text-base font-semibold text-alerta"
              >
                {error}
              </p>
            ) : null}

            <Button type="submit" tamano="grande" disabled={enviando} className="mt-1 w-full">
              {enviando ? 'Entrando...' : 'Entrar'}
            </Button>
          </form>
        </div>
      </section>
    </div>
  )
}
