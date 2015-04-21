package brainsuck

import scala.collection.mutable.ArrayBuffer

import brainsuck.RulesExecutor.{Batch, FixedPoint, Once}
import org.scalatest.FunSuite

trait TestUtils { this: FunSuite =>
  val contractionOptimizer = new Optimizer {
    override def batches = Batch("test", Contraction :: Nil, FixedPoint.Unlimited) :: Nil
  }

  val fullOptimizer = new Optimizer {
    override def batches =
      Batch("Contraction", Contraction :: Nil, FixedPoint.Unlimited) ::
        Batch("LoopSimplification", LoopSimplification :: Nil, Once) :: Nil
  }

  def makeMachine(pointer: Int = 0, initialMemory: Seq[Int] = Seq(0)): Machine =
    new Machine(pointer, new Memory(ArrayBuffer.empty[Int] ++ initialMemory))

  def checkExecution(machine: Machine, instructions: Instruction, expected: Machine): Unit = {
    Instruction.untilHalt(instructions, machine)
    assert(machine same expected)
  }

  def checkWithOptimizer
      (optimizer: Optimizer)
      (source: String, instruction: Instruction): Unit = {
    assertResult(instruction, s"Wrong optimized code for '$source'") {
      optimizer(BrainsuckParser(source))
    }
  }

  def checkExecutionWithOptimizer
      (optimizer: Optimizer)
      (source: String,
       tree: Instruction,
       initialMachine: => Machine,
       expected: Machine): Unit = {
    val m1 = initialMachine
    val unoptimizedCode = BrainsuckParser(source)
    Instruction.untilHalt(unoptimizedCode, m1)
    assert(
      m1 same expected,
      s"""Wrong machine state:
         |Expected state: $expected
         |Actual state:   $m1
         |Raw code:       $unoptimizedCode
       """.stripMargin)

    val m2 = initialMachine
    val optimizedCode = optimizer(BrainsuckParser(source))

    assert(
      optimizedCode same tree,
      s"""Wrong optimized code for $source
         |Expected: $tree
         |Actual:   $optimizedCode
       """.stripMargin
    )

    Instruction.untilHalt(optimizedCode, m2)
    assert(
      m2 same expected,
      s"""Wrong machine state:
         |Expected state: $expected
         |Actual state:   $m2
         |Optimized code: $optimizedCode
       """.stripMargin)
  }
}
