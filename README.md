# Wabisabi

Wabisabi is a REST [ElasticSearch](http://www.elasticsearch.org/) client. It is
extremely minimal, eschewing any sort of parsing, formatting or other such
complication.

Wabisabi is based on the [dispatch](http://dispatch.databinder.net/Dispatch.html)
asynchronous HTTP library. It is, therefore, somewhat different to use as all
of the returned values are `Future[Either[Throwable,String]]`.

## Notes

This library is dreadfully incomplete. It's also possibly wrong, as maybe I'll
change my mind.  But it is spiritually about right, imposing almost no choices
– save `Future` and `Either` – on the user.

## Example

```
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

val client = new Client("http://localhost:9200")

// Create the index
Await.result(client.createIndex(name = "foo"), Duration(1, "second")) match {
  case Left(x) => failure("Failed to create index: " + x.getMessage)
  case Right(body) => println("Worked: " + body)
}

// Verify the index exists
Await.result(client.verifyIndex("foo"), Duration(1, "second")) match {
  case Left(x) => failure("Failed to verify index: " + x.getMessage)
  case Right(body) => {
    // Nothing to do, as it just returns 2XX on success
  }
}

// Add a document to the index.
Await.result(client.index(
  index = "foo", `type` = "foo", id = Some("foo"),
  data = "{\"foo\":\"bar\"}", refresh = true
), Duration(1, "second")) match {
  case Left(x) => println("Failed to index: " + x.getMessage)
  case Right(body) => println("Worked: " + body)
}

// Fetch that document by it's id.
Await.result(client.get("foo", "foo", "foo"), Duration(1, "second")) match {
  case Left(x) => println("Failed to index: " + x.getMessage)
  case Right(body) => println("Worked: " + body)
}

// Search for all documents.
Await.result(client.search("foo", "{\"query\": { \"match_all\": {} }"), Duration(1, "second")) match {
  case Left(x) => failure("Failed to search: " + x.getMessage)
  case Right(body) => println("Worked: " + body)
}

// Delete the document.
Await.result(client.delete("foo", "foo", "foo"), Duration(1, "second")) match {
  case Left(x) => println("Failed to index: " + x.getMessage)
  case Right(body) => println("Worked: " + body)
}

// Delete the index
Await.result(client.deleteIndex("foo"), Duration(1, "second")) match {
  case Left(x) => failure("Failed to delete index: " + x.getMessage)
  case Right(body) => println("Worked: " + body)
}


```

