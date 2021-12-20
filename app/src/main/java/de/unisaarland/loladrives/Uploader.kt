package de.unisaarland.loladrives

import android.content.Context
import androidx.multidex.BuildConfig
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class Uploader(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val directory = File(inputData.getString("directory")!!)
        val privacyPolicyVersion = inputData.getInt("privacyPolicyVersion", 0)
        val sharedPref = applicationContext.getSharedPreferences("uploaded", Context.MODE_PRIVATE)

        val alreadyUploaded = mutableSetOf<String>()
        alreadyUploaded.addAll(sharedPref.getStringSet("uploaded_trips", mutableSetOf())!!)
        if (privacyPolicyVersion <= 0) {
            println("Cancel upload worker because privacy policy is not accepted.")
            return Result.success()
        }

        val successfullyUploadedFiles = mutableSetOf<File>()
        try {
            /**
             * try to upload all files of the directoy
             */
            directory.listFiles()?.forEach { subDirectory ->
                if (subDirectory.isDirectory) {
                    subDirectory.listFiles()?.forEach {
                        if (!alreadyUploaded.contains(it.name) && it.name.contains("ppcdf")) {
                            val authToken = uploadFile(it, privacyPolicyVersion)
                            if (authToken != null) {
                                successfullyUploadedFiles.add(it)
                                // Remember the Authentication Token
                                val metaData = FileTokenPair(it.name, authToken)
                                val metaFile = File(subDirectory, "meta.json")
                                metaFile.appendText(
                                    GsonBuilder().setPrettyPrinting().create().toJson(metaData)
                                )

                                println("File $it uploaded successfully!")
                            } else {
                                println("File $it could not be uploaded!")
                            }
                        }
                    }
                }
            }
        } finally {
            // remember the successfully uploaded files
            for (file in successfullyUploadedFiles) {
                alreadyUploaded.add(file.name)
            }
            sharedPref.edit().putStringSet("uploaded_trips", alreadyUploaded).apply()

            return Result.success()
        }
    }

    private fun uploadFile(file: File, privacyPolicyVersion: Int): String? {
        var success: String? = null

        val appVersion = BuildConfig.VERSION_NAME

        if (file.length() == 0.toLong()) {
            return null
        }
        var connection: HttpsURLConnection? = null
        try {
            val filename = file.name.replace(".ppcdf", "")

            val debugToken = de.unisaarland.loladrives.BuildConfig.DEBUG_API_KEY
            val releaseToken = de.unisaarland.loladrives.BuildConfig.DEPLOY_API_KEY

            val isReleaseVersion = false

            val apiToken = if (isReleaseVersion) releaseToken else debugToken

            val uploadDestinationDebug = "https://api.loladrives.app/debug/donations"
            val uploadDestinationRelease = "https://api.loladrives.app/deploy/donations"
            val uploadDestination =
                if (isReleaseVersion) uploadDestinationRelease else uploadDestinationDebug
            connection = URL(uploadDestination).openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.setRequestProperty("Content-Type", "application/ppcdf")
            connection.setRequestProperty("x-api-token", apiToken)
            connection.setRequestProperty("x-app-version", appVersion)
            connection.setRequestProperty("x-donation-file-name", filename)
            connection.setRequestProperty("x-privacy-policy", "$privacyPolicyVersion")
            connection.connect()
            val outputStream = connection.outputStream
            val fileInputStream = file.inputStream()

            val bytes = ByteArray(1024)
            var count: Int
            do {
                count = fileInputStream.read(bytes)
                outputStream.write(bytes, 0, count)
            } while (count == 1024)

            outputStream.close()

            val responseCode = connection.responseCode
            val inputStream =
                if (responseCode >= 400) connection.errorStream else connection.inputStream
            val responseText = inputStream.bufferedReader().readText()

            val responseData = JSONObject(responseText)
            var authToken: String? = null
            if (responseData.getBoolean("success") == true) {
                authToken = responseData.getString(("authToken"))
                println("Auth Token for $filename: $authToken")
            } else {
                val errorMsg = responseData.getString(("error"))
                println("Error while trying to upload $filename: $errorMsg")
            }

            println("Result for file $filename: ${responseCode}\n$responseText")

            if (responseCode == HTTP_OK) {
                success = authToken
            }
        } catch (e: Exception) {
            e.printStackTrace()
            success = null
        } finally {
            connection?.disconnect()
            return success
        }
    }
}
