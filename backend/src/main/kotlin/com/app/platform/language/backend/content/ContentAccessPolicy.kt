package com.app.platform.language.backend.content

import com.app.platform.language.backend.auth.AuthIdentity

class ContentAccessPolicy(
  ownerEmails: Set<String>,
) {
  private val ownerEmails = ownerEmails.map { it.lowercase() }.toSet()

  fun canSee(
    viewer: AuthIdentity?,
    visibility: Visibility,
  ): Boolean = visibility == Visibility.PUBLIC || isOwner(viewer)

  // Unverified emails are rejected because anyone can register an account with someone else's address.
  fun isOwner(viewer: AuthIdentity?): Boolean =
    viewer != null && viewer.isEmailVerified && viewer.email?.lowercase() in ownerEmails
}
