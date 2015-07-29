package wabisabi

import com.ning.http.client.Response
import dispatch._
import Defaults._
import grizzled.slf4j.Logging
import java.nio.charset.StandardCharsets

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

class Client(esURL: String) extends Logging {

  // XXX update, percolate, more like this,
  //

  /**
   * Use the bulk API to perform many index/delete operations in a single call.
   *
   * @param index The optional index name.
   * @param type The optional type.
   * @param data The operations to perform as described by the [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-bulk.html ElasticSearch Bulk API]].
   */
  def bulk(index: Option[String] = None, `type`: Option[String] = None, data: String): Future[Response] = {
    val freq = (url(esURL) / index.getOrElse("") / `type`.getOrElse("") / "_bulk").setBody(data.getBytes(StandardCharsets.UTF_8))
    doRequest(freq.POST)
  }

  /**
   * Request a count of the documents matching a query.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   * @param query The query to count documents from.
   */
  def count(indices: Seq[String], types: Seq[String], query: String): Future[Response] = {
    val req = (url(esURL) / indices.mkString(",") / types.mkString(",") / "_count").setBody(query.getBytes(StandardCharsets.UTF_8))
    doRequest(req.GET)
  }

  /**
   * Create aliases.
   *
   * @param actions A String of JSON containing the actions to be performed. This string will be placed within the actions array passed
   *
   * As defined in the [[http://www.elasticsearch.org/guide/reference/api/admin-indices-aliases/ ElasticSearch Admin Indices API]] this
   * method takes a string representing a list of operations to be performed. Remember to
   * {{{
   * val actions = """{ "add": { "index": "index1", "alias": "alias1" } }, { "add": { "index": "index2", "alias": "alias2" } }"""
   * }}}
   */
  def createAlias(actions: String): Future[Response] = {
    val req = (url(esURL) / "_aliases").setBody(("""{ "actions": [ """ + actions + """ ] }""").getBytes(StandardCharsets.UTF_8))

    doRequest(req.POST)
  }

  /**
   * Create an index, optionally using the supplied settings.
   *
   * @param name The name of the index.
   * @param settings Optional settings
   */
  def createIndex(name: String, settings: Option[String] = None): Future[Response] = {
    val req = url(esURL) / name

    // Do something hinky to get the trailing slash on the URL
    // TODO: Find out if we really need to do this.  The tests pass without it!
    val withTrailingSlash = Req(_.setUrl(req.toRequest.getUrl + "/"))

    // Add the settings if we have any
    val withSettingsIfPresent =
      settings.map({ s => withTrailingSlash.setBody(s.getBytes(StandardCharsets.UTF_8)) }).getOrElse(withTrailingSlash)

    doRequest(withSettingsIfPresent.PUT)
  }

  /**
   * Delete a document from the index.
   *
   * @param index The name of the index.
   * @param type The type of document to delete.
   * @param id The ID of the document.
   */
  def delete(index: String, `type`: String, id: String): Future[Response] = {
    // XXX Need to add parameters: version, routing, parent, replication,
    // consistency, refresh
    val req = url(esURL) / index / `type` / id

    // Do something hinky to get the trailing slash on the URL
    val trailedReq = new Req(_.setUrl(req.toRequest.getUrl + "/"))
    doRequest(trailedReq.DELETE)
  }

  /**
   * Delete an index alias.
   *
   * @param index The name of the index.
   * @param alias The name of the alias.
   */
  def deleteAlias(index: String, alias: String): Future[Response] = {
    val req = url(esURL) / index / "_alias" / alias

    doRequest(req.DELETE)
  }

  /**
   * Delete documents that match a query.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   * @param query The query to count documents from.
   */
  def deleteByQuery(indices: Seq[String], `types`: Seq[String], query: String): Future[Response] = {
    // XXX Need to add parameters: df, analyzer, default_operator
    val req = (url(esURL) / indices.mkString(",") / types.mkString(",") / "_query").setBody(query.getBytes(StandardCharsets.UTF_8))

    doRequest(req.DELETE)
  }

  /**
   * Delete an index
   *
   * @param name The name of the index to delete.
   */
  def deleteIndex(name: String): Future[Response] = {
    val req = url(esURL) / name
    doRequest(req.DELETE)
  }

  /**
   * Delete a warmer.
   *
   * @param index The index of the warmer.
   * @param name The name of the warmer.
   */
  def deleteWarmer(index: String, name:String): Future[Response] = {
    val req = url(esURL) / index / "_warmer" / name
    doRequest(req.DELETE)
  }

  /**
   * Explain a query and document.
   *
   * @param index The name of the index.
   * @param type The optional type document to explain.
   * @param id The ID of the document.
   * @param query The query.
   */
  def explain(index: String, `type`: String, id: String, query: String): Future[Response] = {
    // XXX Lots of params to add
    val req = (url(esURL) / index / `type` / id / "_explain").setBody(query.getBytes(StandardCharsets.UTF_8))

    doRequest(req.POST)
  }

  /**
   * Get a document by ID.
   *
   * @param index The name of the index.
   * @param type The type of the document.
   * @param id The id of the document.
   */
  def get(index: String, `type`: String, id: String): Future[Response] = {
    val req = url(esURL) / index / `type` / id
    doRequest(req.GET)
  }

  /**
   * Get multiple documents by ID.
   *
   * @param index The optional name of the index.
   * @param type The optional type of the document.
   * @param query The query to execute.
   */
  def mget(index: Option[String], `type`: Option[String], query: String): Future[Response] = {
    val req = (url(esURL) / index.getOrElse("") / `type`.getOrElse("") / "_mget").setBody(query.getBytes(StandardCharsets.UTF_8))
    doRequest(req.POST)
  }

  /**
   * Get aliases for indices.
   *
   * @param index Optional name of an index. If no index is supplied, then the query will check all indices.
   * @param query The name of alias to return in the response. Like the index option, this option supports wildcards and the option the specify multiple alias names separated by a comma.
   */
  def getAliases(index: Option[String], query: String = "*"): Future[Response] = {
    val req = url(esURL)
    val freq = index.map(i => req / i).getOrElse(req) / "_alias" / query

    doRequest(freq.GET)
  }

  /**
   * Get the mappings for a list of indices.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   */
  def getMapping(indices: Seq[String], types: Seq[String]): Future[Response] = {
    val req = url(esURL) / indices.mkString(",") / "_mapping" / types.mkString(",")
    doRequest(req.GET)
  }

  /**
   * Get the settings for a list of indices.
   *
   * @param indices A sequence of index names for which settings will be fetched.
   */
  def getSettings(indices: Seq[String]): Future[Response] = {
    val req = url(esURL) / indices.mkString(",") / "_settings"
    doRequest(req.GET)
  }

  /**
   * Get matching warmers.
   * @param index Name of index to check.
   * @param name Expression to match warmer.
   */
  def getWarmers(index: String, name: String): Future[Response] = {
    val req = url(esURL) / index / "_warmer" / name
    doRequest(req.GET)
  }

  /**
   * Query ElasticSearch for it's health.
   *
   * @param indices Optional list of index names. Defaults to empty.
   * @param level Can be one of cluster, indices or shards. Controls the details level of the health information returned.
   * @param waitForStatus One of green, yellow or red. Will wait until the status of the cluster changes to the one provided, or until the timeout expires.
   * @param waitForRelocatingShards A number controlling to how many relocating shards to wait for.
   * @param waitForNodes The request waits until the specified number N of nodes is available. Is a string because >N and ge(N) type notations are allowed.
   * @param timeout A time based parameter controlling how long to wait if one of the waitForXXX are provided.
   */
  def health(
    indices: Seq[String] = Seq.empty[String], level: Option[String] = None,
    waitForStatus: Option[String] = None, waitForRelocatingShards: Option[String] = None,
    waitForNodes: Option[String] = None, timeout: Option[String] = None
  ): Future[Response] = {
    val req = url(esURL) / "_cluster" / "health" / indices.mkString(",")

    val paramNames = List("level", "wait_for_status", "wait_for_relocation_shards", "wait_for_nodes", "timeout")
    val params = List(level, waitForStatus, waitForRelocatingShards, waitForNodes, timeout)
    val freq = addQueryParams(req, paramNames, params)

    doRequest(freq.GET)
  }

  private def addQueryParams(req: Req, paramNames: List[String], params: List[Option[String]]) = {
    // Fold in all the parameters that might've come in.
    paramNames.zip(params).foldLeft(req)(
      (r, nameAndParam) => {
        nameAndParam._2.map({ p =>
          r.addQueryParameter(nameAndParam._1, p)
        }).getOrElse(r)
      }
    )
  }

  /**
   * Query ElasticSearch Stats. Parameters to enable non-default stats as desired.
   *
   * @param indices Optional list of index names. Defaults to empty.
   * @param clear Clears all the flags (first).
   * @param refresh refresh stats.
   * @param flush flush stats.
   * @param merge merge stats.
   * @param warmer Warmer statistics.
   */
  def stats(indices: Seq[String] = Seq(), clear: Boolean = false, refresh: Boolean = false, flush: Boolean = false, merge: Boolean = false, warmer: Boolean = false): Future[Response] = {
    val req = indices match {
      case Nil => url(esURL) / "_stats"
      case _ => url(esURL) / indices.mkString(",") / "_stats"
    }

    val paramNames = List("clear", "refresh", "flush", "merge", "warmer")
    val params = List(clear, refresh, flush, merge, warmer)
    val reqWithParams = paramNames.zip(params).filter(_._2).foldLeft(req)((r, nameAndParam) => r.addQueryParameter(nameAndParam._1, nameAndParam._2.toString))

    doRequest(reqWithParams.GET)
  }

  /**
   * Index a document.
   *
   * Adds or updates a JSON documented of the specified type in the specified
   * index.
   * @param index The index in which to place the document
   * @param type The type of document to be indexed
   * @param id The id of the document. Specifying None will trigger automatic ID generation by ElasticSearch
   * @param data The document to index, which should be a JSON string
   * @param refresh If true then ElasticSearch will refresh the index so that the indexed document is immediately searchable.
   */
  def index(
    index: String, `type`: String, id: Option[String] = None, data: String,
    refresh: Boolean = false
  ): Future[Response] = {
    // XXX Need to add parameters: version, op_type, routing, parents & children,
    // timestamp, ttl, percolate, timeout, replication, consistency
    val baseRequest = (url(esURL) / index / `type`).setBody(data.getBytes(StandardCharsets.UTF_8))

    val req = id.map({ id => baseRequest / id.get.toString }).getOrElse(baseRequest)

    // Handle the refresh param
    val freq = req.addQueryParameter("refresh", if(refresh) { "true" } else { "false" })

    id.map({ i => doRequest(freq.PUT) }).getOrElse(doRequest(freq.POST))
  }
  
  /**
   * Upsert a document.
   *
   * Adds or updates a JSON documented of the specified type in the specified
   * index using https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html?q=update#upserts
   * @param index The index in which to place the document
   * @param type The type of document to be indexed
   * @param id The id of the document. Specifying None will trigger automatic ID generation by ElasticSearch
   * @param data The document to index, which should be a JSON string
   * @param refresh If true then ElasticSearch will refresh the index so that the indexed document is immediately searchable.
   */
  def docAsUpsert(
    index: String, `type`: String, id: String, data: String,
    refresh: Boolean = false
  ): Future[Response] = {
    val req = (url(esURL) / index / `type` / id / "_update").setBody(s"{\"doc\": ${data.getBytes(StandardCharsets.UTF_8)}, \"doc_as_upsert\":true}")

    // Handle the refresh param
    val freq = req.addQueryParameter("refresh", if (refresh) {
      "true"
    } else {
      "false"
    })

    doRequest(freq.POST)
  }

  /**
   * Put a mapping for a list of indices.
   *
   * @param indices A sequence of index names for which mappings will be added.
   * @param type The type name to which the mappings will be applied.
   * @param body The mapping.
   * @param ignoreConflicts When merge has conflicts overwrite mapping anyway, default false.
   */
  def putMapping(indices: Seq[String], `type`: String, body: String, ignoreConflicts: Boolean = false): Future[Response] = {
    val req = (url(esURL) / indices.mkString(",") / `type` / "_mapping")
      .setQueryParameters(Map("ignore_conflicts" -> List(ignoreConflicts.toString)))
      .setBody(body.getBytes(StandardCharsets.UTF_8))
    doRequest(req.PUT)
  }

  /**
   * Put settings for a list of indices.
   *
   * @param indices A sequence of index names for which settings will be updated.
   * @param body The settings.
   */
  def putSettings(indices: Seq[String], body: String): Future[Response] = {
    val req = (url(esURL) / indices.mkString(",") / "_settings")
      .setBody(body.getBytes(StandardCharsets.UTF_8))
    doRequest(req.PUT)
  }

  /**
   * Add a warmer.
   *
   * @param index The index to add the warmer.
   * @param name The name of the warmer.
   * @param body The warmer content.
   */
  def putWarmer(index: String, name:String, body: String): Future[Response] = {
    val req = (url(esURL) / index / "_warmer" / name).setBody(body.getBytes(StandardCharsets.UTF_8))
    doRequest(req.PUT)
  }

  /**
   * Refresh an index.
   *
   * Makes all operations performed since the last refresh available for search.
   * @param index Name of the index to refresh
   */
  def refresh(index: String) = {
    val req = url(esURL) / index / "_refresh"
    doRequest(req.POST)
  }

  /**
   * Search for documents.
   *
   * @param index The index to search
   * @param type The optional type of document to search
   * @param query The query to execute.
   * @param uriParameters The query uri parameters.
   */
  def search(index: String, query: String, `type`: Option[String] = None,
             uriParameters: SearchUriParameters = SearchUriParameters.withDefaults): Future[Response] = {
    val req = (url(esURL) / index / `type`.getOrElse("") / "_search").setBody(query.getBytes(StandardCharsets.UTF_8))

    val paramNames = List("search_type", "scroll")
    val params = List(uriParameters.searchType.map(_.parameterValue), uriParameters.scroll)
    val freq = addQueryParams(req, paramNames, params)

    doRequest(freq.POST)
  }

  /**
   * Scrolls for more documents.
   *
   * @param scroll The scroll parameter which tells Elasticsearch how long it should keep the “search context” alive
   * @param scrollId The _scroll_id value returned in the response to the previous search or scroll request
   */
  def scroll(scroll: String, scrollId: String): Future[Response] = {
    val req = (url(esURL) / "_search" / "scroll")

    val paramNames = List("scroll", "scroll_id")
    val params = List(Some(scroll), Some(scrollId))
    val freq = addQueryParams(req, paramNames, params)

    doRequest(freq.GET)
  }

  /**
   * Suggest completions based on analyzed documents.
   *
   * @param index The index to search
   * @param query The query to execute.
   */
  def suggest(index: String, query: String): Future[Response] = {
    val req = (url(esURL) / index / "_suggest").setBody(query.getBytes(StandardCharsets.UTF_8))
    doRequest(req.POST)
  }

  /**
   * Get multiple documents by ID.
   *
   * @param index The optional name of the index.
   * @param type The optional type of the document.
   * @param query The query to execute.
   */
  def msearch(index: Option[String] = None, `type`: Option[String] = None, query: String): Future[Response] = {
    val req = (url(esURL) / index.getOrElse("") / `type`.getOrElse("") / "_msearch")
      .setBody(query.getBytes(StandardCharsets.UTF_8))
    doRequest(req.GET)
  }

  /**
   * Validate a query.
   *
   * @param index The name of the index.
   * @param type The optional type of document to validate against.
   * @param query The query.
   * @param explain If true, then the response will contain more detailed information about the query.
   */
  def validate(
    index: String, `type`: Option[String] = None, query: String, explain: Boolean = false
  ): Future[Response] = {
    val req = (url(esURL) / index / `type`.getOrElse("") / "_validate" / "query").setBody(query.getBytes(StandardCharsets.UTF_8))

    // Handle the refresh param
    val freq = req.addQueryParameter("explain", if(explain) { "true" } else { "false"})

    doRequest(req.POST)
  }

  /**
   * Verify that an index exists.
   *
   * @param name The name of the index to verify.
   */
  def verifyIndex(name: String): Future[Response] = {
    val req = url(esURL) / name
    doRequest(req.HEAD)
  }

  /**
   * Verify that a type exists.
   *
   * @param index The name of the index to verify.
   * @param type The name of the document type to verify.
   */
  def verifyType(index: String, `type`: String): Future[Response] = {
    val req = url(esURL) / index / `type`
    doRequest(req.HEAD)
  }

  /**
   * Perform the request with some debugging for good measure.
   *
   * @param req The request
   */
  private def doRequest(req: Req) = {
    val breq = req.toRequest
    debug("%s: %s".format(breq.getMethod, breq.getUrl))
    Http(req.setHeader("Content-type", "application/json; charset=utf-8"))
  }


  private def url(es_url: String) = {

    val uri: Uri = es_url

    val protocol = uri.protocol.getOrElse("http")
    val port = uri.port.getOrElse(protocol match {
      case "http" => 80
      case "https" => 443
    })

    val req = if (uri.user.isDefined && uri.password.isDefined) {

      dispatch.url(protocol+"://"+uri.host.get+":"+port).as_!(uri.user.get, uri.password.get)
    }

    else {
      dispatch.url(protocol+"://"+uri.host.get+":"+port)

    }

    protocol match {
      case "http" => req
      case "https" => req.secure
      case _ => {
        logger.error("Unknown protocol: %s".format(protocol))
        dispatch.url("")
      }
    }
  }
}

sealed trait SearchType { def parameterValue: String }
case object DfsQueryThenFetch extends SearchType { val parameterValue = "dfs_query_then_fetch" }
case object DfsQueryAndFetch extends SearchType { val parameterValue = "dfs_query_and_fetch" }
case object QueryThenFetch extends SearchType { val parameterValue = "query_then_fetch" }
case object QueryAndFetch extends SearchType { val parameterValue = "query_and_fetch" }
case object Count extends SearchType { val parameterValue = "count" }
case object Scan extends SearchType { val parameterValue = "scan" }

case class SearchUriParameters(searchType: Option[SearchType] = None,
                               scroll: Option[String] = None)
object SearchUriParameters {
  val withDefaults = SearchUriParameters(None, None)
}

object Client {
  /**
   * Disconnects any remaining connections. Both idle and active. If you are accessing
   * Elasticsearch through a proxy that keeps connections alive this is useful.
   *
   * If your application uses the dispatch library for other purposes, those connections
   * will also terminate.
   */
  def shutdown() {
    Http.shutdown()
  }
}
