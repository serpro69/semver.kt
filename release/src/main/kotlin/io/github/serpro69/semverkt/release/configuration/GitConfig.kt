package io.github.serpro69.semverkt.release.configuration

/**
 * Git configuration
 *
 * @property repo   git repository configuration
 * @property tag    git tag configuration
 */
interface GitConfig{
    val repo: GitRepoConfig
    val tag: GitTagConfig
    val message: GitMessageConfig
}
