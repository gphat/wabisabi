package wabisabi

import com.ning.http.client.{RequestBuilder,Response}
import dispatch._
import Defaults._
import grizzled.slf4j.Logging
import java.net.URL
import scala.concurrent.Promise

class Client(esURL: String) extends Logging {

  // XXX multiget, update, multisearch, percolate, bulk, more like this,
  //

  /**
   * Request a count of the documents matching a query.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   * @param query The query to count documents from.
   */
  def count(indices: Seq[String], types: Seq[String], query: String): Future[Response] = {
    val req = url(esURL) / indices.mkString(",") / types.mkString(",") / "_count"
    req << query
    doRequest(req.GET)
  }

  /**
   * Create an index, optionally using the supplied settings.
   *
   * @param name The name of the index.
   * @param settings Optional settings
   */
  def createIndex(name: String, settings: Option[String] = None): Future[Response] = {
    val req = url(esURL) / name
    // Add the settings if we have any
    settings.map({ s => req << s })

    // Do something hinky to get the trailing slash on the URL
    val trailedReq = new RequestBuilder().setUrl(req.build.getUrl + "/")
    doRequest(trailedReq.PUT)
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
    val trailedReq = new RequestBuilder().setUrl(req.build.getUrl + "/")
    doRequest(trailedReq.DELETE)
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
    val req = url(esURL) / indices.mkString(",") / types.mkString(",") / "_query"

    req << query

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
   * Explain a query and document.
   *
   * @param index The name of the index.
   * @param type The optional type document to explain.
   * @param id The ID of the document.
   * @param query The query.
   * @param explain If true, then the response will contain more detailed information about the query.
   */
  def explain(index: String, `type`: String, id: String, query: String): Future[Response] = {
    // XXX Lots of params to add
    val req = url(esURL) / index / `type` / id / "_explain"

    req << query

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
   * Get the mappings for a list of indices.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   */
  def getMapping(indices: Seq[String], types: Seq[String]): Future[Response] = {
    val req = url(esURL) / indices.mkString(",") / types.mkString(",") / "_mapping"
    doRequest(req.GET)
  }

  /**
   * Query ElastichSearch for it's health.
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
    waitForStatus: Option[String] = None, waitForRelocatingShards: Option[Int] = None,
    waitForNodes: Option[String] = None, timeout: Option[String] = None
  ): Future[Response] = {
    val req = url(esURL) / "_cluster" / "health" / indices.mkString(",")

    addParam(req, "level", level)
    addParam(req, "wait_for_status", waitForStatus)
    addParam(req, "wait_for_relocation_shards", waitForRelocatingShards.flatMap({ s => Some(s.toString) }))
    addParam(req, "wait_for_nodes", waitForNodes)
    addParam(req, "timeout", timeout)

    doRequest(req.GET)
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
    val baseRequest = url(esURL) / index / `type`

    var req = id.map({ id => baseRequest / id }).getOrElse(
      // Do something hinky to get the trailing slash on the URL
      new RequestBuilder().setUrl(baseRequest.build.getUrl + "/")
    )

    // Handle the refresh param
    if(refresh) {
      addParam(req, "refresh", Some("true"))
    }

    // Add the data to the request
    req << data

    doRequest(req.PUT)
  }

  /**
   * Put a mapping for a list of indices.
   *
   * @param indices A sequence of index names for which mappings will be added.
   * @param type The type name to which the mappings will be applied.
   * @param body The mapping.
   */
  def putMapping(indices: Seq[String], `type`: String, body: String): Future[Response] = {
    val req = url(esURL) / indices.mkString(",") / `type` / "_mapping"
    req << body
    doRequest(req.PUT)
  }

  /**
   * Refresh an index.
   *
   * Makes all operations performed since the last refresh available for search.
   * @param index Name of the index to refresh
   */
  def refresh(index: String) = {
    val req = url(esURL) / index
    doRequest(req.POST)
  }

  /**
   * Search for documents.
   *
   * @param index The index to search
   * @param query The query to execute.
   */
  def search(index: String, query: String): Future[Response] = {
    val req = url(esURL) / index / "_search"
    req << query
    doRequest(req.POST)
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
    val req = url(esURL) / index / `type`.getOrElse("") / "_validate" / "query"

    // Handle the refresh param
    if(explain) {
      addParam(req, "explain", Some("true"))
    }

    req << query

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
   * @param name The name of the type to verify.
   */
  def verifyType(index: String, `type`: String): Future[Response] = {
    val req = url(esURL) / index / `type`
    doRequest(req.HEAD)
  }

  /**
   * Optionally add a parameter to the request.
   *
   * @param req The RequestBuilder to modify
   * @param name The name of the parameter
   * @param value The value of the param. If it's None then no parameter will be added
   */
  private def addParam(
    req: RequestBuilder, name: String, value: Option[String]
  ) = value.map({ v =>
    req.addQueryParameter(name, v)
  })

  /**
   * Perform the request with some debugging for good measure.
   *
   * @param req The request
   */
  private def doRequest(req: RequestBuilder) = {
    val breq = req.build
    debug("%s: %s".format(breq.getMethod, breq.getUrl))
    Http(req.setHeader("Content-type", "application/json"))
  }
}
