package org.isoron.uhabits.utils

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.isoron.uhabits.activities.common.dialogs.CheckmarkDialog
import org.isoron.uhabits.activities.common.dialogs.NumberDialog
import java.lang.ref.WeakReference

var currentDialog: WeakReference<Dialog> = WeakReference(null)
var currentDialogFragment: WeakReference<DialogFragment> = WeakReference(null)

fun dismissCurrentDialog(preserveEntryDrafts: Boolean = false) {
    val fragment = currentDialogFragment.get()
    if (preserveEntryDrafts && (fragment is CheckmarkDialog || fragment is NumberDialog)) return
    currentDialog.get()?.dismiss()
    currentDialog = WeakReference(null)
    currentDialogFragment.get()?.dismiss()
    currentDialogFragment = WeakReference(null)
}

fun Dialog.dismissCurrentAndShow() {
    dismissCurrentDialog()
    currentDialog = WeakReference(this)
    show()
}

fun DialogFragment.dismissCurrentAndShow(fragmentManager: FragmentManager, tag: String) {
    dismissCurrentDialog()
    currentDialogFragment = WeakReference(this)
    show(fragmentManager, tag)
    fragmentManager.executePendingTransactions()
}
