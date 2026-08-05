import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/hooks/useAuth'
import { CLAVE_TOKEN } from '@/utils/constantes'
import { urlWebSocket } from './wsUrl'

const RealtimeContext = createContext({ conectado: false })

export function RealtimeProvider({ children }) {
  const { usuario } = useAuth()
  const queryClient = useQueryClient()
  const [conectado, setConectado] = useState(false)
  const clientRef = useRef(null)

  useEffect(() => {
    if (!usuario) {
      clientRef.current?.deactivate()
      clientRef.current = null
      setConectado(false)
      return
    }

    const token = localStorage.getItem(CLAVE_TOKEN)
    if (!token) return

    let cancelado = false
    let reintentos = 0

    // Import dinámico: evita que sockjs tumbe el bundle al cargar el login.
    import('sockjs-client')
      .then(({ default: SockJS }) => {
        if (cancelado) return

        const client = new Client({
          webSocketFactory: () => new SockJS(urlWebSocket(token)),
          connectHeaders: { Authorization: `Bearer ${token}` },
          reconnectDelay: 0,
          heartbeatIncoming: 10000,
          heartbeatOutgoing: 10000,
          onConnect: () => {
            setConectado(true)
            reintentos = 0
            queryClient.invalidateQueries({ queryKey: ['mesas'] })
            queryClient.invalidateQueries({ queryKey: ['pedidos'] })

            client.subscribe('/topic/mesas', (frame) => {
              try {
                const mesas = JSON.parse(frame.body)
                queryClient.setQueryData(['mesas'], mesas)
                queryClient.invalidateQueries({ queryKey: ['pedidos', 'activos'] })
              } catch {
                queryClient.invalidateQueries({ queryKey: ['mesas'] })
              }
            })
            client.subscribe('/topic/pedidos', (frame) => {
              try {
                const pedido = JSON.parse(frame.body)
                queryClient.setQueryData(['pedido', pedido.id], pedido)
                queryClient.invalidateQueries({ queryKey: ['pedidos'] })
              } catch {
                queryClient.invalidateQueries({ queryKey: ['pedidos'] })
              }
            })
            client.subscribe('/topic/parrilla', () => {
              queryClient.invalidateQueries({ queryKey: ['pedidos', 'parrilla'] })
            })
          },
          onDisconnect: () => setConectado(false),
          onStompError: () => setConectado(false),
          onWebSocketClose: () => {
            setConectado(false)
            reintentos += 1
            const delay = Math.min(30_000, 1000 * 2 ** Math.min(reintentos, 5))
            client.reconnectDelay = delay
          },
        })

        client.activate()
        clientRef.current = client
      })
      .catch((err) => {
        console.warn('No se pudo iniciar WebSocket:', err)
        setConectado(false)
      })

    return () => {
      cancelado = true
      clientRef.current?.deactivate()
      clientRef.current = null
      setConectado(false)
    }
  }, [usuario, queryClient])

  const value = useMemo(() => ({ conectado }), [conectado])
  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>
}

export function useRealtime() {
  return useContext(RealtimeContext)
}
