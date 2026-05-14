package kutira.kone.app.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kutira.kone.app.components.LineGray
import kutira.kone.app.components.PrimaryButton
import kutira.kone.app.components.RoundedInput
import kutira.kone.app.model.AppState

@Composable
fun LoginScreen(
    state: AppState,
    context: Context,
    onLogin: (String, String, Context) -> Unit,
    onSignup: (String, String, Context) -> Unit,
    onGoogleClick: () -> Unit
) {
    var loginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("KUTIRA-KONE", fontSize = 28.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(64.dp))
        Text(
            if (loginMode) "Welcome Back" else "Join the Movement",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            if (loginMode) "Enter your details to access your dashboard."
            else "Enter your details to become part of our conscious community and start trading premium textile scrap.",
            color = Color(0xFF3F3838),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp, bottom = 48.dp)
        )

        if (!loginMode) {
            LabeledInput("FULL NAME", name, { name = it }, "E.g. Elena Rossi")
            Spacer(Modifier.height(24.dp))
        }
        LabeledInput("EMAIL ADDRESS", email, { email = it }, "name@example.com")
        Spacer(Modifier.height(24.dp))
        LabeledInput("PASSWORD", password, { password = it }, "••••••••", password = true)

        if (!loginMode) {
            Row(modifier = Modifier.padding(top = 22.dp), verticalAlignment = Alignment.Top) {
                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                Text(
                    "I agree to sustainable practices and the Kutira-Kone Terms of Service.",
                    fontSize = 18.sp,
                    color = Color(0xFF3F3838),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(38.dp))
        PrimaryButton(
            text = if (loginMode) "Sign In" else "Create Account",
            enabled = !state.loading && (loginMode || agreed)
        ) {
            if (loginMode) onLogin(email, password, context) else onSignup(email, password, context)
        }

        if (loginMode) {
            Row(
                modifier = Modifier.padding(vertical = 34.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f), color = LineGray)
                Text("OR CONTINUE WITH", modifier = Modifier.padding(horizontal = 14.dp), letterSpacing = 2.sp)
                HorizontalDivider(Modifier.weight(1f), color = LineGray)
            }
            PrimaryButton("Continue with Google", enabled = !state.loading, onClick = onGoogleClick)
        }

        Spacer(Modifier.height(36.dp))
        Row {
            Text(if (loginMode) "Don't have an account? " else "Already have an account? ", fontSize = 18.sp)
            Text(
                if (loginMode) "Create an Account" else "Sign In",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.clickable { loginMode = !loginMode }
            )
        }
    }
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color(0xFF453C3C))
        Spacer(Modifier.height(12.dp))
        RoundedInput(value, onValueChange, placeholder, password = password)
    }
}
