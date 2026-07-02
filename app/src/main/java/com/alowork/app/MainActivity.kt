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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
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
fun GpsLocationDeniedScreen(
    modifier: Modifier = Modifier,
    onLocationEnabled: () -> Unit = {},
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

@Preview(showBackground = true)
@Composable
private fun AloworkAppPreview() {
    AloworkTheme {
        AloworkApp()
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsShiftInProgressScreenPreview() {
    AloworkTheme {
        GpsShiftInProgressScreen()
    }
}
