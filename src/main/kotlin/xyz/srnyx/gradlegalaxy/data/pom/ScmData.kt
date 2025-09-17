package xyz.srnyx.gradlegalaxy.data.pom


/**
 * More information:
 * - [Maven Pom Reference](https://maven.apache.org/pom.html#scm)
 * - [Maven Central SCM Information](https://central.sonatype.org/publish/requirements/#scm-information)
 *
 * @property connection This URL gives read access (does not have to be public)
 * @property developerConnection This URL gives write access (does not have to be public)
 * @property url This URL gives the browser access to browse the repository (does not have to be public)
 * @property tag This specifies the tag that this project lives under
 */
data class ScmData(
    /**
     * This URL gives read access (does not have to be public)
     *
     * **Example:** `scm:git:git://github.com/Username/Repository.git`
     *
     * *More information can be found in the docs of this class*
     */
    val connection: String,
    /**
     * This URL gives write access (does not have to be public)
     *
     * **Examples:**
     * - `scm:git:ssh://github.com:Username/Repository.git` (**SSH setup required!**)
     * - `scm:git:git://github.com/Username/Repository.git`
     *
     * *More information can be found in the docs of this class*
     */
    val developerConnection: String,
    /**
     * This URL gives the browser access to browse the repository (does not have to be public)
     *
     * **Example:** `https://github.com/Username/Repository`
     *
     * *More information can be found in the docs of this class*
     */
    val url: String? = null,
    /**
     * This specifies the tag that this project lives under
     *
     * **Example:** HEAD
     *
     * *More information can be found in the docs of this class*
     */
    val tag: String? = null,
)