package com.alowork.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alowork.app.ui.theme.AloworkTheme
import kotlinx.coroutines.delay
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
    var screen by remember { mutableStateOf(AppScreen.WorkerSignUp) }

    fun openWorkerTab(tab: WorkerTab) {
        screen = when (tab) {
            WorkerTab.Home -> AppScreen.WorkerGpsClockIn
            WorkerTab.History -> AppScreen.WorkerHistoryOverview
            WorkerTab.Alerts -> AppScreen.WorkerNotifications
            WorkerTab.Profile -> AppScreen.WorkerProfile
        }
    }

    when (screen) {
        AppScreen.WorkerSignUp -> WorkerSignUpScreen(
            onAccountCreated = {
                screen = AppScreen.WorkerAwaitingApproval
            },
        )

        AppScreen.WorkerAwaitingApproval -> WorkerAwaitingApprovalScreen(
            autoContinue = true,
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
                screen = AppScreen.WorkerShiftInProgress
            },
            onManualEntry = {
                screen = AppScreen.WorkerLogHoursDayDetail
            },
        )

        AppScreen.WorkerShiftInProgress -> GpsShiftInProgressScreen(
            onClockOut = {
                screen = AppScreen.WorkerGpsClockIn
            },
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
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerProfile -> WorkerProfileScreen(
            onSwitchEmployer = {
                screen = AppScreen.WorkerSwitchEmployer
            },
            onAddEmployer = {
                screen = AppScreen.WorkerAddEmployer
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerHistoryOverview -> WorkerHistoryOverviewScreen(
            onDaySelected = {
                screen = AppScreen.WorkerDayViewAdjusted
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerDayViewAdjusted -> WorkerDayViewAdjustedScreen(
            onBack = {
                screen = AppScreen.WorkerHistoryOverview
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerLogHoursDayDetail -> WorkerLogHoursDayDetailScreen(
            onBack = {
                screen = AppScreen.WorkerGpsClockIn
            },
            onSubmitted = {
                screen = AppScreen.WorkerHistoryOverview
            },
            onTabSelected = ::openWorkerTab,
        )

        AppScreen.WorkerAddEmployer -> WorkerAddEmployerScreen(
            onBack = {
                screen = AppScreen.WorkerSwitchEmployer
            },
            onCompanyAdded = {
                screen = AppScreen.WorkerProfile
            },
        )

        AppScreen.WorkerSwitchEmployer -> WorkerSwitchEmployerScreen(
            onBack = {
                screen = AppScreen.WorkerProfile
            },
            onAddCompany = {
                screen = AppScreen.WorkerAddEmployer
            },
        )

        AppScreen.WebRegisterCompanyDetails -> WebRegisterCompanyDetailsScreen(
            onContinue = {
                screen = AppScreen.WebRegisterChoosePlan
            },
        )

        AppScreen.WebRegisterChoosePlan -> WebRegisterChoosePlanScreen(
            onContinue = {
                screen = AppScreen.WebRegisterAdminAccount
            },
        )

        AppScreen.WebRegisterAdminAccount -> WebRegisterAdminAccountScreen(
            onAccountCreated = {
                screen = AppScreen.WebRegisterSuccessCode
            },
        )

        AppScreen.WebRegisterSuccessCode -> WebRegisterSuccessCodeScreen()
    }
}

@Composable
fun WorkerSignUpScreen(
    modifier: Modifier = Modifier,
    onAccountCreated: () -> Unit = {},
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
                        onAccountCreated()
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
                    onClick = {
                        Toast.makeText(context, "Log in selected", Toast.LENGTH_SHORT).show()
                    },
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
    onContinue: () -> Unit = {},
) {
    val context = LocalContext.current
    var companyName by remember { mutableStateOf("Bakkerij Jansen") }
    var industry by remember { mutableStateOf("Bakery") }
    var email by remember { mutableStateOf("admin@bakkerijjansen.nl") }

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
                        onContinue()
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
    onContinue: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf("Starter") }

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
                    selected = selectedPlan == "Starter",
                    onClick = { selectedPlan = "Starter" },
                )
                Spacer(modifier = Modifier.height(12.dp))
                WebPlanOptionCard(
                    title = "Business",
                    price = "\u20AC49 / month",
                    detail = "Up to 60 workers",
                    selected = selectedPlan == "Business",
                    onClick = { selectedPlan = "Business" },
                )
            }

            Button(
                onClick = {
                    Toast.makeText(context, "$selectedPlan plan selected", Toast.LENGTH_SHORT).show()
                    onContinue()
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
    onAccountCreated: () -> Unit = {},
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("Lotte Jansen") }
    var email by remember { mutableStateOf("lotte@bakkerijjansen.nl") }
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
                        onAccountCreated()
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
                    Toast.makeText(context, "Company code copied", Toast.LENGTH_SHORT).show()
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
                    text = "Copy company code",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
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
    autoContinue: Boolean = false,
    onApprovalReceived: () -> Unit = {},
) {
    if (autoContinue) {
        LaunchedEffect(Unit) {
            delay(1800)
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
                .padding(bottom = 54.dp),
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
    }
}

@Composable
fun WorkerNotificationsScreen(
    modifier: Modifier = Modifier,
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
                text = "Alerts",
                color = Color(0xFF17171B),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
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
                body = "Welcome! Your employer approved your\naccount.",
                time = "3 days ago",
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
    onSwitchEmployer: () -> Unit = {},
    onAddEmployer: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current

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
                text = "Pay (set by employer)",
                color = Color(0xFF73737A),
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileSectionCard {
                ProfileInfoRow(label = "Hourly rate", value = "€16.00")
                ProfileDivider()
                ProfileInfoRow(label = "Payout", value = "Net")
                ProfileDivider()
                ProfileInfoRow(label = "Weekend premium", value = "1.5× (Sunday)")
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
                ProfileActionRow(
                    label = "Language",
                    onClick = {
                        Toast.makeText(context, "Language selected", Toast.LENGTH_SHORT).show()
                    },
                )
                ProfileDivider()
                ProfileActionRow(
                    label = "Change password",
                    onClick = {
                        Toast.makeText(context, "Change password selected", Toast.LENGTH_SHORT)
                            .show()
                    },
                )
                ProfileDivider()
                ProfileActionRow(
                    label = "Log out",
                    onClick = {
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    },
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
fun WorkerHistoryOverviewScreen(
    modifier: Modifier = Modifier,
    onDaySelected: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current

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
            MonthlySummaryCard()
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4E4DF)),
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WeekOverviewRow(
                        week = "Week 23",
                        amount = "€608",
                        detail = "38.0 hrs · 2–6 Jun",
                        status = WeekStatus.Approved,
                        onClick = onDaySelected,
                    )
                    ProfileDivider()
                    WeekOverviewRow(
                        week = "Week 24",
                        amount = "€584",
                        detail = "36.5 hrs · 9–13 Jun",
                        status = WeekStatus.Approved,
                        onClick = onDaySelected,
                    )
                    ProfileDivider()
                    WeekOverviewRow(
                        week = "Week 25",
                        amount = "€256",
                        detail = "16.0 hrs · 16–19 Jun",
                        status = WeekStatus.Pending,
                        onClick = onDaySelected,
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
    onBack: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current

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
                    text = "‹",
                    color = Color(0xFF17171B),
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .width(30.dp)
                        .clickable(onClick = onBack),
                )
                Text(
                    text = "Tue 3 Jun",
                    color = Color(0xFF17171B),
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                WeekStatusPill(status = WeekStatus.Adjusted)
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
                            text = "6.0h",
                            color = Color(0xFF17171B),
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "€96.00",
                            color = Color(0xFF17171B),
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adjusted from 7.5h by your employer",
                        color = Color(0xFFE0A12A),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ProfileSectionCard {
                DayInfoRow(label = "Clock in", value = "08:00")
                ProfileDivider()
                DayInfoRow(label = "Clock out", value = "14:00")
                ProfileDivider()
                DayInfoRow(label = "Break", value = "0 min")
                ProfileDivider()
                DayInfoRow(label = "Hourly rate", value = "€16.00")
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
                        text = "Your employer adjusted this day to match the approved schedule.",
                        color = Color(0xFF73737A),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Toast.makeText(context, "Question sent", Toast.LENGTH_SHORT).show()
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
                    text = "Ask about this adjustment",
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
fun WorkerLogHoursDayDetailScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSubmitted: () -> Unit = {},
    onTabSelected: (WorkerTab) -> Unit = {},
) {
    val context = LocalContext.current
    var clockIn by remember { mutableStateOf("08:00") }
    var clockOut by remember { mutableStateOf("16:30") }
    var breakMinutes by remember { mutableStateOf("30") }
    var note by remember { mutableStateOf("") }
    val totalHours = calculateManualHours(clockIn, clockOut, breakMinutes)
    val estimatedPay = totalHours * 16.0

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
                        text = "Estimated with €16.00 hourly rate",
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
                        Toast.makeText(context, "Hours submitted", Toast.LENGTH_SHORT).show()
                        onSubmitted()
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
    onCompanyAdded: () -> Unit = {},
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
                    Toast.makeText(context, "Company request sent", Toast.LENGTH_SHORT).show()
                    onCompanyAdded()
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
    onBack: () -> Unit = {},
    onAddCompany: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedEmployer by remember { mutableStateOf("Bakkerij Jansen") }
    val employers = remember {
        listOf(
            WorkerEmployer("Bakkerij Jansen", "Shift worker", "\u20AC16.00/hr"),
            WorkerEmployer("Cafe De Hoek", "Service", "\u20AC14.50/hr"),
            WorkerEmployer("Tuincentrum Bos", "Weekend help", "\u20AC13.00/hr"),
        )
    }

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
                    selected = selectedEmployer == employer.name,
                    onClick = {
                        selectedEmployer = employer.name
                        Toast.makeText(context, "Switched to ${employer.name}", Toast.LENGTH_SHORT).show()
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
                    text = "${employer.role} · ${employer.rate}",
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
private fun MonthlySummaryCard(modifier: Modifier = Modifier) {
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
                text = "€1,284",
                color = Color(0xFF17171B),
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryMetricTile(
                    value = "78.5h",
                    label = "Hours worked",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                SummaryMetricTile(
                    value = "14",
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
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE8E8E3)),
    )
}

@Composable
private fun NotificationCard(
    type: NotificationType,
    title: String,
    body: String,
    time: String,
    unread: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
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
        NotificationType.HoursAdjusted -> Color(0xFFE7F3FF)
        NotificationType.WeekApproved -> Color(0xFFE3F8EF)
        NotificationType.AccountApproved -> Color(0xFFE3F8EF)
    }
    val iconColor = when (type) {
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
                tab = WorkerTab.Home,
                selected = selected == WorkerTab.Home,
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
                WorkerTab.Home -> {
                    drawCircle(
                        color = iconColor,
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.width * 0.28f,
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.5f, size.height * 0.23f),
                        end = Offset(size.width * 0.5f, size.height * 0.08f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
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
    onClockOut: () -> Unit = {},
) {
    var elapsedSeconds by remember { mutableStateOf(3.hours + 24.minutes + 11.seconds) }
    val earnings = elapsedSeconds / 3600.0 * 13.05

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
            onClick = onClockOut,
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
    return "£%.2f".format(Locale.US, value)
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

private enum class AppScreen {
    WorkerSignUp,
    WorkerAwaitingApproval,
    WorkerLocationPermission,
    WorkerGpsClockIn,
    WorkerShiftInProgress,
    WorkerLocationDenied,
    WorkerNotifications,
    WorkerProfile,
    WorkerHistoryOverview,
    WorkerDayViewAdjusted,
    WorkerLogHoursDayDetail,
    WorkerAddEmployer,
    WorkerSwitchEmployer,
    WebRegisterCompanyDetails,
    WebRegisterChoosePlan,
    WebRegisterAdminAccount,
    WebRegisterSuccessCode,
}

private data class WorkerEmployer(
    val name: String,
    val role: String,
    val rate: String,
)

private enum class NotificationType {
    HoursAdjusted,
    WeekApproved,
    AccountApproved,
}

enum class WorkerTab {
    Home,
    History,
    Alerts,
    Profile,
}

private enum class WeekStatus {
    Approved,
    Pending,
    Adjusted,
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
private fun GpsShiftInProgressScreenPreview() {
    AloworkTheme {
        GpsShiftInProgressScreen()
    }
}
