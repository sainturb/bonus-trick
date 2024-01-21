package miu.edu.localizingdate

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import miu.edu.localizingdate.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalScope.launch(Dispatchers.Main) {
            val sunriseDeferred = async(Dispatchers.IO) { fetchTime("sunrise") }
            val sunsetDeferred = async(Dispatchers.IO) { fetchTime("sunset") }

            val sunriseTime = sunriseDeferred.await()
            val sunsetTime = sunsetDeferred.await()

            if (sunriseTime != null && sunsetTime != null) {
                val localizedSunrise = getLocalizedTime(sunriseTime, this@MainActivity)
                val localizedSunset = getLocalizedTime(sunsetTime, this@MainActivity)
                binding.textviewSunrise.text =
                    "${getString(Locale.SIMPLIFIED_CHINESE, R.string.SunriseTime)} $localizedSunrise"
                binding.textviewSunset.text =
                    "${getString(Locale.SIMPLIFIED_CHINESE, R.string.SunriseTime)} $localizedSunset"
            }
        }
    }

    private fun Context.getString(locale: Locale, @StringRes resId: Int, vararg formatArgs: Any): String {
        var conf: Configuration = resources.configuration
        conf = Configuration(conf)
        conf.setLocale(locale)
        val localizedContext = createConfigurationContext(conf)
        return localizedContext.resources.getString(resId, *formatArgs)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLocalizedTime(time: LocalDateTime, context: Context): String {
        val userPreferredLanguage = Locale.SIMPLIFIED_CHINESE.language
        val sdf = SimpleDateFormat("hh:mm a", Locale(userPreferredLanguage))
        return sdf.format(
            time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchTime(type: String): LocalDateTime? {
        return try {
            val apiUrl =
                URL("https://api.sunrise-sunset.org/json?lat=37.7749&lng=-122.4194&formatted=0")
            val urlConnection: HttpURLConnection = apiUrl.openConnection() as HttpURLConnection
            try {
                val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val jsonResponse = JSONObject(response.toString())
                val timeUTC = jsonResponse.getJSONObject("results").getString(type)
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
                val dateTime = formatter.parse(timeUTC)
                LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault())
            } finally {
                urlConnection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}