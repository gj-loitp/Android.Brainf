package com.fredhappyface.brainf

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import com.google.android.material.card.MaterialCardView

/**
 * Settings activity providing the ability to update the theme
 */
class ActivitySettings : ActivityThemable() {
	/**
	 * Override the onCreate method from ActivityThemable adding the activity_settings view and selecting
	 * the current theme
	 *
	 * @param savedInstanceState saved state
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
		// Theme apply border to selected
		val themeChoices = findViewById<LinearLayout>(R.id.theme)
		val currentTheme = sharedPreferences.getInt("theme", 3)
		val cardView = themeChoices.getChildAt(currentTheme) as MaterialCardView
		cardView.strokeWidth = (3 * applicationContext.resources.displayMetrics.density).toInt()
		cardView.strokeColor = resources.getColor(R.color.red, theme)
	}

	/**
	 * Compare view.id of the radio button selected to set the theme and recreate the activity
	 *
	 * @param view
	 */
	fun changeTheme(view: View) {
		val editor: SharedPreferences.Editor = mSharedPreferences.edit()
		editor.putInt(
			"theme", when (view.id) {
				R.id.radioLight -> 0
				R.id.radioDark -> 1
				R.id.radioBlack -> 2
				else -> 3
			}
		)
		editor.apply()
		recreate()
	}
}