package settings

import android.content.Context
import androidx.lifecycle.ViewModel
import co.appreactor.news.Database
import common.*
import com.nextcloud.android.sso.exceptions.SSOException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class SettingsViewModel(
    private val prefs: PreferencesRepository,
    private val db: Database,
) : ViewModel() {

    fun getPreferences() = runBlocking { prefs.get() }

    fun savePreferences(action: Preferences.() -> Unit) = runBlocking { prefs.save(action) }

    fun getAccountName(context: Context): String {
        val prefs = runBlocking { getPreferences() }

        return if (prefs.nextcloudServerUrl.isNotBlank()) {
            val username = prefs.nextcloudServerUsername
            "$username@${prefs.nextcloudServerUrl.replace("https://", "")}"
        } else {
            try {
                val account = SingleAccountHelper.getCurrentSingleSignOnAccount(context)
                account.name
            } catch (e: SSOException) {
                Timber.e(e)
                "unknown"
            }
        }
    }

    fun logOut() {
        db.apply {
            transaction {
                entryQueries.deleteAll()
                entryEnclosureQueries.deleteAll()
                entryImageQueries.deleteAll()
                entryImagesMetadataQueries.deleteAll()
                feedQueries.deleteAll()
                loggedExceptionQueries.deleteAll()
                preferenceQueries.deleteAll()
            }
        }
    }
}