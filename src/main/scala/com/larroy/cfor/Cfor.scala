package com.larroy.cfor

import scala.language.higherKinds
import scala.language.experimental.macros

object compat {
  type Context = scala.reflect.macros.whitebox.Context

  def freshTermName[C <: Context](c: C)(s: String) =
    c.universe.TermName(c.freshName(s))

  def termName[C <: Context](c: C)(s: String) =
    c.universe.TermName(s)

  def typeCheck[C <: Context](c: C)(t: c.Tree) =
    c.typecheck(t)

  def resetLocalAttrs[C <: Context](c: C)(t: c.Tree) =
    c.untypecheck(t)

  def setOrig[C <: Context](c: C)(tt: c.universe.TypeTree, t: c.Tree) =
    c.universe.internal.setOriginal(tt, t)
}

import compat._

case class SyntaxUtil[C <: Context with Singleton](val c: C) {

  import c.universe._

  def name(s: String) = freshTermName(c)(s + "$")

  def names(bs: String*) = bs.toList.map(name)

  def isClean(es: c.Expr[_]*): Boolean =
    es.forall {
      _.tree match {
        case t @ Ident(_: TermName) if t.symbol.asTerm.isStable => true
        case Function(_, _) => true
        case _ => false
      }
    }
}

class InlineUtil[C <: Context with Singleton](val c: C) {
  import c.universe._
  // This is Scala reflection source compatibility hack between Scala 2.10 and 2.11
  //import compat._

  def inlineAndReset[T](tree: Tree): c.Expr[T] = {
    val inlined = inlineApplyRecursive(tree)
    c.Expr[T](resetLocalAttrs(c)(inlined))
  }

  def inlineApplyRecursive(tree: Tree): Tree = {
    val ApplyName = termName(c)("apply")

    class InlineSymbol(symbol: Symbol, value: Tree) extends Transformer {
      override def transform(tree: Tree): Tree = tree match {
        case Ident(_) if tree.symbol == symbol =>
          value
        case tt: TypeTree if tt.original != null =>
          //super.transform(TypeTree().setOriginal(transform(tt.original)))
          super.transform(setOrig(c)(TypeTree(), transform(tt.original)))
        case _ =>
          super.transform(tree)
      }
    }

    object InlineApply extends Transformer {
      def inlineSymbol(symbol: Symbol, body: Tree, arg: Tree): Tree =
        new InlineSymbol(symbol, arg).transform(body)

      override def transform(tree: Tree): Tree = tree match {
        case Apply(Select(Function(params, body), ApplyName), args) =>
          params.zip(args).foldLeft(body) { case (b, (param, arg)) =>
            inlineSymbol(param.symbol, b, arg)
          }

        case Apply(Function(params, body), args) =>
          params.zip(args).foldLeft(body) { case (b, (param, arg)) =>
            inlineSymbol(param.symbol, b, arg)
          }

        case _ =>
          super.transform(tree)
      }
    }

    InlineApply.transform(tree)
  }
}


object cfor {
    def apply[A](init:A)(test:A => Boolean, next:A => A)(body:A => Unit): Unit =
    macro cforMacro[A]

      def cforMacro[A](c: Context)(init: c.Expr[A])
     (test: c.Expr[A => Boolean], next: c.Expr[A => A])
     (body: c.Expr[A => Unit]): c.Expr[Unit] = {


    import c.universe._
    val util = SyntaxUtil[c.type](c)
    val index = util.name("index")

    /**
     * If our arguments are all "clean" (anonymous functions or simple
     * identifiers) then we can go ahead and just inline them directly
     * into a while loop.
     *
     * If one or more of our arguments are "dirty" (something more
     * complex than an anonymous function or simple identifier) then
     * we will go ahead and bind each argument to a val just to be
     * safe.
     */
    val tree = if (util.isClean(test, next, body)) {
      q"""
      var $index = $init
      while ($test($index)) {
        $body($index)
        $index = $next($index)
      }
      """

    } else {
      val testName = util.name("test")
      val nextName = util.name("next")
      val bodyName = util.name("body")

      q"""
      val $testName: Int => Boolean = $test
      val $nextName: Int => Int = $next
      val $bodyName: Int => Unit = $body
      var $index: Int = $init
      while ($testName($index)) {
        $bodyName($index)
        $index = $nextName($index)
      }
      """
    }

    /**
     * Instead of just returning 'tree', we will go ahead and inline
     * anonymous functions which are immediately applied.
      * v     */
    new InlineUtil[c.type](c).inlineAndReset[Unit](tree)
  }
}




