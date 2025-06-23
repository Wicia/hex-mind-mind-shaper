package pl.hexmind.fastnote.features.details

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import pl.hexmind.fastnote.R

class DetailsActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var editEssence: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    private var thoughtId: Long = -1
    private var isEditMode: Boolean = false
    private var isNewThought: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_layout)

        initializeViews()
        setupClickListeners()
        handleIntent()
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.titleText)
        editEssence = findViewById(R.id.editEssence)
        btnSave = findViewById(R.id.btnSave)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveThought()
        }

        btnEdit.setOnClickListener {
            toggleEditMode()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun handleIntent() {
        thoughtId = intent.getLongExtra("THOUGHT_ID", -1)

        if (thoughtId != -1L) {
            // Istniejąca myśl
            isNewThought = false
            loadThought()
            setViewMode()
            updateTitle("Edytuj myśl")
        } else {
            // Nowa myśl
            isNewThought = true
            setEditMode()
            updateTitle("Dodaj nową myśl")
        }
    }

    private fun updateTitle(newTitle: String) {
        titleText.text = newTitle
    }

    private fun loadThought() {
        // Tu będzie kod do wczytania myśli z bazy danych
        // Na razie symulacja danych
        editEssence.setText("Przykładowa esencja myśli...")
    }

    private fun saveThought() {
        val essence = editEssence.text.toString().trim()

        if (essence.isEmpty()) {
            showToast("Esencja nie może być pusta")
            editEssence.requestFocus()
            return
        }

        if (essence.length < 5) {
            showToast("Esencja musi mieć przynajmniej 5 znaków")
            editEssence.requestFocus()
            return
        }

        // Tu będzie kod do zapisania w bazie danych
        if (isNewThought) {
            // Zapisz nową myśl
            showToast("Myśl została zapisana")
            finish() // Wróć do listy
        } else {
            // Zaktualizuj istniejącą
            showToast("Myśl została zaktualizowana")
            setViewMode()
        }
    }

    private fun toggleEditMode() {
        if (isEditMode) {
            // Anuluj zmiany - przywróć oryginalny tekst
            if (!isNewThought) {
                loadThought() // Przywróć oryginalne dane
            }
            setViewMode()
        } else {
            setEditMode()
        }
    }

    private fun setEditMode() {
        isEditMode = true
        editEssence.isEnabled = true
        editEssence.requestFocus()

        // Pokaż kursor na końcu tekstu
        editEssence.setSelection(editEssence.text?.length ?: 0)

        btnSave.visibility = Button.VISIBLE
        btnEdit.text = "Anuluj"
        btnDelete.visibility = Button.GONE

        updateTitle(if (isNewThought) "Dodaj nową myśl" else "Edytuj myśl")
    }

    private fun setViewMode() {
        isEditMode = false
        editEssence.isEnabled = false
        editEssence.clearFocus()

        btnSave.visibility = Button.GONE
        btnEdit.text = "Edytuj"
        btnDelete.visibility = if (isNewThought) Button.GONE else Button.VISIBLE

        updateTitle("Podgląd myśli")
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usuń myśl")
            .setMessage("Czy na pewno chcesz usunąć tę myśl? Tej operacji nie można cofnąć.")
            .setPositiveButton("Usuń") { _, _ ->
                deleteThought()
            }
            .setNegativeButton("Anuluj", null)
            .setCancelable(true)
            .show()
    }

    private fun deleteThought() {
        // Tu będzie kod do usunięcia z bazy danych
        showToast("Myśl została usunięta")
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (isEditMode && !isNewThought) {
            // Jeśli jesteśmy w trybie edycji, najpierw anuluj
            toggleEditMode()
        } else {
            super.onBackPressed()
        }
    }
}