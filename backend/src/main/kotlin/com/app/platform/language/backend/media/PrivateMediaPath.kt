package com.app.platform.language.backend.media

private val segmentPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

fun isPrivateMediaPath(path: String): Boolean {
  val segments = path.split('/')
  return segments.size == 2 && segments.all(segmentPattern::matches)
}
