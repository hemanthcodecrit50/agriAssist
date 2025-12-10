package com.krishisakhi.farmassistant

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.krishisakhi.farmassistant.personalization.FarmerFileManager
import com.krishisakhi.farmassistant.personalization.PersonalizationEmbeddingManager
import kotlinx.coroutines.launch

/**
 * Activity for managing farmer_profile.txt - a free-form text file
 * Users can type anything they want about themselves
 */
class FarmerProfileEditorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FarmerProfileEditor"
    }

    private lateinit var profileEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var viewInsightsButton: Button

    private lateinit var fileManager: FarmerFileManager
    private lateinit var embeddingManager: PersonalizationEmbeddingManager

    private var currentFarmerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farmer_profile_editor)

        // Initialize managers
        fileManager = FarmerFileManager(this)
        embeddingManager = PersonalizationEmbeddingManager(this)

        // Get current farmer ID
        currentFarmerId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentFarmerId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        profileEditText = findViewById(R.id.profileEditText)
        saveButton = findViewById(R.id.saveButton)
        viewInsightsButton = findViewById(R.id.viewInsightsButton)

        // Load existing profile
        loadProfile()

        // Set up listeners
        saveButton.setOnClickListener {
            saveProfile()
        }

        viewInsightsButton.setOnClickListener {
            viewInsights()
        }
    }

    /**
     * Load existing profile content
     */
    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val farmerId = currentFarmerId ?: return@launch
                val content = fileManager.readProfile(farmerId)
                profileEditText.setText(content)

                if (content.isEmpty()) {
                    profileEditText.hint = "Tell us about yourself...\n\n" +
                            "Examples:\n" +
                            "- Crops you grow\n" +
                            "- Size of your land\n" +
                            "- Location/village\n" +
                            "- Soil type\n" +
                            "- Farming challenges\n" +
                            "- Any other information"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile", e)
                Toast.makeText(this@FarmerProfileEditorActivity, "Error loading profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Save profile content and re-embed
     */
    private fun saveProfile() {
        lifecycleScope.launch {
            try {
                val farmerId = currentFarmerId ?: return@launch
                val content = profileEditText.text.toString()

                // Disable button while saving
                saveButton.isEnabled = false
                saveButton.text = "Saving..."

                // Save to file
                val saved = fileManager.writeProfile(farmerId, content)

                if (!saved) {
                    Toast.makeText(this@FarmerProfileEditorActivity, "Failed to save profile", Toast.LENGTH_SHORT).show()
                    saveButton.isEnabled = true
                    saveButton.text = "Save Profile"
                    return@launch
                }

                // Re-embed both files (profile + insights)
                val indexed = embeddingManager.indexPersonalizationFiles(farmerId)

                if (indexed) {
                    Toast.makeText(this@FarmerProfileEditorActivity, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FarmerProfileEditorActivity, "Profile saved but indexing failed", Toast.LENGTH_SHORT).show()
                }

                // Re-enable button
                saveButton.isEnabled = true
                saveButton.text = "Save Profile"

            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
                Toast.makeText(this@FarmerProfileEditorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = true
                saveButton.text = "Save Profile"
            }
        }
    }

    /**
     * View AI-generated insights (read-only)
     */
    private fun viewInsights() {
        lifecycleScope.launch {
            try {
                val farmerId = currentFarmerId ?: return@launch
                val insights = fileManager.readInsights(farmerId)

                if (insights.isEmpty()) {
                    Toast.makeText(
                        this@FarmerProfileEditorActivity,
                        "No insights yet. Keep asking questions!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Show insights in a dialog or new activity
                    showInsightsDialog(insights)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error viewing insights", e)
                Toast.makeText(this@FarmerProfileEditorActivity, "Error loading insights", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show insights in a simple dialog
     */
    private fun showInsightsDialog(insights: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("AI-Generated Insights")
            .setMessage(insights)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

