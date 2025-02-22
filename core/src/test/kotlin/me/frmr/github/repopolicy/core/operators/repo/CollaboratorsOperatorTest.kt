package me.frmr.github.repopolicy.core.operators.repo

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import me.frmr.github.repopolicy.core.model.PolicyValidationResult
import org.junit.jupiter.api.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.*
import me.frmr.github.repopolicy.core.operators.repo.CollaboratorsOperator.Companion.CollaboratorsDetail

@ExtendWith(MockKExtension::class)
class CollaboratorsOperatorTest {
  @MockK
  lateinit var mockRepo: GHRepository

  @MockK
  lateinit var mockGithub: GitHub

  @BeforeEach
  fun clearMyMocks() {
    clearMocks(mockRepo, mockGithub)
  }

  fun runValidate(desiredCollaborators: Set<CollaboratorsDetail>, currentCollaborators: Set<CollaboratorsDetail>): PolicyValidationResult {
    val allConstructs = desiredCollaborators + currentCollaborators
    val mockOrgs = mutableMapOf<String, GHOrganization>()
    for (currentCollaborator in allConstructs) {
      if (currentCollaborator.org != null) {
        // it's a team
        val mockOrg = mockOrgs.getOrPut(currentCollaborator.org!!) { mockk<GHOrganization>() }
        val mockTeam = mockk<GHTeam>()
        val resultList = if (currentCollaborators.contains(currentCollaborator)) {
          listOf(mockRepo)
        } else {
          listOf()
        }

        every { mockGithub.getOrganization(currentCollaborator.org) } returns mockOrg
        every { mockOrg.getTeamBySlug(currentCollaborator.name) } returns mockTeam
        every { mockTeam.listRepositories().toList() } returns resultList
      } else {
        val mockUser = mockk<GHUser>()
        val resultList = if (currentCollaborators.contains(currentCollaborator)) {
          listOf(mockRepo)
        } else {
          listOf()
        }

        every { mockGithub.getUser(currentCollaborator.name) } returns mockUser
        every { mockUser.listRepositories().toList() } returns resultList
      }
    }

    val sut = CollaboratorsOperator(desiredCollaborators.toList())
    every { mockRepo.fullName } returns "unit-tests/unit-tests"
    return sut.validate(mockRepo, mockGithub)
  }

  @Test
  fun validateFailsWithMissingCollaborators() {
    val desiredCollaborators = setOf(
      CollaboratorsDetail(null, "thing1", "admin"),
      CollaboratorsDetail(null, "thing2", "admin"),
    )
    val currentCollaborators = setOf(
      CollaboratorsDetail(null, "drseuss", "admin"),
      CollaboratorsDetail(null, "thing2", "admin"),
    )
    val result = runValidate(desiredCollaborators, currentCollaborators)

    assertThat(result.passed).isFalse
    assertThat(result.description).isEqualTo("Some collaborators were missing.")
  }

  @Test
  fun validatePassesWithAllCollaboratorsPresent() {
    val desiredCollaborators = setOf(
      CollaboratorsDetail(null, "thing1", "admin"),
      CollaboratorsDetail(null, "thing2", "admin"),
    )
    val currentCollaborators = setOf(
      CollaboratorsDetail(null, "drseuss", "admin"),
      CollaboratorsDetail(null, "thing2", "admin"),
      CollaboratorsDetail(null, "thing1", "admin")
    )
    val result = runValidate(desiredCollaborators, currentCollaborators)

    assertThat(result.passed).isTrue
    assertThat(result.description).isEqualTo("All collaborators are present.")
  }

  @Test
  fun validateFailsWithMissingCollaboratorTeams() {
    val desiredCollaborators = setOf(
      CollaboratorsDetail("UnitTests", "thing1", "admin"),
      CollaboratorsDetail("UnitTests", "thing2", "admin"),
    )
    val currentCollaborators = setOf(
      CollaboratorsDetail("UnitTests", "drseuss", "admin"),
      CollaboratorsDetail("UnitTests", "thing2", "admin"),
    )
    val result = runValidate(desiredCollaborators, currentCollaborators)

    assertThat(result.passed).isFalse
    assertThat(result.description).isEqualTo("Some collaborators were missing.")
  }

  @Test
  fun validatePassesWithAllCollaboratorTeamsPresent() {
    val desiredCollaborators = setOf(
      CollaboratorsDetail("UnitTests", "thing1", "admin"),
      CollaboratorsDetail("UnitTests", "thing2", "admin"),
    )
    val currentCollaborators = setOf(
      CollaboratorsDetail("UnitTests", "drseuss", "admin"),
      CollaboratorsDetail("UnitTests", "thing2", "admin"),
      CollaboratorsDetail("UnitTests", "thing1", "admin")
    )
    val result = runValidate(desiredCollaborators, currentCollaborators)

    assertThat(result.passed).isTrue
    assertThat(result.description).isEqualTo("All collaborators are present.")
  }
}
