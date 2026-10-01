package com.app.platform.language.core.exam

import kotlin.test.Test
import kotlin.test.assertEquals

class WritingWordCounterTest {
  @Test
  fun emptyOrBlankTextHasNoWords() {
    assertEquals(0, WritingWordCounter.count(""))
    assertEquals(0, WritingWordCounter.count(" \n\t "))
  }

  @Test
  fun wordsAreSeparatedByAnyWhitespace() {
    assertEquals(6, WritingWordCounter.count("  Parks make\ncities\tgreener and   calmer. "))
  }

  @Test
  fun hyphenatedWordsCountAsOne() {
    assertEquals(4, WritingWordCounter.count("a well-known long-term plan"))
  }

  @Test
  fun numbersCountAsOneWord() {
    assertEquals(8, WritingWordCounter.count("Visitors rose from 1,000 to 2.5 million (25%)"))
  }

  @Test
  fun contractionsAndAbbreviationsCountAsOne() {
    assertEquals(4, WritingWordCounter.count("It doesn't matter, e.g."))
  }

  @Test
  fun standalonePunctuationIsNotAWord() {
    assertEquals(4, WritingWordCounter.count("Shops - and parks & more ..."))
  }

  @Test
  fun dashesBetweenWordsSeparateThem() {
    assertEquals(4, WritingWordCounter.count("cars—not parks–matter"))
  }
}
