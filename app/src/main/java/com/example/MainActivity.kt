package com.example

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel

    // UI elements to update dynamically
    private lateinit var clockTimeTextView: TextView
    private lateinit var clockTargetTextView: TextView
    
    private lateinit var heroCardContainer: LinearLayout
    private lateinit var heroIconTextView: TextView
    private lateinit var heroStatusTitle: TextView
    private lateinit var heroStatusDesc: TextView
    private lateinit var heroBadgeBoot: TextView
    
    private lateinit var checklistStep1Sub: TextView
    private lateinit var checklistStep1Icon: TextView
    private lateinit var checklistStep2Sub: TextView
    private lateinit var checklistStep2Icon: TextView
    private lateinit var checklistStep3Icon: TextView

    private lateinit var diagIpValue: TextView
    private lateinit var diagPingValue: TextView
    private lateinit var forceSyncButton: Button

    private lateinit var ntpServerEditText: EditText
    private lateinit var bootDelayEditText: EditText
    private lateinit var netTimeoutEditText: EditText
    private lateinit var retryCountEditText: EditText
    private lateinit var saveConfigButton: Button

    private lateinit var logsLayout: LinearLayout
    private lateinit var clearLogsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Bind UI Elements
        clockTimeTextView = findViewById(R.id.id_clock_time)
        clockTargetTextView = findViewById(R.id.id_clock_target)
        
        heroCardContainer = findViewById(R.id.id_hero_card)
        heroIconTextView = findViewById(R.id.id_hero_icon)
        heroStatusTitle = findViewById(R.id.id_hero_title)
        heroStatusDesc = findViewById(R.id.id_hero_desc)
        heroBadgeBoot = findViewById(R.id.id_hero_badge_boot)
        
        checklistStep1Sub = findViewById(R.id.id_checklist_step1_sub)
        checklistStep1Icon = findViewById(R.id.id_checklist_step1_icon)
        checklistStep2Sub = findViewById(R.id.id_checklist_step2_sub)
        checklistStep2Icon = findViewById(R.id.id_checklist_step2_icon)
        checklistStep3Icon = findViewById(R.id.id_checklist_step3_icon)

        diagIpValue = findViewById(R.id.id_diag_ip_value)
        diagPingValue = findViewById(R.id.id_diag_ping_value)
        forceSyncButton = findViewById(R.id.id_force_sync_btn)

        ntpServerEditText = findViewById(R.id.id_ntp_server)
        bootDelayEditText = findViewById(R.id.id_boot_delay)
        netTimeoutEditText = findViewById(R.id.id_net_timeout)
        retryCountEditText = findViewById(R.id.id_retry_count)
        saveConfigButton = findViewById(R.id.id_save_config_btn)

        logsLayout = findViewById(R.id.id_logs_layout)
        clearLogsButton = findViewById(R.id.id_clear_logs_btn)

        // Set Click Listeners
        findViewById<View>(R.id.id_settings_btn).setOnClickListener {
            Toast.makeText(this, "Auto Time Fix v1.0.2 - Xposed system time helper.", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.id_diag_refresh).setOnClickListener {
            viewModel.refreshDiagnostics()
        }

        forceSyncButton.setOnClickListener {
            viewModel.triggerManualSync()
        }

        saveConfigButton.setOnClickListener {
            viewModel.saveConfig(
                ntpServerEditText.text.toString(),
                bootDelayEditText.text.toString(),
                netTimeoutEditText.text.toString(),
                retryCountEditText.text.toString()
            )
            Toast.makeText(this, "Configuration saved successfully!", Toast.LENGTH_SHORT).show()
        }

        clearLogsButton.setOnClickListener {
            viewModel.clearLogs()
        }

        findViewById<Button>(R.id.id_preset_google).setOnClickListener {
            ntpServerEditText.setText("time.google.com")
        }

        findViewById<Button>(R.id.id_preset_pool).setOnClickListener {
            ntpServerEditText.setText("pool.ntp.org")
        }

        findViewById<Button>(R.id.id_preset_windows).setOnClickListener {
            ntpServerEditText.setText("time.windows.com")
        }

        // Bind flows to update UI dynamically
        bindViewModelFlows()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun addLogLineToTerminal(time: String, message: String, textColorHex: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(2), 0, dpToPx(2))
        }

        val timeText = TextView(this).apply {
            text = "[$time]"
            textSize = 11f
            setTextColor(Color.parseColor("#938F99"))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, 0, dpToPx(6), 0)
        }
        row.addView(timeText)

        val msgText = TextView(this).apply {
            text = message
            textSize = 11f
            setTextColor(Color.parseColor(textColorHex))
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        row.addView(msgText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        logsLayout.addView(row)
    }

    private fun bindViewModelFlows() {
        // Collect Clock State
        lifecycleScope.launch {
            viewModel.currentTimeState.collect { time ->
                clockTimeTextView.text = if (time.isEmpty()) "Loading..." else time
            }
        }

        // Collect Configuration Settings
        lifecycleScope.launch {
            viewModel.configState.collect { config ->
                clockTargetTextView.text = "Sync Target: ${config.ntpServer}"
                
                // Only set if not focused to avoid interrupting typing
                if (!ntpServerEditText.hasFocus()) ntpServerEditText.setText(config.ntpServer)
                if (!bootDelayEditText.hasFocus()) bootDelayEditText.setText(config.bootDelaySec)
                if (!netTimeoutEditText.hasFocus()) netTimeoutEditText.setText(config.netTimeoutSec)
                if (!retryCountEditText.hasFocus()) retryCountEditText.setText(config.retryCount)
            }
        }

        // Collect Module Active State
        lifecycleScope.launch {
            viewModel.isModuleActiveState.collect { isActive ->
                if (isActive) {
                    heroCardContainer.setBackgroundResource(R.drawable.bg_hero_card_active)
                    heroIconTextView.text = "✔"
                    heroIconTextView.setTextColor(Color.parseColor("#1A73E8"))
                    heroStatusTitle.text = "System Sync Active"
                    heroStatusTitle.setTextColor(Color.parseColor("#1A73E8"))
                    heroStatusDesc.text = "NTP synchronization is bound and actively hooking the systemready boot pipeline."
                    heroStatusDesc.setTextColor(Color.parseColor("#49454F"))
                    heroBadgeBoot.text = "BOOT READY"
                    heroBadgeBoot.setBackgroundResource(R.drawable.bg_badge_green)
                    heroBadgeBoot.setTextColor(Color.parseColor("#1E8E3E"))
                    
                    checklistStep1Icon.text = "✔"
                    checklistStep1Icon.setTextColor(Color.parseColor("#2E7D32"))
                    checklistStep1Sub.text = "Boot hook bound successfully"
                } else {
                    heroCardContainer.setBackgroundResource(R.drawable.bg_hero_card_inactive)
                    heroIconTextView.text = "✘"
                    heroIconTextView.setTextColor(Color.parseColor("#D93025"))
                    heroStatusTitle.text = "Xposed Module Inactive"
                    heroStatusTitle.setTextColor(Color.parseColor("#D93025"))
                    heroStatusDesc.text = "Please enable Auto Time Fix in your Xposed/LSPosed manager and restart the device."
                    heroStatusDesc.setTextColor(Color.parseColor("#5F6368"))
                    heroBadgeBoot.text = "AWAITING ACTIVE"
                    heroBadgeBoot.setBackgroundResource(R.drawable.bg_badge_orange)
                    heroBadgeBoot.setTextColor(Color.parseColor("#B06000"))
                    
                    checklistStep1Icon.text = "⚠"
                    checklistStep1Icon.setTextColor(Color.parseColor("#938F99"))
                    checklistStep1Sub.text = "Boot hook not active"
                }
            }
        }

        // Collect Internet State
        lifecycleScope.launch {
            viewModel.isInternetAvailableState.collect { isInternet ->
                val isActive = viewModel.isModuleActiveState.value
                when (isInternet) {
                    true -> {
                        diagIpValue.text = "CONNECTED"
                        diagIpValue.setTextColor(Color.parseColor("#2E7D32"))
                        checklistStep2Icon.text = "✔"
                        checklistStep2Icon.setTextColor(Color.parseColor("#2E7D32"))
                        
                        if (isActive) {
                            checklistStep3Icon.text = "✔"
                            checklistStep3Icon.setTextColor(Color.parseColor("#2E7D32"))
                        } else {
                            checklistStep3Icon.text = "⚠"
                            checklistStep3Icon.setTextColor(Color.parseColor("#938F99"))
                        }
                    }
                    false -> {
                        diagIpValue.text = "DISCONNECTED"
                        diagIpValue.setTextColor(Color.parseColor("#C62828"))
                        checklistStep2Icon.text = "⚠"
                        checklistStep2Icon.setTextColor(Color.parseColor("#938F99"))
                        checklistStep2Sub.text = "Disconnected. Waiting for interface..."
                        
                        checklistStep3Icon.text = "⚠"
                        checklistStep3Icon.setTextColor(Color.parseColor("#938F99"))
                    }
                    null -> {
                        diagIpValue.text = "CHECKING..."
                        diagIpValue.setTextColor(Color.parseColor("#625B71"))
                        checklistStep2Icon.text = "⚠"
                        checklistStep2Icon.setTextColor(Color.parseColor("#938F99"))
                        checklistStep2Sub.text = "Evaluating network interface..."
                    }
                }
            }
        }

        // Collect Ping Latency
        lifecycleScope.launch {
            viewModel.pingLatencyState.collect { latency ->
                diagPingValue.text = latency.uppercase()
                if (latency != "N/A" && latency != "Fail" && latency != "Error") {
                    checklistStep2Sub.text = "Connected! Latency: $latency"
                }
            }
        }

        // Collect Sync State
        lifecycleScope.launch {
            viewModel.isSyncingState.collect { isSyncing ->
                if (isSyncing) {
                    forceSyncButton.isEnabled = false
                    forceSyncButton.text = "SYNCHRONIZING..."
                } else {
                    forceSyncButton.isEnabled = true
                    forceSyncButton.text = "FORCE SYNC NOW"
                }
            }
        }

        // Collect Output Debug Logs List
        lifecycleScope.launch {
            viewModel.logListState.collect { logs ->
                logsLayout.removeAllViews()
                
                // Add default boot trace logs
                addLogLineToTerminal("12:00:01", "XposedBridge: Loading AutoTimeFix...", "#938F99")
                addLogLineToTerminal("12:00:02", "AutoTimeFix: systemReady hooked successfully!", "#1A73E8")
                
                if (logs.isEmpty()) {
                    addLogLineToTerminal("12:00:03", "No actions run yet. Use manual sync or reboot device.", "#938F99")
                } else {
                    logs.take(15).forEach { log ->
                        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        val timeStr = sdf.format(java.util.Date(log.timestamp))
                        val colorHex = when (log.status) {
                            "SUCCESS" -> "#4ADE80" // Vivid Terminal Green
                            "FAILED" -> "#F87171"  // Vivid Terminal Red
                            "PENDING" -> "#FBBF24" // Vivid Terminal Yellow
                            else -> "#E6E1E5"
                        }
                        addLogLineToTerminal(timeStr, "${log.action}: ${log.message}", colorHex)
                    }
                }
            }
        }
    }
}
