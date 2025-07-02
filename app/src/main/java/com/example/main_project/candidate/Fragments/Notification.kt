package com.example.main_project.candidate.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.main_project.CandidateInterface
import com.example.main_project.CandidateProfileRetrofitClient
import com.example.main_project.R
import com.example.main_project.databinding.FragmentNotificationBinding
import kotlinx.coroutines.launch

class Notification : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)

        getNotifications()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.searchJob)
            }
        })

        println("Notification is being loaded successfully")

        return binding.root
    }

    private fun getNotifications() {
        if (_binding == null) return

        val retrofit = CandidateProfileRetrofitClient.instance(requireContext())
        val apiService = retrofit.create(CandidateInterface::class.java)

        lifecycleScope.launch {
            try {
                val response = apiService.getNotifications()
                if (response.isSuccessful && response.body() != null) {
                    val notifications = response.body()!!
                    if (notifications.isNotEmpty()) {
                        val notificationText = notifications.joinToString("\n\n") { notification ->
                            "${notification.title}\n${notification.message}"
                        }
                        if (_binding != null) {
                            binding.notify.text = notificationText
                            binding.notify.visibility = View.VISIBLE
                            binding.notificationimages.visibility = View.GONE
                            binding.notificationtext.visibility = View.GONE
                            binding.notificationtext2.visibility = View.GONE
                        }
                    } else {
                        showNoNotificationsUI()
                    }
                } else {
                    showNoNotificationsUI()
                }
            } catch (e: Exception) {
                showNoNotificationsUI()
            }
        }
    }

    private fun showNoNotificationsUI() {
        if (_binding != null) {
            binding.notify.visibility = View.GONE
            binding.notificationimages.visibility = View.VISIBLE
            binding.notificationtext.visibility = View.VISIBLE
            binding.notificationtext2.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
