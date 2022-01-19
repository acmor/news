package common

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

fun Context.showKeyboard() {
    val inputManager = getSystemService<InputMethodManager>()!!
    @Suppress("DEPRECATION")
    inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun Context.hideKeyboard(view: View) {
    val inputManager = getSystemService<InputMethodManager>()!!
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}