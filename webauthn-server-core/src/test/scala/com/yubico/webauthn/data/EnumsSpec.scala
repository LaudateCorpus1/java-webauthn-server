package com.yubico.webauthn.data

import com.yubico.internal.util.JacksonCodecs
import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class EnumsSpec
    extends FunSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  val json = JacksonCodecs.json()

  describe("AttestationConveyancePreference") {
    describe("can be parsed from JSON") {
      it("but throws IllegalArgumentException for unknown values.") {
        val result = Try(
          json.readValue("\"foo\"", classOf[AttestationConveyancePreference])
        )
        result.failed.get.getCause shouldBe an[IllegalArgumentException]
      }
    }
  }

  describe("AuthenticatorAttachment") {
    describe("can be parsed from JSON") {
      it("but throws IllegalArgumentException for unknown values.") {
        val result = Try(
          json.readValue("\"foo\"", classOf[AuthenticatorAttachment])
        )
        result.failed.get.getCause shouldBe an[IllegalArgumentException]
      }
    }
  }

  describe("AuthenticatorTransport") {
    it("sorts in lexicographical order.") {
      val list = List(
        AuthenticatorTransport.USB,
        AuthenticatorTransport.BLE,
        AuthenticatorTransport.NFC,
        AuthenticatorTransport.INTERNAL,
      )
      list.sorted should equal(
        List(
          AuthenticatorTransport.BLE,
          AuthenticatorTransport.INTERNAL,
          AuthenticatorTransport.NFC,
          AuthenticatorTransport.USB,
        )
      )
    }
  }

  describe("COSEAlgorithmIdentifier") {
    describe("can be parsed from JSON") {
      it("but throws IllegalArgumentException for unknown values.") {
        val result = Try(
          json.readValue("1337", classOf[COSEAlgorithmIdentifier])
        )
        result.failed.get.getCause shouldBe an[IllegalArgumentException]
      }
    }
  }

  describe("PublicKeyCredentialType") {
    describe("can be parsed from JSON") {
      it("but throws IllegalArgumentException for unknown values.") {
        val result = Try(
          json.readValue("\"foo\"", classOf[PublicKeyCredentialType])
        )
        result.failed.get.getCause shouldBe an[IllegalArgumentException]
      }
    }
  }

  describe("ResidentKeyRequirement") {
    describe("can be parsed from JSON") {
      it("but throws IllegalArgumentException for unknown values.") {
        val result = Try(
          json.readValue("\"foo\"", classOf[ResidentKeyRequirement])
        )
        result.failed.get.getCause shouldBe an[IllegalArgumentException]
      }
    }
  }

  describe("UserVerificationRequirement") {
    describe("can be parsed from JSON") {
      it("but throws IllegalArgumentException for unknown values.") {
        val result = Try(
          json.readValue("\"foo\"", classOf[UserVerificationRequirement])
        )
        result.failed.get.getCause shouldBe an[IllegalArgumentException]
      }
    }
  }

}
