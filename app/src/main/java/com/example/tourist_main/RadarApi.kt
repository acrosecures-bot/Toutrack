import com.example.tourist_main.RadarGeofenceResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface RadarApi {

    // Get ALL geofences (for Tour selection)
    @GET("v1/geofences")
    suspend fun getGeofences(
        @Header("Authorization") apiKey: String
    ): RadarGeofenceResponse


    // Get SINGLE geofence by externalId (for loading polygon)
    @GET("v1/geofences")
    suspend fun getGeofence(
        @Header("Authorization") apiKey: String,
        @Query("externalId") externalId: String
    ): RadarGeofenceResponse
}