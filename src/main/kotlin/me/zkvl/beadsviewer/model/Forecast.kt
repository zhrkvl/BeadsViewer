package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zkvl.beadsviewer.serialization.InstantSerializer

/**
 * Represents an ETA (Estimated Time of Arrival) prediction for a specific issue.
 *
 * Forecasts use historical velocity data and complexity scoring to predict
 * when an issue will be completed.
 */
@Serializable
data class Forecast(
    /**
     * ID of the issue (bead) this forecast is for.
     */
    @SerialName("bead_id")
    val beadId: String,

    /**
     * Predicted completion date/time.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("eta_date")
    val etaDate: Instant,

    /**
     * Confidence level for this forecast (0.0 to 1.0).
     * 0.0 = no confidence, 1.0 = high confidence.
     */
    val confidence: Double,

    /**
     * List of factors that influenced this forecast
     * (e.g., "velocity", "complexity", "dependencies").
     */
    val factors: List<String> = emptyList(),

    /**
     * Timestamp when this forecast was generated.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant
) {
    /**
     * Validates that the forecast data is logically consistent.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(beadId.isNotBlank()) { "bead_id cannot be empty" }
        require(confidence in 0.0..1.0) {
            "confidence ($confidence) must be between 0 and 1"
        }
    }
}
