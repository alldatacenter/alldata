package third

/**
  * Created by wulinhao on 2020/03/26.
  */
case class Aa(a: Int = 0, b: Int)

class Main {
  def a(): Int = {
    val a = 1
    val b = if (a > 0) {
      1
    }
    else {
      0
    }

    val aa = Aa(1, 0)
    Aa(b = 0, a = 1)
    b
  }
}
