/*
 * Copyright 2014 Yubico.
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file or at
 * https://developers.google.com/open-source/licenses/bsd
 */

package com.yubico.u2f.server.data;

import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.google.gson.Gson;
import com.yubico.u2f.codec.ByteInputStream;
import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Objects;

public class Device implements Serializable {
  private static final long serialVersionUID = -142942195464329902L;

  private final byte[] keyHandle;
  private final byte[] publicKey;
  private final byte[] attestationCert;
  private int counter;

  public Device(
          byte[] keyHandle,
          byte[] publicKey,
          X509Certificate attestationCert,
          int counter) {
    this.keyHandle = keyHandle;
    this.publicKey = publicKey;
    this.attestationCert = attestationCert.getPublicKey().getEncoded();
    this.counter = counter;
  }

  public byte[] getKeyHandle() {
    return keyHandle;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public X509Certificate getAttestationCertificate() throws CertificateException {
    return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteInputStream(attestationCert));
  }
  
  public int getCounter() {
	return counter; 
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(
        keyHandle,
        publicKey, 
        attestationCert);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Device)) {
      return false;
    }
    Device that = (Device) obj;
    return Arrays.equals(this.keyHandle, that.keyHandle) 
        && Arrays.equals(this.publicKey, that.publicKey)
        && Arrays.equals(this.attestationCert, that.attestationCert);
  }
  
  @Override
  public String toString() {
    return "public_key: "
        + Base64.encodeBase64URLSafeString(publicKey) + "\n"
        + "key_handle: "
        + Base64.encodeBase64URLSafeString(keyHandle) + "\n"
        + "counter: "
        + counter + "\n"
        + "attestation certificate:\n"
        + attestationCert;
  }

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  public static Device fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, Device.class);
  }
}
