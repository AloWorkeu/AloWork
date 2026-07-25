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
            language = workerLanguage,
            onAccountCreated = { fullName, email, companyCode, password ->
                submitWorkerSignupForApproval(
                    context = context,
                    fullName = fullName,
                    email = email,
                    companyCode = companyCode,
                )
                saveWorkerPassword(context, email, password)
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
            language = workerLanguage,
            onLogin = { email, password ->
                pendingWorkerEmail = email
                if (isWorkerPasswordValid(context, email, password)) {
                    val worker = findAdminWorkerByEmail(context, email)
                    if (worker?.status == "Active") {
                        val approval = WorkerAccountApproval(name = worker.name, email = worker.email)
                        workerAccountApproval = approval
                        saveWorkerAccountApproval(context, approval)
                        screen = AppScreen.WorkerGpsClockIn
                    } else {
                        screen = AppScreen.WorkerAwaitingApproval
                    }
                } else {
                    Toast.makeText(context, workerLanguage.authCopy().emailOrPasswordIncorrect, Toast.LENGTH_SHORT).show()
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
            language = workerLanguage,
            isApproved = isWorkerApproved(context, pendingWorkerEmail),
            onCheckApproval = {
                isWorkerApproved(context, pendingWorkerEmail)
            },
            onApprovalReceived = {
                screen = AppScreen.WorkerLocationPermission
            },
        )

        AppScreen.WorkerLocationPermission -> WorkerLocationPermissionScreen(
            language = workerLanguage,
            onLocationAllowed = {
                screen = AppScreen.WorkerGpsClockIn
            },
            onManualEntry = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
        )

        AppScreen.WorkerGpsClockIn -> GpsClockInScreen(
            language = workerLanguage,
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
            language = workerLanguage,
            hourlyRate = selectedEmployerHourlyRate,
            onClockOut = { elapsedSeconds ->
                pendingGpsShiftSeconds = elapsedSeconds
                screen = AppScreen.WorkerShiftPhotos
            },
        )

        AppScreen.WorkerShiftPhotos -> WorkerShiftPhotosScreen(
            language = workerLanguage,
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
            language = workerLanguage,
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
            language = workerLanguage,
            onLocationEnabled = {
                screen = AppScreen.WorkerGpsClockIn
            },
            onManualEntry = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
        )

        AppScreen.WorkerNotifications -> WorkerNotificationsScreen(
            language = workerLanguage,
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
            language = workerLanguage,
            onBack = {
                screen = AppScreen.WorkerProfile
            },
            onPasswordChanged = { currentPassword, newPassword ->
                if (updateWorkerPassword(context, pendingWorkerEmail, currentPassword, newPassword)) {
                    screen = AppScreen.WorkerProfile
                    true
                } else {
                    false
                }
            },
        )

        AppScreen.WorkerHistoryOverview -> WorkerHistoryOverviewScreen(
            language = workerLanguage,
            manualSubmission = manualHoursSubmission,
            gpsShiftSubmission = gpsShiftSubmission,
            onDaySelected = { dayDetail ->
                selectedWorkerDayDetail = dayDetail
                screen = AppScreen.WorkerDayViewAdjusted
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerDayViewAdjusted -> WorkerDayViewAdjustedScreen(
            language = workerLanguage,
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
            language = workerLanguage,
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
            language = workerLanguage,
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

        AppScreen.WorkerAddEmployer -> {
            val copy = workerLanguage.employerFlowCopy()
            WorkerAddEmployerScreen(
                language = workerLanguage,
                onBack = {
                    screen = AppScreen.WorkerSwitchEmployer
                },
                onCompanyAdded = { companyCode ->
                    val invitedEmployer = findKnownWorkerEmployerByCode(companyCode)
                    if (invitedEmployer == null) {
                        Toast.makeText(context, copy.companyCodeNotFound, Toast.LENGTH_SHORT).show()
                    } else {
                        val existingEmployer = workerEmployers.firstOrNull { it.name == invitedEmployer.name }
                        if (existingEmployer == null) {
                            workerEmployers = workerEmployers + invitedEmployer.copy(status = "Pending")
                            saveWorkerEmployers(context, workerEmployers)
                            Toast.makeText(context, copy.approvalRequestSent, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                copy.alreadyLinked(existingEmployer.name),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        screen = AppScreen.WorkerSwitchEmployer
                    }
                },
            )
        }

        AppScreen.WorkerSwitchEmployer -> {
            val copy = workerLanguage.employerFlowCopy()
            WorkerSwitchEmployerScreen(
                language = workerLanguage,
                employers = workerEmployers,
                selectedEmployerName = selectedEmployerName,
                onBack = {
                    screen = AppScreen.WorkerProfile
                },
                onAddCompany = {
                    screen = AppScreen.WorkerAddEmployer
                },
                onEmployerSelected = { employer ->
                    if (employer.status == "Active") {
                        selectedEmployerName = employer.name
                        saveSelectedEmployerName(context, selectedEmployerName)
                    } else {
                        Toast.makeText(context, copy.employerApprovalPending, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

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
    language: WorkerLanguage = WorkerLanguage.English,
    onAccountCreated: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onLoginSelected: () -> Unit = {},
    onRegisterCompany: () -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.authCopy()
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
                text = copy.createAccountTitle,
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.createAccountDescription,
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SignUpField(
                label = copy.fullName,
                value = fullName,
                onValueChange = { fullName = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = copy.emailAddress,
                value = email,
                onValueChange = { email = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = copy.companyCode,
                value = companyCode,
                onValueChange = { companyCode = it },
                placeholder = copy.companyCodePlaceholder,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = copy.password,
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
                        fullName.isBlank() -> copy.enterFullName
                        email.isBlank() -> copy.enterEmailAddress
                        companyCode.isBlank() -> copy.enterCompanyCode
                        password.isBlank() -> copy.enterPassword
                        else -> null
                    }
                    if (message == null) {
                        onAccountCreated(fullName.trim(), email.trim(), companyCode.trim(), password)
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
                    text = copy.createAccountAction,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = copy.alreadyHaveAccount,
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
                        text = copy.logIn,
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
                    text = copy.registerCompany,
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
    language: WorkerLanguage = WorkerLanguage.English,
    onLogin: (String, String) -> Unit = { _, _ -> },
    onCreateAccount: () -> Unit = {},
    onRegisterCompany: () -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.authCopy()
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
                text = copy.logInTitle,
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.logInDescription,
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SignUpField(
                label = copy.emailAddress,
                value = email,
                onValueChange = { email = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = copy.password,
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
                        email.isBlank() -> copy.enterEmailAddress
                        password.isBlank() -> copy.enterPassword
                        else -> null
                    }
                    if (message == null) {
                        onLogin(email.trim(), password)
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
                    text = copy.logInAction,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = copy.newToAlowork,
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
                        text = copy.createAccountAction,
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
                    text = copy.registerCompany,
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
                    val trimmedCompanyName = companyName.trim()
                    val trimmedIndustry = industry.trim()
                    val trimmedEmail = email.trim()
                    val message = when {
                        trimmedCompanyName.isBlank() -> "Enter your company name"
                        trimmedIndustry.isBlank() -> "Enter your industry"
                        trimmedEmail.isBlank() -> "Enter your work email"
                        !trimmedEmail.isValidEmailAddress() -> "Enter a valid work email"
                        else -> null
                    }
                    if (message == null) {
                        Toast.makeText(context, "Company details saved", Toast.LENGTH_SHORT).show()
                        onContinue(trimmedCompanyName, trimmedIndustry, trimmedEmail)
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
    language: WorkerLanguage = WorkerLanguage.English,
    isApproved: Boolean = false,
    onCheckApproval: () -> Boolean = { isApproved },
    onApprovalReceived: () -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.workerAccessCopy()

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
                text = copy.accountCreatedTitle,
                color = Color(0xFF17171B),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = copy.accountCreatedDescription,
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
                    Toast.makeText(context, copy.stillWaitingApproval, Toast.LENGTH_SHORT).show()
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
                text = copy.checkApproval,
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
    language: WorkerLanguage = WorkerLanguage.English,
    onLocationAllowed: () -> Unit = {},
    onManualEntry: () -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.workerAccessCopy()
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
                copy.locationRequired,
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
                text = copy.enableLocationTitle,
                color = Color(0xFF17171B),
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = copy.locationPermissionDescription,
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
                    text = copy.allowLocation,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onManualEntry()
                    Toast.makeText(context, copy.manualEntrySelected, Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(
                    text = copy.preferManualEntry,
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
    language: WorkerLanguage = WorkerLanguage.English,
    onClockIn: () -> Unit = {},
    onManualEntry: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.workerTimeEntryCopy()

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
                        text = copy.readyToClockIn,
                        color = Color(0xFF17171B),
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = copy.atWorkLocation,
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
                                text = copy.locationConfirmed,
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
                    text = copy.clockInAction,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onManualEntry()
                    Toast.makeText(context, copy.manualEntrySelected, Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(
                    text = copy.enterHoursManually,
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
    language: WorkerLanguage = WorkerLanguage.English,
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    onDaySelected: () -> Unit = {},
    onLogHours: () -> Unit = {},
    onProfileSelected: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val copy = language.workerTabCopy()
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
                    text = copy.greeting,
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
                language = language,
                isCurrentDesignMonth = displayedMonth == YearMonth.of(2026, 6),
                summary = monthSummary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            WorkerMonthCalendar(
                language = language,
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
                text = copy.logTodayHours,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        WorkerCalendarTabBar(
            language = language,
            selected = WorkerTab.Calendar,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WorkerEarningsCard(
    language: WorkerLanguage = WorkerLanguage.English,
    isCurrentDesignMonth: Boolean,
    summary: WorkerMonthSummary,
) {
    val copy = language.workerCalendarCopy()
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
                text = if (isCurrentDesignMonth) copy.earnedInJuneNet else copy.earnedThisMonthNet,
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
                    label = "${if (isCurrentDesignMonth) formatWholeEuro(summary.approvedPay) else "\u20AC0"} ${copy.approved}",
                )
                EarningsLegendItem(
                    color = Color(0xFFEF9F27),
                    label = "${if (isCurrentDesignMonth) formatWholeEuro(summary.pendingPay) else "\u20AC0"} ${copy.pending}",
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
    language: WorkerLanguage = WorkerLanguage.English,
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
    val copy = language.workerCalendarCopy()
    val monthFormatter = remember(copy.locale) { DateTimeFormatter.ofPattern("MMMM yyyy", copy.locale) }

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
                copy.weekdayLabels.forEach { day ->
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
                CalendarLegendItem(CalendarDayStatus.Approved, copy.approved)
                CalendarLegendItem(CalendarDayStatus.Pending, copy.pending)
                CalendarLegendItem(CalendarDayStatus.Adjusted, copy.adjusted)
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
    language: WorkerLanguage = WorkerLanguage.English,
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
                    language = language,
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
    language: WorkerLanguage = WorkerLanguage.English,
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
            text = language.localizedWorkerTabLabel(tab),
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
    language: WorkerLanguage = WorkerLanguage.English,
    sentMessages: List<WorkerShiftMessage> = emptyList(),
    adminRequests: List<AdminHoursRequest> = emptyList(),
    accountApproval: WorkerAccountApproval? = null,
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    onOpenShiftDetail: (WorkerDayDetail) -> Unit = {},
    onOpenShiftChat: (WorkerDayDetail) -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val copy = language.workerTabCopy()
    val alertsCopy = language.workerAlertsCopy()
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
                text = copy.alertsTitle,
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            latestEmployerReply?.let { message ->
                NotificationCard(
                    type = NotificationType.EmployerReply,
                    title = copy.employerReplied,
                    body = employerReplyNotificationBody(message),
                    time = copy.now,
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
                    title = copy.messageSent,
                    body = shortenNotificationBody(message.message),
                    time = copy.now,
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
                    title = if (request.status == "Adjusted") copy.hoursAdjusted else copy.hoursApproved,
                    body = language.workerAdminDecisionBody(request),
                    time = copy.now,
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
                    title = copy.accountApproved,
                    body = "${copy.welcome}, ${approval.name}. ${copy.accountApprovedBody}",
                    time = copy.now,
                    unread = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            NotificationCard(
                type = NotificationType.HoursAdjusted,
                title = copy.hoursAdjusted,
                body = alertsCopy.adjustedSampleBody,
                time = alertsCopy.twoHoursAgo,
                unread = true,
            )
            Spacer(modifier = Modifier.height(10.dp))
            NotificationCard(
                type = NotificationType.WeekApproved,
                title = copy.weekApproved,
                body = alertsCopy.weekApprovedSampleBody,
                time = alertsCopy.yesterday,
                unread = true,
            )
            Spacer(modifier = Modifier.height(10.dp))
            NotificationCard(
                type = NotificationType.AccountApproved,
                title = copy.accountApproved,
                body = accountApproval?.let { alertsCopy.accountActiveBody }
                    ?: alertsCopy.accountApprovedSampleBody,
                time = if (accountApproval == null) alertsCopy.threeDaysAgo else alertsCopy.earlier,
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
    val copy = language.profileCopy()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = copy.title,
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            ProfileHeaderCard()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = copy.paySection,
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileSectionCard {
                ProfileInfoRow(label = copy.company, value = selectedEmployer.name)
                ProfileDivider()
                ProfileInfoRow(label = copy.role, value = language.localizedEmployerRole(selectedEmployer.role))
                ProfileDivider()
                ProfileInfoRow(label = copy.hourlyRate, value = language.localizedEmployerRate(selectedEmployer.rate))
                ProfileDivider()
                ProfileInfoRow(label = copy.status, value = language.localizedEmployerStatus(selectedEmployer.status))
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileSectionCard {
                ProfileActionRow(
                    label = copy.switchCompany,
                    onClick = onSwitchEmployer,
                )
                ProfileDivider()
                ProfileActionRow(
                    label = copy.addCompany,
                    onClick = onAddEmployer,
                )
                ProfileDivider()
                ProfileActionValueRow(
                    label = copy.language,
                    value = language.localName,
                    onClick = onChangeLanguage,
                )
                ProfileDivider()
                ProfileActionRow(
                    label = copy.changePassword,
                    onClick = onChangePassword,
                )
                ProfileDivider()
                ProfileActionRow(
                    label = copy.logOut,
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
    language: WorkerLanguage = WorkerLanguage.English,
    onBack: () -> Unit = {},
    onPasswordChanged: (String, String) -> Boolean = { _, _ -> true },
) {
    val context = LocalContext.current
    val copy = language.changePasswordCopy()
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
                    text = copy.back,
                    color = Color(0xFF73737A),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.title,
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.description,
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            SignUpField(
                label = copy.currentPassword,
                value = currentPassword,
                onValueChange = { currentPassword = it },
                isPassword = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = copy.newPassword,
                value = newPassword,
                onValueChange = { newPassword = it },
                isPassword = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignUpField(
                label = copy.repeatNewPassword,
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                isPassword = true,
            )
        }

        Button(
            onClick = {
                val message = when {
                    currentPassword.isBlank() -> copy.enterCurrentPassword
                    newPassword.length < 8 -> copy.useAtLeastEightCharacters
                    confirmPassword != newPassword -> copy.passwordsDoNotMatch
                    else -> null
                }
                if (message == null) {
                    if (onPasswordChanged(currentPassword, newPassword)) {
                        Toast.makeText(context, copy.passwordChanged, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, copy.currentPasswordIncorrect, Toast.LENGTH_SHORT).show()
                    }
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
                text = copy.savePassword,
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
    val copy = selectedLanguage.languageSettingsCopy()
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
                    text = copy.back,
                    color = Color(0xFF73737A),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.title,
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.description,
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
    language: WorkerLanguage = WorkerLanguage.English,
    manualSubmission: WorkerManualHoursSubmission? = null,
    gpsShiftSubmission: WorkerGpsShiftSubmission? = null,
    onDaySelected: (WorkerDayDetail) -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val copy = language.workerTabCopy()
    val historyCopy = language.workerHistoryCopy()
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
                    text = copy.overviewTitle,
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
                            text = historyCopy.monthLabel,
                            color = Color(0xFF17171B),
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\u2304",
                            color = Color(0xFF73737A),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            MonthlySummaryCard(
                language = language,
                summary = monthSummary,
            )
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
                            language = language,
                            week = "Week 25",
                            amount = pay,
                            detail = "$hours - ${historyCopy.today} - ${language.localizedPhotoCount(submission.photoCount)}",
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
                            language = language,
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
                        language = language,
                        week = "Week 23",
                        amount = "\u20AC608",
                        detail = "38.0 ${historyCopy.hoursShort} - 2-6 Jun",
                        status = WeekStatus.Approved,
                        onClick = {
                            onDaySelected(approvedWorkerDayDetail())
                        },
                    )
                    ProfileDivider()
                    WeekOverviewRow(
                        language = language,
                        week = "Week 24",
                        amount = "\u20AC584",
                        detail = "36.5 ${historyCopy.hoursShort} - 9-13 Jun",
                        status = WeekStatus.Approved,
                        onClick = {
                            onDaySelected(approvedWorkerDayDetail(title = "Wed 10 Jun", hours = "7.5h", pay = "\u20AC120.00"))
                        },
                    )
                    ProfileDivider()
                    WeekOverviewRow(
                        language = language,
                        week = "Week 25",
                        amount = "\u20AC256",
                        detail = "16.0 ${historyCopy.hoursShort} - 16-19 Jun",
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
    language: WorkerLanguage = WorkerLanguage.English,
    dayDetail: WorkerDayDetail = defaultAdjustedWorkerDayDetail(),
    onBack: () -> Unit = {},
    onAskQuestion: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val copy = language.workerShiftDetailCopy()
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
                    text = "\u2039",
                    color = Color(0xFF17171B),
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(30.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = language.localizedWorkerDayTitle(dayDetail.title),
                    color = Color(0xFF17171B),
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                WeekStatusPill(status = dayDetail.status, language = language)
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
                        text = copy.total,
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
                        text = language.localizedWorkerDaySummary(dayDetail),
                        color = Color(0xFFE0A12A),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ProfileSectionCard {
                DayInfoRow(label = copy.clockIn, value = dayDetail.clockIn)
                ProfileDivider()
                DayInfoRow(label = copy.clockOut, value = language.localizedWorkerShiftValue(dayDetail.clockOut))
                ProfileDivider()
                DayInfoRow(label = copy.breakLabel, value = language.localizedWorkerShiftValue(dayDetail.breakLabel))
                ProfileDivider()
                DayInfoRow(label = copy.hourlyRate, value = dayDetail.hourlyRate)
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
                            text = copy.proofPhotos,
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
                                    unavailableLabel = copy.photoUnavailable,
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
                        text = copy.adjustmentNote,
                        color = Color(0xFF17171B),
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = language.localizedWorkerShiftNote(dayDetail),
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
                    text = language.localizedShiftActionLabel(dayDetail.status),
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
    language: WorkerLanguage = WorkerLanguage.English,
    dayDetail: WorkerDayDetail = defaultAdjustedWorkerDayDetail(),
    sentMessages: List<WorkerShiftMessage> = emptyList(),
    onBack: () -> Unit = {},
    onSendMessage: (WorkerShiftMessage) -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val copy = language.workerShiftDetailCopy()
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
                        text = copy.employer,
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
                            text = "${language.localizedWorkerDayTitle(dayDetail.title)} - ${language.localizedWeekStatusChatLabel(dayDetail.status)}",
                            color = Color(0xFF73737A),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = language.localizedWorkerDayChatSummary(dayDetail),
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
                        message = language.localizedEmployerShiftMessage(dayDetail.status),
                        time = "09:12",
                        isWorker = false,
                    )
                }
                item {
                    ChatMessageBubble(
                        message = copy.shiftChatHelper,
                        time = "09:13",
                        isWorker = false,
                    )
                }
                items(sentMessages) { message ->
                    ChatMessageBubble(
                        message = message.message,
                        time = language.workerTabCopy().now,
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
                            text = copy.writeMessage,
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
                                    shiftStatus = language.localizedWeekStatusChatLabel(dayDetail.status),
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
                        text = copy.send,
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
    language: WorkerLanguage = WorkerLanguage.English,
    hourlyRate: Double = 16.0,
    onBack: () -> Unit = {},
    onSubmitted: (String, String) -> Unit = { _, _ -> },
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.workerTimeEntryCopy()
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
                    text = "\u2039",
                    color = Color(0xFF17171B),
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(30.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = copy.logHoursTitle,
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
                        text = copy.manualBadge,
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
                        text = "${copy.estimatedWith} ${formatEuro(hourlyRate)} ${copy.hourlyRateSuffix}",
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
                        label = copy.clockInLabel,
                        value = clockIn,
                        onValueChange = { clockIn = it },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LogHoursField(
                        label = copy.clockOutLabel,
                        value = clockOut,
                        onValueChange = { clockOut = it },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LogHoursField(
                        label = copy.breakLabel,
                        value = breakMinutes,
                        onValueChange = { breakMinutes = it },
                        helper = copy.minutesHelper,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LogHoursField(
                        label = copy.noteLabel,
                        value = note,
                        onValueChange = { note = it },
                        placeholder = copy.optionalPlaceholder,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (totalHours <= 0.0) {
                        Toast.makeText(context, copy.checkStartEndTime, Toast.LENGTH_SHORT).show()
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
                    text = copy.submitHours,
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
    language: WorkerLanguage = WorkerLanguage.English,
    onBack: () -> Unit = {},
    onCompanyAdded: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.employerFlowCopy()
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
                    text = "\u2039",
                    color = Color(0xFF17171B),
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .width(26.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = copy.addCompanyTitle,
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.addCompanyDescription,
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
                        text = copy.companyCodeLabel,
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
                            letterSpacing = 0.sp,
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
                    text = copy.approvalHint,
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
                    Toast.makeText(context, copy.enterSixCharacterCode, Toast.LENGTH_SHORT).show()
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
                text = copy.addCompanyAction,
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
    language: WorkerLanguage = WorkerLanguage.English,
    employers: List<WorkerEmployer> = defaultWorkerEmployers(),
    selectedEmployerName: String = "Bakkerij Jansen",
    onBack: () -> Unit = {},
    onAddCompany: () -> Unit = {},
    onEmployerSelected: (WorkerEmployer) -> Unit = {},
) {
    val copy = language.employerFlowCopy()
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
                    text = "\u2039",
                    color = Color(0xFF17171B),
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .width(26.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = copy.switchEmployerTitle,
                    color = Color(0xFF17171B),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = copy.switchEmployerDescription,
                color = Color(0xFF73737A),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            employers.forEach { employer ->
                EmployerChoiceRow(
                    language = language,
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
                text = copy.addCompanyLink,
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
    language: WorkerLanguage,
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
                    text = "${language.localizedEmployerRole(employer.role)} - ${language.localizedEmployerRate(employer.rate)} - ${language.localizedEmployerStatus(employer.status)}",
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
    language: WorkerLanguage = WorkerLanguage.English,
    summary: WorkerMonthSummary = workerMonthSummary(),
    modifier: Modifier = Modifier,
) {
    val copy = language.workerHistoryCopy()
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
                text = copy.monthTotalNet,
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
                    label = copy.hoursWorked,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                SummaryMetricTile(
                    value = summary.workdays.toString(),
                    label = copy.workdays,
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
    language: WorkerLanguage = WorkerLanguage.English,
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
            WeekStatusPill(status = status, language = language)
        }
    }
}

@Composable
private fun WeekStatusPill(
    status: WeekStatus,
    language: WorkerLanguage = WorkerLanguage.English,
) {
    val label = language.localizedShortWeekStatus(status)
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
            text = "\u203A",
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
    language: WorkerLanguage = WorkerLanguage.English,
    onLocationEnabled: () -> Unit = {},
    onManualEntry: () -> Unit = {},
) {
    val context = LocalContext.current
    val copy = language.workerAccessCopy()
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
                copy.locationRequired,
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
                text = copy.cantClockInTitle,
                color = Color(0xFF17171B),
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = copy.locationDeniedDescription,
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
                    text = copy.enableLocationAction,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = {
                    onManualEntry()
                    Toast.makeText(context, copy.manualEntrySelected, Toast.LENGTH_SHORT).show()
                },
            ) {
                Text(
                    text = copy.enterHoursManually,
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
    language: WorkerLanguage = WorkerLanguage.English,
    hourlyRate: Double = 13.05,
    onClockOut: (Long) -> Unit = {},
) {
    val copy = language.workerShiftFlowCopy()
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
            ShiftStatusPill(status = copy.shiftInProgressStatus)
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
                        text = copy.timeWorked,
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
                        text = "${copy.earningSoFarPrefix} - ${formatCurrency(earnings)}",
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
                text = copy.clockOut,
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
    language: WorkerLanguage = WorkerLanguage.English,
    photoUris: List<Uri> = emptyList(),
    onBack: () -> Unit = {},
    onPhotoAdded: (Uri) -> Unit = {},
    onPhotoRemoved: (Uri) -> Unit = {},
    onSave: () -> Unit = {},
) {
    val copy = language.workerShiftFlowCopy()
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
                    text = copy.shiftPhotosTitle,
                    color = Color(0xFF17171C),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = copy.shiftPhotosDescription,
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
                                    label = copy.addPhoto,
                                    modifier = Modifier.weight(1f),
                                    onClick = { photoPicker.launch("image/*") },
                                )
                            } else {
                                ShiftPhotoTile(
                                    uri = uri,
                                    unavailableLabel = copy.photoUnavailable,
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
                text = copy.save,
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
    label: String = "Add photo",
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
            text = label,
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
    unavailableLabel: String = "Photo unavailable",
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
                    text = unavailableLabel,
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
    unavailableLabel: String = "Photo unavailable",
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
                text = unavailableLabel,
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
private fun ShiftStatusPill(status: String) {
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
                text = status,
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
    return findKnownWorkerEmployerByCode(companyCode)
        ?: WorkerEmployer("Company ${companyCode.uppercase(Locale.US)}", "Pending approval", "Rate pending", "Pending")
}

private fun findKnownWorkerEmployerByCode(companyCode: String): WorkerEmployer? {
    return when (companyCode.uppercase(Locale.US)) {
        "JANS26" -> WorkerEmployer("Bakkerij Jansen", "Shift worker", "\u20AC16.00/hr", "Active")
        "CAFE24" -> WorkerEmployer("Cafe De Hoek", "Service", "\u20AC14.50/hr", "Active")
        "BOS013" -> WorkerEmployer("Tuincentrum Bos", "Weekend help", "\u20AC13.00/hr", "Active")
        "ROOS24" -> WorkerEmployer("Roos Logistics", "Warehouse assistant", "\u20AC15.25/hr", "Pending")
        else -> null
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

private data class WorkerAuthCopy(
    val createAccountTitle: String,
    val createAccountDescription: String,
    val fullName: String,
    val emailAddress: String,
    val companyCode: String,
    val companyCodePlaceholder: String,
    val password: String,
    val createAccountAction: String,
    val alreadyHaveAccount: String,
    val logIn: String,
    val registerCompany: String,
    val logInTitle: String,
    val logInDescription: String,
    val logInAction: String,
    val newToAlowork: String,
    val enterFullName: String,
    val enterEmailAddress: String,
    val enterCompanyCode: String,
    val enterPassword: String,
    val emailOrPasswordIncorrect: String,
)

private fun WorkerLanguage.authCopy(): WorkerAuthCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerAuthCopy(
            createAccountTitle = "Create account",
            createAccountDescription = "Create an account. Your employer approves it before\nyou can start.",
            fullName = "Full name",
            emailAddress = "Email address",
            companyCode = "Company code",
            companyCodePlaceholder = "Received from your employer",
            password = "Password",
            createAccountAction = "Create account",
            alreadyHaveAccount = "Already have an account?",
            logIn = "Log in",
            registerCompany = "Register a company",
            logInTitle = "Log in",
            logInDescription = "Log in to clock hours, view shifts, and send updates\nto your employer.",
            logInAction = "Log in",
            newToAlowork = "New to Alowork?",
            enterFullName = "Enter your full name",
            enterEmailAddress = "Enter your email address",
            enterCompanyCode = "Enter your company code",
            enterPassword = "Enter your password",
            emailOrPasswordIncorrect = "Email or password is incorrect",
        )
        WorkerLanguage.Dutch -> WorkerAuthCopy(
            createAccountTitle = "Account maken",
            createAccountDescription = "Maak een account. Je werkgever keurt dit goed\nvoordat je kunt starten.",
            fullName = "Volledige naam",
            emailAddress = "E-mailadres",
            companyCode = "Bedrijfscode",
            companyCodePlaceholder = "Ontvangen van je werkgever",
            password = "Wachtwoord",
            createAccountAction = "Account maken",
            alreadyHaveAccount = "Heb je al een account?",
            logIn = "Inloggen",
            registerCompany = "Bedrijf registreren",
            logInTitle = "Inloggen",
            logInDescription = "Log in om uren te klokken, diensten te bekijken en updates\nnaar je werkgever te sturen.",
            logInAction = "Inloggen",
            newToAlowork = "Nieuw bij Alowork?",
            enterFullName = "Vul je volledige naam in",
            enterEmailAddress = "Vul je e-mailadres in",
            enterCompanyCode = "Vul je bedrijfscode in",
            enterPassword = "Vul je wachtwoord in",
            emailOrPasswordIncorrect = "E-mail of wachtwoord is onjuist",
        )
        WorkerLanguage.German -> WorkerAuthCopy(
            createAccountTitle = "Konto erstellen",
            createAccountDescription = "Erstelle ein Konto. Dein Arbeitgeber genehmigt es,\nbevor du starten kannst.",
            fullName = "Vollstandiger Name",
            emailAddress = "E-Mail-Adresse",
            companyCode = "Firmencode",
            companyCodePlaceholder = "Von deinem Arbeitgeber erhalten",
            password = "Passwort",
            createAccountAction = "Konto erstellen",
            alreadyHaveAccount = "Hast du schon ein Konto?",
            logIn = "Anmelden",
            registerCompany = "Firma registrieren",
            logInTitle = "Anmelden",
            logInDescription = "Melde dich an, um Stunden zu erfassen, Schichten anzusehen\nund Updates an deinen Arbeitgeber zu senden.",
            logInAction = "Anmelden",
            newToAlowork = "Neu bei Alowork?",
            enterFullName = "Gib deinen vollstandigen Namen ein",
            enterEmailAddress = "Gib deine E-Mail-Adresse ein",
            enterCompanyCode = "Gib deinen Firmencode ein",
            enterPassword = "Gib dein Passwort ein",
            emailOrPasswordIncorrect = "E-Mail oder Passwort ist falsch",
        )
        WorkerLanguage.French -> WorkerAuthCopy(
            createAccountTitle = "Creer un compte",
            createAccountDescription = "Creez un compte. Votre employeur l'approuve avant\nque vous puissiez commencer.",
            fullName = "Nom complet",
            emailAddress = "Adresse e-mail",
            companyCode = "Code entreprise",
            companyCodePlaceholder = "Recu de votre employeur",
            password = "Mot de passe",
            createAccountAction = "Creer un compte",
            alreadyHaveAccount = "Vous avez deja un compte ?",
            logIn = "Connexion",
            registerCompany = "Inscrire une entreprise",
            logInTitle = "Connexion",
            logInDescription = "Connectez-vous pour pointer vos heures, voir vos shifts\net envoyer des nouvelles a votre employeur.",
            logInAction = "Connexion",
            newToAlowork = "Nouveau sur Alowork ?",
            enterFullName = "Saisissez votre nom complet",
            enterEmailAddress = "Saisissez votre adresse e-mail",
            enterCompanyCode = "Saisissez votre code entreprise",
            enterPassword = "Saisissez votre mot de passe",
            emailOrPasswordIncorrect = "E-mail ou mot de passe incorrect",
        )
    }
}

private data class WorkerProfileCopy(
    val title: String,
    val paySection: String,
    val company: String,
    val role: String,
    val hourlyRate: String,
    val status: String,
    val switchCompany: String,
    val addCompany: String,
    val language: String,
    val changePassword: String,
    val logOut: String,
)

private fun WorkerLanguage.profileCopy(): WorkerProfileCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerProfileCopy(
            title = "Profile",
            paySection = "Pay (selected employer)",
            company = "Company",
            role = "Role",
            hourlyRate = "Hourly rate",
            status = "Status",
            switchCompany = "Switch company",
            addCompany = "Add company",
            language = "Language",
            changePassword = "Change password",
            logOut = "Log out",
        )
        WorkerLanguage.Dutch -> WorkerProfileCopy(
            title = "Profiel",
            paySection = "Loon (gekozen werkgever)",
            company = "Bedrijf",
            role = "Functie",
            hourlyRate = "Uurloon",
            status = "Status",
            switchCompany = "Wissel bedrijf",
            addCompany = "Bedrijf toevoegen",
            language = "Taal",
            changePassword = "Wachtwoord wijzigen",
            logOut = "Uitloggen",
        )
        WorkerLanguage.German -> WorkerProfileCopy(
            title = "Profil",
            paySection = "Lohn (ausgewahlter Arbeitgeber)",
            company = "Firma",
            role = "Rolle",
            hourlyRate = "Stundenlohn",
            status = "Status",
            switchCompany = "Firma wechseln",
            addCompany = "Firma hinzufugen",
            language = "Sprache",
            changePassword = "Passwort andern",
            logOut = "Abmelden",
        )
        WorkerLanguage.French -> WorkerProfileCopy(
            title = "Profil",
            paySection = "Paie (employeur choisi)",
            company = "Entreprise",
            role = "Role",
            hourlyRate = "Taux horaire",
            status = "Statut",
            switchCompany = "Changer d'entreprise",
            addCompany = "Ajouter une entreprise",
            language = "Langue",
            changePassword = "Modifier le mot de passe",
            logOut = "Se deconnecter",
        )
    }
}

private data class WorkerChangePasswordCopy(
    val back: String,
    val title: String,
    val description: String,
    val currentPassword: String,
    val newPassword: String,
    val repeatNewPassword: String,
    val enterCurrentPassword: String,
    val useAtLeastEightCharacters: String,
    val passwordsDoNotMatch: String,
    val passwordChanged: String,
    val currentPasswordIncorrect: String,
    val savePassword: String,
)

private fun WorkerLanguage.changePasswordCopy(): WorkerChangePasswordCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerChangePasswordCopy(
            back = "Back",
            title = "Change password",
            description = "Use at least 8 characters. Your new password must match before it can be saved.",
            currentPassword = "Current password",
            newPassword = "New password",
            repeatNewPassword = "Repeat new password",
            enterCurrentPassword = "Enter your current password",
            useAtLeastEightCharacters = "Use at least 8 characters",
            passwordsDoNotMatch = "Passwords do not match",
            passwordChanged = "Password changed",
            currentPasswordIncorrect = "Current password is incorrect",
            savePassword = "Save password",
        )
        WorkerLanguage.Dutch -> WorkerChangePasswordCopy(
            back = "Terug",
            title = "Wachtwoord wijzigen",
            description = "Gebruik minimaal 8 tekens. Je nieuwe wachtwoord moet overeenkomen voordat het kan worden opgeslagen.",
            currentPassword = "Huidig wachtwoord",
            newPassword = "Nieuw wachtwoord",
            repeatNewPassword = "Herhaal nieuw wachtwoord",
            enterCurrentPassword = "Voer je huidige wachtwoord in",
            useAtLeastEightCharacters = "Gebruik minimaal 8 tekens",
            passwordsDoNotMatch = "Wachtwoorden komen niet overeen",
            passwordChanged = "Wachtwoord gewijzigd",
            currentPasswordIncorrect = "Huidig wachtwoord is onjuist",
            savePassword = "Wachtwoord opslaan",
        )
        WorkerLanguage.German -> WorkerChangePasswordCopy(
            back = "Zuruck",
            title = "Passwort andern",
            description = "Nutze mindestens 8 Zeichen. Dein neues Passwort muss ubereinstimmen, bevor es gespeichert wird.",
            currentPassword = "Aktuelles Passwort",
            newPassword = "Neues Passwort",
            repeatNewPassword = "Neues Passwort wiederholen",
            enterCurrentPassword = "Gib dein aktuelles Passwort ein",
            useAtLeastEightCharacters = "Nutze mindestens 8 Zeichen",
            passwordsDoNotMatch = "Passworter stimmen nicht uberein",
            passwordChanged = "Passwort geandert",
            currentPasswordIncorrect = "Aktuelles Passwort ist falsch",
            savePassword = "Passwort speichern",
        )
        WorkerLanguage.French -> WorkerChangePasswordCopy(
            back = "Retour",
            title = "Modifier le mot de passe",
            description = "Utilisez au moins 8 caracteres. Le nouveau mot de passe doit correspondre avant enregistrement.",
            currentPassword = "Mot de passe actuel",
            newPassword = "Nouveau mot de passe",
            repeatNewPassword = "Repeter le nouveau mot de passe",
            enterCurrentPassword = "Saisissez votre mot de passe actuel",
            useAtLeastEightCharacters = "Utilisez au moins 8 caracteres",
            passwordsDoNotMatch = "Les mots de passe ne correspondent pas",
            passwordChanged = "Mot de passe modifie",
            currentPasswordIncorrect = "Le mot de passe actuel est incorrect",
            savePassword = "Enregistrer le mot de passe",
        )
    }
}

private data class WorkerAccessCopy(
    val accountCreatedTitle: String,
    val accountCreatedDescription: String,
    val stillWaitingApproval: String,
    val checkApproval: String,
    val locationRequired: String,
    val enableLocationTitle: String,
    val locationPermissionDescription: String,
    val allowLocation: String,
    val preferManualEntry: String,
    val manualEntrySelected: String,
    val cantClockInTitle: String,
    val locationDeniedDescription: String,
    val enableLocationAction: String,
    val enterHoursManually: String,
)

private fun WorkerLanguage.workerAccessCopy(): WorkerAccessCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerAccessCopy(
            accountCreatedTitle = "Account created",
            accountCreatedDescription = "Your account has been created. Your employer still needs to approve it. You'll be notified once you can start.",
            stillWaitingApproval = "Still waiting for employer approval",
            checkApproval = "Check approval",
            locationRequired = "Location access is required to clock in",
            enableLocationTitle = "Enable location",
            locationPermissionDescription = "To clock in at the workplace we need your location. It's only used to confirm your shift, not to track you.",
            allowLocation = "Allow location",
            preferManualEntry = "Prefer to enter hours manually",
            manualEntrySelected = "Manual entry selected",
            cantClockInTitle = "Can't clock in",
            locationDeniedDescription = "Without location access you can't clock in. Enable location in your settings, or enter your hours manually.",
            enableLocationAction = "Enable location",
            enterHoursManually = "Enter hours manually",
        )
        WorkerLanguage.Dutch -> WorkerAccessCopy(
            accountCreatedTitle = "Account aangemaakt",
            accountCreatedDescription = "Je account is aangemaakt. Je werkgever moet het nog goedkeuren. Je krijgt bericht zodra je kunt starten.",
            stillWaitingApproval = "Nog in afwachting van goedkeuring",
            checkApproval = "Goedkeuring controleren",
            locationRequired = "Locatie is nodig om in te klokken",
            enableLocationTitle = "Locatie inschakelen",
            locationPermissionDescription = "Om op het werk in te klokken hebben we je locatie nodig. Die wordt alleen gebruikt om je dienst te bevestigen.",
            allowLocation = "Locatie toestaan",
            preferManualEntry = "Liever uren handmatig invoeren",
            manualEntrySelected = "Handmatige invoer geselecteerd",
            cantClockInTitle = "Kan niet inklokken",
            locationDeniedDescription = "Zonder locatie kun je niet inklokken. Schakel locatie in bij instellingen of voer je uren handmatig in.",
            enableLocationAction = "Locatie inschakelen",
            enterHoursManually = "Uren handmatig invoeren",
        )
        WorkerLanguage.German -> WorkerAccessCopy(
            accountCreatedTitle = "Konto erstellt",
            accountCreatedDescription = "Dein Konto wurde erstellt. Dein Arbeitgeber muss es noch genehmigen. Du wirst benachrichtigt, sobald du starten kannst.",
            stillWaitingApproval = "Wartet noch auf Arbeitgeber-Genehmigung",
            checkApproval = "Genehmigung prufen",
            locationRequired = "Standortzugriff ist zum Einstempeln erforderlich",
            enableLocationTitle = "Standort aktivieren",
            locationPermissionDescription = "Zum Einstempeln am Arbeitsplatz benotigen wir deinen Standort. Er wird nur zur Bestatigung deiner Schicht genutzt.",
            allowLocation = "Standort erlauben",
            preferManualEntry = "Stunden lieber manuell eingeben",
            manualEntrySelected = "Manuelle Eingabe ausgewahlt",
            cantClockInTitle = "Einstempeln nicht moglich",
            locationDeniedDescription = "Ohne Standortzugriff kannst du nicht einstempeln. Aktiviere den Standort in den Einstellungen oder gib deine Stunden manuell ein.",
            enableLocationAction = "Standort aktivieren",
            enterHoursManually = "Stunden manuell erfassen",
        )
        WorkerLanguage.French -> WorkerAccessCopy(
            accountCreatedTitle = "Compte cree",
            accountCreatedDescription = "Votre compte a ete cree. Votre employeur doit encore l'approuver. Vous serez averti des que vous pouvez commencer.",
            stillWaitingApproval = "Approbation employeur encore en attente",
            checkApproval = "Verifier l'approbation",
            locationRequired = "L'acces a la localisation est requis pour pointer",
            enableLocationTitle = "Activer la localisation",
            locationPermissionDescription = "Pour pointer au travail, nous avons besoin de votre localisation. Elle sert uniquement a confirmer votre service.",
            allowLocation = "Autoriser la localisation",
            preferManualEntry = "Preferer saisir les heures manuellement",
            manualEntrySelected = "Saisie manuelle selectionnee",
            cantClockInTitle = "Impossible de pointer",
            locationDeniedDescription = "Sans localisation, vous ne pouvez pas pointer. Activez-la dans les reglages ou saisissez vos heures manuellement.",
            enableLocationAction = "Activer la localisation",
            enterHoursManually = "Saisir les heures manuellement",
        )
    }
}

private data class WorkerEmployerFlowCopy(
    val addCompanyTitle: String,
    val addCompanyDescription: String,
    val companyCodeLabel: String,
    val approvalHint: String,
    val enterSixCharacterCode: String,
    val addCompanyAction: String,
    val switchEmployerTitle: String,
    val switchEmployerDescription: String,
    val addCompanyLink: String,
    val companyCodeNotFound: String,
    val approvalRequestSent: String,
    val employerApprovalPending: String,
    val alreadyLinked: (String) -> String,
)

private fun WorkerLanguage.employerFlowCopy(): WorkerEmployerFlowCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerEmployerFlowCopy(
            addCompanyTitle = "Add company",
            addCompanyDescription = "Enter the company code you received from your employer.",
            companyCodeLabel = "Company code",
            approvalHint = "After adding, the employer still needs to approve you.",
            enterSixCharacterCode = "Enter the 6-character company code",
            addCompanyAction = "Add company",
            switchEmployerTitle = "Switch employer",
            switchEmployerDescription = "You work for several companies. Choose which one to use.",
            addCompanyLink = "+ Add company",
            companyCodeNotFound = "Company code not found",
            approvalRequestSent = "Approval request sent",
            employerApprovalPending = "Employer approval is still pending",
            alreadyLinked = { name -> "$name is already linked" },
        )
        WorkerLanguage.Dutch -> WorkerEmployerFlowCopy(
            addCompanyTitle = "Bedrijf toevoegen",
            addCompanyDescription = "Voer de bedrijfscode in die je van je werkgever hebt gekregen.",
            companyCodeLabel = "Bedrijfscode",
            approvalHint = "Na het toevoegen moet de werkgever je nog goedkeuren.",
            enterSixCharacterCode = "Voer de 6-tekens bedrijfscode in",
            addCompanyAction = "Bedrijf toevoegen",
            switchEmployerTitle = "Wissel werkgever",
            switchEmployerDescription = "Je werkt voor meerdere bedrijven. Kies welke je wilt gebruiken.",
            addCompanyLink = "+ Bedrijf toevoegen",
            companyCodeNotFound = "Bedrijfscode niet gevonden",
            approvalRequestSent = "Goedkeuringsverzoek verzonden",
            employerApprovalPending = "Goedkeuring door werkgever is nog in behandeling",
            alreadyLinked = { name -> "$name is al gekoppeld" },
        )
        WorkerLanguage.German -> WorkerEmployerFlowCopy(
            addCompanyTitle = "Firma hinzufugen",
            addCompanyDescription = "Gib den Firmencode ein, den du von deinem Arbeitgeber erhalten hast.",
            companyCodeLabel = "Firmencode",
            approvalHint = "Nach dem Hinzufugen muss der Arbeitgeber dich noch genehmigen.",
            enterSixCharacterCode = "Gib den 6-stelligen Firmencode ein",
            addCompanyAction = "Firma hinzufugen",
            switchEmployerTitle = "Arbeitgeber wechseln",
            switchEmployerDescription = "Du arbeitest fur mehrere Firmen. Wahle aus, welche du verwenden willst.",
            addCompanyLink = "+ Firma hinzufugen",
            companyCodeNotFound = "Firmencode nicht gefunden",
            approvalRequestSent = "Genehmigungsanfrage gesendet",
            employerApprovalPending = "Arbeitgeber-Genehmigung ist noch offen",
            alreadyLinked = { name -> "$name ist bereits verknupft" },
        )
        WorkerLanguage.French -> WorkerEmployerFlowCopy(
            addCompanyTitle = "Ajouter une entreprise",
            addCompanyDescription = "Saisissez le code d'entreprise recu de votre employeur.",
            companyCodeLabel = "Code entreprise",
            approvalHint = "Apres l'ajout, l'employeur doit encore vous approuver.",
            enterSixCharacterCode = "Saisissez le code entreprise a 6 caracteres",
            addCompanyAction = "Ajouter l'entreprise",
            switchEmployerTitle = "Changer d'employeur",
            switchEmployerDescription = "Vous travaillez pour plusieurs entreprises. Choisissez celle a utiliser.",
            addCompanyLink = "+ Ajouter une entreprise",
            companyCodeNotFound = "Code entreprise introuvable",
            approvalRequestSent = "Demande d'approbation envoyee",
            employerApprovalPending = "L'approbation de l'employeur est encore en attente",
            alreadyLinked = { name -> "$name est deja lie" },
        )
    }
}

private fun WorkerLanguage.localizedEmployerStatus(status: String): String {
    return when (status) {
        "Active" -> when (this) {
            WorkerLanguage.English -> "active"
            WorkerLanguage.Dutch -> "actief"
            WorkerLanguage.German -> "aktiv"
            WorkerLanguage.French -> "actif"
        }
        "Pending" -> when (this) {
            WorkerLanguage.English -> "pending"
            WorkerLanguage.Dutch -> "in behandeling"
            WorkerLanguage.German -> "offen"
            WorkerLanguage.French -> "en attente"
        }
        else -> status.lowercase(Locale.US)
    }
}

private fun WorkerLanguage.localizedEmployerRole(role: String): String {
    return when (role) {
        "Shift worker" -> when (this) {
            WorkerLanguage.English -> "Shift worker"
            WorkerLanguage.Dutch -> "Medewerker diensten"
            WorkerLanguage.German -> "Schichtarbeiter"
            WorkerLanguage.French -> "Employe de service"
        }
        "Service" -> when (this) {
            WorkerLanguage.English -> "Service"
            WorkerLanguage.Dutch -> "Bediening"
            WorkerLanguage.German -> "Service"
            WorkerLanguage.French -> "Service"
        }
        "Weekend help" -> when (this) {
            WorkerLanguage.English -> "Weekend help"
            WorkerLanguage.Dutch -> "Weekendhulp"
            WorkerLanguage.German -> "Wochenendhilfe"
            WorkerLanguage.French -> "Aide week-end"
        }
        "Warehouse assistant" -> when (this) {
            WorkerLanguage.English -> "Warehouse assistant"
            WorkerLanguage.Dutch -> "Magazijnmedewerker"
            WorkerLanguage.German -> "Lagerassistent"
            WorkerLanguage.French -> "Assistant entrepot"
        }
        "Pending approval" -> when (this) {
            WorkerLanguage.English -> "Pending approval"
            WorkerLanguage.Dutch -> "Wacht op goedkeuring"
            WorkerLanguage.German -> "Wartet auf Genehmigung"
            WorkerLanguage.French -> "En attente d'approbation"
        }
        else -> role
    }
}

private fun WorkerLanguage.localizedEmployerRate(rate: String): String {
    return when (rate) {
        "Rate pending" -> when (this) {
            WorkerLanguage.English -> "Rate pending"
            WorkerLanguage.Dutch -> "Uurloon in behandeling"
            WorkerLanguage.German -> "Lohn ausstehend"
            WorkerLanguage.French -> "Taux en attente"
        }
        else -> rate
    }
}

private data class WorkerTabCopy(
    val greeting: String,
    val logTodayHours: String,
    val overviewTitle: String,
    val alertsTitle: String,
    val employerReplied: String,
    val messageSent: String,
    val hoursAdjusted: String,
    val hoursApproved: String,
    val weekApproved: String,
    val accountApproved: String,
    val accountApprovedBody: String,
    val welcome: String,
    val now: String,
)

private fun WorkerLanguage.workerTabCopy(): WorkerTabCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerTabCopy(
            greeting = "Hi, Sven",
            logTodayHours = "+  Log today's hours",
            overviewTitle = "Overview",
            alertsTitle = "Alerts",
            employerReplied = "Employer replied",
            messageSent = "Message sent",
            hoursAdjusted = "Hours adjusted",
            hoursApproved = "Hours approved",
            weekApproved = "Week approved",
            accountApproved = "Account approved",
            accountApprovedBody = "Your employer approved your account.",
            welcome = "Welcome",
            now = "Now",
        )
        WorkerLanguage.Dutch -> WorkerTabCopy(
            greeting = "Hoi, Sven",
            logTodayHours = "+  Uren van vandaag loggen",
            overviewTitle = "Overzicht",
            alertsTitle = "Meldingen",
            employerReplied = "Werkgever reageerde",
            messageSent = "Bericht verzonden",
            hoursAdjusted = "Uren aangepast",
            hoursApproved = "Uren goedgekeurd",
            weekApproved = "Week goedgekeurd",
            accountApproved = "Account goedgekeurd",
            accountApprovedBody = "Je werkgever heeft je account goedgekeurd.",
            welcome = "Welkom",
            now = "Nu",
        )
        WorkerLanguage.German -> WorkerTabCopy(
            greeting = "Hallo, Sven",
            logTodayHours = "+  Heutige Stunden erfassen",
            overviewTitle = "Ubersicht",
            alertsTitle = "Hinweise",
            employerReplied = "Arbeitgeber hat geantwortet",
            messageSent = "Nachricht gesendet",
            hoursAdjusted = "Stunden angepasst",
            hoursApproved = "Stunden genehmigt",
            weekApproved = "Woche genehmigt",
            accountApproved = "Konto genehmigt",
            accountApprovedBody = "Dein Arbeitgeber hat dein Konto genehmigt.",
            welcome = "Willkommen",
            now = "Jetzt",
        )
        WorkerLanguage.French -> WorkerTabCopy(
            greeting = "Bonjour, Sven",
            logTodayHours = "+  Saisir les heures du jour",
            overviewTitle = "Apercu",
            alertsTitle = "Alertes",
            employerReplied = "L'employeur a repondu",
            messageSent = "Message envoye",
            hoursAdjusted = "Heures modifiees",
            hoursApproved = "Heures approuvees",
            weekApproved = "Semaine approuvee",
            accountApproved = "Compte approuve",
            accountApprovedBody = "Votre employeur a approuve votre compte.",
            welcome = "Bienvenue",
            now = "Maintenant",
        )
    }
}

private fun WorkerLanguage.localizedWorkerTabLabel(tab: WorkerTab): String {
    return when (tab) {
        WorkerTab.Calendar -> when (this) {
            WorkerLanguage.English -> "Calendar"
            WorkerLanguage.Dutch -> "Kalender"
            WorkerLanguage.German -> "Kalender"
            WorkerLanguage.French -> "Calendrier"
        }
        WorkerTab.History -> when (this) {
            WorkerLanguage.English -> "Overview"
            WorkerLanguage.Dutch -> "Overzicht"
            WorkerLanguage.German -> "Ubersicht"
            WorkerLanguage.French -> "Apercu"
        }
        WorkerTab.Alerts -> when (this) {
            WorkerLanguage.English -> "Alerts"
            WorkerLanguage.Dutch -> "Meldingen"
            WorkerLanguage.German -> "Hinweise"
            WorkerLanguage.French -> "Alertes"
        }
        WorkerTab.Profile -> when (this) {
            WorkerLanguage.English -> "Profile"
            WorkerLanguage.Dutch -> "Profiel"
            WorkerLanguage.German -> "Profil"
            WorkerLanguage.French -> "Profil"
        }
    }
}

private data class WorkerAlertsCopy(
    val adjustedSampleBody: String,
    val weekApprovedSampleBody: String,
    val accountActiveBody: String,
    val accountApprovedSampleBody: String,
    val twoHoursAgo: String,
    val yesterday: String,
    val threeDaysAgo: String,
    val earlier: String,
    val adjustedBody: (AdminHoursRequest) -> String,
    val approvedBody: (AdminHoursRequest) -> String,
)

private fun WorkerLanguage.workerAlertsCopy(): WorkerAlertsCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerAlertsCopy(
            adjustedSampleBody = "Your hours for 5 June were adjusted to 6.0\nhrs.",
            weekApprovedSampleBody = "Your hours for week 23 were approved.\n\u20AC608.",
            accountActiveBody = "Your worker account is active.",
            accountApprovedSampleBody = "Welcome! Your employer approved your\naccount.",
            twoHoursAgo = "2 hours ago",
            yesterday = "yesterday",
            threeDaysAgo = "3 days ago",
            earlier = "Earlier",
            adjustedBody = { request -> "Your ${request.period} hours were adjusted to ${request.hours} (${request.pay})." },
            approvedBody = { request -> "Your ${request.period} hours were approved (${request.hours}, ${request.pay})." },
        )
        WorkerLanguage.Dutch -> WorkerAlertsCopy(
            adjustedSampleBody = "Je uren voor 5 juni zijn aangepast naar 6,0\nuur.",
            weekApprovedSampleBody = "Je uren voor week 23 zijn goedgekeurd.\n\u20AC608.",
            accountActiveBody = "Je werknemersaccount is actief.",
            accountApprovedSampleBody = "Welkom! Je werkgever heeft je account\ngoedgekeurd.",
            twoHoursAgo = "2 uur geleden",
            yesterday = "gisteren",
            threeDaysAgo = "3 dagen geleden",
            earlier = "Eerder",
            adjustedBody = { request -> "Je uren voor ${request.period} zijn aangepast naar ${request.hours} (${request.pay})." },
            approvedBody = { request -> "Je uren voor ${request.period} zijn goedgekeurd (${request.hours}, ${request.pay})." },
        )
        WorkerLanguage.German -> WorkerAlertsCopy(
            adjustedSampleBody = "Deine Stunden fur den 5. Juni wurden auf 6,0\nStd. angepasst.",
            weekApprovedSampleBody = "Deine Stunden fur Woche 23 wurden genehmigt.\n\u20AC608.",
            accountActiveBody = "Dein Mitarbeiterkonto ist aktiv.",
            accountApprovedSampleBody = "Willkommen! Dein Arbeitgeber hat dein\nKonto genehmigt.",
            twoHoursAgo = "vor 2 Stunden",
            yesterday = "gestern",
            threeDaysAgo = "vor 3 Tagen",
            earlier = "Fruher",
            adjustedBody = { request -> "Deine Stunden fur ${request.period} wurden auf ${request.hours} (${request.pay}) angepasst." },
            approvedBody = { request -> "Deine Stunden fur ${request.period} wurden genehmigt (${request.hours}, ${request.pay})." },
        )
        WorkerLanguage.French -> WorkerAlertsCopy(
            adjustedSampleBody = "Vos heures du 5 juin ont ete modifiees a 6,0\nh.",
            weekApprovedSampleBody = "Vos heures de la semaine 23 ont ete approuvees.\n\u20AC608.",
            accountActiveBody = "Votre compte travailleur est actif.",
            accountApprovedSampleBody = "Bienvenue! Votre employeur a approuve\nvotre compte.",
            twoHoursAgo = "il y a 2 heures",
            yesterday = "hier",
            threeDaysAgo = "il y a 3 jours",
            earlier = "Plus tot",
            adjustedBody = { request -> "Vos heures pour ${request.period} ont ete modifiees a ${request.hours} (${request.pay})." },
            approvedBody = { request -> "Vos heures pour ${request.period} ont ete approuvees (${request.hours}, ${request.pay})." },
        )
    }
}

private data class WorkerCalendarCopy(
    val earnedInJuneNet: String,
    val earnedThisMonthNet: String,
    val approved: String,
    val pending: String,
    val adjusted: String,
    val weekdayLabels: List<String>,
    val locale: Locale,
)

private fun WorkerLanguage.workerCalendarCopy(): WorkerCalendarCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerCalendarCopy(
            earnedInJuneNet = "Earned in June - net",
            earnedThisMonthNet = "Earned this month - net",
            approved = "approved",
            pending = "pending",
            adjusted = "adjusted",
            weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            locale = Locale.ENGLISH,
        )
        WorkerLanguage.Dutch -> WorkerCalendarCopy(
            earnedInJuneNet = "Verdienste in juni - netto",
            earnedThisMonthNet = "Verdienste deze maand - netto",
            approved = "goedgekeurd",
            pending = "in afwachting",
            adjusted = "aangepast",
            weekdayLabels = listOf("Ma", "Di", "Wo", "Do", "Vr", "Za", "Zo"),
            locale = Locale("nl", "NL"),
        )
        WorkerLanguage.German -> WorkerCalendarCopy(
            earnedInJuneNet = "Verdient im Juni - netto",
            earnedThisMonthNet = "Diesen Monat verdient - netto",
            approved = "genehmigt",
            pending = "ausstehend",
            adjusted = "angepasst",
            weekdayLabels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"),
            locale = Locale.GERMAN,
        )
        WorkerLanguage.French -> WorkerCalendarCopy(
            earnedInJuneNet = "Gagne en juin - net",
            earnedThisMonthNet = "Gagne ce mois-ci - net",
            approved = "approuve",
            pending = "en attente",
            adjusted = "modifie",
            weekdayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"),
            locale = Locale.FRENCH,
        )
    }
}

private data class WorkerHistoryCopy(
    val monthLabel: String,
    val monthTotalNet: String,
    val hoursWorked: String,
    val workdays: String,
    val today: String,
    val hoursShort: String,
    val photoSingular: String,
    val photoPlural: String,
)

private fun WorkerLanguage.workerHistoryCopy(): WorkerHistoryCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerHistoryCopy(
            monthLabel = "June 2026",
            monthTotalNet = "June total - net",
            hoursWorked = "Hours worked",
            workdays = "Workdays",
            today = "Today",
            hoursShort = "hrs",
            photoSingular = "photo",
            photoPlural = "photos",
        )
        WorkerLanguage.Dutch -> WorkerHistoryCopy(
            monthLabel = "Juni 2026",
            monthTotalNet = "Juni totaal - netto",
            hoursWorked = "Gewerkte uren",
            workdays = "Werkdagen",
            today = "Vandaag",
            hoursShort = "uur",
            photoSingular = "foto",
            photoPlural = "foto's",
        )
        WorkerLanguage.German -> WorkerHistoryCopy(
            monthLabel = "Juni 2026",
            monthTotalNet = "Juni gesamt - netto",
            hoursWorked = "Arbeitsstunden",
            workdays = "Arbeitstage",
            today = "Heute",
            hoursShort = "Std.",
            photoSingular = "Foto",
            photoPlural = "Fotos",
        )
        WorkerLanguage.French -> WorkerHistoryCopy(
            monthLabel = "Juin 2026",
            monthTotalNet = "Total juin - net",
            hoursWorked = "Heures travaillees",
            workdays = "Jours travailles",
            today = "Aujourd'hui",
            hoursShort = "h",
            photoSingular = "photo",
            photoPlural = "photos",
        )
    }
}

private fun WorkerLanguage.localizedPhotoCount(count: Int): String {
    val copy = workerHistoryCopy()
    val label = if (count == 1) copy.photoSingular else copy.photoPlural
    return "$count $label"
}

private data class WorkerTimeEntryCopy(
    val readyToClockIn: String,
    val atWorkLocation: String,
    val locationConfirmed: String,
    val clockInAction: String,
    val enterHoursManually: String,
    val manualEntrySelected: String,
    val logHoursTitle: String,
    val manualBadge: String,
    val estimatedWith: String,
    val hourlyRateSuffix: String,
    val clockInLabel: String,
    val clockOutLabel: String,
    val breakLabel: String,
    val minutesHelper: String,
    val noteLabel: String,
    val optionalPlaceholder: String,
    val submitHours: String,
    val checkStartEndTime: String,
)

private fun WorkerLanguage.workerTimeEntryCopy(): WorkerTimeEntryCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerTimeEntryCopy(
            readyToClockIn = "Ready to clock in",
            atWorkLocation = "You are at the work location.",
            locationConfirmed = "Location confirmed",
            clockInAction = "Clock in",
            enterHoursManually = "Enter hours manually",
            manualEntrySelected = "Manual entry selected",
            logHoursTitle = "Log hours",
            manualBadge = "Manual",
            estimatedWith = "Estimated with",
            hourlyRateSuffix = "hourly rate",
            clockInLabel = "Clock in",
            clockOutLabel = "Clock out",
            breakLabel = "Break",
            minutesHelper = "minutes",
            noteLabel = "Note",
            optionalPlaceholder = "Optional",
            submitHours = "Submit hours",
            checkStartEndTime = "Check your start and end time",
        )
        WorkerLanguage.Dutch -> WorkerTimeEntryCopy(
            readyToClockIn = "Klaar om in te klokken",
            atWorkLocation = "Je bent op de werklocatie.",
            locationConfirmed = "Locatie bevestigd",
            clockInAction = "Inklokken",
            enterHoursManually = "Uren handmatig invoeren",
            manualEntrySelected = "Handmatige invoer geselecteerd",
            logHoursTitle = "Uren loggen",
            manualBadge = "Handmatig",
            estimatedWith = "Geschat met",
            hourlyRateSuffix = "uurloon",
            clockInLabel = "Inklokken",
            clockOutLabel = "Uitklokken",
            breakLabel = "Pauze",
            minutesHelper = "minuten",
            noteLabel = "Notitie",
            optionalPlaceholder = "Optioneel",
            submitHours = "Uren indienen",
            checkStartEndTime = "Controleer je start- en eindtijd",
        )
        WorkerLanguage.German -> WorkerTimeEntryCopy(
            readyToClockIn = "Bereit zum Einstempeln",
            atWorkLocation = "Du bist am Arbeitsort.",
            locationConfirmed = "Standort bestatigt",
            clockInAction = "Einstempeln",
            enterHoursManually = "Stunden manuell erfassen",
            manualEntrySelected = "Manuelle Eingabe ausgewahlt",
            logHoursTitle = "Stunden erfassen",
            manualBadge = "Manuell",
            estimatedWith = "Geschatzt mit",
            hourlyRateSuffix = "Stundenlohn",
            clockInLabel = "Einstempeln",
            clockOutLabel = "Ausstempeln",
            breakLabel = "Pause",
            minutesHelper = "Minuten",
            noteLabel = "Notiz",
            optionalPlaceholder = "Optional",
            submitHours = "Stunden senden",
            checkStartEndTime = "Prufe Start- und Endzeit",
        )
        WorkerLanguage.French -> WorkerTimeEntryCopy(
            readyToClockIn = "Pret a pointer",
            atWorkLocation = "Vous etes sur le lieu de travail.",
            locationConfirmed = "Lieu confirme",
            clockInAction = "Pointer",
            enterHoursManually = "Saisir les heures manuellement",
            manualEntrySelected = "Saisie manuelle selectionnee",
            logHoursTitle = "Saisir les heures",
            manualBadge = "Manuel",
            estimatedWith = "Estime avec",
            hourlyRateSuffix = "taux horaire",
            clockInLabel = "Arrivee",
            clockOutLabel = "Depart",
            breakLabel = "Pause",
            minutesHelper = "minutes",
            noteLabel = "Note",
            optionalPlaceholder = "Facultatif",
            submitHours = "Envoyer les heures",
            checkStartEndTime = "Verifiez l'heure de debut et de fin",
        )
    }
}

private data class WorkerShiftFlowCopy(
    val shiftInProgressStatus: String,
    val timeWorked: String,
    val earningSoFarPrefix: String,
    val clockOut: String,
    val shiftPhotosTitle: String,
    val shiftPhotosDescription: String,
    val addPhoto: String,
    val photoUnavailable: String,
    val save: String,
)

private fun WorkerLanguage.workerShiftFlowCopy(): WorkerShiftFlowCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerShiftFlowCopy(
            shiftInProgressStatus = "Shift in progress - clocked in at 08:00",
            timeWorked = "Time worked",
            earningSoFarPrefix = "Earning so far",
            clockOut = "Clock out",
            shiftPhotosTitle = "Shift photos",
            shiftPhotosDescription = "Add photos as proof of your shift. Your employer may require this for certain shifts.",
            addPhoto = "Add photo",
            photoUnavailable = "Photo unavailable",
            save = "Save",
        )
        WorkerLanguage.Dutch -> WorkerShiftFlowCopy(
            shiftInProgressStatus = "Dienst bezig - ingeklokt om 08:00",
            timeWorked = "Gewerkte tijd",
            earningSoFarPrefix = "Verdienste tot nu toe",
            clockOut = "Uitklokken",
            shiftPhotosTitle = "Dienstfoto's",
            shiftPhotosDescription = "Voeg foto's toe als bewijs van je dienst. Je werkgever kan dit voor bepaalde diensten vragen.",
            addPhoto = "Foto toevoegen",
            photoUnavailable = "Foto niet beschikbaar",
            save = "Opslaan",
        )
        WorkerLanguage.German -> WorkerShiftFlowCopy(
            shiftInProgressStatus = "Schicht lauft - eingestempelt um 08:00",
            timeWorked = "Gearbeitete Zeit",
            earningSoFarPrefix = "Bisher verdient",
            clockOut = "Ausstempeln",
            shiftPhotosTitle = "Schichtfotos",
            shiftPhotosDescription = "Fuge Fotos als Nachweis deiner Schicht hinzu. Dein Arbeitgeber kann dies fur bestimmte Schichten verlangen.",
            addPhoto = "Foto hinzufugen",
            photoUnavailable = "Foto nicht verfugbar",
            save = "Speichern",
        )
        WorkerLanguage.French -> WorkerShiftFlowCopy(
            shiftInProgressStatus = "Service en cours - pointe a 08:00",
            timeWorked = "Temps travaille",
            earningSoFarPrefix = "Gain jusqu'ici",
            clockOut = "Pointer depart",
            shiftPhotosTitle = "Photos du service",
            shiftPhotosDescription = "Ajoutez des photos comme preuve de votre service. Votre employeur peut les demander pour certains services.",
            addPhoto = "Ajouter une photo",
            photoUnavailable = "Photo indisponible",
            save = "Enregistrer",
        )
    }
}

private data class WorkerShiftDetailCopy(
    val total: String,
    val clockIn: String,
    val clockOut: String,
    val breakLabel: String,
    val hourlyRate: String,
    val proofPhotos: String,
    val photoUnavailable: String,
    val adjustmentNote: String,
    val employer: String,
    val shiftChatHelper: String,
    val writeMessage: String,
    val send: String,
)

private fun WorkerLanguage.workerShiftDetailCopy(): WorkerShiftDetailCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerShiftDetailCopy(
            total = "Total",
            clockIn = "Clock in",
            clockOut = "Clock out",
            breakLabel = "Break",
            hourlyRate = "Hourly rate",
            proofPhotos = "Proof photos",
            photoUnavailable = "Photo unavailable",
            adjustmentNote = "Adjustment note",
            employer = "Employer",
            shiftChatHelper = "Send a note here and your employer can review this exact shift.",
            writeMessage = "Write a message",
            send = "Send",
        )
        WorkerLanguage.Dutch -> WorkerShiftDetailCopy(
            total = "Totaal",
            clockIn = "Inklokken",
            clockOut = "Uitklokken",
            breakLabel = "Pauze",
            hourlyRate = "Uurloon",
            proofPhotos = "Bewijsfoto's",
            photoUnavailable = "Foto niet beschikbaar",
            adjustmentNote = "Aanpassingsnotitie",
            employer = "Werkgever",
            shiftChatHelper = "Stuur hier een notitie zodat je werkgever deze dienst kan bekijken.",
            writeMessage = "Schrijf een bericht",
            send = "Sturen",
        )
        WorkerLanguage.German -> WorkerShiftDetailCopy(
            total = "Gesamt",
            clockIn = "Einstempeln",
            clockOut = "Ausstempeln",
            breakLabel = "Pause",
            hourlyRate = "Stundenlohn",
            proofPhotos = "Nachweisfotos",
            photoUnavailable = "Foto nicht verfugbar",
            adjustmentNote = "Anpassungsnotiz",
            employer = "Arbeitgeber",
            shiftChatHelper = "Sende hier eine Notiz, damit dein Arbeitgeber diese Schicht prufen kann.",
            writeMessage = "Nachricht schreiben",
            send = "Senden",
        )
        WorkerLanguage.French -> WorkerShiftDetailCopy(
            total = "Total",
            clockIn = "Arrivee",
            clockOut = "Depart",
            breakLabel = "Pause",
            hourlyRate = "Taux horaire",
            proofPhotos = "Photos preuve",
            photoUnavailable = "Photo indisponible",
            adjustmentNote = "Note d'ajustement",
            employer = "Employeur",
            shiftChatHelper = "Envoyez une note ici pour que votre employeur verifie ce service.",
            writeMessage = "Ecrire un message",
            send = "Envoyer",
        )
    }
}

private fun WorkerLanguage.localizedShiftActionLabel(status: WeekStatus): String {
    return when (status) {
        WeekStatus.Approved,
        WeekStatus.Pending -> when (this) {
            WorkerLanguage.English -> "Ask about this shift"
            WorkerLanguage.Dutch -> "Vraag over deze dienst"
            WorkerLanguage.German -> "Zu dieser Schicht fragen"
            WorkerLanguage.French -> "Question sur ce service"
        }
        WeekStatus.Adjusted -> when (this) {
            WorkerLanguage.English -> "Ask about this adjustment"
            WorkerLanguage.Dutch -> "Vraag over deze aanpassing"
            WorkerLanguage.German -> "Zu dieser Anpassung fragen"
            WorkerLanguage.French -> "Question sur cet ajustement"
        }
    }
}

private fun WorkerLanguage.localizedWorkerShiftNote(dayDetail: WorkerDayDetail): String {
    return when (dayDetail.status) {
        WeekStatus.Approved -> when (this) {
            WorkerLanguage.English -> "Your employer approved this shift and included it in payroll."
            WorkerLanguage.Dutch -> "Je werkgever heeft deze dienst goedgekeurd en meegenomen in de loonlijst."
            WorkerLanguage.German -> "Dein Arbeitgeber hat diese Schicht genehmigt und in die Lohnabrechnung aufgenommen."
            WorkerLanguage.French -> "Votre employeur a approuve ce service et l'a inclus dans la paie."
        }
        WeekStatus.Pending -> when (this) {
            WorkerLanguage.English -> "Your submitted hours are waiting for employer approval."
            WorkerLanguage.Dutch -> "Je ingediende uren wachten op goedkeuring van je werkgever."
            WorkerLanguage.German -> "Deine eingereichten Stunden warten auf die Genehmigung deines Arbeitgebers."
            WorkerLanguage.French -> "Vos heures envoyees attendent l'approbation de votre employeur."
        }
        WeekStatus.Adjusted -> when (this) {
            WorkerLanguage.English -> dayDetail.note
            WorkerLanguage.Dutch -> "Je werkgever heeft deze dag aangepast aan het goedgekeurde rooster."
            WorkerLanguage.German -> "Dein Arbeitgeber hat diesen Tag an den genehmigten Plan angepasst."
            WorkerLanguage.French -> "Votre employeur a ajuste cette journee selon le planning approuve."
        }
    }
}

private fun WorkerLanguage.localizedWorkerDayTitle(title: String): String {
    return when (title) {
        "Today" -> workerHistoryCopy().today
        "Tue 3 Jun" -> when (this) {
            WorkerLanguage.English -> "Tue 3 Jun"
            WorkerLanguage.Dutch -> "Di 3 jun"
            WorkerLanguage.German -> "Di 3. Juni"
            WorkerLanguage.French -> "Mar 3 juin"
        }
        "Wed 10 Jun" -> when (this) {
            WorkerLanguage.English -> "Wed 10 Jun"
            WorkerLanguage.Dutch -> "Wo 10 jun"
            WorkerLanguage.German -> "Mi 10. Juni"
            WorkerLanguage.French -> "Mer 10 juin"
        }
        "Wed 17 Jun" -> when (this) {
            WorkerLanguage.English -> "Wed 17 Jun"
            WorkerLanguage.Dutch -> "Wo 17 jun"
            WorkerLanguage.German -> "Mi 17. Juni"
            WorkerLanguage.French -> "Mer 17 juin"
        }
        "Thu 18 Jun" -> when (this) {
            WorkerLanguage.English -> "Thu 18 Jun"
            WorkerLanguage.Dutch -> "Do 18 jun"
            WorkerLanguage.German -> "Do 18. Juni"
            WorkerLanguage.French -> "Jeu 18 juin"
        }
        else -> title
    }
}

private fun WorkerLanguage.localizedWorkerDaySummary(dayDetail: WorkerDayDetail): String {
    return when (dayDetail.status) {
        WeekStatus.Approved -> when (this) {
            WorkerLanguage.English -> "Approved by your employer"
            WorkerLanguage.Dutch -> "Goedgekeurd door je werkgever"
            WorkerLanguage.German -> "Von deinem Arbeitgeber genehmigt"
            WorkerLanguage.French -> "Approuve par votre employeur"
        }
        WeekStatus.Pending -> localizedPendingWorkerDaySummary(dayDetail)
        WeekStatus.Adjusted -> localizedAdjustedWorkerDaySummary(dayDetail)
    }
}

private fun WorkerLanguage.localizedWorkerShiftValue(value: String): String {
    return when (value) {
        "Now" -> workerTabCopy().now
        "Clocked out" -> when (this) {
            WorkerLanguage.English -> "Clocked out"
            WorkerLanguage.Dutch -> "Uitgeklokt"
            WorkerLanguage.German -> "Ausgestempelt"
            WorkerLanguage.French -> "Depart pointe"
        }
        "0 min" -> when (this) {
            WorkerLanguage.English -> "0 min"
            WorkerLanguage.Dutch -> "0 min"
            WorkerLanguage.German -> "0 Min."
            WorkerLanguage.French -> "0 min"
        }
        "30 min" -> when (this) {
            WorkerLanguage.English -> "30 min"
            WorkerLanguage.Dutch -> "30 min"
            WorkerLanguage.German -> "30 Min."
            WorkerLanguage.French -> "30 min"
        }
        else -> value
    }
}

private fun WorkerLanguage.localizedWorkerDayChatSummary(dayDetail: WorkerDayDetail): String {
    return "${dayDetail.hours} - ${localizedWorkerDaySummary(dayDetail)}"
}

private fun WorkerLanguage.localizedEmployerShiftMessage(status: WeekStatus): String {
    return when (status) {
        WeekStatus.Approved -> when (this) {
            WorkerLanguage.English -> "This shift is approved and included in payroll."
            WorkerLanguage.Dutch -> "Deze dienst is goedgekeurd en meegenomen in de loonlijst."
            WorkerLanguage.German -> "Diese Schicht ist genehmigt und in der Lohnabrechnung enthalten."
            WorkerLanguage.French -> "Ce service est approuve et inclus dans la paie."
        }
        WeekStatus.Pending -> when (this) {
            WorkerLanguage.English -> "This shift is waiting for review. Share any extra context here."
            WorkerLanguage.Dutch -> "Deze dienst wacht op beoordeling. Deel hier extra context."
            WorkerLanguage.German -> "Diese Schicht wartet auf Prufung. Teile hier zusatzlichen Kontext."
            WorkerLanguage.French -> "Ce service attend une verification. Ajoutez du contexte ici."
        }
        WeekStatus.Adjusted -> when (this) {
            WorkerLanguage.English -> "I adjusted this shift to match the approved schedule."
            WorkerLanguage.Dutch -> "Ik heb deze dienst aangepast aan het goedgekeurde rooster."
            WorkerLanguage.German -> "Ich habe diese Schicht an den genehmigten Plan angepasst."
            WorkerLanguage.French -> "J'ai ajuste ce service selon le planning approuve."
        }
    }
}

private fun WorkerLanguage.localizedPendingWorkerDaySummary(dayDetail: WorkerDayDetail): String {
    val photoCount = dayDetail.photoUris.size
    if (photoCount > 0) {
        return when (this) {
            WorkerLanguage.English -> "Submitted with ${localizedPhotoCount(photoCount)}"
            WorkerLanguage.Dutch -> "Ingediend met ${localizedPhotoCount(photoCount)}"
            WorkerLanguage.German -> "Eingereicht mit ${localizedPhotoCount(photoCount)}"
            WorkerLanguage.French -> "Envoye avec ${localizedPhotoCount(photoCount)}"
        }
    }
    return when (this) {
        WorkerLanguage.English -> "Waiting for employer approval"
        WorkerLanguage.Dutch -> "Wacht op goedkeuring van je werkgever"
        WorkerLanguage.German -> "Wartet auf Genehmigung des Arbeitgebers"
        WorkerLanguage.French -> "En attente d'approbation de l'employeur"
    }
}

private fun WorkerLanguage.localizedAdjustedWorkerDaySummary(dayDetail: WorkerDayDetail): String {
    val originalHours = dayDetail.summary
        .takeIf { it.startsWith("Adjusted from ") }
        ?.substringAfter("Adjusted from ")
        ?.substringBefore(" by your employer")
        ?.takeIf { it.isNotBlank() }
    return if (originalHours != null) {
        when (this) {
            WorkerLanguage.English -> "Adjusted from $originalHours by your employer"
            WorkerLanguage.Dutch -> "Aangepast vanaf $originalHours door je werkgever"
            WorkerLanguage.German -> "Von $originalHours durch deinen Arbeitgeber angepasst"
            WorkerLanguage.French -> "Ajuste depuis $originalHours par votre employeur"
        }
    } else {
        when (this) {
            WorkerLanguage.English -> "Adjusted by your employer"
            WorkerLanguage.Dutch -> "Aangepast door je werkgever"
            WorkerLanguage.German -> "Von deinem Arbeitgeber angepasst"
            WorkerLanguage.French -> "Ajuste par votre employeur"
        }
    }
}

private fun WorkerLanguage.localizedShortWeekStatus(status: WeekStatus): String {
    return when (status) {
        WeekStatus.Approved -> when (this) {
            WorkerLanguage.English -> "Approved"
            WorkerLanguage.Dutch -> "Goedgekeurd"
            WorkerLanguage.German -> "Genehmigt"
            WorkerLanguage.French -> "Approuve"
        }
        WeekStatus.Pending -> when (this) {
            WorkerLanguage.English -> "Pending"
            WorkerLanguage.Dutch -> "In behandeling"
            WorkerLanguage.German -> "Offen"
            WorkerLanguage.French -> "En attente"
        }
        WeekStatus.Adjusted -> when (this) {
            WorkerLanguage.English -> "Adjusted"
            WorkerLanguage.Dutch -> "Aangepast"
            WorkerLanguage.German -> "Angepasst"
            WorkerLanguage.French -> "Modifie"
        }
    }
}

private fun WorkerLanguage.localizedWeekStatusChatLabel(status: WeekStatus): String {
    return when (status) {
        WeekStatus.Approved -> when (this) {
            WorkerLanguage.English -> "Approved shift"
            WorkerLanguage.Dutch -> "Goedgekeurde dienst"
            WorkerLanguage.German -> "Genehmigte Schicht"
            WorkerLanguage.French -> "Service approuve"
        }
        WeekStatus.Pending -> when (this) {
            WorkerLanguage.English -> "Pending shift"
            WorkerLanguage.Dutch -> "Dienst in behandeling"
            WorkerLanguage.German -> "Offene Schicht"
            WorkerLanguage.French -> "Service en attente"
        }
        WeekStatus.Adjusted -> when (this) {
            WorkerLanguage.English -> "Hours adjusted"
            WorkerLanguage.Dutch -> "Uren aangepast"
            WorkerLanguage.German -> "Stunden angepasst"
            WorkerLanguage.French -> "Heures modifiees"
        }
    }
}

private data class WorkerLanguageSettingsCopy(
    val back: String,
    val title: String,
    val description: String,
)

private fun WorkerLanguage.languageSettingsCopy(): WorkerLanguageSettingsCopy {
    return when (this) {
        WorkerLanguage.English -> WorkerLanguageSettingsCopy(
            back = "Back",
            title = "Language",
            description = "Choose the language used for your worker app.",
        )
        WorkerLanguage.Dutch -> WorkerLanguageSettingsCopy(
            back = "Terug",
            title = "Taal",
            description = "Kies de taal voor je worker-app.",
        )
        WorkerLanguage.German -> WorkerLanguageSettingsCopy(
            back = "Zuruck",
            title = "Sprache",
            description = "Wahle die Sprache fur deine Worker-App.",
        )
        WorkerLanguage.French -> WorkerLanguageSettingsCopy(
            back = "Retour",
            title = "Langue",
            description = "Choisissez la langue de votre app worker.",
        )
    }
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

private const val WorkerPasswordsPreferences = "worker_passwords"

private fun workerPasswordKey(email: String): String {
    return email.trim().lowercase(Locale.US)
}

private fun defaultWorkerPassword(context: Context, email: String): String? {
    return if (findAdminWorkerByEmail(context, email) != null) "password" else null
}

private fun loadWorkerPassword(context: Context, email: String): String? {
    val key = workerPasswordKey(email)
    if (key.isBlank()) return null
    return context.getSharedPreferences(WorkerPasswordsPreferences, Context.MODE_PRIVATE)
        .getString(key, null)
        ?: defaultWorkerPassword(context, email)
}

private fun saveWorkerPassword(context: Context, email: String, password: String) {
    val key = workerPasswordKey(email)
    if (key.isBlank()) return
    context.getSharedPreferences(WorkerPasswordsPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString(key, password)
        .apply()
}

private fun isWorkerPasswordValid(context: Context, email: String, password: String): Boolean {
    return loadWorkerPassword(context, email) == password
}

private fun updateWorkerPassword(
    context: Context,
    email: String,
    currentPassword: String,
    newPassword: String,
): Boolean {
    if (!isWorkerPasswordValid(context, email, currentPassword)) return false
    saveWorkerPassword(context, email, newPassword)
    return true
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

private fun String.isValidEmailAddress(): Boolean {
    return Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(this)
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

private fun WorkerLanguage.workerAdminDecisionBody(request: AdminHoursRequest): String {
    val copy = workerAlertsCopy()
    return if (request.status == "Adjusted") {
        copy.adjustedBody(request)
    } else {
        copy.approvedBody(request)
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
