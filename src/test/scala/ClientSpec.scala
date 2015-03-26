package test

import org.specs2.matcher.JsonMatchers
import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

class ClientSpec extends Specification with JsonMatchers {

  sequential

  val server = new ElasticsearchEmbeddedServer

  step {
    server.start()
  }

  val testDuration = Duration(3, "second")

  "Client" should {

    def createIndex(client: Client)(index: String) = {
      Await.result(client.createIndex(name = index), testDuration).getResponseBody must contain("acknowledged")
      // Note: we cannot wait for green as the index, by default, will have a missing replica.
      // TODO: When defect #25 is fixed, we can specify that the index should have no replicas, and then we can wait for "green".
      Await.result(client.health(List(index), waitForStatus = Some("yellow"), timeout = Some("5s")), testDuration)
    }

    def index(client: Client)(index: String, `type`: String, id: String, data: Option[String] = None) = {
      Await.result(client.index(
        id = Some(id),
        index = index, `type` = `type`,
        data = data.getOrElse(s"""{"id":"$id"}"""), refresh = true
      ), testDuration).getResponseBody must contain("\"_version\"")
    }

    def deleteIndex(client: Client)(index: String) = {
      Await.result(client.deleteIndex(index), testDuration).getResponseBody must contain("\"acknowledged\"")
    }

    "fail usefully" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      val res = Await.result(client.verifyIndex("foobarbaz"), testDuration)
      res.getStatusCode must beEqualTo(404)
    }

    "create and delete indexes" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("foo")

      Await.result(client.verifyIndex("foo"), testDuration)

      deleteIndex(client)("foo")
    }

    "create and delete aliases" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("foo")

      Await.result(client.createAlias(actions = """{ "add": { "index": "foo", "alias": "foo-write" }}"""), testDuration).getResponseBody must contain("acknowledged")

      Await.result(client.getAliases(index = Some("foo")), testDuration).getResponseBody must contain("foo-write")

      Await.result(client.deleteAlias(index = "foo", alias = "foo-write"), testDuration).getResponseBody must contain("acknowledged")

      deleteIndex(client)("foo")
    }

    "put, get, and delete warmer" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("trogdor")

      Thread.sleep(100) //ES needs some time to make the index first
      Await.result(client.putWarmer(index = "trogdor", name = "fum", body = """{"query": {"match_all":{}}}"""), testDuration).getResponseBody must contain("acknowledged")

      Await.result(client.getWarmers("trogdor", "fu*"), testDuration).getResponseBody must contain("fum")

      Await.result(client.deleteWarmer("trogdor", "fum"), testDuration).getResponseBody must contain("acknowledged")

      Await.result(client.getWarmers("trogdor", "fu*"), testDuration).getResponseBody must not contain("fum")

      deleteIndex(client)("trogdor")
    }

    "index, fetch, and delete a document" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      Await.result(client.index(
        id = Some("foo"),
        index = "foo", `type` = "foo",
        data = "{\"foo\":\"bar₡\"}", refresh = true
      ), testDuration).getResponseBody must contain("\"_version\"")

      Await.result(client.get("foo", "foo", "foo"), testDuration).getResponseBody must contain("\"bar₡\"")
      Await.result(client.delete("foo", "foo", "foo"), testDuration).getResponseBody must contain("\"found\"")

      deleteIndex(client)("foo")
    }

    "get multiple documents" in {

      "with index and type" in {
        val client = new Client(s"http://localhost:${server.httpPort}")

        index(client)(index = "foo", `type` = "bar", id = "1")
        index(client)(index = "foo", `type` = "bar", id = "2")

        Await.result(client.mget(index = Some("foo"), `type` = Some("bar"), query =
          """
            |{
            | "ids" : ["1", "2"]
            |}
          """.stripMargin), testDuration).getResponseBody must / ("docs") /# 0 / ("found" -> "true") and
                                                                        / ("docs") /# 1 / ("found" -> "true")

        deleteIndex(client)("foo")
      }

      "with index" in {
        val client = new Client(s"http://localhost:${server.httpPort}")

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
          """.stripMargin), testDuration).getResponseBody must / ("docs") /# 0 / ("found" -> "true") and
                                                                        / ("docs") /# 1 / ("found" -> "true")

        deleteIndex(client)("foo")
      }

      "without index and type" in {
        val client = new Client(s"http://localhost:${server.httpPort}")

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
          """.stripMargin), testDuration).getResponseBody must / ("docs") /# 0 / ("found" -> "true") and
                                                                        / ("docs") /# 1 / ("found" -> "true")

        deleteIndex(client)("foo1")
        deleteIndex(client)("foo2")
      }
    }

    "search for a document" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      index(client)(index = "foo", `type` = "foo", id = "foo2", data = Some("{\"foo\":\"bar\"}"))

      Await.result(client.search("foo", "{\"query\": { \"match_all\": {} } }"), testDuration).getResponseBody must contain("\"foo2\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }"), testDuration).getResponseBody must contain("\"count\"")

      Await.result(client.delete("foo", "foo", "foo2"), testDuration).getResponseBody must contain("\"found\"")

      deleteIndex(client)("foo")
    }

    "multi-search" in {

      val client = new Client(s"http://localhost:${server.httpPort}")

      "with index and type" in {
        index(client)(index = "foo", `type` = "bar", id = "1", data = Some( """{"name": "Fred Smith"}"""))
        index(client)(index = "foo", `type` = "bar", id = "2", data = Some( """{"name": "Mary Jones"}"""))

        Await.result(client.msearch(index = Some("foo"), `type` = Some("bar"), query =
          """
            |{}
            |{"query" : {"match" : {"name": "Fred"}}}
            |{}
            |{"query" : {"match" : {"name": "Jones"}}}
          """.stripMargin), Duration(1, "second")).getResponseBody must
          /("responses") /# 0 / ("hits") / ("total" -> "1.0") and
          /("responses") /# 1 / ("hits") / ("total" -> "1.0")

        deleteIndex(client)("foo")
      }

      "with index" in {
        index(client)(index = "foo", `type` = "bar1", id = "1", data = Some( """{"name": "Fred Smith"}"""))
        index(client)(index = "foo", `type` = "bar2", id = "2", data = Some( """{"name": "Mary Jones"}"""))

        Await.result(client.msearch(index = Some("foo"), query =
          """
            |{"type": "bar1"}
            |{"query" : {"match" : {"name": "Fred"}}}
            |{"type": "bar2"}
            |{"query" : {"match" : {"name": "Jones"}}}
          """.stripMargin), Duration(1, "second")).getResponseBody must
          /("responses") /# 0 / ("hits") / ("total" -> "1.0") and
          /("responses") /# 1 / ("hits") / ("total" -> "1.0")

        deleteIndex(client)("foo")
      }

      "without index or type" in {
        index(client)(index = "foo1", `type` = "bar", id = "1", data = Some( """{"name": "Fred Smith"}"""))
        index(client)(index = "foo2", `type` = "bar", id = "2", data = Some( """{"name": "Mary Jones"}"""))

        Await.result(client.msearch(index = Some("foo"), query =
          """
            |{"index": "foo1"}
            |{"query" : {"match" : {"name": "Fred"}}}
            |{"index": "foo2"}
            |{"query" : {"match" : {"name": "Jones"}}}
          """.stripMargin), Duration(1, "second")).getResponseBody must
          /("responses") /# 0 / ("hits") / ("total" -> "1.0") and
          /("responses") /# 1 / ("hits") / ("total" -> "1.0")

        deleteIndex(client)("foo1")
        deleteIndex(client)("foo2")
      }
    }

    "delete a document by query" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      index(client)(index = "foo", `type` = "foo", id = "foo2", data = Some("{\"foo\":\"bar\"}"))

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} } }"), testDuration).getResponseBody must contain("\"count\":1")

      Await.result(client.deleteByQuery(Seq("foo"), Seq.empty[String], """{ "query": { "match_all" : { } } }"""), testDuration).getResponseBody must contain("\"successful\"")

      Await.result(client.count(Seq("foo"), Seq("foo"), "{\"query\": { \"match_all\": {} }"), testDuration).getResponseBody must contain("\"count\":0")

      deleteIndex(client)("foo")
    }

    "get settings" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      Await.result(client.createIndex(name = "replicas3",
        settings = Some( """{"settings": {"number_of_shards" : 1, "number_of_replicas": 3}}""")
      ), testDuration).getResponseBody must contain("acknowledged")

      // The tests start a single-node cluster and so the index can never be green.  Hence we only wait for "yellow".
      Await.result(client.health(List("replicas3"), waitForStatus = Some("yellow"), timeout = Some("5s")), testDuration)

      Await.result(client.getSettings(List("replicas3")), testDuration).getResponseBody must
        /("replicas3") /("settings") /("index") / ("number_of_replicas" -> "3")

      Await.result(client.deleteIndex("replicas3"), testDuration).getResponseBody must contain("acknowledged")
    }

    "properly manipulate mappings" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("foo")

      Await.result(client.putMapping(Seq("foo"), "foo", """{"foo": { "properties": { "message": { "type": "string", "store": true } } } }"""), testDuration).getResponseBody must contain("acknowledged")

      Await.result(client.verifyType("foo", "foo"), testDuration)

      Await.result(client.getMapping(Seq("foo"), Seq("foo")), testDuration).getResponseBody must contain("store")

      Await.result(client.putMapping(Seq("foo"), "foo",
        """{"foo": { "properties": { "message": { "type": "integer", "store": true } } } }""",
        ignoreConflicts = false), testDuration).getResponseBody must contain("MergeMappingException")

      Await.result(client.putMapping(Seq("foo"), "foo",
        """{"foo": { "properties": { "message": { "type": "integer", "store": true } } } }""",
        ignoreConflicts = true), testDuration).getResponseBody must contain("acknowledged")

      deleteIndex(client)("foo")
    }

    "suggest completions" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("music")

      Await.result(client.putMapping(Seq("music"), "song",
        """{
          |  "song" : {
          |        "properties" : {
          |            "name" : { "type" : "string" },
          |            "suggest" : { "type" : "completion",
          |                          "index_analyzer" : "simple",
          |                          "search_analyzer" : "simple",
          |                          "payloads" : true
          |            }
          |        }
          |    }
          |}
        """.stripMargin), testDuration)

      index(client)("music", "song", "1",
        Some("""{
          |    "name" : "Nevermind",
          |    "suggest" : {
          |        "input": [ "Nevermind", "Nirvana" ],
          |        "output": "Nirvana - Nevermind",
          |        "payload" : { "artistId" : 2321 },
          |        "weight" : 34
          |    }
          |}
        """.stripMargin))

      Await.result(client.suggest("music",
        """{
          |    "song-suggest" : {
          |        "text" : "n",
          |        "completion" : {
          |            "field" : "suggest"
          |        }
          |    }
          |}
        """.stripMargin), testDuration).getResponseBody must contain("Nirvana - Nevermind")

      deleteIndex(client)("music")
    }

    "validate and explain queries" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("foo")

      index(client)(index = "foo", `type` = "foo", id = "foo2", data = Some("{\"foo\":\"bar\"}"))

      Await.result(client.validate(index = "foo", query = "{\"query\": { \"match_all\": {} }"), testDuration).getResponseBody must contain("\"valid\"")

      Await.result(client.explain(index = "foo", `type` = "foo", id = "foo2", query = "{\"query\": { \"term\": { \"foo\":\"bar\"} } }"), testDuration).getResponseBody must contain("explanation")

      deleteIndex(client)("foo")
    }

    "handle health checking" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      Await.result(client.health(), testDuration).getResponseBody must contain("number_of_nodes")

      Await.result(client.health(level = Some("indices"), timeout = Some("5")), testDuration).getResponseBody must contain("number_of_nodes")
    }

    "handle stats checking" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("foo")
      createIndex(client)("bar")

      val res = Await.result(client.stats(), testDuration).getResponseBody
      res must contain("primaries")
      res must contain("_all")
      res must contain("indices")

      val fooRes = Await.result(client.stats(indices = Seq("foo")), testDuration).getResponseBody
      fooRes must contain("_all")
      fooRes must contain("indices")
      fooRes must contain("foo")
      fooRes must not contain("bar")

      val barRes = Await.result(client.stats(indices = Seq("bar")), testDuration).getResponseBody
      barRes must contain("_all")
      barRes must contain("indices")
      barRes must contain("bar")
      barRes must not contain("foo")

      deleteIndex(client)("foo")
      deleteIndex(client)("bar")
    }

    "handle refresh" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      createIndex(client)("test")

      val res = Await.result(client.refresh("test"), testDuration).getResponseBody
      res must contain("\"successful\"")

      deleteIndex(client)("test")
    }

    "handle bulk requests" in {
      val client = new Client(s"http://localhost:${server.httpPort}")

      val res = Await.result(client.bulk(data = """{ "index" : { "_index" : "test", "_type" : "type1", "_id" : "1" } }
{ "field1" : "value1" }
{ "delete" : { "_index" : "test", "_type" : "type1", "_id" : "2" } }
{ "create" : { "_index" : "test", "_type" : "type1", "_id" : "3" } }
{ "field1" : "value3" }
{ "update" : {"_id" : "1", "_type" : "type1", "_index" : "index1"} }
{ "doc" : {"field2" : "value2"} }"""), testDuration).getResponseBody
      res must contain("\"status\":201")

      deleteIndex(client)("test")
    }
  }

  step {
    server.stop()
  }
}
