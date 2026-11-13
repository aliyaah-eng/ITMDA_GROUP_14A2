package com.securehomes.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.securehomes.app.models.Device
import com.securehomes.app.models.GroupStats
import com.securehomes.app.models.SurveyResult
import com.securehomes.app.models.TelemetryEvent
import com.securehomes.app.models.User
import com.securehomes.app.security.PasswordHasher
import com.securehomes.app.study.StudyConfig
import com.securehomes.app.util.Validators
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Local SQLite database: users, devices, telemetry events and surveys.
 *
 * Version 2 stores each participant's assigned version in users.flow_type, adds per-user password salts, the device
 * fields needed by the scoring engine, and a security score on telemetry events so the
 * analytics screen can be calculated from real data.
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "securehomes_local.db"
        private const val DATABASE_VERSION = 3

        const val SEED_ADMIN_EMAIL = "group14.ad@securehomes.ac.za"
        const val SEED_ADMIN_PASSWORD = "Admin@1234"
        const val SEED_USER_EMAIL = "user@securehomes.ac.za"
        const val SEED_USER_PASSWORD = "User@1234"

        private const val T_USERS = "users"
        private const val T_DEVICES = "devices"
        private const val T_TELEMETRY = "telemetry_events"
        private const val T_SURVEYS = "surveys"

        /** Events that end a setup attempt; used by the analytics. */
        const val EVENT_SUBMITTED = "TASK_SUBMITTED"
        const val EVENT_ABANDONED = "TASK_ABANDONED"

        @Volatile private var instance: DatabaseHelper? = null

        /** One shared connection for the whole app. */
        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }

        fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        /** Unique IDs, so two events in the same millisecond can never collide. */
        fun newId(prefix: String): String = prefix + "_" + UUID.randomUUID().toString()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T_USERS (
                id TEXT PRIMARY KEY,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                created_at TEXT NOT NULL,
                flow_type TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_DEVICES (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                model TEXT,
                ip_address TEXT,
                subnet_mask TEXT,
                gateway TEXT,
                mac_address TEXT,
                status TEXT NOT NULL,
                security_score INTEGER NOT NULL,
                firmware_version TEXT,
                auto_update INTEGER NOT NULL,
                encryption TEXT NOT NULL,
                two_factor_enabled INTEGER NOT NULL,
                guest_isolated INTEGER NOT NULL,
                pwd_changed INTEGER NOT NULL,
                pwd_strength INTEGER NOT NULL,
                upnp_enabled INTEGER NOT NULL,
                mode_configured TEXT,
                last_updated TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_TELEMETRY (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                event_type TEXT NOT NULL,
                setup_mode TEXT,
                duration_ms INTEGER,
                error_count INTEGER,
                security_score INTEGER,
                timestamp TEXT NOT NULL,
                details TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_SURVEYS (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                setup_mode TEXT,
                sus_score REAL NOT NULL,
                nasa_tlx_workload REAL NOT NULL,
                answers TEXT,
                feedback TEXT,
                submitted_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_devices_user ON $T_DEVICES(user_id)")
        db.execSQL("CREATE INDEX idx_telemetry_mode ON $T_TELEMETRY(setup_mode, event_type)")
        seedDatabase(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Test data has no migration path: rebuild with the new schema.
        listOf(T_USERS, T_DEVICES, T_TELEMETRY, T_SURVEYS).forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    /**
     * Demo accounts: a researcher (admin) and a consumer assigned to Version A who owns one
     * hardened device and one in factory state. Seed data is never counted in the analytics,
     * because analytics only use telemetry events.
     */
    private fun seedDatabase(db: SQLiteDatabase) {
        val now = now()
        db.insert(T_USERS, null, userValues(
            User("usr_admin", "GROUP14 Admin", SEED_ADMIN_EMAIL, StudyConfig.ROLE_ADMIN, null, now),
            PasswordHasher.hash(SEED_ADMIN_PASSWORD)))
        db.insert(T_USERS, null, userValues(
            User("usr_demo", "Karabo Homeowner", SEED_USER_EMAIL, StudyConfig.ROLE_USER,
                StudyConfig.GROUP_GUIDED, now), PasswordHasher.hash(SEED_USER_PASSWORD)))

        val camera = Device(
            id = "dev_cam_01", userId = "usr_demo", name = "Front Door Camera",
            category = "Smart Camera", model = "SecureEye Pro HD-4K",
            ipAddress = "192.168.20.105", gateway = "192.168.20.1", macAddress = "3C:84:6A:21:9B:10",
            firmwareVersion = StudyConfig.LATEST_FIRMWARE, autoUpdate = true, encryption = "WPA3_AES256",
            twoFactorEnabled = true, guestNetworkIsolated = true, defaultPasswordChanged = true,
            passwordStrength = 4, upnpEnabled = false, modeConfigured = StudyConfig.GROUP_GUIDED,
            lastUpdated = now
        )
        val plug = factoryDevice("dev_plug_01", "usr_demo", "Garage Smart Plug", "Smart Plug",
            "EcoPower Smart Outlet", "24:6F:28:C4:10:7A", "192.168.1.112")
        listOf(camera, plug).forEach {
            it.securityScore = it.evaluate().score
            db.insert(T_DEVICES, null, deviceValues(it))
        }
    }

    /** A newly discovered device in its insecure factory state. */
    fun factoryDevice(id: String, userId: String, name: String, category: String, model: String,
                      mac: String, ip: String): Device {
        val device = Device(
            id = id, userId = userId, name = name, category = category, model = model,
            ipAddress = ip, macAddress = mac, firmwareVersion = StudyConfig.FACTORY_FIRMWARE,
            autoUpdate = false, encryption = StudyConfig.FACTORY_ENCRYPTION, twoFactorEnabled = false,
            guestNetworkIsolated = false, defaultPasswordChanged = false,
            passwordStrength = Validators.passwordStrength(StudyConfig.DEFAULT_DEVICE_PASSWORD),
            upnpEnabled = true, modeConfigured = "", lastUpdated = now()
        )
        device.securityScore = device.evaluate().score
        return device
    }

    // ---------------------------------------------------------------------
    // Users and authentication
    // ---------------------------------------------------------------------

    private fun userValues(user: User, passwordHash: String) = ContentValues().apply {
        put("id", user.id)
        put("full_name", user.fullName.trim())
        put("email", user.email.trim().lowercase())
        put("password_hash", passwordHash)
        put("role", user.role)
        put("created_at", user.createdAt)
        put("flow_type", user.studyGroup)
    }

    fun emailExists(email: String): Boolean =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_USERS WHERE email = ?",
            arrayOf(email.trim().lowercase())).use { it.moveToFirst() && it.getInt(0) > 0 }

    /** Number of participants already in a study group. */
    fun countInGroup(group: String): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_USERS WHERE role = ? AND flow_type = ?",
            arrayOf(StudyConfig.ROLE_USER, group)).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /**
     * Registers a participant and assigns them to Version A or B (balanced random).
     * @return the new user, or null if the email is already registered
     */
    fun registerParticipant(fullName: String, email: String, rawPassword: String): User? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (emailExists(email)) return null
            val group = StudyConfig.pickGroup(
                countInGroup(StudyConfig.GROUP_GUIDED), countInGroup(StudyConfig.GROUP_UNGUIDED))
            val user = User(newId("usr"), fullName.trim(), email.trim().lowercase(), StudyConfig.ROLE_USER,
                group, now())
            if (db.insert(T_USERS, null, userValues(user, PasswordHasher.hash(rawPassword))) == -1L) return null
            db.setTransactionSuccessful()
            return user
        } finally {
            db.endTransaction()
        }
    }

    fun loginUser(email: String, rawPassword: String): User? {
        readableDatabase.rawQuery("SELECT * FROM $T_USERS WHERE email = ?",
            arrayOf(email.trim().lowercase())).use { c ->
            if (!c.moveToFirst()) return null
            val hash = c.getString(c.getColumnIndexOrThrow("password_hash"))
            return if (PasswordHasher.verify(rawPassword, hash)) readUser(c) else null
        }
    }

    fun getUser(id: String): User? =
        readableDatabase.rawQuery("SELECT * FROM $T_USERS WHERE id = ?", arrayOf(id))
            .use { if (it.moveToFirst()) readUser(it) else null }

    private fun readUser(c: Cursor) = User(
        id = c.str("id"),
        fullName = c.str("full_name"),
        email = c.str("email"),
        role = c.str("role"),
        studyGroup = c.strOrNull("flow_type"),
        createdAt = c.str("created_at")
    )

    // ---------------------------------------------------------------------
    // Devices (CRUD). Participants only ever touch their own devices.
    // ---------------------------------------------------------------------

    private fun deviceValues(d: Device) = ContentValues().apply {
        put("id", d.id)
        put("user_id", d.userId)
        put("name", d.name.trim())
        put("category", d.category)
        put("model", d.model)
        put("ip_address", d.ipAddress)
        put("subnet_mask", d.subnetMask)
        put("gateway", d.gateway)
        put("mac_address", d.macAddress)
        put("status", d.status)
        put("security_score", d.securityScore)
        put("firmware_version", d.firmwareVersion)
        put("auto_update", d.autoUpdate.toInt())
        put("encryption", d.encryption)
        put("two_factor_enabled", d.twoFactorEnabled.toInt())
        put("guest_isolated", d.guestNetworkIsolated.toInt())
        put("pwd_changed", d.defaultPasswordChanged.toInt())
        put("pwd_strength", d.passwordStrength)
        put("upnp_enabled", d.upnpEnabled.toInt())
        put("mode_configured", d.modeConfigured)
        put("last_updated", d.lastUpdated)
    }

    /** All devices for a researcher (userId = null), otherwise only the user's own. */
    fun getDevices(userId: String?): List<Device> {
        val cursor = if (userId == null) {
            readableDatabase.rawQuery("SELECT * FROM $T_DEVICES ORDER BY last_updated DESC", null)
        } else {
            readableDatabase.rawQuery("SELECT * FROM $T_DEVICES WHERE user_id = ? ORDER BY last_updated DESC",
                arrayOf(userId))
        }
        return cursor.use { c -> buildList { while (c.moveToNext()) add(readDevice(c)) } }
    }

    fun getDeviceById(id: String): Device? =
        readableDatabase.rawQuery("SELECT * FROM $T_DEVICES WHERE id = ?", arrayOf(id))
            .use { if (it.moveToFirst()) readDevice(it) else null }

    /** Saves a new device; the score is always recalculated, never trusted from the caller. */
    fun insertDevice(device: Device): Boolean {
        device.securityScore = device.evaluate().score
        device.lastUpdated = now()
        return writableDatabase.insert(T_DEVICES, null, deviceValues(device)) != -1L
    }

    fun updateDevice(device: Device): Boolean {
        device.securityScore = device.evaluate().score
        device.lastUpdated = now()
        return writableDatabase.update(T_DEVICES, deviceValues(device), "id = ? AND user_id = ?",
            arrayOf(device.id, device.userId)) > 0
    }

    fun toggleDeviceStatus(id: String, userId: String): Boolean {
        val device = getDeviceById(id)?.takeIf { it.userId == userId } ?: return false
        device.status = if (device.status == "online") "offline" else "online"
        return updateDevice(device)
    }

    fun updateFirmware(id: String, userId: String): Boolean {
        val device = getDeviceById(id)?.takeIf { it.userId == userId } ?: return false
        device.firmwareVersion = StudyConfig.LATEST_FIRMWARE
        return updateDevice(device)
    }

    fun deleteDevice(id: String, userId: String): Boolean =
        writableDatabase.delete(T_DEVICES, "id = ? AND user_id = ?", arrayOf(id, userId)) > 0

    private fun readDevice(c: Cursor) = Device(
        id = c.str("id"),
        userId = c.str("user_id"),
        name = c.str("name"),
        category = c.str("category"),
        model = c.strOrNull("model") ?: "",
        ipAddress = c.strOrNull("ip_address") ?: "",
        subnetMask = c.strOrNull("subnet_mask") ?: "255.255.255.0",
        gateway = c.strOrNull("gateway") ?: "",
        macAddress = c.strOrNull("mac_address") ?: "",
        status = c.str("status"),
        securityScore = c.int("security_score"),
        firmwareVersion = c.strOrNull("firmware_version") ?: StudyConfig.FACTORY_FIRMWARE,
        autoUpdate = c.int("auto_update") == 1,
        encryption = c.str("encryption"),
        twoFactorEnabled = c.int("two_factor_enabled") == 1,
        guestNetworkIsolated = c.int("guest_isolated") == 1,
        defaultPasswordChanged = c.int("pwd_changed") == 1,
        passwordStrength = c.int("pwd_strength"),
        upnpEnabled = c.int("upnp_enabled") == 1,
        modeConfigured = c.strOrNull("mode_configured") ?: "",
        lastUpdated = c.strOrNull("last_updated") ?: ""
    )

    // ---------------------------------------------------------------------
    // Telemetry
    // ---------------------------------------------------------------------

    fun logTelemetryEvent(event: TelemetryEvent): Boolean {
        val values = ContentValues().apply {
            put("id", event.id)
            put("user_id", event.userId)
            put("event_type", event.eventType)
            put("setup_mode", event.setupMode)
            put("duration_ms", event.durationMs)
            put("error_count", event.errorCount)
            if (event.securityScore != null) put("security_score", event.securityScore) else putNull("security_score")
            put("timestamp", event.timestamp)
            put("details", event.details)
        }
        return writableDatabase.insert(T_TELEMETRY, null, values) != -1L
    }

    /** Convenience for the common case. */
    fun log(userId: String, type: String, mode: String, details: String = "",
            durationMs: Long = 0, errors: Int = 0, score: Int? = null) {
        logTelemetryEvent(TelemetryEvent(newId("evt"), userId, type, mode, durationMs, errors, score, now(), details))
    }

    /** Most recent events first (insertion order, not text timestamps). */
    fun getTelemetryEvents(limit: Int = 50): List<TelemetryEvent> =
        readableDatabase.rawQuery("SELECT * FROM $T_TELEMETRY ORDER BY rowid DESC LIMIT ?",
            arrayOf(limit.toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    TelemetryEvent(
                        id = c.str("id"), userId = c.str("user_id"), eventType = c.str("event_type"),
                        setupMode = c.strOrNull("setup_mode") ?: "", durationMs = c.long("duration_ms"),
                        errorCount = c.int("error_count"),
                        securityScore = if (c.isNull(c.getColumnIndexOrThrow("security_score"))) null else c.int("security_score"),
                        timestamp = c.str("timestamp"), details = c.strOrNull("details") ?: ""
                    )
                )
            }
        }

    // ---------------------------------------------------------------------
    // Surveys
    // ---------------------------------------------------------------------

    fun insertSurvey(survey: SurveyResult): Boolean {
        val values = ContentValues().apply {
            put("id", survey.id)
            put("user_id", survey.userId)
            put("setup_mode", survey.setupMode)
            put("sus_score", survey.susScore)
            put("nasa_tlx_workload", survey.nasaTlxWorkload)
            put("answers", survey.answers)
            put("feedback", survey.feedback)
            put("submitted_at", survey.submittedAt)
        }
        return writableDatabase.insert(T_SURVEYS, null, values) != -1L
    }

    fun hasSurvey(userId: String): Boolean =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_SURVEYS WHERE user_id = ?", arrayOf(userId))
            .use { it.moveToFirst() && it.getInt(0) > 0 }

    // ---------------------------------------------------------------------
    // Analytics: everything is calculated from logged data, nothing is typed in.
    // ---------------------------------------------------------------------

    fun getGroupStats(group: String): GroupStats {
        val db = readableDatabase
        val participants = countInGroup(group)
        var submitted = 0
        var hardened = 0
        var avgDuration: Double? = null
        var avgErrors: Double? = null
        var avgScore: Double? = null
        db.rawQuery(
            "SELECT COUNT(*), SUM(security_score >= 85), AVG(duration_ms), AVG(error_count), AVG(security_score) " +
                "FROM $T_TELEMETRY WHERE setup_mode = ? AND event_type = ?", arrayOf(group, EVENT_SUBMITTED)
        ).use { c ->
            if (c.moveToFirst()) {
                submitted = c.getInt(0)
                hardened = if (c.isNull(1)) 0 else c.getInt(1)
                avgDuration = if (c.isNull(2)) null else c.getDouble(2)
                avgErrors = if (c.isNull(3)) null else c.getDouble(3)
                avgScore = if (c.isNull(4)) null else c.getDouble(4)
            }
        }
        val abandoned = db.rawQuery(
            "SELECT COUNT(*) FROM $T_TELEMETRY WHERE setup_mode = ? AND event_type = ?",
            arrayOf(group, EVENT_ABANDONED)
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        var avgSus: Double? = null
        var avgTlx: Double? = null
        var surveys = 0
        db.rawQuery("SELECT COUNT(*), AVG(sus_score), AVG(nasa_tlx_workload) FROM $T_SURVEYS WHERE setup_mode = ?",
            arrayOf(group)).use { c ->
            if (c.moveToFirst()) {
                surveys = c.getInt(0)
                avgSus = if (c.isNull(1)) null else c.getDouble(1)
                avgTlx = if (c.isNull(2)) null else c.getDouble(2)
            }
        }
        return GroupStats(participants, submitted, abandoned, hardened, avgDuration, avgErrors, avgScore,
            avgSus, avgTlx, surveys)
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun Boolean.toInt() = if (this) 1 else 0
    private fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col))
    private fun Cursor.strOrNull(col: String): String? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.int(col: String): Int = getInt(getColumnIndexOrThrow(col))
    private fun Cursor.long(col: String): Long = getLong(getColumnIndexOrThrow(col))
}
