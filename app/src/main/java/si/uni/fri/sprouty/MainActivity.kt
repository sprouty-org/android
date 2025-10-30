package si.uni.fri.sprouty

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import si.uni.fri.sprouty.ui.garden.GardenFragment
import si.uni.fri.sprouty.ui.leaderboard.LeaderboardFragment
import si.uni.fri.sprouty.ui.planthub.PlanthubFragment
import si.uni.fri.sprouty.ui.sensors.SensorsFragment
import si.uni.fri.sprouty.ui.shop.ShopFragment
import si.uni.fri.sprouty.util.animations.slideInFromLeft

class MainActivity : AppCompatActivity() {

    private val fragments = mapOf(
        "shop" to ShopFragment(),
        "leaderboard" to LeaderboardFragment(),
        "planthub" to PlanthubFragment(),
        "garden" to GardenFragment(),
        "sensors" to SensorsFragment()
    )

    private var currentFragmentTag: String? = null

    private lateinit var bubble: View

    private lateinit var shopButton: ImageView

    private lateinit var leaderboardButton: ImageView

    private lateinit var planthubButton: ImageView

    private lateinit var gardenButton: ImageView

    private lateinit var sensorsButton: ImageView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            initFragments()
            showFragment("planthub")
        }

        initFragmentObjects()

    }

    private fun initFragments() {
        val transaction = supportFragmentManager.beginTransaction()
        for ((tag, fragment) in fragments) {
            transaction.add(R.id.fragmentContainerView, fragment, tag).hide(fragment)
        }
        transaction.commit()
    }

    private fun initFragmentObjects() {
        bubble = findViewById(R.id.bubble)

        shopButton = findViewById(R.id.shop_nav_button)
        leaderboardButton = findViewById(R.id.leaderboard_nav_button)
        planthubButton = findViewById(R.id.planthub_nav_button)
        gardenButton = findViewById(R.id.garden_nav_button)
        sensorsButton = findViewById(R.id.sensors_nav_button)

        shopButton.setOnClickListener {
            moveBubbleToButton(shopButton)
            showFragment("shop")
        }
        leaderboardButton.setOnClickListener {
            moveBubbleToButton(leaderboardButton)
            showFragment("leaderboard")
        }
        planthubButton.setOnClickListener {
            moveBubbleToButton(planthubButton)
            showFragment("planthub")
        }
        gardenButton.setOnClickListener {
            moveBubbleToButton(gardenButton)
            showFragment("garden")
        }
        sensorsButton.setOnClickListener {
            moveBubbleToButton(sensorsButton)
            showFragment("sensors")
        }

        moveBubbleToButton(planthubButton)
    }

    private fun showFragment(tag: String) {
        val header: View = findViewById(R.id.header)
        header.slideInFromLeft()
        //set the text in the header to the fragment name
        val headerTitle: TextView = header.findViewById(R.id.headerTitle)
        headerTitle.text = when (tag) {
            "shop" -> "Shop"
            "leaderboard" -> "Leaderboard"
            "planthub" -> "Plant Hub"
            "garden" -> "Garden"
            "sensors" -> "Sensors"
            else -> ""
        }
        val transaction = supportFragmentManager.beginTransaction()
        currentFragmentTag?.let { tagToHide ->
            fragments[tagToHide]?.let { transaction.hide(it) }
        }
        fragments[tag]?.let { transaction.show(it) }
        transaction.commit()
        currentFragmentTag = tag
    }

    private fun moveBubbleToButton(button: ImageView) {
        val buttonLocation = IntArray(2)
        button.getLocationInWindow(buttonLocation)

        val bubbleParams = bubble.layoutParams as ConstraintLayout.LayoutParams
        bubbleParams.topToTop = button.id
        bubbleParams.bottomToBottom = button.id
        bubbleParams.startToStart = button.id
        bubbleParams.endToEnd = button.id
        bubbleParams.height = 180

        bubble.layoutParams = bubbleParams
    }
}