package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NexaPurgeDatabase
import com.example.data.entities.CleanHistory
import com.example.data.entities.QuarantinedFile
import com.example.data.repository.JunkCategory
import com.example.data.repository.NexaPurgeRepository
import com.example.data.repository.ScannedJunkFile
import com.example.data.repository.StorageStats
import com.example.service.AutoCleanReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NexaPurgeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexaPurgeDatabase.getDatabase(application)
    private val repository = NexaPurgeRepository(
        context = application,
        quarantinedFileDao = db.quarantinedFileDao(),
        cleanHistoryDao = db.cleanHistoryDao()
    )

    // Reactive streams from database
    val quarantinedFiles: StateFlow<List<QuarantinedFile>> = repository.quarantinedFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cleanHistory: StateFlow<List<CleanHistory>> = repository.cleanHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Configuration States
    private val sharedPrefs = application.getSharedPreferences("nexapurge_prefs", Context.MODE_PRIVATE)

    private val _autoCleanEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_clean_enabled", false))
    val autoCleanEnabled: StateFlow<Boolean> = _autoCleanEnabled.asStateFlow()

    private val _autoCleanIntervalHours = MutableStateFlow(sharedPrefs.getInt("auto_clean_interval_hours", 24))
    val autoCleanIntervalHours: StateFlow<Int> = _autoCleanIntervalHours.asStateFlow()

    private val _rootAccessEnabled = MutableStateFlow(sharedPrefs.getBoolean("root_access_enabled", false))
    val rootAccessEnabled: StateFlow<Boolean> = _rootAccessEnabled.asStateFlow()

    private val _alertThresholdPercent = MutableStateFlow(sharedPrefs.getInt("alert_threshold_percent", 15))
    val alertThresholdPercent: StateFlow<Int> = _alertThresholdPercent.asStateFlow()

    private val _currentSelectedPage = MutableStateFlow("dashboard")
    val currentSelectedPage: StateFlow<String> = _currentSelectedPage.asStateFlow()

    // Scanning & Action state
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    private val _scannedFiles = MutableStateFlow<List<ScannedJunkFile>>(emptyList())
    val scannedFiles: StateFlow<List<ScannedJunkFile>> = _scannedFiles.asStateFlow()

    private val _selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFileIds: StateFlow<Set<String>> = _selectedFileIds.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _scanState = MutableStateFlow("idle") // idle, scanning, scanned, cleaned
    val scanState: StateFlow<String> = _scanState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _lastCleanedSummary = MutableStateFlow<Pair<Long, Int>?>(null)
    val lastCleanedSummary: StateFlow<Pair<Long, Int>?> = _lastCleanedSummary.asStateFlow()

    init {
        loadStorageStats()
    }

    fun selectPage(page: String) {
        _currentSelectedPage.value = page
    }

    fun loadStorageStats() {
        _storageStats.value = repository.getStorageStats(_alertThresholdPercent.value)
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // Checking if physical device shows root access indicators
    fun isDeviceRooted(): Boolean = repository.isDeviceRooted()

    // Trigger full storage audit scan
    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanState.value = "scanning"
            _scannedFiles.value = emptyList()
            _selectedFileIds.value = emptySet()
            
            // Artificial delay to make search process feels deep and premium
            delay(1800)

            val files = repository.scanJunkFiles(_rootAccessEnabled.value)
            _scannedFiles.value = files
            _selectedFileIds.value = files.map { it.id }.toSet() // Auto check all by default
            _isScanning.value = false
            _scanState.value = "scanned"
            loadStorageStats()
        }
    }

    // Toggle specific file check status in Scan Results List
    fun toggleFileSelection(id: String) {
        val current = _selectedFileIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedFileIds.value = current
    }

    // Select or Deselect all scanned items
    fun toggleSelectAll(selectAll: Boolean) {
        if (selectAll) {
            _selectedFileIds.value = _scannedFiles.value.map { it.id }.toSet()
        } else {
            _selectedFileIds.value = emptySet()
        }
    }

    // Running the actual cleanup action
    fun executeCleanUp() {
        viewModelScope.launch {
            val selectedList = _scannedFiles.value.filter { _selectedFileIds.value.contains(it.id) }
            if (selectedList.isEmpty()) {
                _toastMessage.value = "Pilih setidaknya satu jenis berkas sampah untuk dibersihkan"
                return@launch
            }

            _isCleaning.value = true
            delay(1500) // Aesthetic delay for pro system optimization scanning animation

            val result = repository.executeClean(
                selectedFiles = selectedList,
                isRootClean = _rootAccessEnabled.value && selectedList.any { it.category == JunkCategory.DEEP_SYSTEM }
            )

            _lastCleanedSummary.value = result
            _scannedFiles.value = emptyList()
            _selectedFileIds.value = emptySet()
            _isCleaning.value = false
            _scanState.value = "cleaned"
            
            loadStorageStats()
            _toastMessage.value = "Berhasil mengoptimalkan perangkat!"
        }
    }

    fun resetStateToIdle() {
        _scanState.value = "idle"
        _lastCleanedSummary.value = null
    }

    // Restore single file from Quarantine
    fun restoreFile(file: QuarantinedFile) {
        viewModelScope.launch {
            val success = repository.restoreQuarantinedFile(file)
            if (success) {
                _toastMessage.value = "${file.filename} berhasil dipulihkan ke lokasi semula"
            } else {
                _toastMessage.value = "Gagal memulihkan berkas"
            }
            loadStorageStats()
        }
    }

    // Permanently remove single quarantined file
    fun permanentlyDeleteQuarantinedFile(file: QuarantinedFile) {
        viewModelScope.launch {
            repository.permanentlyDeleteFile(file)
            _toastMessage.value = "Berkas dihapus permanen"
            loadStorageStats()
        }
    }

    // Empty entire Bin
    fun clearTrashBin() {
        viewModelScope.launch {
            repository.emptyTrash()
            _toastMessage.value = "Seluruh Karantina berhasil dibersihkan permanen"
            loadStorageStats()
        }
    }

    // Configuration Settings modifies
    fun toggleAutoClean(enabled: Boolean) {
        viewModelScope.launch {
            _autoCleanEnabled.value = enabled
            sharedPrefs.edit().putBoolean("auto_clean_enabled", enabled).apply()
            
            if (enabled) {
                AutoCleanReceiver.schedulePeriodicClean(
                    context = getApplication(),
                    intervalHours = _autoCleanIntervalHours.value
                )
                _toastMessage.value = "Auto Clean diaktifkan berkala setiap ${_autoCleanIntervalHours.value} jam"
            } else {
                AutoCleanReceiver.cancelPeriodicClean(getApplication())
                _toastMessage.value = "Auto Clean dimatikan"
            }
        }
    }

    fun setAutoCleanIntervalHours(hours: Int) {
        viewModelScope.launch {
            _autoCleanIntervalHours.value = hours
            sharedPrefs.edit().putInt("auto_clean_interval_hours", hours).apply()
            
            if (_autoCleanEnabled.value) {
                // Re-schedule with new timing parameters
                AutoCleanReceiver.schedulePeriodicClean(getApplication(), hours)
            }
        }
    }

    fun toggleRootAccess(enabled: Boolean) {
        viewModelScope.launch {
            _rootAccessEnabled.value = enabled
            sharedPrefs.edit().putBoolean("root_access_enabled", enabled).apply()
            if (enabled) {
                _toastMessage.value = "Dukungan Root Pro diaktifkan: Membuka akses file cache cadangan sistem"
            } else {
                _toastMessage.value = "Akses Root dimatikan"
            }
        }
    }

    fun setAlertThresholdPercent(percent: Int) {
        viewModelScope.launch {
            _alertThresholdPercent.value = percent
            sharedPrefs.edit().putInt("alert_threshold_percent", percent).apply()
            loadStorageStats()
        }
    }
}
