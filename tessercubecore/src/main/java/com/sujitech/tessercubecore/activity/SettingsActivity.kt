package com.sujitech.tessercubecore.activity

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.UserKeyData

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : BaseActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit()
        title = pref.title
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frameLayout, SettingsFragment())
                    .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "tessercube"
        setPreferencesFromResource(R.xml.root, rootKey)
        findPreference<ListPreference>("keyboard_default_signature")?.also {
            DbContext.data.select(UserKeyData::class).get().toList().mapIndexed { index, userKeyData ->
                index.toString() to "${userKeyData.contactData?.name}(${userKeyData.contactData?.email}) ${userKeyData.contactData?.keyData?.firstOrNull()?.fingerPrint?.toUpperCase()?.takeLast(8)}"
            }.let {
                listOf(("-2" to "Auto"), ("-1" to "Do not sign messages"))// + it
            }.toMap().let { data ->
                it.entries = data.values.toTypedArray()
                it.entryValues = data.keys.toTypedArray()
                it.setValueIndex((Settings.get("keyboard_default_signature", "-2").toIntOrNull() ?: -2) + 2)
            }
        }
    }
}
