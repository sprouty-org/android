package si.uni.fri.sprouty.ui.planthub

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.databinding.FragmentPlanthubBinding
import kotlin.math.abs

class PlanthubFragment : Fragment() {
    private var _binding: FragmentPlanthubBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantImageButton: Button
    private lateinit var plantLifecycleButton: Button
    private lateinit var plantImage: ShapeableImageView

    private var startX = 0f
    private var startY = 0f
    private var dragging = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_planthub, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        plantImageButton = view.findViewById(R.id.button_image)
        plantLifecycleButton = view.findViewById(R.id.button_lifecycle)

        plantImage = view.findViewById(R.id.plant_image)
        plantImage.setImageResource(R.drawable.aspect_plant)

        plantImageButton.setOnClickListener {
            changePlantDisplay(false)
        }
        plantLifecycleButton.setOnClickListener {
            changePlantDisplay(true)
        }


        var movingHorizontally = false
        var movingVertically = false
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 100

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val diffX = e2.x - (e1?.x ?: 0f)
                    val diffY = e2.y - (e1?.y ?: 0f)

                    if (abs(diffX) > abs(diffY)) {
                        showNextImage() // horizontal swipe
                        return true
                    } else {
                        if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            showPlantInfo() // vertical swipe
                            return true
                        }
                    }
                    return false
                }
            })
        plantImage.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    dragging = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dragging) {
                        val deltaX = event.rawX - startX
                        val deltaY = event.rawY - startY

                        if (abs(deltaX) > abs(deltaY) && !movingVertically) {
                            movingHorizontally = true
                            // Horizontal drag → move and rotate image
                            plantImage.translationX = deltaX
                            plantImage.rotation = deltaX / 60f
                        }else if(abs(deltaY) > abs(deltaX) && !movingHorizontally){
                            // Vertical drag → move image
                            movingVertically = true
                            plantImage.translationY = deltaY
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false

                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY
                    val HORIZONTAL_THRESHOLD = 350
                    val VERTICAL_THRESHOLD = 300

                    when {
                        abs(deltaX) > abs(deltaY) && abs(deltaX) > HORIZONTAL_THRESHOLD && movingHorizontally -> {
                            showNextImage()
                        }
                        abs(deltaY) > abs(deltaX) && deltaY < -VERTICAL_THRESHOLD && movingVertically-> {
                            showPlantInfo() // swipe down
                        }
                        else -> {
                            // Not a significant swipe, reset position
                        }
                    }

                    // Animate back to center
                    movingHorizontally = false
                    movingVertically = false
                    plantImage.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .rotation(0f)
                        .setDuration(200)
                        .start()
                }
            }
            true
        }
    }

    private fun showNextImage() {
        //get new image from resources or server
        Toast.makeText(requireContext(), "Show next", Toast.LENGTH_SHORT).show()
    }

    private fun showPlantInfo() {
        // Here you can open a bottom sheet, dialog, or navigate to a new fragment
        Toast.makeText(requireContext(), "Show info", Toast.LENGTH_SHORT).show()
    }

    fun changePlantDisplay(toLifecycle: Boolean) {
        val sproutyGreen = ContextCompat.getColor(requireContext(), R.color.sprouty_green)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)

        val lifecycleBg = plantLifecycleButton.background  //drawable of the button
        val imageBg = plantImageButton.background

        if (toLifecycle) {
            // Load lifecycle GIF
            Glide.with(this)
                .asGif()
                .load(R.drawable.plant_lifecycle)
                .into(plantImage)

            //change button colors
            if(lifecycleBg is GradientDrawable && imageBg is GradientDrawable) {
                lifecycleBg.setColor(sproutyGreen)
                imageBg.setColor(whiteColor)
                plantLifecycleButton.setTextColor(whiteColor)
                plantImageButton.setTextColor(sproutyGreen)
            }
        } else {
            // Load plant image
            plantImage.setImageResource(R.drawable.aspect_plant)

            //change button colors
            if(lifecycleBg is GradientDrawable && imageBg is GradientDrawable) {
                lifecycleBg.setColor(whiteColor)
                imageBg.setColor(sproutyGreen)
                plantLifecycleButton.setTextColor(sproutyGreen)
                plantImageButton.setTextColor(whiteColor)
            }
        }
    }
}