import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Copy, Eye, EyeOff, UserPlus } from 'lucide-react'
import { toast } from 'sonner'
import { AppShell } from '@/components/layout/AppShell'
import { BarraAccion } from '@/components/layout/BarraAccion'
import { EstadoCarga } from '@/components/common/EstadoCarga'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardBoton } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { usuariosApi } from '@/api/usuariosApi'
import { mensajeDeError } from '@/api/axiosClient'
import { useAuth } from '@/hooks/useAuth'
import { INFO_ROLES, ROLES } from '@/utils/constantes'
import { cn } from '@/utils/cn'

const NOTA_ULTIMO_ADMIN = 'Debe haber al menos un administrador.'

export function UsuariosPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { usuario: yo } = useAuth()

  const [editando, setEditando] = useState(null)
  const [cambiandoPassword, setCambiandoPassword] = useState(null)
  const [porQuitarAcceso, setPorQuitarAcceso] = useState(null)

  const { data, isPending } = useQuery({
    queryKey: ['usuarios'],
    queryFn: () => usuariosApi.listar(),
  })

  const refrescar = () => queryClient.invalidateQueries({ queryKey: ['usuarios'] })

  const guardar = useMutation({
    mutationFn: ({ id, persona }) =>
      id
        ? usuariosApi.actualizar(id, { nombre: persona.nombre, rol: persona.rol })
        : usuariosApi.crear(persona),
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const cambiarAcceso = useMutation({
    mutationFn: ({ id, activo }) => usuariosApi.cambiarEstado(id, activo),
    onSuccess: (persona) => {
      refrescar()
      setPorQuitarAcceso(null)
      toast.success(
        persona.activo
          ? `${persona.nombre} ya puede entrar al sistema`
          : `${persona.nombre} ya no puede entrar al sistema`,
      )
    },
    onError: (error) => {
      setPorQuitarAcceso(null)
      toast.error(mensajeDeError(error))
    },
  })

  const cambiarPassword = useMutation({
    mutationFn: ({ id, password }) => usuariosApi.cambiarPassword(id, password),
    onSuccess: () => {
      setCambiandoPassword(null)
      toast.success('Contraseña actualizada')
    },
    onError: (error) => toast.error(mensajeDeError(error)),
  })

  const personas = ordenar(data ?? [])
  const administradoresActivos = personas.filter(
    (p) => p.rol === ROLES.ADMIN && p.activo,
  ).length

  const esUltimoAdmin = (persona) =>
    persona.rol === ROLES.ADMIN && persona.activo && administradoresActivos === 1

  return (
    <>
      <AppShell
        titulo="Personal"
        subtitulo="Quién puede usar el sistema"
        onVolver={() => navigate('/admin')}
        conNav={false}
        ancho="ancho"
        barraInferior={
          <BarraAccion
            etiqueta="Agregar persona"
            icono={UserPlus}
            onAccion={() => setEditando({ rol: ROLES.MOZO })}
          />
        }
      >
        {isPending ? (
          <EstadoCarga filas={4} alto="h-20" />
        ) : (
          <ul className="grid gap-2 lg:grid-cols-2">
            {personas.map((persona) => {
              const soyYo = persona.usuario === yo?.usuario
              const bloqueado = esUltimoAdmin(persona)

              return (
                <li key={persona.id}>
                  <Card
                    className={cn(
                      'flex items-center gap-3 p-3',
                      !persona.activo && 'bg-hundido/50 opacity-75',
                    )}
                  >
                    <span
                      aria-hidden="true"
                      className="flex size-12 shrink-0 items-center justify-center rounded-full bg-brasa-100 text-lg font-extrabold uppercase text-brasa-700"
                    >
                      {iniciales(persona.nombre)}
                    </span>

                    <button
                      type="button"
                      onClick={() => setEditando(persona)}
                      className="min-w-0 grow rounded-app py-1 text-left"
                    >
                      <span className="flex flex-wrap items-center gap-2">
                        <span className="truncate text-lg font-bold text-carbon">
                          {persona.nombre}
                        </span>
                        {soyYo ? <Badge tono="brasa">Eres tú</Badge> : null}
                        {!persona.activo ? <Badge>Sin acceso</Badge> : null}
                      </span>
                      <span className="block truncate text-base text-tinta">
                        {INFO_ROLES[persona.rol].etiqueta} · {INFO_ROLES[persona.rol].descripcion}
                      </span>
                      {bloqueado ? (
                        <span className="mt-1 block text-sm text-tinta">{NOTA_ULTIMO_ADMIN}</span>
                      ) : null}
                    </button>

                    {/* Nadie puede quitarse el acceso a sí mismo: mejor no ofrecer el control. */}
                    {soyYo ? null : (
                      <Switch
                        checked={persona.activo}
                        disabled={bloqueado}
                        aria-label={`${persona.activo ? 'Quitar' : 'Dar'} acceso a ${persona.nombre}`}
                        onCheckedChange={(activo) => {
                          if (activo) {
                            cambiarAcceso.mutate({ id: persona.id, activo: true })
                          } else {
                            setPorQuitarAcceso(persona)
                          }
                        }}
                      />
                    )}
                  </Card>
                </li>
              )
            })}
          </ul>
        )}
      </AppShell>

      <SheetPersona
        persona={editando}
        guardando={guardar.isPending}
        bloquearRol={editando?.id ? esUltimoAdmin(editando) : false}
        onCerrar={() => setEditando(null)}
        onGuardar={async (persona) => {
          const guardada = await guardar.mutateAsync({ id: editando?.id, persona })
          refrescar()
          return guardada
        }}
        onCambiarPassword={(persona) => {
          setEditando(null)
          setCambiandoPassword(persona)
        }}
      />

      <SheetPassword
        persona={cambiandoPassword}
        guardando={cambiarPassword.isPending}
        onCerrar={() => setCambiandoPassword(null)}
        onGuardar={(password) =>
          cambiarPassword.mutate({ id: cambiandoPassword.id, password })
        }
      />

      <ConfirmDialog
        abierto={Boolean(porQuitarAcceso)}
        onOpenChange={(abierto) => !abierto && setPorQuitarAcceso(null)}
        titulo={`¿Quitar el acceso de ${porQuitarAcceso?.nombre}?`}
        descripcion="No podrá entrar al sistema, pero sus pedidos anteriores se mantienen."
        textoConfirmar="Sí, quitar acceso"
        destructivo
        enProceso={cambiarAcceso.isPending}
        onConfirmar={() => cambiarAcceso.mutate({ id: porQuitarAcceso.id, activo: false })}
      />
    </>
  )
}

function SheetPersona({ persona, bloquearRol, onCerrar, onGuardar, onCambiarPassword, guardando }) {
  const esNueva = !persona?.id
  const [paso, setPaso] = useState('form')
  const [nombre, setNombre] = useState('')
  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const [rol, setRol] = useState(ROLES.MOZO)

  useEffect(() => {
    if (persona) {
      setPaso('form')
      setNombre(persona.nombre ?? '')
      setUsuario(persona.usuario ?? '')
      setPassword('')
      setVerPassword(false)
      setRol(persona.rol ?? ROLES.MOZO)
    }
  }, [persona])

  const nombreValido = nombre.trim().length > 0
  const usuarioValido = /^[a-z0-9._-]{3,50}$/.test(usuario)
  const passwordValida = password.length >= 6
  const listo = esNueva
    ? nombreValido && usuarioValido && passwordValida
    : nombreValido && (rol === persona?.rol || !bloquearRol)

  const cambiaRol = !esNueva && rol !== persona?.rol

  async function confirmar() {
    try {
      const guardada = await onGuardar({ nombre: nombre.trim(), usuario, password, rol })
      if (esNueva) {
        setPaso('exito')
      } else {
        onCerrar()
        toast.success(`${guardada.nombre} actualizado`)
      }
    } catch {
      // El error ya se avisó con un toast; vuelve al formulario para poder corregirlo.
      setPaso('form')
    }
  }

  async function copiarCredenciales() {
    try {
      await navigator.clipboard.writeText(`Usuario: ${usuario}\nContraseña: ${password}`)
      toast.success('Datos copiados')
    } catch {
      toast.error('No se pudo copiar. Anota los datos a mano.')
    }
  }

  return (
    <Sheet open={Boolean(persona)} onOpenChange={(abierto) => !abierto && onCerrar()}>
      <SheetContent
        titulo={paso === 'exito' ? 'Listo' : esNueva ? 'Agregar persona' : 'Editar persona'}
      >
        {paso === 'exito' ? (
          <div className="flex flex-col gap-4 pb-2">
            <p className="text-lg text-carbon">
              <strong>{nombre.trim()}</strong> ya puede entrar como{' '}
              {INFO_ROLES[rol].etiqueta.toLowerCase()}.
            </p>

            <div className="rounded-app border-2 border-brasa-200 bg-brasa-50 p-4">
              <p className="text-base text-tinta">Usuario</p>
              <p className="text-2xl font-extrabold text-carbon">{usuario}</p>
              <p className="mt-3 text-base text-tinta">Contraseña</p>
              <p className="text-2xl font-extrabold text-carbon">{password}</p>
            </div>

            <p className="text-base font-semibold text-alerta">
              Anota estos datos, la contraseña no se vuelve a mostrar.
            </p>

            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={copiarCredenciales}>
                <Copy size={22} />
                Copiar datos
              </Button>
              <Button tamano="grande" onClick={onCerrar}>
                Entendido
              </Button>
            </div>
          </div>
        ) : paso === 'confirmar' ? (
          <div className="flex flex-col gap-4 pb-2">
            <p className="rounded-app border-2 border-brasa-200 bg-brasa-50 p-4 text-lg text-carbon">
              {esNueva
                ? `Se creará el acceso de ${nombre.trim()} como ${INFO_ROLES[rol].etiqueta.toLowerCase()}. ${INFO_ROLES[rol].descripcion}.`
                : `${nombre.trim()} pasará de ${INFO_ROLES[persona.rol].etiqueta.toLowerCase()} a ${INFO_ROLES[rol].etiqueta.toLowerCase()}. ${INFO_ROLES[rol].descripcion}.`}
            </p>

            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={() => setPaso('form')}>
                Cancelar
              </Button>
              <Button tamano="grande" disabled={guardando} onClick={confirmar}>
                {guardando ? 'Guardando...' : 'Guardar'}
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-4 pb-2">
            <div className="flex flex-col gap-2">
              <Label htmlFor="persona-nombre">Nombre completo</Label>
              <Input
                id="persona-nombre"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                maxLength={100}
              />
            </div>

            {esNueva ? (
              <>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="persona-usuario">Usuario</Label>
                  <Input
                    id="persona-usuario"
                    value={usuario}
                    onChange={(e) => setUsuario(e.target.value.toLowerCase().replace(/\s/g, ''))}
                    autoCapitalize="none"
                    autoCorrect="off"
                    spellCheck="false"
                    maxLength={50}
                  />
                  <p className="text-sm text-tinta">
                    Sin espacios ni mayúsculas. Es lo que escribirá para entrar.
                  </p>
                </div>

                <div className="flex flex-col gap-2">
                  <Label htmlFor="persona-password">Contraseña</Label>
                  <div className="relative">
                    <Input
                      id="persona-password"
                      type={verPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="pr-14"
                    />
                    <button
                      type="button"
                      onClick={() => setVerPassword((v) => !v)}
                      aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                      className="absolute right-1 top-1/2 flex min-h-12 min-w-12 -translate-y-1/2 items-center justify-center rounded-app text-tinta active:bg-fondo"
                    >
                      {verPassword ? <EyeOff size={24} /> : <Eye size={24} />}
                    </button>
                  </div>
                  <p className="text-sm text-tinta">Mínimo 6 caracteres.</p>
                </div>
              </>
            ) : null}

            <div className="flex flex-col gap-2">
              <Label>¿Qué puede hacer?</Label>
              {Object.entries(INFO_ROLES).map(([valor, info]) => (
                <CardBoton
                  key={valor}
                  seleccionada={rol === valor}
                  aria-pressed={rol === valor}
                  disabled={bloquearRol}
                  onClick={() => setRol(valor)}
                  className="min-h-16 px-4 py-2"
                >
                  <span className="block text-lg font-bold text-carbon">{info.etiqueta}</span>
                  <span className="block text-base text-tinta">{info.descripcion}</span>
                </CardBoton>
              ))}
              {bloquearRol ? (
                <p className="text-base font-semibold text-tinta">{NOTA_ULTIMO_ADMIN}</p>
              ) : null}
            </div>

            {!esNueva ? (
              <Button variante="secundaria" tamano="grande" onClick={() => onCambiarPassword(persona)}>
                Cambiar contraseña
              </Button>
            ) : null}

            <div className="flex flex-col gap-3">
              <Button variante="secundaria" tamano="grande" onClick={onCerrar}>
                Cancelar
              </Button>
              <Button
                tamano="grande"
                disabled={!listo || guardando}
                onClick={() => (esNueva || cambiaRol ? setPaso('confirmar') : confirmar())}
              >
                {guardando ? 'Guardando...' : 'Guardar'}
              </Button>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}

function SheetPassword({ persona, onCerrar, onGuardar, guardando }) {
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)

  useEffect(() => {
    if (persona) {
      setPassword('')
      setVerPassword(false)
    }
  }, [persona])

  return (
    <Sheet open={Boolean(persona)} onOpenChange={(abierto) => !abierto && onCerrar()}>
      <SheetContent titulo="Cambiar contraseña">
        <div className="flex flex-col gap-4 pb-2">
          <p className="text-base text-tinta">
            La contraseña actual de {persona?.nombre} no se puede ver, solo reemplazar.
          </p>

          <div className="flex flex-col gap-2">
            <Label htmlFor="password-nueva">Contraseña nueva</Label>
            <div className="relative">
              <Input
                id="password-nueva"
                type={verPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pr-14"
              />
              <button
                type="button"
                onClick={() => setVerPassword((v) => !v)}
                aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                className="absolute right-1 top-1/2 flex min-h-12 min-w-12 -translate-y-1/2 items-center justify-center rounded-app text-tinta active:bg-fondo"
              >
                {verPassword ? <EyeOff size={24} /> : <Eye size={24} />}
              </button>
            </div>
            <p className="text-sm text-tinta">Mínimo 6 caracteres.</p>
          </div>

          <div className="flex flex-col gap-3">
            <Button variante="secundaria" tamano="grande" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button
              tamano="grande"
              disabled={password.length < 6 || guardando}
              onClick={() => onGuardar(password)}
            >
              {guardando ? 'Guardando...' : 'Guardar'}
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

/** Dos letras para reconocer a la persona de un vistazo en la lista. */
function iniciales(nombre) {
  return (nombre ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((palabra) => palabra[0])
    .join('')
}

/** Con acceso primero y alfabéticos; los que no pueden entrar, al final. */
function ordenar(personas) {
  return [...personas].sort((a, b) => {
    if (a.activo !== b.activo) return a.activo ? -1 : 1
    return a.nombre.localeCompare(b.nombre, 'es')
  })
}
