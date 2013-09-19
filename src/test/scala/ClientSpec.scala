package test

import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

class ClientSpec extends Specification {

  sequential

  "Client" should {

    "fail usefully" in {
      val client = new Client("http://localhost:9200")

      val res = Await.result(client.verifyIndex("foobarbaz"), Duration(1, "second"))
      res.getStatusCode must beEqualTo(404)

      1 must beEqualTo(1)
    }

    "create and delete indexes" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.verifyIndex("foo"), Duration(1, "second"))

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      1 must beEqualTo(1)
    }

    "create and delete aliases" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.createAlias(actions = """{ "add": { "index": "foo", "alias": "foo-write" }}"""), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.getAliases(index = Some("foo")), Duration(1, "second")).getResponseBody must contain("foo-write")

      Await.result(client.deleteAlias(index = "foo", alias = "foo-write"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      1 must beEqualTo(1)
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

      1 must beEqualTo(1)
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

      1 must beEqualTo(1)
    }

    "delete a document by query" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.index(
        index = "foo", `type` = "foo", id = Some("foo2"),
        data = "{\"foo\":\"bar\"}", refresh = true
      ), Duration(1, "second")).getResponseBody must contain("\"_version\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }"), Duration(1, "second")).getResponseBody must contain("\"count\":1")

      Await.result(client.deleteByQuery(Seq("foo"), Seq.empty[String], "{ \"match_all\": {} }"), Duration(1, "second")).getResponseBody must contain("\"successful\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }"), Duration(1, "second")).getResponseBody must contain("\"count\":0")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      1 must beEqualTo(1)
    }

    "properly manipulate mappings" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.putMapping(Seq("foo"), "foo", "{\"tweet\": { \"properties\": { \"message\": { \"type\": \"string\", \"store\": \"yes\" } } } }"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      Await.result(client.verifyType("foo", "foo"), Duration(1, "second"))

      Await.result(client.getMapping(Seq("foo"), Seq("foo")), Duration(1, "second")).getResponseBody must contain("store")

      Await.result(client.deleteIndex("foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      1 must beEqualTo(1)
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

      1 must beEqualTo(1)
    }

    "handle health checking" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.health(), Duration(1, "second")).getResponseBody must contain("number_of_nodes")

      Await.result(client.health(level = Some("indices"), timeout = Some("5")), Duration(1, "second")).getResponseBody must contain("number_of_nodes")

      1 must beEqualTo(1)
    }

    "handle stats checking" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")).getResponseBody must contain("acknowledged")
      Await.result(client.createIndex(name = "bar"), Duration(1, "second")).getResponseBody must contain("acknowledged")

      val res = Await.result(client.stats(), Duration(1, "second")).getResponseBody
      res must contain("\"ok\":true")
      res must contain("_all")
      res must contain("indices")
      res must contain("foo")
      res must contain("bar")

      val fooRes = Await.result(client.stats(indices = Seq("foo")), Duration(1, "second")).getResponseBody
      fooRes must contain("\"ok\":true")
      fooRes must contain("_all")
      fooRes must contain("indices")
      fooRes must contain("foo")
      fooRes must not contain("bar")

      val barRes = Await.result(client.stats(indices = Seq("bar")), Duration(1, "second")).getResponseBody
      barRes must contain("\"ok\":true")
      barRes must contain("_all")
      barRes must contain("indices")
      barRes must contain("bar")
      barRes must not contain("foo")
    }
  }
}