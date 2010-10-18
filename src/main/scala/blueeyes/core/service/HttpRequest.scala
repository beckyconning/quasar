package blueeyes.core.service

import HttpVersions._

sealed case class HttpRequest[T](method: HttpMethod, uri: String, parameters: Map[String, String] = Map(), headers: Map[String, String] = Map(), content: Option[T] = None, version: HttpVersion = Http_1_1)
