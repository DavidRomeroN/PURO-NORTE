import { useState } from 'react'
import { Mail, MessageCircle } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { boletasApi } from '@/api/boletasApi'
import { mensajeDeError } from '@/api/axiosClient'
import { formatoMoneda } from '@/utils/formatoMoneda'

function mensajeWhatsapp(boleta) {
  const serie = `${boleta.serie ?? 'BA'}-${boleta.correlativo ?? boleta.id}`
  return (
    `¡Gracias por tu visita! 🍢\n` +
    `Anticuchería Puro Norte\n\n` +
    `Boleta ${serie}\n` +
    `Total: ${formatoMoneda(boleta.montoTotal)}\n\n` +
    `Descarga tu boleta aquí:\n${boleta.urlPublicaPdf}`
  )
}

export function EnviarBoletaDialog({ abierto, onClose, boleta }) {
  const [via, setVia] = useState(null)
  const [telefono, setTelefono] = useState('')
  const [correo, setCorreo] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (!abierto || !boleta) return null

  function reset() {
    setVia(null)
    setTelefono('')
    setCorreo('')
  }

  function cerrar() {
    reset()
    onClose()
  }

  async function enviarWhatsapp() {
    const digits = telefono.replace(/\D/g, '')
    if (!/^9\d{8}$/.test(digits)) {
      toast.error('Ingresa un número de 9 dígitos que empiece con 9')
      return
    }
    const texto = encodeURIComponent(mensajeWhatsapp(boleta))
    window.open(`https://wa.me/51${digits}?text=${texto}`, '_blank', 'noopener')
    try {
      await boletasApi.marcarWhatsapp(boleta.id)
    } catch {
      // no bloquea
    }
    toast.success('WhatsApp listo. Solo pulsa enviar.')
    cerrar()
  }

  async function enviarCorreo() {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim())) {
      toast.error('Revisa el correo, parece que falta algo')
      return
    }
    setEnviando(true)
    try {
      await boletasApi.enviarCorreo(boleta.id, correo.trim())
      toast.success('Correo en camino')
      cerrar()
    } catch (error) {
      toast.error(mensajeDeError(error) || 'No se pudo enviar. Puedes intentar por WhatsApp.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-carbon/50 p-4 sm:items-center">
      <div className="w-full max-w-md rounded-app bg-superficie p-5 shadow-lg">
        <p className="text-xl font-extrabold text-carbon">¿Cómo se la enviamos?</p>

        {!via ? (
          <div className="mt-4 flex flex-col gap-3">
            <Button tamano="grande" className="w-full min-h-14" onClick={() => setVia('wa')}>
              <MessageCircle size={22} />
              WhatsApp
            </Button>
            <Button
              variante="secundaria"
              tamano="grande"
              className="w-full min-h-14"
              onClick={() => setVia('mail')}
            >
              <Mail size={22} />
              Correo electrónico
            </Button>
            <Button variante="fantasma" className="w-full" onClick={cerrar}>
              Cancelar
            </Button>
          </div>
        ) : via === 'wa' ? (
          <div className="mt-4 flex flex-col gap-3">
            <Input
              inputMode="tel"
              placeholder="9xxxxxxxx"
              value={telefono}
              onChange={(e) => setTelefono(e.target.value)}
              autoFocus
            />
            <Button tamano="grande" className="w-full" onClick={enviarWhatsapp}>
              Abrir WhatsApp
            </Button>
            <Button variante="fantasma" onClick={() => setVia(null)}>
              Atrás
            </Button>
          </div>
        ) : (
          <div className="mt-4 flex flex-col gap-3">
            <Input
              inputMode="email"
              type="email"
              placeholder="cliente@ejemplo.com"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              autoFocus
            />
            <Button tamano="grande" className="w-full" disabled={enviando} onClick={enviarCorreo}>
              {enviando ? 'Enviando...' : 'Enviar correo'}
            </Button>
            <Button variante="fantasma" onClick={() => setVia(null)}>
              Atrás
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
