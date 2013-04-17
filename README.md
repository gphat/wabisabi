# Wabisabi

Wabisabi is a REST [ElasticSearch](http://www.elasticsearch.org/) client. It is
extremely minimal, eschewing any sort of parsing, formatting or other such
complication.

Wabisabi is based on the [dispatch](http://dispatch.databinder.net/Dispatch.html)
asynchronous HTTP library. It is, therefore, somewhat different to use as all
of the returned values are `Future[String]`.

## Notes

This library is dreadfully incomplete. It's also possibly wrong, as maybe I'll
change my mind.  But it is spiritually about right, imposing almost no choices
– save `Future` – on the user.

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

