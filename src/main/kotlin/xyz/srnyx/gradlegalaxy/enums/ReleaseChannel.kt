package xyz.srnyx.gradlegalaxy.enums

import me.modmuss50.mpp.ReleaseType


//TODO will this error if MPP not applied?
enum class ReleaseChannel(val mpp: ReleaseType, val hangar: String) {
    RELEASE(ReleaseType.STABLE, "Release"),
    BETA(ReleaseType.BETA, "Beta"),
    ALPHA(ReleaseType.ALPHA, "Alpha"),
}
