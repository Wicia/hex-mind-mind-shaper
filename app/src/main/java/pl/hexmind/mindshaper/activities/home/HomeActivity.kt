package pl.hexmind.mindshaper.activities.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsActivity
import pl.hexmind.mindshaper.activities.onboarding.OnboardingActivity
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import pl.hexmind.mindshaper.activities.workshop.WorkshopActivity
import pl.hexmind.mindshaper.common.formatting.setColoredText
import pl.hexmind.mindshaper.common.onboarding.OnboardingSection
import pl.hexmind.mindshaper.common.ui.dialogs.WelcomeDialog
import pl.hexmind.mindshaper.common.ui.views.HexSpotlight
import pl.hexmind.mindshaper.services.AppNewsService
import pl.hexmind.mindshaper.services.GreetingsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.DefaultCaptureForm
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.dto.StartScreen
import javax.inject.Inject

/**
 * Main activity handling FAB menu and swipe gestures for  access
 */
@AndroidEntryPoint
class HomeActivity : CoreActivity() {

    @Inject
    lateinit var thoughtsService: ThoughtsService

    @Inject
    lateinit var appNewsService: AppNewsService

    private lateinit var fabNewThought: FloatingActionButton

    private lateinit var captureRing: CaptureRing

    private lateinit var tvHeaderGreetings : TextView

    private lateinit var labBubbles: LabBubblesAnimator

    private lateinit var newsBadgeSignal: NewsBadgeSignal

    private var allThoughts: List<ThoughtDTO> = emptyList()

    // ! Own flag - inside onResume the lifecycle state is not RESUMED yet
    private var isScreenResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start-screen routing: Home is the launcher, so on a cold launch it hands over to the
        // user-chosen entry screen before drawing anything (Home stays below it -> BACK returns here)
        if (openChosenStartScreenIfNeeded(savedInstanceState)) {
            return
        }

        setContentView(R.layout.home_activity)

        initViews()
        setupClickListeners()
        setupNewsBadge()

        if (onboardingManager.isWelcomePending()) {
            showWelcomeDialog()
        }
    }

    private fun showWelcomeDialog() {
        WelcomeDialog(
            context = this,
            onYourNameSaved = { yourName ->
                appSettingsStorage.setYourName(yourName)
                setupHeaderWithGreetings()
            },
            onOnboardingChosen = { enabled ->
                appSettingsStorage.setOnboardingEnabled(enabled)
                syncOnboardingEntry()
                if (enabled) showOnboardingSpotlight()
            },
            onClosed = {
                onboardingManager.markWelcomeShown()
            }
        ).show()
    }

    // Points at the flask once, right after the tips were chosen in the welcome
    private fun showOnboardingSpotlight() {
        HexSpotlight.show(
            activity = this,
            target   = findViewById(R.id.iv_onboarding),
            text       = getString(R.string.home_onboarding_spotlight),
            buttonText = getString(R.string.common_spotlight_btn_got_it)
        ) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    // ! Guarded: a rerouted cold launch finishes in onCreate before the views exist
    override fun onResume() {
        super.onResume()
        isScreenResumed = true
        if (::labBubbles.isInitialized) syncOnboardingEntry()
        if (::newsBadgeSignal.isInitialized) syncNewsBadge()
    }

    override fun onPause() {
        super.onPause()
        isScreenResumed = false
        if (::labBubbles.isInitialized) labBubbles.stop()
        if (::newsBadgeSignal.isInitialized) newsBadgeSignal.stop()
        // Leaving Home (e.g. nav bar) with the ring open = closed on return
        if (::captureRing.isInitialized) captureRing.close(animate = false)
    }

    private fun initViews() {
        setupHeader(R.drawable.ic_activity_home, R.string.common_foobar)
        fabNewThought = findViewById(R.id.fab_new_thought)
        labBubbles = LabBubblesAnimator(
            container = findViewById(R.id.fl_onboarding_entry),
            flaskIcon = findViewById(R.id.iv_onboarding)
        )
        newsBadgeSignal = NewsBadgeSignal(findViewById(R.id.iv_home_news_signal))
        setupHeaderWithGreetings()
    }

    // The flask is always there - tips can be turned on inside; bubbles only while the tips are on
    private fun syncOnboardingEntry() {
        val isOnboardingEnabled = appSettingsStorage.isOnboardingEnabled()

        if (isOnboardingEnabled) {
            labBubbles.start()
        }
        else {
            labBubbles.stop()
        }
    }

    // Counted again on every change of thoughts, on every return to Home (e.g. from an opened entry)
    // and when an entry is marked as read / unread in the news bottom sheet
    private fun setupNewsBadge() {
        thoughtsService.getAllThoughts().observe(this) { thoughts ->
            allThoughts = thoughts
            syncNewsBadge()
        }
        supportFragmentManager.setFragmentResultListener(HomeNewsBottomSheet.RESULT_NEWS_CHANGED, this) { _, _ ->
            syncNewsBadge()
        }
    }

    // Badge always shown, also with 0; the wave only with unread news
    private fun syncNewsBadge() {
        val unreadCount = appNewsService.countUnread(allThoughts)

        findViewById<TextView>(R.id.tv_home_news_badge).text =
            if (unreadCount > NEWS_BADGE_MAX) getString(R.string.home_news_badge_overflow) else unreadCount.toString()

        // ! Thoughts may change while the screen is paused - the wave starts only on a resumed screen
        if (unreadCount > 0 && isScreenResumed) newsBadgeSignal.start() else newsBadgeSignal.stop()
    }

    private fun setupHeaderWithGreetings(){
        tvHeaderGreetings = findViewById(R.id.tv_header_title)
        val currentGreetingsText = tvHeaderGreetings.text.toString()
        var newGreetingsText : String
        do {
            newGreetingsText = GreetingsService.getGreetingsString(this, appSettingsStorage.getYourName())
        } while (currentGreetingsText == newGreetingsText)

        tvHeaderGreetings.setColoredText(newGreetingsText, appSettingsStorage.getYourName(),
            ContextCompat.getColor(this, R.color._palette_lvl_3))
    }

    private fun setupClickListeners() {
        setupCaptureRing()

        findViewById<ImageView>(R.id.iv_onboarding).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        findViewById<View>(R.id.ll_home_news_entry).setOnClickListener {
            HomeNewsBottomSheet.show(supportFragmentManager)
        }
    }

    // Same targets as the launcher shortcuts (AppShortcuts): a capture form or new loose flashcards
    // Feature off = gray option, its tap leads to Preferences (as the add-form buttons in Details)
    private fun setupCaptureRing() {
        captureRing = CaptureRing(this, fabNewThought)
        captureRing.addOption(findViewById(R.id.btn_capture_note)) { openCapture(DefaultCaptureForm.TEXT) }
        captureRing.addOption(
            view          = findViewById(R.id.btn_capture_photo),
            isEnabled     = { appSettingsStorage.isPhotoFeatureEnabled() },
            onDisabledTap = { showEnablePhotoFormDialog() }
        ) { openCapture(DefaultCaptureForm.PHOTO) }
        captureRing.addOption(
            view          = findViewById(R.id.btn_capture_voice),
            isEnabled     = { appSettingsStorage.isVoiceRecordingEnabled() },
            onDisabledTap = { showEnableVoiceFormDialog() }
        ) { openCapture(DefaultCaptureForm.VOICE) }
        captureRing.addOption(
            view          = findViewById(R.id.btn_capture_flashcards),
            isEnabled     = { appSettingsStorage.isFlashcardsFeatureEnabled() },
            onDisabledTap = {
                showEnableAdditionalFeaturesDialog(
                    titleRes       = R.string.home_capture_flashcards_inactive_header,
                    descriptionRes = R.string.home_capture_flashcards_inactive_info,
                    section        = SettingsActivity.Section.MEMORIZING
                )
            }
        ) {
            startActivity(Intent(this, FlashcardsActivity::class.java)
                .putExtra(FlashcardsActivity.EXTRA_NEW_LOOSE_FLASHCARDS, true))
        }
    }

    private fun openCapture(captureForm: DefaultCaptureForm) {
        startActivity(Intent(this, CaptureActivity::class.java)
            .putExtra(CaptureActivity.EXTRA_CAPTURE_FORM, captureForm.name))
    }

    /**
     * Opens the user-chosen start screen on a genuine cold launch from the launcher icon.
     * This launcher instance finishes before its first frame (no Home flash); the stack becomes
     * Home -> chosen screen, and that Home is created only when BACK reaches it.
     * @return true when rerouted (this instance is finishing)
     */
    private fun openChosenStartScreenIfNeeded(savedInstanceState: Bundle?): Boolean {
        // Only a real cold launch reroutes: nav-bar navigation to Home carries no MAIN/LAUNCHER intent,
        // and a config-change recreation (e.g. rotation) has a non-null savedInstanceState
        val isColdLaunchFromLauncher = savedInstanceState == null
                && isTaskRoot
                && intent.action == Intent.ACTION_MAIN
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)

        if (!isColdLaunchFromLauncher) {
            return false
        }

        val targetScreen = when (appSettingsStorage.getStartScreen()) {
            StartScreen.STREAM     -> StreamActivity::class.java
            // Feature off = its screen is hidden, Home stays
            StartScreen.FLASHCARDS -> if (appSettingsStorage.isFlashcardsFeatureEnabled()) FlashcardsActivity::class.java else return false
            StartScreen.WORKSHOP   -> if (appSettingsStorage.isWorkshopFeatureEnabled())   WorkshopActivity::class.java   else return false
            StartScreen.HOME       -> return false
        }

        // Plain Home intent (no MAIN/LAUNCHER) -> the Home below does not reroute again
        startActivities(arrayOf(
            Intent(this, HomeActivity::class.java),
            Intent(this, targetScreen)
        ))
        finish()
        return true
    }

    private companion object {
        const val NEWS_BADGE_MAX = 9
    }
}
