import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const KNOCK_SECRET_KEY = Deno.env.get('KNOCK_SECRET_KEY')!
const KNOCK_API_URL = 'https://api.knock.app'

interface Notification {
  id: string
  user_id: string
  movie_id: string
  group_id: string
  type: string
  title: string
  body: string
  data: any
}

interface WebhookPayload {
  type: 'INSERT'
  table: string
  record: Notification
  schema: 'public'
}

Deno.serve(async (req) => {
  try {
    const payload: WebhookPayload = await req.json()
    
    console.log('Webhook recibido:', payload)
    
    // Solo procesar notificaciones de tipo rating_request
    if (payload.record.type !== 'rating_request') {
      return new Response(JSON.stringify({ message: 'Tipo de notificación ignorado' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    }
    
    // Extraer datos
    const movieTitle = payload.record.data?.movieTitle || 'una película'
    const groupName = payload.record.body.match(/El grupo "(.+)" ha terminado/)?.[1] || 'un grupo'
    
    console.log('Trigger Knock workflow para usuario:', payload.record.user_id)
    
    // Trigger Knock workflow
    const knockResponse = await fetch(`${KNOCK_API_URL}/v1/workflows/movie-rating-request/trigger`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${KNOCK_SECRET_KEY}`
      },
      body: JSON.stringify({
        recipients: [payload.record.user_id],
        data: {
          movie_id: payload.record.movie_id,
          group_id: payload.record.group_id,
          movie_title: movieTitle,
          group_name: groupName
        }
      })
    })
    
    const result = await knockResponse.json()
    console.log('Respuesta de Knock:', result)
    
    return new Response(JSON.stringify({ 
      success: true, 
      knock_response: result 
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    })
    
  } catch (error) {
    console.error('Error:', error)
    return new Response(JSON.stringify({ 
      error: error.message 
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    })
  }
})
