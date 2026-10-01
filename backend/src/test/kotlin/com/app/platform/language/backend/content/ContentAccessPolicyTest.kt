package com.app.platform.language.backend.content

import com.app.platform.language.backend.auth.AuthIdentity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentAccessPolicyTest {
  private val policy = ContentAccessPolicy(setOf("Owner@Example.com"))
  private val owner = AuthIdentity("owner-uid", "owner@example.com", "Owner", isEmailVerified = true)

  @Test
  fun everyoneSeesPublicContent() {
    assertTrue(policy.canSee(viewer = null, Visibility.PUBLIC))
  }

  @Test
  fun anonymousViewerDoesNotSeePrivateContent() {
    assertFalse(policy.canSee(viewer = null, Visibility.PRIVATE))
  }

  @Test
  fun ownerSeesPrivateContentWhateverTheEmailCase() {
    assertTrue(policy.canSee(owner.copy(email = "OWNER@example.COM"), Visibility.PRIVATE))
  }

  @Test
  fun otherUserDoesNotSeePrivateContent() {
    assertFalse(policy.canSee(owner.copy(email = "learner@example.com"), Visibility.PRIVATE))
  }

  @Test
  fun ownerEmailThatIsNotVerifiedDoesNotSeePrivateContent() {
    assertFalse(policy.canSee(owner.copy(isEmailVerified = false), Visibility.PRIVATE))
  }

  @Test
  fun userWithoutEmailIsNotOwner() {
    assertFalse(policy.isOwner(owner.copy(email = null)))
  }

  @Test
  fun nobodyIsOwnerWithoutOwnerEmails() {
    assertFalse(ContentAccessPolicy(emptySet()).isOwner(owner))
  }
}
