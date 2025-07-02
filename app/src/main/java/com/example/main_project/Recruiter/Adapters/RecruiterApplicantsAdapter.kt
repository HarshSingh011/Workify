package com.example.main_project.adapters

import android.R
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.main_project.CandidateInterface
import com.example.main_project.CandidateProfileRetrofitClient
import com.example.main_project.Recruiter.DataClasses.ApplicantApplication
import com.example.main_project.Recruiter.DataClasses.ApplicantContent
import com.example.main_project.Recruiter.DataClasses.UpdateStatusBody
import com.example.main_project.databinding.ApplicantBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class RecruiterApplicantsAdapter(
    private val applicantList: List<ApplicantApplication>,
    private val context: Context
) : RecyclerView.Adapter<RecruiterApplicantsAdapter.ApplicantViewHolder>() {

    inner class ApplicantViewHolder(val binding: ApplicantBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val binding = ApplicantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        val applicantApplication = applicantList[position]
        val applicant = applicantApplication.applicant

        holder.binding.Skills.text = applicant.skills.joinToString(", ")
        holder.binding.Status.text= applicantApplication.status

        val resumeUrl = applicant.resumeKey
        if (!resumeUrl.isNullOrEmpty()) {
            loadPdfThumbnail(resumeUrl, holder.binding.ResumeImage)

            holder.binding.ResumeImage.setOnClickListener {
                openFile(context, resumeUrl)
            }
        } else {
            holder.binding.ResumeImage.setImageResource(R.drawable.ic_menu_report_image)
        }

        holder.binding.Accept.setOnClickListener {
            val applicantId = applicantApplication.id
            val statusBody = UpdateStatusBody("Accepted")
            updateApplicationStatus(applicantId, statusBody)
        }
    }

    private fun loadProfileImage(imageUrl: String, imageView: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = downloadImage(imageUrl)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    Log.e("RecruiterAdapter", "Error loading profile image: ${e.message}")
                }
            }
        }
    }

    private fun downloadImage(imageUrl: String): Bitmap {
        val url = URL(imageUrl)
        val connection = url.openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun updateApplicationStatus(applicantId: Long, statusBody: UpdateStatusBody) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiClient = CandidateProfileRetrofitClient.instance(context)
                    .create(CandidateInterface::class.java)

                val response = apiClient.updateApplicationStatus(applicantId, statusBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message ?: "Status updated successfully"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update application status", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RecruiterAdapter", "Error updating status: ${e.message}")
                }
            }
        }
    }

    override fun getItemCount(): Int = applicantList.size

    private fun loadPdfThumbnail(pdfUrl: String, imageView: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = downloadFile(pdfUrl, imageView.context)
                val pdfRenderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
                val page = pdfRenderer.openPage(0)

                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                page.close()
                pdfRenderer.close()

                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                    Log.e("RecruiterAdapter", "Error loading PDF thumbnail: ${e.message}")
                }
            }
        }
    }

    private fun downloadFile(fileUrl: String, context: Context): File {
        val file = File(context.cacheDir, "temp_file_${System.currentTimeMillis()}")
        val url = URL(fileUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Server returned HTTP ${connection.responseCode}")
        }

        connection.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun openFile(context: Context, fileUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = downloadFile(fileUrl, context)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, getMimeType(fileUrl))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Open File"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("RecruiterAdapter", "Error opening file: ${e.message}")
                }
            }
        }
    }

    private fun getMimeType(url: String): String {
        return when {
            url.endsWith(".pdf", true) -> "application/pdf"
            url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) -> "image/jpeg"
            url.endsWith(".png", true) -> "image/png"
            else -> "*/*"
        }
    }
}

