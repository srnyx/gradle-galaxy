package xyz.srnyx.gradlegalaxy.enums

import me.modmuss50.mpp.ReleaseType


enum class ReleaseChannel(val mpp: ReleaseType, val hangar: String) {
    RELEASE(ReleaseType.STABLE, "Release"),
    BETA(ReleaseType.BETA, "Beta"),
    ALPHA(ReleaseType.ALPHA, "Alpha"),
}
