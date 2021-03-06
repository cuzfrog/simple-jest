package anywhere

import org.scalajs.dom

object Test1 extends sjest.JestSuite {
  test("this is my first test") {
    println("do some test1!")
    expect(1 + 2).toBe(3)
  }

  test("this is a failed test") {
    expect(1 + 1).toBe(5)
  }

  test("dom test") {
    val div = dom.document.createElement("div")
    div.setAttribute("id","my-id-is-app")
    val body = dom.document.body.appendChild(div)
    val found = dom.document.getElementById("my-id-is-app")
    expect(found).toBe(div)
  }
}
