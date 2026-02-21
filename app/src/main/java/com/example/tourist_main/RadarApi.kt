import com.example.tourist_main.RadarGeofenceResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface RadarApi {

    @GET("v1/geofences")
    suspend fun getGeofence(
        @Header("Authorization") apiKey: String,
        @Query("externalId") externalId: String
    ): RadarGeofenceResponse
}