package dev.sasikanth.rss.reader.network

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlOptions
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import dev.sasikanth.rss.reader.models.FeedPayload
import dev.sasikanth.rss.reader.models.PostPayload
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import platform.Foundation.NSDateFormatter
import platform.Foundation.timeIntervalSince1970

private val atomDateFormatter = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ" }

internal fun PostPayload.Companion.mapAtomPost(atomMap: Map<String, String>): PostPayload {
  val pubDate = atomMap["published"]
  val link = atomMap["link"]?.trim()
  val data = atomMap["content"]
  var imageUrl = atomMap["imageUrl"]
  var content: String? = null

  val parser =
    KsoupHtmlParser(
      handler =
        IOSAtomContentParser {
          if (imageUrl.isNullOrBlank()) {
            imageUrl = it.imageUrl
          }

          content = it.content
        },
      options =
        KsoupHtmlOptions(
          xmlMode = true,
        )
    )

  parser.parseComplete(data.orEmpty())

  return PostPayload(
    title = FeedParser.cleanText(atomMap["title"])!!,
    link = link!!,
    description = content.orEmpty(),
    imageUrl = imageUrl,
    date = pubDate.atomDateStringToEpochSeconds()
  )
}

internal fun FeedPayload.Companion.mapAtomFeed(
  feedUrl: String,
  atomMap: Map<String, String>,
  posts: List<PostPayload>
): FeedPayload {
  val link = atomMap["link"]!!.trim()
  val domain = Url(link)
  val iconUrl =
    FeedParser.feedIcon(
      if (domain.host != "localhost") domain.host
      else domain.pathSegments.first().split(" ").first().trim()
    )

  return FeedPayload(
    name = FeedParser.cleanText(atomMap["title"])!!,
    homepageLink = link,
    link = feedUrl,
    description = FeedParser.cleanText(atomMap["subtitle"]).orEmpty(),
    icon = iconUrl,
    posts = posts
  )
}

private fun String?.atomDateStringToEpochSeconds(): Long {
  if (this.isNullOrBlank()) return 0L

  val date =
    try {
      atomDateFormatter.dateFromString(this.trim())
    } catch (e: Exception) {
      Napier.e("Parse date error: ${e.message}")
      null
    }

  return date?.timeIntervalSince1970?.times(1000)?.toLong() ?: 0L
}
