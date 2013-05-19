# Wabisabi

Wabisabi is a Scala [ElasticSearch](http://www.elasticsearch.org/) client that
uses the REST API and has no dependency on ElasticSearch itself. It is
extremely minimal, eschewing any sort of parsing, formatting or other such
complication.

Wabisabi is based on the [dispatch](http://dispatch.databinder.net/Dispatch.html)
asynchronous HTTP library. Therefore, all of the returned values are
`Future[Response]`.

The returned object is a [Response](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/Response.html)
from the async-http-client library. Normally you'll want to use `getResponseBody`
to get the response but you can also check `getStatusCode` to verify something
didn't go awry.

## Dependencies

Depends on [dispatch](http://dispatch.databinder.net/Dispatch.html) and
[grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/). It's compiled for
scala 2.10.

## Notes

Version 2.0.0 switched to returning `Future[Response]` rather than `Future[String]`
so that errors can be handled.

This does not implement every piece of the ElasticSearch API. I will add other
bits as needed or as patches arrive.

# Using It

```
// Add the Dep
libraryDependencies += "wabisabi" %% "wabisabi" % "2.0.0"

// And a the resolver
resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/",
```

## Example

```
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

val client = new Client("http://localhost:9200")

// Get the cluster's health
client.health()

// Create the index
client.createIndex(name = "foo")

// Verify the index exists
client.verifyIndex("foo").getStatusCode // Should be 200!

// Add a document to the index.
client.index(
  index = "foo", `type` = "foo", id = Some("foo"),
  data = "{\"foo\":\"bar\"}", refresh = true
)

// Fetch that document by it's id.
client.get("foo", "foo", "foo").getResponseBody

// Search for all documents.
client.search("foo", "{\"query\": { \"match_all\": {} }").getResponseBody

// Validate a query.
client.validate(index = "foo", query = "{\"query\": { \"match_all\": {} }").getStatusCode // Should be 200

// Explain a query.
client.explain(index = "foo", `type` = "foo", id = "foo2", query = "{\"query\": { \"term\": { \"foo\":\"bar\"} } }")

// Delete the document.
client.delete("foo", "foo", "foo")

// Delete by query, if you prefer
client.deleteByQuery(Seq("foo"), Seq.empty[String], "{ \"match_all\": {} }")

// Count the matches to a query
client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }").getResponseBody

// Delete the index
client.deleteIndex("foo")
```
