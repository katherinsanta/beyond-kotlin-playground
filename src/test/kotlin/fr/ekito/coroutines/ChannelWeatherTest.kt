package fr.ekito.coroutines

import fr.ekito.weather.WeatherWS
import fr.ekito.weather.json.Components
import fr.ekito.weather.json.geocode.Location
import fr.ekito.weather.json.getLocation
import fr.ekito.weather.json.weather.Forecastday_
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class ChannelWeatherTest {

    @Test
    fun testWeather() = runBlocking {
        val ws = Components.retrofitWS(Components.SERVER_URL)
        val address = "Paris, france"
        val locationChannel = channelGeocode(address, ws)
        val weatherChannel = channelWeather(locationChannel, ws)
        weatherChannel.consumeEach { fd ->
            println("got forecast day is $fd")
        }
        locationChannel.cancel()
        weatherChannel.cancel()
        println("weather done !")
    }

    fun channelWeather(locationChannel: ProducerJob<Location?>, ws: WeatherWS): ProducerJob<Forecastday_> = produce(CommonPool) {
        locationChannel.consumeEach { location ->
            if (location != null) {
                val list = ws.weather(location.lat, location.lng, "EN").execute().body().forecast?.simpleforecast?.forecastday?.take(4).orEmpty()
                list.forEach { send(it) }
            } else {
                close()
            }
        }
    }

    fun channelGeocode(location: String, ws: WeatherWS) = produce(CommonPool) {
        val body = ws.geocode(location).execute().body()
        val loc = body.getLocation()
        if (loc != null) {
            send(loc)
        } else {
            close()
        }
    }
}