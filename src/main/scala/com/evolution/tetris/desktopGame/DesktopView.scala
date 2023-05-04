package com.evolution.tetris.desktopGame

import com.evolution.tetris.service.{Figure, Presets}
import javafx.scene.shape.Rectangle
import scalafx.application.Platform
import scalafx.beans.property.IntegerProperty
import scalafx.scene.Group
import scalafx.scene.Group.sfxGroup2jfx
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.{Blue, Red}
import scalafx.scene.text.{Font, Text}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Random

class DesktopView {
  val presetsObject = new Presets()
  val fxSceneProtagonists = new Group()

  val scoreText: Text = new Text() {
    this.text = "SCORE: __"
    this.layoutX = 2
    this.layoutY = 15
    this.stroke = Blue
    this.font.value = new Font("Comic-sans", 13)
  }
  val score: IntegerProperty = new IntegerProperty() {
    onChange { (_, _, newValue) =>
      Platform.runLater(() -> {
        scoreText.setText(s"SCORE: ${newValue.toString} \n " +
          s"Assigned bonus simple figures: ${bonusFiguresQuantity.toInt}\n" +
          s" BONUS SCORE: ${bonusScore.toInt}\n" +
          s" Can use C-key to change a figure: ${presetsObject.presetsArrayOfPauseAndFiguresChoiceAndBreakThruAbilityAndBonusType(1)}\n" +
          s" Can get thru the row: ${presetsObject.presetsArrayOfPauseAndFiguresChoiceAndBreakThruAbilityAndBonusType(2)}")

      })
    }
  }
  val bonusScore: IntegerProperty = new IntegerProperty()
  val bonusFiguresQuantity: IntegerProperty = new IntegerProperty()

  def randomColor(): Color = scalafx.scene.paint.Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))

  def showTheFigureOnTheScene(figure: Figure): Unit = {
    for (i <- figure.shapeFormingBooleanMatrix.indices) {
      for (j <- figure.shapeFormingBooleanMatrix(i).indices) {
        if (figure.shapeFormingBooleanMatrix(i).nonEmpty) {
          if (figure.shapeFormingBooleanMatrix(i)(j)) {
            val rectangle = new Rectangle()
            rectangle.setX((figure.horizontalPosition + j) * presetsObject.figureCellScale)
            rectangle.setY((figure.verticalPosition + i) * presetsObject.figureCellScale)
            rectangle.setWidth(presetsObject.figureCellScale)
            rectangle.setHeight(presetsObject.figureCellScale)
            rectangle.setFill(figure.color)
            rectangle.setStroke(Red)
            rectangle.setArcHeight(2.4)
            sfxGroup2jfx(fxSceneProtagonists).getChildren.add(rectangle)
          }
        }
      }
    }
  }

  def showFallenFiguresAndCurrentFigure(fallenFiguresListBuffer: ListBuffer[Figure], currentFigureContainingArrayBuffer: ArrayBuffer[Figure]): Unit = {
    Platform.runLater(() -> {
      fxSceneProtagonists.getChildren.clear() //to clean up the traces from falling figures
      fallenFiguresListBuffer.foreach(showTheFigureOnTheScene)
      showTheFigureOnTheScene(currentFigureContainingArrayBuffer(0))
    })
  }
}
