package pl.hexmind.mindshaper.common

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import pl.hexmind.mindshaper.R

class HexKeyboard : InputMethodService() {

    private var isShiftOn = false
    private var keyboardView: View? = null

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.hex_keyboard_layout, null)

        setupSpecialCharacters(keyboardView!!)
        setupLetterKeys(keyboardView!!)
        setupFunctionKeys(keyboardView!!)

        updateAllKeys(keyboardView!!)

        return keyboardView!!
    }

    // Upper bar with hex symbols: * # @ & ? !
    private fun setupSpecialCharacters(view: View) {
        view.findViewById<Button>(R.id.btn_star).setOnClickListener {
            commitText("*")
        }
        view.findViewById<Button>(R.id.btn_hash).setOnClickListener {
            commitText("#")
        }
        view.findViewById<Button>(R.id.btn_at).setOnClickListener {
            commitText("@")
        }
        view.findViewById<Button>(R.id.btn_ampersand).setOnClickListener {
            commitText("&")
        }
        view.findViewById<Button>(R.id.btn_quotation_mark).setOnClickListener {
            commitText("?")
        }
        view.findViewById<Button>(R.id.btn_exclamation_mark).setOnClickListener {
            commitText("!")
        }
    }

    private fun setupLetterKeys(view: View) {
        val letterKeys = mapOf(
            R.id.key_q to "q", R.id.key_w to "w", R.id.key_e to "e",
            R.id.key_r to "r", R.id.key_t to "t", R.id.key_y to "y",
            R.id.key_u to "u", R.id.key_i to "i", R.id.key_o to "o",
            R.id.key_p to "p", R.id.key_a to "a", R.id.key_s to "s",
            R.id.key_d to "d", R.id.key_f to "f", R.id.key_g to "g",
            R.id.key_h to "h", R.id.key_j to "j", R.id.key_k to "k",
            R.id.key_l to "l", R.id.key_z to "z", R.id.key_x to "x",
            R.id.key_c to "c", R.id.key_v to "v", R.id.key_b to "b",
            R.id.key_n to "n", R.id.key_m to "m"
        )

        letterKeys.forEach { (id, letter) ->
            view.findViewById<Button>(id).setOnClickListener { button ->

                val text = if (isShiftOn) letter.uppercase() else letter
                commitText(text)

                if (isShiftOn) {
                    isShiftOn = false
                    updateAllKeys(view)
                }
            }
        }
    }

    private fun setupFunctionKeys(view: View) {
        view.findViewById<Button>(R.id.key_shift).setOnClickListener {
            isShiftOn = !isShiftOn
            updateAllKeys(view)
        }

        view.findViewById<Button>(R.id.key_backspace).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        view.findViewById<Button>(R.id.key_space).setOnClickListener {
            commitText(" ")
        }

        view.findViewById<Button>(R.id.key_comma).setOnClickListener {
            commitText(",")
        }

        view.findViewById<Button>(R.id.key_period).setOnClickListener {
            commitText(".")
        }

        view.findViewById<Button>(R.id.key_enter).setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
        }

        // 123...
        view.findViewById<Button>(R.id.key_numbers).setOnClickListener {
            // TODO: Przełączanie na layout z cyframi
        }
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    /**
     * updates all keyboard buttons + shift
     */
    private fun updateAllKeys(view: View) {
        val letters = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m")

        val ids = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        // ! Update all buttons
        for (i in letters.indices) {
            val button = view.findViewById<Button>(ids[i])
            if (button != null) {
                button.text = if (isShiftOn) {
                    letters[i].uppercase()
                } else {
                    letters[i].lowercase()
                }
                button.invalidate()
                button.requestLayout()
            }
        }

        // Shift look change on pressed/not
        val shiftButton = view.findViewById<Button>(R.id.key_shift)
        if (isShiftOn) {
            shiftButton.alpha = 1.0f
            shiftButton.setBackgroundColor(android.graphics.Color.parseColor("#ff9966"))
        } else {
            shiftButton.alpha = 0.6f
            shiftButton.setBackgroundResource(R.drawable.hex_keyboard_function_key_background)
        }
    }
}