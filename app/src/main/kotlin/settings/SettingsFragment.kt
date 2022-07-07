package settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.appreactor.news.R
import co.appreactor.news.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import conf.ConfRepo
import db.databaseFile
import dialog.showErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private val model: SettingsViewModel by viewModel()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Suppress("BlockingMethodInNonBlockingContext")
    private val exportDatabaseLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }

        val databaseFile = requireContext().databaseFile()

        if (!databaseFile.exists()) {
            Toast.makeText(requireContext(), "Database file does not exist", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        lifecycleScope.launchWhenResumed {
            withContext(Dispatchers.IO) {
                runCatching {
                    requireContext().contentResolver.openOutputStream(uri)?.use {
                        databaseFile.inputStream().copyTo(it)
                    }
                }.onFailure { showErrorDialog(it) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val conf = runBlocking { model.loadConf().first() }

        binding.apply {
            syncInBackground.apply {
                isChecked = conf.syncInBackground
                backgroundSyncIntervalButton.isVisible = conf.syncInBackground

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(syncInBackground = isChecked) }
                    model.scheduleBackgroundSync()
                    backgroundSyncIntervalButton.isVisible = isChecked
                }
            }

            backgroundSyncIntervalButton.setOnClickListener {
                val dialog =
                    MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.background_sync_interval))
                        .setView(R.layout.dialog_background_sync_interval).show()

                val setupInterval = fun RadioButton?.(hours: Int) {
                    if (this == null) return

                    text = resources.getQuantityString(R.plurals.d_hours, hours, hours)
                    val millis = TimeUnit.HOURS.toMillis(hours.toLong())
                    isChecked = runBlocking { model.loadConf().first().backgroundSyncIntervalMillis == millis }

                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            model.saveConf { it.copy(backgroundSyncIntervalMillis = millis) }
                            model.scheduleBackgroundSync()
                            backgroundSyncInterval.text = text
                            dialog.dismiss()
                        }
                    }
                }

                setupInterval.apply {
                    invoke(dialog.findViewById(R.id.one_hour), 1)
                    invoke(dialog.findViewById(R.id.three_hours), 3)
                    invoke(dialog.findViewById(R.id.six_hours), 6)
                    invoke(dialog.findViewById(R.id.twelve_hours), 12)
                    invoke(dialog.findViewById(R.id.twenty_four_hours), 24)
                }
            }

            backgroundSyncInterval.text = resources.getQuantityString(
                R.plurals.d_hours,
                TimeUnit.MILLISECONDS.toHours(conf.backgroundSyncIntervalMillis).toInt(),
                TimeUnit.MILLISECONDS.toHours(conf.backgroundSyncIntervalMillis).toInt(),
            )

            syncOnStartup.apply {
                isChecked = conf.syncOnStartup

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(syncOnStartup = isChecked) }
                }
            }

            showOpenedEntries.apply {
                isChecked = conf.showReadEntries

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(showReadEntries = isChecked) }
                }
            }

            showPreviewImages.apply {
                isChecked = conf.showPreviewImages

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(showPreviewImages = isChecked) }
                }
            }

            cropPreviewImages.apply {
                isChecked = conf.cropPreviewImages

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(cropPreviewImages = isChecked) }
                }
            }

            showPreviewText.apply {
                isChecked = conf.showPreviewText

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(showPreviewText = isChecked) }
                }
            }

            markScrolledEntriesAsRead.apply {
                isChecked = conf.markScrolledEntriesAsRead

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(markScrolledEntriesAsRead = isChecked) }
                }
            }

            useBuiltInBrowser.apply {
                isChecked = conf.useBuiltInBrowser

                setOnCheckedChangeListener { _, isChecked ->
                    model.saveConf { it.copy(useBuiltInBrowser = isChecked) }
                }
            }

            manageEnclosures.setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_enclosuresFragment)
            }

            exportDatabase.setOnClickListener {
                exportDatabaseLauncher.launch("news.db")
            }

            logOut.setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    when (model.loadConf().first().backend) {
                        ConfRepo.BACKEND_STANDALONE -> {
                            MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.delete_all_data_warning)
                                .setPositiveButton(
                                    R.string.delete
                                ) { _, _ ->
                                    logOut()
                                }.setNegativeButton(
                                    R.string.cancel, null
                                ).show()
                        }

                        else -> {
                            MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.log_out_warning)
                                .setPositiveButton(
                                    R.string.log_out
                                ) { _, _ ->
                                    logOut()
                                }.setNegativeButton(
                                    R.string.cancel, null
                                ).show()
                        }
                    }
                }
            }

            when (conf.backend) {
                ConfRepo.BACKEND_STANDALONE -> {
                    binding.logOutTitle.setText(R.string.delete_all_data)
                    binding.logOutSubtitle.isVisible = false
                }

                else -> binding.logOutSubtitle.text = model.getAccountName()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun logOut() {
        lifecycleScope.launch {
            model.logOut()

            findNavController().apply {
                while (popBackStack()) {
                    popBackStack()
                }

                navigate(R.id.authFragment)
            }
        }
    }
}