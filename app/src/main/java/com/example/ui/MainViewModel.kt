package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        
        // Populate the DB on start with custom items
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
        }
    }

    // --- Bottom Bar Navigation Tab ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }

    private val _currentTab = MutableStateFlow("buscar")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // --- Professional Candidacy States (Estado A, B, C) ---
    private val _candidacyState = MutableStateFlow("A") // "A" = Default, "B" = Candidatura em análise, "C" = Aprovado
    val candidacyState: StateFlow<String> = _candidacyState.asStateFlow()

    fun setCandidacyState(state: String) {
        _candidacyState.value = state
    }

    // --- Active Mode (Client vs Professional) ---
    private val _activeProfileMode = MutableStateFlow("Cliente") // "Cliente" or "Profissional"
    val activeProfileMode: StateFlow<String> = _activeProfileMode.asStateFlow()

    fun setActiveProfileMode(mode: String) {
        _activeProfileMode.value = mode
        if (mode == "Profissional") {
            _currentTab.value = "dashboard"
        } else {
            _currentTab.value = "buscar"
        }
    }

    // --- Professional Flow States ---
    private val _isProfessionalOnline = MutableStateFlow(true)
    val isProfessionalOnline: StateFlow<Boolean> = _isProfessionalOnline.asStateFlow()

    fun toggleProfessionalOnline() {
        _isProfessionalOnline.value = !_isProfessionalOnline.value
    }

    private val _selectedCalendarDay = MutableStateFlow(19) // default Jun 19
    val selectedCalendarDay: StateFlow<Int> = _selectedCalendarDay.asStateFlow()

    fun selectCalendarDay(day: Int) {
        _selectedCalendarDay.value = day
    }

    // Daily Agenda states per day (e.g. key: "19_08:00")
    private val _agendaSlots = MutableStateFlow<Map<String, String>>(
        mapOf(
            "19_08:00" to "Livre",
            "19_09:00" to "Livre",
            "19_10:00" to "Livre",
            "19_11:00" to "Reservado",
            "19_12:00" to "Bloqueado",
            "19_13:00" to "Livre",
            "19_14:00" to "Livre",
            "20_08:00" to "Livre",
            "20_09:00" to "Livre",
            "20_10:00" to "Reservado",
            "20_11:00" to "Livre",
            "20_12:00" to "Livre",
            "20_13:00" to "Livre",
            "20_14:00" to "Livre"
        )
    )
    val agendaSlots: StateFlow<Map<String, String>> = _agendaSlots.asStateFlow()

    fun toggleAgendaSlot(day: Int, slot: String) {
        val key = "${day}_$slot"
        val currentMap = _agendaSlots.value.toMutableMap()
        val currentVal = currentMap[key] ?: "Livre"
        if (currentVal == "Reservado") return // Reservado slots cannot be edited
        currentMap[key] = if (currentVal == "Livre") "Bloqueado" else "Livre"
        _agendaSlots.value = currentMap
    }

    // --- Global states ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissError() {
        _errorMessage.value = null
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    // --- Search / Filters ---
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    private val _isLoadingProfessionals = MutableStateFlow(false)
    val isLoadingProfessionals: StateFlow<Boolean> = _isLoadingProfessionals.asStateFlow()

    val professionals: StateFlow<List<Professional>> = _selectedCategory
        .flatMapLatest { category ->
            // Simulate network loading for skeleton
            _isLoadingProfessionals.value = true
            delay(1200) 
            
            val flow = if (category == null) {
                repository.professionals
            } else {
                repository.getProfessionalsByCategory(category)
            }
            
            flow.onEach { _isLoadingProfessionals.value = false }
        }
        .catch { e ->
             _errorMessage.value = "Falha ao carregar profissionais. Verifique a sua internet."
             _isLoadingProfessionals.value = false
             emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Professional (for Detail view) ---
    private val _selectedProfessionalId = MutableStateFlow<Long?>(null)
    val selectedProfessionalId: StateFlow<Long?> = _selectedProfessionalId.asStateFlow()

    val selectedProfessional = _selectedProfessionalId
        .map { id -> if (id != null) repository.getProfessionalById(id) else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserProfessionalProfile: StateFlow<Professional?> = repository.professionals
        .map { list -> list.find { it.name == "Valgys Algy" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeReviews = _selectedProfessionalId
        .flatMapLatest { id ->
            if (id != null) repository.getReviewsForProfessional(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectProfessional(id: Long?) {
        _selectedProfessionalId.value = id
    }

    // --- Bookings (Agendamentos) ---
    val bookings: StateFlow<List<Booking>> = repository.bookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Chats ---
    val activeChatMessages = _selectedProfessionalId
        .flatMapLatest { id ->
            if (id != null) {
                // Mark messages as read when opening
                viewModelScope.launch { repository.markChatAsRead(id) }
                repository.getChatMessages(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Notifications ---
    val notifications: StateFlow<List<PushNotification>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Push notification banner simulation in-app
    private val _activeInAppNotification = MutableStateFlow<PushNotification?>(null)
    val activeInAppNotification: StateFlow<PushNotification?> = _activeInAppNotification.asStateFlow()

    fun dismissInAppNotification() {
        _activeInAppNotification.value = null
    }

    private fun triggerPushNotification(bookingId: Long?, title: String, body: String) {
        viewModelScope.launch {
            val notification = PushNotification(
                bookingId = bookingId,
                title = title,
                body = body,
                isRead = false
            )
            val id = repository.addNotification(notification)
            val insertedNotif = notification.copy(id = id)
            // Trigger overlay popup in-app
            _activeInAppNotification.value = insertedNotif
        }
    }

    // --- ACTIONS ---

    // Create a Booking (Agendamento)
    fun createBooking(
        profId: Long,
        date: String,
        time: String,
        hours: Int,
        address: String,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1200) // Simulate network delay
            
            // Validate basic network simulation error
            if (date.isBlank() || time.isBlank()) {
                _errorMessage.value = "Por favor preencha todos os campos corretamente."
                _isLoading.value = false
                return@launch
            }
            
            val prof = repository.getProfessionalById(profId)
            if (prof == null) {
                _errorMessage.value = "Profissional não encontrado."
                _isLoading.value = false
                return@launch
            }

            val total = prof.ratePerHour * hours
            val booking = Booking(
                professionalId = prof.id,
                professionalName = prof.name,
                professionalCategory = prof.category,
                date = date,
                time = time,
                hours = hours,
                totalAmount = total,
                address = address,
                notes = notes,
                status = "Pendente",
                paymentStatus = "Pendente"
            )
            val bookingId = repository.addBooking(booking)
            
            triggerPushNotification(
                bookingId = bookingId,
                title = "Agendamento Criado! \ud83d\udcc5",
                body = "Seu pedido de serviço com ${prof.name} foi recebido. Efetue o pagamento para confirmar."
            )
            
            _isLoading.value = false
            onSuccess(bookingId)
        }
    }

    // Secure Payment Simulation
    fun processPayment(bookingId: Long, cardLast4: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val booking = repository.getBookingById(bookingId)
            if (booking == null) {
                _errorMessage.value = "Agendamento não encontrado."
                _isLoading.value = false
                return@launch
            }
            // Transition to processing
            repository.updateBooking(booking.copy(paymentStatus = "Processando"))
            
            delay(2500) // Simulate processing time
            
            // Simulate 10% chance of random bank error
            if (Math.random() < 0.1) {
                _errorMessage.value = "Falha na transação. Banco emissor recusou o pagamento."
                repository.updateBooking(booking.copy(paymentStatus = "Falhou"))
                _isLoading.value = false
                return@launch
            }
            
            val updated = booking.copy(
                paymentStatus = "Pago",
                status = "Confirmado",
                cardLast4 = cardLast4
            )
            repository.updateBooking(updated)
            
            triggerPushNotification(
                bookingId = bookingId,
                title = "Pagamento Confirmado! \ud83d\udcb3\ud83e\udd1d",
                body = "O pagamento de ${String.format("%.2f", booking.totalAmount)} MT para ${booking.professionalName} foi processado de forma segura. Serviço confirmado!"
            )
            
            // Auto complete booking after some simulation (just for illustrative progress)
            viewModelScope.launch {
                delay(12000) // After 12s, simulate professional arriving/starting
                val current = repository.getBookingById(bookingId)
                if (current != null && current.status == "Confirmado") {
                    repository.updateBooking(current.copy(status = "Em Andamento"))
                    triggerPushNotification(
                        bookingId = bookingId,
                        title = "Profissional a Caminho! \ud83d\ude97",
                        body = "${current.professionalName} iniciou o deslocamento até o seu endereço."
                    )
                }
            }

            _isLoading.value = false
            onComplete()
        }
    }

    // Cancel Booking
    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId) ?: return@launch
            repository.updateBooking(booking.copy(status = "Cancelado"))
            triggerPushNotification(
                bookingId = bookingId,
                title = "Agendamento Cancelado ❌",
                body = "O agendamento com ${booking.professionalName} foi cancelado com sucesso."
            )
        }
    }

    // Confirm Booking (simulates professional confirming the booking)
    fun confirmBooking(bookingId: Long) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId) ?: return@launch
            val updated = booking.copy(status = "Confirmado")
            repository.updateBooking(updated)
            triggerPushNotification(
                bookingId = bookingId,
                title = "Agendamento Confirmado! 👍⚙️",
                body = "O ${booking.professionalName} confirmou o seu agendamento."
            )
        }
    }

    // Complete Booking (manual or simulated)
    fun completeBooking(bookingId: Long) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId) ?: return@launch
            val updated = booking.copy(status = "Concluído")
            repository.updateBooking(updated)
            triggerPushNotification(
                bookingId = bookingId,
                title = "Serviço Concluído! 🤝⚙️",
                body = "O serviço de ${booking.professionalCategory} realizado por ${booking.professionalName} foi concluído com sucesso. Avalie a sua experiência!"
            )
        }
    }

    // Rate performed service
    fun rateBooking(bookingId: Long, rating: Int, comment: String) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId) ?: return@launch
            val updated = booking.copy(isRated = true)
            repository.updateBooking(updated)
            
            // Add a verified review
            addReview(
                profId = booking.professionalId,
                author = "Cliente",
                rating = rating,
                comment = comment
            )
        }
    }

    // Create Chat Message & simulated smart reply
    fun sendChatMessage(profId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // Insert user message
            repository.addChatMessage(
                ChatMessage(
                    professionalId = profId,
                    sender = "CLIENT",
                    messageText = text
                )
            )

            // Dynamic agent reply after delay
            val prof = repository.getProfessionalById(profId) ?: return@launch
            delay(1500)

            val replyText = getSimulatedReply(prof.name, prof.category, text)
            repository.addChatMessage(
                ChatMessage(
                    professionalId = profId,
                    sender = "PROFESSIONAL",
                    messageText = replyText
                )
            )

            // Trigger simulated push notification for incoming chat message!
            triggerPushNotification(
                bookingId = null,
                title = "Mensagem de ${prof.name} 💬",
                body = replyText
            )
        }
    }

    // Add Review
    fun addReview(profId: Long, author: String, rating: Int, comment: String) {
        if (author.isBlank() || comment.isBlank()) return
        viewModelScope.launch {
            val review = Review(
                professionalId = profId,
                authorName = author,
                rating = rating,
                comment = comment,
                dateStr = "Hoje",
                isVerifiedPurchase = true
            )
            repository.addReview(review)

            // Also recalculate professional stats slightly
            val prof = repository.getProfessionalById(profId) ?: return@launch
            val newCount = prof.reviewCount + 1
            val newRating = ((prof.rating * prof.reviewCount) + rating) / newCount
            val roundedRating = Math.round(newRating * 10) / 10.0
            
            val updatedList = listOf(
                prof.copy(rating = roundedRating, reviewCount = newCount)
            )
            repository.db.professionalDao().insertProfessionals(updatedList)

            triggerPushNotification(
                bookingId = null,
                title = "Avaliação Publicada! ⭐",
                body = "Sua avaliação para ${prof.name} foi publicada como compra verificada. Obrigado pela ajuda!"
            )
        }
    }

    // Read general notifications
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markNotificationsAsRead()
        }
    }

    fun saveProfessionalProfile(
        category: String,
        description: String,
        ratePerHour: Double,
        location: String,
        specialization: String,
        yearsOfExperience: Int,
        certificates: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existing = currentUserProfessionalProfile.value
                val newProfile = Professional(
                    id = existing?.id ?: 0,
                    name = "Valgys Algy",
                    category = category,
                    rating = existing?.rating ?: 5.0,
                    reviewCount = existing?.reviewCount ?: 0,
                    ratePerHour = ratePerHour,
                    description = description,
                    location = location,
                    isVerified = true,
                    availabilityStatus = existing?.availabilityStatus ?: "Disponível hoje",
                    specialization = specialization,
                    yearsOfExperience = yearsOfExperience,
                    certificates = certificates
                )
                repository.addProfessional(newProfile)
                triggerPushNotification(
                    bookingId = null,
                    title = "Perfil Profissional Atualizado! ✅",
                    body = "Seu perfil de $category foi salvo com sucesso e já está visível na busca principal."
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao salvar perfil profissional."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProfessionalProfile() {
        viewModelScope.launch {
            val existing = currentUserProfessionalProfile.value ?: return@launch
            _isLoading.value = true
            try {
                repository.deleteProfessional(existing)
                triggerPushNotification(
                    bookingId = null,
                    title = "Perfil Desativado ❌",
                    body = "Seu perfil de profissional foi removido com sucesso."
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao desativar perfil profissional."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper functions for replies
    private fun getSimulatedReply(name: String, category: String, userText: String): String {
        val text = userText.lowercase()
        return when {
            text.contains("olá") || text.contains("oi") || text.contains("bom dia") || text.contains("boa tarde") -> {
                "Olá! Sou o $name ($category). Tudo bem? Como posso te ajudar hoje com o meu serviço?"
            }
            text.contains("preco") || text.contains("preço") || text.contains("valor") || text.contains("orçamento") || text.contains("custa") -> {
                "Meu valor médio é de ${String.format("%.2f", getRateForCategory(category))} MT por hora. Se você iniciar o agendamento no aplicativo, calculamos o total certinho de forma transparente e segura!"
            }
            text.contains("garantia") || text.contains("seguro") || text.contains("confiável") -> {
                "Com certeza, todos os meus agendamentos aqui contam com a garantia da plataforma e avaliações 100% verificadas para sua segurança."
            }
            text.contains("hoje") || text.contains("disponivel") || text.contains("disponível") || text.contains("horário") || text.contains("horario") -> {
                "Eu tenho disponibilidade sim! Você pode selecionar a data e hora desejada no botão 'Agendar Serviço' e faremos a reserva!"
            }
            text.contains("obrigado") || text.contains("obrigada") || text.contains("valeu") -> {
                "De nada! Fico à disposição. Pode contar comigo!"
            }
            else -> {
                "Ótimo! Entendi perfeitamente sobre a sua solicitação de $category. Se quiser pode realizar o agendamento no botão superior e irei te atender com toda a dedicação!"
            }
        }
    }

    private fun getRateForCategory(category: String): Double {
        return when(category) {
            "Eletricista" -> 650.0
            "Diarista / Limpeza" -> 350.0
            "Encanador" -> 600.0
            "Pintor" -> 450.0
            "Chaveiro / Serralheiro" -> 700.0
            "Jardineiro" -> 400.0
            "Técnico de TI" -> 750.0
            else -> 500.0
        }
    }
}
