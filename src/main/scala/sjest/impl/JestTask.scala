package sjest.impl

import org.scalajs.testinterface.TestUtils
import sbt.testing._
import sjest.support.VisibleForTest
import sjest.{JestSuite, TestFrameworkConfig}

import scala.scalajs.reflect.Reflect
import scala.util.{Failure, Success, Try}

private class JestTask(override val taskDef: TaskDef,
                       jsTestConverter: JsTestConverter,
                       nodejsTest: NodejsTest)
                      (implicit config: TestFrameworkConfig) extends sbt.testing.Task {
  override def tags(): Array[String] = Array("jest-test-task")

  override def execute(eventHandler: EventHandler,
                       loggers: Array[Logger]): Array[Task] = {
    val resultEvent = this.executeImpl(loggers)
    eventHandler.handle(resultEvent)
    Array.empty
  }

  override def execute(eventHandler: EventHandler,
                       loggers: Array[Logger],
                       continuation: Array[Task] => Unit): Unit = {
    this.execute(eventHandler, loggers)
    continuation(Array.empty)
  }

  @VisibleForTest
  private[sjest] def executeImpl(loggers: Array[Logger]): Event = {
    implicit val _taskDef: TaskDef = taskDef

    val suite = this.loadJestSuite

    val jsTestCase = suite.getTestCase(taskDef)
    val jsTestPath = jsTestConverter.generateJsTest(jsTestCase)

    val event = if (config.autoRunTestInSbt)
      nodejsTest.runTest(jsTestPath, loggers)
    else {
      loggers.foreach(_.info("*.test.js files are generated," +
        " manually run the tests because auto run has been disabled"))
      JestTestEvent(Status.Ignored)
    }
    event
  }

  private def loadJestSuite: JestSuite = {
    val fqcn = taskDef.fullyQualifiedName()
    Try {
      Reflect.lookupInstantiatableClass(fqcn) match{
        case Some(clazz) => clazz.newInstance().asInstanceOf[JestSuite]
        case None => throw new ClassNotFoundException(fqcn)
      }
    } match {
      case Success(suite) => suite
      case Failure(t) =>
        throw new IllegalArgumentException(s"Cannot load suite for name: $fqcn", t)
    }
  }
}
