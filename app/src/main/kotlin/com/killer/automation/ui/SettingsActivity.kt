package com.killer.automation.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.killer.automation.databinding.ActivitySettingsBinding
import com.killer.automation.model.Offer
import com.killer.automation.engine.DecisionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding

    @Inject
    lateinit var decisionEngine: DecisionEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(com.killer.automation.R.string.settings_title)

        // Setup simple zones spinner (for MVP). These can be replaced by dynamic values later.
        val zones = listOf("Centro", "Aeropuerto", "Suburbios")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, zones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerZones.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { prefs ->
                    binding.switchBotEnabled.isChecked = prefs.isEnabled
                    // avoid resetting text if user is editing
                    if (!binding.etMinPrice.isFocused) {
                        binding.etMinPrice.setText(prefs.minPrice.toString())
                    }
                    binding.tvSubscriptionStatus.text = if (prefs.isSubscriptionValid) {
                        getString(com.killer.automation.R.string.subscription_active)
                    } else {
                        getString(com.killer.automation.R.string.subscription_inactive)
                    }

                    // set spinner selection if present
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
            } else false
        }

        binding.spinnerZones.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val zone = zones[position]
                viewModel.updateSelectedZone(zone)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.btnTestOffer.setOnClickListener {
            // Create a dummy offer and evaluate
            val dummy = Offer(priceRaw = binding.etMinPrice.text?.toString() ?: "${0}", distanceRaw = "2 km")
            try {
                val result = decisionEngine.evaluateOffer(dummy)
                // Try to read shouldAccept and reason fields defensively
                val accepted = try {
                    val field = result::class.java.getDeclaredField("shouldAccept")
                    field.isAccessible = true
                    field.getBoolean(result)
                } catch (t: Throwable) {
                    // fallback: try method
                    try {
                        val method = result::class.java.getMethod("isShouldAccept")
                        method.invoke(result) as? Boolean ?: false
                    } catch (e: Throwable) {
                        false
                    }
                }

                val reason = try {
                    val field = result::class.java.getDeclaredField("reason")
                    field.isAccessible = true
                    field.get(result)?.toString()
                } catch (t: Throwable) {
                    try {
                        val method = result::class.java.getMethod("getReason")
                        method.invoke(result)?.toString()
                    } catch (e: Throwable) {
                        null
                    }
                }

                val message = if (accepted) {
                    getString(com.killer.automation.R.string.offer_accepted) + (reason?.let { ": $it" } ?: "")
                } else {
                    getString(com.killer.automation.R.string.offer_rejected) + (reason?.let { ": $it" } ?: "")
                }

                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

            } catch (t: Throwable) {
                Snackbar.make(binding.root, getString(com.killer.automation.R.string.test_offer_error), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
