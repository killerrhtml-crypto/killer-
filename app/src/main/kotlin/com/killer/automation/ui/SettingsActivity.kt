package com.killer.automation.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.killer.automation.databinding.ActivitySettingsBinding
import com.killer.automation.model.OfferEvaluationResult
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

        title = getString(com.killer.automation.R.string.settings_title)

        // Setup simple zones spinner (MVP)
        val zones = listOf("Centro", "Aeropuerto", "Suburbios")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, zones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerZones.adapter = adapter

        // Observe ViewModel state safely
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { prefs ->
                    binding.switchBotEnabled.isChecked = prefs.isEnabled
                    if (!binding.etMinPrice.isFocused) {
                        binding.etMinPrice.setText(prefs.minPrice.toString())
                    }
                    binding.tvSubscriptionStatus.text = if (prefs.isSubscriptionValid) {
                        getString(com.killer.automation.R.string.subscription_active)
                    } else {
                        getString(com.killer.automation.R.string.subscription_inactive)
                    }

                    val idx = zones.indexOf(prefs.selectedZone)
                    if (idx >= 0) binding.spinnerZones.setSelection(idx)
                }
            }
        }

        binding.switchBotEnabled.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateBotEnabled(isChecked)
        }

        binding.etMinPrice.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = v.text?.toString().orEmpty()
                val price = text.toDoubleOrNull()
                if (price != null) {
                    viewModel.updateMinPrice(price)
                } else {
                    Snackbar.make(binding.root, getString(com.killer.automation.R.string.invalid_price), Snackbar.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        binding.spinnerZones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val zone = zones[position]
                viewModel.updateSelectedZone(zone)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnTestOffer.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val result: OfferEvaluationResult = viewModel.simulateOffer()
                    val message = buildResultMessage(result)
                    Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Toast.makeText(this@SettingsActivity, getString(com.killer.automation.R.string.test_offer_error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun buildResultMessage(result: OfferEvaluationResult): String {
        // Proteger por si los campos no existen exactamente con esos nombres
        return try {
            val accepted = result.shouldAccept
            val scorePart = try { " Score: ${'$'}{result.matchScore}" } catch (_: Throwable) { "" }
            val reasonPart = try { result.rejectionReason?.let { ": $it" } ?: "" } catch (_: Throwable) { "" }
            if (accepted) {
                "${getString(com.killer.automation.R.string.offer_accepted)}$scorePart"
            } else {
                "${getString(com.killer.automation.R.string.offer_rejected)}$reasonPart"
            }
        } catch (t: Throwable) {
            // Fallback genérico
            getString(com.killer.automation.R.string.test_offer_error)
        }
    }
}
