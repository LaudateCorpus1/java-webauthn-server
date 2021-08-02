// Copyright (c) 2018, Yubico AB
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.yubico.webauthn

import com.yubico.internal.util.scala.JavaConverters._
import com.yubico.webauthn.data.AssertionExtensionInputs
import com.yubico.webauthn.data.AuthenticatorAttachment
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.AuthenticatorTransport
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.Generators._
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import com.yubico.webauthn.data.PublicKeyCredentialParameters
import com.yubico.webauthn.data.RegistrationExtensionInputs
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.extension.appid.AppId
import com.yubico.webauthn.extension.appid.Generators._
import org.junit.runner.RunWith
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.util.Optional
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class RelyingPartyStartOperationSpec
    extends FunSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  def credRepo(
      credentials: Set[PublicKeyCredentialDescriptor]
  ): CredentialRepository =
    new CredentialRepository {
      override def getCredentialIdsForUsername(
          username: String
      ): java.util.Set[PublicKeyCredentialDescriptor] = credentials.asJava
      override def getUserHandleForUsername(
          username: String
      ): Optional[ByteArray] = ???
      override def getUsernameForUserHandle(
          userHandleBase64: ByteArray
      ): Optional[String] = ???
      override def lookup(
          credentialId: ByteArray,
          userHandle: ByteArray,
      ): Optional[RegisteredCredential] = ???
      override def lookupAll(
          credentialId: ByteArray
      ): java.util.Set[RegisteredCredential] = ???
    }

  def relyingParty(
      appId: Option[AppId] = None,
      credentials: Set[PublicKeyCredentialDescriptor] = Set.empty,
  ): RelyingParty =
    RelyingParty
      .builder()
      .identity(rpId)
      .credentialRepository(credRepo(credentials))
      .preferredPubkeyParams(List(PublicKeyCredentialParameters.ES256).asJava)
      .origins(Set.empty.asJava)
      .appId(appId.asJava)
      .build()

  val rpId = RelyingPartyIdentity
    .builder()
    .id("localhost")
    .name("Test")
    .build()

  val userId = UserIdentity
    .builder()
    .name("foo")
    .displayName("Foo")
    .id(new ByteArray(Array(0, 1, 2, 3)))
    .build()

  describe("RelyingParty.startRegistration") {

    it("sets excludeCredentials automatically.") {
      forAll { credentials: Set[PublicKeyCredentialDescriptor] =>
        val rp = relyingParty(credentials = credentials)
        val result = rp.startRegistration(
          StartRegistrationOptions
            .builder()
            .user(userId)
            .build()
        )

        result.getExcludeCredentials.asScala.map(_.asScala) should equal(
          Some(credentials)
        )
      }
    }

    it("sets challenge randomly.") {
      val rp = relyingParty()

      val request1 = rp.startRegistration(
        StartRegistrationOptions.builder().user(userId).build()
      )
      val request2 = rp.startRegistration(
        StartRegistrationOptions.builder().user(userId).build()
      )

      request1.getChallenge should not equal request2.getChallenge
      request1.getChallenge.size should be >= 32
      request2.getChallenge.size should be >= 32
    }

    it("allows setting authenticatorSelection.") {
      val authnrSel = AuthenticatorSelectionCriteria
        .builder()
        .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
        .requireResidentKey(true)
        .build()

      val pkcco = relyingParty().startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .authenticatorSelection(authnrSel)
          .build()
      )
      pkcco.getAuthenticatorSelection.asScala should equal(Some(authnrSel))
    }

    it("allows setting the timeout to empty.") {
      val pkcco = relyingParty().startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .timeout(Optional.empty[java.lang.Long])
          .build()
      )
      pkcco.getTimeout.asScala shouldBe empty
    }

    it("allows setting the timeout to a positive value.") {
      val rp = relyingParty()

      forAll(Gen.posNum[Long]) { timeout: Long =>
        val pkcco = rp.startRegistration(
          StartRegistrationOptions
            .builder()
            .user(userId)
            .timeout(timeout)
            .build()
        )

        pkcco.getTimeout.asScala should equal(Some(timeout))
      }
    }

    it("does not allow setting the timeout to zero or negative.") {
      an[IllegalArgumentException] should be thrownBy {
        StartRegistrationOptions
          .builder()
          .user(userId)
          .timeout(0)
      }

      an[IllegalArgumentException] should be thrownBy {
        StartRegistrationOptions
          .builder()
          .user(userId)
          .timeout(Optional.of[java.lang.Long](0L))
      }

      forAll(Gen.negNum[Long]) { timeout: Long =>
        an[IllegalArgumentException] should be thrownBy {
          StartRegistrationOptions
            .builder()
            .user(userId)
            .timeout(timeout)
        }

        an[IllegalArgumentException] should be thrownBy {
          StartRegistrationOptions
            .builder()
            .user(userId)
            .timeout(Optional.of[java.lang.Long](timeout))
        }
      }
    }

    it(
      "sets the appidExclude extension if the RP instance is given an AppId."
    ) {
      forAll { appId: AppId =>
        val rp = relyingParty(appId = Some(appId))
        val result = rp.startRegistration(
          StartRegistrationOptions
            .builder()
            .user(userId)
            .build()
        )

        result.getExtensions.getAppidExclude.asScala should equal(Some(appId))
      }
    }

    it("does not set the appidExclude extension if the RP instance is not given an AppId.") {
      val rp = relyingParty()
      val result = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .build()
      )

      result.getExtensions.getAppidExclude.asScala should equal(None)
    }

    it("by default always sets the credProps extension.") {
      forAll { extensions: RegistrationExtensionInputs =>
        println(extensions.getExtensionIds)
        println(extensions)

        val rp = relyingParty()
        val result = rp.startRegistration(
          StartRegistrationOptions
            .builder()
            .user(userId)
            .extensions(extensions)
            .build()
        )

        result.getExtensions.getCredProps should be(true)
      }
    }

    it("by default does not set the uvm extension.") {
      val rp = relyingParty()
      val result = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .build()
      )
      result.getExtensions.getUvm should be(false)
    }

    it("sets the uvm extension if enabled in StartRegistrationOptions.") {
      forAll { extensions: RegistrationExtensionInputs =>
        val rp = relyingParty()
        val result = rp.startRegistration(
          StartRegistrationOptions
            .builder()
            .user(userId)
            .extensions(extensions.toBuilder.uvm().build())
            .build()
        )

        result.getExtensions.getUvm should be(true)
      }
    }

    it("respects the requireResidentKey setting.") {
      val rp = relyingParty()

      val pkccoFalse = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .authenticatorSelection(
            AuthenticatorSelectionCriteria
              .builder()
              .requireResidentKey(false)
              .build()
          )
          .build()
      )
      val pkccoTrue = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .authenticatorSelection(
            AuthenticatorSelectionCriteria
              .builder()
              .requireResidentKey(true)
              .build()
          )
          .build()
      )

      pkccoFalse.getAuthenticatorSelection.get.isRequireResidentKey should be(
        false
      )
      pkccoTrue.getAuthenticatorSelection.get.isRequireResidentKey should be(
        true
      )
    }

    it("sets requireResidentKey to agree with residentKey.") {
      val rp = relyingParty()

      val pkccoDiscouraged = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .authenticatorSelection(
            AuthenticatorSelectionCriteria
              .builder()
              .residentKey(ResidentKeyRequirement.DISCOURAGED)
              .build()
          )
          .build()
      )
      val pkccoPreferred = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .authenticatorSelection(
            AuthenticatorSelectionCriteria
              .builder()
              .residentKey(ResidentKeyRequirement.PREFERRED)
              .build()
          )
          .build()
      )
      val pkccoRequired = rp.startRegistration(
        StartRegistrationOptions
          .builder()
          .user(userId)
          .authenticatorSelection(
            AuthenticatorSelectionCriteria
              .builder()
              .residentKey(ResidentKeyRequirement.REQUIRED)
              .build()
          )
          .build()
      )

      pkccoDiscouraged.getAuthenticatorSelection.get.isRequireResidentKey should be(
        false
      )
      pkccoPreferred.getAuthenticatorSelection.get.isRequireResidentKey should be(
        false
      )
      pkccoRequired.getAuthenticatorSelection.get.isRequireResidentKey should be(
        true
      )

      pkccoDiscouraged.getAuthenticatorSelection.get.getResidentKey should equal(
        ResidentKeyRequirement.DISCOURAGED
      )
      pkccoPreferred.getAuthenticatorSelection.get.getResidentKey should equal(
        ResidentKeyRequirement.PREFERRED
      )
      pkccoRequired.getAuthenticatorSelection.get.getResidentKey should equal(
        ResidentKeyRequirement.REQUIRED
      )
    }
  }

  describe("RelyingParty.startAssertion") {

    it("sets allowCredentials to empty if not given a username.") {
      forAll { credentials: Set[PublicKeyCredentialDescriptor] =>
        val rp = relyingParty(credentials = credentials)
        val result = rp.startAssertion(StartAssertionOptions.builder().build())

        result.getPublicKeyCredentialRequestOptions.getAllowCredentials.asScala shouldBe empty
      }
    }

    it("sets allowCredentials automatically if given a username.") {
      forAll { credentials: Set[PublicKeyCredentialDescriptor] =>
        val rp = relyingParty(credentials = credentials)
        val result = rp.startAssertion(
          StartAssertionOptions
            .builder()
            .username(userId.getName)
            .build()
        )

        result.getPublicKeyCredentialRequestOptions.getAllowCredentials.asScala
          .map(_.asScala.toSet) should equal(Some(credentials))
      }
    }

    it("includes transports in allowCredentials when available.") {
      forAll(
        Gen.nonEmptyContainerOf[Set, AuthenticatorTransport](
          arbitrary[AuthenticatorTransport]
        ),
        arbitrary[PublicKeyCredentialDescriptor],
        arbitrary[PublicKeyCredentialDescriptor],
        arbitrary[PublicKeyCredentialDescriptor],
      ) {
        (
            cred1Transports: Set[AuthenticatorTransport],
            cred1: PublicKeyCredentialDescriptor,
            cred2: PublicKeyCredentialDescriptor,
            cred3: PublicKeyCredentialDescriptor,
        ) =>
          val rp = relyingParty(credentials =
            Set(
              cred1.toBuilder.transports(cred1Transports.asJava).build(),
              cred2.toBuilder
                .transports(
                  Optional.of(Set.empty[AuthenticatorTransport].asJava)
                )
                .build(),
              cred3.toBuilder
                .transports(
                  Optional.empty[java.util.Set[AuthenticatorTransport]]
                )
                .build(),
            )
          )
          val result = rp.startAssertion(
            StartAssertionOptions
              .builder()
              .username(userId.getName)
              .build()
          )

          val requestCreds =
            result.getPublicKeyCredentialRequestOptions.getAllowCredentials.get.asScala
          requestCreds.head.getTransports.asScala should equal(
            Some(cred1Transports.asJava)
          )
          requestCreds(1).getTransports.asScala should equal(
            Some(Set.empty.asJava)
          )
          requestCreds(2).getTransports.asScala should equal(None)
      }
    }

    it("sets challenge randomly.") {
      val rp = relyingParty()

      val request1 = rp.startAssertion(StartAssertionOptions.builder().build())
      val request2 = rp.startAssertion(StartAssertionOptions.builder().build())

      request1.getPublicKeyCredentialRequestOptions.getChallenge should not equal request2.getPublicKeyCredentialRequestOptions.getChallenge
      request1.getPublicKeyCredentialRequestOptions.getChallenge.size should be >= 32
      request2.getPublicKeyCredentialRequestOptions.getChallenge.size should be >= 32
    }

    it("sets the appid extension if the RP instance is given an AppId.") {
      forAll { appId: AppId =>
        val rp = relyingParty(appId = Some(appId))
        val result = rp.startAssertion(
          StartAssertionOptions
            .builder()
            .username(userId.getName)
            .build()
        )

        result.getPublicKeyCredentialRequestOptions.getExtensions.getAppid.asScala should equal(
          Some(appId)
        )
      }
    }

    it("does not set the appid extension if the RP instance is not given an AppId.") {
      val rp = relyingParty()
      val result = rp.startAssertion(
        StartAssertionOptions
          .builder()
          .username(userId.getName)
          .build()
      )

      result.getPublicKeyCredentialRequestOptions.getExtensions.getAppid.asScala should equal(
        None
      )
    }

    it("allows setting the timeout to empty.") {
      val req = relyingParty().startAssertion(
        StartAssertionOptions
          .builder()
          .timeout(Optional.empty[java.lang.Long])
          .build()
      )
      req.getPublicKeyCredentialRequestOptions.getTimeout.asScala shouldBe empty
    }

    it("allows setting the timeout to a positive value.") {
      val rp = relyingParty()

      forAll(Gen.posNum[Long]) { timeout: Long =>
        val req = rp.startAssertion(
          StartAssertionOptions
            .builder()
            .timeout(timeout)
            .build()
        )

        req.getPublicKeyCredentialRequestOptions.getTimeout.asScala should equal(
          Some(timeout)
        )
      }
    }

    it("does not allow setting the timeout to zero or negative.") {
      an[IllegalArgumentException] should be thrownBy {
        StartAssertionOptions
          .builder()
          .timeout(0)
      }

      an[IllegalArgumentException] should be thrownBy {
        StartAssertionOptions
          .builder()
          .timeout(Optional.of[java.lang.Long](0L))
      }

      forAll(Gen.negNum[Long]) { timeout: Long =>
        an[IllegalArgumentException] should be thrownBy {
          StartAssertionOptions
            .builder()
            .timeout(timeout)
        }

        an[IllegalArgumentException] should be thrownBy {
          StartAssertionOptions
            .builder()
            .timeout(Optional.of[java.lang.Long](timeout))
        }
      }
    }

    it("by default does not set the uvm extension.") {
      val rp = relyingParty()
      val result = rp.startAssertion(
        StartAssertionOptions
          .builder()
          .build()
      )
      result.getPublicKeyCredentialRequestOptions.getExtensions.getUvm should be(
        false
      )
    }

    it("sets the uvm extension if enabled in StartRegistrationOptions.") {
      forAll { extensions: AssertionExtensionInputs =>
        val rp = relyingParty()
        val result = rp.startAssertion(
          StartAssertionOptions
            .builder()
            .extensions(extensions.toBuilder.uvm().build())
            .build()
        )

        result.getPublicKeyCredentialRequestOptions.getExtensions.getUvm should be(
          true
        )
      }
    }
  }

}
