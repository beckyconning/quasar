/*
 * Copyright 2014–2018 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.qscript.qsu

import quasar.contrib.matryoshka._
import quasar.fp._
import quasar.fp.ski._
import quasar.qscript.{construction, ExcludeId, IdOnly, IdStatus, IncludeId, SrcHole}
import quasar.qscript.qsu.{QScriptUniform => QSU}, QSU.ShiftTarget
import ApplyProvenance.AuthenticatedQSU
import QSUGraph.Extractors._

import matryoshka.{Hole => _, _}
import scalaz.syntax.equal._
import scalaz.syntax.foldable._

final class ResolveOwnIdentities[T[_[_]]: BirecursiveT: ShowT: EqualT] private () extends QSUTTypes[T] {

  val func = construction.Func[T]

  def apply(aqsu: AuthenticatedQSU[T]): AuthenticatedQSU[T] = {
    aqsu.copy(graph = aqsu.graph.rewrite {
      case qg @ LeftShift(source, struct, idStatus, onUndefined, repair, rotation)
        if repair.element(ShiftTarget.AccessLeftTarget(Access.Id(IdAccess.Identity(source.root), SrcHole))) =>

        val newRepair = repair.flatMap {
          case ShiftTarget.AccessLeftTarget(Access.Id(IdAccess.Identity(symbol), _)) if symbol == qg.root =>
            func.ProjectIndexI(func.RightTarget, 0)
          case ShiftTarget.AccessLeftTarget(access) =>
            func.AccessLeftTarget(κ(access))
          case ShiftTarget.RightTarget() =>
            if (idStatus === ExcludeId)
              func.ProjectIndexI(func.RightTarget, 1)
            else
              func.RightTarget
          case ShiftTarget.LeftTarget() =>
            scala.sys.error("ShiftTarget.LeftTarget in ResolveOwnIdentities")
        }

        val newIdStatus: IdStatus =
          if (idStatus === IdOnly) IdOnly else IncludeId

        qg.overwriteAtRoot(QSU.LeftShift(source.root, struct, newIdStatus, onUndefined, newRepair, rotation))
    })
  }
}

object ResolveOwnIdentities {
  def apply[T[_[_]]: BirecursiveT: ShowT: EqualT](aqsu: AuthenticatedQSU[T]): AuthenticatedQSU[T] =
    new ResolveOwnIdentities[T].apply(aqsu)
}
