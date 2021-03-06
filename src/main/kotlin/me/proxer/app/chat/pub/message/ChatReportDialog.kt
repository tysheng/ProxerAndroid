package me.proxer.app.chat.pub.message

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding2.widget.editorActionEvents
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.functions.Predicate
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class ChatReportDialog : BaseDialog() {

    companion object {
        private const val MESSAGE_ID_ARGUMENT = "message_id"

        fun show(activity: AppCompatActivity, messageId: String) = ChatReportDialog().apply {
            arguments = bundleOf(MESSAGE_ID_ARGUMENT to messageId)
        }.show(activity.supportFragmentManager, "chat_report_dialog")
    }

    private val viewModel by unsafeLazy { ChatReportViewModelProvider.get(this) }

    private val messageInput: TextInputEditText by bindView(R.id.message)
    private val messageContainer: TextInputLayout by bindView(R.id.messageContainer)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val progress: ProgressBar by bindView(R.id.progress)

    private val messageId: String
        get() = requireArguments().getString(MESSAGE_ID_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(requireContext())
        .autoDismiss(false)
        .title(R.string.dialog_chat_report_title)
        .positiveText(R.string.dialog_chat_report_positive)
        .negativeText(R.string.cancel)
        .onPositive { _, _ -> validateAndSendReport() }
        .onNegative { _, _ -> dismiss() }
        .customView(R.layout.dialog_chat_report, true)
        .build()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        messageInput.editorActionEvents(Predicate { event -> event.actionId() == EditorInfo.IME_ACTION_GO })
            .filter { event -> event.actionId() == EditorInfo.IME_ACTION_GO }
            .autoDispose(this)
            .subscribe { validateAndSendReport() }

        messageInput.textChanges()
            .skipInitialValue()
            .autoDispose(this)
            .subscribe { setError(messageContainer, null) }

        viewModel.data.observe(this, Observer {
            it?.let {
                dismiss()
            }
        })

        viewModel.error.observe(this, Observer {
            it?.let {
                viewModel.error.value = null

                requireContext().longToast(it.message)
            }
        })

        viewModel.isLoading.observe(this, Observer {
            inputContainer.visibility = if (it == true) View.GONE else View.VISIBLE
            progress.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        if (savedInstanceState == null) {
            messageInput.requestFocus()

            dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    private fun validateAndSendReport() {
        val message = messageInput.safeText.trim().toString()

        if (validateInput(message)) {
            viewModel.sendReport(messageId, message)
        }
    }

    private fun validateInput(message: String) = when {
        message.isBlank() -> {
            setError(messageContainer, getString(R.string.dialog_chat_error_message))

            false
        }
        else -> true
    }

    private fun setError(container: TextInputLayout, errorText: String?) {
        container.isErrorEnabled = errorText != null
        container.error = errorText
    }
}
