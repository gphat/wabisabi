package wabisabi

import com.ning.http.client.RequestBuilder
import dispatch._
import Defaults._
import grizzled.slf4j.Logging
import java.net.URL
import scala.concurrent.Promise

class Client(esURL: String) extends Logging {

  // XXX multiget, update, multisearch, percolate, bulk

  /**
   * Request a count of the documents matching a query.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   * @param query The query to count documents from.
   */
  def count(indices: Seq[String], types: Seq[String], query: String): Future[Either[Throwable, String]] = {
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
  def createIndex(name: String, settings: Option[String] = None): Future[Either[Throwable, String]] = {
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
  def delete(index: String, `type`: String, id: String): Future[Either[Throwable, String]] = {
    // XXX Need to add parameters: version, routing, parent, replication,
    // consistency, refresh
    val req = url(esURL) / index / `type` / id

    // Do something hinky to get the trailing slash on the URL
    val trailedReq = new RequestBuilder().setUrl(req.build.getUrl + "/")
    doRequest(trailedReq.DELETE)
  }

  /**
   * Delete an index
   *
   * @param name The name of the index to delete.
   */
  def deleteIndex(name: String): Future[Either[Throwable,String]] = {
    val req = url(esURL) / name
    doRequest(req.DELETE)
  }

  /**
   * Get a document by ID.
   *
   * @param index The name of the index.
   * @param type The type of the document.
   * @param id The id of the document.
   */
  def get(index: String, `type`: String, id: String): Future[Either[Throwable, String]] = {
    val req = url(esURL) / index / `type` / id
    doRequest(req.GET)
  }

  /**
   * Get the mappings for a list of indices.
   *
   * @param indices A sequence of index names for which mappings will be fetched.
   * @param types A sequence of types for which mappings will be fetched.
   */
  def getMapping(indices: Seq[String], types: Seq[String]): Future[Either[Throwable,String]] = {
    val req = url(esURL) / indices.mkString(",") / types.mkString(",") / "_mapping"
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
  ): Future[Either[Throwable, String]] = {
    // XXX Need to add parameters: version, op_type, routing, parents & children,
    // timestamp, ttl, percolate, timeout, replication, consistency
    val baseRequest = url(esURL) / index / `type`
    var req = id.map({ id => baseRequest / id }).getOrElse(baseRequest)

    // Handle the refresh param
    req = if(refresh) {
      addParam(req, "refresh", Some("true"))
    } else {
      req
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
  def putMapping(indices: Seq[String], `type`: String, body: String): Future[Either[Throwable,String]] = {
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
  def search(index: String, query: String): Future[Either[Throwable,String]] = {
    val req = url(esURL) / index / "_search"
    req << query
    doRequest(req.GET)
  }

  /**
   * Verify that an index exists.
   *
   * @param name The name of the index to verify.
   */
  def verifyIndex(name: String): Future[Either[Throwable,String]] = {
    val req = url(esURL) / name
    doRequest(req.HEAD)
  }

  /**
   * Verify that a type exists.
   *
   * @param name The name of the type to verify.
   */
  def verifyType(index: String, `type`: String): Future[Either[Throwable,String]] = {
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
  ): RequestBuilder = value.map({ v =>
    req.addQueryParameter(name, v)
  }).getOrElse(req)

  /**
   * Perform the request with some debugging for good measure.
   *
   * @param req The request
   */
  private def doRequest(req: RequestBuilder) = {
    val breq = req.build
    debug("%s: %s".format(breq.getMethod, breq.getUrl))
    Http(req.setHeader("Content-type", "application/json") OK as.String).either
  }
}