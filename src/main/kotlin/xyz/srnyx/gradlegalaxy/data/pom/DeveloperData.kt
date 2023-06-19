package xyz.srnyx.gradlegalaxy.data.pom

import xyz.srnyx.gradlegalaxy.annotations.Ignore

data class DeveloperData(
    val id: String? = null,
    val name: String? = null,
    val url: String? = null,
    val email: String? = null,
    val timezone: String? = null,
    val organization: String? = null,
    val organizationUrl: String? = null,
    val roles: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap(),
) {
    fun isEmpty(): Boolean = id == null
            && url == null
            && timezone == null
            && roles.isEmpty()
            && email == null
            && name == null
            && organization == null
            && organizationUrl == null
            && properties.isEmpty()

    companion object {
        val srnyx = DeveloperData(
            id = "srnyx",
            url = "https://srnyx.com",
            email = "contact@srnyx.com",
            timezone = "America/New_York",
            organization = "Venox Network",
            organizationUrl = "https://venox.network")
        @Ignore
        val dkim19375 = DeveloperData(
            id = "dkim19375",
            url = "America/New_York")
    }
}
