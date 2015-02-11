package test

import org.specs2.matcher.JsonMatchers
import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

class ClientSpec extends Specification with JsonMatchers {

  sequential

  "Client" should {

    "fail usefully" in {
      val client = new Client("http://localhost:9200")

      val res = Await.result(client.verifyIndex("foobarbaz"), Duration(1, "second"))
      res.getStatusCode must beEqualTo(404)
    }

    "create and delete indexes" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.verifyIndex("foo"), Duration(1, "second"))

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "create and delete aliases" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.createAlias(actions = """{ "add": { "index": "foo", "alias": "foo-write" }}"""), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.getAliases(index = Some("foo")), Duration(1, "second")).getResponseBody must contain("foo-write")

      Await.result(client.deleteAlias(index = "foo", alias = "foo-write"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "index and fetch a document" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.index(
        id = Some("foo"),
        index = "foo", `type` = "foo",
        data = "{\"foo\":\"bar₡\"}", refresh = true
      ), Duration(1, "second")).getResponseBody must contain("\"_version\"")

      Await.result(client.get("foo", "foo", "foo"), Duration(1, "second")).getResponseBody must contain("\"bar₡\"")

      Await.result(client.delete("foo", "foo", "foo"), Duration(1, "second")).getResponseBody must contain("\"found\"")
    }

    "get multiple documents" in {

      def index(client: Client)(index: String, `type`: String, id: String) = {
        Await.result(client.index(
          id = Some(id),
          index = index, `type` = `type`,
          data = s"""{"id":"$id"}""", refresh = true
        ), Duration(1, "second")).getResponseBody must contain("\"_version\"")
      }

      def delete(client: Client)(index: String, `type`: String, id: String) = {
        Await.result(client.delete(index, `type`, id), Duration(1, "second")).getResponseBody must contain("\"found\"")
      }

      "with index and type" in {
        val client = new Client("http://localhost:9200")

        index(client)(index = "foo", `type` = "bar", id = "1")
        index(client)(index = "foo", `type` = "bar", id = "2")

        Await.result(client.mget(index = Some("foo"), `type` = Some("bar"), query =
          """
            |{
            | "ids" : ["1", "2"]
            |}
          """.stripMargin), Duration(1, "second")).getResponseBody must / ("docs") /# 0 / ("found" -> "true") and
                                                                        / ("docs") /# 1 / ("found" -> "true")

        delete(client)("foo", "bar", "1")
        delete(client)("foo", "bar", "2")
      }

      "with index" in {
        val client = new Client("http://localhost:9200")

        index(client)(index = "foo", `type` = "bar1", id = "1")
        index(client)(index = "foo", `type` = "bar2", id = "2")

        Await.result(client.mget(index = Some("foo"), `type` = None, query =
          """{
            |  "docs" : [
            |    {
            |      "_type" : "bar1",
            |      "_id" : "1"
            |    },
            |    {
            |      "_type" : "bar2",
            |      "_id" : "2"
            |    }
            |  ]
            |}
          """.stripMargin), Duration(1, "second")).getResponseBody must / ("docs") /# 0 / ("found" -> "true") and
                                                                        / ("docs") /# 1 / ("found" -> "true")

        delete(client)("foo", "bar1", "1")
        delete(client)("foo", "bar2", "2")
      }

      "without index and type" in {
        val client = new Client("http://localhost:9200")

        index(client)(index = "foo1", `type` = "bar1", id = "1")
        index(client)(index = "foo2", `type` = "bar2", id = "2")

        Await.result(client.mget(index = None, `type` = None, query =
          """{
            |  "docs" : [
            |    {
            |      "_index" : "foo1",
            |      "_type" : "bar1",
            |      "_id" : "1"
            |    },
            |    {
            |      "_index" : "foo2",
            |      "_type" : "bar2",
            |      "_id" : "2"
            |    }
            |  ]
            |}
          """.stripMargin), Duration(1, "second")).getResponseBody must / ("docs") /# 0 / ("found" -> "true") and
                                                                        / ("docs") /# 1 / ("found" -> "true")

        delete(client)("foo1", "bar1", "1")
        delete(client)("foo2", "bar2", "2")
      }
    }

    "index and search for a document" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.index(
        index = "foo", `type` = "foo", id = Some("foo2"),
        data = "{\"foo\":\"bar\"}", refresh = true
      ), Duration(1, "second")).getResponseBody must contain("\"_version\"")

      Await.result(client.search("foo", "{\"query\": { \"match_all\": {} } }"), Duration(1, "second")).getResponseBody must contain("\"foo2\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }"), Duration(1, "second")).getResponseBody must contain("\"count\"")

      Await.result(client.delete("foo", "foo", "foo2"), Duration(1, "second")).getResponseBody must contain("\"found\"")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "delete a document by query" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.index(
        index = "foo", `type` = "foo", id = Some("foo2"),
        data = "{\"foo\":\"bar\"}", refresh = true
      ), Duration(1, "second")).getResponseBody must contain("\"_version\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} } }"), Duration(1, "second")).getResponseBody must contain("\"count\":1")

      Await.result(client.deleteByQuery(Seq("foo"), Seq.empty[String], """{ "query": { "match_all" : { } } }"""), Duration(1, "second")).getResponseBody must contain("\"successful\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }"), Duration(1, "second")).getResponseBody must contain("\"count\":0")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "properly manipulate mappings" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.putMapping(Seq("foo"), "foo", """{"foo": { "properties": { "message": { "type": "string", "store": true } } } }"""), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.verifyType("foo", "foo"), Duration(1, "second"))

      Await.result(client.getMapping(Seq("foo"), Seq("foo")), Duration(1, "second")).getResponseBody must contain("store")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "validate and explain queries" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.index(
        index = "foo", `type` = "foo", id = Some("foo2"),
        data = "{\"foo\":\"bar\"}", refresh = true
      ), Duration(1, "second")).getResponseBody must contain("\"_version\"")

      Await.result(client.validate(index = "foo", query = "{\"query\": { \"match_all\": {} }"), Duration(1, "second")).getResponseBody must contain("\"valid\"")

      Await.result(client.explain(index = "foo", `type` = "foo", id = "foo2", query = "{\"query\": { \"term\": { \"foo\":\"bar\"} } }"), Duration(1, "second")).getResponseBody must contain("explanation")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "handle health checking" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.health(), Duration(1, "second")).getResponseBody must contain("number_of_nodes")

      Await.result(client.health(level = Some("indices"), timeout = Some("5")), Duration(1, "second")).getResponseBody must contain("number_of_nodes")
    }

    "handle stats checking" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
      Await.result(client.createIndex(name = "bar"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      val res = Await.result(client.stats(), Duration(1, "second")).getResponseBody
      res must contain("primaries")
      res must contain("_all")
      res must contain("indices")

      val fooRes = Await.result(client.stats(indices = Seq("foo")), Duration(1, "second")).getResponseBody
      fooRes must contain("_all")
      fooRes must contain("indices")
      fooRes must contain("foo")
      fooRes must not contain("bar")

      val barRes = Await.result(client.stats(indices = Seq("bar")), Duration(1, "second")).getResponseBody
      barRes must contain("_all")
      barRes must contain("indices")
      barRes must contain("bar")
      barRes must not contain("foo")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
      Await.result(client.deleteIndex("bar"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }

    "handle bulk requests" in {
      val client = new Client("http://localhost:9200")

      val res = Await.result(client.bulk(data = """{ "index" : { "_index" : "test", "_type" : "type1", "_id" : "1" } }
{ "field1" : "value1" }
{ "delete" : { "_index" : "test", "_type" : "type1", "_id" : "2" } }
{ "create" : { "_index" : "test", "_type" : "type1", "_id" : "3" } }
{ "field1" : "value3" }
{ "update" : {"_id" : "1", "_type" : "type1", "_index" : "index1"} }
{ "doc" : {"field2" : "value2"} }"""), Duration(1, "second")).getResponseBody
      res must contain("\"status\":201")

      Await.result(client.deleteIndex("test"), Duration(1, "second")).getResponseBody must contain("acknowledged")
    }
  }
}
