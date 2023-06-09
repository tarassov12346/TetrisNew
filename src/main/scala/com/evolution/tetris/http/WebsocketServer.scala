package com.evolution.tetris.http

import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.effect.{ExitCode, IO, Resource}
import com.comcast.ip4s._
import com.evolution.tetris.main.MainHttp.db
import com.typesafe.config.{Config, ConfigFactory}
import fs2.{Pipe, Stream}
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.{HttpRoutes, _}

class WebsocketServer {

  object WebSocketServer {
    // Let's build a WebSocket server using Http4s.
    private def dbRoute(wsb: WebSocketBuilder2[IO],playerDao: db.PlayerDao)=HttpRoutes.of[IO] {

      // websocat "ws://localhost:9002/echo"
      case GET -> Root / "db" =>
        // Pipe is a stream transformation function of type `Stream[F, I] => Stream[F, O]`. In this case
        // `I == O == WebSocketFrame`. So the pipe transforms incoming WebSocket messages from the client to
        // outgoing WebSocket messages to send to the client.
        val dbPipe: Pipe[IO, WebSocketFrame, WebSocketFrame] =
          _.collect { case WebSocketFrame.Text(message, _) =>
            val f =playerDao.from(ConfigFactory.load()).unsafeRunSync().find(message).unsafeRunSync().sortWith((x, y) => x.score > y.score).head
            WebSocketFrame.Text(f.toString)
          }
        for {
          // Unbounded queue to store WebSocket messages from the client, which are pending to be processed.
          // For production use bounded queue seems a better choice. Unbounded queue may result in out of
          // memory error, if the client is sending messages quicker than the server can process them.
          queue <- Queue.unbounded[IO, WebSocketFrame]
          response <- wsb.build(
            // Sink, where the incoming WebSocket messages from the client are pushed to.
            receive = _.evalMap(queue.offer),
            // Outgoing stream of WebSocket messages to send to the client.
            send = Stream.repeatEval(queue.take).through(dbPipe),
          )
        } yield response
    }

    private def httpApp(wsb: WebSocketBuilder2[IO],playerDao: db.PlayerDao): HttpApp[IO] = {
      dbRoute(wsb, playerDao)
    }.orNotFound

    def run(config: Config, playerDao: db.PlayerDao): Resource[IO, ExitCode] = {
      val hostString = config.getString("myServer.host.value")
      val portString = config.getString("myServer.port.value")
      for {
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(Host.fromString(hostString).get)
          .withPort(Port.fromString(portString).get)
          .withHttpWebSocketApp(httpApp(_,playerDao))
          .build
      } yield ExitCode.Success
    }
  }
}
