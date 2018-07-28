import org.openqa.selenium.phantomjs.{PhantomJSDriver, PhantomJSDriverService}
import org.openqa.selenium.remote.{Command, DesiredCapabilities, Response}
import org.openqa.selenium.{Dimension, OutputType, By, WebElement}
import org.openqa.selenium.logging.LogType
import java.nio.file.{Files, Paths}
import java.nio.file.StandardCopyOption._
import java.nio.file.StandardOpenOption._
import java.nio.charset.Charset
import java.net.URLEncoder
import java.util.concurrent.TimeUnit._
import scala.collection.JavaConverters._
import scala.io.Source

case class PhantomJS() {
  private[this] val caps = new DesiredCapabilities
  caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, Array("--ssl-protocol=any", "--ignore-ssl-errors=yes", "--webdriver-loglevel=NONE", "--disk-cache=true", "--max-disk-cache-size=10485760"))
  // caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, Array("--ssl-protocol=any", "--ignore-ssl-errors=yes", "--webdriver-loglevel=INFO", "--disk-cache=true", "--max-disk-cache-size=10485760", "--debug=true"))
  caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.90 Safari/537.36")
  caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages", false)
  caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "resourceTimeout", 3 * 1000)
  // caps.setCapability("takesScreenshot", false)

  val driver = new PhantomJSDriver(caps)
  driver.manage.window.setSize(new Dimension(1080, 1920))
  driver.manage.timeouts.pageLoadTimeout(120, SECONDS)
  driver.manage.timeouts.setScriptTimeout(1, SECONDS)

  def close(): Unit = driver.quit
}

object PhantomJSDemo extends App {
  private def debug(url: String): Unit = {
    use(PhantomJS()){ phantomjs =>
      print(url)
      print("get")
      phantomjs.driver.get(url)
      print("getSessionId")
      val sid = phantomjs.driver.getSessionId
      print("new Command")
      val cmd = new Command(sid, "getPageSource", null)
      print(cmd.toString)
      print("getPageSource")
      print(phantomjs.driver.getCommandExecutor.execute(cmd).getValue.toString)
      // print(phantomjs.driver.getPageSource)
    }
  }

  private def getHtmlText(url: String, prefix: String, index: Long): Unit = {
    use(PhantomJS()){ phantomjs =>
      phantomjs.driver.get(url)

      val text = getText(phantomjs.driver)

      Files.write(
        Paths.get(s"classify/${prefix}_%04d.txt".format(index)),
        List(text).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
    }
  }

  private def getScreenshotAndHtmlAndLinkText(url: String): Unit = {
    use(PhantomJS()){ phantomjs =>
      phantomjs.driver.get(url)

      val encodedUrl = URLEncoder.encode(url)
      takeScreecshot(phantomjs.driver, s"/tmp/${encodedUrl}.png")

      // print(phantomjs.driver.getPageSource)
      // findRectElements(phantomjs.driver).foreach(printElementInfo)
      // printLogs(phantomjs.driver)
      // printResponse(phantomjs.driver)

      val texts = getTexts(phantomjs.driver, url)
      // printTexts(texts)

      val rate = getLinkTextRate(texts)
      // printLinkTextRate(url, getTextLength(texts), getLinkTextLength(texts), rate)

      val prefix = if (rate > 0.6) "patchy" else "non_patchy"

      Files.move(Paths.get(s"/tmp/${encodedUrl}.png"), Paths.get(s"classify/${prefix}_${encodedUrl}.png"), REPLACE_EXISTING)
      Files.write(
        Paths.get(s"classify/${prefix}_${encodedUrl}.html"),
        List(phantomjs.driver.getPageSource).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
      Files.write(
        Paths.get(s"classify/${prefix}_${encodedUrl}.txt"),
        texts.keys.map(x => texts(x)._1).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
      Files.write(
        Paths.get(s"classify/${prefix}_${encodedUrl}_link.txt"),
        texts.keys.map(x => texts(x)._2).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
      Files.write(
        Paths.get(s"classify/${prefix}_${encodedUrl}_rate.txt"),
        List(url, "text length: " + getTextLength(texts), "link text length: " + getLinkTextLength(texts), "rate: " + rate).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
    }
  }

  private def getScreenshotAndLinkText(url: String): Unit = {
    use(PhantomJS()){ phantomjs =>
      phantomjs.driver.get(url)

      val encodedUrl = URLEncoder.encode(url)
      takeScreecshot(phantomjs.driver, s"urls/${encodedUrl}.png")

      val texts = getTexts(phantomjs.driver, url)

      Files.write(
        Paths.get(s"urls/${encodedUrl}.html"),
        List(phantomjs.driver.getPageSource).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
      Files.write(
        Paths.get(s"urls/${encodedUrl}.txt"),
        texts.keys.map(x => texts(x)._1).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
      Files.write(
        Paths.get(s"urls/${encodedUrl}_link.txt"),
        texts.keys.map(x => texts(x)._2).asJava,
        Charset.forName("UTF-8"),
        CREATE, WRITE, TRUNCATE_EXISTING
      )
    }
  }

  private def takeScreecshot(driver: PhantomJSDriver, file: String): Unit = {
    val ss = driver.getScreenshotAs(OutputType.FILE)
    Files.copy(Paths.get(ss.toURI), Paths.get(file), REPLACE_EXISTING)
  }

  private def print(msgs: String*): Unit = {
    println(msgs.mkString(
      "----------------------------------------------------------------------\n",
      "\n",
      "\n----------------------------------------------------------------------"
    ))
  }

  private def printElementInfo(elem: WebElement): Unit = {
    val dimension = elem.getSize
    print(
      s"tag: ${elem.getTagName}",
      s"location: ${elem.getLocation}",
      s"width: ${dimension.width}, height: ${dimension.height}",
      (if (elem.isDisplayed) "displayed" else "not displayed"),
      elem.getText
    )
  }

  private def findRectElements(driver: PhantomJSDriver): Seq[WebElement] = {
    // driver.findElementsByXPath("//div | //iframe").asScala.filter(_.findElements(new By.ByXPath("ancestor::div | ancestor::iframe")).size == 0)
    driver.findElementsByXPath("//div [not(ancestor::div) and not(ancestor::iframe)] | //iframe [not(ancestor::div) and not(ancestor::iframe)]").asScala
  }

  private def printLogs(driver: PhantomJSDriver): Unit = {
    driver.manage.logs.getAvailableLogTypes.asScala.foreach{ x =>
      print(s"[${x}]\n" + driver.manage.logs.get(x).getAll.asScala.map(_.getMessage).mkString("\n"))
    }
  }

  private def printResponse(driver: PhantomJSDriver): Unit = {
    val res = new Response(driver.getSessionId)
    print(res.toString)
  }

  private def getText(driver: PhantomJSDriver): String = {
    driver.findElementByXPath("/html/body").getText
  }

  private def getLinkText(driver: PhantomJSDriver): String = {
    driver.findElementsByXPath("//a").asScala.map(_.getText).filterNot(_.isEmpty).mkString("\n")
  }

  private def findIframeSrcs(driver: PhantomJSDriver): Seq[String] = {
    driver.findElementsByXPath("//iframe[@src]").asScala.map(_.getAttribute("src"))
  }

  private def getTexts(driver: PhantomJSDriver, url: String): Map[String, (String, String)] = {
    Map(url -> (getText(driver), getLinkText(driver))) ++ findIframeSrcs(driver).map{ x =>
      driver.get(x)
      (x -> (getText(driver), getLinkText(driver)))
    }
  }

  private def printTexts(texts: Map[String, (String, String)]): Unit = {
    texts.keys.foreach{ x =>
      print(
        s"[${x}]",
        "text", texts(x)._1,
        "--------------------",
        "link text", texts(x)._2
      )
    }
  }

  private def getTextLength(texts: Map[String, (String, String)]): Int = {
    texts.keys.map(x => texts(x)._1.length).foldLeft(0)(_ + _)
  }

  private def getLinkTextLength(texts: Map[String, (String, String)]): Int = {
    texts.keys.map(x => texts(x)._2.length).foldLeft(0)(_ + _)
  }

  private def getLinkTextRate(texts: Map[String, (String, String)]): Double = {
    getTextLength(texts) match {
      case 0 => 0.0
      case l => getLinkTextLength(texts).toDouble / l
    }
  }

  private def printLinkTextRate(url: String, textLength: Int, linkTextLength: Int, rate: Double): Unit = {
    print(
      s"[${url}]",
      s"text length: ${textLength}",
      s"link text length: ${linkTextLength}",
      s"rate: ${rate}"
    )
  }

  private def use[A <: {def close()}, B](resource: A)(f: A => B): B = {
    try f(resource)
    finally {
      try resource.close
      catch { case _: Throwable => }
    }
  }

  try {
    args.foreach(f => use(Source.fromFile(f))(_.getLines.filterNot(_.startsWith("#")).zipWithIndex.foreach {
      case (url, index) =>
        try getHtmlText(url, f.replaceFirst("_.*$", ""), index)
        catch { case e: Throwable => println(e.getMessage) }
    }))

    // args.foreach(f => use(Source.fromFile(f))(_.getLines.filterNot(_.startsWith("#")).foreach{ url =>
    //   try {
    //     getScreenshotAndHtmlAndLinkText(url)
    //     // getScreenshotAndLinkText(url)
    //     // debug(url)
    //   } catch {
    //     case e: Throwable => println(e.getMessage)
    //   }
    // }))
  } catch {
    case e: Throwable => throw e
  }
}
