package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. ROOM ENTITIES
// ==========================================

@Entity(tableName = "professionals")
data class Professional(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // e.g., "Eletricista", "Pintor"
    val rating: Double,
    val reviewCount: Int,
    val ratePerHour: Double,
    val description: String,
    val location: String, // Neighborhood & distance
    val isVerified: Boolean = true,
    val availabilityStatus: String = "Disponível hoje",
    val specialization: String = "",
    val yearsOfExperience: Int = 3,
    val portfolioImages: String = "", // Pipe-separated values
    val portfolioLinks: String = "",  // Pipe-separated values
    val certificates: String = ""     // Pipe-separated values
) {
    val portfolioImagesList: List<String>
        get() = if (portfolioImages.isEmpty()) emptyList() else portfolioImages.split("|")

    val portfolioLinksList: List<String>
        get() = if (portfolioLinks.isEmpty()) emptyList() else portfolioLinks.split("|")

    val certificatesList: List<String>
        get() = if (certificates.isEmpty()) emptyList() else certificates.split("|")
}

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val professionalId: Long,
    val professionalName: String,
    val professionalCategory: String,
    val date: String,      // YYYY-MM-DD
    val time: String,      // HH:MM
    val hours: Int,
    val totalAmount: Double,
    val address: String,
    val notes: String,
    val status: String,    // "Pendente", "Confirmado", "Em Andamento", "Concluído", "Cancelado"
    val paymentStatus: String, // "Pendente", "Processando", "Pago"
    val cardLast4: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRated: Boolean = false,
    val customerName: String = "",
    val customerPhone: String = ""
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val professionalId: Long,
    val authorName: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val dateStr: String,
    val isVerifiedPurchase: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val professionalId: Long,
    val sender: String, // "CLIENT" or "PROFESSIONAL"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "notifications")
data class PushNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long?,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// ==========================================
// 2. ROOM DAOS
// ==========================================

@Dao
interface ProfessionalDao {
    @Query("SELECT * FROM professionals ORDER BY rating DESC")
    fun getAllProfessionals(): Flow<List<Professional>>

    @Query("SELECT * FROM professionals WHERE category = :category ORDER BY rating DESC")
    fun getProfessionalsByCategory(category: String): Flow<List<Professional>>

    @Query("SELECT * FROM professionals WHERE id = :id")
    suspend fun getProfessionalById(id: Long): Professional?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfessionals(professionals: List<Professional>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfessional(professional: Professional): Long

    @Delete
    suspend fun deleteProfessional(professional: Professional)

    @Query("SELECT COUNT(*) FROM professionals")
    suspend fun getCount(): Int
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: Long): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: Long)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE professionalId = :professionalId ORDER BY id DESC")
    fun getReviewsForProfessional(professionalId: Long): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<Review>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE professionalId = :professionalId ORDER BY timestamp ASC")
    fun getMessagesForProfessional(professionalId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET isRead = 1 WHERE professionalId = :professionalId AND sender = 'PROFESSIONAL'")
    suspend fun markMessagesAsRead(professionalId: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<PushNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: PushNotification): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
}

// ==========================================
// 3. DATABASE HOLDER
// ==========================================

@Database(
    entities = [
        Professional::class,
        Booking::class,
        Review::class,
        ChatMessage::class,
        PushNotification::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun professionalDao(): ProfessionalDao
    abstract fun bookingDao(): BookingDao
    abstract fun reviewDao(): ReviewDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "servicos_locais_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. DATA REPOSITORY
// ==========================================

class AppRepository(val db: AppDatabase) {
    val professionals: Flow<List<Professional>> = db.professionalDao().getAllProfessionals()
    val bookings: Flow<List<Booking>> = db.bookingDao().getAllBookings()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()
    val notifications: Flow<List<PushNotification>> = db.notificationDao().getAllNotifications()
    val allChatMessages: Flow<List<ChatMessage>> = db.chatDao().getAllMessages()

    fun getProfessionalsByCategory(category: String): Flow<List<Professional>> {
        return db.professionalDao().getProfessionalsByCategory(category)
    }

    suspend fun getProfessionalById(id: Long): Professional? {
        return db.professionalDao().getProfessionalById(id)
    }

    suspend fun addProfessional(professional: Professional): Long {
        return db.professionalDao().insertProfessional(professional)
    }

    suspend fun deleteProfessional(professional: Professional) {
        db.professionalDao().deleteProfessional(professional)
    }

    suspend fun getBookingById(id: Long): Booking? {
        return db.bookingDao().getBookingById(id)
    }

    suspend fun addBooking(booking: Booking): Long {
        return db.bookingDao().insertBooking(booking)
    }

    suspend fun updateBooking(booking: Booking) {
        db.bookingDao().updateBooking(booking)
    }

    fun getReviewsForProfessional(profId: Long): Flow<List<Review>> {
        return db.reviewDao().getReviewsForProfessional(profId)
    }

    suspend fun addReview(review: Review) {
        db.reviewDao().insertReview(review)
    }

    fun getChatMessages(profId: Long): Flow<List<ChatMessage>> {
        return db.chatDao().getMessagesForProfessional(profId)
    }

    suspend fun addChatMessage(message: ChatMessage): Long {
        return db.chatDao().insertMessage(message)
    }

    suspend fun markChatAsRead(profId: Long) {
        db.chatDao().markMessagesAsRead(profId)
    }

    suspend fun addNotification(notification: PushNotification): Long {
        return db.notificationDao().insertNotification(notification)
    }

    suspend fun markNotificationsAsRead() {
        db.notificationDao().markAllAsRead()
    }

    // Prepopulate DB with high quality simulated Mozambique professionals
    suspend fun prepopulateIfNeeded() {
        if (db.professionalDao().getCount() == 0) {
            val list = listOf(
                Professional(
                    name = "Marcio Silva",
                    category = "Eletricista",
                    rating = 4.9,
                    reviewCount = 48,
                    ratePerHour = 650.00,
                    description = "Especialista em instalações residenciais, curto-circuitos e padrões de energia. 15 anos de experiência e certificado pelo IFPELAC. Atendimento rápido e seguro.",
                    location = "Sommerschield, Maputo (A 1.2 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Quadros de Força & Redes Trifásicas de Alto Padrão",
                    yearsOfExperience = 15,
                    portfolioImages = "Montagem de Quadro Geral|Iluminação Integrada de Jardim|Reparo de Fiação Condominial",
                    portfolioLinks = "https://marciosilva-eletrica.com.br|https://instagram.com/marcio_silva_eletricista",
                    certificates = "IFPELAC - Eletricista Instalador Residencial e Industrial|Credenciado pela ARENE - Autoridade Reguladora de Energia|Certificação de Segurança Eléctrica Avançada de Moçambique"
                ),
                Professional(
                    name = "Albertina Mondlane",
                    category = "Diarista / Limpeza",
                    rating = 4.9,
                    reviewCount = 124,
                    ratePerHour = 350.00,
                    description = "Arrumação fina, limpeza profunda pós-obra e organização profissional de armários. Dedicação, honestidade e capricho absoluto em cada detalhe.",
                    location = "Polana Cimento, Maputo (A 2.5 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível amanhã",
                    specialization = "Organização Fina (Personal Organizer) & Limpeza Pós-Obra",
                    yearsOfExperience = 8,
                    portfolioImages = "Organização de Closets Contemporâneos|Limpeza Pós-Obra de Apartamento de Alto Padrão|Organização Completa de Cozinha Integrada",
                    portfolioLinks = "https://albertinaorganizer.co.mz|https://instagram.com/albertina_organiza_tudo",
                    certificates = "AMEL - Organização e Gestão de Espaços Domésticos|Curso de Técnicas Avançadas de Higienização de Ambientes|Especialização de Organização de Escritórios de Alto Padrão"
                ),
                Professional(
                    name = "Filipe Macamo",
                    category = "Canalizador",
                    rating = 4.8,
                    reviewCount = 37,
                    ratePerHour = 600.00,
                    description = "Localização e reparo de vazamentos ocultos (caça-vazamentos), desentupimento, troca de colunas, torneiras, reparos hidráulicos gerais e preventivos.",
                    location = "Bairro Central, Maputo (A 0.8 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Caça-Vazamentos por Ultrassom & Tubulações de PPR/Cobre",
                    yearsOfExperience = 12,
                    portfolioImages = "Localização de Vazamento Não-Invasivo|Instalação de Tubulação Termofusão PPR|Troca de Válvula de Pressão e Descargas Acopladas",
                    portfolioLinks = "https://filipemacamohidraulica.co.mz|https://facebook.com/filipemacamencanador",
                    certificates = "Adequa Moçambique - Instalações Prediais Avançadas de Água e Saneamento|IFPELAC - Instalador Hidráulico de Edifícios Residenciais|Certificado de Manutenção de Metais e Válvulas Reguladoras"
                ),
                Professional(
                    name = "Carlos Tembe",
                    category = "Pintor",
                    rating = 4.7,
                    reviewCount = 52,
                    ratePerHour = 450.00,
                    description = "Pintura residencial e comercial de fino acabamento, aplicação de textura, grafiato, massa de nivelamento e restauração de fachadas com segurança elevada.",
                    location = "Coop, Maputo (A 4.1 km)",
                    isVerified = true,
                    availabilityStatus = "Vagas para quinta-feira",
                    specialization = "Pintura Premium de Fino Acabamento & Efeitos Decorativos",
                    yearsOfExperience = 10,
                    portfolioImages = "Pintura de Parede com Efeito Cimento Queimado|Restauração de Fachada Comercial|Aplicação Primorosa de Massa de Nivelamento Hidrorrepelente",
                    portfolioLinks = "https://carlostembepinturas.co.mz|https://instagram.com/carlinhos_pintor_maputo",
                    certificates = "Pinturas CIN Moçambique - Pintura Profissional e Efeitos Decorativos|Sherwin Williams S.A. - Revestimentos Especiais de Alto Padrão|Certificado de HST - Segurança e Trabalho em Alturas"
                ),
                Professional(
                    name = "Ronaldo Langa",
                    category = "Chaveiro / Serralheiro",
                    rating = 5.0,
                    reviewCount = 63,
                    ratePerHour = 700.00,
                    description = "Chaveiro 24h especializado em fechaduras digitais, abertura técnica de portas sem danos, cópias de chaves codificadas e portões automáticos.",
                    location = "Bairro do Triunfo, Maputo (A 1.5 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Instalação de Fechaduras Digitais & Abertura de Alta Segurança",
                    yearsOfExperience = 14,
                    portfolioImages = "Instalação de Fechadura Digital Biométrica Samsung|Abertura de Cofre Digital sem Perfuração|Criação de Sistema de Chave Mestra para Condomínio",
                    portfolioLinks = "https://ronaldolangachaveiro24h.co.mz|https://instagram.com/ronaldo_chaveiro_maputo",
                    certificates = "Yale/Intelbras - Instalador Credenciado de Fechaduras Eletrónicas|Keso Security - Sistemas de Alta Segurança de Cilindros e Fechaduras|Associação Moçambicana de Segurança Eletrónica (Membro Ativo)"
                ),
                Professional(
                    name = "Regina Mucavel",
                    category = "Jardineiro",
                    rating = 4.8,
                    reviewCount = 29,
                    ratePerHour = 400.00,
                    description = "Projeto paisagístico, poda de cercas vivas, corte de capim/gramado, combate a pragas de forma ecológica e adubação para florescimento saudável.",
                    location = "Machava, Matola (A 8.8 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível amanhã",
                    specialization = "Criação de Projetos Paisagísticos Eco-Friendly & Manutenção",
                    yearsOfExperience = 7,
                    portfolioImages = "Paisagismo Completo de Varanda Gourmet|Manutenção de Parede Verde Vertical de Samambaias|Rejuvenescimento de Jardim de Vivenda na Matola",
                    portfolioLinks = "https://reginamucaveljardinagem.co.mz|https://pinterest.com/regina_mucavel_jardins",
                    certificates = "Curso de Jardinagem e Paisagismo Clássico - Jardim Botânico de Maputo|Curso de Controle Orgânico de Pragas Tropicais|Membro da Associação Eco-Sustentável da Província de Maputo"
                ),
                Professional(
                    name = "Rodrigo Nhaca",
                    category = "Técnico de TI",
                    rating = 4.9,
                    reviewCount = 41,
                    ratePerHour = 750.00,
                    description = "Montagem de micros, configuração de roteadores Wi-Fi Mesh rápidos, remoção de vírus, backup seguro de dados e configuração de redes de internet fibra.",
                    location = "Malhangalene, Maputo (A 3.0 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Redes de Internet Wi-Fi Mesh de Alta Performance & Hardware de Elite",
                    yearsOfExperience = 9,
                    portfolioImages = "Montagem e Cable Management de PC Gamer|Cabeamento Estruturado Residencial CAT6|Instalação de Sistema de Backup em Rede Local NAS Synology",
                    portfolioLinks = "https://rodrigonhacati.co.mz|https://github.com/rodrigonhaca",
                    certificates = "Cisco CCNA - Routing & Switching Avançado|CompTIA Network+ - Especialista em Rede de Computadores e WiFi|Certificação de Cabeamento em Fibra Óptica"
                ),
                Professional(
                    name = "Augusto Tembe",
                    category = "Carpinteiro",
                    rating = 4.9,
                    reviewCount = 54,
                    ratePerHour = 500.00,
                    description = "Carpintaria profissional de alta precisão. Projetos personalizados, restauro de móveis antigos e instalação técnica de portas e pavimentos de madeira maciça.",
                    location = "Mavoco, Matola (A 7.5 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Carpintaria Fina, Mobiliário Sob Medida e Madeiras Nobres",
                    yearsOfExperience = 11,
                    portfolioImages = "Móvel de TV Personalizado em Umbila|Restauração de Mesa de Jantar Colonial|Instalação de Portas de Mogno Sob Medida",
                    portfolioLinks = "https://augustotembe-carpintaria.co.mz",
                    certificates = "IFPELAC - Carpinteiro de Limpos Especialista|Certificação de Gestão Sustentável de Madeiras Moçambicanas"
                ),
                Professional(
                    name = "Bernardo Cossa",
                    category = "Motorista",
                    rating = 4.8,
                    reviewCount = 72,
                    ratePerHour = 550.00,
                    description = "Motorista experiente e pontual para deslocamentos urbanos, viagens interprovinciais e transferes executivos ao Aeroporto de Maputo. Viatura própria segura com ar condicionado.",
                    location = "Aero-Porto, Maputo (A 2.3 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível amanhã",
                    specialization = "Transporte de Passageiros e Logística Urbana Segura",
                    yearsOfExperience = 9,
                    portfolioImages = "Viatura Executiva Sedã Higienizada|Rotas Rápidas e Seguras na Área Metropolitana de Maputo",
                    portfolioLinks = "https://cossatransporte-executivo.co.mz",
                    certificates = "Licença de Condução Profissional de Passageiros (Serviço Público)|Curso de Direção Defensiva e Primeiros Socorros da Cruz Vermelha"
                ),
                Professional(
                    name = "Sónia Mucavel",
                    category = "Explicador",
                    rating = 4.9,
                    reviewCount = 43,
                    ratePerHour = 450.00,
                    description = "Aconselhamento e explicações completas de Matemática, Física e Química para ensino secundário, preparatórios de exames nacionais e admissão à UEM.",
                    location = "Alto Maé, Maputo (A 1.1 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Preparação Intensiva para Admissões e Matemática Superior",
                    yearsOfExperience = 6,
                    portfolioImages = "Sala de Estudos Equipada com Material Concreto|Atas de Aprovação Elevada nos Exames Nacionais",
                    portfolioLinks = "https://sonia-explicacoes.co.mz",
                    certificates = "Licenciatura em Ensino de Matemática - Universidade Eduardo Mondlane|Acreditação Pedagógica para Formadores e Explicadores Privados"
                ),
                Professional(
                    id = 11,
                    name = "José Machava",
                    category = "Canalizador / Encanador",
                    rating = 4.8,
                    reviewCount = 30,
                    ratePerHour = 500.00,
                    description = "Técnico especialista em canalização residencial, vazamentos e reparos hidráulicos de emergência no geral.",
                    location = "Bairro Central, Maputo (A 0.9 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível amanhã",
                    specialization = "Caça-Vazamentos de Precisão & Sistemas de Drenagem",
                    yearsOfExperience = 10,
                    portfolioImages = "Limpeza de Caixa de Água de Alto Volume|Reparo Hidráulico de Tubulação de Cobre",
                    portfolioLinks = "https://josemachavacanador.co.mz",
                    certificates = "IFPELAC - Especialista em Tubulação Predial e Saneamento"
                ),
                Professional(
                    id = 12,
                    name = "Amina Juma",
                    category = "Diarista / Limpeza",
                    rating = 4.8,
                    reviewCount = 35,
                    ratePerHour = 320.00,
                    description = "Especialista em limpeza residencial diária, lavagem de cortinas de seda, tapetes orientais e arrumação geral profunda com muito capricho.",
                    location = "Mavoco, Matola (A 6.2 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Limpeza Profunda & Cuidado com Tecidos Delicados",
                    yearsOfExperience = 5,
                    portfolioImages = "Limpeza de Sala de EstAr Estilo Clássico|Arrumação de Sofás e Cortinas de Alto Padrão",
                    portfolioLinks = "https://aminajumalimperas.co.mz",
                    certificates = "Curso Oficial AMEL de Organização Higiénica e Técnica"
                ),
                Professional(
                    id = 13,
                    name = "Mateus Nhantumbo",
                    category = "Eletricista",
                    rating = 4.7,
                    reviewCount = 19,
                    ratePerHour = 600.00,
                    description = "Foco em fiação residencial, manutenção corretiva, instalação rápida de geradores domésticos e sistemas solares fotovoltaicos eficientes.",
                    location = "Aero-Porto, Maputo (A 4.0 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível hoje",
                    specialization = "Sistemas Solares & Instalações de Emergência",
                    yearsOfExperience = 7,
                    portfolioImages = "Instalação de Inversor Solar Híbrido|Troca Completa de Fiação de Cozinha Industrial",
                    portfolioLinks = "https://mateuselétrica.co.mz",
                    certificates = "Credenciado pela ARENE - Especialista Solar e Redes de Distribuição"
                ),
                Professional(
                    id = 14,
                    name = "Cláudia Cossa",
                    category = "Explicador",
                    rating = 5.0,
                    reviewCount = 28,
                    ratePerHour = 500.00,
                    description = "Explicadora e tutora certificada de Inglês, Francês e Português. Conversação rápida, redação elegante e acompanhamento preparatório escolar total.",
                    location = "Polana Cimento, Maputo (A 1.0 km)",
                    isVerified = true,
                    availabilityStatus = "Disponível amanhã",
                    specialization = "Conversação Idiomas & Redação de Redes de Elite",
                    yearsOfExperience = 8,
                    portfolioImages = "Sala de Leitura Interativa para Crianças e Adultos",
                    portfolioLinks = "https://claudiacossaexplicadora.co.mz",
                    certificates = "Licenciatura em Ensino de Línguas Estrangeiras - Faculdade de Letras UEM"
                )
            )
            db.professionalDao().insertProfessionals(list)

            // Let's add some verified reviews as well
            val reviews = listOf(
                Review(
                    professionalId = 1,
                    authorName = "Guilherme Macuácua",
                    rating = 5,
                    comment = "Marcio resolveu um problema de queda de energia na minha cozinha em 30 minutos. Preço justo e muito profissional. Recomendo muito!",
                    dateStr = "12/06/2026",
                    isVerifiedPurchase = true
                ),
                Review(
                    professionalId = 1,
                    authorName = "Mariana Nhantumbo",
                    rating = 4,
                    comment = "Fez a instalação de todos os lustres do apartamento novo no Sommerschield. Trabalho muito limpo.",
                    dateStr = "08/06/2026",
                    isVerifiedPurchase = true
                ),
                Review(
                    professionalId = 2,
                    authorName = "Beatriz Covane",
                    rating = 5,
                    comment = "A Albertina é de uma educação e capricho absurdos! Deixou o apartamento brilhando e muito cheiroso. Voltarei a agendar com certeza.",
                    dateStr = "14/06/2026",
                    isVerifiedPurchase = true
                ),
                Review(
                    professionalId = 3,
                    authorName = "Inácio Marrengula",
                    rating = 5,
                    comment = "Filipe foi super honesto. Achou o vazamento que outro encanador disse que precisaria quebrar a parede toda.",
                    dateStr = "11/06/2026",
                    isVerifiedPurchase = true
                ),
                Review(
                    professionalId = 5,
                    authorName = "Juliana Sitoe",
                    rating = 5,
                    comment = "Trocou a fechadura da minha porta de madrugada super rápido. Preço de emergência justo.",
                    dateStr = "15/06/2026",
                    isVerifiedPurchase = true
                )
            )
            db.reviewDao().insertReviews(reviews)

            // Seed initial bookings matching the mockup and active days (June 18 and June 19, 2026)
            val seedBookings = listOf(
                Booking(
                    id = 1,
                    professionalId = 5,
                    professionalName = "Ronaldo Langa",
                    professionalCategory = "Chaveiro · Serralheiro",
                    date = "2026-06-19",
                    time = "14:00",
                    hours = 1,
                    totalAmount = 700.0,
                    address = "Rua da Resistência, 45 — Polana Cimento",
                    notes = "Porta principal, fechadura Samsung digital",
                    status = "Pendente",
                    paymentStatus = "Pendente",
                    cardLast4 = "",
                    customerName = "Sofia Mabjaia",
                    customerPhone = "+258 84 123 4567"
                ),
                Booking(
                    id = 2,
                    professionalId = 1,
                    professionalName = "Marcio Silva",
                    professionalCategory = "Eletricista",
                    date = "2026-06-19",
                    time = "14:00",
                    hours = 1,
                    totalAmount = 650.0,
                    address = "Polana Cimento",
                    notes = "Instalação",
                    status = "Pendente",
                    paymentStatus = "Pendente",
                    cardLast4 = ""
                ),
                Booking(
                    id = 3,
                    professionalId = 2,
                    professionalName = "Albertina Mondlane",
                    professionalCategory = "Diarista / Limpeza",
                    date = "2026-06-19",
                    time = "09:00",
                    hours = 4,
                    totalAmount = 1400.0,
                    address = "Bairro Central, Maputo",
                    notes = "Limpeza profunda geral",
                    status = "Concluído",
                    paymentStatus = "Pago",
                    cardLast4 = "e-Mola"
                ),
                Booking(
                    id = 4,
                    professionalId = 11,
                    professionalName = "José Machava",
                    professionalCategory = "Canalizador / Encanador",
                    date = "2026-06-12",
                    time = "10:00",
                    hours = 2,
                    totalAmount = 1000.0,
                    address = "Bairro Central, Maputo",
                    notes = "Conserto de vazamento de torneira e sifão",
                    status = "Concluído",
                    paymentStatus = "Pago",
                    cardLast4 = "M-Pesa"
                ),
                Booking(
                    id = 5,
                    professionalId = 5,
                    professionalName = "Ronaldo Langa",
                    professionalCategory = "Chaveiro · Serralheiro",
                    date = "2026-06-20",
                    time = "09:00",
                    hours = 2,
                    totalAmount = 1400.0,
                    address = "Av. Kenneth Kaunda — Maputo Centro",
                    notes = "Abertura de cofre",
                    status = "Pendente",
                    paymentStatus = "Pendente",
                    cardLast4 = "",
                    customerName = "Lurdes Nhamule",
                    customerPhone = "+258 82 987 6543"
                ),
                Booking(
                    id = 6,
                    professionalId = 5,
                    professionalName = "Ronaldo Langa",
                    professionalCategory = "Chaveiro · Serralheiro",
                    date = "2026-06-20",
                    time = "10:00",
                    hours = 1,
                    totalAmount = 350.0,
                    address = "Av. 25 de Setembro — Sommerschield",
                    notes = "Cópia de 3 chaves",
                    status = "Confirmado",
                    paymentStatus = "Pendente",
                    cardLast4 = "e-Mola",
                    customerName = "Carlos Tembe",
                    customerPhone = "+258 85 555 4321"
                )
            )
            for (bk in seedBookings) {
                db.bookingDao().insertBooking(bk)
            }

            // Seed initial chat messages to make list of conversations fully complete
            val seedMessages = listOf(
                ChatMessage(
                    professionalId = 5,
                    sender = "CLIENT",
                    messageText = "principal e verificar a fechadura do portão.",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = true
                ),
                ChatMessage(
                    professionalId = 5,
                    sender = "PROFESSIONAL",
                    messageText = "Perfeito, trago as ferramentas todas. Tem alguma marca específica de fechadura?",
                    timestamp = System.currentTimeMillis() - 3400000,
                    isRead = true
                ),
                ChatMessage(
                    professionalId = 5,
                    sender = "CLIENT",
                    messageText = "Sim, é uma Yale. A do portão acho que é Keso.",
                    timestamp = System.currentTimeMillis() - 3000000,
                    isRead = true
                ),
                ChatMessage(
                    professionalId = 5,
                    sender = "PROFESSIONAL",
                    messageText = "Beleza, já vou separar as peças corretas aqui.",
                    timestamp = System.currentTimeMillis() - 2800000,
                    isRead = false
                ),
                ChatMessage(
                    professionalId = 5,
                    sender = "PROFESSIONAL",
                    messageText = "Já estou a caminho, chego em 10 min ✓",
                    timestamp = System.currentTimeMillis() - 2500000,
                    isRead = false
                ),

                ChatMessage(
                    professionalId = 1,
                    sender = "CLIENT",
                    messageText = "Oi Marcio, consegue confirmar o horário?",
                    timestamp = System.currentTimeMillis() - 86400000 * 1,
                    isRead = true
                ),
                ChatMessage(
                    professionalId = 1,
                    sender = "PROFESSIONAL",
                    messageText = "Ok, confirmado para amanhã às 14h...",
                    timestamp = System.currentTimeMillis() - 86400000 * 1 + 50000,
                    isRead = true
                ),

                ChatMessage(
                    professionalId = 2,
                    sender = "CLIENT",
                    messageText = "Obrigado pelo excelente serviço de ontem!",
                    timestamp = System.currentTimeMillis() - 86400000 * 3,
                    isRead = true
                ),
                ChatMessage(
                    professionalId = 2,
                    sender = "PROFESSIONAL",
                    messageText = "Obrigada pela avaliação! 🙏",
                    timestamp = System.currentTimeMillis() - 86400000 * 3 + 60000,
                    isRead = true
                ),

                ChatMessage(
                    professionalId = 11,
                    sender = "CLIENT",
                    messageText = "José, tudo certo com a torneira?",
                    timestamp = System.currentTimeMillis() - 86400000 * 6,
                    isRead = true
                ),
                ChatMessage(
                    professionalId = 11,
                    sender = "PROFESSIONAL",
                    messageText = "O serviço ficou concluído com sucesso.",
                    timestamp = System.currentTimeMillis() - 86400000 * 6 + 60000,
                    isRead = true
                )
            )
            for (msg in seedMessages) {
                db.chatDao().insertMessage(msg)
            }
            
            // Add initial welcome notifications
            db.notificationDao().insertNotification(
                PushNotification(
                    bookingId = null,
                    title = "Bem-vindo ao Serviços Locais Moçambique! 🇲🇿👋",
                    body = "Encontre eletricistas, diaristas, encanadores e muito mais a poucos passos de si com contratação 100% segura usando M-Pesa, e-Mola e Cartões."
                )
            )
            db.notificationDao().insertNotification(
                PushNotification(
                    bookingId = null,
                    title = "Segurança em Primeiro Lugar! 🔒🛡️",
                    body = "Seu perfil foi verificado com sucesso por biometria. Todos os pagamentos e conversas na plataforma estão cobertos pela Garantia de Serviço Maputo."
                )
            )
        }
    }
}
