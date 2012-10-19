package blueeyes.json

import org.scalacheck._
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import scalaz.Ordering._

object JSchemaSpec extends Specification with ScalaCheck with ArbitraryJPath with ArbitraryJValue {
  override val defaultPrettyParams = Pretty.Params(2)

  def f(v: JValue): JSchema = JSchema.fixed(v)
  def pf(s: String): JSchema = JSchema.fixed(JParser.parse(s))

  "JSchema.JSON" should {
    "validate all JSON without exploding" in {
      check { (value: JValue) =>
        JSchema.JSON.validate(value)
      }
    }
  }

  "JSchema.fixed" should {
    "validate value that created the schema" in {
      check { (value: JValue) =>
        JSchema.fixed(value).validate(value)
      }
    }

    "validate only the value that created the schema" in {
      check { (tuple: (JValue, JValue)) =>
        val (v1, v2) = tuple

        (v1 == v2) | !JSchema.fixed(v1).validate(v2)
      }
    }

    "not break regression" in {
      val v1 = JParser.parse("""{"yitAwQemwwsadhpeGj105763":"wfetLyqmpjrkksvnekdqetmx","zykzjslLdcmtHmusqnwwzft202253":-4.611686018427387904E-2147483609,"cyqk382845":1E+1983360066}""")

      val v2 = JObject.empty

      JSchema.fixed(v1).validate(v2) must beFalse
    }
  }

  "JSchema.minimize" should {
    "when bounded" >> {
      "convert homogeneous fixed to singleton unfixed" in {
        val fixed = f(JNum(1)) | f(JNum(2)) | f(JNum(3)) | f(JNum(4)) | f(JNum(5)) | f(JNum(6))

        fixed.minimize(5) mustEqual JNumSchema
      }

      "convert heterogeneous fixed to heterogeneous unfixed" in {
        val fixed = f(JNum(1)) | f(JNum(2)) | f(JNum(3)) | f(JString("foo")) | f(JNum(5)) | f(JNum(6))

        fixed.minimize(5) mustEqual (JNumSchema | JStringSchema)
      }

      "convert heterogeneous objects to object value schemas" in {
        val fixed = pf(""" {"foo0": "bar1"} """) |
                    pf(""" {"foo1": "bar1"} """) |
                    pf(""" {"foo2": "bar2"} """) |
                    pf(""" {"foo3": "bar2"} """)

        fixed.minimize(3) mustEqual JObjectValueSchema(JFixedSchema(JString("bar1")) | JFixedSchema(JString("bar2")))
      }


      "convert heterogeneous objects to object value schemas and convert fields" in {
        val fixed = pf(""" {"foo0": "bar0"} """) |
                    pf(""" {"foo1": "bar1"} """) |
                    pf(""" {"foo2": "bar2"} """) |
                    pf(""" {"foo3": "bar3"} """)

        fixed.minimize(3) mustEqual JObjectValueSchema(JStringSchema)
      }
    }
  }

  "JSchema.unfixed" should {
    "validate value that created the schema" in {
      check { (value: JValue) =>
        JSchema.unfixed(value).validate(value)
      }
    }

    "validate another value with the same schema" in {
      check { (v1: JValue) =>
        val v2 = v1.mapDown { 
          case JNum(_) => JNum(1L)
          case JString(_) => JString("foo blah blah")
          case JBool(v) => JBool(!v)
          case x => x
        }

        JSchema.unfixed(v1).validate(v2)
      }
    }
  }
}