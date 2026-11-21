package com.vela.player.ui.downloader

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vela.player.data.downloader.InstagramCookieStore
import com.vela.player.ui.components.ColorOverride
import com.vela.player.ui.components.VelaBadge
import com.vela.player.ui.components.VelaBadgeTone
import com.vela.player.ui.components.VelaIconButton
import com.vela.player.ui.components.VelaIcons
import com.vela.player.ui.components.VelaPrimaryButton
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme
import kotlinx.coroutines.launch

/**
 * Full-screen in-app Instagram Login Dialog.
 * Signs the user into instagram.com in a private WebView and exports cookies
 * directly into VelaPlayer's private cookies.txt so Stories and private posts work locally.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginDialog(
    cookieStore: InstagramCookieStore,
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val scope = rememberCoroutineScope()
    var sessionDetected by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(VelaRadius.dialog)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VelaSpacing.s16)
                .clip(shape)
                .background(colors.bgCanvas)
                .border(BorderStroke(1.dp, colors.borderSubtle), shape)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sign in to Instagram",
                        style = typography.section,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Session stays private on this phone for Stories & posts",
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                }
                VelaIconButton(
                    iconRes = VelaIcons.X,
                    contentDescription = "Close Instagram login",
                    tint = ColorOverride.Secondary,
                    onClick = onDismiss
                )
            }

            HorizontalDivider(thickness = 1.dp, color = colors.borderPanel)

            // Embedded Instagram login WebView
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        val cm = CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                cm.flush()
                                val hasSession = cookieStore.hasValidSession()
                                if (hasSession && !sessionDetected) {
                                    sessionDetected = true
                                    scope.launch {
                                        if (cookieStore.syncFromWebView()) {
                                            onAuthenticated()
                                        }
                                    }
                                }
                            }
                        }

                        loadUrl("https://www.instagram.com/accounts/login/")
                    }
                }
            )

            HorizontalDivider(thickness = 1.dp, color = colors.borderPanel)

            // Footer with manual confirmation button in case IG finishes login without redirecting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(VelaSpacing.s12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VelaBadge(
                    label = if (sessionDetected) "Session found" else "Private local cookies",
                    tone = if (sessionDetected) VelaBadgeTone.SUCCESS else VelaBadgeTone.INFO,
                    iconRes = if (sessionDetected) VelaIcons.Check else VelaIcons.ShieldCheck
                )
                VelaPrimaryButton(
                    text = "Done, Continue",
                    iconRes = VelaIcons.Check,
                    onClick = {
                        CookieManager.getInstance().flush()
                        scope.launch {
                            val ok = cookieStore.syncFromWebView()
                            if (ok) {
                                onAuthenticated()
                            } else {
                                onDismiss()
                            }
                        }
                    }
                )
            }
        }
    }
}
