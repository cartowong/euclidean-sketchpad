package euclideansketchpad.model

data class ObjectMetadata(
    var visible: Boolean = true,
    var label: String? = null,
    var labelVisible: Boolean = false,
    var labelOffsetX: Double = 10.0,
    var labelOffsetY: Double = -10.0,
    var inMeasurementTable: Boolean = false,
    var labelAnchorAngle: Double? = null,
    var labelAnchorT: Double? = null,
    var isInput: Boolean = false,
    var isLocked: Boolean = false
)
