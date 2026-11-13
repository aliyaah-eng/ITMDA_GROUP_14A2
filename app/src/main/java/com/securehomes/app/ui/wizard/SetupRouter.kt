package com.securehomes.app.ui.wizard

import android.content.Context
import android.content.Intent
import com.securehomes.app.study.StudyConfig

/**
 * Opens the setup version the participant was assigned to at registration.
 * Participants never choose their version, and always see the same one.
 */
object SetupRouter {

    const val EXTRA_DEVICE_ID = "DEVICE_ID"
    const val EXTRA_CATEGORY = "CATEGORY"
    const val EXTRA_MODEL = "MODEL"
    const val EXTRA_MAC = "MAC"

    fun screenFor(group: String?): Class<*> =
        if (group == StudyConfig.GROUP_GUIDED) GuidedWizardActivity::class.java else UnguidedFormActivity::class.java

    /** Set up a newly discovered device. */
    fun newDevice(context: Context, group: String?, category: String, model: String, mac: String): Intent =
        Intent(context, screenFor(group))
            .putExtra(EXTRA_CATEGORY, category)
            .putExtra(EXTRA_MODEL, model)
            .putExtra(EXTRA_MAC, mac)

    /** Reconfigure an existing device. */
    fun existingDevice(context: Context, group: String?, deviceId: String): Intent =
        Intent(context, screenFor(group)).putExtra(EXTRA_DEVICE_ID, deviceId)
}
