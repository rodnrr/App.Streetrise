package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.ui.CommunityChatViewModel
import com.example.ui.CommunityChatSheet
import com.example.ui.AlwaysOnChatFloatingWidget
import com.example.ui.RealTimeSearchViewModel
import com.example.ui.HubViewMode
import com.example.ui.RealTimeSearchHeader
import com.example.ui.RealTimeGoogleMapScreen
import com.example.ui.RileyPhoneAssistantSheet
import com.example.ui.ProminentCallNowButton
import com.example.ui.EmergencyDialAndCheckInViewModel
import com.example.ui.CallingTabScreen
import com.example.ui.CheckInCallOverlay
import com.example.ui.UserAccountViewModel
import com.example.ui.UserAccountSheet
import com.example.ui.OptionalAccountSyncBanner
import com.example.data.UserAccount
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ResourceSeedItem
import com.example.data.SavedResViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

  // URL of the StreetRise web app
  private val streetRiseWebUrl = "https://app.streetrise.org"

  // Geolocation Callback tracking
  private var pendingOrigin: String? = null
  private var pendingCallback: GeolocationPermissions.Callback? = null

  // Mutable states for Compose
  private val showLocationExplanationDialog = mutableStateOf(false)
  private val isNetworkConnected = mutableStateOf(true)

  // Request location permission launcher
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    
    if (fineGranted || coarseGranted) {
      // Permission granted, trigger the callback
      pendingCallback?.invoke(pendingOrigin, true, false)
    } else {
      // Permission denied
      pendingCallback?.invoke(pendingOrigin, false, false)
      Toast.makeText(
        this,
        "Location permission denied. Map feature will not center on your location.",
        Toast.LENGTH_LONG
      ).show()
    }
    cleanupPendingGeolocation()
  }

  // Request notifications permission launcher (Android 13+)
  private val requestNotificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      android.util.Log.d("Notifications", "POST_NOTIFICATIONS permission granted dynamically.")
    } else {
      android.util.Log.d("Notifications", "POST_NOTIFICATIONS permission was denied.")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    try {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val syncWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.workers.SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES,
            5, java.util.concurrent.TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build()
        
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ServiceDataSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    } catch (e: Throwable) {
        android.util.Log.e("WorkManagerInit", "Failed to enqueue work", e)
    }

    try {
        SupabaseManager.initialize(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY
        )
    } catch (e: Exception) {
        android.util.Log.e("SupabaseInit", "Supabase initialization error: ${e.message}")
    }

    // Push Notification Token Registration
    // Only query FirebaseMessaging if a valid Firebase configuration is genuinely present
    val hasValidFirebaseApp = try {
      FirebaseApp.getApps(this).isNotEmpty()
    } catch (e: Throwable) {
      false
    }

    if (hasValidFirebaseApp) {
      try {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
          try {
            if (task.isSuccessful && !task.result.isNullOrEmpty()) {
              val token = task.result
              android.util.Log.d("FCM", "FCM Device Token: $token")
              PushNotificationManager.updateToken(token)
            } else {
              val fallbackToken = "streetrise_dev_" + (Build.MODEL.hashCode().toString().take(8))
              PushNotificationManager.updateToken(fallbackToken)
            }
          } catch (inner: Throwable) {
            val fallbackToken = "streetrise_dev_" + (Build.MODEL.hashCode().toString().take(8))
            PushNotificationManager.updateToken(fallbackToken)
          }
        }
      } catch (e: Throwable) {
        val fallbackToken = "streetrise_dev_" + (Build.MODEL.hashCode().toString().take(8))
        PushNotificationManager.updateToken(fallbackToken)
      }
    } else {
      // Safe offline/local device registration token for WebView bridge without FIS auth error
      val fallbackToken = "streetrise_dev_" + (Build.MODEL.hashCode().toString().take(8))
      android.util.Log.d("PushNotification", "Initialized local device push token: $fallbackToken")
      PushNotificationManager.updateToken(fallbackToken)
    }

    // Request dynamic POST_NOTIFICATIONS permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    // Check connectivity initially
    isNetworkConnected.value = isDeviceOnline()

    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          StreetRiseMainScreen(
            webUrl = streetRiseWebUrl,
            innerPadding = innerPadding,
            isConnected = isNetworkConnected.value,
            onRefreshConnectivity = {
              isNetworkConnected.value = isDeviceOnline()
            },
            showLocationDialog = showLocationExplanationDialog.value,
            onDismissLocationDialog = {
              pendingCallback?.invoke(pendingOrigin, false, false)
              cleanupPendingGeolocation()
              showLocationExplanationDialog.value = false
            },
            onConfirmLocationDialog = {
              showLocationExplanationDialog.value = false
              requestPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION
                )
              )
            },
            onGeolocationRequested = { origin, callback ->
              handleGeolocationRequest(origin, callback)
            }
          )
        }
      }
    }
  }

  private fun handleGeolocationRequest(origin: String?, callback: GeolocationPermissions.Callback?) {
    // If permission already granted dynamically, invoke immediately
    val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    
    if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
      callback?.invoke(origin, true, false)
    } else {
      // Save pending context and show the custom explaining dialog inside Compose UI
      pendingOrigin = origin
      pendingCallback = callback
      showLocationExplanationDialog.value = true
    }
  }

  private fun cleanupPendingGeolocation() {
    pendingOrigin = null
    pendingCallback = null
  }

  private fun isDeviceOnline(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreetRiseMainScreen(
  webUrl: String,
  innerPadding: PaddingValues,
  isConnected: Boolean,
  onRefreshConnectivity: () -> Unit,
  showLocationDialog: Boolean,
  onDismissLocationDialog: () -> Unit,
  onConfirmLocationDialog: () -> Unit,
  onGeolocationRequested: (String?, GeolocationPermissions.Callback?) -> Unit,
  viewModel: SavedResViewModel = viewModel(),
  chatViewModel: CommunityChatViewModel = viewModel(),
  searchViewModel: RealTimeSearchViewModel = viewModel(),
  dialViewModel: EmergencyDialAndCheckInViewModel = viewModel(),
  accountViewModel: UserAccountViewModel = viewModel()
) {
  val context = LocalContext.current
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var loadingProgress by remember { mutableStateOf(0) }
  var isNavigating by remember { mutableStateOf(true) }
  var isFirstLoadCompleted by remember { mutableStateOf(false) }
  var hasLoadError by remember { mutableStateOf(false) }

  val isAlwaysOn by chatViewModel.isAlwaysOn.collectAsStateWithLifecycle()
  val isChatOpen by chatViewModel.isChatSheetOpen.collectAsStateWithLifecycle()
  val isRileyCalling by searchViewModel.isRileyCalling.collectAsStateWithLifecycle()
  val hubViewMode by searchViewModel.hubViewMode.collectAsStateWithLifecycle()
  val isCheckInCallActive by dialViewModel.isCheckInCallActive.collectAsStateWithLifecycle()

  val userAccount by accountViewModel.account.collectAsStateWithLifecycle()
  val isAccountSheetOpen by accountViewModel.isAccountSheetOpen.collectAsStateWithLifecycle()
  val bookmarkedList by viewModel.bookmarkedResources.collectAsStateWithLifecycle()
  val callLogsList by dialViewModel.checkInLogs.collectAsStateWithLifecycle()
  val emergencyDialsList by dialViewModel.emergencyDials.collectAsStateWithLifecycle()

  val fcmTokenState by PushNotificationManager.fcmToken.collectAsStateWithLifecycle()

  // Register token dispatch and incoming listener dispatch to WebView when page loading or token state changes
  LaunchedEffect(fcmTokenState, webViewRef) {
    val token = fcmTokenState
    if (token != null && webViewRef != null) {
      webViewRef?.evaluateJavascript(
        "if (window.pushListeners && window.pushListeners['registration']) { " +
        "  window.pushListeners['registration'].forEach(cb => cb({ value: '$token' })); " +
        "}", null
      )
    }
  }

  // Handle incoming notification event stream
  LaunchedEffect(webViewRef) {
    PushNotificationManager.incomingNotification.collect { payload ->
      if (webViewRef != null) {
        val title = payload["title"] ?: ""
        val body = payload["body"] ?: ""
        // Marshal additional data as a JSON construct safely
        val jsonPayload = payload.map { (k, v) -> "\"$k\": \"$v\"" }.joinToString(", ")
        webViewRef?.evaluateJavascript(
          "if (window.pushListeners && window.pushListeners['pushNotificationReceived']) { " +
          "  window.pushListeners['pushNotificationReceived'].forEach(cb => cb({ title: '$title', body: '$body', data: { $jsonPayload } })); " +
          "}", null
        )
      }
    }
  }

  // Safety automatic fade out / timeout for splash loader
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(6000)
    isFirstLoadCompleted = true
  }

  // Active bottom navigation tab index:
  // 0 -> Hub (Main Web Map)
  // 1 -> Call (Dedicated Hotline, Speed Dials & Automated Wellness Check-In)
  // 2 -> Offline Guide (Curated verification handbook list)
  // 3 -> Saved Listings (Filtered bookmarked resources only)
  // 4 -> Community Chat
  var selectedTab by remember { mutableIntStateOf(0) }

  // Back navigation handling
  BackHandler(enabled = isAccountSheetOpen) {
    accountViewModel.closeAccountSheet()
  }

  BackHandler(enabled = isCheckInCallActive) {
    dialViewModel.endCheckInCall()
  }

  BackHandler(enabled = isRileyCalling) {
    searchViewModel.dismissRileyAssistant()
  }

  BackHandler(enabled = (isChatOpen || selectedTab == 4)) {
    chatViewModel.closeChat()
    if (selectedTab == 4) {
      selectedTab = 0
    }
  }

  BackHandler(enabled = (webViewRef?.canGoBack() == true && selectedTab == 0 && !isChatOpen)) {
    webViewRef?.goBack()
  }

  BackHandler(enabled = (selectedTab in 1..3 && !isChatOpen)) {
    selectedTab = 0
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("app_bottom_nav_bar")
      ) {
        NavigationBarItem(
          selected = selectedTab == 0 && !isChatOpen,
          onClick = {
            selectedTab = 0
            if (isChatOpen) chatViewModel.closeChat()
          },
          label = { Text("Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          icon = { Icon(Icons.Default.LocationOn, "Hub") },
          modifier = Modifier.testTag("nav_tab_hub")
        )
        NavigationBarItem(
          selected = selectedTab == 1 && !isChatOpen,
          onClick = {
            selectedTab = 1
            if (isChatOpen) chatViewModel.closeChat()
          },
          label = { Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          icon = { Icon(Icons.Default.Call, "Call & Check-In") },
          modifier = Modifier.testTag("nav_tab_call")
        )
        NavigationBarItem(
          selected = selectedTab == 2 && !isChatOpen,
          onClick = {
            selectedTab = 2
            if (isChatOpen) chatViewModel.closeChat()
          },
          label = { Text("Offline Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          icon = { Icon(Icons.AutoMirrored.Filled.List, "Offline Guide") },
          modifier = Modifier.testTag("nav_tab_offline_guide")
        )
        NavigationBarItem(
          selected = selectedTab == 3 && !isChatOpen,
          onClick = {
            selectedTab = 3
            if (isChatOpen) chatViewModel.closeChat()
          },
          label = { Text("Saved", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          icon = { Icon(Icons.Default.Favorite, "Saved Items") },
          modifier = Modifier.testTag("nav_tab_bookmarks")
        )
        NavigationBarItem(
          selected = isChatOpen || selectedTab == 4,
          onClick = {
            selectedTab = 4
            chatViewModel.openChat()
          },
          label = { Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          icon = {
            BadgedBox(
              badge = {
                val activeCount by chatViewModel.activeConversationsCount.collectAsStateWithLifecycle()
                val unread by chatViewModel.unreadCount.collectAsStateWithLifecycle()
                val badgeNumber = if (unread > 0) unread else activeCount
                if (badgeNumber > 0) {
                  Badge { Text("$badgeNumber", fontSize = 9.sp) }
                }
              }
            ) {
              Icon(Icons.Default.Email, "Community Chat")
            }
          },
          modifier = Modifier.testTag("nav_tab_community_chat")
        )
      }
    }
  ) { scaffoldPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(scaffoldPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // 1. Hub Screen (Tab 0): Real-Time Search Header + (Web Portal or Live Search Map)
      Column(
        modifier = Modifier
          .fillMaxSize()
          .zIndex(if (selectedTab == 0 && !isChatOpen && !isRileyCalling) 1f else -1f)
          .graphicsLayer {
            alpha = if (selectedTab == 0 && !isChatOpen && !isRileyCalling) 1f else 0f
            translationY = if (selectedTab == 0 && !isChatOpen && !isRileyCalling) 0f else 10000f
          }
      ) {
        // Clean, spacious Hub top bar without crowded search or calling buttons
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text("SR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
              }
              Text(
                "StreetRise Hub",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Sleek mode toggle between Web Portal and Native Live Map
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(30.dp)
              ) {
                Row(
                  modifier = Modifier.padding(2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(14.dp))
                      .background(if (hubViewMode == HubViewMode.WEB_PORTAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                      .clickable { searchViewModel.switchHubView(HubViewMode.WEB_PORTAL) }
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                      .testTag("toggle_web_view_mode")
                  ) {
                    Text(
                      "Web",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (hubViewMode == HubViewMode.WEB_PORTAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(14.dp))
                      .background(if (hubViewMode == HubViewMode.LIVE_SEARCH_MAP) MaterialTheme.colorScheme.primary else Color.Transparent)
                      .clickable { searchViewModel.switchHubView(HubViewMode.LIVE_SEARCH_MAP) }
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                      .testTag("toggle_map_view_mode")
                  ) {
                    Text(
                      "Live Map",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (hubViewMode == HubViewMode.LIVE_SEARCH_MAP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              // Optional Account & Cloud Sync button
              IconButton(
                onClick = { accountViewModel.openAccountSheet() },
                modifier = Modifier.size(32.dp).testTag("top_bar_account_button")
              ) {
                if (userAccount.isSignedIn) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = userAccount.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                      )
                    }
                  }
                } else {
                  Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Account & Cloud Sync (Optional)",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          // Native Google Map for live search services
          if (hubViewMode == HubViewMode.LIVE_SEARCH_MAP) {
            RealTimeGoogleMapScreen(
              viewModel = searchViewModel,
              modifier = Modifier.fillMaxSize()
            )
          }

          // WebView Container (maintained so web state does not reset)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                alpha = if (hubViewMode == HubViewMode.WEB_PORTAL) 1f else 0f
                translationY = if (hubViewMode == HubViewMode.WEB_PORTAL) 0f else 10000f
              }
          ) {
            AndroidView(
          factory = { ctx ->
            WebView(ctx).apply {
              webViewRef = this
              
              // Layout params
              layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
              )

              setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

              // Register Capacitor compliance JS bridge callback
              addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun registerPush() {
                  post {
                    val token = PushNotificationManager.fcmToken.value
                    if (token != null) {
                      evaluateJavascript("if (window.pushListeners && window.pushListeners['registration']) { window.pushListeners['registration'].forEach(cb => cb({ value: '$token' })); }", null)
                    }
                  }
                }
              }, "AndroidPush")

                settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                setGeolocationEnabled(true)
              }

              val cookieManager = CookieManager.getInstance()
              cookieManager.setAcceptCookie(true)
              cookieManager.setAcceptThirdPartyCookies(this, true)

              webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                  if (url == null) return false

                  val uri = Uri.parse(url)
                  val scheme = uri.scheme ?: ""
                  val host = uri.host ?: ""

                  if (scheme == "tel") {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                  }
                  if (scheme == "mailto") {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                  }
                  if (scheme == "sms") {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                  }

                  val isMapsLink = host.contains("google.com/maps") || 
                                   host.contains("maps.apple.com") || 
                                   host.contains("openstreetmap.org") || 
                                   scheme == "geo"
                  
                  val isStreetRise = host.contains("streetrise.org") || 
                                     host.contains("app.streetrise.org") || 
                                     url.startsWith("file://")

                  if (isMapsLink) {
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    val pm = context.packageManager
                    val resolvedActivity = mapIntent.resolveActivity(pm)
                    if (resolvedActivity != null || url.startsWith("geo:")) {
                      context.startActivity(mapIntent)
                    } else {
                      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                      context.startActivity(browserIntent)
                    }
                    return true
                  }

                  if (!isStreetRise) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                  }

                  return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                  super.onPageStarted(view, url, favicon)
                  isNavigating = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                  super.onPageFinished(view, url)
                  isNavigating = false
                  loadingProgress = 100
                  isFirstLoadCompleted = true

                  // Inject Capacitor Push Notification Compatibility
                  val injectScript = """
                    (function() {
                      if (!window.Capacitor) { window.Capacitor = {}; }
                      if (!window.Capacitor.Plugins) { window.Capacitor.Plugins = {}; }
                      window.Capacitor.Plugins.PushNotifications = {
                        register: function() {
                          if (window.AndroidPush) { window.AndroidPush.registerPush(); }
                        },
                        addListener: function(eventName, callback) {
                          if (!window.pushListeners) { window.pushListeners = {}; }
                          if (!window.pushListeners[eventName]) { window.pushListeners[eventName] = []; }
                          window.pushListeners[eventName].push(callback);
                          
                          // Trigger immediate recall if token exists
                          if (eventName === 'registration' && window.AndroidPush) {
                            window.AndroidPush.registerPush();
                          }
                          return { remove: function() { 
                            if (window.pushListeners[eventName]) {
                              var idx = window.pushListeners[eventName].indexOf(callback);
                              if (idx !== -1) { window.pushListeners[eventName].splice(idx, 1); }
                            }
                          }};
                        }
                      };
                    })();
                  """.trimIndent()
                  view?.evaluateJavascript(injectScript, null)
                }

                override fun onReceivedError(
                  view: WebView?,
                  request: android.webkit.WebResourceRequest?,
                  error: android.webkit.WebResourceError?
                ) {
                  super.onReceivedError(view, request, error)
                  if (request?.isForMainFrame == true) {
                    isNavigating = false
                    hasLoadError = true
                    loadingProgress = 100
                    isFirstLoadCompleted = true
                  }
                }

                override fun onReceivedSslError(
                  view: WebView?,
                  handler: android.webkit.SslErrorHandler?,
                  error: android.net.http.SslError?
                ) {
                  handler?.proceed()
                }
              }

              webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                  super.onProgressChanged(view, newProgress)
                  loadingProgress = newProgress
                  if (newProgress >= 100) {
                    isNavigating = false
                    isFirstLoadCompleted = true
                  }
                }

                override fun onGeolocationPermissionsShowPrompt(
                  origin: String?,
                  callback: GeolocationPermissions.Callback?
                ) {
                  onGeolocationRequested(origin, callback)
                }
              }

              loadUrl(webUrl)
            }
          },
          update = { /* WebView updates handled automatically */ },
          modifier = Modifier.fillMaxSize().testTag("streetrise_webview")
        )

        // Linear progress bar at top of WebView section while loading
        if (isNavigating && loadingProgress < 100) {
          LinearProgressIndicator(
            progress = { loadingProgress / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .align(Alignment.TopCenter)
              .testTag("webview_progress_indicator"),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
          )
        }

        // Lost connection banner inside WebView Map Tab
        if (!isConnected && isFirstLoadCompleted) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, "Offline", tint = MaterialTheme.colorScheme.onErrorContainer)
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                "No connection. Tap Offline Guide below to use localized offline files.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
          }
        }
      }

      // Overlay Screen for Dedicated Calling & Hotline Tab (Tab 1)
      AnimatedVisibility(
        visible = selectedTab == 1 && !isChatOpen,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        CallingTabScreen(
          dialViewModel = dialViewModel,
          onOpenRileyAssistant = { searchViewModel.summonRileyAssistant() },
          userAccount = userAccount,
          onOpenAccount = { accountViewModel.openAccountSheet() },
          modifier = Modifier.fillMaxSize()
        )
      }

      // Overlay Screen for Offline Guide (Tab 2)
      AnimatedVisibility(
        visible = selectedTab == 2 && !isChatOpen,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        LocalResourceDirectoryScreen(
          viewModel = viewModel,
          onBackToWeb = { selectedTab = 0 },
          isDeviceOnline = isConnected && !hasLoadError,
          showOnlyBookmarked = false,
          onCallRiley = { searchViewModel.summonRileyAssistant() },
          userAccount = userAccount,
          onOpenAccount = { accountViewModel.openAccountSheet() }
        )
      }

      // Overlay Screen for Saved Items (Tab 3)
      AnimatedVisibility(
        visible = selectedTab == 3 && !isChatOpen,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        LocalResourceDirectoryScreen(
          viewModel = viewModel,
          onBackToWeb = { selectedTab = 0 },
          isDeviceOnline = isConnected && !hasLoadError,
          showOnlyBookmarked = true,
          onCallRiley = { searchViewModel.summonRileyAssistant() },
          userAccount = userAccount,
          onOpenAccount = { accountViewModel.openAccountSheet() }
        )
      }

      // Always-On Floating Chat Bubble across all tabs
      AlwaysOnChatFloatingWidget(
        viewModel = chatViewModel,
        modifier = Modifier
          .fillMaxSize()
          .zIndex(14f)
      )

      // Full Community Chat Sheet overlay (Tab 4 or Bubble Tap)
      AnimatedVisibility(
        visible = isChatOpen || selectedTab == 4,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
          .fillMaxSize()
          .zIndex(20f)
      ) {
        CommunityChatSheet(
          viewModel = chatViewModel,
          modifier = Modifier.fillMaxSize(),
          onClose = {
            chatViewModel.closeChat()
            if (selectedTab == 4) {
              selectedTab = 0
            }
          }
        )
      }

      // Riley Phone Assistant 463-2-STREET live audio transcription overlay
      AnimatedVisibility(
        visible = isRileyCalling,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
          .fillMaxSize()
          .zIndex(25f)
      ) {
        RileyPhoneAssistantSheet(
          viewModel = searchViewModel,
          onDismiss = { searchViewModel.dismissRileyAssistant() },
          modifier = Modifier.fillMaxSize()
        )
      }

      // Automated Wellness Check-In Call Overlay (Calm voice, affirmation, weather advisory)
      AnimatedVisibility(
        visible = isCheckInCallActive,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
          .fillMaxSize()
          .zIndex(26f)
      ) {
        CheckInCallOverlay(
          viewModel = dialViewModel,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Optional User Account & Data Preservation Sheet overlay
      AnimatedVisibility(
        visible = isAccountSheetOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
          .fillMaxSize()
          .zIndex(28f)
      ) {
        UserAccountSheet(
          viewModel = accountViewModel,
          favoritesCount = bookmarkedList.size,
          callLogsCount = callLogsList.size,
          dialsCount = emergencyDialsList.size,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Modern immersive loading / splash overlay screen!
      AnimatedVisibility(
        visible = !isFirstLoadCompleted && isConnected,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        com.example.ui.StreetRiseLoadingScreen(
          modifier = Modifier.fillMaxSize(),
          stateText = "Loading nearby resources...",
          onDismiss = {
            isFirstLoadCompleted = true
          }
        )
      }

      // Dynamic Geolocation Explanation Dialog inside Compose
      if (showLocationDialog) {
        AlertDialog(
          onDismissRequest = onDismissLocationDialog,
          icon = {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Location Access Needed",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
          },
          title = {
            Text(
              text = "Enable Location Services?",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              textAlign = TextAlign.Center
            )
          },
          text = {
            Text(
              text = "StreetRise uses your device location to locate food, shelter, hygiene centers, and work exchange opportunities nearby. This allows the map to center accurately and show verified routes relative to you.",
              fontSize = 14.sp,
              textAlign = TextAlign.Center
            )
          },
          confirmButton = {
            Button(
              onClick = onConfirmLocationDialog,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("location_permission_confirm_btn")
            ) {
              Text("Enable Location")
            }
          },
          dismissButton = {
            TextButton(
              onClick = onDismissLocationDialog,
              modifier = Modifier.testTag("location_permission_cancel_btn")
            ) {
              Text("Not Now")
            }
          },
          shape = RoundedCornerShape(16.dp),
          tonalElevation = 6.dp,
          modifier = Modifier.testTag("location_rational_dialog")
        )
      }
    }
  }
}

@Composable
fun LocalResourceDirectoryScreen(
  viewModel: SavedResViewModel,
  onBackToWeb: () -> Unit,
  isDeviceOnline: Boolean,
  showOnlyBookmarked: Boolean = false,
  onCallRiley: () -> Unit = {},
  userAccount: UserAccount? = null,
  onOpenAccount: () -> Unit = {}
) {
  val resourcesStateFlow = if (showOnlyBookmarked) viewModel.bookmarkedResources else viewModel.resourcesState
  val resources by resourcesStateFlow.collectAsStateWithLifecycle()
  val query by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val focusManager = LocalFocusManager.current

  // Filter keys
  val categories = listOf(
    Pair("shelter", "🏠 Shelter"),
    Pair("food", "🍲 Food Assistance"),
    Pair("hygiene", "🚿 Hygiene & Day Center"),
    Pair("medical", "🩺 Medical Clinic"),
    Pair("work_exchange", "🤝 Employment / Job Guide")
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header Bar
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (showOnlyBookmarked) "Saved Resources" else "StreetRise Offline Guide",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = if (showOnlyBookmarked) "Your customized physical and support access bookmarks" else "30 Tampa, Orlando & Lakeland verified resources saved locally",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isDeviceOnline) {
            Button(
              onClick = onBackToWeb,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Show Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            IconButton(
              onClick = onBackToWeb,
              modifier = Modifier.testTag("directory_offline_retry_btn")
            ) {
              Icon(Icons.Default.Refresh, "Retry Online Connection", tint = MaterialTheme.colorScheme.primary)
            }
          }

          IconButton(
            onClick = onOpenAccount,
            modifier = Modifier.size(32.dp).testTag("guide_account_btn")
          ) {
            if (userAccount != null && userAccount.isSignedIn) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(userAccount.displayName.take(1).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            } else {
              Icon(Icons.Default.AccountCircle, contentDescription = "Account & Cloud Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
          }
        }
      }
    }

    if (showOnlyBookmarked && userAccount != null) {
      OptionalAccountSyncBanner(
        userAccount = userAccount,
        onOpenAccountSheet = onOpenAccount,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )
    }

    // Prominent Call Riley 463-2-STREET Bar
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "Need Live Help or Bed Status?",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            "Riley is on call 24/7 at 463-2-STREET",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
        }
        ProminentCallNowButton(onCallRiley = onCallRiley)
      }
    }

    // Search bar
    OutlinedTextField(
      value = query,
      onValueChange = { viewModel.searchQuery.value = it },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("directory_search_input"),
      placeholder = { Text("Search shelter name, county, description...") },
      leadingIcon = { Icon(Icons.Default.Search, "Search Icon") },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { viewModel.searchQuery.value = "" }) {
            Icon(Icons.Default.Clear, "Clear Text")
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
      ),
      shape = RoundedCornerShape(12.dp)
    )

    // Category filter chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Clear filters
      FilterChip(
        selected = selectedCategory == null,
        onClick = { viewModel.selectedCategory.value = null },
        label = { Text("All") }
      )

      categories.forEach { (key, display) ->
        FilterChip(
          selected = selectedCategory == key,
          onClick = {
            if (selectedCategory == key) {
              viewModel.selectedCategory.value = null
            } else {
              viewModel.selectedCategory.value = key
            }
          },
          label = { Text(display) }
        )
      }
    }

    // Resource rows list
    if (resources.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          Icons.Default.Search,
          "No search results",
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = if (showOnlyBookmarked) "No Bookmarks Saved Yet" else "No resources match your search.",
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = if (showOnlyBookmarked) "Tapping bookmark hearts under Offline Guide listings organizes items here." else "Try clearing search queries or choosing another filter category.",
          textAlign = TextAlign.Center,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("offline_resources_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        items(resources, key = { it.id }) { item ->
          ResourceSeedRowCard(
            item = item,
            onToggleBookmark = { viewModel.toggleBookmark(item.id) },
            onUpdateNotes = { notes -> viewModel.updateNotes(item.id, notes) },
            onDial = {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}"))
              context.startActivity(intent)
            },
            onNavigate = {
              val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${item.lat},${item.lng}?q=${item.lat},${item.lng}(${Uri.encode(item.name)})"))
              context.startActivity(geoIntent)
            }
          )
        }
      }
    }
  }
}

@Composable
fun AppInfoScreen(
  isDeviceOnline: Boolean,
  onResetWeb: () -> Unit,
  chatViewModel: CommunityChatViewModel = viewModel()
) {
  val context = LocalContext.current
  val fcmTokenState by PushNotificationManager.fcmToken.collectAsStateWithLifecycle()
  val isChatAlwaysOn by chatViewModel.isAlwaysOn.collectAsStateWithLifecycle()
  val chatUserName by chatViewModel.userProfileName.collectAsStateWithLifecycle()
  val chatUserRole by chatViewModel.userRole.collectAsStateWithLifecycle()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Modern styled App Icon badge
    Surface(
      shadowElevation = 4.dp,
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.size(96.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = "SR", 
          fontSize = 36.sp, 
          fontWeight = FontWeight.ExtraBold, 
          color = MaterialTheme.colorScheme.primary
        )
      }
    }

    Text(
      text = "StreetRise Android",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Text(
      text = "Offline-First Support Shell v1.2",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.outline
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // Always-On Community Chat Settings Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("always_on_chat_settings_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Email, "Community Chat", tint = MaterialTheme.colorScheme.primary)
            Text(
              "Always-On Community Chat",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Switch(
            checked = isChatAlwaysOn,
            onCheckedChange = { chatViewModel.toggleAlwaysOn(it) },
            modifier = Modifier.testTag("app_info_always_on_switch")
          )
        }

        Text(
          text = if (isChatAlwaysOn) {
            "✓ Active: A floating live chat pill is displayed across all tabs (Web Map, Verified, Offline Guide, Saved, Providers) so everyone can chat in real-time."
          } else {
            "✕ Inactive: Floating chat bubble is hidden. You can still access community chat from the bottom navigation bar anytime."
          },
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Handle: $chatUserName ($chatUserRole)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )

          Button(
            onClick = { chatViewModel.openChat() },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("open_community_chat_btn")
          ) {
            Text("Open Chat", fontSize = 12.sp)
          }
        }
      }
    }

    // Network Sync Status card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("System Sync Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Connection State", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(if (isDeviceOnline) Color.Green else Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isDeviceOnline) "Online" else "Offline", fontWeight = FontWeight.Bold)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Web App Host", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("app.streetrise.org", fontWeight = FontWeight.SemiBold)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Cookie Persistence", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("Active & Synced", color = Color(0xFF4C8A32), fontWeight = FontWeight.Bold)
        }
      }
    }

    // Local DB Directory card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Local Resource Database", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        
        Text(
          "This companion application is integrated with a local Room SQLite database containing curated, verified safety assistance resources (Shelter, Kitchens, Hygiene, General Medical) across Tampa, Orlando & Lakeland so help is accessible in any highway dead-zone or cellular blackhole.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Curated Assets", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("30 Verified Seeds", fontWeight = FontWeight.Bold)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Database Engine", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("SQLite Room v2", fontWeight = FontWeight.Bold)
        }
      }
    }

    // Push Notifications Status Card
    Card(
      modifier = Modifier.fillMaxWidth().testTag("push_notification_status_card"),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Push Alerts & Notification State", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("FCM State", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(if (fcmTokenState != null) Color.Green else Color.Yellow, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (fcmTokenState != null) "Active" else "Idle / Connecting", fontWeight = FontWeight.Bold)
          }
        }

        if (fcmTokenState != null) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Registration token:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              text = fcmTokenState ?: "",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
              modifier = Modifier
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
                .padding(6.dp)
                .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Button(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("FCM Token", fcmTokenState)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Registration Token copied to Clipboard!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.align(Alignment.End),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.Share, "Copy Token", modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy Token", fontSize = 11.sp)
            }
          }
        } else {
          Text(
            "Waiting for Google Cloud FCM registry assignment. Verify Play Services access on device.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
      onClick = onResetWeb,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Refresh Map Connection")
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun ResourceSeedRowCard(
  item: ResourceSeedItem,
  onToggleBookmark: () -> Unit,
  onUpdateNotes: (String) -> Unit,
  onDial: () -> Unit,
  onNavigate: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }
  var personalNotesText by remember { mutableStateOf(item.notes) }

  // Sync state if saved database updates programmatically
  LaunchedEffect(item.notes) {
    personalNotesText = item.notes
  }

  val categoryColor = when (item.category) {
    "shelter" -> Color(0xFFD0E8B2) // High Density Light green
    "food" -> Color(0xFFFFEBDD) // Warm peach
    "medical" -> Color(0xFFE8F3D7) // Light Sage
    "hygiene" -> Color(0xFFE2F0FD) // Light blue
    "work_exchange" -> Color(0xFFF9F0FF) // Light Violet
    else -> MaterialTheme.colorScheme.surfaceVariant
  }

  val categoryLabelColor = when (item.category) {
    "shelter" -> Color(0xFF113800)
    "food" -> Color(0xFF855300)
    "medical" -> Color(0xFF284B10)
    "hygiene" -> Color(0xFF0D3254)
    "work_exchange" -> Color(0xFF4C1D6B)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  val categoryIcon = when (item.category) {
    "shelter" -> "🏠"
    "food" -> "🍲"
    "medical" -> "🩺"
    "hygiene" -> "🚿"
    "work_exchange" -> "🤝"
    else -> "📍"
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { isExpanded = !isExpanded }
      .testTag("resource_card_${item.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Thumbnail avatar
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(categoryColor, shape = RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(categoryIcon, fontSize = 22.sp)
        }

        // Title and description
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Text(
              text = item.name,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )

            IconButton(
              onClick = onToggleBookmark,
              modifier = Modifier
                .size(24.dp)
                .testTag("bookmark_btn_${item.id}")
            ) {
              Icon(
                imageVector = if (item.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (item.isBookmarked) "Bookmarked" else "Bookmark",
                tint = if (item.isBookmarked) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Text(
            text = item.org,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )

          Text(
            text = "${item.hours} • ${item.county.replaceFirstChar { it.uppercase() }} Co.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Main metadata and text
      Text(
        text = item.description,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = if (isExpanded) 100 else 2,
        overflow = TextOverflow.Ellipsis
      )

      if (isExpanded) {
        Spacer(modifier = Modifier.height(16.dp))

        // Full address block
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.LocationOn, "Address", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          Text(item.address, fontSize = 12.sp, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Call details
        if (item.phone.isNotEmpty()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Call, "Phone", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(item.phone, fontSize = 12.sp, style = MaterialTheme.typography.bodyMedium)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (item.phone.isNotEmpty()) {
            OutlinedButton(
              onClick = onDial,
              modifier = Modifier.weight(1f).height(40.dp).testTag("dial_button_${item.id}"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Call, "Call", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Dial Dialer", fontSize = 11.sp)
            }
          }

          Button(
            onClick = onNavigate,
            modifier = Modifier.weight(1f).height(40.dp).testTag("directions_button_${item.id}"),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.LocationOn, "Map", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Directions", fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Room Personal Notes Section
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          "My Notes (Saved to local database):",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = personalNotesText,
          onValueChange = {
            personalNotesText = it
            onUpdateNotes(it)
          },
          placeholder = { Text("Add private notes here (e.g. check-in times)...", fontSize = 12.sp) },
          maxLines = 4,
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          ),
          textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .testTag("room_notes_input_${item.id}")
        )
      } else {
        // Simple expanded helper prompt
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (item.notes.trim().isNotEmpty()) {
            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, "Has Notes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Has Notes", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
              }
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          Text(
            "Tap Card to expand",
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    }
  }
}
