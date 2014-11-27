package slamdata.engine.javascript

import scala.collection.immutable.ListMap

import slamdata.engine.analysis.fixplate._

/**
 ADT for a simplified, composable, core language for JavaScript. Provides only
 expressions, including lets.
*/
sealed trait JsCore[+A]
object JsCore {
  case class Literal(value: Js.Lit) extends JsCore[Nothing]
  case class Ident(name: String) extends JsCore[Nothing]

  case class Access[A](expr: A, key: A) extends JsCore[A]
  case class Call[A](callee: A, args: List[A]) extends JsCore[A]

  case class UnOp[A](op: String, arg: A) extends JsCore[A]
  case class BinOp[A](op: String, left: A, right: A) extends JsCore[A]

  // TODO: Cond
  // TODO: Fn?

  case class Arr[A](values: List[A]) extends JsCore[A]
  case class Obj[A](values: ListMap[String, A]) extends JsCore[A]

  case class Let[A](bindings: ListMap[String, A], expr: A) extends JsCore[A]
  
  def Select(expr: Term[JsCore], name: String): Access[Term[JsCore]] = Access(expr, Literal(Js.Str(name)).fix)

  implicit class UnFixedJsCoreOps(expr: JsCore[Term[JsCore]]) {
    def fix = Term[JsCore](expr)
  }

  implicit class JsCoreOps(expr: Term[JsCore]) {
    def toJs: Js.Expr = expr.unFix match {
      case Literal(value)      => value
      case Ident(name)         => Js.Ident(name)

      case Access(expr, key)   => key.unFix match {
        case Literal(Js.Str(name @ Js.SimpleNamePattern())) => Js.Select(expr.toJs, name)
        case _ => Js.Access(expr.toJs, key.toJs)
      }
      case Call(callee, args)  => Js.Call(callee.toJs, args.map(_.toJs))

      case UnOp(op, arg)       => Js.UnOp(op, arg.toJs)
      case BinOp(op, left, right) => Js.BinOp(op, left.toJs, right.toJs)

      case Arr(values)         => Js.AnonElem(values.map(_.toJs))
      case Obj(values)         => Js.AnonObjDecl(values.toList.map { case (k, v) => k -> v.toJs })

      case Let(bindings, expr) => 
        Js.Call(
          Js.AnonFunDecl(
            bindings.keys.toList, 
            List(Js.Return(expr.toJs))),
          bindings.values.map(_.toJs).toList)
    }
  }
}

case class JsMacro(expr: Term[JsCore] => Term[JsCore]) {
  def apply(x: Term[JsCore]) = expr(x)
  
  def >>>(right: JsMacro): JsMacro = JsMacro(x => right.expr(this.expr(x)))
  
  override def toString = expr(JsCore.Ident("_").fix).toJs.render(0)
  
  private val impossibleName = JsCore.Ident("\\").fix
  override def equals(obj: Any) = obj match {
    case JsMacro(expr2) => expr(impossibleName) == expr2(impossibleName)
    case _ => false
  }
  override def hashCode = expr(impossibleName).hashCode
}