package xyz.srnyx.gradlegalaxy.data.pom

@Suppress("unused")
data class LicenseData(
    val name: String,
    val url: String,
    val distribution: LicenseDistribution? = null,
    val comments: String? = null,
) {
    companion object {
        val MIT = LicenseData("MIT", "https://opensource.org/licenses/MIT", LicenseDistribution.REPO)
        val APACHE_2_0 = LicenseData("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt", LicenseDistribution.REPO)
        val GPL_V3 = LicenseData("GPL-3.0", "https://www.gnu.org/licenses/gpl-3.0.en.html", LicenseDistribution.REPO)
    }

    enum class LicenseDistribution(val value: String) {
        /**
         * May be downloaded from a Maven repository
         */
        REPO("repo"),

        /**
         * Must be manually installed
         *
         * @constructor Create empty Manual
         */
        MANUAL("manual"),
    }
}