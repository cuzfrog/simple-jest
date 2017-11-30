package sjest

import nodejs.ChildProcess
import org.scalajs.testinterface.TestUtils
import sbt.testing._

import scala.concurrent.duration.Deadline
import scala.util.control.NonFatal

private class JestTask(override val taskDef: TaskDef,
                       testClassLoader: ClassLoader)
                      (implicit config: TestFrameworkConfig) extends sbt.testing.Task {
  override def tags(): Array[String] = Array("jest-test-task")

  override def execute(eventHandler: EventHandler,
                       loggers: Array[Logger]): Array[Task] = {
    implicit val _taskDef: TaskDef = taskDef

    val suite =
      TestUtils.loadModule(taskDef.fullyQualifiedName, testClassLoader).asInstanceOf[JestSuite]

    val jsTestCase = suite.getTestCase(taskDef)
    val jsTestPath = JsTestConverter.generateJsTest(jsTestCase)

    val resultEvent =
      if (config.autoRunTestInSbt) runNpmTest(jsTestPath, loggers)
      else {
        loggers.foreach(_.info("---jest--- test.js files are generated," +
          " manually run the tests because auto run has been disabled"))
        JestTestEvent(Status.Success)
      }
    eventHandler.handle(resultEvent)
    Array.empty
  }

  override def execute(eventHandler: EventHandler,
                       loggers: Array[Logger],
                       continuation: Array[Task] => Unit): Unit = {
    this.execute(eventHandler, loggers)
    continuation(Array.empty)
  }

  private def runNpmTest(jsTestPath: String,
                         loggers: Array[Logger])(implicit taskDef: TaskDef): Event = {
    val startTime = Deadline.now
    val event = try {
      loggers.foreach(_.info(s"---jest--- Testing against: ${taskDef.fullyQualifiedName}"))
      val childProcess = ChildProcess.spawnSync(config.npmCmdOfPath(jsTestPath)) //run code with nodejs
      val output = Option(childProcess.output)
      output.foreach(out => loggers.foreach(_.info(out.toString)))
      loggers.foreach(_.info(s"PASS ${taskDef.fullyQualifiedName}"))
      JestTestEvent(Status.Success)
    } catch {
      case NonFatal(t) =>
        loggers.foreach(_.error(s"test failed with $t}"))
        JestTestEvent(Status.Failure)
    }

    val duration = (Deadline.now - startTime).toMillis
    event.copy(duration = duration)
  }
}
