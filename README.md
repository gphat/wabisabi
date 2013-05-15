# Wabisabi

Wabisabi is a REST [ElasticSearch](http://www.elasticsearch.org/) client. It is
extremely minimal, eschewing any sort of parsing, formatting or other such
complication.

Wabisabi is based on the [dispatch](http://dispatch.databinder.net/Dispatch.html)
asynchronous HTTP library. Therefore, all of the returned values are
`Future[String]`.

## Dependencies

Depends on [dispatch](http://dispatch.databinder.net/Dispatch.html) and
[grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/).

## Notes

This does not implement every piece of the ElasticSearch API. I will add other
bits as needed or as patches arrive.

# Using It

```
libraryDependencies += "wabisabi" %% "wabisabi" % "1.0.0"
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
client.verifyIndex("foo")

// Add a document to the index.
client.index(
  index = "foo", `type` = "foo", id = Some("foo"),
  data = "{\"foo\":\"bar\"}", refresh = true
)

// Fetch that document by it's id.
client.get("foo", "foo", "foo")

// Search for all documents.
client.search("foo", "{\"query\": { \"match_all\": {} }")

// Validate a query.
client.validate(index = "foo", query = "{\"query\": { \"match_all\": {} }")

// Explain a query.
client.explain(index = "foo", `type` = "foo", id = "foo2", query = "{\"query\": { \"term\": { \"foo\":\"bar\"} } }")

// Delete the document.
client.delete("foo", "foo", "foo")

// Delete by query, if you prefer
client.deleteByQuery(Seq("foo"), Seq.empty[String], "{ \"match_all\": {} }")

// Count the matches to a query
client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }")

// Delete the index
client.deleteIndex("foo")
```
