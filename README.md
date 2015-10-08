# Wabisabi

[![Build Status](https://travis-ci.org/gphat/wabisabi.svg?branch=master)](https://travis-ci.org/gphat/wabisabi)

Wabisabi is a Scala [Elasticearch](http://www.elasticsearch.org/) client that
uses the REST API and has no dependency on Elasticsearch itself. It is
extremely minimal, eschewing any sort of parsing, formatting or other such
complication. You can [read about why I wrote it if you like](http://onemogin.com/programming/oss/wabisabi-scala-http-client-for-elasticsearch.html).

Wabisabi is based on the [dispatch](http://dispatch.databinder.net/Dispatch.html)
asynchronous HTTP library. Therefore, all of the returned values are
`Future[Response]`.

The returned object is a [Response](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/Response.html)
from the async-http-client library. Normally you'll want to use `getResponseBody`
to get the response but you can also check `getStatusCode` to verify something
didn't go awry. Since the returned response object is wrapped in a Future, you would need to `map` over it to get a
Future of response code or body, e.g.:

```
val futureStatusCode: Future[Int] = client.verifyIndex("foo").map(_.getStatusCode)
```

Or,

```
val futureResponseBody: Future[String] = client.verifyIndex("foo").map(_.getResponseBody)
```

## Dependencies

Depends on [dispatch](http://dispatch.databinder.net/Dispatch.html) and
[grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/). It's compiled for
scala 2.11.

## Notes

Wabisabi was tested against Elasticsearch >= 1.4.0.

Version 2.0.0 switched to returning `Future[Response]` rather than `Future[String]`
so that errors can be handled.

This does not implement every piece of the Elasticsearch API. I will add other
bits as needed or as patches arrive.

Version 2.1.0 uses Elasticsearch 1.5.0

# Using It

```
// Add the Dep
libraryDependencies += "wabisabi" %% "wabisabi" % "2.1.4"

// And a the resolver
resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/",
```

See the [API docs here](http://gphat.github.io/wabisabi/api/index.html#package)!

## Example

```
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

val client = new Client("http://localhost:9200")

// Get the cluster's health
client.health

// Create the index
client.createIndex(name = "foo")

// Verify the index exists
client.verifyIndex("foo").map(_.getStatusCode) // Should be Future(200)

// Get mapping
client.getMapping(indices = Seq("foo"), types = Seq("bar"))

// Put mapping, optionally ignore conflicts
client.putMapping(indices = Seq("foo"), `type` = "bar",
  body = "{\"bar\": {\"properties\": {\"baz\": {\"type\": \"string\"} } } }",
  ignoreConflicts = true
)

// Add a document to the index.
client.index(
  index = "foo", `type` = "foo", id = Some("foo"),
  data = "{\"foo\":\"bar\"}", refresh = true
)

// Fetch that document by it's id.
client.get("foo", "foo", "foo").map(_.getResponseBody)

// Search for all documents.
client.search(index = "foo", query = "{\"query\": { \"match_all\": {} }").map(_.getResponseBody)

// Search for all documents of a specific type!
client.search(index = "foo", query = "{\"query\": { \"match_all\": {} }", `type`= "tweet").map(_.getResponseBody)

// SearchUriParameters case class allows for customised search URI parameters, such as search_type to be passed in a search request as per [this](http://www.elastic.co/guide/en/elasticsearch/reference/current/search-uri-request.html) reference document.
// The list of different search types, in addition to documentation on how each search type is executed by elasticsearch can be seen [here](http://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-search-type.html).
// For example, to search for all documents with `Count` search type:
client.search(index = "foo", query = "{\"query\": { \"match_all\": {} }}",
    uriParameters = SearchUriParameters(searchType = Some(Count)))

// To retrieve large numbers of documents (e.g. all the available documents) from elasticsearch efficiently without sorting,
// issue a search request with `Scan` search type, followed by some scroll requests, as per [this](https://www.elastic.co/guide/en/elasticsearch/guide/master/scan-scroll.html) reference document.
// A scroll timeout needs to be passed in both the initial search request and the following scroll requests:
val searchResponse = client.search(index = "foo", query = "{\"query\": { \"match_all\": {} }}",
    uriParameters = SearchUriParameters(scroll = Some("1m"), searchType = Some(Scan)))
val searchScrollId = // Extract _scroll_id from the search response body using your favourite JSON decoder library.
val firstScrollResponse = client.scroll("1m", searchScrollId)
val firstPageOfResults = // Extract results from the first scroll response.
val nextScrollId = // Extract _scroll_id from the first scroll response.
// Continue with subsequent scroll requests using the _scroll_id returned from previous requests until no more hits are returned.

// Fetch all the documents by ids.
// MGetUriParameters case class allows you to specify the source fields in result documents.
val mgetResponse = client.mget(index = "foo",`type` = Some("tweet"), query = "{\"ids\":[\"1\", \"2\"]}",
    uriParameters = MGetUriParameters(sourceFields = Seq("name", "age")))

// Validate a query.
client.validate(index = "foo", query = "{\"query\": { \"match_all\": {} }").map(_.getStatusCode) // Should be Future(200)

// Explain a query.
client.explain(index = "foo", `type` = "foo", id = "foo2", query = "{\"query\": { \"term\": { \"foo\":\"bar\"} } }")

// Suggestion possible term/phrase completions.
client.suggest(index = "foo", query = "{\"suggest\": {\"text\": \"bar\", \"completion\": {\"field\": \"foo\"} } }")

// Delete the document.
client.delete("foo", "foo", "foo")

// Delete by query, if you prefer
client.deleteByQuery(Seq("foo"), Seq.empty[String], "{ \"match_all\": {} }")

// Count the matches to a query
client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }").map(_.getResponseBody)

// Delete the index
client.deleteIndex("foo")

// Shut down the client. NOTE: This will also shutdown any other Dispatch-based
// HTTP stuff you have going on. Be careful!
Client.shutdown()
```
