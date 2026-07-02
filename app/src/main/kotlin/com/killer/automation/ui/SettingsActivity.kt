package com.killer.automation.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.killer.automation.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observar cambios
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { prefs ->
                    binding.switchBotEnabled.isChecked = prefs.isEnabled
                    binding.etMinPrice.setText(prefs.minPrice.toString())
                }
            }
        }

        binding.switchBotEnabled.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateBotEnabled(isChecked)
        }

        binding.etMinPrice.setOnEditorActionListener { v, _, _ ->
            v.text.toString().toDoubleOrNull()?.let { viewModel.updateMinPrice(it) }
            true
        }

        binding.btnTestOffer.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val result = viewModel.simulateOffer()
                    val msg = if (result.shouldAccept) "Aceptada! Score: ${'$'}{result.matchScore}" else "Rechazada: ${'$'}{result.rejectionReason}"
                    Toast.makeText(this@SettingsActivity, msg, Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Toast.makeText(this@SettingsActivity, "Error al simular oferta", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
