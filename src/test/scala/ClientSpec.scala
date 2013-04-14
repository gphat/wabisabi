package test

import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration._
import wabisabi._

class ClientSpec extends Specification {

  sequential

  "Client" should {
    "create and delete indexes" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to create index: " + x.getMessage)
        case Right(body) => body must contain("acknowledged")
      }

      Await.result(client.verifyIndex("foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to verify index: " + x.getMessage)
        case Right(body) => {
          // Nothing to do, as it just returns 2XX on success
        }
      }

      Await.result(client.deleteIndex("foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to delete index: " + x.getMessage)
        case Right(body) => body must contain("acknowledged")
      }

      1 must beEqualTo(1)
    }

    "index and fetch a document" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.index(
        index = "foo", `type` = "foo", id = Some("foo"),
        data = "{\"foo\":\"bar\"}", refresh = true
      ), Duration(1, "second")) match {
        case Left(x) => failure("Failed to index: " + x.getMessage)
        case Right(body) => body must contain("\"_version\"")
      }

      Await.result(client.get("foo", "foo", "foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to get: " + x.getMessage)
        case Right(body) => body must contain("\"foo\"")
      }

      Await.result(client.delete("foo", "foo", "foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to delete: " + x.getMessage)
        case Right(body) => body must contain("\"found\"")
      }

      1 must beEqualTo(1)
    }

    "index and search for a document" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.index(
        index = "foo", `type` = "foo", id = Some("foo2"),
        data = "{\"foo\":\"bar\"}", refresh = true
      ), Duration(1, "second")) match {
        case Left(x) => failure("Failed to index: " + x.getMessage)
        case Right(body) => body must contain("\"_version\"")
      }

      Await.result(client.search("foo", "{\"query\": { \"match_all\": {} }"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to search: " + x.getMessage)
        case Right(body) => body must contain("\"foo2\"")
      }

      Await.result(client.delete("foo", "foo", "foo2"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to delete: " + x.getMessage)
        case Right(body) => body must contain("\"found\"")
      }

      Await.result(client.deleteIndex("foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to delete index: " + x.getMessage)
        case Right(body) => body must contain("acknowledged")
      }

      1 must beEqualTo(1)
    }

    "properly manipulate mappings" in {
      val client = new Client("http://localhost:9200")

      Await.result(client.createIndex(name = "foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to create index: " + x.getMessage)
        case Right(body) => body must contain("acknowledged")
      }

      Await.result(client.putMapping(Seq("foo"), "foo", "{\"tweet\": { \"properties\": { \"message\": { \"type\": \"string\", \"store\": \"yes\" } } } }"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to create mapping: " + x.getMessage)
        case Right(body) => body must contain("acknowledged")
      }

      Await.result(client.verifyType("foo", "foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to verify type: " + x.getMessage)
        case Right(body) => {
          // Nothing to do, as it just returns 2XX on success
        }
      }

      Await.result(client.getMapping(Seq("foo"), Seq("foo")), Duration(1, "second")) match {
        case Left(x) => failure("Failed to create mapping: " + x.getMessage)
        case Right(body) => body must contain("store")
      }

      Await.result(client.deleteIndex("foo"), Duration(1, "second")) match {
        case Left(x) => failure("Failed to delete index: " + x.getMessage)
        case Right(body) => body must contain("acknowledged")
      }

      1 must beEqualTo(1)
    }
  }
}