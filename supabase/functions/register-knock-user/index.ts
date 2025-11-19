import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const KNOCK_SECRET_KEY = Deno.env.get('KNOCK_SECRET_KEY')!
const KNOCK_API_URL = 'https://api.knock.app'

interface RegisterUserRequest {
  user_id: string
  fcm_token: string
}

Deno.serve(async (req) => {
  try {
    const { user_id, fcm_token }: RegisterUserRequest = await req.json()
    
    console.log('Registrando usuario en Knock:', user_id)
    
    // Primero, identificar al usuario en Knock
    const identifyResponse = await fetch(`${KNOCK_API_URL}/v1/users/${user_id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${KNOCK_SECRET_KEY}`
      },
      body: JSON.stringify({
        id: user_id
      })
    })
    
    if (!identifyResponse.ok) {
      const error = await identifyResponse.text()
      console.error('Error identificando usuario:', error)
      throw new Error(`Error identificando usuario: ${error}`)
    }
    
    console.log('Usuario identificado en Knock')
    
    // Luego, registrar el token FCM para el canal
    // Usando el UUID del canal en lugar del key
    const channelDataResponse = await fetch(
      `${KNOCK_API_URL}/v1/users/${user_id}/channel_data/dcc00202-f7b7-4a73-8b4d-44ab550e0c3e`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${KNOCK_SECRET_KEY}`
        },
        body: JSON.stringify({
          data: {
            tokens: [fcm_token]
          }
        })
      }
    )
    
    if (!channelDataResponse.ok) {
      const error = await channelDataResponse.text()
      console.error('Error registrando token FCM:', error)
      throw new Error(`Error registrando token FCM: ${error}`)
    }
    
    const channelResult = await channelDataResponse.json()
    console.log('Token FCM registrado en Knock:', channelResult)
    
    return new Response(JSON.stringify({ 
      success: true,
      message: 'Usuario y token registrados en Knock'
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
