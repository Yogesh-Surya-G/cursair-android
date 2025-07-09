import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private val ONBOARDING_FINISHED_KEY = booleanPreferencesKey("onboarding_finished")

    val isOnboardingFinished: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_FINISHED_KEY] ?: false
        }

    suspend fun setOnboardingFinished() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_FINISHED_KEY] = true
        }
    }
}