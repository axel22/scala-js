/*                     __                                               *\
**     ________ ___   / /  ___      __ ____  Scala.js Test Suite        **
**    / __/ __// _ | / /  / _ | __ / // __/  (c) 2013, LAMP/EPFL        **
**  __\ \/ /__/ __ |/ /__/ __ |/_// /_\ \    http://scala-js.org/       **
** /____/\___/_/ |_/____/_/ | |__/ /____/                               **
**                          |/____/                                     **
\*                                                                      */
package org.scalajs.testsuite.javalib.io

import scala.language.implicitConversions

import java.io._

import scala.scalajs.js
import org.scalajs.jasminetest.JasmineTest

object PrintWriterTest extends JasmineTest {
  private def newPrintWriter(
      autoFlush: Boolean = false): (MockPrintWriter, MockStringWriter) = {
    val sw = new MockStringWriter
    val pw = new MockPrintWriter(sw, autoFlush)
    (pw, sw)
  }

  describe("java.io.PrintWriter") {
    it("flush") {
      val (pw, sw) = newPrintWriter()
      pw.print("hello")
      expect(sw.flushed).toBeFalsy
      pw.flush()
      expect(sw.flushed).toBeTruthy
    }

    it("close") {
      val (pw, sw) = newPrintWriter()
      pw.write("begin")
      expect(sw.flushed).toBeFalsy

      pw.close()
      expect(sw.flushed).toBeTruthy
      expect(sw.closed).toBeTruthy
      expect(pw.checkError()).toBeFalsy

      // can double-close without error
      pw.close()
      expect(pw.checkError()).toBeFalsy
      pw.clearError()

      // when closed, other operations cause error
      def expectCausesError(body: => Unit): Unit = {
        body
        expect(pw.checkError()).toBeTruthy
        pw.clearError()
      }
      expectCausesError(pw.print("never printed"))
      expectCausesError(pw.write(Array('a', 'b')))
      expectCausesError(pw.append("hello", 1, 3))
      expectCausesError(pw.flush())

      // at the end of it all, sw is still what it was when it was closed
      expect(sw.toString()).toBe("begin")
    }

    it("write, does not flush even with \\n") {
      def test(body: PrintWriter => Unit, expected: String): Unit = {
        val (pw, sw) = newPrintWriter(autoFlush = true)
        body(pw)
        expect(sw.flushed).toBeFalsy
        expect(pw.checkError()).toBeFalsy
        expect(sw.toString()).toBe(expected)
      }

      test(_.write('\n'), "\n")
      test(_.write("hello\n"), "hello\n")
      test(_.write("hello\nworld", 3, 3), "lo\n")
      test(_.write(Array('A', '\n')), "A\n")
      test(_.write(Array('A', 'B', '\n', 'C'), 1, 2), "B\n")
    }

    it("print, does not flush even with \\n") {
      def test(body: PrintWriter => Unit, expected: String): Unit = {
        val (pw, sw) = newPrintWriter(autoFlush = true)
        body(pw)
        expect(sw.flushed).toBeFalsy
        expect(pw.checkError()).toBeFalsy
        expect(sw.toString()).toBe(expected)
      }

      test(_.print(true), "true")
      test(_.print('Z'), "Z")
      test(_.print('\n'), "\n")
      test(_.print(5), "5")
      test(_.print(1234567891011L), "1234567891011")
      test(_.print(1.5f), "1.5")
      test(_.print(Math.PI), "3.141592653589793")
      test(_.print(Array('A', '\n')), "A\n")
      test(_.print("hello\n"), "hello\n")
      test(_.print(null: String), "null")
      test(_.print((1, 2)), "(1,2)")
      test(_.print(null: AnyRef), "null")
    }

    for (autoFlush <- Seq(true, false)) {
      val title =
        if (autoFlush) "println forwards and flushes when autoFlush is true"
        else           "println forwards, does not flush when autoFlush is false"
      it(title) {
        def test(body: PrintWriter => Unit, expected: String): Unit = {
          val (pw, sw) = newPrintWriter(autoFlush = autoFlush)
          body(pw)
          if (autoFlush) expect(sw.flushed).toBeTruthy
          else           expect(sw.flushed).toBeFalsy
          expect(pw.checkError()).toBeFalsy
          expect(sw.toString()).toBe(expected)
        }

        test(_.println(), "\n")
        test(_.println(true), "true\n")
        test(_.println('Z'), "Z\n")
        test(_.println('\n'), "\n\n")
        test(_.println(5), "5\n")
        test(_.println(1234567891011L), "1234567891011\n")
        test(_.println(1.5f), "1.5\n")
        test(_.println(Math.PI), "3.141592653589793\n")
        test(_.println(Array('A', '\n')), "A\n\n")
        test(_.println("hello\n"), "hello\n\n")
        test(_.println(null: String), "null\n")
        test(_.println((1, 2)), "(1,2)\n")
        test(_.println(null: AnyRef), "null\n")
      }
    }

    for (autoFlush <- Seq(true, false)) {
      val title =
        if (autoFlush) "printf/format, which flushes when autoFlush is true"
        else           "printf/format, does not flush when autoFlush is false"
      it(title) {
        def test(body: PrintWriter => Unit, expected: String): Unit = {
          val (pw, sw) = newPrintWriter(autoFlush = autoFlush)
          body(pw)
          if (autoFlush) expect(sw.flushed).toBeTruthy
          else           expect(sw.flushed).toBeFalsy
          expect(pw.checkError()).toBeFalsy
          expect(sw.toString()).toBe(expected)
        }

        test(_.printf("%04d", Int.box(5)), "0005")
        test(_.format("%.5f", Double.box(Math.PI)), "3.14159")
      }
    }

    it("append, does not flush even with \\n") {
      def test(body: PrintWriter => Unit, expected: String): Unit = {
        val (pw, sw) = newPrintWriter(autoFlush = true)
        body(pw)
        expect(sw.flushed).toBeFalsy
        expect(pw.checkError()).toBeFalsy
        expect(sw.toString()).toBe(expected)
      }

      test(_.append("hello\n"), "hello\n")
      test(_.append(null: CharSequence), "null")
      test(_.append("hello\nworld", 3, 6), "lo\n")
      test(_.append(null: CharSequence, 1, 2), "u")
      test(_.append('A'), "A")
      test(_.append('\n'), "\n")
    }

    it("traps all IOException and updates checkError") {
      def test(body: PrintWriter => Unit): Unit = {
        val (pw, sw) = newPrintWriter()
        sw.throwing = true
        body(pw)
        expect(pw.checkError()).toBeTruthy
      }

      test(_.flush())
      test(_.close())

      test(_.write('Z'))
      test(_.write("booh"))
      test(_.write("booh", 1, 1))
      test(_.write(Array('A', 'B')))
      test(_.write(Array('A', 'B'), 1, 1))

      test(_.print(true))
      test(_.print('Z'))
      test(_.print('\n'))
      test(_.print(5))
      test(_.print(1234567891011L))
      test(_.print(1.5f))
      test(_.print(Math.PI))
      test(_.print(Array('A', '\n')))
      test(_.print("hello\n"))
      test(_.print(null: String))
      test(_.print((1, 2)))
      test(_.print(null: AnyRef))

      test(_.println())
      test(_.println(true))
      test(_.println('Z'))
      test(_.println('\n'))
      test(_.println(5))
      test(_.println(1234567891011L))
      test(_.println(1.5f))
      test(_.println(Math.PI))
      test(_.println(Array('A', '\n')))
      test(_.println("hello\n"))
      test(_.println(null: String))
      test(_.println((1, 2)))
      test(_.println(null: AnyRef))

      test(_.append("hello\n"))
      test(_.append(null: CharSequence))
      test(_.append("hello\nworld", 3, 6))
      test(_.append(null: CharSequence, 1, 2))
      test(_.append('A'))
      test(_.append('\n'))
    }
  }

  /** A PrintWriter that exposes various hooks for testing purposes. */
  private class MockPrintWriter(out: Writer,
      autoFlush: Boolean) extends PrintWriter(out, autoFlush) {
    def this(out: Writer) = this(out, false)

    override def clearError(): Unit = super.clearError()
  }

  /** A StringWriter that exposes various hooks for testing purposes. */
  private class MockStringWriter extends StringWriter {
    private var _flushed: Boolean = true
    private var _closed: Boolean = false

    var throwing: Boolean = false

    def flushed: Boolean = _flushed
    def closed: Boolean = _closed

    private def maybeThrow(): Unit = {
      if (throwing)
        throw new IOException("MockStringWriter throws")
    }

    private def writeOp[A](op: => A): A = {
      maybeThrow()
      _flushed = false
      op
    }

    override def flush(): Unit = {
      maybeThrow()
      super.flush()
      _flushed = true
    }

    override def close(): Unit = {
      maybeThrow()
      super.close()
      _closed = true
    }

    override def append(c: Char): StringWriter =
      writeOp(super.append(c))

    override def append(csq: CharSequence): StringWriter =
      writeOp(super.append(csq))

    override def append(csq: CharSequence, start: Int, end: Int): StringWriter =
      writeOp(super.append(csq, start, end))

    override def write(c: Int): Unit =
      writeOp(super.write(c))

    override def write(cbuf: Array[Char]): Unit =
      writeOp(super.write(cbuf))

    override def write(cbuf: Array[Char], off: Int, len: Int): Unit =
      writeOp(super.write(cbuf, off, len))

    override def write(str: String): Unit =
      writeOp(super.write(str))

    override def write(str: String, off: Int, len: Int): Unit =
      writeOp(super.write(str, off, len))
  }
}