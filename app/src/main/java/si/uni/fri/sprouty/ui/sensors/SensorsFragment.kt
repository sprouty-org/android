package si.uni.fri.sprouty.ui.sensors

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.databinding.FragmentSensorsBinding

class SensorsFragment : Fragment() {
    private var _binding: FragmentSensorsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sensors, container, false)
    }
}