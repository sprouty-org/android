package si.uni.fri.sprouty.ui.leaderboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }
}