package com.alowork.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.alowork.app.ui.theme.AloworkTheme
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AloworkTheme {
                AloworkApp()
            }
        }
    }
}

@Composable
fun AloworkApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(AppScreen.WorkerSignUp) }
    var pendingAdminReviewWorker by remember { mutableStateOf<String?>(null) }
    var pendingAdminReviewPeriod by remember { mutableStateOf<String?>(null) }
    var manualHoursSubmission by remember { mutableStateOf(loadManualHoursSubmission(context)) }
    var gpsShiftSubmission by remember { mutableStateOf(loadWorkerGpsShiftSubmission(context)) }
    var pendingGpsShiftSeconds by remember { mutableStateOf<Long?>(null) }
    var selectedWorkerDayDetail by remember { mutableStateOf(defaultAdjustedWorkerDayDetail()) }
    var workerChatMessages by remember { mutableStateOf(loadWorkerChatMessages(context)) }
    var workerShiftPhotoUris by remember { mutableStateOf(emptyList<Uri>()) }
    var workerAccountApproval by remember { mutableStateOf(loadWorkerAccountApproval(context)) }
    var weekendPremiumSettings by remember { mutableStateOf(loadWeekendPremiumSettings(context)) }
    var workerLanguage by remember { mutableStateOf(loadWorkerLanguage(context)) }
    var adminCompanyProfile by remember { mutableStateOf(loadAdminCompanyProfile(context)) }
    var selectedEmployerName by remember { mutableStateOf(loadSelectedEmployerName(context)) }
    var pendingWorkerEmail by remember { mutableStateOf("") }
    var workerEmployers by remember {
        mutableStateOf(loadWorkerEmployers(context))
    }
    val selectedEmployer = workerEmployers.firstOrNull { it.name == selectedEmployerName }
        ?: defaultWorkerEmployers().first()
    val selectedEmployerHourlyRate = selectedEmployer.hourlyRateValue()

    fun openWorkerTab(tab: WorkerTab) {
        screen = when (tab) {
            WorkerTab.Calendar -> AppScreen.WorkerCalendarEarnings
            WorkerTab.History -> AppScreen.WorkerHistoryOverview
            WorkerTab.Alerts -> AppScreen.WorkerNotifications
            WorkerTab.Profile -> AppScreen.WorkerProfile
        }
    }

    fun openAdminHome() {
        screen = AppScreen.AdminDashboardHome
    }

    fun openAdminHours() {
        pendingAdminReviewWorker = null
        pendingAdminReviewPeriod = null
        screen = AppScreen.AdminHoursApprovalQueue
    }

    fun openAdminHoursForWorker(workerName: String, period: String? = null) {
        pendingAdminReviewWorker = workerName
        pendingAdminReviewPeriod = period
        screen = AppScreen.AdminHoursApprovalQueue
    }

    fun openAdminTeam() {
        screen = AppScreen.AdminTeam
    }

    fun openAdminPlaces() {
        screen = AppScreen.AdminWorkLocations
    }

    fun openAdminWeekendPremium() {
        screen = AppScreen.AdminWeekendPremiumSettings
    }

    when (screen) {
        AppScreen.WorkerSignUp -> WorkerSignUpScreen(
            onAccountCreated = { fullName, email, companyCode ->
                submitWorkerSignupForApproval(
                    context = context,
                    fullName = fullName,
                    email = email,
                    companyCode = companyCode,
                )
                pendingWorkerEmail = email
                screen = AppScreen.WorkerAwaitingApproval
            },
            onLoginSelected = {
                screen = AppScreen.WorkerLogin
            },
            onRegisterCompany = {
                screen = AppScreen.WebRegisterCompanyDetails
            },
        )

        AppScreen.WorkerLogin -> WorkerLoginScreen(
            onLogin = { email ->
                pendingWorkerEmail = email
                val worker = findAdminWorkerByEmail(context, email)
                if (worker?.status == "Active") {
                    val approval = WorkerAccountApproval(name = worker.name, email = worker.email)
                    workerAccountApproval = approval
                    saveWorkerAccountApproval(context, approval)
                    screen = AppScreen.WorkerGpsClockIn
                } else {
                    screen = AppScreen.WorkerAwaitingApproval
                }
            },
            onCreateAccount = {
                screen = AppScreen.WorkerSignUp
            },
            onRegisterCompany = {
                screen = AppScreen.WebRegisterCompanyDetails
            },
        )

        AppScreen.WorkerAwaitingApproval -> WorkerAwaitingApprovalScreen(
            isApproved = isWorkerApproved(context, pendingWorkerEmail),
            onCheckApproval = {
                isWorkerApproved(context, pendingWorkerEmail)
            },
            onApprovalReceived = {
                screen = AppScreen.WorkerLocationPermission
            },
        )

        AppScreen.WorkerLocationPermission -> WorkerLocationPermissionScreen(
            onLocationAllowed = {
                screen = AppScreen.WorkerGpsClockIn
            },
            onManualEntry = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
        )

        AppScreen.WorkerGpsClockIn -> GpsClockInScreen(
            onClockIn = {
                pendingGpsShiftSeconds = null
                workerShiftPhotoUris = emptyList()
                screen = AppScreen.WorkerShiftInProgress
            },
            onManualEntry = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerShiftInProgress -> GpsShiftInProgressScreen(
            hourlyRate = selectedEmployerHourlyRate,
            onClockOut = { elapsedSeconds ->
                pendingGpsShiftSeconds = elapsedSeconds
                screen = AppScreen.WorkerShiftPhotos
            },
        )

        AppScreen.WorkerShiftPhotos -> WorkerShiftPhotosScreen(
            photoUris = workerShiftPhotoUris,
            onBack = {
                screen = AppScreen.WorkerShiftInProgress
            },
            onPhotoAdded = { uri ->
                workerShiftPhotoUris = (workerShiftPhotoUris + uri).distinct().take(4)
            },
            onPhotoRemoved = { uri ->
                workerShiftPhotoUris = workerShiftPhotoUris.filterNot { it == uri }
            },
            onSave = {
                val elapsedSeconds = pendingGpsShiftSeconds ?: (3.hours + 24.minutes + 11.seconds)
                val hours = elapsedSeconds / 3600.0
                val submission = WorkerGpsShiftSubmission(
                    hours = formatHours(hours),
                    pay = formatEuro(hours * selectedEmployerHourlyRate),
                    photoCount = workerShiftPhotoUris.size,
                    photoUris = workerShiftPhotoUris,
                )
                gpsShiftSubmission = submission
                saveWorkerGpsShiftSubmission(context, submission)
                saveWorkerSubmittedHoursForAdmin(
                    context = context,
                    period = "Today",
                    hours = submission.hours,
                    pay = submission.pay,
                    proofLabel = proofLabelForPhotoCount(submission.photoCount),
                )
                pendingGpsShiftSeconds = null
                workerShiftPhotoUris = emptyList()
                screen = AppScreen.WorkerGpsClockIn
            },
        )

        AppScreen.WorkerCalendarEarnings -> WorkerCalendarEarningsScreen(
            manualSubmission = manualHoursSubmission,
            gpsShiftSubmission = gpsShiftSubmission,
            onDaySelected = {
                screen = AppScreen.WorkerDayViewAdjusted
            },
            onLogHours = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
            onProfileSelected = {
                screen = AppScreen.WorkerProfile
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerLocationDenied -> GpsLocationDeniedScreen(
            onLocationEnabled = {
                screen = AppScreen.WorkerGpsClockIn
            },
            onManualEntry = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
        )

        AppScreen.WorkerNotifications -> WorkerNotificationsScreen(
            sentMessages = workerChatMessages,
            adminRequests = loadWorkerAdminHoursRequests(context),
            accountApproval = workerAccountApproval,
            manualSubmission = manualHoursSubmission,
            gpsShiftSubmission = gpsShiftSubmission,
            onOpenShiftDetail = { dayDetail ->
                selectedWorkerDayDetail = dayDetail
                screen = AppScreen.WorkerDayViewAdjusted
            },
            onOpenShiftChat = { dayDetail ->
                selectedWorkerDayDetail = dayDetail
                screen = AppScreen.WorkerChatWithEmployer
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerProfile -> WorkerProfileScreen(
            language = workerLanguage,
            selectedEmployer = selectedEmployer,
            onSwitchEmployer = {
                screen = AppScreen.WorkerSwitchEmployer
            },
            onAddEmployer = {
                screen = AppScreen.WorkerAddEmployer
            },
            onChangeLanguage = {
                screen = AppScreen.WorkerLanguageSettings
            },
            onChangePassword = {
                screen = AppScreen.WorkerChangePassword
            },
            onLogout = {
                manualHoursSubmission = null
                clearManualHoursSubmission(context)
                gpsShiftSubmission = null
                clearWorkerGpsShiftSubmission(context)
                pendingGpsShiftSeconds = null
                selectedWorkerDayDetail = defaultAdjustedWorkerDayDetail()
                workerChatMessages = emptyList()
                clearWorkerChatMessages(context)
                workerShiftPhotoUris = emptyList()
                workerAccountApproval = null
                clearWorkerAccountApproval(context)
                selectedEmployerName = "Bakkerij Jansen"
                workerEmployers = defaultWorkerEmployers()
                clearWorkerEmployers(context)
                screen = AppScreen.WorkerSignUp
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerLanguageSettings -> WorkerLanguageSettingsScreen(
            selectedLanguage = workerLanguage,
            onBack = {
                screen = AppScreen.WorkerProfile
            },
            onLanguageSelected = { language ->
                workerLanguage = language
                saveWorkerLanguage(context, language)
                screen = AppScreen.WorkerProfile
            },
        )

        AppScreen.WorkerChangePassword -> WorkerChangePasswordScreen(
            onBack = {
                screen = AppScreen.WorkerProfile
            },
            onPasswordChanged = {
                screen = AppScreen.WorkerProfile
            },
        )

        AppScreen.WorkerHistoryOverview -> WorkerHistoryOverviewScreen(
            manualSubmission = manualHoursSubmission,
            gpsShiftSubmission = gpsShiftSubmission,
            onDaySelected = { dayDetail ->
                selectedWorkerDayDetail = dayDetail
                screen = AppScreen.WorkerDayViewAdjusted
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerDayViewAdjusted -> WorkerDayViewAdjustedScreen(
            dayDetail = selectedWorkerDayDetail,
            onBack = {
                screen = AppScreen.WorkerHistoryOverview
            },
            onAskQuestion = {
                screen = AppScreen.WorkerChatWithEmployer
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerChatWithEmployer -> WorkerChatWithEmployerScreen(
            dayDetail = selectedWorkerDayDetail,
            sentMessages = workerChatMessages,
            onBack = {
                screen = AppScreen.WorkerDayViewAdjusted
            },
            onSendMessage = { message ->
                workerChatMessages = workerChatMessages + message
                saveWorkerChatMessages(context, workerChatMessages)
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerLogHoursDayDetail -> WorkerLogHoursDayDetailScreen(
            hourlyRate = selectedEmployerHourlyRate,
            onBack = {
                screen = AppScreen.WorkerGpsClockIn
            },
            onSubmitted = { hours, pay ->
                val submission = WorkerManualHoursSubmission(
                    hours = hours,
                    pay = pay,
                )
                manualHoursSubmission = submission
                saveManualHoursSubmission(context, submission)
                saveWorkerSubmittedHoursForAdmin(
                    context = context,
                    period = "17 Jun",
                    hours = submission.hours,
                    pay = submission.pay,
                )
                screen = AppScreen.WorkerHistoryOverview
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerAddEmployer -> WorkerAddEmployerScreen(
            onBack = {
                screen = AppScreen.WorkerSwitchEmployer
            },
            onCompanyAdded = { companyCode ->
                val newEmployer = resolveWorkerEmployerByCode(companyCode)
                val existingEmployer = workerEmployers.firstOrNull { it.name == newEmployer.name }
                workerEmployers = if (existingEmployer == null) {
                    workerEmployers + newEmployer
                } else {
                    workerEmployers
                }
                saveWorkerEmployers(context, workerEmployers)
                selectedEmployerName = existingEmployer?.name ?: newEmployer.name
                saveSelectedEmployerName(context, selectedEmployerName)
                screen = AppScreen.WorkerSwitchEmployer
            },
        )

        AppScreen.WorkerSwitchEmployer -> WorkerSwitchEmployerScreen(
            employers = workerEmployers,
            selectedEmployerName = selectedEmployerName,
            onBack = {
                screen = AppScreen.WorkerProfile
            },
            onAddCompany = {
                screen = AppScreen.WorkerAddEmployer
            },
            onEmployerSelected = { employer ->
                selectedEmployerName = employer.name
                saveSelectedEmployerName(context, selectedEmployerName)
            },
        )

        AppScreen.WebRegisterCompanyDetails -> WebRegisterCompanyDetailsScreen(
            companyProfile = adminCompanyProfile,
            onContinue = { companyName, industry, email ->
                val updatedProfile = adminCompanyProfile.copy(
                    companyName = companyName,
                    industry = industry,
                    workEmail = email,
                )
                adminCompanyProfile = updatedProfile
                saveAdminCompanyProfile(context, updatedProfile)
                screen = AppScreen.WebRegisterChoosePlan
            },
        )

        AppScreen.WebRegisterChoosePlan -> WebRegisterChoosePlanScreen(
            selectedPlan = adminCompanyProfile.plan,
            onContinue = { plan ->
                val updatedProfile = adminCompanyProfile.copy(plan = plan)
                adminCompanyProfile = updatedProfile
                saveAdminCompanyProfile(context, updatedProfile)
                screen = AppScreen.WebRegisterAdminAccount
            },
        )

        AppScreen.WebRegisterAdminAccount -> WebRegisterAdminAccountScreen(
            companyProfile = adminCompanyProfile,
            onAccountCreated = { adminName, adminEmail ->
                val updatedProfile = adminCompanyProfile.copy(
                    adminName = adminName,
                    adminEmail = adminEmail,
                )
                adminCompanyProfile = updatedProfile
                saveAdminCompanyProfile(context, updatedProfile)
                screen = AppScreen.WebRegisterSuccessCode
            },
        )

        AppScreen.WebRegisterSuccessCode -> WebRegisterSuccessCodeScreen(
            onOpenDashboard = {
                screen = AppScreen.AdminDashboardHome
            },
        )

        AppScreen.AdminDashboardHome -> AdminDashboardHomeScreen(
            companyProfile = adminCompanyProfile,
            onOpenApprovalQueue = {
                openAdminHours()
            },
            onReviewWorker = { workerName, period ->
                openAdminHoursForWorker(workerName, period)
            },
            onOpenHome = {
                openAdminHome()
            },
            onOpenTeam = {
                openAdminTeam()
            },
            onOpenWorkLocations = {
                openAdminPlaces()
            },
            onOpenWeekendPremium = {
                openAdminWeekendPremium()
            },
        )

        AppScreen.AdminHoursApprovalQueue -> AdminHoursApprovalQueueScreen(
            initialSelectedWorker = pendingAdminReviewWorker,
            initialSelectedPeriod = pendingAdminReviewPeriod,
            onInitialSelectionConsumed = {
                pendingAdminReviewWorker = null
                pendingAdminReviewPeriod = null
            },
            onWorkerMessagesChanged = { messages ->
                workerChatMessages = messages
            },
            onOpenHome = {
                openAdminHome()
            },
            onOpenHours = {
                openAdminHours()
            },
            onOpenTeam = {
                openAdminTeam()
            },
            onOpenWorkLocations = {
                openAdminPlaces()
            },
            onOpenWeekendPremium = {
                openAdminWeekendPremium()
            },
        )

        AppScreen.AdminWorkLocations -> AdminWorkLocationsScreen(
            onOpenHome = {
                openAdminHome()
            },
            onOpenHours = {
                openAdminHours()
            },
            onOpenTeam = {
                openAdminTeam()
            },
            onOpenWorkLocations = {
                openAdminPlaces()
            },
            onOpenWeekendPremium = {
                openAdminWeekendPremium()
            },
        )

        AppScreen.AdminTeam -> AdminTeamScreen(
            onWorkerApproved = { name, email ->
                val approval = WorkerAccountApproval(name = name, email = email)
                workerAccountApproval = approval
                saveWorkerAccountApproval(context, approval)
            },
            onOpenHome = {
                openAdminHome()
            },
            onOpenHours = {
                openAdminHours()
            },
            onOpenTeam = {
                openAdminTeam()
            },
            onOpenWorkLocations = {
                openAdminPlaces()
            },
            onOpenWeekendPremium = {
                openAdminWeekendPremium()
            },
        )

        AppScreen.AdminWeekendPremiumSettings -> AdminWeekendPremiumSettingsScreen(
            settings = weekendPremiumSettings,
            onSettingsChanged = {
                weekendPremiumSettings = it
                saveWeekendPremiumSettings(context, it)
            },
            onBack = {
                openAdminHome()
            },
            onSave = {
                openAdminHome()
            },
        )
    }
}

@Composable
fun WorkerSignUpScreen(
    modifier: Modifier = Modifier,
    onAccountCreated: (String, String, String) -> Unit = { _, _, _ -> },
    onLoginSelected: () -> Unit = {},
    onRegisterCompany: () -> Unit = {},
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("Sven de Vries") }
    var email by remember { mutableStateOf("sven@email.nl") }
    var companyCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("password") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 80.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Create account",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Create an account. Your employer approves it before\nyou can start.",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SignUpField(
                label = "Full name",
                value = fullName,
                onValueChange = { fullName = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = "Email address",
                value = email,
                onValueChange = { email = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = "Company code",
                value = companyCode,
                onValueChange = { companyCode = it },
                placeholder = "Received from your employer",
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                isPassword = true,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    val message = when {
                        fullName.isBlank() -> "Enter your full name"
                        email.isBlank() -> "Enter your email address"
                        companyCode.isBlank() -> "Enter your company code"
                        password.isBlank() -> "Enter a password"
                        else -> null
                    }
                    if (message == null) {
                        onAccountCreated(fullName.trim(), email.trim(), companyCode.trim())
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Create account",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Already have an account?",
                    color = Color(0xFF8C8C91),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                TextButton(
                    onClick = onLoginSelected,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 4.dp,
                        vertical = 0.dp,
                    ),
                ) {
                    Text(
                        text = "Log in",
                        color = Color(0xFF111116),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            TextButton(
                onClick = onRegisterCompany,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp,
                    vertical = 0.dp,
                ),
            ) {
                Text(
                    text = "Register a company",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun WorkerLoginScreen(
    modifier: Modifier = Modifier,
    onLogin: (String) -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onRegisterCompany: () -> Unit = {},
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("sven@email.nl") }
    var password by remember { mutableStateOf("password") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 80.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Log in",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Log in to clock hours, view shifts, and send updates\nto your employer.",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SignUpField(
                label = "Email address",
                value = email,
                onValueChange = { email = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                isPassword = true,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    val message = when {
                        email.isBlank() -> "Enter your email address"
                        password.isBlank() -> "Enter your password"
                        else -> null
                    }
                    if (message == null) {
                        onLogin(email.trim())
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Log in",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "New to Alowork?",
                    color = Color(0xFF8C8C91),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                TextButton(
                    onClick = onCreateAccount,
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 0.dp,
                    ),
                ) {
                    Text(
                        text = "Create account",
                        color = Color(0xFF111116),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            TextButton(
                onClick = onRegisterCompany,
                contentPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 0.dp,
                ),
            ) {
                Text(
                    text = "Register a company",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SignUpField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            textStyle = TextStyle(
                color = Color(0xFF17171B),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFB5B5B5),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color(0xFFDADAD5),
                unfocusedBorderColor = Color(0xFFE1E1DC),
                cursorColor = Color(0xFF111116),
            ),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
        )
    }
}

@Composable
fun WebRegisterCompanyDetailsScreen(
    modifier: Modifier = Modifier,
    companyProfile: AdminCompanyProfile = defaultAdminCompanyProfile(),
    onContinue: (String, String, String) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    var companyName by remember { mutableStateOf(companyProfile.companyName) }
    var industry by remember { mutableStateOf(companyProfile.industry) }
    var email by remember { mutableStateOf(companyProfile.workEmail) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        WebRegisterSidePanel(
            activeStep = "Company",
            modifier = Modifier.width(132.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 44.dp, bottom = 22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Register your company",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start with your company details.",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
                SignUpField(
                    label = "Company name",
                    value = companyName,
                    onValueChange = { companyName = it },
                )
                Spacer(modifier = Modifier.height(14.dp))
                SignUpField(
                    label = "Industry",
                    value = industry,
                    onValueChange = { industry = it },
                )
                Spacer(modifier = Modifier.height(14.dp))
                SignUpField(
                    label = "Work email",
                    value = email,
                    onValueChange = { email = it },
                )
            }

            Button(
                onClick = {
                    val message = when {
                        companyName.isBlank() -> "Enter your company name"
                        industry.isBlank() -> "Enter your industry"
                        email.isBlank() -> "Enter your work email"
                        else -> null
                    }
                    if (message == null) {
                        Toast.makeText(context, "Company details saved", Toast.LENGTH_SHORT).show()
                        onContinue(companyName, industry, email)
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Continue",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun WebRegisterChoosePlanScreen(
    modifier: Modifier = Modifier,
    selectedPlan: String = "Starter",
    onContinue: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var currentPlan by remember { mutableStateOf(selectedPlan) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        WebRegisterSidePanel(
            activeStep = "Plan",
            modifier = Modifier.width(132.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 44.dp, bottom = 22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Choose plan",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select the plan that fits your team.",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
                WebPlanOptionCard(
                    title = "Starter",
                    price = "\u20AC19 / month",
                    detail = "Up to 15 workers",
                    selected = currentPlan == "Starter",
                    onClick = { currentPlan = "Starter" },
                )
                Spacer(modifier = Modifier.height(12.dp))
                WebPlanOptionCard(
                    title = "Business",
                    price = "\u20AC49 / month",
                    detail = "Up to 60 workers",
                    selected = currentPlan == "Business",
                    onClick = { currentPlan = "Business" },
                )
            }

            Button(
                onClick = {
                    Toast.makeText(context, "$currentPlan plan selected", Toast.LENGTH_SHORT).show()
                    onContinue(currentPlan)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Continue",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun WebRegisterAdminAccountScreen(
    modifier: Modifier = Modifier,
    companyProfile: AdminCompanyProfile = defaultAdminCompanyProfile(),
    onAccountCreated: (String, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(companyProfile.adminName) }
    var email by remember { mutableStateOf(companyProfile.adminEmail) }
    var password by remember { mutableStateOf("password") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        WebRegisterSidePanel(
            activeStep = "Admin",
            modifier = Modifier.width(132.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 44.dp, bottom = 22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Create admin account",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Use this account to manage hours and payroll.",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
                SignUpField(
                    label = "Full name",
                    value = fullName,
                    onValueChange = { fullName = it },
                )
                Spacer(modifier = Modifier.height(14.dp))
                SignUpField(
                    label = "Email address",
                    value = email,
                    onValueChange = { email = it },
                )
                Spacer(modifier = Modifier.height(14.dp))
                SignUpField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    isPassword = true,
                )
            }

            Button(
                onClick = {
                    val message = when {
                        fullName.isBlank() -> "Enter your full name"
                        email.isBlank() -> "Enter your email address"
                        password.length < 6 -> "Use at least 6 password characters"
                        else -> null
                    }
                    if (message == null) {
                        Toast.makeText(context, "Admin account created", Toast.LENGTH_SHORT).show()
                        onAccountCreated(fullName, email)
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Create account",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun WebRegisterSuccessCodeScreen(
    modifier: Modifier = Modifier,
    onOpenDashboard: () -> Unit = {},
) {
    val context = LocalContext.current
    val companyCode = "JANS26"

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        WebRegisterSidePanel(
            activeStep = "Admin",
            modifier = Modifier.width(132.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 44.dp, bottom = 22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your company is ready.",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Share this company code with workers so they can create accounts.",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Company code",
                            color = Color(0xFF73737A),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = companyCode,
                            color = Color(0xFF17171B),
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEAF5EF),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = "Workers will wait for your approval before they can clock in.",
                        color = Color(0xFF2F8F63),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    )
                }
            }

            Button(
                onClick = {
                    Toast.makeText(context, "Opening dashboard", Toast.LENGTH_SHORT).show()
                    onOpenDashboard()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Open dashboard",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun AdminDashboardHomeScreen(
    modifier: Modifier = Modifier,
    companyProfile: AdminCompanyProfile = defaultAdminCompanyProfile(),
    onOpenApprovalQueue: () -> Unit = {},
    onReviewWorker: (String, String?) -> Unit = { _, _ -> },
    onOpenHome: () -> Unit = {},
    onOpenTeam: () -> Unit = {},
    onOpenWorkLocations: () -> Unit = {},
    onOpenWeekendPremium: () -> Unit = {},
) {
    val context = LocalContext.current
    val hourRequests = loadAdminHoursRequests(context)
    val adminWorkers = loadAdminWorkers(context)
    val totalHours = hourRequests.sumOf { parseHoursValue(it.hours) }
    val estimatedPayroll = hourRequests.sumOf { parseEuroValue(it.pay) }
    val pendingRequests = hourRequests.filter { it.status != "Approved" }
    val activeWorkerCount = adminWorkers.count { it.status == "Active" }
    val workerMessages = loadWorkerChatMessages(context)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        AdminWebSidebar(
            activeItem = "Home",
            companyInitial = companyProfile.companyName.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
            onHomeClick = onOpenHome,
            onHoursClick = onOpenApprovalQueue,
            onTeamClick = onOpenTeam,
            onPlacesClick = onOpenWorkLocations,
            onSettingsClick = onOpenWeekendPremium,
            modifier = Modifier.width(82.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 34.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dashboard",
                        color = Color(0xFF17171B),
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${companyProfile.companyName} · ${companyProfile.plan}",
                        color = Color(0xFF73737A),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = "June",
                        color = Color(0xFF17171B),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard(
                    label = "Hours",
                    value = formatHours(totalHours),
                    detail = "This month",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AdminMetricCard(
                    label = "Payroll",
                    value = formatWholeEuro(estimatedPayroll),
                    detail = "Estimated",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard(
                    label = "Pending",
                    value = pendingRequests.size.toString(),
                    detail = "Need review",
                    onClick = onOpenApprovalQueue,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AdminMetricCard(
                    label = "Workers",
                    value = activeWorkerCount.toString(),
                    detail = "Active",
                    onClick = onOpenTeam,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Hours approval",
                color = Color(0xFF17171B),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (pendingRequests.isEmpty()) {
                        Text(
                            text = "No pending approvals",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(18.dp),
                        )
                    } else {
                        val previewRequests = pendingRequests.take(3)
                        previewRequests.forEachIndexed { index, request ->
                            AdminApprovalRow(
                                name = request.name,
                                detail = "${request.period} - ${request.hours}",
                                amount = request.pay,
                                proofLabel = request.proofLabel,
                                onClick = {
                                    onReviewWorker(request.name, request.period)
                                },
                            )
                            if (index < previewRequests.lastIndex) {
                                ProfileDivider()
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Worker messages",
                color = Color(0xFF17171B),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (workerMessages.isEmpty()) {
                        Text(
                            text = "No worker messages",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(18.dp),
                        )
                    } else {
                        workerMessages.takeLast(3).asReversed().forEachIndexed { index, message ->
                            AdminWorkerMessageRow(
                                message = message,
                                onClick = {
                                    onReviewWorker(message.workerName, message.shiftTitle)
                                },
                            )
                            if (index < workerMessages.takeLast(3).lastIndex) {
                                ProfileDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminHoursApprovalQueueScreen(
    modifier: Modifier = Modifier,
    initialSelectedWorker: String? = null,
    initialSelectedPeriod: String? = null,
    onInitialSelectionConsumed: () -> Unit = {},
    onWorkerMessagesChanged: (List<WorkerShiftMessage>) -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenHours: () -> Unit = {},
    onOpenTeam: () -> Unit = {},
    onOpenWorkLocations: () -> Unit = {},
    onOpenWeekendPremium: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Pending") }
    var hourRequests by remember {
        mutableStateOf(loadAdminHoursRequests(context))
    }
    var workerMessages by remember {
        mutableStateOf(loadWorkerChatMessages(context))
    }
    var selectedRequest by remember { mutableStateOf<AdminHoursRequest?>(null) }
    val pendingCount = hourRequests.count { it.status != "Approved" }
    val filteredRequests = when (selectedFilter) {
        "Pending" -> hourRequests.filter { it.status != "Approved" }
        "Approved" -> hourRequests.filter { it.status == "Approved" }
        else -> hourRequests
    }

    LaunchedEffect(initialSelectedWorker, initialSelectedPeriod) {
        if (initialSelectedWorker != null) {
            selectedRequest = if (initialSelectedPeriod != null) {
                hourRequests.firstOrNull { request ->
                    request.name == initialSelectedWorker && request.period == initialSelectedPeriod
                }
            } else {
                null
            } ?: hourRequests.firstOrNull { request ->
                request.name == initialSelectedWorker && request.status != "Approved"
            } ?: hourRequests.firstOrNull { request ->
                request.name == initialSelectedWorker
            }
            onInitialSelectionConsumed()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            AdminWebSidebar(
                activeItem = "Hours",
                onHomeClick = onOpenHome,
                onHoursClick = onOpenHours,
                onTeamClick = onOpenTeam,
                onPlacesClick = onOpenWorkLocations,
                onSettingsClick = onOpenWeekendPremium,
                modifier = Modifier.width(82.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 34.dp, bottom = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hours approval",
                            color = Color(0xFF17171B),
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Review submitted hours before payroll",
                            color = Color(0xFF73737A),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                        shadowElevation = 0.dp,
                    ) {
                        Text(
                            text = "$pendingCount pending",
                            color = Color(0xFF17171B),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    AdminQueueFilter(
                        label = "Pending",
                        active = selectedFilter == "Pending",
                        onClick = { selectedFilter = "Pending" },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AdminQueueFilter(
                        label = "Approved",
                        active = selectedFilter == "Approved",
                        onClick = { selectedFilter = "Approved" },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AdminQueueFilter(
                        label = "All",
                        active = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                    shadowElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (filteredRequests.isEmpty()) {
                            Text(
                                text = "No ${selectedFilter.lowercase(Locale.US)} requests",
                                color = Color(0xFF73737A),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(18.dp),
                            )
                        } else {
                            filteredRequests.forEachIndexed { index, request ->
                                AdminApprovalQueueRow(
                                    name = request.name,
                                    period = request.period,
                                    hours = request.hours,
                                    pay = request.pay,
                                    status = request.status,
                                    proofLabel = request.proofLabel,
                                    onReview = {
                                        selectedRequest = request
                                    },
                                )
                                if (index < filteredRequests.lastIndex) {
                                    ProfileDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedRequest?.let { requestToReview ->
            AdminAdjustHoursModal(
                workerName = requestToReview.name,
                period = requestToReview.period,
                submittedHours = requestToReview.hours,
                submittedPay = requestToReview.pay,
                proofLabel = requestToReview.proofLabel,
                messages = workerMessages.filter { message ->
                    message.workerName == requestToReview.name && message.shiftTitle == requestToReview.period
                },
                onDismiss = {
                    selectedRequest = null
                },
                onSendReply = { reply ->
                    val replyMessage = WorkerShiftMessage(
                        workerName = requestToReview.name,
                        shiftTitle = requestToReview.period,
                        shiftStatus = requestToReview.status.workerChatLabel(),
                        shiftSummary = "${requestToReview.hours} - ${requestToReview.status.workerSummaryLabel()}",
                        pay = requestToReview.pay,
                        message = reply,
                        isWorker = false,
                    )
                    workerMessages = workerMessages + replyMessage
                    saveWorkerChatMessages(context, workerMessages)
                    onWorkerMessagesChanged(workerMessages)
                    Toast.makeText(context, "Reply sent to ${requestToReview.name}", Toast.LENGTH_SHORT).show()
                },
                onApprove = {
                    val updatedRequests = hourRequests.map { request ->
                        if (request.isSameAdminRequest(requestToReview)) {
                            request.copy(status = "Approved")
                        } else {
                            request
                        }
                    }
                    hourRequests = updatedRequests
                    saveAdminHoursRequests(context, updatedRequests)
                    Toast.makeText(context, "Hours approved for ${requestToReview.name}", Toast.LENGTH_SHORT).show()
                    selectedRequest = null
                },
                onSave = { updatedHours, updatedPay ->
                    val updatedRequests = hourRequests.map { request ->
                        if (request.isSameAdminRequest(requestToReview)) {
                            request.copy(hours = updatedHours, pay = updatedPay, status = "Adjusted")
                        } else {
                            request
                        }
                    }
                    hourRequests = updatedRequests
                    saveAdminHoursRequests(context, updatedRequests)
                    Toast.makeText(context, "Hours updated for ${requestToReview.name}", Toast.LENGTH_SHORT).show()
                    selectedRequest = null
                },
            )
        }
    }
}

@Composable
fun AdminWorkLocationsScreen(
    modifier: Modifier = Modifier,
    onOpenHome: () -> Unit = {},
    onOpenHours: () -> Unit = {},
    onOpenTeam: () -> Unit = {},
    onOpenWorkLocations: () -> Unit = {},
    onOpenWeekendPremium: () -> Unit = {},
) {
    val context = LocalContext.current
    var locations by remember {
        mutableStateOf(loadAdminWorkLocations(context))
    }
    var selectedLocation by remember { mutableStateOf("Bakery floor") }
    var locationName by remember { mutableStateOf("Bakery floor") }
    var address by remember { mutableStateOf("Lijnbaan 24") }
    var radius by remember { mutableStateOf("120") }
    var addLocationOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        AdminWebSidebar(
            activeItem = "Places",
            onHomeClick = onOpenHome,
            onHoursClick = onOpenHours,
            onTeamClick = onOpenTeam,
            onPlacesClick = onOpenWorkLocations,
            onSettingsClick = onOpenWeekendPremium,
            modifier = Modifier.width(82.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 34.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Work locations",
                        color = Color(0xFF17171B),
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set where workers can clock in",
                        color = Color(0xFF73737A),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }
                Button(
                    onClick = {
                        addLocationOpen = true
                    },
                    modifier = Modifier
                        .width(94.dp)
                        .height(34.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111116),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = "Add place",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(0.9f)) {
                    locations.forEachIndexed { index, location ->
                        AdminLocationListItem(
                            name = location.name,
                            detail = "${location.address} · ${location.radius}m",
                            active = selectedLocation == location.name,
                            onClick = {
                                selectedLocation = location.name
                                locationName = location.name
                                address = location.address
                                radius = location.radius
                            },
                        )
                        if (index < locations.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1.25f)) {
                    AdminLocationMapCard(locationName = selectedLocation)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                        shadowElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AdminAdjustField(
                                label = "Location name",
                                value = locationName,
                                onValueChange = { locationName = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AdminAdjustField(
                                label = "Address",
                                value = address,
                                onValueChange = { address = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AdminAdjustField(
                                label = "Clock-in radius metres",
                                value = radius,
                                onValueChange = { radius = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val updatedLocation = AdminLocation(
                                        name = locationName,
                                        address = address,
                                        radius = radius,
                                    )
                                    locations = locations.map { location ->
                                        if (location.name == selectedLocation) updatedLocation else location
                                    }
                                    saveAdminWorkLocations(context, locations)
                                    selectedLocation = locationName
                                    Toast.makeText(context, "$locationName saved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(7.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF111116),
                                    contentColor = Color.White,
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            ) {
                                Text(
                                    text = "Save location",
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (addLocationOpen) {
        AdminAddLocationDialog(
            onDismiss = { addLocationOpen = false },
            onSaveLocation = { newName, newAddress, newRadius ->
                val newLocation = AdminLocation(
                    name = newName,
                    address = newAddress,
                    radius = newRadius,
                )
                locations = locations.filterNot { it.name == newName } + newLocation
                saveAdminWorkLocations(context, locations)
                selectedLocation = newName
                locationName = newName
                address = newAddress
                radius = newRadius
                Toast.makeText(context, "$newName added", Toast.LENGTH_SHORT).show()
                addLocationOpen = false
            },
        )
    }
}

@Composable
private fun AdminAddLocationDialog(
    onDismiss: () -> Unit,
    onSaveLocation: (String, String, String) -> Unit,
) {
    var locationName by remember { mutableStateOf("New shop") }
    var address by remember { mutableStateOf("Coolsingel 10") }
    var radius by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Add work location",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create a place where workers can clock in",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                AdminAdjustField(
                    label = "Location name",
                    value = locationName,
                    onValueChange = { locationName = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                AdminAdjustField(
                    label = "Address",
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                AdminAdjustField(
                    label = "Clock-in radius metres",
                    value = radius,
                    onValueChange = { radius = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(42.dp),
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onSaveLocation(locationName, address, radius) },
                        modifier = Modifier
                            .width(122.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF111116),
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "Save place",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTeamScreen(
    modifier: Modifier = Modifier,
    onWorkerApproved: (String, String) -> Unit = { _, _ -> },
    onOpenHome: () -> Unit = {},
    onOpenHours: () -> Unit = {},
    onOpenTeam: () -> Unit = {},
    onOpenWorkLocations: () -> Unit = {},
    onOpenWeekendPremium: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedWorker by remember { mutableStateOf("Sven de Vries") }
    var inviteOpen by remember { mutableStateOf(false) }
    var workers by remember {
        mutableStateOf(loadAdminWorkers(context))
    }
    val selectedWorkerDetails = workers.firstOrNull { it.name == selectedWorker } ?: workers.first()
    val role = selectedWorkerDetails.role
    val email = selectedWorkerDetails.email
    val location = selectedWorkerDetails.location
    val reminderStatus = if (selectedWorkerDetails.reminderSent) "Sent" else "Not sent"

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        AdminWebSidebar(
            activeItem = "Team",
            onHomeClick = onOpenHome,
            onHoursClick = onOpenHours,
            onTeamClick = onOpenTeam,
            onPlacesClick = onOpenWorkLocations,
            onSettingsClick = onOpenWeekendPremium,
            modifier = Modifier.width(82.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 34.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Team",
                        color = Color(0xFF17171B),
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage workers and approvals",
                        color = Color(0xFF73737A),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }
                Button(
                    onClick = {
                        inviteOpen = true
                    },
                    modifier = Modifier
                        .width(104.dp)
                        .height(34.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111116),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = "Invite",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    workers.forEachIndexed { index, worker ->
                        AdminWorkerListItem(
                            name = worker.name,
                            detail = "${worker.role} · ${worker.status.lowercase(Locale.US)}",
                            active = selectedWorker == worker.name,
                            status = worker.status,
                            onClick = { selectedWorker = worker.name },
                        )
                        if (index < workers.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
                    shadowElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFFEAF0E9), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = selectedWorker.take(1),
                                    color = Color(0xFF2F8F63),
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedWorker,
                                    color = Color(0xFF17171B),
                                    fontSize = 15.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = email,
                                    color = Color(0xFF73737A),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AdminTeamDetailRow(label = "Role", value = role)
                        ProfileDivider()
                        AdminTeamDetailRow(label = "Default location", value = location)
                        ProfileDivider()
                        AdminTeamDetailRow(label = "Rate", value = selectedWorkerDetails.rate)
                        ProfileDivider()
                        AdminTeamDetailRow(label = "This month", value = selectedWorkerDetails.thisMonthHours)
                        ProfileDivider()
                        AdminTeamDetailRow(label = "Reminder", value = reminderStatus)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val updatedWorkers = workers.map { worker ->
                                        if (worker.name == selectedWorker) {
                                            worker.copy(status = "Active")
                                        } else {
                                            worker
                                        }
                                    }
                                    workers = updatedWorkers
                                    saveAdminWorkers(context, updatedWorkers)
                                    updatedWorkers.firstOrNull { it.name == selectedWorker }
                                        ?.let { worker -> onWorkerApproved(worker.name, worker.email) }
                                    Toast.makeText(context, "$selectedWorker approved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(7.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF111116),
                                    contentColor = Color.White,
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(
                                    text = "Approve",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    val updatedWorkers = workers.map { worker ->
                                        if (worker.name == selectedWorker) {
                                            worker.copy(reminderSent = true)
                                        } else {
                                            worker
                                        }
                                    }
                                    workers = updatedWorkers
                                    saveAdminWorkers(context, updatedWorkers)
                                    Toast.makeText(context, "Reminder sent to $selectedWorker", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(40.dp),
                            ) {
                                Text(
                                    text = if (selectedWorkerDetails.reminderSent) "Sent" else "Remind",
                                    color = Color(0xFF73737A),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (inviteOpen) {
        AdminInviteWorkerDialog(
            onDismiss = { inviteOpen = false },
            onSendInvite = { fullName, inviteEmail, inviteRole, inviteLocation ->
                val newWorker = AdminWorker(
                    name = fullName,
                    role = inviteRole,
                    email = inviteEmail,
                    location = inviteLocation,
                    rate = "\u20AC16.00 / hour",
                    thisMonthHours = "0.0h",
                    status = "Pending",
                    reminderSent = false,
                )
                val updatedWorkers = workers.filterNot { it.email == inviteEmail } + newWorker
                workers = updatedWorkers
                saveAdminWorkers(context, updatedWorkers)
                selectedWorker = fullName
                Toast.makeText(context, "Invite sent to $inviteEmail", Toast.LENGTH_SHORT).show()
                inviteOpen = false
            },
        )
    }
}

@Composable
private fun AdminInviteWorkerDialog(
    onDismiss: () -> Unit,
    onSendInvite: (String, String, String, String) -> Unit,
) {
    var fullName by remember { mutableStateOf("Lotte Smit") }
    var email by remember { mutableStateOf("lotte@email.nl") }
    var role by remember { mutableStateOf("Baker") }
    var location by remember { mutableStateOf("Bakery floor") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Invite worker",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Send an invite to join Bakkerij Jansen",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                AdminAdjustField(
                    label = "Full name",
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                AdminAdjustField(
                    label = "Email address",
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    AdminAdjustField(
                        label = "Role",
                        value = role,
                        onValueChange = { role = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AdminAdjustField(
                        label = "Location",
                        value = location,
                        onValueChange = { location = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(42.dp),
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onSendInvite(fullName, email, role, location) },
                        modifier = Modifier
                            .width(112.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF111116),
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "Send invite",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWeekendPremiumSettingsScreen(
    settings: WeekendPremiumSettings = defaultWeekendPremiumSettings(),
    onSettingsChanged: (WeekendPremiumSettings) -> Unit = {},
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<WeekendPremiumEmployee?>(null) }
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F2)).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 28.sp, modifier = Modifier.width(26.dp).clickable(onClick = onBack))
                Text("Weekend premium", color = Color(0xFF17171B), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Text("Applies to Sunday. Set a default for the whole company\nand override per employee where needed.", color = Color(0xFF73737A), fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE4E4DF))) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Company default", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Premium on Sunday", color = Color(0xFF73737A), fontSize = 10.sp)
                        }
                        Box(Modifier.size(34.dp, 20.dp).background(if (settings.enabled) Color(0xFF1D9E75) else Color(0xFFB8B8BC), CircleShape).clickable { onSettingsChanged(settings.copy(enabled = !settings.enabled)) }) {
                            Box(Modifier.align(if (settings.enabled) Alignment.CenterEnd else Alignment.CenterStart).padding(3.dp).size(14.dp).background(Color.White, CircleShape))
                        }
                    }
                    if (settings.enabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().height(32.dp)) {
                            PremiumModeButton("Multiplier", settings.mode == PremiumMode.Multiplier, Modifier.weight(1f)) { onSettingsChanged(settings.copy(mode = PremiumMode.Multiplier)) }
                            PremiumModeButton("Fixed amount", settings.mode == PremiumMode.FixedAmount, Modifier.weight(1f)) { onSettingsChanged(settings.copy(mode = PremiumMode.FixedAmount)) }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = settings.value, onValueChange = { onSettingsChanged(settings.copy(value = it.filter { c -> c.isDigit() || c == '.' }.take(4))) }, modifier = Modifier.fillMaxWidth().height(48.dp), singleLine = true, suffix = { Text(if (settings.mode == PremiumMode.Multiplier) "× normal rate" else "per hour", fontSize = 10.sp, color = Color(0xFF73737A)) }, shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE4E4DF), unfocusedBorderColor = Color(0xFFE4E4DF)))
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE1F7EF)) { Text("Example: €16/hr becomes ${if (settings.mode == PremiumMode.Multiplier) "€24/hr" else "€${16 + (settings.value.toDoubleOrNull() ?: 0.0)}/hr"} on Sunday", color = Color(0xFF167A5B), fontSize = 10.sp, modifier = Modifier.padding(10.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Per employee", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE4E4DF))) { Column { settings.employees.forEachIndexed { index, employee -> PremiumEmployeeRow(employee, settings, { editing = employee }); if (index < settings.employees.lastIndex) ProfileDivider() } } }
        }
        Button(onClick = { Toast.makeText(context, "Weekend premium saved", Toast.LENGTH_SHORT).show(); onSave() }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111116), contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) { Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
    }
    editing?.let { employee -> PremiumOverrideDialog(employee, settings) { updated -> onSettingsChanged(settings.copy(employees = settings.employees.map { if (it.name == updated.name) updated else it })); editing = null } }
}

@Composable private fun PremiumModeButton(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) { Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(7.dp), color = if (active) Color.White else Color(0xFFF5F5F2), border = BorderStroke(1.dp, Color(0xFFE4E4DF))) { Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 11.sp, color = Color(0xFF17171B)) } } }
@Composable private fun PremiumEmployeeRow(employee: WeekendPremiumEmployee, settings: WeekendPremiumSettings, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(26.dp).background(Color(0xFFDCEBFD), CircleShape), contentAlignment = Alignment.Center) { Text(employee.initials, color = Color(0xFF378ADD), fontSize = 9.sp) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(employee.name, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text(employee.summary(settings), color = Color(0xFF73737A), fontSize = 10.sp) }; Text(if (employee.mode == EmployeePremiumMode.Default) "default" else "adjusted", color = Color(0xFF73737A), fontSize = 9.sp); Spacer(Modifier.width(6.dp)); Text("›", color = Color(0xFF73737A), fontSize = 18.sp) } }
@Composable private fun PremiumOverrideDialog(employee: WeekendPremiumEmployee, settings: WeekendPremiumSettings, onDone: (WeekendPremiumEmployee) -> Unit) { var mode by remember { mutableStateOf(employee.mode) }; Dialog(onDismissRequest = { onDone(employee) }) { Surface(shape = RoundedCornerShape(14.dp), color = Color.White) { Column(Modifier.padding(20.dp)) { Text("Edit premium", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(employee.name, color = Color(0xFF73737A), fontSize = 12.sp); Spacer(Modifier.height(14.dp)); EmployeePremiumMode.entries.forEach { option -> Text(option.label, modifier = Modifier.fillMaxWidth().clickable { mode = option }.padding(vertical = 10.dp), color = if (mode == option) Color(0xFF1D9E75) else Color(0xFF17171B), fontSize = 14.sp) }; Button(onClick = { onDone(employee.copy(mode = mode)) }, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111116))) { Text("Apply") } } } } }

@Composable
private fun AdminWebSidebar(
    activeItem: String,
    companyInitial: String = "J",
    onHomeClick: () -> Unit = {},
    onHoursClick: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onPlacesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111116))
            .padding(horizontal = 12.dp, vertical = 22.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = "alo",
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(46.dp))
            AdminNavItem(label = "Home", active = activeItem == "Home", onClick = onHomeClick)
            Spacer(modifier = Modifier.height(10.dp))
            AdminNavItem(label = "Hours", active = activeItem == "Hours", onClick = onHoursClick)
            Spacer(modifier = Modifier.height(10.dp))
            AdminNavItem(label = "Team", active = activeItem == "Team", onClick = onTeamClick)
            Spacer(modifier = Modifier.height(10.dp))
            AdminNavItem(label = "Places", active = activeItem == "Places", onClick = onPlacesClick)
            Spacer(modifier = Modifier.height(10.dp))
            AdminNavItem(label = "Premium", active = activeItem == "Premium", onClick = onSettingsClick)
        }

        Text(
            text = companyInitial,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(28.dp)
                .background(Color(0xFF2F8F63), CircleShape)
                .padding(top = 6.dp),
        )
    }
}

@Composable
private fun AdminNavItem(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (active) Color(0xFF24242A) else Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Color(0xFF85858D),
            fontSize = 9.sp,
            lineHeight = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun AdminQueueFilter(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = if (active) Color(0xFF111116) else Color.White,
        border = BorderStroke(1.dp, if (active) Color(0xFF111116) else Color(0xFFE1E1DC)),
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (active) Color.White else Color(0xFF73737A),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AdminAdjustHoursModal(
    workerName: String,
    period: String,
    submittedHours: String,
    submittedPay: String,
    proofLabel: String = "",
    messages: List<WorkerShiftMessage> = emptyList(),
    onDismiss: () -> Unit,
    onSendReply: (String) -> Unit = {},
    onApprove: () -> Unit,
    onSave: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var clockIn by remember(workerName, period) { mutableStateOf("09:00") }
    var clockOut by remember(workerName, period) { mutableStateOf("17:30") }
    var breakMinutes by remember(workerName, period) { mutableStateOf("30") }
    var note by remember(workerName, period) { mutableStateOf("Adjusted after manager review") }
    var reply by remember(workerName, period) { mutableStateOf("") }
    val submittedHourlyRate = effectiveHourlyRateValue(submittedHours, submittedPay)
    fun sendReply() {
        val trimmedReply = reply.trim()
        if (trimmedReply.isNotEmpty()) {
            onSendReply(trimmedReply)
            reply = ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = "Adjust hours",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$workerName - $period",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                if (proofLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE9F2FF),
                        shadowElevation = 0.dp,
                    ) {
                        Text(
                            text = "Proof: $proofLabel",
                            color = Color(0xFF4973A9),
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    AdminAdjustField(
                        label = "Clock in",
                        value = clockIn,
                        onValueChange = { clockIn = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AdminAdjustField(
                        label = "Clock out",
                        value = clockOut,
                        onValueChange = { clockOut = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                AdminAdjustField(
                    label = "Break minutes",
                    value = breakMinutes,
                    onValueChange = { breakMinutes = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                AdminAdjustField(
                    label = "Note",
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minHeight = 84.dp,
                )
                if (messages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Messages",
                        color = Color(0xFF17171B),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    messages.takeLast(3).forEach { message ->
                        AdminShiftMessagePreview(message = message)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    AdminAdjustField(
                        label = "Reply",
                        value = reply,
                        onValueChange = { reply = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minHeight = 64.dp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = ::sendReply,
                        enabled = reply.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3F8EF),
                            contentColor = Color(0xFF167A5B),
                            disabledContainerColor = Color(0xFFE8E8E3),
                            disabledContentColor = Color(0xFF9A9A9F),
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            text = "Send reply",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(42.dp),
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .width(92.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3F8EF),
                            contentColor = Color(0xFF167A5B),
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "Approve",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val adjustedHours = calculateManualHours(clockIn, clockOut, breakMinutes)
                            onSave(formatHours(adjustedHours), formatEuro(adjustedHours * submittedHourlyRate))
                        },
                        modifier = Modifier
                            .width(112.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF111116),
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "Save changes",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAdjustField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 54.dp,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight),
            singleLine = singleLine,
            textStyle = TextStyle(
                color = Color(0xFF17171B),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
            shape = RoundedCornerShape(7.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF111116),
                unfocusedBorderColor = Color(0xFFE1E1DC),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF111116),
            ),
        )
    }
}

@Composable
private fun AdminShiftMessagePreview(
    message: WorkerShiftMessage,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (message.isWorker) Color(0xFFF5F5F2) else Color(0xFFE3F8EF),
        border = BorderStroke(1.dp, if (message.isWorker) Color(0xFFE1E1DC) else Color(0xFFC7ECDD)),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = if (message.isWorker) message.workerName else "Admin reply",
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = message.message,
                color = Color(0xFF17171B),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun AdminLocationListItem(
    name: String,
    detail: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (active) Color(0xFF111116) else Color(0xFFE1E1DC)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            Text(
                text = name,
                color = Color(0xFF17171B),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = detail,
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (active) "Selected" else "Tap to edit",
                color = if (active) Color(0xFF2F8F63) else Color(0xFF8C8C91),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AdminLocationMapCard(
    locationName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEAF0E9),
        border = BorderStroke(1.dp, Color(0xFFD9DFD7)),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val road = Color(0xFFCAD3C8)
                val thinRoad = Color(0xFFDDE4DA)
                drawLine(
                    color = road,
                    start = Offset(size.width * 0.08f, size.height * 0.22f),
                    end = Offset(size.width * 0.92f, size.height * 0.36f),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = road,
                    start = Offset(size.width * 0.23f, size.height * 0.08f),
                    end = Offset(size.width * 0.62f, size.height * 0.92f),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = thinRoad,
                    start = Offset(size.width * 0.05f, size.height * 0.68f),
                    end = Offset(size.width * 0.95f, size.height * 0.58f),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color(0x332F8F63),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.56f, size.height * 0.48f),
                )
                drawCircle(
                    color = Color(0xFF2F8F63),
                    radius = 10.dp.toPx(),
                    center = Offset(size.width * 0.56f, size.height * 0.48f),
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(7.dp),
                color = Color.White,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = locationName,
                    color = Color(0xFF17171B),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun AdminWorkerListItem(
    name: String,
    detail: String,
    active: Boolean,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (active) Color(0xFF111116) else Color(0xFFE1E1DC)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color(0xFF17171B),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = detail,
                    color = Color(0xFF73737A),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }
            AdminWorkerStatusChip(status = status)
        }
    }
}

@Composable
private fun AdminWorkerStatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val active = status == "Active"
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (active) Color(0xFFEAF7F0) else Color(0xFFFFF4D8),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = status,
            color = if (active) Color(0xFF2F8F63) else Color(0xFFAA7A00),
            fontSize = 9.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun AdminTeamDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = Color(0xFF17171B),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = if (onClick == null) {
        modifier.height(94.dp)
    } else {
        modifier
            .height(94.dp)
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE1E1DC)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Text(
                text = label,
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = value,
                color = Color(0xFF17171B),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = detail,
                color = Color(0xFF8C8C91),
                fontSize = 9.sp,
                lineHeight = 12.sp,
            )
        }
    }
}

@Composable
private fun AdminApprovalQueueRow(
    name: String,
    period: String,
    hours: String,
    pay: String,
    status: String,
    proofLabel: String = "",
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (proofLabel.isBlank()) 74.dp else 88.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color(0xFF17171B),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$period · $status",
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            if (proofLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Proof: $proofLabel",
                    color = Color(0xFF4973A9),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = hours,
                color = Color(0xFF17171B),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pay,
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onReview,
            modifier = Modifier
                .width(66.dp)
                .height(34.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111116),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "Review",
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AdminApprovalRow(
    name: String,
    detail: String,
    amount: String,
    proofLabel: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (proofLabel.isBlank()) 62.dp else 76.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color(0xFF17171B),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = detail,
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            if (proofLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Proof: $proofLabel",
                    color = Color(0xFF4973A9),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Text(
            text = amount,
            color = Color(0xFF17171B),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AdminWorkerMessageRow(
    message: WorkerShiftMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.workerName,
                color = Color(0xFF17171B),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${message.shiftTitle} - ${message.shiftStatus} - ${message.shiftSummary}",
                color = Color(0xFF73737A),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = shortenNotificationBody(message.message),
                color = Color(0xFF17171B),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = message.pay,
            color = Color(0xFF17171B),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun WebPlanOptionCard(
    title: String,
    price: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) Color(0xFF111116) else Color(0xFFE1E1DC),
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFF17171B),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    color = Color(0xFF17171B),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.height(8.dp))
                EmployerSelectionIndicator(selected = selected)
            }
        }
    }
}

@Composable
private fun WebRegisterSidePanel(
    activeStep: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111116))
            .padding(horizontal = 14.dp, vertical = 22.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = "alowork",
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(72.dp))
            Text(
                text = "Track hours and salaries, without the spreadsheet.",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Create your account and invite workers with one company code.",
                color = Color(0xFFA8A8AD),
                fontSize = 9.sp,
                lineHeight = 12.sp,
            )
        }

        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            WebRegisterStep(active = activeStep == "Company", label = "Company")
            Spacer(modifier = Modifier.height(8.dp))
            WebRegisterStep(active = activeStep == "Plan", label = "Plan")
            Spacer(modifier = Modifier.height(8.dp))
            WebRegisterStep(active = activeStep == "Admin", label = "Admin")
        }
    }
}

@Composable
private fun WebRegisterStep(
    active: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (active) Color(0xFF1D9D73) else Color(0xFF5B5B61),
                    shape = CircleShape,
                ),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            color = if (active) Color.White else Color(0xFFA8A8AD),
            fontSize = 9.sp,
            lineHeight = 12.sp,
        )
    }
}

@Composable
fun WorkerAwaitingApprovalScreen(
    modifier: Modifier = Modifier,
    isApproved: Boolean = false,
    onCheckApproval: () -> Boolean = { isApproved },
    onApprovalReceived: () -> Unit = {},
) {
    val context = LocalContext.current

    if (isApproved) {
        LaunchedEffect(Unit) {
            delay(700)
            onApprovalReceived()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 40.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-18).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AwaitingApprovalIcon(size = 34.dp)
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Account created",
                color = Color(0xFF17171B),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your account has been created. Your employer still needs to approve it. You'll be notified once you can start.",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick = {
                if (isApproved || onCheckApproval()) {
                    onApprovalReceived()
                } else {
                    Toast.makeText(context, "Still waiting for employer approval", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111116),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Check approval",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AwaitingApprovalIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val iconColor = Color(0xFFE0A12A)
        val radius = size.toPx() / 2f
        val center = Offset(radius, radius)

        drawCircle(
            color = iconColor,
            center = center,
            radius = radius - strokeWidth,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = iconColor,
            start = center,
            end = Offset(center.x, center.y - radius * 0.42f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = iconColor,
            start = center,
            end = Offset(center.x + radius * 0.34f, center.y + radius * 0.22f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun WorkerLocationPermissionScreen(
    modifier: Modifier = Modifier,
    onLocationAllowed: () -> Unit = {},
    onManualEntry: () -> Unit = {},
) {
    val context = LocalContext.current
    val permissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted) {
            onLocationAllowed()
        } else {
            Toast.makeText(
                context,
                "Location access is required to clock in",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 32.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-56).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LocationPermissionIcon(size = 28.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Enable location",
                color = Color(0xFF17171B),
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "To clock in at the workplace we need your\nlocation. It's only used to confirm your shift, not\nto track you.",
                color = Color(0xFF747474),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    if (hasAnyLocationPermission(context)) {
                        onLocationAllowed()
                    } else {
                        permissionLauncher.launch(permissions)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Allow location",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onManualEntry()
                    Toast.makeText(context, "Manual entry selected", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(
                    text = "Prefer to enter hours manually",
                    color = Color(0xFF1D1D22),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val iconColor = Color(0xFF2B7DBF)
        val width = size.toPx()
        val height = size.toPx()
        val center = Offset(width / 2f, height * 0.38f)

        drawCircle(
            color = iconColor,
            center = center,
            radius = width * 0.23f,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = iconColor,
            start = Offset(width * 0.5f, height * 0.62f),
            end = Offset(width * 0.5f, height * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = iconColor,
            start = Offset(width * 0.34f, height * 0.62f),
            end = Offset(width * 0.5f, height * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = iconColor,
            start = Offset(width * 0.66f, height * 0.62f),
            end = Offset(width * 0.5f, height * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun WorkerFlowPreviewScreen() {
    val context = LocalContext.current
    var shiftInProgress by remember {
        mutableStateOf(
            hasAnyLocationPermission(context = context),
        )
    }

    if (shiftInProgress) {
        GpsShiftInProgressScreen(
            onClockOut = {
                shiftInProgress = false
                Toast.makeText(context, "Clocked out", Toast.LENGTH_SHORT).show()
            },
        )
    } else {
        GpsLocationDeniedScreen(
            onLocationEnabled = {
                shiftInProgress = true
            },
        )
    }
}

@Composable
fun GpsClockInScreen(
    modifier: Modifier = Modifier,
    onClockIn: () -> Unit = {},
    onManualEntry: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        WorkLocationMapHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 286.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(134.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E5E0)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "Ready to clock in",
                        color = Color(0xFF17171B),
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are at the work location.",
                        color = Color(0xFF747474),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFFE1F7EF),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFF18B57F), CircleShape),
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Location confirmed",
                                color = Color(0xFF167A5B),
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onClockIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Clock in",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onManualEntry()
                    Toast.makeText(context, "Manual entry selected", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(
                    text = "Enter hours manually",
                    color = Color(0xFF1D1D22),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.Calendar,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun WorkerCalendarEarningsScreen(
    modifier: Modifier = Modifier,
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    onDaySelected: () -> Unit = {},
    onLogHours: () -> Unit = {},
    onProfileSelected: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.of(2026, 6)) }
    val context = LocalContext.current
    val adminRequests = loadWorkerAdminHoursRequests(context)
    val manualAdminRequest = adminRequests.firstOrNull { it.period == "17 Jun" }
    val gpsAdminRequest = adminRequests.firstOrNull { it.period == "Today" }
    val monthSummary = workerMonthSummary(
        manualSubmission = manualSubmission,
        gpsShiftSubmission = gpsShiftSubmission,
        manualAdminRequest = manualAdminRequest,
        gpsAdminRequest = gpsAdminRequest,
    )
    val calendarData = workerCalendarData(
        manualSubmission = manualSubmission,
        gpsShiftSubmission = gpsShiftSubmission,
        manualAdminRequest = manualAdminRequest,
        gpsAdminRequest = gpsAdminRequest,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 93.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hi, Sven",
                    color = Color(0xFF17171C),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFDCEBFD), CircleShape)
                        .clickable(onClick = onProfileSelected),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        color = Color(0xFF378ADD),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            WorkerEarningsCard(
                isCurrentDesignMonth = displayedMonth == YearMonth.of(2026, 6),
                summary = monthSummary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WorkerMonthCalendar(
                month = displayedMonth,
                calendarData = calendarData,
                onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                onDaySelected = onDaySelected,
            )
        }

        Button(
            onClick = onLogHours,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 93.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF17171C),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "+  Log today's hours",
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        WorkerCalendarTabBar(
            selected = WorkerTab.Calendar,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WorkerEarningsCard(
    isCurrentDesignMonth: Boolean,
    summary: WorkerMonthSummary,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0DE)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = if (isCurrentDesignMonth) "Earned in June \u00B7 net" else "Earned this month \u00B7 net",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isCurrentDesignMonth) formatWholeEuro(summary.totalPay) else "\u20AC0",
                color = Color(0xFF17171C),
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EarningsLegendItem(
                    color = Color(0xFF1D9E75),
                    label = if (isCurrentDesignMonth) "${formatWholeEuro(summary.approvedPay)} approved" else "\u20AC0 approved",
                )
                EarningsLegendItem(
                    color = Color(0xFFEF9F27),
                    label = if (isCurrentDesignMonth) "${formatWholeEuro(summary.pendingPay)} pending" else "\u20AC0 pending",
                )
            }
        }
    }
}

@Composable
private fun EarningsLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 12.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun WorkerMonthCalendar(
    month: YearMonth,
    calendarData: Map<Int, CalendarDayData>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: () -> Unit,
) {
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val cells = firstDayOffset + month.lengthOfMonth()
    val weekCount = ((cells + 6) / 7).coerceAtLeast(5)
    val rowHeight = if (weekCount > 5) 38.dp else 46.dp
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(368.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0DE)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = month.format(monthFormatter),
                    color = Color(0xFF17171C),
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                MonthArrowButton(symbol = "\u2039", onClick = onPreviousMonth)
                Spacer(modifier = Modifier.width(8.dp))
                MonthArrowButton(symbol = "\u203A", onClick = onNextMonth)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    Text(
                        text = day,
                        color = Color(0xFF9E9EA6),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            repeat(weekCount) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val dayNumber = week * 7 + weekday - firstDayOffset + 1
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val dayData = calendarDayData(month, dayNumber, calendarData)
                                WorkerCalendarDayCell(
                                    day = dayNumber,
                                    data = dayData,
                                    height = rowHeight,
                                    onClick = onDaySelected,
                                )
                            } else {
                                Spacer(modifier = Modifier.height(rowHeight))
                            }
                        }
                    }
                }
                if (week < weekCount - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalendarLegendItem(CalendarDayStatus.Approved, "approved")
                CalendarLegendItem(CalendarDayStatus.Pending, "pending")
                CalendarLegendItem(CalendarDayStatus.Adjusted, "adjusted")
            }
        }
    }
}

@Composable
private fun MonthArrowButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = Color(0xFF17171C),
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WorkerCalendarDayCell(
    day: Int,
    data: CalendarDayData?,
    height: Dp,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(40.dp)
            .height(height)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0DE)),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = day.toString(),
                color = Color(0xFF17171C),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(if (data == null) Alignment.Center else Alignment.TopCenter)
                    .padding(top = if (data == null) 0.dp else 3.dp),
            )
            data?.let { shift ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 2.dp)
                        .size(6.dp)
                        .background(shift.status.color, CircleShape),
                )
                Text(
                    text = shift.hours,
                    color = Color(0xFF9E9EA6),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(status: CalendarDayStatus, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(status.color, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
    }
}

private fun calendarDayData(
    month: YearMonth,
    day: Int,
    calendarData: Map<Int, CalendarDayData>,
): CalendarDayData? {
    if (month != YearMonth.of(2026, 6)) return null

    return calendarData[day] ?: when (day) {
        2, 3, 4, 6, 9, 10, 11, 12, 16, 17 ->
            CalendarDayData(CalendarDayStatus.Approved, "8h")
        13 -> CalendarDayData(CalendarDayStatus.Approved, "4h")
        5 -> CalendarDayData(CalendarDayStatus.Adjusted, "6h")
        18, 19 -> CalendarDayData(CalendarDayStatus.Pending, "8h")
        else -> null
    }
}

private fun workerCalendarData(
    manualSubmission: WorkerManualHoursSubmission?,
    gpsShiftSubmission: WorkerGpsShiftSubmission?,
    manualAdminRequest: AdminHoursRequest?,
    gpsAdminRequest: AdminHoursRequest?,
): Map<Int, CalendarDayData> = buildMap {
    manualSubmission?.let { submission ->
        put(
            17,
            CalendarDayData(
                status = manualAdminRequest.workerWeekStatus().calendarDayStatus(),
                hours = manualAdminRequest?.hours ?: submission.hours,
            ),
        )
    }
    gpsShiftSubmission?.let { submission ->
        put(
            19,
            CalendarDayData(
                status = gpsAdminRequest.workerWeekStatus().calendarDayStatus(),
                hours = gpsAdminRequest?.hours ?: submission.hours,
            ),
        )
    }
}

private fun WeekStatus.calendarDayStatus(): CalendarDayStatus = when (this) {
    WeekStatus.Approved -> CalendarDayStatus.Approved
    WeekStatus.Pending -> CalendarDayStatus.Pending
    WeekStatus.Adjusted -> CalendarDayStatus.Adjusted
}

@Composable
private fun WorkerCalendarTabBar(
    selected: WorkerTab,
    modifier: Modifier = Modifier,
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(93.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFFE5E5E3),
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 28.dp),
        ) {
            WorkerTab.entries.forEach { tab ->
                WorkerCalendarTabItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = onTabSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WorkerCalendarTabItem(
    tab: WorkerTab,
    selected: Boolean,
    onClick: (WorkerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) Color(0xFF17171C) else Color(0xFF9999A1)
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClick(tab) }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        WorkerTabIcon(tab = tab, color = color)
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun WorkerTabIcon(
    tab: WorkerTab,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        when (tab) {
            WorkerTab.Calendar -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.14f, size.height * 0.22f),
                    size = Size(size.width * 0.72f, size.height * 0.68f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(width = strokeWidth),
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.14f, size.height * 0.40f),
                    end = Offset(size.width * 0.86f, size.height * 0.40f),
                    strokeWidth = strokeWidth,
                )
                listOf(0.34f, 0.66f).forEach { x ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * x, size.height * 0.10f),
                        end = Offset(size.width * x, size.height * 0.30f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            WorkerTab.History -> {
                listOf(
                    0.31f to 0.78f,
                    0.50f to 0.66f,
                    0.69f to 0.52f,
                ).forEach { (y, endX) ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.22f, size.height * y),
                        end = Offset(size.width * endX, size.height * y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            WorkerTab.Alerts -> {
                drawArc(
                    color = color,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.25f, size.height * 0.20f),
                    size = Size(size.width * 0.5f, size.height * 0.62f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.20f, size.height * 0.72f),
                    end = Offset(size.width * 0.80f, size.height * 0.72f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = color,
                    center = Offset(size.width * 0.5f, size.height * 0.84f),
                    radius = size.width * 0.06f,
                )
            }

            WorkerTab.Profile -> {
                drawCircle(
                    color = color,
                    center = Offset(size.width * 0.5f, size.height * 0.31f),
                    radius = size.width * 0.17f,
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.48f),
                    size = Size(size.width * 0.64f, size.height * 0.45f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
fun WorkerNotificationsScreen(
    modifier: Modifier = Modifier,
    sentMessages: List<WorkerShiftMessage> = emptyList(),
    adminRequests: List<AdminHoursRequest> = emptyList(),
    accountApproval: WorkerAccountApproval? = null,
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    onOpenShiftDetail: (WorkerDayDetail) -> Unit = {},
    onOpenShiftChat: (WorkerDayDetail) -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val latestWorkerMessage = sentMessages.lastOrNull { it.isWorker }
    val latestEmployerReply = sentMessages.lastOrNull { !it.isWorker }
    val latestAdminDecision = adminRequests
        .filter { it.status == "Approved" || it.status == "Adjusted" }
        .lastOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Alerts",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            latestEmployerReply?.let { message ->
                NotificationCard(
                    type = NotificationType.EmployerReply,
                    title = "Employer replied",
                    body = employerReplyNotificationBody(message),
                    time = "Now",
                    unread = true,
                    onClick = {
                        onOpenShiftChat(message.workerDayDetail(manualSubmission, gpsShiftSubmission, adminRequests))
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            latestWorkerMessage?.let { message ->
                NotificationCard(
                    type = NotificationType.MessageSent,
                    title = "Message sent",
                    body = shortenNotificationBody(message.message),
                    time = "Now",
                    unread = true,
                    onClick = {
                        onOpenShiftChat(message.workerDayDetail(manualSubmission, gpsShiftSubmission, adminRequests))
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            latestAdminDecision?.let { request ->
                NotificationCard(
                    type = if (request.status == "Adjusted") {
                        NotificationType.HoursAdjusted
                    } else {
                        NotificationType.WeekApproved
                    },
                    title = if (request.status == "Adjusted") "Hours adjusted" else "Hours approved",
                    body = workerAdminDecisionBody(request),
                    time = "Now",
                    unread = true,
                    onClick = {
                        onOpenShiftDetail(request.workerDayDetail(manualSubmission, gpsShiftSubmission))
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            accountApproval?.let { approval ->
                NotificationCard(
                    type = NotificationType.AccountApproved,
                    title = "Account approved",
                    body = "Welcome, ${approval.name}. Your employer approved your account.",
                    time = "Now",
                    unread = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            NotificationCard(
                type = NotificationType.HoursAdjusted,
                title = "Hours adjusted",
                body = "Your hours for 5 June were adjusted to 6.0\nhrs.",
                time = "2 hours ago",
                unread = true,
            )
            Spacer(modifier = Modifier.height(10.dp))
            NotificationCard(
                type = NotificationType.WeekApproved,
                title = "Week approved",
                body = "Your hours for week 23 were approved.\n€608.",
                time = "yesterday",
                unread = true,
            )
            Spacer(modifier = Modifier.height(10.dp))
            NotificationCard(
                type = NotificationType.AccountApproved,
                title = "Account approved",
                body = accountApproval?.let { "Your worker account is active." }
                    ?: "Welcome! Your employer approved your\naccount.",
                time = if (accountApproval == null) "3 days ago" else "Earlier",
                unread = false,
            )
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.Alerts,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun WorkerProfileScreen(
    modifier: Modifier = Modifier,
    language: WorkerLanguage = WorkerLanguage.English,
    selectedEmployer: WorkerEmployer = defaultWorkerEmployers().first(),
    onSwitchEmployer: () -> Unit = {},
    onAddEmployer: () -> Unit = {},
    onChangeLanguage: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Profile",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            ProfileHeaderCard()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pay (selected employer)",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileSectionCard {
                ProfileInfoRow(label = "Company", value = selectedEmployer.name)
                ProfileDivider()
                ProfileInfoRow(label = "Role", value = selectedEmployer.role)
                ProfileDivider()
                ProfileInfoRow(label = "Hourly rate", value = selectedEmployer.rate)
                ProfileDivider()
                ProfileInfoRow(label = "Status", value = selectedEmployer.status)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileSectionCard {
                ProfileActionRow(
                    label = "Switch company",
                    onClick = onSwitchEmployer,
                )
                ProfileDivider()
                ProfileActionRow(
                    label = "Add company",
                    onClick = onAddEmployer,
                )
                ProfileDivider()
                ProfileActionValueRow(
                    label = "Language",
                    value = language.label,
                    onClick = onChangeLanguage,
                )
                ProfileDivider()
                ProfileActionRow(
                    label = "Change password",
                    onClick = onChangePassword,
                )
                ProfileDivider()
                ProfileActionRow(
                    label = "Log out",
                    onClick = onLogout,
                )
            }
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.Profile,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun WorkerChangePasswordScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPasswordChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "Back",
                    color = Color(0xFF73737A),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Change password",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Use at least 8 characters. Your new password must\nmatch before it can be saved.",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SignUpField(
                label = "Current password",
                value = currentPassword,
                onValueChange = { currentPassword = it },
                isPassword = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = "New password",
                value = newPassword,
                onValueChange = { newPassword = it },
                isPassword = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = "Repeat new password",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                isPassword = true,
            )
        }

        Button(
            onClick = {
                val message = when {
                    currentPassword.isBlank() -> "Enter your current password"
                    newPassword.length < 8 -> "Use at least 8 characters"
                    confirmPassword != newPassword -> "Passwords do not match"
                    else -> null
                }
                if (message == null) {
                    Toast.makeText(context, "Password changed", Toast.LENGTH_SHORT).show()
                    onPasswordChanged()
                } else {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111116),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Save password",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun WorkerLanguageSettingsScreen(
    modifier: Modifier = Modifier,
    selectedLanguage: WorkerLanguage = WorkerLanguage.English,
    onBack: () -> Unit = {},
    onLanguageSelected: (WorkerLanguage) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "Back",
                    color = Color(0xFF73737A),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Language",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Choose the language used for your worker app.",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            ProfileSectionCard {
                WorkerLanguage.entries.forEachIndexed { index, language ->
                    WorkerLanguageRow(
                        language = language,
                        selected = language == selectedLanguage,
                        onClick = { onLanguageSelected(language) },
                    )
                    if (index != WorkerLanguage.entries.lastIndex) {
                        ProfileDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerLanguageRow(
    language: WorkerLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.label,
                color = Color(0xFF17171B),
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = language.localName,
                color = Color(0xFF8C8C91),
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Color.Transparent, CircleShape)
                .drawBehind {
                    drawCircle(
                        color = if (selected) Color(0xFF111116) else Color(0xFFE4E4DF),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    if (selected) {
                        drawCircle(
                            color = Color(0xFF111116),
                            radius = size.minDimension * 0.28f,
                        )
                    }
                },
        )
    }
}

@Composable
fun WorkerHistoryOverviewScreen(
    modifier: Modifier = Modifier,
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    onDaySelected: (WorkerDayDetail) -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current
    val adminRequests = loadWorkerAdminHoursRequests(context)
    val gpsAdminRequest = adminRequests.firstOrNull { it.period == "Today" }
    val manualAdminRequest = adminRequests.firstOrNull { it.period == "17 Jun" }
    val monthSummary = workerMonthSummary(
        manualSubmission = manualSubmission,
        gpsShiftSubmission = gpsShiftSubmission,
        manualAdminRequest = manualAdminRequest,
        gpsAdminRequest = gpsAdminRequest,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Overview",
                    color = Color(0xFF17171B),
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "June 2026",
                            color = Color(0xFF17171B),
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⌄",
                            color = Color(0xFF73737A),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            MonthlySummaryCard(summary = monthSummary)
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    gpsShiftSubmission?.let { submission ->
                        val request = gpsAdminRequest
                        val status = request.workerWeekStatus()
                        val hours = request?.hours ?: submission.hours
                        val pay = request?.pay ?: submission.pay
                        WeekOverviewRow(
                            week = "Week 25",
                            amount = pay,
                            detail = "$hours - Today - ${formatPhotoCount(submission.photoCount)}",
                            status = status,
                            onClick = {
                                onDaySelected(gpsShiftWorkerDayDetail(submission, request))
                            },
                        )
                        ProfileDivider()
                    }
                    manualSubmission?.let { submission ->
                        val request = manualAdminRequest
                        val status = request.workerWeekStatus()
                        val hours = request?.hours ?: submission.hours
                        val pay = request?.pay ?: submission.pay
                        WeekOverviewRow(
                            week = "Week 25",
                            amount = pay,
                            detail = "$hours - 17 Jun",
                            status = status,
                            onClick = {
                                onDaySelected(manualWorkerDayDetail(submission, request))
                            },
                        )
                        ProfileDivider()
                    }
                    WeekOverviewRow(
                        week = "Week 23",
                        amount = "€608",
                        detail = "38.0 hrs · 2–6 Jun",
                        status = WeekStatus.Approved,
                        onClick = {
                            onDaySelected(approvedWorkerDayDetail())
                        },
                    )
                    ProfileDivider()
                    WeekOverviewRow(
                        week = "Week 24",
                        amount = "€584",
                        detail = "36.5 hrs · 9–13 Jun",
                        status = WeekStatus.Approved,
                        onClick = {
                            onDaySelected(approvedWorkerDayDetail(title = "Wed 10 Jun", hours = "7.5h", pay = "\u20AC120.00"))
                        },
                    )
                    ProfileDivider()
                    WeekOverviewRow(
                        week = "Week 25",
                        amount = "€256",
                        detail = "16.0 hrs · 16–19 Jun",
                        status = WeekStatus.Pending,
                        onClick = {
                            onDaySelected(pendingWorkerDayDetail())
                        },
                    )
                }
            }
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.History,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun WorkerDayViewAdjustedScreen(
    modifier: Modifier = Modifier,
    dayDetail: WorkerDayDetail = defaultAdjustedWorkerDayDetail(),
    onBack: () -> Unit = {},
    onAskQuestion: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 190.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    color = Color(0xFF17171B),
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(30.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = dayDetail.title,
                    color = Color(0xFF17171B),
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                WeekStatusPill(status = dayDetail.status)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                ) {
                    Text(
                        text = "Total",
                        color = Color(0xFF8C8C91),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = dayDetail.hours,
                            color = Color(0xFF17171B),
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = dayDetail.pay,
                            color = Color(0xFF17171B),
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dayDetail.summary,
                        color = Color(0xFFE0A12A),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ProfileSectionCard {
                DayInfoRow(label = "Clock in", value = dayDetail.clockIn)
                ProfileDivider()
                DayInfoRow(label = "Clock out", value = dayDetail.clockOut)
                ProfileDivider()
                DayInfoRow(label = "Break", value = dayDetail.breakLabel)
                ProfileDivider()
                DayInfoRow(label = "Hourly rate", value = dayDetail.hourlyRate)
            }
            if (dayDetail.photoUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Proof photos",
                            color = Color(0xFF17171B),
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            dayDetail.photoUris.take(2).forEach { uri ->
                                ShiftProofPhotoTile(
                                    uri = uri,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (dayDetail.photoUris.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Adjustment note",
                        color = Color(0xFF17171B),
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dayDetail.note,
                        color = Color(0xFF73737A),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAskQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = dayDetail.actionLabel,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.History,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun WorkerChatWithEmployerScreen(
    modifier: Modifier = Modifier,
    dayDetail: WorkerDayDetail = defaultAdjustedWorkerDayDetail(),
    sentMessages: List<WorkerShiftMessage> = emptyList(),
    onBack: () -> Unit = {},
    onSendMessage: (WorkerShiftMessage) -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    var draftMessage by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2039",
                    color = Color(0xFF17171B),
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(30.dp)
                        .clickable(onClick = onBack),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bakkerij Jansen",
                        color = Color(0xFF17171B),
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Employer",
                        color = Color(0xFF73737A),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDFF1E6),
                ) {
                    Text(
                        text = "BJ",
                        color = Color(0xFF247347),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .padding(top = 11.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${dayDetail.title} \u00B7 ${dayDetail.status.chatLabel}",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayDetail.chatSummary,
                            color = Color(0xFF17171B),
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = dayDetail.pay,
                        color = Color(0xFF17171B),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    ChatMessageBubble(
                        message = dayDetail.employerMessage,
                        time = "09:12",
                        isWorker = false,
                    )
                }
                item {
                    ChatMessageBubble(
                        message = "Send a note here and your employer can review this exact shift.",
                        time = "09:13",
                        isWorker = false,
                    )
                }
                items(sentMessages) { message ->
                    ChatMessageBubble(
                        message = message.message,
                        time = "Now",
                        isWorker = message.isWorker,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draftMessage,
                    onValueChange = { draftMessage = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    placeholder = {
                        Text(
                            text = "Write a message",
                            color = Color(0xFFA7A7AC),
                            fontSize = 13.sp,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = TextStyle(
                        color = Color(0xFF17171B),
                        fontSize = 13.sp,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF17171B),
                        unfocusedBorderColor = Color(0xFFDADAD5),
                        cursorColor = Color(0xFF17171B),
                    ),
                )
                Button(
                    onClick = {
                        val message = draftMessage.trim()
                        if (message.isNotEmpty()) {
                            onSendMessage(
                                WorkerShiftMessage(
                                    workerName = "Sven de Vries",
                                    shiftTitle = dayDetail.title,
                                    shiftStatus = dayDetail.status.chatLabel,
                                    shiftSummary = dayDetail.chatSummary,
                                    pay = dayDetail.pay,
                                    message = message,
                                    isWorker = true,
                                ),
                            )
                            draftMessage = ""
                        }
                    },
                    enabled = draftMessage.isNotBlank(),
                    modifier = Modifier
                        .width(76.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111116),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD7D7D2),
                        disabledContentColor = Color(0xFF8C8C91),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = "Send",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.History,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: String,
    time: String,
    isWorker: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isWorker) Alignment.End else Alignment.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.78f),
            shape = RoundedCornerShape(10.dp),
            color = if (isWorker) Color(0xFF17171B) else Color.White,
            border = if (isWorker) null else BorderStroke(1.dp, Color(0xFFE4E4DF)),
            shadowElevation = 0.dp,
        ) {
            Text(
                text = message,
                color = if (isWorker) Color.White else Color(0xFF17171B),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time,
            color = Color(0xFF9A9A9F),
            fontSize = 10.sp,
            lineHeight = 13.sp,
        )
    }
}

@Composable
fun WorkerLogHoursDayDetailScreen(
    modifier: Modifier = Modifier,
    hourlyRate: Double = 16.0,
    onBack: () -> Unit = {},
    onSubmitted: (String, String) -> Unit = { _, _ -> },
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current
    var clockIn by remember { mutableStateOf("08:00") }
    var clockOut by remember { mutableStateOf("16:30") }
    var breakMinutes by remember { mutableStateOf("30") }
    var note by remember { mutableStateOf("") }
    val totalHours = calculateManualHours(clockIn, clockOut, breakMinutes)
    val estimatedPay = totalHours * hourlyRate

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 84.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    color = Color(0xFF17171B),
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(30.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "Log hours",
                    color = Color(0xFF17171B),
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEAF5EF),
                ) {
                    Text(
                        text = "Manual",
                        color = Color(0xFF2F8F63),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                ) {
                    Text(
                        text = "Tue 17 Jun",
                        color = Color(0xFF8C8C91),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = formatHours(totalHours),
                            color = Color(0xFF17171B),
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatEuro(estimatedPay),
                            color = Color(0xFF17171B),
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Estimated with ${formatEuro(hourlyRate)} hourly rate",
                        color = Color(0xFF73737A),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    LogHoursField(
                        label = "Clock in",
                        value = clockIn,
                        onValueChange = { clockIn = it },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LogHoursField(
                        label = "Clock out",
                        value = clockOut,
                        onValueChange = { clockOut = it },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LogHoursField(
                        label = "Break",
                        value = breakMinutes,
                        onValueChange = { breakMinutes = it },
                        helper = "minutes",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LogHoursField(
                        label = "Note",
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "Optional",
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (totalHours <= 0.0) {
                        Toast.makeText(context, "Check your start and end time", Toast.LENGTH_SHORT).show()
                    } else {
                        onSubmitted(formatHours(totalHours), formatEuro(estimatedPay))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Submit hours",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        WorkerBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selected = WorkerTab.History,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun WorkerAddEmployerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onCompanyAdded: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var companyCode by remember { mutableStateOf("JANS26") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    color = Color(0xFF17171B),
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .width(26.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "Add company",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Enter the company code you received from your\nemployer.",
                color = Color(0xFF73737A),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Text(
                        text = "Company code",
                        color = Color(0xFF73737A),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = companyCode,
                        onValueChange = { value ->
                            companyCode = value
                                .filter { it.isLetterOrDigit() }
                                .uppercase(Locale.US)
                                .take(6)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF17171B),
                            fontSize = 21.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 13.sp,
                            textAlign = TextAlign.Center,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE4E4DF),
                            unfocusedBorderColor = Color(0xFFE4E4DF),
                            focusedContainerColor = Color(0xFFF9F9F6),
                            unfocusedContainerColor = Color(0xFFF9F9F6),
                            cursorColor = Color(0xFF111116),
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE9F2FF),
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = "After adding, the employer still needs to approve you.",
                    color = Color(0xFF4973A9),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                )
            }
        }

        Button(
            onClick = {
                if (companyCode.length < 6) {
                    Toast.makeText(context, "Enter the 6-character company code", Toast.LENGTH_SHORT).show()
                } else {
                    onCompanyAdded(companyCode)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111116),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Add company",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun WorkerSwitchEmployerScreen(
    modifier: Modifier = Modifier,
    employers: List<WorkerEmployer> = defaultWorkerEmployers(),
    selectedEmployerName: String = "Bakkerij Jansen",
    onBack: () -> Unit = {},
    onAddCompany: () -> Unit = {},
    onEmployerSelected: (WorkerEmployer) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    color = Color(0xFF17171B),
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .width(26.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "Switch employer",
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "You work for several companies. Choose which one to\nuse.",
                color = Color(0xFF73737A),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            employers.forEach { employer ->
                EmployerChoiceRow(
                    employer = employer,
                    selected = selectedEmployerName == employer.name,
                    onClick = {
                        onEmployerSelected(employer)
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        TextButton(
            onClick = onAddCompany,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .height(48.dp),
        ) {
            Text(
                text = "+ Add company",
                color = Color(0xFF17171B),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun EmployerChoiceRow(
    employer: WorkerEmployer,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) Color(0xFF111116) else Color(0xFFE4E4DF),
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employer.name,
                    color = Color(0xFF17171B),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${employer.role} · ${employer.rate} · ${employer.status.lowercase(Locale.US)}",
                    color = Color(0xFF73737A),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            EmployerSelectionIndicator(selected = selected)
        }
    }
}

@Composable
private fun EmployerSelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        drawCircle(
            color = if (selected) Color(0xFF111116) else Color(0xFFE4E4DF),
            radius = size.minDimension * 0.45f,
            style = if (selected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun LogHoursField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String = "",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFB8B8BC),
                    fontSize = 14.sp,
                )
            },
            textStyle = TextStyle(
                color = Color(0xFF17171B),
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF111116),
                unfocusedBorderColor = Color(0xFFE4E4DF),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF111116),
            ),
            trailingIcon = {
                if (helper.isNotBlank()) {
                    Text(
                        text = helper,
                        color = Color(0xFF8C8C91),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            },
        )
    }
}

@Composable
private fun DayInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = Color(0xFF17171B),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MonthlySummaryCard(
    summary: WorkerMonthSummary = workerMonthSummary(),
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            Text(
                text = "June total · net",
                color = Color(0xFF8C8C91),
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatWholeEuro(summary.totalPay),
                color = Color(0xFF17171B),
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryMetricTile(
                    value = formatHours(summary.totalHours),
                    label = "Hours worked",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                SummaryMetricTile(
                    value = summary.workdays.toString(),
                    label = "Workdays",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(64.dp)
            .background(Color(0xFFF4F4F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = value,
            color = Color(0xFF17171B),
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun WeekOverviewRow(
    week: String,
    amount: String,
    detail: String,
    status: WeekStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = week,
                color = Color(0xFF17171B),
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                color = Color(0xFF73737A),
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount,
                color = Color(0xFF17171B),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeekStatusPill(status = status)
        }
    }
}

@Composable
private fun WeekStatusPill(status: WeekStatus) {
    val label = when (status) {
        WeekStatus.Approved -> "Approved"
        WeekStatus.Pending -> "Pending"
        WeekStatus.Adjusted -> "Adjusted"
    }
    val background = when (status) {
        WeekStatus.Approved -> Color(0xFFE1F7EF)
        WeekStatus.Pending -> Color(0xFFFFF0DB)
        WeekStatus.Adjusted -> Color(0xFFFFF0DB)
    }
    val foreground = when (status) {
        WeekStatus.Approved -> Color(0xFF1D9D73)
        WeekStatus.Pending -> Color(0xFF9A6A22)
        WeekStatus.Adjusted -> Color(0xFF9A6A22)
    }

    Surface(
        shape = RoundedCornerShape(5.dp),
        color = background,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            color = foreground,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ProfileHeaderCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(avatarSize = 52.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Sven de Vries",
                    color = Color(0xFF17171B),
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "sven@email.nl",
                    color = Color(0xFF73737A),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(avatarSize: Dp) {
    Box(
        modifier = Modifier
            .size(avatarSize)
            .background(Color(0xFFE2EDFF), CircleShape),
    ) {
        Canvas(modifier = Modifier.align(Alignment.Center).size(24.dp)) {
            val strokeWidth = 2.dp.toPx()
            val iconColor = Color(0xFF68758A)
            val canvasSize = this.size
            drawCircle(
                color = iconColor,
                center = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.34f),
                radius = canvasSize.width * 0.18f,
                style = Stroke(width = strokeWidth),
            )
            drawLine(
                color = iconColor,
                start = Offset(canvasSize.width * 0.22f, canvasSize.height * 0.82f),
                end = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.6f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.6f),
                end = Offset(canvasSize.width * 0.78f, canvasSize.height * 0.82f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ProfileSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF73737A),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = Color(0xFF17171B),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ProfileActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF17171B),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "›",
            color = Color(0xFF8C8C91),
            fontSize = 18.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ProfileActionValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF17171B),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = Color(0xFF8C8C91),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE8E8E3)),
    )
}

private fun shortenNotificationBody(message: String): String {
    val cleaned = message.replace('_', ' ').trim()
    return if (cleaned.length <= 52) {
        cleaned
    } else {
        cleaned.take(49).trimEnd() + "..."
    }
}

@Composable
private fun NotificationCard(
    type: NotificationType,
    title: String,
    body: String,
    time: String,
    unread: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 15.dp, end = 14.dp),
        ) {
            NotificationIcon(type = type, iconSize = 36.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFF17171B),
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = body,
                    color = Color(0xFF73737A),
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = time,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                )
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(7.dp)
                        .background(Color(0xFF3D95DD), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun NotificationIcon(type: NotificationType, iconSize: Dp) {
    val background = when (type) {
        NotificationType.MessageSent -> Color(0xFFF0EDFF)
        NotificationType.EmployerReply -> Color(0xFFE3F8EF)
        NotificationType.HoursAdjusted -> Color(0xFFE7F3FF)
        NotificationType.WeekApproved -> Color(0xFFE3F8EF)
        NotificationType.AccountApproved -> Color(0xFFE3F8EF)
    }
    val iconColor = when (type) {
        NotificationType.MessageSent -> Color(0xFF6E55D8)
        NotificationType.EmployerReply -> Color(0xFF20A977)
        NotificationType.HoursAdjusted -> Color(0xFF3D95DD)
        NotificationType.WeekApproved -> Color(0xFF20A977)
        NotificationType.AccountApproved -> Color(0xFF20A977)
    }

    Box(
        modifier = Modifier
            .size(iconSize)
            .background(background, CircleShape),
    ) {
        Canvas(modifier = Modifier.align(Alignment.Center).size(18.dp)) {
            val strokeWidth = 2.dp.toPx()
            val canvasSize = this.size
            when (type) {
                NotificationType.MessageSent,
                NotificationType.EmployerReply -> {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(canvasSize.width * 0.16f, canvasSize.height * 0.22f),
                        size = Size(canvasSize.width * 0.68f, canvasSize.height * 0.48f),
                        cornerRadius = CornerRadius(canvasSize.width * 0.12f, canvasSize.width * 0.12f),
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.34f, canvasSize.height * 0.70f),
                        end = Offset(canvasSize.width * 0.24f, canvasSize.height * 0.86f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.34f, canvasSize.height * 0.70f),
                        end = Offset(canvasSize.width * 0.48f, canvasSize.height * 0.70f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                NotificationType.HoursAdjusted -> {
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.3f, canvasSize.height * 0.72f),
                        end = Offset(canvasSize.width * 0.72f, canvasSize.height * 0.3f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.26f, canvasSize.height * 0.76f),
                        end = Offset(canvasSize.width * 0.42f, canvasSize.height * 0.72f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.62f, canvasSize.height * 0.22f),
                        end = Offset(canvasSize.width * 0.78f, canvasSize.height * 0.38f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                NotificationType.WeekApproved -> {
                    drawCircle(
                        color = iconColor,
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.3f, canvasSize.height * 0.52f),
                        end = Offset(canvasSize.width * 0.44f, canvasSize.height * 0.66f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.44f, canvasSize.height * 0.66f),
                        end = Offset(canvasSize.width * 0.72f, canvasSize.height * 0.36f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                NotificationType.AccountApproved -> {
                    drawCircle(
                        color = iconColor,
                        center = Offset(canvasSize.width * 0.42f, canvasSize.height * 0.32f),
                        radius = canvasSize.width * 0.16f,
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.18f, canvasSize.height * 0.78f),
                        end = Offset(canvasSize.width * 0.42f, canvasSize.height * 0.58f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.42f, canvasSize.height * 0.58f),
                        end = Offset(canvasSize.width * 0.58f, canvasSize.height * 0.74f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.62f, canvasSize.height * 0.34f),
                        end = Offset(canvasSize.width * 0.72f, canvasSize.height * 0.44f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(canvasSize.width * 0.72f, canvasSize.height * 0.44f),
                        end = Offset(canvasSize.width * 0.86f, canvasSize.height * 0.26f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerBottomNavigation(
    selected: WorkerTab,
    modifier: Modifier = Modifier,
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111116),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkerNavItem(
                tab = WorkerTab.Calendar,
                selected = selected == WorkerTab.Calendar,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            WorkerNavItem(
                tab = WorkerTab.History,
                selected = selected == WorkerTab.History,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            WorkerNavItem(
                tab = WorkerTab.Alerts,
                selected = selected == WorkerTab.Alerts,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            WorkerNavItem(
                tab = WorkerTab.Profile,
                selected = selected == WorkerTab.Profile,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WorkerNavItem(
    tab: WorkerTab,
    selected: Boolean,
    onClick: (WorkerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClick(tab) },
        contentAlignment = Alignment.Center,
    ) {
        val iconColor = if (selected) Color.White else Color(0xFF7B7B81)
        Canvas(modifier = Modifier.size(22.dp)) {
            val strokeWidth = 2.dp.toPx()
            when (tab) {
                WorkerTab.Calendar -> {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(size.width * 0.14f, size.height * 0.22f),
                        size = Size(size.width * 0.72f, size.height * 0.68f),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.14f, size.height * 0.40f),
                        end = Offset(size.width * 0.86f, size.height * 0.40f),
                        strokeWidth = strokeWidth,
                    )
                    listOf(0.34f, 0.66f).forEach { x ->
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * x, size.height * 0.10f),
                            end = Offset(size.width * x, size.height * 0.30f),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                WorkerTab.History -> {
                    drawCircle(
                        color = iconColor,
                        radius = size.width * 0.38f,
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.5f, size.height * 0.5f),
                        end = Offset(size.width * 0.5f, size.height * 0.28f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.5f, size.height * 0.5f),
                        end = Offset(size.width * 0.68f, size.height * 0.58f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                WorkerTab.Alerts -> {
                    drawCircle(
                        color = iconColor,
                        center = Offset(size.width * 0.5f, size.height * 0.84f),
                        radius = size.width * 0.06f,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.22f, size.height * 0.68f),
                        end = Offset(size.width * 0.78f, size.height * 0.68f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.3f, size.height * 0.68f),
                        end = Offset(size.width * 0.36f, size.height * 0.26f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.7f, size.height * 0.68f),
                        end = Offset(size.width * 0.64f, size.height * 0.26f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                WorkerTab.Profile -> {
                    drawCircle(
                        color = iconColor,
                        center = Offset(size.width * 0.5f, size.height * 0.34f),
                        radius = size.width * 0.18f,
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.22f, size.height * 0.82f),
                        end = Offset(size.width * 0.5f, size.height * 0.6f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.5f, size.height * 0.6f),
                        end = Offset(size.width * 0.78f, size.height * 0.82f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkLocationMapHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(302.dp)
            .background(Color(0xFFEAF5EF)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color(0xFFD6E8DE),
                start = Offset(size.width * 0.18f, 0f),
                end = Offset(size.width * 0.64f, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = Color(0xFFD6E8DE),
                start = Offset(size.width * 0.78f, 0f),
                end = Offset(size.width * 0.34f, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = Color(0xFFD6E8DE),
                start = Offset(0f, size.height * 0.56f),
                end = Offset(size.width, size.height * 0.32f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp)
                .size(56.dp)
                .background(Color(0xFFC4EADF), CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(18.dp)
                    .background(Color(0xFF69B9E8), CircleShape),
            )
        }
    }
}

@Composable
fun GpsLocationDeniedScreen(
    modifier: Modifier = Modifier,
    onLocationEnabled: () -> Unit = {},
    onManualEntry: () -> Unit = {},
) {
    val context = LocalContext.current
    val permissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted) {
            onLocationEnabled()
        } else {
            Toast.makeText(
                context,
                "Location access is required to clock in",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-44).dp)
                .padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DeniedLocationIcon(size = 28.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Can't clock in",
                color = Color(0xFF17171B),
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Without location access you can't clock in.\nEnable location in your settings, or enter your\nhours manually.",
                color = Color(0xFF747474),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(bottom = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    if (hasAnyLocationPermission(context)) {
                        onLocationEnabled()
                    } else {
                        permissionLauncher.launch(permissions)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111116),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Enable location",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onManualEntry()
                    Toast.makeText(context, "Manual entry selected", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(
                    text = "Enter hours manually",
                    color = Color(0xFF1D1D22),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun GpsShiftInProgressScreen(
    modifier: Modifier = Modifier,
    hourlyRate: Double = 13.05,
    onClockOut: (Long) -> Unit = {},
) {
    var elapsedSeconds by remember { mutableStateOf(3.hours + 24.minutes + 11.seconds) }
    val earnings = elapsedSeconds / 3600.0 * hourlyRate

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(302.dp)
                .background(Color(0xFFEAF5EF)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-12).dp)
                    .size(56.dp)
                    .background(Color(0xFFC4EADF), CircleShape),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 286.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShiftStatusPill()
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E5E0)),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Time worked",
                        color = Color(0xFF858585),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = formatElapsedTime(elapsedSeconds),
                        color = Color(0xFF111116),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Earning so far - ${formatCurrency(earnings)}",
                        color = Color(0xFF22A77A),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    )
                }
            }
        }

        Button(
            onClick = { onClockOut(elapsedSeconds) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 54.dp)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE9444F),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Clock out",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun WorkerShiftPhotosScreen(
    modifier: Modifier = Modifier,
    photoUris: List<Uri> = emptyList(),
    onBack: () -> Unit = {},
    onPhotoAdded: (Uri) -> Unit = {},
    onPhotoRemoved: (Uri) -> Unit = {},
    onSave: () -> Unit = {},
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                onPhotoAdded(uri)
            }
        },
    )
    val gridItems: List<Uri?> = if (photoUris.size < 4) {
        photoUris + listOf(null)
    } else {
        photoUris
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2039",
                    color = Color(0xFF17171C),
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(24.dp)
                        .clickable(onClick = onBack),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Shift photos",
                    color = Color(0xFF17171C),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Add photos as proof of your shift. Your employer may require this for certain shifts.",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                gridItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowItems.forEach { uri ->
                            if (uri == null) {
                                AddShiftPhotoTile(
                                    modifier = Modifier.weight(1f),
                                    onClick = { photoPicker.launch("image/*") },
                                )
                            } else {
                                ShiftPhotoTile(
                                    uri = uri,
                                    onRemove = { onPhotoRemoved(uri) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF17171C),
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Save",
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AddShiftPhotoTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(150.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFE0E0DE),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                        ),
                    ),
                )
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "+",
            color = Color(0xFF73737A),
            fontSize = 30.sp,
            lineHeight = 32.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add photo",
            color = Color(0xFF73737A),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShiftPhotoTile(
    uri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val image = remember(uri) { loadShiftPhotoPreview(context, uri) }

    Surface(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFD6DBE0),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "Shift proof photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "Photo unavailable",
                    color = Color(0xFF73737A),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clickable(onClick = onRemove),
                shape = CircleShape,
                color = Color(0xCC17171C),
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = "\u00D7",
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun ShiftProofPhotoTile(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val image = remember(uri) { loadShiftPhotoPreview(context, uri) }

    Surface(
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFD6DBE0),
        shadowElevation = 0.dp,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Shift proof photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = "Photo unavailable",
                color = Color(0xFF73737A),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
            )
        }
    }
}

private fun loadShiftPhotoPreview(
    context: Context,
    uri: Uri,
) = runCatching {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > 1200 ||
        bounds.outHeight / sampleSize > 1200
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
    }
}.getOrNull()

@Composable
private fun ShiftStatusPill() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFFE1F7EF),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFF18B57F), CircleShape),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Shift in progress - clocked in at 08:00",
                color = Color(0xFF167A5B),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DeniedLocationIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val iconColor = Color(0xFFD94A5B)
        drawCircle(
            color = iconColor,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = iconColor,
            start = Offset(size.toPx() * 0.27f, size.toPx() * 0.27f),
            end = Offset(size.toPx() * 0.73f, size.toPx() * 0.73f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun hasAnyLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}

private val Int.hours: Long
    get() = this * 60L * 60L

private val Int.minutes: Long
    get() = this * 60L

private val Int.seconds: Long
    get() = this.toLong()

private fun formatElapsedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
}

private fun formatCurrency(value: Double): String {
    return formatEuro(value)
}

private fun calculateManualHours(
    clockIn: String,
    clockOut: String,
    breakMinutes: String,
): Double {
    val start = parseTimeMinutes(clockIn) ?: return 0.0
    val end = parseTimeMinutes(clockOut) ?: return 0.0
    val breakValue = breakMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val workedMinutes = (end - start - breakValue).coerceAtLeast(0)
    return workedMinutes / 60.0
}

private fun parseTimeMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null

    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null

    return hours * 60 + minutes
}

private fun formatHours(value: Double): String {
    return "%.1fh".format(Locale.US, value)
}

private fun formatEuro(value: Double): String {
    return "\u20AC%.2f".format(Locale.US, value)
}

private fun formatPhotoCount(count: Int): String {
    return if (count == 1) "1 photo" else "$count photos"
}

private fun proofLabelForPhotoCount(count: Int): String {
    return if (count > 0) formatPhotoCount(count) else ""
}

private fun formatWholeEuro(value: Double): String {
    return "\u20AC%,.0f".format(Locale.US, value)
}

private fun parseHoursValue(value: String): Double {
    return value.removeSuffix("h").replace(",", ".").toDoubleOrNull() ?: 0.0
}

private fun parseEuroValue(value: String): Double {
    return value
        .filter { it.isDigit() || it == '.' || it == ',' }
        .replace(",", ".")
        .toDoubleOrNull() ?: 0.0
}

private fun WorkerEmployer.hourlyRateValue(): Double {
    return parseEuroValue(rate).takeIf { it > 0.0 } ?: 16.0
}

private fun effectiveHourlyRateLabel(
    hours: String,
    pay: String,
    fallbackRate: Double = 16.0,
): String {
    return formatEuro(effectiveHourlyRateValue(hours, pay, fallbackRate))
}

private fun effectiveHourlyRateValue(
    hours: String,
    pay: String,
    fallbackRate: Double = 16.0,
): Double {
    val hourValue = parseHoursValue(hours)
    val payValue = parseEuroValue(pay)
    return if (hourValue > 0.0 && payValue > 0.0) {
        payValue / hourValue
    } else {
        fallbackRate
    }
}

private enum class AppScreen {
    WorkerSignUp,
    WorkerLogin,
    WorkerAwaitingApproval,
    WorkerLocationPermission,
    WorkerCalendarEarnings,
    WorkerGpsClockIn,
    WorkerShiftInProgress,
    WorkerShiftPhotos,
    WorkerLocationDenied,
    WorkerNotifications,
    WorkerProfile,
    WorkerLanguageSettings,
    WorkerChangePassword,
    WorkerHistoryOverview,
    WorkerDayViewAdjusted,
    WorkerChatWithEmployer,
    WorkerLogHoursDayDetail,
    WorkerAddEmployer,
    WorkerSwitchEmployer,
    WebRegisterCompanyDetails,
    WebRegisterChoosePlan,
    WebRegisterAdminAccount,
    WebRegisterSuccessCode,
    AdminDashboardHome,
    AdminHoursApprovalQueue,
    AdminWorkLocations,
    AdminTeam,
    AdminWeekendPremiumSettings,
}

private enum class PremiumMode { Multiplier, FixedAmount }
private enum class EmployeePremiumMode(val label: String) { Default("Use company default"), Multiplier("Multiplier: 1.5×"), FixedAmount("Fixed amount: +€5/hr"), None("No premium") }
private data class WeekendPremiumEmployee(val name: String, val initials: String, val mode: EmployeePremiumMode) {
    fun summary(settings: WeekendPremiumSettings): String = when (mode) { EmployeePremiumMode.Default -> if (settings.enabled) "${settings.value}${if (settings.mode == PremiumMode.Multiplier) "× (default)" else " per hour (default)"}" else "No premium (default)"; EmployeePremiumMode.Multiplier -> "1.5× on Sunday"; EmployeePremiumMode.FixedAmount -> "+€5/hr on Sunday"; EmployeePremiumMode.None -> "No premium" }
}
private data class WeekendPremiumSettings(val enabled: Boolean, val mode: PremiumMode, val value: String, val employees: List<WeekendPremiumEmployee>)
private fun defaultWeekendPremiumSettings() = WeekendPremiumSettings(true, PremiumMode.Multiplier, "1.5", listOf(WeekendPremiumEmployee("Sven de Vries", "SV", EmployeePremiumMode.Default), WeekendPremiumEmployee("Anke Jansen", "AJ", EmployeePremiumMode.FixedAmount), WeekendPremiumEmployee("Mo El Amrani", "ME", EmployeePremiumMode.Default), WeekendPremiumEmployee("Lotte Bakker", "LB", EmployeePremiumMode.None)))

private const val WeekendPremiumPreferences = "weekend_premium"

private fun loadWeekendPremiumSettings(context: Context): WeekendPremiumSettings {
    val defaults = defaultWeekendPremiumSettings()
    val preferences = context.getSharedPreferences(WeekendPremiumPreferences, Context.MODE_PRIVATE)
    return defaults.copy(
        enabled = preferences.getBoolean("enabled", defaults.enabled),
        mode = preferences.getString("mode", defaults.mode.name)
            ?.let { runCatching { PremiumMode.valueOf(it) }.getOrNull() }
            ?: defaults.mode,
        value = preferences.getString("value", defaults.value) ?: defaults.value,
        employees = defaults.employees.map { employee ->
            val storedMode = preferences.getString("employee_${employee.initials}", employee.mode.name)
                ?.let { runCatching { EmployeePremiumMode.valueOf(it) }.getOrNull() }
                ?: employee.mode
            employee.copy(mode = storedMode)
        },
    )
}

private fun saveWeekendPremiumSettings(context: Context, settings: WeekendPremiumSettings) {
    context.getSharedPreferences(WeekendPremiumPreferences, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("enabled", settings.enabled)
        .putString("mode", settings.mode.name)
        .putString("value", settings.value)
        .apply {
            settings.employees.forEach { employee ->
                putString("employee_${employee.initials}", employee.mode.name)
            }
        }
        .apply()
}

data class WorkerEmployer(
    val name: String,
    val role: String,
    val rate: String,
    val status: String,
)

fun defaultWorkerEmployers(): List<WorkerEmployer> {
    return listOf(
        WorkerEmployer("Bakkerij Jansen", "Shift worker", "\u20AC16.00/hr", "Active"),
        WorkerEmployer("Cafe De Hoek", "Service", "\u20AC14.50/hr", "Active"),
        WorkerEmployer("Tuincentrum Bos", "Weekend help", "\u20AC13.00/hr", "Active"),
    )
}

private fun resolveWorkerEmployerByCode(companyCode: String): WorkerEmployer {
    return when (companyCode.uppercase(Locale.US)) {
        "JANS26" -> WorkerEmployer("Bakkerij Jansen", "Shift worker", "\u20AC16.00/hr", "Active")
        "CAFE24" -> WorkerEmployer("Cafe De Hoek", "Service", "\u20AC14.50/hr", "Active")
        "BOS013" -> WorkerEmployer("Tuincentrum Bos", "Weekend help", "\u20AC13.00/hr", "Active")
        "ROOS24" -> WorkerEmployer("Roos Logistics", "Warehouse assistant", "\u20AC15.25/hr", "Pending")
        else -> WorkerEmployer("Company $companyCode", "Pending approval", "Rate pending", "Pending")
    }
}

private const val WorkerEmployersPreferences = "worker_employers"

private fun loadWorkerEmployers(context: Context): List<WorkerEmployer> {
    val stored = context.getSharedPreferences(WorkerEmployersPreferences, Context.MODE_PRIVATE)
        .getString("companies", null)
        ?: return defaultWorkerEmployers()
    return stored.lineSequence().mapNotNull { row ->
        val parts = row.split('|')
        if (parts.size == 4) WorkerEmployer(parts[0], parts[1], parts[2], parts[3]) else null
    }.toList().ifEmpty(::defaultWorkerEmployers)
}

private fun saveWorkerEmployers(context: Context, employers: List<WorkerEmployer>) {
    context.getSharedPreferences(WorkerEmployersPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("companies", employers.joinToString("\n") { "${it.name}|${it.role}|${it.rate}|${it.status}" })
        .apply()
}

private fun loadSelectedEmployerName(context: Context): String {
    return context.getSharedPreferences(WorkerEmployersPreferences, Context.MODE_PRIVATE)
        .getString("selected", "Bakkerij Jansen")
        ?: "Bakkerij Jansen"
}

private fun saveSelectedEmployerName(context: Context, name: String) {
    context.getSharedPreferences(WorkerEmployersPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("selected", name)
        .apply()
}

enum class WorkerLanguage(val label: String, val localName: String) {
    English("English", "English"),
    Dutch("Dutch", "Nederlands"),
    German("German", "Deutsch"),
    French("French", "Francais"),
}

private const val WorkerSettingsPreferences = "worker_settings"

private fun loadWorkerLanguage(context: Context): WorkerLanguage {
    val stored = context.getSharedPreferences(WorkerSettingsPreferences, Context.MODE_PRIVATE)
        .getString("language", WorkerLanguage.English.name)
        ?: WorkerLanguage.English.name
    return runCatching { WorkerLanguage.valueOf(stored) }.getOrDefault(WorkerLanguage.English)
}

private fun saveWorkerLanguage(context: Context, language: WorkerLanguage) {
    context.getSharedPreferences(WorkerSettingsPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("language", language.name)
        .apply()
}

data class WorkerAccountApproval(
    val name: String,
    val email: String,
)

private fun loadWorkerAccountApproval(context: Context): WorkerAccountApproval? {
    val preferences = context.getSharedPreferences(WorkerSettingsPreferences, Context.MODE_PRIVATE)
    val name = preferences.getString("approved_name", null) ?: return null
    val email = preferences.getString("approved_email", null) ?: return null
    return WorkerAccountApproval(name = name, email = email)
}

private fun saveWorkerAccountApproval(context: Context, approval: WorkerAccountApproval) {
    context.getSharedPreferences(WorkerSettingsPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("approved_name", approval.name)
        .putString("approved_email", approval.email)
        .apply()
}

private fun clearWorkerAccountApproval(context: Context) {
    context.getSharedPreferences(WorkerSettingsPreferences, Context.MODE_PRIVATE)
        .edit()
        .remove("approved_name")
        .remove("approved_email")
        .apply()
}

private fun clearWorkerEmployers(context: Context) {
    context.getSharedPreferences(WorkerEmployersPreferences, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

data class WorkerManualHoursSubmission(
    val hours: String,
    val pay: String,
)

data class WorkerGpsShiftSubmission(
    val hours: String,
    val pay: String,
    val photoCount: Int,
    val photoUris: List<Uri> = emptyList(),
)

data class WorkerDayDetail(
    val title: String,
    val status: WeekStatus,
    val hours: String,
    val pay: String,
    val summary: String,
    val clockIn: String,
    val clockOut: String,
    val breakLabel: String,
    val hourlyRate: String,
    val note: String,
    val actionLabel: String,
    val photoUris: List<Uri> = emptyList(),
) {
    val chatSummary: String
        get() = "$hours - $summary"

    val employerMessage: String
        get() = when (status) {
            WeekStatus.Approved -> "This shift is approved and included in payroll."
            WeekStatus.Pending -> "This shift is waiting for review. Share any extra context here."
            WeekStatus.Adjusted -> "I adjusted this shift to match the approved schedule."
        }
}

private fun defaultAdjustedWorkerDayDetail(): WorkerDayDetail {
    return WorkerDayDetail(
        title = "Tue 3 Jun",
        status = WeekStatus.Adjusted,
        hours = "6.0h",
        pay = "\u20AC96.00",
        summary = "Adjusted from 7.5h by your employer",
        clockIn = "08:00",
        clockOut = "14:00",
        breakLabel = "0 min",
        hourlyRate = "\u20AC16.00",
        note = "Your employer adjusted this day to match the approved schedule.",
        actionLabel = "Ask about this adjustment",
    )
}

private fun gpsShiftWorkerDayDetail(
    submission: WorkerGpsShiftSubmission,
    adminRequest: AdminHoursRequest? = null,
): WorkerDayDetail {
    val status = adminRequest.workerWeekStatus()
    val hours = adminRequest?.hours ?: submission.hours
    val pay = adminRequest?.pay ?: submission.pay
    return WorkerDayDetail(
        title = "Today",
        status = status,
        hours = hours,
        pay = pay,
        summary = workerSubmissionSummary(status, "Submitted with ${formatPhotoCount(submission.photoCount)}"),
        clockIn = "08:00",
        clockOut = if (status == WeekStatus.Pending) "Now" else "Clocked out",
        breakLabel = "0 min",
        hourlyRate = effectiveHourlyRateLabel(hours, pay, fallbackRate = 13.05),
        note = workerSubmissionNote(status),
        actionLabel = workerSubmissionActionLabel(status),
        photoUris = submission.photoUris,
    )
}

private fun manualWorkerDayDetail(
    submission: WorkerManualHoursSubmission,
    adminRequest: AdminHoursRequest? = null,
): WorkerDayDetail {
    val status = adminRequest.workerWeekStatus()
    val hours = adminRequest?.hours ?: submission.hours
    val pay = adminRequest?.pay ?: submission.pay
    return WorkerDayDetail(
        title = "Wed 17 Jun",
        status = status,
        hours = hours,
        pay = pay,
        summary = workerSubmissionSummary(status, "Manual hours waiting for approval"),
        clockIn = "08:00",
        clockOut = "16:30",
        breakLabel = "30 min",
        hourlyRate = effectiveHourlyRateLabel(hours, pay),
        note = workerSubmissionNote(status),
        actionLabel = workerSubmissionActionLabel(status),
    )
}

private fun WorkerShiftMessage.workerDayDetail(
    manualSubmission: WorkerManualHoursSubmission?,
    gpsShiftSubmission: WorkerGpsShiftSubmission?,
    adminRequests: List<AdminHoursRequest>,
): WorkerDayDetail {
    val request = adminRequests.firstOrNull { it.period == shiftTitle }
    return when (shiftTitle) {
        "Today" -> gpsShiftSubmission?.let { gpsShiftWorkerDayDetail(it, request) }
        "17 Jun", "Wed 17 Jun" -> manualSubmission?.let { manualWorkerDayDetail(it, request) }
        else -> null
    } ?: workerMessageFallbackDayDetail(this, request)
}

private fun AdminHoursRequest.workerDayDetail(
    manualSubmission: WorkerManualHoursSubmission?,
    gpsShiftSubmission: WorkerGpsShiftSubmission?,
): WorkerDayDetail {
    return when (period) {
        "Today" -> gpsShiftSubmission?.let { gpsShiftWorkerDayDetail(it, this) }
        "17 Jun", "Wed 17 Jun" -> manualSubmission?.let { manualWorkerDayDetail(it, this) }
        else -> null
    } ?: WorkerDayDetail(
        title = period,
        status = workerWeekStatus(),
        hours = hours,
        pay = pay,
        summary = workerSubmissionSummary(workerWeekStatus(), "Waiting for employer approval"),
        clockIn = "08:00",
        clockOut = if (workerWeekStatus() == WeekStatus.Pending) "Now" else "Clocked out",
        breakLabel = "0 min",
        hourlyRate = effectiveHourlyRateLabel(hours, pay),
        note = workerSubmissionNote(workerWeekStatus()),
        actionLabel = workerSubmissionActionLabel(workerWeekStatus()),
    )
}

private fun workerMessageFallbackDayDetail(
    message: WorkerShiftMessage,
    adminRequest: AdminHoursRequest?,
): WorkerDayDetail {
    val status = adminRequest.workerWeekStatus()
    return WorkerDayDetail(
        title = message.shiftTitle,
        status = status,
        hours = adminRequest?.hours ?: message.shiftSummary.substringBefore(" - "),
        pay = adminRequest?.pay ?: message.pay,
        summary = adminRequest?.let { workerSubmissionSummary(status, message.shiftSummary) } ?: message.shiftSummary,
        clockIn = "08:00",
        clockOut = if (status == WeekStatus.Pending) "Now" else "Clocked out",
        breakLabel = "0 min",
        hourlyRate = effectiveHourlyRateLabel(
            hours = adminRequest?.hours ?: message.shiftSummary.substringBefore(" - "),
            pay = adminRequest?.pay ?: message.pay,
        ),
        note = workerSubmissionNote(status),
        actionLabel = workerSubmissionActionLabel(status),
    )
}

private fun approvedWorkerDayDetail(
    title: String = "Tue 3 Jun",
    hours: String = "8.0h",
    pay: String = "\u20AC128.00",
): WorkerDayDetail {
    return WorkerDayDetail(
        title = title,
        status = WeekStatus.Approved,
        hours = hours,
        pay = pay,
        summary = "Approved by your employer",
        clockIn = "08:00",
        clockOut = "16:30",
        breakLabel = "30 min",
        hourlyRate = "\u20AC16.00",
        note = "This shift has been approved and included in payroll.",
        actionLabel = "Ask about this shift",
    )
}

private fun pendingWorkerDayDetail(): WorkerDayDetail {
    return WorkerDayDetail(
        title = "Thu 18 Jun",
        status = WeekStatus.Pending,
        hours = "8.0h",
        pay = "\u20AC128.00",
        summary = "Waiting for employer approval",
        clockIn = "08:00",
        clockOut = "16:30",
        breakLabel = "30 min",
        hourlyRate = "\u20AC16.00",
        note = "These hours are still in review before payroll.",
        actionLabel = "Ask about this shift",
    )
}

private data class WorkerMonthSummary(
    val totalPay: Double,
    val approvedPay: Double,
    val pendingPay: Double,
    val totalHours: Double,
    val workdays: Int,
)

private fun workerMonthSummary(
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    manualAdminRequest: AdminHoursRequest? = null,
    gpsAdminRequest: AdminHoursRequest? = null,
): WorkerMonthSummary {
    val submissions = listOfNotNull(
        manualSubmission?.let { submission ->
            WorkerSummaryEntry(
                hours = manualAdminRequest?.hours ?: submission.hours,
                pay = manualAdminRequest?.pay ?: submission.pay,
                status = manualAdminRequest.workerWeekStatus(),
            )
        },
        gpsShiftSubmission?.let { submission ->
            WorkerSummaryEntry(
                hours = gpsAdminRequest?.hours ?: submission.hours,
                pay = gpsAdminRequest?.pay ?: submission.pay,
                status = gpsAdminRequest.workerWeekStatus(),
            )
        },
    )
    val submittedPay = submissions.sumOf { entry -> parseEuroValue(entry.pay) }
    val submittedHours = submissions.sumOf { entry -> parseHoursValue(entry.hours) }
    val approvedSubmittedPay = submissions
        .filter { it.status == WeekStatus.Approved || it.status == WeekStatus.Adjusted }
        .sumOf { entry -> parseEuroValue(entry.pay) }
    val pendingSubmittedPay = submissions
        .filter { it.status == WeekStatus.Pending }
        .sumOf { entry -> parseEuroValue(entry.pay) }

    return WorkerMonthSummary(
        totalPay = 1284.0 + submittedPay,
        approvedPay = 1092.0 + approvedSubmittedPay,
        pendingPay = 192.0 + pendingSubmittedPay,
        totalHours = 78.5 + submittedHours,
        workdays = 14 + submissions.size,
    )
}

private data class WorkerSummaryEntry(
    val hours: String,
    val pay: String,
    val status: WeekStatus,
)

private const val ManualHoursPreferences = "manual_hours"
private const val WorkerGpsShiftPreferences = "gps_shift"

private fun loadManualHoursSubmission(context: Context): WorkerManualHoursSubmission? {
    val preferences = context.getSharedPreferences(ManualHoursPreferences, Context.MODE_PRIVATE)
    val hours = preferences.getString("hours", null) ?: return null
    val pay = preferences.getString("pay", null) ?: return null
    return WorkerManualHoursSubmission(hours = hours, pay = pay)
}

private fun saveManualHoursSubmission(context: Context, submission: WorkerManualHoursSubmission) {
    context.getSharedPreferences(ManualHoursPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("hours", submission.hours)
        .putString("pay", submission.pay)
        .apply()
}

private fun clearManualHoursSubmission(context: Context) {
    context.getSharedPreferences(ManualHoursPreferences, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

private fun loadWorkerGpsShiftSubmission(context: Context): WorkerGpsShiftSubmission? {
    val preferences = context.getSharedPreferences(WorkerGpsShiftPreferences, Context.MODE_PRIVATE)
    val hours = preferences.getString("hours", null) ?: return null
    val pay = preferences.getString("pay", null) ?: return null
    val photoCount = preferences.getInt("photo_count", 0)
    val photoUris = preferences.getString("photo_uris", null)
        ?.lineSequence()
        ?.mapNotNull { row -> runCatching { Uri.parse(row) }.getOrNull() }
        ?.toList()
        .orEmpty()
    return WorkerGpsShiftSubmission(
        hours = hours,
        pay = pay,
        photoCount = maxOf(photoCount, photoUris.size),
        photoUris = photoUris,
    )
}

private fun saveWorkerGpsShiftSubmission(context: Context, submission: WorkerGpsShiftSubmission) {
    context.getSharedPreferences(WorkerGpsShiftPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("hours", submission.hours)
        .putString("pay", submission.pay)
        .putInt("photo_count", submission.photoCount)
        .putString("photo_uris", submission.photoUris.joinToString("\n") { it.toString() })
        .apply()
}

private fun clearWorkerGpsShiftSubmission(context: Context) {
    context.getSharedPreferences(WorkerGpsShiftPreferences, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

private fun saveWorkerSubmittedHoursForAdmin(
    context: Context,
    period: String,
    hours: String,
    pay: String,
    proofLabel: String = "",
) {
    val submittedRequest = AdminHoursRequest(
        name = "Sven de Vries",
        period = period,
        hours = hours,
        pay = pay,
        status = "Submitted",
        proofLabel = proofLabel,
    )
    val existingRequests = loadAdminHoursRequests(context)
    val updatedRequests = existingRequests
        .filterNot { request ->
            request.name == submittedRequest.name && request.period == submittedRequest.period
        } + submittedRequest
    saveAdminHoursRequests(context, updatedRequests)
}

data class WorkerShiftMessage(
    val workerName: String,
    val shiftTitle: String,
    val shiftStatus: String,
    val shiftSummary: String,
    val pay: String,
    val message: String,
    val isWorker: Boolean = true,
)

private const val WorkerChatPreferences = "worker_chat"

private fun loadWorkerChatMessages(context: Context): List<WorkerShiftMessage> {
    return context.getSharedPreferences(WorkerChatPreferences, Context.MODE_PRIVATE)
        .getStringSet("messages", emptySet())
        ?.mapNotNull { row ->
            val separator = row.indexOf('|')
            if (separator <= 0) null else row.substring(0, separator).toIntOrNull()
                ?.let { index -> index to decodeWorkerShiftMessage(row.substring(separator + 1)) }
        }
        ?.sortedBy { it.first }
        ?.map { it.second }
        ?: emptyList()
}

private fun saveWorkerChatMessages(context: Context, messages: List<WorkerShiftMessage>) {
    context.getSharedPreferences(WorkerChatPreferences, Context.MODE_PRIVATE)
        .edit()
        .putStringSet("messages", messages.mapIndexed { index, message -> "$index|${encodeWorkerShiftMessage(message)}" }.toSet())
        .apply()
}

private fun clearWorkerChatMessages(context: Context) {
    context.getSharedPreferences(WorkerChatPreferences, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

private fun encodeWorkerShiftMessage(message: WorkerShiftMessage): String {
    return listOf(
        message.workerName,
        message.shiftTitle,
        message.shiftStatus,
        message.shiftSummary,
        message.pay,
        message.message,
        message.isWorker.toString(),
    ).joinToString("|") { Uri.encode(it) }
}

private fun decodeWorkerShiftMessage(row: String): WorkerShiftMessage {
    val parts = row.split('|')
    return if (parts.size == 6 || parts.size == 7) {
        WorkerShiftMessage(
            workerName = Uri.decode(parts[0]),
            shiftTitle = Uri.decode(parts[1]),
            shiftStatus = Uri.decode(parts[2]),
            shiftSummary = Uri.decode(parts[3]),
            pay = Uri.decode(parts[4]),
            message = Uri.decode(parts[5]),
            isWorker = parts.getOrNull(6)?.let { Uri.decode(it).toBooleanStrictOrNull() } ?: true,
        )
    } else {
        WorkerShiftMessage(
            workerName = "Sven de Vries",
            shiftTitle = "Shift",
            shiftStatus = WeekStatus.Pending.chatLabel,
            shiftSummary = "Worker message",
            pay = "",
            message = row,
            isWorker = true,
        )
    }
}

data class AdminCompanyProfile(
    val companyName: String,
    val industry: String,
    val workEmail: String,
    val plan: String,
    val adminName: String,
    val adminEmail: String,
)

fun defaultAdminCompanyProfile(): AdminCompanyProfile {
    return AdminCompanyProfile(
        companyName = "Bakkerij Jansen",
        industry = "Bakery",
        workEmail = "admin@bakkerijjansen.nl",
        plan = "Starter",
        adminName = "Lotte Jansen",
        adminEmail = "lotte@bakkerijjansen.nl",
    )
}

private const val AdminCompanyPreferences = "admin_company"

private fun loadAdminCompanyProfile(context: Context): AdminCompanyProfile {
    val defaults = defaultAdminCompanyProfile()
    val preferences = context.getSharedPreferences(AdminCompanyPreferences, Context.MODE_PRIVATE)
    return defaults.copy(
        companyName = preferences.getString("companyName", defaults.companyName) ?: defaults.companyName,
        industry = preferences.getString("industry", defaults.industry) ?: defaults.industry,
        workEmail = preferences.getString("workEmail", defaults.workEmail) ?: defaults.workEmail,
        plan = preferences.getString("plan", defaults.plan) ?: defaults.plan,
        adminName = preferences.getString("adminName", defaults.adminName) ?: defaults.adminName,
        adminEmail = preferences.getString("adminEmail", defaults.adminEmail) ?: defaults.adminEmail,
    )
}

private fun saveAdminCompanyProfile(context: Context, profile: AdminCompanyProfile) {
    context.getSharedPreferences(AdminCompanyPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("companyName", profile.companyName)
        .putString("industry", profile.industry)
        .putString("workEmail", profile.workEmail)
        .putString("plan", profile.plan)
        .putString("adminName", profile.adminName)
        .putString("adminEmail", profile.adminEmail)
        .apply()
}

data class AdminHoursRequest(
    val name: String,
    val period: String,
    val hours: String,
    val pay: String,
    val status: String,
    val proofLabel: String = "",
)

private fun AdminHoursRequest.isSameAdminRequest(other: AdminHoursRequest): Boolean {
    return name == other.name && period == other.period
}

private fun AdminHoursRequest?.workerWeekStatus(): WeekStatus {
    return when (this?.status) {
        "Approved" -> WeekStatus.Approved
        "Adjusted" -> WeekStatus.Adjusted
        else -> WeekStatus.Pending
    }
}

private fun workerSubmissionSummary(status: WeekStatus, pendingSummary: String): String {
    return when (status) {
        WeekStatus.Approved -> "Approved by your employer"
        WeekStatus.Pending -> pendingSummary
        WeekStatus.Adjusted -> "Adjusted by your employer"
    }
}

private fun workerSubmissionNote(status: WeekStatus): String {
    return when (status) {
        WeekStatus.Approved -> "Your employer approved this shift and included it in payroll."
        WeekStatus.Pending -> "Your submitted hours are waiting for employer approval."
        WeekStatus.Adjusted -> "Your employer adjusted this shift before payroll."
    }
}

private fun workerSubmissionActionLabel(status: WeekStatus): String {
    return when (status) {
        WeekStatus.Approved -> "Ask about this shift"
        WeekStatus.Pending -> "Ask about this shift"
        WeekStatus.Adjusted -> "Ask about this adjustment"
    }
}

private fun workerAdminDecisionBody(request: AdminHoursRequest): String {
    return if (request.status == "Adjusted") {
        "Your ${request.period} hours were adjusted to ${request.hours} (${request.pay})."
    } else {
        "Your ${request.period} hours were approved (${request.hours}, ${request.pay})."
    }
}

private fun employerReplyNotificationBody(message: WorkerShiftMessage): String {
    return shortenNotificationBody("${message.shiftTitle}: ${message.message}")
}

private fun loadWorkerAdminHoursRequests(context: Context): List<AdminHoursRequest> {
    return loadAdminHoursRequests(context).filter { it.name == "Sven de Vries" }
}

private fun String.workerChatLabel(): String {
    return when (this) {
        "Approved" -> WeekStatus.Approved.chatLabel
        "Adjusted" -> WeekStatus.Adjusted.chatLabel
        else -> WeekStatus.Pending.chatLabel
    }
}

private fun String.workerSummaryLabel(): String {
    return when (this) {
        "Approved" -> "Approved by your employer"
        "Adjusted" -> "Adjusted by your employer"
        else -> "Waiting for review"
    }
}

private const val AdminHoursPreferences = "admin_hours"

private fun defaultAdminHoursRequests(): List<AdminHoursRequest> = listOf(
    AdminHoursRequest("Sven de Vries", "Week 25", "16.0h", "\u20AC256", "Submitted"),
    AdminHoursRequest("Mila Bakker", "Week 25", "12.5h", "\u20AC200", "Submitted"),
    AdminHoursRequest("Noah Visser", "Week 25", "11.0h", "\u20AC176", "Adjusted"),
)

private fun loadAdminHoursRequests(context: Context): List<AdminHoursRequest> {
    val stored = context.getSharedPreferences(AdminHoursPreferences, Context.MODE_PRIVATE)
        .getString("requests", null)
        ?: return defaultAdminHoursRequests()
    return stored.lineSequence().mapNotNull { row ->
        val parts = row.split('|')
        if (parts.size == 5 || parts.size == 6) {
            AdminHoursRequest(
                name = parts[0],
                period = parts[1],
                hours = parts[2],
                pay = parts[3],
                status = parts[4],
                proofLabel = parts.getOrNull(5).orEmpty(),
            )
        } else {
            null
        }
    }.toList().ifEmpty(::defaultAdminHoursRequests)
}

private fun saveAdminHoursRequests(context: Context, requests: List<AdminHoursRequest>) {
    context.getSharedPreferences(AdminHoursPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString(
            "requests",
            requests.joinToString("\n") {
                "${it.name}|${it.period}|${it.hours}|${it.pay}|${it.status}|${it.proofLabel}"
            },
        )
        .apply()
}

private data class AdminLocation(
    val name: String,
    val address: String,
    val radius: String,
)

private const val AdminLocationsPreferences = "admin_locations"

private fun defaultAdminWorkLocations(): List<AdminLocation> = listOf(
    AdminLocation(name = "Bakery floor", address = "Lijnbaan 24", radius = "120"),
    AdminLocation(name = "Market stall", address = "Binnenrotte 101", radius = "80"),
    AdminLocation(name = "Warehouse", address = "Schuttevaerweg 12", radius = "160"),
)

private fun loadAdminWorkLocations(context: Context): List<AdminLocation> {
    val stored = context.getSharedPreferences(AdminLocationsPreferences, Context.MODE_PRIVATE)
        .getString("locations", null)
        ?: return defaultAdminWorkLocations()
    return stored.lineSequence().mapNotNull { row ->
        val parts = row.split('|')
        if (parts.size == 3) AdminLocation(parts[0], parts[1], parts[2]) else null
    }.toList().ifEmpty(::defaultAdminWorkLocations)
}

private fun saveAdminWorkLocations(context: Context, locations: List<AdminLocation>) {
    context.getSharedPreferences(AdminLocationsPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString("locations", locations.joinToString("\n") { "${it.name}|${it.address}|${it.radius}" })
        .apply()
}

private data class AdminWorker(
    val name: String,
    val role: String,
    val email: String,
    val location: String,
    val rate: String,
    val thisMonthHours: String,
    val status: String,
    val reminderSent: Boolean = false,
)

private const val AdminWorkersPreferences = "admin_workers"

private fun defaultAdminWorkers(): List<AdminWorker> = listOf(
    AdminWorker("Sven de Vries", "Baker", "sven@email.nl", "Bakery floor", "\u20AC16.00 / hour", "16.0h", "Active"),
    AdminWorker("Mila Bakker", "Cashier", "mila@email.nl", "Bakery floor", "\u20AC16.00 / hour", "0.0h", "Pending"),
    AdminWorker("Noah Visser", "Driver", "noah@email.nl", "Warehouse", "\u20AC16.00 / hour", "12.0h", "Active"),
)

private fun loadAdminWorkers(context: Context): List<AdminWorker> {
    val stored = context.getSharedPreferences(AdminWorkersPreferences, Context.MODE_PRIVATE)
        .getString("workers", null)
        ?: return defaultAdminWorkers()
    return stored.lineSequence().mapNotNull { row ->
        val parts = row.split('|')
        if (parts.size >= 7) {
            AdminWorker(
                name = parts[0],
                role = parts[1],
                email = parts[2],
                location = parts[3],
                rate = parts[4],
                thisMonthHours = parts[5],
                status = parts[6],
                reminderSent = parts.getOrNull(7)?.toBooleanStrictOrNull() ?: false,
            )
        } else {
            null
        }
    }.toList().ifEmpty(::defaultAdminWorkers)
}

private fun saveAdminWorkers(context: Context, workers: List<AdminWorker>) {
    context.getSharedPreferences(AdminWorkersPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString(
            "workers",
            workers.joinToString("\n") {
                "${it.name}|${it.role}|${it.email}|${it.location}|${it.rate}|${it.thisMonthHours}|${it.status}|${it.reminderSent}"
            },
        )
        .apply()
}

private fun findAdminWorkerByEmail(context: Context, email: String): AdminWorker? {
    if (email.isBlank()) return null
    return loadAdminWorkers(context).firstOrNull { worker ->
        worker.email.equals(email, ignoreCase = true)
    }
}

private fun submitWorkerSignupForApproval(
    context: Context,
    fullName: String,
    email: String,
    companyCode: String,
) {
    val employer = resolveWorkerEmployerByCode(companyCode)
    val newWorker = AdminWorker(
        name = fullName,
        role = employer.role,
        email = email,
        location = "Bakery floor",
        rate = employer.rate.replace("/hr", " / hour"),
        thisMonthHours = "0.0h",
        status = "Pending",
        reminderSent = false,
    )
    val existingWorkers = loadAdminWorkers(context)
    val updatedWorkers = existingWorkers.map { worker ->
        if (worker.email.equals(email, ignoreCase = true)) {
            if (worker.status == "Active") {
                worker
            } else {
                newWorker
            }
        } else {
            worker
        }
    }.let { workers ->
        if (workers.any { it.email.equals(email, ignoreCase = true) }) workers else workers + newWorker
    }
    saveAdminWorkers(context, updatedWorkers)
}

private fun isWorkerApproved(context: Context, email: String): Boolean {
    return findAdminWorkerByEmail(context, email)?.status == "Active"
}

private enum class NotificationType {
    MessageSent,
    EmployerReply,
    HoursAdjusted,
    WeekApproved,
    AccountApproved,
}

enum class WorkerTab(val label: String) {
    Calendar("Calendar"),
    History("Overview"),
    Alerts("Alerts"),
    Profile("Profile"),
}

private enum class CalendarDayStatus(val color: Color) {
    Approved(Color(0xFF1D9E75)),
    Pending(Color(0xFFEF9F27)),
    Adjusted(Color(0xFF378ADD)),
}

private data class CalendarDayData(
    val status: CalendarDayStatus,
    val hours: String,
)

enum class WeekStatus {
    Approved,
    Pending,
    Adjusted,
}

private val WeekStatus.chatLabel: String
    get() = when (this) {
        WeekStatus.Approved -> "Approved shift"
        WeekStatus.Pending -> "Pending shift"
        WeekStatus.Adjusted -> "Hours adjusted"
    }

@Preview(showBackground = true)
@Composable
private fun AloworkAppPreview() {
    AloworkTheme {
        AloworkApp()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerAwaitingApprovalScreenPreview() {
    AloworkTheme {
        WorkerAwaitingApprovalScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerLoginScreenPreview() {
    AloworkTheme {
        WorkerLoginScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerLocationPermissionScreenPreview() {
    AloworkTheme {
        WorkerLocationPermissionScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsClockInScreenPreview() {
    AloworkTheme {
        GpsClockInScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerCalendarEarningsScreenPreview() {
    AloworkTheme {
        WorkerCalendarEarningsScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerNotificationsScreenPreview() {
    AloworkTheme {
        WorkerNotificationsScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerProfileScreenPreview() {
    AloworkTheme {
        WorkerProfileScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerLanguageSettingsScreenPreview() {
    AloworkTheme {
        WorkerLanguageSettingsScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerChangePasswordScreenPreview() {
    AloworkTheme {
        WorkerChangePasswordScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerHistoryOverviewScreenPreview() {
    AloworkTheme {
        WorkerHistoryOverviewScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerDayViewAdjustedScreenPreview() {
    AloworkTheme {
        WorkerDayViewAdjustedScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerChatWithEmployerScreenPreview() {
    AloworkTheme {
        WorkerChatWithEmployerScreen(
            sentMessages = listOf(
                WorkerShiftMessage(
                    workerName = "Sven de Vries",
                    shiftTitle = "Today",
                    shiftStatus = WeekStatus.Pending.chatLabel,
                    shiftSummary = "3.4h - Submitted with 1 photo",
                    pay = "\u20AC44.42",
                    message = "I worked until 15:30. Could you check it?",
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerLogHoursDayDetailScreenPreview() {
    AloworkTheme {
        WorkerLogHoursDayDetailScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerAddEmployerScreenPreview() {
    AloworkTheme {
        WorkerAddEmployerScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerSwitchEmployerScreenPreview() {
    AloworkTheme {
        WorkerSwitchEmployerScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WebRegisterCompanyDetailsScreenPreview() {
    AloworkTheme {
        WebRegisterCompanyDetailsScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WebRegisterChoosePlanScreenPreview() {
    AloworkTheme {
        WebRegisterChoosePlanScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WebRegisterAdminAccountScreenPreview() {
    AloworkTheme {
        WebRegisterAdminAccountScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WebRegisterSuccessCodeScreenPreview() {
    AloworkTheme {
        WebRegisterSuccessCodeScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminDashboardHomeScreenPreview() {
    AloworkTheme {
        AdminDashboardHomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminHoursApprovalQueueScreenPreview() {
    AloworkTheme {
        AdminHoursApprovalQueueScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminWorkLocationsScreenPreview() {
    AloworkTheme {
        AdminWorkLocationsScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminTeamScreenPreview() {
    AloworkTheme {
        AdminTeamScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsShiftInProgressScreenPreview() {
    AloworkTheme {
        GpsShiftInProgressScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkerShiftPhotosScreenPreview() {
    AloworkTheme {
        WorkerShiftPhotosScreen()
    }
}
