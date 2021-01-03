package me.frmr.github.repopolicy.core.operators.repo

import me.frmr.github.repopolicy.core.model.PolicyEnforcementResult
import me.frmr.github.repopolicy.core.model.PolicyRuleOperator
import me.frmr.github.repopolicy.core.model.PolicyValidationResult
import org.kohsuke.github.GHRepository

class FeaturesOperator(val issuesEnabled: Boolean?, val projectsEnabled: Boolean?, val wikiEnabled: Boolean?): PolicyRuleOperator {
  override val description: String = "Enforce enabled/disabled repo features"

  private fun issueLanguage(state: Boolean) = when(state) {
    false -> "disabled"
    true -> "enabled"
  }

  override fun validate(target: GHRepository): PolicyValidationResult {
    var passedValidation = true
    val failureReasons: MutableList<String> = mutableListOf()

    if (issuesEnabled != null && issuesEnabled != target.hasIssues()) {
      passedValidation = false
      failureReasons.add("policy requires issues be ${issueLanguage(issuesEnabled)}, found ${issueLanguage(target.hasIssues())}")
    }

    if (projectsEnabled != null && projectsEnabled != target.hasProjects()) {
      passedValidation = false
      failureReasons.add("policy requires projects be ${issueLanguage(projectsEnabled)}, found ${issueLanguage(target.hasProjects())}")
    }

    if (wikiEnabled != null && wikiEnabled != target.hasWiki()) {
      passedValidation = false
      failureReasons.add("policy requires wikis be ${issueLanguage(wikiEnabled)}, found ${issueLanguage(target.hasWiki())}")
    }

    val fullDescription = failureReasons.joinToString(", ")

    return if (passedValidation) {
      PolicyValidationResult(
        target.fullName,
        "Passed Validation",
        true
      )
    } else {
      PolicyValidationResult(
        target.fullName,
        fullDescription,
        false
      )
    }
  }

  override fun enforce(target: GHRepository): PolicyEnforcementResult {
    TODO("Not yet implemented")
  }
}
