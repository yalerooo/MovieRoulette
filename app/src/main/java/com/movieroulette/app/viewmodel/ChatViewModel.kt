package com.movieroulette.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.*
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.utils.E2EEncryption
import com.movieroulette.app.utils.EncryptedMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

class ChatViewModel : ViewModel() {
    
    private val _messagesState = MutableStateFlow<MessagesState>(MessagesState.Loading)
    val messagesState: StateFlow<MessagesState> = _messagesState
    
    private val _otherUserProfile = MutableStateFlow<UserProfile?>(null)
    val otherUserProfile: StateFlow<UserProfile?> = _otherUserProfile
    
    private var myKeyPair: KeyPair? = null
    private var myPrivateKey: PrivateKey? = null
    private var otherUserPublicKey: PublicKey? = null
    private var realtimeChannel: RealtimeChannel? = null
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var lastMessageTimestamp: String? = null // Timestamp del último mensaje conocido
    private var isLoadingOlderMessages = false
    private val messagesPerPage = 30 // Número de mensajes a cargar por página
    private var hasMoreMessages = true // Indica si hay más mensajes antiguos para cargar
    
    sealed class MessagesState {
        object Loading : MessagesState()
        data class Success(val messages: List<DecryptedChatMessage>) : MessagesState()
        data class Error(val message: String) : MessagesState()
    }
    
    /**
     * Inicializa el chat cargando claves y mensajes
     */
    fun initializeChat(context: Context, otherUserId: String) {
        viewModelScope.launch {
            try {
                // Asegurar que yo tengo claves
                com.movieroulette.app.utils.KeyManager.ensureUserHasKeys(context)
                
                // Cargar perfil del otro usuario
                loadOtherUserProfile(otherUserId)
                
                // Inicializar claves de cifrado
                initializeEncryptionKeys(context, otherUserId)
                
                // Cargar mensajes existentes
                loadMessages(otherUserId)
                
                // Marcar mensajes del otro usuario como leídos
                markMessagesAsRead(otherUserId)
                
                // Suscribirse a nuevos mensajes en tiempo real
                subscribeToMessages(otherUserId)
                
                // RESPALDO: Polling cada 3 segundos para actualizar mensajes
                // Esto asegura que funcione incluso si Realtime no está configurado
                startPollingMessages(otherUserId)
            } catch (e: Exception) {
                _messagesState.value = MessagesState.Error(e.message ?: "Error al inicializar chat")
            }
        }
    }
    
    /**
     * Carga el perfil del otro usuario
     */
    private suspend fun loadOtherUserProfile(otherUserId: String) {
        try {
            val profile = SupabaseConfig.client.from("profiles")
                .select {
                    filter {
                        eq("id", otherUserId)
                    }
                }
                .decodeSingle<UserProfile>()
            _otherUserProfile.value = profile
        } catch (e: Exception) {
            // Perfil no encontrado
        }
    }
    
    /**
     * Inicializa las claves de cifrado
     */
    private suspend fun initializeEncryptionKeys(context: Context, otherUserId: String) {
        val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
        
        android.util.Log.d("ChatViewModel", "Initializing encryption keys for users: $currentUserId -> $otherUserId")
        
        // Obtener o generar mi par de claves
        myKeyPair = getOrCreateKeyPair(context, currentUserId)
        myPrivateKey = myKeyPair?.private
        
        android.util.Log.d("ChatViewModel", "My keypair loaded: ${myKeyPair != null}")
        
        // Obtener clave pública del otro usuario
        otherUserPublicKey = getPublicKey(otherUserId)
        
        android.util.Log.d("ChatViewModel", "Other user public key loaded: ${otherUserPublicKey != null}")
    }
    
    /**
     * Obtiene o crea el par de claves del usuario actual
     */
    private suspend fun getOrCreateKeyPair(context: Context, userId: String): KeyPair {
        // Primero intentar cargar desde SharedPreferences
        val sharedPrefs = context.getSharedPreferences("e2e_keys", Context.MODE_PRIVATE)
        val privateKeyString = sharedPrefs.getString("private_key", null)
        
        if (privateKeyString != null) {
            // Recuperar par de claves existente
            try {
                val privateKey = E2EEncryption.stringToPrivateKey(privateKeyString)
                val publicKeyString = sharedPrefs.getString("public_key", null)!!
                val publicKey = E2EEncryption.stringToPublicKey(publicKeyString)
                // Crear un nuevo KeyPair con las claves recuperadas
                val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
                return KeyPair(publicKey, privateKey)
            } catch (e: Exception) {
                // Si hay error, generar nuevas claves
            }
        }
        
        // Generar nuevo par de claves
        val keyPair = E2EEncryption.generateRSAKeyPair()
        
        // Guardar en SharedPreferences
        sharedPrefs.edit()
            .putString("private_key", E2EEncryption.privateKeyToString(keyPair.private))
            .putString("public_key", E2EEncryption.publicKeyToString(keyPair.public))
            .apply()
        
        // Subir clave pública a Supabase
        uploadPublicKey(userId, keyPair.public)
        
        return keyPair
    }
    
    /**
     * Sube la clave pública del usuario a Supabase
     */
    private suspend fun uploadPublicKey(userId: String, publicKey: PublicKey) {
        try {
            val publicKeyString = E2EEncryption.publicKeyToString(publicKey)
            
            android.util.Log.d("ChatViewModel", "Uploading public key for user: $userId")
            
            // Verificar si ya existe
            val existing = try {
                SupabaseConfig.client.from("user_public_keys")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserPublicKey>()
            } catch (e: Exception) {
                null
            }
            
            if (existing != null) {
                // Actualizar
                android.util.Log.d("ChatViewModel", "Updating existing public key")
                SupabaseConfig.client.from("user_public_keys")
                    .update({
                        set("public_key", publicKeyString)
                    }) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
            } else {
                // Insertar
                android.util.Log.d("ChatViewModel", "Inserting new public key")
                SupabaseConfig.client.from("user_public_keys")
                    .insert(buildJsonObject {
                        put("user_id", userId)
                        put("public_key", publicKeyString)
                    })
            }
            
            android.util.Log.d("ChatViewModel", "Public key uploaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error uploading public key", e)
        }
    }
    
    /**
     * Obtiene la clave pública de otro usuario desde Supabase
     */
    private suspend fun getPublicKey(userId: String): PublicKey? {
        return try {
            android.util.Log.d("ChatViewModel", "Fetching public key for user: $userId")
            
            val userKey = SupabaseConfig.client.from("user_public_keys")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingle<UserPublicKey>()
            
            android.util.Log.d("ChatViewModel", "Public key found in database: ${userKey.publicKey.take(50)}...")
            
            val publicKey = E2EEncryption.stringToPublicKey(userKey.publicKey)
            
            android.util.Log.d("ChatViewModel", "Public key decoded successfully")
            
            publicKey
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error fetching public key for user $userId", e)
            
            // Si el otro usuario no tiene clave pública, esperar un momento y reintentar
            kotlinx.coroutines.delay(1000)
            
            try {
                android.util.Log.d("ChatViewModel", "Retrying to fetch public key...")
                val userKey = SupabaseConfig.client.from("user_public_keys")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<UserPublicKey>()
                E2EEncryption.stringToPublicKey(userKey.publicKey)
            } catch (e2: Exception) {
                android.util.Log.e("ChatViewModel", "Retry also failed", e2)
                null
            }
        }
    }
    
    /**
     * Carga los últimos mensajes del chat (inicial - solo los más recientes)
     */
    private suspend fun loadMessages(otherUserId: String) {
        try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
            
            // Cargar solo los últimos messagesPerPage mensajes (orden descendente = más recientes primero)
            val messages = SupabaseConfig.client.from("chat_messages")
                .select {
                    filter {
                        or {
                            and {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", otherUserId)
                            }
                            and {
                                eq("sender_id", otherUserId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(messagesPerPage.toLong())
                }
                .decodeList<ChatMessage>()
            
            // Si se cargaron menos mensajes que el límite, no hay más antiguos
            hasMoreMessages = messages.size >= messagesPerPage
            
            // Los mensajes vienen del más reciente al más antiguo, hay que revertirlos
            // para mostrarlos cronológicamente (antiguos arriba, recientes abajo)
            val chronologicalMessages = messages.reversed()
            
            // Descifrar mensajes
            val decryptedMessages = chronologicalMessages.mapNotNull { encryptedMsg ->
                try {
                    decryptMessage(encryptedMsg, currentUserId)
                } catch (e: Exception) {
                    null // Si no se puede descifrar, omitir
                }
            }
            
            // Actualizar el timestamp del último mensaje
            if (decryptedMessages.isNotEmpty()) {
                lastMessageTimestamp = decryptedMessages.last().createdAt
            }
            
            // Mostrar mensajes directamente sin estado Loading
            _messagesState.value = MessagesState.Success(decryptedMessages)
        } catch (e: Exception) {
            _messagesState.value = MessagesState.Error(e.message ?: "Error al cargar mensajes")
        }
    }
    
    /**
     * Carga mensajes más antiguos (paginación hacia atrás)
     */
    fun loadOlderMessages(otherUserId: String) {
        if (isLoadingOlderMessages || !hasMoreMessages) return
        
        viewModelScope.launch {
            try {
                isLoadingOlderMessages = true
                val currentState = _messagesState.value
                if (currentState !is MessagesState.Success || currentState.messages.isEmpty()) {
                    isLoadingOlderMessages = false
                    return@launch
                }
                
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                // El mensaje más antiguo que tenemos actualmente es el primero de la lista
                val oldestTimestamp = currentState.messages.first().createdAt
                
                // Cargar mensajes anteriores al más antiguo actual
                val olderMessages = SupabaseConfig.client.from("chat_messages")
                    .select {
                        filter {
                            and {
                                or {
                                    and {
                                        eq("sender_id", currentUserId)
                                        eq("receiver_id", otherUserId)
                                    }
                                    and {
                                        eq("sender_id", otherUserId)
                                        eq("receiver_id", currentUserId)
                                    }
                                }
                                lt("created_at", oldestTimestamp)
                            }
                        }
                        order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(messagesPerPage.toLong())
                    }
                    .decodeList<ChatMessage>()
                
                // Si se cargaron menos mensajes que el límite, no hay más antiguos
                if (olderMessages.size < messagesPerPage) {
                    hasMoreMessages = false
                }
                
                if (olderMessages.isEmpty()) {
                    isLoadingOlderMessages = false
                    return@launch
                }
                
                // Revertir el orden (vienen del más reciente al más antiguo)
                val chronologicalOlderMessages = olderMessages.reversed()
                
                // Descifrar mensajes antiguos
                val decryptedOlderMessages = chronologicalOlderMessages.mapNotNull { encryptedMsg ->
                    try {
                        decryptMessage(encryptedMsg, currentUserId)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                // Combinar: mensajes antiguos primero, luego los que ya teníamos
                val updatedMessages = decryptedOlderMessages + currentState.messages
                _messagesState.value = MessagesState.Success(updatedMessages)
                
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading older messages", e)
            } finally {
                isLoadingOlderMessages = false
            }
        }
    }
    
    /**
     * Carga solo los mensajes nuevos desde el último timestamp conocido
     */
    private suspend fun loadNewMessages(otherUserId: String) {
        try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
            val lastTimestamp = lastMessageTimestamp ?: return // Si no hay timestamp, no buscar
            
            android.util.Log.d("ChatViewModel", "Checking for new messages after: $lastTimestamp")
            
            val newMessages = SupabaseConfig.client.from("chat_messages")
                .select {
                    filter {
                        and {
                            or {
                                and {
                                    eq("sender_id", currentUserId)
                                    eq("receiver_id", otherUserId)
                                }
                                and {
                                    eq("sender_id", otherUserId)
                                    eq("receiver_id", currentUserId)
                                }
                            }
                            gt("created_at", lastTimestamp) // Solo mensajes después del último
                        }
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<ChatMessage>()
            
            if (newMessages.isEmpty()) {
                android.util.Log.d("ChatViewModel", "No new messages found")
                return
            }
            
            android.util.Log.d("ChatViewModel", "Found ${newMessages.size} new messages")
            
            // Descifrar solo los mensajes nuevos
            val decryptedNewMessages = newMessages.mapNotNull { encryptedMsg ->
                try {
                    decryptMessage(encryptedMsg, currentUserId)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (decryptedNewMessages.isEmpty()) {
                return
            }
            
            // Actualizar el timestamp del último mensaje
            lastMessageTimestamp = decryptedNewMessages.last().createdAt
            
            // Añadir los nuevos mensajes a la lista existente, evitando duplicados
            val currentState = _messagesState.value
            if (currentState is MessagesState.Success) {
                // Obtener IDs existentes (excluyendo temporales)
                val existingIds = currentState.messages
                    .filter { !it.id.startsWith("temp_") }
                    .map { it.id }
                    .toSet()
                
                // Solo añadir mensajes que no existan ya
                val messagesNotDuplicated = decryptedNewMessages.filter { it.id !in existingIds }
                
                if (messagesNotDuplicated.isNotEmpty()) {
                    // Reemplazar mensajes temporales por reales si coincide el contenido
                    val messagesWithoutMatchingTemp = currentState.messages.filter { existing ->
                        if (!existing.id.startsWith("temp_")) {
                            true // Mantener mensajes reales
                        } else {
                            // Verificar si hay un mensaje nuevo con el mismo contenido
                            !messagesNotDuplicated.any { new -> 
                                new.message == existing.message && 
                                new.senderId == existing.senderId
                            }
                        }
                    }
                    
                    val updatedMessages = (messagesWithoutMatchingTemp + messagesNotDuplicated).sortedBy { it.createdAt }
                    _messagesState.value = MessagesState.Success(updatedMessages)
                    android.util.Log.d("ChatViewModel", "Added ${messagesNotDuplicated.size} new messages to chat")
                } else {
                    android.util.Log.d("ChatViewModel", "All new messages were duplicates, skipping")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error loading new messages", e)
        }
    }
    
    /**
     * Actualiza solo los estados de los mensajes existentes sin recargar todo
     * Útil para detectar cuando mensajes pasan de SENT a READ
     */
    private suspend fun updateMessageStatuses(otherUserId: String) {
        try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
            val currentState = _messagesState.value
            
            if (currentState !is MessagesState.Success || currentState.messages.isEmpty()) {
                return
            }
            
            // Solo obtener IDs que nos interesan (nuestros mensajes enviados)
            val myMessageIds = currentState.messages
                .filter { it.isMine }
                .map { it.id }
            
            if (myMessageIds.isEmpty()) {
                return
            }
            
            android.util.Log.d("ChatViewModel", "Checking status updates for ${myMessageIds.size} messages")
            
            // Consultar solo el status de nuestros mensajes
            // Como Supabase no permite select de campos específicos fácilmente,
            // vamos a cargar solo los mensajes que enviamos
            val messages = SupabaseConfig.client.from("chat_messages")
                .select {
                    filter {
                        and {
                            eq("sender_id", currentUserId)
                            eq("receiver_id", otherUserId)
                        }
                    }
                }
                .decodeList<ChatMessage>()
            
            // Crear mapa de ID -> status
            val statusMap = messages.associate { 
                val status = it.status ?: (if (it.read) "read" else "sent")
                it.id to status
            }
            
            // Actualizar estados en los mensajes actuales
            var hasChanges = false
            val updatedMessages = currentState.messages.map { msg ->
                if (!msg.isMine) {
                    // No actualizar mensajes que recibimos
                    msg
                } else {
                    val newStatusStr = statusMap[msg.id]
                    if (newStatusStr != null) {
                        val newMessageStatus = when (newStatusStr.lowercase()) {
                            "sending" -> MessageStatus.SENDING
                            "sent" -> MessageStatus.SENT
                            "read" -> MessageStatus.READ
                            else -> msg.status
                        }
                        
                        if (newMessageStatus != msg.status) {
                            hasChanges = true
                            android.util.Log.d("ChatViewModel", "✓ Message ${msg.id.take(8)} status: ${msg.status} -> $newMessageStatus")
                            msg.copy(status = newMessageStatus)
                        } else {
                            msg
                        }
                    } else {
                        msg
                    }
                }
            }
            
            // Solo actualizar UI si hubo cambios
            if (hasChanges) {
                _messagesState.value = MessagesState.Success(updatedMessages)
                android.util.Log.d("ChatViewModel", "✓✓ Message statuses updated in UI")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error updating message statuses", e)
        }
    }
    
    /**
     * Descifra un mensaje
     */
    private fun decryptMessage(encryptedMsg: ChatMessage, currentUserId: String): DecryptedChatMessage? {
        val privateKey = myPrivateKey ?: return null
        
        // Determinar qué versión cifrada usar según si soy sender o receiver
        val isSender = encryptedMsg.senderId == currentUserId
        
        val encryptedMessage = if (isSender) {
            // Soy el remitente, usar la versión cifrada para mí
            EncryptedMessage(
                encryptedContent = encryptedMsg.encryptedContentSender,
                encryptedKey = encryptedMsg.encryptedKeySender,
                iv = encryptedMsg.ivSender
            )
        } else {
            // Soy el destinatario, usar la versión cifrada para mí
            EncryptedMessage(
                encryptedContent = encryptedMsg.encryptedContentReceiver,
                encryptedKey = encryptedMsg.encryptedKeyReceiver,
                iv = encryptedMsg.ivReceiver
            )
        }
        
        val decryptedText = try {
            E2EEncryption.decryptMessage(encryptedMessage, privateKey)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error decrypting message", e)
            return null
        }
        
        // Convertir estado de string a enum
        val statusString = encryptedMsg.status
        android.util.Log.d("ChatViewModel", "Message ${encryptedMsg.id} - Status: '$statusString', Read: ${encryptedMsg.read}, IsMine: $isSender")
        
        val messageStatus = if (statusString != null) {
            // Si existe el campo status, usarlo
            when (statusString.lowercase()) {
                "sending" -> MessageStatus.SENDING
                "sent" -> MessageStatus.SENT
                "read" -> MessageStatus.READ
                else -> {
                    android.util.Log.w("ChatViewModel", "Unknown status: '$statusString', using read field")
                    if (encryptedMsg.read) MessageStatus.READ else MessageStatus.SENT
                }
            }
        } else {
            // Si no existe status, usar el campo read como fallback
            android.util.Log.d("ChatViewModel", "Status field is null, using read field: ${encryptedMsg.read}")
            if (encryptedMsg.read) MessageStatus.READ else MessageStatus.SENT
        }
        
        // Convertir tipo de mensaje
        val messageType = when (encryptedMsg.messageType.lowercase()) {
            "image" -> MessageType.IMAGE
            "movie" -> MessageType.MOVIE
            else -> MessageType.TEXT
        }
        
        return DecryptedChatMessage(
            id = encryptedMsg.id,
            senderId = encryptedMsg.senderId,
            receiverId = encryptedMsg.receiverId,
            message = decryptedText,
            read = encryptedMsg.read,
            status = messageStatus,
            messageType = messageType,
            imageUrl = encryptedMsg.imageUrl,
            movieTmdbId = encryptedMsg.movieTmdbId,
            moviePosterUrl = encryptedMsg.moviePosterUrl,
            createdAt = encryptedMsg.createdAt,
            isMine = isSender
        )
    }
    
    /**
     * Envía un mensaje cifrado
     */
    fun sendMessage(message: String, otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    android.util.Log.e("ChatViewModel", "No user logged in")
                    return@launch
                }
                
                val otherPublicKey = otherUserPublicKey
                if (otherPublicKey == null) {
                    android.util.Log.e("ChatViewModel", "No public key for other user")
                    _messagesState.value = MessagesState.Error("No se pudo obtener la clave pública del destinatario")
                    return@launch
                }
                
                val myPublicKey = myKeyPair?.public
                if (myPublicKey == null) {
                    android.util.Log.e("ChatViewModel", "No public key for current user")
                    return@launch
                }
                
                // Crear mensaje temporal ANTES de enviar
                val tempId = "temp_${System.currentTimeMillis()}"
                val tempMessage = DecryptedChatMessage(
                    id = tempId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    message = message,
                    read = false,
                    status = MessageStatus.SENDING,
                    messageType = MessageType.TEXT,
                    createdAt = java.time.Instant.now().toString(),
                    isMine = true
                )
                
                // Añadir mensaje temporal a la UI
                val currentState = _messagesState.value
                if (currentState is MessagesState.Success) {
                    _messagesState.value = MessagesState.Success(currentState.messages + tempMessage)
                }
                
                android.util.Log.d("ChatViewModel", "Encrypting message: $message")
                
                // Cifrar mensaje con mi clave pública (para que yo pueda leerlo)
                val encryptedForMe = E2EEncryption.encryptMessage(message, myPublicKey)
                
                // Cifrar mensaje con la clave pública del destinatario
                val encryptedForOther = E2EEncryption.encryptMessage(message, otherPublicKey)
                
                android.util.Log.d("ChatViewModel", "Sending to Supabase...")
                
                // Enviar a Supabase con ambas versiones cifradas y estado 'sent'
                SupabaseConfig.client.from("chat_messages")
                    .insert(buildJsonObject {
                        put("sender_id", currentUserId)
                        put("receiver_id", otherUserId)
                        put("encrypted_content_sender", encryptedForMe.encryptedContent)
                        put("encrypted_key_sender", encryptedForMe.encryptedKey)
                        put("iv_sender", encryptedForMe.iv)
                        put("encrypted_content_receiver", encryptedForOther.encryptedContent)
                        put("encrypted_key_receiver", encryptedForOther.encryptedKey)
                        put("iv_receiver", encryptedForOther.iv)
                        put("status", "sent") // Estado enviado al servidor
                    })
                
                android.util.Log.d("ChatViewModel", "Message sent to server successfully")
                
                // Pequeña espera para que la BD procese
                kotlinx.coroutines.delay(300)
                
                // Cargar el mensaje real desde la BD
                // loadNewMessages reemplazará el temporal automáticamente
                loadNewMessages(otherUserId)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error sending message", e)
                
                // Remover mensaje temporal si hubo error
                val stateOnError = _messagesState.value
                if (stateOnError is MessagesState.Success) {
                    val messagesWithoutTemp = stateOnError.messages.filter { !it.id.startsWith("temp_") }
                    _messagesState.value = MessagesState.Success(messagesWithoutTemp)
                }
                
                _messagesState.value = MessagesState.Error(e.message ?: "Error al enviar mensaje")
            }
        }
    }
    
    /**
     * Suscribirse a nuevos mensajes en tiempo real
     */
    private fun subscribeToMessages(otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                android.util.Log.d("ChatViewModel", "=== Setting up Realtime subscription ===")
                android.util.Log.d("ChatViewModel", "Current user: $currentUserId")
                android.util.Log.d("ChatViewModel", "Other user: $otherUserId")
                
                // Crear canal único para este chat
                val channelId = "chat_messages"
                realtimeChannel = SupabaseConfig.client.channel(channelId)
                
                android.util.Log.d("ChatViewModel", "Channel created: $channelId")
                
                // Suscribirse a cambios en la tabla
                realtimeChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chat_messages"
                }?.onEach { action ->
                    android.util.Log.d("ChatViewModel", "=== Realtime event received ===")
                    android.util.Log.d("ChatViewModel", "Action type: ${action.javaClass.simpleName}")
                    
                    when (action) {
                        is PostgresAction.Insert -> {
                            android.util.Log.d("ChatViewModel", "Insert action received")
                            try {
                                // Extraer datos del registro
                                val record = action.record
                                android.util.Log.d("ChatViewModel", "Record keys: ${record.keys}")
                                
                                val senderId = record["sender_id"]?.toString()
                                val receiverId = record["receiver_id"]?.toString()
                                
                                android.util.Log.d("ChatViewModel", "Message from: $senderId to: $receiverId")
                                
                                // Verificar si el mensaje es parte de este chat
                                val isMyMessage = senderId == currentUserId && receiverId == otherUserId
                                val isOtherMessage = senderId == otherUserId && receiverId == currentUserId
                                
                                if (isMyMessage || isOtherMessage) {
                                    android.util.Log.d("ChatViewModel", "Message is relevant to this chat, loading new messages...")
                                    // Pequeño delay para asegurar que el mensaje esté en la BD
                                    kotlinx.coroutines.delay(200)
                                    loadNewMessages(otherUserId)
                                } else {
                                    android.util.Log.d("ChatViewModel", "Message not relevant to this chat, ignoring")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatViewModel", "Error processing realtime insert", e)
                            }
                        }
                        is PostgresAction.Update -> {
                            android.util.Log.d("ChatViewModel", "Update action received, loading new messages")
                            loadNewMessages(otherUserId)
                        }
                        is PostgresAction.Delete -> {
                            android.util.Log.d("ChatViewModel", "Delete action received, reloading all")
                            loadMessages(otherUserId) // Para deletes sí recargamos todo
                        }
                        else -> {
                            android.util.Log.d("ChatViewModel", "Other action type: $action")
                        }
                    }
                }?.launchIn(viewModelScope)
                
                // Suscribirse al canal
                realtimeChannel?.subscribe()
                android.util.Log.d("ChatViewModel", "=== Successfully subscribed to Realtime channel ===")
                
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "=== Error in Realtime subscription ===", e)
            }
        }
    }
    
    /**
     * Inicia polling para actualizar mensajes cada 3 segundos
     * Esto funciona como respaldo si Realtime no está configurado
     */
    private fun startPollingMessages(otherUserId: String) {
        // Cancelar polling anterior si existe
        pollingJob?.cancel()
        
        pollingJob = viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "Starting message polling every 3 seconds")
            var pollCount = 0
            while (true) {
                try {
                    kotlinx.coroutines.delay(3000) // Esperar 3 segundos
                    pollCount++
                    
                    // Alternar entre buscar nuevos mensajes y actualizar estados
                    if (pollCount % 2 == 0) {
                        // Cada 6 segundos, actualizar estados (SENT -> READ)
                        android.util.Log.d("ChatViewModel", "Polling: Updating message statuses")
                        updateMessageStatuses(otherUserId)
                    } else {
                        // Cada 3 segundos, buscar mensajes nuevos
                        android.util.Log.d("ChatViewModel", "Polling: Checking for new messages...")
                        loadNewMessages(otherUserId)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.d("ChatViewModel", "Polling cancelled")
                    break
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error in polling", e)
                }
            }
        }
    }
    
    /**
     * Detiene el polling de mensajes
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        android.util.Log.d("ChatViewModel", "Message polling stopped")
    }
    
    /**
     * Marca los mensajes del otro usuario como leídos
     */
    private suspend fun markMessagesAsRead(otherUserId: String) {
        try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
            
            android.util.Log.d("ChatViewModel", "Marking messages as read from user: $otherUserId")
            
            // Actualizar mensajes del otro usuario a estado 'read'
            val updateResult = SupabaseConfig.client.from("chat_messages")
                .update({
                    set("status", "read")
                    set("read", true)
                }) {
                    filter {
                        and {
                            eq("sender_id", otherUserId)
                            eq("receiver_id", currentUserId)
                        }
                    }
                }
            
            android.util.Log.d("ChatViewModel", "Messages marked as read successfully")
            
            // Actualizar estados localmente en lugar de recargar todo
            val currentState = _messagesState.value
            if (currentState is MessagesState.Success) {
                val updatedMessages = currentState.messages.map { msg ->
                    if (msg.senderId == otherUserId && !msg.isMine) {
                        // Marcar mensajes del otro usuario como leídos
                        msg.copy(read = true)
                    } else {
                        msg
                    }
                }
                _messagesState.value = MessagesState.Success(updatedMessages)
                android.util.Log.d("ChatViewModel", "Local messages updated to read status")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error marking messages as read", e)
        }
    }
    
    /**
     * Enviar imagen al chat
     */
    fun sendImage(context: Context, imageUri: android.net.Uri, otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    android.util.Log.e("ChatViewModel", "No user logged in")
                    return@launch
                }
                
                // Leer bytes de la imagen
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (imageBytes == null) {
                    _messagesState.value = MessagesState.Error("No se pudo leer la imagen")
                    return@launch
                }
                
                // Subir imagen a Supabase Storage
                val timestamp = System.currentTimeMillis()
                val fileName = "chat_images/${currentUserId}_${timestamp}.jpg"
                
                android.util.Log.d("ChatViewModel", "Uploading image to: $fileName")
                
                SupabaseConfig.client.storage.from("chat-images").upload(fileName, imageBytes)
                
                // Obtener URL pública
                val imageUrl = SupabaseConfig.client.storage.from("chat-images").publicUrl(fileName)
                
                android.util.Log.d("ChatViewModel", "Image uploaded successfully: $imageUrl")
                
                // Enviar mensaje con la URL de la imagen
                sendMessageWithType(
                    message = "📷 Imagen",
                    messageType = "image",
                    imageUrl = imageUrl,
                    otherUserId = otherUserId
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error sending image", e)
                _messagesState.value = MessagesState.Error("Error al enviar imagen: ${e.message}")
            }
        }
    }
    
    /**
     * Enviar película al chat
     */
    fun sendMovie(movieTmdbId: Int, movieTitle: String, moviePosterPath: String?, otherUserId: String) {
        viewModelScope.launch {
            try {
                val posterUrl = moviePosterPath?.let { "https://image.tmdb.org/t/p/w185$it" }
                sendMessageWithType(
                    message = "🎬 $movieTitle",
                    messageType = "movie",
                    movieTmdbId = movieTmdbId,
                    moviePosterUrl = posterUrl,
                    otherUserId = otherUserId
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error sending movie", e)
                _messagesState.value = MessagesState.Error("Error al enviar película: ${e.message}")
            }
        }
    }
    
    /**
     * Enviar mensaje con tipo específico (texto, imagen, película)
     */
    private suspend fun sendMessageWithType(
        message: String,
        messageType: String = "text",
        imageUrl: String? = null,
        movieTmdbId: Int? = null,
        moviePosterUrl: String? = null,
        otherUserId: String
    ) {
        val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            android.util.Log.e("ChatViewModel", "No user logged in")
            return
        }
        
        val otherPublicKey = otherUserPublicKey
        if (otherPublicKey == null) {
            android.util.Log.e("ChatViewModel", "No public key for other user")
            _messagesState.value = MessagesState.Error("No se pudo obtener la clave pública del destinatario")
            return
        }
        
        val myPublicKey = myKeyPair?.public
        if (myPublicKey == null) {
            android.util.Log.e("ChatViewModel", "No public key for current user")
            return
        }
        
        // Crear mensaje temporal
        val tempId = "temp_${System.currentTimeMillis()}"
        val msgType = when (messageType) {
            "image" -> MessageType.IMAGE
            "movie" -> MessageType.MOVIE
            else -> MessageType.TEXT
        }
        
        val tempMessage = DecryptedChatMessage(
            id = tempId,
            senderId = currentUserId,
            receiverId = otherUserId,
            message = message,
            read = false,
            status = MessageStatus.SENDING,
            messageType = msgType,
            imageUrl = imageUrl,
            movieTmdbId = movieTmdbId,
            moviePosterUrl = moviePosterUrl,
            createdAt = java.time.Instant.now().toString(),
            isMine = true
        )
        
        // Añadir mensaje temporal a la UI
        val currentState = _messagesState.value
        if (currentState is MessagesState.Success) {
            _messagesState.value = MessagesState.Success(currentState.messages + tempMessage)
        }
        
        // Cifrar mensaje
        val encryptedForMe = E2EEncryption.encryptMessage(message, myPublicKey)
        val encryptedForOther = E2EEncryption.encryptMessage(message, otherPublicKey)
        
        // Enviar a Supabase
        val jsonBuilder = buildJsonObject {
            put("sender_id", currentUserId)
            put("receiver_id", otherUserId)
            put("encrypted_content_sender", encryptedForMe.encryptedContent)
            put("encrypted_key_sender", encryptedForMe.encryptedKey)
            put("iv_sender", encryptedForMe.iv)
            put("encrypted_content_receiver", encryptedForOther.encryptedContent)
            put("encrypted_key_receiver", encryptedForOther.encryptedKey)
            put("iv_receiver", encryptedForOther.iv)
            put("status", "sent")
            put("message_type", messageType)
            
            if (imageUrl != null) {
                put("image_url", imageUrl)
            }
            if (movieTmdbId != null) {
                put("movie_tmdb_id", movieTmdbId)
            }
            if (moviePosterUrl != null) {
                put("movie_poster_url", moviePosterUrl)
            }
        }
        
        SupabaseConfig.client.from("chat_messages").insert(jsonBuilder)
        
        android.util.Log.d("ChatViewModel", "Message sent to server successfully (type: $messageType)")
        
        // Esperar y recargar mensajes
        kotlinx.coroutines.delay(300)
        loadNewMessages(otherUserId)
    }
    
    /**
     * Limpiar recursos al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        
        // Cancelar el polling primero (síncrono)
        pollingJob?.cancel()
        pollingJob = null
        
        // Desuscribirse del canal de Realtime
        // Usar runBlocking solo si es absolutamente necesario, o hacerlo asíncrono
        try {
            // Lanzar en un scope que no esté cancelado
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    realtimeChannel?.unsubscribe()
                    android.util.Log.d("ChatViewModel", "Realtime channel unsubscribed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error unsubscribing from realtime", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error in onCleared", e)
        }
        
        android.util.Log.d("ChatViewModel", "ChatViewModel resources cleaned up")
    }
}
