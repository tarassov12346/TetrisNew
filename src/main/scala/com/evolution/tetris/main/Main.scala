package com.evolution.tetris.main

import cats.effect.{ExitCode, IO, IOApp}
import com.evolution.tetris.http.{TetrisGame, WebsocketServer}
import com.evolution.tetris.main.MainHttp.db
import com.typesafe.config.ConfigFactory

object Main extends IOApp {
  val wb = new WebsocketServer()

  override def run(args: List[String]): IO[ExitCode] = {
    println("Enter your name:")
    val playerName = scala.io.StdIn.readLine()
    val http = new TetrisGame(playerName)
    for {
      _ <- http.start()
      config <- IO(ConfigFactory.load())
      playerDao <- db.PlayerDao.from(config)
      _ <- wb.WebSocketServer.run(config, playerDao).useForever

    } yield ExitCode.Success
  }
}
