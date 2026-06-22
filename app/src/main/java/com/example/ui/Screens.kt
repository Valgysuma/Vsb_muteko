package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R


// Centralized colors matching the Moçambique Dark Premium branding
val MarketTeal = Color(0xFF4DD99B) // Modern Mint Green Accent
val MarketTealLight = Color(0xFF133125) // Smooth translucent dark green for status tags
val DarkBackground = Color(0xFF161616) // Charcoal dark background
val CardBackground = Color(0xFF1E1E1E) // Premium flat dark card backgrounds
val AccentGold = Color(0xFFFFA000)     // Classic Gold Rating Star
val OutlinedBorderColor = Color(0xFF2E2E2E) // Fine borders for cards & categories



// ==========================================
// CENTRAL MARKETPLACE ENGINE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeInAppNotification by viewModel.activeInAppNotification.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeMode by viewModel.activeProfileMode.collectAsState()
    var isOnboardingCompleted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(errorMessage) {
        errorMessage?.let { errorMsg ->
            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    if (!isLoggedIn) {
        if (!isOnboardingCompleted) {
            OnboardingScreen(
                onOnboardingComplete = { isOnboardingCompleted = true },
                onGoToLogin = { isOnboardingCompleted = true }
            )
        } else {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { viewModel.setLoggedIn(true) }
            )
        }
        return
    }

    // Screen stack navigation
    var screenStack by remember { mutableStateOf<List<String>>(listOf("main")) }
    val currentScreen = screenStack.last()

    val selectedProfId by viewModel.selectedProfessionalId.collectAsState()
    val selectedProfessional by viewModel.selectedProfessional.collectAsState(initial = null)

    var currentBookingIdForPayment by remember { mutableStateOf<Long?>(null) }

    fun navigateTo(screen: String) {
        screenStack = screenStack + screen
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack = screenStack.dropLast(1)
        }
    }

    fun resetToMain() {
        screenStack = listOf("main")
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentScreen == "main") {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4DD99B),
                        unselectedIconColor = Color(0xFF5A5A5A),
                        selectedTextColor = Color(0xFF4DD99B),
                        unselectedTextColor = Color(0xFF555555),
                        indicatorColor = Color.Transparent
                    )
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF1A1A1A))
                        )
                        NavigationBar(
                            containerColor = Color(0xFF0A0A0A),
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("main_navigation_bar")
                        ) {
                            if (activeMode == "Profissional") {
                                NavigationBarItem(
                                    selected = currentTab == "dashboard" || currentTab == "buscar",
                                    onClick = { viewModel.selectTab("dashboard") },
                                    label = { Text("Dashboard", fontSize = 11.sp, fontWeight = if (currentTab == "dashboard" || currentTab == "buscar") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "dashboard" || currentTab == "buscar") Icons.Filled.GridView else Icons.Outlined.GridView,
                                            contentDescription = "Dashboard"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_dashboard_item")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "pedidos_tab",
                                    onClick = { viewModel.selectTab("pedidos_tab") },
                                    label = { Text("Pedidos", fontSize = 11.sp, fontWeight = if (currentTab == "pedidos_tab") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "pedidos_tab") Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                            contentDescription = "Pedidos recebidos"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_pedidos_item")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "agenda_tab",
                                    onClick = { viewModel.selectTab("agenda_tab") },
                                    label = { Text("Agenda", fontSize = 11.sp, fontWeight = if (currentTab == "agenda_tab") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "agenda_tab") Icons.Filled.DateRange else Icons.Outlined.DateRange,
                                            contentDescription = "Disponibilidade"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_agenda_prof_item")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "perfil_tab" || currentTab == "perfil",
                                    onClick = { viewModel.selectTab("perfil_tab") },
                                    label = { Text("Perfil", fontSize = 11.sp, fontWeight = if (currentTab == "perfil_tab" || currentTab == "perfil") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "perfil_tab" || currentTab == "perfil") Icons.Filled.Person else Icons.Outlined.Person,
                                            contentDescription = "Perfil"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_perfil_prof_item")
                                )
                            } else {
                                NavigationBarItem(
                                    selected = currentTab == "buscar",
                                    onClick = { viewModel.selectTab("buscar") },
                                    label = { Text("Buscar", fontSize = 11.sp, fontWeight = if (currentTab == "buscar") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "buscar") Icons.Filled.Search else Icons.Outlined.Search,
                                            contentDescription = "Buscar profissionais"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_buscar_item")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "agendamentos",
                                    onClick = { viewModel.selectTab("agendamentos") },
                                    label = { Text("Agenda", fontSize = 11.sp, fontWeight = if (currentTab == "agendamentos") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "agendamentos") Icons.Filled.DateRange else Icons.Outlined.DateRange,
                                            contentDescription = "Meus agendamentos"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_agenda_item")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "mensagens",
                                    onClick = { viewModel.selectTab("mensagens") },
                                    label = { Text("Conversas", fontSize = 11.sp, fontWeight = if (currentTab == "mensagens") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "mensagens") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = "Canais de chat"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_chat_item")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "perfil",
                                    onClick = { viewModel.selectTab("perfil") },
                                    label = { Text("Perfil", fontSize = 11.sp, fontWeight = if (currentTab == "perfil") FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == "perfil") Icons.Filled.Person else Icons.Outlined.Person,
                                            contentDescription = "Meu Perfil"
                                        )
                                    },
                                    colors = navItemColors,
                                    modifier = Modifier.testTag("tab_perfil_item")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // NAVIGATION DISPATCHER
                when (currentScreen) {
                    "main" -> {
                        if (activeMode == "Profissional") {
                            when (currentTab) {
                                "dashboard" -> ProfessionalDashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToTab = { tab -> viewModel.selectTab(tab) }
                                )
                                "pedidos_tab" -> ProfessionalRequestsScreen(
                                    viewModel = viewModel,
                                    onOpenChat = { profId ->
                                        viewModel.selectProfessional(profId)
                                        navigateTo("chat")
                                    }
                                )
                                "agenda_tab" -> ProfessionalAvailabilityScreen(
                                    viewModel = viewModel
                                )
                                "perfil_tab" -> ProfessionalProfileTabScreen(
                                    viewModel = viewModel,
                                    onBackToClient = {
                                        viewModel.setActiveProfileMode("Cliente")
                                    }
                                )
                                else -> {
                                    ProfessionalDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToTab = { tab -> viewModel.selectTab(tab) }
                                    )
                                }
                            }
                        } else {
                            when (currentTab) {
                                "buscar" -> HomeScreen(
                                    viewModel = viewModel,
                                    onProfessionalClick = { profId ->
                                        viewModel.selectProfessional(profId)
                                        navigateTo("detail")
                                    },
                                    onNotificationsClick = { navigateTo("notifications") }
                                )
                                "agendamentos" -> BookingsScreen(
                                    viewModel = viewModel,
                                    onPayNow = { bookingId ->
                                        currentBookingIdForPayment = bookingId
                                        navigateTo("payment")
                                    },
                                    onChatWith = { profId ->
                                        viewModel.selectProfessional(profId)
                                        navigateTo("chat")
                                    }
                                )
                                "mensagens" -> ActiveChatsListScreen(
                                    viewModel = viewModel,
                                    onOpenChat = { profId ->
                                        viewModel.selectProfessional(profId)
                                        navigateTo("chat")
                                    }
                                )
                                "perfil" -> UserProfileScreen(
                                    viewModel = viewModel,
                                    onNavigate = { screen -> navigateTo(screen) }
                                )
                            }
                        }
                    }
                    "detail" -> {
                        selectedProfessional?.let { prof ->
                            ProfessionalDetailScreen(
                                professional = prof,
                                viewModel = viewModel,
                                onBack = { navigateBack() },
                                onBookClick = { navigateTo("schedule") },
                                onChatClick = { navigateTo("chat") }
                            )
                        } ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Carregando profissional...")
                            }
                        }
                    }
                    "schedule" -> {
                        selectedProfessional?.let { prof ->
                            SchedulingScreen(
                                professional = prof,
                                viewModel = viewModel,
                                onBack = { navigateBack() },
                                onBookingCreated = { bookingId ->
                                    resetToMain()
                                    viewModel.selectTab("agendamentos")
                                }
                            )
                        }
                    }
                    "payment" -> {
                        currentBookingIdForPayment?.let { bookingId ->
                            PaymentScreen(
                                bookingId = bookingId,
                                viewModel = viewModel,
                                onPaymentDone = {
                                    resetToMain()
                                    viewModel.selectTab("agendamentos")
                                },
                                onCancel = { navigateBack() }
                            )
                        }
                    }
                    "chat" -> {
                        selectedProfessional?.let { prof ->
                            ChatScreen(
                                professional = prof,
                                viewModel = viewModel,
                                onBack = { navigateBack() },
                                onRebook = { navigateTo("schedule") }
                            )
                        }
                    }
                    "notifications" -> {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onBack = { navigateBack() },
                            onNavigateToTab = { tab ->
                                viewModel.selectTab(tab)
                                resetToMain()
                            },
                            onNavigateToChat = { profId ->
                                viewModel.selectProfessional(profId)
                                resetToMain()
                                navigateTo("chat")
                            }
                        )
                    }
                    "professional_profile" -> {
                        MyProfessionalProfileScreen(
                            viewModel = viewModel,
                            onBack = { navigateBack() }
                        )
                    }
                }

                // IN-APP PUSH NOTIFICATION SIMULATION OVERLAY BANNER
                AnimatedVisibility(
                    visible = activeInAppNotification != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .testTag("push_notification_banner_overlay")
                ) {
                    activeInAppNotification?.let { notif ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 480.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MarketTeal, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        tint = Color.White,
                                        contentDescription = "Alerta de push"
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = notif.body,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { viewModel.dismissInAppNotification() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar notificação",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN (BUSCAR PROFISSIONAIS)
// ==========================================

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onProfessionalClick: (Long) -> Unit,
    onNotificationsClick: () -> Unit
) {
    val professionals by viewModel.professionals.collectAsState()
    val isLoadingProfessionals by viewModel.isLoadingProfessionals.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf(
        "Todos",
        "Eletricista",
        "Diarista / Limpeza",
        "Canalizador",
        "Explicador",
        "Carpinteiro",
        "Motorista",
        "Pintor",
        "Chaveiro / Serralheiro",
        "Jardineiro",
        "Técnico de TI"
    )

    // Dynamic search filtering over the database professionals list
    val filteredProfessionals = remember(professionals, searchQuery, selectedCategory) {
        val categoryFiltered = if (selectedCategory == null) {
            professionals
        } else {
            professionals.filter { it.category == selectedCategory }
        }
        
        if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { prof ->
                prof.name.contains(searchQuery, ignoreCase = true) ||
                prof.category.contains(searchQuery, ignoreCase = true) ||
                prof.specialization.contains(searchQuery, ignoreCase = true) ||
                prof.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var selectedSort by remember { mutableStateOf("Relevância") }

    val sortedProfessionals = remember(filteredProfessionals, selectedSort) {
        when (selectedSort) {
            "Avaliação" -> filteredProfessionals.sortedByDescending { it.rating }
            "Preço" -> filteredProfessionals.sortedBy { it.ratePerHour }
            "Distância" -> filteredProfessionals.sortedBy { prof ->
                val distStr = extractDistanceValue(prof.location)
                distStr.toDoubleOrNull() ?: 1.5
            }
            else -> filteredProfessionals // Relevância
        }
    }

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    
    val headerHeightExpanded = 270.dp
    val headerHeightCollapsed = 145.dp
    val collapseRangePx = with(density) { (headerHeightExpanded - headerHeightCollapsed).toPx() }
    
    val collapseProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (scrollState.firstVisibleItemScrollOffset / collapseRangePx).coerceIn(0f, 1f)
            }
        }
    }

    val currentHeaderHeight = headerHeightExpanded - (headerHeightExpanded - headerHeightCollapsed) * collapseProgress

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        // 1. SCROLLABLE CONTENT BODY (LazyColumn)
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("professionals_list"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // First item is a phantom spacer so that items begin exactly below the expanded header and can scroll underneath it
            item {
                Spacer(modifier = Modifier.height(headerHeightExpanded))
            }

            // Categories Section Title
            item {
                Text(
                    text = "CATEGORIAS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
                )
            }

            // Categories horizontal list row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = (cat == "Todos" && selectedCategory == null) || (selectedCategory == cat)
                        val displayName = when (cat) {
                            "Todos" -> "Todos"
                            "Eletricista" -> "Elétrica"
                            "Diarista / Limpeza" -> "Limpeza"
                            "Canalizador" -> "Canalizador"
                            "Explicador" -> "Explicador"
                            "Carpinteiro" -> "Carpintaria"
                            "Motorista" -> "Motorista"
                            "Pintor" -> "Pintura"
                            "Chaveiro / Serralheiro" -> "Reparações"
                            "Jardineiro" -> "Jardins"
                            "Técnico de TI" -> "Suporte TI"
                            else -> cat
                        }
                        
                        val icon = if (cat == "Todos") Icons.Default.Apps else getCategoryIcon(cat)

                        CategoryItem(
                            categoryName = cat,
                            displayName = displayName,
                            icon = icon,
                            isSelected = isSelected,
                            onClick = {
                                viewModel.selectCategory(if (cat == "Todos") null else cat)
                            }
                        )
                    }
                }
            }

            // Divider
            item {
                Divider(
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color(0xFF2E2E2E)
                )
            }

            // Interactive Sort Chips Ribbon
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(
                        text = "ORDENAR POR",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortOptions = listOf(
                            Triple("Relevância", Icons.Default.Tune, "Relevância"),
                            Triple("Avaliação", Icons.Default.Star, "Melhor Avaliação"),
                            Triple("Preço", Icons.Default.TrendingDown, "Menor Preço"),
                            Triple("Distância", Icons.Default.Navigation, "Mais Próximo")
                        )
                        
                        sortOptions.forEach { (sortKey, sortIcon, sortText) ->
                            val isSorted = selectedSort == sortKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSorted) Color(0xFF133125) else Color(0xFF1E1E1E)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSorted) Color(0xFF2ECC71) else Color(0xFF333333),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedSort = sortKey }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = sortIcon,
                                        contentDescription = null,
                                        tint = if (isSorted) Color(0xFF2ECC71) else Color(0xFF8FA79C),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sortText,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSorted) Color.White else Color(0xFF8FA79C)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // "PRÓXIMOS DE SI" Section Title (with result count)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRÓXIMOS DE SI",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    
                    if (!isLoadingProfessionals && sortedProfessionals.isNotEmpty()) {
                        Text(
                            text = "${sortedProfessionals.size} encontrados",
                            fontSize = 11.sp,
                            color = Color(0xFF2ECC71),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Professionals items / skeleton items
            if (isLoadingProfessionals) {
                items(4) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ProfessionalSkeletonCard()
                    }
                }
            } else if (sortedProfessionals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "Nenhum profissional",
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nenhum profissional nesta busca.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.selectCategory(null)
                                    selectedSort = "Relevância"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF133125),
                                    contentColor = Color(0xFF2ECC71)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Limpar filtros", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(sortedProfessionals) { prof ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ProfessionalItemCard(
                            professional = prof,
                            onClick = { onProfessionalClick(prof.id) }
                        )
                    }
                }
            }
        }

        // 2. FLOATING COLLAPSIBLE HERO HEADER (Moçambique Dark Premium style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeaderHeight)
                .background(Color(0xFF112D21)) // Enforcing dark green theme base
                .shadow(elevation = if (collapseProgress > 0.9f) 6.dp else 0.dp) // Subtle shadow when fully collapsed
        ) {
            Column {
                // Top row with Location Chip and Notification Bell
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = "Localização",
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Maputo, Sommerschield",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Notification Bell (matches mockup top right)
                    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsState()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable { onNotificationsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Alertas",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        if (unreadNotifCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .size(16.dp)
                                    .background(Color(0xFFE53935), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadNotifCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // If collapse progress is not 1f, we show the welcome section
                val titleAlpha = 1f - collapseProgress
                
                if (titleAlpha > 0.05f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .graphicsLayer { alpha = titleAlpha }
                    ) {
                        Spacer(modifier = Modifier.height((24 * titleAlpha).dp))

                        // Welcome bold headline with fine typography
                        Text(
                            text = "Serviços locais,",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = (28 * (0.8f + 0.2f * titleAlpha)).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "prontos para si",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = (28 * (0.8f + 0.2f * titleAlpha)).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4DD99B), // Mint color accent
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height((8 * titleAlpha).dp))

                        Text(
                            text = "Profissionais verificados perto de você",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.65f * titleAlpha),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Modern Search Text Field (fully functional!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Carpinteiro, explicador, canalizador...",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Buscar",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpar busca",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            focusedBorderColor = Color(0xFF4DD99B).copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            cursorColor = Color(0xFF4DD99B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessionalSkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = alpha))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.4f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(60.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = alpha))
                    )
                    
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = alpha))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .width(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun InteractiveStarRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: androidx.compose.ui.unit.Dp = 30.dp
) {
    val ratingTexts = remember {
        listOf(
            "Péssimo 😟",
            "Regular 😐",
            "Bom 🙂",
            "Muito Bom 😊",
            "Excelente! 🌟"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..maxStars).forEach { starIndex ->
                val isSelected = starIndex <= rating
                
                // Fine dynamic scaling with bouncy physical spring effect!
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.25f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "star_scale_$starIndex"
                )

                val starColor by animateColorAsState(
                    targetValue = if (isSelected) AccentGold else Color(0xFF424242),
                    animationSpec = tween(durationMillis = 150),
                    label = "star_color_$starIndex"
                )

                // 48dp x 48dp perfect accessible touch targets
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onRatingChanged(starIndex) }
                        .testTag("interactive_star_$starIndex"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Nota $starIndex de $maxStars",
                        tint = starColor,
                        modifier = Modifier
                            .size(starSize)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        AnimatedVisibility(
            visible = rating in 1..maxStars,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (rating in 1..maxStars) {
                Text(
                    text = ratingTexts[rating - 1],
                    color = AccentGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp).testTag("star_rating_label")
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    categoryName: String,
    displayName: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val catColor = getCategoryThemeColor(categoryName)
    val boxBg = if (isSelected) catColor.copy(alpha = 0.15f) else Color(0xFF1E1E1E)
    val iconTint = if (isSelected) catColor else Color.White.copy(alpha = 0.4f)
    val textStyleColor = if (isSelected) catColor else Color.White.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(80.dp)
            .padding(vertical = 4.dp)
            .testTag("category_item_${categoryName.replace(" ", "_").replace(" / ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(boxBg)
                .border(
                    width = 1.dp,
                    color = if (isSelected) catColor.copy(alpha = 0.5f) else Color(0xFF2E2E2E),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayName,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = displayName,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textStyleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProfessionalItemCard(
    professional: Professional,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("professional_card_${professional.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1715)), // Beautiful dark emerald-charcoal container background
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1B2E28)) // High-end thin custom dark-emerald border
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials Square Avatar with rounded corners (12.dp) and thin elegant border
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = getAvatarBgColor(professional.name))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(professional.name),
                    color = getAvatarTextColor(professional.name),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + Verified Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = professional.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (professional.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verificado",
                            tint = Color(0xFF2ECC71), // Mint verification check
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Specialty Row with custom colored tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val catIcon = getCategoryIcon(professional.category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = professional.category,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Years of Experience Badge
                    if (professional.yearsOfExperience > 0) {
                        Text(
                            text = "•  ${professional.yearsOfExperience} anos exp",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Specialization Subtitle or short description snippet
                val subtitleText = if (professional.specialization.isNotEmpty()) {
                    professional.specialization
                } else {
                    professional.description
                }
                if (subtitleText.isNotEmpty()) {
                    Text(
                        text = subtitleText,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                        fontStyle = if (professional.specialization.isEmpty()) FontStyle.Italic else FontStyle.Normal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meta metadata row: ratings & distance compactly styled
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Rating score and reviews count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFA000), // Amber Gold
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", professional.rating),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFFA000)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${professional.reviewCount})",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }

                    // Distance
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = "Distância",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val distVal = extractDistanceValue(professional.location)
                        Text(
                            text = "$distVal km",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pricing & availability on the right side
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(
                    text = "${String.format("%.0f", professional.ratePerHour)} MT",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFF2ECC71) // Nice vivid solid green highlighting hourly rate
                )
                Text(
                    text = "por hora",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Active status tag (deep emerald green for today, amber brown for tomorrow)
                val isToday = professional.availabilityStatus.lowercase().contains("hoje")
                val isTomorrow = professional.availabilityStatus.lowercase().contains("amanhã")
                
                val badgeBgColor = if (isToday) Color(0xFF0D281E) else Color(0xFF251A0F)
                val badgeTextColor = if (isToday) Color(0xFF2ECC71) else Color(0xFFFFA000)
                val badgeText = if (isToday) "Hoje" else if (isTomorrow) "Amanhã" else professional.availabilityStatus

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(badgeBgColor, RoundedCornerShape(8.dp))
                        .border(0.5.dp, badgeTextColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(badgeTextColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
        }
    }
}


// ==========================================
// 2. PROFESSIONAL DETAIL (AVALIAÇÕES & AGENDAR)
// =================// Reusable helper to get specialty tags based on professional categories
fun getSpecialtyTagsForCategory(category: String): List<String> {
    return when (category) {
        "Chaveiro / Serralheiro", "Chaveiro", "Serralheiro" -> listOf("Fechaduras", "Cópias de chaves", "Cofres", "Portões", "Automóveis", "Urgências")
        "Eletricista" -> listOf("Instalações", "Quadros Elétricos", "Manutenção", "Iluminação", "Curto-Circuitos", "Urgências")
        "Canalizador" -> listOf("Fugas de Água", "Desentupimentos", "Torneiras", "Canalização", "Sanitários", "Piquetes")
        "Diarista / Limpeza", "Limpeza" -> listOf("Limpeza Geral", "Passar a Ferro", "Vidros", "Limpeza Pós-Obra", "Escritórios")
        "Pintor" -> listOf("Pintura Interior", "Pintura Exterior", "Massa Corrida", "Verniz", "Texturas")
        "Jardineiro" -> listOf("Corte de Relva", "Poda", "Plantação", "Manutenção de Jardim", "Adubação")
        "Técnico de TI" -> listOf("Formatação", "Redes", "Remoção de Vírus", "Reparação PC", "Configuração Wi-Fi")
        "Carpinteiro" -> listOf("Móveis por Medida", "Reparação", "Portas", "Montagem de Armários", "Restauro")
        "Motorista" -> listOf("Viagens Curtas", "Entregas", "Aeroporto", "Serviço Privado", "Carrinha")
        "Explicador" -> listOf("Matemática", "Física", "Química", "Inglês", "Preparação de Exames")
        else -> listOf("Serviço Rápido", "Profissional", "Experiência", "Garantia", "Pontual")
    }
}

// Reusable helper to get 3 beautiful high-quality reviews tailored for candidate
fun getSimulatedReviewsForCategory(category: String, profName: String): List<Review> {
    val firstName = profName.split(" ").firstOrNull() ?: "Ronaldo"
    return when (category) {
        "Chaveiro / Serralheiro", "Chaveiro", "Serralheiro" -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Juliana Sitoe", rating = 5, comment = "Trocou a fechadura da minha porta de madrugada super rápido. Preço de emergência justo.", dateStr = "15/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Carlos Tembe", rating = 5, comment = "Copiei 3 chaves e ficou tudo perfeito. Rápido e profissional.", dateStr = "08/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Lurdes Nhamule", rating = 5, comment = "Serviço de alta segurança realizado com total mestria no portão. Profissional super recomendado!", dateStr = "01/06/2025")
        )
        "Eletricista" -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Rui Langa", rating = 5, comment = "Excelente eletricista. Identificou um curto-circuito sério no meu quadro elétrico e resolveu com muita classe e segurança.", dateStr = "14/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Sara Tembe", rating = 5, comment = "Instalou todos os novos candeeiros da minha casa com muito capricho e rapidez. Super recomendável!", dateStr = "08/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Filipe Cossa", rating = 5, comment = "Trabalho muito limpo e preço justo de mercado. Respondeu super rápido ao meu chamado de emergência residencial.", dateStr = "25/05/2025")
        )
        "Canalizador" -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Gisela Sitoe", rating = 5, comment = "Resolveu uma fuga de água na cozinha que já durava dias. Muito limpo, rápido e profissional.", dateStr = "13/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Daniel Macuácua", rating = 5, comment = "Substituiu as torneiras da casa de banho com maestria. $firstName é super educado e profissional.", dateStr = "06/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Nelson Mubai", rating = 5, comment = "Fez o desentupimento da pia principal de forma rápida e segura. Recomendo imenso o trabalho.", dateStr = "21/05/2025")
        )
        "Diarista / Limpeza", "Limpeza" -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Helena Mondlane", rating = 5, comment = "Passou a ferro e arrumou toda a casa de forma excelente. Uma atenção aos detalhes magnífica.", dateStr = "15/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Inácio Maposse", rating = 5, comment = "Contratei para limpeza pós-obra e o resultado foi fantástico. Casa a brilhar, sem qualquer vestígio de pó.", dateStr = "09/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Amélia Chauque", rating = 5, comment = "Super pontual, de confiança e muito simpática. Deixou tudo incrivelmente limpo. Sempre recomendável.", dateStr = "29/05/2025")
        )
        "Pintor" -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Telma Bila", rating = 5, comment = "Pintou o meu apartamento inteiro e a fachada com acabamento de excelência. Cuidado absoluto em proteger os móveis.", dateStr = "12/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Zacarias Manjate", rating = 5, comment = "Aplicou textura numa parede de destaque de forma perfeita. Ficou exatamente como eu queria. Recomendo muito.", dateStr = "05/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Leonor Cuamba", rating = 5, comment = "Profissional extremamente caprichoso, pontual e muito focado em entregar qualidade.", dateStr = "18/05/2025")
        )
        "Explicador" -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Sofia Matsinhe", rating = 5, comment = "As explicações de Matemática ajudaram muito o meu filho. Ele recuperou as notas e agora compreende os conceitos com facilidade.", dateStr = "11/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Eduardo Tembe", rating = 5, comment = "Método de ensino muito simples e prático. $firstName tem imensa paciência e competência.", dateStr = "04/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Luísa Mandlate", rating = 5, comment = "Excelente explicador. Recomendo vivamente para quem pretende passar no exame com notas altas.", dateStr = "22/05/2025")
        )
        else -> listOf(
            Review(id = 101, professionalId = 0, authorName = "Geraldo Algy", rating = 5, comment = "Prestou um excelente serviço de qualidade. Muito pontual, educado e focado em resolver o problema com perfeição.", dateStr = "15/06/2025"),
            Review(id = 102, professionalId = 0, authorName = "Carla Nhaca", rating = 5, comment = "Trabalho impecável and atendimento fantástico. Recomendo fortemente a quem procura um profissional de total confiança.", dateStr = "07/06/2025"),
            Review(id = 103, professionalId = 0, authorName = "Mateus Sitoe", rating = 5, comment = "Serviço super rápido e limpo. Preço transparente e honesto. Voltarei a agendar sem dúvidas.", dateStr = "26/05/2025")
        )
    }
}

fun getBannerAttrsForIndex(index: Int): Triple<Color, ImageVector, Color> {
    val idx = index % 3
    return when (idx) {
        0 -> Triple(Color(0xFF112D21), Icons.Default.Lock, Color(0xFF4DD99B))
        1 -> Triple(Color(0xFF15293A), Icons.Default.Security, Color(0xFF5AB6E0))
        2 -> Triple(Color(0xFF2E193C), Icons.Default.VpnKey, Color(0xFFC78FE0))
        else -> Triple(Color(0xFF1E1E1E), Icons.Default.Image, Color(0xFF8FA79C))
    }
}

fun getPortfolioDrawableId(category: String, index: Int, title: String): Int {
    val titleLower = title.lowercase()
    if (category.contains("Chaveiro") || category.contains("Serralheiro") || titleLower.contains("fechadura") || titleLower.contains("cofre") || titleLower.contains("chave")) {
        return when {
            titleLower.contains("fechadura") || titleLower.contains("samsung") -> R.drawable.img_fechadura_samsung_1781818628458
            titleLower.contains("cofre") -> R.drawable.img_cofre_digital_1781818644782
            titleLower.contains("chave") || titleLower.contains("mestra") || titleLower.contains("condomínio") -> R.drawable.img_chave_mestra_1781818659780
            else -> {
                when (index % 3) {
                    0 -> R.drawable.img_fechadura_samsung_1781818628458
                    1 -> R.drawable.img_cofre_digital_1781818644782
                    else -> R.drawable.img_chave_mestra_1781818659780
                }
            }
        }
    }
    return when (index % 3) {
        0 -> R.drawable.img_onboarding_1_1781797432255
        1 -> R.drawable.img_onboarding_2_1781797446056
        else -> R.drawable.img_onboarding_3_1781797459849
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProfessionalDetailScreen(
    professional: Professional,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onBookClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val databaseReviews by viewModel.activeReviews.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Sobre, 1 = Portfólio, 2 = Avaliações
    var isReplyingToReview by remember { mutableStateOf(false) }

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    val collapseProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 1) {
                1f
            } else if (scrollState.firstVisibleItemIndex == 1) {
                val offset = scrollState.firstVisibleItemScrollOffset.toFloat()
                (offset / 250f).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val showPinnedTabs by remember {
        derivedStateOf {
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                false
            } else {
                val firstItem = visibleItems.firstOrNull()
                val topBarHeightPx = with(density) { 56.dp.toPx() }
                
                // Index 5 is the Tab Row inside LazyColumn
                val tabsItem = visibleItems.find { it.index == 5 }
                if (tabsItem != null) {
                    tabsItem.offset <= topBarHeightPx
                } else {
                    val firstIndex = firstItem?.index ?: 0
                    firstIndex > 5
                }
            }
        }
    }

    // Dynamic state to input reviews
    var textReviewAuthor by remember { mutableStateOf("") }
    var reviewRatingSelected by remember { mutableStateOf(5) }
    var textReviewComment by remember { mutableStateOf("") }

    // Fetch premium simulated real reviews tailored to candidate's trade
    val premiumReviews = remember(professional.id, professional.category) {
        getSimulatedReviewsForCategory(professional.category, professional.name)
    }

    // Combine database reviews and our premium verified ones
    val allReviews = remember(databaseReviews, premiumReviews) {
        databaseReviews + premiumReviews
    }

    Scaffold(
        containerColor = Color(0xFF131313),
        bottomBar = {
            // Fixed Bottom Bar with Price and "Agendar serviço" CTA Button
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${professional.ratePerHour.toInt()} MT",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "/ hora",
                            color = Color(0xFF8FA79C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { onBookClick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4DD99B),
                            contentColor = Color(0xFF032B1C)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("fixed_book_button")
                    ) {
                        Text(
                            text = "Agendar serviço",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616))
                .padding(paddingValues)
        ) {
            // Background decorative green gradient banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF112D21),
                                Color(0xFF161616)
                            )
                        )
                    )
            )

            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("professional_detail_scroll")
            ) {
                // Spacer item representing top app bar height so content is not clipped
                item {
                    Spacer(modifier = Modifier.height(56.dp))
                }

                // Centered Profile Avatar, Name and Status Dot
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, Color(0xFF4DD99B), RoundedCornerShape(26.dp))
                                    .background(Color(0xFF142E23), RoundedCornerShape(26.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getInitials(professional.name),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                )
                            }
                            // Green online status indicator dot at bottom right
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0xFF161616), CircleShape)
                                    .padding(3.dp)
                                    .background(Color(0xFF4DD99B), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = professional.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                tint = Color(0xFF4DD99B),
                                contentDescription = "Verificado",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val cleanLocation = professional.location.replace(" (A 1.5 km)", "").split(",").firstOrNull() ?: professional.location
                        Text(
                            text = "${professional.category} • $cleanLocation",
                            color = Color(0xFF8FA79C),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Subheader Stats Row (Rating | ReviewsCount | Services | Exp)
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val stats = listOf(
                                Triple(String.format("%.1f", professional.rating), "Avaliação", Color(0xFF4DD99B)),
                                Triple("${allReviews.size}", "Avaliações", Color.White),
                                Triple("${100 + professional.id * 7}", "Serviços", Color.White),
                                Triple("${professional.yearsOfExperience}a", "Experiência", Color(0xFF4DD99B))
                            )
                            stats.forEachIndexed { idx, triple ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = triple.first,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = triple.third
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = triple.second,
                                        fontSize = 11.sp,
                                        color = Color(0xFF8FA79C)
                                    )
                                }
                                if (idx < stats.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(28.dp)
                                            .background(Color(0xFF2E2E2E))
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Primary Buttons (Chamada + SMS + Chat)
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    val context = LocalContext.current
                    val rawPhoneNumber = remember(professional.id) {
                        // Generate a realistic local Mozambican number (+258 84/85) based on ID for authenticity
                        val digitSuffix = String.format("%07d", (professional.id * 17 + 123456) % 10000000)
                        "+25884$digitSuffix"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:$rawPhoneNumber")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4DD99B),
                                contentColor = Color(0xFF032B1C)
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("detail_call_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Chamada",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chamada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("smsto:$rawPhoneNumber")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B2F24),
                                contentColor = Color(0xFF4DD99B)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF264C38)),
                            modifier = Modifier
                                .weight(1.0f)
                                .height(48.dp)
                                .testTag("detail_sms_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "SMS",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4DD99B)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { onChatClick() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .testTag("detail_chat_now_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Chat",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                // Three-tab navigation layout row (Sobre | Portfólio | Avaliações)
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161616))
                            .border(width = 1.dp, color = Color(0xFF2E2E2E), shape = RoundedCornerShape(0.dp))
                            .padding(top = 12.dp)
                    ) {
                        val tabLabels = listOf("Sobre", "Portfólio", "Avaliações")
                        tabLabels.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color(0xFF4DD99B) else Color(0xFF8FA79C),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(if (isSelected) Color(0xFF4DD99B) else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                // Screen components depending on selectedTab state
                when (selectedTab) {
                    0 -> { // TAB 0: SOBRE
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Real Description Bio
                                Text(
                                    text = professional.description,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // List attributes with beautiful icons matching exact layout
                                val attributes = listOf(
                                    Pair(Icons.Default.Place, "Bairro do Triunfo, Maputo • 1.5 km de si"),
                                    Pair(Icons.Default.Schedule, "Disponível hoje • 08:00 - 18:00"),
                                    Pair(Icons.Default.Phone, "Responde em menos de 30 min"),
                                    Pair(Icons.Default.Security, "Identidade verificada • Março 2024")
                                )

                                attributes.forEach { itemAttr ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = itemAttr.first,
                                            contentDescription = null,
                                            tint = Color(0xFF4DD99B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = itemAttr.second,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Dynamic Specialty pills formatted in a clean 3-column chunk grid
                                val specialties = getSpecialtyTagsForCategory(professional.category)
                                val chunkedRows = specialties.chunked(3)

                                chunkedRows.forEach { rowTags ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowTags.forEach { childTag ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF132A20), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0xFF1B3D2F), RoundedCornerShape(8.dp))
                                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = childTag,
                                                    color = Color(0xFF4DD99B),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        // Empty cell fillings
                                        if (rowTags.size < 3) {
                                            repeat(3 - rowTags.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // Secção de Certificados & Credenciais
                                if (professional.certificatesList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Text(
                                        text = "CERTIFICADOS & CREDENCIAIS",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8FA79C),
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    professional.certificatesList.forEach { cert ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13231B)),
                                            border = BorderStroke(1.dp, Color(0xFF1F3D2F)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.WorkspacePremium,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4DD99B),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text(
                                                    text = cert.replace("Eletrónicas", "Electrónicas").replace("Ativo", "Activo"),
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 20.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // TAB 1: PORTFÓLIO
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                val pastWorks = professional.portfolioImagesList
                                if (pastWorks.isEmpty()) {
                                    // Empty state visually handled beautifully
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Amostras de portfólio a carregar...",
                                            color = Color(0xFF8FA79C),
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    pastWorks.forEachIndexed { i, title ->
                                        val imageResId = getPortfolioDrawableId(professional.category, i, title)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                            border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Column {
                                                // Beautiful service image representation
                                                Image(
                                                    painter = painterResource(id = imageResId),
                                                    contentDescription = title,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp),
                                                    contentScale = ContentScale.Crop
                                                )

                                                // Card texts with titles and descriptions
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        text = title.replace("Criação de Sistema de ", ""),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "Exemplo real de trabalho feito com materiais certificados e acabamento de alta qualidade.",
                                                        color = Color(0xFF8FA79C),
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Links Profissionais Section at bottom
                                if (professional.portfolioLinksList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Text(
                                        text = "LINKS PROFISSIONAIS",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8FA79C),
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    val context = LocalContext.current
                                    professional.portfolioLinksList.forEach { rawLink ->
                                        val isInstagram = rawLink.contains("instagram.com") || rawLink.startsWith("@")
                                        val linkIcon = if (isInstagram) Icons.Default.Link else Icons.Default.Language
                                        val linkTitle = if (isInstagram) "Instagram" else "Website Profissional"
                                        val linkDisplay = if (isInstagram) {
                                            rawLink.substringAfter("instagram.com/").removeSuffix("/").let { "@$it" }
                                        } else {
                                            rawLink.replace("https://", "").replace("http://", "").removeSuffix("/")
                                        }

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                            border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    try {
                                                        val finalUrl = if (!rawLink.startsWith("http://") && !rawLink.startsWith("https://")) {
                                                            "https://$rawLink"
                                                        } else {
                                                            rawLink
                                                        }
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // Ignore safe fallback
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = linkIcon,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4DD99B),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = linkTitle,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = linkDisplay,
                                                            color = Color(0xFF8FA79C),
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.OpenInNew,
                                                    contentDescription = "Abrir",
                                                    tint = Color(0xFF8FA79C),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // TAB 2: AVALIAÇÕES
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Star Distribution Summary section
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left rating avg score column
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = String.format("%.1f", professional.rating),
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                (1..5).forEach { _ ->
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = null,
                                                        tint = AccentGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${allReviews.size} avaliações",
                                                fontSize = 11.sp,
                                                color = Color(0xFF8FA79C),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(60.dp)
                                                .background(Color(0xFF2E2E2E))
                                                .padding(horizontal = 8.dp)
                                        )

                                        // Right progress bar items
                                        Column(
                                            modifier = Modifier.weight(1.5f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val barsList = listOf(
                                                Pair(5, 0.90f),
                                                Pair(4, 0.08f),
                                                Pair(3, 0.02f),
                                                Pair(2, 0.00f),
                                                Pair(1, 0.00f)
                                            )
                                            barsList.forEach { bar ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "${bar.first}",
                                                        color = Color(0xFF8FA79C),
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.width(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(6.dp)
                                                            .background(Color(0xFF2E2E2E), CircleShape)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth(bar.second)
                                                                .height(6.dp)
                                                                .background(Color(0xFF4DD99B), CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Write high-quality review toggle header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Comentários no Muteko",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )

                                    TextButton(
                                        onClick = { isReplyingToReview = !isReplyingToReview },
                                        modifier = Modifier.testTag("write_review_toggle")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isReplyingToReview) Icons.Default.Close else Icons.Default.RateReview,
                                                tint = Color(0xFF4DD99B),
                                                modifier = Modifier.size(16.dp),
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isReplyingToReview) "Fechar" else "Avaliar",
                                                color = Color(0xFF4DD99B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Interactive collapsible New Review Input Form
                                if (isReplyingToReview) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Deixar Avaliação",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            OutlinedTextField(
                                                value = textReviewAuthor,
                                                onValueChange = { textReviewAuthor = it },
                                                label = { Text("Seu Nome") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("review_author_input"),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4DD99B),
                                                    unfocusedBorderColor = Color(0xFF2E2E2E),
                                                    focusedLabelColor = Color(0xFF4DD99B),
                                                    unfocusedLabelColor = Color(0xFF8FA79C),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Sua Nota:",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8FA79C)
                                            )
                                            InteractiveStarRating(
                                                rating = reviewRatingSelected,
                                                onRatingChanged = { reviewRatingSelected = it }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = textReviewComment,
                                                onValueChange = { textReviewComment = it },
                                                label = { Text("Seu Comentário") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("review_comment_input"),
                                                shape = RoundedCornerShape(8.dp),
                                                minLines = 2,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4DD99B),
                                                    unfocusedBorderColor = Color(0xFF2E2E2E),
                                                    focusedLabelColor = Color(0xFF4DD99B),
                                                    unfocusedLabelColor = Color(0xFF8FA79C),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    if (textReviewAuthor.isNotBlank() && textReviewComment.isNotBlank()) {
                                                        viewModel.addReview(
                                                            profId = professional.id,
                                                            author = textReviewAuthor,
                                                            rating = reviewRatingSelected,
                                                            comment = textReviewComment
                                                        )
                                                        textReviewAuthor = ""
                                                        textReviewComment = ""
                                                        isReplyingToReview = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD99B), contentColor = Color(0xFF032B1C)),
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .testTag("submit_review_button")
                                            ) {
                                                Text("Publicar Avaliação", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Feed of combined reviews (simulated premium + database reviews)
                        items(allReviews) { rev ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    border = BorderStroke(0.5.dp, Color(0xFF2E2E2E)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(getAvatarBgColor(rev.authorName), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = getInitials(rev.authorName),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = rev.authorName,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = rev.dateStr,
                                                fontSize = 12.sp,
                                                color = Color(0xFF8FA79C)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                (1..5).forEach { star ->
                                                    Icon(
                                                        imageVector = Icons.Filled.Star,
                                                        contentDescription = null,
                                                        tint = if (star <= rev.rating) AccentGold else Color.Gray,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            if (rev.isVerifiedPurchase) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF133125), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Compra verificada",
                                                            tint = Color(0xFF4DD99B),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "Contratação Verificada",
                                                            color = Color(0xFF4DD99B),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = rev.comment,
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Collapsing Custom Top App Bar overlay and Pinned tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF161616).copy(alpha = collapseProgress))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Back Button on left and Share Button on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val btnBgAlpha = 0.4f * (1f - collapseProgress)
                        val btnBgColor = Color.Black.copy(alpha = btnBgAlpha)
                        
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(btnBgColor, CircleShape)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(btnBgColor, CircleShape)
                                .clickable { /* Partilhar flow */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partilhar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Collapsing Center content (fades in as user scrolls)
                    Row(
                        modifier = Modifier.graphicsLayer { alpha = collapseProgress },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Small circular avatar representation
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF142E23), CircleShape)
                                .border(1.dp, Color(0xFF4DD99B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(professional.name),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = professional.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            tint = Color(0xFF4DD99B),
                            contentDescription = "Verificado",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Smoothly animated Pinned Tabs row that sticks below the 56.dp app bar
                AnimatedVisibility(
                    visible = showPinnedTabs,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161616))
                            .border(width = 1.dp, color = Color(0xFF2E2E2E), shape = RoundedCornerShape(0.dp))
                            .padding(top = 12.dp)
                    ) {
                        val tabLabels = listOf("Sobre", "Portfólio", "Avaliações")
                        tabLabels.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color(0xFF4DD99B) else Color(0xFF8FA79C),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(if (isSelected) Color(0xFF4DD99B) else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewRowCard(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = review.authorName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = review.dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (star <= review.rating) AccentGold else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (review.isVerifiedPurchase) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(MarketTealLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Compra verificada",
                                tint = MarketTeal,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Contratação Verificada",
                                color = MarketTeal,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = review.comment,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// ==========================================
// 3. SCHEDULING SCREEN (AGENDAMENTO INTEGRADO)
// ==========================================

@Composable
fun SchedulingScreen(
    professional: Professional,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onBookingCreated: (Long) -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    
    var currentStep by remember { mutableStateOf(1) } // 1, 2, 3
    var isRecordingBooking by remember { mutableStateOf(false) }
    var isSuccessCompleted by remember { mutableStateOf(false) }
    var bookingCreatedSuccessId by remember { mutableStateOf<Long?>(null) }

    // Step 1: Data & Hora selection states (Prefilled with 18th of June and 11:00 as per mockup)
    var selectedDay by remember { mutableStateOf(18) }
    var selectedTime by remember { mutableStateOf("11:00") }

    // Step 2: Servico Detalhes (Prefilled with high quality details as per mockup)
    var serviceDescription by remember { mutableStateOf("Precisava de duplicar a chave da porta principal e verificar a fechadura do portão da garagem.") }
    var serviceLocation by remember { mutableStateOf("Av. Kim Il Sung, 142 — Sommerschield") }
    var estimatedDurationHours by remember { mutableStateOf(1) } // Default 1 hour as per mockup

    // Step 3: Confirmar & Pagar states (Choice between M-Pesa, e-Mola or Dinheiro)
    var selectedPaymentMethod by remember { mutableStateOf("M-Pesa") }

    // Computations
    val rate = professional.ratePerHour
    val servicePrice = rate * estimatedDurationHours
    val platformFee = 35 // Muteko platform fee
    val totalAmountToPay = servicePrice + platformFee

    if (isSuccessCompleted) {
        // Confirmation Success Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success check circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFF133125), CircleShape)
                    .border(2.dp, Color(0xFF4DD99B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    tint = Color(0xFF4DD99B),
                    modifier = Modifier.size(46.dp),
                    contentDescription = null
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "Agendado com sucesso!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "O ${professional.name} foi notificado e confirmará o pedido em menos de 30 minutos.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Summary Details card matching Mockup 4
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(18.dp),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$selectedDay de Junho · $selectedTime",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(18.dp),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = serviceLocation,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(18.dp),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$selectedPaymentMethod · ${totalAmountToPay.toInt()} MT",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(18.dp),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Confirmação em até 30 min",
                            color = Color(0xFF4DD99B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = {
                    onBookingCreated(bookingCreatedSuccessId ?: 0L)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4DD99B),
                    contentColor = Color(0xFF032B1C)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("voltar_inicio_success_btn")
            ) {
                Text(
                    text = "Voltar ao início",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    } else if (isRecordingBooking) {
        // Safe payment execution and creation animation
        var paymentStageState by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            paymentStageState = 0
            delay(1000)
            paymentStageState = 1
            delay(1000)
            paymentStageState = 2
            delay(800)
            
            viewModel.createBooking(
                profId = professional.id,
                date = "2026-06-$selectedDay",
                time = selectedTime,
                hours = estimatedDurationHours,
                address = serviceLocation,
                notes = serviceDescription,
                onSuccess = { bookingId ->
                    // Submit simulation transaction
                    viewModel.processPayment(
                        bookingId = bookingId,
                        cardLast4 = if (selectedPaymentMethod == "Dinheiro") "Cash" else selectedPaymentMethod,
                        onComplete = {
                            bookingCreatedSuccessId = bookingId
                            isRecordingBooking = false
                            isSuccessCompleted = true
                        }
                    )
                }
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    tint = Color(0xFF4DD99B),
                    modifier = Modifier.size(72.dp),
                    contentDescription = "Pagamento SSL"
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Processando Pagamento...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                val currentStageMessage = when (paymentStageState) {
                    0 -> "Conectando ao canal seguro do $selectedPaymentMethod..."
                    1 -> "Aguardando aprovação via Push USSD..."
                    else -> "Transação concluída com sucesso! 🎉"
                }
                Text(
                    text = currentStageMessage,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(color = Color(0xFF4DD99B), modifier = Modifier.size(36.dp))
            }
        }
    } else {
        // Multi-Step Interactive Layout (Steps 1, 2, 3)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616))
        ) {
            // Dynamic Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onBack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
                Text(
                    text = when (currentStep) {
                        1 -> "Agendar serviço"
                        2 -> "Detalhes do serviço"
                        else -> "Confirmar e pagar"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFF133125), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$currentStep/3",
                        color = Color(0xFF4DD99B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // High Fidelity Step Progress Bar (Updates dynamically as requested)
            BookingStepIndicator(currentStep = currentStep)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep == 1) {
                    // STEP 1: Date & Time Selecting
                    Text(
                        text = "SELECCIONAR DATA",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Calendar header with controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Previous Month placeholder */ }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Mês anterior",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Junho 2026",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        IconButton(onClick = { /* Next Month placeholder */ }) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Próximo mês",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Mathematically accurate June 2026 calendar view (interactive selection)
                    CalendarGridSection(
                        selectedDay = selectedDay,
                        onDayClick = { selectedDay = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HORÁRIO DISPONÍVEL",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    // Interactive Time slots picker
                    TimeSlotsGridSection(
                        selectedTime = selectedTime,
                        onTimeClick = { selectedTime = it }
                    )
                    
                    Spacer(modifier = Modifier.height(30.dp))
                } else if (currentStep == 2) {
                    // STEP 2: Service Description & Duration Estimated
                    Text(
                        text = "DESCREVER O SERVIÇO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = serviceDescription,
                        onValueChange = { serviceDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_desc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedBorderColor = Color(0xFF4DD99B),
                            unfocusedBorderColor = Color(0xFF2E2E2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4
                    )

                    Text(
                        text = "LOCALIZAÇÃO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = serviceLocation,
                        onValueChange = { serviceLocation = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_location_input"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Localização",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedBorderColor = Color(0xFF4DD99B),
                            unfocusedBorderColor = Color(0xFF2E2E2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text(
                        text = "DURAÇÃO ESTIMADA",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    // Est duration toggles: 1h, 2h, 3h
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val durationOptionsList = listOf(1, 2, 3)
                        durationOptionsList.forEach { hrs ->
                            val isSel = estimatedDurationHours == hrs
                            val btnBg = if (isSel) Color(0xFF133125) else Color(0xFF1E1E1E)
                            val btnBorder = if (isSel) Color(0xFF4DD99B) else Color(0xFF2E2E2E)
                            val btnText = if (isSel) Color(0xFF4DD99B) else Color(0xFF8FA79C)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(btnBg)
                                    .border(1.dp, btnBorder, RoundedCornerShape(10.dp))
                                    .clickable { estimatedDurationHours = hrs }
                                    .testTag("duration_$hrs"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$hrs " + if (hrs == 1) "hora" else "horas",
                                    color = btnText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "FOTOS DO LOCAL (OPCIONAL)",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    // Grid slots matching Screenshot 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Slot 1 (Dashed / Add)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                .border(
                                    border = BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { /* Mock choose photo */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar Foto",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Slot 2 (Lock/Security icon)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF133125), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF1E583F), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Segurança",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Slot 3 (Home icon)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Instalação principal",
                                tint = Color(0xFF4DD99B).copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(30.dp))
                } else {
                    // STEP 3: Confirm & Pay choose
                    Text(
                        text = "RESUMO DO PEDIDO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    // Bill Summary panel with platform fee included
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    tint = Color(0xFF4DD99B),
                                    modifier = Modifier.size(18.dp),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detalhes do serviço", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                            
                            Divider(color = Color(0xFF2E2E2E), thickness = 1.dp)
                            
                            SummaryRow(label = "Profissional", value = professional.name)
                            SummaryRow(label = "Data & Hora", value = "$selectedDay Jun · $selectedTime")
                            SummaryRow(label = "Duração", value = if (estimatedDurationHours == 1) "1 hora" else "$estimatedDurationHours horas")
                            SummaryRow(label = "Local", value = serviceLocation.substringBefore("—").trim())
                            SummaryRow(
                                label = "Serviço (${estimatedDurationHours}h × ${rate.toInt()} MT)",
                                value = "${servicePrice.toInt()} MT"
                            )
                            SummaryRow(label = "Taxa da plataforma", value = "$platformFee MT")
                            
                            Divider(color = Color(0xFF2E2E2E), thickness = 1.dp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total a pagar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("${totalAmountToPay.toInt()} MT", fontWeight = FontWeight.ExtraBold, color = Color(0xFF4DD99B), fontSize = 18.sp)
                            }
                        }
                    }

                    Text(
                        text = "MÉTODO DE PAGAMENTO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    // Payments selector grid matching Screenshot 3 limits (M-Pesa, e-Mola or Dinheiro)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val paymentMethodsList = listOf(
                            Triple("M-Pesa", Icons.Default.PhoneAndroid, Color(0xFFD32F2F)),
                            Triple("e-Mola", Icons.Default.PhoneAndroid, Color(0xFFFFA000)),
                            Triple("Dinheiro", Icons.Default.CreditCard, Color(0xFF388E3C))
                        )
                        
                        paymentMethodsList.forEach { (name, icon, accentColorVisual) ->
                            val isSel = selectedPaymentMethod == name
                            val itemBg = if (isSel) Color(0xFF133125) else Color(0xFF1E1E1E)
                            val itemBorder = if (isSel) Color(0xFF4DD99B) else Color(0xFF2E2E2E)
                            val itemTextColor = if (isSel) Color(0xFF4DD99B) else Color.Gray
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(82.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemBg)
                                    .border(1.dp, itemBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedPaymentMethod = name }
                                    .testTag("pay_method_$name"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    tint = if (isSel) Color(0xFF4DD99B) else accentColorVisual,
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = name
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    color = itemTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // Custom Dynamic Back + Continue Bottom Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Circle Border Back control
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF161616), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(14.dp))
                            .clickable {
                                if (currentStep > 1) {
                                    currentStep--
                                } else {
                                    onBack()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Large visual CTA Button
                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                isRecordingBooking = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4DD99B),
                            contentColor = Color(0xFF032B1C)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag(if (currentStep == 3) "step3_confirm_button" else "step_next_button")
                    ) {
                        Text(
                            text = if (currentStep == 3) "Confirmar pedido" else "Continuar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// Sub-component Calendar
@Composable
fun CalendarGridSection(
    selectedDay: Int,
    onDayClick: (Int) -> Unit
) {
    val daysInJune = 30
    val spacerCellsBeforeMon = 1 // Sunday is blank
    
    val items = remember {
        val list = mutableListOf<Int?>()
        for (i in 0 until spacerCellsBeforeMon) {
            list.add(null)
        }
        for (day in 1..daysInJune) {
            list.add(day)
        }
        list
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Initials Row
        Row(modifier = Modifier.fillMaxWidth()) {
            val initials = listOf("D", "S", "T", "Q", "Q", "S", "S")
            initials.forEach { text ->
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Grid Rows
        val rowsCount = (items.size + 6) / 7
        for (r in 0 until rowsCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val index = r * 7 + c
                    if (index < items.size) {
                        val dayNum = items[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum != null) {
                                val isToday = dayNum == 18
                                val isSelected = selectedDay == dayNum
                                val isPast = dayNum < 18
                                
                                val textDayColor = when {
                                    isSelected -> Color(0xFF4DD99B)
                                    isPast -> Color.DarkGray
                                    isToday -> Color.White
                                    else -> Color.White
                                }
                                
                                val boxModifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF133125)
                                            isToday -> Color(0xFF1E1E1E)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF4DD99B) else if (isToday) Color(0xFF4DD99B).copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isPast) {
                                        onDayClick(dayNum)
                                    }
                                    .testTag("day_cell_$dayNum")
                                    
                                Box(
                                    modifier = boxModifier,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = textDayColor,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Sub-component Time Slots
@Composable
fun TimeSlotsGridSection(
    selectedTime: String,
    onTimeClick: (String) -> Unit
) {
    val slots = listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00")
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (i in slots.indices step 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0..2) {
                    val index = i + j
                    if (index < slots.size) {
                        val slotTime = slots[index]
                        val isSelected = selectedTime == slotTime
                        
                        val boxBgColor = if (isSelected) Color(0xFF133125) else Color(0xFF1E1E1E)
                        val boxBorderColor = if (isSelected) Color(0xFF4DD99B) else Color(0xFF2E2E2E)
                        val boxTextColor = if (isSelected) Color(0xFF4DD99B) else Color.Gray
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(boxBgColor)
                                .border(1.dp, boxBorderColor, RoundedCornerShape(10.dp))
                                .clickable { onTimeClick(slotTime) }
                                .testTag("time_slot_$slotTime"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slotTime,
                                color = boxTextColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Sub-component Step Indicator with green checkmark
@Composable
fun BookingStepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step 1
        StepIndicatorCircle(
            stepNumber = 1,
            isCompleted = currentStep > 1,
            isActive = currentStep == 1
        )
        
        // Line 1-2
        val line1Color = if (currentStep > 1) Color(0xFF4DD99B) else Color(0xFF22362C)
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .background(line1Color)
        )
        
        // Step 2
        StepIndicatorCircle(
            stepNumber = 2,
            isCompleted = currentStep > 2,
            isActive = currentStep == 2
        )
        
        // Line 2-3
        val line2Color = if (currentStep > 2) Color(0xFF4DD99B) else Color(0xFF22362C)
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .background(line2Color)
        )
        
        // Step 3
        StepIndicatorCircle(
            stepNumber = 3,
            isCompleted = false,
            isActive = currentStep == 3
        )
    }
}

@Composable
fun StepIndicatorCircle(stepNumber: Int, isCompleted: Boolean, isActive: Boolean) {
    val size = 30.dp
    if (isCompleted) {
        Box(
            modifier = Modifier
                .size(size)
                .background(Color(0xFF4DD99B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF032B1C),
                modifier = Modifier.size(16.dp)
            )
        }
    } else if (isActive) {
        Box(
            modifier = Modifier
                .size(size)
                .background(Color(0xFF133125), CircleShape)
                .border(2.dp, Color(0xFF4DD99B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = Color(0xFF4DD99B),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(Color(0xFF1A1A1A), CircleShape)
                .border(1.dp, Color(0xFF2E2E2E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = Color(0xFF666666),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp)
    }
}

// ==========================================
// 4. SECURE PAYMENT SIMULATION
// ==========================================

@Composable
fun PaymentScreen(
    bookingId: Long,
    viewModel: MainViewModel,
    onPaymentDone: () -> Unit,
    onCancel: () -> Unit
) {
    val bookings by viewModel.bookings.collectAsState()
    val activeBooking = bookings.find { it.id == bookingId }

    var cardHolderName by remember { mutableStateOf("") }
    var creditCardNumber by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var securityCode by remember { mutableStateOf("") }

    // Mozambique Localization Payment Options (Mobile Money vs Card)
    var paymentMethod by remember { mutableStateOf(1) } // Default 1 = Mobile Money (Highly Preferred in Mozambique)
    var mobileMoneyProvider by remember { mutableStateOf("M-Pesa") } // M-Pesa, e-Mola, m-Kesh
    var mobileMoneyNumber by remember { mutableStateOf("") }

    var isProcessingTransition by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf(0) }

    val focusManager = LocalFocusManager.current

    if (activeBooking == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Agendamento não localizado.")
        }
        return
    }

    if (isProcessingTransition) {
        // Secure Transaction Loading animation view
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    tint = MarketTeal,
                    modifier = Modifier
                        .size(80.dp)
                        .animateContentSize(),
                    contentDescription = "🔒 Conexão Segura SSL"
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Processando Pagamento...",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))

                val stageMessage = if (paymentMethod == 0) {
                    when (processingStage) {
                        0 -> "Iniciando criptografia ponta a ponta SSL de 256 bits..."
                        1 -> "Verificando saldo e limites do emissor..."
                        2 -> "Autorizando débito de ${String.format("%.2f", activeBooking.totalAmount)} MT..."
                        else -> "Finalizando transação com sucesso! 🎉"
                    }
                } else {
                    when (processingStage) {
                        0 -> "Conectando ao gateway do $mobileMoneyProvider..."
                        1 -> "Enviando Push USSD de pagamento para +258 $mobileMoneyNumber..."
                        2 -> "Aguardando aprovação digitando PIN no telemóvel..."
                        else -> "Pagamento recebido via $mobileMoneyProvider com sucesso! 🇲🇿🎉"
                    }
                }
                
                Text(
                    text = stageMessage,
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(color = MarketTeal)

                // Simulate progress stages
                LaunchedEffect(Unit) {
                    processingStage = 0
                    delay(1200)
                    processingStage = 1
                    delay(1200)
                    processingStage = 2
                    delay(1500)
                    processingStage = 3
                    delay(800)
                    
                    viewModel.processPayment(
                        bookingId = bookingId,
                        cardLast4 = if (paymentMethod == 0) {
                            if (creditCardNumber.length >= 4) creditCardNumber.takeLast(4) else "4321"
                        } else {
                            if (mobileMoneyNumber.length >= 4) mobileMoneyNumber.takeLast(4) else "MZ"
                        },
                        onComplete = {
                            onPaymentDone()
                        }
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Toolbar header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                }
                Text(
                    text = "🔒 Pagamento Seguro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Invoice details summary adapted to Meticais (MT)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumo do Pedido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(activeBooking.professionalCategory, color = Color.Gray, fontSize = 13.sp)
                            Text("${String.format("%.2f", activeBooking.totalAmount / activeBooking.hours)} MT /h", fontSize = 13.sp)
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Quantidade estimada", color = Color.Gray, fontSize = 13.sp)
                            Text("${activeBooking.hours} horas", fontSize = 13.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total a pagar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${String.format("%.2f", activeBooking.totalAmount)} MT",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MarketTeal
                            )
                        }
                    }
                }

                // Payment Method Selector Custom Segmented Row
                Text("Selecione o Método de Pagamento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mobile Money Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (paymentMethod == 1) MarketTeal else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { paymentMethod = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (paymentMethod == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Carteiras Móveis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (paymentMethod == 1) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Card Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (paymentMethod == 0) MarketTeal else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { paymentMethod = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = if (paymentMethod == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cartão de Crédito",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (paymentMethod == 0) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (paymentMethod == 1) {
                    // Mobile Money forms
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Pagar com Carteira Móvel (Moçambique)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            // Network Provider selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("M-Pesa", "e-Mola", "m-Kesh").forEach { providerName ->
                                    val isSelected = mobileMoneyProvider == providerName
                                    val brandColor = when (providerName) {
                                        "M-Pesa" -> Color(0xFFE11900) // Red
                                        "e-Mola" -> Color(0xFFFFA500) // Yellow/Orange
                                        else -> Color(0xFF008080) // Greenish m-Kesh
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                2.dp,
                                                if (isSelected) brandColor else Color.LightGray.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (isSelected) brandColor.copy(alpha = 0.08f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { mobileMoneyProvider = providerName }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = providerName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = when (providerName) {
                                                    "M-Pesa" -> "Vodacom"
                                                    "e-Mola" -> "Movitel"
                                                    else -> "mcel-T+"
                                                },
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            // Mobile Number with country prefix
                            OutlinedTextField(
                                value = mobileMoneyNumber,
                                onValueChange = { input ->
                                    if (input.length <= 9) {
                                        mobileMoneyNumber = input.filter { it.isDigit() }
                                    }
                                },
                                label = { Text("Número de Telemóvel ($mobileMoneyProvider)") },
                                placeholder = { Text("Ex: 841234567") },
                                leadingIcon = {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("+258", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.5f)))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_mobile_money_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Security Info Alert Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MarketTeal.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MarketTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    tint = MarketTeal,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Pagamento Moçambicano 100% Protegido",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MarketTeal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Uma mensagem pop-up de autorização USSD será enviada ao seu telemóvel para confirmar com o seu código PIN do $mobileMoneyProvider de forma instantânea.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Credit Card visual overlay component matching card styling guidelines
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(MarketTeal, Color(0xFF047857))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GARANTIA SECURA",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        tint = Color.White,
                                        contentDescription = null
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = if (creditCardNumber.isBlank()) "•••• •••• •••• ••••" else creditCardNumber,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("TITULAR", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text(
                                            text = if (cardHolderName.isBlank()) "SUA CONTA" else cardHolderName.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("VALIDADE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text(
                                            text = if (expirationDate.isBlank()) "MM/AA" else expirationDate,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Inputs for Credit Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Dados do Cartão de Crédito", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            OutlinedTextField(
                                value = cardHolderName,
                                onValueChange = { cardHolderName = it },
                                label = { Text("Nome do Titular (conforme cartão)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_cardholder_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = creditCardNumber,
                                onValueChange = {
                                    if (it.length <= 16) creditCardNumber = it.filter { char -> char.isDigit() }
                                },
                                label = { Text("Número do Cartão") },
                                placeholder = { Text("4532 1100 2341 0021") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_cardnumber_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = expirationDate,
                                    onValueChange = {
                                        if (it.length <= 5) expirationDate = it
                                    },
                                    label = { Text("Validade (MM/AA)") },
                                    placeholder = { Text("08/29") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("payment_expiry_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = securityCode,
                                    onValueChange = {
                                        if (it.length <= 4) securityCode = it.filter { char -> char.isDigit() }
                                    },
                                    label = { Text("CVV") },
                                    placeholder = { Text("123") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("payment_cvv_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Field validations
                val areFieldsValid = if (paymentMethod == 0) {
                    cardHolderName.isNotBlank() && creditCardNumber.length >= 12 && securityCode.length >= 3
                } else {
                    mobileMoneyNumber.length == 9 && mobileMoneyNumber.startsWith("8")
                }

                // Submit pay button
                Button(
                    onClick = {
                        if (areFieldsValid) {
                            focusManager.clearFocus()
                            isProcessingTransition = true
                        }
                    },
                    enabled = areFieldsValid,
                    colors = ButtonDefaults.buttonColors(containerColor = MarketTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_payment_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirmar e Pagar ${String.format("%.2f", activeBooking.totalAmount)} MT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. CHAT SCREEN (CHAT EM TEMPO REAL)
// ==========================================

@Composable
fun ChatScreen(
    professional: Professional,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onRebook: () -> Unit
) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    var typedMessageText by remember { mutableStateOf("") }
    
    // Find booking matching this professional to show context in top card
    val profBooking = remember(bookings, professional) {
        bookings.firstOrNull { it.professionalId == professional.id }
    }

    val context = LocalContext.current
    val appDarkBg = Color(0xFF0C1311)
    val appHeaderBg = Color(0xFF0F1E19)
    val cardBg = Color(0xFF14241F)
    val clientBubbleColor = Color(0xFF1E3D32)
    val profBubbleColor = Color(0xFF1F2C27)
    val textMuted = Color(0xFF8B9E96)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                var displayName = "ficheiro_anexo"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                } catch (e: Exception) {
                    displayName = uri.lastPathSegment ?: "ficheiro_anexo"
                }
                viewModel.sendChatMessage(professional.id, "📎 Anexou o ficheiro: $displayName")
                snackbarHostState.showSnackbar("Ficheiro enviado!")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = appDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat Header Contact Profile Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appHeaderBg)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .testTag("chat_back_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(modifier = Modifier.size(40.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(getAvatarBgColor(professional.name), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getInitials(professional.name),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    // Green dot for online ones
                    val isOnline = professional.name == "Ronaldo Langa" || professional.name == "Albertina Mondlane"
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF2ECC71), CircleShape)
                                .border(1.5.dp, appHeaderBg, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = professional.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        if (professional.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                tint = Color(0xFF2ECC71),
                                contentDescription = "Verificado",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    val isOnline = professional.name == "Ronaldo Langa" || professional.name == "Albertina Mondlane"
                    Text(
                        text = if (isOnline) "Online agora" else "Disponível amanhã",
                        fontSize = 11.sp,
                        color = if (isOnline) Color(0xFF2ECC71) else textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Small Calendar / Booking Action from header
                IconButton(onClick = onRebook) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Agendar",
                        tint = Color.White
                    )
                }
            }

            // Booking card context
            profBooking?.let { booking ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("chat_booking_context_card"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF1D342C))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(getAvatarBgColor(booking.professionalName).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(booking.professionalName),
                                color = getAvatarBgColor(booking.professionalName),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val formattedDate = when (booking.date) {
                                "2026-06-19" -> "19 Jun"
                                else -> booking.date
                            }
                            Text(
                                text = "Agendamento · ${formattedDate}, ${booking.time}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "${booking.professionalCategory} · ${booking.address}",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                            
                            // Reschedule offer for completed services
                            if (booking.status == "Concluído") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.clickable { onRebook() },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reagendar",
                                        tint = Color(0xFF2ECC71),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reagendar serviço",
                                        color = Color(0xFF2ECC71),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val badgeColor = when (booking.status) {
                            "Confirmado" -> Color(0xFF2ECC71)
                            "Pendente" -> Color(0xFFF1C40F)
                            else -> Color(0xFF95A5A6)
                        }
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, badgeColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = booking.status,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            val listState = rememberLazyListState()
            
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg)
                            ) {
                                Text(
                                    text = "Inicie a conversa para tirar dúvidas ou combinar detalhes do serviço de forma segura.",
                                    color = textMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { msg: com.example.data.ChatMessage ->
                        val isClient = msg.sender == "CLIENT"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isClient) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = if (isClient) Alignment.End else Alignment.Start
                            ) {
                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .testTag("chat_bubble_${msg.sender}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isClient) clientBubbleColor else profBubbleColor
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isClient) 16.dp else 2.dp,
                                        bottomEnd = if (isClient) 2.dp else 16.dp
                                    ),
                                    border = BorderStroke(0.5.dp, Color(0xFF1D342C))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.messageText,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                ) {
                                    val formattedTimeNow = getFormattedMessageTime(msg.messageText, msg.timestamp)
                                    Text(
                                        text = formattedTimeNow,
                                        fontSize = 10.sp,
                                        color = textMuted
                                    )
                                    
                                    if (isClient) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = "Mensagem lida",
                                            tint = Color(0xFF2ECC71),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick replies row
            val contextualReplies = remember(professional.id) {
                when (professional.id) {
                    5L -> listOf("👍 Ok, perfeito", "📍 Estou cá", "⏰ Pode atrasar")
                    1L -> listOf("🗓️ Confirmar às 14h", "📍 Combinado", "❓ Qual o valor?")
                    2L -> listOf("🥰 Obrigado!", "🙏 Igualmente", "🌸 Até à próxima")
                    11L -> listOf("🔄 Reagendar", "👍 Tudo resolvido", "💬 Obrigado, José")
                    else -> listOf("👍 Obrigado", "🗓️ Agendar novo", "📍 Estou a caminho")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(appDarkBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contextualReplies.forEach { sug ->
                    Card(
                        modifier = Modifier.clickable {
                            viewModel.sendChatMessage(professional.id, sug)
                        },
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF1D342C))
                    ) {
                        Text(
                            text = sug,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2ECC71),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Typing inputs bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(appHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            filePickerLauncher.launch("*/*")
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Erro ao abrir seletor de ficheiros")
                            }
                        }
                    },
                    modifier = Modifier
                        .background(cardBg, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile, 
                        tint = Color(0xFF2ECC71),
                        contentDescription = "Anexar ficheiro"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = typedMessageText,
                    onValueChange = { typedMessageText = it },
                    placeholder = {
                        Text(
                            text = "Escrever mensagem...",
                            color = Color(0xFF75857F),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF152A22),
                        unfocusedContainerColor = Color(0xFF152A22),
                        focusedBorderColor = Color(0xFF244439),
                        unfocusedBorderColor = Color(0xFF1C342C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (typedMessageText.isNotBlank()) {
                            viewModel.sendChatMessage(professional.id, typedMessageText)
                            typedMessageText = ""
                        }
                    },
                    modifier = Modifier
                        .background(Color(0xFF2ECC71), CircleShape)
                        .size(42.dp)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        tint = Color(0xFF0C1311),
                        contentDescription = "Enviar",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. BOOKINGS SCREEN (MEUS AGENDAMENTOS)
// ==========================================

@Composable
fun BookingsScreen(
    viewModel: MainViewModel,
    onPayNow: (Long) -> Unit,
    onChatWith: (Long) -> Unit
) {
    val bookings by viewModel.bookings.collectAsState()
    var selectedFilter by remember { mutableStateOf("Todos") }
    var selectedDay by remember { mutableStateOf<Int?>(19) } // Default to June 19 to match the mockup!

    val filteredBookings = remember(bookings, selectedFilter, selectedDay) {
        bookings.filter { booking ->
            // Date filter: if selectedDay is set, match the day of month (e.g. 2026-06-DD)
            val matchesDate = if (selectedDay != null) {
                val dayStr = String.format("%02d", selectedDay)
                booking.date.endsWith("-$dayStr") || booking.date.endsWith("/$dayStr")
            } else {
                true
            }
            // Status filter
            val matchesStatus = when (selectedFilter) {
                "Confirmados" -> booking.status == "Confirmado"
                "Pendentes" -> booking.status == "Pendente"
                "Em Andamento" -> booking.status == "Em Andamento"
                "Concluídos" -> booking.status == "Concluído"
                "Todos" -> true
                else -> true
            }
            matchesDate && matchesStatus
        }
    }

    val sectionTitle = when (selectedDay) {
        16 -> "SEGUNDA, 16 DE JUNHO"
        17 -> "TERÇA, 17 DE JUNHO"
        18 -> "QUARTA, 18 DE JUNHO"
        19 -> "HOJE, 19 DE JUNHO"
        20 -> "SEXTA, 20 DE JUNHO"
        21 -> "SÁBADO, 21 DE JUNHO"
        else -> "TODOS OS AGENDAMENTOS"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B09))
    ) {
        // Redesigned bold header with interactive week days
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1E19)) // Premium dark slate forest color
                .padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Agenda",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horizontal week-day calendar slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val daysList = listOf(
                        Triple("SEG", 16, "2026-06-16"),
                        Triple("TER", 17, "2026-06-17"),
                        Triple("QUA", 18, "2026-06-18"),
                        Triple("QUI", 19, "2026-06-19"),
                        Triple("SEX", 20, "2026-06-20"),
                        Triple("SÁB", 21, "2026-06-21")
                    )
                    
                    daysList.forEach { (lbl, dayNum, dateStr) ->
                        val isSelected = selectedDay == dayNum
                        val hasBooking = bookings.any { it.date == dateStr }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFF5AE0A8) else Color.Transparent
                                )
                                .clickable {
                                    selectedDay = if (selectedDay == dayNum) null else dayNum
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFF0C1411) else Color(0xFF4FA081),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 16.sp,
                                    color = if (isSelected) Color(0xFF0C1411) else Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                
                                // Dot indicator
                                if (hasBooking) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                if (isSelected) Color(0xFF0C1411) else Color(0xFF5AE0A8),
                                                CircleShape
                                            )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(9.dp)) // Spacing placeholder
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF060B09))
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("Todos", "Confirmados", "Pendentes", "Concluídos")
            filters.forEach { filterStr ->
                val isSelected = selectedFilter == filterStr
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) Color(0xFF5AE0A8) else Color(0xFF10211B)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF5AE0A8) else Color(0xFF1C3129),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = filterStr }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filterStr,
                        color = if (isSelected) Color(0xFF0C1411) else Color(0xFF8FA79C),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Section date summary text (e.g., "HOJE, 19 DE JUNHO")
        Text(
            text = sectionTitle,
            color = Color(0xFF4FA081),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )

        if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF1C3129)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (bookings.isEmpty()) "Você ainda não contratou nenhum serviço." else "Nenhum agendamento para este filtro.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8FA79C),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toque em outros dias acima ou altere os filtros na barra para ver sua agenda completa.",
                        fontSize = 11.sp,
                        color = Color(0xFF4FA081),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("bookings_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredBookings) { booking ->
                    BookingRowItem(
                        booking = booking,
                        viewModel = viewModel,
                        onPay = { onPayNow(booking.id) },
                        onChat = { onChatWith(booking.professionalId) },
                        onCancel = { viewModel.cancelBooking(booking.id) },
                        onComplete = { viewModel.completeBooking(booking.id) },
                        onRate = { rating, comment -> viewModel.rateBooking(booking.id, rating, comment) }
                    )
                }
            }
        }
    }
}

private fun calculateEndTime(timeStr: String, hours: Int): String {
    return try {
        val parts = timeStr.split(":")
        val h = parts[0].trim().toInt()
        val m = parts.getOrNull(1)?.trim()?.toInt() ?: 0
        val endH = (h + hours) % 24
        String.format("%02d:%02d", endH, m)
    } catch (e: Exception) {
        timeStr
    }
}

@Composable
fun BookingRowItem(
    booking: Booking,
    viewModel: MainViewModel,
    onPay: () -> Unit,
    onChat: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onRate: (Int, String) -> Unit
) {
    val statusBgColor = when (booking.status) {
        "Pendente" -> Color(0xFF2D2316)
        "Confirmado" -> Color(0xFF132A21)
        "Em Andamento" -> Color(0xFF152A2D)
        "Concluído" -> Color(0xFF142D15)
        else -> Color(0xFF222222) // Cancelado
    }
    
    val statusTxtColor = when (booking.status) {
        "Pendente" -> Color(0xFFEAA342)
        "Confirmado" -> Color(0xFF5AE0A8)
        "Em Andamento" -> Color(0xFF42A5F5)
        "Concluído" -> Color(0xFF81C784)
        else -> Color.Gray
    }
    
    val statusSideColor = when (booking.status) {
        "Pendente" -> Color(0xFFEAA342) // Âmbar
        "Confirmado" -> Color(0xFF2ECC71) // Verde
        "Em Andamento" -> Color(0xFF2ECC71) // Verde
        "Concluído" -> Color(0xFF2ECC71) // Verde
        else -> Color(0xFF7F8C8D) // Cinza
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_item_${booking.id}")
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121D19)),
        border = BorderStroke(1.dp, Color(0xFF1C2B24))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left Status Strip Bar (verde/âmbar/cinza)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusSideColor)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top header row: Avatar, details, Verified check, and status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar circle bubble
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF152A21), CircleShape)
                                .border(1.dp, Color(0xFF264C3B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(booking.professionalName),
                                color = Color(0xFF5AE0A8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = booking.professionalName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verificado",
                                    tint = Color(0xFF5AE0A8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = booking.professionalCategory,
                                fontSize = 11.sp,
                                color = Color(0xFF8FA79C)
                            )
                        }
                    }
                    
                    // Top Right Status Capsule
                    Box(
                        modifier = Modifier
                            .background(statusBgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = booking.status,
                            color = statusTxtColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Embedded details panel - redesigned per instructions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1411), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Previsão de data + estimativa de tempo na mesma linha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Previsão",
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val formattedDate = when (booking.date) {
                            "2026-06-16" -> "16 Jun"
                            "2026-06-17" -> "17 Jun"
                            "2026-06-18" -> "18 Jun"
                            "2026-06-19" -> "19 Jun (Hoje)"
                            "2026-06-20" -> "20 Jun"
                            "2026-06-21" -> "21 Jun"
                            else -> booking.date
                        }
                        val endT = calculateEndTime(booking.time, booking.hours)
                        Text(
                            text = "Previsão: $formattedDate, às ${booking.time} • Est: ${booking.hours}h (término às $endT)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Morada completa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Morada",
                            tint = Color(0xFF8FA79C),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Morada: ${booking.address}",
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Notas do serviço
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val specIcon = if (booking.professionalCategory.contains("Eletricista", ignoreCase = true)) {
                            Icons.Default.FlashOn
                        } else if (booking.professionalCategory.contains("Diarista", ignoreCase = true) || booking.professionalCategory.contains("Limpeza", ignoreCase = true)) {
                            Icons.Default.Brush
                        } else {
                            Icons.Default.Build
                        }
                        Icon(
                            imageVector = specIcon,
                            contentDescription = "Notas",
                            tint = Color(0xFF8FA79C),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notas: ${if (booking.notes.isNotEmpty()) booking.notes else "Nenhuma nota adicionada."}",
                            color = Color(0xFF8FA79C),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (booking.status != "Concluído" && booking.status != "Cancelado") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current
                        
                        // Chamada (Phone) button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+258849991234"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1C2B24)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF10211B),
                                contentColor = Color(0xFF5AE0A8)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Chamada",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chamada", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // SMS button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+258849991234"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1C2B24)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF10211B),
                                contentColor = Color(0xFF5AE0A8)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = "SMS",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Chat button (abre conversas)
                        OutlinedButton(
                            onClick = onChat,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1C2B24)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF10211B),
                                contentColor = Color(0xFF4DD99B)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Chat",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Bottom Pricing & action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Valor Total em destaque com cor verde
                    Column {
                        Text(
                            text = "Valor Total",
                            fontSize = 10.sp,
                            color = Color(0xFF8FA79C),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${booking.totalAmount.toInt()} MT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2ECC71)
                        )
                    }
                    
                    // Action Buttons Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val context = LocalContext.current
                        
                        if (booking.status == "Pendente") {
                            // Cancelar button
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFE74C3C).copy(alpha = 0.85f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancelar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Confirmar button
                            Button(
                                onClick = { viewModel.confirmBooking(booking.id) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4DD99B),
                                    contentColor = Color(0xFF0C1411)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Confirmar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (booking.status == "Confirmado") {
                            // Cancelar button (vermelho discreto)
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFE74C3C).copy(alpha = 0.85f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancelar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Concluir button
                            Button(
                                onClick = onComplete,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2ECC71),
                                    contentColor = Color(0xFF0C1411)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).testTag("complete_booking_button_${booking.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Concluir",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (booking.status == "Em Andamento") {
                            // Cancelar button (vermelho discreto)
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFE74C3C).copy(alpha = 0.85f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancelar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Concluir Serviço button
                            Button(
                                onClick = onComplete,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2ECC71),
                                    contentColor = Color(0xFF0C1411)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).testTag("complete_booking_button_${booking.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Concluir",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (booking.status == "Concluído") {
                            if (booking.paymentStatus == "Pendente") {
                                Button(
                                    onClick = onPay,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2ECC71),
                                        contentColor = Color(0xFF0C1411)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pagar Agora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF133125), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            tint = Color(0xFF2ECC71),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pago (${booking.cardLast4})",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2ECC71),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Inline rating form for Concluídos that are not rated!
                if (booking.status == "Concluído" && !booking.isRated) {
                    var localRating by remember { mutableStateOf(5) }
                    var localComment by remember { mutableStateOf("") }
                    var isFormExpanded by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF1C2B24))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isFormExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF133125), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF1E3C2F), RoundedCornerShape(12.dp))
                                .clickable { isFormExpanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    tint = AccentGold,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Classificar Qualidade do Serviço ⭐",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Toque para dar estrelas ao profissional",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8FA79C)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                tint = Color(0xFF8FA79C),
                                contentDescription = "Expandir",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1B16), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF1C3A2E), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("evaluation_form_${booking.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Como foi o atendimento?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                IconButton(
                                    onClick = { isFormExpanded = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dê a sua nota real de 1 a 5 estrelas a ${booking.professionalName}:",
                                fontSize = 11.sp,
                                color = Color(0xFF8FA79C)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            InteractiveStarRating(
                                rating = localRating,
                                onRatingChanged = { localRating = it },
                                starSize = 32.dp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = localComment,
                                onValueChange = { localComment = it },
                                placeholder = {
                                    Text(
                                        text = "Conta mais detalhes sobre o atendimento...",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("booking_rate_comment_${booking.id}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF070E0C),
                                    unfocusedContainerColor = Color(0xFF070E0C),
                                    focusedBorderColor = Color(0xFF5AE0A8),
                                    unfocusedBorderColor = Color(0xFF1C3129)
                                ),
                                maxLines = 3,
                                textStyle = TextStyle(fontSize = 12.sp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    onRate(
                                        localRating,
                                        if (localComment.isBlank()) "Excelente serviço realizado!" else localComment
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4DD99B),
                                    contentColor = Color(0xFF0C1411)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("submit_booking_rate_button_${booking.id}")
                            ) {
                                Text(
                                    text = "Enviar feedback",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else if (booking.status == "Concluído" && booking.isRated) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF1C2B24))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF133125), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            tint = Color(0xFF4DD99B),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Serviço Avaliado! ⭐",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Obrigado! Sua opinião ajuda a manter a comunidade de serviços locais segura.",
                                fontSize = 10.sp,
                                color = Color(0xFF8FA79C)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper Composable modifiers
@Composable
private fun size14() = Modifier.size(14.dp)

// ==========================================
// 6B. ACTIVE CHATS CHANNELS LIST
// ==========================================

@Composable
fun ActiveChatsListScreen(
    viewModel: MainViewModel,
    onOpenChat: (Long) -> Unit
) {
    val professionals by viewModel.professionals.collectAsState()
    val allMessages by viewModel.allChatMessages.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Screen background: Dark Slate/Teal
    val appDarkBg = Color(0xFF0C1311)
    val appHeaderBg = Color(0xFF0F1E19)
    val cardBg = Color(0xFF14241F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appDarkBg)
    ) {
        // Custom Header matches the mockup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(appHeaderBg)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Conversas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Pesquisar mensagens...",
                        color = Color(0xFF75857F),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color(0xFF75857F),
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF152A22),
                    unfocusedContainerColor = Color(0xFF152A22),
                    focusedBorderColor = Color(0xFF244439),
                    unfocusedBorderColor = Color(0xFF1C342C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_search_bar")
            )
        }

        // Logic to construct list of conversations
        val activeConversations = remember(professionals, allMessages, searchQuery) {
            professionals.mapNotNull { prof ->
                val profMsgs = allMessages.filter { it.professionalId == prof.id }
                    .sortedBy { it.timestamp }
                val lastMsg = profMsgs.lastOrNull()
                val unreadCount = profMsgs.count { !it.isRead && it.sender == "PROFESSIONAL" }

                // Determine if this item matches search query
                val matchesSearch = searchQuery.isBlank() || 
                        prof.name.contains(searchQuery, ignoreCase = true) || 
                        profMsgs.any { it.messageText.contains(searchQuery, ignoreCase = true) }

                // If searched is not blank, show any professional who matches name
                // If searched is blank, show professionals of active conversations (those who have messages)
                if (matchesSearch && (lastMsg != null || searchQuery.isNotBlank())) {
                    val formattedTime = if (lastMsg != null) {
                        getFormattedChatTime(prof.id, lastMsg.messageText, lastMsg.timestamp)
                    } else ""
                    
                    val isOnline = prof.name == "Ronaldo Langa" || prof.name == "Albertina Mondlane"

                    val finalLastMsgText = lastMsg?.messageText ?: "Nenhuma mensagem iniciada"

                    ConversationItemState(
                        professional = prof,
                        lastMessage = finalLastMsgText,
                        timeLabel = formattedTime,
                        unreadCount = unreadCount,
                        isOnline = isOnline,
                        timestamp = lastMsg?.timestamp ?: 0L
                    )
                } else null
            }.sortedByDescending { it.timestamp }
        }

        if (activeConversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Sem conversas",
                        tint = Color(0xFF4C5E57),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma conversa encontrada",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Digite o nome de um profissional no campo de busca para iniciar um chat de suporte.",
                        color = Color(0xFF8B9E96),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("chat_channels_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeConversations) { conv ->
                    val prof = conv.professional
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(prof.id) }
                            .testTag("chat_channel_${prof.id}"),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar section with online badge
                            Box(modifier = Modifier.size(48.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(getAvatarBgColor(prof.name), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getInitials(prof.name),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                
                                // Ponto verde de profissional online
                                if (conv.isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(0xFF2ECC71), CircleShape)
                                            .border(2.dp, cardBg, CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name/Profession and message text description
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = prof.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (prof.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            tint = Color(0xFF2ECC71),
                                            contentDescription = "Verificado",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // Timestamp aligned to top right
                                    if (conv.timeLabel.isNotEmpty()) {
                                        Text(
                                            text = conv.timeLabel,
                                            fontSize = 11.sp,
                                            color = Color(0xFF75857F),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = conv.lastMessage,
                                        fontSize = 13.sp,
                                        color = Color(0xFF8B9E96),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // Unread message circle badge
                                    if (conv.unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color(0xFF2ECC71), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = conv.unreadCount.toString(),
                                                color = Color(0xFF0F1E19),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data class to store calculated conversation item state
data class ConversationItemState(
    val professional: Professional,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val timestamp: Long
)

// Helper function to format chat times
fun getFormattedChatTime(profId: Long, messageText: String, lastMsgTimestamp: Long): String {
    if (profId == 5L && (messageText.contains("a caminho") || messageText.contains("10 min"))) return "11:24"
    if (profId == 1L && messageText.contains("confirmado para amanhã")) return "Ontem"
    if (profId == 2L && messageText.contains("avaliação")) return "Seg"
    if (profId == 11L && messageText.contains("concluído com sucesso")) return "Sex"
    
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(lastMsgTimestamp))
}

// Helper to format double checklist and timestamp nicely
fun getFormattedMessageTime(msgText: String, timestamp: Long): String {
    return when {
        msgText.contains("fechadura do portão.") -> "10:55"
        msgText.contains("marca específica") -> "10:57"
        msgText.contains("Yale. A do portão") -> "11:02"
        msgText.contains("peças corretas") -> "11:07"
        msgText.contains("a caminho") -> "11:24"
        msgText.contains("confirmar o horário?") -> "13:58"
        msgText.contains("confirmado para amanhã") -> "14:02"
        msgText.contains("serviço de ontem!") -> "17:35"
        msgText.contains("obrigada pela avaliação") -> "17:40"
        msgText.contains("certo com a torneira?") -> "09:12"
        msgText.contains("concluído com sucesso") -> "09:30"
        else -> {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

// ==========================================
// 7. NOTIFICATIONS HISTORY LIST SCREEN
// ==========================================

@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToTab: (String) -> Unit = {},
    onNavigateToChat: (Long) -> Unit = {}
) {
    val alerts by viewModel.notifications.collectAsState()
    val professionals by viewModel.professionals.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }

    // Mark read on entering
    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsAsRead()
    }

    // Capture which items were unread when opening during this session
    // to preserve visual green unread indicators until navigate back
    val initiallyUnreadAlertIds = remember(alerts) {
        alerts.filter { !it.isRead }.map { it.id }.toSet()
    }

    // Helper to categorize messages
    val getAlertCategory = { title: String, body: String ->
        val text = (title + " " + body).lowercase()
        when {
            text.contains("agendamento") || text.contains("pedido") || text.contains("agenda") || text.contains("cancelado") || text.contains("pendente") -> "Pedidos"
            text.contains("pagamento") || text.contains("confirmado") || text.contains("mt") || text.contains("transação") || text.contains("pago") -> "Pagamentos"
            text.contains("mensagem") || text.contains("enviou") || text.contains("escreveu") || text.contains("escrever") || text.contains("chat") -> "Mensagens"
            else -> "Outros"
        }
    }

    val alertsCountMap = remember(alerts) {
        val map = mutableMapOf<String, Int>()
        map["Todas"] = alerts.size
        map["Pedidos"] = alerts.count { getAlertCategory(it.title, it.body) == "Pedidos" }
        map["Pagamentos"] = alerts.count { getAlertCategory(it.title, it.body) == "Pagamentos" }
        map["Mensagens"] = alerts.count { getAlertCategory(it.title, it.body) == "Mensagens" }
        map
    }

    val filteredAlerts = remember(alerts, selectedCategoryFilter) {
        if (selectedCategoryFilter == "Todas") {
            alerts
        } else {
            alerts.filter { getAlertCategory(it.title, it.body) == selectedCategoryFilter }
        }
    }

    // Dynamic grouping: Hoje (Today - within last 24h approximation) and Anteriores (Older)
    val (todayAlerts, olderAlerts) = remember(filteredAlerts) {
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L
        val today = filteredAlerts.filter { now - it.timestamp <= dayMillis }
        val older = filteredAlerts.filter { now - it.timestamp > dayMillis }
        Pair(today, older)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1311)) // Match deep dark premium background
    ) {
        // Aesthetic Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1E19))
                .padding(horizontal = 12.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Notificações",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Marcar lidas",
                color = Color(0xFF4DD99B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { viewModel.markAllNotificationsAsRead() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Horizontal Category Filter Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filterOptions = listOf("Todas", "Pedidos", "Pagamentos", "Mensagens")
            filterOptions.forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                val count = alertsCountMap[cat] ?: 0
                val dotColor = when (cat) {
                    "Pedidos" -> Color(0xFFFFA000) // Amber
                    "Pagamentos" -> Color(0xFF2ECC71) // Green
                    "Mensagens" -> Color(0xFF3498DB) // Blue
                    else -> null
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF4DD99B) else Color(0xFF14241F)
                    ),
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF1B2E28)),
                    modifier = Modifier
                        .clickable { selectedCategoryFilter = cat }
                        .testTag("filter_chip_$cat")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSelected && dotColor != null) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(6.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                        Text(
                            text = cat,
                            color = if (isSelected) Color(0xFF0C1311) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "($count)",
                                color = if (isSelected) Color(0xFF0C1311).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Notifications List Content
        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF133125), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF4DD99B)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "Tudo em dia!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Não tem nenhuma notificação de momento nesta categoria.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    if (selectedCategoryFilter != "Todas") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { selectedCategoryFilter = "Todas" },
                            border = BorderStroke(1.dp, Color(0xFF4DD99B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4DD99B))
                        ) {
                            Text("Ver todas")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("notifications_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: HOJE
                if (todayAlerts.isNotEmpty()) {
                    item {
                        Text(
                            text = "HOJE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8B9E98),
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                    items(todayAlerts) { alert ->
                        NotificationItemCard(
                            alert = alert,
                            isUnread = initiallyUnreadAlertIds.contains(alert.id),
                            getAlertCategory = getAlertCategory,
                            professionals = professionals,
                            onNavigateToTab = onNavigateToTab,
                            onNavigateToChat = onNavigateToChat
                        )
                    }
                }

                // Section: MAIS ANTIGAS (Older)
                if (olderAlerts.isNotEmpty()) {
                    item {
                        Text(
                            text = "MAIS ANTIGAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8B9E98),
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(olderAlerts) { alert ->
                        NotificationItemCard(
                            alert = alert,
                            isUnread = initiallyUnreadAlertIds.contains(alert.id),
                            getAlertCategory = getAlertCategory,
                            professionals = professionals,
                            onNavigateToTab = onNavigateToTab,
                            onNavigateToChat = onNavigateToChat
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    alert: PushNotification,
    isUnread: Boolean,
    getAlertCategory: (String, String) -> String,
    professionals: List<Professional>,
    onNavigateToTab: (String) -> Unit,
    onNavigateToChat: (Long) -> Unit
) {
    val category = remember(alert.title, alert.body) { getAlertCategory(alert.title, alert.body) }
    
    // Choose appropriate icon color and background
    val (icon, iconBgColor, iconColor) = remember(category) {
        when (category) {
            "Pagamentos" -> Triple(Icons.Default.CreditCard, Color(0xFF0F2B1E), Color(0xFF2ECC71))
            "Pedidos" -> Triple(Icons.Default.DateRange, Color(0xFF2A1F0D), Color(0xFFFFA000))
            "Mensagens" -> Triple(Icons.Default.ChatBubble, Color(0xFF0C2434), Color(0xFF3498DB))
            else -> Triple(Icons.Default.Notifications, Color(0xFF14241F), Color(0xFF4DD99B))
        }
    }

    val displayTime = remember(alert.timestamp) {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(alert.timestamp))
    }

    val matchedProf = remember(alert.title, alert.body, professionals) {
        val combinedText = alert.title + " " + alert.body
        professionals.find {
            combinedText.lowercase().contains(it.name.lowercase()) ||
            it.name.split(" ").any { namePart -> namePart.length > 2 && combinedText.lowercase().contains(namePart.lowercase()) }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14241F)),
        border = BorderStroke(1.dp, if (isUnread) Color(0xFF2ECC71).copy(alpha = 0.4f) else Color(0xFF1F352E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Squircle rounded icon box matching user mockup!
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    tint = iconColor,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTime,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        if (isUnread) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(7.dp)
                                    .background(Color(0xFF2ECC71), CircleShape)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                // Content text: Format with italicized quotes if it's a message, just like a user message preview!
                val isDirectMessage = category == "Mensagens"
                Text(
                    text = if (isDirectMessage) "\"${alert.body}\"" else alert.body,
                    fontSize = 13.sp,
                    fontStyle = if (isDirectMessage) FontStyle.Italic else FontStyle.Normal,
                    color = Color.White.copy(alpha = 0.65f)
                )

                // Inline Contextual Action Button matching user screenshot!
                if (category == "Pedidos" || category == "Mensagens") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1E19))
                            .clickable {
                                if (category == "Pedidos") {
                                    onNavigateToTab("agendamentos")
                                } else {
                                    if (matchedProf != null) {
                                        onNavigateToChat(matchedProf.id)
                                    } else {
                                        onNavigateToTab("mensagens")
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (category == "Pedidos") "Ver agenda" else "Responder",
                            color = Color(0xFF4DD99B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. USER PROFILE SCREEN
// ==========================================

@Composable
fun UserProfileScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val candidacyState by viewModel.candidacyState.collectAsState()
    val activeMode by viewModel.activeProfileMode.collectAsState()

    var pushPedidosEnabled by remember { mutableStateOf(true) }
    var pushMensagensEnabled by remember { mutableStateOf(true) }
    var pushPromocoesEnabled by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0C241B), // Rich dark green
            Color(0xFF133125), // Medium green
            Color(0xFF161616)  // Transition to background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        // Hero background with custom gradient and subtle geometric dot patterns
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val drawWidth = this.size.width
                    val drawHeight = this.size.height
                    // Draw rich vertical gradient
                    drawRect(brush = gradientBrush)
                    
                    // Draw high-fidelity subtle dot grid pattern matching designer layouts
                    val dotColor = Color(0xFF4DD99B).copy(alpha = 0.04f)
                    val spacing = 28f
                    val radius = 2f
                    for (x in 0..drawWidth.toInt() step spacing.toInt()) {
                        for (y in 0..drawHeight.toInt() step spacing.toInt()) {
                            drawCircle(
                                color = dotColor,
                                radius = radius,
                                center = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())
                            )
                        }
                    }
                }
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top-bar Toggle (Frictionless navigation "Modo Cliente / Modo Profissional")
                // Only shown if Approved (CandidacyState = C)
                if (candidacyState == "C") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F).copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                        modifier = Modifier
                            .width(280.dp)
                            .height(44.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (activeMode == "Cliente") Color(0xFF4DD99B) else Color.Transparent)
                                    .clickable { viewModel.setActiveProfileMode("Cliente") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Modo Cliente",
                                    color = if (activeMode == "Cliente") Color(0xFF0C241B) else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (activeMode == "Profissional") Color(0xFF4DD99B) else Color.Transparent)
                                    .clickable { viewModel.setActiveProfileMode("Profissional") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Modo Profissional",
                                    color = if (activeMode == "Profissional") Color(0xFF0C241B) else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Squarish highly rounded custom Avatar with soft shadow and green border
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                clip = false,
                                ambientColor = Color(0xFF4DD99B).copy(alpha = 0.25f),
                                spotColor = Color(0xFF4DD99B).copy(alpha = 0.45f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF133125))
                            .border(2.dp, Color(0xFF4DD99B), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VL",
                            color = Color(0xFF4DD99B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                    }

                    // Little camera floating badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFF4DD99B), CircleShape)
                            .border(2.dp, Color(0xFF133125), CircleShape)
                            .clickable { /* Select Photo */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Atualizar foto",
                            tint = Color(0xFF0C241B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Username with premium styling + verified icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Valgy",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    if (candidacyState == "C" || activeMode == "Profissional") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verificado",
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Phone number with Smartphone icon outline (per user requests)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Smartphone,
                        contentDescription = "Telefone",
                        tint = Color(0xFF4DD99B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+258 84 942 1826",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Stats card: unified card with vertical dividers (completely restructured!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF262626)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Feature 1: Serviços
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (activeMode == "Profissional") "34" else "12",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4DD99B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Serviços",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Divider 1
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF2A2A2A))
                )

                // Feature 2: Avaliação
                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (activeMode == "Profissional") "5.0" else "4.9",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(15.dp).padding(start = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeMode == "Profissional") "Avaliação média" else "Avaliação dada",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // Divider 2
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF2A2A2A))
                )

                // Feature 3: Favoritos
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (activeMode == "Profissional") "14" else "3",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4DD99B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeMode == "Profissional") "Recomendações" else "Favoritos",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dynamic Candidacy Banner / Promotion Section (States A vs B vs C)
        when (candidacyState) {
            "A" -> {
                // Estado A: Default Green Invitation Banner (Active state clicks to State B!)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF133125)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable { onNavigate("professional_profile") }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = "Trabalhar",
                                tint = Color(0xFF4DD99B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quer oferecer serviços?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Candidate-se para se tornar um profissional verificado no Muteko",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            tint = Color(0xFF4DD99B),
                            contentDescription = "Continuar"
                        )
                    }
                }
            }
            "B" -> {
                // Estado B: Under Analysis Yellow Banner (Clicking simulated approval to State C!)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF383015)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable { viewModel.setCandidacyState("C") }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = "Em análise",
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Candidatura em análise",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A sua candidatura está a ser revista pela nossa equipa de qualidade.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            tint = Color(0xFFFFA000),
                            contentDescription = "Revisar"
                        )
                    }
                }
            }
            "C" -> {
                // Estado C: Approved! No warning banner needed. We show a nice congratulatory tag.
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF133125).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4DD99B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Muteko Profissional Ativado & Verificado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4DD99B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Actions menu structured strictly to match screenshots perfectly
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category 1: CONTA
            Text(
                text = "CONTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Outlined.Person,
                        title = "Informações pessoais",
                        onClick = { /* Edit Personal Info */ }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.PinDrop,
                        title = "Endereços guardados",
                        badge = "2",
                        onClick = { /* Manage Addresses */ }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "Profissionais favoritos",
                        badge = if (activeMode == "Profissional") "14" else "3",
                        onClick = { /* View Favorites */ }
                    )
                }
            }

            // Category 2: PAGAMENTOS
            Text(
                text = "PAGAMENTOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Outlined.Smartphone,
                        title = "M-Pesa",
                        badge = "+258 84 ...",
                        onClick = { /* Mobile Money configuration */ }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "Histórico de pagamentos",
                        onClick = { /* Payment History */ }
                    )
                }
            }

            // Category 3: NOTIFICAÇÕES (with responsive Switch items as shown in mockup!)
            Text(
                text = "NOTIFICAÇÕES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileSwitchRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Estado dos pedidos",
                        checked = pushPedidosEnabled,
                        onCheckedChange = { pushPedidosEnabled = it }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileSwitchRow(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Mensagens",
                        checked = pushMensagensEnabled,
                        onCheckedChange = { pushMensagensEnabled = it }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileSwitchRow(
                        icon = Icons.Outlined.Redeem,
                        title = "Promoções",
                        checked = pushPromocoesEnabled,
                        onCheckedChange = { pushPromocoesEnabled = it }
                    )
                }
            }

            // Category 4: SUPORTE & SAIR
            Text(
                text = "SUPORTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Centro de ajuda",
                        onClick = { /* Open Help Center */ }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.Security,
                        title = "Privacidade & Segurança",
                        onClick = { /* Privacy Settings */ }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    
                    // Terminar sessão row styled natively
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLoggedIn(false) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sair",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Terminar sessão",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }

            // Developer State Simulator Tool for fast live verification!
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SIMULADOR DE CANDIDATURA (DEMO)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "A" to "Padrão (A)",
                            "B" to "Análise (B)",
                            "C" to "Aprovado (C)"
                        ).forEach { (state, label) ->
                            val isSelected = candidacyState == state
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF133125) else Color(0xFF1E1E1E))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF4DD99B) else Color(0xFF2A2A2A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setCandidacyState(state)
                                        if (state != "C") {
                                            viewModel.setActiveProfileMode("Cliente")
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF4DD99B) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4DD99B),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Text(
                text = badge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            tint = Color.White.copy(alpha = 0.2f),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ProfileSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4DD99B),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4DD99B),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2C2C2C)
            )
        )
    }
}

// ==========================================
// UTILITY HELPERS
// ==========================================

fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts[0].take(1) + parts[parts.size - 1].take(1)).uppercase()
}

fun getAvatarBgColor(name: String): Color {
    return when (name) {
        "Ronaldo Langa" -> Color(0xFF113826) // Deep green-teal
        "Marcio Silva" -> Color(0xFF1E293B) // Slate blue
        "Albertina Mondlane" -> Color(0xFF2E1C3F) // Deep purple
        "Filipe Macamo" -> Color(0xFF3F2E1C) // Warm brown
        "Carlos Tembe" -> Color(0xFF3B1E1E) // Deep burgundy
        "Regina Mucavel" -> Color(0xFF1A333E) // Muted azure
        "Rodrigo Nhaca" -> Color(0xFF2C3E2C) // Forest moss
        else -> {
            val hash = Math.abs(name.hashCode())
            val backgroundColors = listOf(
                Color(0xFF113826),
                Color(0xFF1E293B),
                Color(0xFF2E1C3F),
                Color(0xFF3F2E1C),
                Color(0xFF3B1E1E),
                Color(0xFF1A333E),
                Color(0xFF2C3E2C)
            )
            backgroundColors[hash % backgroundColors.size]
        }
    }
}

fun getAvatarTextColor(name: String): Color {
    return when (name) {
        "Ronaldo Langa" -> Color(0xFF4DD99B) // Mint Green
        "Marcio Silva" -> Color(0xFF90CDF4) // Sky Blue
        "Albertina Mondlane" -> Color(0xFFD6BCFA) // Lavender
        "Filipe Macamo" -> Color(0xFFFEEBC8) // Cream
        "Carlos Tembe" -> Color(0xFFFEB2B2) // Coral
        "Regina Mucavel" -> Color(0xFF9DECF9) // Cyan
        "Rodrigo Nhaca" -> Color(0xFFC6F6D5) // Mint
        else -> Color(0xFF4DD99B)
    }
}

fun extractDistanceValue(location: String): String {
    // Extract something like "1.2" from "(A 1.2 km)"
    val regex = """(\d+(\.\d+)?)\s*km""".toRegex()
    val match = regex.find(location)
    return match?.groupValues?.get(1) ?: "1.5"
}

fun getCategoryEmoji(category: String): String {
    return when {
        category.contains("Eletricista", ignoreCase = true) -> "⚡"
        category.contains("Diarista", ignoreCase = true) || category.contains("Limpeza", ignoreCase = true) -> "🧹"
        category.contains("Canalizador", ignoreCase = true) || category.contains("Encanador", ignoreCase = true) -> "🚰"
        category.contains("Explicador", ignoreCase = true) -> "📚"
        category.contains("Carpinteiro", ignoreCase = true) -> "🪚"
        category.contains("Motorista", ignoreCase = true) -> "🚗"
        category.contains("Pintor", ignoreCase = true) -> "🎨"
        category.contains("Chaveiro", ignoreCase = true) || category.contains("Serralheiro", ignoreCase = true) -> "🔑"
        category.contains("Jardineiro", ignoreCase = true) -> "🏡"
        category.contains("Técnico de TI", ignoreCase = true) -> "💻"
        else -> "🛠️"
    }
}

fun getCategoryThemeColor(category: String): Color {
    return when {
        category.contains("Eletricista", ignoreCase = true) -> Color(0xFFFFB300) // Âmbar
        category.contains("Canalizador", ignoreCase = true) || category.contains("Encanador", ignoreCase = true) -> Color(0xFF2196F3) // Azul
        category.contains("Explicador", ignoreCase = true) -> Color(0xFF9C27B0) // Roxo
        category.contains("Carpinteiro", ignoreCase = true) -> Color(0xFF4CAF50) // Verde
        category.contains("Diarista", ignoreCase = true) || category.contains("Limpeza", ignoreCase = true) -> Color(0xFFE53935) // Vermelho
        category.contains("Motorista", ignoreCase = true) -> Color(0xFFFF9800) // Laranja
        else -> Color(0xFF5AE0A8) // Default Muteko Green
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("Eletricista", ignoreCase = true) -> Icons.Default.FlashOn
        category.contains("Canalizador", ignoreCase = true) || category.contains("Encanador", ignoreCase = true) -> Icons.Default.WaterDrop
        category.contains("Explicador", ignoreCase = true) -> Icons.Default.School
        category.contains("Carpinteiro", ignoreCase = true) -> Icons.Default.Build
        category.contains("Diarista", ignoreCase = true) || category.contains("Limpeza", ignoreCase = true) -> Icons.Default.CleaningServices
        category.contains("Motorista", ignoreCase = true) -> Icons.Default.DirectionsCar
        category.contains("Pintor", ignoreCase = true) -> Icons.Default.Brush
        category.contains("Chaveiro", ignoreCase = true) || category.contains("Serralheiro", ignoreCase = true) -> Icons.Default.Build
        category.contains("Jardineiro", ignoreCase = true) -> Icons.Default.Nature
        category.contains("Técnico de TI", ignoreCase = true) -> Icons.Default.Computer
        else -> Icons.Default.Build
    }
}

// ==========================================
// 9. PREMIUM LOGIN FLOW (MUTEKO BRAND)
// ==========================================

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    var phoneText by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(0) } // 0 = Phone Entry, 1 = OTP Verif

    var resendTimer by remember { mutableStateOf(28) }
    LaunchedEffect(currentStep) {
        if (currentStep == 1) {
            resendTimer = 28
            while (resendTimer > 0) {
                delay(1000L)
                resendTimer--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D2318))
    ) {
        if (currentStep == 0) {
            // STEP 0: PHONE NUMBER ENTRY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 32.dp)
            ) {
                // Muteko App Logo & Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF4DD99B), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF0D2318),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Muteko",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Welcome back titles
                Text(
                    text = "Bem-vindo de volta",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Entre com o seu número de telefone",
                    fontSize = 15.sp,
                    color = Color(0xFF6B8F7D)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // PHONE NUMBER LABEL
                Text(
                    text = "NÚMERO DE TELEFONE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B8F7D),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Mozambique Phone Input Field Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF142A1E), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E3D2C), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🇲🇿",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+258",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Divider Line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0xFF1E3D2C))
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    // TextField wrapper for interactive usage
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (phoneText.isEmpty()) {
                            Text(
                                text = "84 000 0000",
                                color = Color(0xFF4D6B5A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        BasicTextField(
                            value = phoneText,
                            onValueChange = { input ->
                                if (input.length <= 12 && input.all { it.isDigit() || it == ' ' }) {
                                    phoneText = input
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = java.lang.Object().run { androidx.compose.ui.graphics.SolidColor(Color(0xFF4DD99B)) }, // safe static wrap
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("login_phone_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Main Button: Receber código OTP
                Button(
                    onClick = {
                        if (phoneText.trim().isNotEmpty()) {
                            currentStep = 1
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4DD99B),
                        contentColor = Color(0xFF0D2318)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_otp_request_button")
                ) {
                    Text(
                        text = "Receber código OTP",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // OR Divider matching mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF1E3D2C)))
                    Text(
                        text = "ou continuar com",
                        color = Color(0xFF3D6652),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF1E3D2C)))
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Social Grid Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google
                    Button(
                        onClick = { onLoginSuccess() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF142A1E),
                            contentColor = Color(0xFF9DB8A8)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF1E3D2C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF9DB8A8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Facebook
                    Button(
                        onClick = { onLoginSuccess() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF142A1E),
                            contentColor = Color(0xFF9DB8A8)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF1E3D2C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "f",
                            color = Color(0xFF9DB8A8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Facebook",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Legal text footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ao entrar, aceita os nossos",
                        color = Color(0xFF3D6652),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text(
                            text = "Termos de uso",
                            color = Color(0xFF4DD99B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { /* Doc */ }
                        )
                        Text(
                            text = " e ",
                            color = Color(0xFF3D6652),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Política de privacidade",
                            color = Color(0xFF4DD99B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { /* Doc */ }
                        )
                    }
                }
            }
        } else {
            // STEP 1: OTP VERIFICATION
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 32.dp)
            ) {
                // Return Navigation button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = { currentStep = 0 },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Heading titles
                Text(
                    text = "Verificar número",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                val displayPhone = if (phoneText.trim().isEmpty()) "84 000 0000" else phoneText
                Text(
                    text = "Introduza o código de 4 dígitos enviado para +258 $displayPhone",
                    fontSize = 15.sp,
                    color = Color(0xFF6B8F7D),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Interactive 4 cells OTP field built with BasicTextField Custom decobox
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = otpCode,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                otpCode = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
                            ) {
                                for (i in 0 until 4) {
                                    val digit = otpCode.getOrNull(i)?.toString() ?: ""
                                    val isFocused = otpCode.length == i
                                    Box(
                                        modifier = Modifier
                                            .size(width = 64.dp, height = 64.dp)
                                            .background(Color(0xFF142A1E), RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isFocused) 2.dp else 1.dp,
                                                color = if (isFocused) Color(0xFF4DD99B) else if (digit.isNotEmpty()) Color(0xFF4DD99B).copy(alpha = 0.5f) else Color(0xFF1E3D2C),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = digit,
                                            color = if (digit.isNotEmpty()) Color(0xFF4DD99B) else Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_otp_input_field")
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Resend section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Não recebeu o código? ",
                        color = Color(0xFF6B8F7D),
                        fontSize = 14.sp
                    )
                    if (resendTimer > 0) {
                        Text(
                            text = "Reenviar (${resendTimer}s)",
                            color = Color(0xFF4DD99B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Reenviar código",
                            color = Color(0xFF4DD99B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clickable {
                                otpCode = ""
                                resendTimer = 28
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Verify Action Button
                Button(
                    onClick = {
                        if (otpCode.length == 4) {
                            onLoginSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4DD99B),
                        contentColor = Color(0xFF0D2318)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_otp_verify_button"),
                    enabled = otpCode.length == 4
                ) {
                    Text(
                        text = "Verificar",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Change phone number alternative button
                Button(
                    onClick = { currentStep = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF4DD99B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1E3D2C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_back_phone_button")
                ) {
                    Text(
                        text = "Mudar número",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 10. PREMIUM ONBOARDING FLOW
// ==========================================

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onGoToLogin: () -> Unit
) {
    var pageIndex by remember { mutableStateOf(0) }
    var isContratarSelected by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF032B1C)) // Fundo principal: Verde escuro
    ) {
        // TOP HALF: Visual Illustration with Slide + Fade Transition
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
        ) {
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                },
                label = "TopVisualTransition"
            ) { targetPageState ->
                val imageRes = when (targetPageState) {
                    0 -> R.drawable.img_onboarding_1_1781797432255
                    1 -> R.drawable.img_onboarding_2_1781797446056
                    else -> R.drawable.img_onboarding_3_1781797459849
                }
                
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Subtle gradient to blend the image seamlessly into the bottom content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF032B1C)
                            )
                        )
                    )
            )
        }

        // BOTTOM HALF: Interactive content & buttons with smooth slide + fade transitions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(animationSpec = tween(500)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing),
                        targetOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeOut(animationSpec = tween(500))
                },
                modifier = Modifier.fillMaxWidth(),
                label = "BottomTextTransition"
            ) { targetPageState ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (targetPageState) {
                        0 -> {
                            val title = buildAnnotatedString {
                                append("Serviços de Confiança perto de ")
                                withStyle(SpanStyle(color = Color(0xFF5AE0A8), fontWeight = FontWeight.ExtraBold)) {
                                    append("si")
                                }
                            }
                            Text(
                                text = title,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 36.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Encontre e contrate eletricistas, canalizadores, explicadores, pintores e diversos profissionais qualificados no seu bairro com total facilidade e segurança.",
                                fontSize = 15.sp,
                                color = Color(0xFF8FA79C), // Secondary text
                                lineHeight = 22.sp
                            )
                        }
                        1 -> {
                            val title = buildAnnotatedString {
                                append("Profissionais ")
                                withStyle(SpanStyle(color = Color(0xFF5AE0A8), fontWeight = FontWeight.ExtraBold)) {
                                    append("Verificados")
                                }
                            }
                            Text(
                                text = title,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 36.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Garantimos tranquilidade e máxima confiança. Cada prestador passa por uma rigorosa verificação de identidade e possui avaliações e comentários reais de clientes.",
                                fontSize = 15.sp,
                                color = Color(0xFF8FA79C), // Secondary text
                                lineHeight = 22.sp
                            )
                        }
                        2 -> {
                            val title = buildAnnotatedString {
                                append("Qual será o seu ")
                                withStyle(SpanStyle(color = Color(0xFF5AE0A8), fontWeight = FontWeight.ExtraBold)) {
                                    append("papel")
                                }
                                append(" no Muteko?")
                            }
                            Text(
                                text = title,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 36.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            // Role Selection Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    onClick = { isContratarSelected = true },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A4A34)),
                                    border = BorderStroke(
                                        width = if (isContratarSelected) 1.5.dp else 1.dp,
                                        color = if (isContratarSelected) Color(0xFF5AE0A8) else Color(0xFF032B1C).copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(80.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = if (isContratarSelected) Color(0xFF5AE0A8) else Color(0xFF8FA79C), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Contratar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isContratarSelected) Color.White else Color(0xFF8FA79C))
                                    }
                                }

                                Card(
                                    onClick = { isContratarSelected = false },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A4A34)),
                                    border = BorderStroke(
                                        width = if (!isContratarSelected) 1.5.dp else 1.dp,
                                        color = if (!isContratarSelected) Color(0xFF5AE0A8) else Color(0xFF032B1C).copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(80.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Handyman, contentDescription = null, tint = if (!isContratarSelected) Color(0xFF5AE0A8) else Color(0xFF8FA79C), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Trabalhar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isContratarSelected) Color.White else Color(0xFF8FA79C))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Escolha 'Contratar' para buscar prestadores qualificados ou 'Trabalhar' para oferecer os seus serviços e gerar renda extra. Pode alterar isto a qualquer momento.",
                                fontSize = 13.sp,
                                color = Color(0xFF8FA79C), // Secondary text
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Page indicator elements
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    for (i in 0 until 3) {
                        val isSelected = i == pageIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 20.dp else 6.dp)
                                .background(
                                    color = if (isSelected) Color(0xFF5AE0A8) else Color(0xFF0A4A34),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                // Primary Button
                val primaryButtonText = if (pageIndex == 2) "Criar conta" else "Continuar"
                Button(
                    onClick = {
                        if (pageIndex < 2) {
                            pageIndex++
                        } else {
                            onOnboardingComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF52D19A), // Botão "Continuar": Verde menta
                        contentColor = Color(0xFF032B1C)    // Fundo principal: Verde escuro
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_primary_button")
                ) {
                    Text(
                        text = primaryButtonText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Button
                val secondaryButtonText = if (pageIndex == 2) "Já tenho conta" else "Saltar"
                Button(
                    onClick = {
                        onGoToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF5AE0A8)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF0A4A34)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_secondary_button")
                ) {
                    Text(
                        text = secondaryButtonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfessionalProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val candidacyState by viewModel.candidacyState.collectAsState()
    val userProfile by viewModel.currentUserProfessionalProfile.collectAsState()

    // Wizard navigation step: 1 (Category & bio), 2 (ID card upload), 3 (Selfie camera capture), 4 (Summary review), 5 (Amber Confirmed Pending Screen)
    var currentStep by remember { mutableStateOf(1) }

    // Step 1 input states
    var selectedCategory by remember { mutableStateOf("Eletricista") }
    var descriptionText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }

    // Step 2 ID card upload state
    var uploadFileName by remember { mutableStateOf<String?>(null) }

    // Step 3 Face verification selfie state
    var uploadSelfieSelected by remember { mutableStateOf(false) }

    // Step 4 Terms and conditions consent
    var termsAccepted by remember { mutableStateOf(false) }

    // Active Professional Profile Edit States (for when approved status "C" editing)
    var isEditing by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf("Eletricista") }
    var editDescription by remember { mutableStateOf("") }
    var editRatePerHour by remember { mutableStateOf("450") }
    var editLocationText by remember { mutableStateOf("Maputo, Moçambique") }
    var editSpecialization by remember { mutableStateOf("") }
    var editExperience by remember { mutableStateOf("5") }

    // Hook up active professional data
    LaunchedEffect(userProfile, isEditing) {
        userProfile?.let { prof ->
            if (!isEditing) {
                editCategory = prof.category
                editDescription = prof.description
                editRatePerHour = prof.ratePerHour.toInt().toString()
                editLocationText = prof.location
                editSpecialization = prof.specialization
                editExperience = prof.yearsOfExperience.toString()
            }
        }
    }

    // Categories list for registration screen
    val categoriesWithIcons = listOf(
        Triple("Eletricista", Icons.Default.FlashOn, Color(0xFFFFA000)),
        Triple("Canalizador", Icons.Outlined.WaterDrop, Color(0xFF29B6F6)),
        Triple("Chaveiro", Icons.Default.Key, Color(0xFF26A69A)),
        Triple("Carpinteiro", Icons.Default.Construction, Color(0xFFAB47BC)),
        Triple("Diarista", Icons.Default.LocalLaundryService, Color(0xFFEC407A)),
        Triple("Motorista", Icons.Default.DirectionsCar, Color(0xFFFF7043))
    )

    // Force Confirmation Screen if candidacyState is already analytical pending ("B")
    val effectiveStep = if (candidacyState == "B") 5 else currentStep

    if (candidacyState == "C" && !isEditing) {
        // ==========================================
        // 1. ACTIVE APPROVED PROFESSIONAL DASHBOARD
        // ==========================================
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Perfil de Profissional", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF112D21),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF161616)
        ) { paddingValues ->
            val prof = userProfile ?: Professional(
                id = 9999,
                name = "Valgy",
                category = editCategory,
                rating = 5.0,
                reviewCount = 14,
                ratePerHour = editRatePerHour.toDoubleOrNull() ?: 650.0,
                description = editDescription.ifEmpty { "Eletricista certificado com 5 anos de experiência em instalações residenciais no Muteko." },
                location = editLocationText,
                isVerified = true,
                availabilityStatus = "Disponível hoje",
                specialization = editSpecialization.ifEmpty { "Painéis Solares e Motores Geradores" },
                yearsOfExperience = editExperience.toIntOrNull() ?: 5,
                certificates = "Certificação Técnica profissional INFP"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Congratulations Active Badge Header
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF112D21)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFF133125), CircleShape)
                                    .border(2.dp, Color(0xFF4DD99B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF4DD99B),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cadastro Ativo!",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color(0xFF4DD99B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Você está visível como profissional verificado na plataforma de Serviços do Muteko",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Professional Metadata details
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Seus Dados de Exibição",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val detailItems = listOf(
                                "Nome" to "Valgy",
                                "Categoria de Serviço" to prof.category,
                                "Preço por Hora" to "${prof.ratePerHour.toInt()} MT / hora",
                                "Área de Atendimento" to prof.location,
                                "Anos de Experiência" to "${prof.yearsOfExperience} anos",
                                "Especialização" to prof.specialization.ifEmpty { "Nenhuma informada" },
                                "Rating Médio" to "5.0 ★ (14 recomendações)"
                            )

                            detailItems.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                    Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Divider(color = Color(0xFF2E2E2E))
                            }

                            if (prof.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Sobre Você", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prof.description,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52D19A), contentColor = Color(0xFF032B1C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editar Informações do Perfil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.setCandidacyState("A")
                                viewModel.setActiveProfileMode("Cliente")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFFF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Desativar Modo Profissional", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    } else if (candidacyState == "C" && isEditing) {
        // ==========================================
        // 2. ACTIVE APPROVED PROFESSIONAL EDIT MODE
        // ==========================================
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Editar Perfil", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { isEditing = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF112D21),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF161616)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Atualize os seus detalhes profissionais",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )

                            // Hourly Rate
                            Column {
                                Text("Preço por Hora (MT / hora)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editRatePerHour,
                                    onValueChange = { editRatePerHour = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4DD99B),
                                        unfocusedBorderColor = Color(0xFF2E2E2E),
                                        focusedContainerColor = Color(0xFF2E2E2E),
                                        unfocusedContainerColor = Color(0xFF2E2E2E)
                                    )
                                )
                            }

                            // Years of Experience
                            Column {
                                Text("Anos de Experiência", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editExperience,
                                    onValueChange = { editExperience = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4DD99B),
                                        unfocusedBorderColor = Color(0xFF2E2E2E),
                                        focusedContainerColor = Color(0xFF2E2E2E),
                                        unfocusedContainerColor = Color(0xFF2E2E2E)
                                    )
                                )
                            }

                            // Location
                            Column {
                                Text("Área de Atendimento principal", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editLocationText,
                                    onValueChange = { editLocationText = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4DD99B),
                                        unfocusedBorderColor = Color(0xFF2E2E2E),
                                        focusedContainerColor = Color(0xFF2E2E2E),
                                        unfocusedContainerColor = Color(0xFF2E2E2E)
                                    )
                                )
                            }

                            // Description Bio
                            Column {
                                Text("Descrição profissional / Biografia curta", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editDescription,
                                    onValueChange = { editDescription = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4DD99B),
                                        unfocusedBorderColor = Color(0xFF2E2E2E),
                                        focusedContainerColor = Color(0xFF2E2E2E),
                                        unfocusedContainerColor = Color(0xFF2E2E2E)
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (editDescription.trim().isNotEmpty()) {
                                    viewModel.saveProfessionalProfile(
                                        category = editCategory,
                                        description = editDescription,
                                        ratePerHour = editRatePerHour.toDoubleOrNull() ?: 650.0,
                                        location = editLocationText,
                                        specialization = editSpecialization,
                                        yearsOfExperience = editExperience.toIntOrNull() ?: 5
                                    )
                                    isEditing = false
                                } else {
                                    viewModel.setErrorMessage("A biografia não pode ficar vazia.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52D19A), contentColor = Color(0xFF032B1C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Salvar Perfil", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // ==========================================
        // 3. CANDIDACY STEPS WIZARD (EffectiveStep 1 to 5)
        // ==========================================
        val screenTitle = when (effectiveStep) {
            1 -> "Tornar-se profissional"
            2 -> "Verificação de identidade"
            3 -> "Confirme que é você"
            4 -> "Resumo da candidatura"
            else -> "Candidatura enviada!"
        }

        Scaffold(
            topBar = {
                if (effectiveStep <= 4) {
                    TopAppBar(
                        title = {
                            Text(
                                text = screenTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (currentStep > 1) {
                                        currentStep--
                                    } else {
                                        onBack()
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .background(Color(0xFF133125), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$effectiveStep / 4",
                                    color = Color(0xFF4DD99B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF161616),
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            containerColor = Color(0xFF161616)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                // Wizard top step indicator (Omit on Confirmation screen)
                if (effectiveStep <= 4) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        for (i in 1..4) {
                            val isActive = effectiveStep == i
                            val isCompleted = effectiveStep > i

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) Color(0xFF133125)
                                        else if (isCompleted) Color(0xFF133125)
                                        else Color(0xFF1E1E1E)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isActive) Color(0xFF4DD99B)
                                        else if (isCompleted) Color(0xFF4DD99B).copy(alpha = 0.4f)
                                        else Color(0xFF2E2E2E),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4DD99B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = i.toString(),
                                        color = if (isActive) Color(0xFF4DD99B) else Color.White.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (i < 4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(
                                            if (effectiveStep > i) Color(0xFF4DD99B).copy(alpha = 0.4f)
                                            else Color(0xFF2E2E2E)
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Wizard Steps Content
                Box(modifier = Modifier.weight(1f)) {
                    when (effectiveStep) {
                        1 -> {
                            // ==========================================
                            // STEP 1 — CATEGORY, BIO & HOURLY RATE
                            // ==========================================
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "QUAL É A SUA ÁREA?",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )

                                // Responsive Grid of Categories (2 columns)
                                val gridItems = categoriesWithIcons.chunked(2)
                                gridItems.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { (catName, catIcon, catColor) ->
                                            val isSelected = selectedCategory == catName
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFF1E1E1E)
                                                ),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(
                                                    width = 2.dp,
                                                    color = if (isSelected) Color(0xFF4DD99B) else Color(0xFF262626)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(90.dp)
                                                    .clickable { selectedCategory = catName }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = catIcon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color(0xFF4DD99B) else catColor,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = catName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color(0xFF4DD99B) else Color.White.copy(alpha = 0.5f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "CONTE-NOS SOBRE SI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                Column {
                                    Text(
                                        text = "Breve biografia profissional",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = descriptionText,
                                        onValueChange = { descriptionText = it },
                                        placeholder = {
                                            Text(
                                                text = "Ex: Eletricista com 5 anos de experiência em instalações residenciais e comerciais em Maputo...",
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontSize = 13.sp
                                            )
                                        },
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4DD99B),
                                            unfocusedBorderColor = Color(0xFF262626),
                                            focusedContainerColor = Color(0xFF1A1A1A),
                                            unfocusedContainerColor = Color(0xFF1E1E1E).copy(alpha = 0.2f)
                                        )
                                    )
                                }

                                Text(
                                    text = "PREÇO POR HORA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                OutlinedTextField(
                                    value = rateText,
                                    onValueChange = { rateText = it },
                                    placeholder = {
                                        Text(
                                            text = "Ex: 650 MT/hora",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Paid,
                                            contentDescription = null,
                                            tint = Color(0xFF4DD99B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4DD99B),
                                        unfocusedBorderColor = Color(0xFF262626),
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1E1E1E).copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }

                        2 -> {
                            // ==========================================
                            // STEP 2 — IDENTITY DOCUMENT QUALITY CHECK
                            // ==========================================
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "DOCUMENTO DE IDENTIDADE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = "Bilhete de Identidade ou Passaporte",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )

                                // Clickable custom dotted file upload box
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (uploadFileName != null) Color(0xFF133125) else Color(0xFF1A1A1A)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (uploadFileName != null) Color(0xFF4DD99B) else Color(0xFF2E2E2E)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clickable {
                                            uploadFileName = "BI_Valgy_Algy_Nacional.jpg"
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (uploadFileName != null) Color(0xFF4DD99B).copy(alpha = 0.1f)
                                                    else Color(0xFF1E1E1E)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (uploadFileName != null) Icons.Default.CheckCircle else Icons.Outlined.Badge,
                                                contentDescription = null,
                                                tint = Color(0xFF4DD99B),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = uploadFileName ?: "Toque para carregar foto do BI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (uploadFileName != null) Color(0xFF4DD99B) else Color.White
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (uploadFileName != null) "Mudar ficheiro" else "JPG ou PNG, máx. 5MB",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Interactive checklist tips card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF262626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val checks = listOf(
                                            "Documento legível, sem reflexos ou cortes",
                                            "Nome e número de identificação visíveis",
                                            "Documento ainda válido"
                                        )
                                        checks.forEach { text ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4DD99B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = text,
                                                    fontSize = 13.sp,
                                                    color = Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Security privacy card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF383015).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Security,
                                            contentDescription = null,
                                            tint = Color(0xFFFFA000),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "O seu documento é usado apenas para verificação de identidade e não é partilhado publicamente.",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            lineHeight = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // ==========================================
                            // STEP 3 — VERIFICAÇÃO FACIAL (SELFIE ACCURACY MASK)
                            // ==========================================
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "VERIFICAÇÃO FACIAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = "Tire uma selfie para confirmarmos que é a mesma pessoa do documento.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )

                                // Custom Oval/Dome shape face container frame
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(if (uploadSelfieSelected) Color(0xFF133125) else Color(0xFF1E1E1E))
                                        .border(
                                            width = 1.dp,
                                            color = if (uploadSelfieSelected) Color(0xFF4DD99B) else Color(0xFF262626),
                                            shape = RoundedCornerShape(32.dp)
                                        )
                                        .clickable { uploadSelfieSelected = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uploadSelfieSelected) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .background(Color(0xFF112D21), CircleShape)
                                                    .border(2.dp, Color(0xFF4DD99B), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4DD99B),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Selfie Capturada com sucesso!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF4DD99B)
                                            )
                                            Text(
                                                text = "Toque para tirar outra",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                        }
                                    } else {
                                        // Face arch outline drawer box representation
                                        Box(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .height(180.dp)
                                                .drawBehind {
                                                    val brush = Brush.linearGradient(
                                                        colors = listOf(Color(0xFF4DD99B).copy(alpha = 0.5f), Color(0xFF4DD99B).copy(alpha = 0.1f))
                                                    )
                                                    drawRoundRect(
                                                        brush = brush,
                                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f, 100f),
                                                        style = Stroke(
                                                            width = 4f,
                                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                                        )
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "Câmara",
                                                    tint = Color(0xFF4DD99B).copy(alpha = 0.7f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Toque para\ntirar a selfie",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Helpful hints checklist card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF262626)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        listOf(
                                            Icons.Filled.LightMode to "Use boa iluminação, de frente para a câmara",
                                            Icons.Filled.Face to "Sem óculos escuros, boné ou máscara",
                                            Icons.Filled.CropFree to "Enquadre o rosto completo, sem cortar"
                                        ).forEach { (icon, tip) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4DD99B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = tip,
                                                    fontSize = 13.sp,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // ==========================================
                            // STEP 4 — SUMMARY, PRIVACY CONSENT & SUBMIT
                            // ==========================================
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "RESUMO DA CANDIDATURA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )

                                // Custom Structured Summary detail card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF262626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        val summaryList = listOf(
                                            "Categoria" to selectedCategory,
                                            "Preço por hora" to (if (rateText.isNotEmpty()) "$rateText MT" else "650 MT / hora"),
                                            "Documento" to (if (uploadFileName != null) "Carregado" else "Pendente"),
                                            "Selfie" to (if (uploadSelfieSelected) "Carregada" else "Pendente")
                                        )

                                        summaryList.forEachIndexed { index, (label, valText) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (valText == "Carregado" || valText == "Carregada") {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF4DD99B),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        text = valText,
                                                        color = if (valText == "Pendente") Color(0xFFFFA000) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                            if (index < summaryList.size - 1) {
                                                Divider(color = Color(0xFF262626))
                                            }
                                        }
                                    }
                                }

                                // Checkbox consent section
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { termsAccepted = !termsAccepted }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = termsAccepted,
                                        onCheckedChange = { termsAccepted = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF4DD99B),
                                            uncheckedColor = Color(0xFF2E2E2E),
                                            checkmarkColor = Color(0xFF032B1C)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Confirmo que os dados fornecidos são verdadeiros e aceito os Termos para Profissionais e a Política de Verificação do Muteko.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        lineHeight = 16.sp
                                    )
                                }

                                // Timeline Amber warning box
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF383015).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = Color(0xFFFFA000),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "A análise demora entre 24 a 72 horas. Receberá uma notificação assim que for concluída.",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            lineHeight = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            // ==========================================
                            // STEP 5 — OVERLAY EM ÂMBBAR: CONFIRMATION SCREEN
                            // ==========================================
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Large Amber circle with outline clock matching layout perfectly
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(Color(0xFF383015).copy(alpha = 0.4f), CircleShape)
                                        .border(2.dp, Color(0xFFFFA000), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(44.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Text(
                                    text = "Candidatura enviada!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "A nossa equipa vai rever os seus dados.\nIsto demora normalmente até 72 horas.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Informational bullet points inside container
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF262626)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 320.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Work,
                                                contentDescription = null,
                                                tint = Color(0xFF4DD99B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "$selectedCategory · ${if (rateText.isNotEmpty()) rateText else "650"} MT/hora",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4DD99B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Documento e selfie recebidos",
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = Color(0xFF4DD99B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Será notificado por aqui",
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Wizard Bottom Navigation Action Row
                if (effectiveStep <= 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small Circular ArrowBackButton as shown in screenshots
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (currentStep > 1) {
                                        currentStep--
                                    } else {
                                        onBack()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retroceder",
                                tint = Color(0xFF4DD99B)
                            )
                        }

                        // Primary Action button
                        val buttonText = if (effectiveStep == 4) "Enviar candidatura" else "Continuar"
                        val stepValidated = when (effectiveStep) {
                            1 -> descriptionText.trim().isNotEmpty() && rateText.trim().isNotEmpty()
                            2 -> uploadFileName != null
                            3 -> uploadSelfieSelected
                            4 -> termsAccepted
                            else -> true
                        }

                        Button(
                            onClick = {
                                if (stepValidated) {
                                    if (effectiveStep < 4) {
                                        currentStep++
                                    } else {
                                        // Save fields, raise pending state to ViewModel, and transition screen
                                        val hr = rateText.toDoubleOrNull() ?: 650.0
                                        viewModel.saveProfessionalProfile(
                                            category = selectedCategory,
                                            description = descriptionText,
                                            ratePerHour = hr,
                                            location = "Maputo",
                                            specialization = "",
                                            yearsOfExperience = 5
                                        )
                                        viewModel.setCandidacyState("B") // Change candidacyState!
                                        currentStep = 5 // Go to confirmation page
                                    }
                                } else {
                                    // Soft failure guidance
                                    val errTip = when (effectiveStep) {
                                        1 -> "Preencha a biografia e o preço por hora."
                                        2 -> "Por favor toque para carregar o seu BI para simulação."
                                        3 -> "Por favor de um toque no enquadramento facial para simular selfie."
                                        4 -> "Aceite os termos para prosseguir."
                                        else -> ""
                                    }
                                    viewModel.setErrorMessage(errTip)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stepValidated) Color(0xFF4DD99B) else Color(0xFF1E1E1E),
                                contentColor = if (stepValidated) Color(0xFF0C241B) else Color.White.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    // Confirmation Screen direct "Voltar ao perfil" action at bottom:
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Button(
                            onClick = {
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4DD99B),
                                contentColor = Color(0xFF0C241B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Voltar ao perfil",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


// =========================================================================
// MUTABLE DUAL-MODE DISPATCHED PROFESSIONAL WORKSPACE SCREENS
// =========================================================================

@Composable
fun ProfessionalDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (String) -> Unit
) {
    val isOnline by viewModel.isProfessionalOnline.collectAsState()
    val bookingsAll by viewModel.bookings.collectAsState()
    val myBookings = remember(bookingsAll) {
        bookingsAll.filter { it.professionalId == 5L }
    }
    val pendingBookings = remember(myBookings) {
        myBookings.filter { it.status == "Pendente" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Greeting Header with Avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bom dia, Ronaldo 👋",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                // Squarish rounded avatar with glowing green border
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF133125))
                            .border(2.dp, Color(0xFF4DD99B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RL",
                            color = Color(0xFF4DD99B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    // Small status dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFF4DD99B), CircleShape)
                            .border(2.dp, Color(0xFF161616), CircleShape)
                    )
                }
            }
        }

        item {
            // Dual-mode toggle pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F0F0F))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modo Cliente
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.setActiveProfileMode("Cliente") }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Modo Cliente",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                // Modo Profissional (Active)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF4DD99B))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        tint = Color(0xFF0C241B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Modo Profissional",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0C241B)
                    )
                }
            }
        }

        item {
            // Online switch card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Disponível para serviços",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isOnline) "Clientes podem encontrar-te" else "Inativo · Ficas invisível para buscas",
                            fontSize = 12.sp,
                            color = if (isOnline) Color(0xFF4DD99B) else Color.White.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { viewModel.toggleProfessionalOnline() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0C241B),
                            checkedTrackColor = Color(0xFF4DD99B),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color(0xFF2C2C2C)
                        )
                    )
                }
            }
        }

        item {
            // Earnings Card with custom bar chart
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GANHOS ESTE MÊS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Junho 2026",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "12 450 MT",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4DD99B)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF133125))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "▲ +18% vs Maio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4DD99B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Minimal aesthetic custom Bar Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(20.dp, 35.dp, 15.dp, 50.dp, 40.dp, 25.dp, 75.dp)
                        heights.forEachIndexed { idx, ht ->
                            val isCurrent = idx == heights.lastIndex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(ht)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (isCurrent) Color(0xFF4DD99B) else Color(0xFF133125))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "S${idx + 1}",
                                    fontSize = 10.sp,
                                    color = if (isCurrent) Color(0xFF4DD99B) else Color.White.copy(alpha = 0.4f),
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // 2x2 Statistics Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBox(
                        value = "8",
                        label = "Serviços este mês",
                        indicator = "▲ +3 vs Maio",
                        indicatorColor = Color(0xFF4DD99B),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "5.0 ★",
                        label = "Avaliação média",
                        indicator = "63 avaliações",
                        indicatorColor = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBox(
                        value = "${pendingBookings.size}",
                        label = "Pedidos pendentes",
                        indicator = "Responder agora",
                        indicatorColor = Color(0xFFFFB74D),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "94%",
                        label = "Taxa de conclusão",
                        indicator = "▲ Excelente",
                        indicatorColor = Color(0xFF4DD99B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            // Recent bookings category row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PEDIDOS RECENTES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Ver todos",
                    fontSize = 13.sp,
                    color = Color(0xFF4DD99B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToTab("pedidos_tab") }
                )
            }
        }

        if (myBookings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum pedido recebido ainda", color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        } else {
            items(myBookings.take(2)) { bk ->
                ProfessionalBookingRow(booking = bk, onAction = { onNavigateToTab("pedidos_tab") })
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatBox(
    value: String,
    label: String,
    indicator: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = indicator,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
        }
    }
}

@Composable
fun ProfessionalBookingRow(
    booking: Booking,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                val initials = if (booking.customerName.isNotEmpty()) {
                    booking.customerName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                } else {
                    "CL"
                }
                Text(
                    text = initials,
                    color = Color(0xFF4DD99B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (booking.customerName.isNotEmpty()) booking.customerName else "Cliente Muteko",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${booking.totalAmount.toInt()} MT",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4DD99B),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = booking.notes.take(30) + (if (booking.notes.length > 30) "..." else ""),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val (statusText, tagColor, tagBg) = when(booking.status) {
                        "Pendente" -> Triple("Novo", Color(0xFFFFB74D), Color(0xFF383015))
                        "Confirmado" -> Triple("Aceite", Color(0xFF4DD99B), Color(0xFF133125))
                        "Concluído" -> Triple("Concluído", Color.White.copy(alpha = 0.5f), Color(0xFF2C2C2C))
                        else -> Triple(booking.status, Color.White, Color.Gray)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalRequestsScreen(
    viewModel: MainViewModel,
    onOpenChat: (Long) -> Unit
) {
    val bookingsAll by viewModel.bookings.collectAsState()
    val myBookings = remember(bookingsAll) {
        bookingsAll.filter { it.professionalId == 5L }
    }

    var selectedFilter by remember { mutableStateOf("Todos") }
    val filteredBookings = remember(myBookings, selectedFilter) {
        when (selectedFilter) {
            "Novos" -> myBookings.filter { it.status == "Pendente" }
            "Aceites" -> myBookings.filter { it.status == "Confirmado" }
            "Concluídos" -> myBookings.filter { it.status == "Concluído" }
            else -> myBookings
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        Text(
            text = "Pedidos recebidos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Novos", "Aceites", "Concluídos").forEach { flt ->
                val isSel = flt == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) Color(0xFF4DD99B) else Color(0xFF1E1E1E))
                        .border(1.dp, if (isSel) Color(0xFF4DD99B) else Color(0xFF2E2E2E), RoundedCornerShape(12.dp))
                        .clickable { selectedFilter = flt }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = flt,
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color(0xFF0C241B) else Color.White
                    )
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Assignment,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum pedido nesta secção",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredBookings) { bk ->
                    ProfessionalDetailedRequestCard(
                        booking = bk,
                        onAccept = { viewModel.confirmBooking(bk.id) },
                        onDecline = { viewModel.cancelBooking(bk.id) },
                        onComplete = { viewModel.completeBooking(bk.id) },
                        onChat = { onOpenChat(bk.professionalId) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ProfessionalDetailedRequestCard(
    booking: Booking,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onComplete: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF133125)),
                        contentAlignment = Alignment.Center
                    ) {
                        val init = booking.customerName.split(" ").map { it.uppercase().take(1) }.joinToString("").take(2)
                        Text(init, color = Color(0xFF4DD99B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = booking.customerName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = booking.professionalCategory,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                val (statusText, tagColor, tagBg) = when(booking.status) {
                    "Pendente" -> Triple("Novo", Color(0xFFFFB74D), Color(0xFF383015))
                    "Confirmado" -> Triple("Aceite", Color(0xFF4DD99B), Color(0xFF133125))
                    "Concluído" -> Triple("Concluído", Color.White.copy(alpha = 0.5f), Color(0xFF2C2C2C))
                    "Cancelado" -> Triple("Recusado", Color(0xFFFF5252), Color(0xFF3C1F1F))
                    else -> Triple(booking.status, Color.White, Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.DateRange, contentDescription = null, tint = Color(0xFF4DD99B), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val isToday = booking.date == "2026-06-19"
                        val dayLabel = if (isToday) "Hoje, 19 Jun" else "Amanhã, 20 Jun"
                        Text(
                            text = "$dayLabel · ${booking.time} · Estimativa ${booking.hours}h",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4DD99B), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = booking.address,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(imageVector = Icons.Outlined.Assignment, contentDescription = null, tint = Color(0xFF4DD99B), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = booking.notes,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total a receber:", color = Color.White.copy(0.4f), fontSize = 13.sp)
                Text(
                    text = "${booking.totalAmount.toInt()} MT",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4DD99B),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (booking.status) {
                "Pendente" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFFF5252)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Recusar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD99B), contentColor = Color(0xFF0C241B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aceitar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                "Confirmado" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onChat,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color(0xFF4DD99B)),
                            border = BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF4DD99B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD99B), contentColor = Color(0xFF0C241B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Concluir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ProfessionalAvailabilityScreen(
    viewModel: MainViewModel
) {
    val selectedDay by viewModel.selectedCalendarDay.collectAsState()
    val agendaSlots by viewModel.agendaSlots.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Disponibilidade",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Mês anterior",
                        tint = Color(0xFF4DD99B),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Junho 2026",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Mês seguinte",
                        tint = Color(0xFF4DD99B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { dayName ->
                        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                            Text(dayName, color = Color.White.copy(0.3f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = 30
                var dayCounter = 1

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (week in 0 until 5) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (weekday in 0 until 7) {
                                val absoluteIndex = week * 7 + weekday
                                if (absoluteIndex < 1 || dayCounter > daysInMonth) {
                                    Box(modifier = Modifier.size(36.dp)) {}
                                } else {
                                    val currentDay = dayCounter
                                    val isSelected = currentDay == selectedDay
                                    val isAvailable = currentDay in listOf(19, 20, 23, 24, 25, 26, 27, 29, 30)
                                    val isOccupied = currentDay == 28

                                    val bg = when {
                                        isSelected -> Color(0xFF4DD99B)
                                        isAvailable -> Color(0xFF133125)
                                        isOccupied -> Color(0xFF381A1A)
                                        else -> Color.Transparent
                                    }

                                    val border = when {
                                        isSelected -> BorderStroke(2.dp, Color(0xFF4DD99B))
                                        isAvailable -> BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.3f))
                                        isOccupied -> BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
                                        else -> null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bg)
                                            .then(if (border != null) Modifier.border(border.width, border.brush, RoundedCornerShape(10.dp)) else Modifier)
                                            .clickable { viewModel.selectCalendarDay(currentDay) }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$currentDay",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = when {
                                                    isSelected -> Color(0xFF0C241B)
                                                    isAvailable -> Color(0xFF4DD99B)
                                                    isOccupied -> Color(0xFFFF5252)
                                                    else -> Color.White
                                                }
                                            )
                                            if (currentDay in listOf(19, 20, 23, 28)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(if (isSelected) Color(0xFF0C241B) else if (isOccupied) Color(0xFFFF5252) else Color(0xFF4DD99B), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                    dayCounter++
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = Color(0xFF4DD99B), label = "Disponível", outline = true)
            LegendItem(color = Color.White.copy(alpha = 0.3f), label = "Indisponível", outline = false)
            LegendItem(color = Color(0xFFFF5252), label = "Ocupado", outline = true)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "HORÁRIOS — " + when(selectedDay) {
                19 -> "HOJE, 19 JUN"
                20 -> "AMANHÃ, 20 JUN"
                else -> "$selectedDay JUN"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        val slotsList = listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slotsList.forEach { time ->
                val slotKey = "${selectedDay}_$time"
                val slotStatus = agendaSlots[slotKey] ?: "Livre"

                val containerBg = when (slotStatus) {
                    "Livre" -> Color(0xFF133125).copy(alpha = 0.3f)
                    "Bloqueado" -> Color(0xFF1E1E1E)
                    "Reservado" -> Color(0xFF1A1A1A)
                    else -> Color.Transparent
                }

                val border = when (slotStatus) {
                    "Livre" -> BorderStroke(1.dp, Color(0xFF4DD99B).copy(alpha = 0.3f))
                    "Bloqueado" -> BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
                    "Reservado" -> BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    else -> null
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBg)
                        .then(if (border != null) Modifier.border(border.width, border.brush, RoundedCornerShape(12.dp)) else Modifier)
                        .clickable { viewModel.toggleAgendaSlot(selectedDay, time) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = time,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = when (slotStatus) {
                                "Livre" -> "Disponível para agendamento"
                                "Bloqueado" -> "Horário bloqueado"
                                "Reservado" -> "Reservado · Serviço agendado"
                                else -> "Sob consulta"
                            },
                            fontSize = 12.sp,
                            color = when (slotStatus) {
                                "Livre" -> Color(0xFF4DD99B)
                                "Bloqueado" -> Color(0xFFFF5252)
                                else -> Color.White.copy(alpha = 0.4f)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (slotStatus) {
                                    "Livre" -> Color(0xFF133125)
                                    "Bloqueado" -> Color(0xFF3C1F1F)
                                    else -> Color(0xFF2C2C2C)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = slotStatus.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (slotStatus) {
                                "Livre" -> Color(0xFF4DD99B)
                                "Bloqueado" -> Color(0xFFFF5252)
                                else -> Color.White.copy(alpha = 0.5f)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    outline: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (outline) color.copy(alpha = 0.2f) else color)
                .then(if (outline) Modifier.border(1.dp, color, RoundedCornerShape(4.dp)) else Modifier)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
fun ProfessionalProfileTabScreen(
    viewModel: MainViewModel,
    onBackToClient: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF133125))
                        .border(3.dp, Color(0xFF4DD99B), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RL",
                        color = Color(0xFF4DD99B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF4DD99B), CircleShape)
                        .border(2.dp, Color(0xFF161616), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verificado",
                        tint = Color(0xFF0C241B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ronaldo Langa",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Chaveiro · Serralheiro · Maputo",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF262626)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "142",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4DD99B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Serviços",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF2A2A2A))
                )

                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "5.0",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(15.dp).padding(start = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Avaliação média",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF2A2A2A))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "94%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4DD99B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Taxa Conclusão",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CONTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Outlined.Person,
                        title = "Informações pessoais",
                        onClick = { }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.Verified,
                        title = "Certificados & Credenciais",
                        badge = "3",
                        onClick = { }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.Work,
                        title = "Portfólio de trabalhos",
                        badge = "3 fotos",
                        onClick = { }
                    )
                }
            }

            Text(
                text = "PAGAMENTOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Outlined.Smartphone,
                        title = "M-Pesa",
                        badge = "+258 84 ...",
                        onClick = { }
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Ganhos & Transferências",
                        badge = "12 450 MT",
                        onClick = { }
                    )
                }
            }

            Text(
                text = "MODO DE TRABALHO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626))
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.Sync,
                        title = "Mudar para Modo Cliente",
                        onClick = onBackToClient
                    )
                    Divider(color = Color(0xFF262626), thickness = 1.dp)
                    ProfileMenuRow(
                        icon = Icons.Default.ExitToApp,
                        title = "Terminar sessão",
                        onClick = { viewModel.setLoggedIn(false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

